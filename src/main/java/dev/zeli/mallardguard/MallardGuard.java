package dev.zeli.mallardguard;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;

@Mod(MallardGuard.ID)
public final class MallardGuard {
    public static final String ID = "mallardguard";
    private static final ThreadLocal<Boolean> RETURNING_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<LivingEntity> RETURN_KNOCKBACK_TARGET = new ThreadLocal<>();

    public MallardGuard(IEventBus modBus, ModContainer container) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            dev.zeli.mallardguard.client.GuardClient.registerConfigScreen(container);
            modBus.addListener(dev.zeli.mallardguard.client.GuardClient::registerKeyMappings);
            modBus.addListener(dev.zeli.mallardguard.client.GuardClient::registerArtworkReload);
        }
        GuardConfig.setCurrentVersion(container.getModInfo().getVersion().toString());
        GuardConfig.prepareConfigFolders();
        GuardConfig.snapshotExistingConfigs();
        modBus.addListener(GuardConfig::onLoad);
        modBus.addListener(GuardConfig::onReload);
        container.registerConfig(ModConfig.Type.SERVER, GuardConfig.SERVER_SPEC, "mallard_guard/server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, GuardConfig.CLIENT_SPEC, "mallard_guard/client.toml");
        container.registerConfig(ModConfig.Type.CLIENT, GuardConfig.PUNCHY_SPEC, "mallard_guard/mg_punchy/config.toml");
        modBus.addListener(GuardCompatibility::setup);
        NeoForge.EVENT_BUS.addListener(GuardCompatibility::serverStarted);
        GuardSounds.EVENTS.register(modBus);
        GuardParticles.TYPES.register(modBus);
        modBus.addListener(GuardPackets::register);
        NeoForge.EVENT_BUS.addListener(GuardState::tick);
        NeoForge.EVENT_BUS.addListener(GuardMobState::tick);
        NeoForge.EVENT_BUS.addListener(GuardMobState::damaged);
        NeoForge.EVENT_BUS.addListener(GuardMobState::equip);
        NeoForge.EVENT_BUS.addListener(GuardMobState::tagsUpdated);
        NeoForge.EVENT_BUS.addListener(GuardMobState::join);
        NeoForge.EVENT_BUS.addListener(GuardMobState::leave);
        NeoForge.EVENT_BUS.addListener(GuardMobState::stopping);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->GuardConfigTransfer.expireUploads());
        NeoForge.EVENT_BUS.addListener(GuardPoses::tracking);
        NeoForge.EVENT_BUS.addListener(GuardState::attacked);
        NeoForge.EVENT_BUS.addListener(GuardState::preventVanillaShieldUse);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::playerJoined);
        NeoForge.EVENT_BUS.addListener(this::playerLeft);
        NeoForge.EVENT_BUS.addListener(this::playerRespawned);
        NeoForge.EVENT_BUS.addListener(this::incomingDamage);
        NeoForge.EVENT_BUS.addListener(this::returnKnockback);
        NeoForge.EVENT_BUS.addListener(this::shieldBlock);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::projectileImpact);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::tickStun);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::startRangedUse);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::rangedItemInteraction);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::projectileSpawn);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::leave);
    }

    private void serverStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){
        GuardPoseLibrary.clearEnforcedCache();GuardState.clearSession();GuardItemRules.clearServerRules();GuardRetaliation.clearSession();GuardDamageRules.clearSession();GuardEffects.clearSession();GuardCombatEffects.clearSession();GuardConfig.serverClosed();GuardConfigTransfer.clearUploads();GuardPlayerPalettes.clearPlayerPalettes();
    }

    private void commands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mallardguardconfig").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("mgc").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
    }

    private int open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new GuardPackets.OpenConfig());
        return 1;
    }

    private void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuardConfigTransfer.sendInitialConfig(player);
            GuardPlayerPalettes.playerPalettesJoined(player);
        }
    }

    private void playerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuardConfigTransfer.forgetUpload(player.getUUID());
            GuardPlayerPalettes.forgetPlayerPalette(player.getUUID());
            GuardState.forget(player);
            GuardDamageRules.forget(player);
            GuardEffects.forget(player);
            GuardRetaliation.forget(player);
        }
    }

    private void playerRespawned(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            GuardState.forget(player);GuardRetaliation.cancelPending(player);GuardEffects.forget(player);
        }
    }

    private void incomingDamage(LivingIncomingDamageEvent event) {
        if (RETURNING_DAMAGE.get()) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacking
            && event.getSource().getDirectEntity() == attacking && GuardCombatEffects.stunned(attacking)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            GuardMobState.incoming(event, mob);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GuardCombatEffects.isOwnFallBlast(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        GuardDamageRules.record(player, event.getSource());
        GuardState.Result result = GuardState.handleHit(player, event.getSource());
        if (result == GuardState.Result.PERFECT || result == GuardState.Result.PARRY) {
            boolean shield = GuardState.shieldGuard(player);
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : 1, false, result == GuardState.Result.PERFECT ? GuardRetaliation.begin(player) : 0, GuardState.inputId(player), event.getSource().getEntity()==null?-1:event.getSource().getEntity().getId()));
            GuardEffects.onHit(player, event.getSource(), result,event.getAmount());
            GuardState.wear(player, result);
            event.setCanceled(true);
            if (event.getSource().is(DamageTypes.FALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
                GuardCombatEffects.fall(player, event.getAmount(), result == GuardState.Result.PERFECT);
            } else {
                Vec3 origin = event.getSource().getSourcePosition();
                GuardCombatEffects.pushDefender(player, origin, shield, result == GuardState.Result.PERFECT);
            }
            if (shield && result == GuardState.Result.PERFECT)
                GuardCombatEffects.shieldPerfect(player, event.getAmount());
            else if (!shield && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                && event.getSource().getDirectEntity() == attacker) {
                float multiplier = result == GuardState.Result.PERFECT ? GuardConfig.PERFECT_RETALIATION.get().floatValue() : GuardConfig.PARRY_RETALIATION.get().floatValue();
                if (GuardConfig.RETALIATION_ENABLED.get() && multiplier > 0 && event.getAmount() > 0) {
                    if (result == GuardState.Result.PERFECT) GuardRetaliation.damage(player, attacker, event.getAmount() * multiplier);
                    else returnDamage(player, attacker, event.getAmount() * multiplier);
                }
            }
            if ((result == GuardState.Result.PARRY || !shield && result == GuardState.Result.PERFECT)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player && event.getSource().getDirectEntity() == attacker)
                GuardCombatEffects.pushAttacker(player, attacker, shield, result == GuardState.Result.PERFECT);
            return;
        }
        if (result == GuardState.Result.BLOCK) {
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3, GuardState.guardBreakPending(player), 0, GuardState.inputId(player), event.getSource().getEntity()==null?-1:event.getSource().getEntity().getId()));
            GuardEffects.onHit(player, event.getSource(), result,event.getAmount());
            GuardState.wear(player, result);
            event.setAmount(event.getAmount() * (1.0F - GuardConfig.BLOCK_REDUCTION.get().floatValue()));
        }
    }

    private void shieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GuardConfig.BLOCK.get() && GuardItemRules.shieldLike(player.getUseItem())) {
            event.setBlocked(false);
            return;
        }
        if (GuardState.guardBreakPending(player)) { event.setBlocked(false); return; }
        if (!GuardState.isShieldBlocking(player)) return;
        if (!event.getBlocked() || event.getBlockedDamage() <= 0) return;
        float intercepted=event.getBlockedDamage();
        if(event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))
            event.setBlockedDamage((float)(intercepted*GuardShieldExpansionCompat.explosionBlockedFraction(player.getUseItem())));
        GuardState.shieldBlocked(player);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3, GuardState.guardBreakPending(player), 0, GuardState.inputId(player), event.getDamageSource().getEntity()==null?-1:event.getDamageSource().getEntity().getId()));
        GuardEffects.onHit(player, event.getDamageSource(), GuardState.Result.BLOCK,intercepted);
        // The item's own shield logic handles damage reduction and durability.
    }

    private void returnKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() == RETURN_KNOCKBACK_TARGET.get()) event.setCanceled(true);
    }

    static boolean returningDamage() { return RETURNING_DAMAGE.get(); }

    public static void returnDamage(LivingEntity defender, LivingEntity target, float amount) {
        int cap = GuardConfig.RETALIATION_CAP.get();
        if (cap > 0) amount = Math.min(amount, cap);
        if (!Float.isFinite(amount) || amount <= 0) return;
        int previousInvulnerability=target.invulnerableTime;
        var damageState=(dev.zeli.mallardguard.mixin.LivingEntityDamageAccessor)target;
        float previousDamage=damageState.mallardguard$getLastHurt();
        boolean previousReturning = RETURNING_DAMAGE.get();
        LivingEntity previousTarget = RETURN_KNOCKBACK_TARGET.get();
        RETURNING_DAMAGE.set(true);
        RETURN_KNOCKBACK_TARGET.set(target);
        try { target.hurt(defender instanceof net.minecraft.world.entity.player.Player player
            ? defender.damageSources().playerAttack(player) : defender.damageSources().mobAttack(defender), amount); }
        finally {
            // Retaliation does not create a recovery window or alter an existing hit's threshold.
            target.invulnerableTime=previousInvulnerability;
            damageState.mallardguard$setLastHurt(previousDamage);
            if (previousTarget == null) RETURN_KNOCKBACK_TARGET.remove();
            else RETURN_KNOCKBACK_TARGET.set(previousTarget);
            if (previousReturning) RETURNING_DAMAGE.set(true);
            else RETURNING_DAMAGE.remove();
        }
    }
}
