package me.earth.earthhack.impl.modules.combat.autocrystal;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.RenderDamage;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.RenderDamagePos;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.MotionTracker;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import me.earth.earthhack.impl.util.render.mutables.MutableBB;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


// TODO: Finish this up
final class ListenerRender extends ModuleListener<AutoCrystal, Render3DEvent> {
    private final Map<BlockPos, Long> fadeList = new HashMap<>();
    private final MutableBB bb = new MutableBB();

    public ListenerRender(AutoCrystal module) {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event) {
        RenderDamagePos mode = module.renderDamage.getValue();
        BlockPos currentPos = module.getRenderPos();

        if (module.render.getValue()
                && module.box.getValue()
                && module.fade.getValue()
                && !module.isPingBypass()) {
            for (Map.Entry<BlockPos, Long> set : fadeList.entrySet()) {
                if (currentPos != null && currentPos.equals(set.getKey())) {
                    continue;
                }

                final Color boxColor = module.boxColor.getValue();
                final Color outlineColor = module.outLine.getValue();
                final float maxBoxAlpha = boxColor.getAlpha();
                final float maxOutlineAlpha = outlineColor.getAlpha();
                final float alphaBoxAmount = maxBoxAlpha / module.fadeTime.getValue();
                final float alphaOutlineAmount = maxOutlineAlpha / module.fadeTime.getValue();
                final int fadeBoxAlpha = MathHelper.clamp((int) (alphaBoxAmount * (set.getValue() + module.fadeTime.getValue() - System.currentTimeMillis())), 0, (int) maxBoxAlpha);
                final int fadeOutlineAlpha = MathHelper.clamp((int) (alphaOutlineAmount * (set.getValue() + module.fadeTime.getValue() - System.currentTimeMillis())), 0, (int) maxOutlineAlpha);

                RenderUtil.renderBox(event.getStack(),
                    Interpolation.interpolatePos(set.getKey(), 1.0f),
                    new Color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), fadeBoxAlpha),
                    new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), fadeOutlineAlpha),
                    1.5f);
            }
        }

        BlockPos pos;
        if (module.render.getValue() && !module.isPingBypass() && (pos = currentPos) != null) {
            double zoomScale = 1.0;
            if (module.zoom.getValue() && module.zoomingIn) {
                double zoomRaw = Math.min(1.0, module.zoomTimer.getTime() / Math.max(1.0, module.zoomTime.getValue()));
                double zt = 1.0 - zoomRaw;
                zoomScale = 1.0 - zt * zt * zt;
            }

            if (module.box.getValue()) {
                BlockPos slide;
                if (module.slide.getValue()
                    && (slide = module.slidePos) != null
                    && !slide.equals(pos)) {
                    double rawFactor = Math.min(1.0, module.slideTimer.getTime() / Math.max(1.0, module.slideTime.getValue()));
                    double factor;
                    if (module.smoothSlide.getValue()) {
                        double t = 1.0 - rawFactor;
                        factor = 1.0 - t * t * t;
                    } else {
                        factor = rawFactor;
                    }
                    if (factor >= 1.0) {
                        renderBoxZoomed(event.getStack(), pos, zoomScale);
                    } else {
                        double x = module.slideX + (pos.getX() - module.slideX) * factor;
                        double y = module.slideY + (pos.getY() - module.slideY) * factor;
                        double z = module.slideZ + (pos.getZ() - module.slideZ) * factor;
                        double half = 0.5 * zoomScale;
                        double cx = x + 0.5;
                        double cy = y + 0.5;
                        double cz = z + 0.5;
                        bb.setBB(
                            cx - half,
                            cy - half,
                            cz - half,
                            cx + half,
                            cy + half,
                            cz + half);
                        Interpolation.interpolateMutable(bb);
                        renderCurrentMutableBox(event.getStack());
                    }
                } else {
                    renderBoxZoomed(event.getStack(), pos, zoomScale);
                }
            }

            if (module.fade.getValue()) {
                fadeList.put(pos, System.currentTimeMillis());
            }

            module.lastZoomPos = pos;
        } else if (module.render.getValue()
                && module.zoom.getValue()
                && !module.zoomingIn
                && module.lastZoomPos != null) {
            double zoomRaw = Math.min(1.0, module.zoomTimer.getTime() / Math.max(1.0, module.zoomTime.getValue()));
            double zt = zoomRaw;
            double zoomScale = 1.0 - zt * zt * zt;
            if (zoomScale > 0.001 && module.box.getValue()) {
                renderBoxZoomed(event.getStack(), module.lastZoomPos, zoomScale);
            }
            if (zoomRaw >= 1.0) {
                module.lastZoomPos = null;
            }
        }

        fadeList.entrySet().removeIf(e ->
                e.getValue() + module.fadeTime.getValue()
                        < System.currentTimeMillis());

        if (module.renderExtrapolation.getValue())
        {
            for (PlayerEntity player : mc.world.getPlayers())
            {
                MotionTracker tracker;
                if (player == null
                    || EntityUtil.isDead(player)
                    || RenderUtil.getEntity().squaredDistanceTo(player) > 200
                    || !RenderUtil.isInFrustum(player.getBoundingBox())
                    || player.equals(RotationUtil.getRotationPlayer())
                    || (tracker = module.extrapolationHelper
                                        .getTrackerFromEntity(player)) == null
                    || !tracker.active)
                {
                    continue;
                }

                Vec3d interpolation = Interpolation.interpolateEntity(player);
                double x = interpolation.x;
                double y = interpolation.y;
                double z = interpolation.z;

                double tX = tracker.getX() - Interpolation.getRenderPosX();
                double tY = tracker.getY() - Interpolation.getRenderPosY();
                double tZ = tracker.getZ() - Interpolation.getRenderPosZ();

                RenderUtil.startRender();
                RenderSystem.enableCull();
                RenderSystem.enableBlend();
                event.getStack().push();

                if (Managers.FRIENDS.contains(player))
                {
                    RenderSystem.setShaderColor(0.33333334f, 0.78431374f, 0.78431374f, 0.55f);
                }
                else
                {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }

                boolean viewBobbing = mc.options.getBobView().getValue();
                mc.options.getBobView().setValue(false);
                // ((IEntityRenderer) mc.entityRenderer)
                //      .invokeOrientCamera(event.getDelta());
                mc.options.getBobView().setValue(viewBobbing);

                RenderSystem.lineWidth(1.5f);
                Matrix4f posMatrix = event.getStack().peek().getPositionMatrix();
                double iX = x - Interpolation.getRenderPosX();
                double iY = y - Interpolation.getRenderPosY();
                double iZ = z - Interpolation.getRenderPosZ();
                float r;
                float g;
                float b;
                float a;
                if (Managers.FRIENDS.contains(player))
                {
                    r = 0.33333334f;
                    g = 0.78431374f;
                    b = 0.78431374f;
                    a = 0.55f;
                }
                else
                {
                    r = 1.0f;
                    g = 1.0f;
                    b = 1.0f;
                    a = 1.0f;
                }

                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                BufferBuilder bufferBuilder = Tessellator.getInstance()
                        .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                bufferBuilder.vertex(posMatrix, (float) tX, (float) tY, (float) tZ).color(r, g, b, a);
                bufferBuilder.vertex(posMatrix, (float) iX, (float) iY, (float) iZ).color(r, g, b, a);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

                event.getStack().pop();
                RenderSystem.disableCull();
                RenderSystem.disableBlend();
                RenderUtil.endRender();
            }
        }
    }

    private void renderBoxMutable(MatrixStack stack, BlockPos pos) {
        bb.setFromBlockPos(pos);
        Interpolation.interpolateMutable(bb);
        renderCurrentMutableBox(stack);
    }

    private void renderBoxZoomed(MatrixStack stack, BlockPos pos, double scale) {
        if (scale >= 1.0) {
            renderBoxMutable(stack, pos);
            return;
        }
        double half = 0.5 * scale;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        bb.setBB(cx - half, cy - half, cz - half,
                 cx + half, cy + half, cz + half);
        Interpolation.interpolateMutable(bb);
        renderCurrentMutableBox(stack);
    }

    private void renderCurrentMutableBox(MatrixStack stack) {
        RenderUtil.renderBox(
            stack,
            new Box(bb.getMinX(), bb.getMinY(), bb.getMinZ(),
                    bb.getMaxX(), bb.getMaxY(), bb.getMaxZ()),
            module.boxColor.getValue(),
            module.outLine.getValue(),
            1.5f);
    }

    private void renderDamage(MatrixStack stack, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        renderDamage(stack, x, y, z);
    }

    private void renderDamage(MatrixStack stack, double x, double yIn, double z) {
        if (module.damage == null || module.damage.isEmpty() || mc.player == null) {
            return;
        }

        RenderDamage renderMode = module.renderMode.getValue();
        double y = yIn + (module.renderDamage.getValue() == RenderDamagePos.OnTop ? 1.35 : 0.5);
        String text = module.damage;
        stack.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(1.0f, -1500000.0f);
        RenderSystem.disableColorLogicOp();
        RenderSystem.disableDepthTest();

        float scale = 0.016666668f * (renderMode == RenderDamage.Indicator ? 0.95f : 1.3f);
        stack.translate(x - Interpolation.getRenderPosX(),
                y - Interpolation.getRenderPosY(),
                z - Interpolation.getRenderPosZ());
        stack.multiply(mc.getEntityRenderDispatcher().getRotation());

        stack.scale(-scale, -scale, scale);

        int distance = (int) Math.sqrt(mc.player.squaredDistanceTo(x, y, z));
        float scaleD = (distance / 2.0f) / (2.0f + (2.0f - 1));
        if (scaleD < 1.0f) {
            scaleD = 1;
        }

        stack.scale(scaleD, scaleD, scaleD);
        int width = Managers.TEXT.getStringWidth(text);
        float drawX = -(width / 2.0f);
        float drawY = renderMode == RenderDamage.Indicator ? 4.0f : 0.0f;
        Matrix4f matrix = stack.peek().getPositionMatrix();
        VertexConsumerProvider.Immediate immediate =
                mc.getBufferBuilders().getEntityVertexConsumers();

        if (renderMode == RenderDamage.Indicator) {
            renderIndicator(matrix, width);
        }

        mc.textRenderer.draw(
                text,
                drawX,
                drawY,
                Color.WHITE.getRGB(),
                true,
                matrix,
                immediate,
                TextRenderer.TextLayerType.SEE_THROUGH,
                renderMode == RenderDamage.Indicator ? 0x55000000 : 0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        mc.textRenderer.draw(
                text,
                drawX,
                drawY,
                renderMode == RenderDamage.Indicator
                        ? module.indicatorColor.getValue().getRGB()
                        : Color.WHITE.getRGB(),
                true,
                matrix,
                immediate,
                TextRenderer.TextLayerType.NORMAL,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(1.0f, 1500000.0f);
        stack.pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderIndicator(Matrix4f matrix, int textWidth) {
        Color color = module.indicatorColor.getValue();
        float alpha = color.getAlpha() / 255.0f;
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;
        float halfWidth = textWidth / 2.0f;
        float radius = Math.max(halfWidth + 10.0f, 16.0f);

        RenderUtil.startRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.0f);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0.0f, -16.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, radius, 0.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, 0.0f, 18.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -radius, 0.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, 0.0f, -16.0f, 0.0f).color(red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0.0f, -10.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, 0.0f, -3.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -halfWidth, 1.5f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, halfWidth, 1.5f, 0.0f).color(red, green, blue, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.0f);
        RenderUtil.endRender();
    }

}
