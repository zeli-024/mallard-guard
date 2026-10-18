package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Shared appearance settings; retired preset positions stay reserved for compatibility. */
public final class GuardParticleColors {
    public static final int OFFSET=GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.DEFAULTS.length;
    public static final int BASE=OFFSET,FLYING_SIZE=OFFSET+2,FLYING_SIZE_VARIANCE=OFFSET+3,DEBRIS_COLOR=OFFSET+4,DEBRIS_SIZE=OFFSET+5;
    public static final int TRACER_START_COLOR=OFFSET+6,TRACER_MIDDLE_COLOR=OFFSET+7,TRACER_END_COLOR=OFFSET+8,TRACER_SIZE=OFFSET+9,TRACER_TRAIL_TICKS=OFFSET+10;
    public static final int IMPACT_SIZE=OFFSET+11,IMPACT_DURATION=OFFSET+12,IMPACT_OPACITY=OFFSET+13,IMPACT_SPIN=OFFSET+25;
    public static final int FLYING_LIFETIME=OFFSET+38,DEBRIS_LIFETIME=OFFSET+39,TRACER_LIFETIME=OFFSET+40;
    public static final int SHIELD_FORWARD_BIAS=OFFSET+50;
    public static final int FLYING_ENABLED=OFFSET+56,DEBRIS_ENABLED=OFFSET+57,TRACER_ENABLED=OFFSET+58,IMPACT_ENABLED=OFFSET+59,PALETTE_PREVIOUS_LENGTH=OFFSET+60,LENGTH=OFFSET+63;
    public static final int CONTROLS_PREVIOUS_LENGTH=OFFSET+56;
    // Historical serialized lengths only; these do not define live settings.
    public static final int PREVIOUS_LENGTH=OFFSET+11,IMPACT_BASE_LENGTH=OFFSET+13,IMPACT_PREVIOUS_LENGTH=OFFSET+25,IMPACT_SETTINGS_LENGTH=OFFSET+28,IMPACT_COMPONENT_LENGTH=OFFSET+33,APPEARANCE_LENGTH=OFFSET+38,LIFETIME_LENGTH=OFFSET+41,LEGACY_LENGTH=OFFSET+42,SERIALIZED_LENGTH_49=OFFSET+49,SERIALIZED_LENGTH_51=OFFSET+51;
    public static final int TRACER_START=0xFFFFFF,TRACER_MIDDLE=0xFFFFFF,TRACER_END=0xFFFFFF;
    private static final String[] KEYS={
        "particles.flyingSparks.baseColor","particles.flyingSparks.colorVariation","particles.flyingSparks.size","particles.flyingSparks.sizeVariance",
        "particles.dotStreaks.baseColor","particles.dotStreaks.size","particles.shootingSparks.startColor","particles.shootingSparks.middleColor",
        "particles.shootingSparks.endColor","particles.shootingSparks.overallSize","particles.shootingSparks.trailTicks",
        "particles.centerImpact.size","particles.centerImpact.durationTicks","particles.centerImpact.opacity","particles.centerImpact.crossColor",
        "particles.centerImpact.fadeInFrames","particles.centerImpact.fadeOutFrames","particles.centerImpact.rotation","particles.centerImpact.sizeVariance",
        "particles.centerImpact.durationVariance","particles.centerImpact.opacityVariance","particles.centerImpact.rotationVariance",
        "particles.centerImpact.horizontalOffset","particles.centerImpact.verticalOffset","particles.centerImpact.depthOffset","particles.centerImpact.spinSpeed",
        "particles.centerImpact.endShrink","particles.centerImpact.shrinkTicks","particles.centerImpact.ringColor","particles.centerImpact.ringColorStrength",
        "particles.centerImpact.ringOpacity","particles.centerImpact.crossColorStrength","particles.centerImpact.crossOpacity","particles.centerImpact.crossStartSize",
        "particles.centerImpact.crossExpandTicks","particles.centerImpact.ringStartSize","particles.centerImpact.ringExpandTicks","particles.centerImpact.ringSize",
        "particles.flyingSparks.lifetimeTicks","particles.dotStreaks.lifetimeTicks","particles.shootingSparks.lifetimeTicks","particles.centerImpact.crossFlashTicks",
        null,null,null,null,null,null,null,null,"particles.shieldForwardBias",
        null,null,null,null,null,
        "particles.flyingSparks.enabled","particles.dotStreaks.enabled","particles.shootingSparks.enabled","particles.centerImpact.enabled",
        "particles.shootingSparks.perfectStartColor","particles.shootingSparks.perfectMiddleColor","particles.shootingSparks.perfectEndColor"
    };
    public static final String[] LABELS=new String[KEYS.length];
    private static final int[] DEFAULTS={
        0xFFFFFF,0,80,50,0xFFFFFF,100,TRACER_START,TRACER_MIDDLE,TRACER_END,125,2,250,8,100,0,0,0,0,0,0,0,0,0,0,0,45,0,0,0,0,0,0,0,0,0,0,0,0,20,30,12,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,1,1,1,0,0,0
    };
    private static final ModConfigSpec.IntValue[] VALUES=new ModConfigSpec.IntValue[DEFAULTS.length];
    public static ModConfigSpec.IntValue FLYING_COLOR;
    static {
        java.util.Arrays.fill(LABELS,"Retired particle setting");
        int[] slots={BASE,FLYING_SIZE,FLYING_SIZE_VARIANCE,DEBRIS_COLOR,DEBRIS_SIZE,TRACER_START_COLOR,TRACER_MIDDLE_COLOR,TRACER_END_COLOR,TRACER_SIZE,TRACER_TRAIL_TICKS,IMPACT_SIZE,IMPACT_DURATION,IMPACT_OPACITY,IMPACT_SPIN,FLYING_LIFETIME,DEBRIS_LIFETIME,TRACER_LIFETIME,SHIELD_FORWARD_BIAS,FLYING_ENABLED,DEBRIS_ENABLED,TRACER_ENABLED,IMPACT_ENABLED};
        String[] names={"Flying Base Color","Flying Size","Flying Size Variance","Debris Base Color","Debris Size","Tracer Start Color","Tracer Middle Color","Tracer End Color","Tracer Size","Tracer Tail Length","Impact Size","Impact Duration","Impact Opacity","Impact Spin Speed","Flying Lifetime","Debris Lifetime","Tracer Lifetime","Shield Forward Bias","Flying Sparks Enabled","Spark Debris Enabled","Spark Tracers Enabled","Center Impact Enabled"};
        for(int k=0;k<slots.length;k++)LABELS[slots[k]-OFFSET]=names[k];
    }
    private GuardParticleColors() {}
    public static boolean slot(int i){return i>=OFFSET&&i<LENGTH;}
    public static boolean retired(int i){if(!slot(i))return false;int k=i-OFFSET;return k==1||k>=14&&k<=37&&k!=25||k>=41&&k<=49||k>=51&&k<=55||k>=60;}
    public static boolean toggleSlot(int i){return i==SHIELD_FORWARD_BIAS||i>=FLYING_ENABLED&&i<=IMPACT_ENABLED;}
    public static boolean colorSlot(int i){return i==BASE||i==DEBRIS_COLOR||i>=TRACER_START_COLOR&&i<=TRACER_END_COLOR;}
    private static boolean lifetimeSlot(int i){return i>=FLYING_LIFETIME&&i<=TRACER_LIFETIME;}
    public static int min(int i){return lifetimeSlot(i)||i==IMPACT_DURATION||i==TRACER_TRAIL_TICKS?1:0;}
    public static int max(int i){
        if(retired(i))return 0;if(toggleSlot(i))return 1;if(colorSlot(i))return 0xFFFFFF;if(lifetimeSlot(i))return 80;
        if(i==IMPACT_DURATION)return 40;if(i==IMPACT_SPIN)return 720;if(i==TRACER_TRAIL_TICKS)return 4;
        if(i==FLYING_SIZE_VARIANCE||i==IMPACT_OPACITY)return 100;
        return 400;
    }
    public static String unit(int i){if(lifetimeSlot(i)||i==IMPACT_DURATION||i==TRACER_TRAIL_TICKS)return " ticks";if(i==IMPACT_SPIN)return "°/s";return "%";}
    public static String tip(int i){
        if(i>=FLYING_ENABLED&&i<=IMPACT_ENABLED)return "Enable this effect without changing its intensity or appearance.\nOff stops new particles from spawning. Default: On.";
        if(i==IMPACT_SIZE)return "Scale of the six-frame impact at the shared hit origin.\n0% turns only the animated impact off. Default: 250%.";
        if(i==IMPACT_SPIN)return "Spin speed; each impact randomly chooses left or right.\nIts starting angle is always randomized. Default: 45°/s.";
        if(i==IMPACT_OPACITY)return "Visibility of the entire animated impact.\n0% hides it; source transparency remains. Default: 100%.";
        if(i==IMPACT_DURATION)return "Total lifetime and playback time for all six frames.\nLonger is slower. 20 ticks = 1 second. Default: 8 ticks.";
        if(i==SHIELD_FORWARD_BIAS)return "Favor forward launches on shield parries, with side and rear sparks.\nApplies to all three spark types; fall parries use a ring. Default: On.";
        if(lifetimeSlot(i))return "Base lifetime before the existing per-particle variance.\n20 ticks = 1 second; tails finish separately. Default: "+DEFAULTS[i-OFFSET]+" ticks.";
        if(colorSlot(i)){
            if(i==TRACER_START_COLOR)return "Head and new tail color before fading.\nAlso colors the beginning of the center impact.";
            if(i==TRACER_MIDDLE_COLOR)return "Color halfway through fading.\nAlso colors the middle of the center impact.";
            if(i==TRACER_END_COLOR)return "Final color near disappearance.\nAlso colors the end of the center impact.";
            if(i==DEBRIS_COLOR)return "Debris color; colored sparks vary toward warmer shades.\nWhite stays white. Click the preview or enter a hex code.";
            return "Flying Spark color, kept throughout life.\nSaturation varies from 70–100% at spawn; white stays white.";
        }
        if(i==TRACER_TRAIL_TICKS)return "Tail group lifetime before shared variance; range 1–4 ticks.\nLonger tails keep more squares. Default: 2 ticks.";
        if(i==FLYING_SIZE_VARIANCE)return "Random size per spark. Higher values widen the range.\n0% uses one size. Default: 50%.";
        return "Visual size; physics stay the same. 0% disables this particle.\n100% is the original size. Default: "+DEFAULTS[i-OFFSET]+"%.";
    }
    public static void define(ModConfigSpec.Builder b){for(int k=0;k<VALUES.length;k++){int i=OFFSET+k;if(!retired(i))VALUES[k]=GuardSettingRanges.integer(b.comment(tip(i)), KEYS[k],DEFAULTS[k],min(i),max(i));}FLYING_COLOR=VALUES[0];}
    public static int get(int i){return retired(i)?0:VALUES[i-OFFSET].get();}
    public static int indexOf(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> value){for(int k=0;k<VALUES.length;k++)if(VALUES[k]!=null&&VALUES[k]==value)return OFFSET+k;return -1;}
    public static ModConfigSpec.IntValue setting(int index){return VALUES[index-OFFSET];}
    public static String key(int i){return KEYS[i-OFFSET];}
    public static boolean valid(int i,int v){return v>=min(i)&&v<=max(i);}
    public static int[] append(int[] input,boolean local){int[] out=java.util.Arrays.copyOf(input,LENGTH);for(int k=0;k<VALUES.length;k++)out[OFFSET+k]=local?get(OFFSET+k):DEFAULTS[k];return out;}
    public static void apply(int[] a){for(int i=OFFSET;i<LENGTH;i++)if(!retired(i))VALUES[i-OFFSET].set(a[i]);}
    public static void read(com.electronwill.nightconfig.core.Config c,int[] a){
        for(int i=OFFSET;i<LENGTH;i++){
            if(retired(i)){a[i]=0;continue;}Object raw=c.get(key(i));
            if(raw instanceof Number n){int v=n.intValue();if(i==TRACER_TRAIL_TICKS&&v>=1&&v<=20)v=Math.min(4,v);if(i==IMPACT_SPIN&&v>=-720&&v<=720)v=Math.abs(v);if(valid(i,v))a[i]=v;}
        }
        if(c.get(key(IMPACT_DURATION))==null){Object old=c.get("particles.centerImpact.animationSpeed");if(old instanceof Number n&&n.intValue()>=25&&n.intValue()<=400)a[IMPACT_DURATION]=durationFromSpeed(n.intValue());}
    }
    public static int durationFromSpeed(int percent){return Math.max(1,Math.min(40,Math.round(600F/percent)));}
    public static void migrateClient(byte[] original){var old=new com.electronwill.nightconfig.toml.TomlParser().parse(new String(original,java.nio.charset.StandardCharsets.UTF_8));int[] a=append(new int[OFFSET],true);read(old,a);apply(a);}
    public static void write(com.electronwill.nightconfig.core.Config c,int[] a){for(int i=OFFSET;i<LENGTH;i++){if(retired(i)){if(key(i)!=null)c.remove(key(i));}else c.set(key(i),a[i]);}c.remove("particles.centerImpact.color");c.remove("particles.centerImpact.animationSpeed");}
    public static int blend(int a,int b,double t){t=Math.max(0,Math.min(1,t));int r=(int)Math.round((a>>16&255)*(1-t)+(b>>16&255)*t),g=(int)Math.round((a>>8&255)*(1-t)+(b>>8&255)*t),blue=(int)Math.round((a&255)*(1-t)+(b&255)*t);return r<<16|g<<8|blue;}
    /** Keep hue and brightness; sample 70–100% of the chosen saturation once at spawn. */
    public static int flying(int base,double sample){int value=Math.max(base>>16&255,Math.max(base>>8&255,base&255));int gray=value<<16|value<<8|value;return blend(gray,base,0.70+0.30*Math.max(0,Math.min(1,sample)));}
    public static int debris(double sample){return debris(0xFFFFFF,sample);}
    public static int debris(int base,double sample){if((base>>16&255)==(base>>8&255)&&(base>>8&255)==(base&255))return base;int warm=(base>>16&255)<<16|((base>>8&255)*208/230)<<8|(base&255)*138/181;return blend(base,warm,sample*sample);}
    public static int tracer(double cooling){return tracer(cooling,TRACER_START,TRACER_MIDDLE,TRACER_END);}
    public static int tracer(double cooling,int start,int middle,int end){return cooling<=.5?blend(start,middle,cooling*2):blend(middle,end,(cooling-.5)*2);}
}
