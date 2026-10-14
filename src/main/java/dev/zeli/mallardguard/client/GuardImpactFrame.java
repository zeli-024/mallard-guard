package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Draws one captured scene with a rough grayscale ink treatment. */
final class GuardImpactFrame {
    private static final String VERTEX = """
        #version 150
        out vec2 uv;
        void main() {
            vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
            uv = p;
            gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
        }
        """;
    private static final String FRAGMENT = """
        #version 150
        uniform sampler2D scene;
        uniform sampler2D clean;
        uniform vec2 dimensions;
        uniform float brightness;
        uniform float contrast;
        uniform float edges;
        uniform float roughness;
        uniform int invert;
        uniform float chromatic;
        uniform int colorMode;
        uniform vec3 highlightColor;
        in vec2 uv;
        out vec4 result;
        float luminance(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
        float grain(ivec2 coordinate) {
            // Independent integer mixing avoids correlated diagonal bands.
            uint h = uint(coordinate.x) * 0x8da6b343u ^ uint(coordinate.y) * 0xd8163841u;
            h ^= h >> 16u;
            h *= 0x7feb352du;
            h ^= h >> 15u;
            h *= 0x846ca68bu;
            h ^= h >> 16u;
            return float(h & 255u) / 255.0;
        }
        float inkAt(vec2 at) {
            vec2 pixel = 1.0 / dimensions;
            float light = luminance(texture(scene, clamp(at, vec2(0.0), vec2(1.0))).rgb);
            float horizontal = abs(light - luminance(texture(scene, at + vec2(pixel.x * 2.0, 0.0)).rgb));
            float vertical = abs(light - luminance(texture(scene, at + vec2(0.0, pixel.y * 2.0)).rgb));
            float edge = min(0.28, (horizontal + vertical) * 0.68);
            float textureNoise = grain(ivec2(floor(gl_FragCoord.xy * 0.5)));
            float ink = clamp((light - 0.5) * 1.20 * contrast + 0.65 + (brightness - 1.0) * 0.35
                - edge * 0.82 * edges + (textureNoise - 0.5) * 0.08 * roughness, 0.0, 1.0);
            ink = mix(ink, smoothstep(0.17, 0.79, ink), 0.18);
            ink = 0.10 + 0.90 * pow(ink, 0.78);
            return invert == 1 ? 1.0 - ink : ink;
        }
        void main() {
            if (colorMode == 4) {
                vec3 original=texture(scene,uv).rgb;
                float silhouette=texture(clean,uv).a;
                vec3 world=original*(1.0-clamp(edges,0.0,1.0)*chromatic*(1.0-silhouette));
                world=mix(world,original,silhouette*chromatic);
                if(roughness>0.0){
                    vec2 pixel=vec2(1.0)/dimensions;
                    float interior=silhouette;
                    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)interior=min(interior,texture(clean,clamp(uv+vec2(x,y)*pixel,vec2(0.0),vec2(1.0))).a);
                    float edge=max(0.0,silhouette-interior);
                    float tint=(silhouette*0.35+edge*0.65)*roughness*chromatic;
                    world=mix(world,max(original,highlightColor),clamp(tint,0.0,1.0));
                }
                result=vec4(world,1.0);return;
            }
            if (colorMode == 5) { result=vec4(texture(scene,uv).rgb*(1.0-clamp(edges,0.0,1.0)*chromatic),1.0);return; }
            if (colorMode == 3) { result = mix(texture(clean, uv), texture(scene, uv), chromatic); return; }
            if (colorMode == 2) {
                vec2 stepSize = vec2(5.0) / dimensions;
                vec3 blurred = texture(scene, uv).rgb * 0.20;
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++)
                    if (x != 0 || y != 0) blurred += texture(scene, clamp(uv + vec2(x,y) * stepSize, vec2(0.0), vec2(1.0))).rgb * 0.10;
                result = vec4(blurred, 1.0); return;
            }
            vec2 shift = (uv - 0.5) * chromatic * 0.012;
            vec2 redUV = clamp(uv + shift, vec2(0.0), vec2(1.0));
            vec2 blueUV = clamp(uv - shift, vec2(0.0), vec2(1.0));
            if (colorMode == 1) {
                result = vec4(texture(scene, redUV).r, texture(scene, uv).g, texture(scene, blueUV).b, 1.0);
            } else if (chromatic == 0.0) {
                float ink = inkAt(uv);
                result = vec4(vec3(ink), 1.0);
            } else result = vec4(inkAt(redUV), inkAt(uv), inkAt(blueUV), 1.0);
        }
        """;
    private static int program;
    private static int vertexArray;
    private static int uniformHighlightColor, uniformClean, uniformScene, uniformInvert, uniformColorMode, uniformChromatic, uniformDimensions, uniformBrightness, uniformContrast, uniformEdges, uniformRoughness;
    private static boolean unavailable;

