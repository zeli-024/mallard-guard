package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardDamageRules;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Runtime damage rules edited as part of the main screen's unsaved draft. */
final class GuardDamageSourcesScreen extends Screen {
    private static final int INSET=8;
    private static final int ROW_HEIGHT=24, ROW_CONTROL_HEIGHT=GuardUiLayout.CONTROL_HEIGHT;
    private static final int TEXT=GuardUi.TEXT, MUTED=GuardUi.MUTED;
    private static final int ENABLED=0xFFB6E8B6, DISABLED=0xFFA7A7A7;
    private final Screen parent;
    private final List<String> recent, catalog;
    private final Map<String,Boolean> rules;
    private final boolean editable;
    private final Function<String,String> modName;
    private final Consumer<Map<String,Boolean>> accept;
    private int left,top,w,h,offset,rows;
    private GuardUiLayout.DamageBrowser layout;
    private boolean all,draggingScroll;
    private double scrollGrab;
    private String query="";
    private List<String> filtered=List.of();
    private final List<String> rowIds=new ArrayList<>();

    GuardDamageSourcesScreen(Screen parent,List<String> recent,List<String> catalog,Map<String,Boolean> rules,boolean editable,Function<String,String> modName,Consumer<Map<String,Boolean>> accept){
        super(Component.literal("Damage Sources"));
        this.parent=parent;this.recent=recent.stream().distinct().limit(10).toList();
        this.catalog=List.copyOf(catalog);this.rules=new LinkedHashMap<>(rules);
        this.editable=editable;this.modName=modName;this.accept=accept;
    }
    private boolean enabled(String id){return rules.getOrDefault(id,GuardDamageRules.defaultAllowed(id));}
    private void filter(){
        String[] terms=query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        filtered=(all?catalog:recent).stream().filter(id->{
            String key=id.toLowerCase(Locale.ROOT),mod=modName.apply(id).toLowerCase(Locale.ROOT);
            for(String term:terms){
                if(term.isEmpty())continue;
                if(term.equals("#enabled")){if(!enabled(id))return false;}
                else if(term.equals("#disabled")){if(enabled(id))return false;}
                else if(term.startsWith("@")){
                    if(!mod.contains(term.substring(1))&&!key.split(":",2)[0].contains(term.substring(1)))return false;
                }else if(!key.contains(term)&&!mod.contains(term))return false;
            }
            return true;
        }).toList();
    }
    private int rowTop(){return layout.listY();}
    private int listBottom(){return rowTop()+rows*ROW_HEIGHT;}
    private int trackX(){return layout.trackX();}
    private int toggleX(){return layout.toggleX();}
    private int sourceX(){return layout.sourceX();}
    private int maxScroll(){return Math.max(0,filtered.size()-rows);}
    private int trackHeight(){return rows*ROW_HEIGHT;}
    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(trackX(),rowTop(),trackHeight(),rows,filtered.size(),offset);}

    @Override protected void init(){
        layout=GuardUiLayout.damageBrowser(width,height);w=layout.width();top=layout.top();h=layout.bottom()-top;left=layout.left();rows=layout.rows();
        filter();offset=Math.clamp(offset,0,maxScroll());rowIds.clear();draggingScroll=false;
        int half=(w-2*INSET-6)/2;
        var recentButton=addRenderableWidget(GuardUi.button("Recent Hits",layout.x(),layout.tabsY(),half,20,()->selectList(false)));
        recentButton.active=all;
        var allButton=addRenderableWidget(GuardUi.button("All Sources",layout.x()+half+6,layout.tabsY(),layout.inner()-half-6,20,()->selectList(true)));
        allButton.active=!all;
        EditBox search=addRenderableWidget(GuardUi.editBox(font,layout.x(),layout.searchY(),layout.inner(),"Search damage sources"));
        search.setMaxLength(128);search.setHint(Component.literal("name, @mod, #enabled, #disabled"));search.setValue(query);
        search.setTooltip(GuardUi.tooltip("Filter this list by source ID, name or @mod. Use #enabled or #disabled to filter parry rules."));
        search.setResponder(text->{query=text;offset=0;filter();refreshRows();});
        refreshRows();
        var help=addRenderableWidget(GuardUi.button("?",left+w-28,top+4,20,20,()->GuardUi.choices(this,"Damage Sources Help","Recent Hits lists the last ten damage types; All Sources lists the server catalogue. Search IDs or mod names, @mod namespaces, or #enabled and #disabled rules.\n\nOn permits parrying that source, subject to normal eligibility; Off excludes it. Done returns edits to settings. Apply there saves them.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        help.setTooltip(GuardUi.tooltip("Help with searching, rules and saving."));
        var done=addRenderableWidget(GuardUi.button("Done",left+w-INSET-78,layout.footerY(),78,20,this::onClose));
        done.setTooltip(GuardUi.tooltip("Return to settings. Use Apply there to save these rules."));
    }
    private void selectList(boolean showAll){all=showAll;query="";offset=0;clearWidgets();init();}
    private void refreshRows(){
        for(var child:List.copyOf(children()))if(child instanceof SourceButton)removeWidget(child);
        rowIds.clear();offset=Math.clamp(offset,0,maxScroll());
        for(int i=0;i<rows&&offset+i<filtered.size();i++){
            String id=filtered.get(offset+i);rowIds.add(id);
            SourceButton b=addRenderableWidget(new SourceButton(toggleX(),rowTop()+i*ROW_HEIGHT+2,id));
            b.active=editable;
            b.setTooltip(GuardUi.tooltip("Mod: "+modName.apply(id)+"\n"+id+"\nDefault: "+(GuardDamageRules.defaultAllowed(id)?"On":"Off")
                +(editable?". Save with Apply in settings.":". Server operator permission required.")));
        }
    }
    private final class SourceButton extends Button {
        SourceButton(int x,int y,String id){
            super(x,y,layout.toggleWidth(),ROW_CONTROL_HEIGHT,Component.literal(GuardUi.enabledLabel(enabled(id))),b->{
                rules.put(id,!enabled(id));filter();refreshRows();
            },message->message.get());
        }
        @Override public void renderWidget(GuiGraphics g,int x,int y,float d){GuardUi.paint(g,this,x,y,false,false);}
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();
        GuardUi.panel(g,left,top,w,h,false);
        g.drawCenteredString(font,title,width/2,top+10,TEXT);
        int listLeft=layout.x(),listRight=left+w-INSET;
        GuardUi.panel(g,listLeft,layout.headingY()-6,listRight-listLeft,listBottom()+4-(layout.headingY()-6),true);
        g.drawString(font,"Mod",layout.x()+6,layout.headingY(),MUTED);
        g.drawString(font,"Damage Source",sourceX(),layout.headingY(),MUTED);
        g.drawCenteredString(font,"Parry",toggleX()+layout.toggleWidth()/2,layout.headingY(),MUTED);
        String hoveredId=null;
        for(int i=0;i<rowIds.size();i++){
            String id=rowIds.get(i);int ry=rowTop()+i*ROW_HEIGHT;
            boolean hovered=x>=listLeft&&x<toggleX()+layout.toggleWidth()&&y>=ry&&y<ry+ROW_HEIGHT;
            if(hovered||(i&1)==0)g.fill(listLeft+1,ry,listRight-1,ry+ROW_HEIGHT,hovered?0x51493E55:0x183D3349);
            int color=enabled(id)?ENABLED:DISABLED;
            g.drawString(font,font.plainSubstrByWidth(modName.apply(id),layout.modWidth()-6),layout.x()+6,ry+8,MUTED);
            g.drawString(font,font.plainSubstrByWidth(id,Math.max(0,layout.sourceWidth())),sourceX(),ry+8,color);
            if(hovered&&x<toggleX())hoveredId=id;
        }
        if(rowIds.isEmpty())g.drawCenteredString(font,query.isBlank()&&!all?"No recent damage sources":"No matching sources",width/2,rowTop()+12,MUTED);
        GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);
        g.drawString(font,filtered.size()+" source"+(filtered.size()==1?"":"s")+(editable?"":" · Read only"),layout.x(),layout.footerY()+6,MUTED);
        for(var widget:renderables)widget.render(g,x,y,d);
        if(hoveredId!=null)g.renderTooltip(font,List.of(Component.literal("Mod: "+modName.apply(hoveredId)),Component.literal(hoveredId)),Optional.empty(),x,y);
    }
    private void dragScroll(double y){
        int next=scrollbar().scrollAt(y,scrollGrab);
        if(next!=offset){offset=next;refreshRows();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&scrollbar().contains(x,y)){
            draggingScroll=true;
            scrollGrab=scrollbar().grab(y);
            dragScroll(y);setDragging(true);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&draggingScroll){dragScroll(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&draggingScroll){draggingScroll=false;setDragging(false);return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=left+INSET&&x<left+w-INSET&&y>=rowTop()&&y<listBottom()&&sy!=0){
            int next=Math.clamp(offset-(int)Math.signum(sy),0,maxScroll());
            if(next!=offset){offset=next;refreshRows();}return true;
        }
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void onClose(){accept.accept(new LinkedHashMap<>(rules));Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    @Override public void removed(){draggingScroll=false;super.removed();}

}
