package dev.zeli.mallardguard.client;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Shared integer slider: coarse drag, precise scroll/keys, and one visual style. */
final class GuardSlider extends AbstractSliderButton {
    private final int minimum, maximum, dragStep;
    private final IntFunction<String> format;
    private final IntConsumer change;
    private final BooleanSupplier changed;
    private int dragWidth;

    GuardSlider(int x, int y, int width, int height, int current, int minimum, int maximum,
                int dragStep, IntFunction<String> format, IntConsumer change, BooleanSupplier changed) {
        super(x, y, width, height, Component.empty(), fraction(current, minimum, maximum));
        this.minimum = minimum; this.maximum = maximum; this.dragStep = Math.max(1, dragStep);
        this.format = format; this.change = change; this.changed = changed;
        updateMessage();
    }
    private static double fraction(int current, int minimum, int maximum) {
        return maximum <= minimum ? 0 : Math.clamp((current - minimum) / (double) (maximum - minimum), 0, 1);
    }
    private int selected() { return minimum + (int) Math.round(value * (maximum - minimum)); }
    private void precise(int direction) {
        int previous = selected();
        int next = Math.clamp(previous + direction * GuardUi.preciseStep(), minimum, maximum);
        value = fraction(next, minimum, maximum);
        if (next != previous) change.accept(next);
        updateMessage();
    }
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!active || !isMouseOver(x, y) || sy == 0) return false;
        precise((int) Math.signum(sy)); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if(!active)return false;
        if (active && (key == 263 || key == 262)) { precise(key == 263 ? -1 : 1); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void onClick(double x, double y) { dragWidth = getWidth(); super.onClick(x, y); }
    @Override protected void onDrag(double x, double y, double dx, double dy) {
        if(!active)return;
        int displayedWidth = getWidth(); if (dragWidth > 0) setWidth(dragWidth);
        try { super.onDrag(x, y, dx, dy); } finally { setWidth(displayedWidth); }
    }
    @Override public void onRelease(double x, double y) { dragWidth = 0; super.onRelease(x, y); }
    @Override protected void applyValue() {
        int next = Math.clamp((int) Math.round(selected() / (double) dragStep) * dragStep, minimum, maximum);
        value = fraction(next, minimum, maximum); change.accept(next);
    }
    @Override protected void updateMessage() { setMessage(Component.literal(format.apply(selected()))); }
    @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuardUi.slider(graphics, this, value, changed.getAsBoolean());
    }
}
