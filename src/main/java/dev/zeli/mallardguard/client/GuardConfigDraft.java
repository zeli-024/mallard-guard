package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import java.util.*;
import java.util.function.*;

/** Editable values, snapshots and save ownership; independent of screen layout. */
final class GuardConfigDraft {
    boolean parriedEnabled,shieldConeEnabled;
    boolean retaliationEnabled,perfectProjectilesOnly,perfectFallOnly;
    int perfectKnockbackMultiplier;
    int parriedGuardBreakTicks,parriedStunTicks,incomingReaction;
    boolean enforceEnabled;
    int dontEnforceMask;
    int[] enforcedDefaults;
    int[] extraClient = GuardClientPreset.readLocal();
    boolean mobGuard;
    String mobWhitelist;
    String mobGearWhitelist;
    String mobGearBlacklist;
    int mobFlyingColor;
    int mobDebrisColor;
    int mobTracerStartColor;
    int mobTracerMiddleColor;
    int mobTracerEndColor;
    String enforcedPoseData="";
    int mobDifficulty;
    int mobGearChance;
    LinkedHashMap<String, Boolean> damageRules = new LinkedHashMap<>();
    boolean parryHealing;
    int parryHealingHearts;
    int debugFlags = GuardConfig.DEBUG_CLIENT_FLAGS.get();
    boolean parry;
    boolean block;
    boolean parryDrowningFire;
    boolean parryStarvation;
    boolean parryGenericKill;
    boolean hud;
    boolean shieldEffects;
    boolean memeFlash;
    boolean hitParticles;
    boolean perfectOnlyFlash;
    boolean impactPerfectOnly;
    int flashStrength;
    int shakeStrength;
    int hitlagTicks;
    int impactFrames;
    int impactBrightness;
    int impactContrast;
    int impactEdges;
    int impactGrain;
    int masterVolume;
    int perfectVolume;
    int parryVolume;
    int blockVolume;
    boolean serverHitSounds;
    int serverMasterVolume;
    int serverPerfectVolume;
    int serverParryVolume;
    int serverBlockVolume;
    int perfect;
    int window;
    int recharge;
    int angle;
    int reductionPercent;
    int followUp;
    int parryReturnPercent;
    int perfectReturnPercent;
    int retaliationCap;
    int parryWear;
    int perfectWear;
    int blockWear;
    boolean fallParry;
    boolean fallBreakBlocks;
    boolean parryExplosions;
    boolean perfectExplosionsOnly;
    boolean blockExplosions;
    boolean parryProjectiles;
    boolean blockProjectiles;
    int fallBlastStrength;
    int fallLaunchPower;
    int knockbackStrength;
    int guardMovementPercent;
    int blockDeflectChance;
    boolean allowAnyItem;
    boolean allowEmptyHand;
    boolean allowUsableItems;
    boolean firstPersonAnimation;
    boolean thirdPersonAnimation;
    boolean consumablePriority;
    boolean cooldownPreventsGuard;
    boolean forceCrouchOffhand;
    String itemBlockCounts="";
    int shieldPerfect;
    int shieldWindow;
    int shieldRechargeTicks;
    int shieldMaxBlocks;
    int toolMaxBlocks;
    int shieldBreakTicks;
    int shieldCone;
    int shieldReach;
    int shieldReturn;
    int shieldStun;
    int shieldPerfectPush;
    int shieldRegularPush;
    int toolPush;
    String stunnableBosses;
    String includedItems;
    String excludedItems;
    String shieldItems;
    String shieldBlacklist;
    int parryHandPriority;
    int shieldParryPriority;
    boolean invalidItemRules;

