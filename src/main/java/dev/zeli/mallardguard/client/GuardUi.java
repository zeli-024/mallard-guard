package dev.zeli.mallardguard.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared action colors and compact confirmation dialogs. */
final class GuardUi {
    static final int PANEL=0x92211B2A, SIDEBAR=0x962C2535, BORDER=0xFFA88FAE, TEXT=0xFFF5F0F6, MUTED=0xFFC3B4CA, WARM=0xFFD8BEAA, HIGHLIGHT=0xFFE7CD87;
    static void panel(GuiGraphics g,int x,int y,int width,int height,boolean sidebar){
        g.fill(x,y,x+width,y+height,sidebar?SIDEBAR:PANEL);
        g.renderOutline(x,y,width,height,BORDER);
        g.fill(x,y,x+width,y+2,WARM);
    }
    static net.minecraft.client.gui.components.EditBox editBox(net.minecraft.client.gui.Font font,int x,int y,int width,String label){
        return new net.minecraft.client.gui.components.EditBox(font,x,y,width,GuardUiLayout.CONTROL_HEIGHT,Component.literal(label));
    }

    static void scrollbar(GuiGraphics g,GuardUiLayout.Scrollbar bar,int mouseX,int mouseY,boolean dragging){
        if(bar.max()==0)return;
        g.fill(bar.x(),bar.y(),bar.x()+GuardUiLayout.SCROLL_WIDTH,bar.y()+bar.height(),0x77302737);
        int color=dragging||bar.contains(mouseX,mouseY)?HIGHLIGHT:WARM;
        g.fill(bar.x()+1,bar.thumbY(),bar.x()+GuardUiLayout.SCROLL_WIDTH-1,bar.thumbY()+bar.thumbHeight(),color);
    }
    static String tooltipLines(String description) {
        return description.strip().replaceAll("(?<=[.!?]) +(?=[A-Z0-9])", "\n");
    }
    static net.minecraft.client.gui.components.Tooltip tooltip(String description) {
        return net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltipLines(description)));
    }
    static int preciseStep() { return Screen.hasShiftDown() ? 10 : 1; }
    static int dragStep(int min,int max,int divisor){return divisor==1 && (long)max-min>200?10:1;}
    static void slider(GuiGraphics g,net.minecraft.client.gui.components.AbstractSliderButton widget,double value,boolean changed){
        int x=widget.getX(),y=widget.getY(),w=widget.getWidth(),h=widget.getHeight();
        g.fill(x,y,x+w,y+h,0xA0211B2A);
        g.renderOutline(x,y,w,h,changed?HIGHLIGHT:widget.isHoveredOrFocused()?HIGHLIGHT:BORDER);
        int thumb=x+2+(int)(value*(w-7));
        g.fill(thumb,y+2,thumb+4,y+h-2,widget.active?HIGHLIGHT:0xFF62566B);
        var font=Minecraft.getInstance().font;
        g.drawCenteredString(font,font.plainSubstrByWidth(widget.getMessage().getString(),Math.max(0,w-8)),x+w/2,y+(h-8)/2,widget.active?TEXT:0xFF9B8EA4);
    }
    static String enabledLabel(boolean enabled){return enabled?"Enabled":"Disabled";}
    record Choice(String label, Runnable action) {}
    static int accent(String text) {
        if(text.equals("On"))return 0xFFADD4AE;
        if(text.equals("Undo"))return HIGHLIGHT;
        if (text.startsWith("Reset") || text.startsWith("Delete")) return 0xFFE79999;
        if (text.startsWith("Apply") || text.startsWith("Save") || text.startsWith("Confirm") || text.startsWith("Use")) return 0xFFADD4AE;
        return BORDER;
    }
    static int body(String label,boolean hover,boolean active) {
        if(!active)return 0x69292431;
        if(label.equals("On"))return hover?0xBC819C87:0xA86A8773;
        if(label.equals("Undo"))return hover?0xBC927C48:0xA86E5B36;
        if(label.equals("Issue Tracker"))return hover?0xFF161F2D:0xFF0D1726;
        if(label.startsWith("Reset")||label.startsWith("Delete"))return hover?0xBC854457:0xA8623044;
        if(label.startsWith("Apply")||label.startsWith("Save")||label.startsWith("Confirm"))return hover?0xBC819C87:0xA86A8773;
        return hover?0xA45C4D68:0x84372D42;
    }
    private static long lastClickSound;
    static Builder builder(Component text,Button.OnPress action){return new Builder(text,action);}
    static final class Builder {
        final Component text;final Button.OnPress action;int x,y,w=150,h=20;
        Builder(Component text,Button.OnPress action){this.text=text;this.action=action;}
        Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
        Button build(){return new Button(x,y,w,h,text,action,message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){paint(g,this,mx,my,false,false);}
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){
                long now=System.nanoTime();
                if(now-lastClickSound<60_000_000L)return;
                lastClickSound=now; super.playDownSound(manager);
            }
        };}
    }
    static Button button(String text,int x,int y,int w,int h,Runnable action){return builder(Component.literal(text),b->action.run()).bounds(x,y,w,h).build();}
    static Button tab(String text,int x,int y,int w,boolean selected,Runnable action){
        Button button=new Button(x,y,w,GuardUiLayout.CONTROL_HEIGHT,Component.literal(text),ignored->action.run(),message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){paint(g,this,mx,my,selected,false);}
        };
        button.active=!selected;return button;
    }
    static void paint(GuiGraphics g,Button button,int mx,int my,boolean selected,boolean red){
        paint(g,button,mx,my,selected,red,true);
    }
    private static void paint(GuiGraphics g,Button button,int mx,int my,boolean selected,boolean red,boolean labelVisible){
        if(!button.visible)return;
        var font=Minecraft.getInstance().font;int x=button.getX(),y=button.getY(),w=button.getWidth(),h=button.getHeight();
        boolean hover=button.active&&button.isMouseOver(mx,my);String label=button.getMessage().getString();
        int edge=selected?0xFFD8BEAA:hover?0xFFC3A5BE:0xFF76647F;
        if(label.equals("On")||label.equals("Undo")||label.startsWith("Reset")||label.startsWith("Apply")||label.startsWith("Save")||label.startsWith("Confirm")||label.startsWith("Delete"))edge=accent(label);
        if(label.equals("Issue Tracker"))edge=hover?0xFF6D86A8:0xFF40516B;
        int fill=red?(hover?0xFF753E50:0xFF552B3C):selected?0x98493E55:body(label,hover,button.active);
        g.fill(x,y,x+w,y+h,edge);g.fill(x+1,y+1,x+w-1,y+h-1,fill);
        if(hover)g.fill(x+2,y+2,x+w-2,y+3,0x88F5E1F3);
        if(selected)g.fill(x+1,y+1,x+4,y+h-1,0xFFD8BEAA);
        if(!labelVisible)return;
        int textColor=button.active||selected?0xFFF5F0F6:0xFF978C9E;
        if(label.equals("<")||label.equals(">")){
            chevron(g,x+(w-GuardUiLayout.ARROW_SIZE)/2,y+(h-GuardUiLayout.ARROW_SIZE)/2,label.equals("<")?2:0,textColor);
        }else if(label.endsWith(" ▾")){
            String text=label.substring(0,label.length()-2);
            g.drawCenteredString(font,font.plainSubstrByWidth(text,w-GuardUiLayout.ARROW_SLOT-8),x+w/2,y+(h-8)/2,textColor);
            chevron(g,x+w-GuardUiLayout.ARROW_SIZE-GuardUiLayout.CONTROL_GAP,y+(h-GuardUiLayout.ARROW_SIZE)/2,1,textColor);
        }else g.drawCenteredString(font,font.plainSubstrByWidth(label,w-8),x+w/2,y+(h-8)/2,textColor);
    }
    static void listRow(GuiGraphics g,Button button,String detail,int mx,int my,boolean selected){
        paint(g,button,mx,my,selected,false,false);
        var font=Minecraft.getInstance().font;int x=button.getX()+9,w=button.getWidth()-18;
        g.drawString(font,font.plainSubstrByWidth(button.getMessage().getString(),w),x,button.getY()+6,button.active?TEXT:MUTED);
        g.drawString(font,font.plainSubstrByWidth(detail,w),x,button.getY()+20,MUTED);
    }
    static Button iconButton(String icon,String label,int x,int y,int size,Runnable action){
        Button b=new Button(x,y,size,size,Component.literal(label),ignored->action.run(),message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){
                boolean hover=active&&isMouseOver(mx,my);int edge=hover?0xFFD8BEAA:0xFF76647F;
                int fill=body(label,hover,active);if(icon.equals("delete"))edge=0xFFE79999;if(icon.equals("undo"))edge=0xFFE7CD87;if(icon.equals("github"))edge=hover?0xFF6D86A8:0xFF40516B;
                g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),fill);
                g.renderOutline(getX(),getY(),getWidth(),getHeight(),edge);
                int color=icon.equals("delete")?0xFF973C49:icon.equals("undo")?0xFFE7CD87:active?0xFFF5F0F6:0xFF766D7E;
                drawIcon(g,icon,getX()+(getWidth()-12)/2,getY()+(getHeight()-12)/2,color);
            }
        };b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(label)));return b;
    }
    static void drawIcon(GuiGraphics g,String icon,int x,int y,int c){
        switch(icon){
            case "github" -> {
                String[] rows={"001100001100","011111111110","011111111110","111111111111","111111111111","111111111111","011111111110","001111111100","000111111000","011011110000","001111110000","000111110000"};
                for(int r=0;r<rows.length;r++)for(int k=0;k<12;k++)if(rows[r].charAt(k)=='1')g.fill(x+k,y+r,x+k+1,y+r+1,c);
            }
            case "edit" -> {for(int i=0;i<7;i++)g.fill(x+2+i,y+8-i,x+5+i,y+11-i,c);g.fill(x+1,y+10,x+3,y+12,c);}
            case "undo" -> {
                var font=Minecraft.getInstance().font;
                g.pose().pushPose();g.pose().translate(x+6,y+6,0);
                g.pose().scale(GuardUiLayout.UNDO_ICON_SCALE,GuardUiLayout.UNDO_ICON_SCALE,1);
                g.drawString(font,"↺",-font.width("↺")/2,-4,c,false);g.pose().popPose();
            }
            case "delete" -> {g.fill(x+2,y+3,x+10,y+4,c);g.fill(x+5,y+1,x+7,y+3,c);g.renderOutline(x+3,y+5,6,7,c);g.fill(x+5,y+6,x+6,y+10,c);}
            case "motion" -> {g.renderOutline(x+1,y+1,10,10,c);g.fill(x+5,y+3,x+6,y+7,c);g.fill(x+5,y+6,x+9,y+7,c);}
            case "assign" -> {g.renderOutline(x+1,y+3,7,5,c);g.renderOutline(x+5,y+5,7,5,c);}
            case "copy" -> {g.renderOutline(x+1,y+1,7,8,c);g.renderOutline(x+5,y+4,7,8,c);}
            case "hide" -> {g.renderOutline(x+1,y+4,10,5,c);g.fill(x+5,y+5,x+7,y+8,c);}
            case "preview" -> {for(int i=0;i<5;i++)g.fill(x+3+i,y+2+i,x+4+i,y+11-i,c);}
            case "on" -> {g.renderOutline(x+2,y+4,8,7,c);g.fill(x+5,y+1,x+7,y+7,c);}
            case "off" -> {g.renderOutline(x+2,y+4,8,7,c);g.fill(x+5,y+1,x+7,y+7,c);g.fill(x+1,y+11,x+11,y+12,c);}
            default -> {g.drawCenteredString(Minecraft.getInstance().font,"T",x+6,y+2,c);}
        }
    }
    /** Direction: right=0, down=1, left=2. */
    static void chevron(GuiGraphics g,int x,int y,int direction,int color){
        int size=GuardUiLayout.ARROW_SIZE, thickness=GuardUiLayout.ARROW_THICKNESS;
        for(int i=0;i<size;i++){
            int offset=Math.min(i,size-1-i);
            int px=direction==2?size/2-offset:offset;
            if(direction==1)g.fill(x+i,y+offset,x+i+1,y+offset+thickness,color);
            else g.fill(x+px,y+i,x+px+thickness,y+i+1,color);
        }
    }
    static void logo(GuiGraphics g,int x,int y,int size){
        g.fill(x-1,y-1,x+size+1,y+size+1,0xFF947D9A);
        g.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mallardguard","textures/gui/mod_logo.png"),x,y,0,0,size,size,size,size);
    }
    static void openLink(Screen parent,String title,String url){
        net.minecraft.Util.getPlatform().openUri(java.net.URI.create(url));
        choices(parent,title,"Open the link in your browser or copy it if your desktop cannot open it.",
            new Choice("Copy Link",()->{Minecraft.getInstance().keyboardHandler.setClipboard(url);Minecraft.getInstance().setScreen(parent);}),
            new Choice("Open Browser",()->net.minecraft.Util.getPlatform().openUri(java.net.URI.create(url))),
            new Choice("Back",()->Minecraft.getInstance().setScreen(parent)));
    }
    static void sections(Screen parent,int selected,java.util.function.IntConsumer choose,int x,int y,int width,int height){
        Minecraft.getInstance().setScreen(new SectionMenu(parent,selected,choose,x,y,width,height));
    }
    private static final class SectionMenu extends Screen {
        final Screen parent;final int selected,anchorX,anchorY,anchorWidth,anchorHeight;
        final java.util.function.IntConsumer choose;
        int left,top,w,menuHeight;private com.mojang.blaze3d.pipeline.TextureTarget backdrop;
        SectionMenu(Screen parent,int selected,java.util.function.IntConsumer choose,int x,int y,int w,int h){
            super(Component.literal("Config section"));this.parent=parent;this.selected=selected;this.choose=choose;
            anchorX=x;anchorY=y;anchorWidth=w;anchorHeight=h;
            var target=Minecraft.getInstance().getMainRenderTarget();
            if(target.width>0&&target.height>0){backdrop=new com.mojang.blaze3d.pipeline.TextureTarget(target.width,target.height,false,false);GuardPreviewScreen.copy(target,backdrop);}
        }
        @Override protected void init(){
            w=Math.min(anchorWidth+8,width-8);menuHeight=102;
            left=Math.clamp(anchorX-4,4,Math.max(4,width-w-4));
            top=Math.clamp(anchorY-5,4,Math.max(4,height-menuHeight-4));
            String[] names={"Client","Server","Enforce","Experimental"};
            var order=new ArrayList<Integer>();
            order.add(selected);
            for(int section:new int[]{0,1,2})if(section!=selected)order.add(section);
            if(selected!=3)order.add(3);
            for(int i=0;i<order.size();i++){
                int section=order.get(i);
                Button b=new Button(left+4,top+5+i*24,w-8,20,Component.literal(names[section]),ignored->{choose.accept(section);Minecraft.getInstance().setScreen(parent);},message->message.get()){
                    @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){paint(g,this,mx,my,false,section==3);}
                };
                b.active=section!=selected;addRenderableWidget(b);
            }
        }
        @Override public void render(GuiGraphics g,int x,int y,float d){
            g.flush();var target=Minecraft.getInstance().getMainRenderTarget();
            if(backdrop!=null){GuardPreviewScreen.copy(backdrop,target);target.bindWrite(false);}
            renderBackground(g,x,y,d);g.flush();
            g.fill(left,top,left+w,top+menuHeight,0xDC211B2A);g.renderOutline(left,top,w,menuHeight,0xFF947D9A);
            for(var widget:renderables)widget.render(g,x,y,d);
        }
        @Override public boolean mouseClicked(double x,double y,int button){
            if(x<left||x>=left+w||y<top||y>=top+menuHeight){onClose();return true;}
            if(button==0&&y>=top+5&&y<top+25){onClose();return true;}
            return super.mouseClicked(x,y,button);
        }
        @Override public void removed(){if(backdrop!=null){backdrop.destroyBuffers();backdrop=null;}}
        @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
        @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    }
    static void choices(Screen parent,String title,String message,Choice... choices) { Minecraft.getInstance().setScreen(new Dialog(parent,title,message,List.of(choices))); }
    static void confirm(Screen parent,String title,String message,Runnable yes) {
        choices(parent,title,message,new Choice(title.startsWith("Reset")?"Reset":"Confirm",()->{yes.run();Minecraft.getInstance().setScreen(parent);}),new Choice("Go Back",()->Minecraft.getInstance().setScreen(parent)));
    }

    private static final class Dialog extends Screen {
        final Screen parent;final String message;final List<Choice> choices;int left,top,w,panelHeight,buttonTop;
        private List<net.minecraft.util.FormattedCharSequence> messageLines=List.of();
        private int textScroll,visibleLines;private boolean draggingScroll;private double scrollGrab;
        Dialog(Screen parent,String title,String message,List<Choice> choices){super(Component.literal(title));this.parent=parent;this.message=message;this.choices=choices;}
        @Override protected void init(){
            int count=choices.size();boolean vertical=count>3;int widest=0;
            for(var choice:choices)widest=Math.max(widest,font.width(choice.label()));
            w=Math.min(width-24,Math.max(280,Math.max(font.width(title)+24,vertical?widest+40:(widest+20)*count+6*(count-1)+16)));
            vertical=vertical||(widest+20)*count+6*(count-1)+16>w;
            messageLines=font.split(Component.literal(message),Math.max(1,w-28));
            int actions=vertical?count*24:24;
            visibleLines=Math.max(1,Math.min(messageLines.size(),(height-50-actions)/font.lineHeight));
            textScroll=Math.max(0,Math.min(textScroll,messageLines.size()-visibleLines));
            buttonTop=30+visibleLines*font.lineHeight;
            left=(width-w)/2;panelHeight=buttonTop+actions+8;top=Math.max(4,(height-panelHeight)/2);
            int bw=vertical?w-16:(w-16-6*(count-1))/count;
            for(int i=0;i<count;i++){
                var choice=choices.get(i);var b=addRenderableWidget(button(choice.label(),left+8+(vertical?0:i*(bw+6)),top+buttonTop+(vertical?i*24:0),bw,20,choice.action()));
                String detail=switch(choice.label()){
                    case "Apply & Stay" -> "Save changes and keep this screen open.";
                    case "Apply & Close" -> "Save changes and close this screen.";
                    case "Reset Screen" -> "Reset this category to defaults. Apply to save.";
                    case "Reset Section" -> "Reset this section to defaults. Apply to save.";
                    case "Go Back", "Cancel" -> "Return without performing this action.";
                    case "Discard" -> "Discard unsaved changes.";
                    default -> choice.label();
                };
                b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(detail)));
            }
        }
        private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(left+w-12,top+26,visibleLines*font.lineHeight,visibleLines,messageLines.size(),textScroll);}
        @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&scrollbar().contains(x,y)){draggingScroll=true;scrollGrab=scrollbar().grab(y);textScroll=scrollbar().scrollAt(y,scrollGrab);return true;}return super.mouseClicked(x,y,button);}
        @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&draggingScroll){textScroll=scrollbar().scrollAt(y,scrollGrab);return true;}return super.mouseDragged(x,y,button,dx,dy);}
        @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingScroll){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);}
        @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
            if(x>=left&&x<left+w&&y>=top+26&&y<top+buttonTop){textScroll=Math.max(0,Math.min(textScroll-(int)Math.signum(sy)*3,messageLines.size()-visibleLines));return true;}
            return super.mouseScrolled(x,y,sx,sy);
        }
        @Override public void render(GuiGraphics g,int x,int y,float d){g.flush();renderBackground(g,x,y,d);g.flush();panel(g,left,top,w,panelHeight,false);g.drawCenteredString(font,title,width/2,top+11,0xFFF5F0F6);
            for(int i=0;i<visibleLines&&textScroll+i<messageLines.size();i++)g.drawString(font,messageLines.get(textScroll+i),left+10,top+26+i*font.lineHeight,TEXT);
            GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);
            for(var widget:renderables)widget.render(g,x,y,d);}
        @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
        @Override public void removed(){draggingScroll=false;super.removed();}
        @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    }
}
