package me.earth.earthhack.impl.modules.render.trails;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;

final class ListenerRender extends ModuleListener<Trails, Render3DEvent>
{
    public ListenerRender(Trails module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        Iterator<Map.Entry<Integer, TrailState>> iterator = module.trails.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer, TrailState> entry = iterator.next();
            TrailState state = entry.getValue();
            if (state.getPoints().size() < 2)
            {
                continue;
            }

            state.getFade().add(event.getDelta());
            int alpha = Math.max(0, module.color.getValue().getAlpha() - (int) state.getFade().getCurrent());
            if (alpha <= 0)
            {
                iterator.remove();
                continue;
            }

            Color base = module.color.getValue();
            Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            drawTrail(event, state, color);
        }
    }

    private void drawTrail(Render3DEvent event, TrailState state, Color color)
    {
        Matrix4f positionMatrix = event.getStack().peek().getPositionMatrix();
        float alpha = color.getAlpha() / 255.0f;
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;

        RenderUtil.startRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(module.width.getValue());
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (Vec3d point : state.getPoints())
        {
            buffer.vertex(positionMatrix,
                    (float) (point.x - Interpolation.getRenderPosX()),
                    (float) (point.y - Interpolation.getRenderPosY()),
                    (float) (point.z - Interpolation.getRenderPosZ()))
                    .color(red, green, blue, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
        RenderUtil.endRender();
    }
}
