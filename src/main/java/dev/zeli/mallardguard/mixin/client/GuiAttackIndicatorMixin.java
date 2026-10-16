package dev.zeli.mallardguard.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/** Suppress the vanilla indicator without changing options seen by other HUD renderers. */
@Mixin(Gui.class)
public abstract class GuiAttackIndicatorMixin {
    @ModifyExpressionValue(
        method = {"renderCrosshair", "renderItemHotbar"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;attackIndicator()Lnet/minecraft/client/OptionInstance;")),
        require = 2,
        expect = 2
    )
    private Object mallardguard$attackIndicator(Object original) {
        return original instanceof AttackIndicatorStatus && GuardClient.hideVanillaAttackIndicator()
            ? AttackIndicatorStatus.OFF : original;
    }
}
