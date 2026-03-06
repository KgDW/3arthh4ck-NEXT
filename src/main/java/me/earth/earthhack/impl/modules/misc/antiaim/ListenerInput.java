package me.earth.earthhack.impl.modules.misc.antiaim;

import me.earth.earthhack.impl.event.events.movement.MovementInputEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.PlayerInput;

final class ListenerInput extends ModuleListener<AntiAim, MovementInputEvent> {
    private boolean sneak;

    public ListenerInput(AntiAim module) {
        super(module, MovementInputEvent.class, 10_000);
    }

    @Override
    public void invoke(MovementInputEvent event) {
        if (module.sneak.getValue() && !event.getInput().playerInput.sneak()) {
            if (module.timer.passed(module.sneakDelay.getValue())) {
                sneak = !sneak;
                module.timer.reset();
            }

            PlayerInput input = event.getInput().playerInput;
            event.getInput().playerInput = new PlayerInput(
                    input.forward(),
                    input.backward(),
                    input.left(),
                    input.right(),
                    input.jump(),
                    sneak,
                    input.sprint());
        }
    }

}
