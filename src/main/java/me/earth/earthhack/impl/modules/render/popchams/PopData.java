package me.earth.earthhack.impl.modules.render.popchams;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.Vec3d;

record PopData(OtherClientPlayerEntity player,
               PlayerEntityRenderState state,
               Vec3d position,
               Vec3d positionOffset,
               long time,
               boolean self,
               boolean friend)
{
}
