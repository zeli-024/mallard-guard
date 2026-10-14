package dev.zeli.mallardguard;

import java.util.Arrays;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-reaction controls appended after the legacy pose slots. */
public final class GuardShieldReactions {
    public static final int OFFSET=GuardPoseSettings.OFFSET+GuardPoseSettings.COUNT*GuardPoseSettings.STRIDE;
    public static final String[] NAMES={"Regular Parry","Perfect Parry","Blocked Hit","Guard Broken","Parry Ready"};
    public static final String[] KEYS={"parry","perfect","block","guardBreak","recharge"};
    public static final String[] CONTROLS={"Enabled","Expansion","Shake","Flash","Echo","Duration"};
    public static final int STRIDE=6, SIZE=NAMES.length*STRIDE;
    private static final ModConfigSpec.IntValue[] SETTINGS=new ModConfigSpec.IntValue[SIZE];
    private static final int[] DURATIONS={4,9,5,5,6};
    private GuardShieldReactions(){}
    public static boolean used(int i){int r=i/STRIDE,c=i%STRIDE;return !(r==0&&c==4||r==4&&(c==2||c==4));}
    public static int defaultValue(int i){return i%STRIDE==0?1:i%STRIDE==5?DURATIONS[i/STRIDE]:100;}
    public static int max(int i){return i%STRIDE==0?1:i%STRIDE==5?40:200;}
    public static String key(int i){return "shieldReactions."+KEYS[i/STRIDE]+"."+CONTROLS[i%STRIDE].toLowerCase(java.util.Locale.ROOT);}
    public static void configure(ModConfigSpec.Builder builder){
        builder.push("shieldReactions");
        for(int r=0;r<NAMES.length;r++){
            builder.push(KEYS[r]);
            for(int c=0;c<STRIDE;c++){int i=r*STRIDE+c;if(!used(i))continue;SETTINGS[i]=GuardSettingRanges.integer(builder.comment(c==5?"Reaction lifetime in ticks. 20 ticks = one second.":"Reaction control. 100% retains the default effect; 0 disables this component."), CONTROLS[c].toLowerCase(java.util.Locale.ROOT),defaultValue(i),c==5?1:0,max(i));}
            builder.pop();
        }builder.pop();
    }
    public static int[] append(int[] base,boolean local){int[] result=Arrays.copyOf(base,OFFSET+SIZE);for(int i=0;i<SIZE;i++)result[OFFSET+i]=local&&used(i)?SETTINGS[i].get():defaultValue(i);return result;}
    public static void apply(int[] values){for(int i=0;i<SIZE;i++)if(used(i))SETTINGS[i].set(values[OFFSET+i]);}
    public static boolean enabled(int reaction){return SETTINGS[reaction*STRIDE].get()!=0;}
    public static float strength(int reaction,int control){int i=reaction*STRIDE+control;return used(i)?SETTINGS[i].get()/100F:1F;}
    public static float factor(int reaction,int control){return GuardScreenSettings.component(reaction,control)?strength(reaction,control):0F;}
    public static long millis(int reaction){return SETTINGS[reaction*STRIDE+5].get()*50L;}
    public static void readTemplate(com.electronwill.nightconfig.core.UnmodifiableConfig config,int[] values){for(int i=0;i<SIZE;i++){if(!used(i))continue;Object raw=config.get(key(i));if(raw instanceof Number n&&n.intValue()>=(i%STRIDE==5?1:0)&&n.intValue()<=max(i))values[OFFSET+i]=n.intValue();}}
    public static void writeTemplate(com.electronwill.nightconfig.core.Config config,int[] values){for(int i=0;i<SIZE;i++)if(used(i))config.set(key(i),values[OFFSET+i]);}
    public static ModConfigSpec.ConfigValue<?> setting(int index) { return SETTINGS[index]; }

}
