package me.earth.earthhack.impl.util.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.customfont.FontMod;
import me.earth.earthhack.impl.util.text.ChatUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

// thanks to Mironov and the demo <a href="https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/nanovg/">Demo</a>
// Thanks to FeSis/asphyxia1337 for the state saving thing (I forgot who made it I'm sorry)
@SuppressWarnings("unused")
public class NVGRenderer implements Globals {

    private final ModuleCache<FontMod> CUSTOM_FONT =
            Caches.getModule(FontMod.class);

    private int program, blendSrc, blendDst, stencilMask, stencilRef, stencilFuncMask, activeTexture, vertexArray, arrayBuffer, textureBinding;
    private boolean depthTest, scissorTest, init = false;
    private final boolean[] colorMask = new boolean[4];
    private static final float BLUR = 0.0f;
    private ByteBuffer buf = null;
    private long context = 0;
    private int id = -1;
    private float globalScale = 1.0f;
    private float globalOffsetX = 0.0f;
    private float globalOffsetY = 0.0f;
    private float globalAlpha = 1.0f;
    private final Deque<float[]> clipStack = new ArrayDeque<>();
    private boolean frameClipApplied = false;

    public void initialize() {
        context = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS);
        System.out.println("NanoVG context: " + context);

        try {
            byte[] fontBytes = CUSTOM_FONT.get().getSelectedFont();

            destroyBuffer();
            buf = MemoryUtil.memAlloc(fontBytes.length);
            buf.put(fontBytes);
            buf.flip();

            if (NanoVG.nvgCreateFontMem(context, CUSTOM_FONT.get().fontName.getValue(), buf, false) == -1)
                throw new RuntimeException("Failed to create font " + CUSTOM_FONT.get().fontName.getValue());

            // font id
            id = NanoVG.nvgFindFont(context, CUSTOM_FONT.get().fontName.getValue());
            if (id == -1) {
                CUSTOM_FONT.disable();
                ChatUtil.sendMessage("Failed to find font " + CUSTOM_FONT.get().fontName.getValue() + " in memory", "FontMod");
            }

            System.out.println("Loaded font " + CUSTOM_FONT.get().fontName.getValue() + " into memory");
            init = true;
        } catch (Exception e) {
            e.printStackTrace();
            CUSTOM_FONT.disable();
            ChatUtil.sendMessage("Failed to load font " + CUSTOM_FONT.get().fontName.getValue() + " into memory", "FontMod");
        }
    }

    public void destroyBuffer() {
        if (buf != null) {
            MemoryUtil.memFree(buf);
            buf = null;
        }
    }

    public void reInit() {
        if (context != 0 && init) {
            init = false;
            NanoVGGL3.nvgDelete(context);
            context = 0;
            id = -1;
            destroyBuffer();
            initialize();
        }
    }
    
    public boolean isInitialized() {
        return init;
    }
    
    // ----------------- Drawing -----------------

    public void setGlobalTransform(float offsetX, float offsetY, float scale, float alpha) {
        this.globalOffsetX = offsetX;
        this.globalOffsetY = offsetY;
        this.globalScale = scale;
        this.globalAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public void resetGlobalTransform() {
        this.globalOffsetX = 0.0f;
        this.globalOffsetY = 0.0f;
        this.globalScale = 1.0f;
        this.globalAlpha = 1.0f;
    }

    private float tx(float x) {
        return x * globalScale + globalOffsetX;
    }

    private float ty(float y) {
        return y * globalScale + globalOffsetY;
    }

    private float ts(float value) {
        return value * globalScale;
    }

    private int applyAlpha(int color) {
        int alpha = (color >> 24) & 255;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * globalAlpha)));
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }

    private Color applyAlpha(Color color) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * globalAlpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void textSized(String text, float x, float y, float size, Color color) {
        NanoVG.nvgBeginPath(context);

        NanoVG.nvgFontFaceId(context, id);
        NanoVG.nvgFillColor(context, getColorNVG(applyAlpha(color)));
        NanoVG.nvgFontSize(context, ts(size));
        NanoVG.nvgFontBlur(context, BLUR);
        NanoVG.nvgTextAlign(context, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(context, tx(x), ty(y), text);

        NanoVG.nvgClosePath(context);
    }

    private void textSizedShadow(String text, float x, float y, float size, Color color, Color shadowColor) {
        NanoVG.nvgBeginPath(context);

        NanoVG.nvgFontFaceId(context, id);
        NanoVG.nvgFontSize(context, ts(size));
        NanoVG.nvgTextAlign(context, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);

        NanoVG.nvgFontBlur(context, BLUR + (CUSTOM_FONT.get().blurShadow.getValue() ? 1.0f : 0.0f));
        NanoVG.nvgFillColor(context, getColorNVG(applyAlpha(shadowColor)));
        NanoVG.nvgText(context, tx(x + CUSTOM_FONT.get().shadowOffset.getValue()), ty(y + CUSTOM_FONT.get().shadowOffset.getValue()), text);

        NanoVG.nvgFontBlur(context, BLUR);
        NanoVG.nvgFillColor(context, getColorNVG(applyAlpha(color)));
        NanoVG.nvgText(context, tx(x), ty(y), text);

        NanoVG.nvgClosePath(context);
    }

    public void drawText(String text, float x, float y, float size, Color color, boolean shadow) {
        Color activeColor = color;
        Color shadowColor = new Color(ColorUtil.getDarker(activeColor));
        text = text.trim();

        String[] textParts = text.split("§");

        if (textParts.length == 1) {
            if (shadow)
                textSizedShadow(text, x, y, size, color, shadowColor);
            else
                textSized(text, x, y, size, color);
            return;
        }

        for (String s : textParts) {
            if (s.isEmpty())
                continue;

            switch (s.charAt(0)) {
                case '0' -> activeColor = Color.BLACK;
                case '1' -> activeColor = new Color(170);
                case '2' -> activeColor = new Color(43520);
                case '3' -> activeColor = new Color(43690);
                case '4' -> activeColor = new Color(11141120);
                case '5' -> activeColor = new Color(11141290);
                case '6' -> activeColor = new Color(16755200);
                case '7' -> activeColor = Color.GRAY;
                case '8' -> activeColor = Color.DARK_GRAY;
                case '9' -> activeColor = Color.BLUE;
                case 'a' -> activeColor = Color.GREEN;
                case 'b' -> activeColor = new Color(5636095);
                case 'c' -> activeColor = Color.RED;
                case 'd' -> activeColor = new Color(16733695);
                case 'e' -> activeColor = Color.YELLOW;
                case 'f' -> activeColor = Color.WHITE;
                case 'l' -> size += 1;
                case 'm' -> size -= 1;
                case 'n' -> shadow = true;
                case 'o' -> shadow = false;
                case '+' -> {
                    int value = Color.HSBtoRGB(Managers.COLOR.getHueByPosition(y), 1.0f, 1.0f);
                    activeColor = convertRainbowColor(value);
                }
                case '-' -> {
                    int value = Color.HSBtoRGB(Managers.COLOR.getHueByPosition(x), 1.0f, 1.0f);
                    activeColor = convertRainbowColor(value);
                }
                default -> activeColor = color;
            }
            if (activeColor != color)
                shadowColor = new Color(ColorUtil.getDarker(activeColor));

            if (s.length() > 1)  {
                if (activeColor != color)
                    s = s.substring(1);

                if (shadow)
                    textSizedShadow(s, x, y, size, activeColor, shadowColor);
                else
                    textSized(s, x, y, size, activeColor);
                x += getWidth(s) + (s.endsWith(" ") ? getWidth("a") / 1.5f : 0);
            }
        }
    }

    private Color convertRainbowColor(int color) {
        return new Color(
                (color >> 16 & 0xFF) / 255.0f
                        / (1),
                (color >> 8 & 0xFF)  / 255.0f
                        / (1),
                (color & 0xFF)       / 255.0f
                        / (1),
                1);
    }

    public void drawRect(float x, float y, float x2, float y2, int color) {
        NanoVG.nvgBeginPath(context);
        NanoVG.nvgRect(context, tx(x), ty(y), ts(x2 - x), ts(y2 - y));
        NanoVG.nvgFillColor(context, getColorNVG(applyAlpha(color)));
        NanoVG.nvgFill(context);
        NanoVG.nvgClosePath(context);
    }

    public void drawGradientRect(float x, float y, float w, float h, int startColor, int endColor) {
        NVGPaint paint = NVGPaint.create();

        float xT = tx(x);
        float yT = ty(y);
        float wT = ts(w);
        float hT = ts(h);
        NanoVG.nvgLinearGradient(context, xT, yT, xT + wT, yT + hT, getColorNVG(applyAlpha(startColor)), getColorNVG(applyAlpha(endColor)), paint);
        NanoVG.nvgBeginPath(context);
        NanoVG.nvgRect(context, xT, yT, wT, hT);
        NanoVG.nvgFillPaint(context, paint);
        NanoVG.nvgFill(context);
    }

    public void drawRoundedRect(float x, float y, float w, float h, float r, int color) {
        NanoVG.nvgBeginPath(context);
        NanoVG.nvgRoundedRect(context, tx(x), ty(y), ts(w), ts(h), ts(r));
        NanoVG.nvgFillColor(context, getColorNVG(applyAlpha(color)));
        NanoVG.nvgFill(context);
        NanoVG.nvgClosePath(context);
    }

    public void drawLine(float x, float y, float x2, float y2, float w, int color) {
        NanoVG.nvgBeginPath(context);
        NanoVG.nvgMoveTo(context, tx(x), ty(y));
        NanoVG.nvgLineTo(context, tx(x2), ty(y2));
        NanoVG.nvgStrokeWidth(context, ts(w));
        NanoVG.nvgStrokeColor(context, getColorNVG(applyAlpha(color)));
        NanoVG.nvgStroke(context);
        NanoVG.nvgClosePath(context);
    }

    public void enableScissors(float x, float y, float w, float h) {
        NanoVG.nvgSave(context);
        NanoVG.nvgScissor(context, tx(x), ty(y), ts(w), ts(h));
    }

    public void disableScissors() {
        NanoVG.nvgResetScissor(context);
        NanoVG.nvgRestore(context);
    }

    public void endScissor() {
        NanoVG.nvgResetScissor(context);
    }

    public void pushClip(float x, float y, float x2, float y2) {
        float sx = Math.min(x, x2);
        float sy = Math.min(y, y2);
        float ex = Math.max(x, x2);
        float ey = Math.max(y, y2);

        if (!clipStack.isEmpty()) {
            float[] parent = clipStack.peek();
            sx = Math.max(sx, parent[0]);
            sy = Math.max(sy, parent[1]);
            ex = Math.min(ex, parent[2]);
            ey = Math.min(ey, parent[3]);
        }

        clipStack.push(new float[]{sx, sy, ex, ey});
    }

    public void popClip() {
        if (!clipStack.isEmpty()) {
            clipStack.pop();
        }
    }

    public static NVGColor getColorNVG(Color color) {
        NVGColor clr = NVGColor.create();
        clr.r(color.getRed() / 255.0f);
        clr.g(color.getGreen() / 255.0f);
        clr.b(color.getBlue() / 255.0f);
        clr.a(color.getAlpha() / 255.0f);
        return clr;
    }

    public static NVGColor getColorNVG(int color) {
        NVGColor clr = NVGColor.create();
        clr.r((color >> 16 & 255) / 255.0f);
        clr.g((color >> 8 & 255) / 255.0f);
        clr.b((color & 255) / 255.0f);
        clr.a((color >> 24 & 255) / 255.0f);
        return clr;
    }

    public float getWidth(String text) {
        text = text.replaceAll("§[0-9abcdeflmno]", "");
        float[] bounds = new float[4];
        NanoVG.nvgTextBounds(context, 0, 0, text, bounds);
        return bounds[2] - bounds[0];
    }

    public float getHeight() {
        float[] bounds = new float[4];
        NanoVG.nvgTextBounds(context, 0, 0, "Aa", bounds);
        return bounds[3] - bounds[1];
    }

    public float measureWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '禮' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                if (code == 'z') {
                    i = Math.min(text.length() - 1, i + 8);
                } else if (code == 'p') {
                    i = Math.min(text.length() - 1, i + 36);
                }

                continue;
            }

            builder.append(c);
        }

        NanoVG.nvgFontFaceId(context, id);
        NanoVG.nvgFontSize(context, CUSTOM_FONT.get().fontSize.getValue());
        NanoVG.nvgTextAlign(context, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        float[] bounds = new float[4];
        NanoVG.nvgTextBounds(context, 0, 0, builder.toString(), bounds);
        return bounds[2] - bounds[0];
    }

    public float measureHeight() {
        NanoVG.nvgFontFaceId(context, id);
        NanoVG.nvgFontSize(context, CUSTOM_FONT.get().fontSize.getValue());
        NanoVG.nvgTextAlign(context, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        float[] bounds = new float[4];
        NanoVG.nvgTextBounds(context, 0, 0, "Aa", bounds);
        return bounds[3] - bounds[1];
    }

    public void startDraw() {
        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        glGetIntegerv(GL_CURRENT_PROGRAM, buffer);
        program = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_BLEND_SRC, buffer);
        blendSrc = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_BLEND_DST, buffer);
        blendDst = buffer.get(0);
        depthTest = glIsEnabled(GL_DEPTH_TEST);
        scissorTest = glIsEnabled(GL_SCISSOR_TEST);
        ByteBuffer colorMaskBuffer = BufferUtils.createByteBuffer(4);
        glGetBooleanv(GL_COLOR_WRITEMASK, colorMaskBuffer);
        for (int i = 0; i < 4; i++)
            colorMask[i] = colorMaskBuffer.get(i) != 0;
        buffer.clear();
        glGetIntegerv(GL_STENCIL_WRITEMASK, buffer);
        stencilMask = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_STENCIL_FUNC, buffer);
        stencilRef = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_STENCIL_VALUE_MASK, buffer);
        stencilFuncMask = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL13.GL_ACTIVE_TEXTURE, buffer);
        activeTexture = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, buffer);
        vertexArray = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL15.GL_ARRAY_BUFFER_BINDING, buffer);
        arrayBuffer = buffer.get(0);
        buffer.clear();
        glGetIntegerv(GL_TEXTURE_BINDING_2D, buffer);
        textureBinding = buffer.get(0);

        NanoVG.nvgBeginFrame(context, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 3f);
        if (!clipStack.isEmpty()) {
            float[] clip = clipStack.peek();
            NanoVG.nvgSave(context);
            NanoVG.nvgScissor(context, tx(clip[0]), ty(clip[1]), ts(Math.max(0.0f, clip[2] - clip[0])), ts(Math.max(0.0f, clip[3] - clip[1])));
            frameClipApplied = true;
        } else {
            frameClipApplied = false;
        }
    }

    public void endDraw() {
        if (frameClipApplied) {
            NanoVG.nvgResetScissor(context);
            NanoVG.nvgRestore(context);
            frameClipApplied = false;
        }

        NanoVG.nvgEndFrame(context);

        glUseProgram(program);
        glBlendFunc(blendSrc, blendDst);
        if (depthTest)
            glEnable(GL_DEPTH_TEST);
        else
            glDisable(GL_DEPTH_TEST);
        if (scissorTest)
            glEnable(GL_SCISSOR_TEST);
        else
            glDisable(GL_SCISSOR_TEST);
        glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        glStencilMask(stencilMask);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        glStencilFunc(stencilRef, stencilFuncMask, 0xffffffff);
        GL13.glActiveTexture(activeTexture);
        glBindVertexArray(vertexArray);
        glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        glBindTexture(GL_TEXTURE_2D, textureBinding);
    }

}
