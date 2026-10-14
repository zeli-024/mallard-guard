package dev.zeli.mallardguard.client;

/** Shared geometry for settings panels, dropdown arrows and per-setting actions. */
final class GuardUiLayout {
    static final int OUTER_MARGIN=8;
    static final int SIDEBAR_MIN=150, SIDEBAR_MAX=166, CONTENT_MAX=640;
    static final int PANEL_INSETS=40, SIDEBAR_LEFT_INSET=6, PANEL_GAP=20;
    static final int CONTROL_GAP=4, UNDO_SIZE=20;
    static final int ARROW_SIZE=7, ARROW_THICKNESS=1, ARROW_SLOT=18, SECTION_ARROW_WIDTH=20;
    static final float UNDO_ICON_SCALE=1.65F;
    static final int DIVIDER_INSET=20, DIVIDER_COLOR=0xFF907D9A;
    record Panels(int sidebarLeft,int sidebarWidth,int contentLeft,int contentWidth) {}
    static Panels panels(int screenWidth){
        int sidebar=Math.min(SIDEBAR_MAX,Math.max(SIDEBAR_MIN,screenWidth/4));
        int content=Math.max(12,Math.min(CONTENT_MAX,screenWidth-2*OUTER_MARGIN-sidebar-PANEL_INSETS));
        int left=(screenWidth-sidebar-content-PANEL_INSETS)/2+SIDEBAR_LEFT_INSET;
        return new Panels(left,sidebar,left+sidebar+PANEL_GAP,content);
    }
    static int wideWidth(int screenWidth){
        Panels p=panels(screenWidth);return Math.min(screenWidth-16,p.contentLeft()+p.contentWidth()+14-(p.sidebarLeft()-6));
    }
    static int screenTop(int screenHeight){return Math.min(14,Math.max(6,screenHeight/16));}
    record Workspace(int left,int top,int sidebarWidth,int contentLeft,int contentWidth,int bottom) {
        int height(){return bottom-top;}
        int sidebarInner(){return sidebarWidth-16;}
        int contentX(){return contentLeft+8;}
        int contentInner(){return contentWidth-16;}
        int bodyY(){return top+68;}
        int previewY(){return bottom-54;}
        int footerY(){return bottom-28;}
    }
    static Workspace workspace(int width,int height){
        int available=wideWidth(width),sidebar=Math.clamp(available/4,100,148),gap=12;
        int left=(width-available)/2;
        return new Workspace(left,screenTop(height),sidebar,left+sidebar+gap,available-sidebar-gap,height-6);
    }
    record IconEditor(int left,int top,int width,int height) {
        int x(){return left+8;}
        int inner(){return width-16;}
        int controlsY(){return top+24;}
        int statusY(){return top+73;}
        int footerY(){return top+90;}
        int presetWidth(){return width-16;}
        int presetButtonWidth(){return (presetWidth()-8)/3;}
        int presetX(){return left+8;}
        int presetY(){return Math.max(4,(top+height-128)/2);}
        boolean inDock(double x,double y){return x>=left&&x<left+width&&y>=top&&y<top+height;}
        boolean inPresets(double x,double y){return x>=left&&x<left+width&&y>=presetY()&&y<presetY()+128;}
    }
    static IconEditor iconEditor(int width,int height){int available=Math.min(420,wideWidth(width));return new IconEditor((width-available)/2,height-124,available,116);}
    static final int POSE_ROW_HEIGHT=34, POSE_ROW_STEP=38;
    record PoseBrowser(int left,int top,int width,int bottom) {
        int x(){return left+8;}
        int inner(){return width-16;}
        int searchY(){return top+34;}
        int tabsY(){return top+60;}
        int listY(){return top+86;}
        int footerY(){return bottom-28;}
        int rows(){return Math.max(1,(footerY()-listY()-8)/POSE_ROW_STEP);}
        int listHeight(){return rows()*POSE_ROW_STEP;}
        int rowWidth(){return inner()-12;}
    }
    static PoseBrowser poseBrowser(int width,int height){
        int available=wideWidth(width);
        return new PoseBrowser((width-available)/2,screenTop(height),available,height-6);
    }
    record PoseEditor(int left,int top,int width,int bottom,boolean compact) {
        int headerY(){return top+4;}
        int undoX(){return left+8;}
        int copyX(){return left+width-54;}
        int helpX(){return left+width-28;}
        int titleWidth(){return Math.max(0,width-124);}
        int x(){return left+20;}
        int inner(){return width-40;}
        int tabsY(){return top+52;}
        int contentY(){return tabsY()+26;}
        int contentBottom(){return previewY()-6;}
        int contentWidth(){return inner();}
        int previewY(){return bottom-(compact?66:90);}
        int opacityY(){return previewY()+(compact?0:24);}
        int statusY(){return footerY()-12;}
        int footerY(){return bottom-28;}
        int trackX(){return left+width-12;}
    }
    static PoseEditor poseEditor(int width,int height){
        int available=Math.min(wideWidth(width),420),panelHeight=Math.min(height-16,420),top=(height-panelHeight)/2;
        return new PoseEditor((width-available)/2,top,available,top+panelHeight,panelHeight<300);
    }
    record DamageBrowser(int left,int top,int width,int bottom) {
        int x(){return left+8;}
        int inner(){return width-16;}
        int tabsY(){return top+30;}
        int searchY(){return top+56;}
        int headingY(){return top+86;}
        int listY(){return top+104;}
        int footerY(){return bottom-28;}
        int rows(){return Math.max(1,(footerY()-listY()-10)/24);}
        int listBottom(){return listY()+rows()*24;}
        int toggleWidth(){return Math.clamp(inner()/6,56,80);}
        int trackX(){return left+width-20;}
        int toggleX(){return trackX()-12-toggleWidth();}
        int modWidth(){return Math.clamp(inner()/4,72,160);}
        int sourceX(){return x()+modWidth()+12;}
        int sourceWidth(){return toggleX()-sourceX()-12;}
    }
    static DamageBrowser damageBrowser(int width,int height){int available=wideWidth(width);return new DamageBrowser((width-available)/2,screenTop(height),available,height-6);}
    static final int CONTROL_HEIGHT=20, ITEM_STEP=23, ITEM_SIZE=21, ASSIGNMENT_INSET=8;
    record Assignment(int left,int top,int browserWidth,int assignedLeft,int assignedWidth,int bottom,int tabWidth,int actionWidth) {
        int width(){return assignedLeft+assignedWidth-left;}
        int searchY(){return top+38;}
        int tabsY(){return top+64;}
        int bulkY(){return top+90;}
        int ruleY(){return top+116;}
        int assignedY(boolean counts){return top+(counts?90:64);}
        int gridY(boolean filters,boolean controls){return top+(filters?94:controls?140:116);}
        int footerY(){return bottom-28;}
        int contentBottom(){return bottom-56;}
    }
    static Assignment assignment(int width,int height){
        int available=wideWidth(width),gap=12;
        int assigned=Math.max(128,(available-gap)*2/5),browser=available-gap-assigned;
        int left=(width-available)/2;
        return new Assignment(left,screenTop(height),browser,left+browser+gap,assigned,height-6,
            Math.min(58,(browser-88)/2),Math.min(80,(available-36)/3));
    }
    static final int SCROLL_WIDTH=8, SCROLL_MIN_THUMB=16;
    record Scrollbar(int x,int y,int height,int visible,int total,int offset) {
        int max(){return Math.max(0,total-visible);}
        int thumbHeight(){return Math.min(height,Math.max(SCROLL_MIN_THUMB,height*Math.max(1,visible)/Math.max(1,total)));}
        int thumbY(){return y+(max()==0?0:Math.max(0,Math.min(max(),offset))*(height-thumbHeight())/max());}
        boolean contains(double mx,double my){return max()>0&&mx>=x-2&&mx<x+SCROLL_WIDTH+2&&my>=y&&my<y+height;}
        double grab(double my){return my>=thumbY()&&my<thumbY()+thumbHeight()?my-y-Math.max(0,Math.min(max(),offset))*(height-thumbHeight())/(double)Math.max(1,max()):thumbHeight()/2.0;}
        int scrollAt(double my,double grab){int travel=height-thumbHeight();return travel<=0?0:Math.max(0,Math.min(max(),(int)Math.round((my-y-grab)*max()/travel)));}
    }
    private GuardUiLayout() {}
}
