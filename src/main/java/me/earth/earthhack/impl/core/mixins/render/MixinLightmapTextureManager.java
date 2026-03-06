package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.fullbright.Fullbright;
import me.earth.earthhack.impl.modules.render.fullbright.mode.BrightMode;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Since Minecraft doesn't allow you to set Gamma to higher than 100,
 * we can just set the world light to the maximum. =D
 * {@link Fullbright}
 *
 * I think we also need to set Ambience on here
 */
@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager
{
    @Unique
    private static final ModuleCache<Fullbright> FULL_BRIGHT =
            Caches.getModule(Fullbright.class);

    @ModifyArg(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Uniform;set(F)V",
                    ordinal = 6),
            index = 0,
            require = 0)
    public float updateHook(float brightnessFactor)
    {
        if (FULL_BRIGHT.isEnabled() && FULL_BRIGHT.get().getBrightMode().equals(BrightMode.Gamma)) {
            return 1.0f;
        }

        return brightnessFactor;
    }
}
