package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.core.ducks.entity.ILivingEntity;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.safety.Safety;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.ACRotate;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.AntiFriendPop;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.Target;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.AntiTotemData;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.ForcePosition;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.PlaceData;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.PositionData;
import me.earth.earthhack.impl.util.math.DistanceUtil;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.RayTraceUtil;
import me.earth.earthhack.impl.util.math.geocache.Sphere;
import me.earth.earthhack.impl.util.math.position.PositionUtil;
import me.earth.earthhack.impl.util.math.raytrace.Ray;
import me.earth.earthhack.impl.util.math.raytrace.RayTraceFactory;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.MotionTracker;
import me.earth.earthhack.impl.util.minecraft.blocks.BlockUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.ncp.Visible;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

/**
 * Helper class for crystal placements.
 */
public class HelperPlace implements Globals
{
    private static final SettingCache<Float, NumberSetting<Float>, Safety> MD =
        Caches.getSetting(Safety.class, NumberSetting.class, "MaxDamage", 4.0f);

    private final AutoCrystal module;

    public HelperPlace(AutoCrystal module)
    {
        this.module = module;
    }

    public PlaceData getData(List<PlayerEntity> general,
                             List<PlayerEntity> players,
                             List<PlayerEntity> enemies,
                             List<PlayerEntity> friends,
                             List<Entity> entities,
                             float minDamage,
                             Set<BlockPos> blackList,
                             double maxY)
    {
        PlaceData data = new PlaceData(minDamage);
        DebugStats stats = module.debug.getValue() ? new DebugStats() : null;
        PlayerEntity target = module.isSuicideModule()
            ? RotationUtil.getRotationPlayer()
            : module.targetMode.getValue().getTarget(
                players, enemies, module.targetRange.getValue());

        if (target == null && module.targetMode.getValue() != Target.Damage)
        {
            return data;
        }

        data.setTarget(target);
        evaluate(data, general, friends, entities, blackList, maxY, stats);
        data.addAllCorrespondingData();
        if (stats != null)
        {
            module.debug("place-stats", stats.summarize(data, minDamage));
        }
        return data;
    }

    private void evaluate(PlaceData data,
                          List<PlayerEntity> players,
                          List<PlayerEntity> friends,
                          List<Entity> entities,
                          Set<BlockPos> blackList,
                          double maxY,
                          DebugStats stats)
    {
        boolean obby = module.obsidian.getValue()
                && module.obbyTimer.passed(module.obbyDelay.getValue())
                && (InventoryUtil.isHolding(Blocks.OBSIDIAN)
                    || (module.obbySwitch.getValue() || module.canAutoSwitch())
                    && InventoryUtil.findHotbarBlock(Blocks.OBSIDIAN) != -1);

        switch (module.preCalc.getValue())
        {
            case Damage:
                for (PlayerEntity player : players)
                {
                    preCalc(data, player, obby, entities, friends, blackList, stats);
                }
            case Target:
                if (data.getTarget() == null)
                {
                    if (data.getData().isEmpty())
                    {
                        break;
                    }
                }
                else
                {
                    preCalc(data, data.getTarget(),
                            obby, entities, friends, blackList, stats);
                }

                for (PositionData positionData : data.getData())
                {
                    if (positionData.getMaxDamage()
                                > data.getMinDamage()
                            && positionData.getMaxDamage()
                                > module.preCalcDamage.getValue())
                    {
                        return;
                    }
                }

                break;
            default:
        }

        BlockPos middle =
                PositionUtil.getPosition(RotationUtil.getRotationPlayer());

        int maxRadius = Sphere.getRadius(module.placeRange.getValue());
        for (int i = 1; i < maxRadius; i++)
        {
            if (stats != null)
            {
                stats.sphereOffsets++;
            }
            calc(middle.add(Sphere.get(i)), data, players, friends,
                 entities, obby, blackList, maxY, stats);
        }
    }

