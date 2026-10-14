package dev.zeli.mallardguard;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardCombatEffects {
    private static final Set<UUID> FALL_BLASTS = new HashSet<>();
    private static final Map<LivingEntity, Stun> STUNNED = new java.util.WeakHashMap<>();
    private record Stun(int remaining,boolean firstTick) {}
    public static final ResourceLocation STUN_SPEED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "shield_stun");

    public static void clearSession(){for(var entity:STUNNED.keySet()){var speed=entity.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(STUN_SPEED);}STUNNED.clear();FALL_BLASTS.clear();}

    private GuardCombatEffects() {}

    /** Once per consumed parry attempt; callers exclude follow-up safety hits. */
    public static void healOnParry(LivingEntity defender, GuardState.Result result) {
        if (defender.level().isClientSide || !GuardConfig.PARRY_HEALING.get()
            || result != GuardState.Result.PARRY && result != GuardState.Result.PERFECT) return;
        int hearts = GuardConfig.PARRY_HEALING_HEARTS.get();
        if (hearts > 0) defender.heal(result == GuardState.Result.PERFECT ? hearts * 2.0F : hearts);
    }

    public static boolean isOwnFallBlast(ServerPlayer player, DamageSource source) {
        return FALL_BLASTS.contains(player.getUUID()) && source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static void fall(ServerPlayer player, float damage, boolean perfect) {
        ServerLevel level = player.serverLevel();
        float distance = player.fallDistance > 0 ? player.fallDistance : damage + 3.0F;
        if (distance > 10.0F && GuardConfig.FALL_BLAST_STRENGTH.get() > 0) {
            float radius = Math.min(8.0F, 0.2F + damage * 0.1F * GuardConfig.FALL_BLAST_STRENGTH.get() / 100.0F);
            FALL_BLASTS.add(player.getUUID());
            try {
                level.explode(player, player.getX(), player.getY(), player.getZ(), radius, false,
                    GuardConfig.FALL_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            } finally {
                FALL_BLASTS.remove(player.getUUID());
            }
        }
        double power = GuardConfig.FALL_LAUNCH_POWER.get() / 100.0D * (perfect ? 2.0D : 1.0D);
        player.fallDistance = 0;
        if (power == 0) return;
        // Preserve the player's actual horizontal momentum at impact. Looking
        // down is still required, but looking direction must not create a lunge.
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.hypot(motion.x, motion.z);
        double boost = (1.0D + damage * 0.25D) * power;
        double carried = horizontal < 0.02D ? 0.0D : Math.min(boost, horizontal * (1.0D + power));
        player.setDeltaMovement(horizontal < 0.02D ? 0.0D : motion.x / horizontal * carried,
            0.6D * power,
            horizontal < 0.02D ? 0.0D : motion.z / horizontal * carried);
        player.hurtMarked = true;
    }

    public static void pushDefender(LivingEntity player, Vec3 origin, boolean shield, boolean perfect) {
        if (!GuardConfig.DEFENDER_KNOCKBACK.get() || GuardConfig.KNOCKBACK_STRENGTH.get() == 0) return;
        Vec3 away = origin == null ? player.getLookAngle().scale(-1.0D) : player.position().subtract(origin);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(-player.getLookAngle().x, 0, -player.getLookAngle().z);
        if (away.lengthSqr() < 1.0E-6D) return;
        double strength = GuardConfig.KNOCKBACK_STRENGTH.get() / 100.0D * (shield ? 0.5D : 1.0D) * (perfect?GuardConfig.PERFECT_KNOCKBACK_MULTIPLIER.get()/100.0D:1.0D);
        player.setDeltaMovement(player.getDeltaMovement().add(away.normalize().scale(strength)).add(0, strength * 0.25D, 0));
        player.hurtMarked = true;
    }

    public static void pushAttacker(LivingEntity player, LivingEntity attacker, boolean shield, boolean perfect) {
        if (player.distanceToSqr(attacker) > 16.0D) return;
        int percent = shield ? GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT.get() : GuardConfig.TOOL_PUSHBACK_PERCENT.get();
        if(perfect&&!shield)percent=(int)Math.round(percent*GuardConfig.PERFECT_KNOCKBACK_MULTIPLIER.get()/100.0D);
        if (percent == 0) return;
        pushLikeHit(player, attacker, percent);
    }

    public static void shieldPerfect(ServerPlayer player, float incomingDamage) {
        if(!GuardConfig.SHIELD_CONE_ENABLED.get())return;
        int cone = GuardConfig.SHIELD_CONE_DEGREES.get();
        if (cone == 0) return;
        double reach = GuardConfig.SHIELD_CONE_REACH.get();
        Vec3 eye = player.getEyePosition();
        double minDot = Math.cos(Math.toRadians(cone));
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(reach), entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.lengthSqr() < 1.0E-6D || toTarget.lengthSqr() > reach * reach ||
                player.getLookAngle().dot(toTarget.normalize()) < minDot) continue;
            pushLikeHit(player, target, GuardConfig.SHIELD_PUSHBACK_PERCENT.get());
            if (GuardConfig.SHIELD_STUN_TICKS.get() > 0 && (!boss(target) || allowedBoss(target))) {
                stun(target,GuardConfig.SHIELD_STUN_TICKS.get());
                stopRangedUse(target);
            }
            float damage = GuardConfig.RETALIATION_ENABLED.get()?incomingDamage * GuardConfig.SHIELD_RETALIATION_PERCENT.get() / 100.0F:0;
            if (damage > 0) GuardRetaliation.damage(player, target, damage);
        }
    }

    private static void pushLikeHit(LivingEntity defender, LivingEntity target, int percent) {
        if (percent <= 0) return;
        double dx = defender.getX() - target.getX();
        double dz = defender.getZ() - target.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            Vec3 look = defender.getLookAngle();
            dx = -look.x; dz = -look.z;
        }
        target.knockback(percent / 200.0D, dx, dz);
    }

    private static boolean boss(LivingEntity target) {
        return target instanceof WitherBoss || target instanceof EnderDragon
            || target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses")))
            || target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses")));
    }

    private static boolean allowedBoss(LivingEntity target) {
        String rules = GuardConfig.SHIELD_STUN_BOSSES.get();
        for (String entry : rules.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(trimmed.startsWith("#") ? trimmed.substring(1) : trimmed);
            if (id == null) continue;
            if (trimmed.startsWith("#") && target.getType().is(TagKey.create(Registries.ENTITY_TYPE, id))
                || !trimmed.startsWith("#") && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))) return true;
        }
        return false;
    }

    public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity entity) {
            STUNNED.remove(entity);
            AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null) movement.removeModifier(STUN_SPEED);
        }
    }

    private static void stun(LivingEntity target,int ticks){
        if(ticks<=0)return;Stun old=STUNNED.get(target);int remaining=Math.max(ticks,old==null?0:old.remaining());STUNNED.put(target,new Stun(remaining,old==null));stopRangedUse(target);if(target instanceof ServerPlayer player)PacketDistributor.sendToPlayer(player,new GuardPackets.StunState(remaining));
    }
    /** The direct attack outcome is authoritative; feedback packets only describe it. */
    public static int incomingParry(ServerPlayer attacker){
        int mode=GuardConfig.INCOMING_REACTION.get();
        if(mode==0||!GuardConfig.PARRIED_ENABLED.get())return 0;
        int ticks=(mode==1?GuardConfig.PARRIED_STUN_TICKS:GuardConfig.PARRIED_GUARD_BREAK_TICKS).get();
        if(ticks>0){
            if(mode==1){stun(attacker,ticks);GuardState.input(attacker,false,false);}
            else GuardState.parriedGuardBreak(attacker,ticks);
        }
        attacker.swinging=false;
        Stun state=STUNNED.get(attacker);return state==null?0:state.remaining();
    }
    public static boolean stunned(Entity entity) {
        // Integrated clients share these statics with the server; never touch its weak map from the client thread.
        return entity instanceof LivingEntity living && !entity.level().isClientSide && STUNNED.containsKey(living);
    }

    private static boolean rangedUse(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return animation == UseAnim.BOW || animation == UseAnim.CROSSBOW || animation == UseAnim.SPEAR;
    }

    private static void stopRangedUse(LivingEntity entity) {
        if (entity.isUsingItem() && rangedUse(entity.getUseItem())) entity.stopUsingItem();
    }

    public static void startRangedUse(LivingEntityUseItemEvent.Start event) {
        if (!event.getEntity().level().isClientSide && stunned(event.getEntity()) && rangedUse(event.getItem()))
            event.setCanceled(true);
    }

    public static void rangedItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!event.getEntity().level().isClientSide && stunned(event.getEntity())
            && rangedUse(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void projectileSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
            && event.getEntity() instanceof Projectile projectile && stunned(projectile.getOwner()))
            event.setCanceled(true);
    }

    public static void tickStun(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        if (STUNNED.isEmpty()) return;
        Stun state=STUNNED.get(entity);
        if(state==null)return;int ticks=state.remaining();
        stopRangedUse(entity);
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getModifier(STUN_SPEED) == null)
            movement.addTransientModifier(new AttributeModifier(STUN_SPEED, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (!state.firstTick()) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            entity.hurtMarked = true;
        }
        if (ticks <= 1 || !entity.isAlive()) {
            STUNNED.remove(entity);if(entity instanceof ServerPlayer player&&player.isAlive())PacketDistributor.sendToPlayer(player,new GuardPackets.StunState(0));
            if (movement != null && movement.getModifier(STUN_SPEED) != null) movement.removeModifier(STUN_SPEED);
        } else STUNNED.put(entity,new Stun(ticks-1,false));
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel) || !(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        if (hit.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            mobProjectile(event, mob, hit);
            return;
        }
        if (!(hit.getEntity() instanceof ServerPlayer player) || event.getProjectile().getOwner() == player) return;
        Projectile projectile = event.getProjectile();
        GuardState.Result result = GuardState.handleProjectile(player, projectile);
        if (result == GuardState.Result.NONE) return;
        boolean shield = GuardState.shieldGuard(player);
        event.setCanceled(true);
        Entity owner = projectile.getOwner();
        Vec3 sparkDirection = owner != null && owner != player ? owner.position().subtract(player.position())
            : projectile.getDeltaMovement().scale(-1.0D);
        float particleDamage=GuardEffects.projectileDamageEstimate(projectile);
        reflectProjectile(player, projectile, result == GuardState.Result.PERFECT);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : result == GuardState.Result.BLOCK ? 3 : 1, GuardState.guardBreakPending(player), result == GuardState.Result.PERFECT ? GuardRetaliation.begin(player) : 0, GuardState.inputId(player), event.getProjectile().getOwner()==null?-1:event.getProjectile().getOwner().getId()));
        GuardEffects.onHit(player, owner instanceof net.minecraft.world.entity.player.Player attackingPlayer ? player.damageSources().playerAttack(attackingPlayer)
            : owner instanceof LivingEntity attackingMob ? player.damageSources().mobAttack(attackingMob) : player.damageSources().playerAttack(player), result, sparkDirection,particleDamage);
        GuardState.wear(player, result);
        if (result != GuardState.Result.BLOCK) pushDefender(player, hit.getLocation(), shield, result == GuardState.Result.PERFECT);
        if (shield && result == GuardState.Result.PERFECT) {
            // Projectile impact fires before Minecraft calculates its actual damage.
            // Arrow base damage is known; other modded projectiles use a modest estimate.
            float estimate = projectile instanceof AbstractArrow arrow ? (float) arrow.getBaseDamage()
                : (float) Math.max(2.0D, projectile.getDeltaMovement().length() * 2.0D);
            shieldPerfect(player, estimate);
        } else if ((result == GuardState.Result.PARRY || !shield && result == GuardState.Result.PERFECT)
            && owner instanceof LivingEntity attacker)
            pushAttacker(player, attacker, shield, result == GuardState.Result.PERFECT);
    }

    public static void reflectProjectile(LivingEntity player, Projectile projectile, boolean perfect) {
        Entity owner = projectile.getOwner();
        Vec3 direction;
        if (perfect && owner != null && owner != player) {
            direction = owner.getEyePosition().subtract(player.getEyePosition());
        } else {
            // Keep a firm horizontal component; downward deflections should be as common as upward ones.
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            direction = new Vec3(Math.cos(angle), player.getRandom().nextDouble() * 0.32D - 0.20D,
                Math.sin(angle));
        }
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        direction = direction.normalize();
        double speed = Math.max(1.4D, projectile.getDeltaMovement().length() * 1.1D);
        if (projectile instanceof GuardTridentReturn trident) trident.mallardguard$rememberThrower(owner);
        // Changing an arrow's owner can also change its pickup mode.
        AbstractArrow.Pickup tridentPickup = projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident trident
            ? trident.pickup : null;
        projectile.setOwner(player);
        if (tridentPickup != null) ((net.minecraft.world.entity.projectile.ThrownTrident) projectile).pickup = tridentPickup;
        projectile.setPos(player.getEyePosition().add(direction.scale(0.85D)));
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        projectile.hasImpulse = true;
    }

    private static void mobProjectile(ProjectileImpactEvent event, net.minecraft.world.entity.Mob mob, EntityHitResult hit) {
        Projectile projectile = event.getProjectile();
        Entity owner = projectile.getOwner();
        if (owner == mob) return;
        GuardState.Result result = GuardMobState.react(mob, projectile.position(), GuardDamageRules.canParryProjectile(projectile), true, false);
        // Cache the chosen outcome so the damage event cannot process the same projectile twice.
        if (result == GuardState.Result.NONE || result == GuardState.Result.BLOCK &&
            mob.getRandom().nextDouble() * 100.0D >= GuardConfig.BLOCK_DEFLECT_CHANCE.get()) {
            GuardMobState.projectileResult(projectile, mob, result);
            return;
        }
        boolean shield = GuardItemRules.shieldLike(mob.getItemInHand(GuardMobState.defenseHand(mob)));
        event.setCanceled(true);
        float particleDamage=GuardEffects.projectileDamageEstimate(projectile);
        // Keep incidental shots defensive; do not aim a perfect return at an unrelated mob.
        boolean returnToOwner = !(owner instanceof net.minecraft.world.entity.Mob attacker) || GuardMobState.combatOpponent(mob, attacker);
        reflectProjectile(mob, projectile, result == GuardState.Result.PERFECT && returnToOwner);
        DamageSource source = mob.damageSources().mobAttack(owner instanceof LivingEntity living ? living : mob);
        GuardEffects.onMobHit(mob, source, result, shield,particleDamage);
        float amount = projectile instanceof AbstractArrow arrow ? (float) arrow.getBaseDamage() : 2.0F;
        GuardMobState.wear(mob, result, amount);
        if (result != GuardState.Result.BLOCK) {
            pushDefender(mob, hit.getLocation(), shield, result==GuardState.Result.PERFECT);
            if (owner instanceof LivingEntity attacker && GuardMobState.combatOpponent(mob, attacker)) pushAttacker(mob, attacker, shield, result==GuardState.Result.PERFECT);
        }
    }
}
