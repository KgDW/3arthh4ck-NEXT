package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.combat.autocrystal.AutoCrystal;
import me.earth.earthhack.impl.modules.player.spectate.Spectate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer
{
    @Unique
    private static final ModuleCache<Spectate> SPECTATE =
            Caches.getModule(Spectate.class);
    @Unique
    private static final ModuleCache<AutoCrystal> AUTO_CRYSTAL =
            Caches.getModule(AutoCrystal.class);

    @Unique
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void earthhack$applySpoofedRotations(LivingEntity entity,
                                                 LivingEntityRenderState state,
                                                 float tickDelta,
                                                 CallbackInfo ci)
    {
        if (MC.player == null
                || entity != MC.player
                && !(SPECTATE.isEnabled() && entity.equals(SPECTATE.get().getFake())))
        {
            return;
        }

        float bodyYaw = MathHelper.lerpAngleDegrees(
                tickDelta,
                Managers.ROTATION.getPrevRenderYawOffset(),
                Managers.ROTATION.getRenderYawOffset());
        float headYaw = MathHelper.lerpAngleDegrees(
                tickDelta,
                Managers.ROTATION.getPrevRotationYawHead(),
                Managers.ROTATION.getRotationYawHead());
        float pitch = MathHelper.lerp(
                tickDelta,
                Managers.ROTATION.getPrevPitch(),
                Managers.ROTATION.getRenderPitch());

        if (entity == MC.player && AUTO_CRYSTAL.isEnabled())
        {
            AutoCrystal autoCrystal = AUTO_CRYSTAL.get();
            if (autoCrystal.hasRenderRotations())
            {
                bodyYaw = autoCrystal.getRenderRotationYaw();
                headYaw = bodyYaw;
                pitch = autoCrystal.getRenderRotationPitch();
            }
        }

        state.bodyYaw = bodyYaw;
        state.yawDegrees = MathHelper.wrapDegrees(headYaw - bodyYaw);
        state.pitch = pitch;

        if (state.flipUpsideDown)
        {
            state.yawDegrees *= -1.0f;
            state.pitch *= -1.0f;
        }
    }
}