    private void preCalc(PlaceData data,
                         PlayerEntity player,
                         boolean obby,
                         List<Entity> entities,
                         List<PlayerEntity> friends,
                         Set<BlockPos> blackList,
                         DebugStats stats)
    {
        MotionTracker extrapolationEntity = switch (module.preCalcExtra.getValue()) {
            case Place -> module.extrapol.getValue() == 0
                    ? null
                    : module.extrapolationHelper.getTrackerFromEntity(player);
            case Break -> module.bExtrapol.getValue() == 0
                    ? null
                    : module.extrapolationHelper.getBreakTrackerFromEntity(player);
            case Block -> module.blockExtrapol.getValue() == 0
                    ? null
                    : module.extrapolationHelper.getBlockTracker(player);
            default -> null;
        };

        BlockPos pos =
            extrapolationEntity == null || !extrapolationEntity.active
                ? PositionUtil.getPosition(player).down()
                : PositionUtil.getPosition(extrapolationEntity).down();

        for (Direction facing : Direction.HORIZONTAL)
        {
            PositionData pData = selfCalc(data, pos.offset(facing),
                                          entities, friends, obby, blackList, stats);
            if (pData == null)
            {
                continue;
            }

            checkPlayer(data, player, pData);
        }
    }

    private PositionData selfCalc(PlaceData placeData,
                                  BlockPos pos,
                                  List<Entity> entities,
                                  List<PlayerEntity> friends,
                                  boolean obby,
                                  Set<BlockPos> blackList,
                                  DebugStats stats)
    {
        if (stats != null)
        {
            stats.selfCalcCalls++;
        }

        if (blackList.contains(pos))
        {
            if (stats != null)
            {
                stats.blackListRejects++;
            }

            return null;
        }

        PositionData data = PositionData.create(
            pos,
            obby,
            module.rotate.getValue() != ACRotate.None
                                && module.rotate.getValue() != ACRotate.Break
                                ? 0 // TODO: ???
                                : module.helpingBlocks.getValue(),
            module.newVer.getValue(),
            module.newVerEntities.getValue(),
            module.getDeathTime(),
            entities,
            module.lava.getValue(),
            module.water.getValue(),
            module.ignoreLavaItems.getValue(), module);

        if (data.isBlocked() && !module.fallBack.getValue())
        {
            if (stats != null)
            {
                stats.blockedRejects++;
            }

            return null;
        }

        if (data.isLiquid())
        {
            if (!data.isLiquidValid()
                // we won't be able to raytrace the
                // 2 blocks on top if its above us
                || module.liquidRayTrace.getValue()
                    && (module.newVer.getValue()
                        && data.getPos().getY()
                            >= RotationUtil.getRotationPlayer().getY() + 2
                        || !module.newVer.getValue()
                            && data.getPos().getY()
                                >= RotationUtil.getRotationPlayer().getY() + 1)
                || BlockUtil.getDistanceSq(pos.up())
                    >= MathUtil.square(module.placeRange.getValue())
                || BlockUtil.getDistanceSq(pos.up(2))
                    >= MathUtil.square(module.placeRange.getValue()))
            {
                return null;
            }

            if (data.usesObby())
            {
                if (data.isObbyValid())
                {
                    placeData.getLiquidObby().put(data.getPos(), data);
                }

                if (stats != null)
                {
                    stats.obbyOnlyRejects++;
                }

                return null;
            }

            placeData.getLiquid().add(data);
            if (stats != null)
            {
                stats.liquidRejects++;
            }
            return null;
        }
        else if (data.usesObby())
        {
            if (data.isObbyValid())
            {
                placeData.getAllObbyData().put(data.getPos(), data);
            }

            if (stats != null)
            {
                stats.obbyOnlyRejects++;
            }

            return null;
        }

        if (!data.isValid())
        {
            if (stats != null)
            {
                stats.invalidPositionRejects++;
                switch (data.getInvalidReason())
                {
                    case BASE_BLOCK -> stats.invalidBaseRejects++;
                    case BASE_OBBY_BLOCKED -> stats.invalidBaseObbyRejects++;
                    case UP_BLOCKED -> stats.invalidUpRejects++;
                    case UPUP_BLOCKED -> stats.invalidUpUpRejects++;
                    case ENTITY_BLOCKED_UP -> stats.invalidEntityUpRejects++;
                    case ENTITY_BLOCKED_UP2 -> stats.invalidEntityUpUpRejects++;
                    default -> { }
                }
            }

            return null;
        }

        return validate(placeData, data, friends, stats);
    }

    public PositionData validate(PlaceData placeData, PositionData data,
                                 List<PlayerEntity> friends)
    {
        return validate(placeData, data, friends, null);
    }

