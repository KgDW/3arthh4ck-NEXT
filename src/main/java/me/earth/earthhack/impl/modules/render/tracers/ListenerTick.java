package me.earth.earthhack.impl.modules.render.tracers;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ListenerTick extends ModuleListener<Tracers, TickEvent>
{
    public ListenerTick(Tracers module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (!event.isSafe() || mc.player == null)
        {
            return;
        }

        List<Entity> sorted = new ArrayList<>(Managers.ENTITIES.getEntities());
        try
        {
            sorted.sort(Comparator.comparingDouble(mc.player::squaredDistanceTo));
        }
        catch (IllegalStateException ignored)
        {
            // Entity positions can mutate while the list is being sorted.
        }

        module.sorted = sorted;
    }
}
