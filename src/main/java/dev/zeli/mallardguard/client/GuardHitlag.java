package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardScreenSettings;
import dev.zeli.mallardguard.MallardGuard;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** One local freeze; its impact treatment never adds a second hold. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardHitlag {
    private static final long NANOS_PER_TICK=50_000_000L;
    private static final Queue<Runnable> afterFeedback=new ArrayDeque<>();
    private static TextureTarget captured;
    private static boolean capturePending,impactEnabled,incoming,perfect;
    private static final GuardRenderTargets.State captureState=new GuardRenderTargets.State();
    private static int opponent=-1;
    private static long started,duration,impactStarted,impactDuration;
    private static Runnable parried;
    private GuardHitlag() {}
    public static int ticks(boolean perfect){return !GuardScreenSettings.enabled(GuardScreenSettings.HITLAG_ENABLED)||!perfect&&GuardScreenSettings.enabled(GuardScreenSettings.HITLAG_PERFECT_ONLY)?0:(perfect?GuardConfig.HITLAG_TICKS:GuardConfig.REGULAR_HITLAG_TICKS).get();}
    public static void trigger(boolean perfect,int opponentId,Runnable recoil,Runnable feedback){start(perfect,opponentId,false,recoil,feedback);}
    public static void triggerMobCounter(boolean perfect,int opponentId,Runnable feedback){start(perfect,opponentId,true,()->{},feedback);}
    private static void start(boolean nextPerfect,int opponentId,boolean nextIncoming,Runnable recoil,Runnable feedback){
        GuardParryHighlight.cancelEffect();parried=null;capturePending=false;started=impactStarted=0;
        perfect=nextPerfect;opponent=opponentId;incoming=nextIncoming;
        duration=ticks(perfect)*NANOS_PER_TICK;
        impactEnabled=GuardConfig.IMPACT_FRAMES.get()!=0&&(perfect||!GuardConfig.IMPACT_PERFECT_ONLY.get());
        if(GuardDiagnostics.enabled(GuardDiagnostics.SCREEN_EFFECTS))GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Feedback started: perfect="+perfect+", hitlagTicks="+duration/NANOS_PER_TICK+", impact="+impactEnabled+", opponent="+opponent);
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null){finish();recoil.run();feedback.run();return;}
        GuardParryHighlight.prepare(opponent,perfect,incoming);
        if(duration==0){
            finish();
            impactDuration=Math.max(1,perfect||GuardConfig.REGULAR_HITLAG_TICKS.get()==0?GuardConfig.HITLAG_TICKS.get():GuardConfig.REGULAR_HITLAG_TICKS.get())*NANOS_PER_TICK;
            if(impactEnabled)impactStarted=System.nanoTime();
            recoil.run();feedback.run();return;
        }
        parried=recoil;capturePending=true;enqueue(feedback);PunchyGuardCompat.parryHitlagStarted();
    }
    static boolean initialCapturePending(){return capturePending;}
    public static boolean freezeActive(){return capturePending||started!=0;}
    public static void afterFreeze(Runnable action){if(freezeActive())enqueue(action);else action.run();}
    private static void enqueue(Runnable action){if(afterFeedback.size()>=256)afterFeedback.remove().run();afterFeedback.add(action);}
    private static void finish(){
        if((parried!=null||!afterFeedback.isEmpty())&&GuardDiagnostics.enabled(GuardDiagnostics.SCREEN_EFFECTS))GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Hitlag finished; releasing queued feedback and parried pose");
        Runnable action=parried;parried=null;if(action!=null)action.run();
        while(!afterFeedback.isEmpty())afterFeedback.remove().run();
    }
    public static void clear(){
        capturePending=impactEnabled=false;started=duration=impactStarted=impactDuration=0;parried=null;afterFeedback.clear();GuardParryHighlight.clear();
        if(captured!=null){captured.destroyBuffers();captured=null;}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void render(RenderGuiEvent.Pre event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.player.isAlive()){clear();return;}
        if(mc.screen!=null){capturePending=false;started=impactStarted=0;finish();if(!(mc.screen instanceof GuardHighlightPreviewScreen))GuardParryHighlight.cancelEffect();return;}
        RenderTarget screen=mc.getMainRenderTarget();if(screen.width<=0||screen.height<=0)return;
        long now=System.nanoTime();
        if(freezeActive()&&ticks(perfect)==0){capturePending=false;started=0;finish();}
        if(!freezeActive()){GuardParryHighlight.advance(now,false);GuardParryHighlight.drawCurrent(screen);drawLiveImpact(screen,now);return;}
        if(capturePending){
            captureState.save();
            try{
                if(captured==null||captured.width!=screen.width||captured.height!=screen.height){if(captured!=null)captured.destroyBuffers();captured=new TextureTarget(screen.width,screen.height,false,false);}
                copy(screen,captured);
            }finally{captureState.restore();}
            capturePending=false;started=now;
        }
        long age=now-started;
        if(age>=duration){started=0;finish();GuardParryHighlight.advance(now,false);GuardParryHighlight.drawCurrent(screen);return;}
        GuardParryHighlight.advance(now,true);
        // Highlight below the impact frame: the latter part of the same freeze uses the filter.
        if(impactEnabled&&GuardConfig.IMPACT_FRAMES.get()!=0&&(perfect||!GuardConfig.IMPACT_PERFECT_ONLY.get())&&age>=duration/2){if(!GuardImpactFrame.draw(captured,screen,incoming))copy(captured,screen);}
        else if(!GuardParryHighlight.draw(captured,screen))copy(captured,screen);
    }
    private static void drawLiveImpact(RenderTarget screen,long now){
        if(impactStarted==0)return;
        if(now-impactStarted>=impactDuration||GuardConfig.IMPACT_FRAMES.get()==0||!perfect&&GuardConfig.IMPACT_PERFECT_ONLY.get()){
            impactStarted=0;GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Live Impact Frame ended");return;
        }
        captureState.save();
        try{
            if(captured==null||captured.width!=screen.width||captured.height!=screen.height){if(captured!=null)captured.destroyBuffers();captured=new TextureTarget(screen.width,screen.height,false,false);}
            copy(screen,captured);GuardImpactFrame.draw(captured,screen,incoming);
        }finally{captureState.restore();}
    }
    static void copy(RenderTarget source,RenderTarget target){GuardRenderTargets.copy(source,target);}
}
