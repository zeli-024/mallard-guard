package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import dev.zeli.mallardguard.GuardPoseSettings;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class GuardPoseEditorScreen extends GuardPreviewScreen {
    private final GuardPoseManagerScreen parent;
    private GuardPoseLibrary.Pose pose,saved;
    private int part,scroll,contentHeight,sectionOffset;
    private int workspace;
    private GuardUiLayout.PoseEditor layout;
    private Button undoButton;
    private int uiOpacity=100;
    private boolean previewOn=true,previewStarted,sequencePreview,parried,draggingScroll;
    private double scrollGrab;
    private final List<AbstractWidget> formWidgets=new ArrayList<>();
    private final List<Caption> captions=new ArrayList<>();
    private record Caption(String text,int x,int y,int width) {}
    private static final String[] PARTS={"Guard Hand","Support Hand","Held Item"};
    private static final String[] AXES={"Side","Height","Depth","Pitch","Yaw","Roll"};
    private static final String[] CURVES={"Constant","Slow Start","Slow Finish","Smooth"};

    GuardPoseEditorScreen(GuardPoseManagerScreen parent,GuardPoseLibrary.Pose pose){super("Punchy Pose Editor");this.parent=parent;this.pose=pose.copy();saved=parent.saved.poses.stream().filter(p->p.id.equals(pose.id)).findFirst().orElse(pose).copy();}
    private int[] values(){return parried?pose.parriedValues:pose.values;}
    private int[] savedValues(){return parried?saved.parriedValues:saved.values;}
    private boolean changed(){return !GuardPoseLibrary.samePose(pose,saved);}
    private void rebuild(){clearWidgets();init();}
    private void selectStage(boolean next){parried=next;scroll=0;sequencePreview=false;previewStarted=false;rebuild();}
    private void preview(){if(previewOn){sequencePreview=false;PunchyGuardCompat.livePreviewPose(values());previewStarted=true;}}
    private void playSequence(){previewOn=sequencePreview=true;PunchyGuardCompat.previewSequence(pose);previewStarted=true;rebuild();}
    private void mirrorGuardHand(){for(int axis=0;axis<3;axis++){values()[7+axis]=values()[1+axis]*(axis==0?-1:1);values()[10+axis]=values()[4+axis]*(axis==0?1:-1);}preview();rebuild();}
    private void useGuardPlacement(){System.arraycopy(pose.values,1,pose.parriedValues,1,18);preview();rebuild();}
    private void undoAll(){
        if(!parent.editable)return;
        pose=saved.copy();sequencePreview=previewStarted=draggingScroll=adjusting=false;rebuild();
    }
    private void undoPart(){System.arraycopy(savedValues(),1+part*6,values(),1+part*6,6);preview();rebuild();}
    private void undoTiming(){System.arraycopy(savedValues(),19,values(),19,4);if(parried)pose.parriedHoldMs=saved.parriedHoldMs;else pose.maintainHeld=saved.maintainHeld;PunchyGuardCompat.previewMaintain(parried||pose.maintainHeld);preview();rebuild();}
    private void undoItems(){pose.items=new ArrayList<>(saved.items);pose.otherHandItems=new ArrayList<>(saved.otherHandItems);pose.weaponItems=new ArrayList<>(saved.weaponItems);pose.excludedItems=new ArrayList<>(saved.excludedItems);pose.otherHandExcluded=new ArrayList<>(saved.otherHandExcluded);pose.weaponExcluded=new ArrayList<>(saved.weaponExcluded);pose.exclusiveItems=new ArrayList<>(saved.exclusiveItems);pose.handMode=saved.handMode;pose.otherHandHeldOnly=saved.otherHandHeldOnly;pose.prepareRules();preview();rebuild();}
    private void copyPreset(){
        Minecraft.getInstance().setScreen(new GuardPoseCopyScreen(this,parent.draft.poses,pose.id,source->{
            String id=pose.id,name=pose.name;boolean builtIn=pose.preset,enabled=pose.enabled;
            pose=source.copy();pose.exclusiveItems.clear();pose.id=id;pose.name=name;pose.preset=builtIn;pose.enabled=enabled;pose.values[0]=enabled?1:0;
            previewStarted=sequencePreview=false;
        }));
    }
    private static boolean position(int field){return (field-1)%6<3;}
    private static double limit(int field){return position(field)?20:180;}
    private double offset(int field){return values()[field]/(position(field)?10.0:1.0);}
    private Button button(String text,int x,int y,int w,Runnable action,String tip){
        var button=addRenderableWidget(GuardUi.button(text,x,y,w,20,action));button.setTooltip(GuardUi.tooltip(tip));return button;
    }
    private <T extends AbstractWidget> T form(T widget){formWidgets.add(widget);return addRenderableWidget(widget);}
    private void formButton(String label,int row,Runnable action,String tip){var b=form(GuardUi.button(label,layout.x(),contentTop()+sectionOffset+row-scroll,layout.contentWidth(),20,action));b.active=parent.editable;b.setTooltip(GuardUi.tooltip(tip));}
    private int contentTop(){return layout.contentY()+(workspace==0?26:0);}
    private void caption(String text,int x,int row){captions.add(new Caption(text,x,contentTop()+sectionOffset+row-scroll,layout.contentWidth()));}
    private void columnCaption(String text,int x,int row,int width){captions.add(new Caption(text,x,contentTop()+sectionOffset+row-scroll,width));}
    private void help(){GuardUi.choices(this,"Pose Editor Help","Guarding is the held stance; Parried is the recoil after a successful parry and hitlag.\n\nChoose the guard hand, support hand or held item. Position uses model pixels; rotation uses degrees, relative to the original pose. Drag values or right-click to type.\n\nTiming controls entry, hold and return. Play Animation previews both poses, which share item assignments. Undo restores the whole preset. Save commits the preset; Cancel discards your edits.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));}
    private void selectWorkspace(int value){workspace=value;scroll=0;rebuild();}
    @Override protected void init(){
        layout=GuardUiLayout.poseEditor(width,height);formWidgets.clear();captions.clear();
        int half=(layout.width()-22)/2;
        for(int i=0;i<2;i++){boolean stage=i==1;var tab=addRenderableWidget(GuardUi.tab(stage?"Parried":"Guarding",layout.left()+8+i*(half+6),layout.top()+28,half,stage==parried,()->selectStage(stage)));tab.setTooltip(GuardUi.tooltip(stage?"Successful-parry recoil, played after hitlag.":"Stance entered when guarding."));}
        undoButton=addRenderableWidget(GuardUi.iconButton("undo","Undo",layout.undoX(),layout.headerY(),20,this::undoAll));
        undoButton.active=parent.editable&&changed();undoButton.setTooltip(GuardUi.tooltip("Restore both poses, timing and item assignments to their last saved values.\nKeeps your current tab and UI opacity. New presets restore their starting values."));
        var copy=addRenderableWidget(GuardUi.iconButton("copy","Copy Preset",layout.copyX(),layout.headerY(),20,this::copyPreset));copy.active=parent.editable;
        button("?",layout.helpX(),layout.headerY(),20,this::help,"Help with poses, placement, timing and assignments.");
        int tabWidth=(layout.inner()-8)/3;String[] tabs={"Pose","Timing","Items"};
        for(int i=0;i<tabs.length;i++){int page=i;addRenderableWidget(GuardUi.tab(tabs[i],layout.x()+i*(tabWidth+4),layout.tabsY(),i==2?layout.inner()-2*(tabWidth+4):tabWidth,workspace==i,()->selectWorkspace(page)));}
        contentHeight=workspace==0?118+(part==1?24:0)+(parried?24:0):workspace==1?168:160;
        scroll=Math.clamp(scroll,0,Math.max(0,contentHeight-(layout.contentBottom()-contentTop())));
        sectionOffset=Math.max(0,(layout.contentBottom()-contentTop()-contentHeight)/2);
        if(workspace==0){
            int w=(layout.contentWidth()-8)/3;
            for(int i=0;i<PARTS.length;i++){int selected=i;var b=addRenderableWidget(GuardUi.tab(PARTS[i],layout.x()+i*(w+4),layout.contentY(),i==2?layout.contentWidth()-2*(w+4):w,part==i,()->selectPart(selected)));b.setTooltip(GuardUi.tooltip(i==0?"The hand selected to guard, which may be your offhand.":i==1?"The other, supporting hand.":"Held item placement relative to the guarding hand."));}
            placement();
        }else if(workspace==1)timing();else assignments();
        for(var widget:formWidgets){widget.visible=widget.getY()>=contentTop()&&widget.getY()+widget.getHeight()<=layout.contentBottom();if(!widget.visible)widget.active=false;}
        int pw=layout.compact()?68:(layout.inner()-6)/2,px=layout.x(),py=layout.previewY();
        button(sequencePreview?"Edit Pose":layout.compact()?"Play":"Play Animation",px,py,pw,()->{if(sequencePreview){previewOn=true;sequencePreview=false;preview();rebuild();}else playSequence();},"Play guarding entry, recoil, hold and return. Edit Pose returns to a still pose.");
        visibility=button("Hide UI",layout.compact()?px+layout.inner()-pw:px+pw+6,py,layout.compact()?pw:layout.inner()-pw-6,this::toggleVisibility,"Hide controls to inspect the pose. Click or press Escape to restore.");
        var opacity=addRenderableWidget(new GuardSlider(layout.compact()?px+pw+6:px,layout.opacityY(),layout.compact()?layout.inner()-2*(pw+6):layout.inner(),20,uiOpacity,0,100,1,v->"UI Opacity: "+v+"%",v->{uiOpacity=v;previewOpacityChanged();},()->false));opacity.setTooltip(GuardUi.tooltip("Controls stay at this opacity while editing.\nAt 0%, click anywhere or press Escape to restore them."));
        int footerWidth=(layout.inner()-6)/2;
        button("Cancel",layout.x(),layout.footerY(),footerWidth,this::cancel,"Discard this unsaved preset draft and return.");
        button("Save",layout.x()+footerWidth+6,layout.footerY(),layout.inner()-footerWidth-6,this::save,"Save both poses, timing and item assignments.").active=parent.editable;
        if(previewOn&&!previewStarted){sequencePreview=false;PunchyGuardCompat.previewPose(values(),parried||pose.maintainHeld);previewStarted=true;}
    }
    private void cancel(){if(changed())GuardUi.confirm(this,"Discard changes?","Discard the unsaved edits to this preset?",()->Minecraft.getInstance().setScreen(parent));else Minecraft.getInstance().setScreen(parent);}
    private void selectPart(int selected){part=selected;scroll=0;rebuild();}
    private void placement(){
        int col=(layout.contentWidth()-6)/2,right=layout.x()+col+6;
        columnCaption("Position (px)",layout.x(),0,col);columnCaption("Rotation (°)",right,0,col);
        for(int axis=0;axis<3;axis++){
            var move=form(new PoseSlider(layout.x(),contentTop()+sectionOffset+14+axis*24-scroll,col,1+part*6+axis));move.active=parent.editable;
            var rotate=form(new PoseSlider(right,contentTop()+sectionOffset+14+axis*24-scroll,col,4+part*6+axis));rotate.active=parent.editable;
        }
        int row=90;
        if(part==1){formButton("Mirror Guard Hand",row,this::mirrorGuardHand,"Mirror guard-hand placement into the supporting hand for this pose.");row+=24;}
        if(parried){formButton("Use Guarding Placement",row,this::useGuardPlacement,"Copy all guarding hand and weapon placement into recoil.\nTiming and assignments are kept.");row+=24;}
        formButton("Restore Saved Placement",row,this::undoPart,"Restore this part of this pose to its last saved placement.");
    }
    private void timing(){
        caption(parried?"Move to recoil, hold, return":"Enter guard, hold, return",layout.x(),0);
        sectionOffset+=18;
        motion(parried?"Recoil Time":"Entry Time",0,19,500,"Time to reach this pose. 0 ms switches immediately.\nDrag: 10 ms; scroll: 1 ms; Shift-scroll: 10 ms.");
        if(parried){
            var hold=form(new GuardSlider(layout.x(),contentTop()+sectionOffset+26-scroll,layout.contentWidth(),20,pose.parriedHoldMs,0,500,GuardUi.dragStep(0,500,1),v->"Hold Time: "+v+" ms",v->pose.parriedHoldMs=v,()->pose.parriedHoldMs!=saved.parriedHoldMs));hold.active=parent.editable;hold.setTooltip(GuardUi.tooltip("Hold recoil after reaching it, then return.\nRecoil starts after hitlag. Edit Pose stays still for editing."));
        }else formButton("Hold While Guarding: "+GuardUi.enabledLabel(pose.maintainHeld),26,()->{pose.maintainHeld=!pose.maintainHeld;PunchyGuardCompat.previewMaintain(pose.maintainHeld);rebuild();},"On holds until guarding ends. Off plays entry and return once.");
        motion("Return Time",52,20,500,"Time to return to rest. A newer animation replaces it immediately.");
        formButton("Movement: "+CURVES[values()[21]],78,()->GuardUi.choices(this,"Movement Curve","Choose how the transition changes speed.",new GuardUi.Choice("Constant Speed",()->setCurve(0)),new GuardUi.Choice("Slow Start",()->setCurve(1)),new GuardUi.Choice("Slow Finish",()->setCurve(2)),new GuardUi.Choice("Smooth Start & Finish",()->setCurve(3))),"Constant keeps one speed. The other curves accelerate, slow down or do both.");
        if(values()[21]!=0)motion("Curve Amount",104,22,200,"How strongly movement follows the chosen curve. 100% is normal.");
        formButton("Restore Saved Timing",130,this::undoTiming,"Restore this pose's last saved timing. Placement and items are kept.");
    }
    private void setCurve(int value){values()[21]=value;preview();Minecraft.getInstance().setScreen(this);}
    private static String summary(List<String> included,List<String> excluded){return included.size()+" allow rules / "+excluded.size()+" block rules";}
    private void assignSelection(){
        Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Preset Item Assignments",pose.items,pose.excludedItems,pose.handMode,true,value->{pose.items=new ArrayList<>(value.allowed());pose.excludedItems=new ArrayList<>(value.denied());pose.handMode=value.handMode();pose.prepareRules();
            if(pose.items.stream().noneMatch(q->!q.startsWith("!"))){pose.exclusiveItems.clear();pose.prepareRules();return;}
            GuardUi.choices(this,"Preset Assignment Mode","Should assigned items use only this preset, or remain available to other matching presets?",new GuardUi.Choice("Use Only This Preset",()->{pose.exclusiveItems=new ArrayList<>(pose.items);pose.prepareRules();Minecraft.getInstance().setScreen(this);}),new GuardUi.Choice("Allow Other Presets",()->{pose.exclusiveItems.clear();pose.prepareRules();Minecraft.getInstance().setScreen(this);}));
        }));
    }
    private void assignments(){
        caption("Both poses share these assignments",layout.x(),0);
        formButton("Preset Items",16,this::assignSelection,"Choose items that use this preset.\n"+summary(pose.items,pose.excludedItems)+". No whitelist follows eligibility.");
        formButton("Supporting Hand Items",42,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Supporting Hand Items",pose.otherHandItems,pose.otherHandExcluded,0,false,value->{pose.otherHandItems=new ArrayList<>(value.allowed());pose.otherHandExcluded=new ArrayList<>(value.denied());pose.prepareRules();})),"Items receiving supporting-hand placement.\n"+summary(pose.otherHandItems,pose.otherHandExcluded));
        formButton("Item Offset Items",68,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Item Offset Items",pose.weaponItems,pose.weaponExcluded,0,false,value->{pose.weaponItems=new ArrayList<>(value.allowed());pose.weaponExcluded=new ArrayList<>(value.denied());pose.prepareRules();})),"Items receiving weapon offsets.\n"+summary(pose.weaponItems,pose.weaponExcluded));
        formButton("Empty Other Hand: "+(pose.otherHandHeldOnly?"Hidden":"Allowed"),94,()->{pose.otherHandHeldOnly=!pose.otherHandHeldOnly;preview();rebuild();},"Allowed also animates an empty supporting hand.\nHeld items must match the supporting-hand assignments.");
        formButton("Restore Saved Assignments",120,this::undoItems,"Restore assignments, exclusions, exclusivity and hand rules.");
        caption(pose.exclusiveItems.isEmpty()?"Selection: shared preset pool":"Selection: exclusive preset",layout.x(),146);
    }
    private void motion(String label,int row,int field,int maximum,String tip){
        var slider=form(new GuardSlider(layout.x(),contentTop()+sectionOffset+row-scroll,layout.contentWidth(),20,values()[field],0,maximum,GuardUi.dragStep(0,maximum,1),value->label+": "+value+(field==22?"%":" ms"),value->{if(value!=values()[field]){values()[field]=value;preview();}},()->values()[field]!=savedValues()[field]));
        slider.active=parent.editable;slider.setTooltip(GuardUi.tooltip(tip));
    }
    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(layout.trackX(),contentTop(),layout.contentBottom()-contentTop(),layout.contentBottom()-contentTop(),contentHeight,scroll);}
    private void scrollTo(double y){int next=scrollbar().scrollAt(y,scrollGrab);if(next!=scroll){scroll=next;rebuild();}}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(!hidden&&previewOpacity()>0.001F&&button==0&&scrollbar().contains(x,y)){draggingScroll=true;scrollGrab=scrollbar().grab(y);scrollTo(y);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll&&button==0){scrollTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingScroll){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(hidden||previewOpacity()<=0.001F)return super.mouseScrolled(x,y,sx,sy);
        if(super.mouseScrolled(x,y,sx,sy))return true;
        if(x>=layout.left()&&x<layout.left()+layout.width()&&y>=contentTop()&&y<layout.contentBottom()&&sy!=0){int next=Math.clamp(scroll-(int)Math.signum(sy)*24,0,scrollbar().max());if(next!=scroll){scroll=next;rebuild();}return true;}
        return false;
    }
    private void save(){if(pose.name.isBlank())Minecraft.getInstance().setScreen(new GuardPoseNameScreen(this,pose.name,parent.draft.poses,name->{pose.name=name;commitSave();}));else commitSave();}
    private void commitSave(){
        var proposed=parent.draft.poses.stream().map(GuardPoseLibrary.Pose::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int existing=-1;for(int i=0;i<proposed.size();i++)if(proposed.get(i).id.equals(pose.id)){existing=i;break;}
        if(existing<0)proposed.add(pose.copy());else proposed.set(existing,pose.copy());
        GuardPoseOwnershipScreen.review(this,proposed,pose.exclusiveItems.isEmpty()?null:pose.id,result->{
            var previous=parent.draft.poses;parent.draft.poses=result;
            if(parent.applyDraft()){saved=pose.copy();Minecraft.getInstance().setScreen(parent);}else parent.draft.poses=previous;
        });
    }
    @Override protected boolean blurPreview(){return false;}
    @Override protected float previewOpacity(){return uiOpacity/100.0F;}
    @Override protected void restorePreviewOpacity(){uiOpacity=100;rebuild();}
    @Override protected boolean fadesWhileAdjusting(AbstractSliderButton slider){return false;}
    @Override public void render(GuiGraphics g,int x,int y,float d){
        beginPreview(g);
        boolean modified=changed();undoButton.active=parent.editable&&modified;
        GuardUi.panel(g,layout.left(),layout.top(),layout.width(),layout.bottom()-layout.top(),false);
        g.drawCenteredString(font,font.plainSubstrByWidth(pose.name.isBlank()?"New preset":pose.name,layout.titleWidth()),layout.left()+layout.width()/2,layout.top()+7,GuardUi.TEXT);
        g.drawCenteredString(font,pose.name.isBlank()?"New draft":modified?"Unsaved edits":"Saved values",layout.left()+layout.width()/2,layout.statusY(),modified?GuardUi.HIGHLIGHT:GuardUi.MUTED);
        for(var widget:renderables)if(!formWidgets.contains(widget))widget.render(g,x,y,d);
        g.enableScissor(layout.x(),contentTop(),layout.trackX()-4,layout.contentBottom());
        for(var text:captions)g.drawCenteredString(font,font.plainSubstrByWidth(text.text(),text.width()),text.x()+text.width()/2,text.y(),GuardUi.MUTED);
        for(var widget:formWidgets)widget.render(g,x,y,d);
        g.disableScissor();GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);endPreview(g);
    }
    @Override public void removed(){draggingScroll=false;PunchyGuardCompat.clearPreview();previewStarted=false;super.removed();}
    @Override public void onClose(){if(changed())GuardUi.choices(this,"Unsaved preset","Save or discard these changes.",new GuardUi.Choice("Save",this::save),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    private final class PoseSlider extends AbstractSliderButton {
        final int field;boolean typing;String text="";
        PoseSlider(int x,int y,int w,int field){super(x,y,w,20,net.minecraft.network.chat.Component.empty(),Math.clamp((offset(field)+limit(field))/(2*limit(field)),0,1));this.field=field;updateMessage();setTooltip(GuardUi.tooltip(AXES[(field-1)%6]+" ("+"XYZ".charAt((field-1)%3)+") in the part's local coordinates. Zero keeps its original value.\nRange: "+(position(field)?"-20 to 20 model pixels":"-180 to 180 degrees")+".\nDrag or scroll; right-click to type.\nShift-scroll or Shift-arrow uses ten steps."));}
        @Override public void onClick(double x,double y){typing=false;super.onClick(x,y);}
        @Override public boolean mouseClicked(double x,double y,int b){if(active&&visible&&isMouseOver(x,y)&&b==1){typing=true;text="";setFocused(true);return true;}return super.mouseClicked(x,y,b);}
        @Override public boolean charTyped(char c,int modifiers){if(!active||!isFocused()||!(Character.isDigit(c)||c=='-'||c=='.'))return false;if(!typing){typing=true;text="";}if(text.length()<8)text+=c;parse();return true;}
        @Override public boolean keyPressed(int key,int scan,int mods){if(!active||!visible)return false;if(typing&&key==GLFW.GLFW_KEY_BACKSPACE){if(!text.isEmpty())text=text.substring(0,text.length()-1);parse();return true;}if(typing&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){parse();typing=false;return true;}if(!typing&&(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)){precise(key==GLFW.GLFW_KEY_LEFT?-1:1);return true;}return super.keyPressed(key,scan,mods);}
        private void parse(){try{double n=Double.parseDouble(text);if(Double.isFinite(n)&&n>=-limit(field)&&n<=limit(field)){value=(n+limit(field))/(2*limit(field));applyValue();updateMessage();}}catch(NumberFormatException ignored){}}
        private void precise(int direction){value=Math.clamp((offset(field)+direction*(position(field)?.1:1)*GuardUi.preciseStep()+limit(field))/(2*limit(field)),0,1);applyValue();updateMessage();}
        @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(!active||!visible||!isMouseOver(x,y)||sy==0)return false;precise((int)Math.signum(sy));return true;}
        @Override protected void updateMessage(){setMessage(net.minecraft.network.chat.Component.literal(AXES[(field-1)%6]+": "+(offset(field)==(int)offset(field)?Integer.toString((int)offset(field)):String.format(Locale.ROOT,"%.1f",offset(field)))+(position(field)?" px":"°")));}
        @Override protected void applyValue(){double n=value*(2*limit(field))-limit(field);int scale=position(field)?10:1;int next=Math.clamp((int)Math.round(n*scale),GuardPoseSettings.min(field),GuardPoseSettings.max(field));if(next!=values()[field]){values()[field]=next;preview();}}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float d){
            Component previous=getMessage();
            if(typing)setMessage(Component.literal(AXES[(field-1)%6]+": "+text+"_"));
            GuardUi.slider(g,this,value,values()[field]!=savedValues()[field]);
            if(typing)setMessage(previous);
        }
    }
}
