package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardScreenSettings;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Screen anchors use this frame's HUD spacing; no world or entity references are retained. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardIconLayout {
    public static final int CENTER=0,TOP_LEFT=1,BOTTOM_LEFT=2,TOP_RIGHT=3,BOTTOM_RIGHT=4,
        TOP_CENTER=5,BOTTOM_CENTER=6,HOTBAR_LEFT=7,HOTBAR_RIGHT=8,CUSTOM=9;
    static final int SIZE=18;
    static final String[] NAMES={"Center","Top Left","Bottom Left","Top Right","Bottom Right","Top Center","Bottom Center","Hotbar Left","Hotbar Right","Custom"};
    private static int bossBottom,hotbarLeft,hotbarRight,hotbarTop,hotbarBottom,bottomTop=Integer.MAX_VALUE;
    private static boolean tracking;
    record Position(float x,float y) {}
    private GuardIconLayout() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void beginFrame(RenderGuiEvent.Pre event){
        bossBottom=0;bottomTop=Integer.MAX_VALUE;tracking=false;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        tracking=GuardConfig.HUD.get()||mc.screen instanceof GuardIconPositionScreen;
        int w=event.getGuiGraphics().guiWidth(),h=event.getGuiGraphics().guiHeight();
        hotbarLeft=w/2-91;hotbarRight=w/2+91;hotbarTop=h-22;hotbarBottom=h;
        if(!mc.player.getOffhandItem().isEmpty()){
            if(mc.player.getMainArm()==HumanoidArm.RIGHT)hotbarLeft-=29;else hotbarRight+=29;
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress event){
        if(tracking&&!event.isCanceled())bossBottom=Math.max(bossBottom,event.getY()+Math.max(10,event.getIncrement()));
    }
    /** Call during HUD rendering, before our LOWEST-priority Post pass, only for visible slots. GUI pixels. */
    public static void reserveHotbar(int left,int top,int right,int bottom){
        if(!tracking||left>=right||top>=bottom)return;
        hotbarLeft=Math.min(hotbarLeft,left);hotbarRight=Math.max(hotbarRight,right);
        hotbarTop=Math.min(hotbarTop,top);hotbarBottom=Math.max(hotbarBottom,bottom);
    }
    /** Independently drawn bottom bars can report their visible upper edge during the same HUD frame. */
    public static void reserveBottom(int top){if(tracking)bottomTop=Math.min(bottomTop,top);}
    static int scaledSize(int percent){return Math.max(1,Math.round(SIZE*percent/100F));}
    static int size(){return scaledSize(GuardScreenSettings.get(GuardScreenSettings.ICON_SIZE));}
    static Position current(int width,int height,int size){
        return position(GuardScreenSettings.get(GuardScreenSettings.ICON_ANCHOR),GuardScreenSettings.get(GuardScreenSettings.ICON_X),GuardScreenSettings.get(GuardScreenSettings.ICON_Y),GuardScreenSettings.get(GuardScreenSettings.ICON_GAP),width,height,size);
    }
    static Position position(int mode,int x,int y,int gap,int width,int height,int size){
        var mc=Minecraft.getInstance();
        int hudHeight=mc.gameMode!=null&&mc.gameMode.canHurtPlayer()?Math.max(mc.gui.leftHeight,mc.gui.rightHeight):23;
        hudHeight=Math.max(hudHeight,height-bottomTop);
        int left=tracking?hotbarLeft:width/2-91,right=tracking?hotbarRight:width/2+91;
        int top=tracking?hotbarTop:height-22,bottom=tracking?hotbarBottom:height;
        return resolve(mode,x,y,gap,width,height,size,bossBottom,hudHeight,left,right,top,bottom);
    }
    static Position resolve(int mode,int x,int y,int gap,int width,int height,int size,int bosses,int hudHeight,int left,int right,int top,int bottom){
        float half=size/2F,edge=half+gap,cx=width/2F,cy=height/2F;
        switch(mode){
            case CENTER -> {cx=(width-15)/2+7.5F;cy=(height-15)/2+17+half;}
            case TOP_LEFT -> {cx=edge;cy=edge;}
            case BOTTOM_LEFT -> {cx=edge;cy=height-edge;}
            case TOP_RIGHT -> {cx=width-edge;cy=edge;}
            case BOTTOM_RIGHT -> {cx=width-edge;cy=height-edge;}
            case TOP_CENTER -> cy=Math.max(edge,bosses+gap+half);
            case BOTTOM_CENTER -> cy=height-Math.max(23,hudHeight)-gap-half;
            case HOTBAR_LEFT -> {cx=left-gap-half;cy=(top+bottom)/2F;}
            case HOTBAR_RIGHT -> {cx=right+gap+half;cy=(top+bottom)/2F;}
            case CUSTOM -> {cx=half+Math.clamp(x,0,10000)/10000F*Math.max(0,width-size);cy=half+Math.clamp(y,0,10000)/10000F*Math.max(0,height-size);}
            default -> {cx=(width-15)/2+7.5F;cy=(height-15)/2+17+half;}
        }
        return new Position(Math.clamp(cx,half,Math.max(half,width-half)),Math.clamp(cy,half,Math.max(half,height-half)));
    }
    static int fraction(double coordinate,int length,int size){return (int)Math.round(Math.clamp((coordinate-size/2.0)/Math.max(1,length-size),0.0,1.0)*10000);}
}
