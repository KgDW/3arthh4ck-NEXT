package me.earth.earthhack.impl.modules.player.fakeplayer;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.core.ducks.entity.IPlayerEntity;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.player.fakeplayer.util.Position;
import me.earth.earthhack.impl.util.client.ModuleUtil;
import me.earth.earthhack.impl.util.text.TextColor;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

final class ListenerMotion extends
        ModuleListener<FakePlayer, MotionUpdateEvent>
{
    private boolean wasRecording;

    public ListenerMotion(FakePlayer module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (module.fakePlayer == null)
        {
            return;
        }

        boolean record = module.record.getValue();
        if (!record && wasRecording)
        {
            wasRecording = false;
        }

        if (module.gapple.getValue()
                && module.timer.passed(module.gappleDelay.getValue()))
        {
            module.fakePlayer.setHealth(module.fakePlayer.getMaxHealth());
            module.fakePlayer.setAbsorptionAmount(16.0f);
            module.fakePlayer.addStatusEffect(
                new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
            module.fakePlayer.addStatusEffect(
                new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
            module.fakePlayer.addStatusEffect(
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
            module.fakePlayer.addStatusEffect(
                new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));

            module.timer.reset();
        }

        if (event.getStage() == Stage.PRE && !record)
        {
            if (module.playRecording.getValue())
            {
                if (module.positions.isEmpty())
                {
                    ModuleUtil.sendMessage(module,
                            TextColor.RED
                                + "No recording was found for this world!");
                    module.playRecording.setValue(false);
                    return;
                }

                if (module.index >= module.positions.size())
                {
                    if (!module.loop.getValue())
                    {
                        module.playRecording.setValue(false);
                        // Stop at the last recorded frame instead of snapping back to index 0.
                        module.index = module.positions.size() - 1;
                        return;
                    }

                    module.index = 0;
                }

                Position p = module.positions.get(module.index++);
                module.fakePlayer.setYaw(p.getYaw());
                module.fakePlayer.setPitch(p.getPitch());
                module.fakePlayer.setHeadYaw(p.getHead());
                module.fakePlayer.setBodyYaw(p.getYaw());
                module.fakePlayer.setSneaking(p.isSneaking());
                module.fakePlayer.setOnGround(p.isOnGround());
                // Use tracked interpolation instead of hard refresh to avoid choppy playback.
                module.fakePlayer.updateTrackedPositionAndAngles(
                    p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(), 3);
                module.fakePlayer.setVelocity(p.getVelocity());
                module.fakePlayer.limbAnimator.setSpeed(p.getLimbSpeed());
                ((IPlayerEntity) module.fakePlayer)
                    .earthhack$setTicksWithoutMotionUpdate(0);
            }
            else
            {
                module.index = 0;
                module.fakePlayer.setVelocity(0.0, 0.0, 0.0);
                module.fakePlayer.limbAnimator.setSpeed(0.0f);
            }
        }
        else if (event.getStage() == Stage.POST && record)
        {
            module.playRecording.setValue(false);
            module.fakePlayer.setVelocity(0.0, 0.0, 0.0);

            if (!wasRecording)
            {
                ModuleUtil.sendMessage(module, "Recording...");
                module.positions.clear();
                wasRecording = true;
            }

            module.positions.add(new Position(mc.player));
        }
    }

}
