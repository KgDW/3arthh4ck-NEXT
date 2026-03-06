package me.earth.earthhack.impl.modules.player.freecam;

import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.modules.player.freecam.mode.CamMode;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.helpers.disabling.DisablingModule;
import me.earth.earthhack.impl.util.minecraft.PlayerUtil;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Freecam extends DisablingModule
{
    protected final Setting<CamMode> mode =
            register(new EnumSetting<>("Mode", CamMode.Position));
    protected final Setting<Float> speed =
            register(new NumberSetting<>("Speed", 0.5f, 0.1f, 5.0f));
    protected final Setting<Boolean> dismount =
            register(new BooleanSetting("Dismount", true));

    protected OtherClientPlayerEntity fakePlayer;

    public Freecam()
    {
        super("Freecam", Category.Player);
        this.listeners.add(new ListenerPacket(this));
        this.listeners.add(new ListenerUpdate(this));
        this.listeners.add(new ListenerOverlay(this));
        this.listeners.add(new ListenerPush(this));
        this.listeners.add(new ListenerPosLook(this));
        this.listeners.add(new ListenerMove(this));
        this.listeners.add(new ListenerMotion(this));

        SimpleData data = new SimpleData(this,
                "Allows you to look around freely. Spectate is still the more vanilla option.");
        data.register(mode,
                "-Cancel cancels movement packets."
              + "\n-Spanish leaves key interaction packets through."
              + "\n-Position spoofs your server position from the fake body.");
        data.register(speed, "Movement speed while in freecam.");
        this.setData(data);
    }

    public CamMode getMode()
    {
        return mode.getValue();
    }

    @Override
    protected void onEnable()
    {
        mc.chunkCullingEnabled = false;
        if (mc.player == null || mc.world == null)
        {
            this.disable();
            return;
        }

        if (dismount.getValue())
        {
            mc.player.dismountVehicle();
        }

        fakePlayer = PlayerUtil
                .createFakePlayerAndAddToWorld(mc.player.getGameProfile());
        fakePlayer.setOnGround(mc.player.isOnGround());
    }

    @Override
    protected void onDisable()
    {
        mc.chunkCullingEnabled = true;
        if (mc.player == null)
        {
            return;
        }

        if (fakePlayer != null)
        {
            mc.player.setPosition(fakePlayer.getX(),
                                  fakePlayer.getY(),
                                  fakePlayer.getZ());
            mc.player.setVelocity(0.0, 0.0, 0.0);
            PlayerUtil.removeFakePlayer(fakePlayer);
            fakePlayer = null;
        }

        mc.player.noClip = false;
    }

    public PlayerEntity getPlayer()
    {
        return fakePlayer;
    }

    public void rotate(float yaw, float pitch)
    {
        if (fakePlayer != null)
        {
            fakePlayer.setYaw(yaw);
            fakePlayer.setPitch(pitch);
            fakePlayer.headYaw = yaw;
            fakePlayer.setBodyYaw(yaw);
            fakePlayer.prevYaw = yaw;
            fakePlayer.prevPitch = pitch;
            fakePlayer.prevHeadYaw = yaw;
        }
    }
}
