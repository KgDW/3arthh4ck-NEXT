package me.earth.earthhack.impl.modules.render.crystalchams;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.Color;

public class CrystalChams extends Module
{
    public final Setting<CrystalChamsMode> mode =
            register(new EnumSetting<>("Mode", CrystalChamsMode.Normal));
    public final Setting<Boolean> chams =
            register(new BooleanSetting("Chams", false));
    public final Setting<Boolean> throughWalls =
            register(new BooleanSetting("ThroughWalls", false));
    public final Setting<Boolean> wireframe =
            register(new BooleanSetting("Wireframe", false));
    public final Setting<Boolean> wireWalls =
            register(new BooleanSetting("WireThroughWalls", false));
    public final Setting<Boolean> texture =
            register(new BooleanSetting("Texture", false));
    public final NumberSetting<Float> lineWidth =
            register(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 4.0f));
    public final Setting<Color> color =
            register(new ColorSetting("Color",
                                      new Color(255, 255, 255, 255)));
    public final Setting<Color> wireFrameColor =
            register(new ColorSetting("WireframeColor",
                                      new Color(255, 255, 255, 255)));

    public CrystalChams()
    {
        super("CrystalChams", Category.Render);
        this.setData(new SimpleData(this, "Applies custom chams to end crystals."));
    }
}
