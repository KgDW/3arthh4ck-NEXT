package me.earth.earthhack.impl.modules.render.nametags;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

final class ListenerRender extends ModuleListener<Nametags, Render3DEvent>
{
    public ListenerRender(Nametags module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        Entity renderEntity = RenderUtil.getEntity();
        if (renderEntity == null || mc.player == null || mc.world == null)
        {
            return;
        }

        module.updateNametags();
        Nametag.isRendering = true;
        for (Nametag nametag : module.nametags)
        {
            PlayerEntity player = nametag.player;
            if (!player.isAlive()
                    || player.isInvisible() && !module.invisibles.getValue()
                    || module.withDistance.getValue()
                        && renderEntity.squaredDistanceTo(player)
                           > MathUtil.square(module.distance.getValue())
                    || module.fov.getValue()
                        && !(RotationUtil.inFov(player)
                             || renderEntity.squaredDistanceTo(player) <= 1.0
                                && module.close.getValue()))
            {
                continue;
            }

            renderNametag(event.getStack(), nametag, player, renderEntity);
        }

        Nametag.isRendering = false;
    }

    private void renderNametag(MatrixStack matrices,
                               Nametag nametag,
                               PlayerEntity player,
                               Entity renderEntity)
    {
        double distance = Math.sqrt(renderEntity.squaredDistanceTo(player));
        float scale = 0.0018f + module.scale.getValue() * (float) distance;
        if (distance <= 8.0)
        {
            scale = 0.0245f;
        }

        var pos = Interpolation.interpolateEntityNoRenderPos(player);
        float yOffset = player.isSneaking() ? 0.5f : 0.7f;

        matrices.push();
        matrices.translate(pos.x - Interpolation.getRenderPosX(),
                           pos.y - Interpolation.getRenderPosY() + player.getHeight() + yOffset,
                           pos.z - Interpolation.getRenderPosZ());
        matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
        matrices.scale(-scale, -scale, scale);

        if (module.outlineColor.getValue().getAlpha() > 0)
        {
            renderOutline(matrices.peek().getPositionMatrix(), nametag.nameWidth);
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider.Immediate consumers =
                mc.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer textRenderer = mc.textRenderer;
        float x = -nametag.nameWidth / 2.0f;

        textRenderer.draw(nametag.nameString,
                          x,
                          0.0f,
                          nametag.nameColor,
                          true,
                          matrix,
                          consumers,
                          TextRenderer.TextLayerType.SEE_THROUGH,
                          0,
                          LightmapTextureManager.MAX_LIGHT_COORDINATE);
        textRenderer.draw(nametag.nameString,
                          x,
                          0.0f,
                          nametag.nameColor,
                          true,
                          matrix,
                          consumers,
                          TextRenderer.TextLayerType.NORMAL,
                          0,
                          LightmapTextureManager.MAX_LIGHT_COORDINATE);
        consumers.draw();
        matrices.pop();
    }

    private void renderOutline(Matrix4f matrix, int nameWidth)
    {
        int color = module.outlineColor.getValue().getRGB();
        float halfWidth = nameWidth / 2.0f + 2.0f;
        float top = -2.0f;
        float bottom = MathHelper.ceil(mc.textRenderer.fontHeight) + 1.0f;

        float alpha = (color >> 24 & 0xFF) / 255.0f;
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        RenderUtil.startRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(module.outlineWidth.getValue());
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, -halfWidth, top, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, halfWidth, top, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, halfWidth, bottom, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -halfWidth, bottom, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -halfWidth, top, 0.0f).color(red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
        RenderUtil.endRender();
    }
}
