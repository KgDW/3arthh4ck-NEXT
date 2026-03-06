package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.impl.core.ducks.render.IEndCrystalEntityRenderState;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EndCrystalEntityRenderState.class)
public class MixinEndCrystalEntityRenderState
        implements IEndCrystalEntityRenderState
{
    @Unique
    private float earthhack$crystalScale = 1.0f;

    @Override
    public float earthhack$getCrystalScale()
    {
        return earthhack$crystalScale;
    }

    @Override
    public void earthhack$setCrystalScale(float scale)
    {
        earthhack$crystalScale = scale;
    }
}
