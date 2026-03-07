package me.earth.earthhack.impl.modules.misc.antiaim;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

public class AntiAim extends Module
{
    protected final Setting<AntiAimMode> mode =
            register(new EnumSetting<>("Mode", AntiAimMode.Spin));
    protected final Setting<Float> hSpeed =
            register(new NumberSetting<>("H-Speed", 10.0f, 0.1f, 180.0f));
    protected final Setting<Float> vSpeed =
            register(new NumberSetting<>("V-Speed", 10.0f, 0.1f, 180.0f));
    protected final Setting<Boolean> strict =
            register(new BooleanSetting("Strict", true));
    protected final Setting<Boolean> sneak =
            register(new BooleanSetting("Sneak", false));
    protected final Setting<Integer> sneakDelay =
            register(new NumberSetting<>("Sneak-Delay", 500, 0, 5000));
    protected final Setting<Float> yaw =
            register(new NumberSetting<>("Yaw", 0.0f, -360.0f, 360.0f));
    protected final Setting<Float> pitch =
            register(new NumberSetting<>("Pitch", 0.0f, -90.0f, 90.0f));
    protected final Setting<Integer> skip =
            register(new NumberSetting<>("Skip", 1, 1, 20));
    protected final Setting<Boolean> flipYaw =
            register(new BooleanSetting("FlipYaw", true));
    protected final Setting<Boolean> flipPitch =
            register(new BooleanSetting("FlipPitch", true));

    protected final Setting<Integer> yawSlices =
            register(new NumberSetting<>("YawSlices", 4, 1, 4));

    protected final Setting<Integer> pitchSlices =
            register(new NumberSetting<>("PitchSlices", 3, 1, 4));

    protected final Setting<Boolean> sliceYaw =
            register(new BooleanSetting("SliceYaw", true));

    protected final Setting<Boolean> slicePitch =
            register(new BooleanSetting("SlicePitch", true));

    protected final StopWatch timer = new StopWatch();
    protected float lastYaw;
    protected float lastPitch;

    public AntiAim()
    {
        super("AntiAim", Category.Misc);
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerInput(this));
        this.setData(new AntiAimData(this));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player != null)
        {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            timer.reset();
        }
    }

    public boolean dontRotate()
    {
        if (!strict.getValue() || mc.player == null)
        {
            return false;
        }

        ItemStack activeItem = mc.player.getActiveItem();
        boolean usingFood = !activeItem.isEmpty()
                && activeItem.getComponents().contains(DataComponentTypes.FOOD);
        boolean attack = mc.options.attackKey.isPressed();
        boolean use = mc.options.useKey.isPressed();

        return mc.mouse.middleButtonClicked || attack || use && !usingFood;
    }
}