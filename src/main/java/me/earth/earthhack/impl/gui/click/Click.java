package me.earth.earthhack.impl.gui.click;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.impl.ColorComponent;
import me.earth.earthhack.impl.gui.click.component.impl.KeybindComponent;
import me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent;
import me.earth.earthhack.impl.gui.click.component.impl.StringComponent;
import me.earth.earthhack.impl.gui.click.frame.Frame;
import me.earth.earthhack.impl.gui.click.frame.impl.CategoryFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.DescriptionFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.ModulesFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.SearchFrame;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.managers.client.ModuleManager;
import me.earth.earthhack.impl.managers.render.TextRenderer;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.modules.client.commands.Commands;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.NonnullDefault;

import java.awt.*;
import java.util.ArrayList;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class Click extends Screen {
    public static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);
    private static final long OPEN_ANIMATION_TIME_MS = 500L;
    private static final long CLOSE_ANIMATION_TIME_MS = 260L;
    private static final float FRAME_VERTICAL_STAGGER_MS = 190.0f;
    private static final float SPREAD_X_DISTANCE = 80.0f;
    private static final float SPREAD_Y_DISTANCE = 60.0f;

    private static final SettingCache<Boolean, BooleanSetting, Commands> BACK =
            Caches.getSetting(Commands.class, BooleanSetting.class, "BackgroundGui", false);
    private static final Identifier BLACK_PNG =
            Identifier.of("earthhack:textures/gui/black.png");
    private final ArrayList<Frame> frames = new ArrayList<>();
    private Category[] categories = Category.values();
    private final ModuleManager moduleManager;
    private boolean oldVal = false;
    private boolean attached = false;
    private boolean addDescriptionFrame = true;
    private boolean pingBypass;
    private long openAnimationStartedAt;
    private boolean openAnimationStarted;
    private boolean closing;
    private long closeAnimationStartedAt;
    private float closeStartVisibility;
    private boolean closingNow;

    public final Screen screen;

    public static DescriptionFrame descriptionFrame =
            new DescriptionFrame(0, 0, 200, 18);
    private static float renderOffsetX;
    private static float renderOffsetY;
    private static float renderFrameAlpha = 1.0f;

    public Click(Screen screen) {
        super(Text.of("ClickGui"));
        this.moduleManager = Managers.MODULES;
        this.screen = screen;
    }

    public Click(Screen screen, ModuleManager moduleManager) {
        super(Text.of("ClickGui"));
        this.moduleManager = moduleManager;
        this.screen = screen;
    }

    public void init() {
        if (!attached) {
            CLICK_GUI.get().descriptionWidth.addObserver(e -> descriptionFrame.setWidth(e.getValue()));
            attached = true;
        }

        getFrames().clear();
        int x = CLICK_GUI.get().catEars.getValue() ? 14 : 2;
        int y = CLICK_GUI.get().catEars.getValue() ? 14 : 2;
        for (Category moduleCategory : categories) {
            getFrames().add(new CategoryFrame(moduleCategory, moduleManager, x, y, 110, 16));
            if (x + 220 >= MinecraftClient.getInstance().getWindow().getScaledWidth()) {
                x = CLICK_GUI.get().catEars.getValue() ? 14 * Math.round(CLICK_GUI.get().guiScale.getValue()) : 2;
                y += CLICK_GUI.get().catEars.getValue() ? 32 * CLICK_GUI.get().guiScale.getValue() : 20;
            } else
                x += (CLICK_GUI.get().catEars.getValue() ? 132 * CLICK_GUI.get().guiScale.getValue() : 112);
        }

        if (addDescriptionFrame) {
            descriptionFrame = new DescriptionFrame(CLICK_GUI.get().descPosX.getValue(), CLICK_GUI.get().descPosY.getValue(), CLICK_GUI.get().descriptionWidth.getValue(), 16);
            getFrames().add(descriptionFrame);
        }

        if (pingBypass) {
            DescriptionFrame hint = new DescriptionFrame("Info", x, y + 100, CLICK_GUI.get().descriptionWidth.getValue(), 16);
            hint.setDescription("You are editing the modules running on the PingBypass server, not the ones which run here on your client.");
            getFrames().add(hint);

            ModulesFrame pbFrame = new ModulesFrame("PingBypass", x, y + 200, 110, 16);

            // java.lang.NoClassDefFoundError: me/earth/earthhack/pingbypass/modules/SyncModule
            //	    at me.earth.earthhack.impl.modules.client.clickgui.ClickGui.lambda$new$7(ClickGui.java:152) ~[main/:?]
            //
            // pbFrame.getComponents().add(new ModuleComponent(new SyncModule(), pbFrame.getPosX(), pbFrame.getPosY(), 0, pbFrame.getHeight() + 1, pbFrame.getWidth(), 14));

            getFrames().add(pbFrame);
        }

        if (CLICK_GUI.get().search.getValue() != ClickGui.SearchStyle.None) {
            SearchFrame searchFrame = new SearchFrame();
            getFrames().add(searchFrame);
            searchFrame.clearInput();
        }

        getFrames().forEach(Frame::init);
        oldVal = CLICK_GUI.get().catEars.getValue();
    }

    @Override
    @NonnullDefault
    public void resize(MinecraftClient mcIn, int w, int h) {
        super.resize(mcIn, w, h);
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (mc.world == null)
        {
            if (BACK.getValue())
            {
                this.renderBackground(context, mouseX, mouseY, delta);
            }
            else
            {
                RenderSystem.disableCull();
                // RenderSystem.disableFog();
                RenderSystem.setShaderTexture(0, BLACK_PNG);
                RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
                BufferBuilder bufferbuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferbuilder.vertex(0.0f, this.height, 0.0f).texture(0.0F, (float)this.height / 32.0F + (float)0).color(64, 64, 64, 255);
                bufferbuilder.vertex(this.width, this.height, 0.0f).texture((float)this.width / 32.0F, (float)this.height / 32.0F + (float)0).color(64, 64, 64, 255);
                bufferbuilder.vertex(this.width, 0.0f, 0.0f).texture((float)this.width / 32.0F, 0).color(64, 64, 64, 255);
                bufferbuilder.vertex(0.0f, 0.0f, 0.0f).texture(0.0F, 0).color(64, 64, 64, 255);
                bufferbuilder.end();
            }
        }

        if (oldVal != CLICK_GUI.get().catEars.getValue()) {
            init();
            oldVal = CLICK_GUI.get().catEars.getValue();
        }

        if (CLICK_GUI.get().blur.getValue() == ClickGui.BlurStyle.Directional) {
            Window scaledResolution = MinecraftClient.getInstance().getWindow();
            // Render2DUtil.drawBlurryRect(context.getMatrices() , 0, 0, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), CLICK_GUI.get().blurAmount.getValue(), CLICK_GUI.get().blurSize.getValue());
        }

        if (shouldAnimate() && !openAnimationStarted) {
            openAnimationStarted = true;
            openAnimationStartedAt = System.currentTimeMillis();
        }

        if (closing && getCloseProgress() >= 1.0f) {
            closeNow();
            return;
        }

        ModuleComponent.context = context;
        if (shouldAnimate()) {
            boolean customFont = Managers.TEXT.usingCustomFont();
            float screenCenterX = width / 2.0f;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            for (int i = 0; i < getFrames().size(); i++) {
                Frame frame = getFrames().get(i);
                float frameAlpha = getFrameAlpha(frame);
                float slideProgress = getFrameSlideProgress(frame);
                float xOffset = getFrameXOffset(frame, slideProgress, screenCenterX);
                float yOffset = getFrameYOffset(slideProgress);

                if (!customFont) {
                    xOffset = Math.round(xOffset);
                    yOffset = Math.round(yOffset);
                }

                if (frameAlpha <= 0.001f) {
                    continue;
                }

                if (customFont) {
                    TextRenderer.FONTS.setGlobalTransform(xOffset, yOffset, 1.0f, frameAlpha);
                }

                context.getMatrices().push();
                context.getMatrices().translate(xOffset, yOffset, 0.0f);
                renderOffsetX = xOffset;
                renderOffsetY = yOffset;
                renderFrameAlpha = frameAlpha;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, frameAlpha);
                frame.drawScreen(context, mouseX, mouseY, delta);
                context.getMatrices().pop();
                renderOffsetX = 0.0f;
                renderOffsetY = 0.0f;
                renderFrameAlpha = 1.0f;
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (customFont) {
                TextRenderer.FONTS.resetGlobalTransform();
            }
            return;
        }

        TextRenderer.FONTS.resetGlobalTransform();
        renderOffsetX = 0.0f;
        renderOffsetY = 0.0f;
        getFrames().forEach(frame -> frame.drawScreen(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (shouldBlockInput()) {
            return true;
        }

        getFrames().forEach(frame -> frame.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (shouldBlockInput()) {
            return true;
        }

        getFrames().forEach(frame -> frame.keyPressed(keyCode));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (shouldBlockInput()) {
            return true;
        }

        getFrames().forEach(frame -> frame.mouseScrolled(mouseX, mouseY, verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (shouldBlockInput()) {
            return true;
        }

        getFrames().forEach(frame -> frame.mouseClicked(mouseX, mouseY, mouseButton));
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (shouldBlockInput()) {
            return true;
        }

        getFrames().forEach(frame -> frame.mouseReleased(mouseX, mouseY, mouseButton));
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }


    @Override
    public void close() {
        if (!closing && shouldAnimate()) {
            if (!openAnimationStarted) {
                openAnimationStarted = true;
                openAnimationStartedAt = System.currentTimeMillis();
            }

            closing = true;
            closeAnimationStartedAt = System.currentTimeMillis();
            closeStartVisibility = getOpenVisibility();
            return;
        }

        closeNow();
    }

    private boolean shouldAnimate() {
        return CLICK_GUI.get().animations.getValue();
    }

    private boolean shouldBlockInput() {
        return shouldAnimate() && (closing || getOpenProgress() < 0.92f);
    }

    private float getOpenProgress() {
        if (!openAnimationStarted) {
            return 0.0f;
        }

        float progress = (System.currentTimeMillis() - openAnimationStartedAt) / (float) OPEN_ANIMATION_TIME_MS;
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private float getCloseProgress() {
        if (!closing) {
            return 0.0f;
        }

        float progress = (System.currentTimeMillis() - closeAnimationStartedAt) / (float) CLOSE_ANIMATION_TIME_MS;
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private float getOpenVisibility() {
        float phase = clamp01(getOpenProgress() / 0.5f);
        return easeOutCubic(phase);
    }

    private float getFrameAlpha(Frame frame) {
        if (!closing) {
            long elapsed = System.currentTimeMillis() - openAnimationStartedAt;
            float delay = getFrameOpenDelay(frame);
            float local = clamp01((elapsed - delay) / 200.0f);
            return easeOutCubic(local);
        }

        float closeP = getCloseProgress();
        float delay = getFrameCloseDelay(frame);
        float totalCloseMs = CLOSE_ANIMATION_TIME_MS;
        float local = clamp01((closeP * totalCloseMs - delay) / 200.0f);
        return 1.0f - easeInCubic(local);
    }

    private float getFrameSlideProgress(Frame frame) {
        if (!closing) {
            long elapsed = System.currentTimeMillis() - openAnimationStartedAt;
            float delay = getFrameOpenDelay(frame);
            float duration = Math.max(1.0f, OPEN_ANIMATION_TIME_MS - delay);
            float local = clamp01((elapsed - delay) / duration);
            return easeOutCubic(local);
        }

        float closeP = getCloseProgress();
        float delay = getFrameCloseDelay(frame);
        float totalCloseMs = CLOSE_ANIMATION_TIME_MS;
        float duration = Math.max(1.0f, totalCloseMs - delay);
        float local = clamp01((closeP * totalCloseMs - delay) / duration);
        return 1.0f - easeInCubic(local);
    }

    private float getFrameOpenDelay(Frame frame) {
        float normalizedY = clamp01(frame.getPosY() / Math.max(1.0f, (float) height));
        return normalizedY * FRAME_VERTICAL_STAGGER_MS;
    }

    private float getFrameCloseDelay(Frame frame) {
        float normalizedY = clamp01(frame.getPosY() / Math.max(1.0f, (float) height));
        return (1.0f - normalizedY) * (FRAME_VERTICAL_STAGGER_MS * 0.6f);
    }

    private float getFrameXOffset(Frame frame, float slideProgress, float screenCenterX) {
        float frameCenterX = frame.getPosX() + frame.getWidth() / 2.0f;
        float normalizedFromCenter = (frameCenterX - screenCenterX) / Math.max(1.0f, screenCenterX);
        float spreadOffset = normalizedFromCenter * SPREAD_X_DISTANCE;
        return spreadOffset * (1.0f - slideProgress);
    }

    private float getFrameYOffset(float slideProgress) {
        return -SPREAD_Y_DISTANCE * (1.0f - slideProgress);
    }

    private float getVisibility() {
        if (!closing) {
            return getOpenVisibility();
        }

        return lerp(closeStartVisibility, 0.0f, easeInCubic(getCloseProgress()));
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3.0f);
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    private float smoothStep(float x) {
        float t = clamp01(x);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float getModuleOpenReveal(float x, float y, float elementHeight) {
        if (!(mc.currentScreen instanceof Click click)) {
            return 1.0f;
        }

        return click.getModuleOpenRevealInternal(x, y, elementHeight);
    }

    public static float getModuleEnabledFillReveal(float x, float y, float elementHeight) {
        if (!(mc.currentScreen instanceof Click click)) {
            return 1.0f;
        }

        return click.getModuleEnabledFillRevealInternal(x, y, elementHeight);
    }

    private float getModuleOpenRevealInternal(float x, float y, float elementHeight) {
        if (!shouldAnimate()) {
            return 1.0f;
        }

        if (closing) {
            // Cascade text fade-out bottom-to-top: bottom modules disappear first
            long elapsed = System.currentTimeMillis() - closeAnimationStartedAt;
            float normalizedY = clamp01(y / Math.max(1.0f, (float) height));
            float textDelay = (1.0f - normalizedY) * 160.0f;
            float textDuration = 140.0f;
            float local = clamp01((elapsed - textDelay) / textDuration);
            return smoothStep(1.0f - local);
        }

        long elapsed = System.currentTimeMillis() - openAnimationStartedAt;
        float normalizedY = clamp01(y / Math.max(1.0f, (float) height));
        float textDelay = 120.0f + normalizedY * 500.0f;
        float textDuration = 220.0f;
        float local = clamp01((elapsed - textDelay) / textDuration);
        return smoothStep(local);
    }

    private float getModuleEnabledFillRevealInternal(float x, float y, float elementHeight) {
        if (!shouldAnimate()) {
            return 1.0f;
        }

        if (closing) {
            long elapsed = System.currentTimeMillis() - closeAnimationStartedAt;
            float normalizedY = clamp01(y / Math.max(1.0f, (float) height));
            float fillDelay = (1.0f - normalizedY) * 180.0f;
            float fillDuration = 120.0f;
            float local = clamp01((elapsed - fillDelay) / fillDuration);
            return easeOutCubic(1.0f - local);
        }
        
        long elapsed = System.currentTimeMillis() - openAnimationStartedAt;
        float normalizedY = clamp01(y / Math.max(1.0f, (float) height));
        float fillDelay = 200.0f + normalizedY * 600.0f;
        float fillDuration = 180.0f;
        float local = clamp01((elapsed - fillDelay) / fillDuration);
        return easeOutCubic(local);
    }

    private float getFanDownLinearProgressForY(float y) {
        float normalizedY = clamp01(y / Math.max(1.0f, (float) height));
        if (!closing) {
            long elapsed = System.currentTimeMillis() - openAnimationStartedAt;
            float delay = normalizedY * FRAME_VERTICAL_STAGGER_MS;
            float duration = Math.max(1.0f, OPEN_ANIMATION_TIME_MS - delay);
            return clamp01((elapsed - delay) / duration);
        }

        float closeP = getCloseProgress();
        float delay = (1.0f - normalizedY) * (FRAME_VERTICAL_STAGGER_MS * 0.6f);
        float totalCloseMs = CLOSE_ANIMATION_TIME_MS;
        float duration = Math.max(1.0f, totalCloseMs - delay);
        float local = clamp01((closeP * totalCloseMs - delay) / duration);
        return 1.0f - local;
    }

    private float getFanDownProgressForY(float y) {
        return easeOutCubic(getFanDownLinearProgressForY(y));
    }

    private void closeNow() {
        if (closingNow) {
            return;
        }

        closingNow = true;
        super.close();
        getFrames().forEach(frame -> {
            for (Component comp : frame.getComponents()) {
                if (comp instanceof ModuleComponent moduleComponent) {
                    for (Component component : moduleComponent.getComponents()) {
                        if (component instanceof KeybindComponent keybindComponent) {
                            keybindComponent.setBinding(false);
                        }
                        if (component instanceof StringComponent stringComponent) {
                            stringComponent.setListening(false);
                        }
                    }
                }
            }
        });
        CLICK_GUI.disable();
        closingNow = false;
    }

    public void onGuiOpened() {
        getFrames().forEach(frame -> {
            for (Component comp : frame.getComponents()) {
                if (comp instanceof ModuleComponent moduleComponent) {
                    for (Component component : moduleComponent.getComponents()) {
                        if (component instanceof ColorComponent colorComponent) {
                            float[] hsb = Color.RGBtoHSB(colorComponent.getColorSetting().getRed(), colorComponent.getColorSetting().getGreen(), colorComponent.getColorSetting().getBlue(), null);
                            colorComponent.setHue(hsb[0]);
                            colorComponent.setSaturation(hsb[1]);
                            colorComponent.setBrightness(hsb[2]);
                            colorComponent.setAlpha(colorComponent.getColorSetting().getAlpha() / 255.f);
                        }
                    }
                }
            }
        });
    }

    public ArrayList<Frame> getFrames() {
        return frames;
    }

    public static float getRenderOffsetX() {
        return renderOffsetX;
    }

    public static float getRenderOffsetY() {
        return renderOffsetY;
    }

    public static float getRenderFrameAlpha() {
        return renderFrameAlpha;
    }

    public void setPingBypass(boolean pingBypass) {
        this.pingBypass = pingBypass;
    }

    public void setAddDescriptionFrame(boolean addDescriptionFrame) {
        this.addDescriptionFrame = addDescriptionFrame;
    }

    public void setCategories(Category[] categories) {
        this.categories = categories;
    }

}
