package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.GlStateBackup;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Copies color targets while restoring framebuffer and scissor state. */
final class GuardRenderTargets {
    private GuardRenderTargets(){}
    static void copy(RenderTarget source, RenderTarget target) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, source.width, source.height,
                0, 0, target.width, target.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
    /** Reusable snapshot for material draws; framebuffer state is not part of GlStateBackup. */
    static final class State {
        private final GlStateBackup flags=new GlStateBackup();
        private final IntBuffer viewport=BufferUtils.createIntBuffer(4);
        private final int[] shaderTextures=new int[3],textures=new int[3];
        private final float[] color=new float[4];
        private final Matrix4f textureMatrix=new Matrix4f();
        private ShaderInstance shader;
        private int read,draw,program,array,activeTexture;
        void save(){
            RenderSystem.backupGlState(flags);
            read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            viewport.clear();GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);array=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            shader=RenderSystem.getShader();System.arraycopy(RenderSystem.getShaderColor(),0,color,0,4);
            textureMatrix.set(RenderSystem.getTextureMatrix());
            for(int i=0;i<3;i++){shaderTextures[i]=RenderSystem.getShaderTexture(i);GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);textures[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);}
            GlStateManager._activeTexture(activeTexture);
        }
        void restore(){
            RenderSystem.restoreGlState(flags);
            RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.setTextureMatrix(textureMatrix);
            for(int i=0;i<3;i++){RenderSystem.setShaderTexture(i,shaderTextures[i]);GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);GlStateManager._bindTexture(textures[i]);}
            GlStateManager._activeTexture(activeTexture);
            // An extra material draw changed the uploader's cached buffer binding.
            BufferUploader.reset();RenderSystem.glBindVertexArray(array);GL20.glUseProgram(program);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));
        }
    }

}
