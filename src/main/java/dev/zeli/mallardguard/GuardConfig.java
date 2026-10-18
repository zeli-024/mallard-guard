package dev.zeli.mallardguard;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GuardConfig {
    public static final ModConfigSpec.IntValue DEBUG_CLIENT_FLAGS;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec PUNCHY_SPEC;
    public static final ModConfigSpec.BooleanValue PARRY;
    public static final ModConfigSpec.BooleanValue PARRY_HEALING;
    public static final ModConfigSpec.IntValue PARRY_HEALING_HEARTS;
    public static final ModConfigSpec.ConfigValue<String> DAMAGE_RULES;
    public static final ModConfigSpec.ConfigValue<String> OBSERVED_PROJECTILE_SOURCES;
    public static final ModConfigSpec.BooleanValue BLOCK;
    public static final ModConfigSpec.IntValue PERFECT_TICKS;
    public static final ModConfigSpec.IntValue PARRY_TICKS;
    public static final ModConfigSpec.IntValue RECHARGE_TICKS;
    public static final ModConfigSpec.IntValue FACING_ANGLE;
    public static final ModConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ModConfigSpec.IntValue FOLLOW_UP_TICKS;
    public static final ModConfigSpec.DoubleValue PARRY_RETALIATION;
    public static final ModConfigSpec.DoubleValue PERFECT_RETALIATION;
    public static final ModConfigSpec.IntValue RETALIATION_CAP;
    public static final ModConfigSpec.IntValue PARRY_WEAR;
    public static final ModConfigSpec.IntValue PERFECT_WEAR;
    public static final ModConfigSpec.IntValue BLOCK_WEAR;
    public static final ModConfigSpec.BooleanValue HIT_SOUNDS;
    public static final ModConfigSpec.BooleanValue HIT_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENFORCE_CLIENT;
    public static final ModConfigSpec.IntValue CLIENT_EXEMPT_MASK;
    public static final ModConfigSpec.ConfigValue<String> CLIENT_PRESET;
    public static final ModConfigSpec.IntValue MASTER_VOLUME;
    public static final ModConfigSpec.IntValue PERFECT_VOLUME;
    public static final ModConfigSpec.IntValue PARRY_VOLUME;
    public static final ModConfigSpec.IntValue BLOCK_VOLUME;
    public static final ModConfigSpec.BooleanValue FALL_PARRY, PERFECT_FALL_ONLY, PERFECT_PROJECTILES_ONLY, RETALIATION_ENABLED;
    public static final ModConfigSpec.IntValue PERFECT_KNOCKBACK_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue FALL_BREAK_BLOCKS;
    public static final ModConfigSpec.IntValue FALL_BLAST_STRENGTH;
    public static final ModConfigSpec.IntValue FALL_LAUNCH_POWER;
    public static final ModConfigSpec.BooleanValue PARRY_EXPLOSIONS;
    public static final ModConfigSpec.BooleanValue PERFECT_EXPLOSIONS_ONLY;
    public static final ModConfigSpec.BooleanValue BLOCK_EXPLOSIONS;
    public static final ModConfigSpec.BooleanValue PARRY_PROJECTILES;
    public static final ModConfigSpec.BooleanValue BLOCK_PROJECTILES;
    public static final ModConfigSpec.BooleanValue DEFENDER_KNOCKBACK;
    public static final ModConfigSpec.IntValue KNOCKBACK_STRENGTH;
    public static final ModConfigSpec.IntValue GUARD_MOVEMENT_PERCENT;
    public static final ModConfigSpec.IntValue BLOCK_DEFLECT_CHANCE;
    public static final ModConfigSpec.IntValue SHIELD_PERFECT_TICKS, SHIELD_PARRY_TICKS, SHIELD_RECHARGE_TICKS, SHIELD_MAX_BLOCKS, TOOL_MAX_BLOCKS, SHIELD_BREAK_TICKS;
    public static final ModConfigSpec.BooleanValue COOLDOWN_PREVENTS_GUARD, FORCE_CROUCH_OFFHAND;
    public static final ModConfigSpec.BooleanValue PARRIED_ENABLED,SHIELD_CONE_ENABLED;
    public static final ModConfigSpec.IntValue PARRIED_GUARD_BREAK_TICKS,PARRIED_STUN_TICKS,INCOMING_REACTION;
    public static final ModConfigSpec.IntValue SHIELD_CONE_DEGREES, SHIELD_CONE_REACH, SHIELD_RETALIATION_PERCENT, SHIELD_STUN_TICKS, SHIELD_PUSHBACK_PERCENT, TOOL_PUSHBACK_PERCENT, SHIELD_PARRY_PUSHBACK_PERCENT;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_STUN_BOSSES;
    public static final ModConfigSpec.BooleanValue ALLOW_USABLE_ITEMS;
    public static final ModConfigSpec.BooleanValue ALLOW_ANY_ITEM, ALLOW_EMPTY_HAND;
    public static final ModConfigSpec.BooleanValue CONSUMABLE_PRIORITY;
    public static final ModConfigSpec.ConfigValue<String> INCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_ITEMS, ITEM_BLOCK_COUNTS;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.BooleanValue SHIELD_EFFECTS;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH, PERFECT_ONLY_FLASH, SIMPLY_SWORDS_NOTICE_SHOWN, SHIELD_EXPANSION_NOTICE_SHOWN, PUNCHY_MOVEMENT_DEFAULTS_APPLIED;
    public static final ModConfigSpec.BooleanValue MEME_FLASH, IMPACT_PERFECT_ONLY;
    public static final ModConfigSpec.IntValue HITLAG_TICKS, REGULAR_HITLAG_TICKS, IMPACT_FRAMES;
    public static final ModConfigSpec.IntValue IMPACT_BRIGHTNESS, IMPACT_CONTRAST, IMPACT_EDGES, IMPACT_GRAIN;
    public static final ModConfigSpec.IntValue IMPACT_CHROMATIC, MOB_IMPACT_BRIGHTNESS, MOB_IMPACT_CONTRAST, MOB_IMPACT_EDGES, MOB_IMPACT_GRAIN, MOB_IMPACT_CHROMATIC;
    public static final ModConfigSpec.BooleanValue MOB_IMPACT_ADAPT, MOB_IMPACT_INVERT, CHROMATIC_PERFECT_ONLY, CHROMATIC_HUD;
    public static final ModConfigSpec.IntValue CHROMATIC_INTENSITY, CHROMATIC_TICKS;
    public static final ModConfigSpec.IntValue FLASH_STRENGTH;
    public static final ModConfigSpec.IntValue SHAKE_STRENGTH;
    public static final ModConfigSpec.IntValue PARRY_HAND_PRIORITY, SHIELD_PARRY_PRIORITY;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_BLACKLIST;
    public static final ModConfigSpec.BooleanValue PUNCHY_COMPAT;
    public static final ModConfigSpec.IntValue PUNCHY_RELEASE_DELAY;
    public static final java.util.List<ModConfigSpec.ConfigValue<String>> PUNCHY_POSES;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_ANIMATION, THIRD_PERSON_ANIMATION, MOB_GUARD;
    public static final ModConfigSpec.BooleanValue MOB_REGULAR_RETALIATION, MOB_PERFECT_RETALIATION;
    public static final ModConfigSpec.ConfigValue<String> MOB_WHITELIST, MOB_GEAR_WHITELIST, MOB_GEAR_BLACKLIST;
    public static final ModConfigSpec.IntValue MOB_FLYING_COLOR, MOB_DEBRIS_COLOR, MOB_TRACER_START, MOB_TRACER_MIDDLE, MOB_TRACER_END;
    public static final ModConfigSpec.IntValue MOB_APPROACH_CHANCE, MOB_TACTICAL_CHANCE, MOB_TACTICAL_SECONDS, MOB_TACTICAL_COOLDOWN, MOB_MOVEMENT, MOB_APPROACH_DISTANCE, MOB_APPROACH_SECONDS, MOB_RUSH_CHANCE, MOB_GEAR_CHANCE;
    public static final ModConfigSpec.IntValue MOB_COUNTER_TICKS, MOB_DIFFICULTY, MOB_HITS_TO_GUARD, MOB_PERFECT_TICKS;
    public static final ModConfigSpec.IntValue LOCAL_MASTER_VOLUME, LOCAL_PARRY_VOLUME, LOCAL_PERFECT_VOLUME, LOCAL_BLOCK_VOLUME;
    // This changes only when the config schema or a default changes, never with the mod version.
    static final int CONFIG_REVISION = 103;
    private static final ModConfigSpec.IntValue SERVER_REVISION;
    private static final ModConfigSpec.IntValue CLIENT_REVISION;
    private static final ModConfigSpec.IntValue PUNCHY_REVISION;
    private static String currentVersion;
    private static Path serverConfigFolder;
    private record PendingUpdate(ModConfig config, List<String> changes) {}
    private static final List<PendingUpdate> PENDING_UPDATES = new ArrayList<>();
    private static final Map<Path, Path> ORIGINAL_FILES = new ConcurrentHashMap<>();

    // Record default changes in GuardConfigMigration whenever CONFIG_REVISION is raised. A user's saved value
    // cannot tell us what an older default was, so this history must be explicit.
    private static final String DEFAULT_MOB_GEAR =
        "minecraft:leather_helmet,minecraft:leather_chestplate,minecraft:leather_leggings,minecraft:leather_boots," +
        "minecraft:chainmail_helmet,minecraft:chainmail_chestplate,minecraft:chainmail_leggings,minecraft:chainmail_boots," +
        "minecraft:iron_helmet,minecraft:iron_chestplate,minecraft:iron_leggings,minecraft:iron_boots," +
        "minecraft:golden_helmet,minecraft:golden_chestplate,minecraft:golden_leggings,minecraft:golden_boots," +
        "minecraft:diamond_helmet,minecraft:diamond_chestplate,minecraft:diamond_leggings,minecraft:diamond_boots," +
        "minecraft:wooden_sword,minecraft:wooden_pickaxe,minecraft:wooden_shovel,minecraft:wooden_hoe," +
        "minecraft:stone_sword,minecraft:stone_pickaxe,minecraft:stone_shovel,minecraft:stone_hoe," +
        "minecraft:iron_sword,minecraft:iron_pickaxe,minecraft:iron_shovel,minecraft:iron_hoe," +
        "minecraft:golden_sword,minecraft:golden_pickaxe,minecraft:golden_shovel,minecraft:golden_hoe," +
        "minecraft:diamond_sword,minecraft:diamond_pickaxe,minecraft:diamond_shovel,minecraft:diamond_hoe";

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SERVER_REVISION = GuardSettingRanges.integer(server.comment("Mallard Guard config format revision. This changes only when settings or their defaults change."),
            "configRevision", CONFIG_REVISION, 0, Integer.MAX_VALUE);
        server.push("stance");
        PARRY = server.comment("Allow parries when the guard key is pressed. Right Click by default; the key can be changed.").define("parry", true);
        DAMAGE_RULES = server.comment("Per-damage-type parry rules, stored as damage_type_id=1 or =0, separated by commas. Configure in game.")
            .define("damageRules", "minecraft:generic_kill=1,minecraft:arrow=1,minecraft:lava=0,minecraft:hot_floor=0", value -> value instanceof String rules && GuardDamageRules.validRules(rules));
        OBSERVED_PROJECTILE_SOURCES = server.comment("Automatically learned projectile entity ID to damage type ID associations. Updated after players are hit; used to honor modded projectile source rules at impact.")
            .define("observedProjectileSources", "", value -> value instanceof String mapping && GuardDamageRules.validProjectileSources(mapping));
        BLOCK = server.comment("Keep weapons and shields guarding after the parry window while the guard key is held. Off also lowers shields after successful parries.").define("block", true);
        PARRY_HEALING = server.comment("Players and guarding mobs restore health once per successful parry attempt. Blocking and follow-up safety hits do not heal. Off by default.").define("parryHealing", false);
        PARRY_HEALING_HEARTS = GuardSettingRanges.integer(server.comment("Hearts restored by a perfect parry; regular parries restore half. 0 disables healing."), "parryHealingHearts", 1, 0, 5);
        TOOL_MAX_BLOCKS = GuardSettingRanges.integer(server.comment("Successful weapon or empty-hand blocks before guard breaks; 0 allows unlimited blocks. Guard break delays the next guard but does not disable attacking with the item."), "toolMaxBlocks", 3, 0, 5);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Perfect parry duration for tools; zero disables perfect parries. Default: 6 ticks."), "perfectTicks", 6, 0, 10);
        PARRY_TICKS = GuardSettingRanges.integer(server.comment("Total weapon parry duration, including perfect ticks. Default: 9 ticks (6 perfect, then 3 regular)."), "parryTicks", 9, 1, 10);
        RECHARGE_TICKS = GuardSettingRanges.integer(server.comment("Time before another attempt; continues during held guard."), "rechargeTicks", 10, 1, 60);
        server.pop();
        server.push("defense");
        FACING_ANGLE = GuardSettingRanges.integer(server.comment("Maximum angle from the player's view direction; 180 allows the front half, 360 allows attacks from any direction."), "facingAngle", 180, 0, 360);
        BLOCK_REDUCTION = GuardSettingRanges.decimal(server.comment("Fraction of incoming entity damage prevented while holding guard."), "blockReduction", 0.5, 0.0D, 1.0D);
        FOLLOW_UP_TICKS = GuardSettingRanges.integer(server.comment("Ticks after a successful parry that catch additional valid hits; zero disables it. Follow-up hits never extend this window."), "followUpParryTicks", 5, 0, 20);
        RETALIATION_ENABLED = server.comment("Return damage on parries. Off preserves both damage multipliers.").define("retaliationEnabled", true);
        PARRY_RETALIATION = GuardSettingRanges.decimal(server.comment("Regular parry damage returned as a fraction of the incoming damage. This damage adds no knockback; attacker pushback is separate. 0 disables it; 2 doubles it."), "parryRetaliation", 0.4, 0.0D, 2.0D);
        PERFECT_RETALIATION = GuardSettingRanges.decimal(server.comment("Perfect parry damage returned as a fraction of the incoming damage. This damage adds no knockback; attacker pushback is separate. 0 disables it; 2 doubles it."), "perfectRetaliation", 0.8, 0.0D, 2.0D);
        RETALIATION_CAP = GuardSettingRanges.integer(server.comment("Maximum damage returned by one parry to each target after the retaliation multiplier. 0 removes the cap. Applies to weapon and shield retaliation."), "retaliationCap", 25, 0, 1000);
        PARRY_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per regular or follow-up parry. 2 means 0.2%; rounded up. 0 disables wear."), "parryWear", 2, 0, 100);
        PERFECT_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per perfect parry. 1 means 0.1%; rounded up. 0 disables wear."), "perfectWear", 1, 0, 100);
        BLOCK_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per blocked hit. 4 means 0.4%; rounded up. Real shields keep their own block wear."), "blockWear", 4, 0, 100);
        server.pop();
        server.push("effects");
        HIT_SOUNDS = server.comment("Play the supplied parry and block sounds to nearby players.").define("hitSounds", true);
        HIT_PARTICLES = server.comment("Send animated sparks to nearby players. Blocks create none.").define("hitParticles", true);
        MASTER_VOLUME = GuardSettingRanges.integer(server.comment("Server master volume for guard sounds sent to nearby clients. 100% plays the recorded level; each listener's client master and individual volume settings multiply it. 0% mutes the sounds."), "masterVolume", 100, 0, 200);
        PERFECT_VOLUME = GuardSettingRanges.integer(server.comment("Perfect parry sound volume, multiplied by master volume."), "perfectVolume", 130, 0, 200);
        PARRY_VOLUME = GuardSettingRanges.integer(server.comment("Regular parry sound volume, multiplied by master volume."), "parryVolume", 65, 0, 200);
        BLOCK_VOLUME = GuardSettingRanges.integer(server.comment("Held block sound volume, multiplied by master volume."), "blockVolume", 65, 0, 200);
        server.pop();
        server.push("clientEnforcement");
        ENFORCE_CLIENT = server.comment("Apply server chosen client defaults when players connect and lock categories without a Don't Enforce exception.").define("enabled", false);
        CLIENT_EXEMPT_MASK = GuardSettingRanges.integer(server.comment("Client categories editable after receiving server defaults: 1 world particles, 2 HUD icons, 4 audio, 8 screen effects, 16 animations, 32 experimental Punchy poses."), "dontEnforceMask", 0, 0, GuardClientPreset.ALL_CATEGORIES);
        CLIENT_PRESET = server.comment("Client defaults shared by the server; changed through the Enforce configuration screen.")
            .define("defaults", GuardClientPreset.encode(GuardClientPreset.DEFAULTS), value -> value instanceof String data && GuardClientPreset.valid(data));
        server.pop();
        server.push("sources");
        FALL_PARRY = server.comment("Parry falls while looking at least 40 degrees down.").define("fallParry", true);
        PERFECT_FALL_ONLY = server.comment("Require perfect timing for fall and wall-impact parries.").define("perfectFallOnly", false);
        FALL_BREAK_BLOCKS = server.comment("Allow the blast from a long fall parry to break terrain. Disable to prevent terrain damage.").define("fallBlastBreaksBlocks", true);
        FALL_BLAST_STRENGTH = GuardSettingRanges.integer(server.comment("Fall parry blast scaling as a percentage of Parry It's default 0.1 radius per damage; 0 disables the blast."), "fallBlastStrength", 100, 0, 200);
        FALL_LAUNCH_POWER = GuardSettingRanges.integer(server.comment("Fall parry launch strength as a percentage of Parry It's default. Perfect falls launch twice as far. 0 disables launch."), "fallLaunchPower", 10, 0, 200);
        PARRY_EXPLOSIONS = server.comment("Allow parry timing to negate explosion damage.").define("parryExplosions", true);
        PERFECT_EXPLOSIONS_ONLY = server.comment("When explosion parrying is enabled, require a perfect parry; regular parries do not negate explosions.").define("perfectExplosionsOnly", false);
        BLOCK_EXPLOSIONS = server.comment("Allow held block to reduce explosion damage by the normal block reduction.").define("blockExplosions", true);
        PERFECT_PROJECTILES_ONLY = server.comment("Only perfect parries redirect or prevent projectile hits.").define("perfectProjectilesOnly", false);
        PARRY_PROJECTILES = server.comment("Parry projectile entities. Regular parries deflect them randomly; perfect parries send them toward the original owner.").define("parryProjectiles", true);
        BLOCK_PROJECTILES = server.comment("Allow held block to reduce projectile damage by the normal block reduction.").define("blockProjectiles", true);
        DEFENDER_KNOCKBACK = server.comment("Push the defender away from the impact on successful melee, projectile, and explosion parries. Shield parries use half strength.").define("defenderKnockback", true);
        PERFECT_KNOCKBACK_MULTIPLIER = GuardSettingRanges.integer(server.comment("Scale attacker and defender knockback on perfect parries. 100% retains normal strength."), "perfectKnockbackMultiplier", 100, 0, 300);
        KNOCKBACK_STRENGTH = GuardSettingRanges.integer(server.comment("Push strength for the defender in percent; shield parries use half the configured strength. 0 disables both."), "knockbackStrength", 50, 0, 200);
        GUARD_MOVEMENT_PERCENT = GuardSettingRanges.integer(server.comment("Movement speed while guarding, including parry and block. 100 keeps normal movement speed."), "guardMovementPercent", 55, 0, 100);
        BLOCK_DEFLECT_CHANCE = GuardSettingRanges.integer(server.comment("Chance to deflect a projectile during held block. 0 disables deflection; every projectile type uses the same roll."), "blockDeflectChance", 0, 0, 100);
        server.pop();
        server.push("items");
        ALLOW_ANY_ITEM = server.comment("Off: recognized weapons and shields plus extra allowed items may guard. On: all held items except excluded items may guard. Empty hands use a separate toggle.").define("allowAnyItem", false);
        ALLOW_EMPTY_HAND = server.comment("Allow guarding with an empty hand. Separate from allowing all held items; off by default.").define("allowEmptyHand", false);
        CONSUMABLE_PRIORITY = server.comment("When enabled, food and drinks in either hand take priority over guard, even if the other hand holds a weapon. Enabled by default.").define("consumablePriority", true);
        ALLOW_USABLE_ITEMS = server.comment("Allow eligible items with a hold-to-use action to guard. Whitelisted items and universal guard are also allowed.").define("allowUsableItems", false);
        INCLUDED_ITEMS = server.comment("Item IDs, #tags, @mod namespaces, or ID/tag keywords allowed beyond the tool requirement. The item blacklist still takes priority.").define("includedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        EXCLUDED_ITEMS = server.comment("Item IDs, #tags, @mod namespaces, or ID/tag keywords that cannot guard in any mode. Overrides whitelist and shield recognition. Empty hands use a separate toggle.").define("excludedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        SHIELD_ITEMS = server.comment("Extra item IDs, #tags, @mod namespaces, or ID/tag keywords treated as shields. Built-in shields and blocking items are detected automatically. A whitelist or blacklist can still restrict them.").define("shieldItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        COOLDOWN_PREVENTS_GUARD = server.comment("When on, an item with an active vanilla or modded cooldown cannot enter the guard stance. Another eligible hand can still guard.").define("cooldownPreventsGuard", true);
        PARRY_HAND_PRIORITY = GuardSettingRanges.integer(server.comment("Guarding hand: 0 Random, 1 Off-Hand, 2 Main-Hand. Falls back to the other eligible hand."), "parryHandPriority", 0, 0, 2);
        SHIELD_PARRY_PRIORITY = GuardSettingRanges.integer(server.comment("When either eligible hand holds a shield: 0 Random, 1 Shield, 2 Default (player main hand). Falls back to the other eligible hand. Force Crouch Off-Hand overrides this."), "shieldParryPriority", 2, 0, 2);
        FORCE_CROUCH_OFFHAND = server.comment("Crouching selects the eligible offhand before other hand priorities.").define("forceCrouchOffhand", true);
        SHIELD_BLACKLIST = server.comment("Item IDs, #tags, @mod namespaces, or ID/tag keywords excluded from shield recognition. Takes priority over shield tags and whitelist.").define("shieldBlacklist", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        server.push("parried");
        INCOMING_REACTION=GuardSettingRanges.integer(server.comment("When your attack is parried: 0 off, 1 stun, 2 guard break, 3 both. Both applies the guard-break penalty and stun together, using their separate durations."),"reaction",1,0,3);
        PARRIED_ENABLED=server.comment("Enable the selected Stun, Guard Break or Both reaction when your attack is parried. The current attack is interrupted automatically.").define("enabled",true);
        PARRIED_GUARD_BREAK_TICKS=GuardSettingRanges.integer(server.comment("Recharge penalty in Guard Break or Both mode. 20 ticks = one second. Default: 15 ticks."),"guardBreakTicks",15,0,60);
        PARRIED_STUN_TICKS=GuardSettingRanges.integer(server.comment("Duration in Stun or Both mode. Stops movement, attack and guarding; 20 ticks = one second. Default: 15 ticks."),"stunTicks",15,0,60);
        server.pop();
        server.push("shields");
        SHIELD_CONE_ENABLED=server.comment("Enable shield perfect-parry area effects without changing their stored angle.").define("perfectConeEnabled",true);
        SHIELD_PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Shield perfect parry window, in ticks. 0 disables shield perfect parries; cannot exceed total shield parry ticks. Default: 6 ticks."), "perfectTicks", 6, 0, 10);
        SHIELD_PARRY_TICKS = GuardSettingRanges.integer(server.comment("Total shield parry window, including perfect ticks. Default: 9 ticks (6 perfect, then 3 regular). Blocking follows without restarting shield use."), "parryTicks", 9, 1, 10);
        SHIELD_RECHARGE_TICKS = GuardSettingRanges.integer(server.comment("Shield parry recharge after leaving guard; separate from guard break recharge. Regular shield parries halve it and perfect shield parries reset it, like tool parries."), "rechargeTicks", 10, 1, 60);
        ITEM_BLOCK_COUNTS = server.comment("Exact item block limits: mod:item=count, separated by commas. Overrides Shield Expansion counts and ordinary shield/weapon limits. 0 allows unlimited blocks; valid counts are 0–100.").define("itemBlockCounts", "", value -> value instanceof String text && GuardItemBlockCounts.valid(text));
        SHIELD_MAX_BLOCKS = GuardSettingRanges.integer(server.comment("Successful shield blocks before guard breaks; 0 allows unlimited blocks. Disabled shields still obey vanilla cooldown."), "maxBlocks", 5, 0, 10);
        SHIELD_BREAK_TICKS = GuardSettingRanges.integer(server.comment("Recharge after the last allowed shield or weapon block. Shields also receive an item cooldown; weapons remain usable for attacking. At least the normal parry recharge applies. Default 100 ticks = 5 seconds."), "breakTicks", 100, 1, 200);
        SHIELD_CONE_DEGREES = GuardSettingRanges.integer(server.comment("Angle to each side of your view for the shield perfect parry cone. 90 degrees covers the whole front half (180 degrees total). 0 disables cone effects."), "perfectCone", 90, 0, 90);
        SHIELD_CONE_REACH = GuardSettingRanges.integer(server.comment("Maximum eye-to-eye distance in blocks for shield perfect parry cone effects."), "perfectConeReach", 5, 1, 10);
        SHIELD_RETALIATION_PERCENT = GuardSettingRanges.integer(server.comment("Shield perfect parry damage to each target in its cone as a percentage of incoming damage. 0 disables damage; stun and pushback still apply."), "perfectDamage", 10, 0, 30);
        SHIELD_STUN_TICKS = GuardSettingRanges.integer(server.comment("Time in ticks that normal targets hit by a shield perfect parry cannot voluntarily move or attack; 0 disables the stun."), "stunTicks", 30, 0, 100);
        SHIELD_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Shield perfect parry cone pushback strength; 0 disables pushback. Knockback applies before stun."), "perfectPushback", 35, 0, 200);
        SHIELD_PARRY_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Shield regular parry pushback on the attacker; 0 disables pushback."), "regularPushback", 100, 0, 200);
        TOOL_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Regular and perfect weapon parry pushback on the attacker; 0 disables pushback."), "weaponPushback", 80, 0, 200);
        SHIELD_STUN_BOSSES = server.comment("Boss entity IDs or #entity tags explicitly allowed to be stunned. By default, bosses are immune to stun.").define("stunnableBosses", "", value -> value instanceof String rules && GuardItemRules.validEntityRules(rules));
        server.pop();
        server.push("punchy");
        server.pop();
        server.push("mobs");
        MOB_GUARD = server.comment("Allow whitelisted non-boss humanoids with eligible weapons or shields to guard. Empty hands, bows and crossbows do not qualify. Default: false.").define("enabled", false);
        MOB_DIFFICULTY = GuardSettingRanges.integer(server.comment("Scales all chance-based mob guard decisions, including rush and repeat guard. 100% uses normal chances; 50% halves them; 0 prevents new decisions. Gear chance and timing are independent. Default: 100%."), "guardDifficulty", 100, 0, 100);
        MOB_HITS_TO_GUARD = GuardSettingRanges.integer(server.comment("Landed damaging combat hits needed for a pressure-based guarding attempt. Does not limit approach or tactical guarding. Default: 1. The hit that triggers the stance still deals damage; the mob can parry subsequent attacks."), "hitsBeforeGuard", 1, 1, 3);
        MOB_PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Perfect timing at the start of each mob stance. Remaining stance time is regular parry. 0 disables perfect. Default: 15 ticks."), "perfectTicks", 15, 0, 20);
        MOB_COUNTER_TICKS = GuardSettingRanges.integer(server.comment("Maximum rush after a successful mob parry; ends after one melee attempt. 0 disables rushing. Default: 20 ticks."), "counterTicks", 20, 0, 40);
        MOB_APPROACH_CHANCE = GuardSettingRanges.integer(server.comment("Chance to raise guard on the first close approach to a target, within the randomly chosen approach distance. Scaled by mob guard difficulty. 0 disables. Default: 100%."), "approachChance", 100, 0, 100);
        MOB_TACTICAL_CHANCE = GuardSettingRanges.integer(server.comment("Chance per scheduled combat decision (every 2-4 seconds) to begin tactical guarding. Scaled by difficulty and limited by its cooldown. 0 disables. Default: 100%."), "tacticalChance", 100, 0, 100);
        MOB_TACTICAL_SECONDS = GuardSettingRanges.integer(server.comment("Maximum tactical stance duration, randomly 1 second through this value. One continuous parry window; no repeated raises. Default: 2 seconds."), "tacticalMaxSeconds", 2, 1, 10);
        MOB_TACTICAL_COOLDOWN = GuardSettingRanges.integer(server.comment("Wait after the reserved tactical stance end before another tactical attempt. Default: 100 ticks."), "tacticalCooldownTicks", 100, 20, 1200);
        MOB_WHITELIST = server.comment("Only these exact entity IDs can guard or receive generated gear. Comma-separated; bosses remain excluded. Empty means none.").define("whitelist", "minecraft:skeleton,minecraft:zombie,minecraft:piglin,minecraft:zombified_piglin,minecraft:bogged,minecraft:husk", value -> value instanceof String ids && GuardItemRules.validEntityIds(ids));
        MOB_REGULAR_RETALIATION = server.comment("Return damage on regular mob parries using the regular server multiplier. Shield regular parries return no damage. Default: true.").define("regularRetaliation", true);
        MOB_PERFECT_RETALIATION = server.comment("Return damage on perfect mob parries using the perfect server multiplier, or shield retaliation for shields. Default: true.").define("perfectRetaliation", true);
        MOB_MOVEMENT = GuardSettingRanges.integer(server.comment("Movement speed retained while a mob guards. Independent of player guarding speed. 100% causes no guard slowdown; 0% prevents deliberate movement. Default: 100%."), "guardMovement", 100, 0, 100);
        MOB_APPROACH_DISTANCE = GuardSettingRanges.integer(server.comment("Random close-approach trigger distance from 1 block through this maximum. Default: 3 blocks."), "approachDistance", 3, 1, 5);
        MOB_APPROACH_SECONDS = GuardSettingRanges.integer(server.comment("Maximum approach or pressure stance duration, randomly 1 second through this value. One continuous parry window. Default: 2 seconds."), "approachMaxSeconds", 2, 1, 10);
        MOB_RUSH_CHANCE = GuardSettingRanges.integer(server.comment("Chance to rush after a successful parry, scaled by difficulty. Otherwise the mob lowers guard or attempts one follow-up stance. Default: 100%."), "rushChance", 100, 0, 100);
        MOB_GEAR_CHANCE = GuardSettingRanges.integer(server.comment("Chance to add assigned equipment to a fresh whitelisted vanilla humanoid. Existing melee gear and occupied armor slots are preserved. Independent of Difficulty. Default: 50%."), "gearChance", 50, 0, 100);
        MOB_GEAR_WHITELIST = server.comment("Allowed mob spawn equipment. IDs, #tags, @mods and ! exceptions use the shared assignment rules. Only usable equipment enters the gear pool. Default: vanilla armor and swords/pickaxes/shovels/hoes, without netherite or turtle helmets.")
            .define("gearWhitelist", DEFAULT_MOB_GEAR, value -> value instanceof String rules && GuardItemRules.valid(rules));
        MOB_GEAR_BLACKLIST = server.comment("Disallowed mob spawn equipment; overrides the whitelist. Default: empty. Items absent from the whitelist are not generated.")
            .define("gearBlacklist", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        MOB_FLYING_COLOR = GuardSettingRanges.integer(server.comment("Mob Flying Sparks base color. Default: #FFFFFF."), "flyingSparkColor", 0xFFFFFF, 0, 0xFFFFFF);
        MOB_DEBRIS_COLOR = GuardSettingRanges.integer(server.comment("Mob Spark Debris color. Default: #FFFFFF."), "sparkDebrisColor", 0xFFFFFF, 0, 0xFFFFFF);
        MOB_TRACER_START = GuardSettingRanges.integer(server.comment("Mob tracer color at spawn; also colors the beginning of its impact. Default: #FFFFFF."), "tracerStartColor", 0xFFFFFF, 0, 0xFFFFFF);
        MOB_TRACER_MIDDLE = GuardSettingRanges.integer(server.comment("Mob tracer color halfway through fading; also colors the middle of its impact. Default: #FFFFFF."), "tracerMiddleColor", 0xFFFFFF, 0, 0xFFFFFF);
        MOB_TRACER_END = GuardSettingRanges.integer(server.comment("Mob tracer color near disappearance; also colors the end of its impact. Default: #FFFFFF."), "tracerEndColor", 0xFFFFFF, 0, 0xFFFFFF);
        server.pop();

        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CLIENT_REVISION = GuardSettingRanges.integer(client.comment("Mallard Guard config format revision. This changes only when settings or their defaults change."), "configRevision", CONFIG_REVISION, 0, Integer.MAX_VALUE);
        client.push("display");
        HUD = client.comment("Show the parry shield at its configured HUD position.").define("hud", GuardClientSettings.initialFlag(0));
        SHIELD_EFFECTS = client.comment("Animate the guard icon: a ready outline and parry flashes. Tool blocks flash red, shield blocks emit a short white echo, and the darker icon trembles with repeating echoes while held.").define("shieldEffects", GuardClientSettings.initialFlag(1));
        SCREEN_FLASH = client.comment("Brief white screen flash after parries. Meme flash always uses a full white flash. Blocks never flash.").define("screenFlash", GuardClientSettings.initialFlag(2));
        PERFECT_ONLY_FLASH = client.comment("When on, the ordinary white screen flash occurs only on perfect parries. Meme flash still shows a white flash for either parry.").define("perfectOnlyFlash", GuardClientSettings.initialFlag(27));
        MEME_FLASH = client.comment("Show one of four full-screen meme images alongside a local meme clip. Both start with the white parry flash after hitlag; the image holds 0.5 seconds and fades over 0.2 seconds. Off by default; blocks never trigger it.").define("memeFlash", GuardClientSettings.initialFlag(24));
        IMPACT_FRAMES = GuardSettingRanges.integer(client.comment("Brief high-contrast grayscale impact, using the configured hitlag duration.\nFor the best effect, set perfect hitlag to 6 ticks. Off by default."), "impactFrames", GuardClientSettings.initial(34), GuardClientSettings.minimum(34), GuardClientSettings.maximum(34));
        IMPACT_PERFECT_ONLY = client.comment("When enabled, only perfect parries show the grayscale impact image. Regular parries skip it. Enabled by default.").define("impactPerfectOnly", GuardClientSettings.initialFlag(35));
        IMPACT_BRIGHTNESS = GuardSettingRanges.integer(client.comment("Brightness of the captured impact image; adjust inside the live preview."), "impactBrightness", GuardClientSettings.initial(40), GuardClientSettings.minimum(40), GuardClientSettings.maximum(40));
        IMPACT_CONTRAST = GuardSettingRanges.integer(client.comment("Contrast of the captured impact image; adjust inside the live preview."), "impactContrast", GuardClientSettings.initial(41), GuardClientSettings.minimum(41), GuardClientSettings.maximum(41));
        IMPACT_EDGES = GuardSettingRanges.integer(client.comment("Darkening of edges in the captured impact image; adjust inside the live preview."), "impactEdges", GuardClientSettings.initial(42), GuardClientSettings.minimum(42), GuardClientSettings.maximum(42));
        IMPACT_GRAIN = GuardSettingRanges.integer(client.comment("Fine roughness in the captured impact image; adjust inside the live preview."), "impactGrain", GuardClientSettings.initial(43), GuardClientSettings.minimum(43), GuardClientSettings.maximum(43));
        IMPACT_CHROMATIC = GuardSettingRanges.integer(client.comment("RGB channel separation in your still impact image. 0 disables it. Adjust in the preview. Default: 400%."), "impactChromatic", GuardClientSettings.initial(50), GuardClientSettings.minimum(50), GuardClientSettings.maximum(50));
        MOB_IMPACT_BRIGHTNESS = GuardSettingRanges.integer(client.comment("Independent brightness when a mob perfectly parries you; used with Adapt off."), "mobImpactBrightness", GuardClientSettings.initial(51), GuardClientSettings.minimum(51), GuardClientSettings.maximum(51));
        MOB_IMPACT_CONTRAST = GuardSettingRanges.integer(client.comment("Independent mob impact contrast, with Adapt off."), "mobImpactContrast", GuardClientSettings.initial(52), GuardClientSettings.minimum(52), GuardClientSettings.maximum(52));
        MOB_IMPACT_EDGES = GuardSettingRanges.integer(client.comment("Independent mob impact edge strength, with Adapt off."), "mobImpactEdges", GuardClientSettings.initial(53), GuardClientSettings.minimum(53), GuardClientSettings.maximum(53));
        MOB_IMPACT_GRAIN = GuardSettingRanges.integer(client.comment("Independent mob impact grain, with Adapt off."), "mobImpactGrain", GuardClientSettings.initial(54), GuardClientSettings.minimum(54), GuardClientSettings.maximum(54));
        MOB_IMPACT_CHROMATIC = GuardSettingRanges.integer(client.comment("Independent mob impact RGB separation, with Adapt off."), "mobImpactChromatic", GuardClientSettings.initial(55), GuardClientSettings.minimum(55), GuardClientSettings.maximum(55));
        MOB_IMPACT_ADAPT = client.comment("Mob impact images follow your own impact settings. Turn off to customize them independently. Default: true.").define("mobImpactAdapt", GuardClientSettings.initialFlag(56));
        MOB_IMPACT_INVERT = client.comment("Invert the mob impact image, independently of Adapt. Default: true.").define("mobImpactInvert", GuardClientSettings.initialFlag(57));
        CHROMATIC_INTENSITY = GuardSettingRanges.integer(client.comment("Animated RGB separation after parries. Smoothly expands and settles. 0 disables it. Default: 200%."), "chromaticIntensity", GuardClientSettings.initial(58), GuardClientSettings.minimum(58), GuardClientSettings.maximum(58));
        CHROMATIC_TICKS = GuardSettingRanges.integer(client.comment("Duration of animated RGB separation; changing duration also changes animation speed. Default: 6 ticks."), "chromaticTicks", GuardClientSettings.initial(59), GuardClientSettings.minimum(59), GuardClientSettings.maximum(59));
        CHROMATIC_PERFECT_ONLY = client.comment("Only perfect parries trigger animated RGB separation when enabled. Off applies it to both parry types. Default: false.").define("chromaticPerfectOnly", GuardClientSettings.initialFlag(60));
        CHROMATIC_HUD = client.comment("Also separate HUD color channels. Off processes the world view before HUD drawing. Default: true.").define("chromaticHud", GuardClientSettings.initialFlag(61));

        REGULAR_HITLAG_TICKS = GuardSettingRanges.integer(client.comment("Regular parry freeze duration, in ticks.\nUsed when Perfect Only is disabled. 0 disables regular hitlag. Default: 1."), "regularHitlagTicks", GuardClientSettings.initial(62), GuardClientSettings.minimum(62), GuardClientSettings.maximum(62));
        HITLAG_TICKS = GuardSettingRanges.integer(client.comment("Perfect parry local freeze duration in ticks. 20 ticks = one second. Impact Frame occurs inside this freeze; 0 disables hitlag."), "hitlagTicks", GuardClientSettings.initial(3), GuardClientSettings.minimum(3), GuardClientSettings.maximum(3));
        FLASH_STRENGTH = GuardSettingRanges.integer(client.comment("Strength of the brief screen flash, as a percentage. 0 disables it."), "flashStrength", GuardClientSettings.initial(4), GuardClientSettings.minimum(4), GuardClientSettings.maximum(4));
        SHAKE_STRENGTH = GuardSettingRanges.integer(client.comment("Perfect-parry camera shake strength. 0 disables motion. Regular strength has its own setting."), "shakeStrength", GuardClientSettings.initial(5), GuardClientSettings.minimum(5), GuardClientSettings.maximum(5));
        GuardScreenSettings.define(client);
        client.pop();
        client.push("animation");
        PUNCHY_MOVEMENT_DEFAULTS_APPLIED = client.comment("Punchy's walking and sprint arm animations were automatically disabled once. You may re-enable them in Punchy's menu; Mallard Guard will not override later edits.").define("punchyMovementDefaultsApplied", false);
        SHIELD_EXPANSION_NOTICE_SHOWN = client.comment("Whether the automatic Shield Expansion Item Only Mode notice has appeared on this client.").define("shieldExpansionNoticeShown", false);
        SIMPLY_SWORDS_NOTICE_SHOWN = client.comment("Whether the Simply Swords keybind notice has already appeared in chat on this client.").define("simplySwordsNoticeShown", false);
        THIRD_PERSON_ANIMATION = client.comment("Show the simple blocking pose on yourself and other guarding players and humanoid mobs. Off hides this pose for everyone on your client; server guard mechanics do not change.").define("thirdPersonGuardPose", GuardClientSettings.initialFlag(49));
        FIRST_PERSON_ANIMATION = client.comment("Simple first-person weapon pose preference when Punchy guard integration is disabled. The simple pose is automatically enabled when Punchy is absent or its enabled integration is unavailable. Compatible Punchy animations take priority without changing this preference; shields use their own animation.").define("firstPersonGuardPose", GuardClientSettings.initialFlag(11));
        client.pop();
        ModConfigSpec.Builder punchy = new ModConfigSpec.Builder();
        PUNCHY_REVISION=GuardSettingRanges.integer(punchy.comment("Internal config schema revision."), "configRevision",CONFIG_REVISION,0,Integer.MAX_VALUE);
        PUNCHY_COMPAT = punchy.comment("Render eligible guard items through Punchy when its required animation API is available, including compatible newer versions. The simple first-person pose is used when Punchy is absent or integration fails. Saved animation preferences are preserved. Default: true.").define("enabled", true);
        PUNCHY_RELEASE_DELAY = GuardSettingRanges.integer(punchy.comment("Extra hold after client hitlag finishes. Releases without hitlag have no extra delay. Applies to every preset. 0 removes the delay. Default: 50 ms."), "releaseDelayMillis", 50, 0, 1000);
        java.util.List<ModConfigSpec.ConfigValue<String>> poses = new java.util.ArrayList<>();
        for (int i = 0; i < GuardPoseSettings.COUNT; i++) {
            poses.add(punchy.comment("Preset " + (i + 1) + ": enabled, active hand position/rotation, supporting hand position/rotation, weapon position/rotation, raise/lower milliseconds, easing curve, easing strength. Positions use tenths of a model pixel; rotations use degrees. Curves: 0 linear, 1 ease in, 2 ease out, 3 ease in/out.")
                .define("pose" + (i + 1), GuardPoseSettings.encodePose(GuardPoseSettings.defaultPose(i)), value -> value instanceof String text && GuardPoseSettings.parsePose(text) != null));
        }
        PUNCHY_POSES = java.util.List.copyOf(poses);
        PUNCHY_SPEC = punchy.build();
        client.push("audio");
        LOCAL_MASTER_VOLUME = GuardSettingRanges.integer(client.comment("Your own master playback level for Mallard Guard sounds, including the meme clip. 100% is the recorded level before other volume settings; 0% mutes it. Levels above 100% use a louder recording where available."), "masterVolume", GuardClientSettings.initial(12), GuardClientSettings.minimum(12), GuardClientSettings.maximum(12));
        LOCAL_PARRY_VOLUME = GuardSettingRanges.integer(client.comment("Your playback volume for regular parries and regular fall parries and their meme clip; multiplied by the master level."), "parryVolume", GuardClientSettings.initial(13), GuardClientSettings.minimum(13), GuardClientSettings.maximum(13));
        LOCAL_PERFECT_VOLUME = GuardSettingRanges.integer(client.comment("Your playback volume for perfect parries, including shield and enabled perfect fall parries, and their meme clip; multiplied by the master level."), "perfectVolume", GuardClientSettings.initial(14), GuardClientSettings.minimum(14), GuardClientSettings.maximum(14));
        LOCAL_BLOCK_VOLUME = GuardSettingRanges.integer(client.comment("Your playback volume for blocks and regular shield parries; multiplied by the master level."), "blockVolume", GuardClientSettings.initial(15), GuardClientSettings.minimum(15), GuardClientSettings.maximum(15));
        client.pop();
        GuardParticleConfig.define(client);
        GuardSparkTracerConfig.define(client);
        GuardParticleColors.define(client);

        GuardShieldReactions.configure(client);
        client.push("debug");
        DEBUG_CLIENT_FLAGS=GuardSettingRanges.integer(client.comment("Local diagnostic flags: 1 guard, 2 damage, 4 animation, 8 config, 16 mob, 32/64/128 Punchy, 256 collision profiling, 512 screen effects, 1024 prediction, 2048 particle bursts, 4096 assignments. Zero disables all logging and profiling. Collision profiling is opt-in; summaries every 100 game ticks."), "logging",0,0,GuardDiagnostics.ALL);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private GuardConfig() {}

    public static void setCurrentVersion(String version) {
        currentVersion = version;
    }

    public static void prepareConfigFolders() {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("mallard_guard");
        GuardPoseLibrary.prepare();
        try {
            Files.createDirectories(folder.resolve("enforce"));
            clearBackups(folder);
            clearBackups(folder.resolve("enforce"));
            clearLegacyBackups(FMLPaths.CONFIGDIR.get());
            Path instructions = folder.resolve("enforce/README.txt");
            if (Files.notExists(instructions)) Files.writeString(instructions,
                "Mallard Guard client enforcement\n\n"
                + "To use your client settings as the server's enforced values, copy\n"
                + "config/mallard_guard/client.toml here as client.toml. On a dedicated server,\n"
                + "copy the client.toml from your Minecraft installation to this server's\n"
                + "config/mallard_guard/enforce/ folder. Enable enforcement in game as an OP.\n\n"
                + "When this template exists, it supplies the Enforce screen's values.\n"
                + "For Punchy presets and motion, also copy mg_punchy/config.toml into enforce/mg_punchy/config.toml.\n"
                + "Applying changes in that screen also updates the copied files.\n"
                + "The categories marked Don't Enforce still start with these values,\n"
                + "but players can edit them afterward. The original client file is not changed.\n");
            copyIfAbsent(FMLPaths.CONFIGDIR.get().resolve("mallardguard-client.toml"), folder.resolve("client.toml"));
            copyIfAbsent(FMLPaths.CONFIGDIR.get().resolve("mallardguard-server.toml"), folder.resolve("server.toml"));
            Path game = FMLPaths.GAMEDIR.get();
            Path properties = game.resolve("server.properties");
            if (Files.isRegularFile(properties)) {
                java.util.Properties settings = new java.util.Properties();
                try (var input = Files.newInputStream(properties)) { settings.load(input); }
                Path world = game.resolve(settings.getProperty("level-name", "world")).normalize();
                if (world.startsWith(game)) migrateWorldConfig(world);
            }
            Path saves = game.resolve("saves");
            if (Files.isDirectory(saves)) try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saves)) {
                for (Path world : worlds) migrateWorldConfig(world);
            }
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not prepare config folders: " + error.getMessage());
        }
    }

    private static void migrateWorldConfig(Path world) throws IOException {
        Path folder = world.resolve("serverconfig/mallard_guard");
        clearBackups(folder);
        clearLegacyBackups(world.resolve("serverconfig"));
        Path source = world.resolve("serverconfig/mallardguard-server.toml");
        if (Files.isRegularFile(source) && !Files.exists(folder.resolve("server.toml"))) {
            Files.createDirectories(folder);
            copyIfAbsent(source, folder.resolve("server.toml"));
        }
    }

    public static void clearConfigBackups() {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("mallard_guard");
        clearBackups(folder);
        clearBackups(folder.resolve("enforce")); clearBackups(folder.resolve("mg_punchy"));
        if (serverConfigFolder != null) clearBackups(serverConfigFolder);
    }

    public static void clearBackups(Path folder) {
        deleteConfigBackups(folder, "(?:client|server|config)(?:\\.toml)?(?:[-.]\\d+)?(?:\\.toml)?\\.bak");
    }

    private static void clearLegacyBackups(Path folder) {
        deleteConfigBackups(folder, "mallardguard-(?:client|server|config)(?:\\.toml)?(?:[-.]\\d+)?(?:\\.toml)?\\.bak");
    }

    private static void deleteConfigBackups(Path folder, String pattern) {
        if (!Files.isDirectory(folder, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (file.getFileName().toString().matches(pattern)
                    && Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) Files.deleteIfExists(file);
            }
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not remove config backups: " + error.getMessage());
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) GuardPlayerPalettes.refreshPalettePolicy();
        if (event.getConfig().getSpec() == CLIENT_SPEC || event.getConfig().getSpec() == SERVER_SPEC || event.getConfig().getSpec() == PUNCHY_SPEC)
            clearBackups(event.getConfig().getFullPath().getParent());
    }

    private static void copyIfAbsent(Path oldFile, Path newFile) throws IOException {
        if (Files.isRegularFile(oldFile) && !Files.exists(newFile)) Files.copy(oldFile, newFile);
    }

    /** Capture existing files before NeoForge validates them, without making backup files. */
    public static void snapshotExistingConfigs() {
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/client.toml"));
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/mg_punchy/config.toml"));
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/server.toml"));
        Path game = FMLPaths.GAMEDIR.get();
        Path dedicatedProperties = game.resolve("server.properties");
        if (Files.isRegularFile(dedicatedProperties)) {
            try {
                java.util.Properties properties = new java.util.Properties();
                try (var input = Files.newInputStream(dedicatedProperties)) { properties.load(input); }
                String level = properties.getProperty("level-name", "world");
                Path world = game.resolve(level).normalize();
                if (world.startsWith(game)) remember(world.resolve("serverconfig/mallard_guard/server.toml"));
            } catch (IOException error) {
                System.err.println("Mallard Guard: could not inspect server.properties: " + error.getMessage());
            }
        }
        Path saves = FMLPaths.GAMEDIR.get().resolve("saves");
        if (!Files.isDirectory(saves)) return;
        try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saves)) {
            for (Path world : worlds) remember(world.resolve("serverconfig/mallard_guard/server.toml"));
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not inspect saved world configs: " + error.getMessage());
        }
    }

    private static void remember(Path path) {
        try {
            if(!Files.isRegularFile(path))return;
            // Preserve unopened worlds on disk, rather than retaining every config's bytes in heap.
            Path original=Files.createTempFile("mallard-guard-config-", ".toml");
            original.toFile().deleteOnExit();
            try{Files.copy(path,original,StandardCopyOption.REPLACE_EXISTING);}
            catch(IOException error){Files.deleteIfExists(original);throw error;}
            Path previous=ORIGINAL_FILES.put(path.toAbsolutePath().normalize(),original);
            if(previous!=null)Files.deleteIfExists(previous);
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not preserve config: " + error.getMessage());
        }
    }
    private static byte[] readOriginal(Path path) {
        Path original=ORIGINAL_FILES.get(path.toAbsolutePath().normalize());
        if(original==null)return null;
        try{return Files.readAllBytes(original);}
        catch(IOException error){System.err.println("Mallard Guard: could not read original config: "+error.getMessage());return null;}
    }
    private static void forgetOriginal(Path path) {
        Path original=ORIGINAL_FILES.remove(path.toAbsolutePath().normalize());
        if(original!=null)try{Files.deleteIfExists(original);}catch(IOException error){System.err.println("Mallard Guard: could not remove config snapshot: "+error.getMessage());}
    }

    public static String version() { return currentVersion == null ? "unknown" : currentVersion; }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == PUNCHY_SPEC) GuardPoseLibrary.loaded();
        ModConfigSpec.IntValue marker;
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            marker = SERVER_REVISION;
            serverConfigFolder = event.getConfig().getFullPath().getParent();
        } else if (event.getConfig().getSpec() == PUNCHY_SPEC) {
            marker = PUNCHY_REVISION;
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            marker = CLIENT_REVISION;
        } else return;
        Path path = event.getConfig().getFullPath().toAbsolutePath().normalize();
        clearBackups(path.getParent());
        byte[] original = readOriginal(path);
        // Fresh installations receive the current revision without a prompt.
        if (original == null) return;
        int oldRevision = GuardConfigMigration.revisionIn(original);
        if (oldRevision == CONFIG_REVISION) {
            forgetOriginal(path);
            return;
        }
        if(event.getConfig().getSpec()==SERVER_SPEC && oldRevision<96){
            var previous=new com.electronwill.nightconfig.toml.TomlParser().parse(new String(original,java.nio.charset.StandardCharsets.UTF_8));
            Object oldMode=previous.get("whenParried.reaction");
            if(previous.get("parried.reaction")==null&&oldMode instanceof Number n){
                int mode=Math.clamp(n.intValue(),0,2);PARRIED_ENABLED.set(mode!=0);INCOMING_REACTION.set(Math.max(1,mode));
                Object regular=previous.get("whenParried.regularStunTicks"),perfect=previous.get("whenParried.perfectStunTicks");
                int ticks=Math.clamp(Math.max(regular instanceof Number r?r.intValue():0,perfect instanceof Number p?p.intValue():0),0,60);
                if(mode!=0)(mode==1?PARRIED_STUN_TICKS:PARRIED_GUARD_BREAK_TICKS).set(ticks);
            }
        }
        if(event.getConfig().getSpec()==SERVER_SPEC && oldRevision<83)GuardConfigMigration.migrateMobSettings(original);
        if(event.getConfig().getSpec()==CLIENT_SPEC && oldRevision<88){
            var previous=new com.electronwill.nightconfig.toml.TomlParser().parse(new String(original,java.nio.charset.StandardCharsets.UTF_8));
            if(previous.get("display.regularHitlagFrames")==null)REGULAR_HITLAG_TICKS.set(GuardClientSettings.legacyRegularHitlag(previous));
        }
        if(event.getConfig().getSpec()==CLIENT_SPEC && oldRevision<96){
            GuardParticleColors.migrateClient(original);
            GuardScreenSettings.migrateClient(original);
        }
        if(event.getConfig().getSpec()==CLIENT_SPEC && oldRevision<70){if(oldRevision<69)GuardSparkTracerConfig.migrateClient(original);GuardParticleConfig.migrateClientPerfect(original);}
        Set<String> currentPaths = new HashSet<>();
        collectPaths(((ModConfigSpec) event.getConfig().getSpec()).getValues(), "", currentPaths);
        Set<String> oldPaths = GuardConfigMigration.pathsIn(original);
        oldPaths.remove("configVersion"); // Version marker used by 1.22 and older.
        oldPaths.remove("configRevision");
        currentPaths.remove("configRevision");
        List<String> changes = new ArrayList<>();
        changes.addAll(GuardConfigMigration.defaultChanges(oldRevision, currentPaths));
        for (String added : currentPaths.stream().filter(key -> !oldPaths.contains(key)).sorted().toList())
            changes.add("Added: " + GuardConfigMigration.label(added));
        for (String removed : oldPaths.stream().filter(key -> !currentPaths.contains(key)).sorted().toList())
            changes.add("Removed: " + GuardConfigMigration.label(removed));
        if (changes.isEmpty()) {
            // A revision change without actual setting changes needs no prompt.
            marker.set(CONFIG_REVISION);
            ((ModConfigSpec) event.getConfig().getSpec()).save();
            currentPaths.add("configRevision");
            cleanFile(event.getConfig(), currentPaths);
            forgetOriginal(path);
            return;
        }
        synchronized (PENDING_UPDATES) {
            PENDING_UPDATES.removeIf(update -> update.config().getFullPath().toAbsolutePath().normalize().equals(path));
            PENDING_UPDATES.add(new PendingUpdate(event.getConfig(), List.copyOf(changes)));
        }
    }

    public static void serverClosed(){
        synchronized(PENDING_UPDATES){PENDING_UPDATES.removeIf(update->update.config().getSpec()==SERVER_SPEC);}
        serverConfigFolder=null;
    }

    public static int pendingUpdates() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.size();
        }
    }

    public static boolean pendingServerUpdate() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.stream().anyMatch(update -> update.config().getSpec() == SERVER_SPEC);
        }
    }

    public static List<String> pendingChanges() {
        synchronized (PENDING_UPDATES) {
            List<String> lines = new ArrayList<>();
            for (PendingUpdate update : PENDING_UPDATES) {
                lines.add(update.config().getSpec() == PUNCHY_SPEC ? "Client / Punchy settings" : update.config().getSpec() == CLIENT_SPEC ? "Client settings" : "World/server settings");
                if (update.changes().isEmpty()) lines.add("Config format changed; settings remain available.");
                else lines.addAll(update.changes());
            }
            return List.copyOf(lines);
        }
    }

    public static List<String> pendingServerChanges() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.stream().filter(update -> update.config().getSpec() == SERVER_SPEC)
                .flatMap(update -> update.changes().stream()).toList();
        }
    }

    /** Return false on a write failure so the user can retry without losing the prompt. */
    public static boolean respondToUpdate(boolean keepOldValues) {
        return respondToUpdate(null, keepOldValues);
    }

    public static boolean respondToServerUpdate(boolean keepOldValues) {
        return respondToUpdate(SERVER_SPEC, keepOldValues);
    }

    private static boolean respondToUpdate(ModConfigSpec onlySpec, boolean keepOldValues) {
        synchronized (PENDING_UPDATES) {
            for (PendingUpdate update : List.copyOf(PENDING_UPDATES)) {
                ModConfig entry = update.config();
                if (onlySpec != null && entry.getSpec() != onlySpec) continue;
                ModConfigSpec spec = (ModConfigSpec) entry.getSpec();
                Set<String> paths = new HashSet<>();
                collectPaths(spec.getValues(), "", paths);
                try {
                    if (!keepOldValues) {
                        resetValues(spec.getValues());
                        if(spec==CLIENT_SPEC||spec==PUNCHY_SPEC){if(spec==CLIENT_SPEC){resetValues(PUNCHY_SPEC.getValues());PUNCHY_SPEC.save();}var presets=new java.util.ArrayList<>(GuardPoseLibrary.snapshot());if(presets.size()>=GuardPoseSettings.COUNT){for(int i=0;i<GuardPoseSettings.COUNT;i++)presets.set(i,GuardPoseLibrary.preset(i));GuardPoseLibrary.save(presets);}}
                    }
                    if (keepOldValues && spec == SERVER_SPEC) {
                        byte[] previous=readOriginal(entry.getFullPath());
                        if(previous!=null&&GuardConfigMigration.revisionIn(previous)<28){
                            String text=new String(previous,java.nio.charset.StandardCharsets.UTF_8);
                            var dual=java.util.regex.Pattern.compile("(?m)^\\s*randomDualTools\\s*=\\s*(true|false)\\s*$").matcher(text);
                            if(dual.find())PARRY_HAND_PRIORITY.set(Boolean.parseBoolean(dual.group(1))?0:2);
                            var shield=java.util.regex.Pattern.compile("(?m)^\\s*randomShieldWeapon\\s*=\\s*(true|false)\\s*$").matcher(text);
                            if(shield.find())SHIELD_PARRY_PRIORITY.set(Boolean.parseBoolean(shield.group(1))?0:1);
                        }
                        if(previous!=null&&GuardConfigMigration.revisionIn(previous)<35&&(CLIENT_EXEMPT_MASK.get()&2)!=0)CLIENT_EXEMPT_MASK.set(CLIENT_EXEMPT_MASK.get()|24);
                        CLIENT_EXEMPT_MASK.set(CLIENT_EXEMPT_MASK.get() & GuardClientPreset.ALL_CATEGORIES);
                        int[] preset = GuardClientPreset.parse(CLIENT_PRESET.get());
                        if (preset != null) CLIENT_PRESET.set(GuardClientPreset.encode(preset));
                    }
                    (spec == CLIENT_SPEC ? CLIENT_REVISION : spec == PUNCHY_SPEC ? PUNCHY_REVISION : SERVER_REVISION).set(CONFIG_REVISION);
                    spec.save();
                    cleanFile(entry, paths);
                    clearBackups(entry.getFullPath().getParent());
                    forgetOriginal(entry.getFullPath());
                    PENDING_UPDATES.remove(update);
                } catch (RuntimeException error) {
                    System.err.println("Mallard Guard: could not update config " + entry.getFullPath() + ": " + error.getMessage());
                    return false;
                }
            }
            return true;
        }
    }

    private static void cleanFile(ModConfig entry, Set<String> paths) {
        try (CommentedFileConfig file = CommentedFileConfig.builder(entry.getFullPath()).sync().build()) {
            file.load();
            GuardConfigMigration.removeStale(file, "", paths);
            file.save();
        }
    }

    private static void resetValues(UnmodifiableConfig entries) {
        for (var entry : entries.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig group) resetValues(group);
            else if (value instanceof ModConfigSpec.ConfigValue<?> setting) resetValue(setting);
        }
    }

    private static <T> void resetValue(ModConfigSpec.ConfigValue<T> setting) {
        setting.set(setting.getDefault());
    }

    private static void collectPaths(UnmodifiableConfig entries, String prefix, Set<String> paths) {
        for (var entry : entries.entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue() instanceof UnmodifiableConfig group) collectPaths(group, path + ".", paths);
            else if (entry.getValue() instanceof ModConfigSpec.ConfigValue<?>) paths.add(path);
        }
    }

    private static final class FileBackup {
        private final Map<Path,byte[]> files=new java.util.LinkedHashMap<>();
        void preserve(Path path) {
            Path normalized=path.toAbsolutePath().normalize();
            if(files.containsKey(normalized))return;
            try{files.put(normalized,Files.isRegularFile(normalized)?Files.readAllBytes(normalized):null);}
            catch(IOException error){throw new IllegalStateException("Could not preserve "+path.getFileName(),error);}
        }
        void restore(java.util.function.Predicate<Path> keep) {
            for(var entry:files.entrySet()){
                if(!keep.test(entry.getKey()))continue;
                try{
                    Path path=entry.getKey();byte[] bytes=entry.getValue();
                    if(bytes==null){Files.deleteIfExists(path);continue;}
                    Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),"mg-restore-", ".tmp");
                    try{Files.write(temporary,bytes);try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException error){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}}
                    finally{Files.deleteIfExists(temporary);}
                }catch(IOException error){System.err.println("Mallard Guard: could not restore config file: "+error.getMessage());}
            }
        }
        void clear(){files.clear();}
    }
    private static final ThreadLocal<ServerEdit> SERVER_EDIT=new ThreadLocal<>();

    /** One config request: independent validation, one disk save, notifications after success. */
    public static final class ServerEdit implements AutoCloseable {
        private final Map<ModConfigSpec.ConfigValue<?>,Object> original=captureServerValues();
        private final FileBackup files=new FileBackup();
        private final List<Runnable> notifications=new ArrayList<>();
        private boolean dirty,committed;
        public ServerEdit() {
            if(SERVER_EDIT.get()!=null)throw new IllegalStateException("Nested server config edit.");
            SERVER_EDIT.set(this);
            try{if(serverConfigFolder!=null)preserve(serverConfigFolder.resolve("server.toml"));}
            catch(RuntimeException error){SERVER_EDIT.remove();throw error;}
        }
        public boolean attempt(java.util.function.BooleanSupplier action) {
            Map<ModConfigSpec.ConfigValue<?>,Object> checkpoint=captureServerValues();
            int noticeCount=notifications.size();boolean wasDirty=dirty;
            try{if(action.getAsBoolean())return true;}
            catch(RuntimeException error){System.err.println("Mallard Guard: config section failed: "+error.getMessage());}
            restoreServerValues(checkpoint);dirty=wasDirty;
            notifications.subList(noticeCount,notifications.size()).clear();
            // Only enforcement writes auxiliary files, and it is the final section in a request.
            restoreFiles(false);
            GuardPoseLibrary.clearEnforcedCache();
            return false;
        }
        private void preserve(Path path) { files.preserve(path); }
        private void restoreFiles(boolean includeServer) {
            Path server=serverConfigFolder==null?null:serverConfigFolder.resolve("server.toml").toAbsolutePath().normalize();
            files.restore(path->includeServer||!path.equals(server));
        }
        public boolean commit() {
            try{if(dirty)SERVER_SPEC.save();}
            catch(RuntimeException error){System.err.println("Mallard Guard: config save failed; restoring previous values: "+error.getMessage());return false;}
            committed=true;
            // Cleanup/notification failures must not turn a successful disk commit into a false rejection.
            try{if(dirty)clearConfigBackups();}catch(RuntimeException error){System.err.println("Mallard Guard: backup cleanup failed: "+error.getMessage());}
            for(Runnable notification:notifications)try{notification.run();}catch(RuntimeException error){System.err.println("Mallard Guard: config notification failed: "+error.getMessage());}
            return true;
        }
        @Override public void close() {
            try{if(!committed){restoreServerValues(original);restoreFiles(true);GuardPoseLibrary.clearEnforcedCache();}}
            finally{SERVER_EDIT.remove();files.clear();notifications.clear();}
        }
    }
    private static Map<ModConfigSpec.ConfigValue<?>,Object> captureServerValues() {
        Map<ModConfigSpec.ConfigValue<?>,Object> values=new java.util.IdentityHashMap<>();
        collectServerValues(SERVER_SPEC.getValues(),values);return values;
    }
    private static void collectServerValues(UnmodifiableConfig entries,Map<ModConfigSpec.ConfigValue<?>,Object> values) {
        for(var entry:entries.entrySet()){
            Object value=entry.getValue();
            if(value instanceof UnmodifiableConfig group)collectServerValues(group,values);
            else if(value instanceof ModConfigSpec.ConfigValue<?> setting)values.put(setting,setting.get());
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void restoreServerValues(Map<ModConfigSpec.ConfigValue<?>,Object> values) {
        values.forEach((setting,value)->((ModConfigSpec.ConfigValue)setting).set(value));
    }
    public static void persistServer() {
        ServerEdit edit=SERVER_EDIT.get();
        if(edit!=null){edit.dirty=true;return;}
        SERVER_SPEC.save();clearConfigBackups();
    }
    public static void afterServerSave(Runnable notification) {
        ServerEdit edit=SERVER_EDIT.get();if(edit==null)notification.run();else edit.notifications.add(notification);
    }
    public static void preserveServerFile(Path path) {
        ServerEdit edit=SERVER_EDIT.get();if(edit!=null)edit.preserve(path);
    }

    private static final ThreadLocal<ClientEdit> CLIENT_EDIT=new ThreadLocal<>();
    /** Restore client values and only the files actually touched if Apply cannot finish. */
    public static final class ClientEdit implements AutoCloseable {
        private final Map<ModConfigSpec.ConfigValue<?>,Object> original=new java.util.IdentityHashMap<>();
        private final FileBackup files=new FileBackup();
        private boolean committed;
        public ClientEdit() {
            if(CLIENT_EDIT.get()!=null)throw new IllegalStateException("Nested client config edit.");
            collectServerValues(CLIENT_SPEC.getValues(),original);collectServerValues(PUNCHY_SPEC.getValues(),original);
            Path folder=FMLPaths.CONFIGDIR.get().resolve("mallard_guard");
            files.preserve(folder.resolve("client.toml"));files.preserve(folder.resolve("mg_punchy/config.toml"));
            CLIENT_EDIT.set(this);
        }
        public void commit(){committed=true;}
        @Override public void close(){
            try{if(!committed){restoreServerValues(original);files.restore(path->true);GuardPoseLibrary.loaded();}}
            finally{CLIENT_EDIT.remove();files.clear();}
        }
    }
    public static void preserveClientFile(Path path){ClientEdit edit=CLIENT_EDIT.get();if(edit!=null)edit.files.preserve(path);}

    public static void apply(GuardPackets.Settings data) {
        PARRY.set(data.parry());
        BLOCK.set(data.block());
        PERFECT_TICKS.set(data.perfect());
        PARRY_TICKS.set(data.window());
        RECHARGE_TICKS.set(data.recharge());
        FACING_ANGLE.set(data.angle());
        BLOCK_REDUCTION.set(data.reductionPercent() / 100.0D);
        FOLLOW_UP_TICKS.set(data.followUp());
        PARRY_RETALIATION.set(data.parryReturnPercent() / 100.0D);
        PERFECT_RETALIATION.set(data.perfectReturnPercent() / 100.0D);
        PARRY_WEAR.set(data.parryWear());
        PERFECT_WEAR.set(data.perfectWear());
        BLOCK_WEAR.set(data.blockWear());
        ALLOW_ANY_ITEM.set(data.allowAnyItem());
        CONSUMABLE_PRIORITY.set(data.consumablePriority());
        PARRY_HEALING.set(data.parryHealing()); PARRY_HEALING_HEARTS.set(data.parryHealingHearts());
        HIT_SOUNDS.set(data.hitSounds());
        HIT_PARTICLES.set(data.hitParticles());
        MASTER_VOLUME.set(data.masterVolume());
        PERFECT_VOLUME.set(data.perfectVolume());
        PARRY_VOLUME.set(data.parryVolume());
        BLOCK_VOLUME.set(data.blockVolume());
        FALL_PARRY.set(data.fallParry());
        FALL_BREAK_BLOCKS.set(data.fallBreakBlocks());
        FALL_BLAST_STRENGTH.set(data.fallBlastStrength());
        FALL_LAUNCH_POWER.set(data.fallLaunchPower());
        PARRY_EXPLOSIONS.set(data.parryExplosions());
        PERFECT_EXPLOSIONS_ONLY.set(data.perfectExplosionsOnly());
        BLOCK_EXPLOSIONS.set(data.blockExplosions());
        PARRY_PROJECTILES.set(data.parryProjectiles());
        BLOCK_PROJECTILES.set(data.blockProjectiles());
        DEFENDER_KNOCKBACK.set(data.defenderKnockback());
        KNOCKBACK_STRENGTH.set(data.knockbackStrength());
        GUARD_MOVEMENT_PERCENT.set(data.guardMovementPercent());
        BLOCK_DEFLECT_CHANCE.set(data.blockDeflectChance());
        ALLOW_USABLE_ITEMS.set(data.allowUsableItems());
        INCLUDED_ITEMS.set(data.includedItems());
        EXCLUDED_ITEMS.set(data.excludedItems());
        SHIELD_ITEMS.set(data.shieldItems());
        persistServer();
    }

    private static <T> T read(ModConfigSpec.ConfigValue<T> value, boolean defaults) {
        return defaults ? value.getDefault() : value.get();
    }

    public static GuardPackets.Settings snapshot(boolean operator) { return settingsSnapshot(operator, false); }
    public static GuardPackets.Settings defaultSnapshot(boolean operator) { return settingsSnapshot(operator, true); }
    private static GuardPackets.Settings settingsSnapshot(boolean operator, boolean defaults) {
        return new GuardPackets.Settings(read(PARRY, defaults), read(BLOCK, defaults), false, false, false, read(PERFECT_TICKS, defaults), read(PARRY_TICKS, defaults), read(RECHARGE_TICKS, defaults), read(FACING_ANGLE, defaults), (int) Math.round(read(BLOCK_REDUCTION, defaults) * 100.0D), read(FOLLOW_UP_TICKS, defaults), (int) Math.round(read(PARRY_RETALIATION, defaults) * 100), (int) Math.round(read(PERFECT_RETALIATION, defaults) * 100), read(PARRY_WEAR, defaults), read(PERFECT_WEAR, defaults), read(BLOCK_WEAR, defaults), read(HIT_SOUNDS, defaults), read(HIT_PARTICLES, defaults), read(MASTER_VOLUME, defaults), read(PERFECT_VOLUME, defaults), read(PARRY_VOLUME, defaults), read(BLOCK_VOLUME, defaults), read(FALL_PARRY, defaults), true, true, read(FALL_BREAK_BLOCKS, defaults), read(FALL_BLAST_STRENGTH, defaults), read(FALL_LAUNCH_POWER, defaults), read(PARRY_EXPLOSIONS, defaults), read(PERFECT_EXPLOSIONS_ONLY, defaults), read(BLOCK_EXPLOSIONS, defaults), read(PARRY_PROJECTILES, defaults), read(BLOCK_PROJECTILES, defaults), read(DEFENDER_KNOCKBACK, defaults), read(KNOCKBACK_STRENGTH, defaults), read(GUARD_MOVEMENT_PERCENT, defaults), read(BLOCK_DEFLECT_CHANCE, defaults), read(ALLOW_ANY_ITEM, defaults), read(ALLOW_USABLE_ITEMS, defaults), read(INCLUDED_ITEMS, defaults), read(EXCLUDED_ITEMS, defaults), read(SHIELD_ITEMS, defaults), read(CONSUMABLE_PRIORITY, defaults), operator, read(PARRY_HEALING, defaults), read(PARRY_HEALING_HEARTS, defaults));
    }

    public static GuardPackets.MobSettings mobSnapshot(boolean defaults) {
        return new GuardPackets.MobSettings(
            defaults ? MOB_GUARD.getDefault() : MOB_GUARD.get(),
            defaults ? MOB_DIFFICULTY.getDefault() : MOB_DIFFICULTY.get(),
            defaults ? MOB_WHITELIST.getDefault() : MOB_WHITELIST.get(),
            defaults ? MOB_GEAR_CHANCE.getDefault() : MOB_GEAR_CHANCE.get(),
            defaults ? MOB_GEAR_WHITELIST.getDefault() : MOB_GEAR_WHITELIST.get(),
            defaults ? MOB_GEAR_BLACKLIST.getDefault() : MOB_GEAR_BLACKLIST.get(),
            defaults ? MOB_FLYING_COLOR.getDefault() : MOB_FLYING_COLOR.get(),
            defaults ? MOB_DEBRIS_COLOR.getDefault() : MOB_DEBRIS_COLOR.get(),
            defaults ? MOB_TRACER_START.getDefault() : MOB_TRACER_START.get(),
            defaults ? MOB_TRACER_MIDDLE.getDefault() : MOB_TRACER_MIDDLE.get(),
            defaults ? MOB_TRACER_END.getDefault() : MOB_TRACER_END.get());
    }

    public static GuardPackets.ShieldSettings shieldSnapshot() { return shieldSnapshotValues(false); }
    public static GuardPackets.ShieldSettings defaultShieldSnapshot() { return shieldSnapshotValues(true); }
    private static GuardPackets.ShieldSettings shieldSnapshotValues(boolean defaults) {
        return new GuardPackets.ShieldSettings(read(SHIELD_PERFECT_TICKS, defaults), read(SHIELD_PARRY_TICKS, defaults),
            read(SHIELD_RECHARGE_TICKS, defaults), read(SHIELD_MAX_BLOCKS, defaults), read(TOOL_MAX_BLOCKS, defaults), read(SHIELD_BREAK_TICKS, defaults), read(SHIELD_CONE_DEGREES, defaults), read(SHIELD_CONE_REACH, defaults),
            read(SHIELD_RETALIATION_PERCENT, defaults), read(SHIELD_STUN_TICKS, defaults), read(SHIELD_PUSHBACK_PERCENT, defaults),
            read(SHIELD_PARRY_PUSHBACK_PERCENT, defaults), read(TOOL_PUSHBACK_PERCENT, defaults), read(SHIELD_STUN_BOSSES, defaults), read(CONSUMABLE_PRIORITY, defaults), read(COOLDOWN_PREVENTS_GUARD, defaults), read(ALLOW_EMPTY_HAND, defaults), read(RETALIATION_CAP, defaults), read(SHIELD_PARRY_PRIORITY, defaults), read(PARRY_HAND_PRIORITY, defaults), read(FORCE_CROUCH_OFFHAND, defaults), read(SHIELD_BLACKLIST, defaults), read(ITEM_BLOCK_COUNTS, defaults), GuardShieldExpansionCompat.active(),read(PARRIED_ENABLED,defaults),read(PARRIED_GUARD_BREAK_TICKS,defaults),read(PARRIED_STUN_TICKS,defaults),read(SHIELD_CONE_ENABLED,defaults),read(INCOMING_REACTION,defaults),read(RETALIATION_ENABLED,defaults),read(PERFECT_PROJECTILES_ONLY,defaults),read(PERFECT_FALL_ONLY,defaults),read(PERFECT_KNOCKBACK_MULTIPLIER,defaults));
    }

    public static void applyShield(GuardPackets.ShieldSettings data) {
        RETALIATION_ENABLED.set(data.retaliationEnabled());PERFECT_PROJECTILES_ONLY.set(data.perfectProjectilesOnly());PERFECT_FALL_ONLY.set(data.perfectFallOnly());PERFECT_KNOCKBACK_MULTIPLIER.set(data.perfectKnockbackMultiplier());
        INCOMING_REACTION.set(data.incomingReaction());PARRIED_ENABLED.set(data.parriedEnabled());PARRIED_GUARD_BREAK_TICKS.set(data.parriedGuardBreakTicks());PARRIED_STUN_TICKS.set(data.parriedStunTicks());SHIELD_CONE_ENABLED.set(data.coneEnabled());
        PARRY_HAND_PRIORITY.set(data.parryHandPriority()); FORCE_CROUCH_OFFHAND.set(data.forceCrouchOffhand()); SHIELD_BLACKLIST.set(data.shieldBlacklist()); ITEM_BLOCK_COUNTS.set(data.itemBlockCounts());
        SHIELD_PERFECT_TICKS.set(data.perfect()); SHIELD_PARRY_TICKS.set(data.window());
        SHIELD_RECHARGE_TICKS.set(data.rechargeTicks()); SHIELD_MAX_BLOCKS.set(data.maxBlocks()); TOOL_MAX_BLOCKS.set(data.toolMaxBlocks()); SHIELD_BREAK_TICKS.set(data.breakTicks());
        SHIELD_CONE_DEGREES.set(data.cone()); SHIELD_CONE_REACH.set(data.reach()); SHIELD_RETALIATION_PERCENT.set(data.retaliation());
        SHIELD_STUN_TICKS.set(data.stunTicks()); SHIELD_PUSHBACK_PERCENT.set(data.perfectPushback());
        SHIELD_PARRY_PUSHBACK_PERCENT.set(data.regularPushback()); TOOL_PUSHBACK_PERCENT.set(data.weaponPushback());
        SHIELD_STUN_BOSSES.set(data.stunnableBosses()); CONSUMABLE_PRIORITY.set(data.consumablePriority()); COOLDOWN_PREVENTS_GUARD.set(data.cooldownPreventsGuard()); ALLOW_EMPTY_HAND.set(data.allowEmptyHand()); RETALIATION_CAP.set(data.retaliationCap());
        SHIELD_PARRY_PRIORITY.set(data.shieldParryPriority());
        persistServer();
    }
}