    private GuardImpactFrame() {}

    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(4);
    static boolean draw(RenderTarget source, RenderTarget destination) { return draw(source, destination, false); }
    static boolean draw(RenderTarget source, RenderTarget destination, boolean mob) {
        boolean separate = mob && !dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_ADAPT.get();
        return draw(source, destination,
            (separate ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_BRIGHTNESS : dev.zeli.mallardguard.GuardConfig.IMPACT_BRIGHTNESS).get(),
            (separate ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_CONTRAST : dev.zeli.mallardguard.GuardConfig.IMPACT_CONTRAST).get(),
            (separate ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_EDGES : dev.zeli.mallardguard.GuardConfig.IMPACT_EDGES).get(),
            (separate ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_GRAIN : dev.zeli.mallardguard.GuardConfig.IMPACT_GRAIN).get(),
            (separate ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_CHROMATIC : dev.zeli.mallardguard.GuardConfig.IMPACT_CHROMATIC).get(),
            mob ? dev.zeli.mallardguard.GuardConfig.MOB_IMPACT_INVERT.get() : dev.zeli.mallardguard.GuardScreenSettings.enabled(dev.zeli.mallardguard.GuardScreenSettings.PLAYER_IMPACT_INVERT));
    }

    static boolean draw(RenderTarget source, RenderTarget destination, int brightness, int contrast, int edges, int roughness, int chromatic, boolean invert) {
        return render(source, destination, brightness, contrast, edges, roughness, chromatic / 100.0F, invert, 0, null);
    }
    static boolean drawChromatic(RenderTarget source, RenderTarget destination, float strength) {
        return render(source, destination, 100, 100, 0, 0, strength, false, 1, null);
    }
    static boolean drawBlur(RenderTarget source, RenderTarget destination) {
        return render(source, destination, 100, 100, 0, 0, 1, false, 2, null);
    }
    static boolean drawHighlight(RenderTarget source,RenderTarget mask,RenderTarget destination,float amount,int dim,int glow,int color){
        return render(source,destination,100,100,dim,glow,amount,false,mask==null?5:4,mask,color);
    }
    static boolean drawMix(RenderTarget source, RenderTarget clean, RenderTarget destination, float opacity) {
        return render(source, destination, 100, 100, 0, 0, opacity, false, 3, clean);
    }
    private static boolean render(RenderTarget source, RenderTarget destination, int brightness, int contrast, int edges, int roughness, float chromatic, boolean invert, int colorMode, RenderTarget clean) {
        return render(source,destination,brightness,contrast,edges,roughness,chromatic,invert,colorMode,clean,0xFFFFFF);
    }
    private static boolean render(RenderTarget source, RenderTarget destination, int brightness, int contrast, int edges, int roughness, float chromatic, boolean invert, int colorMode, RenderTarget clean,int color) {
        if (!ensureShader()) return false;
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        IntBuffer viewport = VIEWPORT; viewport.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int previousClean = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, destination.frameBufferId);
            GL11.glViewport(0, 0, destination.width, destination.height);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, source.getColorTextureId());
            GL20.glUniform1i(uniformScene, 0);
            GL20.glUniform3f(uniformHighlightColor,(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);
            if (clean != null) { GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, clean.getColorTextureId()); GL20.glUniform1i(uniformClean, 1); GL13.glActiveTexture(GL13.GL_TEXTURE0); }
            GL20.glUniform1i(uniformInvert, invert ? 1 : 0);
            GL20.glUniform1i(uniformColorMode, colorMode);
            GL20.glUniform1f(uniformChromatic, chromatic);
            GL20.glUniform2f(uniformDimensions, destination.width, destination.height);
            GL20.glUniform1f(uniformBrightness, brightness / 100.0F);
            GL20.glUniform1f(uniformContrast, contrast / 100.0F);
            GL20.glUniform1f(uniformEdges, edges / 100.0F);
            GL20.glUniform1f(uniformRoughness, roughness / 100.0F);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            return true;
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousClean);
            GL13.glActiveTexture(previousTextureUnit);
            GL30.glBindVertexArray(previousArray);
            GL20.glUseProgram(previousProgram);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (blend) GL11.glEnable(GL11.GL_BLEND);
        }
    }

    private static boolean ensureShader() {
        if (program != 0) return true;
        if (unavailable) return false;
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            unavailable = true;
            return false;
        }
        int candidate = GL20.glCreateProgram();
        GL20.glAttachShader(candidate, vertex);
        GL20.glAttachShader(candidate, fragment);
        GL20.glLinkProgram(candidate);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(candidate, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Mallard Guard impact frame shader link failed: " + GL20.glGetProgramInfoLog(candidate));
            GL20.glDeleteProgram(candidate);
            unavailable = true;
            return false;
        }
        program = candidate;
        uniformScene = GL20.glGetUniformLocation(program, "scene");
        uniformHighlightColor=GL20.glGetUniformLocation(program,"highlightColor");
        uniformClean = GL20.glGetUniformLocation(program, "clean");
        uniformInvert = GL20.glGetUniformLocation(program, "invert");
        uniformColorMode = GL20.glGetUniformLocation(program, "colorMode");
        uniformChromatic = GL20.glGetUniformLocation(program, "chromatic");
        uniformDimensions = GL20.glGetUniformLocation(program, "dimensions");
        uniformBrightness = GL20.glGetUniformLocation(program, "brightness");
        uniformContrast = GL20.glGetUniformLocation(program, "contrast");
        uniformEdges = GL20.glGetUniformLocation(program, "edges");
        uniformRoughness = GL20.glGetUniformLocation(program, "roughness");
        vertexArray = GL30.glGenVertexArrays();
        return true;
    }

    private static int compile(int kind, String source) {
        int shader = GL20.glCreateShader(kind);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE) return shader;
        System.err.println("Mallard Guard impact frame shader compile failed: " + GL20.glGetShaderInfoLog(shader));
        GL20.glDeleteShader(shader);
        return 0;
    }
}
