package dev.zeli.mallardguard;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Stable client setting slots shared by config files, presets, enforcement and the UI. */
public final class GuardClientSettings {
    private record Slot(String key, int category, int initial, int min, int max,
                        boolean flag, Supplier<ModConfigSpec.ConfigValue<?>> value, int scale) {
        Slot(String key,int category,int initial,int min,int max,boolean flag,Supplier<ModConfigSpec.ConfigValue<?>> value) {
            this(key,category,initial,min,max,flag,value,1);
        }
    }
    // Reserved slots retain their old positions so saved presets stay compatible.
    private static final Slot[] SLOTS = {
        new Slot("display.hud", 2, 1, 0, 1, true, () -> GuardConfig.HUD),
        new Slot("display.shieldEffects", 2, 1, 0, 1, true, () -> GuardConfig.SHIELD_EFFECTS),
        new Slot("display.screenFlash", 8, 0, 0, 1, true, () -> GuardConfig.SCREEN_FLASH),
        new Slot("display.hitlagTicks", 8, 3, 0, 30, false, () -> GuardConfig.HITLAG_TICKS),
        new Slot("display.flashStrength", 8, 100, 0, 100, false, () -> GuardConfig.FLASH_STRENGTH),
        new Slot("display.shakeStrength", 8, 100, 0, 100, false, () -> GuardConfig.SHAKE_STRENGTH),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("animation.firstPersonGuardPose", 16, 1, 0, 1, true, () -> GuardConfig.FIRST_PERSON_ANIMATION),
        new Slot("audio.masterVolume", 4, 100, 0, 200, false, () -> GuardConfig.LOCAL_MASTER_VOLUME),
        new Slot("audio.parryVolume", 4, 65, 0, 200, false, () -> GuardConfig.LOCAL_PARRY_VOLUME),
        new Slot("audio.perfectVolume", 4, 130, 0, 200, false, () -> GuardConfig.LOCAL_PERFECT_VOLUME),
        new Slot("audio.blockVolume", 4, 65, 0, 200, false, () -> GuardConfig.LOCAL_BLOCK_VOLUME),
        new Slot("particles.flyingSparks.baseIntensity", 1, GuardParticleConfig.DEFAULT_BASE_INTENSITY, 0, GuardParticleConfig.MAX_BASE_COUNT, false, () -> GuardParticleConfig.FLYING_BASE_INTENSITY),
        new Slot("particles.flyingSparks.perfectIntensity", 1, GuardParticleConfig.DEFAULT_PERFECT_INTENSITY, 0, GuardParticleConfig.MAX_PERFECT_INTENSITY, false, () -> GuardParticleConfig.FLYING_PERFECT_INTENSITY),
        new Slot("particles.flyingSparks.damageScaling", 1, 2, 0, 100, false, () -> GuardParticleConfig.FLYING_DAMAGE_SCALING),
        new Slot("particles.dotStreaks.baseIntensity", 1, GuardParticleConfig.DEFAULT_BASE_INTENSITY, 0, GuardParticleConfig.MAX_BASE_COUNT, false, () -> GuardParticleConfig.DEBRIS_BASE_INTENSITY),
        new Slot("particles.dotStreaks.perfectIntensity", 1, GuardParticleConfig.DEFAULT_PERFECT_INTENSITY, 0, GuardParticleConfig.MAX_PERFECT_INTENSITY, false, () -> GuardParticleConfig.DEBRIS_PERFECT_INTENSITY),
        new Slot("particles.dotStreaks.damageScaling", 1, 2, 0, 100, false, () -> GuardParticleConfig.DEBRIS_DAMAGE_SCALING),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("display.memeFlash", 8, 0, 0, 1, true, () -> GuardConfig.MEME_FLASH),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("display.perfectOnlyFlash", 8, 1, 0, 1, true, () -> GuardConfig.PERFECT_ONLY_FLASH),
        new Slot(null, 8, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("display.impactFrames", 8, 0, 0, 1, false, () -> GuardConfig.IMPACT_FRAMES),
        new Slot("display.impactPerfectOnly", 8, 1, 0, 1, true, () -> GuardConfig.IMPACT_PERFECT_ONLY),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("display.impactBrightness", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_BRIGHTNESS),
        new Slot("display.impactContrast", 8, 200, 0, 400, false, () -> GuardConfig.IMPACT_CONTRAST),
        new Slot("display.impactEdges", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_EDGES),
        new Slot("display.impactGrain", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_GRAIN),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, GuardParticleConfig.PRESET_REVISION, GuardParticleConfig.PRESET_REVISION, GuardParticleConfig.PRESET_REVISION, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot("animation.thirdPersonGuardPose", 16, 1, 0, 1, true, () -> GuardConfig.THIRD_PERSON_ANIMATION),
        new Slot("display.impactChromatic", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_CHROMATIC),
        new Slot("display.mobImpactBrightness", 8, 186, 0, 400, false, () -> GuardConfig.MOB_IMPACT_BRIGHTNESS),
        new Slot("display.mobImpactContrast", 8, 110, 0, 400, false, () -> GuardConfig.MOB_IMPACT_CONTRAST),
        new Slot("display.mobImpactEdges", 8, 200, 0, 400, false, () -> GuardConfig.MOB_IMPACT_EDGES),
        new Slot("display.mobImpactGrain", 8, 101, 0, 400, false, () -> GuardConfig.MOB_IMPACT_GRAIN),
        new Slot("display.mobImpactChromatic", 8, 30, 0, 400, false, () -> GuardConfig.MOB_IMPACT_CHROMATIC),
        new Slot("display.mobImpactAdapt", 8, 1, 0, 1, true, () -> GuardConfig.MOB_IMPACT_ADAPT),
        new Slot("display.mobImpactInvert", 8, 1, 0, 1, true, () -> GuardConfig.MOB_IMPACT_INVERT),
        new Slot("display.chromaticIntensity", 8, 200, 0, 200, false, () -> GuardConfig.CHROMATIC_INTENSITY),
        new Slot("display.chromaticTicks", 8, 6, 1, 40, false, () -> GuardConfig.CHROMATIC_TICKS),
        new Slot("display.chromaticPerfectOnly", 8, 0, 0, 1, true, () -> GuardConfig.CHROMATIC_PERFECT_ONLY),
        new Slot("display.chromaticHud", 8, 1, 0, 1, true, () -> GuardConfig.CHROMATIC_HUD),
        new Slot("display.regularHitlagTicks", 8, 1, 0, 30, false, () -> GuardConfig.REGULAR_HITLAG_TICKS),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
        new Slot(null, 1, 0, 0, 0, false, () -> null),
    };
    private GuardClientSettings() {}
    static int legacyRegularHitlag(com.electronwill.nightconfig.core.Config previous) {
        Object frames=previous.get("display.hitlagFrames");
        return Boolean.FALSE.equals(previous.get("display.perfectOnlyHitlag"))
            ? Math.clamp(frames instanceof Number n?(int)Math.round(n.doubleValue()/3):initial(3),0,30):0;
    }
    public static int count() { return SLOTS.length; }
    public static String key(int index) { return SLOTS[index].key(); }
    public static int category(int index) { return SLOTS[index].category(); }
    public static boolean flag(int index) { return SLOTS[index].flag(); }
    public static int initial(int index) { return SLOTS[index].initial(); }
    public static boolean reserved(int index) { return GuardParticleConfig.legacySlot(index) || (index<SLOTS.length && index!=47 && SLOTS[index].key()==null); }
    public static boolean initialFlag(int index) { return initial(index) != 0; }
    public static int minimum(int index) { return SLOTS[index].min(); }
    public static int maximum(int index) { return SLOTS[index].max(); }
    public static int scale(int index) { return SLOTS[index].scale(); }
    public static int local(int index) {
        var setting=SLOTS[index].value().get();
        if(setting==null)return initial(index);
        Object value=setting.get();
        return value instanceof Boolean flag?flag?1:0:(int)Math.round(((Number)value).doubleValue()*scale(index));
    }
    public static int[] defaults() {
        int[] values = new int[count()];
        for (int i = 0; i < values.length; i++) values[i] = SLOTS[i].initial();
        return values;
    }
    public static int[] minimums() { int[] out = new int[count()]; for(int i=0;i<out.length;i++)out[i]=minimum(i); return out; }
    public static int[] maximums() { int[] out = new int[count()]; for(int i=0;i<out.length;i++)out[i]=maximum(i); return out; }
    public static String[] keys() { String[] out = new String[count()]; for(int i=0;i<out.length;i++)out[i]=key(i); return out; }
    public static int indexOf(ModConfigSpec.ConfigValue<?> value) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i].value().get() == value) return i;
        return -1;
    }
    public static int[] readLocal() {
        int[] values = defaults();
        for (int i = 0; i < values.length; i++) {
            var setting = SLOTS[i].value().get();
            if (setting == null) continue;
            Object value = setting.get();
            values[i] = value instanceof Boolean enabled ? enabled ? 1 : 0 : (int)Math.round(((Number)value).doubleValue()*scale(i));
        }

        return values;
    }
    @SuppressWarnings("unchecked")
    public static void apply(int[] values, int category) {
        for (int i = 0; i < SLOTS.length; i++) {
            Slot slot = SLOTS[i];
            if (slot.category() != category) continue;
            var setting = slot.value().get();
            if (setting == null) continue;
            if (slot.flag()) ((ModConfigSpec.ConfigValue<Boolean>) setting).set(values[i] != 0);
            else if(slot.scale()!=1) ((ModConfigSpec.ConfigValue<Double>) setting).set(values[i]/(double)slot.scale());
            else ((ModConfigSpec.ConfigValue<Integer>) setting).set(values[i]);
        }

    }
    public static ModConfigSpec.ConfigValue<?> setting(int index) { return SLOTS[index].value().get(); }

}
