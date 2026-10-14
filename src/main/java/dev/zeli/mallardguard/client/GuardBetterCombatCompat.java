package dev.zeli.mallardguard.client;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardState;
import dev.zeli.mallardguard.mixin.client.LivingEntityAttackTickerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Optional swing-state and feint integration. External API lookups are resolved once. */
public final class GuardBetterCombatCompat {
    private static KeyMapping feintKey;
    private static Method cancelSwing;
    private static Method stopAnimation, currentAttackHand, currentUpswing;
    private static Method swingInProgress, weaponAttributes, weaponAttacks;
    private static Field combatEnabled, harvesting;
    private static Class<?> animationOwnerClass;
    private static Field attackAnimation, animationBase;
    private static Method animationActive;
    private static boolean animationLookupFailed;
    private static boolean renderTailRequired;
    private static long renderTailUntil;
    private static boolean resolved;
    private static boolean swingHooksReady;
    private static long cancelledUntil = Long.MIN_VALUE;
    private GuardBetterCombatCompat() {}
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!net.neoforged.fml.ModList.get().isLoaded("bettercombat")) return;
        try{cancelSwing=Minecraft.class.getDeclaredMethod("cancelWeaponSwing");cancelSwing.setAccessible(true);}catch(ReflectiveOperationException|RuntimeException ignored){cancelSwing=null;}
        renderTailRequired=net.neoforged.fml.ModList.get().isLoaded("bc_punchy_fix");
        try {
            feintKey = (KeyMapping) Class.forName("net.bettercombat.client.Keybindings").getField("feintKeyBinding").get(null);
            stopAnimation = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable").getMethod("stopAttackAnimation", float.class);
            currentAttackHand = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("getCurrentAttackHand");
            currentUpswing = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("getUpswingTicks");
            swingInProgress = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("isWeaponSwingInProgress");
            weaponAttributes = Class.forName("net.bettercombat.logic.WeaponRegistry").getMethod("getAttributes", ItemStack.class);
            weaponAttacks = Class.forName("net.bettercombat.api.WeaponAttributes").getMethod("attacks");
            combatEnabled = Class.forName("net.bettercombat.client.BetterCombatClientMod").getField("ENABLED");
            harvesting = Minecraft.class.getDeclaredField("isHarvesting");harvesting.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            swingInProgress=weaponAttributes=null;combatEnabled=harvesting=null;
            System.err.println("Mallard Guard: Better Combat integration unavailable: " + failure.getClass().getSimpleName());
        }
    }
    public static boolean managedWeapon() {
        resolve();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || !swingHooksReady || weaponAttributes==null || combatEnabled==null
            || currentAttackHand==null || currentUpswing==null || swingInProgress==null)return false;
        try {
            if(!combatEnabled.getBoolean(null) || harvesting.getBoolean(mc))return false;
            Object attributes=weaponAttributes.invoke(null,mc.player.getMainHandItem());
            return attributes!=null && weaponAttacks.invoke(attributes)!=null;
        }
        catch(ReflectiveOperationException | RuntimeException failure){weaponAttributes=null;return false;}
    }
    public static void swingHookReady() {
        if(!swingHooksReady)dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.GUARD,"Better Combat swing hooks active");
        swingHooksReady=true;
    }
    public static boolean swingActive() {
        return renderedSwingActive() || logicalSwingActive();
    }
    private static boolean logicalSwingActive() {
        resolve();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || currentAttackHand==null || swingInProgress==null)return false;
        try {
            // A cancelled windup clears the hand even while the old animation clock is running.
            return currentAttackHand.invoke(mc)!=null && ((int)currentUpswing.invoke(mc)>0 || (boolean)swingInProgress.invoke(mc));
        } catch(ReflectiveOperationException | RuntimeException failure){swingInProgress=null;return false;}
    }
    private static boolean renderedSwingActive() {
        resolve();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || swingInProgress==null)return false;
        try {
            Class<?> playerClass=mc.player.getClass();
            if(animationOwnerClass!=playerClass){
                animationOwnerClass=playerClass;animationLookupFailed=false;
                attackAnimation=animationBase=null;animationActive=null;
                for(Class<?> type=playerClass;type!=null;type=type.getSuperclass()){
                    try { attackAnimation=type.getDeclaredField("attackAnimation");break; }
                    catch(NoSuchFieldException absent) { /* Search the player superclass. */ }
                }
                if(attackAnimation==null)throw new NoSuchFieldException("attackAnimation");
                attackAnimation.setAccessible(true);
            }
            if(animationLookupFailed)return false;
            Object stack=attackAnimation.get(mc.player);
            if(stack==null)return false;
            if(animationBase==null)animationBase=stack.getClass().getField("base");
            Object layer=animationBase.get(stack);
            if(layer==null)return false;
            if(animationActive==null)animationActive=layer.getClass().getMethod("isActive");
            long now=System.nanoTime();
            if((boolean)animationActive.invoke(layer)){
                // Match the optional render fix's short tail without calling its state-mutating helper.
                renderTailUntil=renderTailRequired?now+20_000_000L:0;
                return true;
            }
            return renderTailUntil!=0 && now<renderTailUntil;
        } catch(ReflectiveOperationException | RuntimeException failure){
            if(!animationLookupFailed)System.err.println("Mallard Guard: Better Combat animation lookup unavailable: "+failure);
            animationLookupFailed=true;return false;
        }
    }
    public static boolean windingUp() {
        resolve();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || currentUpswing==null)return false;
        try { return (int)currentUpswing.invoke(mc)>0; }
        catch(ReflectiveOperationException | RuntimeException failure){currentUpswing=null;return false;}
    }
    public static void attacked() { cancelledUntil=Long.MIN_VALUE; }
    public static boolean feintPressed() { resolve();return feintKey!=null && feintKey.isDown(); }
    public static boolean cancelling(boolean attackDue) {
        resolve();Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || !feintPressed() || currentAttackHand==null || currentUpswing==null)return false;
        try { return currentAttackHand.invoke(mc)!=null && (attackDue || (int)currentUpswing.invoke(mc)>0); }
        catch(ReflectiveOperationException failure){currentAttackHand=currentUpswing=null;return false;}
    }
    public static void cancelled() {
        resolve();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || feintKey == null || !feintKey.isDown()) return;
        cancelledUntil = mc.player.level().getGameTime() + 10;
        dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.GUARD,"Better Combat windup cancelled; guard recovery available");
        GuardState.clearClientAttackLock();
        if (mc.getConnection() != null) PacketDistributor.sendToServer(new GuardPackets.FeintEnded());
    }
    public static void beforeGuard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || logicalSwingActive() || mc.player.level().getGameTime() > cancelledUntil) return;
        ((LivingEntityAttackTickerAccessor) mc.player).mallardguard$attackTicker((int) Math.ceil(mc.player.getCurrentItemAttackStrengthDelay()));
        cancelledUntil = Long.MIN_VALUE;
        stopAttackAnimation();
    }
    public static void interruptAttack(){resolve();if(cancelSwing!=null)try{cancelSwing.invoke(Minecraft.getInstance());}catch(ReflectiveOperationException|RuntimeException failure){cancelSwing=null;}stopAttackAnimation();renderTailUntil=0;}
    public static void stopAttackAnimation() {
        resolve();
        if (stopAnimation == null || Minecraft.getInstance().player == null) return;
        try { stopAnimation.invoke(Minecraft.getInstance().player, 0.0F); }
        catch (ReflectiveOperationException failure) { stopAnimation = null; }
    }
    public static void clear() { cancelledUntil = Long.MIN_VALUE;renderTailUntil=0; }
}
