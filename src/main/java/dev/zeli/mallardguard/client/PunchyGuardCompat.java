package dev.zeli.mallardguard.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardPoseSettings;
import dev.zeli.mallardguard.GuardPoseLibrary;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** Optional client pose bridge; animation never changes guard timing or server input. */
@EventBusSubscriber(modid = "mallardguard", value = Dist.CLIENT)
public final class PunchyGuardCompat {
    private static boolean checked, available;
    private static Object handler, ownedClip;
    private static Object[][] clips = new Object[0][2];
    private static Object[][] parriedClips = new Object[0][2];
    private static java.util.List<GuardPoseLibrary.Pose> library = java.util.List.of();
    private static long libraryGeneration = -1;
    private static final String[] poseKeys = new String[GuardPoseSettings.COUNT];
    private static boolean lockKey;
    private static Object policyKey;
    private static int[][] poses = new int[0][];
    private static InteractionHand ownedHand;
    private static WeakReference<LocalPlayer> owner = new WeakReference<>(null);
    private static int selected = -1, last = -1, previous = -1;
    private static int[] preview;
    private static Object previewRight, previewLeft;
    private static boolean livePreview;
    private static int[] previewParried;
    private static int previewParriedHoldMs;
    private static boolean previewSequence;
    private static long previewGuardUntil;
    private static boolean parriedActive,parriedPending;
    private static long parriedGeneration;
    private static long parriedHoldRemaining,parriedHoldClock;
    private static boolean previewMaintain=true,previewConsumed,spentGuard,automaticReturn,ownedMaintain=true;
    private static boolean lowerEmptyOffhand;
    private static net.minecraft.world.item.Item ownedItem, ownedSupportItem;
    private static WeakReference<LocalPlayer> loweredOwner = new WeakReference<>(null);
    private static boolean enteringEmptyOffhand;
    private static float emptyEntryLower=1.0F,emptyReturnLower;
    private static Method explicitTransform;
    private static java.lang.reflect.Field[] pausedCurrentFields, pausedPreviousFields;
    private static boolean interpolationPaused;
    private static float lastOwnedPartialTick=1.0F;
    private static boolean restartPreview,startingTransition;
    private static Method enabled, blacklisted, buildClip, currentClip, activeArm, looping;
    private static Method blendIn, blendOut, blendOutSlow, play, stop, clearClip, sourceHand, clearSourceHand;
    private static Method captureArm,resetOffhandVisual,blendOutActive,blendOutProgress;
    private static java.lang.reflect.Field blendSourcesActive;
    private static Method blendWeight, blendSamples, identitySample, boneForArm;
    private static java.lang.reflect.Field nativeReturnSources, blendSources, blendDuration, blendRemaining, previousBlendRemaining, preBlend;
    private static java.lang.reflect.Field returnDuration, returnRemaining, previousReturnRemaining;
    private static java.lang.reflect.Field offhandAwaitIn, offhandAwaitOut, lastOffhandEmpty, lastOffhandHeld;
    private static java.util.Map<?, ?> transitionSources = java.util.Map.of();
    private static float transitionWeight;
    private static boolean transitionSnapshot, transitionReturning;
    private static boolean activeDefaultEligible, activeEmpty;
    private static boolean reactionActive;
    private static float reactionX, reactionY, reactionZ;
    private static float reactionPitch, reactionYaw, reactionRoll;
    private static final float REACTION_SNAP_WEIGHT=0.65F;
    private static long reactionElapsed, reactionClock;
    private static boolean reactionRendered, reactionMissingArmLogged, reactionFreezeObserved, reactionFreezeLogged;
    private static float reactionPeak;
    private static final long REACTION_RISE_NANOS = 70_000_000L;
    private static final long REACTION_HOLD_NANOS = 100_000_000L;
    private static final long BLOCK_RECOVERY_NANOS = 140_000_000L;
    private static final float EMPTY_HAND_EXIT_SECONDS=0.18F;
    private static boolean reactionHoldLogged,emptyHandSlideLogged,emptyHandSlideRendered;
    private static float emptyHandSlidePeak;
    private static String lastPriorityReason="";

    private PunchyGuardCompat() {}

