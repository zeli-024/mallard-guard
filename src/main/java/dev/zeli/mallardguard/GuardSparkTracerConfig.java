package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Selected Spark Tracers physics with base amount and event increases. */
public final class GuardSparkTracerConfig {
    public static final int OFFSET=GuardParticleConfig.LEGACY_COLOR_OFFSET+4;
    public static final int ENABLED=0;
    public static final int INTENSITY=1,BASEINTENSITY=1;
    public static final int LAUNCHSPEED=2;
    public static final int SPEEDVARIANCE=3;
    public static final int SPREADBIAS=4;
    public static final int SIZE=5;
    public static final int HEADSIZE=6;
    public static final int HEADLIFETIME=7;
    public static final int GRAVITY=8;
    public static final int DROPDELAY=9;
    public static final int VELOCITYLOSS=10;
    public static final int BOUNCE=11;
    public static final int BOUNCEDECAY=12;
    public static final int SURFACEFRICTION=13;
    public static final int LEGACY_MOTION=14;
    public static final int HEADOPACITY=15;
    public static final int HEADFADE=16;
    public static final int TRAILLIFETIME=17;
    public static final int TRAILOPACITY=18;
    public static final int TRAILFADE=19;
    public static final int TRAILENABLED=20;
    public static final int SQUARESPERTICK=21;
    public static final int HEADSHRINK=22,TRAILSHRINK=23;
    public static final int LIFETIMEVARIANCE=24;
    public static final int GRAVITYVARIANCE=25,VELOCITYLOSSVARIANCE=26,FADEVARIANCE=27;
    public static final int VARIANCE_LENGTH=OFFSET+25;
    public static final int SHRINK_LENGTH=OFFSET+24;
    public static final int PREVIOUS_LENGTH=OFFSET+22;
    public static final int LEGACY_LENGTH=OFFSET+21;
    public static final int PERFECTINTENSITY=28,DAMAGESCALING=29;
    public static final int TUNING_LENGTH=OFFSET+28;
    private static final int[] BASELINES={1,16,85,50,100,100,140,16,18,0,22,30,50,14,0,100,4,2,100,4,1,24,30,50,100,100,20,25};
    public static final int[] DISPLAY_ORDER={BASEINTENSITY,PERFECTINTENSITY,DAMAGESCALING};
    public static final String[] LABELS=new String[30],TIPS=new String[30];
    public static final int[] DEFAULTS=new int[30],MIN=new int[30],MAX=new int[30];
    private static final ModConfigSpec.IntValue[] VALUES=new ModConfigSpec.IntValue[30];
    static {
        java.util.Arrays.fill(LABELS,"Retired particle setting");
        java.util.Arrays.fill(TIPS,"Selected physics and appearance use fixed baselines.");
        LABELS[BASEINTENSITY]="Tracer Base Intensity";
        LABELS[PERFECTINTENSITY]="Tracer Perfect Intensity";
        LABELS[DAMAGESCALING]="Tracer Damage Intensity";
        TIPS[BASEINTENSITY]="Base amount. 100% produces 100 tracer heads; 0% disables Spark Tracers completely.";
        TIPS[PERFECTINTENSITY]="Extra amount on perfect parries. 0% keeps the base; 100% doubles it. Base 0% disables the effect.";
        TIPS[DAMAGESCALING]="Increase amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All particles share burst and active head caps.";
        DEFAULTS[BASEINTENSITY]=GuardParticleConfig.DEFAULT_BASE_INTENSITY;DEFAULTS[PERFECTINTENSITY]=GuardParticleConfig.DEFAULT_PERFECT_INTENSITY;DEFAULTS[DAMAGESCALING]=2;
        for(int i:DISPLAY_ORDER)MAX[i]=100;
        MAX[PERFECTINTENSITY]=GuardParticleConfig.MAX_PERFECT_INTENSITY;
    }
    private GuardSparkTracerConfig() {}
    public static boolean retired(int index) {return slot(index)&&index!=OFFSET+BASEINTENSITY&&index!=OFFSET+PERFECTINTENSITY&&index!=OFFSET+DAMAGESCALING;}
    public static void define(ModConfigSpec.Builder builder) {
        for(int i:DISPLAY_ORDER)VALUES[i]=GuardSettingRanges.integer(builder.comment(TIPS[i]), key(OFFSET+i),DEFAULTS[i],MIN[i],MAX[i]);
    }
    public static int get(int local) {return local==BASEINTENSITY||local==PERFECTINTENSITY||local==DAMAGESCALING?VALUES[local].get():BASELINES[local];}
    public static boolean slot(int index) {return index>=OFFSET&&index<OFFSET+DEFAULTS.length;}
    public static String key(int index) {
        return switch(index-OFFSET){case BASEINTENSITY->"particles.shootingSparks.baseIntensity";case PERFECTINTENSITY->"particles.shootingSparks.perfectIntensity";case DAMAGESCALING->"particles.shootingSparks.damageScaling";default->null;};
    }
    public static boolean valid(int index,int value) {int i=index-OFFSET;return value>=MIN[i]&&value<=MAX[i];}
    public static int[] append(int[] input,boolean local) {
        int[] out=java.util.Arrays.copyOf(input,OFFSET+DEFAULTS.length);
        for(int i:DISPLAY_ORDER)out[OFFSET+i]=local?get(i):DEFAULTS[i];
        for(int i=0;i<DEFAULTS.length;i++)if(retired(OFFSET+i))out[OFFSET+i]=0;
        return out;
    }
    public static void apply(int[] values) {for(int i:DISPLAY_ORDER)VALUES[i].set(values[OFFSET+i]);}
    public static void migrateAmounts(int[] values,int intensity,boolean enabled) {
        int amount=enabled?Math.max(0,Math.min(100,intensity)):0;
        values[OFFSET+BASEINTENSITY]=amount;values[OFFSET+PERFECTINTENSITY]=amount;
        values[OFFSET+DAMAGESCALING]=0;
    }
    public static void read(com.electronwill.nightconfig.core.Config config,int[] values) {
        Object intensity=config.get("particles.shootingSparks.intensity"),enabled=config.get("particles.shootingSparks.enabled");
        if(intensity instanceof Number n&&n.intValue()>=0&&n.intValue()<=100)migrateAmounts(values,n.intValue(),!(enabled instanceof Number e)||e.intValue()!=0);
        for(int i:DISPLAY_ORDER){Object raw=config.get(key(OFFSET+i));if(raw instanceof Number n&&valid(OFFSET+i,n.intValue()))values[OFFSET+i]=n.intValue();}
    }
    /** Preserve old amount controls before schema correction removes the tuning fields. */
    public static void migrateClient(byte[] original) {
        String section="";Integer intensity=null;boolean enabled=true;
        for(String line:new String(original,java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String text=line.trim();
            if(text.startsWith("[")&&text.endsWith("]")){section=text.substring(1,text.length()-1);continue;}
            if(!section.equals("particles.shootingSparks"))continue;
            int at=text.indexOf('=');if(at<0)continue;
            String name=text.substring(0,at).trim(),value=text.substring(at+1).split("#",2)[0].trim();
            try{if(name.equals("intensity"))intensity=Integer.parseInt(value);if(name.equals("enabled"))enabled=Integer.parseInt(value)!=0;}catch(NumberFormatException ignored){}
        }
        if(intensity!=null&&intensity>=0&&intensity<=100){int[] values=append(new int[OFFSET],true);migrateAmounts(values,intensity,enabled);apply(values);}
    }
    public static void write(com.electronwill.nightconfig.core.Config config,int[] values) {
        config.remove("particles.shootingSparks");
        for(int i:DISPLAY_ORDER)config.set(key(OFFSET+i),values[OFFSET+i]);
    }
    public static ModConfigSpec.ConfigValue<?> setting(int index) { return VALUES[index - OFFSET]; }

    public static int indexOf(ModConfigSpec.ConfigValue<?> setting) {
        for (int i : DISPLAY_ORDER) if (VALUES[i] == setting) return OFFSET + i;
        return -1;
    }

}