    private PositionData validate(PlaceData placeData,
                                  PositionData data,
                                  List<PlayerEntity> friends,
                                  DebugStats stats)
    {
        if (BlockUtil.getDistanceSq(data.getPos())
                >= MathUtil.square(module.placeTrace.getValue())
            && noPlaceTrace(data.getPos()))
        {
            if (module.rayTraceBypass.getValue()
                && module.forceBypass.getValue()
                && !data.isLiquid()
                && !data.usesObby())
            {
                data.setRaytraceBypass(true);
            }
            else
            {
                if (stats != null)
                {
                    stats.traceRejects++;
                }
                return null;
            }
        }

        float selfDamage = module.damageHelper.getDamage(data.getPos());
        if (selfDamage > placeData.getHighestSelfDamage())
        {
            placeData.setHighestSelfDamage(selfDamage);
        }

        if (selfDamage > EntityUtil.getHealth(mc.player) - 1.0)
        {
            if (!data.usesObby() && !data.isLiquid())
            {
                Managers.SAFETY.setSafe(false);
            }

            if (!module.suicide.getValue())
            {
                if (stats != null)
                {
                    stats.selfLethalRejects++;
                }
                return null;
            }
        }

        if (selfDamage > MD.getValue()
                && (!data.usesObby() && !data.isLiquid()))
        {
            Managers.SAFETY.setSafe(false);
        }

        if (selfDamage > module.maxSelfPlace.getValue()
                && !module.override.getValue())
        {
            if (stats != null)
            {
                stats.selfDamageRejects++;
            }
            return null;
        }

        if (checkFriends(data, friends))
        {
            if (stats != null)
            {
                stats.friendRejects++;
            }
            return null;
        }

        data.setSelfDamage(selfDamage);
        if (stats != null)
        {
            stats.validated++;
            if (data.getSelfDamage() > stats.maxSelfDamage)
            {
                stats.maxSelfDamage = data.getSelfDamage();
            }
        }

        return data;
    }

    private boolean noPlaceTrace(BlockPos pos)
    {
        if (module.isNotCheckingRotations()
            || module.rayTraceBypass.getValue()
            && !Visible.INSTANCE.check(pos, module.bypassTicks.getValue()))
        {
            return false;
        }

        if (module.smartTrace.getValue())
        {
            for (Direction facing : Direction.values())
            {
                Ray ray = RayTraceFactory.rayTrace(
                                            mc.player,
                                            pos,
                                            facing,
                                            mc.world,
                                            Blocks.OBSIDIAN.getDefaultState(),
                                            module.traceWidth.getValue());
                if (ray.isLegit())
                {
                    return false;
                }
            }

            return true;
        }

        if (module.ignoreNonFull.getValue())
        {
            for (Direction facing : Direction.values())
            {
                Ray ray = RayTraceFactory.rayTrace(
                        mc.player,
                        pos,
                        facing,
                        mc.world,
                        Blocks.OBSIDIAN.getDefaultState(),
                        module.traceWidth.getValue());

                //noinspection deprecation
                if (!mc.world.getBlockState(ray.getResult().getBlockPos()).isFullCube(mc.world, ray.getResult().getBlockPos()))
                {
                    return false;
                }
            }
        }

        return !RayTraceUtil.raytracePlaceCheck(mc.player, pos);
    }

    private void calc(BlockPos pos,
                      PlaceData data,
                      List<PlayerEntity> players,
                      List<PlayerEntity> friends,
                      List<Entity> entities,
                      boolean obby,
                      Set<BlockPos> blackList,
                      double maxY,
                      DebugStats stats)
    {
        if (stats != null)
        {
            stats.calcCalls++;
        }

        if (placeCheck(pos, maxY, stats)
                || (data.getTarget() != null
                        && data.getTarget().squaredDistanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                                > MathUtil.square(module.range.getValue())))
        {
            if (stats != null
                && data.getTarget() != null
                && data.getTarget().squaredDistanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                    > MathUtil.square(module.range.getValue()))
            {
                stats.targetRangeRejects++;
            }

            return;
        }

        PositionData positionData = selfCalc(
                data, pos, entities, friends, obby, blackList, stats);

        if (positionData == null)
        {
            return;
        }

        calcPositionData(data, positionData, players, stats);
    }

    public void calcPositionData(PlaceData data,
                                 PositionData positionData,
                                 List<PlayerEntity> players)
    {
        calcPositionData(data, positionData, players, null);
    }

