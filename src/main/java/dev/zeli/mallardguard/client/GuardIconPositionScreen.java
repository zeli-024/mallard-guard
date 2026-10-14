package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardScreenSettings;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A detached position draft; only Apply commits it to the parent config. */
final class GuardIconPositionScreen extends GuardPreviewScreen {
    static final int[] SLOTS={GuardScreenSettings.ICON_ANCHOR,GuardScreenSettings.ICON_X,GuardScreenSettings.ICON_Y,GuardScreenSettings.ICON_GAP,GuardScreenSettings.ICON_SIZE};
    private static final int[] PRESETS={GuardIconLayout.TOP_LEFT,GuardIconLayout.TOP_CENTER,GuardIconLayout.TOP_RIGHT,
        GuardIconLayout.HOTBAR_LEFT,GuardIconLayout.CENTER,GuardIconLayout.HOTBAR_RIGHT,
        GuardIconLayout.BOTTOM_LEFT,GuardIconLayout.BOTTOM_CENTER,GuardIconLayout.BOTTOM_RIGHT};
    private final Screen parent;
    private final int[] values,saved;
    private final BooleanSupplier canEdit;
    private boolean wasEditable;
    private final Consumer<int[]> accept;
    private GuardUiLayout.IconEditor layout;
    private Button presetsButton;
    private boolean presetsOpen,dragging;
    private double dragX,dragY;
    GuardIconPositionScreen(Screen parent,int[] values,BooleanSupplier canEdit,Consumer<int[]> accept){
        super("Icon Position");this.parent=parent;this.values=values.clone();this.saved=values.clone();this.canEdit=canEdit;this.accept=accept;
    }
    @Override protected void init(){
        dragging=false;wasEditable=editable();layout=GuardUiLayout.iconEditor(width,height);
        if(presetsOpen){
            for(int i=0;i<PRESETS.length;i++){
                int mode=PRESETS[i],column=i%3,row=i/3;
                Button b=addRenderableWidget(GuardUi.button(GuardIconLayout.NAMES[mode],layout.presetX()+column*(layout.presetButtonWidth()+4),layout.presetY()+24+row*24,layout.presetButtonWidth(),20,()->{values[GuardScreenSettings.ICON_ANCHOR]=mode;presetsOpen=false;rebuild();}));
                b.active=editable();b.setTooltip(GuardUi.tooltip(presetTip(mode)));
            }
            addRenderableWidget(GuardUi.button("Back",layout.presetX(),layout.presetY()+100,layout.presetWidth(),20,()->{presetsOpen=false;rebuild();}));
            return;
        }
        int x=layout.x(),y=layout.controlsY(),inner=layout.inner(),presetWidth=(inner-4)/2;
        presetsButton=addRenderableWidget(GuardUi.button(GuardIconLayout.NAMES[values[GuardScreenSettings.ICON_ANCHOR]]+" ▾",x,y,presetWidth,20,()->{presetsOpen=true;rebuild();}));
        presetsButton.active=editable();presetsButton.setTooltip(GuardUi.tooltip("Choose a screen anchor. Dragging the icon changes it to Custom."));
        GuardSlider size=addRenderableWidget(new GuardSlider(x,y+24,presetWidth,20,values[GuardScreenSettings.ICON_SIZE],GuardScreenSettings.min(GuardScreenSettings.ICON_SIZE),GuardScreenSettings.max(GuardScreenSettings.ICON_SIZE),1,v->"Size: "+v+"%",v->{values[GuardScreenSettings.ICON_SIZE]=v;valueChanged();},()->values[GuardScreenSettings.ICON_SIZE]!=saved[GuardScreenSettings.ICON_SIZE]));
        size.active=editable();size.setTooltip(GuardUi.tooltip(GuardScreenSettings.tip(GuardScreenSettings.ICON_SIZE)));
        GuardSlider gap=addRenderableWidget(new GuardSlider(x+presetWidth+4,y+24,inner-presetWidth-4,20,values[GuardScreenSettings.ICON_GAP],0,80,1,v->"Gap: "+v+" px",v->{values[GuardScreenSettings.ICON_GAP]=v;valueChanged();},()->values[GuardScreenSettings.ICON_GAP]!=saved[GuardScreenSettings.ICON_GAP]));
        gap.active=editable();gap.setTooltip(GuardUi.tooltip("Space from edges, boss bars or the hotbar.\nCustom drag positions ignore this gap."));
        visibility=addRenderableWidget(GuardUi.button("Hide UI",x+presetWidth+4,y,inner-presetWidth-4,20,this::toggleVisibility));
        visibility.setTooltip(GuardUi.tooltip("Hide controls to inspect placement.\nClick empty space or press Escape to restore them."));
        int third=(inner-8)/3;
        addRenderableWidget(GuardUi.button("Reset",x,layout.footerY(),third,20,()->{for(int i:SLOTS)values[i]=GuardScreenSettings.initial(i);rebuild();})).active=editable();
        addRenderableWidget(GuardUi.button("Apply",x+third+4,layout.footerY(),third,20,this::apply)).active=editable();
        addRenderableWidget(GuardUi.button("Cancel",x+2*(third+4),layout.footerY(),inner-2*(third+4),20,this::onClose));
    }
    private static String presetTip(int mode){return switch(mode){
        case GuardIconLayout.CENTER -> "Original position beneath the crosshair.";
        case GuardIconLayout.TOP_CENTER -> "Below visible boss bars.";
        case GuardIconLayout.BOTTOM_CENTER -> "Above the hotbar and visible bars using shared HUD spacing.";
        case GuardIconLayout.HOTBAR_LEFT,GuardIconLayout.HOTBAR_RIGHT -> "Beside the hotbar, outside the offhand slot and reported extra slots. Adjust Gap for other HUDs.";
        default -> "Position near this corner; Gap controls the edge margin.";
    };}
    private boolean editable(){return canEdit.getAsBoolean();}
    @Override public void tick(){if(wasEditable!=editable())rebuild();}
    private void rebuild(){clearWidgets();init();}
    private int size(){return GuardIconLayout.scaledSize(values[GuardScreenSettings.ICON_SIZE]);}
    private GuardIconLayout.Position position(){return GuardIconLayout.position(values[GuardScreenSettings.ICON_ANCHOR],values[GuardScreenSettings.ICON_X],values[GuardScreenSettings.ICON_Y],values[GuardScreenSettings.ICON_GAP],width,height,size());}
    private boolean overIcon(double x,double y){var p=position();int radius=size()/2+4;return Math.abs(x-p.x())<=radius&&Math.abs(y-p.y())<=radius;}
    private void move(double x,double y){
        values[GuardScreenSettings.ICON_ANCHOR]=GuardIconLayout.CUSTOM;
        values[GuardScreenSettings.ICON_X]=GuardIconLayout.fraction(x-dragX,width,size());
        values[GuardScreenSettings.ICON_Y]=GuardIconLayout.fraction(y-dragY,height,size());
        if(presetsButton!=null)presetsButton.setMessage(Component.literal("Custom ▾"));
        valueChanged();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(!hidden&&previewOpacity()>0.001F&&(presetsOpen&&layout.inPresets(x,y)||!presetsOpen&&layout.inDock(x,y)))return super.mouseClicked(x,y,button);
        if(button==0&&editable()&&!presetsOpen&&overIcon(x,y)){var p=position();dragX=x-p.x();dragY=y-p.y();dragging=true;setFocused(null);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(dragging&&button==0&&editable()){move(x,y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(dragging&&button==0&&editable()){move(x,y);dragging=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(hidden||previewOpacity()<=0.001F)return super.keyPressed(key,scan,modifiers);
        if(key==GLFW.GLFW_KEY_ESCAPE&&presetsOpen){presetsOpen=false;rebuild();return true;}
        if(editable()&&getFocused()==null&&key>=GLFW.GLFW_KEY_RIGHT&&key<=GLFW.GLFW_KEY_UP){
            var p=position();int step=GuardUi.preciseStep();dragX=dragY=0;
            move(p.x()+(key==GLFW.GLFW_KEY_RIGHT?step:key==GLFW.GLFW_KEY_LEFT?-step:0),p.y()+(key==GLFW.GLFW_KEY_DOWN?step:key==GLFW.GLFW_KEY_UP?-step:0));return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    private boolean changed(){for(int i:SLOTS)if(values[i]!=saved[i])return true;return false;}
    private void apply(){
        if(!editable())return;
        accept.accept(values.clone());if(parent instanceof GuardConfigScreen config&&!config.applyExternalChanges())return;
        for(int i:SLOTS)saved[i]=values[i];Minecraft.getInstance().setScreen(parent);
    }
    @Override public void onClose(){
        if(editable()&&changed())GuardUi.choices(this,"Unsaved position","Apply your icon position or discard these changes.",new GuardUi.Choice("Apply",this::apply),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
        else Minecraft.getInstance().setScreen(parent);
    }
    @Override protected boolean blurPreview(){return false;}
    @Override public boolean isPauseScreen(){return true;}
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float delta) {}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        var p=position();GuardClient.renderIconPreview(graphics,p.x(),p.y(),size());
        if(dragging||editable()&&overIcon(mouseX,mouseY))graphics.renderOutline(Math.round(p.x()-size()/2F)-4,Math.round(p.y()-size()/2F)-4,size()+8,size()+8,GuardUi.HIGHLIGHT);
        beginPreview(graphics);
        if(presetsOpen){
            GuardUi.panel(graphics,layout.presetX()-8,layout.presetY(),layout.presetWidth()+16,128,false);
            graphics.drawCenteredString(font,"POSITION PRESETS",width/2,layout.presetY()+8,GuardUi.TEXT);
        }else{
            GuardUi.panel(graphics,layout.left(),layout.top(),layout.width(),layout.height(),false);
            graphics.drawCenteredString(font,"ICON POSITION",width/2,layout.top()+7,GuardUi.TEXT);
            graphics.drawCenteredString(font,editable()?"Drag the shield; arrows nudge. Shift: 10 px.":"Position is locked by the server.",width/2,layout.statusY(),GuardUi.MUTED);
        }
        super.render(graphics,mouseX,mouseY,delta);endPreview(graphics);
    }
}
