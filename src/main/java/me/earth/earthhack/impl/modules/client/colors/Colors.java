package me.earth.earthhack.impl.modules.client.colors;

import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.hud.HudElement;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.impl.event.events.client.PostInitEvent;
import me.earth.earthhack.impl.event.listeners.LambdaListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.util.ArrayList;
import java.util.List;

public class Colors extends Module
{
    protected static List<ColorSetting> cSettings = new ArrayList<>();
    public Colors()
    {
        super("Colors", Category.Client);
        register(Managers.COLOR.getColorSetting());
        register(Managers.COLOR.getRainbowSpeed());

        Bus.EVENT_BUS.register(new LambdaListener<>(PostInitEvent.class, event -> loadColorSettings()));
        Bus.EVENT_BUS.register(new ListenerTick(this));

        this.setData(new SimpleData(this, "Gui colors. This module is always on."));
    }

    public static void loadColorSettings() {
        cSettings.clear();
        for (Module module : Managers.MODULES.getRegistered()) {
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof ColorSetting) {
                    cSettings.add((ColorSetting) setting);
                }
            }
        }

        for (HudElement element : Managers.ELEMENTS.getRegistered()) {
            for (Setting<?> setting : element.getSettings()) {
                if (setting instanceof ColorSetting) {
                    cSettings.add((ColorSetting) setting);
                }
            }
        }
    }

}
