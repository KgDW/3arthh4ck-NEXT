package me.earth.earthhack.impl.modules.combat.autocrystal.helpers;

import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.autocrystal.AbstractCalculation;
import me.earth.earthhack.impl.modules.combat.autocrystal.AutoCrystal;
import me.earth.earthhack.impl.modules.combat.autocrystal.Calculation;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.ACRotate;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.RotationThread;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Helps with processing {@link Calculation}s.
 */
public class ThreadHelper implements Globals
{
    private final StopWatch threadTimer = new StopWatch();
    private final Setting<Boolean> multiThread;
    private final Setting<Boolean> mainThreadThreads;
    private final Setting<Integer> threadDelay;
    private final Setting<RotationThread> rotationThread;
    private final Setting<ACRotate> rotate;
    private final AutoCrystal module;

    private volatile AbstractCalculation<?> currentCalc;

    public ThreadHelper(AutoCrystal module,
                        Setting<Boolean> multiThread,
                        Setting<Boolean> mainThreadThreads,
                        Setting<Integer> threadDelay,
                        Setting<RotationThread> rotationThread,
                        Setting<ACRotate> rotate)
    {
        this.module = module;
        this.multiThread = multiThread;
        this.mainThreadThreads = mainThreadThreads;
        this.threadDelay = threadDelay;
        this.rotationThread = rotationThread;
        this.rotate = rotate;
    }

    public synchronized void start(AbstractCalculation<?> calculation,
                                   boolean multiThread)
    {
        if (module.isPingBypass())
        {
            module.debug("thread-skip-pingbypass",
                    "start(calc) skipped: ping bypass active.");
            return;
        }

        if (!threadTimer.passed(threadDelay.getValue()))
        {
            module.debug("thread-skip-delay",
                    "start(calc) skipped: delay not passed (" + threadDelay.getValue() + "ms).");
            return;
        }

        if (currentCalc != null && !currentCalc.isFinished())
        {
            module.debug("thread-skip-busy",
                    "start(calc) skipped: previous calculation still running.");
            return;
        }

        currentCalc = calculation;
        module.debug("thread-start-calc",
                "start(calc) executing, multiThread=" + multiThread + ".");
        execute(currentCalc, multiThread);
    }

    public synchronized void startThread(BlockPos...blackList)
    {
        if (mc.world == null || mc.player == null)
        {
            module.debug("thread-skip-null",
                    "startThread skipped: world/player null.");
            return;
        }

        if (module.isPingBypass())
        {
            module.debug("thread-skip-pingbypass",
                    "startThread skipped: ping bypass active.");
            return;
        }

        if (!threadTimer.passed(threadDelay.getValue()))
        {
            module.debug("thread-skip-delay",
                    "startThread skipped: delay not passed (" + threadDelay.getValue() + "ms).");
            return;
        }

        if (currentCalc != null && !currentCalc.isFinished())
        {
            module.debug("thread-skip-busy",
                    "startThread skipped: previous calculation still running.");
            return;
        }

        List<Entity> entities = Managers.ENTITIES.getEntities(!mc.isOnThread());
        List<PlayerEntity> players = Managers.ENTITIES.getPlayers(!mc.isOnThread());
        module.debug("thread-start",
                "startThread executing: entities=" + sizeOf(entities)
                    + ", players=" + sizeOf(players)
                    + ", multiThread=" + multiThread.getValue() + ".");
        startThread(entities, players, blackList);
    }

    public synchronized void startThread(boolean breakOnly, boolean noBreak, BlockPos...blackList)
    {
        if (mc.world == null || mc.player == null)
        {
            module.debug("thread-skip-null",
                    "startThread(breakOnly) skipped: world/player null.");
            return;
        }

        if (module.isPingBypass())
        {
            module.debug("thread-skip-pingbypass",
                    "startThread(breakOnly) skipped: ping bypass active.");
            return;
        }

        if (!threadTimer.passed(threadDelay.getValue()))
        {
            module.debug("thread-skip-delay",
                    "startThread(breakOnly) skipped: delay not passed (" + threadDelay.getValue() + "ms).");
            return;
        }

        if (currentCalc != null && !currentCalc.isFinished())
        {
            module.debug("thread-skip-busy",
                    "startThread(breakOnly) skipped: previous calculation still running.");
            return;
        }

        List<Entity> entities = Managers.ENTITIES.getEntities(!mc.isOnThread());
        List<PlayerEntity> players = Managers.ENTITIES.getPlayers(!mc.isOnThread());
        module.debug("thread-start-breakOnly",
                "startThread(breakOnly=" + breakOnly + ", noBreak=" + noBreak + ") executing: entities="
                    + sizeOf(entities) + ", players=" + sizeOf(players) + ".");
        startThread(entities,
                players,
                breakOnly,
                noBreak,
                blackList);
    }

    private void startThread(List<Entity> entities,
                             List<PlayerEntity> players,
                             boolean breakOnly,
                             boolean noBreak,
                             BlockPos...blackList)
    {
        currentCalc = new Calculation(module, entities, players, breakOnly, noBreak, blackList);
        execute(currentCalc, multiThread.getValue());
    }

    private void startThread(List<Entity> entities,
                             List<PlayerEntity> players,
                             BlockPos...blackList)
    {
        currentCalc = new Calculation(module, entities, players, blackList);
        execute(currentCalc, multiThread.getValue());
    }

    private void execute(AbstractCalculation<?> calculation,
                         boolean multiThread)
    {
        if (multiThread)
        {
            Managers.THREAD.submitRunnable(calculation);
            threadTimer.reset();
        }
        else
        {
            threadTimer.reset();
            calculation.run();
        }
    }

    public void schedulePacket(PacketEvent.Receive<?> event)
    {
        if ((multiThread.getValue() || mainThreadThreads.getValue())
            && (rotate.getValue() == ACRotate.None
                || rotationThread.getValue() != RotationThread.Predict))
        {
            event.addPostEvent(this::startThread);
        }
    }

    /** @return the currently running, or last finished calculation. */
    public AbstractCalculation<?> getCurrentCalc()
    {
        return currentCalc;
    }

    public void reset()
    {
        currentCalc = null;
    }

    private static int sizeOf(List<?> list)
    {
        return list == null ? -1 : list.size();
    }

}
