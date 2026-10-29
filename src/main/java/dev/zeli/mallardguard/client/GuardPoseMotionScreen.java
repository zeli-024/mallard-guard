package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseMotionScreen extends Screen {
    private final Screen parent;
    private final Consumer<GuardPoseLibrary.Pose> accept;
    private final GuardPoseLibrary.Pose original;
    private GuardPoseLibrary.Pose draft;
    private GuardUiLayout.PoseBrowser layout;
    private boolean parried;
    private int[] values(){return parried?draft.parriedValues:draft.values;}
    private int[] originalValues(){return parried?original.parriedValues:original.values;}
    GuardPoseMotionScreen(Screen parent,GuardPoseLibrary.Pose pose,Consumer<GuardPoseLibrary.Pose> accept){super(Component.literal("Preset Timing"));this.parent=parent;this.original=pose.copy();this.draft=pose.copy();this.accept=accept;}
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        layout=GuardUiLayout.poseBrowser(width,height);
        int x=layout.x(),inner=layout.inner(),col=(inner-6)/2,right=x+col+6,y=layout.top()+66;
        for(int i=0;i<2;i++){final boolean stage=i==1;addRenderableWidget(GuardUi.tab(stage?"Parried":"Guarding",i==0?x:right,layout.searchY(),i==0?col:inner-col-6,stage==parried,()->{parried=stage;rebuild();}));}
        slider(parried?"Recoil Time":"Entry Time",19,500,x,y,col,"Time to reach this pose. 0 ms switches immediately.\nDrag: 10 ms; scroll: 1 ms; Shift-scroll: 10 ms.");
        if(parried){
            var hold=addRenderableWidget(new GuardSlider(right,y,inner-col-6,20,draft.parriedHoldMs,0,500,GuardUi.dragStep(0,500,1),value->"Hold Time: "+value+" ms",value->draft.parriedHoldMs=value,()->draft.parriedHoldMs!=original.parriedHoldMs));
            hold.setTooltip(GuardUi.tooltip("Wait at recoil before returning. Recoil begins after hitlag.\nNewer animations can replace the pose."));
        }else addRenderableWidget(GuardUi.button("Hold While Guarding: "+GuardUi.enabledLabel(draft.maintainHeld),right,y,inner-col-6,20,()->{draft.maintainHeld=!draft.maintainHeld;rebuild();})).setTooltip(GuardUi.tooltip("On maintains the pose until guarding ends.\nOff plays entry and return once."));
        slider("Return Time",20,500,x,y+26,col,"Time to return to rest. Newer animations can replace it.");
        addRenderableWidget(GuardUi.button("Movement: "+new String[]{"Constant","Slow Start","Slow Finish","Smooth"}[values()[21]],right,y+26,inner-col-6,20,()->GuardUi.choices(this,"Movement Curve","Choose how the transition changes speed.",new GuardUi.Choice("Constant Speed",()->setCurve(0)),new GuardUi.Choice("Slow Start",()->setCurve(1)),new GuardUi.Choice("Slow Finish",()->setCurve(2)),new GuardUi.Choice("Smooth Start & Finish",()->setCurve(3))))).setTooltip(GuardUi.tooltip("Movement curve for entering and returning."));
        slider("Curve Amount",22,200,x,y+52,inner,"Curve strength from 0 to 200%. Constant Speed ignores this value.");
        addRenderableWidget(GuardUi.button("Undo Both Tabs",x,y+80,inner,20,()->{draft=original.copy();rebuild();})).setTooltip(GuardUi.tooltip("Restore guarding and parried timing to their values when this screen opened."));
        addRenderableWidget(GuardUi.button("?",x,layout.footerY(),22,20,()->GuardUi.choices(this,"Timing Help","Entry or Recoil Time controls travel into a pose. Hold waits before returning; Guarding Hold lasts until guarding ends.\n\nDrag in 10 ms steps or scroll in 1 ms steps; Shift-scroll uses 10 ms. Save applies both tabs to every selected preset.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        int actionWidth=Math.min(100,(inner-38)/2);
        addRenderableWidget(GuardUi.button("Save",x+inner-2*actionWidth-6,layout.footerY(),actionWidth,20,()->accept.accept(draft.copy())));
        addRenderableWidget(GuardUi.button("Close",x+inner-actionWidth,layout.footerY(),actionWidth,20,this::onClose));
    }
    private void setCurve(int value){values()[21]=value;Minecraft.getInstance().setScreen(this);}
    private void slider(String label,int field,int max,int x,int y,int w,String tooltip){
        var slider=addRenderableWidget(new GuardSlider(x,y,w,20,values()[field],0,max,GuardUi.dragStep(0,max,1),value->label+": "+value+(field==22?"%":" ms"),value->values()[field]=value,()->values()[field]!=originalValues()[field]));slider.setTooltip(GuardUi.tooltip(tooltip));
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,layout.left(),layout.top(),layout.width(),layout.bottom()-layout.top(),false);
        g.drawString(font,"Preset Timing",layout.x(),layout.top()+10,GuardUi.TEXT);
        g.drawString(font,"Both tabs are saved together",layout.x(),layout.top()+22,GuardUi.MUTED);
        for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void onClose(){
        if(!GuardPoseLibrary.samePose(draft,original))GuardUi.choices(this,"Unsaved timing","Save or discard these timing changes.",new GuardUi.Choice("Save",()->accept.accept(draft.copy())),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
        else Minecraft.getInstance().setScreen(parent);
    }
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
