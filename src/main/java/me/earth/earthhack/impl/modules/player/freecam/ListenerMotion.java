package me.earth.earthhack.impl.modules.player.freecam;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.player.freecam.mode.CamMode;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;

final class ListenerMotion extends ModuleListener<Freecam, MotionUpdateEvent>
{
    public ListenerMotion(Freecam module)
    {
        super(module, MotionUpdateEvent.class, 99_999_999);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (event.getStage() != Stage.PRE
                || module.getMode() != CamMode.Position)
        {
            return;
        }

        PlayerEntity fakePlayer = module.getPlayer();
        if (fakePlayer == null)
        {
            return;
        }

        HitResult result = mc.crosshairTarget;
        if (result != null)
        {
            float[] rotations = RotationUtil.getRotations(result.getPos().x,
                                                          result.getPos().y,
                                                          result.getPos().z,
                                                          fakePlayer);
            module.rotate(rotations[0], rotations[1]);
        }

        fakePlayer.getInventory().clone(mc.player.getInventory());
        event.setX(fakePlayer.getX());
        event.setY(fakePlayer.getBoundingBox().minY);
        event.setZ(fakePlayer.getZ());
        event.setYaw(fakePlayer.getYaw());
        event.setPitch(fakePlayer.getPitch());
        event.setOnGround(fakePlayer.isOnGround());
    }
}
