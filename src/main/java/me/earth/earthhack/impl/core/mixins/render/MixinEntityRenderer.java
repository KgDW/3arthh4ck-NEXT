package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.nametags.IEntityNoNametag;
import me.earth.earthhack.impl.modules.render.nametags.Nametags;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer
{
    private static final ModuleCache<Nametags> NAMETAGS =
            Caches.getModule(Nametags.class);

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void earthhack$hideVanillaNametags(Entity entity,
                                               double squaredDistanceToCamera,
                                               CallbackInfoReturnable<Boolean> cir)
    {
        if (NAMETAGS.isEnabled()
                && entity instanceof PlayerEntity
                && !(entity instanceof IEntityNoNametag))
        {
            cir.setReturnValue(true);
        }
        else if (NAMETAGS.isEnabled()
                && entity instanceof IEntityNoNametag)
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void earthhack$forcePlayerNametagState(Entity entity,
                                                   EntityRenderState state,
                                                   float tickDelta,
                                                   CallbackInfo ci)
    {
        if (!NAMETAGS.isEnabled()
                || !(entity instanceof PlayerEntity)
                || entity instanceof IEntityNoNametag)
        {
            return;
        }

        IEntityRenderState accessor = (IEntityRenderState) state;
        accessor.setDisplayName(entity.getDisplayName());
        accessor.setNameLabelPos(
                entity.getAttachments()
                      .getPointNullable(EntityAttachmentType.NAME_TAG,
                                        0,
                                        entity.getLerpedYaw(tickDelta)));
    }
}
