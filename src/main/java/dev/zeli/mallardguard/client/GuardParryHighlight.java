package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import dev.zeli.mallardguard.GuardScreenSettings;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Participant-only mask reuses the opponent's normal model submission at capture time. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardParryHighlight {
    private static int opponent=-1;
    private static boolean pending,running,preview,after,perfect,incoming,maskReady,failed;
    private static long started,duration,fade,fadeOut,lastUsed,afterReadyAt;
    private static final long AFTER_DELAY_NANOS=2*50_000_000L;
    private static final long IDLE_NANOS=5_000_000_000L;
    private static final GuardRenderTargets.State maskState=new GuardRenderTargets.State(),screenState=new GuardRenderTargets.State();
    private static int color;
    private static TextureTarget mask,scratch;
    private static final Map<RenderType,MaskBatch> batches=new LinkedHashMap<>();
    private static final class MaskBatch {
        final ByteBufferBuilder allocator=new ByteBufferBuilder(16_384);
        BufferBuilder builder;
        BufferBuilder vertices(RenderType type){if(builder==null)builder=new BufferBuilder(allocator,type.mode(),type.format());return builder;}
        MeshData take(){BufferBuilder pending=builder;builder=null;return pending==null?null:pending.build();}
        void close(){builder=null;allocator.close();}
    }
    private GuardParryHighlight(){}
    static void prepare(int entityId,boolean perfect,boolean incoming){
        cancelEffect();GuardParryHighlight.perfect=perfect;GuardParryHighlight.incoming=incoming;opponent=entityId;pending=!failed&&entityId>=0&&enabled();
        duration=GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_DURATION)*50_000_000L;
        fade=GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_FADE)*50_000_000L;
        fadeOut=GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_FADE_OUT)*50_000_000L;
        color=GuardScreenSettings.get(incoming?GuardScreenSettings.HIGHLIGHT_TARGET_COLOR:GuardScreenSettings.HIGHLIGHT_ATTACKER_COLOR);
        after=GuardScreenSettings.enabled(GuardScreenSettings.HIGHLIGHT_AFTER);
        if(GuardDiagnostics.enabled(GuardDiagnostics.SCREEN_EFFECTS))GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Highlight prepared: enabled="+pending+", timing="+(after?"After":"Before")+", opponent="+opponent);
        if(pending)lastUsed=System.nanoTime();
    }
    static void beginPreview(int entityId){if(!preview)cancelEffect();preview=true;pending=running=false;opponent=entityId;lastUsed=System.nanoTime();}
    private static boolean enabled(){return GuardScreenSettings.enabled(GuardScreenSettings.HIGHLIGHT_ENABLED)
        &&GuardScreenSettings.enabled(incoming?GuardScreenSettings.HIGHLIGHT_TARGET_ENABLED:GuardScreenSettings.HIGHLIGHT_ATTACKER_ENABLED)
        &&(perfect||!GuardScreenSettings.enabled(GuardScreenSettings.HIGHLIGHT_PERFECT_ONLY))
        &&(GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_DIM)>0||GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_GLOW)>0);}
    /** One render-phase transition owns the clock, including its pending state. */
    static void advance(long now,boolean freezing){
        if(preview)return;
        if((pending||running)&&!enabled())cancelEffect();
        if(running&&now-started>=duration)cancelEffect();
        if(pending){
            if(after){
                if(freezing||GuardClient.flashPendingOrActive())afterReadyAt=0;
                else {
                    if(afterReadyAt==0)afterReadyAt=now+AFTER_DELAY_NANOS;
                    if(now>=afterReadyAt&&maskReady)start(now,duration);
                }
            }
            else if(maskReady)start(now,duration);
        }
        if(pending||running)lastUsed=now;
        else if(lastUsed!=0&&now-lastUsed>=IDLE_NANOS){dispose();lastUsed=0;}
    }
    private static void start(long now,long lifetime){
        pending=false;running=lifetime>0;started=now;duration=lifetime;
        if(GuardDiagnostics.enabled(GuardDiagnostics.SCREEN_EFFECTS))GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Highlight started: timing="+(after?"After":"Before")+", durationTicks="+lifetime/50_000_000L);
    }
    static boolean active(){return pending||running||preview;}
    static boolean needsCapture(){
        if(preview)return true;
        if(!pending&&!running||!enabled())return false;
        if(GuardHitlag.initialCapturePending())return !after;
        if(GuardHitlag.freezeActive())return false;
        return running||pending&&(!after||afterReadyAt!=0&&System.nanoTime()>=afterReadyAt&&!GuardClient.flashPendingOrActive());
    }
    public static MultiBufferSource capture(LivingEntity entity,MultiBufferSource original){
        if(!active()||entity.getId()!=opponent||!needsCapture())return original;
        var mc=Minecraft.getInstance();if(mc.level!=entity.level()||mc.player==null||mc.screen!=null&&!(preview&&mc.screen instanceof GuardHighlightPreviewScreen)||!mc.player.isAlive())return original;
        var screen=mc.getMainRenderTarget();if(screen.width<=0||screen.height<=0)return original;
        return type->type.format()==DefaultVertexFormat.NEW_ENTITY
            ?VertexMultiConsumer.create(original.getBuffer(type),batches.computeIfAbsent(type,ignored->new MaskBatch()).vertices(type)):original.getBuffer(type);
    }
    private static void ensureMask(RenderTarget screen){
        if(mask!=null&&mask.width==screen.width&&mask.height==screen.height)return;
        if(mask!=null)mask.destroyBuffers();
        mask=new TextureTarget(screen.width,screen.height,true,false);mask.setClearColor(0,0,0,0);maskReady=false;
        if(GuardDiagnostics.enabled(GuardDiagnostics.SCREEN_EFFECTS))GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Highlight mask allocated: "+screen.width+"x"+screen.height);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void captureMask(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        if(!needsCapture()){discardVertices();return;}
        var screen=Minecraft.getInstance().getMainRenderTarget();if(screen.width<=0||screen.height<=0)return;
        maskState.save();
        try{
            // Allocation belongs to this isolated pass, never the mob's renderer callback.
            ensureMask(screen);RenderSystem.disableScissor();RenderSystem.disableBlend();RenderSystem.colorMask(true,true,true,true);
            mask.clear(false);mask.copyDepthFrom(screen);mask.bindWrite(true);
            var iterator=batches.entrySet().iterator();
            while(iterator.hasNext()){
                var entry=iterator.next();MaskBatch batch=entry.getValue();
                if(batch.builder==null){batch.close();iterator.remove();continue;}
                drawMaskBatch(entry.getKey(),batch);
            }
            maskReady=true;lastUsed=System.nanoTime();
        }catch(RuntimeException error){
            if(!failed)org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Could not capture parry highlight",error);
            failed=true;cancelEffect();
        }finally{maskState.restore();}
    }
    private static void drawMaskBatch(RenderType type,MaskBatch batch){
        MeshData mesh=batch.take();if(mesh==null){batch.allocator.clear();return;}
        boolean handedOff=false;
        try{
            if(type.sortOnUpload())mesh.sortQuads(batch.allocator,RenderSystem.getVertexSorting());
            // Original textures and shaders preserve cutout alpha, including held-item sprites.
            type.setupRenderState();
            // Some materials select a different world target; this draw belongs to our mask.
            mask.bindWrite(true);handedOff=true;BufferUploader.drawWithShader(mesh);
        }finally{
            if(!handedOff)mesh.close();
            type.clearRenderState();batch.allocator.clear();
        }
    }
    static boolean draw(RenderTarget source,RenderTarget destination){
        if(!running||preview||!maskReady||!enabled())return false;
        float amount=GuardScreenSettings.envelope(System.nanoTime()-started,duration,fade,fadeOut);
        if(amount<=0)return false;
        return GuardImpactFrame.drawHighlight(source,mask,destination,amount,GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_DIM),GuardScreenSettings.get(GuardScreenSettings.HIGHLIGHT_GLOW),color);
    }
    static void drawCurrent(RenderTarget destination){
        if(!running||preview||!maskReady||!enabled())return;
        if(GuardScreenSettings.envelope(System.nanoTime()-started,duration,fade,fadeOut)<=0)return;
        screenState.save();
        try{
            if(scratch==null||scratch.width!=destination.width||scratch.height!=destination.height){if(scratch!=null)scratch.destroyBuffers();scratch=new TextureTarget(destination.width,destination.height,false,false);}
            GuardHitlag.copy(destination,scratch);draw(scratch,destination);
        }finally{screenState.restore();}
    }
    static boolean drawPreview(RenderTarget source,RenderTarget destination,float amount,int[] values){return GuardImpactFrame.drawHighlight(source,maskReady?mask:null,destination,amount,values[GuardScreenSettings.HIGHLIGHT_DIM],values[GuardScreenSettings.HIGHLIGHT_GLOW],values[GuardScreenSettings.HIGHLIGHT_ATTACKER_COLOR]);}
    private static void discardVertices(){
        for(MaskBatch batch:batches.values())if(batch.builder!=null){MeshData mesh=batch.take();if(mesh!=null)mesh.close();batch.allocator.clear();}
    }
    static void cancelEffect(){
        if(pending||running)GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Highlight ended or cancelled");
        pending=running=preview=maskReady=false;opponent=-1;started=afterReadyAt=0;discardVertices();
    }
    private static void releaseTargets(){if(mask!=null){mask.destroyBuffers();mask=null;}if(scratch!=null){scratch.destroyBuffers();scratch=null;}for(MaskBatch batch:batches.values())batch.close();batches.clear();}
    private static void dispose(){
        if(mask==null&&scratch==null&&batches.isEmpty())return;
        GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"Highlight render resources released");
        screenState.save();try{releaseTargets();}finally{screenState.restore();}
    }
    static void clear(){cancelEffect();dispose();failed=false;lastUsed=duration=fade=fadeOut=0;}
}
