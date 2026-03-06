package me.earth.earthhack.impl.core.mixins.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.render.blockhighlight.BlockHighlight;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Made by cattyn <a href="https://github.com/mioclient/oyvey-ported/blob/master/src/main/java/me/alpha432/oyvey/mixin/MixinWorldRenderer.java">...</a>
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Unique
    private static final ModuleCache<BlockHighlight>
            BLOCK_HIGHLIGHT = Caches.getModule(BlockHighlight.class);

    @Inject(method = "render", at = @At("RETURN"))
    private void render(ObjectAllocator objectAllocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
    {
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(matrix4f);
        RenderUtil.captureWorldMatrices(matrix4f, matrix4f2);
        Profilers.get().push("earthhack-render-3d");
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        Bus.EVENT_BUS.post(new Render3DEvent(matrices, tickCounter.getTickDelta(true)));
        Profilers.get().pop();
    }

    @ModifyVariable(method = "drawBlockOutline", at = @At("HEAD"), argsOnly = true)
    private int modifyBlockOutlineColor(int color)
    {
        // Keep vanilla draw pipeline intact to avoid unused BufferBuilder batches.
        return BLOCK_HIGHLIGHT.isEnabled() ? 0 : color;
    }
}
