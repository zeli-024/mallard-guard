package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Fades the complete editor layer and blur together without changing widget hitboxes. */
abstract class GuardPreviewScreen extends Screen {
    protected boolean hidden, adjusting;
    private float opacity = 1;
    private long frameTime, editingUntil, noticeUntil;
    private TextureTarget clean, composed;
    protected Button visibility;
    GuardPreviewScreen(String title) { super(Component.literal(title)); }
    protected void visibilityButton() {
        visibility=addRenderableWidget(GuardUi.button(hidden?"Show UI":"Hide UI",Math.max(4,width-78),5,72,20,()->{
            toggleVisibility();
        }));
    }
    protected final void toggleVisibility(){hidden=!hidden;noticeUntil=hidden?System.nanoTime()+2_000_000_000L:0;}
    protected void beginPreview(GuiGraphics g) {
        g.flush(); RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        long now = System.nanoTime(); float elapsed = frameTime == 0 ? 1 : Math.min(1, (now - frameTime) / 140000000.0F); frameTime = now;
        float fadeTarget=hidden?0:(adjusting||now<editingUntil)?Math.min(0.06F,previewOpacity()):previewOpacity();
        opacity += (fadeTarget - opacity) * elapsed;
        boolean blur=blurPreview();
        if(!blur&&opacity>=0.999F){opacity=1;return;}
        if (clean == null || clean.width != target.width || clean.height != target.height) {
            dispose(); clean = new TextureTarget(target.width, target.height, false, false);
        }
        copy(target, clean);
        if (blur) GuardImpactFrame.drawBlur(clean, target);
        target.bindWrite(false);
    }
    protected boolean blurPreview(){return true;}
    protected float previewOpacity(){return 1.0F;}
    protected void restorePreviewOpacity(){}
    protected final void previewOpacityChanged(){if(previewOpacity()<=0.001F)noticeUntil=System.nanoTime()+2_000_000_000L;}
    protected boolean fadesWhileAdjusting(net.minecraft.client.gui.components.AbstractSliderButton slider){return true;}
    protected final void valueChanged(){editingUntil=System.nanoTime()+400_000_000L;}
    protected void endPreview(GuiGraphics g) {
        if (clean == null) return;
        if(opacity<0.999F){
            g.flush();RenderTarget target=Minecraft.getInstance().getMainRenderTarget();
            if(composed==null)composed=new TextureTarget(target.width,target.height,false,false);copy(target,composed);
            GuardImpactFrame.drawMix(composed,clean,target,opacity);target.bindWrite(false);
        }
        if((hidden||previewOpacity()<=0.001F)&&System.nanoTime()<noticeUntil){
            long remaining=noticeUntil-System.nanoTime();
            float alpha=Math.min(1,Math.min((2_000_000_000L-remaining)/180_000_000F,remaining/350_000_000F));
            String text="Click anywhere to show the controls.";int w=font.width(text)+20,x=(width-w)/2,y=(height-22)/2;
            int a=Math.clamp((int)(255*alpha),0,255);
            g.fill(x,y,x+w,y+22,((int)(187*alpha)<<24)|0x493E55);g.renderOutline(x,y,w,22,(a<<24)|0x947D9A);if(a>3)g.drawCenteredString(font,text,width/2,y+7,(a<<24)|0xF5F0F6);
        }
    }
    protected static void copy(RenderTarget from,RenderTarget to){GuardRenderTargets.copy(from,to);}
    @Override public boolean mouseClicked(double x, double y, int button) {
        if(hidden||previewOpacity()<=0.001F){hidden=false;noticeUntil=0;if(previewOpacity()<=0.001F)restorePreviewOpacity();return true;}
        adjusting = button == 0 && children().stream().anyMatch(w -> w instanceof net.minecraft.client.gui.components.AbstractSliderButton a && a.active && a.visible && fadesWhileAdjusting(a) && a.isMouseOver(x,y));
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { adjusting = false; return super.mouseReleased(x,y,button); }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){return hidden||previewOpacity()<=0.001F||super.mouseScrolled(x,y,sx,sy);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(hidden||previewOpacity()<=0.001F){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){hidden=false;noticeUntil=0;if(previewOpacity()<=0.001F)restorePreviewOpacity();}return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean charTyped(char ch,int mods){return hidden||previewOpacity()<=0.001F||super.charTyped(ch,mods);}
    private void dispose() { if (clean != null) clean.destroyBuffers(); if (composed != null) composed.destroyBuffers(); clean = composed = null; }
    @Override public void removed() { dispose();adjusting=false;editingUntil=frameTime=0;opacity=1; }
    @Override public boolean isPauseScreen() { return false; }
}
