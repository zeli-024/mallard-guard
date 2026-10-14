package dev.zeli.mallardguard;

import com.electronwill.nightconfig.core.Config;
import java.util.Arrays;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Stable preset positions; retired controls are cleaned instead of renumbered. */
public final class GuardScreenSettings {
    public static final int OFFSET=GuardParticleColors.LENGTH;
    public static final int CAMERA_ENABLED=OFFSET,CHROMATIC_ENABLED=OFFSET+1,HITLAG_ENABLED=OFFSET+2,HITLAG_PERFECT_ONLY=OFFSET+3;
    public static final int HIGHLIGHT_ENABLED=OFFSET+11,HIGHLIGHT_DIM=OFFSET+12,HIGHLIGHT_GLOW=OFFSET+14,HIGHLIGHT_FADE=OFFSET+15;
    public static final int REACTION_TOGGLES=OFFSET+16,PREVIOUS_LENGTH=OFFSET+36,BETA2_LENGTH=OFFSET+53;
    public static final int HIGHLIGHT_DURATION=OFFSET+36,PLAYER_IMPACT_INVERT=OFFSET+37,HIGHLIGHT_FADE_OUT=OFFSET+53;
    public static final int HIGHLIGHT_PERFECT_ONLY=OFFSET+54,HIGHLIGHT_AFTER=OFFSET+55,HIGHLIGHT_ATTACKER_COLOR=OFFSET+56,HIGHLIGHT_TARGET_COLOR=OFFSET+57;
    public static final int FLASH_REGULAR_STRENGTH=OFFSET+58,SHAKE_REGULAR_STRENGTH=OFFSET+59,SHAKE_PERFECT_DURATION=OFFSET+60,SHAKE_REGULAR_DURATION=OFFSET+61,SHAKE_PERFECT_ONLY=OFFSET+62,CHROMATIC_REGULAR_DURATION=OFFSET+63,BETA5_LENGTH=OFFSET+64;
    public static final int HIGHLIGHT_ATTACKER_ENABLED=OFFSET+64,HIGHLIGHT_TARGET_ENABLED=OFFSET+65,BETA7_LENGTH=OFFSET+66;
    public static final int ICON_ANCHOR=OFFSET+66,ICON_X=OFFSET+67,ICON_Y=OFFSET+68,ICON_GAP=OFFSET+69,V155H_LENGTH=OFFSET+70,ICON_SIZE=OFFSET+70,LENGTH=OFFSET+71;
    private record Control(String key,String label,int initial,int min,int max,String unit,String tip,boolean live) {}
    private static final Control[] CONTROLS=new Control[LENGTH-OFFSET];
    private static final ModConfigSpec.IntValue[] VALUES=new ModConfigSpec.IntValue[CONTROLS.length];
    static {
        Arrays.fill(CONTROLS,new Control(null,"Retired screen setting",0,0,0,"","",false));
        retired(0,"cameraShakeEnabled");
        retired(1,"chromaticEnabled");
        retired(2,"perfectHitlagEnabled");
        retired(3,"regularHitlagEnabled");
        retired(4,"incomingHitlagEnabled");
        retired(5,"incomingRegularFrames");
        retired(6,"incomingPerfectFrames");
        retired(7,"regularFlashMillis");
        retired(8,"perfectFlashMillis");
        retired(9,"vignetteEnabled");
        retired(10,"vignetteOpacity");
        retired(11,"perfectHighlightEnabled");
        retired(12,"perfectHighlightDimming");
        retired(13,"perfectHighlightBrightness");
        retired(14,"perfectHighlightGlow");
        retired(15,"perfectHighlightFadeMillis");
        retired(36,"perfectHighlightDurationMillis");
        retired(37,"playerImpactInvert");
        retired(38,"incomingFovEnabled");
        retired(39,"incomingFovStrength");
        retired(40,"incomingFovMillis");
        retired(41,"incomingShakeEnabled");
        retired(42,"incomingShakeStrength");
        retired(43,"incomingShakeMillis");
        retired(44,"incomingVignetteEnabled");
        retired(45,"incomingVignetteOpacity");
        retired(46,"incomingVignetteMillis");
        retired(47,"incomingFlashEnabled");
        retired(48,"incomingFlashOpacity");
        retired(49,"incomingFlashMillis");
        retired(50,"incomingChromaticEnabled");
        retired(51,"incomingChromaticStrength");
        retired(52,"incomingChromaticTicks");
        control(0,"cameraShakeEnabled","Camera Shake",1,0,1,"","Briefly shakes your view during parry feedback. Strength and duration control the motion.");
        control(1,"chromaticEnabled","Chromatic Aberration",1,0,1,"","Slightly separates red, green and blue channels, creating a brief color fringe around edges.");
        control(2,"hitlagEnabled","Hitlag",1,0,1,"","Briefly holds your view on the impact to emphasize the parry. Only your view freezes; server combat continues.");
        control(3,"hitlagPerfectOnly","Perfect Only",0,0,1,"","Hitlag only on perfect parries. Regular duration is kept while hidden.");
        control(11,"targetHighlightEnabled","Target Highlight",0,0,1,"","Highlight the opposing participant and dim the world. Only the two participants see it.");
        control(12,"targetHighlightDim","World Dim",60,0,100,"%","World darkening. The opponent silhouette and HUD retain their brightness.");
        control(14,"targetHighlightGlow","Glow Strength",80,0,100,"%","Color strength inside the opponent silhouette. Texture transparency is respected.");
        control(15,"targetHighlightFadeInTicks","Fade In",2,0,40," ticks","How quickly the highlight and world dim appear. 0 appears immediately; 20 ticks = one second.");
        control(36,"targetHighlightTicks","Duration",6,1,80," ticks","Total highlight lifetime, including fades. Independent of hitlag. 20 ticks = one second.");
        control(37,"playerImpactInvert","Invert",0,0,1,"","Invert your impact frame. Configured in the appearance preview.");
        control(53,"targetHighlightFadeOutTicks","Fade Out",2,0,40," ticks","Time to fade before the effect ends. 0 ends immediately.");
        control(54,"targetHighlightPerfectOnly","Perfect Only",1,0,1,"","Highlight only perfect parries. Off includes regular parries.");
        control(55,"targetHighlightAfterHitlag","Timing",0,0,1,"","Before starts with the parry feedback and uses its own duration. After starts two ticks after hitlag and White Flash end.");
        control(56,"attackerHighlightColor","Attacker Highlight Color",16777215,0,16777215,"","Opponent color when you parry their attack.");
        control(57,"targetHighlightColor","Target Highlight Color",16711680,0,16777215,"","Opponent color when they parry your attack.");
        control(58,"regularFlashIntensity","Regular Intensity",82,0,100,"%","How visible the brief white flash is on a regular parry.");
        control(59,"regularShakeStrength","Regular Strength",55,0,100,"%","Camera shake strength after a regular parry.");
        control(60,"perfectShakeTicks","Perfect Duration",6,0,40," ticks","Perfect-parry camera shake lifetime. 0 disables it.");
        control(61,"regularShakeTicks","Regular Duration",6,0,40," ticks","Regular-parry camera shake lifetime. 0 disables it.");
        control(62,"shakePerfectOnly","Perfect Only",0,0,1,"","Only perfect parries shake the camera. Regular values are kept while hidden.");
        control(63,"regularChromaticTicks","Regular Duration",6,1,40," ticks","Regular-parry RGB separation lifetime. Independent of flash timing.");
        control(64,"attackerHighlightEnabled","Attacker Highlight",1,0,1,"","When you parry an attack, enable the opponent highlight and world dim. Off skips this whole effect for that event.");
        control(65,"targetParticipantHighlightEnabled","Target Highlight",1,0,1,"","When your attack is parried, enable the opponent highlight and world dim. Off skips this whole effect for that event.");
        control(66,"iconAnchor","Icon Position",6,0,9,"","Choose an anchor or drag the icon. Center retains the original crosshair position.");
        control(67,"iconX","Horizontal Position",5000,0,10000,"","Custom position as a fraction of the available screen width.");
        control(68,"iconY","Vertical Position",5000,0,10000,"","Custom position as a fraction of the available screen height.");
        control(69,"iconGap","HUD Gap",6,0,80," px","Space from screen edges or neighboring HUD elements. Custom positions ignore this gap.");
        control(70,"iconSize","Icon Size",100,50,200,"%","Size of the shield and its reactions. 100% is the original 18 GUI pixels.");
        for(int r=0;r<5;r++)for(int c=0;c<4;c++)control(16+r*4+c,"icon"+r+"Component"+c,GuardShieldReactions.NAMES[r]+" "+new String[]{r==4?"Lift":"Expansion","Shake","Highlight","Echo"}[c],c==0&&r<3?0:1,0,1,"","Enable this icon component while preserving its strength.");
    }
    private static void retired(int i,String key){CONTROLS[i]=new Control(key,"Retired screen setting",0,0,0,"","",false);}
    private static void control(int i,String key,String label,int initial,int min,int max,String unit,String tip){CONTROLS[i]=new Control(key,label,initial,min,max,unit,tip,true);}
    private GuardScreenSettings() {}
    public static boolean slot(int i){return i>=OFFSET&&i<LENGTH;}
    public static boolean retired(int i){return slot(i)&&!used(i);}
    public static int min(int i){return CONTROLS[i-OFFSET].min();}
    public static int max(int i){return CONTROLS[i-OFFSET].max();}
    public static int initial(int i){return CONTROLS[i-OFFSET].initial();}
    public static String key(int i){String key=CONTROLS[i-OFFSET].key();return key==null?null:"display."+key;}
    public static String label(int i){return CONTROLS[i-OFFSET].label();}
    public static String unit(int i){return CONTROLS[i-OFFSET].unit();}
    public static String tip(int i){return CONTROLS[i-OFFSET].tip();}
    public static int category(int i){return i>=ICON_ANCHOR||i>=REACTION_TOGGLES&&i<PREVIOUS_LENGTH?GuardClientPreset.HUD:GuardClientPreset.SCREEN;}
    private static boolean used(int i){return CONTROLS[i-OFFSET].live()&&(i<REACTION_TOGGLES||i>=PREVIOUS_LENGTH||GuardShieldReactions.used((i-REACTION_TOGGLES)/4*6+(i-REACTION_TOGGLES)%4+1));}
    public static ModConfigSpec.IntValue setting(int i){return VALUES[i-OFFSET];}
    public static int get(int i){return used(i)?VALUES[i-OFFSET].get():initial(i);}
    public static boolean enabled(int i){return get(i)!=0;}
    public static boolean component(int reaction,int control){return enabled(REACTION_TOGGLES+reaction*4+control-1);}
    public static void define(ModConfigSpec.Builder builder){for(int i=OFFSET;i<LENGTH;i++)if(used(i))VALUES[i-OFFSET]=GuardSettingRanges.integer(builder.comment(tip(i)),CONTROLS[i-OFFSET].key(),initial(i),min(i),max(i));}
    public static int[] append(int[] old,boolean local){int[] values=Arrays.copyOf(old,LENGTH);for(int i=OFFSET;i<LENGTH;i++)values[i]=local?get(i):initial(i);return values;}
    public static void apply(int[] values,int category){for(int i=OFFSET;i<LENGTH;i++)if(used(i)&&category(i)==category)VALUES[i-OFFSET].set(values[i]);}
    private static void legacy(Config c,int[] a,String key,int slot,int divisor){Object raw=c.get("display."+key);if(c.get(key(slot))==null&&raw instanceof Number n)a[slot]=Math.clamp((int)Math.round(n.doubleValue()/divisor),min(slot),max(slot));}
    public static void read(Config config,int[] values){
        for(int i=OFFSET;i<LENGTH;i++){if(!used(i))continue;Object raw=config.get(key(i));if(raw instanceof Number n&&n.intValue()>=min(i)&&n.intValue()<=max(i))values[i]=n.intValue();}
        legacy(config,values,"perfectHighlightDimming",HIGHLIGHT_DIM,1);
        legacy(config,values,"perfectHighlightGlow",HIGHLIGHT_GLOW,1);
        legacy(config,values,"perfectHighlightFadeMillis",HIGHLIGHT_FADE,50);
        legacy(config,values,"perfectHighlightDurationMillis",HIGHLIGHT_DURATION,50);
        if(config.get(key(HITLAG_ENABLED))==null&&config.get("display.perfectHitlagEnabled") instanceof Number n){Object regular=config.get("display.regularHitlagEnabled");values[HITLAG_ENABLED]=n.intValue()!=0||regular instanceof Number r&&r.intValue()!=0?1:0;}
        if(config.get(key(HITLAG_PERFECT_ONLY))==null&&config.get("display.regularHitlagEnabled") instanceof Number n)values[HITLAG_PERFECT_ONLY]=n.intValue()==0?1:0;
        if(config.get(key(HIGHLIGHT_ENABLED))==null&&config.get("display.perfectHighlightEnabled") instanceof Number n)values[HIGHLIGHT_ENABLED]=n.intValue()==0?0:1;
        if(config.get(key(CAMERA_ENABLED))==null&&Boolean.FALSE.equals(config.get("display.screenShake")))values[CAMERA_ENABLED]=0;
        legacy(config,values,"chromaticTicks",CHROMATIC_REGULAR_DURATION,1);
        if(config.get(key(FLASH_REGULAR_STRENGTH))==null&&config.get("display.flashStrength") instanceof Number n)values[FLASH_REGULAR_STRENGTH]=Math.clamp((int)Math.round(n.doubleValue()*0.82),0,100);
        if(config.get(key(SHAKE_REGULAR_STRENGTH))==null&&config.get("display.shakeStrength") instanceof Number n)values[SHAKE_REGULAR_STRENGTH]=Math.clamp((int)Math.round(n.doubleValue()*0.55),0,100);
        if(config.get("display.hitlagTicks")==null&&config.get("display.hitlagFrames") instanceof Number n)values[3]=Math.clamp((int)Math.round(n.doubleValue()/3),0,30);
        if(config.get("display.regularHitlagTicks")==null){Object raw=config.get("display.regularHitlagFrames");values[62]=raw instanceof Number n?Math.clamp((int)Math.round(n.doubleValue()/3),0,30):GuardClientSettings.legacyRegularHitlag(config);}
    }
    public static void migrateClient(byte[] original){
        // Read only the loaded client spec; Punchy's config loads separately.
        Config previous=new com.electronwill.nightconfig.toml.TomlParser().parse(new String(original,java.nio.charset.StandardCharsets.UTF_8));
        int[] values=append(new int[OFFSET],true);values[3]=GuardConfig.HITLAG_TICKS.get();values[62]=GuardConfig.REGULAR_HITLAG_TICKS.get();
        read(previous,values);apply(values,GuardClientPreset.SCREEN);GuardConfig.HITLAG_TICKS.set(values[3]);GuardConfig.REGULAR_HITLAG_TICKS.set(values[62]);
    }
    public static void write(Config config,int[] values){
        for(int i=OFFSET;i<LENGTH;i++){String key=key(i);if(key==null)continue;if(used(i))config.set(key,values[i]);else config.remove(key);}
        for(String key:new String[]{"perfectHitlagEnabled","regularHitlagEnabled","perfectHighlightEnabled","perfectHighlightDimming","perfectHighlightGlow","perfectHighlightFadeMillis","perfectHighlightDurationMillis","hitlagFrames","regularHitlagFrames"})config.remove("display."+key);
        config.remove("display.screenShake");
    }
    public static float envelope(long age,long duration,long fadeIn,long fadeOut){
        if(age<0||age>=duration)return 0;
        float in=fadeIn<=0?1F:Math.clamp(age/(float)fadeIn,0F,1F);
        float out=fadeOut<=0?1F:Math.clamp((duration-age)/(float)fadeOut,0F,1F);
        return Math.min(in,out);
    }
}