    private void calcPositionData(PlaceData data,
                                  PositionData positionData,
                                  List<PlayerEntity> players,
                                  DebugStats stats)
    {
        boolean isAntiTotem = false;
        if (data.getTarget() == null)
        {
            for (PlayerEntity player : players)
            {
                isAntiTotem = checkPlayer(data, player, positionData)
                        || isAntiTotem;
            }
        }
        else
        {
            isAntiTotem = checkPlayer(data, data.getTarget(), positionData);
        }

        if (positionData.isRaytraceBypass()
            && (module.rayBypassFacePlace.getValue()
                    && positionData.getFacePlacer() != null
                || positionData.getMaxDamage() > data.getMinDamage()))
        {
            data.getRaytraceData().add(positionData);
            return;
        }

        if (positionData.isForce())
        {
            ForcePosition forcePosition = new ForcePosition(positionData, module);
            for (PlayerEntity forced : positionData.getForced())
            {
                data.addForceData(forced, forcePosition);
            }
        }

        if (isAntiTotem)
        {
            data.addAntiTotem(new AntiTotemData(positionData, module));
        }

        if (positionData.getFacePlacer() != null
                || positionData.getMaxDamage() > data.getMinDamage())
        {
            data.getData().add(positionData);
            if (stats != null)
            {
                stats.accepted++;
            }
        }
        else if (module.shield.getValue()
            && !positionData.usesObby()
            && !positionData.isLiquid()
            && positionData.isValid()
            && positionData.getSelfDamage()
                <= module.shieldSelfDamage.getValue())
        {
            if (module.shieldPrioritizeHealth.getValue())
            {
                positionData.setDamage(0.0f);
            }

            positionData.setTarget(data.getShieldPlayer());
            data.getShieldData().add(positionData);
            if (stats != null)
            {
                stats.shieldAccepted++;
            }
        }
        else if (stats != null)
        {
            stats.lowDamageRejects++;
        }

        if (stats != null && positionData.getMaxDamage() > stats.maxDamage)
        {
            stats.maxDamage = positionData.getMaxDamage();
        }
    }

    private boolean placeCheck(BlockPos pos, double maxY)
    {
        return placeCheck(pos, maxY, null);
    }

    private boolean placeCheck(BlockPos pos, double maxY, DebugStats stats)
    {
        if (pos.getY() < mc.world.getBottomY())
        {
            if (stats != null)
            {
                stats.rangeRejects++;
                stats.belowWorldRejects++;
            }
            return true;
        }

        if (pos.getY() - 1 >= maxY)
        {
            if (stats != null)
            {
                stats.rangeRejects++;
                stats.maxYRejects++;
            }
            return true;
        }

        if (module.isOutsidePlaceRange(pos))
        {
            if (stats != null)
            {
                stats.rangeRejects++;
                stats.placeRangeRejects++;
            }
            return true;
        }

        if (module.placeBreakRange.getValue().isOutsideBreakRange(pos, module)
            || module.rangeHelper.isCrystalOutsideNegativeRange(pos)) {
            if (stats != null)
            {
                stats.breakRangeRejects++;
            }
            return true;
        }

        if (DistanceUtil.distanceSq(
            pos.getX() + 0.5f, pos.getY() + 1, pos.getZ() + 0.5f,
            RotationUtil.getRotationPlayer())
                > MathUtil.square(module.pbTrace.getValue()))
        {
            Vec3d crystalCenter = new Vec3d(pos.getX() + 0.5f,
                                            pos.getY() + 2.0f,
                                            pos.getZ() + 0.5f);
            Vec3d crystalHighPoint = new Vec3d(pos.getX() + 0.5f,
                                               pos.getY() + 2.7f,
                                               pos.getZ() + 0.5f);
            boolean canSee = RayTraceUtil.canBeSeen(
                    crystalCenter,
                    mc.player)
                || RayTraceUtil.canBeSeen(crystalHighPoint, mc.player);
            if (stats != null && !canSee)
            {
                stats.pbTraceRejects++;
            }

            return !canSee;
        }

        return false;
    }

    private static final class DebugStats
    {
        private int sphereOffsets;
        private int calcCalls;
        private int selfCalcCalls;
        private int validated;
        private int accepted;
        private int shieldAccepted;
        private int lowDamageRejects;
        private int rangeRejects;
        private int belowWorldRejects;
        private int maxYRejects;
        private int placeRangeRejects;
        private int breakRangeRejects;
        private int pbTraceRejects;
        private int targetRangeRejects;
        private int blackListRejects;
        private int blockedRejects;
        private int liquidRejects;
        private int obbyOnlyRejects;
        private int invalidPositionRejects;
        private int invalidBaseRejects;
        private int invalidBaseObbyRejects;
        private int invalidUpRejects;
        private int invalidUpUpRejects;
        private int invalidEntityUpRejects;
        private int invalidEntityUpUpRejects;
        private int traceRejects;
        private int selfLethalRejects;
        private int selfDamageRejects;
        private int friendRejects;
        private float maxDamage;
        private float maxSelfDamage;

