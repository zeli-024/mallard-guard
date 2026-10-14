package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseNameScreen extends Screen {
    private final Screen parent;
    private final List<GuardPoseLibrary.Pose> poses;
    private final Consumer<String> accept;
    private String name;
    private int left,top,w;
    GuardPoseNameScreen(Screen parent,String name,List<GuardPoseLibrary.Pose> poses,Consumer<String> accept){
        super(Component.literal("Name Preset"));this.parent=parent;this.name=name;this.poses=poses;this.accept=accept;
    }
    @Override protected void init(){
        w=Math.min(340,GuardUiLayout.wideWidth(width));left=(width-w)/2;top=Math.max(GuardUiLayout.screenTop(height),(height-116)/2);
        EditBox box=addRenderableWidget(GuardUi.editBox(font,left+8,top+29,w-16,"Preset name"));
        box.setMaxLength(64);box.setValue(name);box.setResponder(value->name=value);
        box.setTooltip(GuardUi.tooltip("Name this preset. Leave blank for a random unused name.\nSaving commits both poses, motion and assignments."));
        addRenderableWidget(GuardUi.button("Randomize Name",left+8,top+53,w-16,18,()->box.setValue(GuardPoseNames.unused(poses))));
        int bw=(w-22)/2;
        addRenderableWidget(GuardUi.button("Save",left+8,top+87,bw,20,()->accept.accept(name.isBlank()?GuardPoseNames.unused(poses):name.strip())));
        addRenderableWidget(GuardUi.button("Back",left+14+bw,top+87,bw,20,this::onClose));
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,left,top,w,116,false);g.drawCenteredString(font,title,width/2,top+9,GuardUi.TEXT);for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
