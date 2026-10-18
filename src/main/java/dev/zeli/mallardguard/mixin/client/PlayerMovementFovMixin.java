package dev.zeli.mallardguard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.zeli.mallardguard.client.GuardMovementFov;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Filter our speed modifiers before vanilla and other mods calculate the FOV. */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerMovementFovMixin {
    @ModifyExpressionValue(
        method = "getFieldOfViewModifier",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"),
        require = 1
    )
    private double mallardguard$movementFovSpeed(double original) {
        return GuardMovementFov.withoutGuardSlowdown((AbstractClientPlayer) (Object) this, original);
    }
}
