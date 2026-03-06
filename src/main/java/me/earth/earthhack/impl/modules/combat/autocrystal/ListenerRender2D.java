package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.RenderDamage;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.RenderDamagePos;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;

final class ListenerRender2D extends ModuleListener<AutoCrystal, Render2DEvent>
{
    public ListenerRender2D(AutoCrystal module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (!module.render.getValue()
                || module.isPingBypass()
                || module.renderDamage.getValue() == RenderDamagePos.None
                || module.damage == null
                || module.damage.isEmpty()
                || mc.player == null
                || mc.world == null)
        {
            return;
        }

        BlockPos pos = module.getRenderPos();
        if (pos == null)
        {
            return;
        }

        RenderPos renderPos = getRenderPos(pos);
        Vector4f projected = RenderUtil.projectToScreen(
                renderPos.x - Interpolation.getRenderPosX(),
                renderPos.y - Interpolation.getRenderPosY(),
                renderPos.z - Interpolation.getRenderPosZ());
        if (projected == null)
        {
            return;
        }

        renderDamage(event.getContext(), renderPos, projected);
    }

    private RenderPos getRenderPos(BlockPos pos)
    {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        if (module.slide.getValue()
                && module.slidePos != null
                && !module.slidePos.equals(pos))
        {
            double rawFactor = Math.min(1.0,
                    module.slideTimer.getTime() / Math.max(1.0, module.slideTime.getValue()));
            double factor;
            if (module.smoothSlide.getValue())
            {
                double t = 1.0 - rawFactor;
                factor = 1.0 - t * t * t;
            }
            else
            {
                factor = rawFactor;
            }

            if (factor < 1.0)
            {
                x = module.slideX + (pos.getX() - module.slideX) * factor + 0.5;
                y = module.slideY + (pos.getY() - module.slideY) * factor;
                z = module.slideZ + (pos.getZ() - module.slideZ) * factor + 0.5;
            }
        }

        y += module.renderDamage.getValue() == RenderDamagePos.OnTop ? 1.35 : 0.5;
        return new RenderPos(x, y, z);
    }

    private void renderDamage(DrawContext context, RenderPos renderPos, Vector4f projected)
    {
        String text = module.damage;
        RenderDamage mode = module.renderMode.getValue();
        float distance = (float) Math.sqrt(mc.player.squaredDistanceTo(renderPos.x, renderPos.y, renderPos.z));
        float scale = MathHelper.clamp(1.38f - distance * 0.035f, 0.92f, 1.38f);
        int textWidth = Managers.TEXT.getStringWidth(text);
        int textHeight = Managers.TEXT.getStringHeightI();
        float scaledWidth = textWidth * scale;
        float scaledHeight = textHeight * scale;
        float x = projected.x - scaledWidth / 2.0f;
        float y = projected.y - scaledHeight / 2.0f;
        int color = mode == RenderDamage.Indicator
                ? module.indicatorColor.getValue().getRGB()
                : Color.WHITE.getRGB();

        Managers.TEXT.drawStringScaled(
                context,
                text,
                x,
                y,
                color,
                true,
                scale);
    }

    private record RenderPos(double x, double y, double z) { }
}
