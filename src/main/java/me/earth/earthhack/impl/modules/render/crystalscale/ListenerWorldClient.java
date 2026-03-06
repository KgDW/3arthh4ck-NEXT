package me.earth.earthhack.impl.modules.render.crystalscale;

import me.earth.earthhack.impl.event.events.network.WorldClientEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerWorldClient
        extends ModuleListener<CrystalScale, WorldClientEvent.Load>
{
    ListenerWorldClient(CrystalScale module)
    {
        super(module, WorldClientEvent.Load.class);
    }

    @Override
    public void invoke(WorldClientEvent.Load event)
    {
        module.clear();
    }
}
