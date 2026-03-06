package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.nametags.Nametags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer
{
    private static final ModuleCache<Nametags> NAMETAGS =
            Caches.getModule(Nametags.class);

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void earthhack$cancelVanillaPlayerNametag(PlayerEntityRenderState state,
                                                      Text text,
                                                      MatrixStack matrices,
                                                      VertexConsumerProvider vertexConsumers,
                                                      int light,
                                                      CallbackInfo ci)
    {
        if (NAMETAGS.isEnabled())
        {
            Nametags module = NAMETAGS.get();
            if (!module.usesScreenSpaceRendering())
            {
                Entity entity = module.mc.world == null
                        ? null
                        : module.mc.world.getEntityById(state.id);
                if (entity instanceof net.minecraft.entity.player.PlayerEntity player)
                {
                    module.renderLabel(player,
                                       state.nameLabelPos,
                                       state.sneaking,
                                       matrices,
                                       vertexConsumers,
                                       light);
                }
            }

            ci.cancel();
        }
    }
}
