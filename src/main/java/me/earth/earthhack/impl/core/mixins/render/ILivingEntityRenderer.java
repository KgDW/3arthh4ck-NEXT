package me.earth.earthhack.impl.core.mixins.render;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface ILivingEntityRenderer
{
    @Invoker("setupTransforms")
    void earthhack$setupTransforms(LivingEntityRenderState state,
                                   MatrixStack matrices,
                                   float bodyYaw,
                                   float baseScale);

    @Invoker("scale")
    void earthhack$scale(LivingEntityRenderState state, MatrixStack matrices);
}
