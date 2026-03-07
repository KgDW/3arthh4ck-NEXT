package me.earth.earthhack.impl.core.mixins.entity.living.player;

import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.core.ducks.network.IClientPlayerInteractionManager;
import me.earth.earthhack.impl.core.ducks.network.IPlayerActionC2SPacket;
import me.earth.earthhack.impl.event.events.misc.BlockDestroyEvent;
import me.earth.earthhack.impl.event.events.misc.ClickBlockEvent;
import me.earth.earthhack.impl.event.events.misc.DamageBlockEvent;
import me.earth.earthhack.impl.event.events.misc.ResetBlockEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements IClientPlayerInteractionManager {

    @Shadow
    private float currentBreakingProgress;

    @Shadow
    private int blockBreakingCooldown;

    @Override
    @Invoker(value = "syncSelectedSlot")
    public abstract void earthhack$syncItem();

    @Override
    @Accessor(value = "lastSelectedSlot")
    public abstract int earthhack$getItem();

    @Override
    @Accessor(value = "blockBreakingCooldown")
    public abstract void earthhack$setBlockHitDelay(int delay);

    @Override
    @Accessor(value = "blockBreakingCooldown")
    public abstract int earthhack$getBlockHitDelay();

    @Override
    @Accessor(value = "currentBreakingProgress")
    public abstract float earthhack$getCurrentBreakingProgress();

    @Override
    @Accessor(value = "currentBreakingProgress")
    public abstract void earthhack$setCurrentBreakingProgress(float damage);

    @Override
    @Accessor(value = "breakingBlock")
    public abstract boolean earthhack$getIsHittingBlock();

    @Override
    @Accessor(value = "breakingBlock")
    public abstract void earthhack$setIsHittingBlock(boolean hitting);

    @Override
    @Accessor(value = "networkHandler")
    public abstract ClientPlayNetworkHandler getConnection();

    @Inject(
            method = "updateBlockBreakingProgress",
            at = @At("HEAD"),
            cancellable = true)
    public void onPlayerDamageBlock(BlockPos pos,
                                    Direction direction,
                                    CallbackInfoReturnable<Boolean> cir)
    {
        DamageBlockEvent event = new DamageBlockEvent(pos,
                direction,
                this.currentBreakingProgress,
                this.blockBreakingCooldown);
        Bus.EVENT_BUS.post(event);

        this.currentBreakingProgress = event.getDamage();
        this.blockBreakingCooldown = event.getDelay();

        if (event.isCancelled())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "attackBlock",
            at = @At(value = "HEAD"),
            cancellable = true)
    public void attackBlockHook(BlockPos pos,
                                Direction direction,
                                CallbackInfoReturnable<Boolean> cir)
    {
        ClickBlockEvent event = new ClickBlockEvent(pos, direction);
        Bus.EVENT_BUS.post(event);

        if (event.isCancelled())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "cancelBlockBreaking",
            at = @At("HEAD"),
            cancellable = true)
    public void resetBlockRemovingHook(CallbackInfo info)
    {
        ResetBlockEvent event = new ResetBlockEvent();
        Bus.EVENT_BUS.post(event);

        if (event.isCancelled())
        {
            info.cancel();
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void breakBlockPreHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        BlockDestroyEvent event = new BlockDestroyEvent(Stage.PRE, pos);
        Bus.EVENT_BUS.post(event);
        if (event.isCancelled())
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void breakBlockPostHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ())
        {
            Bus.EVENT_BUS.post(new BlockDestroyEvent(Stage.POST, pos));
        }
    }

    @Redirect(
            method = "attackBlock",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;"))
    private PlayerActionC2SPacket attackBlockInitPacketHook(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction)
    {
        return earthhack$initDigging(action, pos, direction);
    }

    @Redirect(
            method = "cancelBlockBreaking",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;"))
    private PlayerActionC2SPacket cancelBlockBreakingInitPacketHook(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction)
    {
        return earthhack$initDigging(action, pos, direction);
    }

    @Unique
    private PlayerActionC2SPacket earthhack$initDigging(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction)
    {
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket(action, pos, direction);
        if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                || action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
                || action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)
        {
            IPlayerActionC2SPacket digging = (IPlayerActionC2SPacket) packet;
            digging.earthhack$setNormalDigging(true);
            if (action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK)
            {
                digging.earthhack$setClientSideBreaking(true);
            }
        }

        return packet;
    }
}
