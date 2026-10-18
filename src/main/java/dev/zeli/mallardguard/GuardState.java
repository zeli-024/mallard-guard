package dev.zeli.mallardguard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GuardState {
    private static final Map<UUID, GuardState> STATES = new HashMap<>();
    private static final Map<UUID, Long> ATTACK_LOCKS = new HashMap<>();
    private static long clientAttackUntil = Long.MIN_VALUE;

    public static boolean attackReady(Player player) {
        long until = player.level().isClientSide ? clientAttackUntil : ATTACK_LOCKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        return !GuardCombatEffects.stunned(player) && player.level().getGameTime() >= until && player.getAttackStrengthScale(0.0F) >= 1.0F;
    }

    public static void feinted(ServerPlayer player) {
        // A windup cancellation may release the input lock, never an actual hit's cooldown.
        if (net.neoforged.fml.ModList.get().isLoaded("bettercombat") && player.getAttackStrengthScale(0) >= 1.0F)
            ATTACK_LOCKS.remove(player.getUUID());
    }

    public static void clearClientAttackLock() { clientAttackUntil = Long.MIN_VALUE; }

    /** Server authority for an active stance, independent of the client presentation. */
    public static boolean guarding(Player player) {
        GuardState state=STATES.get(player.getUUID());
        return player.isAlive() && state!=null && state.held && state.phase!=0;
    }

    public static void attackStarted(Player player) {
        if(!player.level().isClientSide && guarding(player))return;
        long until = player.level().getGameTime() + Math.max(1, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()));
        if (player.level().isClientSide) clientAttackUntil = Math.max(clientAttackUntil, until);
        else if (player instanceof ServerPlayer serverPlayer) {
            ATTACK_LOCKS.merge(player.getUUID(), until, Math::max);
            // Attacking invalidates an active guard before the next damage event.
            input(serverPlayer, false, false);
        }
    }

    public static void attacked(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if(event.isCanceled() || event.getEntity().level().isClientSide)return;
        if(guarding(event.getEntity()))event.setCanceled(true);
        else attackStarted(event.getEntity());
    }
    public static final ResourceLocation GUARD_SLOWDOWN = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "guard_slowdown");
    private boolean held;
    private int elapsed;
    private int recharge;
    private int rechargeDuration;
    private int phase;
    private ItemStack heldItem = ItemStack.EMPTY;
    private InteractionHand guardHand = InteractionHand.MAIN_HAND;
    private InteractionHand lastToolHand;
    private int toolHandRun;
    private long safetyThroughTick = -1;
    private int blocksTaken;
    private boolean breakPending;
    private boolean guardReleaseRequired;
    private GuardPackets.Status lastStatus;
    private long inputId;
    private boolean inputAccepted, inputProcessing;
    private static boolean clientCooldownPreventsGuard = true;
    private static boolean clientAllowEmptyHand;
    private static GuardPackets.Settings clientRules;
    private static String clientShieldBlacklist="";
    private static int clientShieldPriority, clientHandPriority;
    private static boolean clientCrouchOffhand;
    public static void setClientHandRules(int shieldPriority, int handPriority, boolean crouchOffhand) {
        clientShieldPriority=shieldPriority;clientHandPriority=handPriority;clientCrouchOffhand=crouchOffhand;
    }
    private static final GuardItemRules.Source CLIENT_INCLUDED=new GuardItemRules.Source(),CLIENT_EXCLUDED=new GuardItemRules.Source();
    public static void setClientShieldBlacklist(String blacklist){clientShieldBlacklist=blacklist;}
    public static void setClientRules(GuardPackets.Settings rules){clientRules=rules;if(rules==null){CLIENT_INCLUDED.clear();CLIENT_EXCLUDED.clear();GuardItemRules.clearClientShieldRules();}}

    public enum Result { NONE, PERFECT, PARRY, BLOCK }

    public static void clearSession(){STATES.clear();ATTACK_LOCKS.clear();}

    private GuardState() {}

    public static void forget(ServerPlayer player) {
        updateMovement(player, false);
        GuardState previous=STATES.remove(player.getUUID());
        ATTACK_LOCKS.remove(player.getUUID());
        if(previous!=null){
            if(previous.held&&GuardItemRules.shieldLike(previous.heldItem)&&player.isUsingItem()
                &&player.getUsedItemHand()==previous.guardHand)player.stopUsingItem();
            sync(player,new GuardState());
        }
    }

    public static boolean eligible(Player player) {
        return eligible(player, InteractionHand.MAIN_HAND) || eligible(player, InteractionHand.OFF_HAND);
    }

    public static boolean eligible(Player player, InteractionHand hand) {
        if(!attackReady(player))return false;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && (player.level().isClientSide ? clientCooldownPreventsGuard : GuardConfig.COOLDOWN_PREVENTS_GUARD.get())
            && player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        if (stack.isEmpty()) {
            boolean allowed = player.level().isClientSide ? clientAllowEmptyHand : GuardConfig.ALLOW_EMPTY_HAND.get();
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            return allowed && player.getItemInHand(other).isEmpty();
        }
        boolean client=player.level().isClientSide && clientRules!=null;
        String excluded=client?clientRules.excludedItems():"";
        String included=client?clientRules.includedItems():"";
        if (client?CLIENT_EXCLUDED.get(excluded).matches(stack):GuardItemRules.excluded(stack)) return false;
        if (client?clientRules.allowAnyItem():GuardConfig.ALLOW_ANY_ITEM.get()) return true;
        if (client?GuardItemRules.clientShieldLike(stack,clientRules.shieldItems(),clientShieldBlacklist):GuardItemRules.shieldLike(stack)) return true;
        if (client?CLIENT_INCLUDED.get(included).matches(stack):GuardItemRules.included(stack)) return true;
        if (!(client?clientRules.allowUsableItems():GuardConfig.ALLOW_USABLE_ITEMS.get()) && stack.getUseAnimation() != UseAnim.NONE) return false;
        // MAINHAND attack weapons remain valid when held in the offhand.
        return GuardItemRules.hasAttackDamage(stack, hand == InteractionHand.OFF_HAND);
    }

    public static void setClientCooldownPreventsGuard(boolean enabled) {
        clientCooldownPreventsGuard = enabled;
    }

    public static void setClientAllowEmptyHand(boolean enabled) {
        clientAllowEmptyHand = enabled;
    }

    /** Both sides use the same priorities; the server validates the requested random hand. */
    public static InteractionHand chooseHand(Player player, boolean randomOffhand, InteractionHand lastToolHand, int toolHandRun) {
        boolean mainEligible=eligible(player,InteractionHand.MAIN_HAND), offEligible=eligible(player,InteractionHand.OFF_HAND);
        if (!mainEligible || !offEligible) return mainEligible?InteractionHand.MAIN_HAND:offEligible?InteractionHand.OFF_HAND:null;
        boolean client=player.level().isClientSide && clientRules!=null;
        boolean crouch=client?clientCrouchOffhand:GuardConfig.FORCE_CROUCH_OFFHAND.get();
        if (crouch && player.isShiftKeyDown()) return InteractionHand.OFF_HAND;
        boolean mainShield=client?GuardItemRules.clientShieldLike(player.getMainHandItem(),clientRules.shieldItems(),clientShieldBlacklist):GuardItemRules.shieldLike(player.getMainHandItem());
        boolean offShield=client?GuardItemRules.clientShieldLike(player.getOffhandItem(),clientRules.shieldItems(),clientShieldBlacklist):GuardItemRules.shieldLike(player.getOffhandItem());
        if (mainShield || offShield) {
            int priority=client?clientShieldPriority:GuardConfig.SHIELD_PARRY_PRIORITY.get();
            if (priority==2) return InteractionHand.MAIN_HAND;
            if (priority==1) return mainShield?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        } else {
            int priority=client?clientHandPriority:GuardConfig.PARRY_HAND_PRIORITY.get();
            if (priority==1) return InteractionHand.OFF_HAND;
            if (priority==2) return InteractionHand.MAIN_HAND;
            if (toolHandRun>=2) return lastToolHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        }
        return randomOffhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
    }

    public static long inputId(ServerPlayer player) {
        GuardState state=STATES.get(player.getUUID());return state==null?0:state.inputId;
    }

    public static void input(ServerPlayer player, GuardPackets.Input request) {
        if (!player.isAlive() || request.inputId()<=0) return;
        GuardState state=STATES.computeIfAbsent(player.getUUID(),ignored->new GuardState());
        if (request.inputId()<=state.inputId) return;
        state.inputId=request.inputId();state.inputAccepted=false;
        state.inputProcessing=true;
        try { input(player,request.pressed(),request.offhand()); }
        finally {
            state.inputProcessing=false;
            state.inputAccepted=request.pressed() && state.held && state.phase!=0;
            // Rejections also acknowledge the attempt, even when no combat value changed.
            sync(player,state);
        }
    }

    public static void input(ServerPlayer player, boolean down, boolean requestedOffhand) {
        if(!player.isAlive()){forget(player);GuardRetaliation.cancelPending(player);return;}
        GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
        if (down && GuardConfig.CONSUMABLE_PRIORITY.get() && GuardItemRules.consumableInEitherHand(player)) down = false;
        if (!down) {
            // Releasing guard in the same tick as the final block must not skip the break penalty.
            if (state.breakPending) applyGuardBreak(player, state);
            state.guardReleaseRequired = false;
            if (state.held && GuardItemRules.shieldLike(state.heldItem)
                && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                player.stopUsingItem();
            state.held = false;
            state.phase = 0;
            updateMovement(player, false);
            sync(player, state);
            return;
        }
        if (state.held || state.guardReleaseRequired || state.recharge > 0 || (!GuardConfig.PARRY.get() && !GuardConfig.BLOCK.get())) return;
        InteractionHand hand = chooseHand(player, requestedOffhand, state.lastToolHand, state.toolHandRun);
        if (hand != null && eligible(player, InteractionHand.MAIN_HAND) && eligible(player, InteractionHand.OFF_HAND)) {
            state.toolHandRun = hand == state.lastToolHand ? state.toolHandRun + 1 : 1;
            state.lastToolHand = hand;
        }
        if (hand == null) return;
        if (GuardItemRules.shieldLike(player.getItemInHand(hand))
            && player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) return;
        state.held = true;
        state.elapsed = 0;
        state.guardHand = hand;
        state.heldItem = player.getItemInHand(hand);
        boolean shield = GuardItemRules.shieldLike(state.heldItem);
        state.blocksTaken = 0;
        state.breakPending = false;
        state.phase = GuardConfig.PARRY.get() ? (perfectTicks(state.heldItem, shield) > 0 ? 1 : 2)
            : shield ? 4 : 3;
        if (shield && (!player.isUsingItem() || player.getUsedItemHand() != hand)) player.startUsingItem(hand);
        state.recharge = (shield ? GuardShieldExpansionCompat.cooldownTicks(state.heldItem,GuardConfig.SHIELD_RECHARGE_TICKS.get()) : GuardConfig.RECHARGE_TICKS.get());
        state.rechargeDuration = state.recharge;
        updateMovement(player, true);
        sync(player, state);
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive()) {
            forget(player);
            GuardRetaliation.cancelPending(player);
            return;
        }
        GuardRetaliation.tick(player);
        GuardState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (state.held && (state.phase == 3 || state.phase == 4) && !GuardConfig.BLOCK.get()) {
            input(player, false, false);
        }
        if (state.recharge > 0 && state.phase != 3 && state.phase != 4) state.recharge--;
        if (state.held) {
            if (state.breakPending) applyGuardBreak(player, state);
            ItemStack currentItem = player.getItemInHand(state.guardHand);
            if (GuardConfig.CONSUMABLE_PRIORITY.get() && GuardItemRules.consumableInEitherHand(player) || !eligible(player, state.guardHand)
                || GuardItemRules.shieldLike(state.heldItem) && player.getCooldowns().isOnCooldown(currentItem.getItem())
                || (!(state.heldItem.isEmpty() && currentItem.isEmpty()) && state.heldItem != currentItem)) {
                if (GuardItemRules.shieldLike(state.heldItem) && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                    player.stopUsingItem();
                state.held = false;
                state.phase = 0;
            } else if (state.phase == 1 || state.phase == 2) {
                state.elapsed++;
                boolean shield = GuardItemRules.shieldLike(state.heldItem);
                if (state.elapsed >= (shield ? GuardShieldExpansionCompat.parryTicks(state.heldItem,GuardConfig.SHIELD_PARRY_TICKS.get()) : GuardConfig.PARRY_TICKS.get())) {
                    state.phase = GuardConfig.BLOCK.get() ? shield ? 4 : 3 : 0;
                    if (state.phase == 4) {
                        // Keep the existing use action so the shield pose never restarts.
                        state.recharge = GuardShieldExpansionCompat.cooldownTicks(state.heldItem,GuardConfig.SHIELD_RECHARGE_TICKS.get());
                        state.rechargeDuration = state.recharge;
                    } else if (state.phase == 3) { state.recharge = GuardConfig.RECHARGE_TICKS.get(); state.rechargeDuration = state.recharge; }
                    else {
                        state.held = false;
                        if (shield && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                            player.stopUsingItem();
                    }
                } else if (state.elapsed >= perfectTicks(state.heldItem, shield)) {
                    state.phase = 2;
                }
            }
            if (state.phase == 4 && (!player.isUsingItem() || player.getUsedItemHand() != state.guardHand)) {
                state.held = false;
                state.phase = 0;
            }
        }
        updateMovement(player, state.held && state.phase != 0);
        sync(player, state);
        if (!state.held && state.phase == 0 && state.recharge == 0
            && player.level().getGameTime() > state.safetyThroughTick) state.heldItem = ItemStack.EMPTY;
    }

    private static int perfectTicks(ItemStack held, boolean shield) {
        return shield ? GuardShieldExpansionCompat.perfectTicks(held, GuardConfig.SHIELD_PERFECT_TICKS.get(), GuardConfig.SHIELD_PARRY_TICKS.get())
            : GuardConfig.PERFECT_TICKS.get();
    }

    private static void applyGuardBreak(ServerPlayer player, GuardState state) {
        state.breakPending = false;
        state.guardReleaseRequired = true;
        boolean shieldBreak = GuardItemRules.shieldLike(state.heldItem);
        if (!shieldBreak || !player.getCooldowns().isOnCooldown(state.heldItem.getItem())) {
            state.recharge = Math.max(GuardConfig.RECHARGE_TICKS.get(), shieldBreak ? GuardShieldExpansionCompat.cooldownTicks(state.heldItem,GuardConfig.SHIELD_BREAK_TICKS.get()) : GuardConfig.SHIELD_BREAK_TICKS.get());
            state.rechargeDuration = state.recharge;
        }
        if (shieldBreak) {
            if (player.isUsingItem() && player.getUsedItemHand() == state.guardHand) player.stopUsingItem();
            // A broken shield cannot immediately resume its vanilla use action.
            player.getCooldowns().addCooldown(state.heldItem.getItem(), state.recharge);
        }
        state.phase = 0;
        state.held = false;
    }

    /** Reuse the recharge meter without imposing movement or attack stun. */
    public static void parriedGuardBreak(ServerPlayer player,int ticks){
        if(ticks<=0)return;
        GuardState state=STATES.computeIfAbsent(player.getUUID(),ignored->new GuardState());
        input(player,false,false);
        state.recharge=Math.max(state.recharge,ticks);
        state.rechargeDuration=state.recharge;
        state.breakPending=false;
        updateMovement(player,false);
        sync(player,state);
    }

    public static void updateMovement(LivingEntity player, boolean guarding) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        AttributeModifier modifier = speed.getModifier(GUARD_SLOWDOWN);
        int percent = GuardConfig.GUARD_MOVEMENT_PERCENT.get();
        double multiplier=percent/100.0D;
        if(guarding && player.isUsingItem() && GuardItemRules.shieldLike(player.getUseItem())){
            double shieldMovement=GuardShieldExpansionCompat.movement(player.getUseItem(),-1);
            // Shield Expansion's speed stat compensates vanilla's 20% shield-use movement.
            if(shieldMovement>=0)multiplier=shieldMovement/0.2D;
        }
        double amount=multiplier-1.0D;
        if (modifier != null && (!guarding || multiplier == 1 || modifier.amount() != amount))
            speed.removeModifier(GUARD_SLOWDOWN);
        if (guarding && multiplier != 1 && speed.getModifier(GUARD_SLOWDOWN) == null)
            speed.addTransientModifier(new AttributeModifier(GUARD_SLOWDOWN, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static Result handleHit(ServerPlayer player, DamageSource source) {
        boolean parryAllowed = GuardDamageRules.canParry(source);
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            // Ground impacts require deliberate downward aim. Keep the existing
            // flying-into-wall behavior separate from fall damage.
            if (source.is(DamageTypes.FALL)
                && player.getLookAngle().y > -Math.sin(Math.toRadians(40.0D)))
                return Result.NONE;
            return handleHit(player, SourceKind.FALL, null, parryAllowed);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            Vec3 position = source.getSourcePosition();
            return handleHit(player, SourceKind.EXPLOSION, position, parryAllowed);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
            // Entity projectiles parry at ProjectileImpactEvent, before their collision can consume them.
            // Damage-only projectiles can be parried but have no entity to redirect.
            return handleHit(player, SourceKind.PROJECTILE_DAMAGE, source.getSourcePosition(), parryAllowed);
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && player.distanceTo(attacker) <= 4.0D)
            return handleHit(player, SourceKind.MELEE, attacker.position(), parryAllowed);
        return handleHit(player, SourceKind.OTHER, source.getSourcePosition(), parryAllowed);
    }

    public static Result handleProjectile(ServerPlayer player, Projectile projectile) {
        GuardState state = STATES.get(player.getUUID());
        if (state != null && state.phase == 4) return Result.NONE;
        if (state != null && state.phase == 3) {
            if (GuardConfig.BLOCK_DEFLECT_CHANCE.get() == 0 ||
                player.getRandom().nextInt(100) >= GuardConfig.BLOCK_DEFLECT_CHANCE.get()) return Result.NONE;
        } else if (!GuardConfig.PARRY_PROJECTILES.get() || !GuardDamageRules.canParryProjectile(projectile)) return Result.NONE;
        return handleHit(player, SourceKind.PROJECTILE_IMPACT, projectile.position(), true);
    }

    private enum SourceKind { MELEE, FALL, EXPLOSION, PROJECTILE_IMPACT, PROJECTILE_DAMAGE, OTHER }

    private static Result handleHit(ServerPlayer player, SourceKind kind, Vec3 origin, boolean parryAllowed) {
        GuardState state = STATES.get(player.getUUID());
        if (state == null || !eligible(player, state.guardHand) || (kind != SourceKind.FALL && !inAngle(player, origin))) return Result.NONE;
        if (state.breakPending && !GuardItemRules.shieldLike(state.heldItem)) return Result.NONE;
        // A successful parry closes the stance immediately; safety is a separate deadline.
        if (state.phase == 0 && player.level().getGameTime() <= state.safetyThroughTick) {
            return parryAllowed && canParry(kind, Result.PARRY) ? Result.PARRY : Result.NONE;
        }
        if (state.phase == 0 || state.phase == 4) return Result.NONE;
        Result result = state.phase == 1 ? Result.PERFECT : state.phase == 2 ? Result.PARRY : Result.BLOCK;
        if (result == Result.BLOCK && !canBlock(kind) || result != Result.BLOCK && (!parryAllowed || !canParry(kind, result))) return Result.NONE;
        if (result == Result.BLOCK && !GuardItemRules.shieldLike(state.heldItem)) {
            int max = GuardItemBlockCounts.limit(state.heldItem, false);
            if (max > 0 && ++state.blocksTaken >= max) state.breakPending = true;
        }
        if (result == Result.PERFECT) state.recharge = 0;
        else if (result == Result.PARRY) state.recharge = Math.max(0,
            (GuardItemRules.shieldLike(state.heldItem) ? GuardShieldExpansionCompat.cooldownTicks(state.heldItem,GuardConfig.SHIELD_RECHARGE_TICKS.get()) : GuardConfig.RECHARGE_TICKS.get()) / 2);
        if (result == Result.PERFECT || result == Result.PARRY) {
            state.safetyThroughTick = GuardConfig.FOLLOW_UP_TICKS.get() == 0 ? -1 : player.level().getGameTime() + GuardConfig.FOLLOW_UP_TICKS.get();
            boolean shield = GuardItemRules.shieldLike(state.heldItem);
            if (shield) state.safetyThroughTick = -1;
            boolean keepBlocking = shield && GuardConfig.BLOCK.get();
            GuardCombatEffects.healOnParry(player, result);
            state.held = keepBlocking;
            state.phase = keepBlocking ? 4 : 0;
            state.elapsed = 0;
            if (shield && !keepBlocking && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                player.stopUsingItem();
            updateMovement(player, keepBlocking);
        }
        sync(player, state);
        return result;
    }

    public static boolean emptyHandGuard(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.heldItem.isEmpty();
    }

    public static boolean isShieldBlocking(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.held && state.phase == 4 && player.isUsingItem()
            && player.getUsedItemHand() == state.guardHand && GuardItemRules.shieldLike(player.getUseItem())
            && !state.breakPending && !player.getCooldowns().isOnCooldown(player.getUseItem().getItem());
    }

    public static boolean shieldGuard(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && GuardItemRules.shieldLike(state.heldItem);
    }

    public static void shieldBlocked(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        if (state == null || state.phase != 4) return;
        int max = GuardItemBlockCounts.limit(state.heldItem, true);
        if (max > 0 && ++state.blocksTaken >= max) state.breakPending = true;
    }

    public static boolean guardBreakPending(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.breakPending;
    }

    public static void preventVanillaShieldUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !(GuardItemRules.shieldLike(player.getItemInHand(event.getHand()))
                || GuardCompatibility.simplySwordsWeapon(player.getItemInHand(event.getHand())))
            || GuardConfig.CONSUMABLE_PRIORITY.get() && GuardItemRules.consumableInEitherHand(player)) return;
        event.setCanceled(true);
        // Guard owns shield use. Consume this standalone attempt without a swing,
        // even while the guard recharge or vanilla shield cooldown is active.
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    public static void wear(ServerPlayer player, Result result) {
        int base = switch (result) {
            case PERFECT -> GuardConfig.PERFECT_WEAR.get();
            case PARRY -> GuardConfig.PARRY_WEAR.get();
            case BLOCK -> GuardConfig.BLOCK_WEAR.get();
            default -> 0;
        };
        if (base <= 0) return;
        GuardState state = STATES.get(player.getUUID());
        InteractionHand hand = state == null ? InteractionHand.MAIN_HAND : state.guardHand;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isDamageableItem()) return;
        // Only items with their own durability lose wear. Empty hands and stackable items are unaffected.
        int wear = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * (base / 1000.0D)));
        stack.hurtAndBreak(wear, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    private static boolean canParry(SourceKind kind, Result result) {
        return switch (kind) {
            case MELEE, OTHER -> true;
            case FALL -> GuardConfig.FALL_PARRY.get() && (!GuardConfig.PERFECT_FALL_ONLY.get() || result == Result.PERFECT);
            case EXPLOSION -> GuardConfig.PARRY_EXPLOSIONS.get() && (!GuardConfig.PERFECT_EXPLOSIONS_ONLY.get() || result == Result.PERFECT);
            case PROJECTILE_IMPACT, PROJECTILE_DAMAGE -> GuardConfig.PARRY_PROJECTILES.get() && (!GuardConfig.PERFECT_PROJECTILES_ONLY.get() || result == Result.PERFECT);
        };
    }

    private static boolean canBlock(SourceKind kind) {
        if (!GuardConfig.BLOCK.get()) return false;
        return switch (kind) {
            case MELEE -> true;
            case FALL -> false;
            case PROJECTILE_IMPACT -> GuardConfig.BLOCK_DEFLECT_CHANCE.get() > 0;
            case EXPLOSION -> GuardConfig.BLOCK_EXPLOSIONS.get();
            case PROJECTILE_DAMAGE -> GuardConfig.BLOCK_PROJECTILES.get();
            case OTHER -> false;
        };
    }

    private static boolean inAngle(ServerPlayer player, Vec3 origin) {
        if (origin == null) return true;
        if (GuardConfig.FACING_ANGLE.get() >= 360) return true;
        Vec3 offset = origin.subtract(player.position());
        if (offset.lengthSqr() < 1.0E-8D) return true;
        double dot = player.getLookAngle().normalize().dot(offset.normalize());
        return dot >= Math.cos(Math.toRadians(GuardConfig.FACING_ANGLE.get() / 2.0D));
    }

    public static InteractionHand poseHand(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.held && state.phase != 0 ? state.guardHand : null;
    }

    private static void sync(ServerPlayer player, GuardState state) {
        if (state.inputProcessing) return;
        boolean shield = GuardItemRules.shieldLike(state.heldItem);
        int window=(shield ? GuardShieldExpansionCompat.parryTicks(state.heldItem,GuardConfig.SHIELD_PARRY_TICKS.get()) : GuardConfig.PARRY_TICKS.get());
        int duration=Math.max(1,state.rechargeDuration);
        boolean offhand=state.guardHand==InteractionHand.OFF_HAND;
        GuardPackets.Status previous=state.lastStatus;
        if(previous!=null && previous.phase()==state.phase && previous.elapsed()==state.elapsed
            && previous.recharge()==state.recharge && previous.window()==window
            && previous.rechargeMax()==duration && previous.offhand()==offhand
            && previous.inputId()==state.inputId && previous.accepted()==state.inputAccepted
            && previous.toolHandRun()==state.toolHandRun && previous.toolOffhand()==(state.lastToolHand==InteractionHand.OFF_HAND))return;
        GuardPackets.Status status = new GuardPackets.Status(state.phase, state.elapsed, state.recharge,
            window,duration,offhand,state.inputId,state.inputAccepted,state.toolHandRun,state.lastToolHand==InteractionHand.OFF_HAND);
        if (!status.equals(state.lastStatus)) {
            if (state.lastStatus == null || (state.lastStatus.phase() == 0) != (status.phase() == 0)
                || state.lastStatus.offhand() != status.offhand())
                GuardPoses.broadcast(player, status.phase() == 0 ? null : state.guardHand);
            state.lastStatus = status;
            PacketDistributor.sendToPlayer(player, status);
        }
    }
}
