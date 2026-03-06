package me.earth.earthhack.impl.modules.render.crystalscale;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.event.events.network.EntityChunkEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.decoration.EndCrystalEntity;

final class ListenerEntityChunk
        extends ModuleListener<CrystalScale, EntityChunkEvent>
{
    ListenerEntityChunk(CrystalScale module)
    {
        super(module, EntityChunkEvent.class);
    }

    @Override
    public void invoke(EntityChunkEvent event)
    {
        if (!(event.getEntity() instanceof EndCrystalEntity crystal))
        {
            return;
        }

        if (event.getStage() == Stage.PRE)
        {
            module.onCrystalAdded(crystal);
        }
        else if (event.getStage() == Stage.POST)
        {
            module.onCrystalRemoved(crystal);
        }
    }
}
