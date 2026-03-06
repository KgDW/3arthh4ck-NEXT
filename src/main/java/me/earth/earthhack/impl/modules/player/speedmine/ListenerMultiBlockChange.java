package me.earth.earthhack.impl.modules.player.speedmine;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.player.speedmine.mode.MineMode;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.BlockPos;

final class ListenerMultiBlockChange extends
        ModuleListener<Speedmine, PacketEvent.Receive<ExplosionS2CPacket>>
{
    public ListenerMultiBlockChange(Speedmine module)
    {
        super(module, PacketEvent.Receive.class, ExplosionS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<ExplosionS2CPacket> event)
    {
        // 1.21.4 explosion packets no longer provide affected block positions.
    }

}
