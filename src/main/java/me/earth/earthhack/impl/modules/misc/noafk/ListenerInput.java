package me.earth.earthhack.impl.modules.misc.noafk;

import me.earth.earthhack.impl.event.events.movement.MovementInputEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.util.PlayerInput;

import java.util.Random;

final class ListenerInput extends ModuleListener<NoAFK, MovementInputEvent>
{
    private final Random random = new Random();
    private boolean backwards = false;

    public ListenerInput(NoAFK module)
    {
        super(module, MovementInputEvent.class);
    }

    @Override
    public void invoke(MovementInputEvent event)
    {
        if (Managers.NCP.passed(module.lagTime.getValue()))
        {
            if (module.sneak.getValue())
            {
                if (module.sneak_timer.passed(2000))
                {
                    module.sneaking = !module.sneaking;
                    module.sneak_timer.reset();
                }

                PlayerInput input = event.getInput().playerInput;
                event.getInput().playerInput = new PlayerInput(
                        input.forward(),
                        input.backward(),
                        input.left(),
                        input.right(),
                        input.jump(),
                        module.sneaking,
                        input.sprint());
            }

            if (module.jump.getValue()
                && module.jumpTimer.passed(module.jumpDelay.getValue() * 1000))
            {
                PlayerInput input = event.getInput().playerInput;
                event.getInput().playerInput = new PlayerInput(
                        input.forward(),
                        input.backward(),
                        input.left(),
                        input.right(),
                        true,
                        input.sneak(),
                        input.sprint());
                module.jumpTimer.reset();
            }

            if (module.walk.getValue())
            {
                if (module.walkTimer.passed(module.walking ? (module.walkFor.getValue() * 1000) : (module.waitFor.getValue()) * 1000))
                {
                    backwards = module.randomlyBackwards.getValue() && random.nextBoolean();
                    module.walking = !module.walking;
                    module.walkTimer.reset();
                    if (!module.walking && mc.player != null)
                    {
                        mc.player.headYaw = (mc.player.headYaw + module.yaw.getValue()) % 360;
                    }
                }

                event.getInput().movementForward = module.walking ? (backwards ? -1.0f : 1.0f) : 0.0f;
                if (event.getInput().playerInput.sneak())
                {
                    event.getInput().movementForward *= 0.3f;
                }
            }
        }
    }

}
