package me.earth.earthhack.impl.modules.player.freecam;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.player.freecam.mode.CamMode;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

final class ListenerPacket extends ModuleListener<Freecam, PacketEvent.Send<?>>
{
    public ListenerPacket(Freecam module)
    {
        super(module, PacketEvent.Send.class);
    }

    @Override
    public void invoke(PacketEvent.Send<?> event)
    {
        switch (module.getMode())
        {
            case Cancel -> {
                if (event.getPacket() instanceof PlayerMoveC2SPacket)
                {
                    event.setCancelled(true);
                }
            }
            case Spanish -> {
                Packet<?> packet = event.getPacket();
                if (!(packet instanceof PlayerInteractEntityC2SPacket
                        || packet instanceof PlayerInteractItemC2SPacket
                        || packet instanceof PlayerInteractBlockC2SPacket
                        || packet instanceof PlayerMoveC2SPacket
                        || packet instanceof VehicleMoveC2SPacket
                        || packet instanceof ChatMessageC2SPacket
                        || packet instanceof KeepAliveC2SPacket
                        || packet instanceof CommonPongC2SPacket
                        || packet instanceof TeleportConfirmC2SPacket
                        || packet instanceof ClientStatusC2SPacket))
                {
                    event.setCancelled(true);
                }
            }
            case Position -> {
            }
        }
    }
}
