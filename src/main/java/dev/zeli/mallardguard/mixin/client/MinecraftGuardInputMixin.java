package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reject guarded attacks before vanilla or optional mods start their attack animations. */
@Mixin(value = Minecraft.class, priority = 1100)
public abstract class MinecraftGuardInputMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mallardguard$preventGuardAttack(CallbackInfoReturnable<Boolean> callback) {
        if(GuardClient.attacksBlocked())callback.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mallardguard$preventGuardAttackRepeat(boolean held, CallbackInfo callback) {
        if(GuardClient.attacksBlocked())callback.cancel();
    }
}