        private String summarize(PlaceData data, float minDamage)
        {
            return "PlaceStats: candidates=" + sphereOffsets
                + ", calcCalls=" + calcCalls
                + ", selfCalc=" + selfCalcCalls
                + ", validated=" + validated
                + ", accepted=" + accepted
                + ", shield=" + shieldAccepted
                + ", lowDmgRejects=" + lowDamageRejects
                + ", rangeRejects=" + rangeRejects
                + " (belowWorld=" + belowWorldRejects
                + ", maxY=" + maxYRejects
                + ", placeRange=" + placeRangeRejects + ")"
                + ", breakRangeRejects=" + breakRangeRejects
                + ", pbTraceRejects=" + pbTraceRejects
                + ", targetRangeRejects=" + targetRangeRejects
                + ", blackListRejects=" + blackListRejects
                + ", blockedRejects=" + blockedRejects
                + ", liquidRejects=" + liquidRejects
                + ", obbyOnlyRejects=" + obbyOnlyRejects
                + ", invalidPosRejects=" + invalidPositionRejects
                + " (base=" + invalidBaseRejects
                + ", baseObby=" + invalidBaseObbyRejects
                + ", up=" + invalidUpRejects
                + ", up2=" + invalidUpUpRejects
                + ", entUp=" + invalidEntityUpRejects
                + ", entUp2=" + invalidEntityUpUpRejects + ")"
                + ", traceRejects=" + traceRejects
                + ", selfLethalRejects=" + selfLethalRejects
                + ", selfDmgRejects=" + selfDamageRejects
                + ", friendRejects=" + friendRejects
                + ", maxDamage=" + MathUtil.round(maxDamage, 2)
                + ", maxSelf=" + MathUtil.round(maxSelfDamage, 2)
                + ", finalData=" + data.getData().size()
                + ", minDamage=" + MathUtil.round(minDamage, 2) + ".";
        }
    }

    private boolean checkFriends(PositionData data, List<PlayerEntity> friends)
    {
        if (!module.antiFriendPop.getValue().shouldCalc(AntiFriendPop.Place))
        {
            return false;
        }

        for (PlayerEntity friend : friends)
        {
            if (friend != null
                    && !EntityUtil.isDead(friend)
                    && module.damageHelper.getDamage(data.getPos(), friend)
                            > EntityUtil.getHealth(friend) - 0.5f)
            {
                return true;
            }
        }

        return false;
    }

    private boolean checkPlayer(PlaceData data,
                                PlayerEntity player,
                                PositionData positionData)
    {
        BlockPos pos = positionData.getPos();
        if (data.getTarget() == null
                && player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())
                > MathUtil.square(module.range.getValue()))
        {
            return false;
        }

        boolean result = false;
        float health = EntityUtil.getHealth(player);
        float damage = module.damageHelper.getDamage(pos, player);
        if (module.antiTotem.getValue()
                && !positionData.usesObby()
                && !positionData.isLiquid()
                && !positionData.isRaytraceBypass())
        {
            if (module.antiTotemHelper.isDoublePoppable(player))
            {
                if (damage > module.popDamage.getValue())
                {
                    data.addCorrespondingData(player, positionData);
                }
                else if (damage < health + module.maxTotemOffset.getValue()
                        && damage > health + module.minTotemOffset.getValue())
                {
                    positionData.addAntiTotem(player);
                    result = true;
                }
            }
            else if (module.forceAntiTotem.getValue()
                    && Managers.COMBAT.lastPop(player) > 500)
            {
                if (damage > module.popDamage.getValue())
                {
                    data.confirmHighDamageForce(player);
                }

                if (damage > 0.0f
                    && damage < module.totemHealth.getValue()
                                    + module.maxTotemOffset.getValue())
                {
                    data.confirmPossibleAntiTotem(player);
                }

                float force = health - damage;
                if (force > 0.0f && force < module.totemHealth.getValue())
                {
                    positionData.addForcePlayer(player);
                    if (force < positionData.getMinDiff())
                    {
                        positionData.setMinDiff(force);
                    }
                }
            }
        }

        if (damage > module.minFaceDmg.getValue())
        {
            if (health < module.facePlace.getValue()
                    || ((ILivingEntity) player).earthhack$getLowestDurability()
                        <= module.armorPlace.getValue())
            {
                positionData.setFacePlacer(player);
            }
        }

        if (damage > positionData.getMaxDamage())
        {
            positionData.setDamage(damage);
            positionData.setTarget(player);
        }

        return result;
    }

}
