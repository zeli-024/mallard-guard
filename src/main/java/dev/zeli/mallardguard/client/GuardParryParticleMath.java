package dev.zeli.mallardguard.client;

/** Shared pure calculations for particle counts, spread, motion, reflection, and fading. */
public final class GuardParryParticleMath {
    private GuardParryParticleMath() {}
    /** Proportional counts with largest remainders; disabled effects cannot receive spare heads. */
    public static int[] allocateCounts(int budget,int... requested) {
        int[] counts=requested.clone();long total=0;
        for(int i=0;i<counts.length;i++){counts[i]=Math.max(0,counts[i]);total+=counts[i];}
        if(total<=Math.max(0,budget))return counts;
        budget=Math.max(0,budget);int used=0;long[] remainders=new long[counts.length];
        for(int i=0;i<counts.length;i++){long scaled=(long)counts[i]*budget;counts[i]=(int)(scaled/total);remainders[i]=scaled%total;used+=counts[i];}
        for(int n=used;n<budget;n++) {
            int best=0;for(int i=1;i<counts.length;i++)if(remainders[i]>remainders[best])best=i;
            counts[best]++;remainders[best]=-1;
        }
        return counts;
    }
    public static double varianceFactor(double sample,int percent) {
        return 1+(sample*2-1)*Math.clamp(percent,0,100)/100.0;
    }
    public static int scaledLifetime(int ticks,double factor) {
        return Math.max(1,(int)Math.round(ticks*factor));
    }
    public static float sizeScale(int age,int lifetime,int shrinkPercent) {
        float progress=Math.max(0,Math.min(1,age/(float)Math.max(1,lifetime)));
        return 1-Math.clamp(shrinkPercent,0,100)/100.0F*progress;
    }
    public static double velocityRetention(double lossPercent) { return 1-lossPercent/100.0; }
    public static double flyingSparkReflection(double velocity,double normal,double dot,double restitution) {
        return (velocity-2*dot*normal)*restitution;
    }
    public static int flyingSparkLifetime(int maximum,double variation,boolean lingering) {
        double factor=lingering?1.10+variation*0.25:0.50+variation*0.50;
        return Math.max(1,(int)Math.round(maximum*factor));
    }
    public static int flyingSparkFadeDuration(int baseline,int lifetime,double variation,boolean lingering) {
        double factor=lingering?1.50+variation:0.50+variation;
        return Math.min(lifetime,Math.max(1,(int)Math.round(baseline*factor)));
    }
    public static int particleCount(int intensity,int perfectIncrease,boolean perfect,double damage,int damageScaling){
        if(!Double.isFinite(damage)||damage<0||intensity<=0)return 0;
        double base=dev.zeli.mallardguard.GuardParticleConfig.MAX_BASE_COUNT*Math.clamp(intensity,0,100)/100.0;
        double extra=perfect?1+Math.clamp(perfectIncrease,0,dev.zeli.mallardguard.GuardParticleConfig.MAX_PERFECT_INTENSITY)/100.0:1;
        return (int)Math.min(dev.zeli.mallardguard.GuardParticleConfig.MAX_BURST_HEADS,Math.round(base*extra*(1+damage*Math.clamp(damageScaling,0,100)/100.0)));
    }
    public static double directionAcceptance(double dot,double bias) {
        return (1+bias*Math.clamp(dot,-1.0,1.0))/(1+bias);
    }
    /** Sample spherical height with density (1 + 0.8*y)/2: 70% up, 30% down before side weighting. */
    public static double launchHeight(double sample) {
        double bias=0.8;
        return (Math.sqrt((1-bias)*(1-bias)+4*bias*sample)-1)/bias;
    }
    public static double debrisLaunchSpeed(double variation) {
        return 0.18+variation*variation*1.27;
    }
    public static double debrisReflection(double velocity,double normal,double dot,double rebound,double surfaceRetention) {
        return (velocity-dot*normal)*surfaceRetention-dot*normal*rebound;
    }
    public static double flyingSparkLaunchSpeed(double variation) {
        return (0.07+variation*variation*1.23)*dev.zeli.mallardguard.GuardParticleConfig.SPEED_SCALE;
    }
    private static float clampOpacity(float value) { return Math.max(0,Math.min(1,value)); }
    public static float fadeOpacity(float time,float lifetime,float span) {
        if(span<=0)return time<lifetime?1:0;
        return clampOpacity((lifetime-time)/Math.min(span,lifetime));
    }
}
