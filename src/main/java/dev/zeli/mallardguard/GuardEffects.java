package dev.zeli.mallardguard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class GuardEffects {
    // Track the last two actual clips for each player, across all guard outcomes.
    private static final Map<net.minecraft.world.entity.LivingEntity, int[]> LAST_SOUNDS = new WeakHashMap<>();
    public static void clearSession(){LAST_SOUNDS.clear();}

    private GuardEffects() {}
    public static float projectileDamageEstimate(net.minecraft.world.entity.projectile.Projectile projectile) {
        if(projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident)return 8;
        if(projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)
            return (float)Math.min(1000000,Math.ceil(Math.max(0,arrow.getBaseDamage()*projectile.getDeltaMovement().length())));
        return 2;
    }

    public static void forget(ServerPlayer player) {
        LAST_SOUNDS.remove(player);
    }

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result, float damage) {
        onHit(player,source,result,null,damage);
    }

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result, Vec3 impactDirection,float damage) {
        if(result!=GuardState.Result.BLOCK && source.getEntity() instanceof ServerPlayer attacker && attacker!=player)
            PacketDistributor.sendToPlayer(attacker,new GuardPackets.MobCounter(result==GuardState.Result.PERFECT,player.getId(),GuardConfig.INCOMING_REACTION.get()!=0&&GuardConfig.PARRIED_ENABLED.get(),GuardCombatEffects.incomingParry(attacker)));
        emit(player, source, result, impactDirection, GuardState.shieldGuard(player), GuardState.emptyHandGuard(player),damage);
    }

    public static void onMobHit(net.minecraft.world.entity.Mob mob, DamageSource source, GuardState.Result result, boolean shield,float damage) {
        if (result != GuardState.Result.BLOCK && source.getEntity() instanceof ServerPlayer attacker) {
            PacketDistributor.sendToPlayer(attacker, new GuardPackets.MobCounter(result == GuardState.Result.PERFECT,mob.getId(),GuardConfig.INCOMING_REACTION.get()!=0&&GuardConfig.PARRIED_ENABLED.get(),GuardCombatEffects.incomingParry(attacker)));
        }
        emit(mob, source, result, null, shield, false,damage);
    }

    private static void emit(net.minecraft.world.entity.LivingEntity player, DamageSource source, GuardState.Result result,
        Vec3 impactDirection, boolean shield, boolean empty,float damage) {
        ServerLevel level = (ServerLevel) player.level();
        if (GuardConfig.HIT_SOUNDS.get()) {
            boolean shieldSound = shield;
            boolean fallSound = result != GuardState.Result.BLOCK
                && (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL));
            int kind = fallSound ? result == GuardState.Result.PERFECT ? 5 : 6 : shieldSound && result == GuardState.Result.PERFECT ? 4
                : shieldSound ? 3 : result == GuardState.Result.PERFECT ? 2
                : result == GuardState.Result.PARRY ? 1 : 3;
            List<DeferredHolder<SoundEvent, SoundEvent>> sounds = fallSound ? kind == 5 ? GuardSounds.FALL_PARRY : GuardSounds.FALL_REGULAR
                : kind == 4 ? GuardSounds.SHIELD_PERFECT : shieldSound ? GuardSounds.BLOCK : switch (result) {
                case PERFECT -> GuardSounds.PERFECT;
                case PARRY -> GuardSounds.PARRY;
                case BLOCK -> GuardSounds.BLOCK;
                default -> List.of();
            };
            int individual = kind == 3 ? GuardConfig.BLOCK_VOLUME.get() : switch (result) {
                case PERFECT -> GuardConfig.PERFECT_VOLUME.get();
                case PARRY -> GuardConfig.PARRY_VOLUME.get();
                case BLOCK -> GuardConfig.BLOCK_VOLUME.get();
                default -> 0;
            };
            // Preserve the complete server volume so each client can select its louder clip when needed.
            float volume = GuardConfig.MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
            if (!sounds.isEmpty() && volume > 0) {
                int[] previous = LAST_SOUNDS.computeIfAbsent(player, ignored -> new int[]{-1, -1});
                int[] eligible = new int[sounds.size()];
                int count = 0;
                for (int variant = 0; variant < sounds.size(); variant++) {
                    int clip = kind * 16 + variant;
                    if (clip != previous[0] && clip != previous[1]) eligible[count++] = variant;
                }
                // Each fall outcome has one recording; let it play on every fall.
                if (count > 0 || kind == 5 || kind == 6) {
                    int choice = count > 0 ? eligible[ThreadLocalRandom.current().nextInt(count)] : 0;
                    previous[1] = previous[0];
                    previous[0] = kind * 16 + choice;
                    // Send the selected clip once; each listener applies their own local volume.
                    for (ServerPlayer viewer : level.players()) {
                        if (viewer.distanceToSqr(player) <= 32.0D * 32.0D)
                            PacketDistributor.sendToPlayer(viewer, new GuardPackets.HitSound(player.getX(), player.getY(), player.getZ(),
                                kind, choice, volume, viewer == player, result != GuardState.Result.BLOCK));
                    }
                }
            }
        }
        if (!GuardConfig.HIT_PARTICLES.get() || result == GuardState.Result.BLOCK || empty) return;

        Entity attacker = source.getEntity();
        Vec3 direction = impactDirection != null ? impactDirection
            : attacker == null ? player.getLookAngle() : attacker.position().subtract(player.position());
        if (direction.horizontalDistanceSqr() < 1.0E-6D) direction = player.getLookAngle();
        Vec3 center = player.position().add(direction.normalize().scale(0.55D)).add(0, player.getBbHeight() * 0.67D, 0);
        // Capture horizontal facing once for shield-biased launches on every viewer.
        double yaw=Math.toRadians(player.getYRot());
        Vec3 facing=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        boolean mobPalette=player instanceof net.minecraft.world.entity.Mob;
        GuardPackets.Sparks packet = new GuardPackets.Sparks(center.x,center.y,center.z,
            result==GuardState.Result.PERFECT,false,Float.isFinite(damage)?Math.max(0,damage):0,
            (float)facing.x,(float)facing.y,(float)facing.z,shield,source.is(DamageTypes.FALL)||source.is(DamageTypes.FLY_INTO_WALL),
            player instanceof ServerPlayer serverPlayer ? GuardPlayerPalettes.playerPaletteId(serverPlayer) : 0,
            mobPalette?GuardConfig.MOB_TRACER_START.get():-1,mobPalette?GuardConfig.MOB_TRACER_MIDDLE.get():-1,mobPalette?GuardConfig.MOB_TRACER_END.get():-1);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= 32.0D * 32.0D) PacketDistributor.sendToPlayer(viewer, (viewer == player || player instanceof net.minecraft.world.entity.Mob && result == GuardState.Result.PERFECT && source.getEntity() == viewer)
                ? new GuardPackets.Sparks(packet.x(),packet.y(),packet.z(),packet.perfect(),true,packet.damage(),packet.facingX(),packet.facingY(),packet.facingZ(),packet.shield(),packet.fall(),packet.paletteId(),packet.tracerStartColor(),packet.tracerMiddleColor(),packet.tracerEndColor()) : packet);
        }
    }
}