    private record Field<T>(String key, Supplier<T> read, Consumer<T> write, int serverGroups) {
        @SuppressWarnings("unchecked") void restore(Object value) {
            Object current = read.get();
            if (current instanceof int[] array) System.arraycopy((int[]) value, 0, array, 0, array.length);
            else if (current instanceof Map<?, ?> map) {
                var mutable = (Map<Object, Object>) map;
                mutable.clear(); mutable.putAll((Map<Object, Object>) value);
            } else write.accept((T) value);
        }
    }
    private final List<Field<?>> fields = List.of(
        new Field<Boolean>("enforceEnabled", () -> enforceEnabled, value -> enforceEnabled = value, GuardPackets.SAVE_POLICY),
        new Field<Integer>("dontEnforceMask", () -> dontEnforceMask, value -> dontEnforceMask = value, GuardPackets.SAVE_POLICY),
        new Field<int[]>("enforcedDefaults", () -> enforcedDefaults, value -> enforcedDefaults = value, GuardPackets.SAVE_POLICY),
        new Field<int[]>("extraClient", () -> extraClient, value -> extraClient = value, 0),
        new Field<Boolean>("mobGuard", () -> mobGuard, value -> mobGuard = value, GuardPackets.SAVE_MOBS),
        new Field<String>("mobWhitelist", () -> mobWhitelist, value -> mobWhitelist = value, GuardPackets.SAVE_MOBS),
        new Field<String>("mobGearWhitelist", () -> mobGearWhitelist, value -> mobGearWhitelist = value, GuardPackets.SAVE_MOBS),
        new Field<String>("mobGearBlacklist", () -> mobGearBlacklist, value -> mobGearBlacklist = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobFlyingColor", () -> mobFlyingColor, value -> mobFlyingColor = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobDebrisColor", () -> mobDebrisColor, value -> mobDebrisColor = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobTracerStartColor", () -> mobTracerStartColor, value -> mobTracerStartColor = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobTracerMiddleColor", () -> mobTracerMiddleColor, value -> mobTracerMiddleColor = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobTracerEndColor", () -> mobTracerEndColor, value -> mobTracerEndColor = value, GuardPackets.SAVE_MOBS),
        new Field<String>("enforcedPoseData", () -> enforcedPoseData, value -> enforcedPoseData = value, GuardPackets.SAVE_POLICY),
        new Field<Integer>("mobDifficulty", () -> mobDifficulty, value -> mobDifficulty = value, GuardPackets.SAVE_MOBS),
        new Field<Integer>("mobGearChance", () -> mobGearChance, value -> mobGearChance = value, GuardPackets.SAVE_MOBS),
        new Field<LinkedHashMap<String, Boolean>>("damageRules", () -> damageRules, value -> damageRules = value, GuardPackets.SAVE_DAMAGE),
        new Field<Boolean>("retaliationEnabled",()->retaliationEnabled,value->retaliationEnabled=value,GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("perfectProjectilesOnly",()->perfectProjectilesOnly,value->perfectProjectilesOnly=value,GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("perfectFallOnly",()->perfectFallOnly,value->perfectFallOnly=value,GuardPackets.SAVE_SHIELD),
        new Field<Integer>("perfectKnockbackMultiplier",()->perfectKnockbackMultiplier,value->perfectKnockbackMultiplier=value,GuardPackets.SAVE_SHIELD),
        new Field<Integer>("incomingReaction",()->incomingReaction,value->incomingReaction=value,GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("parriedEnabled",()->parriedEnabled,value->parriedEnabled=value,GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("shieldConeEnabled",()->shieldConeEnabled,value->shieldConeEnabled=value,GuardPackets.SAVE_SHIELD),
        new Field<Integer>("parriedGuardBreakTicks",()->parriedGuardBreakTicks,value->parriedGuardBreakTicks=value,GuardPackets.SAVE_SHIELD),
        new Field<Integer>("parriedStunTicks",()->parriedStunTicks,value->parriedStunTicks=value,GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("parryHealing", () -> parryHealing, value -> parryHealing = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("parryHealingHearts", () -> parryHealingHearts, value -> parryHealingHearts = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("debugFlags", () -> debugFlags, value -> debugFlags = value, 0),
        new Field<Boolean>("parry", () -> parry, value -> parry = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("block", () -> block, value -> block = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("parryDrowningFire", () -> parryDrowningFire, value -> parryDrowningFire = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("parryStarvation", () -> parryStarvation, value -> parryStarvation = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("parryGenericKill", () -> parryGenericKill, value -> parryGenericKill = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("hud", () -> hud, value -> hud = value, 0),
        new Field<Boolean>("shieldEffects", () -> shieldEffects, value -> shieldEffects = value, 0),
        new Field<Boolean>("memeFlash", () -> memeFlash, value -> memeFlash = value, 0),
        new Field<Boolean>("hitParticles", () -> hitParticles, value -> hitParticles = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("perfectOnlyFlash", () -> perfectOnlyFlash, value -> perfectOnlyFlash = value, 0),
        new Field<Boolean>("impactPerfectOnly", () -> impactPerfectOnly, value -> impactPerfectOnly = value, 0),
        new Field<Integer>("flashStrength", () -> flashStrength, value -> flashStrength = value, 0),
        new Field<Integer>("shakeStrength", () -> shakeStrength, value -> shakeStrength = value, 0),
        new Field<Integer>("hitlagTicks", () -> hitlagTicks, value -> hitlagTicks = value, 0),
        new Field<Integer>("impactFrames", () -> impactFrames, value -> impactFrames = value, 0),
        new Field<Integer>("impactBrightness", () -> impactBrightness, value -> impactBrightness = value, 0),
        new Field<Integer>("impactContrast", () -> impactContrast, value -> impactContrast = value, 0),
        new Field<Integer>("impactEdges", () -> impactEdges, value -> impactEdges = value, 0),
        new Field<Integer>("impactGrain", () -> impactGrain, value -> impactGrain = value, 0),
        new Field<Integer>("masterVolume", () -> masterVolume, value -> masterVolume = value, 0),
        new Field<Integer>("perfectVolume", () -> perfectVolume, value -> perfectVolume = value, 0),
        new Field<Integer>("parryVolume", () -> parryVolume, value -> parryVolume = value, 0),
        new Field<Integer>("blockVolume", () -> blockVolume, value -> blockVolume = value, 0),
        new Field<Boolean>("serverHitSounds", () -> serverHitSounds, value -> serverHitSounds = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("serverMasterVolume", () -> serverMasterVolume, value -> serverMasterVolume = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("serverPerfectVolume", () -> serverPerfectVolume, value -> serverPerfectVolume = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("serverParryVolume", () -> serverParryVolume, value -> serverParryVolume = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("serverBlockVolume", () -> serverBlockVolume, value -> serverBlockVolume = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("perfect", () -> perfect, value -> perfect = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("window", () -> window, value -> window = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("recharge", () -> recharge, value -> recharge = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("angle", () -> angle, value -> angle = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("reductionPercent", () -> reductionPercent, value -> reductionPercent = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("followUp", () -> followUp, value -> followUp = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("parryReturnPercent", () -> parryReturnPercent, value -> parryReturnPercent = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("perfectReturnPercent", () -> perfectReturnPercent, value -> perfectReturnPercent = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("retaliationCap", () -> retaliationCap, value -> retaliationCap = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("parryWear", () -> parryWear, value -> parryWear = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("perfectWear", () -> perfectWear, value -> perfectWear = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("blockWear", () -> blockWear, value -> blockWear = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("fallParry", () -> fallParry, value -> fallParry = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("fallBreakBlocks", () -> fallBreakBlocks, value -> fallBreakBlocks = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("parryExplosions", () -> parryExplosions, value -> parryExplosions = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("perfectExplosionsOnly", () -> perfectExplosionsOnly, value -> perfectExplosionsOnly = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("blockExplosions", () -> blockExplosions, value -> blockExplosions = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("parryProjectiles", () -> parryProjectiles, value -> parryProjectiles = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("blockProjectiles", () -> blockProjectiles, value -> blockProjectiles = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("fallBlastStrength", () -> fallBlastStrength, value -> fallBlastStrength = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("fallLaunchPower", () -> fallLaunchPower, value -> fallLaunchPower = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("knockbackStrength", () -> knockbackStrength, value -> knockbackStrength = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("guardMovementPercent", () -> guardMovementPercent, value -> guardMovementPercent = value, GuardPackets.SAVE_RULES),
        new Field<Integer>("blockDeflectChance", () -> blockDeflectChance, value -> blockDeflectChance = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("allowAnyItem", () -> allowAnyItem, value -> allowAnyItem = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("allowEmptyHand", () -> allowEmptyHand, value -> allowEmptyHand = value, GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("allowUsableItems", () -> allowUsableItems, value -> allowUsableItems = value, GuardPackets.SAVE_RULES),
        new Field<Boolean>("firstPersonAnimation", () -> firstPersonAnimation, value -> firstPersonAnimation = value, 0),
        new Field<Boolean>("thirdPersonAnimation", () -> thirdPersonAnimation, value -> thirdPersonAnimation = value, 0),
        new Field<Boolean>("consumablePriority", () -> consumablePriority, value -> consumablePriority = value, GuardPackets.SAVE_RULES | GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("cooldownPreventsGuard", () -> cooldownPreventsGuard, value -> cooldownPreventsGuard = value, GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("forceCrouchOffhand", () -> forceCrouchOffhand, value -> forceCrouchOffhand = value, GuardPackets.SAVE_SHIELD),
        new Field<String>("itemBlockCounts", () -> itemBlockCounts, value -> itemBlockCounts = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldPerfect", () -> shieldPerfect, value -> shieldPerfect = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldWindow", () -> shieldWindow, value -> shieldWindow = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldRechargeTicks", () -> shieldRechargeTicks, value -> shieldRechargeTicks = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldMaxBlocks", () -> shieldMaxBlocks, value -> shieldMaxBlocks = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("toolMaxBlocks", () -> toolMaxBlocks, value -> toolMaxBlocks = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldBreakTicks", () -> shieldBreakTicks, value -> shieldBreakTicks = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldCone", () -> shieldCone, value -> shieldCone = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldReach", () -> shieldReach, value -> shieldReach = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldReturn", () -> shieldReturn, value -> shieldReturn = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldStun", () -> shieldStun, value -> shieldStun = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldPerfectPush", () -> shieldPerfectPush, value -> shieldPerfectPush = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldRegularPush", () -> shieldRegularPush, value -> shieldRegularPush = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("toolPush", () -> toolPush, value -> toolPush = value, GuardPackets.SAVE_SHIELD),
        new Field<String>("stunnableBosses", () -> stunnableBosses, value -> stunnableBosses = value, GuardPackets.SAVE_SHIELD),
        new Field<String>("includedItems", () -> includedItems, value -> includedItems = value, GuardPackets.SAVE_RULES),
        new Field<String>("excludedItems", () -> excludedItems, value -> excludedItems = value, GuardPackets.SAVE_RULES),
        new Field<String>("shieldItems", () -> shieldItems, value -> shieldItems = value, GuardPackets.SAVE_RULES),
        new Field<String>("shieldBlacklist", () -> shieldBlacklist, value -> shieldBlacklist = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("parryHandPriority", () -> parryHandPriority, value -> parryHandPriority = value, GuardPackets.SAVE_SHIELD),
        new Field<Integer>("shieldParryPriority", () -> shieldParryPriority, value -> shieldParryPriority = value, GuardPackets.SAVE_SHIELD),
        new Field<Boolean>("invalidItemRules", () -> invalidItemRules, value -> invalidItemRules = value, 0)
    );
    private final Map<String, Field<?>> byKey = fields.stream().collect(java.util.stream.Collectors.toMap(Field::key, Function.identity()));

    Map<String, Object> capture() {
        var snapshot = new LinkedHashMap<String, Object>();
        for (var field : fields) {
            Object value = field.read().get();
            if (value instanceof int[] array) value = array.clone();
            else if (value instanceof Map<?, ?> map) value = new LinkedHashMap<>(map);
            snapshot.put(field.key(), value);
        }
        return snapshot;
    }
    void restore(String key, Object value) { byKey.get(key).restore(value); }
    int serverGroups(String key) { return byKey.get(key).serverGroups(); }
}
