package me.earth.earthhack.impl.modules.misc.antiaim;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

final class ListenerMotion extends ModuleListener<AntiAim, MotionUpdateEvent>
{
    private int skip;

    public ListenerMotion(AntiAim module)
    {
        super(module, MotionUpdateEvent.class, Integer.MAX_VALUE - 1000);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (event.getStage() == Stage.POST || mc.player == null || module.dontRotate())
        {
            return;
        }

        if (module.skip.getValue() != 1 && skip++ % module.skip.getValue() == 0)
        {
            event.setYaw(module.lastYaw);
            event.setPitch(module.lastPitch);
            return;
        }

        switch (module.mode.getValue())
        {
            case Random:
                module.lastYaw = (float) ThreadLocalRandom.current().nextDouble(-180.0, 180.0);
                module.lastPitch = (float) ThreadLocalRandom.current().nextDouble(-90.0, 90.0);
                break;
            case Spin:
                module.lastYaw = MathHelper.wrapDegrees(module.lastYaw + module.hSpeed.getValue());
                module.lastPitch = wrapPitch(module.lastPitch + module.vSpeed.getValue());
                break;
            case Down:
                module.lastYaw = event.getYaw();
                module.lastPitch = 90.0f;
                break;
            case HeadBang:
                module.lastYaw = event.getYaw();
                module.lastPitch = wrapPitch(module.lastPitch + module.vSpeed.getValue());
                break;
            case Horizontal:
                module.lastPitch = event.getPitch();
                module.lastYaw = MathHelper.wrapDegrees(module.lastYaw + module.hSpeed.getValue());
                break;
            case Constant:
                module.lastYaw = MathHelper.wrapDegrees(module.yaw.getValue());
                module.lastPitch = MathHelper.clamp(module.pitch.getValue(), -90.0f, 90.0f);
                break;
            case Flip:
                module.lastYaw = module.flipYaw.getValue()
                        ? MathHelper.wrapDegrees(event.getYaw() + 180.0f)
                        : event.getYaw();
                module.lastPitch = module.flipPitch.getValue()
                        ? MathHelper.clamp(-event.getPitch(), -90.0f, 90.0f)
                        : event.getPitch();
                break;
            case ViewLock:
                module.lastYaw = module.sliceYaw.getValue()
                        ? roundToClosestYaw(event.getYaw(), getYawSlices(module.yawSlices.getValue()))
                        : event.getYaw();
                module.lastPitch = module.slicePitch.getValue()
                        ? roundToClosestPitch(event.getPitch(), getPitchSlices(module.pitchSlices.getValue()))
                        : event.getPitch();
                break;
            default:
                return;
        }

        module.lastYaw = MathHelper.wrapDegrees(module.lastYaw);
        module.lastPitch = MathHelper.clamp(module.lastPitch, -90.0f, 90.0f);
        event.setYaw(module.lastYaw);
        event.setPitch(module.lastPitch);
    }

    private static float wrapPitch(float pitch)
    {
        if (pitch > 90.0f)
        {
            return -90.0f;
        }

        if (pitch < -90.0f)
        {
            return 90.0f;
        }

        return pitch;
    }

    private static int[] getYawSlices(int slices)
    {
        return switch (slices)
        {
            case 1 -> new int[] {0};
            case 2 -> new int[] {-180, 0};
            case 3 -> new int[] {-120, 0, 120};
            default -> new int[] {-180, -90, 0, 90};
        };
    }

    private static int[] getPitchSlices(int slices)
    {
        return switch (slices)
        {
            case 1 -> new int[] {0};
            case 2 -> new int[] {-90, 90};
            case 3 -> new int[] {-90, 0, 90};
            default -> new int[] {-90, -30, 30, 90};
        };
    }

    private static float roundToClosestYaw(float value, int[] targets)
    {
        int best = targets[0];
        float bestDistance = Math.abs(MathHelper.wrapDegrees(value - targets[0]));
        for (int i = 1; i < targets.length; i++)
        {
            float distance = Math.abs(MathHelper.wrapDegrees(value - targets[i]));
            if (distance < bestDistance)
            {
                best = targets[i];
                bestDistance = distance;
            }
        }

        return best;
    }

    private static float roundToClosestPitch(float value, int[] targets)
    {
        int best = targets[0];
        float bestDistance = Math.abs(value - targets[0]);
        for (int i = 1; i < targets.length; i++)
        {
            float distance = Math.abs(value - targets[i]);
            if (distance < bestDistance)
            {
                best = targets[i];
                bestDistance = distance;
            }
        }

        return best;
    }
}