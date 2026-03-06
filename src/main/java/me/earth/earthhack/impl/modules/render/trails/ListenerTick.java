package me.earth.earthhack.impl.modules.render.trails;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class ListenerTick extends ModuleListener<Trails, TickEvent>
{
    public ListenerTick(Trails module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (!event.isSafe() || mc.world == null || mc.player == null)
        {
            return;
        }

        Set<Integer> seen = new HashSet<>();
        for (Entity entity : Managers.ENTITIES.getEntities())
        {
            if (!module.shouldTrack(entity))
            {
                continue;
            }

            int id = entity.getId();
            seen.add(id);
            TrailState state = module.trails.computeIfAbsent(id,
                    ignored -> new TrailState(module.time.getValue(), module.color.getValue().getAlpha()));
            Vec3d pos = entity.getPos();
            var points = state.getPoints();
            if (points.isEmpty() || !points.get(points.size() - 1).equals(pos))
            {
                points.add(pos);
            }

            if (shouldFade(entity))
            {
                state.startFade();
            }
        }

        Iterator<Map.Entry<Integer, TrailState>> iterator = module.trails.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer, TrailState> entry = iterator.next();
            if (!seen.contains(entry.getKey()))
            {
                entry.getValue().startFade();
            }

            if (entry.getValue().getPoints().size() < 2 && entry.getValue().isFading())
            {
                iterator.remove();
            }
        }
    }

    private boolean shouldFade(Entity entity)
    {
        return entity instanceof ArrowEntity
                && (entity.isOnGround()
                || entity.horizontalCollision
                || entity.verticalCollision
                || entity.getVelocity().lengthSquared() < 1.0E-4);
    }
}
