package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.data.ModuleData;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.*;
import me.earth.earthhack.impl.gui.chat.factory.ComponentFactory;
import me.earth.earthhack.impl.gui.click.Click;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.modules.client.configs.ConfigHelperModule;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import me.earth.earthhack.impl.util.text.TextColor;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModuleComponent extends Component {
    private static final long DROPDOWN_OPEN_DURATION_MS = 300L;
    private static final long DROPDOWN_CLOSE_DURATION_MS = 220L;
    private static final long TOGGLE_ON_DURATION_MS = 240L;
    private static final long TOGGLE_OFF_DURATION_MS = 180L;

    private final Module module;
    private final float height;
    private final ArrayList<Component> components = new ArrayList<>();

    private long dropdownToggleTime = 0;
    private boolean dropdownOpening = false;
    private float dropdownProgress = 0.0f;
    private float dropdownAnimStart = 0.0f;
    private long enabledToggleTime = 0L;
    private boolean enabledAnimatingOn = false;
    private float enabledProgress;
    private float enabledAnimStart;
    private boolean lastEnabledState;

    public ModuleComponent(Module module, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(module.getName(), posX, posY, offsetX, offsetY, width, height);
        this.height = height;
        this.module = module;
        this.lastEnabledState = module.isEnabled();
        this.enabledProgress = this.lastEnabledState ? 1.0f : 0.0f;
        this.enabledAnimStart = this.enabledProgress;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        getComponents().clear();
        float offY = getHeight();
        ModuleData<?> data = getModule().getData();
        if (data != null) {
            this.setDescription(data::getDescription);
        }

        if (!getModule().getSettings().isEmpty()) {
            for (Setting<?> setting : getModule().getSettings()) {
                if (!setting.getVisibility()) continue;
                float before = offY;
                if (setting instanceof BooleanSetting && !setting.getName().equalsIgnoreCase("enabled")) {
                    getComponents().add(new BooleanComponent((BooleanSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof BindSetting) {
                    getComponents().add(new KeybindComponent((BindSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof NumberSetting) {
                    getComponents().add(new NumberComponent((NumberSetting<Number>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof EnumSetting) {
                    getComponents().add(new EnumComponent<>((EnumSetting<?>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof ColorSetting) {
                    getComponents().add(new ColorComponent((ColorSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof StringSetting) {
                    getComponents().add(new StringComponent((StringSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                if (setting instanceof ListSetting) {
                    getComponents().add(new ListComponent<>((ListSetting<?>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }

                // -_- lazy
                if (data != null && before != offY) {
                    Supplier<String> supplier = () -> {
                        String desc = data.settingDescriptions().get(setting);
                        if (desc == null) {
                            desc = "A Setting (" + setting.getInitial().getClass().getSimpleName() + ").";
                        }

                        if (Click.CLICK_GUI.get().descNameValue.getValue()) {
                            desc = ComponentFactory.create(setting).getText() + "\n\n" + TextColor.WHITE + desc;
                        }

                        return desc;
                    };

                    getComponents().get(getComponents().size() - 1).setDescription(supplier);
                }
            }
        }
        getComponents().forEach(Component::init);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
        getComponents().forEach(component -> component.moved(getFinishedX(), getFinishedY()));
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        if (!module.searchVisibility) {
            setHeight(0);
            if (isExtended())
                setExtended(false);
            dropdownProgress = 0.0f;
            return;
        } else {
            setHeight(height);
        }

        updateDropdownProgress();
        updateEnabledProgress();
        updatePositions();

        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        final float openReveal = Click.getModuleOpenReveal(getFinishedX(), getFinishedY(), getHeight());
        if (openReveal <= 0.001f) {
            return;
        }
        final float toggleReveal = enabledProgress;
        final float enabledReveal = toggleReveal * Click.getModuleEnabledFillReveal(getFinishedX(), getFinishedY(), getHeight());
        final float textAlpha = Managers.TEXT.usingCustomFont() ? openReveal : 1.0f;
        final int onTextColor = getClickGui().get().getOnModule().brighter().getRGB();
        final int offTextColor = getClickGui().get().getOffModule().brighter().getRGB();
        final int moduleTextBaseColor = blendColor(offTextColor, onTextColor, toggleReveal);
        final int moduleTextColor = multiplyAlpha(moduleTextBaseColor, textAlpha);
        if (hovered)
            Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + getWidth() - 1, getFinishedY() + getHeight() - 0.5f, multiplyAlpha(getClickGui().get().getModuleHover().brighter().getRGB(), openReveal));
        if (enabledReveal > 0.001f) {
            float easedFill = (float) Math.pow(enabledReveal, 1.08f);
            float fillRight = getFinishedX() + 1 + (getWidth() - 2) * easedFill;
            int fillColor = hovered ? getClickGui().get().getModulesColor().brighter().getRGB() : getClickGui().get().getModulesColor().getRGB();
            fillColor = multiplyAlpha(fillColor, openReveal * (float) Math.pow(enabledReveal, 1.35f));
            Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 1, getFinishedY() + 0.5f, fillRight, getFinishedY() + getHeight() - 0.5f, fillColor);
        }

        String label = module instanceof ConfigHelperModule && ((ConfigHelperModule) module).isDeleted() ? TextColor.RED + getLabel() : getLabel();
        drawStringWithShadow(label, getFinishedX() + 4, getFinishedY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1), moduleTextColor);
        if (!getComponents().isEmpty())
            drawStringWithShadow(isExtended() ? getClickGui().get().close.getValue() : getClickGui().get().open.getValue(), getFinishedX() + getWidth() - 4 - Managers.TEXT.getStringWidth(isExtended() ? getClickGui().get().close.getValue() : getClickGui().get().open.getValue()), getFinishedY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1), moduleTextColor);

        if (getClickGui().get().showBind.getValue() && !getModule().getBind().toString().equalsIgnoreCase("none")) {
            String moduleBinding = getModule().getBind().toString().toLowerCase().replace("none", "-");
            moduleBinding = String.valueOf(moduleBinding.charAt(0)).toUpperCase() + moduleBinding.substring(1);
            if (moduleBinding.length() > 3) {
                moduleBinding = moduleBinding.substring(0, 3);
            }
            moduleBinding = "[" + moduleBinding + "]";
            float offset = getFinishedX() + getWidth() - Managers.TEXT.getStringWidth(isExtended() ? getClickGui().get().close.getValue() : getClickGui().get().open.getValue()) * 2;
            Managers.TEXT.drawStringScaled(context, moduleBinding, offset - (Managers.TEXT.getStringWidth(moduleBinding) >> 1), (getFinishedY() + getHeight() / 1.5f - (Managers.TEXT.getStringHeightI() >> 1)), moduleTextColor, true, 0.5f);
        }

        final float dp = dropdownProgress;
        if (dp > 0.001f) {
            final float fullSize = getComponentsSize();
            final float animatedSize = fullSize * dp;
            final float dropTop = getFinishedY() + getHeight();
            final float dropBottom = dropTop + animatedSize;

            for (Component component : getComponents()) {
                if (component instanceof SettingComponent
                    && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    if (component.getFinishedY() + component.getHeight() <= dropBottom + 0.5f) {
                        component.drawScreen(context, mouseX, mouseY, partialTicks);
                    }
                }
            }
            if (enabledReveal > 0.001f) {
                int accentColor = hovered ? getClickGui().get().getModulesColor().brighter().getRGB() : getClickGui().get().getModulesColor().getRGB();
                accentColor = multiplyAlpha(accentColor, openReveal * (float) Math.pow(enabledReveal, 1.35f));
                Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 1.0f, dropTop - 0.5f, getFinishedX() + 3, dropBottom, accentColor);
                Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 1.0f, dropBottom, getFinishedX() + 1.0f + (getWidth() - 2.0f) * enabledReveal, dropBottom + 2, accentColor);
                if (enabledReveal > 0.92f) {
                    Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + getWidth() - 3.f, dropTop - 0.5f, getFinishedX() + getWidth() - 1.f, dropBottom, accentColor);
                }
            }
            if (getClickGui().get().moduleBox.getValue() == ClickGui.ModuleBox.Old)
                Render2DUtil.drawBorderedRect(context.getMatrices(), getFinishedX() + 3.0f, dropTop - 0.5f, getFinishedX() + getWidth() - 3.f, dropBottom + 0.5f, 0.5f, 0, multiplyAlpha(0xff000000, openReveal));
            else if (getClickGui().get().moduleBox.getValue() == ClickGui.ModuleBox.New)
                Render2DUtil.drawBorderedRect(context.getMatrices(), getFinishedX() + 3.0f, dropTop - 0.5f, getFinishedX() + getWidth() - 2.5f, dropBottom, 0.5f, 0, multiplyAlpha(0xff000000, openReveal));
        }
        if (getClickGui().get().getBoxes())
            Render2DUtil.drawBorderedRect(context.getMatrices(), getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + 1 + getWidth() - 2, getFinishedY() - 0.5f + getHeight() + (dp > 0.001f ? (getComponentsSize() * dp + 3.0f) : 0), 0.5f, 0, multiplyAlpha(0xff000000, openReveal));
    }


    @Override
    public void charTyped(char character, int keyCode) {
        super.charTyped(character, keyCode);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent
                    && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.charTyped(character, keyCode);
                }
            }
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent
                        && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.keyPressed(keyCode);
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered) {
            switch (mouseButton) {
                case 0 -> {
                    getModule().toggle();
                    syncEnabledAnimationState(getModule().isEnabled());
                }
                case 1 -> {
                    if (!getComponents().isEmpty()) {
                        dropdownAnimStart = dropdownProgress;
                        dropdownToggleTime = System.currentTimeMillis();
                        dropdownOpening = !isExtended();
                        setExtended(!isExtended());
                    }
                }
                default -> {
                }
            }
        }
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent
                        && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent
                        && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.mouseReleased(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    private void updateEnabledProgress() {
        boolean enabled = getModule().isEnabled();
        if (!getClickGui().get().animations.getValue()) {
            enabledProgress = enabled ? 1.0f : 0.0f;
            enabledAnimStart = enabledProgress;
            enabledToggleTime = 0L;
            lastEnabledState = enabled;
            return;
        }

        if (enabled != lastEnabledState) {
            syncEnabledAnimationState(enabled);
        }

        if (enabledToggleTime == 0L) {
            enabledProgress = enabled ? 1.0f : 0.0f;
            return;
        }

        long elapsed = System.currentTimeMillis() - enabledToggleTime;
        long duration = enabledAnimatingOn ? TOGGLE_ON_DURATION_MS : TOGGLE_OFF_DURATION_MS;
        float raw = Math.min(1.0f, elapsed / (float) duration);
        float eased = enabledAnimatingOn ? easeOutCubic(raw) : easeInCubic(raw);
        float target = enabledAnimatingOn ? 1.0f : 0.0f;
        enabledProgress = enabledAnimStart + (target - enabledAnimStart) * eased;

        if (raw >= 1.0f) {
            enabledProgress = target;
            enabledAnimStart = target;
            enabledToggleTime = 0L;
        }
    }

    private void syncEnabledAnimationState(boolean enabled) {
        enabledAnimStart = enabledProgress;
        enabledToggleTime = System.currentTimeMillis();
        enabledAnimatingOn = enabled;
        lastEnabledState = enabled;
    }

    private void updateDropdownProgress() {
        if (!getClickGui().get().animations.getValue() || dropdownToggleTime == 0) {
            dropdownProgress = isExtended() ? 1.0f : 0.0f;
            return;
        }
        long elapsed = System.currentTimeMillis() - dropdownToggleTime;
        if (dropdownOpening) {
            float raw = Math.min(1.0f, elapsed / (float) DROPDOWN_OPEN_DURATION_MS);
            float eased = easeOutCubic(raw);
            dropdownProgress = dropdownAnimStart + (1.0f - dropdownAnimStart) * eased;
        } else {
            float raw = Math.min(1.0f, elapsed / (float) DROPDOWN_CLOSE_DURATION_MS);
            float eased = easeInCubic(raw);
            dropdownProgress = dropdownAnimStart * (1.0f - eased);
        }
    }

    private float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3.0);
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    public float getDropdownProgress() {
        return dropdownProgress;
    }

    public float getAnimatedComponentsSize() {
        return getComponentsSize() * dropdownProgress;
    }

    private float getComponentsSize() {
        float size = 0;
        for (Component component : getComponents()) {
            if (component instanceof SettingComponent
                    && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                size += component.getHeight();
            }
        }
        return size;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int multiplyAlpha(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * clamp01(factor))));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendColor(int from, int to, float progress) {
        float t = clamp01(progress);
        int a1 = (from >>> 24) & 0xFF;
        int r1 = (from >>> 16) & 0xFF;
        int g1 = (from >>> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >>> 24) & 0xFF;
        int r2 = (to >>> 16) & 0xFF;
        int g2 = (to >>> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updatePositions() {
        float offsetY = getHeight();
        for (Component component : getComponents()) {
            if (component instanceof SettingComponent
                    && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                component.setOffsetY(offsetY);
                component.moved(getFinishedX(), getFinishedY());
                offsetY += component.getHeight();
            }
        }
    }

    public Module getModule() {
        return module;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }
}
