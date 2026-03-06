package me.earth.earthhack.impl.modules.render.nametags;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Complexity;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.StopWatch;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.PushMode;
import me.earth.earthhack.impl.util.render.RenderUtil;
import me.earth.earthhack.impl.util.thread.SafeRunnable;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Nametags extends Module
{
    private static final float DEFAULT_SCALE = 0.25f;
    private static final float SCALE_DIVISOR = 100.0f;
    private static final float DEFAULT_INTERNAL_SCALE = 0.003f;
    private static final float CLOSE_RANGE = 8.0f;
    private static final float CLOSE_SCALE = 0.0245f;
    private static final float DISTANCE_SCALE_OFFSET = 0.0018f;

    protected final Setting<Boolean> twoD =
            register(new BooleanSetting("2D", false));
    protected final Setting<Boolean> health =
            register(new BooleanSetting("Health", true));
    protected final Setting<Boolean> ping =
            register(new BooleanSetting("Ping", true));
    protected final Setting<Boolean> id =
            register(new BooleanSetting("Id", false));
    protected final Setting<Boolean> itemStack =
            register(new BooleanSetting("StackName", false));
    protected final Setting<Boolean> armor =
            register(new BooleanSetting("Armor", true));
    protected final Setting<Boolean> durability =
            register(new BooleanSetting("Durability", true));
    protected final Setting<Boolean> max =
            register(new BooleanSetting("EnchantMax", false));
    protected final Setting<Boolean> gameMode =
            register(new BooleanSetting("GameMode", false));
    protected final Setting<Boolean> illegalEffects =
            register(new BooleanSetting("IllegalEffects", false));
    protected final Setting<Boolean> invisibles =
            register(new BooleanSetting("Invisibles", false));
    protected final Setting<Boolean> pops =
            register(new BooleanSetting("Pops", true));
    protected final Setting<Boolean> burrow =
            register(new BooleanSetting("Burrow", true));
    protected final Setting<Boolean> fov =
            register(new BooleanSetting("Fov", false));
    protected final Setting<Boolean> sneak =
            register(new BooleanSetting("Sneak", true));
    protected final Setting<Float> scale =
            register(new NumberSetting<>("Scale", DEFAULT_SCALE, 0.1f, 5.0f));
    protected final Setting<Integer> delay =
            register(new NumberSetting<>("Delay", 16, 0, 100));
    protected final Setting<Color> outlineColor =
            register(new ColorSetting("Outline-Color", new Color(135, 135, 135, 0)));
    protected final Setting<Float> outlineWidth =
            register(new NumberSetting<>("Outline-Width", 1.8f, 0.2f, 10.0f));
    protected final Setting<Boolean> media =
            register(new BooleanSetting("Media", true));
    protected final Setting<Boolean> phase =
            register(new BooleanSetting("Phase", false));
    protected final Setting<PushMode> pushMode =
            register(new EnumSetting<>("PhasePushDetect", PushMode.None))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> multiThread =
            register(new BooleanSetting("MultiThread", true));
    protected final Setting<Boolean> motion =
            register(new BooleanSetting("MotionXYZ", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> motionKpH =
            register(new BooleanSetting("MotionKpH", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> close =
            register(new BooleanSetting("Close", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> withDistance =
            register(new BooleanSetting("WithDistance", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> distance =
            register(new NumberSetting<>("Distance", 110.0, 0.0, 200.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> debug =
            register(new BooleanSetting("Debug", false))
                .setComplexity(Complexity.Dev);

    protected List<Nametag> nametags = new ArrayList<>();
    protected final StopWatch timer = new StopWatch();

    public Nametags()
    {
        super("Nametags", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new NametagsData(this));
    }

    public boolean usesScreenSpaceRendering()
    {
        return twoD.getValue();
    }

    public void renderLabel(PlayerEntity player,
                            Vec3d nameLabelPos,
                            boolean sneaking,
                            MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers,
                            int light)
    {
        Entity renderEntity;
        if (mc.player == null
                || mc.world == null
                || player == null
                || player == mc.player
                || nameLabelPos == null
                || player instanceof IEntityNoNametag
                || twoD.getValue()
                || (renderEntity = RenderUtil.getEntity()) == null)
        {
            return;
        }

        if (player.isInvisible() && !invisibles.getValue()
                || withDistance.getValue()
                    && renderEntity.squaredDistanceTo(player)
                        > MathUtil.square(distance.getValue())
                || fov.getValue()
                    && !(RotationUtil.inFov(player)
                         || renderEntity.squaredDistanceTo(player) <= 1.0
                            && close.getValue()))
        {
            return;
        }

        Nametag nametag = new Nametag(this, player);
        TextRenderer textRenderer = mc.textRenderer;
        boolean visibleThroughWalls = !sneaking;
        float drawX = -nametag.nameWidth / 2.0f;
        float distance = (float) Math.sqrt(renderEntity.squaredDistanceTo(player));
        float labelScale = getWorldScale(distance);

        int background = 0x55000000;

        matrices.push();
        matrices.translate(nameLabelPos.x, nameLabelPos.y + 0.5, nameLabelPos.z);
        matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
        matrices.scale(labelScale, -labelScale, labelScale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        textRenderer.draw(nametag.nameString,
                          drawX,
                          0.0f,
                          nametag.nameColor,
                          false,
                          matrix,
                          vertexConsumers,
                          visibleThroughWalls
                              ? TextRenderer.TextLayerType.SEE_THROUGH
                              : TextRenderer.TextLayerType.NORMAL,
                          background,
                          light);

        if (visibleThroughWalls)
        {
            textRenderer.draw(nametag.nameString,
                              drawX,
                              0.0f,
                              nametag.nameColor,
                              false,
                              matrix,
                              vertexConsumers,
                              TextRenderer.TextLayerType.NORMAL,
                              0,
                              LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }

        matrices.pop();
    }

    protected void updateNametags()
    {
        if (timer.passed(delay.getValue()))
        {
            List<PlayerEntity> players = Managers.ENTITIES.getPlayers();
            if (players == null)
            {
                return;
            }

            SafeRunnable runnable = () ->
            {
                List<Nametag> nametags = new ArrayList<>(players.size());
                for (PlayerEntity player : players)
                {
                    if (player != null
                            && player.isAlive()
                            && !player.equals(mc.player)
                            && !(player instanceof IEntityNoNametag))
                    {
                        nametags.add(new Nametag(this, player));
                    }
                }

                this.nametags = nametags;
            };

            if (multiThread.getValue())
            {
                Managers.THREAD.submit(runnable);
            }
            else
            {
                runnable.run();
            }

            timer.reset();
        }
    }

    protected int getFontOffset(int enchHeight)
    {
        int armorOffset = armor.getValue() ? -26 : -27;
        if (enchHeight > 4)
        {
            armorOffset -= (enchHeight - 4) * 8;
        }

        return armorOffset;
    }

    protected float getScaleMultiplier()
    {
        return getInternalScale() / DEFAULT_INTERNAL_SCALE;
    }

    protected float getScreenScale()
    {
        return getScaleMultiplier();
    }

    protected float getWorldScale(float distance)
    {
        if (distance <= CLOSE_RANGE)
        {
            return CLOSE_SCALE * getScaleMultiplier();
        }

        return DISTANCE_SCALE_OFFSET + getInternalScale() * distance;
    }

    protected float getInternalScale()
    {
        return scale.getValue() / SCALE_DIVISOR;
    }

}
