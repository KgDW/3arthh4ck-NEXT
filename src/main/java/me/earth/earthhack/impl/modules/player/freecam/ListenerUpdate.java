package me.earth.earthhack.impl.modules.player.freecam;

import me.earth.earthhack.impl.event.events.misc.UpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.minecraft.MovementUtil;

final class ListenerUpdate extends ModuleListener<Freecam, UpdateEvent>
{
    public ListenerUpdate(Freecam module)
    {
        super(module, UpdateEvent.class);
    }

    @Override
    public void invoke(UpdateEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        mc.player.noClip = true;
        mc.player.setSprinting(false);
        mc.player.fallDistance = 0.0f;

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        if (mc.player.input.movementForward != 0.0f
                || mc.player.input.movementSideways != 0.0f)
        {
            double[] dir = MovementUtil.strafe(module.speed.getValue());
            x = dir[0];
            z = dir[1];
        }

        if (mc.player.input.playerInput.jump())
        {
            y += module.speed.getValue();
        }

        if (mc.player.input.playerInput.sneak())
        {
            y -= module.speed.getValue();
        }

        mc.player.setVelocity(x, y, z);
    }
}
