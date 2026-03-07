package me.earth.earthhack.impl.modules.player.speedmine;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.player.speedmine.mode.ESPMode;
import me.earth.earthhack.impl.modules.player.speedmine.mode.MineMode;
import me.earth.earthhack.impl.util.minecraft.PlayerUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import net.minecraft.util.math.Box;

final class ListenerRender extends ModuleListener<Speedmine, Render3DEvent>
{
    public ListenerRender(Speedmine module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (!PlayerUtil.isCreative(mc.player)
                && module.esp.getValue() != ESPMode.None
                && module.bb != null)
        {
            if ((module.getMode() == MineMode.Instant || module.getMode() == MineMode.Fast)
                    && !module.renderOnAir.getValue() && mc.world.getBlockState(module.pos).isAir()) {
                return;
            }

            event.getStack().push();

            float prev = Math.min(module.prevMaxDamage, 1.0f);
            float curr = Math.min(module.maxDamage, 1.0f);
            float max = prev + (curr - prev) * event.getDelta();
            Box renderBB = module.bb;
            if (module.growRender.getValue() && max < 1.0f)
            {
                float eased = module.growCurve.getValue().apply(max);
                renderBB = renderBB.expand(-0.5 + (eased / 2.0));
            }

            Box bb = Interpolation.interpolateAxis(renderBB);
            module.esp.getValue().drawEsp(event.getStack(), module, bb.offset(module.getPos()), max);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            event.getStack().pop();
        }
    }
}
