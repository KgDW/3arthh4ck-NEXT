package me.earth.earthhack.impl.util.render.entity;

import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.player.PlayerEntity;

public class StaticModelPlayer<T extends PlayerEntity> {
    private final T player;
    private float limbSwing;
    private float limbSwingAmount;
    private float yaw;
    private float yawHead;
    private float pitch;

    public StaticModelPlayer(T playerIn, boolean smallArms, ModelPart modelSize) {
        this.player = playerIn;
        this.limbSwing = player.limbAnimator.getPos();
        this.limbSwingAmount = player.limbAnimator.getSpeed();
        this.yaw = player.getBodyYaw();
        this.yawHead = player.getHeadYaw();
        this.pitch = player.getPitch();
    }

    // public void render(float scale) {
    //     this.render(null, null, player, limbSwing, limbSwingAmount, player.age, yawHead, pitch, scale);
    // }

    public void disableArmorLayers() {
        // no-op on 1.21.4; model state API was reworked.
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public float getLimbSwing() {
        return limbSwing;
    }

    public void setLimbSwing(float limbSwing) {
        this.limbSwing = limbSwing;
    }

    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    public void setLimbSwingAmount(float limbSwingAmount) {
        this.limbSwingAmount = limbSwingAmount;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYawHead() {
        return yawHead;
    }

    public void setYawHead(float yawHead) {
        this.yawHead = yawHead;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
