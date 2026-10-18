package dev.zeli.mallardguard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.zeli.mallardguard.client.GuardMovementFov;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keep Waddling Tails' visual denominator consistent with vanilla's filtered speed. */
@Pseudo
@Mixin(targets = "dev.zeli.waddletrail.client.MovementClient", remap = false)
public abstract class WaddlingTailsFovMixin {
    @ModifyExpressionValue(
        method = "keepMovementOutOfFov(Lnet/neoforged/neoforge/client/event/ComputeFovModifierEvent;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D", remap = true),
        remap = false,
        require = 0
    )
    private static double mallardguard$movementFovDenominator(double original) {
        return GuardMovementFov.withoutGuardSlowdown(Minecraft.getInstance().player, original);
    }
}
