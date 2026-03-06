package me.earth.earthhack.impl.core.mixins.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.core.ducks.render.IEndCrystalEntityRenderState;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.crystalchams.CrystalChams;
import me.earth.earthhack.impl.modules.render.crystalchams.CrystalChamsMode;
import me.earth.earthhack.impl.modules.render.crystalscale.CrystalScale;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer
{
    private static final String RENDER_METHOD =
            "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    private static final String UPDATE_RENDER_STATE_METHOD =
            "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V";
    private static final ModuleCache<CrystalScale> SCALE =
            Caches.getModule(CrystalScale.class);
    private static final ModuleCache<CrystalChams> CHAMS =
            Caches.getModule(CrystalChams.class);
    private static final Identifier WHITE_TEXTURE =
            Identifier.ofVanilla("textures/misc/white.png");
    private static final Identifier RAINBOW_TEXTURE =
            Identifier.ofVanilla("textures/rainbow.png");
    private static final float WIREFRAME_SCALE = 1.0015f;

    @Inject(
            method = UPDATE_RENDER_STATE_METHOD,
            at = @At("TAIL"))
    private void earthhack$updateCrystalScale(EndCrystalEntity entity,
                                              EndCrystalEntityRenderState state,
                                              float tickDelta,
                                              CallbackInfo ci)
    {
        float scale = 1.0f;
        if (SCALE.isEnabled())
        {
            scale = SCALE.get().getScale(entity);
        }

        ((IEndCrystalEntityRenderState) state).earthhack$setCrystalScale(scale);
    }

    @Inject(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
    private void earthhack$scaleCrystal(EndCrystalEntityRenderState state,
                                        MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers,
                                        int light,
                                        CallbackInfo ci)
    {
        float scale =
                ((IEndCrystalEntityRenderState) state).earthhack$getCrystalScale();
        if (scale != 1.0f)
        {
            matrices.scale(scale, scale, scale);
        }
    }

    @Redirect(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
    private void earthhack$renderCrystalChams(EndCrystalEntityModel model,
                                              MatrixStack matrices,
                                              VertexConsumer vertexConsumer,
                                              int light,
                                              int overlay,
                                              EndCrystalEntityRenderState state,
                                              MatrixStack renderMatrices,
                                              VertexConsumerProvider vertexConsumers,
                                              int renderLight)
    {
        CrystalChams module = CHAMS.isEnabled() ? CHAMS.get() : null;
        if (module == null)
        {
            model.render(matrices, vertexConsumer, light, overlay);
            return;
        }

        if (module.texture.getValue())
        {
            model.render(matrices, vertexConsumer, light, overlay);
        }

        renderCustomCrystal(model,
                            matrices,
                            vertexConsumers,
                            overlay,
                            module);
    }

    private void renderCustomCrystal(EndCrystalEntityModel model,
                                     MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     int overlay,
                                     CrystalChams module)
    {
        if (module.mode.getValue() == CrystalChamsMode.Gradient)
        {
            Color alpha = module.color.getValue();
            renderFilled(model,
                         matrices,
                         vertexConsumers,
                         overlay,
                         new Color(255, 255, 255, alpha.getAlpha()),
                         RAINBOW_TEXTURE,
                         module.throughWalls.getValue());
            return;
        }

        if (module.wireframe.getValue())
        {
            renderWireframe(model,
                            matrices,
                            vertexConsumers,
                            overlay,
                            module.wireFrameColor.getValue(),
                            module.lineWidth.getValue(),
                            module.wireWalls.getValue());
        }

        if (module.chams.getValue())
        {
            renderFilled(model,
                         matrices,
                         vertexConsumers,
                         overlay,
                         module.color.getValue(),
                         WHITE_TEXTURE,
                         module.throughWalls.getValue());
        }
    }

    private void renderFilled(EndCrystalEntityModel model,
                              MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers,
                              int overlay,
                              Color color,
                              Identifier texture,
                              boolean throughWalls)
    {
        if (color.getAlpha() <= 0)
        {
            return;
        }

        beginLayerState(throughWalls);
        model.render(matrices,
                     vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)),
                     LightmapTextureManager.MAX_LIGHT_COORDINATE,
                     overlay,
                     color.getRGB());
        endLayerState(throughWalls);
    }

    private void renderWireframe(EndCrystalEntityModel model,
                                 MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers,
                                 int overlay,
                                 Color color,
                                 float lineWidth,
                                 boolean throughWalls)
    {
        if (color.getAlpha() <= 0 || lineWidth <= 0.0f)
        {
            return;
        }

        beginLayerState(throughWalls);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        matrices.push();
        matrices.scale(WIREFRAME_SCALE, WIREFRAME_SCALE, WIREFRAME_SCALE);
        model.render(matrices,
                     vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE)),
                     LightmapTextureManager.MAX_LIGHT_COORDINATE,
                     overlay,
                     color.getRGB());
        matrices.pop();
        RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        RenderSystem.lineWidth(1.0f);
        endLayerState(throughWalls);
    }

    private void beginLayerState(boolean throughWalls)
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls)
        {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }
    }

    private void endLayerState(boolean throughWalls)
    {
        if (throughWalls)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
