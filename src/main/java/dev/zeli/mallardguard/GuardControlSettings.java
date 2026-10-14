package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Explicit setting identities and numeric conversions shared by UI controls. */
public final class GuardControlSettings {
    private GuardControlSettings() {}
    public enum Control {
        INCOMING_REACTION,PARRIED_ENABLED,PARRIED_GUARD_BREAK_TICKS,PARRIED_STUN_TICKS,SHIELD_CONE_ENABLED,PUNCHY_RELEASE_DELAY, PERFECT_TICKS, PARRY_TICKS, RECHARGE_TICKS, FACING_ANGLE, PARRY_HEALING, PARRY_HEALING_HEARTS, FOLLOW_UP_TICKS, PARRY_RETALIATION, PERFECT_RETALIATION, RETALIATION_CAP, KNOCKBACK_STRENGTH, SHIELD_PARRY_PUSHBACK_PERCENT, TOOL_PUSHBACK_PERCENT, PARRY_PROJECTILES, PARRY_EXPLOSIONS, PERFECT_EXPLOSIONS_ONLY, MOB_WHITELIST, MOB_GUARD, MOB_DIFFICULTY, MOB_FLYING_COLOR, MOB_DEBRIS_COLOR, MOB_TRACER_START, MOB_TRACER_MIDDLE, MOB_TRACER_END, MOB_GEAR_WHITELIST, MOB_GEAR_BLACKLIST, MOB_GEAR_CHANCE, SHIELD_PERFECT_TICKS, SHIELD_PARRY_TICKS, SHIELD_RECHARGE_TICKS, SHIELD_MAX_BLOCKS, SHIELD_CONE_DEGREES, SHIELD_CONE_REACH, SHIELD_RETALIATION_PERCENT, SHIELD_STUN_TICKS, SHIELD_PUSHBACK_PERCENT, FALL_BLAST_STRENGTH, FALL_LAUNCH_POWER, FALL_BREAK_BLOCKS, FALL_PARRY, PERFECT_FALL_ONLY, PERFECT_PROJECTILES_ONLY, RETALIATION_ENABLED, PERFECT_KNOCKBACK_MULTIPLIER, BLOCK, BLOCK_REDUCTION, TOOL_MAX_BLOCKS, SHIELD_BREAK_TICKS, GUARD_MOVEMENT_PERCENT, BLOCK_DEFLECT_CHANCE, BLOCK_PROJECTILES, BLOCK_EXPLOSIONS, HIT_PARTICLES, HIT_SOUNDS, ALLOW_EMPTY_HAND, COOLDOWN_PREVENTS_GUARD, CONSUMABLE_PRIORITY, FORCE_CROUCH_OFFHAND, PARRY_WEAR, PERFECT_WEAR, BLOCK_WEAR, DEBRIS_BASE_INTENSITY, DEBRIS_PERFECT_INTENSITY, DEBRIS_DAMAGE_SCALING, FLYING_BASE_INTENSITY, FLYING_PERFECT_INTENSITY, FLYING_DAMAGE_SCALING, SHIELD_EFFECTS, FLASH_STRENGTH, REGULAR_HITLAG_TICKS, PERFECT_ONLY_FLASH, MEME_FLASH, SHAKE_STRENGTH, HITLAG_TICKS, IMPACT_FRAMES, IMPACT_PERFECT_ONLY, THIRD_PERSON_ANIMATION, FIRST_PERSON_ANIMATION, MASTER_VOLUME, PERFECT_VOLUME, PARRY_VOLUME, BLOCK_VOLUME, CHROMATIC_INTENSITY, CHROMATIC_TICKS, CHROMATIC_PERFECT_ONLY, CHROMATIC_HUD, ALLOW_ANY_ITEM, PARRY_HAND_PRIORITY, SHIELD_PARRY_PRIORITY, INCLUDED_ITEMS, EXCLUDED_ITEMS, SHIELD_ITEMS, SHIELD_BLACKLIST, SHIELD_STUN_BOSSES;
        public Binding binding(boolean server) {
            return switch (this) {
                case INCOMING_REACTION -> new Binding(GuardConfig.INCOMING_REACTION,1);
                case PARRIED_ENABLED -> new Binding(GuardConfig.PARRIED_ENABLED,1);
                case PARRIED_GUARD_BREAK_TICKS -> new Binding(GuardConfig.PARRIED_GUARD_BREAK_TICKS,1);
                case PARRIED_STUN_TICKS -> new Binding(GuardConfig.PARRIED_STUN_TICKS,1);
                case SHIELD_CONE_ENABLED -> new Binding(GuardConfig.SHIELD_CONE_ENABLED,1);
                case PUNCHY_RELEASE_DELAY -> new Binding(GuardConfig.PUNCHY_RELEASE_DELAY, 1);
                case PERFECT_TICKS -> new Binding(GuardConfig.PERFECT_TICKS, 1);
                case PARRY_TICKS -> new Binding(GuardConfig.PARRY_TICKS, 1);
                case RECHARGE_TICKS -> new Binding(GuardConfig.RECHARGE_TICKS, 1);
                case FACING_ANGLE -> new Binding(GuardConfig.FACING_ANGLE, 1);
                case PARRY_HEALING -> new Binding(GuardConfig.PARRY_HEALING, 1);
                case PARRY_HEALING_HEARTS -> new Binding(GuardConfig.PARRY_HEALING_HEARTS, 1);
                case FOLLOW_UP_TICKS -> new Binding(GuardConfig.FOLLOW_UP_TICKS, 1);
                case PARRY_RETALIATION -> new Binding(GuardConfig.PARRY_RETALIATION, 100);
                case PERFECT_RETALIATION -> new Binding(GuardConfig.PERFECT_RETALIATION, 100);
                case RETALIATION_CAP -> new Binding(GuardConfig.RETALIATION_CAP, 1);
                case KNOCKBACK_STRENGTH -> new Binding(GuardConfig.KNOCKBACK_STRENGTH, 1);
                case SHIELD_PARRY_PUSHBACK_PERCENT -> new Binding(GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT, 1);
                case TOOL_PUSHBACK_PERCENT -> new Binding(GuardConfig.TOOL_PUSHBACK_PERCENT, 1);
                case PARRY_PROJECTILES -> new Binding(GuardConfig.PARRY_PROJECTILES, 1);
                case PARRY_EXPLOSIONS -> new Binding(GuardConfig.PARRY_EXPLOSIONS, 1);
                case PERFECT_EXPLOSIONS_ONLY -> new Binding(GuardConfig.PERFECT_EXPLOSIONS_ONLY, 1);
                case MOB_WHITELIST -> new Binding(GuardConfig.MOB_WHITELIST, 1);
                case MOB_GUARD -> new Binding(GuardConfig.MOB_GUARD, 1);
                case MOB_DIFFICULTY -> new Binding(GuardConfig.MOB_DIFFICULTY, 1);
                case MOB_FLYING_COLOR -> new Binding(GuardConfig.MOB_FLYING_COLOR,1);
                case MOB_DEBRIS_COLOR -> new Binding(GuardConfig.MOB_DEBRIS_COLOR,1);
                case MOB_TRACER_START -> new Binding(GuardConfig.MOB_TRACER_START,1);
                case MOB_TRACER_MIDDLE -> new Binding(GuardConfig.MOB_TRACER_MIDDLE,1);
                case MOB_TRACER_END -> new Binding(GuardConfig.MOB_TRACER_END,1);
                case MOB_GEAR_WHITELIST -> new Binding(GuardConfig.MOB_GEAR_WHITELIST,1);
                case MOB_GEAR_BLACKLIST -> new Binding(GuardConfig.MOB_GEAR_BLACKLIST,1);
                case MOB_GEAR_CHANCE -> new Binding(GuardConfig.MOB_GEAR_CHANCE, 1);
                case SHIELD_PERFECT_TICKS -> new Binding(GuardConfig.SHIELD_PERFECT_TICKS, 1);
                case SHIELD_PARRY_TICKS -> new Binding(GuardConfig.SHIELD_PARRY_TICKS, 1);
                case SHIELD_RECHARGE_TICKS -> new Binding(GuardConfig.SHIELD_RECHARGE_TICKS, 1);
                case SHIELD_MAX_BLOCKS -> new Binding(GuardConfig.SHIELD_MAX_BLOCKS, 1);
                case SHIELD_CONE_DEGREES -> new Binding(GuardConfig.SHIELD_CONE_DEGREES, 1);
                case SHIELD_CONE_REACH -> new Binding(GuardConfig.SHIELD_CONE_REACH, 1);
                case SHIELD_RETALIATION_PERCENT -> new Binding(GuardConfig.SHIELD_RETALIATION_PERCENT, 1);
                case SHIELD_STUN_TICKS -> new Binding(GuardConfig.SHIELD_STUN_TICKS, 1);
                case SHIELD_PUSHBACK_PERCENT -> new Binding(GuardConfig.SHIELD_PUSHBACK_PERCENT, 1);
                case FALL_BLAST_STRENGTH -> new Binding(GuardConfig.FALL_BLAST_STRENGTH, 1);
                case FALL_LAUNCH_POWER -> new Binding(GuardConfig.FALL_LAUNCH_POWER, 1);
                case FALL_BREAK_BLOCKS -> new Binding(GuardConfig.FALL_BREAK_BLOCKS, 1);
                case FALL_PARRY -> new Binding(GuardConfig.FALL_PARRY, 1);
                case PERFECT_FALL_ONLY -> new Binding(GuardConfig.PERFECT_FALL_ONLY, 1);
                case PERFECT_PROJECTILES_ONLY -> new Binding(GuardConfig.PERFECT_PROJECTILES_ONLY, 1);
                case RETALIATION_ENABLED -> new Binding(GuardConfig.RETALIATION_ENABLED, 1);
                case PERFECT_KNOCKBACK_MULTIPLIER -> new Binding(GuardConfig.PERFECT_KNOCKBACK_MULTIPLIER, 1);
                case BLOCK -> new Binding(GuardConfig.BLOCK, 1);
                case BLOCK_REDUCTION -> new Binding(GuardConfig.BLOCK_REDUCTION, 100);
                case TOOL_MAX_BLOCKS -> new Binding(GuardConfig.TOOL_MAX_BLOCKS, 1);
                case SHIELD_BREAK_TICKS -> new Binding(GuardConfig.SHIELD_BREAK_TICKS, 1);
                case GUARD_MOVEMENT_PERCENT -> new Binding(GuardConfig.GUARD_MOVEMENT_PERCENT, 1);
                case BLOCK_DEFLECT_CHANCE -> new Binding(GuardConfig.BLOCK_DEFLECT_CHANCE, 1);
                case BLOCK_PROJECTILES -> new Binding(GuardConfig.BLOCK_PROJECTILES, 1);
                case BLOCK_EXPLOSIONS -> new Binding(GuardConfig.BLOCK_EXPLOSIONS, 1);
                case HIT_PARTICLES -> new Binding(GuardConfig.HIT_PARTICLES, 1);
                case HIT_SOUNDS -> new Binding(GuardConfig.HIT_SOUNDS, 1);
                case ALLOW_EMPTY_HAND -> new Binding(GuardConfig.ALLOW_EMPTY_HAND, 1);
                case COOLDOWN_PREVENTS_GUARD -> new Binding(GuardConfig.COOLDOWN_PREVENTS_GUARD, 1);
                case CONSUMABLE_PRIORITY -> new Binding(GuardConfig.CONSUMABLE_PRIORITY, 1);
                case FORCE_CROUCH_OFFHAND -> new Binding(GuardConfig.FORCE_CROUCH_OFFHAND, 1);
                case PARRY_WEAR -> new Binding(GuardConfig.PARRY_WEAR, 1);
                case PERFECT_WEAR -> new Binding(GuardConfig.PERFECT_WEAR, 1);
                case BLOCK_WEAR -> new Binding(GuardConfig.BLOCK_WEAR, 1);
                case DEBRIS_BASE_INTENSITY -> new Binding(GuardParticleConfig.DEBRIS_BASE_INTENSITY,1);
                case DEBRIS_PERFECT_INTENSITY -> new Binding(GuardParticleConfig.DEBRIS_PERFECT_INTENSITY,1);
                case DEBRIS_DAMAGE_SCALING -> new Binding(GuardParticleConfig.DEBRIS_DAMAGE_SCALING,1);
                case FLYING_BASE_INTENSITY -> new Binding(GuardParticleConfig.FLYING_BASE_INTENSITY,1);
                case FLYING_PERFECT_INTENSITY -> new Binding(GuardParticleConfig.FLYING_PERFECT_INTENSITY,1);
                case FLYING_DAMAGE_SCALING -> new Binding(GuardParticleConfig.FLYING_DAMAGE_SCALING,1);
                case SHIELD_EFFECTS -> new Binding(GuardConfig.SHIELD_EFFECTS, 1);
                case FLASH_STRENGTH -> new Binding(GuardConfig.FLASH_STRENGTH, 1);
                case REGULAR_HITLAG_TICKS -> new Binding(GuardConfig.REGULAR_HITLAG_TICKS, 1);
                case PERFECT_ONLY_FLASH -> new Binding(GuardConfig.PERFECT_ONLY_FLASH, 1);
                case MEME_FLASH -> new Binding(GuardConfig.MEME_FLASH, 1);
                case SHAKE_STRENGTH -> new Binding(GuardConfig.SHAKE_STRENGTH, 1);
                case HITLAG_TICKS -> new Binding(GuardConfig.HITLAG_TICKS, 1);
                case IMPACT_FRAMES -> new Binding(GuardConfig.IMPACT_FRAMES, 1);
                case IMPACT_PERFECT_ONLY -> new Binding(GuardConfig.IMPACT_PERFECT_ONLY, 1);
                case THIRD_PERSON_ANIMATION -> new Binding(GuardConfig.THIRD_PERSON_ANIMATION, 1);
                case FIRST_PERSON_ANIMATION -> new Binding(GuardConfig.FIRST_PERSON_ANIMATION, 1);
                case MASTER_VOLUME -> new Binding(server ? GuardConfig.MASTER_VOLUME : GuardConfig.LOCAL_MASTER_VOLUME, 1);
                case PERFECT_VOLUME -> new Binding(server ? GuardConfig.PERFECT_VOLUME : GuardConfig.LOCAL_PERFECT_VOLUME, 1);
                case PARRY_VOLUME -> new Binding(server ? GuardConfig.PARRY_VOLUME : GuardConfig.LOCAL_PARRY_VOLUME, 1);
                case BLOCK_VOLUME -> new Binding(server ? GuardConfig.BLOCK_VOLUME : GuardConfig.LOCAL_BLOCK_VOLUME, 1);
                case CHROMATIC_INTENSITY -> new Binding(GuardConfig.CHROMATIC_INTENSITY, 1);
                case CHROMATIC_TICKS -> new Binding(GuardConfig.CHROMATIC_TICKS, 1);
                case CHROMATIC_PERFECT_ONLY -> new Binding(GuardConfig.CHROMATIC_PERFECT_ONLY, 1);
                case CHROMATIC_HUD -> new Binding(GuardConfig.CHROMATIC_HUD, 1);
                case ALLOW_ANY_ITEM -> new Binding(GuardConfig.ALLOW_ANY_ITEM, 1);
                case PARRY_HAND_PRIORITY -> new Binding(GuardConfig.PARRY_HAND_PRIORITY, 1);
                case SHIELD_PARRY_PRIORITY -> new Binding(GuardConfig.SHIELD_PARRY_PRIORITY, 1);
                case INCLUDED_ITEMS -> new Binding(GuardConfig.INCLUDED_ITEMS, 1);
                case EXCLUDED_ITEMS -> new Binding(GuardConfig.EXCLUDED_ITEMS, 1);
                case SHIELD_ITEMS -> new Binding(GuardConfig.SHIELD_ITEMS, 1);
                case SHIELD_BLACKLIST -> new Binding(GuardConfig.SHIELD_BLACKLIST, 1);
                case SHIELD_STUN_BOSSES -> new Binding(GuardConfig.SHIELD_STUN_BOSSES, 1);
            };
        }
    }
    public record Binding(ModConfigSpec.ConfigValue<?> setting, int scale) {
        public int minimum() {
            var range = GuardSettingRanges.range(setting);
            if (range == null) throw new IllegalArgumentException("Numeric setting has no declared bounds");
            return (int) Math.round(range.min() * scale);
        }
        public int maximum() {
            var range = GuardSettingRanges.range(setting);
            if (range == null) throw new IllegalArgumentException("Numeric setting has no declared bounds");
            return (int) Math.round(range.max() * scale);
        }
        public Object defaultValue() {
            Object value = setting.getDefault();
            return value instanceof Double number ? (int) Math.round(number * scale) : value;
        }
    }
    public static Binding presetBinding(int index) {
        if(GuardScreenSettings.slot(index))return new Binding(GuardScreenSettings.setting(index),1);
        if (GuardParticleColors.slot(index) && !GuardParticleColors.retired(index)) return new Binding(GuardParticleColors.setting(index), 1);
        if (GuardSparkTracerConfig.slot(index) && !GuardSparkTracerConfig.retired(index)) return new Binding(GuardSparkTracerConfig.setting(index), 1);
        if (index == GuardPoseSettings.RELEASE_DELAY_SLOT) return new Binding(GuardConfig.PUNCHY_RELEASE_DELAY, 1);
        if (index >= GuardShieldReactions.OFFSET && index < GuardShieldReactions.OFFSET + GuardShieldReactions.SIZE)
            return new Binding(GuardShieldReactions.setting(index - GuardShieldReactions.OFFSET), 1);
        if (index < GuardClientSettings.count()) {
            var setting = GuardClientSettings.setting(index);
            if (setting != null) return new Binding(setting, GuardClientSettings.scale(index));
        }
        throw new IllegalArgumentException("No editable setting at preset slot " + index);
    }
    public static int dragStep(Binding binding, int min, int max, int divisor, String suffix) {
        if (divisor == 1 && (long) max - min > 200) return 10;
        if (binding != null) {
            var setting = binding.setting();
            if (GuardParticleColors.indexOf(setting) >= 0 || GuardClientSettings.indexOf(setting) >= 16
                && GuardClientSettings.indexOf(setting) <= 21 || GuardSparkTracerConfig.indexOf(setting) >= 0) return 1;
            if (setting == GuardConfig.LOCAL_MASTER_VOLUME || setting == GuardConfig.LOCAL_PERFECT_VOLUME
                || setting == GuardConfig.LOCAL_PARRY_VOLUME || setting == GuardConfig.LOCAL_BLOCK_VOLUME
                || setting == GuardConfig.MASTER_VOLUME || setting == GuardConfig.PERFECT_VOLUME
                || setting == GuardConfig.PARRY_VOLUME || setting == GuardConfig.BLOCK_VOLUME
                || setting == GuardConfig.MOB_GEAR_CHANCE || setting == GuardConfig.MOB_DIFFICULTY
                || setting == GuardConfig.PARRY_RETALIATION || setting == GuardConfig.PERFECT_RETALIATION
                || setting == GuardConfig.SHIELD_RETALIATION_PERCENT || setting == GuardConfig.BLOCK_DEFLECT_CHANCE
                || setting == GuardConfig.KNOCKBACK_STRENGTH || setting == GuardConfig.TOOL_PUSHBACK_PERCENT
                || setting == GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT || setting == GuardConfig.SHIELD_PUSHBACK_PERCENT
                || setting == GuardConfig.GUARD_MOVEMENT_PERCENT) return divisor == 1 && suffix.equals("%") ? 5 : 1;
        }
        return divisor == 1 && suffix.equals("%") ? 10 : 1;
    }

}
