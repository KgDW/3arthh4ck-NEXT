package me.earth.earthhack.impl.modules.render.nametags;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

final class ListenerRender2D extends ModuleListener<Nametags, Render2DEvent>
{
    public ListenerRender2D(Nametags module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        Entity renderEntity = RenderUtil.getEntity();
        if (mc.player == null
                || mc.world == null
                || renderEntity == null
                || !module.twoD.getValue())
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

            renderNametag(event.getContext(),
                    event.getTickDelta(),
                    nametag,
                    player,
                    renderEntity);
        }

        Nametag.isRendering = false;
    }

    private void renderNametag(DrawContext context,
                               float tickDelta,
                               Nametag nametag,
                               PlayerEntity player,
                               Entity renderEntity)
    {
        Vec3d labelOffset = player.getAttachments()
                .getPointNullable(EntityAttachmentType.NAME_TAG,
                        0,
                        player.getLerpedYaw(tickDelta));
        if (labelOffset == null)
        {
            return;
        }

        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX())
                - Interpolation.getRenderPosX()
                + labelOffset.x;
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY())
                - Interpolation.getRenderPosY()
                + labelOffset.y
                + 0.5;
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ())
                - Interpolation.getRenderPosZ()
                + labelOffset.z;

        Vector4f projected = RenderUtil.projectToScreen(x, y, z);
        if (projected == null)
        {
            return;
        }

        float textScale = module.getScreenScale();
        float textWidth = nametag.nameWidth * textScale;
        float xPos = projected.x - textWidth / 2.0f;
        float yPos = projected.y - mc.textRenderer.fontHeight * textScale;
        int bgPadding = Math.max(2, Math.round(2.0f * textScale));
        float scaledHeight = mc.textRenderer.fontHeight * textScale;
        Render2DUtil.drawRect(context.getMatrices(),
                xPos - bgPadding,
                yPos - bgPadding,
                xPos + textWidth + bgPadding,
                yPos + scaledHeight + bgPadding,
                0x55000000);
        context.getMatrices().push();
        context.getMatrices().scale(textScale, textScale, 1.0f);
        context.drawText(mc.textRenderer,
                nametag.nameString,
                (int) (xPos / textScale),
                (int) (yPos / textScale),
                nametag.nameColor,
                true);
        context.getMatrices().pop();
    }
}
