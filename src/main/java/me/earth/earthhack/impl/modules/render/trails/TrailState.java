package me.earth.earthhack.impl.modules.render.trails;

import me.earth.earthhack.impl.util.animation.AnimationMode;
import me.earth.earthhack.impl.util.animation.TimeAnimation;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

final class TrailState
{
    private final TimeAnimation fade;
    private final List<Vec3d> points = new ArrayList<>();
    private boolean fading;

    TrailState(int timeSeconds, int alpha)
    {
        this.fade = new TimeAnimation(timeSeconds * 1000L, 0, alpha, false, AnimationMode.LINEAR);
        this.fade.stop();
    }

    public List<Vec3d> getPoints()
    {
        return points;
    }

    public TimeAnimation getFade()
    {
        return fade;
    }

    public boolean isFading()
    {
        return fading;
    }

    public void startFade()
    {
        if (!fading)
        {
            fading = true;
            fade.play();
        }
    }
}
