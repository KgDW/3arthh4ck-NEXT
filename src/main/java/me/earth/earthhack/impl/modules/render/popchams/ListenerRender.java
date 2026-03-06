package me.earth.earthhack.impl.modules.render.popchams;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.core.mixins.render.ILivingEntityRenderer;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;

final class ListenerRender extends ModuleListener<PopChams, Render3DEvent>
{
    private static final float OUTLINE_SCALE = 1.0025f;
    private static final Identifier WHITE_TEXTURE =
            Identifier.ofVanilla("textures/misc/white.png");

    public ListenerRender(PopChams module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        long now = System.currentTimeMillis();
        for (PopData data : module.getPopDataList())
        {
            if (module.fadeTime.getValue() <= 0)
            {
                module.getPopDataList().remove(data);
                continue;
            }

            long passed = now - data.time();
            if (passed >= module.fadeTime.getValue())
            {
                module.getPopDataList().remove(data);
                continue;
            }

            double progress = passed / module.fadeTime.getValue().doubleValue();
            renderPop(data, event, progress);
        }
    }

    private void renderPop(PopData data, Render3DEvent event, double progress)
    {
        if (!(mc.getEntityRenderDispatcher().getRenderer(data.player()) instanceof PlayerEntityRenderer renderer))
        {
            return;
        }

        Color fill = fade(module.getColor(data), progress);
        Color outline = fade(module.getOutlineColor(data), progress);
        Vec3d pos = data.position();
        Vec3d offset = data.positionOffset();

        event.getStack().push();
        event.getStack().translate(pos.x - Interpolation.getRenderPosX() + offset.x,
                                   pos.y - Interpolation.getRenderPosY() + offset.y + module.yAnimations.getValue() * progress,
                                   pos.z - Interpolation.getRenderPosZ() + offset.z);
        ((ILivingEntityRenderer) renderer).earthhack$setupTransforms(data.state(),
                                                                     event.getStack(),
                                                                     data.state().bodyYaw,
                                                                     data.state().baseScale);
        event.getStack().scale(-1.0f, -1.0f, 1.0f);
        ((ILivingEntityRenderer) renderer).earthhack$scale(data.state(), event.getStack());
        event.getStack().translate(0.0f, -1.501f, 0.0f);

        renderModel(renderer, data.state(), event, fill, outline);
        event.getStack().pop();
    }

    private void renderModel(PlayerEntityRenderer renderer,
                             net.minecraft.client.render.entity.state.PlayerEntityRenderState state,
                             Render3DEvent event,
                             Color fill,
                             Color outline)
    {
        VertexConsumerProvider.Immediate consumers =
                mc.getBufferBuilders().getEntityVertexConsumers();

        RenderUtil.startRender();
        renderer.getModel().setAngles(state);
        renderer.getModel().render(event.getStack(),
                                   consumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE)),
                                   LightmapTextureManager.MAX_LIGHT_COORDINATE,
                                   OverlayTexture.DEFAULT_UV,
                                   fill.getRGB());
        consumers.draw();

        if (outline.getAlpha() > 0 && module.lineWidth.getValue() > 0.0f)
        {
            RenderSystem.lineWidth(module.lineWidth.getValue());
            RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            event.getStack().push();
            event.getStack().scale(OUTLINE_SCALE, OUTLINE_SCALE, OUTLINE_SCALE);
            renderer.getModel().render(event.getStack(),
                                       consumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE)),
                                       LightmapTextureManager.MAX_LIGHT_COORDINATE,
                                       OverlayTexture.DEFAULT_UV,
                                       outline.getRGB());
            event.getStack().pop();
            consumers.draw();
            RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            RenderSystem.lineWidth(1.0f);
        }

        RenderUtil.endRender();
    }

    private Color fade(Color color, double progress)
    {
        int alpha = Math.max(0, (int) Math.round(color.getAlpha() * (1.0 - progress)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
