package me.earth.earthhack.impl.modules.player.freecam;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.network.PacketUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

final class ListenerPosLook extends
        ModuleListener<Freecam, PacketEvent.Receive<PlayerPositionLookS2CPacket>>
{
    public ListenerPosLook(Freecam module)
    {
        super(module, PacketEvent.Receive.class, PlayerPositionLookS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<PlayerPositionLookS2CPacket> event)
    {
        event.setCancelled(true);
        mc.execute(() ->
        {
            if (mc.player == null)
            {
                return;
            }

            PlayerEntity player = module.getPlayer();
            PacketUtil.handlePosLook(event.getPacket(),
                                     player == null ? mc.player : player,
                                     false);

            if (player != null)
            {
                player.setOnGround(true);
            }
        });
    }
}
