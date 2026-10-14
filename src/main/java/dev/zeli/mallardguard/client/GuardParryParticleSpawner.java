package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardParticleConfig;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardSparkTracerConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardParticles;
import net.minecraft.client.Minecraft;

/** Context-aware bursts; all enabled effects share a favored side and bounded head budget. */
public final class GuardParryParticleSpawner {
    private GuardParryParticleSpawner() {}
    public static void clear() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            GuardParryParticleRenderer.clear(); GuardSparkTracerParticle.clear(); GuardImpactParticle.clear();
        }
    }
    private static void palette(net.minecraft.client.particle.Particle particle,GuardPackets.Palette colors){
        if(particle instanceof GuardImpactParticle impact)impact.setPalette(colors.start(),colors.middle(),colors.end());
        else if(particle instanceof GuardSparkTracerParticle tracer)tracer.setPalette(colors.start(),colors.middle(),colors.end());
        else if(particle instanceof GuardParryParticle spark)spark.setBaseColor(colors.flying(),colors.debris());
    }
    public static void spawn(GuardPackets.Sparks hit, GuardPackets.Palette playerPalette) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            var mc=Minecraft.getInstance();
            if(mc.level==null || !Float.isFinite(hit.damage()) || hit.damage()<0)return;
            boolean colored=hit.flyingColor()>=0&&hit.flyingColor()<=0xFFFFFF&&hit.debrisColor()>=0&&hit.debrisColor()<=0xFFFFFF&&hit.tracerStartColor()>=0&&hit.tracerStartColor()<=0xFFFFFF&&hit.tracerMiddleColor()>=0&&hit.tracerMiddleColor()<=0xFFFFFF&&hit.tracerEndColor()>=0&&hit.tracerEndColor()<=0xFFFFFF;
            GuardPackets.Palette colors = (playerPalette==null?GuardPackets.Palette.readLocal():playerPalette);
            if(colored)colors=new GuardPackets.Palette(hit.flyingColor(),hit.debrisColor(),hit.tracerStartColor(),hit.tracerMiddleColor(),hit.tracerEndColor());
            if(GuardImpactParticle.enabled()){
                var impact=mc.particleEngine.createParticle(GuardParticles.CENTER_IMPACT.get(),hit.x(),hit.y(),hit.z(),0,0,0);
                if(colors!=null)palette(impact,colors);
            }
            int flyingCount=GuardParryParticleMath.particleCount(GuardParticleConfig.FLYING_BASE_INTENSITY.get(),GuardParticleConfig.FLYING_PERFECT_INTENSITY.get(),hit.perfect(),hit.damage(),GuardParticleConfig.FLYING_DAMAGE_SCALING.get());
            int debrisCount=GuardParryParticleMath.particleCount(GuardParticleConfig.DEBRIS_BASE_INTENSITY.get(),GuardParticleConfig.DEBRIS_PERFECT_INTENSITY.get(),hit.perfect(),hit.damage(),GuardParticleConfig.DEBRIS_DAMAGE_SCALING.get());
            int tracerCount=GuardParryParticleMath.particleCount(GuardSparkTracerConfig.get(GuardSparkTracerConfig.BASEINTENSITY),GuardSparkTracerConfig.get(GuardSparkTracerConfig.PERFECTINTENSITY),hit.perfect(),hit.damage(),GuardSparkTracerConfig.get(GuardSparkTracerConfig.DAMAGESCALING));
            // Invisible families need neither physics nor a share of the visible head budget.
            if(GuardParticleColors.get(GuardParticleColors.FLYING_ENABLED)==0||GuardParticleColors.get(GuardParticleColors.FLYING_SIZE)==0)flyingCount=0;
            if(GuardParticleColors.get(GuardParticleColors.DEBRIS_ENABLED)==0||GuardParticleColors.get(GuardParticleColors.DEBRIS_SIZE)==0)debrisCount=0;
            if(GuardParticleColors.get(GuardParticleColors.TRACER_ENABLED)==0||GuardParticleColors.get(GuardParticleColors.TRACER_SIZE)==0)tracerCount=0;
            int budget=Math.min(GuardParticleConfig.MAX_BURST_HEADS,GuardParryParticleRenderer.availableCapacity(mc.level));
            int[] counts=GuardParryParticleMath.allocateCounts(budget,flyingCount,debrisCount,tracerCount);
            if(GuardDiagnostics.enabled(GuardDiagnostics.PARTICLE_SPAWNS))GuardDiagnostics.event(GuardDiagnostics.PARTICLE_SPAWNS,"Particle burst: perfect="+hit.perfect()+", requested="+flyingCount+"/"+debrisCount+"/"+tracerCount+", allocated="+counts[0]+"/"+counts[1]+"/"+counts[2]+", budget="+budget+", impact="+GuardImpactParticle.enabled());
            flyingCount=counts[0];debrisCount=counts[1];tracerCount=counts[2];
            int total=flyingCount+debrisCount+tracerCount;if(total==0)return;
            double debrisBias=GuardParticleConfig.DEBRIS_BIAS/100.0;
            double debrisSpeedScale=GuardParticleConfig.DEBRIS_SPEED/50.0;
            double debrisVariation=GuardParticleConfig.DEBRIS_MOTION_VARIANCE/100.0;
            double tracerBias=GuardSparkTracerConfig.get(GuardSparkTracerConfig.SPREADBIAS)/100.0;
            int tracerVariation=GuardSparkTracerConfig.get(GuardSparkTracerConfig.SPEEDVARIANCE);
            double tracerScale=2.5*GuardSparkTracerConfig.get(GuardSparkTracerConfig.LAUNCHSPEED)/100.0;
            var random=mc.level.random;
            // Keep a random heavier side, gently tilted upward; launches still cover the entire sphere.
            double ay=random.nextDouble()*.5,angle=random.nextDouble()*Math.PI*2,ar=Math.sqrt(Math.max(0,1-ay*ay));
            double ax=Math.cos(angle)*ar,az=Math.sin(angle)*ar;
            boolean forward=hit.shield()&&!hit.fall()&&GuardParticleColors.get(GuardParticleColors.SHIELD_FORWARD_BIAS)!=0;
            if(forward){
                double length=Math.hypot(hit.facingX(),hit.facingZ());
                if(length>1E-6){ax=hit.facingX()/length;az=hit.facingZ()/length;ay=.25;}
            }
            for(int i=0;i<total;i++) {
                boolean flying=i<flyingCount,debris=!flying&&i<flyingCount+debrisCount;
                double bias=forward?.7:flying?.4:debris?debrisBias:tracerBias;
                double dx,dy,dz;
                do {
                    dy=hit.fall()?(random.nextDouble()-.5)*.12:GuardParryParticleMath.launchHeight(random.nextDouble());angle=random.nextDouble()*Math.PI*2;
                    double horizontal=Math.sqrt(Math.max(0,1-dy*dy));
                    dx=Math.cos(angle)*horizontal;dz=Math.sin(angle)*horizontal;
                } while(!hit.fall()&&random.nextDouble()>GuardParryParticleMath.directionAcceptance(dx*ax+dy*ay+dz*az,bias));
                double variation=random.nextDouble(),mid=.18+1.27/3;
                double speed=flying?GuardParryParticleMath.flyingSparkLaunchSpeed(variation):
                    debris?(mid+debrisVariation*(GuardParryParticleMath.debrisLaunchSpeed(variation)-mid))*debrisSpeedScale:
                    mid*tracerScale*GuardParryParticleMath.varianceFactor(variation,tracerVariation);
                var particle=mc.particleEngine.createParticle(flying?GuardParticles.FLYING_SPARK.get():debris?GuardParticles.SPARK_DEBRIS.get():GuardParticles.SPARK_TRACER.get(),hit.x(),hit.y(),hit.z(),dx*speed,dy*speed,dz*speed);
                if(colors!=null && (colored||playerPalette!=null || !flying&&!debris))palette(particle,colors);
            }
        }
    }
}
