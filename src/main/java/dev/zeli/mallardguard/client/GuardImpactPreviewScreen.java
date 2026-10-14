package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.zeli.mallardguard.GuardScreenSettings;
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Preview an actual captured scene before committing impact-image settings. */
final class GuardImpactPreviewScreen extends GuardPreviewScreen {
    private final Screen parent;
    private final Consumer<int[]> accept;
    private final boolean editable;
    private final int[] values;
    private final List<Button> buttons = new ArrayList<>();
    private final java.util.Map<Button, String> buttonTips = new java.util.HashMap<>();
    private TextureTarget scene;
    private final GuardRenderTargets.State previewState=new GuardRenderTargets.State();
    private boolean capturePending = true;
    private boolean shaderAvailable = true;

    private int[] saved;

    private void apply(boolean close){accept.accept(values.clone());if(parent instanceof GuardConfigScreen config&&!config.applyExternalChanges())return;saved=values.clone();Minecraft.getInstance().setScreen(close?parent:this);}
    private boolean mob;
    private int panelWidth, left, top, rowGap, panelHeight;
    private static final String[] NAMES = {"Brightness", "Contrast", "Dark outlines", "Grain", "Chromatic aberration"};
    GuardImpactPreviewScreen(Screen parent, int[] values, boolean editable, Consumer<int[]> accept) {
        super("Impact frame preview");
        this.parent = parent; this.accept = accept; this.editable = editable; this.values = values.clone(); saved=this.values.clone();
    }
    private int slot(int index) { return mob ? 51 + index : index == 4 ? 50 : 40 + index; }
    @Override protected void init() {
        buttons.clear(); buttonTips.clear();
        panelWidth = Math.min(420, Math.max(120, width - 20)); left = (width - panelWidth) / 2;
        rowGap = Math.min(23, Math.max(10, (height - 124) / 5)); panelHeight = 118 + rowGap * 5;
        top = Math.max(3, (height - panelHeight) / 2);
        int half = (panelWidth - 18) / 2;
        buttons.add(addRenderableWidget(GuardUi.builder(Component.literal("Your parry"), b -> { mob = false; rebuild(); })
            .bounds(left + 6, top + 22, half, 18).build()));
        buttons.add(addRenderableWidget(GuardUi.builder(Component.literal("Mob parry"), b -> { mob = true; rebuild(); })
            .bounds(left + 12 + half, top + 22, half, 18).build()));
        buttons.get(mob ? 1 : 0).active = false;
        if (mob) {
            Button adapt = addRenderableWidget(GuardUi.builder(Component.literal("Adapt: " + GuardUi.enabledLabel(values[56] != 0)), b -> {  values[56] = values[56] == 0 ? 1 : 0;rebuild(); })
                .bounds(left + 6, top + 43, half, 18).build()); adapt.active = editable; buttons.add(adapt);
            buttonTips.put(adapt, "Follow your parry impact settings instead of independent mob settings. Default: On.");
            Button invert = addRenderableWidget(GuardUi.builder(Component.literal("Invert: " + GuardUi.enabledLabel(values[57] != 0)), b -> {  values[57] = values[57] == 0 ? 1 : 0;rebuild(); })
                .bounds(left + 12 + half, top + 43, half, 18).build()); invert.active = editable; buttons.add(invert);
            buttonTips.put(invert, "Invert the mob impact filter. Works independently of Adapt. Default: On.");
        }
        if(!mob){
            int slot=GuardScreenSettings.PLAYER_IMPACT_INVERT;
            Button invert=addRenderableWidget(GuardUi.button("Invert: "+GuardUi.enabledLabel(values[slot]!=0),left+6,top+43,half,18,()->{values[slot]=values[slot]==0?1:0;rebuild();}));
            invert.active=editable;buttons.add(invert);buttonTips.put(invert,"Invert your impact frame. Default: Off.");
            Button perfectOnly=addRenderableWidget(GuardUi.button("Perfect Only: "+GuardUi.enabledLabel(values[35]!=0),left+12+half,top+43,half,18,()->{values[35]=values[35]==0?1:0;rebuild();}));perfectOnly.active=editable;buttons.add(perfectOnly);buttonTips.put(perfectOnly,"Only perfect parries use Impact Frame. Default: On.");
        }
        int labelWidth = Math.min(130, panelWidth / 2);
        for (int i = 0; i < NAMES.length; i++) {
            final int index = i;
            int source = mob && values[56] != 0 ? i == 4 ? 50 : 40 + i : slot(i);
            GuardSlider slider = addRenderableWidget(new GuardSlider(left + labelWidth, top + 65 + i * rowGap,
                panelWidth - labelWidth - 8, Math.min(20, rowGap - 2), values[source], 0, 400, 10,
                v -> v+"%", v -> { valueChanged(); values[slot(index)] = v; }, () -> false));
            slider.active = editable && (!mob || values[56] == 0);
            slider.setTooltip(GuardUi.tooltip(
                NAMES[i] + ": 0–400%. Default: " + dev.zeli.mallardguard.GuardClientPreset.DEFAULTS[slot(i)] + "%."
                + (mob ? " Adapt follows your parry settings; turn it off for independent sliders. Invert is independent." : " Changes are saved by Apply on the main config screen.")));
        }
        int bw=(panelWidth-24)/3;
        Button reset=addRenderableWidget(GuardUi.button("Reset",left+6,top+panelHeight-25,bw,20,()->GuardUi.confirm(this,"Reset impact values?","Restore both impact screens to their defaults.",()->{values[GuardScreenSettings.PLAYER_IMPACT_INVERT]=0;values[35]=1;for(int i=40;i<=43;i++)values[i]=dev.zeli.mallardguard.GuardClientPreset.DEFAULTS[i];for(int i=50;i<=57;i++)values[i]=dev.zeli.mallardguard.GuardClientPreset.DEFAULTS[i];})));reset.active=editable;buttons.add(reset);
        Button apply=addRenderableWidget(GuardUi.button("Apply",left+12+bw,top+panelHeight-25,bw,20,()->apply(true)));apply.active=editable;buttons.add(apply);
        buttons.add(addRenderableWidget(GuardUi.button("Close",left+18+2*bw,top+panelHeight-25,bw,20,this::onClose)));
        visibilityButton();buttons.add(visibility);
    }
    private void rebuild() { clearWidgets(); init(); }
    @Override public boolean isPauseScreen(){return true;}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.flush();RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.width > 0 && target.height > 0) {
            previewState.save();
            try{
            if (scene == null || scene.width != target.width || scene.height != target.height) {
                if (scene != null) scene.destroyBuffers();
                scene = new TextureTarget(target.width, target.height, false, false);
                capturePending = true;
            }
            target.bindWrite(false);
            if (capturePending) {
                copy(target, scene);
                capturePending = false;
            }
            int base = mob && values[56] == 0 ? 51 : 40;
            int chromatic = mob && values[56] == 0 ? values[55] : values[50];
            shaderAvailable = GuardImpactFrame.draw(scene, target, values[base], values[base + 1], values[base + 2], values[base + 3], chromatic, mob ? values[57] != 0 : values[GuardScreenSettings.PLAYER_IMPACT_INVERT]!=0);
            if (!shaderAvailable) copy(scene, target);
            }finally{previewState.restore();}
        }
        beginPreview(graphics);
        {
        graphics.fill(left - 2, top, left + panelWidth + 2, top + panelHeight, 0xED211B2A);
        graphics.fill(left - 2, top, left + panelWidth + 2, top + 1, GuardUi.BORDER);
        graphics.drawCenteredString(font, "IMPACT PREVIEW", width / 2, top + 7, GuardUi.TEXT);
        for (int i = 0; i < NAMES.length; i++) {
            String label = NAMES[i];
            int max = Math.min(120, panelWidth / 2 - 12);
            while (font.width(label) > max && label.length() > 1) label = label.substring(0, label.length() - 2) + "…";
            graphics.drawString(font, label, left + 7, top + 69 + i * rowGap, GuardUi.TEXT);
        }
        if (!shaderAvailable) graphics.drawCenteredString(font, "Impact filter unavailable on this renderer", width / 2,
            Math.max(3, top - 18), 0xFFFFAAAA);
        }
        for (var widget : renderables) if (!(widget instanceof Button) && (!(widget instanceof net.minecraft.client.gui.components.AbstractWidget a) || a.visible)) widget.render(graphics, mouseX, mouseY, delta);
        for (Button button : buttons) {
            if (!button.visible) continue;
            GuardUi.paint(graphics,button,mouseX,mouseY,false,false);
        }
        for (var entry : buttonTips.entrySet()) if (entry.getKey().visible && entry.getKey().isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.literal(entry.getValue()), Math.min(300, width - 24)), mouseX, mouseY);
            break;
        }
        endPreview(graphics);
    }

    @Override public void removed() {
        if (scene != null) scene.destroyBuffers();
        scene = null; super.removed();
    }

    @Override public void onClose() {if(!java.util.Arrays.equals(values,saved))GuardUi.choices(this,"Unsaved impact changes","Apply these values or discard edits.",new GuardUi.Choice("Apply & Close",()->apply(true)),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}

}
