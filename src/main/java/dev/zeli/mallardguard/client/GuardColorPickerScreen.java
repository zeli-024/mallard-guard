package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.function.IntConsumer;

/** A cached hue wheel and saturation/value triangle, opened from a color swatch. */
final class GuardColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer accept;
    private final int original;
    private double hue,saturation,value;
    private DynamicTexture texture;
    private double paintedHue=Double.NaN;
    private ResourceLocation location;
    private EditBox hex;
    private int left,top,drag;
    private boolean syncing;
    GuardColorPickerScreen(Screen parent,String title,int color,IntConsumer accept){
        super(Component.literal(title));this.parent=parent;this.accept=accept;original=color;
        double[] hsv=GuardColorPickerMath.hsv(color);hue=hsv[0];saturation=hsv[1];value=hsv[2];
    }
    @Override protected void init(){
        left=(width-224)/2;top=(height-224)/2;
        if(texture==null){paintedHue=Double.NaN;texture=new DynamicTexture(new NativeImage(128,128,false));location=Minecraft.getInstance().getTextureManager().register("mallard_guard_color_picker",texture);}
        updateTexture();
        hex=addRenderableWidget(GuardUi.editBox(font,left+90,top+161,102,"Hex color"));
        hex.setMaxLength(7);hex.setValue(String.format("#%06X",color()));
        hex.setResponder(text->{if(syncing)return;String raw=text.startsWith("#")?text.substring(1):text;if(!raw.matches("[0-9a-fA-F]{6}"))return;double[] hsv=GuardColorPickerMath.hsv(Integer.parseInt(raw,16));hue=hsv[0];saturation=hsv[1];value=hsv[2];updateTexture();});
        addRenderableWidget(GuardUi.button("?",left+196,top+161,20,20,()->GuardUi.choices(this,"Color Help","Choose hue on the wheel and saturation and brightness in the triangle, or enter a six-digit hex color such as #FFFFFF.\n\nApply uses the color; Cancel leaves it unchanged.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        addRenderableWidget(GuardUi.button("Apply",left+16,top+194,92,20,()->{accept.accept(color());onClose();}));
        addRenderableWidget(GuardUi.button("Cancel",left+116,top+194,92,20,this::onClose));
    }
    private int color(){return GuardColorPickerMath.rgb(hue,saturation,value);}
    private void updateTexture(){
        if(paintedHue==hue)return;
        NativeImage image=texture.getPixels();if(image==null)return;
        // Subpixel coverage is computed only when hue changes; rendering still uses one cached quad.
        final int samples=4;
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int count=0,red=0,green=0,blue=0;
            for(int sy=0;sy<samples;sy++)for(int sx=0;sx<samples;sx++){
                double px=x+(sx+.5)/samples,py=y+(sy+.5)/samples;
                double dx=px-64,dy=py-64,r=Math.sqrt(dx*dx+dy*dy);int rgb;
                if(r>=60&&r<=62||r>=47&&r<48)rgb=GuardUi.HIGHLIGHT&0xFFFFFF;
                else if(r>=48&&r<60)rgb=GuardColorPickerMath.rgb(Math.atan2(dy,dx)/(2*Math.PI),1,1);
                else {
                    double pure=(px-38)/68,black=(py-28-36*pure)/72,white=1-pure-black;
                    if(pure<0||white<0||black<0)continue;
                    double v=pure+white;
                    rgb=pure*68<1||white*63.9<1||black*63.9<1?GuardUi.HIGHLIGHT&0xFFFFFF:GuardColorPickerMath.rgb(hue,v==0?0:pure/v,v);
                }
                count++;red+=rgb>>16&255;green+=rgb>>8&255;blue+=rgb&255;
            }
            int pixel=count==0?0:Math.round(count*255f/(samples*samples))<<24|Math.round(blue/(float)count)<<16|Math.round(green/(float)count)<<8|Math.round(red/(float)count);
            image.setPixelRGBA(x,y,pixel);
        }
        texture.upload();paintedHue=hue;
    }
    private boolean pick(double mx,double my,boolean starting){
        double x=mx-(left+48),y=my-(top+25),dx=x-64,dy=y-64,r=Math.sqrt(dx*dx+dy*dy);
        if(starting){double[] w=GuardColorPickerMath.weights(x,y);drag=r>=47&&r<=62?1:w[0]>=0&&w[1]>=0&&w[2]>=0?2:0;}
        if(drag==0)return false;
        if(drag==1){hue=Math.atan2(dy,dx)/(2*Math.PI);hue-=Math.floor(hue);updateTexture();}
        else {double[] sv=GuardColorPickerMath.selection(x,y);saturation=sv[0];value=sv[1];}
        syncing=true;hex.setValue(String.format("#%06X",color()));syncing=false;return true;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.flush();
        renderBackground(g,mouseX,mouseY,partial);
        g.flush();
        g.fill(0,0,width,height,0x3219141E);
        GuardUi.panel(g,left,top,224,224,false);
        g.drawCenteredString(font,title,left+112,top+10,GuardUi.TEXT);
        g.fill(left+12,top+22,left+212,top+23,GuardUi.WARM);
        g.blit(location,left+48,top+25,0,0,128,128,128,128);
        double angle=hue*Math.PI*2;marker(g,left+48+64+54*Math.cos(angle),top+25+64+54*Math.sin(angle));
        double[] point=GuardColorPickerMath.point(saturation,value);marker(g,left+48+point[0],top+25+point[1]);
        g.drawString(font,"Old",left+28,top+150,GuardUi.MUTED);
        g.drawString(font,"New",left+54,top+150,GuardUi.MUTED);
        swatch(g,left+28,top+161,original);swatch(g,left+54,top+161,color());
        for(var widget:renderables)widget.render(g,mouseX,mouseY,partial);
    }
    private static void swatch(GuiGraphics g,int x,int y,int color){g.fill(x+1,y+1,x+21,y+19,0xFF000000|color);g.renderOutline(x,y,22,20,GuardUi.BORDER);}
    private static void marker(GuiGraphics g,double x,double y){int a=(int)Math.round(x),b=(int)Math.round(y);g.fill(a-3,b-3,a+4,b+4,0xFF000000);g.fill(a-2,b-2,a+3,b+3,0xFFFFFFFF);g.fill(a-1,b-1,a+2,b+2,0xFF000000);}
    @Override public boolean mouseClicked(double x,double y,int button){return button==0&&pick(x,y,true)||super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return button==0&&drag!=0&&pick(x,y,false)||super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){drag=0;return super.mouseReleased(x,y,button);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){if(location!=null)Minecraft.getInstance().getTextureManager().release(location);texture=null;location=null;drag=0;}
}
