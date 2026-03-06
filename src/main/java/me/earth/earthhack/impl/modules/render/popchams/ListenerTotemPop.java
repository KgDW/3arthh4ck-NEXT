package me.earth.earthhack.impl.modules.render.popchams;

import me.earth.earthhack.impl.event.events.misc.TotemPopEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.minecraft.PlayerUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

final class ListenerTotemPop extends ModuleListener<PopChams, TotemPopEvent>
{
    public ListenerTotemPop(PopChams module)
    {
        super(module, TotemPopEvent.class);
    }

    @Override
    public void invoke(TotemPopEvent event)
    {
        PlayerEntity player = event.getEntity();
        if (player == null || !module.isValidEntity(player))
        {
            return;
        }

        OtherClientPlayerEntity copy = PlayerUtil.copyPlayer(player, module.copyAnimations.getValue());
        AbstractClientPlayerEntity renderPlayer =
                player instanceof AbstractClientPlayerEntity abstractClientPlayer
                        ? abstractClientPlayer
                        : copy;
        if (!(mc.getEntityRenderDispatcher().getRenderer(renderPlayer) instanceof PlayerEntityRenderer renderer))
        {
            return;
        }

        PlayerEntityRenderState state = renderer.createRenderState();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        renderer.updateRenderState(renderPlayer, state, tickDelta);
        state.bodyYaw = player.getBodyYaw();
        state.yawDegrees = MathHelper.wrapDegrees(player.headYaw - state.bodyYaw);
        state.pitch = player.getLerpedPitch(tickDelta);
        state.pose = player.getPose();
        state.isInSneakingPose = player.isInSneakingPose();
        state.limbFrequency = player.limbAnimator.getPos(tickDelta);
        state.limbAmplitudeMultiplier = player.limbAnimator.getSpeed(tickDelta);
        Vec3d positionOffset = renderer.getPositionOffset(state);
        module.getPopDataList().add(new PopData(
                copy,
                state,
                player.getPos(),
                positionOffset,
                System.currentTimeMillis(),
                player == mc.player,
                Managers.FRIENDS.contains(player) && player != mc.player));
    }
}
