package me.earth.earthhack.impl.modules.player.scaffold;

import me.earth.earthhack.impl.event.events.movement.MovementInputEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.PlayerInput;

final class ListenerInput extends ModuleListener<Scaffold, MovementInputEvent>
{
    public ListenerInput(Scaffold module)
    {
        super(module, MovementInputEvent.class);
    }

    @Override
    public void invoke(MovementInputEvent event)
    {
        if (module.down.getValue()
                && module.fastSneak.getValue()
                && mc.options.sneakKey.isPressed()
                && mc.options.jumpKey.isPressed())
        {
            PlayerInput input = event.getInput().playerInput;
            event.getInput().playerInput = new PlayerInput(
                    input.forward(),
                    input.backward(),
                    input.left(),
                    input.right(),
                    input.jump(),
                    false,
                    input.sprint());
            event.setCancelled(true);
        }
    }

}
