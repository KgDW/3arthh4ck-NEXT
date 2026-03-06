package me.earth.earthhack.impl.modules.render.tracers;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.esp.ESP;
import me.earth.earthhack.impl.modules.render.tracers.mode.BodyPart;
import me.earth.earthhack.impl.modules.render.tracers.mode.TracerMode;
import me.earth.earthhack.impl.util.render.ColorHelper;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

final class ListenerRender extends ModuleListener<Tracers, Render3DEvent>
{
    private static final ModuleCache<ESP> ESP = Caches.getModule(ESP.class);

    public ListenerRender(Tracers module)
    {
        super(module, Render3DEvent.class, Integer.MIN_VALUE);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        Entity renderEntity = RenderUtil.getEntity();
        if (renderEntity == null || mc.player == null)
        {
            return;
        }

        int rendered = 0;
        for (Entity entity : module.sorted)
        {
            if (rendered >= module.tracers.getValue())
            {
                break;
            }

            if (!module.isValid(entity))
            {
                continue;
            }

            Vec3d interpolation = Interpolation.interpolateEntity(entity);
            double x = interpolation.x;
            double y = interpolation.y;
            double z = interpolation.z;
            double height = entity.getHeight();

            Box bb = module.target.getValue() == BodyPart.Head
                    ? new Box(x - 0.25, y + height - 0.45, z - 0.25, x + 0.25, y + height + 0.055, z + 0.25)
                    : new Box(x - 0.4, y, z - 0.4, x + 0.4, y + height + 0.18, z + 0.4);

            Color color = getColor(renderEntity, entity);

            if (module.lines.getValue())
            {
                drawLine(event.getStack(), Vec3d.ZERO, getTargetPoint(entity, x, y, z), color, module.lineWidth.getValue());
            }

            if (module.mode.getValue() == TracerMode.Stem && !ESP.isEnabled())
            {
                drawLine(event.getStack(), new Vec3d(x, y, z), new Vec3d(x, y + height, z), color, module.lineWidth.getValue());
            }
            else if (module.mode.getValue() == TracerMode.Outline)
            {
                RenderUtil.drawOutline(event.getStack(), bb, module.lineWidth.getValue(), color);
            }
            else if (module.mode.getValue() == TracerMode.Fill)
            {
                RenderUtil.drawBox(event.getStack(), bb, color);
            }

            rendered++;
        }
    }

    private Color getColor(Entity renderEntity, Entity entity)
    {
        if (entity instanceof PlayerEntity player && Managers.FRIENDS.contains(player))
        {
            return new Color(85, 200, 200, 140);
        }

        float distance = renderEntity.distanceTo(entity);
        float hue = distance >= 60.0f ? 120.0f : distance * 2.0f;
        return ColorHelper.toColor(hue, 100.0f, 50.0f, 0.55f);
    }

    private Vec3d getTargetPoint(Entity entity, double x, double y, double z)
    {
        return switch (module.target.getValue())
        {
            case Head -> new Vec3d(x, y + entity.getHeight() - 0.18, z);
            case Feet -> new Vec3d(x, y, z);
            case Body -> new Vec3d(x, y + entity.getHeight() / 2.0f, z);
        };
    }

    private void drawLine(net.minecraft.client.util.math.MatrixStack matrix,
                          Vec3d from,
                          Vec3d to,
                          Color color,
                          float lineWidth)
    {
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        float alpha = color.getAlpha() / 255.0f;
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;

        RenderUtil.startRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(lineWidth);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        buffer.vertex(positionMatrix, (float) from.x, (float) from.y, (float) from.z).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, (float) to.x, (float) to.y, (float) to.z).color(red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
        RenderUtil.endRender();
    }
}
