package me.earth.earthhack.impl.core;

import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.plugin.PluginConfig;
import me.earth.earthhack.impl.managers.client.FileManager;
import me.earth.earthhack.impl.managers.client.PluginManager;
import me.earth.earthhack.impl.managers.thread.scheduler.Scheduler;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/**
 * 3arthh4ck's Core
 */
public final class Core implements PreLaunchEntrypoint {
    /** Logger for the Core. */
    public static final Logger LOGGER = LogManager.getLogger("3arthh4ck-Core");
    public static final ClassLoader CLASS_LOADER = FabricLauncherBase.getLauncher().getTargetClassLoader();

    /** Load the core */
    @Override
    public void onPreLaunch() {
        Bus.EVENT_BUS.subscribe(Scheduler.getInstance());
        new FileManager();

        PluginManager.getInstance().createPluginConfigs();

        MixinEnvironment.getEnvironment(MixinEnvironment.Phase.DEFAULT)
                .setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getEnvironment(MixinEnvironment.Phase.PREINIT)
                .setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getEnvironment(MixinEnvironment.Phase.INIT)
                .setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getEnvironment(MixinEnvironment.Phase.DEFAULT)
                .setSide(MixinEnvironment.Side.CLIENT);

        for (PluginConfig config : PluginManager.getInstance().getPluginConfigs()) {
            if (config.getAccessWidener() != null) {
                LOGGER.warn("Skipping plugin AccessWidener {} for {} on Fabric Loader 0.18.4.",
                        config.getAccessWidener(), config.getName());
            }

            if (config.getMixinConfig() != null) {
                LOGGER.info("Adding "
                        + config.getName()
                        + "'s MixinConfig: "
                        + config.getMixinConfig());

                Mixins.addConfiguration(config.getMixinConfig());
            }
        }
    }
}
