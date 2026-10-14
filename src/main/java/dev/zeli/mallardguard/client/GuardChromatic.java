package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** One reusable capture target; channel separation runs alongside other feedback. */
@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardChromatic {
    private static TextureTarget captured;
    private static long started;
    private static int duration;
    private static boolean perfect;
    private static final GuardRenderTargets.State captureState=new GuardRenderTargets.State();
    private GuardChromatic() {}
    public static void trigger(boolean perfect) {
        GuardChromatic.perfect=perfect;
        duration=perfect?GuardConfig.CHROMATIC_TICKS.get():dev.zeli.mallardguard.GuardScreenSettings.get(dev.zeli.mallardguard.GuardScreenSettings.CHROMATIC_REGULAR_DURATION);
        if (enabled()) started = System.nanoTime();
        else started=0;
    }
    private static boolean enabled(){return duration>0&&dev.zeli.mallardguard.GuardScreenSettings.enabled(dev.zeli.mallardguard.GuardScreenSettings.CHROMATIC_ENABLED)
        &&GuardConfig.CHROMATIC_INTENSITY.get()>0&&(perfect||!GuardConfig.CHROMATIC_PERFECT_ONLY.get());}
    public static void clear() {
        started = 0;
        if (captured != null) { captured.destroyBuffers(); captured = null; }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeHud(RenderGuiEvent.Pre event) {
        if (started!=0&&!GuardConfig.CHROMATIC_HUD.get()) {if(!enabled()){started=0;return;}event.getGuiGraphics().flush();draw();}
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterHud(RenderGuiEvent.Post event) {
        if (GuardConfig.CHROMATIC_HUD.get() && started != 0) {if(!enabled()){started=0;return;}event.getGuiGraphics().flush();draw();}
    }
    private static void draw() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) { clear(); return; }
        if (started == 0 || mc.screen != null) return;
        if(!enabled()){started=0;return;}
        double progress = (System.nanoTime() - started) / (duration * 50_000_000.0);
        if (progress < 0 || progress >= 1) { started = 0; return; }
        int intensity=GuardConfig.CHROMATIC_INTENSITY.get();
        float strength = intensity / 100.0F * (float) Math.sin(Math.PI * progress);
        if (strength <= 0) return;
        var destination = mc.getMainRenderTarget();
        if (destination.width <= 0 || destination.height <= 0) return;
        captureState.save();
        try{
            if (captured == null || captured.width != destination.width || captured.height != destination.height) {
                if (captured != null) captured.destroyBuffers();
                captured = new TextureTarget(destination.width, destination.height, false, false);
            }
            GuardHitlag.copy(destination, captured);
            GuardImpactFrame.drawChromatic(captured, destination, strength);
        }finally{captureState.restore();}
    }
}
