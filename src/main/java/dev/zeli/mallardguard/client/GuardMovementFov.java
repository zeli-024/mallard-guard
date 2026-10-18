package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardCombatEffects;
import dev.zeli.mallardguard.GuardState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Supplies a visual-only movement speed without our guard or stun modifiers. */
public final class GuardMovementFov {
    private GuardMovementFov() {}

    public static double withoutGuardSlowdown(Player player, double original) {
        if (player == null || player != Minecraft.getInstance().player || !Double.isFinite(original)) return original;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.getModifier(GuardState.GUARD_SLOWDOWN) == null
            && speed.getModifier(GuardCombatEffects.STUN_SPEED) == null) return original;
        // An earlier expression modifier may already supply a different visual speed.
        // Preserve that result instead of compensating it a second time.
        if (Double.compare(original, speed.getValue()) != 0) return original;

        double base = speed.getBaseValue(), multipliedBase = 0;
        var modifiers = speed.getModifiers();
        for (AttributeModifier modifier : modifiers) {
            if (owned(modifier)) continue;
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) base += modifier.amount();
            else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) multipliedBase += modifier.amount();
        }
        double clean = base * (1 + multipliedBase);
        for (AttributeModifier modifier : modifiers) {
            if (!owned(modifier) && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                clean *= 1 + modifier.amount();
        }
        clean = speed.getAttribute().value().sanitizeValue(clean);
        return Double.isFinite(clean) ? clean : original;
    }

    private static boolean owned(AttributeModifier modifier) {
        return modifier.id().equals(GuardState.GUARD_SLOWDOWN) || modifier.id().equals(GuardCombatEffects.STUN_SPEED);
    }
}
