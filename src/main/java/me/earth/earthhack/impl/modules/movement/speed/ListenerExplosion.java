package me.earth.earthhack.impl.modules.movement.speed;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.minecraft.MovementUtil;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

final class ListenerExplosion extends
        ModuleListener<Speed, PacketEvent.Receive<ExplosionS2CPacket>>
{
    public ListenerExplosion(Speed module)
    {
        super(module, PacketEvent.Receive.class, ExplosionS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<ExplosionS2CPacket> event)
    {
        if (module.explosions.getValue()
                && MovementUtil.isMoving()
                && Managers.NCP.passed(module.lagTime.getValue())
                && mc.player != null)
        {
            ExplosionS2CPacket packet = event.getPacket();
            Vec3d pos = packet.center();

            if (mc.player.squaredDistanceTo(pos) < 100
                    && (!module.directional.getValue()
                    || !MovementUtil.isInMovementDirection(pos.x,
                    pos.y,
                    pos.z)))
            {
                Vec3d knockback = packet.playerKnockback().orElse(Vec3d.ZERO);
                double speed = Math.sqrt(
                        knockback.x * knockback.x
                                + knockback.z * knockback.z);

                module.lastExp = module.expTimer
                        .passed(module.coolDown.getValue())
                        ? speed
                        : (speed - module.lastExp);

                if (module.lastExp > 0)
                {
                    module.expTimer.reset();
                    mc.execute(() ->
                    {
                        module.speed +=
                                module.lastExp * module.multiplier.getValue();

                        module.distance +=
                                module.lastExp * module.multiplier.getValue();

                        if (mc.player.getVelocity().getY() > 0)
                        {
                            mc.player.setVelocity(
                                    mc.player.getVelocity().withAxis(Direction.Axis.Y,
                                            mc.player.getVelocity().getY() * module.vertical.getValue())
                            );
                        }
                    });
                }
            }
        }
    }

}
