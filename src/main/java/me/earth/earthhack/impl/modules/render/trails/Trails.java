package me.earth.earthhack.impl.modules.render.trails;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Trails extends Module
{
    protected final Setting<Boolean> arrows =
            register(new BooleanSetting("Arrows", false));
    protected final Setting<Boolean> pearls =
            register(new BooleanSetting("Pearls", false));
    protected final Setting<Boolean> snowballs =
            register(new BooleanSetting("Snowballs", false));
    protected final Setting<Integer> time =
            register(new NumberSetting<>("Time", 1, 1, 10));
    protected final ColorSetting color =
            register(new ColorSetting("Color", new Color(255, 0, 0, 255)));
    protected final Setting<Float> width =
            register(new NumberSetting<>("Width", 1.6f, 0.1f, 10.0f));

    protected Map<Integer, TrailState> trails = new ConcurrentHashMap<>();

    public Trails()
    {
        super("Trails", Category.Render);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this, "Renders fading trails behind projectiles."));
    }

    @Override
    protected void onEnable()
    {
        trails = new ConcurrentHashMap<>();
    }

    protected boolean shouldTrack(Entity entity)
    {
        return arrows.getValue() && entity instanceof ArrowEntity
                || pearls.getValue() && entity instanceof EnderPearlEntity
                || snowballs.getValue() && entity instanceof SnowballEntity;
    }
}
