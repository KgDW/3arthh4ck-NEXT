package me.earth.earthhack.impl.modules.render.popchams;

import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.helpers.render.BlockESPModule;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PopChams extends BlockESPModule
{
    protected final Setting<Integer> fadeTime =
            register(new NumberSetting<>("Fade-Time", 1500, 0, 5000));
    protected final Setting<Boolean> selfPop =
            register(new BooleanSetting("Self-Pop", false));
    public final ColorSetting selfColor =
            register(new ColorSetting("Self-Color", new Color(80, 80, 255, 80)));
    public final ColorSetting selfOutline =
            register(new ColorSetting("Self-Outline", new Color(80, 80, 255, 255)));
    public final BooleanSetting copyAnimations =
            register(new BooleanSetting("Copy-Animations", true));
    public final NumberSetting<Double> yAnimations =
            register(new NumberSetting<>("Y-Animation", 0.0, -7.0, 7.0));
    protected final Setting<Boolean> friendPop =
            register(new BooleanSetting("Friend-Pop", false));
    public final ColorSetting friendColor =
            register(new ColorSetting("Friend-Color", new Color(45, 255, 45, 80)));
    public final ColorSetting friendOutline =
            register(new ColorSetting("Friend-Outline", new Color(45, 255, 45, 255)));
    private final List<PopData> popDataList = new CopyOnWriteArrayList<>();

    public PopChams()
    {
        super("PopChams", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.listeners.add(new ListenerTotemPop(this));
        super.color.setValue(new Color(255, 45, 45, 80));
        super.outline.setValue(new Color(255, 45, 45, 255));
        this.setData(new SimpleData(this, "Renders fading ghosts when players pop a totem."));
        this.unregister(super.height);
    }

    @Override
    protected void onDisable()
    {
        popDataList.clear();
    }

    public List<PopData> getPopDataList()
    {
        return popDataList;
    }

    protected Color getColor(PopData data)
    {
        if (data.self())
        {
            return selfColor.getValue();
        }
        else if (data.friend())
        {
            return friendColor.getValue();
        }
        else
        {
            return color.getValue();
        }
    }

    protected Color getOutlineColor(PopData data)
    {
        if (data.self())
        {
            return selfOutline.getValue();
        }
        else if (data.friend())
        {
            return friendOutline.getValue();
        }
        else
        {
            return outline.getValue();
        }
    }

    protected boolean isValidEntity(PlayerEntity entity)
    {
        return !(entity == mc.player && !selfPop.getValue())
                && !((Managers.FRIENDS.contains(entity) && entity != mc.player) && !friendPop.getValue());
    }
}
