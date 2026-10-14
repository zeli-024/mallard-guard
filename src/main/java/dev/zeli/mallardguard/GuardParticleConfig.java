package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original particle baselines and stable preset storage. */
public final class GuardParticleConfig {
    public static final int PRESET_REVISION = 95;
    public static final int MAX_BASE_COUNT = 100;
    public static final int MAX_BURST_HEADS = 512, MAX_ACTIVE_HEADS = 1024;
    public static final int FADE_TICKS = 8, DROP_DELAY_TICKS = 5;
    public static final double SIZE_SCALE = 0.65, SPEED_SCALE = 5.0;
    public static final double VELOCITY_LOSS = 24.0, GRAVITY_PERCENT = 10.0;
    public static ModConfigSpec.IntValue FLYING_BASE_INTENSITY, FLYING_PERFECT_INTENSITY, FLYING_DAMAGE_SCALING;
    public static ModConfigSpec.IntValue DEBRIS_BASE_INTENSITY, DEBRIS_PERFECT_INTENSITY, DEBRIS_DAMAGE_SCALING;
    public static final int DEBRIS_SIZE = 30;
    public static final int DEBRIS_SPEED = 100;
    public static final int DEBRIS_LOSS = 3;
    public static final int DEBRIS_GRAVITY = 78;
    public static final int DEBRIS_DELAY = 0;
    public static final int DEBRIS_FADE = 7;
    public static final int DEBRIS_BOUNCE = 64;
    public static final int DEBRIS_DECAY = 18;
    public static final int DEBRIS_FRICTION = 14;
    public static final int DEBRIS_TAIL = 3;
    public static final float DEBRIS_TAIL_LIFETIME = 2.7F;
    public static final int DEBRIS_TAIL_SIZE = 80;
    public static final int DEBRIS_SIZE_VARIANCE = 100;
    public static final int DEBRIS_MOTION_VARIANCE = 100;
    public static final int DEBRIS_FADE_VARIANCE = 100;
    public static final int DEBRIS_BIAS = 50;
    public static final int LEGACY_COLOR_OFFSET=GuardPoseSettings.RELEASE_DELAY_SLOT+1;
    private GuardParticleConfig() {}
    public static int[] appendParticleSettings(int[] values,boolean local) {
        return GuardParticleColors.append(GuardSparkTracerConfig.append(java.util.Arrays.copyOf(values,LEGACY_COLOR_OFFSET+4),local),local);
    }
    public static void define(ModConfigSpec.Builder builder) {
        FLYING_BASE_INTENSITY = GuardSettingRanges.integer(builder.comment("Regular-parry spark amount as a percentage of 100 sparks. Zero disables regular-parry sparks. Electric and animated appearances share this amount."), "particles.flyingSparks.baseIntensity",8,0,100);
        FLYING_PERFECT_INTENSITY = GuardSettingRanges.integer(builder.comment("Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 disables the effect completely."), "particles.flyingSparks.perfectIntensity",32,0,100);
        FLYING_DAMAGE_SCALING = GuardSettingRanges.integer(builder.comment("Extra percentage of the selected amount per intercepted damage point. Zero disables damage scaling. Counts are capped at 512 per burst; projectile hits use an estimate when exact damage is unavailable."), "particles.flyingSparks.damageScaling",2,0,100);
        DEBRIS_BASE_INTENSITY = GuardSettingRanges.integer(builder.comment("Regular-parry spark debris amount as a percentage of 100 debris sparks. Zero disables regular-parry spark debris."), "particles.dotStreaks.baseIntensity",8,0,100);
        DEBRIS_PERFECT_INTENSITY = GuardSettingRanges.integer(builder.comment("Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 disables the effect completely."), "particles.dotStreaks.perfectIntensity",32,0,100);
        DEBRIS_DAMAGE_SCALING = GuardSettingRanges.integer(builder.comment("Extra percentage of the selected debris amount per intercepted damage point. Zero disables scaling. Flying Sparks and Spark Debris share the burst and active budgets."), "particles.dotStreaks.damageScaling",2,0,100);
    }
    public static int perfectIncrease(int base,int previous){return base<=0?0:Math.max(0,Math.min(100,(int)Math.round((previous/(double)base-1)*100)));}
    public static void migratePerfect(int[] values){values[17]=perfectIncrease(values[16],values[17]);values[20]=perfectIncrease(values[19],values[20]);int o=GuardSparkTracerConfig.OFFSET;values[o+GuardSparkTracerConfig.PERFECTINTENSITY]=perfectIncrease(values[o+GuardSparkTracerConfig.BASEINTENSITY],values[o+GuardSparkTracerConfig.PERFECTINTENSITY]);}
    public static void migrateClientPerfect(byte[] original){
        String section="";java.util.Map<String,Integer> old=new java.util.HashMap<>();
        for(String line:new String(original,java.nio.charset.StandardCharsets.UTF_8).split("\\R")){String t=line.trim();if(t.startsWith("[")&&t.endsWith("]")){section=t.substring(1,t.length()-1);continue;}int at=t.indexOf('=');if(at<0||!section.startsWith("particles."))continue;try{old.put(section+"."+t.substring(0,at).trim(),Integer.parseInt(t.substring(at+1).split("#",2)[0].trim()));}catch(NumberFormatException ignored){}}
        String[] groups={"flyingSparks","dotStreaks","shootingSparks"};
        ModConfigSpec.IntValue[] perfect={FLYING_PERFECT_INTENSITY,DEBRIS_PERFECT_INTENSITY,null};
        for(int i=0;i<groups.length;i++){String prefix="particles."+groups[i]+".";Integer base=old.get(prefix+"baseIntensity"),previous=old.get(prefix+"perfectIntensity");if(i==2){if(base==null)base=old.get(prefix+"intensity");if(previous==null)previous=base;}if(base==null||previous==null)continue;int value=perfectIncrease(base,previous);if(i<2)perfect[i].set(value);else{int[] a=GuardSparkTracerConfig.append(new int[GuardSparkTracerConfig.OFFSET],true);a[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY]=value;GuardSparkTracerConfig.apply(a);}}
    }
    public static void writeTemplate(com.electronwill.nightconfig.core.Config config,int[] values) {
        config.remove("particles");
        GuardSparkTracerConfig.write(config,values);
        GuardParticleColors.write(config,values);
        config.set("particles.flyingSparks.baseIntensity",values[16]);
        config.set("particles.flyingSparks.perfectIntensity",values[17]);
        config.set("particles.flyingSparks.damageScaling",values[18]);
        config.set("particles.dotStreaks.baseIntensity",values[19]);
        config.set("particles.dotStreaks.perfectIntensity",values[20]);
        config.set("particles.dotStreaks.damageScaling",values[21]);
    }
    public static boolean legacySlot(int index) {
        return (index>=LEGACY_COLOR_OFFSET && index<LEGACY_COLOR_OFFSET+4) || switch(index) {
            case 6,7,8,9,22,23,25,26,29,30,31,32,33,37,38,39,44,45,46,48,63,64 -> true;
            default -> false;
        };
    }
    public static boolean slot(int index) { return (index>=16 && index<=21) || legacySlot(index); }
}
