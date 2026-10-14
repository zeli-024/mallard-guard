package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.zeli.mallardguard.GuardScreenSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Uses the normal world mask and shader, without sending a combat result. */
final class GuardHighlightPreviewScreen extends GuardPreviewScreen {
    private static final int[] CONTROLS={GuardScreenSettings.HIGHLIGHT_DIM,GuardScreenSettings.HIGHLIGHT_GLOW,GuardScreenSettings.HIGHLIGHT_FADE,GuardScreenSettings.HIGHLIGHT_DURATION,GuardScreenSettings.HIGHLIGHT_FADE_OUT};
    private final Screen parent;
    private final int[] values;
    private int[] saved;
    private final boolean editable;
    private final Consumer<int[]> accept;
    private final List<LivingEntity> targets=new ArrayList<>();
    private final List<Button> buttons=new ArrayList<>();
    private int selected,left,top,panelWidth,panelHeight,rowGap;
    private long started;
    private boolean animated;
    private TextureTarget scene;
    private final GuardRenderTargets.State previewState=new GuardRenderTargets.State();
    GuardHighlightPreviewScreen(Screen parent,int[] values,boolean editable,Consumer<int[]> accept){
        super("Target Highlight Preview");this.parent=parent;this.values=values.clone();saved=this.values.clone();this.editable=editable;this.accept=accept;
        var mc=Minecraft.getInstance();
        if(mc.crosshairPickEntity instanceof LivingEntity living&&living!=mc.player)targets.add(living);
        if(mc.level!=null&&mc.player!=null)for(var entity:mc.level.entitiesForRendering())if(entity instanceof LivingEntity living&&living!=mc.player&&living.isAlive()&&living.distanceToSqr(mc.player)<=1024&&!targets.contains(living))targets.add(living);
    }
    private void selectTarget(){
        int id=targets.isEmpty()?-1:targets.get(selected).getId();
        GuardParryHighlight.beginPreview(id);
    }
    private void still(){animated=false;started=0;}
    private void playOnce(){animated=true;started=System.nanoTime();}
    @Override protected void init(){
        buttons.clear();panelWidth=Math.min(420,Math.max(120,width-20));left=(width-panelWidth)/2;
        rowGap=Math.min(24,Math.max(12,(height-115)/5));panelHeight=109+rowGap*5;top=Math.max(3,(height-panelHeight)/2);
        int third=(panelWidth-24)/3;
        buttons.add(addRenderableWidget(GuardUi.button("Still Preview",left+6,top+24,third,20,this::still)));
        buttons.add(addRenderableWidget(GuardUi.button("Play Once",left+12+third,top+24,third,20,this::playOnce)));
        buttons.add(addRenderableWidget(GuardUi.button("Change Target",left+18+2*third,top+24,third,20,()->{if(!targets.isEmpty())selected=(selected+1)%targets.size();still();selectTarget();})));
        buttons.get(0).setTooltip(GuardUi.tooltip("Hold the effect at full strength while you tune its appearance."));
        buttons.get(1).setTooltip(GuardUi.tooltip("Play fade-in, hold and fade-out once. The effect stays off afterward until you choose Still Preview or Play Once."));
        buttons.get(2).active=targets.size()>1;
        for(int n=0;n<CONTROLS.length;n++){
            int slot=CONTROLS[n],labelWidth=Math.min(140,panelWidth/2);
            GuardSlider slider=addRenderableWidget(new GuardSlider(left+labelWidth,top+55+n*rowGap,panelWidth-labelWidth-8,Math.min(20,rowGap-2),values[slot],GuardScreenSettings.min(slot),GuardScreenSettings.max(slot),GuardUi.dragStep(GuardScreenSettings.min(slot),GuardScreenSettings.max(slot),1),v->v+GuardScreenSettings.unit(slot),v->{values[slot]=v;valueChanged();still();},()->false));
            slider.active=editable;slider.setTooltip(GuardUi.tooltip(GuardScreenSettings.tip(slot)));
        }
        int w=(panelWidth-24)/3;
        buttons.add(addRenderableWidget(GuardUi.button("Reset",left+6,top+panelHeight-25,w,20,()->GuardUi.confirm(this,"Reset highlight?","Restore the highlight preview controls to their defaults.",()->{for(int i:CONTROLS)values[i]=GuardScreenSettings.initial(i);rebuild();}))));buttons.get(3).active=editable;
        buttons.add(addRenderableWidget(GuardUi.button("Apply",left+12+w,top+panelHeight-25,w,20,()->apply())));buttons.get(4).active=editable;
        buttons.add(addRenderableWidget(GuardUi.button("Close",left+18+w*2,top+panelHeight-25,w,20,this::onClose)));
        visibilityButton();buttons.add(visibility);still();selectTarget();
    }
    private void rebuild(){clearWidgets();init();}
    private void apply(){accept.accept(values.clone());if(parent instanceof GuardConfigScreen config&&!config.applyExternalChanges())return;saved=values.clone();Minecraft.getInstance().setScreen(parent);}
    @Override protected boolean blurPreview(){return false;}
    @Override public boolean isPauseScreen(){return true;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        graphics.flush();var target=Minecraft.getInstance().getMainRenderTarget();
        if(target.width>0&&target.height>0){
            previewState.save();
            try{
            if(scene==null||scene.width!=target.width||scene.height!=target.height){if(scene!=null)scene.destroyBuffers();scene=new TextureTarget(target.width,target.height,false,false);}
            long elapsed=System.nanoTime()-started;long duration=values[GuardScreenSettings.HIGHLIGHT_DURATION]*50_000_000L,fade=values[GuardScreenSettings.HIGHLIGHT_FADE]*50_000_000L;
            if(!animated||elapsed<duration){copy(target,scene);float amount=!animated?1F:GuardScreenSettings.envelope(elapsed,duration,fade,values[GuardScreenSettings.HIGHLIGHT_FADE_OUT]*50_000_000L);GuardParryHighlight.drawPreview(scene,target,amount,values);}
            }finally{previewState.restore();}
        }
        beginPreview(graphics);
        graphics.fill(left-2,top,left+panelWidth+2,top+panelHeight,0xED211B2A);graphics.renderOutline(left-2,top,panelWidth+4,panelHeight,GuardUi.BORDER);
        graphics.drawCenteredString(font,"HIGHLIGHT PREVIEW",width/2,top+7,GuardUi.TEXT);
        for(int n=0;n<CONTROLS.length;n++)graphics.drawString(font,font.plainSubstrByWidth(GuardScreenSettings.label(CONTROLS[n]),Math.min(130,panelWidth/2-12)),left+7,top+59+n*rowGap,GuardUi.TEXT);
        String name=targets.isEmpty()?"No nearby target: previewing world dimming":targets.get(selected).getDisplayName().getString();
        graphics.drawCenteredString(font,font.plainSubstrByWidth(name,width-20),width/2,Math.max(2,top-12),GuardUi.TEXT);
        for(var widget:renderables)if(!(widget instanceof Button))widget.render(graphics,mouseX,mouseY,delta);
        for(Button button:buttons)if(button.visible)GuardUi.paint(graphics,button,mouseX,mouseY,false,false);
        endPreview(graphics);
    }
    @Override public void removed(){if(scene!=null)scene.destroyBuffers();scene=null;GuardParryHighlight.clear();super.removed();}
    @Override public void onClose(){
        if(!java.util.Arrays.equals(values,saved))GuardUi.choices(this,"Unsaved highlight changes","Apply your values or discard edits.",new GuardUi.Choice("Apply & Close",this::apply),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
        else Minecraft.getInstance().setScreen(parent);
    }
}
