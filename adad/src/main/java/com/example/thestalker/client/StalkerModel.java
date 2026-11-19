package com.example.thestalker.client;

import com.example.thestalker.TheStalkerMod;
import com.example.thestalker.entity.StalkerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Long-limbed silhouette with a narrow skull, hooked fingers and deliberately uneven movement. */
public class StalkerModel<T extends Entity> extends EntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(TheStalkerMod.id("stalker"), "main");

    private static final float DEGREES_TO_RADIANS = 0.017453292F;
    private static final float[] CHEST_TORSO_PITCH = {0, 1.32F, 10, 1.19F, 25, 0.79F, 42, 0.39F, 58, 0.20F, 72, 0.12F};
    private static final float[] CHEST_TORSO_Y = {0, 11.2F, 10, 11.4F, 25, 10.6F, 42, 9.55F, 58, 9.08F, 72, 9.0F};
    private static final float[] CHEST_HEAD_PITCH = {0, -0.42F, 9, -0.37F, 20, -0.08F, 34, 0.18F, 50, 0.08F, 72, 0.0F};
    private static final float[] CHEST_LEFT_ARM_PITCH = {0, -2.82F, 9, -2.67F, 22, -1.72F, 39, -0.96F, 57, -0.43F, 72, -0.15F};
    private static final float[] CHEST_RIGHT_ARM_PITCH = {0, -2.48F, 9, -2.73F, 22, -1.13F, 39, -1.28F, 57, -0.36F, 72, -0.15F};
    private static final float[] CHEST_LEFT_ARM_ROLL = {0, 0.52F, 14, 0.43F, 36, 0.22F, 56, 0.11F, 72, 0.0F};
    private static final float[] CHEST_RIGHT_ARM_ROLL = {0, -0.30F, 14, -0.52F, 36, -0.17F, 56, -0.09F, 72, 0.0F};
    private static final float[] CHEST_LEFT_ARM_YAW = {0, -0.28F, 18, -0.16F, 45, 0.08F, 72, 0.0F};
    private static final float[] CHEST_RIGHT_ARM_YAW = {0, 0.18F, 18, 0.28F, 45, -0.06F, 72, 0.0F};
    private static final float[] CHEST_LEFT_LEG_PITCH = {0, 1.42F, 15, 1.28F, 31, 1.13F, 48, 0.53F, 62, 0.18F, 72, 0.0F};
    private static final float[] CHEST_RIGHT_LEG_PITCH = {0, 1.18F, 15, 1.38F, 31, 0.92F, 48, 0.72F, 62, 0.12F, 72, 0.0F};
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart[] leftClaws;
    private final ModelPart[] rightClaws;

    public StalkerModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftClaws = getClaws(leftArm, "left_claw_");
        this.rightClaws = getClaws(rightArm, "right_claw_");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData torso = root.addChild("torso", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-3, -10, -2, 6, 15, 4)
                .uv(24, 0).cuboid(-4, -12, -2.5f, 8, 3, 5), ModelTransform.pivot(0, 9, 0));

        torso.addChild("head", ModelPartBuilder.create()
                .uv(0, 18).cuboid(-3, -8, -2.5f, 6, 8, 5)
                .uv(22, 18).cuboid(-2, -10, -2, 4, 2, 4), ModelTransform.pivot(0, -12, 0));

        ModelPartData leftArm = torso.addChild("left_arm", ModelPartBuilder.create().uv(0, 31)
                .cuboid(-1.5f, -1, -1.5f, 3, 14, 3)
                .uv(12, 31).cuboid(-1, 12, -1, 2, 9, 2), ModelTransform.pivot(5, -8, 0));
        ModelPartData rightArm = torso.addChild("right_arm", ModelPartBuilder.create().uv(22, 31)
                .cuboid(-1.5f, -1, -1.5f, 3, 14, 3)
                .uv(34, 31).cuboid(-1, 12, -1, 2, 9, 2), ModelTransform.pivot(-5, -8, 0));
        addClaws(leftArm, "left_claw_");
        addClaws(rightArm, "right_claw_");

        root.addChild("left_leg", ModelPartBuilder.create().uv(42, 0)
                .cuboid(-1.5f, 0, -1.5f, 3, 15, 3), ModelTransform.pivot(2, 9, 0));
        root.addChild("right_leg", ModelPartBuilder.create().uv(42, 17)
                .cuboid(-1.5f, 0, -1.5f, 3, 15, 3), ModelTransform.pivot(-2, 9, 0));
        return TexturedModelData.of(data, 64, 64);
    }

    /** Add four long, independently curling fingers to each hand. */
    private static void addClaws(ModelPartData arm, String prefix) {
        arm.addChild(prefix + "outer", ModelPartBuilder.create().uv(48, 31)
                .cuboid(-0.5f, 0, -0.5f, 1, 5, 1), ModelTransform.pivot(-1.8f, 19.5f, 0));
        arm.addChild(prefix + "middle", ModelPartBuilder.create().uv(50, 31)
                .cuboid(-0.5f, 0, -0.5f, 1, 5, 1), ModelTransform.pivot(-0.55f, 20.0f, -0.1f));
        arm.addChild(prefix + "inner", ModelPartBuilder.create().uv(52, 31)
                .cuboid(-0.5f, 0, -0.5f, 1, 5, 1), ModelTransform.pivot(0.7f, 19.5f, 0));
        arm.addChild(prefix + "thumb", ModelPartBuilder.create().uv(54, 31)
                .cuboid(-0.5f, 0, -0.5f, 1, 4, 1), ModelTransform.pivot(1.55f, 17.5f, -0.35f));
    }

    private static ModelPart[] getClaws(ModelPart arm, String prefix) {
        return new ModelPart[] {
                arm.getChild(prefix + "outer"),
                arm.getChild(prefix + "middle"),
                arm.getChild(prefix + "inner"),
                arm.getChild(prefix + "thumb")
        };
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        // Reset every transform first. This prevents a previous special pose leaking into the next one.
        torso.pivotX = 0;
        torso.pivotY = 9;
        torso.pivotZ = 0;
        torso.pitch = 0.12F + MathHelper.sin(animationProgress * 0.08F) * 0.025F;
        torso.yaw = 0;
        torso.roll = 0;

        head.pivotX = 0;
        head.pivotY = -12;
        head.pivotZ = 0;
        head.yaw = headYaw * DEGREES_TO_RADIANS;
        head.pitch = headPitch * DEGREES_TO_RADIANS;
        head.roll = 0;

        leftArm.pitch = -0.15F;
        rightArm.pitch = -0.15F;
        leftArm.yaw = rightArm.yaw = 0;
        leftArm.roll = rightArm.roll = 0;
        leftLeg.pitch = rightLeg.pitch = 0;
        leftLeg.yaw = rightLeg.yaw = 0;
        leftLeg.roll = rightLeg.roll = 0;
        resetClaws();

        float swing = MathHelper.cos(limbAngle * 0.6662F) * 1.1F * limbDistance;
        leftLeg.pitch = swing;
        rightLeg.pitch = -swing;
        leftArm.pitch = -0.15F - swing * 0.55F;
        rightArm.pitch = -0.15F + swing * 0.55F;

        if (!(entity instanceof StalkerEntity stalker)) return;

        if (stalker.isJumpscarePose()) {
            float grab = 0.5F + 0.5F * MathHelper.sin(animationProgress * 0.42F);
            torso.pitch = -0.18F;
            leftArm.pitch = -2.05F - grab * 0.48F;
            rightArm.pitch = -2.05F - grab * 0.48F;
            leftArm.roll = 0.12F + grab * 0.2F;
            rightArm.roll = -0.12F - grab * 0.2F;
            leftArm.yaw = -0.18F;
            rightArm.yaw = 0.18F;
            head.pitch = 0.12F;
            animateClaws(0.75F, grab);
        } else if (stalker.isClimbing()) {
            float reach = MathHelper.sin(animationProgress * 0.5F) * 0.2F;
            torso.pitch = -0.28F;
            leftArm.pitch = -2.15F + reach;
            rightArm.pitch = -1.35F - reach;
            leftArm.roll = 0.2F;
            rightArm.roll = -0.2F;
            leftLeg.pitch = 0.9F + reach;
            rightLeg.pitch = -0.8F - reach;
            head.pitch = -0.35F;
            animateClaws(0.45F, reach);
        } else if (stalker.getTravelAnimation() == 1) {
            animateVanishing(stalker, animationProgress);
        } else if (stalker.getTravelAnimation() == 2) {
            animateChestEmergence(stalker, animationProgress, headYaw);
        } else if (stalker.isCrawling()) {
            animateLowCrawl(animationProgress);
        }
    }

    /** A compacting, twisting fold used immediately before the stalker disappears into its hiding place. */
    private void animateVanishing(StalkerEntity stalker, float animationProgress) {
        float ticks = stalker.getTravelAnimationTicks()
                + animationProgress - MathHelper.floor(animationProgress);
        float fold = smoothStep(ticks, 0, 16);
        float twitch = MathHelper.sin(animationProgress * 0.9F) * (1.0F - fold);
        torso.pitch = 0.2F + fold * 1.25F;
        torso.yaw = twitch * 0.12F;
        torso.pivotY = 9.0F + fold * 2.0F;
        head.pitch = -0.12F - fold * 0.48F;
        leftArm.pitch = -0.3F - fold * 1.65F + twitch * 0.12F;
        rightArm.pitch = -0.3F - fold * 1.25F - twitch * 0.12F;
        leftArm.roll = 0.25F + fold * 0.45F;
        rightArm.roll = -0.25F - fold * 0.45F;
        leftLeg.pitch = fold * 0.9F;
        rightLeg.pitch = fold * 1.1F;
        animateClaws(fold, twitch);
    }

    /**
     * A 72-tick, six-beat chest escape: hooked fingers, searching hands, a peeking head,
     * alternating shoulder pulls, dragging hips, then a wary, uneven recovery.
     */
    private void animateChestEmergence(StalkerEntity stalker, float animationProgress, float headYaw) {
        float partialTick = animationProgress - MathHelper.floor(animationProgress);
        float t = MathHelper.clamp(stalker.getTravelAnimationTicks() + partialTick, 0.0F, 72.0F);

        float firstGrip = smoothStep(t, 3.0F, 14.0F);
        float shoulderPull = smoothStep(t, 15.0F, 37.0F);
        float hipRelease = smoothStep(t, 32.0F, 53.0F);
        float settle = smoothStep(t, 56.0F, 72.0F);
        float pullWave = MathHelper.sin((t - 12.0F) * 0.66F)
                * smoothStep(t, 10.0F, 20.0F) * (1.0F - smoothStep(t, 43.0F, 57.0F));
        float shudder = MathHelper.sin(t * 1.55F) * (1.0F - smoothStep(t, 46.0F, 62.0F));

        // The chest lip catches the shoulders; the spine uncoils in several deliberate increments.
        torso.pitch = sample(t, CHEST_TORSO_PITCH);
        torso.pivotY = sample(t, CHEST_TORSO_Y);
        torso.yaw = MathHelper.sin(t * 0.19F) * 0.15F * (1.0F - settle);
        torso.roll = MathHelper.sin(t * 0.31F) * 0.075F * (1.0F - settle);

        // The head appears between the arms, pauses, then slowly turns toward the player.
        head.pitch = sample(t, CHEST_HEAD_PITCH);
        head.yaw = headYaw * DEGREES_TO_RADIANS
                + MathHelper.sin(t * 0.16F) * 0.13F
                + smoothStep(t, 39.0F, 53.0F) * 0.22F * (1.0F - settle);
        head.roll = MathHelper.sin(t * 0.25F) * 0.08F * (1.0F - settle);

        // One hand finds the edge first. The other slips, catches, and hauls the shoulder forward.
        leftArm.pitch = sample(t, CHEST_LEFT_ARM_PITCH) + pullWave * 0.34F + shudder * 0.045F;
        rightArm.pitch = sample(t, CHEST_RIGHT_ARM_PITCH) - pullWave * 0.42F - shudder * 0.035F;
        leftArm.roll = sample(t, CHEST_LEFT_ARM_ROLL) + pullWave * 0.11F;
        rightArm.roll = sample(t, CHEST_RIGHT_ARM_ROLL) - pullWave * 0.13F;
        leftArm.yaw = sample(t, CHEST_LEFT_ARM_YAW);
        rightArm.yaw = sample(t, CHEST_RIGHT_ARM_YAW);

        // The hips stay trapped while the upper body clears the chest, then the legs scrape free.
        float kick = MathHelper.sin((t - 29.0F) * 0.72F) * hipRelease * (1.0F - settle) * 0.30F;
        leftLeg.pitch = sample(t, CHEST_LEFT_LEG_PITCH) + kick;
        rightLeg.pitch = sample(t, CHEST_RIGHT_LEG_PITCH) - kick * 0.85F;
        leftLeg.roll = MathHelper.sin(t * 0.42F) * 0.12F * (1.0F - settle);
        rightLeg.roll = -MathHelper.sin(t * 0.42F + 0.7F) * 0.12F * (1.0F - settle);

        // Fingertips curl over the rim, then release one by one as the creature drags itself clear.
        animateClaws(firstGrip * (1.0F - settle), pullWave);
        for (int i = 0; i < leftClaws.length; i++) {
            float offset = (i - 1.5F) * 0.09F;
            leftClaws[i].pitch += offset * (1.0F - settle);
            rightClaws[i].pitch -= offset * (1.0F - settle);
        }

        // Ease all major joints back to the normal idle pose before the travel state ends.
        torso.pitch = MathHelper.lerp(settle, torso.pitch, 0.12F + MathHelper.sin(animationProgress * 0.08F) * 0.025F);
        leftArm.pitch = MathHelper.lerp(settle, leftArm.pitch, -0.15F);
        rightArm.pitch = MathHelper.lerp(settle, rightArm.pitch, -0.15F);
        leftArm.roll = MathHelper.lerp(settle, leftArm.roll, 0.0F);
        rightArm.roll = MathHelper.lerp(settle, rightArm.roll, 0.0F);
        leftLeg.pitch = MathHelper.lerp(settle, leftLeg.pitch, 0.0F);
        rightLeg.pitch = MathHelper.lerp(settle, rightLeg.pitch, 0.0F);
    }

    private void animateLowCrawl(float animationProgress) {
        float crawlSwing = MathHelper.sin(animationProgress * 0.32F) * 0.22F;
        float weightShift = MathHelper.sin(animationProgress * 0.16F) * 0.045F;
        torso.pitch = 1.32F + MathHelper.sin(animationProgress * 0.22F) * 0.035F;
        torso.roll = weightShift;
        leftArm.pitch = -1.35F + crawlSwing;
        rightArm.pitch = -1.35F - crawlSwing;
        leftArm.roll = 0.28F;
        rightArm.roll = -0.28F;
        leftLeg.pitch = 1.05F - crawlSwing;
        rightLeg.pitch = 1.05F + crawlSwing;
        head.pitch = -0.22F;
        animateClaws(0.32F, crawlSwing);
    }

    private void animateClaws(float grip, float movement) {
        for (int i = 0; i < leftClaws.length; i++) {
            float curl = grip * (i == 3 ? 0.62F : 0.95F) + movement * (i % 2 == 0 ? 0.12F : -0.12F);
            leftClaws[i].pitch = curl;
            rightClaws[i].pitch = curl * 0.92F;
            leftClaws[i].roll = (i - 1.5F) * 0.055F;
            rightClaws[i].roll = -(i - 1.5F) * 0.055F;
        }
    }

    private void resetClaws() {
        for (ModelPart claw : leftClaws) {
            claw.pitch = claw.yaw = claw.roll = 0.0F;
        }
        for (ModelPart claw : rightClaws) {
            claw.pitch = claw.yaw = claw.roll = 0.0F;
        }
    }

    /** Smoothly samples a prebuilt array of time/value pairs: (tick, value, tick, value, ...). */
    private static float sample(float time, float[] keyframes) {
        if (keyframes.length < 4 || keyframes.length % 2 != 0) return 0.0F;
        if (time <= keyframes[0]) return keyframes[1];
        int last = keyframes.length - 2;
        if (time >= keyframes[last]) return keyframes[last + 1];
        for (int i = 0; i < last; i += 2) {
            float start = keyframes[i];
            float end = keyframes[i + 2];
            if (time <= end) {
                float local = MathHelper.clamp((time - start) / (end - start), 0.0F, 1.0F);
                local = local * local * (3.0F - 2.0F * local);
                return MathHelper.lerp(local, keyframes[i + 1], keyframes[i + 3]);
            }
        }
        return keyframes[last + 1];
    }

    private static float smoothStep(float value, float start, float end) {
        float t = MathHelper.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void render(net.minecraft.client.util.math.MatrixStack matrices,
                       net.minecraft.client.render.VertexConsumer vertices, int light, int overlay,
                       int color) {
        root.render(matrices, vertices, light, overlay, color);
    }
}
