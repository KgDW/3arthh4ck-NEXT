package me.earth.earthhack.impl.core.mixins.render;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderState.class)
public interface IEntityRenderState
{
    @Mutable
    @Accessor("displayName")
    void setDisplayName(Text displayName);

    @Mutable
    @Accessor("nameLabelPos")
    void setNameLabelPos(Vec3d nameLabelPos);
}
