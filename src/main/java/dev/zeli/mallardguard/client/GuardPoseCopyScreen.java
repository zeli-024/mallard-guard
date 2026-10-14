package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseCopyScreen extends Screen {
    private final Screen parent;
    private final List<GuardPoseLibrary.Pose> poses;
    private final String current;
    private final Consumer<GuardPoseLibrary.Pose> accept;
    private GuardUiLayout.PoseBrowser layout;
    private List<GuardPoseLibrary.Pose> results=List.of();
    private EditBox search;
    private String searchText="";
    private int start;
    private boolean draggingScroll;
    private double scrollGrab;
    GuardPoseCopyScreen(Screen parent,List<GuardPoseLibrary.Pose> poses,String current,Consumer<GuardPoseLibrary.Pose> accept){
        super(Component.literal("Copy Preset"));this.parent=parent;this.poses=poses.stream().filter(p->!p.deleted).map(GuardPoseLibrary.Pose::copy).toList();this.current=current;this.accept=accept;
    }
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        layout=GuardUiLayout.poseBrowser(width,height);
        String query=searchText.strip().toLowerCase(Locale.ROOT);
        results=poses.stream().filter(p->p.name.toLowerCase(Locale.ROOT).contains(query)).toList();
        start=Math.clamp(start,0,Math.max(0,results.size()-layout.rows()));
        search=addRenderableWidget(GuardUi.editBox(font,layout.x(),layout.searchY(),layout.inner(),"Search presets"));
        search.setMaxLength(64);search.setValue(searchText);search.setResponder(value->{int cursor=search.getCursorPosition();searchText=value;start=0;rebuild();setFocused(search);search.setFocused(true);search.setCursorPosition(Math.min(cursor,value.length()));});
        for(int i=start;i<Math.min(results.size(),start+layout.rows());i++){
            var pose=results.get(i);String detail=(pose.preset?"Built-in":"Custom")+" · "+(pose.id.equals(current)?"Current preset":"Copy poses, timing and items");
            var row=addRenderableWidget(new Button(layout.x(),layout.listY()+(i-start)*GuardUiLayout.POSE_ROW_STEP,layout.rowWidth(),GuardUiLayout.POSE_ROW_HEIGHT,Component.literal(pose.name),ignored->{accept.accept(pose.copy());Minecraft.getInstance().setScreen(parent);},message->message.get()){
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){GuardUi.listRow(g,this,detail,mx,my,false);}
            });
            row.active=!pose.id.equals(current);row.setTooltip(GuardUi.tooltip(pose.name+"\n"+(row.active?"Copy both poses, timing and items into the draft.\nSave in the editor to commit.":"You are already editing this preset.")));
        }
        addRenderableWidget(GuardUi.button("?",layout.x(),layout.footerY(),22,20,()->GuardUi.choices(this,"Copy Preset Help","COPY\nChoose a source preset to copy both poses, timing and items.\nYour preset's name and enabled state stay unchanged.\n\nSAVE\nSave in the editor commits the copied values.\nBack leaves the draft unchanged.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        addRenderableWidget(GuardUi.button("Back",layout.x()+layout.inner()-72,layout.footerY(),72,20,this::onClose));
    }
    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(layout.x()+layout.inner()-8,layout.listY(),layout.listHeight(),layout.rows(),results.size(),start);}
    private void scrollTo(double y){int next=scrollbar().scrollAt(y,scrollGrab);if(next!=start){start=next;rebuild();}}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&scrollbar().contains(x,y)){draggingScroll=true;scrollGrab=scrollbar().grab(y);scrollTo(y);return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll&&button==0){scrollTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(draggingScroll&&button==0){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=layout.left()&&x<layout.left()+layout.width()&&y>=layout.listY()&&y<layout.listY()+layout.listHeight()&&sy!=0){start=Math.clamp(start-(int)Math.signum(sy),0,scrollbar().max());rebuild();return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,layout.left(),layout.top(),layout.width(),layout.bottom()-layout.top(),false);
        g.drawString(font,"Copy Preset",layout.x(),layout.top()+10,GuardUi.TEXT);
        g.drawString(font,"Choose a source for this draft",layout.x(),layout.top()+22,GuardUi.MUTED);
        g.drawString(font,"Copying replaces both poses, timing and items.",layout.x(),layout.tabsY()+6,GuardUi.MUTED);
        if(results.isEmpty())g.drawString(font,"No matching presets",layout.x()+8,layout.listY()+10,GuardUi.MUTED);
        GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);
        for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void removed(){draggingScroll=false;super.removed();}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
