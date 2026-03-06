package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.math.BlockPos;

final class ListenerTick extends ModuleListener<AutoCrystal, TickEvent>
{
    public ListenerTick(AutoCrystal module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (event.isSafe())
        {
            module.debug("tick-safe", "Tick safe event active.");
            module.checkExecutor();
            module.placed.values().removeIf(stamp ->
                System.currentTimeMillis() - stamp.getTimeStamp()
                        > module.removeTime.getValue());

            module.crystalRender.tick();
            if (!module.idHelper.isUpdated())
            {
                module.idHelper.update();
                module.idHelper.setUpdated(true);
            }

            module.weaknessHelper.updateWeakness();
            if (!module.multiThread.getValue()
                && module.motionThread.getValue()
                && module.shouldUseTickFallback())
            {
                module.debug("tick-fallback",
                        "No MotionUpdateEvent recently, running Tick fallback calculation.");
                module.threadHelper.startThread();
            }

            render();
        }
    }

    private void render()
    {
        BlockPos pos;
        if (module.render.getValue()
            // && PingBypass.isConnected()
            && (pos = module.getRenderPos()) != null)
        {
            // PingBypass.sendPacket(new S2CRenderPacket(
            //     pos, module.outLine.getValue(), module.boxColor.getValue()));
        }
    }

}
