package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseManagerScreen extends Screen {
    static final class Draft {
        List<GuardPoseLibrary.Pose> poses;boolean enabled=true;
        Draft(List<GuardPoseLibrary.Pose> poses){this.poses=new ArrayList<>(poses.stream().map(GuardPoseLibrary.Pose::copy).toList());}
        boolean same(Draft other){return other!=null&&enabled==other.enabled&&GuardPoseLibrary.samePoses(poses,other.poses);}
        Draft copy(){Draft d=new Draft(poses);d.enabled=enabled;return d;}
    }
    final Screen parent;final Consumer<Draft> accept;final boolean editable;
    Draft draft,saved;
    private GuardUiLayout.PoseBrowser layout;
    private int start,kind;
    private EditBox search;
    private String searchText="";
    private List<GuardPoseLibrary.Pose> visiblePoses=List.of();
    private boolean draggingScroll,selecting;
    private double scrollGrab;
    private final Set<String> selectedIds=new HashSet<>();

    GuardPoseManagerScreen(Screen parent,Draft draft,boolean editable,Consumer<Draft> accept){super(Component.literal("Punchy Presets"));this.parent=parent;this.draft=draft.copy();saved=this.draft.copy();this.editable=editable;this.accept=accept;}
    private boolean changed(){return !draft.same(saved);}
    boolean applyDraft(){accept.accept(draft.copy());if(parent instanceof GuardConfigScreen config&&!config.applyExternalChanges())return false;saved=draft.copy();return true;}
    private void rebuild(){clearWidgets();init();}
    private void commit(){if(applyDraft())Minecraft.getInstance().setScreen(this);}
    private List<GuardPoseLibrary.Pose> selected(){return draft.poses.stream().filter(p->selectedIds.contains(p.id)).toList();}
    private Button button(String label,int x,int y,int width,Runnable action,String tip){var b=addRenderableWidget(GuardUi.button(label,x,y,width,20,action));b.setTooltip(GuardUi.tooltip(tip));return b;}
    private void motion(List<GuardPoseLibrary.Pose> targets){
        if(targets.isEmpty())return;
        Minecraft.getInstance().setScreen(new GuardPoseMotionScreen(this,targets.getFirst(),value->{for(var p:targets){System.arraycopy(value.values,19,p.values,19,4);System.arraycopy(value.parriedValues,19,p.parriedValues,19,4);p.parriedHoldMs=value.parriedHoldMs;p.maintainHeld=value.maintainHeld;}commit();}));
    }
    private void assign(List<GuardPoseLibrary.Pose> targets){
        if(targets.isEmpty())return;var first=targets.getFirst();
        Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Batch Preset Assignments",first.items,first.excludedItems,first.handMode,true,value->{
            Runnable shared=()->assignMode(targets,value,false);
            if(targets.size()==1&&value.allowed().stream().anyMatch(q->!q.startsWith("!"))){
                GuardUi.choices(this,"Preset Assignment Mode","Should assigned items use only this preset, or remain available to other matching presets?",new GuardUi.Choice("Use Only This Preset",()->assignMode(targets,value,true)),new GuardUi.Choice("Allow Other Presets",shared),new GuardUi.Choice("Cancel",()->Minecraft.getInstance().setScreen(this)));
            }else GuardUi.choices(this,"Shared Preset Assignments","Apply these rules to the selected presets?\nThey will share matching items; previous exclusive ownership on these presets will be cleared.",new GuardUi.Choice("Apply Shared Rules",shared),new GuardUi.Choice("Cancel",()->Minecraft.getInstance().setScreen(this)));
        }));
    }
    private void assignMode(List<GuardPoseLibrary.Pose> targets,GuardPoseItemScreen.PoseAssignments value,boolean exclusive){
        var ids=targets.stream().map(p->p.id).collect(java.util.stream.Collectors.toSet());
        var proposed=draft.poses.stream().map(GuardPoseLibrary.Pose::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for(var p:proposed)if(ids.contains(p.id)){p.items=new ArrayList<>(value.allowed());p.excludedItems=new ArrayList<>(value.denied());p.handMode=value.handMode();p.exclusiveItems=exclusive?new ArrayList<>(value.allowed()):new ArrayList<>();p.prepareRules();}
        GuardPoseOwnershipScreen.review(this,proposed,exclusive?targets.getFirst().id:null,result->{var previous=draft.poses;draft.poses=result;if(applyDraft())Minecraft.getInstance().setScreen(this);else draft.poses=previous;});
    }
    private void rename(GuardPoseLibrary.Pose pose){Minecraft.getInstance().setScreen(new GuardPoseNameScreen(this,pose.name,draft.poses,name->{pose.name=name;commit();}));}
    private void delete(List<GuardPoseLibrary.Pose> targets){
        Set<String> ids=new HashSet<>();for(var p:targets)ids.add(p.id);if(ids.isEmpty())return;
        GuardUi.choices(this,"Delete "+ids.size()+(ids.size()==1?" preset?":" presets?"),"Remove the selected presets and their exclusive assignments? This cannot be undone here.",new GuardUi.Choice("Delete",()->{for(var p:draft.poses)if(ids.contains(p.id)){p.enabled=false;p.values[0]=0;p.deleted=true;p.exclusiveItems.clear();}draft.poses.removeIf(p->ids.contains(p.id)&&!p.preset);selectedIds.removeAll(ids);commit();}),new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
    }
    private void toggle(List<GuardPoseLibrary.Pose> targets,boolean enabled){
        if(!enabled&&targets.stream().anyMatch(p->!p.exclusiveItems.isEmpty())){GuardUi.choices(this,"Disable Exclusive Presets?","These presets own exclusive item assignments. Clear exclusivity so those items can use other presets, or retain it until re-enabled.",new GuardUi.Choice("Clear Exclusivity",()->{for(var p:targets)p.exclusiveItems.clear();setEnabled(targets,false);}),new GuardUi.Choice("Keep Exclusivity",()->setEnabled(targets,false)),new GuardUi.Choice("Cancel",()->Minecraft.getInstance().setScreen(this)));return;}setEnabled(targets,enabled);
    }
    private void setEnabled(List<GuardPoseLibrary.Pose> targets,boolean enabled){for(var p:targets){p.enabled=enabled;p.values[0]=enabled?1:0;}commit();}
    private void batchActions(){
        List<GuardPoseLibrary.Pose> targets=selected();if(targets.isEmpty())return;
        List<GuardUi.Choice> choices=new ArrayList<>();
        choices.add(new GuardUi.Choice("Timing",()->motion(targets)));
        choices.add(new GuardUi.Choice("Items",()->assign(targets)));
        choices.add(new GuardUi.Choice("Enable",()->toggle(targets,true)));
        choices.add(new GuardUi.Choice("Disable",()->toggle(targets,false)));
        if(!targets.isEmpty())choices.add(new GuardUi.Choice("Delete",()->delete(targets)));
        choices.add(new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
        GuardUi.choices(this,"Edit "+targets.size()+" presets","Timing and item edits replace those values\non every selected preset.",choices.toArray(GuardUi.Choice[]::new));
    }
    private void help(){GuardUi.choices(this,"Preset Help","Open a preset to edit its poses. Enabled matching presets are chosen randomly; use the pencil to rename one.\n\nBatch Actions edits several presets together. Select Results includes offscreen search matches.\n\nSave in the editor commits the preset. Other preset actions save immediately.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));}
    @Override protected void init(){
        layout=GuardUiLayout.poseBrowser(width,height);
        String query=searchText.strip().toLowerCase(Locale.ROOT);
        visiblePoses=draft.poses.stream().filter(p->!p.deleted&&(kind==0 || kind==1&&p.preset || kind==2&&!p.preset)&&p.name.toLowerCase(Locale.ROOT).contains(query)).toList();
        start=Math.clamp(start,0,Math.max(0,visiblePoses.size()-layout.rows()));
        int batchWidth=96;
        search=addRenderableWidget(GuardUi.editBox(font,layout.x(),layout.searchY(),layout.inner()-batchWidth-6,"Search presets"));
        search.setMaxLength(64);search.setValue(searchText);search.setTooltip(GuardUi.tooltip("Find presets by name. Filters apply to the search results."));
        search.setResponder(value->{int cursor=search.getCursorPosition();searchText=value;start=0;rebuild();setFocused(search);search.setFocused(true);search.setCursorPosition(Math.min(cursor,value.length()));});
        button(selecting?"Done Selecting":"Batch Select",layout.x()+layout.inner()-batchWidth,layout.searchY(),batchWidth,()->{selecting=!selecting;selectedIds.clear();rebuild();},"Select several presets for shared timing or item changes.").active=editable;
        if(selecting){
            int col=(layout.inner()-6)/2;
            boolean all=!visiblePoses.isEmpty()&&visiblePoses.stream().allMatch(p->selectedIds.contains(p.id));
            button(all?"Clear Results":"Select Results",layout.x(),layout.tabsY(),col,()->{for(var p:visiblePoses){if(all)selectedIds.remove(p.id);else selectedIds.add(p.id);}rebuild();},"Select or clear all results, including off-screen rows.").active=!visiblePoses.isEmpty();
            button("Clear Selection",layout.x()+col+6,layout.tabsY(),layout.inner()-col-6,()->{selectedIds.clear();rebuild();},"Clear every selection, including hidden results.").active=!selectedIds.isEmpty();
        }else{
            String[] names={"All","Built-in","Custom"};int col=(layout.inner()-12)/3;
            for(int i=0;i<names.length;i++){int next=i;addRenderableWidget(GuardUi.tab(names[i],layout.x()+i*(col+6),layout.tabsY(),i==2?layout.inner()-2*(col+6):col,kind==i,()->{kind=next;start=0;rebuild();}));}
        }
        for(int i=start;i<Math.min(visiblePoses.size(),start+layout.rows());i++){
            var pose=visiblePoses.get(i);int y=layout.listY()+(i-start)*GuardUiLayout.POSE_ROW_STEP;
            int actionSize=24,gap=4,rowX=layout.x()+(selecting?0:2*(actionSize+gap));
            int rowWidth=layout.rowWidth()-(selecting?0:3*(actionSize+gap));
            String details=(pose.preset?"Built-in":"Custom")+" · "+(pose.enabled?"Enabled":"Disabled")+" · "+(pose.handMode==2?"Empty hand":pose.items.isEmpty()?"Eligible items":pose.items.size()+" items");
            var row=addRenderableWidget(new Button(rowX,y,rowWidth,GuardUiLayout.POSE_ROW_HEIGHT,Component.literal(pose.name),ignored->{
                if(selecting){if(!selectedIds.remove(pose.id))selectedIds.add(pose.id);rebuild();}
                else Minecraft.getInstance().setScreen(new GuardPoseEditorScreen(this,pose));
            },message->message.get()){
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){GuardUi.listRow(g,this,details,mx,my,selecting&&selectedIds.contains(pose.id));}
            });
            row.setTooltip(GuardUi.tooltip(pose.name+"\n"+details+"\n"+(selecting?"Click to select or deselect.":editable?"Click to edit both poses.":"Click to view this preset.")));
            if(!selecting){
                var enabled=button(pose.enabled?"ON":"OFF",layout.x(),y+5,actionSize,()->toggle(List.of(pose),!pose.enabled),"Enable or disable this preset. Matching enabled presets are chosen randomly.");enabled.setHeight(actionSize);enabled.active=editable;
                var rename=addRenderableWidget(GuardUi.iconButton("edit","Rename Preset",layout.x()+actionSize+gap,y+5,actionSize,()->rename(pose)));rename.active=editable;
                var delete=addRenderableWidget(GuardUi.iconButton("delete","Delete Preset",rowX+rowWidth+gap,y+5,actionSize,()->delete(List.of(pose))));delete.active=editable;
            }
        }
        int closeWidth=68,helpX=layout.x()+layout.inner()-closeWidth-28;
        button("New Preset",layout.x(),layout.footerY(),96,()->Minecraft.getInstance().setScreen(new GuardPoseEditorScreen(this,GuardPoseLibrary.create())),"Create a custom paired pose preset.").active=editable&&draft.poses.size()<GuardPoseLibrary.MAX_POSES;
        if(selecting)button("Batch Actions",layout.x()+102,layout.footerY(),Math.min(112,helpX-layout.x()-108),this::batchActions,"Apply timing, items, enable or delete to the selected presets.").active=!selectedIds.isEmpty();
        button("?",helpX,layout.footerY(),22,this::help,"Help with presets and batch editing.");
        button("Close",layout.x()+layout.inner()-closeWidth,layout.footerY(),closeWidth,this::onClose,"Return to settings.");
    }
    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(layout.x()+layout.inner()-8,layout.listY(),layout.listHeight(),layout.rows(),visiblePoses.size(),start);}
    private void scrollTo(double y){int next=scrollbar().scrollAt(y,scrollGrab);if(next!=start){start=next;rebuild();}}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&scrollbar().contains(x,y)){draggingScroll=true;scrollGrab=scrollbar().grab(y);scrollTo(y);return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll&&button==0){scrollTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingScroll){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=layout.left()&&x<layout.left()+layout.width()&&y>=layout.listY()&&y<layout.listY()+layout.listHeight()&&sy!=0){start=Math.clamp(start-(int)Math.signum(sy),0,scrollbar().max());rebuild();return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,layout.left(),layout.top(),layout.width(),layout.bottom()-layout.top(),false);
        g.drawString(font,"Punchy Presets",layout.x(),layout.top()+10,GuardUi.TEXT);
        String status=selecting?selectedIds.size()+" selected":visiblePoses.size()+" presets · "+(editable?"Click a row to edit":"View only");
        g.drawString(font,font.plainSubstrByWidth(status,layout.inner()),layout.x(),layout.top()+22,GuardUi.MUTED);
        if(visiblePoses.isEmpty())g.drawString(font,"No matching presets",layout.x()+8,layout.listY()+10,GuardUi.MUTED);
        GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);
        for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void onClose(){if(changed())GuardUi.choices(this,"Unsaved presets","Save or discard these preset changes.",new GuardUi.Choice("Save",()->{if(applyDraft())Minecraft.getInstance().setScreen(parent);}),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    @Override public void removed(){draggingScroll=false;super.removed();}
}
