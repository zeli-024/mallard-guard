package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfigTransfer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardScreenSettings;
import dev.zeli.mallardguard.GuardCombatEffects;
import dev.zeli.mallardguard.GuardCompatibility;
import dev.zeli.mallardguard.GuardShieldReactions;
import dev.zeli.mallardguard.GuardShieldExpansionCompat;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardSounds;
import dev.zeli.mallardguard.GuardState;
import dev.zeli.mallardguard.MallardGuard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardClient {
    private static final KeyMapping GUARD_KEY = new KeyMapping("key.mallardguard.guard",
        InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.mallardguard");
    private static final KeyMapping CONFIG_KEY = new KeyMapping("key.mallardguard.open_config",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "key.categories.mallardguard");
    private static final ResourceLocation RED_SHIELD = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/shield_red.png");
    private static final ResourceLocation SHIELD = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/shield.png");
    private static final ResourceLocation[] MEME_FLASHES = {
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_1.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_2.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_3.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_plankton.png")
    };
    private static final long MEME_HOLD_MS = 500;
    private static final long MEME_FADE_MS = 200;
    public static void registerArtworkReload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
        GuardShieldArtwork.reload(resources);
        GuardParryParticleSpawner.clear();
    });}
    private static boolean wasDown;
    private static final GuardPrediction prediction=new GuardPrediction();
    private static int phase;
    private static boolean offhand;
    private static int elapsed;
    private static int recharge;
    private static int window = 4;
    private static int rechargeMax = 12;
    private static int hitResult;
    private static boolean guardBreakHitResult;
    private static long resultStartMs;
    private static long screenFlashStartMs;
    private static int pendingFlash,pendingFlashSequence;
    private static boolean pendingMeme;
    private static long memeStartMs;
    private static int memeIndex = -1;
    private static int feedbackSequence;
    private static final long BLOCK_REACTION_MS = 240;
    private static long chargeGlowStartMs;
    private static long stanceStartMs;
    private static long blockStartMs;
    private static long strainedBlockStartMs;
    private static boolean readyGlowQueued;
    private static boolean wasGuardKeyDown;
    private static long incomingStunUntil=Long.MIN_VALUE;
    public static boolean incomingStunned(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.player==combatPlayer&&mc.player.isAlive()&&mc.player.level().getGameTime()<incomingStunUntil;}
    public static boolean attacksBlocked(){return incomingStunned()||guardBlocksAttack();}
    private static long reportedAttackTick = Long.MIN_VALUE;

    /** Predicted or acknowledged guarding blocks attacks until the Guard key is released. */
    public static boolean guardBlocksAttack() {
        Minecraft mc=Minecraft.getInstance();
        return mc.player!=null && mc.player==combatPlayer && mc.player.isAlive() && mc.getConnection()!=null
            && mc.screen==null && GUARD_KEY.isDown() && !waitForGuardRelease && phase>=1 && phase<=4;
    }

    /** Existing swings prevent guard entry; attack input cannot interrupt an active guard. */
    public static boolean attackBlocksGuard() {
        Minecraft mc=Minecraft.getInstance();
        if(incomingStunned())return true;
        if(guardBlocksAttack())return false;
        if(GuardBetterCombatCompat.swingActive())return true;
        return mc.options.keyAttack.isDown() && !GuardBetterCombatCompat.managedWeapon();
    }

    public static void combatSwingStarted() { reportAttackStart(Minecraft.getInstance()); }

    private static void reportAttackStart(Minecraft mc) {
        if (mc.player == null || !mc.player.isAlive() || mc.getConnection() == null || mc.screen != null) return;
        if(!combatPlayerReady(mc) || attacksBlocked())return;
        long tick = mc.player.level().getGameTime();
        if (reportedAttackTick == tick) return;
        reportedAttackTick = tick;
        GuardDiagnostics.event(GuardDiagnostics.GUARD,"Attack started outside guarding, tick="+tick);
        GuardBetterCombatCompat.attacked();
        releaseGuard(mc);
        GuardState.attackStarted(mc.player);
        PacketDistributor.sendToServer(new GuardPackets.AttackStarted());
    }

    @SubscribeEvent
    public static void attackClick(InputEvent.InteractionKeyMappingTriggered event) {
        if(!event.isAttack() || event.isCanceled())return;
        if(attacksBlocked()){
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if(!GuardBetterCombatCompat.managedWeapon())reportAttackStart(Minecraft.getInstance());
    }
    private static boolean waitForGuardRelease;
    private static boolean sessionActive, deathCleared;
    private static Player combatPlayer;
    private static ItemStack guardedShieldStack = ItemStack.EMPTY;
    private static InteractionHand guardedShieldHand;

    private static int flashDuration(boolean perfect){return perfect?260:190;}
    private static boolean flashPerfect;
    private GuardClient() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GUARD_KEY);
        event.register(CONFIG_KEY);
    }

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> {
            if (GuardConfig.pendingUpdates() > 0) return new GuardConfigUpdateScreen(parent);
            if (Minecraft.getInstance().getConnection() == null)
                return new GuardConfigScreen(GuardConfig.defaultSnapshot(false), parent);
            return new GuardConfigLoadingScreen(parent);
        });
    }

    public static boolean isBlockingStance() { return phase == 3 || phase == 4; }

    public static boolean isStanceActive() {
        return phase != 0;
    }

    public static InteractionHand thirdPersonHand() {
        return phase == 0 ? null : offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static InteractionHand guardHand(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) return phase == 0 || phase == 4 ? null : (offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return null;
    }

    public static boolean isCurrentGuardHand(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && phase != 0 && phase != 4 && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,
            minecraft.player.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
    }

    public static void status(GuardPackets.Status data) {
        Minecraft mc=Minecraft.getInstance();
        if(!combatPlayerReady(mc))return;
        if(!prediction.receive(data,mc.player.level().getGameTime()))return;
        applyGuardView(prediction.view(mc.player.level().getGameTime()));
    }

    private static void applyGuardView(GuardPackets.Status data) {
        if (phase == 4 && data.phase() == 0 && GUARD_KEY.isDown()
            && !attackBlocksGuard() && data.recharge() > 0) {
            waitForGuardRelease = true;
        }
        if (phase != 3 && phase != 4 && (data.phase() == 3 || data.phase() == 4)) blockStartMs = System.currentTimeMillis();
        if ((phase == 3 || phase == 4) && data.phase() != 3 && data.phase() != 4) {
            blockStartMs = 0;
            strainedBlockStartMs = 0;
        }
        if (phase == 0 && data.phase() != 0) {
            ++feedbackSequence;
            stanceStartMs = System.currentTimeMillis();
            hitResult = 0;
            resultStartMs = 0;
            cancelFlash();
            readyGlowQueued = false;
            chargeGlowStartMs = 0;
            strainedBlockStartMs = 0;
        }
        if (recharge > 0 && data.recharge() == 0 && data.phase() == 0 && phase != 1) {
            // A perfect parry resets recharge instantly. Its reaction is the only visual.
            readyGlowQueued = true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (data.phase() == 0) {
            if (ownsGuardedShieldUse(mc)) mc.player.stopUsingItem();
            clearGuardedShieldUse();
        } else if (mc.player != null) {
            InteractionHand hand = data.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (GUARD_KEY.isDown() && !attackBlocksGuard() && !waitForGuardRelease
                && (!mc.player.isUsingItem() || mc.player.getUsedItemHand() != hand)
                && shieldLike(mc.player.getItemInHand(hand))
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getItemInHand(hand).getItem())) mc.player.startUsingItem(hand);
            if (guardedShieldStack.isEmpty() && GUARD_KEY.isDown() && !waitForGuardRelease
                && mc.player.isUsingItem() && mc.player.getUsedItemHand() == hand
                && mc.player.getUseItem() == mc.player.getItemInHand(hand)
                && shieldLike(mc.player.getUseItem())) {
                guardedShieldHand = hand; guardedShieldStack = mc.player.getUseItem();
            }
        }
        boolean handChanged=phase!=0 && data.phase()!=0 && offhand!=data.offhand();
        if(handChanged)PunchyGuardCompat.correctGuardHand();
        if(phase!=data.phase())GuardDiagnostics.event(GuardDiagnostics.GUARD,"Guard state "+phase+" -> "+data.phase()+", hand="+(data.offhand()?"off":"main")+", recharge="+data.recharge());
        phase = data.phase();
        offhand = data.offhand();
        PunchyGuardCompat.guardStatus(phase);
        elapsed = data.elapsed();
        recharge = data.recharge();
        window = data.window();
        rechargeMax = data.rechargeMax();
    }

    private static GuardPackets.Settings latestEligibility;
    public static void configSaveResult(GuardPackets.SaveResult result) {
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.saveResult(result);
    }

    public static void settings(GuardPackets.Settings data) {
        latestEligibility=data;GuardState.setClientRules(data);

        serverConsumablePriority = data.consumablePriority();
        Minecraft minecraft = Minecraft.getInstance();
        // Settings can arrive during join; only an explicit editor request opens UI.
        if (!(minecraft.screen instanceof GuardConfigLoadingScreen loading)) return;
        Screen parent = loading.parent();
        minecraft.setScreen(GuardConfig.pendingUpdates() > 0
            ? new GuardConfigUpdateScreen(parent) : new GuardConfigScreen(data, parent));
    }

    public static void serverConfigUpdate(GuardPackets.ConfigUpdate data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GuardConfigLoadingScreen loading)) return;
        Screen parent = loading.parent();
        minecraft.setScreen(new GuardConfigUpdateScreen(parent, data.changes()));
    }

    public static void openConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof GuardConfigLoadingScreen)) return;
        mc.setScreen(GuardConfig.pendingUpdates() > 0 ? new GuardConfigUpdateScreen(null)
            : new GuardConfigLoadingScreen(null));
    }

    public static void damageState(GuardPackets.DamageState data) {
        GuardDiagnostics.event(GuardDiagnostics.DAMAGE,"Damage source list synchronized");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GuardConfigScreen screen) screen.updateDamageState(data);
    }

    public static void damageHit(GuardPackets.DamageHit data) {
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.updateDamageHit(data.id());
    }

    private static boolean serverConsumablePriority = true;
    private static GuardPackets.ShieldSettings latestShieldSettings;
    private static long mobCounterStart;
    private static int mobReactionResult;

    public static void stunState(GuardPackets.StunState data){
        var mc=Minecraft.getInstance();if(!combatPlayerReady(mc))return;int ticks=Math.clamp(data.ticks(),0,100);
        incomingStunUntil=ticks==0?Long.MIN_VALUE:mc.player.level().getGameTime()+ticks;
        if(ticks>0){GuardBetterCombatCompat.interruptAttack();mc.player.swinging=false;releaseGuard(mc);}
    }
    @SubscribeEvent public static void stunnedMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event){if(incomingStunned()){var input=event.getInput();input.leftImpulse=input.forwardImpulse=0;input.jumping=false;}}
    public static void mobCounter(GuardPackets.MobCounter d) {
        if(!combatPlayerReady(Minecraft.getInstance()))return;
        var mc=Minecraft.getInstance();int stun=Math.clamp(d.stunTicks(),0,100);
        // Positive stun is handled once by the preceding authoritative StunState packet.
        if(d.stagger()&&stun==0){GuardBetterCombatCompat.interruptAttack();mc.player.swinging=false;}
        int sequence=++feedbackSequence;cancelFlash();
        hitResult=0;resultStartMs=0;mobCounterStart=0;
        GuardChromatic.trigger(d.perfect());
        Runnable feedback = () -> {
            if(sequence!=feedbackSequence)return;
            mobReactionResult = d.perfect() ? 2 : 1;
            GuardDiagnostics.event(GuardDiagnostics.MOB,"Enemy guard feedback: "+(d.perfect()?"perfect":"regular"));
            var player = Minecraft.getInstance().player;
            if (player == null || !player.isAlive()) return;
            mobCounterStart = System.currentTimeMillis();

            queueFlash(true,d.perfect(),GuardConfig.MEME_FLASH.get());
        };
        GuardHitlag.triggerMobCounter(d.perfect(),d.opponentId(),feedback);
    }

    private static GuardPackets.MobSettings latestMobSettings;
    public static GuardPackets.MobSettings currentMobSettings() {
        return latestMobSettings == null ? GuardConfig.mobSnapshot(true) : latestMobSettings;
    }
    public static void mobSettings(GuardPackets.MobSettings data) {
        latestMobSettings = data;
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.updateMobSettings(data);
    }

    public static void shieldSettings(GuardPackets.ShieldSettings data) {
        latestShieldSettings = data;
        GuardState.setClientHandRules(data.shieldParryPriority(),data.parryHandPriority(),data.forceCrouchOffhand());
        GuardState.setClientShieldBlacklist(data.shieldBlacklist());
        serverConsumablePriority = data.consumablePriority();
        GuardState.setClientCooldownPreventsGuard(data.cooldownPreventsGuard());
        GuardState.setClientAllowEmptyHand(data.allowEmptyHand());
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.updateShieldSettings(data);
    }

    public static boolean shieldLike(net.minecraft.world.item.ItemStack stack){
        return GuardItemRules.clientShieldLike(stack,latestEligibility==null?GuardConfig.SHIELD_ITEMS.get():latestEligibility.shieldItems(),latestShieldSettings==null?GuardConfig.SHIELD_BLACKLIST.get():latestShieldSettings.shieldBlacklist());
    }

    public static GuardPackets.ShieldSettings currentShieldSettings() {
        return latestShieldSettings == null ? GuardConfig.defaultShieldSnapshot() : latestShieldSettings;
    }

    public static boolean clientCategoryLocked(int category) { return GuardClientPolicy.clientCategoryLocked(category); }
    static java.util.List<dev.zeli.mallardguard.GuardPoseLibrary.Pose> enforcedAnimations() { return GuardClientPolicy.enforcedAnimations(); }
    public static GuardPackets.ClientPolicy currentPolicy() { return GuardClientPolicy.currentPolicy(); }
    public static void playerPalette(GuardPackets.PlayerPalette data) { GuardClientPolicy.playerPalette(data); }
    public static void clientPolicy(GuardPackets.ClientPolicy data) { GuardClientPolicy.clientPolicy(data); }
    public static void saveClientConfig() { GuardClientPolicy.saveClientConfig(); }
    @SubscribeEvent
    public static void disconnected(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        GuardClientPolicy.clearClientPolicy();
        clearCombatState(Minecraft.getInstance());
    }

    static void configMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(message, false);
        else org.slf4j.LoggerFactory.getLogger("MallardGuard").info("{}", message.getString());
    }

    public static void hitResult(GuardPackets.HitResult data) {
        if(!combatPlayerReady(Minecraft.getInstance()))return;
        if (data.result() < 1 || data.result() > 3) return;
        if(data.inputId()!=prediction.inputId()){
            if(data.retaliationToken()>0)PacketDistributor.sendToServer(new GuardPackets.FeedbackComplete(data.retaliationToken()));
            return;
        }
        hitResult = data.result();
        guardBreakHitResult = data.guardBroken();
        resultStartMs = data.result() == 3 ? System.currentTimeMillis() : 0;
        cancelFlash();
        int sequence = ++feedbackSequence;
        if (data.result() == 1 || data.result() == 2) {
            int result = data.result();mobCounterStart=0;
            GuardChromatic.trigger(result==2);
            boolean meme = GuardConfig.MEME_FLASH.get();
            boolean freeze=GuardHitlag.ticks(result==2)>0;
            var mc=Minecraft.getInstance();
            GuardHitlag.trigger(result == 2,data.opponentId(),PunchyGuardCompat.prepareParried(freeze && mc.screen==null && mc.level!=null), () -> {
                if (data.retaliationToken() > 0 && Minecraft.getInstance().getConnection() != null)
                    PacketDistributor.sendToServer(new GuardPackets.FeedbackComplete(data.retaliationToken()));
                if (sequence != feedbackSequence || Minecraft.getInstance().player == null) return;
                if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.isAlive()) return;
                resultStartMs = System.currentTimeMillis();
                queueFlash(false,result==2,meme);
            });
        }
        if (hitResult == 3) PunchyGuardCompat.blockHit();
        chargeGlowStartMs = 0;
        if (hitResult == 3 && strainedBlockStartMs == 0) strainedBlockStartMs = resultStartMs;
        if (hitResult == 2) readyGlowQueued = false;
    }

    public static void hitSound(GuardPackets.HitSound data) {
        GuardDiagnostics.event(GuardDiagnostics.DAMAGE,"Hit feedback: kind="+data.kind()+", variant="+data.variant()+", defender="+data.defender());
        if (data.defender() && data.parry()) {
            GuardHitlag.afterFreeze(() -> playHitSoundNow(data));
            return;
        }
        playHitSoundNow(data);
    }

    private static void playHitSoundNow(GuardPackets.HitSound data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Float.isFinite(data.volume()) || data.volume() < 0.0F || data.volume() > 4.0F) return;
        List<DeferredHolder<SoundEvent, SoundEvent>> sounds = switch (data.kind()) {
            case 1 -> GuardSounds.PARRY;
            case 2 -> GuardSounds.PERFECT;
            case 3 -> GuardSounds.BLOCK;
            case 4 -> GuardSounds.SHIELD_PERFECT;
            case 5 -> GuardSounds.FALL_PARRY;
            case 6 -> GuardSounds.FALL_REGULAR;
            default -> List.of();
        };
        if (data.variant() < 0 || data.variant() >= sounds.size()) return;
        int individual = switch (data.kind()) {
            case 1 -> GuardConfig.LOCAL_PARRY_VOLUME.get();
            case 2, 4, 5 -> GuardConfig.LOCAL_PERFECT_VOLUME.get();
            case 6 -> GuardConfig.LOCAL_PARRY_VOLUME.get();
            default -> GuardConfig.LOCAL_BLOCK_VOLUME.get();
        };
        float gain = data.volume() * GuardConfig.LOCAL_MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
        if (gain <= 0) return;
        if (gain > 1.0F) sounds = switch (data.kind()) {
            case 2 -> GuardSounds.PERFECT_BOOST;
            case 3 -> GuardSounds.BLOCK_BOOST;
            case 4 -> GuardSounds.SHIELD_PERFECT_BOOST;
            case 5 -> GuardSounds.FALL_PARRY_BOOST;
            case 6 -> GuardSounds.FALL_REGULAR_BOOST;
            default -> GuardSounds.PARRY_BOOST;
        };
        playWithHeadroom(mc, data.x(), data.y(), data.z(), sounds.get(data.variant()).get(), gain);
    }

    private static void playWithHeadroom(Minecraft mc, double x, double y, double z, SoundEvent sound, float gain) {
        // Use the louder recording above 100%; one source avoids simultaneous
        // copies of the same clip interfering with short parry sounds.
        float primary = gain <= 1.0F ? gain : Math.min(1.0F, 0.85F + 0.15F * (float) (Math.log(gain) / Math.log(16)));
        mc.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, primary, 1.0F, false);
    }

    private static void playMemeFlashSound(int result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int individual = result == 2 ? GuardConfig.LOCAL_PERFECT_VOLUME.get() : GuardConfig.LOCAL_PARRY_VOLUME.get();
        float gain = GuardConfig.LOCAL_MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
        if (gain <= 0) return;
        SoundEvent sound = gain > 1.0F ? GuardSounds.MEME_FLASH_BOOST.get() : GuardSounds.MEME_FLASH.get();
        playWithHeadroom(mc, mc.player.getX(), mc.player.getY(), mc.player.getZ(), sound, gain);
    }

    public static void sparks(GuardPackets.Sparks data) {
        GuardDiagnostics.event(GuardDiagnostics.DAMAGE,"Particle feedback received: "+data);
        GuardPackets.Palette palette = GuardClientPolicy.palette(data.paletteId());
        if (data.defender()) {
            GuardHitlag.afterFreeze(() -> GuardParryParticleSpawner.spawn(data, palette));
            return;
        }
        GuardParryParticleSpawner.spawn(data, palette);
    }

    @SubscribeEvent
    public static void cameraShake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||!mc.player.isAlive()||mc.screen!=null||mc.getConnection()==null)return;
        if(!cameraShakeActive())return;
        boolean perfect=shakeResult()==2;
        long age=System.currentTimeMillis()-shakeStart();
        int duration=shakeDuration(perfect),strength=shakeStrength(perfect);
        float amplitude=(1-age/(float)duration)*strength/100F;
        event.setRoll(event.getRoll()+(float)Math.sin(age*0.095D)*2.5F*amplitude);
        event.setPitch(event.getPitch()+(float)Math.sin(age*0.135D)*1.4F*amplitude);
    }

    private static int shakeResult(){return mobCounterStart>resultStartMs?mobReactionResult:hitResult;}
    private static long shakeStart(){return mobCounterStart>resultStartMs?mobCounterStart:resultStartMs;}
    private static int shakeDuration(boolean perfect){return GuardScreenSettings.get(perfect?GuardScreenSettings.SHAKE_PERFECT_DURATION:GuardScreenSettings.SHAKE_REGULAR_DURATION)*50;}
    private static int shakeStrength(boolean perfect){return perfect?GuardConfig.SHAKE_STRENGTH.get():GuardScreenSettings.get(GuardScreenSettings.SHAKE_REGULAR_STRENGTH);}
    static boolean cameraShakeActive(){
        if(!GuardScreenSettings.enabled(GuardScreenSettings.CAMERA_ENABLED))return false;
        int result=shakeResult();if(result!=1&&result!=2)return false;
        boolean perfect=result==2;if(!perfect&&GuardScreenSettings.enabled(GuardScreenSettings.SHAKE_PERFECT_ONLY))return false;
        long start=shakeStart(),age=System.currentTimeMillis()-start;
        return start!=0&&age>=0&&age<shakeDuration(perfect)&&shakeStrength(perfect)>0;
    }

    private static void clearCombatState(Minecraft mc){
        ++feedbackSequence; // Invalidate queued feedback from the previous life.
        cancelFlash();
        if(ownsGuardedShieldUse(mc))mc.player.stopUsingItem();
        clearGuardedShieldUse();
        if(mc.player!=null)GuardState.updateMovement(mc.player,false);
        wasDown=wasGuardKeyDown=false;
        waitForGuardRelease=mc.player!=null&&GUARD_KEY.isDown();
        reportedAttackTick=Long.MIN_VALUE;
        GuardState.clearClientAttackLock();GuardBetterCombatCompat.clear();
        prediction.clear(mc.player==null?0:mc.player.level().getGameTime());
        phase=elapsed=recharge=hitResult=mobReactionResult=0;
        incomingStunUntil=Long.MIN_VALUE;
        offhand=guardBreakHitResult=readyGlowQueued=false;
        resultStartMs=screenFlashStartMs=memeStartMs=chargeGlowStartMs=stanceStartMs=blockStartMs=strainedBlockStartMs=mobCounterStart=0;
        memeIndex=-1;
        GuardChromatic.clear();GuardHitlag.clear();PunchyGuardCompat.clearSession();
    }
    private static boolean combatPlayerReady(Minecraft mc){
        if(mc.player==null||mc.getConnection()==null)return false;
        if(combatPlayer!=mc.player){clearCombatState(mc);combatPlayer=mc.player;deathCleared=false;}
        if(!mc.player.isAlive()){
            if(!deathCleared){clearCombatState(mc);deathCleared=true;}
            return false;
        }
        deathCleared=false;return true;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            GuardClientPolicy.clearClientPolicy();
            GuardThirdPerson.clear();
            if(sessionActive){GuardConfigTransfer.clearClientSnapshot();PunchyGuardCompat.clearSession();GuardHitlag.clear();GuardParryParticleSpawner.clear();GuardDiagnostics.clear();sessionActive=false;}
            latestShieldSettings = null;latestEligibility=null;GuardState.setClientRules(null);GuardState.setClientShieldBlacklist("");
            latestMobSettings = null; GuardChromatic.clear(); mobCounterStart=0;mobReactionResult=0;
            serverConsumablePriority = true;
            GuardState.setClientCooldownPreventsGuard(true);
            GuardState.setClientAllowEmptyHand(false);
            if (mc.player != null) GuardState.updateMovement(mc.player, false);
            if(combatPlayer!=null){clearCombatState(mc);combatPlayer=null;deathCleared=false;}
            return;
        }
        sessionActive = true;GuardConfigTransfer.expireClientSnapshot();
        if(!combatPlayerReady(mc))return;
        if(mc.screen!=null||mc.options.hideGui)cancelFlash();
        GuardCompatibility.clientReady(message->mc.player.displayClientMessage(message,false),latestShieldSettings!=null&&latestShieldSettings.shieldExpansionActive());
        GuardClientPolicy.requestIfNeeded();
        while (CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                openConfig();
                break;
            }
        }
        boolean guardKeyDown = mc.screen == null && GUARD_KEY.isDown()&&!incomingStunned();
        boolean hasConsumable = serverConsumablePriority && GuardItemRules.consumableInEitherHand(mc.player);
        if (!guardKeyDown) waitForGuardRelease = false;
        if (guardKeyDown && hasConsumable) waitForGuardRelease = true;
        if (guardKeyDown && !wasGuardKeyDown && hasConsumable && !mc.options.keyUse.isDown()
            && !mc.player.isUsingItem() && mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, GuardItemRules.consumable(mc.player.getMainHandItem())
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
        if(guardKeyDown&&!wasGuardKeyDown){
            String reason=hasConsumable?"consumable priority":attackBlocksGuard()?"active attack":!GuardState.attackReady(mc.player)?"attack cooldown":recharge>0?"parry recharge":!GuardState.eligible(mc.player)?"item eligibility or item cooldown":"eligible; awaiting server validation";
            GuardDiagnostics.event(GuardDiagnostics.GUARD,"Guard key pressed: "+reason);
        }
        if (guardKeyDown && !hasConsumable && !waitForGuardRelease) GuardBetterCombatCompat.beforeGuard();
        applyGuardView(prediction.view(mc.player.level().getGameTime()));
        wasGuardKeyDown = guardKeyDown;
        boolean down = guardKeyDown && !hasConsumable && !waitForGuardRelease && !attackBlocksGuard() && GuardState.attackReady(mc.player);
        if (!down) {
            if (ownsGuardedShieldUse(mc)) mc.player.stopUsingItem();
            clearGuardedShieldUse();
        }
        GuardState.updateMovement(mc.player, down && (phase != 0 || !wasDown && recharge == 0 && GuardState.eligible(mc.player)));
        if (!down) releaseGuard(mc);
        else if (!wasDown) {
            wasDown=true;
            InteractionHand hand=GuardState.chooseHand(mc.player,mc.player.getRandom().nextBoolean(),prediction.lastToolHand(),prediction.toolHandRun());
            boolean chosenOffhand=hand==InteractionHand.OFF_HAND;
            long id=prediction.nextInput();
            prediction.requestStart(mc.player.level().getGameTime());
            if(hand!=null && recharge==0 && latestEligibility!=null && latestShieldSettings!=null
                && (latestEligibility.parry() || latestEligibility.block())){
                boolean shield=shieldLike(mc.player.getItemInHand(hand));
                prediction.start(mc.player.level().getGameTime(),chosenOffhand,shield,latestEligibility.parry(),latestEligibility.block(),
                    shield?GuardShieldExpansionCompat.perfectTicks(mc.player.getItemInHand(hand),latestShieldSettings.perfect(),latestShieldSettings.window()):latestEligibility.perfect(),
                    shield?GuardShieldExpansionCompat.parryTicks(mc.player.getItemInHand(hand),latestShieldSettings.window()):latestEligibility.window(),
                    shield?GuardShieldExpansionCompat.cooldownTicks(mc.player.getItemInHand(hand),latestShieldSettings.rechargeTicks()):latestEligibility.recharge());
                applyGuardView(prediction.view(mc.player.level().getGameTime()));
                GuardDiagnostics.event(GuardDiagnostics.GUARD,"Guard predicted: input="+id+", hand="+hand);
            }
            PacketDistributor.sendToServer(new GuardPackets.Input(true,chosenOffhand,GuardHitlag.ticks(true),id));
        }
    }

    private static void releaseGuard(Minecraft mc) {
        if(!wasDown)return;
        wasDown=false;
        long id=prediction.nextInput();
        prediction.release(mc.player.level().getGameTime());
        applyGuardView(prediction.view(mc.player.level().getGameTime()));
        GuardState.updateMovement(mc.player,false);
        if(mc.getConnection()!=null)PacketDistributor.sendToServer(new GuardPackets.Input(false,false,GuardHitlag.ticks(true),id));
    }

    private static boolean ownsGuardedShieldUse(Minecraft mc) {
        return mc.player != null && guardedShieldHand != null && !guardedShieldStack.isEmpty()
            && mc.player.isUsingItem() && mc.player.getUsedItemHand() == guardedShieldHand
            && mc.player.getUseItem() == guardedShieldStack
            && mc.player.getItemInHand(guardedShieldHand) == guardedShieldStack;
    }

    private static void clearGuardedShieldUse() {
        guardedShieldStack = ItemStack.EMPTY; guardedShieldHand = null;
    }

    /** A rebound Guard key maintains only the shield use that our active stance owns. */
    public static boolean maintainGuardShieldUse() {
        Minecraft mc = Minecraft.getInstance();
        return mc.screen == null && GUARD_KEY.isDown() && !attackBlocksGuard() && !waitForGuardRelease
            && phase != 0 && ownsGuardedShieldUse(mc)
            && !mc.player.getCooldowns().isOnCooldown(guardedShieldStack.getItem());
    }

    @SubscribeEvent
    public static void blockStandaloneShieldUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isUseItem() && mc.player != null && event.getHand() != null
            && !(serverConsumablePriority && GuardItemRules.consumableInEitherHand(mc.player))
            && (shieldLike(mc.player.getItemInHand(event.getHand()))
                || GuardCompatibility.simplySwordsWeapon(mc.player.getItemInHand(event.getHand())))) {
            event.setCanceled(true);
            // NeoForge swings the canceled hand by default unless explicitly disabled.
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void prioritizeConsumable(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!serverConsumablePriority || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || mc.player == null
            || mc.gameMode == null || mc.player.isUsingItem()
            || GuardItemRules.consumable(mc.player.getMainHandItem())
            || !GuardItemRules.consumable(mc.player.getOffhandItem())) return;
        // Bypass the main-hand item so eating/drinking in the offhand works even
        // when that item (such as a shield) normally consumes the right click.
        event.setCanceled(true);
        event.setSwingHand(false);
        mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
    }

    /** Only vanilla's attack-indicator reads use this; player options stay untouched. */
    public static boolean hideVanillaAttackIndicator() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isAlive() && !mc.options.hideGui && GuardConfig.HUD.get()
            && (phase != 0 || recharge != 0 || resultVisible() || readyGlowQueued || readyGlowVisible());
    }

    private static boolean resultVisible() {
        long age = System.currentTimeMillis() - resultStartMs;
        return GuardConfig.SHIELD_EFFECTS.get() && GuardShieldReactions.enabled(hitResult==2?1:hitResult==3?guardBreakHitResult?3:2:0) && hitResult != 0 && resultStartMs != 0 && age >= 0 && age < reactionDuration();
    }

    private static long reactionDuration() {
        return GuardShieldReactions.millis(hitResult==2?1:hitResult==3?guardBreakHitResult?3:2:0);
    }

    private static boolean readyGlowVisible() {
        long age = System.currentTimeMillis() - chargeGlowStartMs;
        return GuardConfig.SHIELD_EFFECTS.get() && GuardShieldReactions.enabled(4) && chargeGlowStartMs != 0 && age >= 0 && age < GuardShieldReactions.millis(4);
    }

    private static void cancelFlash(){pendingFlash=0;pendingMeme=false;screenFlashStartMs=memeStartMs=0;}
    static boolean flashEnabled(boolean perfect){
        return GuardConfig.SCREEN_FLASH.get()&&(perfect||!GuardConfig.PERFECT_ONLY_FLASH.get())
            &&(GuardConfig.MEME_FLASH.get()||(perfect?GuardConfig.FLASH_STRENGTH.get():GuardScreenSettings.get(GuardScreenSettings.FLASH_REGULAR_STRENGTH))>0);
    }
    private static void queueFlash(boolean incoming,boolean perfect,boolean meme){
        if(!flashEnabled(perfect))return;
        pendingFlash=incoming?(perfect?4:3):(perfect?2:1);pendingMeme=meme;pendingFlashSequence=feedbackSequence;
    }
    static boolean flashPendingOrActive(){
        if(Minecraft.getInstance().options.hideGui)return false;
        if(pendingFlash!=0&&pendingFlashSequence==feedbackSequence&&flashEnabled(pendingFlash==2||pendingFlash==4))return true;
        if(screenFlashStartMs==0||!flashEnabled(flashPerfect))return false;
        long now=System.currentTimeMillis(),age=now-screenFlashStartMs;
        return age>=0&&age<flashDuration(flashPerfect)||GuardConfig.MEME_FLASH.get()&&memeStartMs!=0&&now>=memeStartMs&&now-memeStartMs<MEME_HOLD_MS+MEME_FADE_MS;
    }
    private static void updateFlash(long now){
        if(pendingFlash==0)return;
        if(pendingFlashSequence!=feedbackSequence||!flashEnabled(pendingFlash==2||pendingFlash==4)){cancelFlash();return;}
        if(GuardHitlag.freezeActive())return;
        int kind=pendingFlash;pendingFlash=0;
        GuardDiagnostics.event(GuardDiagnostics.SCREEN_EFFECTS,"White Flash started after hitlag");
        {
            flashPerfect=kind==2||kind==4;
            screenFlashStartMs=now;
            if(pendingMeme&&GuardConfig.MEME_FLASH.get()){
                memeStartMs=now;playMemeFlashSound(flashPerfect?2:1);var random=Minecraft.getInstance().player.getRandom();
                if(memeIndex<0)memeIndex=random.nextInt(MEME_FLASHES.length);
                else{int next=random.nextInt(MEME_FLASHES.length-1);memeIndex=next>=memeIndex?next+1:next;}
            }
        }
        pendingMeme=false;
    }

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public static void screenEffects(RenderGuiEvent.Pre event){
        var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.isAlive()||mc.screen!=null||mc.options.hideGui){cancelFlash();return;}
        long now=System.currentTimeMillis();updateFlash(now);
        if(screenFlashStartMs==0)return;
        if(!flashEnabled(flashPerfect)){cancelFlash();return;}
        long flashAge=now-screenFlashStartMs;
        boolean white=flashAge>=0&&flashAge<flashDuration(flashPerfect);
        boolean meme=GuardConfig.MEME_FLASH.get()&&memeStartMs!=0&&memeIndex>=0&&now>=memeStartMs&&now-memeStartMs<MEME_HOLD_MS+MEME_FADE_MS;
        if(!white&&!meme){cancelFlash();return;}
        var graphics=event.getGuiGraphics();graphics.flush();
        if(meme)renderMemeFlash(graphics,now-memeStartMs);
        if(white)renderScreenFlash(graphics,flashAge);
        graphics.flush();
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.options.hideGui) return;
        long counterAge = System.currentTimeMillis() - mobCounterStart;
        long now=System.currentTimeMillis(),resultAge=now-resultStartMs;
        if (!GuardConfig.HUD.get() || mc.screen instanceof GuardIconPositionScreen) return;
        int shieldSize = GuardIconLayout.size();
        var position=GuardIconLayout.current(event.getGuiGraphics().guiWidth(),event.getGuiGraphics().guiHeight(),shieldSize);
        float shieldCenterX=position.x(),shieldCenterY=position.y();

        if (GuardConfig.SHIELD_EFFECTS.get() && mobCounterStart != 0 && counterAge >= 0
            && counterAge < (GuardShieldReactions.millis(mobReactionResult==2?1:0))) {
            renderMobReaction(event.getGuiGraphics(), shieldCenterX, shieldCenterY, shieldSize, counterAge);
            return;
        }
        boolean showingResult = resultVisible();
        // A reaction owns the shield until it finishes. The ready glow starts afterward.
        if (readyGlowQueued && !showingResult && phase == 0 && recharge == 0) {
            readyGlowQueued = false;
            chargeGlowStartMs = now;
        }
        long chargeAge = now - chargeGlowStartMs;
        boolean chargeGlow = readyGlowVisible() && !showingResult && phase == 0 && recharge == 0;
        if (phase == 0 && recharge == 0 && !showingResult && !chargeGlow) return;
        boolean strainedBlock = GuardShieldReactions.enabled(2) && GuardConfig.SHIELD_EFFECTS.get() && (phase == 3 || phase == 4) && strainedBlockStartMs != 0 && !showingResult;
        float strainedFade = strainedBlock ? Math.clamp((now - strainedBlockStartMs - BLOCK_REACTION_MS) / 220.0F, 0.0F, 1.0F) : 0.0F;
        GuiGraphics graphics = event.getGuiGraphics();
        float progress = showingResult ? Math.clamp(resultAge / (float) reactionDuration(), 0.0F, 1.0F) : 1.0F;
        int reaction=hitResult==2?1:hitResult==3?guardBreakHitResult?3:2:0;
        float expansionFactor=GuardShieldReactions.factor(reaction,1),shakeFactor=GuardShieldReactions.factor(reaction,2),flashFactor=GuardShieldReactions.factor(reaction,3),echoFactor=GuardShieldReactions.factor(reaction,4);
        showingResult &= GuardShieldReactions.enabled(reaction);
        float chargeProgress = chargeGlow ? chargeAge / (float) GuardShieldReactions.millis(4) : 1.0F;
        float bob = chargeGlow ? -(float) Math.sin(Math.PI * chargeProgress) * 2.0F * GuardShieldReactions.factor(4,1) : 0.0F;
        float chargeFade = chargeGlow ? Math.clamp((1.0F - chargeProgress) / 0.48F, 0.0F, 1.0F) : 1.0F;
        float entrance = phase != 0 && stanceStartMs != 0 ? Math.clamp((now - stanceStartMs) / 90.0F, 0.0F, 1.0F) : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(shieldCenterX, shieldCenterY + bob, 0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (showingResult && hitResult == 2) {
            // One large, fading copy of the same artwork, behind the perfect parry.
            renderEcho(graphics, shieldSize / 16.0F * (1.18F + 2.75F * progress*GuardShieldReactions.strength(1,1)), 0.82F * (1.0F - progress) * echoFactor, 0, 0);
        } else if (showingResult && hitResult == 3) {
            // Every block, including a weapon block, uses the shield's compact echo.
            renderEcho(graphics, shieldSize / 16.0F * (1.03F + 0.65F * progress*GuardShieldReactions.strength(2,1)),
                0.48F * (1.0F - progress) * echoFactor, 0, 0);
            for (int i = 0; i < 3; i++) {
                float echoAge = (resultAge - i * 48.0F) / Math.max(1F,GuardShieldReactions.millis(2)-i*48F);
                if (echoAge < 0 || echoAge >= 1) continue;
                float offset = (i % 2 == 0 ? -1.0F : 1.0F) * (1.0F - echoAge);
                renderBlockEcho(graphics, shieldSize / 16.0F * (1.03F + 0.24F * echoAge),
                    0.40F * (1.0F - echoAge) * echoFactor, offset, -offset * 0.5F);
            }
        } else if (strainedBlock) {
            // After the first blocked hit, small dark ripples persist until guard ends.
            for (int i = 0; i < 2; i++) {
                float echoProgress = ((now - strainedBlockStartMs + i * 190) % 380) / 380.0F;
                renderBlockEcho(graphics, shieldSize / 16.0F * (1.02F + 0.29F * echoProgress),
                    0.34F * (1.0F - echoProgress) * strainedFade * GuardShieldReactions.factor(2,4), 0, 0);
            }
        }
        float pulse = showingResult ? (float) Math.sin(Math.PI * progress) *
            (hitResult == 2 ? 1.22F : hitResult == 1 ? 0.37F : guardBreakHitResult ? 0.23F : 0.18F) : 0.0F;
        float shakeStrength = hitResult == 2 ? 3.8F : hitResult == 3 ? 1.6F : 1.4F;
        float shake = showingResult ? (float) Math.sin(resultAge * (hitResult == 3 ? 0.20D : 0.15D)) * shakeStrength * (1.0F - progress) : 0.0F;
        float verticalShake = showingResult ? (float) Math.cos(resultAge * 0.17D) * shakeStrength * 0.35F * (1.0F - progress) : 0.0F;
        float trembleX = strainedBlock ? (float) Math.sin(now * 0.09D) * 0.80F * strainedFade : 0.0F;
        float trembleY = strainedBlock ? (float) Math.cos(now * 0.13D) * 0.55F * strainedFade : 0.0F;
        graphics.pose().translate(shake*shakeFactor + trembleX*GuardShieldReactions.factor(2,2), verticalShake*shakeFactor + trembleY*GuardShieldReactions.factor(2,2), 0);
        graphics.pose().scale(shieldSize / 16.0F * (1.0F + pulse*expansionFactor), shieldSize / 16.0F * (1.0F + pulse*expansionFactor), 1.0F);
        graphics.pose().translate(-8, -8, 0);
        if (showingResult) {
            if (hitResult == 3) {
                if (guardBreakHitResult) {
                    // Red marks the block that breaks guard, for both shields and weapons.
                    drawShieldTinted(graphics, 0, 16, (0.38F + 0.38F * (1.0F - progress)) * entrance,
                        0.42F, 0.37F, 0.45F);
                    graphics.pose().pushPose();
                    graphics.pose().translate(8, 8, 0);
                    float redExpansion = 1.0F + 0.23F * (float) Math.sin(Math.PI * progress)*GuardShieldReactions.factor(3,1);
                    graphics.pose().scale(redExpansion, redExpansion, 1.0F);
                    graphics.pose().translate(-8, -8, 0);
                    drawRedShield(graphics, 0.94F * (1.0F - progress * progress)*flashFactor);
                    graphics.pose().popPose();
                } else {
                    drawShield(graphics, 0.76F * entrance);
                    drawShieldMask(graphics, 0xFFFFFF, 0.18F * (1.0F - progress)*flashFactor);
                }
            } else {
                drawShield(graphics, 0.88F * entrance);
            }
            if (hitResult == 2) {
                // Briefly outgrow the expanding shield so its white edge reads clearly.
                graphics.pose().pushPose();
                graphics.pose().translate(8, 8, 0);
                float whiteExpansion = 1.0F + 0.11F * (float) Math.sin(Math.PI * progress)*GuardShieldReactions.factor(1,1);
                graphics.pose().scale(whiteExpansion, whiteExpansion, 1.0F);
                graphics.pose().translate(-8, -8, 0);
                drawShieldMask(graphics, 0xFFFFFF, 0.23F * (1.0F - progress)*flashFactor);
                graphics.pose().popPose();
            } else if (hitResult == 1) {
                drawShieldMask(graphics, 0xFFFFFF, 0.16F * (1.0F - progress)*flashFactor);
            }
        } else {
            if (phase == 3 || phase == 4) {
                // The held-block icon is visible before impact, then settles into a darker tremble.
                float holdFade = blockStartMs == 0 ? 1.0F : Math.clamp((now - blockStartMs) / 130.0F, 0.0F, 1.0F);
                float baseOpacity = (0.33F + 0.27F * holdFade) * entrance;
                drawShieldTinted(graphics, 0, 16, baseOpacity * (1.0F - 0.10F * strainedFade),
                    1.0F - 0.45F * strainedFade, 1.0F - 0.50F * strainedFade, 1.0F - 0.41F * strainedFade);
            } else {
                drawShield(graphics, 0.33F * entrance * chargeFade);
            }
            if (chargeGlow) {
                drawShield(graphics, 0.82F * chargeFade);
            } else if (phase == 1 || phase == 2) {
                // Remove the active layer from the top while the parry window runs out.
                int drained = Math.clamp(Math.round(16.0F * elapsed / Math.max(1, window)), 0, 16);
                if (drained < 16) drawShieldRegion(graphics, drained, 16 - drained, 0.82F * entrance);
            } else if (phase != 3 && phase != 4) {
                // Refill the active layer from the bottom after releasing the stance.
                int filled = Math.clamp(Math.round(16.0F * (1.0F - recharge / (float) Math.max(1, rechargeMax))), 0, 16);
                if (filled > 0) drawShieldRegion(graphics, 16 - filled, filled, 0.82F);
            }
        }
        if (chargeGlow) {
            float glow = Math.min(1.0F, chargeProgress / 0.20F) * (1.0F - chargeProgress);
            drawShieldOutline(graphics, glow*GuardShieldReactions.factor(4,3));
        }
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    static void renderIconPreview(GuiGraphics graphics,float x,float y,int size){
        graphics.pose().pushPose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        try{graphics.pose().translate(x,y,0);graphics.pose().scale(size/16F,size/16F,1);graphics.pose().translate(-8,-8,0);drawShield(graphics,0.88F);}
        finally{graphics.pose().popPose();RenderSystem.disableBlend();}
    }

    private static void renderMobReaction(GuiGraphics graphics, float x, float y, int size, long age) {
        boolean perfect = mobReactionResult == 2;
        int reaction=perfect?1:0;
        if(!GuardShieldReactions.enabled(reaction))return;
        float progress = age / (float) (GuardShieldReactions.millis(perfect?1:0));
        float baseScale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        // The resting layer remains purple; only the reactive artwork and echo are red.
        graphics.pose().pushPose(); graphics.pose().scale(baseScale, baseScale, 1);
        graphics.pose().translate(-8, -8, 0); drawShield(graphics, 0.33F); graphics.pose().popPose();
        if (perfect) {
            graphics.pose().pushPose();
            float echoScale = baseScale * (1.18F + 2.75F * progress*GuardShieldReactions.strength(1,1));
            graphics.pose().scale(echoScale, echoScale, 1); graphics.pose().translate(-8, -8, 0);
            drawRedShield(graphics, 0.82F * (1 - progress)*GuardShieldReactions.factor(1,4)); graphics.pose().popPose();
        }
        float strength = (perfect ? 3.8F : 1.4F)*GuardShieldReactions.factor(reaction,2);
        graphics.pose().translate((float) Math.sin(age * 0.15D) * strength * (1 - progress),
            (float) Math.cos(age * 0.17D) * strength * 0.35F * (1 - progress), 0);
        float scale = baseScale * (1 + (float) Math.sin(Math.PI * progress) * (perfect ? 1.22F : 0.37F)*GuardShieldReactions.factor(perfect?1:0,1));
        graphics.pose().scale(scale, scale, 1); graphics.pose().translate(-8, -8, 0);
        drawRedShield(graphics, 0.88F * (1 - progress * progress));
        drawShieldMask(graphics, 0xFFFFFF, (perfect ? 0.32F : 0.16F) * (1 - progress)*GuardShieldReactions.factor(perfect?1:0,3),true);
        RenderSystem.disableBlend(); graphics.pose().popPose();
    }

    private static void drawRedShield(GuiGraphics graphics, float opacity) {
        if (opacity <= 0) return;
        RenderSystem.setShaderColor(1, 1, 1, opacity);
        graphics.blit(RED_SHIELD, 0, 0, 0, 0, 16, 16, 16, 16);
        graphics.flush(); RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void drawShield(GuiGraphics graphics, float opacity) {
        drawShieldRegion(graphics, 0, 16, opacity);
    }

    private static void drawShieldRegion(GuiGraphics graphics, int top, int height, float opacity) {
        drawShieldTinted(graphics, top, height, opacity, 1.0F, 1.0F, 1.0F);
    }

    private static void drawShieldTinted(GuiGraphics graphics, int top, int height, float opacity,
                                         float red, float green, float blue) {
        if (height <= 0 || opacity <= 0.0F) return;
        RenderSystem.setShaderColor(red, green, blue, opacity);
        graphics.blit(SHIELD, 0, top, 0, top, 16, height, 16, 16);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderEcho(GuiGraphics graphics, float scale, float opacity, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-8, -8, 0);
        if (hitResult == 3 && guardBreakHitResult) drawRedShield(graphics, opacity);
        else {
            drawShield(graphics, opacity);
            drawShieldMask(graphics, 0xFFFFFF, opacity * 0.35F);
        }
        graphics.pose().popPose();
    }

    private static void renderBlockEcho(GuiGraphics graphics, float scale, float opacity, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-8, -8, 0);
        drawShieldTinted(graphics, 0, 16, opacity, 0.58F, 0.51F, 0.62F);
        graphics.pose().popPose();
    }

    private static void drawShieldMask(GuiGraphics graphics, int color, float opacity) {
        drawShieldMask(graphics,color,opacity,false);
    }

    private static void drawShieldMask(GuiGraphics graphics, int color, float opacity, boolean red) {
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 255.0F);
        if (alpha == 0) return;
        GuardShieldArtwork.drawMask(graphics, (alpha << 24) | color, red);
    }

    private static void drawShieldOutline(GuiGraphics graphics, float opacity) {
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 215.0F);
        if (alpha == 0) return;
        GuardShieldArtwork.drawOutline(graphics, (alpha << 24) | 0xFFFFFF);
    }

    private static void renderScreenFlash(net.minecraft.client.gui.GuiGraphics graphics, long age) {
        renderScreenFlash(graphics, age, flashPerfect, GuardConfig.MEME_FLASH.get() && memeStartMs != 0);
    }

    private static void renderScreenFlash(GuiGraphics graphics, long age, boolean perfect, boolean meme) {
        float intensity=meme?1F:(perfect?GuardConfig.FLASH_STRENGTH.get():GuardScreenSettings.get(GuardScreenSettings.FLASH_REGULAR_STRENGTH))/100F;
        renderScreenFlash(graphics,age,flashDuration(perfect),intensity,meme?1F:0.85F);
    }
    private static void renderScreenFlash(GuiGraphics graphics,long age,int duration,float intensity,float peak){
        float progress=Math.clamp(age/(float)duration,0F,1F);
        int alpha = Math.round(255.0F * peak * (1.0F - progress) * intensity);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
    }

    private static void renderMemeFlash(GuiGraphics graphics, long age) {
        if (age < 0 || age >= MEME_HOLD_MS + MEME_FADE_MS) return;
        float alpha = age < MEME_HOLD_MS ? 1.0F
            : 1.0F - (age - MEME_HOLD_MS) / (float) MEME_FADE_MS;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        // The declared UV size matches the destination, so the entire image fills the GUI.
        graphics.blit(MEME_FLASHES[memeIndex], 0, 0, 0, 0, width, height, width, height);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (memeIndex == 3) {
            // A brief white overlay lifts the darker image.
            float burst = Math.max(0.0F, 1.0F - age / 180.0F);
            int brightness = Math.round((85.0F + 95.0F * burst) * alpha);
            graphics.fill(0, 0, width, height, brightness << 24 | 0xFFFFFF);
            graphics.flush();
        }
        RenderSystem.disableBlend();
    }

}
