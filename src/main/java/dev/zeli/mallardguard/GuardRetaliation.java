package dev.zeli.mallardguard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Server-owned damage waits for the defender's local feedback completion. */
public final class GuardRetaliation {
    private record Hit(UUID target, float amount) {}
    private record Pending(ResourceKey<Level> dimension, long expiresAt, List<Hit> hits) {}
    private static final Map<UUID, LinkedHashMap<Integer, Pending>> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> CURRENT = new HashMap<>(), CLIENT_TICKS = new HashMap<>();
    private static int nextToken, enforcedTicks = GuardClientSettings.initial(3);

    public static void clearSession(){PENDING.clear();CURRENT.clear();CLIENT_TICKS.clear();nextToken=0;}

    private GuardRetaliation() {}

    public static void clientTicks(ServerPlayer player, int ticks) {
        CLIENT_TICKS.put(player.getUUID(), Math.clamp(ticks, 0, 30));
    }

    public static void enforcedTicks(int ticks) {
        enforcedTicks = Math.clamp(ticks, 0, 30);
    }

    public static int begin(ServerPlayer player) {
        UUID id = player.getUUID();
        CURRENT.remove(id);
        boolean locked = GuardConfig.ENFORCE_CLIENT.get()
            && (GuardConfig.CLIENT_EXEMPT_MASK.get() & GuardClientPreset.SCREEN) == 0;
        int ticks = locked ? enforcedTicks : CLIENT_TICKS.getOrDefault(id, 0);
        if (ticks == 0) return 0;
        LinkedHashMap<Integer, Pending> pending = PENDING.computeIfAbsent(id, ignored -> new LinkedHashMap<>());
        // Bound retained work even when completion packets never arrive.
        if (pending.size() >= 16) complete(player, pending.keySet().iterator().next());
        pending = PENDING.computeIfAbsent(id, ignored -> new LinkedHashMap<>());
        nextToken = nextToken == Integer.MAX_VALUE ? 1 : nextToken + 1;
        pending.put(nextToken, new Pending(player.level().dimension(), System.nanoTime() + 2_000_000_000L, new ArrayList<>()));
        CURRENT.put(id, nextToken);
        return nextToken;
    }

    public static void damage(ServerPlayer player, LivingEntity target, float amount) {
        int cap = GuardConfig.RETALIATION_CAP.get();
        if (cap > 0) amount = Math.min(amount, cap);
        if (!Float.isFinite(amount) || amount <= 0) return;
        Map<Integer, Pending> pending = PENDING.get(player.getUUID());
        Pending entry = pending == null ? null : pending.get(CURRENT.get(player.getUUID()));
        if (entry == null) {
            MallardGuard.returnDamage(player, target, amount);
            return;
        }
        entry.hits().add(new Hit(target.getUUID(), amount));
    }

    public static void complete(ServerPlayer player, int token) {
        Map<Integer, Pending> pending = PENDING.get(player.getUUID());
        if (pending == null) return;
        Pending entry = pending.remove(token);
        if (entry == null) return;
        if (pending.isEmpty()) PENDING.remove(player.getUUID());
        if (CURRENT.getOrDefault(player.getUUID(), 0) == token) CURRENT.remove(player.getUUID());
        if (!player.isAlive() || !player.level().dimension().equals(entry.dimension())) return;
        ServerLevel level = player.serverLevel();
        for (Hit hit : entry.hits()) {
            if (level.getEntity(hit.target()) instanceof LivingEntity target && target.isAlive() && target != player)
                MallardGuard.returnDamage(player, target, hit.amount());
        }
    }

    public static void tick(ServerPlayer player) {
        Map<Integer, Pending> pending = PENDING.get(player.getUUID());
        if (pending == null) return;
        long now = System.nanoTime();
        List<Integer> expired = null;
        for (var entry : pending.entrySet()) if (now >= entry.getValue().expiresAt()) {
            if (expired == null) expired = new ArrayList<>();
            expired.add(entry.getKey());
        }
        if (expired != null) for (int token : expired) complete(player, token);
    }

    public static void cancelPending(ServerPlayer player) {
        UUID id = player.getUUID();
        PENDING.remove(id); CURRENT.remove(id);
    }

    public static void forget(ServerPlayer player) {
        UUID id = player.getUUID();
        cancelPending(player); CLIENT_TICKS.remove(id);
    }
}
