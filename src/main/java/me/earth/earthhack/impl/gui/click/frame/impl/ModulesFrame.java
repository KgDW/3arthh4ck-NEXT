package me.earth.earthhack.impl.gui.click.frame.impl;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.gui.click.Click;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent;
import me.earth.earthhack.impl.gui.click.frame.Frame;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class ModulesFrame extends Frame {
    private static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    public ModulesFrame(String name, float posX, float posY, float width, float height) {
        super(name, posX, posY, width, height);
        this.setExtended(true);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        updatePositions();
        final float scrollMaxHeight = mc.getWindow().getScaledHeight();
        final float frameReveal = Click.getModuleOpenReveal(getPosX(), getPosY() + getHeight(), getCurrentHeight());
        if (frameReveal <= 0.001f) {
            return;
        }

        if (CLICK_GUI.get().catEars.getValue()) {
            CategoryFrame.catEarsRender(context, getPosX(), getPosY(), getWidth());
        }

        int topBgColor = multiplyAlpha(CLICK_GUI.get().getTopBgColor().getRGB(), frameReveal);
        int topBorderColor = multiplyAlpha(CLICK_GUI.get().getTopColor().getRGB(), frameReveal);
        int textColor = Managers.TEXT.usingCustomFont()
                ? multiplyAlpha(0xFFFFFFFF, frameReveal) : 0xFFFFFFFF;
        int bodyBgColor = multiplyAlpha(0x92000000, frameReveal);

        if (isExtended()) {
            if (getScrollTargetY() > 0) setScrollTargetY(0);
            if (getScrollCurrentHeight() > scrollMaxHeight) {
                if (getScrollTargetY() - 6 < -(getScrollCurrentHeight() - scrollMaxHeight))
                    setScrollTargetY(-(getScrollCurrentHeight() - scrollMaxHeight));
            } else if (getScrollTargetY() < 0) setScrollTargetY(0);
            updateScrollAnimation();

            float revealedHeight = getCurrentHeight() * frameReveal;
            if (revealedHeight > 0.001f) {
                float bodyTop = getPosY() + getHeight();
                float bodyBottom = bodyTop + 1 + revealedHeight;
                Render2DUtil.drawRect(context.getMatrices(), getPosX(), bodyTop, getPosX() + getWidth(), bodyBottom, bodyBgColor);
                Render2DUtil.scissor(getPosX(), bodyTop, getPosX() + getWidth(), bodyBottom);
                for (Component component : getComponents()) {
                    float componentBottom = component.getFinishedY() + component.getHeight();
                    if (componentBottom <= bodyBottom + 0.5f) {
                        component.drawScreen(context, mouseX, mouseY, partialTicks);
                    }
                }
                Render2DUtil.disableScissor();
            }
        }

        // Draw the category header last so overflowing child content gets covered.
        Render2DUtil.drawRect(context.getMatrices(), getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), topBgColor);
        if (CLICK_GUI.get().getBoxes())
            Render2DUtil.drawBorderedRect(context.getMatrices(), getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), 0.5f, 0, topBorderColor);
        drawStringWithShadow(context, getLabel(), getPosX() + 3, getPosY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1), textColor);
        if (CLICK_GUI.get().size.getValue()) {
            String disString = "[" + getComponents().size() + "]";
            drawStringWithShadow(context, disString, (getPosX() + getWidth() - 3 - Managers.TEXT.getStringWidth(disString)), (getPosY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1)), textColor);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        super.mouseScrolled(mouseX, mouseY, scrollAmount);
        if (isExtended()) {
            final float scrollMaxHeight = mc.getWindow().getScaledHeight();
            if (Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY() + getHeight(), getWidth(), (Math.min(getScrollCurrentHeight(), scrollMaxHeight)) + 1) && getScrollCurrentHeight() > scrollMaxHeight) {
                final float scrollSpeed =(CLICK_GUI.get().scrollSpeed.getValue() >> 2);
                if (scrollAmount < 0) {
                    float minScroll = -(getScrollCurrentHeight() - Math.min(getScrollCurrentHeight(), scrollMaxHeight));
                    if (getScrollTargetY() - scrollSpeed < minScroll)
                        setScrollTargetY(minScroll);
                    else setScrollTargetY(getScrollTargetY() - scrollSpeed);
                } else if (scrollAmount > 0) {
                    setScrollTargetY(getScrollTargetY() + scrollSpeed);
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final float scrollMaxHeight = MinecraftClient.getInstance().getWindow().getScaledHeight() - getHeight();
        if (isExtended() && Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY() + getHeight(), getWidth(), (Math.min(getScrollCurrentHeight(), scrollMaxHeight)) + 1))
            getComponents().forEach(component -> component.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    private void updatePositions() {
        float offsetY = getHeight() + 1;
        for (Component component : getComponents()) {
            component.setOffsetY(offsetY);
            component.moved(getPosX(), getPosY() + getScrollY());
            if (component instanceof ModuleComponent moduleComp) {
                float dp = moduleComp.getDropdownProgress();
                if (dp > 0.001f) {
                    float childrenHeight = 0;
                    for (Component component1 : moduleComp.getComponents()) {
                        if (component1 instanceof SettingComponent
                                && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component1).getSetting())) {
                            childrenHeight += component1.getHeight();
                        }
                    }
                    offsetY += childrenHeight * dp;
                    offsetY += 3.f * dp;
                }
            }
            offsetY += component.getHeight();
        }
    }

    private float getScrollCurrentHeight() {
        return getCurrentHeight() + getHeight() + 3.f;
    }

    private float getCurrentHeight() {
        float cHeight = 1;
        for (Component component : getComponents()) {
            if (component instanceof ModuleComponent moduleComp) {
                float dp = moduleComp.getDropdownProgress();
                if (dp > 0.001f) {
                    float childrenHeight = 0;
                    for (Component component1 : moduleComp.getComponents()) {
                        if (component1 instanceof SettingComponent
                            && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component1).getSetting())) {
                            childrenHeight += component1.getHeight();
                        }
                    }
                    cHeight += childrenHeight * dp;
                    cHeight += 3.f * dp;
                }
            }
            cHeight += component.getHeight();
        }
        return cHeight;
    }

    private static int multiplyAlpha(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * clamp01(factor))));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
