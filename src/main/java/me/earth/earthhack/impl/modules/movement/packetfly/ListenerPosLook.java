package me.earth.earthhack.impl.modules.movement.packetfly;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.movement.packetfly.util.Mode;
import me.earth.earthhack.impl.modules.movement.packetfly.util.TimeVec;
import me.earth.earthhack.impl.util.network.PacketUtil;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

final class ListenerPosLook extends
        ModuleListener<PacketFly, PacketEvent.Receive<PlayerPositionLookS2CPacket>>
{
    public ListenerPosLook(PacketFly module)
    {
        super(module, PacketEvent.Receive.class, PlayerPositionLookS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<PlayerPositionLookS2CPacket> event)
    {
        if (module.mode.getValue() == Mode.Compatibility)
        {
            return;
        }

        PlayerPositionLookS2CPacket packet = event.getPacket();
        double x = packet.change().position().x;
        double y = packet.change().position().y;
        double z = packet.change().position().z;

        if (mc.player.isAlive()
                && module.mode.getValue() != Mode.Setback
                && module.mode.getValue() != Mode.Slow
                && !(mc.currentScreen instanceof DownloadingTerrainScreen)
                && mc.world.isPosLoaded((int) mc.player.getX(), (int) mc.player.getZ())) // hmm
        {
            TimeVec vec = module.posLooks.remove(packet.teleportId());
            if (vec != null
                    && vec.x == x
                    && vec.y == y
                    && vec.z == z)
            {
                event.setCancelled(true);
                return;
            }
        }

        module.teleportID.set(packet.teleportId());

        if (module.answer.getValue())
        {
            event.setCancelled(true);
            mc.execute(() ->
                    PacketUtil.handlePosLook(event.getPacket(),
                                             mc.player,
                                             true,
                                             false));
            return;
        }

        event.setCancelled(true);
        mc.execute(() ->
                PacketUtil.handlePosLook(packet,
                                         mc.player,
                                         true,
                                         false));
    }

}