    public static float guardPlaybackSpeed(Object tickingHandler, float speed) {
        return tickingHandler == handler && shouldTakeOver() ? 1.0F : speed;
    }
    private static boolean initialize() {
        if (checked) return available;
        checked = true;
        if (!installed() || !dev.zeli.mallardguard.GuardPunchyApi.compatible()) return false;
        try {
            Class<?> manager = Class.forName("punchy.client.animation.PunchyAnimationManager");
            Class<?> pose = Class.forName("punchy.client.animation.PoseHandler");
            Class<?> clip = Class.forName("punchy.client.animation.data.AnimationClip");
            Class<?> config = Class.forName("punchy.config.PunchyConfig");
            handler = manager.getField("POSE_HANDLER").get(null);
            enabled = config.getMethod("isModEnabled");
            blacklisted = manager.getMethod("isAnyHandBlacklisted");
            buildClip = Class.forName("punchy.client.animation.io.PunchyAnimationLoader").getDeclaredMethod("buildClip", String.class, JsonObject.class);
            buildClip.setAccessible(true);
            currentClip = pose.getMethod("getCurrentClip");
            activeArm = pose.getMethod("setActiveArm", HumanoidArm.class);
            looping = pose.getMethod("setLooping", boolean.class);
            blendIn = pose.getMethod("setBlendInDurationNext", float.class);
            blendOut = pose.getMethod("setBlendOutDurationNext", float.class);
            blendOutSlow=pose.getMethod("setBlendOutSlowNext",float.class);
            clearClip=pose.getMethod("clearCurrentClipState");
            returnDuration=poseField(pose,"blendOutDuration");
            returnRemaining=poseField(pose,"blendRemaining");
            previousReturnRemaining=poseField(pose,"prevBlendRemaining");
            blendOutActive=pose.getMethod("isBlendOutActive");
            blendOutProgress=pose.getMethod("getBlendOutProgress",float.class);
            nativeReturnSources=poseField(pose,"blendFrom");
            play = pose.getMethod("playWithBlendFromCurrent", clip, float.class, boolean.class);
            stop = pose.getMethod("stopImmediateWithBlendOut");
            sourceHand = manager.getMethod("setSourceHand", Minecraft.class, InteractionHand.class);
            clearSourceHand = manager.getMethod("clearSourceHand");
            blendWeight = pose.getMethod("getCurrentClipBlendWeight", float.class);
            blendSources = poseField(pose, "blendInFrom");
            blendDuration = poseField(pose, "blendInDuration");
            blendRemaining = poseField(pose, "blendInRemaining");
            previousBlendRemaining = poseField(pose, "prevBlendInRemaining");
            preBlend = poseField(pose, "preBlendInActive");
            pausedCurrentFields=new java.lang.reflect.Field[]{poseField(pose,"currentTime"),poseField(pose,"expressionTime"),blendRemaining,poseField(pose,"blendInTime"),returnRemaining};
            pausedPreviousFields=new java.lang.reflect.Field[]{poseField(pose,"prevTime"),poseField(pose,"prevExpressionTime"),previousBlendRemaining,poseField(pose,"blendInPrevTime"),previousReturnRemaining};
            Class<?> sample = Class.forName("punchy.client.animation.PoseHandler$Sample");
            blendSamples = pose.getDeclaredMethod("blendBetweenSamples", sample, sample, float.class); blendSamples.setAccessible(true);
            boneForArm = pose.getDeclaredMethod("isBoneForArm", String.class, HumanoidArm.class); boneForArm.setAccessible(true);
            identitySample = sample.getDeclaredMethod("identity"); identitySample.setAccessible(true);
            explicitTransform=Class.forName("punchy.client.access.TransformablePart").getMethod("punchy$getExplicitTransform");
            captureArm=pose.getDeclaredMethod("captureCurrentArmSamplesInto",java.util.Map.class,HumanoidArm.class);captureArm.setAccessible(true);
            blendSourcesActive=poseField(pose,"blendInFromActive");
            Class<?> renderer=Class.forName("punchy.client.render.PunchyArmRenderer");
            resetOffhandVisual=renderer.getMethod("forceResetOffhandVisualState");
            offhandAwaitIn=poseField(renderer,"offhandAwaitHandInFrames");
            offhandAwaitOut=poseField(renderer,"offhandAwaitHandOutFrames");
            lastOffhandEmpty=poseField(renderer,"lastOffhandEmptyForLower");
            lastOffhandHeld=poseField(renderer,"lastOffhandHasItemForLower");
            available = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) { fail(exception); }
        return available;
    }
    private static java.lang.reflect.Field poseField(Class<?> type, String name) throws NoSuchFieldException {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    public static boolean installed() { return net.neoforged.fml.ModList.get().isLoaded("punchy"); }
    public static boolean supportedVersion() { return initialize(); }
    private static InteractionHand hand(LocalPlayer player) {
        if (preview != null) return player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return GuardClient.guardHand(player);
    }
    private static boolean otherHandEmpty,otherHandVisible,cachedOtherHandVisible,exiting;
    private static String activeItem="";
    private static boolean weaponVisible=true,cachedWeaponVisible=true;
    private static int[] ownedValues;
    private static boolean ownedOtherHandVisible;
    private static final float HIDDEN_HAND_Y=32.0F;
    private static int guardPhase;
    private static boolean releasePending, releaseHadHitlag;
    private static long releaseAtNanos;
    public static void correctGuardHand(){
        if(preview!=null)return;
        loweredOwner.clear();finishRelease();guardPhase=0;
    }
    public static void guardStatus(int phase){
        boolean entry=phase>=1&&phase<=3&&(guardPhase<1||guardPhase>3);
        guardPhase=phase;
        if(phase<1||phase>3)spentGuard=false;
        if(preview==null && entry && initialize()){
            // A new guard owns the animation immediately, including during the old hit reaction/return.
            clearReaction();clearParried();interpolationPaused=false;
            releasePending=releaseHadHitlag=exiting=automaticReturn=false;releaseAtNanos=0;
            selected=-1;spentGuard=false;restartPreview=true;
            beforePoseTick(handler);
        }
        if(available&&preview==null&&ownedClip!=null&&(phase<1||phase>3))release();
    }
    public static boolean beforeIntentFlush(){
        if(!initialize())return false;
        if(preview==null && GuardClient.attackBlocksGuard())return false;
        // Once guarding ends, a newer native attack may replace our reaction or return.
        if(preview==null&&(guardPhase<1||guardPhase>3||spentGuard))return false;
        if(ownedClip!=null&&!exiting&&!shouldTakeOver())release();
        return shouldTakeOver();
    }
    /** Scope the native visible-pose capture and easing to our transition only. */
    public static boolean captureVisibleStart(Object tickingHandler) {
        return tickingHandler == handler && startingTransition;
    }
    public static Float transitionEasing(Object tickingHandler, float progress) {
        if (tickingHandler != handler || ownedValues == null || ownedClip == null && !startingTransition) return null;
        try {
            if (!startingTransition && currentClip.invoke(handler) != ownedClip) return null;
            return (float) GuardPoseSettings.ease(progress, ownedValues[21], ownedValues[22]);
        } catch (ReflectiveOperationException | LinkageError error) { fail(error); return null; }
    }
    private static float nativeReturnElapsed(float partialTick) throws IllegalAccessException {
        float remaining=returnRemaining.getFloat(handler),previous=previousReturnRemaining.getFloat(handler);
        return Math.max(0.0F,returnDuration.getFloat(handler)-(previous+(remaining-previous)*Math.clamp(partialTick,0.0F,1.0F)));
    }
    public static Float returnEasing(Object tickingHandler,float partialTick){
        if(tickingHandler!=handler || !exiting || ownedValues==null)return null;
        try{
            if(currentClip.invoke(handler)!=ownedClip)return null;
            float linear=ownedValues[20]<=0?1.0F:Math.clamp(nativeReturnElapsed(partialTick)/(ownedValues[20]/1000.0F),0.0F,1.0F);
            return Math.clamp((float)GuardPoseSettings.ease(linear,ownedValues[21],ownedValues[22]),0.0F,1.0F);
        }catch(ReflectiveOperationException|LinkageError error){fail(error);return null;}
    }
    private static Object visibleTransitionSample(String key,Object raw) throws ReflectiveOperationException {
        if(transitionReturning){
            Object source=transitionSources.get(key);
            if(source==null)source=identitySample.invoke(null);
            return blendSamples.invoke(handler,identitySample.invoke(null),source,transitionWeight);
        }
        Object from=transitionSources.get(key);
        if(from==null)from=identitySample.invoke(null);
        return blendSamples.invoke(handler,from,raw,transitionWeight);
    }
    public static void preserveVisibleTransition(Object tickingHandler, java.util.Map<String, Object> samples, HumanoidArm arm) {
        if (tickingHandler != handler || !startingTransition || !transitionSnapshot) return;
        try {
            for (var entry : samples.entrySet()) {
                if (!(boolean)boneForArm.invoke(handler,entry.getKey(),arm)) continue;
                entry.setValue(visibleTransitionSample(entry.getKey(),entry.getValue()));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) { fail(error); }
    }
    private static void playTransition(Object clip, int[] values, float duration, HumanoidArm arm, boolean supportVisible) throws ReflectiveOperationException {
        if (currentClip.invoke(handler) != null) {
            transitionWeight = ((Number) blendWeight.invoke(handler, 1.0F)).floatValue();
            transitionSnapshot = transitionWeight < 1.0F;
            transitionReturning=(boolean)blendOutActive.invoke(handler);
            if (transitionSnapshot) transitionSources = new java.util.HashMap<>((java.util.Map<?, ?>)(transitionReturning?nativeReturnSources:blendSources).get(handler));
        }
        if(ownedClip==null){resetOffhandVisual.invoke(null);loweredOwner.clear();}
        LocalPlayer player=Minecraft.getInstance().player;
        String emptyOffhandBone=ownedClip==null && player!=null && player.getOffhandItem().isEmpty()
            && (arm!=player.getMainArm() || supportVisible)
            ? (player.getMainArm()==HumanoidArm.RIGHT?"left_arm":"right_arm") : null;
        if(emptyOffhandBone!=null){enteringEmptyOffhand=true;emptyEntryLower=1.0F;}
        else if(transitionReturning && player!=null && player.getOffhandItem().isEmpty() && (arm!=player.getMainArm() || supportVisible)){
            enteringEmptyOffhand=true;
            emptyEntryLower=returningEmptyHandLower(1.0F);
        }else enteringEmptyOffhand=false;
        String supportBone=arm==HumanoidArm.RIGHT?"left_arm":"right_arm";
        Object supportSource=null;
        if(supportVisible){
            java.util.Map<String,Object> visible=new java.util.HashMap<>();
            if(currentClip.invoke(handler)!=null && (ownedClip==null || ownedOtherHandVisible)){
                captureArm.invoke(handler,visible,arm.getOpposite());
                supportSource=visible.get(supportBone);
                if(supportSource!=null && transitionSnapshot)supportSource=visibleTransitionSample(supportBone,supportSource);
            }
            if(supportSource==null)supportSource=identitySample.invoke(null);
        }
        ownedValues = values.clone();
        blendIn.invoke(handler, duration);
        startingTransition = true;
        try {
            play.invoke(handler, clip, 1.0F, true);
            if (currentClip.invoke(handler) == clip) {
                // Set only our clip's transition clock; Punchy's other clips retain their timing.
                blendDuration.setFloat(handler, duration);
                blendRemaining.setFloat(handler, duration);
                previousBlendRemaining.setFloat(handler, duration);
                preBlend.setBoolean(handler, duration > 0.0F);
                if(supportSource!=null){
                    // Give the supporting hand its own visible source, including on release.
                    nativeSamples(blendSources).put(supportBone,supportSource);
                    blendSourcesActive.setBoolean(handler,true);
                }
                if(emptyOffhandBone!=null){
                    nativeSamples(blendSources).put(emptyOffhandBone,identitySample.invoke(null));
                    blendSourcesActive.setBoolean(handler,true);
                }
            }
        }
        finally { startingTransition = transitionSnapshot = transitionReturning = false; transitionSources = java.util.Map.of(); }
    }
    private static boolean defaultAnimationEligible(ItemStack item) {
        // Attack, mining and inspect animations do not prevent a guard pose.
        // Item-use poses remain native unless the player explicitly assigns the item.
        return !item.isEmpty() && item.getUseAnimation() == net.minecraft.world.item.UseAnim.NONE
            && dev.zeli.mallardguard.GuardItemRules.hasAttackDamage(item);
    }
    private static void priorityDiagnostic(String reason){
        if(reason.equals(lastPriorityReason))return;
        lastPriorityReason=reason;
        GuardDiagnostics.event(GuardDiagnostics.PUNCHY_PRIORITY,"Punchy priority: "+reason);
    }
    public static boolean shouldTakeOver(){
        LocalPlayer player=Minecraft.getInstance().player;
        if(player==null||!player.isAlive()||!initialize())return false;
        if(preview==null && GuardClient.attackBlocksGuard())return false;
        if(preview==null&&player.isUsingItem()){
            if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_PRIORITY))priorityDiagnostic(player.getUsedItemHand()==InteractionHand.MAIN_HAND?"native main-hand item use":"native offhand item use");return false;
        }
        if(parriedPending)return owner.get()==player&&stillOwnsClip()&&releaseAssignmentMatches(player)&&GuardConfig.PUNCHY_COMPAT.get();
        if(parriedActive)return owner.get()==player && releaseAssignmentMatches(player) && (preview!=null || GuardConfig.PUNCHY_COMPAT.get()) && stillOwnsClip();
        if(reactionActive)return owner.get()==player && releaseAssignmentMatches(player) && (preview!=null || GuardConfig.PUNCHY_COMPAT.get());
        if(releasePending) return owner.get()==player && GuardConfig.PUNCHY_COMPAT.get() && releaseAssignmentMatches(player);
        if(exiting){try{return !player.isUsingItem()&&owner.get()==player&&releaseAssignmentMatches(player)&&(currentClip.invoke(handler)==ownedClip&&!returnFinished());}catch(ReflectiveOperationException|LinkageError error){fail(error);return false;}}
        if(preview!=null&&previewConsumed)return false;
        if(preview==null&&(spentGuard||!GuardConfig.PUNCHY_COMPAT.get()||guardPhase<1||guardPhase>3))return false;
        InteractionHand hand=hand(player);if(hand==null)return false;
        ItemStack item=player.getItemInHand(hand);
        activeItem=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        otherHandEmpty=player.getItemInHand(other).isEmpty();
        otherHandVisible=preview!=null||otherHandEmpty||dev.zeli.mallardguard.GuardState.eligible(player,other);
        refreshSettings();
        activeDefaultEligible=defaultAnimationEligible(item);activeEmpty=item.isEmpty();
        if(preview==null){boolean any=false;for(int i=0;i<poses.length;i++)if(poses[i][0]!=0&&matches(i)){any=true;break;}if(!any){if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_PRIORITY))priorityDiagnostic("no enabled preset matches the guarding item");return false;}}
        try{
            boolean allowed=(boolean)enabled.invoke(null)&&!(boolean)blacklisted.invoke(null);
            if(allowed)lastPriorityReason="";
            else if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_PRIORITY))priorityDiagnostic("Punchy is disabled or a held item is blacklisted");
            return allowed;
        }
        catch(ReflectiveOperationException|LinkageError error){fail(error);return false;}
    }
    public static void beforePoseTick(Object tickingHandler){
        if(!initialize()||tickingHandler!=handler)return;
        if(parriedPending){if(!stillOwnsClip())clearParried();else return;}
        if(parriedActive){advanceParried();return;}
        if(reactionActive&&!exiting){advanceReaction();return;}
        if(releasePending){
            if(preview==null && guardPhase>=1 && guardPhase<=3 && !spentGuard){releasePending=releaseHadHitlag=false;releaseAtNanos=0;}
            else {advanceRelease();return;}
        }
        if(exiting){
            boolean resume=false;
            if((!automaticReturn||preview==null&&!spentGuard)&&(preview!=null||guardPhase>=1&&guardPhase<=3&&!spentGuard)){exiting=false;resume=shouldTakeOver();exiting=!resume;}
            if(!resume){advanceReturn();return;}
            ownedMaintain=true;automaticReturn=false;lowerEmptyOffhand=false;restartPreview=true;
        }
        if(!shouldTakeOver()){
            release();
            return;
        }
        if(previewSequence && ownedClip!=null && !restartPreview){
            try{
                if(transitionFinished()){
                    if(previewGuardUntil==0)previewGuardUntil=System.nanoTime()+250_000_000L;
                    if(System.nanoTime()>=previewGuardUntil){previewSequence=false;startParried(previewParried,previewParriedHoldMs,false);return;}
                }
            }catch(ReflectiveOperationException|LinkageError error){fail(error);return;}
        }
        if(ownedClip!=null&&!ownedMaintain&&!restartPreview&&!(preview!=null&&livePreview)){
            try{if(currentClip.invoke(handler)==ownedClip&&transitionFinished()){if(preview!=null)previewConsumed=true;else spentGuard=true;release();automaticReturn=exiting;return;}}
            catch(ReflectiveOperationException|LinkageError error){fail(error);return;}
        }
        LocalPlayer player=Minecraft.getInstance().player;InteractionHand hand=hand(player);
        HumanoidArm arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        try{
            GuardPoseLibrary.Pose assignment=null;
            if(preview==null){if(selected<0||selected>=poses.length||poses[selected][0]==0||!matches(selected))selected=choose(player);if(selected<0){release();return;}assignment=library.get(selected);}
            InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            ItemStack support=player.getItemInHand(other);
            if(preview==null&&assignment!=null){otherHandVisible=(!assignment.otherHandHeldOnly||!otherHandEmpty)&&(otherHandEmpty?otherHandVisible:assignment.supporting(support,otherHandVisible));}
            if (preview == null && !support.isEmpty() && support.getUseAnimation() != net.minecraft.world.item.UseAnim.NONE
                && (assignment == null || !assignment.supporting(support,false))) otherHandVisible = false;
            weaponVisible=preview!=null||assignment==null||assignment.weapon(player.getItemInHand(hand));
            if(cachedWeaponVisible!=weaponVisible){invalidateClips();cachedWeaponVisible=weaponVisible;}
            if(cachedOtherHandVisible!=otherHandVisible){invalidateClips();cachedOtherHandVisible=otherHandVisible;}
            if(preview==null){
                if(clips[selected][0]==null){clips[selected][0]=makeClip(poses[selected],selected,false);clips[selected][1]=makeClip(poses[selected],selected,true);}
            }else if(previewRight==null){previewRight=makeClip(preview,0,false);previewLeft=makeClip(preview,0,true);}
            Object clip=preview!=null?(arm==HumanoidArm.RIGHT?previewRight:previewLeft):clips[selected][arm==HumanoidArm.RIGHT?0:1];
            if(ownedClip!=clip||ownedHand!=hand||owner.get()!=player||currentClip.invoke(handler)!=clip||restartPreview){
                int[] values=preview!=null?preview:poses[selected];
                if(preview==null){
                    clearReaction();releasePending=releaseHadHitlag=exiting=automaticReturn=false;releaseAtNanos=0;
                }
                sourceHand.invoke(null,Minecraft.getInstance(),hand);activeArm.invoke(handler,arm);looping.invoke(handler,false);
                // Start now, then blend for the configured entry time. Placement previews stay instant.
                playTransition(clip,values,livePreview?0.0F:values[19]/1000.0F,arm,otherHandVisible);
                if(dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.ANIMATION))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.ANIMATION,"Punchy pose started: hand="+hand+", preview="+(preview!=null)+", enterMs="+values[19]+", returnMs="+values[20]+", easing="+values[21]+", strength="+values[22]);ownedClip=clip;ownedHand=hand;ownedItem=player.getItemInHand(hand).getItem();ownedSupportItem=support.getItem();owner=new WeakReference<>(player);ownedMaintain=preview!=null?(livePreview||previewMaintain):assignment.maintainHeld;ownedOtherHandVisible=otherHandVisible;restartPreview=false;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);}
    }
    private static void refreshSettings(){
        boolean locked=GuardClient.clientCategoryLocked(dev.zeli.mallardguard.GuardClientPreset.PUNCHY);
        boolean changed=policyKey!=GuardClient.enforcedAnimations()||libraryGeneration!=GuardPoseLibrary.generation()||lockKey!=locked;
        for(int i=0;i < GuardPoseSettings.COUNT;i++)if(!GuardConfig.PUNCHY_POSES.get(i).get().equals(poseKeys[i]))changed=true;
        if(!changed)return;
        lockKey=locked;policyKey=GuardClient.enforcedAnimations();
        var entries=locked?(GuardClient.enforcedAnimations().isEmpty()?java.util.stream.IntStream.range(0,GuardPoseSettings.COUNT).mapToObj(GuardPoseLibrary::preset).toList():GuardClient.enforcedAnimations()):GuardPoseLibrary.snapshot();
        library=locked?entries.stream().map(GuardPoseLibrary.Pose::copy).toList():entries;poses=new int[library.size()][];clips=new Object[library.size()][2];parriedClips=new Object[library.size()][2];
        for(int i=0;i<library.size();i++){var pose=library.get(i);int preset=pose.preset?Integer.parseInt(pose.id.substring(6))-1:-1;if(preset>=0)poseKeys[preset]=GuardConfig.PUNCHY_POSES.get(preset).get();
            int[] configured=preset>=0&&locked&&GuardClient.enforcedAnimations().isEmpty()?GuardPoseSettings.parsePose(poseKeys[preset]):null;
            if(configured!=null){pose.items.clear();pose.handMode=0;pose.parriedValues=GuardPoseSettings.defaultParriedPose(configured);}
            poses[i]=configured==null?pose.values.clone():configured;poses[i][0]=configured==null?(pose.enabled?1:0):configured[0];
        }
        libraryGeneration=GuardPoseLibrary.generation();selected=last=previous=-1;
    }
    private static int exclusiveOwner(ItemStack stack){for(int i=0;i<library.size();i++)if(library.get(i).exclusive(stack))return i;return -1;}
    private static int choose(LocalPlayer player){
        var held=hand(player);if(held==null)return -1;var stack=player.getItemInHand(held);int owner=exclusiveOwner(stack),count=0;
        boolean[] matching=new boolean[poses.length];
        for(int i=0;i<poses.length;i++){matching[i]=poses[i][0]!=0&&(owner<0||owner==i)&&library.get(i).matches(stack,activeDefaultEligible);if(matching[i])count++;}
        if(count==0)return -1;int candidates=0;
        for(int i=0;i<poses.length;i++)if(matching[i]&&(count<2||i!=last)&&(count<3||i!=previous))candidates++;
        int roll=player.getRandom().nextInt(candidates);
        for(int i=0;i<poses.length;i++)if(matching[i]&&(count<2||i!=last)&&(count<3||i!=previous)&&roll--==0){previous=last;last=i;return i;}
        return -1;
    }
    private static boolean matches(int i){
        return i>=0 && i<library.size() && presetMatches(i);
    }
    private static boolean presetMatches(int index){
        var player=Minecraft.getInstance().player;if(player==null)return false;var selectedHand=hand(player);if(selectedHand==null)return false;var stack=player.getItemInHand(selectedHand);
        int owner=exclusiveOwner(stack);if(owner>=0&&owner!=index)return false;
        return library.get(index).matches(stack,activeDefaultEligible);
    }
    private static boolean releaseAssignmentMatches(LocalPlayer player){
        // An empty-hand-only clip must not linger on a weapon after a slot change.
        if(preview!=null||selected<0||selected>=library.size()||ownedHand==null)return true;
        ItemStack item=player.getItemInHand(ownedHand);
        InteractionHand support=ownedHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        if(item.getItem()!=ownedItem || ownedOtherHandVisible && player.getItemInHand(support).getItem()!=ownedSupportItem)return false;
        boolean eligible=defaultAnimationEligible(item);
        return library.get(selected).matches(item,eligible);
    }
    private static Object makeClip(int[] values, int index, boolean left) throws ReflectiveOperationException {
        return makeClip(values,index,left,false);
    }
    private static Object makeClip(int[] values, int index, boolean left, boolean parried) throws ReflectiveOperationException {
        JsonObject animation = new JsonObject(), bones = new JsonObject(), timeline = new JsonObject();
        animation.addProperty("loop", "hold_on_last_frame");
        double duration = Math.max(0.05, values[19] / 1000.0);
        animation.addProperty("animation_length", duration);
        if(otherHandVisible){timeline.addProperty("0", "dual_handed"); animation.add("timeline", timeline);}
        bones.add(left ? "left_arm" : "right_arm", bone(values, 1, left));
        if(otherHandVisible)bones.add(left ? "right_arm" : "left_arm", bone(values, 7, left));
        if(weaponVisible)bones.add(left ? "itemgrip_left" : "itemgrip_right", bone(values, 13, left));
        animation.add("bones", bones);
        return buildClip.invoke(null, "mallardguard_" + (parried ? "parried_" : "pose_") + index + (left ? "_left" : "_right"), animation);
    }
    private static void invalidateClips(){
        for(Object[] pair:clips)Arrays.fill(pair,null);
        for(Object[] pair:parriedClips)Arrays.fill(pair,null);
        previewRight=previewLeft=null;
    }
    private static JsonObject bone(int[] values, int offset, boolean mirrored) {
        JsonObject bone = new JsonObject(), position = new JsonObject(), rotation = new JsonObject();
        JsonArray p = new JsonArray(), r = new JsonArray();
        for (int axis = 0; axis < 3; axis++) {
            p.add(values[offset + axis] / 10.0 * (mirrored && axis == 0 ? -1 : 1));
            r.add(values[offset + 3 + axis] * (mirrored && axis != 0 ? -1 : 1));
        }
        position.add("0", p); rotation.add("0", r);
        bone.add("position", position); bone.add("rotation", rotation); return bone;
    }

    public static boolean pauseOwnedAnimation(Object tickingHandler) {
        if(tickingHandler!=handler || ownedClip==null || preview!=null)return false;
        boolean hold=reactionActive&&!exiting;
        if(!GuardHitlag.freezeActive()&&!hold){interpolationPaused=false;return false;}
        try {
            if(currentClip.invoke(handler)!=ownedClip){interpolationPaused=false;return false;}
            pauseInterpolation();
            return true;
        } catch(ReflectiveOperationException | LinkageError error){fail(error);return false;}
    }
    private static void pauseInterpolation() throws IllegalAccessException {
        if(interpolationPaused)return;
        // Stop at the last rendered sample, not at either end of its tick interval.
        float partial=Math.clamp(lastOwnedPartialTick,0.0F,1.0F);
        for(int i=0;i<pausedCurrentFields.length;i++){
            float previous=pausedPreviousFields[i].getFloat(handler);
            float current=pausedCurrentFields[i].getFloat(handler);
            float held=previous+(current-previous)*partial;
            pausedCurrentFields[i].setFloat(handler,held);
            pausedPreviousFields[i].setFloat(handler,held);
        }
        interpolationPaused=true;
        if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy pose interpolation paused: partialTick="+partial);
    }

    /** A confirmed parry replaces the guard clip with its paired recoil pose. */
    public static Runnable prepareParried(boolean freeze){
        if(preview!=null||selected<0||selected>=library.size()||ownedClip==null){clearParried();return ()->{};}
        long generation=++parriedGeneration;int selection=selected;var pose=library.get(selected);Object clip=ownedClip;
        parriedPending=freeze;
        return ()->{if(generation!=parriedGeneration||selection!=selected||clip!=ownedClip||!stillOwnsClip())return;if(Minecraft.getInstance().screen!=null){parriedPending=false;return;}parriedPending=false;startParried(pose.parriedValues,pose.parriedHoldMs,freeze);};
    }
    private static void startParried(int[] values,int holdMs,boolean freeze){
        LocalPlayer player=owner.get();
        if(!available || ownedClip==null || ownedHand==null || values==null){reactionSkipped("no paired guard pose");return;}
        if(player==null || player!=Minecraft.getInstance().player || !player.isAlive() || player.isUsingItem()
            || preview==null&&!GuardConfig.PUNCHY_COMPAT.get() || !releaseAssignmentMatches(player)){reactionSkipped("guard pose no longer applies");return;}
        try{
            if(currentClip.invoke(handler)!=ownedClip){reactionSkipped("a newer animation owns the hands");return;}
            HumanoidArm arm=ownedHand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
            int side=arm==HumanoidArm.RIGHT?0:1;
            Object clip;
            if(preview!=null)clip=makeClip(values,0,side==1,true);
            else{
                if(parriedClips[selected][side]==null)parriedClips[selected][side]=makeClip(values,selected,side==1,true);
                clip=parriedClips[selected][side];
            }
            clearReaction();interpolationPaused=false;
            releasePending=exiting=automaticReturn=false;releaseAtNanos=0;
            releaseHadHitlag=freeze;parriedActive=true;spentGuard=preview==null;previewConsumed=false;
            parriedHoldRemaining=holdMs*1_000_000L;parriedHoldClock=0;
            playTransition(clip,values,values[19]/1000.0F,arm,ownedOtherHandVisible);
            ownedClip=clip;
            if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy parried pose started: hand="+ownedHand+", impactMs="+values[19]+", holdMs="+holdMs+", returnMs="+values[20]+", hitlag="+freeze);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);}
    }
    private static void advanceParried(){
        LocalPlayer player=owner.get();
        if(player==null || player!=Minecraft.getInstance().player || !player.isAlive() || player.isUsingItem()
            || preview==null&&!GuardConfig.PUNCHY_COMPAT.get() || !releaseAssignmentMatches(player)){finishRelease();return;}
        try{
            if(currentClip.invoke(handler)!=ownedClip){finishRelease();return;}
            if(exiting){advanceReturn();return;}
            if(preview==null && GuardHitlag.freezeActive()){parriedHoldClock=0;return;}
            if(!transitionFinished()){parriedHoldClock=0;return;}
            long now=System.nanoTime();
            if(parriedHoldClock!=0)parriedHoldRemaining=Math.max(0,parriedHoldRemaining-Math.max(0,now-parriedHoldClock));
            parriedHoldClock=now;
            if(parriedHoldRemaining>0)return;
            if(preview!=null)previewConsumed=true;
            releasePending=true;
            advanceRelease();
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);}
    }
    public static void blockHit() {
        LocalPlayer player=owner.get();
        if(player!=null && ownedHand!=null && GuardClient.shieldLike(player.getItemInHand(ownedHand))){
            reactionSkipped("shield blocking uses its native reaction");return;
        }
        startBlockReaction();
    }
    private static boolean animationLogging(){return GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_REACTION);}
    private static void reactionSkipped(String reason){
        if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction skipped: "+reason);
    }
    private static void startBlockReaction() {
        LocalPlayer player=owner.get();
        if(!available || ownedClip==null || ownedValues==null || ownedHand==null){reactionSkipped("no active guard pose");return;}
        if(player==null || player!=Minecraft.getInstance().player || !player.isAlive()){reactionSkipped("no active player");return;}
        if(player.isUsingItem()){reactionSkipped("native item-use animation has priority");return;}
        if(!releaseAssignmentMatches(player)){reactionSkipped("held item or pose assignment changed");return;}
        try {
            if(exiting && currentClip.invoke(handler)==ownedClip){
                clearClip.invoke(handler);exiting=automaticReturn=false;lowerEmptyOffhand=false;
                HumanoidArm arm=ownedHand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
                playTransition(ownedClip,ownedValues,0.0F,arm,ownedOtherHandVisible);
            }
            if(currentClip.invoke(handler)!=ownedClip){reactionSkipped("Punchy is playing a different clip");return;}
            if(reactionActive)clearReaction();
            reactionX=(player.getRandom().nextFloat()-0.5F)*0.9F;
            reactionY=(player.getRandom().nextFloat()-0.5F)*0.7F;
            reactionZ=2.4F+player.getRandom().nextFloat()*0.6F;
            // Pick one direction per impact; follow-through never changes that direction.
            reactionPitch=(player.getRandom().nextFloat()-0.5F)*10.0F;
            reactionYaw=(player.getRandom().nextFloat()-0.5F)*8.0F;
            reactionRoll=-reactionX*10.0F+(player.getRandom().nextFloat()-0.5F)*4.0F;
            pauseInterpolation();
            reactionActive=true;
            reactionElapsed=0;
            reactionClock=System.nanoTime();
            releasePending=false;releaseAtNanos=0;
            releaseHadHitlag=false;
            reactionFreezeObserved=false;
            if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy block reaction started: hand="+ownedHand+", offset="+reactionX+","+reactionY+","+reactionZ+", angleDegrees="+reactionPitch+","+reactionYaw+","+reactionRoll+", emptyHand="+player.getItemInHand(ownedHand).isEmpty()+", snapWeight="+REACTION_SNAP_WEIGHT);
        }catch(ReflectiveOperationException | RuntimeException | LinkageError error){fail(error);}
    }
    private static void advanceReaction() {
        LocalPlayer player=owner.get();
        if(player==null || player!=Minecraft.getInstance().player || !player.isAlive() || player.isUsingItem()
            || preview==null&&!GuardConfig.PUNCHY_COMPAT.get() || !releaseAssignmentMatches(player)){finishRelease();return;}
        try {
            if(currentClip.invoke(handler)!=ownedClip){finishRelease();return;}
            long now=System.nanoTime();
            long elapsed=Math.max(0,now-reactionClock);reactionClock=now;
            if(preview==null&&GuardHitlag.freezeActive()){reactionFreezeObserved=true;return;}
            if(reactionFreezeObserved&&!reactionFreezeLogged){
                reactionFreezeLogged=true;
                if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction resumed after hitlag: hand="+ownedHand);
            }
            reactionElapsed+=elapsed;
            if(reactionElapsed>=REACTION_RISE_NANOS&&!reactionHoldLogged){
                reactionHoldLogged=true;
                if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction hold started: hand="+ownedHand+", holdMs="+REACTION_HOLD_NANOS/1_000_000L);
            }
            if(reactionElapsed>=REACTION_RISE_NANOS+REACTION_HOLD_NANOS+BLOCK_RECOVERY_NANOS){
                clearReaction();
            }
        }catch(ReflectiveOperationException | RuntimeException | LinkageError error){fail(error);}
    }
    private static float reactionAmount(){
        if(!reactionActive)return 0;
        long elapsed=reactionElapsed;
        if(!exiting && !releasePending && !GuardHitlag.freezeActive())elapsed+=Math.max(0,System.nanoTime()-reactionClock);
        if(elapsed<=REACTION_RISE_NANOS){
            float t=Math.clamp((float)elapsed/REACTION_RISE_NANOS,0.0F,1.0F);
            float remaining=1.0F-t;
            // Snap first, then rapidly decelerate the remaining backward travel.
            return REACTION_SNAP_WEIGHT+(1.0F-REACTION_SNAP_WEIGHT)*(1.0F-remaining*remaining*remaining);
        }
        if(elapsed<=REACTION_RISE_NANOS+REACTION_HOLD_NANOS)return 1.0F;
        float t=Math.clamp(1.0F-(float)(elapsed-REACTION_RISE_NANOS-REACTION_HOLD_NANOS)/BLOCK_RECOVERY_NANOS,0.0F,1.0F);
        return t*t*(3.0F-2.0F*t);
    }
    private static void clearReaction(){
        if(reactionActive && animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction ended: hand="+ownedHand+", rendered="+reactionRendered+", peakWeight="+reactionPeak+", released="+exiting);
        reactionActive=false;reactionElapsed=reactionClock=0;interpolationPaused=false;
        reactionRendered=reactionMissingArmLogged=reactionFreezeObserved=reactionFreezeLogged=reactionHoldLogged=false;reactionPeak=0.0F;
    }

    public static void previewPose(int[] values,boolean maintain) {
        clearReaction();clearParried();previewSequence=false;previewParried=null;previewGuardUntil=0;
        releasePending=releaseHadHitlag=false;releaseAtNanos=0;previewMaintain=maintain;previewConsumed=false;automaticReturn=false;livePreview=false;preview=values.clone();previewRight=previewLeft=null;restartPreview=true;
    }
    public static void previewSequence(GuardPoseLibrary.Pose pose){
        previewPose(pose.values,true);
        previewParried=pose.parriedValues.clone();
        previewParriedHoldMs=pose.parriedHoldMs;
        previewSequence=true;
    }
    public static void previewMaintain(boolean maintain){previewMaintain=maintain;if(preview!=null){previewConsumed=false;ownedMaintain=livePreview||maintain;}}
    public static void livePreviewPose(int[] values) {
        clearReaction();clearParried();previewSequence=false;previewParried=null;previewGuardUntil=0;
        releasePending=releaseHadHitlag=false;releaseAtNanos=0;int[] held=values.clone();held[19]=0;
        if(preview!=null && java.util.Arrays.equals(preview,held))return;
        previewConsumed=false;automaticReturn=false;livePreview=true;preview=held;previewRight=previewLeft=null;
    }
    private static final int[][] NO_POSES=new int[0][];
    private static final Object[][] NO_CLIPS=new Object[0][2];
    public static void clearSession(){loweredOwner.clear();finishRelease();clearPreview();guardPhase=0;spentGuard=startingTransition=false;library=java.util.List.of();poses=NO_POSES;clips=parriedClips=NO_CLIPS;libraryGeneration=-1;policyKey=null;last=previous=-1;}
    public static void clearPreview(){if(preview!=null)finishRelease();preview=null;previewParried=null;previewParriedHoldMs=0;previewSequence=false;previewGuardUntil=0;previewRight=previewLeft=null;restartPreview=livePreview=false;previewConsumed=false;}
    public static void parryHitlagStarted(){if(ownedClip!=null&&!exiting){releaseHadHitlag=true;releaseAtNanos=0;}}
    public static boolean ownsHandVisibility(Object tickingHandler){return tickingHandler==handler&&ownsHandVisibility();}
    public static boolean ownsHandVisibility(){
        LocalPlayer player=Minecraft.getInstance().player;
        if(!available||ownedClip==null||player==null||!player.isAlive()||owner.get()!=player||player.isUsingItem()||!(ownedOtherHandVisible || ownedHand==InteractionHand.OFF_HAND && player.getOffhandItem().isEmpty()))return false;
        try{return currentClip.invoke(handler)==ownedClip;}catch(ReflectiveOperationException|LinkageError error){fail(error);return false;}
    }
    private static boolean stillOwnsClip(){
        try{return currentClip.invoke(handler)==ownedClip;}
        catch(ReflectiveOperationException|LinkageError error){fail(error);return false;}
    }
    private static void release(){
        if(parriedPending||parriedActive||reactionActive||exiting||releasePending)return;
        LocalPlayer player=owner.get();
        if(ownedClip!=null && player!=null && player.isAlive() && !player.isUsingItem() && releaseAssignmentMatches(player)){
            releasePending=true;releaseAtNanos=0;
            return;
        }
        beginRelease();
    }
    private static void advanceRelease(){
        LocalPlayer player=Minecraft.getInstance().player;
        if(player==null||!player.isAlive()||owner.get()!=player||player.isUsingItem()||preview==null&&!GuardConfig.PUNCHY_COMPAT.get()||!releaseAssignmentMatches(player)){finishRelease();return;}
        if(preview==null && guardPhase>=1 && guardPhase<=3 && !spentGuard){releasePending=releaseHadHitlag=false;releaseAtNanos=0;return;}
        if(preview==null && GuardHitlag.freezeActive()){releaseHadHitlag=true;releaseAtNanos=0;return;}
        long now=System.nanoTime();
        if(releaseAtNanos==0){
            int delay=releaseHadHitlag?GuardConfig.PUNCHY_RELEASE_DELAY.get():0;
            releaseAtNanos=now+delay*1_000_000L;
            if(reactionActive && animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction release scheduled: hand="+ownedHand+", afterHitlag="+releaseHadHitlag+", delayMs="+delay);
        }
        if(now>=releaseAtNanos){releasePending=releaseHadHitlag=false;releaseAtNanos=0;beginRelease();}
    }
    private static void beginRelease(){
        if(exiting)return;
        if(ownedClip==null){selected=-1;return;}
        try{
            LocalPlayer player=owner.get();
            if(player!=null&&player.isAlive()&&!player.isUsingItem()&&releaseAssignmentMatches(player)&&ownedValues!=null&&(ownedValues[20]>0 || player.getOffhandItem().isEmpty() && (ownedHand==InteractionHand.OFF_HAND || ownedOtherHandVisible))&&currentClip.invoke(handler)==ownedClip){
                lowerEmptyOffhand=player.getOffhandItem().isEmpty() && (ownedHand==InteractionHand.OFF_HAND || ownedOtherHandVisible);
                emptyReturnLower=enteringEmptyOffhand?emptyEntryLower*(1.0F-((Number)blendWeight.invoke(handler,1.0F)).floatValue()):0.0F;
                HumanoidArm arm=ownedHand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
                java.util.Map<String,Object> visible=new java.util.HashMap<>();
                captureArm.invoke(handler,visible,arm);
                if(ownedOtherHandVisible)captureArm.invoke(handler,visible,arm.getOpposite());
                float weight=((Number)blendWeight.invoke(handler,1.0F)).floatValue();
                var from=(java.util.Map<?,?>)blendSources.get(handler);
                if(weight<1.0F)for(var entry:visible.entrySet()){
                    Object source=from.get(entry.getKey());if(source==null)source=identitySample.invoke(null);
                    entry.setValue(blendSamples.invoke(handler,source,entry.getValue(),weight));
                }
                float returnSeconds=ownedValues[20]/1000.0F;
                blendOut.invoke(handler,returnSeconds+(lowerEmptyOffhand?EMPTY_HAND_EXIT_SECONDS:0.0F));
                emptyHandSlideLogged=emptyHandSlideRendered=false;emptyHandSlidePeak=0.0F;
                if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_HANDS))GuardDiagnostics.event(GuardDiagnostics.PUNCHY_HANDS,"Punchy hand return started: hand="+ownedHand+", returnMs="+ownedValues[20]+", emptyOffhand="+lowerEmptyOffhand+", slideMs="+(lowerEmptyOffhand?(int)(EMPTY_HAND_EXIT_SECONDS*1000):0));
                blendOutSlow.invoke(handler,1.0F);
                stop.invoke(handler);
                var target=nativeSamples(nativeReturnSources);
                target.clear();target.putAll(visible);
                interpolationPaused=false;
                exiting=true;
                if(reactionActive && animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction native return started: hand="+ownedHand+", returnMs="+ownedValues[20]+", lowerEmptyOffhand="+lowerEmptyOffhand);
                return;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);}
        finishRelease();
    }
    private static boolean transitionFinished() throws IllegalAccessException {
        return blendRemaining.getFloat(handler) <= 0.0F;
    }
    private static boolean returnFinished(){
        try{return currentClip.invoke(handler)!=ownedClip || !(boolean)blendOutActive.invoke(handler);}
        catch(ReflectiveOperationException|LinkageError error){fail(error);return true;}
    }
    private static void advanceReturn(){
        LocalPlayer player=owner.get();
        if(player==null || player!=Minecraft.getInstance().player || !player.isAlive() || player.isUsingItem() || !releaseAssignmentMatches(player)){finishRelease();return;}
        if(!returnFinished())return;
        if(lowerEmptyOffhand){
            loweredOwner=new WeakReference<>(player);
            if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_HANDS))GuardDiagnostics.event(GuardDiagnostics.PUNCHY_HANDS,"Punchy empty offhand exit completed: rendered="+emptyHandSlideRendered+", peakLowerWeight="+emptyHandSlidePeak+", visibilityReturnedToNative=true");
        }
        finishRelease();
    }
    public static void applyOwnedArmOffsets(Object tickingHandler,java.util.Map<String,net.minecraft.client.model.geom.ModelPart> parts,float partialTick){
        if(tickingHandler!=handler || ownedClip==null)return;
        LocalPlayer player=owner.get();
        if(player==null || player!=Minecraft.getInstance().player || player.isUsingItem())return;
        try{
            if(currentClip.invoke(handler)!=ownedClip)return;
            lastOwnedPartialTick=partialTick;
            float progress=exiting?((Number)blendOutProgress.invoke(handler,partialTick)).floatValue():0.0F;
            float amount=reactionAmount()*(exiting?1.0F-progress:1.0F);
            if(amount>0.0F){
                HumanoidArm arm=ownedHand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
                String bone=arm==HumanoidArm.RIGHT?"right_arm":"left_arm";
                var part=parts.get(bone);
                if(part!=null){
                    boolean explicit=applyReactionToArm(part,amount);
                    reactionPeak=Math.max(reactionPeak,amount);
                    if(!reactionRendered){
                        reactionRendered=true;
                        if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction arm offset applied: bone="+bone+", explicitTransform="+explicit+", weight="+amount+", offset="+(reactionX*amount)+","+(reactionY*amount)+","+(reactionZ*amount)+", angleDegrees="+(reactionPitch*amount)+","+(reactionYaw*amount)+","+(reactionRoll*amount));
                    }
                }else if(!reactionMissingArmLogged){
                    reactionMissingArmLogged=true;
                    if(animationLogging())GuardDiagnostics.event(GuardDiagnostics.PUNCHY_REACTION,"Punchy reaction arm missing: bone="+bone);
                }
            }
            if(player.getOffhandItem().isEmpty() && ownsHandVisibility()){
                float lower=0.0F;
                if(exiting && lowerEmptyOffhand){
                    lower=returningEmptyHandLower(partialTick);
                }else if(enteringEmptyOffhand){
                    lower=emptyEntryLower*(1.0F-((Number)blendWeight.invoke(handler,partialTick)).floatValue());
                }
                if(lower>0.0F){
                    var part=parts.get(player.getMainArm()==HumanoidArm.RIGHT?"left_arm":"right_arm");
                    if(part!=null){
                        boolean explicit=translateArm(part,0.0F,HIDDEN_HAND_Y*lower,0.0F);
                        if(exiting){
                            emptyHandSlideRendered=true;emptyHandSlidePeak=Math.max(emptyHandSlidePeak,lower);
                            if(!emptyHandSlideLogged && nativeReturnElapsed(partialTick)>=ownedValues[20]/1000.0F){
                                emptyHandSlideLogged=true;
                                if(GuardDiagnostics.enabled(GuardDiagnostics.PUNCHY_HANDS))GuardDiagnostics.event(GuardDiagnostics.PUNCHY_HANDS,"Punchy empty offhand slide started: explicitTransform="+explicit+", lowerWeight="+lower+", offsetY="+HIDDEN_HAND_Y*lower);
                            }
                        }
                    }
                }
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);}
    }
    private static float returningEmptyHandLower(float partialTick) throws IllegalAccessException {
        float elapsed=nativeReturnElapsed(partialTick);
        float baseSeconds=ownedValues[20]/1000.0F;
        float baseProgress=Math.clamp(elapsed/Math.max(baseSeconds,0.0001F),0.0F,1.0F);
        float slide=Math.clamp((elapsed-baseSeconds)/EMPTY_HAND_EXIT_SECONDS,0.0F,1.0F);
        return emptyReturnLower*(1.0F-baseProgress)+slide*slide*(3.0F-2.0F*slide);
    }
    private static boolean applyReactionToArm(net.minecraft.client.model.geom.ModelPart part,float amount) throws ReflectiveOperationException {
        org.joml.Matrix4f transform=(org.joml.Matrix4f)explicitTransform.invoke(part);
        float pitch=(float)Math.toRadians(reactionPitch*amount);
        float yaw=(float)Math.toRadians(reactionYaw*amount);
        float roll=(float)Math.toRadians(reactionRoll*amount);
        if(transform!=null){
            transform.translateLocal(reactionX*amount,reactionY*amount,reactionZ*amount);
            transform.rotateXYZ(pitch,yaw,roll);
        }else{
            part.x+=reactionX*amount;part.y+=reactionY*amount;part.z+=reactionZ*amount;
            part.xRot+=pitch;part.yRot+=yaw;part.zRot+=roll;
        }
        return transform!=null;
    }
    private static boolean translateArm(net.minecraft.client.model.geom.ModelPart part,float x,float y,float z) throws ReflectiveOperationException {
        if(part==null)return false;
        org.joml.Matrix4f transform=(org.joml.Matrix4f)explicitTransform.invoke(part);
        // Punchy renders the explicit transform when present, bypassing ModelPart positions.
        if(transform!=null)transform.translateLocal(x,y,z);
        else {part.x+=x;part.y+=y;part.z+=z;}
        return transform!=null;
    }
    public static void prepareOwnedHandVisibility(){
        if(!ownsHandVisibility())return;
        LocalPlayer player=owner.get();
        try{
            offhandAwaitIn.setInt(null,0);offhandAwaitOut.setInt(null,0);
            boolean empty=player.getOffhandItem().isEmpty();
            lastOffhandEmpty.setBoolean(null,empty);lastOffhandHeld.setBoolean(null,!empty);
        }catch(ReflectiveOperationException|LinkageError error){fail(error);}
    }
    public static boolean hideLoweredOffhand(){
        LocalPlayer player=Minecraft.getInstance().player;
        if(!available || loweredOwner.get()!=player || player==null || !player.getOffhandItem().isEmpty() || player.isUsingItem())return false;
        try{return currentClip.invoke(handler)==null;}catch(ReflectiveOperationException|LinkageError error){fail(error);return false;}
    }
    // Punchy's blendInFrom and blendFrom fields contain String -> Sample entries.
    @SuppressWarnings("unchecked")
    private static java.util.Map<String,Object> nativeSamples(java.lang.reflect.Field field) throws IllegalAccessException {
        return (java.util.Map<String,Object>)field.get(handler);
    }
    private static void clearParried(){
        parriedGeneration++;parriedPending=parriedActive=false;parriedHoldRemaining=parriedHoldClock=0;
    }
    private static void clearOwnedState(){
        clearReaction();
        clearParried();
        lastOwnedPartialTick=1.0F;
        releasePending=releaseHadHitlag=false;
        releaseAtNanos=0;
        transitionSources=java.util.Map.of();
        transitionSnapshot=transitionReturning=startingTransition=false;
        automaticReturn=false;
        emptyEntryLower=1.0F;
        emptyReturnLower=0.0F;
        emptyHandSlideLogged=emptyHandSlideRendered=false;
        emptyHandSlidePeak=0.0F;
        lastPriorityReason="";
        exiting=lowerEmptyOffhand=enteringEmptyOffhand=false;
        ownedItem=ownedSupportItem=null;
        ownedValues=null;
        ownedClip=null;
        ownedHand=null;
        owner.clear();
        selected=-1;
    }
    private static void finishRelease(){
        try{
            if(available&&ownedClip!=null){
                Object current=currentClip.invoke(handler);
                if(current==ownedClip)clearClip.invoke(handler);
                if(current==ownedClip||current==null)clearSourceHand.invoke(null);
            }
        }catch(ReflectiveOperationException|LinkageError error){fail(error);}
        finally{clearOwnedState();}
    }
    @SubscribeEvent public static void endTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.isAlive()) { clearSession(); }
        else if(parriedActive) advanceParried();
        else if(reactionActive&&!exiting) advanceReaction();
        else if(releasePending) advanceRelease();
        else if(available&&ownedClip!=null){if(exiting)advanceReturn();else if(!shouldTakeOver())release();}
    }
    public static void resourcesReloaded() {
        loweredOwner.clear();finishRelease();clearPreview(); libraryGeneration = -1;
        invalidateClips();
    }
    private static void fail(Throwable exception) {
        loweredOwner.clear();
        clearOwnedState();
        available=false;
        LoggerFactory.getLogger("MallardGuard").warn("Punchy guard animation is unavailable; using the simple first-person guard pose.", exception);
    }
}
