package me.earth.earthhack.impl.modules.movement.phase;

import me.earth.earthhack.impl.event.events.movement.MovementInputEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.PlayerInput;

final class ListenerInput extends ModuleListener<Phase, MovementInputEvent>
{
    public ListenerInput(Phase module)
    {
        super(module, MovementInputEvent.class);
    }

    @Override
    public void invoke(MovementInputEvent event)
    {
        if (module.autoSneak.getValue())
        {
            PlayerInput input = event.getInput().playerInput;
            event.getInput().playerInput = new PlayerInput(
                    input.forward(),
                    input.backward(),
                    input.left(),
                    input.right(),
                    input.jump(),
                    !module.requireForward.getValue() || mc.options.forwardKey.isPressed(),
                    input.sprint());
        }
    }

}
