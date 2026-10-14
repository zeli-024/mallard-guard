package dev.zeli.mallardguard.mixin.client;
import dev.zeli.mallardguard.client.GuardBetterCombatCompat;
import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block new swings during guarding; observe permitted starts and feints. */
@Mixin(value = Minecraft.class, priority = 400)
abstract class BetterCombatGuardMixin {
    @Unique private boolean mallardguard$cancelledSwing, mallardguard$attackDue;
    @Unique private boolean mallardguard$wasWindingUp;
    @Inject(method = "startUpswing", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mallardguard$beforeSwing(CallbackInfo ci) {
        GuardBetterCombatCompat.swingHookReady();
        mallardguard$wasWindingUp=GuardBetterCombatCompat.windingUp();
        if(GuardClient.attacksBlocked())ci.cancel();
    }
    @Inject(method = "startUpswing", at = @At("RETURN"), remap = false, require = 0)
    private void mallardguard$swingStarted(CallbackInfo ci) {
        if(!mallardguard$wasWindingUp && GuardBetterCombatCompat.windingUp())GuardClient.combatSwingStarted();
    }
    @Inject(method = "performAttack", at = @At("HEAD"), cancellable=true, remap = false, require = 0)
    private void mallardguard$attackDue(CallbackInfo ci) { if(GuardClient.incomingStunned()){ci.cancel();return;}mallardguard$attackDue=GuardBetterCombatCompat.feintPressed(); }
    @Inject(method = "performAttack", at = @At("RETURN"), remap = false, require = 0)
    private void mallardguard$attackResolved(CallbackInfo ci) { mallardguard$attackDue=false; }
    @Inject(method = "cancelWeaponSwing", at = @At("HEAD"), remap = false, require = 0)
    private void mallardguard$beforeFeint(CallbackInfo ci) { mallardguard$cancelledSwing=GuardBetterCombatCompat.cancelling(mallardguard$attackDue); }
    @Inject(method = "cancelWeaponSwing", at = @At("RETURN"), remap = false, require = 0)
    private void mallardguard$feinted(CallbackInfo ci) {
        if(mallardguard$cancelledSwing){mallardguard$cancelledSwing=false;GuardBetterCombatCompat.cancelled();}
    }
}
