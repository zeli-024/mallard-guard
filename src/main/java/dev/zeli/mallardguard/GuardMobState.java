package dev.zeli.mallardguard;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Bounded combat behaviors for explicitly whitelisted humanoids. State never retains a world or target entity. */
public final class GuardMobState {
    private static final java.util.Set<Mob> GOALS_ADDED = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private record ProjectileDefense(java.util.UUID defender, GuardState.Result result, long tick) {}
    private static final Map<net.minecraft.world.entity.projectile.Projectile, ProjectileDefense> PROJECTILE_DEFENSES = new WeakHashMap<>();
    public static void projectileResult(net.minecraft.world.entity.projectile.Projectile projectile, Mob mob, GuardState.Result result) {
        PROJECTILE_DEFENSES.put(projectile, new ProjectileDefense(mob.getUUID(), result, mob.level().getGameTime()));
    }
    private static final String EQUIPMENT_ROLLED = "MallardGuardEquipmentRoll";
    private enum Behavior { IDLE, APPROACH, TACTICAL, PRESSURE, COUNTER }
    private static final net.minecraft.resources.ResourceLocation MOB_SLOWDOWN = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "mob_guard_slowdown");
    private static void movement(Mob mob, boolean active) {
        var speed = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = active ? GuardConfig.MOB_MOVEMENT.get() / 100.0D - 1 : 0;
        var old = speed.getModifier(MOB_SLOWDOWN);
        if (old != null && old.amount() == amount) return;
        if (old != null) speed.removeModifier(MOB_SLOWDOWN);
        if (amount != 0) speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MOB_SLOWDOWN, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private enum Movement { STILL, STRAFE }
    private static final Movement[] MOVEMENTS = Movement.values();
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> COMMON_BOSSES = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> FORGE_BOSSES = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private static final class State {
        long rechargeUntil, guardUntil, startedAt, nextAttempt, lastPressureHit;
        long behaviorUntil, tacticalReady, nextDecision, attackReady;
        int pressureHits, chains;
        java.util.UUID target;
        Behavior behavior = Behavior.IDLE;
        Movement movement = Movement.STILL;
        float side;
        double radius, approachDistance;
        boolean guarding, approached;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack item = ItemStack.EMPTY;
    }
    private static String cachedIds="";
    private static java.util.Set<net.minecraft.resources.ResourceLocation> whitelistIds=java.util.Set.of();
    private static boolean supported(Mob mob) {
        if(mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            ||mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
            ||mob.getType().is(COMMON_BOSSES)||mob.getType().is(FORGE_BOSSES))return false;
        String rules=GuardConfig.MOB_WHITELIST.get();
        if(!rules.equals(cachedIds)){
            var ids=new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
            for(String value:rules.split(",")){
                var id=net.minecraft.resources.ResourceLocation.tryParse(value.trim());if(id!=null)ids.add(id);
            }
            whitelistIds=java.util.Set.copyOf(ids);cachedIds=rules;
        }
        return whitelistIds.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
    }
    private GuardMobState() {}

    /** An incidental mob hit must not choose a new opponent for experimental guard AI. */
    public static boolean combatOpponent(Mob mob, LivingEntity attacker) {
        return attacker != mob && (!(attacker instanceof Mob) || mob.getTarget() == attacker);
    }

    public static boolean controlsAi(Mob mob){
        if(mob.level().isClientSide||!GuardConfig.MOB_GUARD.get()||!GuardConfig.PARRY.get())return false;
        State state=STATES.get(mob);
        return state!=null&&state.behavior!=Behavior.IDLE&&mob.isAlive()&&!mob.isNoAi()
            &&mob.level().getGameTime()<state.behaviorUntil&&!GuardCombatEffects.stunned(mob)&&supported(mob);
    }

    public static InteractionHand defenseHand(Mob mob) { State s = STATES.get(mob); return s == null ? InteractionHand.MAIN_HAND : s.hand; }

    public static InteractionHand guardHand(Mob mob) {
        State state = STATES.get(mob);
        return state != null && state.guarding ? state.hand : null;
    }

    private static boolean ranged(Mob mob) {
        return mob.getMainHandItem().is(Items.BOW) || mob.getMainHandItem().is(Items.CROSSBOW)
            || mob.getOffhandItem().is(Items.BOW) || mob.getOffhandItem().is(Items.CROSSBOW);
    }
    private static boolean eligible(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW) && !GuardItemRules.consumable(stack)
            && !GuardItemRules.excluded(stack)
            && (GuardItemRules.shieldLike(stack) || GuardItemRules.hasAttackDamage(stack)
                || GuardItemRules.included(stack));
    }

    public static GuardState.Result react(Mob mob, Vec3 origin, boolean parryAllowed, boolean projectile, boolean explosion) {
        if (mob.level().isClientSide || !GuardConfig.MOB_GUARD.get() || !supported(mob) || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob)
            || origin == null || ranged(mob)) return GuardState.Result.NONE;
        Vec3 toward = origin.subtract(mob.getEyePosition());
        int angle = GuardConfig.FACING_ANGLE.get();
        if (angle < 360 && toward.lengthSqr() > 1.0E-6D
            && mob.getLookAngle().dot(toward.normalize()) < Math.cos(Math.toRadians(angle / 2.0D))) return GuardState.Result.NONE;
        State previous = STATES.get(mob);
        long now = mob.level().getGameTime();
        if (previous == null || !previous.guarding || now >= previous.guardUntil || now < previous.rechargeUntil
            || mob.getItemInHand(previous.hand) != previous.item) return GuardState.Result.NONE;
        long elapsed=now-previous.startedAt;
        boolean perfect=elapsed<GuardConfig.MOB_PERFECT_TICKS.get();
        if(!parryAllowed||!GuardConfig.PARRY.get()
            ||projectile&&!GuardConfig.PARRY_PROJECTILES.get()||explosion&&!GuardConfig.PARRY_EXPLOSIONS.get()
            ||projectile&&GuardConfig.PERFECT_PROJECTILES_ONLY.get()&&!perfect
            ||explosion&&GuardConfig.PERFECT_EXPLOSIONS_ONLY.get()&&!perfect)return GuardState.Result.NONE;
        GuardState.Result result=perfect?GuardState.Result.PERFECT:GuardState.Result.PARRY;
        boolean shield=GuardItemRules.shieldLike(previous.item);
        int recharge=(shield?GuardConfig.SHIELD_RECHARGE_TICKS:GuardConfig.RECHARGE_TICKS).get();
        previous.rechargeUntil=now+(perfect?1:Math.max(1,recharge/2));
        counter(mob,previous);
        GuardCombatEffects.healOnParry(mob,result);
        return result;
    }

    public static void incoming(LivingIncomingDamageEvent event, Mob mob) {
        if (mob.level().isClientSide) return;
        DamageSource source = event.getSource();
        // Mobs react to combat, not environmental ticks such as hunger or burning.
        if (source.getEntity() == null && !source.is(DamageTypeTags.IS_EXPLOSION)) return;
        Vec3 origin = source.getSourcePosition();
        if (origin == null && source.getDirectEntity() != null) origin = source.getDirectEntity().position();
        ProjectileDefense remembered = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
            ? PROJECTILE_DEFENSES.remove(projectile) : null;
        GuardState.Result result = remembered != null && remembered.tick() == mob.level().getGameTime() && remembered.defender().equals(mob.getUUID()) ? remembered.result()
            : react(mob, origin, GuardDamageRules.canParry(source), source.is(DamageTypeTags.IS_PROJECTILE), source.is(DamageTypeTags.IS_EXPLOSION));
        if (result == GuardState.Result.NONE) return;
        State state = STATES.get(mob);
        boolean shield = GuardItemRules.shieldLike(state.item);
        GuardEffects.onMobHit(mob, source, result, shield,event.getAmount());
        wear(mob, state, result, event.getAmount());
        event.setCanceled(true);
        GuardCombatEffects.pushDefender(mob,origin,shield,result==GuardState.Result.PERFECT);
        if(source.getEntity() instanceof LivingEntity attacker&&combatOpponent(mob,attacker)){
            GuardCombatEffects.pushAttacker(mob,attacker,shield,result==GuardState.Result.PERFECT);
            boolean retaliate=(result==GuardState.Result.PERFECT?GuardConfig.MOB_PERFECT_RETALIATION:GuardConfig.MOB_REGULAR_RETALIATION).get();
            if(retaliate&&GuardConfig.RETALIATION_ENABLED.get()&&source.getDirectEntity()==attacker){
                float multiplier=shield?result==GuardState.Result.PERFECT?GuardConfig.SHIELD_RETALIATION_PERCENT.get()/100F:0
                    :(result==GuardState.Result.PERFECT?GuardConfig.PERFECT_RETALIATION:GuardConfig.PARRY_RETALIATION).get().floatValue();
                if(multiplier>0)MallardGuard.returnDamage(mob,attacker,event.getAmount()*multiplier);
            }
        }
    }

    public static void wear(Mob mob, GuardState.Result result, float amount) {
        State state = STATES.get(mob);
        if (state != null) wear(mob, state, result, amount);
    }
    private static void wear(Mob mob, State state, GuardState.Result result, float amount) {
        if (!state.item.isDamageableItem()) return;
        int percent=(result==GuardState.Result.PERFECT?GuardConfig.PERFECT_WEAR:GuardConfig.PARRY_WEAR).get();
        int wear=percent==0?0:Math.max(1,(int)Math.ceil(state.item.getMaxDamage()*percent/1000D));
        if (wear > 0) state.item.hurtAndBreak(wear, mob, state.hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    private static LivingEntity target(Mob mob, State state) {
        if (state.target != null && mob.level() instanceof net.minecraft.server.level.ServerLevel level
            && level.getEntity(state.target) instanceof LivingEntity living && living.isAlive()) return living;
        return mob.getTarget() != null && mob.getTarget().isAlive() ? mob.getTarget() : null;
    }
    private static void stopMovement(Mob mob) {
        mob.getNavigation().stop(); mob.getMoveControl().strafe(0, 0);
        mob.setXxa(0); mob.setZza(0); mob.setSpeed(0);
    }
    private static void pose(Mob mob, State state, boolean raised) {
        if (state.guarding == raised) return;
        state.guarding = raised; GuardPoses.broadcast(mob, raised ? state.hand : null);
    }
    private static void finish(Mob mob, State state) {
        if (state.behavior != Behavior.IDLE) state.nextAttempt = Math.max(state.nextAttempt, mob.level().getGameTime()
            + (GuardItemRules.shieldLike(state.item) ? GuardConfig.SHIELD_RECHARGE_TICKS : GuardConfig.RECHARGE_TICKS).get());
        pose(mob, state, false); state.behavior = Behavior.IDLE;
        state.behaviorUntil = state.guardUntil = 0;
        stopMovement(mob); movement(mob, false);
    }
    private static boolean chooseHand(Mob mob, State state) {
        boolean main = eligible(mob.getMainHandItem()), off = eligible(mob.getOffhandItem());
        if (!main && !off) return false;
        state.hand = off && (GuardItemRules.shieldLike(mob.getOffhandItem()) || !main) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack item = mob.getItemInHand(state.hand);
        state.item = item; return true;
    }
    private static boolean begin(Mob mob, State state, LivingEntity target, Behavior behavior) {
        if (!GuardConfig.PARRY.get() || target == null || !target.isAlive() || !chooseHand(mob, state) || mob.isPassenger()) return false;
        long now = mob.level().getGameTime();
        state.target = target.getUUID(); state.behavior = behavior; state.pressureHits = 0;
        state.side = mob.getRandom().nextBoolean() ? 0.65F : -0.65F;
        state.radius = Math.max(1.2D, Math.hypot(target.getX() - mob.getX(), target.getZ() - mob.getZ()));
        state.movement = behavior == Behavior.TACTICAL
            ? MOVEMENTS[mob.getRandom().nextInt(MOVEMENTS.length)] : Movement.STILL;
        int maximum=(behavior==Behavior.TACTICAL?GuardConfig.MOB_TACTICAL_SECONDS:GuardConfig.MOB_APPROACH_SECONDS).get();
        state.startedAt=now;
        state.guardUntil=state.behaviorUntil=now+(1+mob.getRandom().nextInt(maximum))*20;
        pose(mob,state,true);
        if(behavior==Behavior.TACTICAL)state.tacticalReady=state.behaviorUntil+GuardConfig.MOB_TACTICAL_COOLDOWN.get();
        // Stop conflicting interruptible goals immediately; keep their registrations intact.
        for (var goal : mob.goalSelector.getAvailableGoals()) {
            if (!goal.isRunning() || !goal.isInterruptable() || goal.getGoal() instanceof GuardBehaviorGoal) continue;
            var flags = goal.getFlags();
            if (flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE)
                || flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK)
                || flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP)) goal.stop();
        }
        mob.stopUsingItem(); mob.setAggressive(false);
        movement(mob, true); stopMovement(mob);
        return true;
    }
    private static boolean decision(Mob mob,double chance){
        double effective=Math.max(0,Math.min(100,chance))*GuardConfig.MOB_DIFFICULTY.get()/100D;
        return effective>0 && mob.getRandom().nextDouble()*100<effective;
    }
    private static void counter(Mob mob, State state) {
        LivingEntity target = target(mob, state); long now = mob.level().getGameTime();
        finish(mob, state);
        if (target == null) return;
        state.nextAttempt = now + (GuardItemRules.shieldLike(state.item) ? GuardConfig.SHIELD_RECHARGE_TICKS : GuardConfig.RECHARGE_TICKS).get();
        // At most one immediate follow-up stance before committing to a counter.
        boolean rush = decision(mob,GuardConfig.MOB_RUSH_CHANCE.get());
        if (!rush && state.chains == 0 && decision(mob,50) && begin(mob, state, target, Behavior.PRESSURE)) {
            state.chains = 1; state.rechargeUntil = now; return;
        }
        state.chains = 0;
        int ticks = GuardConfig.MOB_COUNTER_TICKS.get(); if (!rush || ticks == 0) return;
        state.behavior = Behavior.COUNTER; state.behaviorUntil = now + ticks;
        state.side = mob.getRandom().nextBoolean() ? 0 : mob.getRandom().nextBoolean() ? 0.65F : -0.65F;
    }
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        State state = STATES.get(mob); long now = mob.level().getGameTime();
        if (!GuardConfig.MOB_GUARD.get() || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob) || !supported(mob) || ranged(mob)
            || !GuardConfig.PARRY.get()) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); } return;
        }
        if (!eligible(mob.getMainHandItem()) && !eligible(mob.getOffhandItem())) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); }
            return;
        }
        if (GOALS_ADDED.add(mob)) mob.goalSelector.addGoal(-1, new GuardBehaviorGoal(mob));
        LivingEntity target = state == null || state.behavior == Behavior.IDLE && mob.getTarget() != null ? mob.getTarget() : target(mob, state);
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 256) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); } return;
        }
        if (state == null) { state = new State(); state.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); state.target = target.getUUID(); STATES.put(mob, state); }
        if (state.behavior != Behavior.IDLE) {
            if (now >= state.behaviorUntil || mob.getItemInHand(state.hand) != state.item) { finish(mob, state); state.chains = 0; return; }
            return;
        }
        if (!target.getUUID().equals(state.target)) { state.target = target.getUUID(); state.approached = false; state.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); state.pressureHits = 0; }
        if ((mob.tickCount + mob.getId()) % 5 != 0 || now < state.nextAttempt || now < state.rechargeUntil) return;
        if (!state.approached && mob.distanceToSqr(target) <= Math.max(1, state.approachDistance * state.approachDistance)) {
            state.approached = true;
            if (decision(mob,GuardConfig.MOB_APPROACH_CHANCE.get()) && begin(mob, state, target, Behavior.APPROACH)) return;
        }
        if (now < state.nextDecision) return;
        state.nextDecision = now + 40 + mob.getRandom().nextInt(41);
        if (now >= state.tacticalReady && decision(mob,GuardConfig.MOB_TACTICAL_CHANCE.get()))
            begin(mob, state, target, Behavior.TACTICAL);
    }
    public static void damaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !GuardConfig.MOB_GUARD.get() || !supported(mob) || ranged(mob)
            || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob) || event.getNewDamage() <= 0
            || !GuardConfig.PARRY.get() || MallardGuard.returningDamage()
            || !(event.getSource().getEntity() instanceof LivingEntity attacker)
            || !combatOpponent(mob,attacker)) return;
        State state = STATES.computeIfAbsent(mob, ignored -> { State fresh = new State(); fresh.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); return fresh; });
        if (state.behavior != Behavior.IDLE || !chooseHand(mob, state)) return;
        long now = mob.level().getGameTime();
        if (now - state.lastPressureHit > 200) state.pressureHits = 0;
        state.lastPressureHit = now; state.target = attacker.getUUID(); state.pressureHits = Math.min(100, state.pressureHits + 1);
        int threshold = GuardConfig.MOB_HITS_TO_GUARD.get();
        if (state.pressureHits < threshold || now < state.rechargeUntil || now < state.nextAttempt) return;
        double chance = Math.min(100, 40.0D * state.pressureHits / threshold);
        if (decision(mob,chance)) { state.chains = 0; begin(mob, state, attacker, Behavior.PRESSURE); }
    }
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob) {
            State state = STATES.remove(mob); if (state != null && state.behavior != Behavior.IDLE) finish(mob, state);
        }
    }
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var entry : STATES.entrySet()) if (entry.getValue().behavior != Behavior.IDLE) finish(entry.getKey(), entry.getValue());
        STATES.clear(); GOALS_ADDED.clear(); PROJECTILE_DEFENSES.clear(); clearGear(); whitelistIds = java.util.Set.of(); cachedIds = "";
    }
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && GuardConfig.MOB_GUARD.get() && event.getEntity() instanceof Mob mob && supported(mob) && GOALS_ADDED.add(mob))
            mob.goalSelector.addGoal(-1, new GuardBehaviorGoal(mob));
    }
    private static final class GuardBehaviorGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Mob mob;
        GuardBehaviorGoal(Mob mob) { this.mob = mob; setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() {
            State state = STATES.get(mob); return GuardConfig.MOB_GUARD.get() && mob.isAlive() && !mob.isNoAi()
                && !GuardCombatEffects.stunned(mob) && state != null && state.behavior != Behavior.IDLE && target(mob, state) != null;
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { mob.getNavigation().stop(); mob.stopUsingItem(); mob.setAggressive(false); }
        @Override public void stop() { stopMovement(mob); }
        @Override public void tick() {
            State state = STATES.get(mob); if (state == null || state.behavior == Behavior.IDLE) return;
            LivingEntity target = target(mob, state); if (target == null) return;
            mob.getNavigation().stop(); mob.setAggressive(false);
            double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ(), distance = Math.hypot(dx, dz);
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            mob.setYRot(yaw); mob.yBodyRot = yaw; mob.getLookControl().setLookAt(target, 30, 30);
            boolean counter = state.behavior == Behavior.COUNTER;
            if (!counter) { mob.swinging = false; mob.swingTime = 0; mob.attackAnim = 0; }
            float forward = 0, side = 0;
            if (!counter && state.behavior == Behavior.APPROACH) forward = distance > 1.1D ? 1.0F : 0;
            else if (!counter && state.movement == Movement.STRAFE) {
                side = Math.signum(state.side) * 0.90F;
                forward = (float) net.minecraft.util.Mth.clamp((distance - state.radius) * 0.5D, -0.12D, 0.12D);
            }
            if (counter) {
                // Direct, bounded approach; normal collision and move control still apply.
                double offset = state.side * Math.min(0.8D, distance * 0.3D);
                double divisor = Math.max(0.001D, distance);
                mob.getMoveControl().setWantedPosition(target.getX() - dz / divisor * offset,
                    target.getY(), target.getZ() + dx / divisor * offset, 1.35D);
            } else { movement(mob, true); mob.getMoveControl().strafe(forward, side); }
            long now = mob.level().getGameTime();
            if (counter && now >= state.attackReady && mob.isWithinMeleeAttackRange(target) && mob.getSensing().hasLineOfSight(target)) {
                state.attackReady = now + 20; mob.swing(state.hand); mob.doHurtTarget(target); finish(mob, state);
            }
        }
    }

    /** Equipment shown by the shared browser and accepted by the spawn gear pools. */
    public static EquipmentSlot equipmentSlot(net.minecraft.world.item.Item item){
        if(item instanceof net.minecraft.world.item.ArmorItem armor){
            EquipmentSlot slot=armor.getType().getSlot();
            return slot==EquipmentSlot.HEAD||slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET?slot:null;
        }
        if(item instanceof net.minecraft.world.item.ElytraItem)return EquipmentSlot.CHEST;
        if(item instanceof net.minecraft.world.item.ShieldItem)return EquipmentSlot.OFFHAND;
        return item instanceof net.minecraft.world.item.TieredItem||item instanceof net.minecraft.world.item.ProjectileWeaponItem
            ||GuardItemRules.hasAttackDamage(item.getDefaultInstance())?EquipmentSlot.MAINHAND:null;
    }
    private record GearPool(net.minecraft.world.item.Item[] items,int[] ends,int total){
        net.minecraft.world.item.Item choose(Mob mob){
            if(total==0)return null;
            int at=java.util.Arrays.binarySearch(ends,1+mob.getRandom().nextInt(total));
            return items[at<0?-at-1:at];
        }
    }
    private record GearPools(java.util.Map<EquipmentSlot,GearPool> slots,GearPool melee,GearPool ranged){}
    private static GearPools cachedGear;
    private static String cachedGearAllowed="",cachedGearBlocked="";
    private static synchronized void clearGear(){cachedGear=null;cachedGearAllowed=cachedGearBlocked="";}
    public static void tagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event){clearGear();}
    private static GearPool pool(java.util.List<net.minecraft.world.item.Item> items){
        int[] ends=new int[items.size()];int total=0;
        for(int i=0;i<items.size();i++){
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(items.get(i)).getPath();
            int weight=id.startsWith("netherite_")?1:id.startsWith("diamond_")||id.startsWith("golden_")?5:id.startsWith("iron_")?30:60;
            ends[i]=total+=weight;
        }
        return new GearPool(items.toArray(net.minecraft.world.item.Item[]::new),ends,total);
    }
    private static synchronized GearPools gearPools(){
        String allowed=GuardConfig.MOB_GEAR_WHITELIST.get(),blocked=GuardConfig.MOB_GEAR_BLACKLIST.get();
        if(cachedGear!=null&&allowed.equals(cachedGearAllowed)&&blocked.equals(cachedGearBlocked))return cachedGear;
        var allow=GuardItemRules.compile(allowed);var deny=GuardItemRules.compile(blocked);
        var slots=new java.util.EnumMap<EquipmentSlot,java.util.List<net.minecraft.world.item.Item>>(EquipmentSlot.class);
        var melee=new java.util.ArrayList<net.minecraft.world.item.Item>();var ranged=new java.util.ArrayList<net.minecraft.world.item.Item>();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
            EquipmentSlot slot=equipmentSlot(item);if(slot==null)continue;
            var stack=item.getDefaultInstance();if(!allow.matches(stack)||deny.matches(stack))continue;
            if(slot==EquipmentSlot.MAINHAND){if(item instanceof net.minecraft.world.item.ProjectileWeaponItem)ranged.add(item);else melee.add(item);}
            else slots.computeIfAbsent(slot,ignored->new java.util.ArrayList<>()).add(item);
        }
        var built=new java.util.EnumMap<EquipmentSlot,GearPool>(EquipmentSlot.class);
        slots.forEach((slot,items)->built.put(slot,pool(items)));
        cachedGearAllowed=allowed;cachedGearBlocked=blocked;
        return cachedGear=new GearPools(java.util.Map.copyOf(built),pool(melee),pool(ranged));
    }
    private static void equipEmpty(Mob mob,EquipmentSlot slot,GearPools pools){
        if(!mob.getItemBySlot(slot).isEmpty())return;
        GearPool pool=pools.slots().get(slot);var item=pool==null?null:pool.choose(mob);
        if(item!=null){mob.setItemSlot(slot,new ItemStack(item));mob.setDropChance(slot,0);}
    }
    public static void equip(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||event.loadedFromDisk()||!GuardConfig.MOB_GUARD.get()
            ||!(event.getEntity() instanceof Mob mob)||!supported(mob)||!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft")
            ||!(mob instanceof Zombie||mob instanceof AbstractSkeleton||mob instanceof net.minecraft.world.entity.monster.Vindicator||mob instanceof net.minecraft.world.entity.monster.Pillager||mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin)
            ||mob.getPersistentData().getBoolean(EQUIPMENT_ROLLED))return;
        mob.getPersistentData().putBoolean(EQUIPMENT_ROLLED,true);
        if(mob.getRandom().nextInt(100)>=GuardConfig.MOB_GEAR_CHANCE.get())return;
        GearPools pools=gearPools();
        equipEmpty(mob,EquipmentSlot.HEAD,pools);equipEmpty(mob,EquipmentSlot.CHEST,pools);
        equipEmpty(mob,EquipmentSlot.LEGS,pools);equipEmpty(mob,EquipmentSlot.FEET,pools);
        ItemStack held=mob.getMainHandItem();
        // Preserve existing melee gear; a ranged loadout may be rerolled as in the prior implementation.
        if(held.isEmpty()||held.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem){
            boolean ranged=mob instanceof net.minecraft.world.entity.monster.Pillager||mob instanceof AbstractSkeleton&&mob.getRandom().nextInt(3)==0;
            GearPool pool=ranged?pools.ranged():pools.melee();
            if(pool.total()==0)pool=ranged?pools.melee():pools.ranged();
            var item=pool.choose(mob);if(item!=null)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(item));
        }
        if(!ranged(mob)&&mob.getRandom().nextInt(100)<35)equipEmpty(mob,EquipmentSlot.OFFHAND,pools);
    }
}
