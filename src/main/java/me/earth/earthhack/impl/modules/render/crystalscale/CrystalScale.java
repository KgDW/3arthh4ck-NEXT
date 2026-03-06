package me.earth.earthhack.impl.modules.render.crystalscale;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.animation.AnimationMode;
import me.earth.earthhack.impl.util.animation.TimeAnimation;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.decoration.EndCrystalEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalScale extends Module
{
    public final Setting<Float> scale =
            register(new NumberSetting<>("Scale", 1.0f, 0.1f, 2.0f));
    public final Setting<Boolean> animate =
            register(new BooleanSetting("Animate", false));
    public final Setting<Integer> time =
            register(new NumberSetting<>("AnimationTime", 200, 1, 500));
    public final Map<Integer, TimeAnimation> scaleMap =
            new ConcurrentHashMap<>();

    public CrystalScale()
    {
        super("CrystalScale", Category.Render);
        this.listeners.add(new ListenerEntityChunk(this));
        this.listeners.add(new ListenerWorldClient(this));
        this.setData(new SimpleData(this, "Scales end crystal rendering."));
    }

    @Override
    protected void onDisable()
    {
        scaleMap.clear();
    }

    public float getScale(EndCrystalEntity entity)
    {
        if (!animate.getValue())
        {
            return scale.getValue();
        }

        TimeAnimation animation =
                scaleMap.computeIfAbsent(entity.getId(), id -> newAnimation());
        animation.add(0.0f);
        if (!animation.isPlaying())
        {
            scaleMap.remove(entity.getId());
            return scale.getValue();
        }

        return (float) animation.getCurrent();
    }

    void onCrystalAdded(EndCrystalEntity entity)
    {
        if (!animate.getValue())
        {
            scaleMap.remove(entity.getId());
            return;
        }

        scaleMap.put(entity.getId(), newAnimation());
    }

    void onCrystalRemoved(EndCrystalEntity entity)
    {
        scaleMap.remove(entity.getId());
    }

    void clear()
    {
        scaleMap.clear();
    }

    private TimeAnimation newAnimation()
    {
        return new TimeAnimation(time.getValue(),
                                 0.1f,
                                 scale.getValue(),
                                 false,
                                 AnimationMode.LINEAR);
    }
}
