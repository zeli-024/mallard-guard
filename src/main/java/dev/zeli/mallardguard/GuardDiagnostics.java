package dev.zeli.mallardguard;

/** Optional client event logging; bounded suppression prevents repeated packet spam. */
public final class GuardDiagnostics {
    public static final int GUARD=1,DAMAGE=2,ANIMATION=4,CONFIG=8,MOB=16,PUNCHY_REACTION=32,PUNCHY_HANDS=64,PUNCHY_PRIORITY=128;
    public static final int PARTICLE_COLLISIONS=256;
    public static final int SCREEN_EFFECTS=512,PREDICTION=1024,PARTICLE_SPAWNS=2048,ASSIGNMENTS=4096;
    public static final int DEFAULT_EVENT_GROUPS=255|SCREEN_EFFECTS|PREDICTION|PARTICLE_SPAWNS|ASSIGNMENTS, ALL=8191;
    public record Channel(int flag,String name,String category,String description,boolean profiling) {}
    private static final java.util.List<Channel> CHANNELS=java.util.List.of(
        new Channel(GUARD,"Guard Events","Combat","Guard input and server-confirmed stance changes.",false),
        new Channel(DAMAGE,"Damage & Projectiles","Combat","Damage outcomes, sounds and projectile feedback.",false),
        new Channel(MOB,"Mob Guard","Combat","Incoming mob parry feedback.",false),
        new Channel(PREDICTION,"Prediction & Replies","Combat","Predicted guard, reply delay, stale replies and acknowledgement timeouts.",false),
        new Channel(ANIMATION,"Animations","Animations","Guard pose selection, timing and easing.",false),
        new Channel(PUNCHY_REACTION,"Punchy Reactions","Animations","Reaction starts, holds and hitlag recovery.",false),
        new Channel(PUNCHY_HANDS,"Punchy Hand Transitions","Animations","Pose release, supporting-hand lowering and visibility.",false),
        new Channel(PUNCHY_PRIORITY,"Punchy Item Priority","Animations","When native item use or another animation takes priority.",false),
        new Channel(SCREEN_EFFECTS,"Screen Effect Timing","Effects & Performance","Effect starts and ends, skipped effects, captures and resource cleanup.",false),
        new Channel(PARTICLE_SPAWNS,"Particle Bursts & Limits","Effects & Performance","Enabled families, requested counts and final shared-budget allocations.",false),
        new Channel(PARTICLE_COLLISIONS,"Particle Collision Profiling","Effects & Performance","Time collision queries; summaries every 100 game ticks. Adds profiling overhead.",true),
        new Channel(CONFIG,"Config & Sync","Config & Assignments","Config saves and synchronization.",false),
        new Channel(ASSIGNMENTS,"Assignment Changes","Config & Assignments","Assignment apply requests and rule counts; no per-item logs.",false)
    );
    public static java.util.List<Channel> channels(){return CHANNELS;}
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("Mallard Guard/Debug");
    private static final String[] LAST=new String[13];
    private static final long[] TIMES=new long[13];
    private GuardDiagnostics(){}
    public static boolean enabled(int group){return (GuardConfig.DEBUG_CLIENT_FLAGS.get()&group)!=0;}
    public static void event(int group,String message){
        if(!enabled(group))return;
        int index=Integer.numberOfTrailingZeros(group);if(index<0||index>=LAST.length)return;
        long now=System.nanoTime();
        if(message.equals(LAST[index])&&now-TIMES[index]<2_000_000_000L)return;
        LAST[index]=message;TIMES[index]=now;LOGGER.info("{}",message);
    }
    public static void clear(){java.util.Arrays.fill(LAST,null);java.util.Arrays.fill(TIMES,0);}
}
