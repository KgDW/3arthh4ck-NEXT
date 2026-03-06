package me.earth.earthhack.impl.modules.movement.velocity;

import me.earth.earthhack.impl.core.mixins.network.server.IExplosionS2CPacket;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

final class ListenerExplosion extends
        ModuleListener<Velocity, PacketEvent.Receive<ExplosionS2CPacket>>
{
    public ListenerExplosion(Velocity module)
    {
        super(module,
                PacketEvent.Receive.class,
                -1000000,
                ExplosionS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<ExplosionS2CPacket> event)
    {
        if (module.explosions.getValue())
        {
            IExplosionS2CPacket explosion = (IExplosionS2CPacket) (Object) event.getPacket();
            Optional<Vec3d> knockback = explosion.getPlayerKnockback();
            if (knockback.isPresent())
            {
                Vec3d vec = knockback.get();
                explosion.setPlayerKnockback(Optional.of(new Vec3d(
                        (vec.x / 100.0) * module.horizontal.getValue(),
                        (vec.y / 100.0) * module.vertical.getValue(),
                        (vec.z / 100.0) * module.horizontal.getValue())));
            }
        }
    }

}
