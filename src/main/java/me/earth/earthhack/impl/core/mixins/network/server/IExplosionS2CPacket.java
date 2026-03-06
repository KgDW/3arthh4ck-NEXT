package me.earth.earthhack.impl.core.mixins.network.server;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(ExplosionS2CPacket.class)
public interface IExplosionS2CPacket
{
    @Mutable
    @Accessor(value = "playerKnockback")
    void setPlayerKnockback(Optional<Vec3d> knockback);

    @Accessor(value = "playerKnockback")
    Optional<Vec3d> getPlayerKnockback();

}
