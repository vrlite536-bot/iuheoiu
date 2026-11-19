package com.example.thestalker.client;

import com.example.thestalker.TheStalkerMod;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Long-limbed silhouette with a narrow skull, hunched shoulders and overlong arms. */
public class StalkerModel<T extends Entity> extends EntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(TheStalkerMod.id("stalker"), "main");
    private final ModelPart root, torso, head, leftArm, rightArm, leftLeg, rightLeg;

    public StalkerModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData torso = root.addChild("torso", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4, -9, -2, 8, 13, 4)
                .uv(24, 0).cuboid(-3.5f, -12, -2.5f, 7, 4, 5), ModelTransform.pivot(0, 9, 0));
        torso.addChild("head", ModelPartBuilder.create()
                .uv(0, 18).cuboid(-3, -7, -2.5f, 6, 7, 5)
                .uv(22, 18).cuboid(-1.5f, -8, -2, 3, 2, 4), ModelTransform.pivot(0, -11, 0));
        torso.addChild("left_arm", ModelPartBuilder.create().uv(0, 31)
                .cuboid(-1.5f, -1, -1.5f, 3, 16, 3)
                .uv(12, 31).cuboid(-1, 13, -1, 2, 7, 2), ModelTransform.pivot(5, -8, 0));
        torso.addChild("right_arm", ModelPartBuilder.create().uv(22, 31)
                .cuboid(-1.5f, -1, -1.5f, 3, 16, 3)
                .uv(34, 31).cuboid(-1, 13, -1, 2, 7, 2), ModelTransform.pivot(-5, -8, 0));
        root.addChild("left_leg", ModelPartBuilder.create().uv(42, 0).cuboid(-1.5f, 0, -1.5f, 3, 13, 3), ModelTransform.pivot(2, 9, 0));
        root.addChild("right_leg", ModelPartBuilder.create().uv(42, 17).cuboid(-1.5f, 0, -1.5f, 3, 13, 3), ModelTransform.pivot(-2, 9, 0));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        head.yaw = headYaw * 0.017453292F;
        head.pitch = headPitch * 0.017453292F;
        float swing = MathHelper.cos(limbAngle * 0.6662F) * 1.1F * limbDistance;
        leftLeg.pitch = swing;
        rightLeg.pitch = -swing;
        leftArm.pitch = -0.15F - swing * 0.55F;
        rightArm.pitch = -0.15F + swing * 0.55F;
        torso.pitch = 0.12F + MathHelper.sin(animationProgress * 0.08F) * 0.025F;
        if (entity instanceof com.example.thestalker.entity.StalkerEntity scareStalker && scareStalker.isJumpscarePose()) {
            // The grab starts with a sudden reach, then the arms close around the victim.
            float grab = 0.5F + 0.5F * MathHelper.sin(animationProgress * 0.42F);
            torso.pitch = -0.18F;
            leftArm.pitch = -2.05F - grab * 0.48F;
            rightArm.pitch = -2.05F - grab * 0.48F;
            leftArm.roll = 0.12F + grab * 0.2F;
            rightArm.roll = -0.12F - grab * 0.2F;
            leftArm.yaw = -0.18F;
            rightArm.yaw = 0.18F;
            head.pitch = 0.12F;
        } else if (entity instanceof com.example.thestalker.entity.StalkerEntity animated && animated.getTravelAnimation() == 1) {
            // Fold the shoulders and pull the long arms inward as it crawls into a chest or frame.
            float fold = MathHelper.clamp(animated.getTravelAnimationTicks() / 18.0F, 0.0F, 1.0F);
            torso.pitch = 0.45F + fold * 0.85F;
            torso.yaw = MathHelper.sin(animationProgress * 0.35F) * 0.08F;
            leftArm.pitch = -0.55F - fold * 1.1F;
            rightArm.pitch = -0.55F - fold * 1.1F;
            leftArm.roll = 0.35F + fold * 0.35F;
            rightArm.roll = -0.35F - fold * 0.35F;
            leftLeg.pitch = 0.65F;
            rightLeg.pitch = 0.65F;
            head.pitch = 0.35F;
        } else if (entity instanceof com.example.thestalker.entity.StalkerEntity emerging && emerging.getTravelAnimation() == 2) {
            // Hands appear first; the spine uncoils and drags the rest of the body out.
            float emerge = MathHelper.clamp(emerging.getTravelAnimationTicks() / 24.0F, 0.0F, 1.0F);
            torso.pitch = 1.1F - emerge * 1.25F;
            leftArm.pitch = -2.7F + emerge * 1.5F;
            rightArm.pitch = -2.7F + emerge * 1.5F;
            leftArm.roll = 0.22F;
            rightArm.roll = -0.22F;
            leftLeg.pitch = 1.0F - emerge * 0.8F;
            rightLeg.pitch = 1.0F - emerge * 0.8F;
            head.pitch = -0.25F + emerge * 0.3F;
        } else if (entity instanceof com.example.thestalker.entity.StalkerEntity crawlStalker && crawlStalker.isCrawling()) {
            torso.pitch = 0.95F;
            leftArm.pitch = -1.15F + swing * 0.35F;
            rightArm.pitch = -1.15F - swing * 0.35F;
            leftLeg.pitch = 0.85F - swing * 0.3F;
            rightLeg.pitch = 0.85F + swing * 0.3F;
        }
    }

    @Override public void render(net.minecraft.client.util.math.MatrixStack matrices,
                                 net.minecraft.client.render.VertexConsumer vertices, int light, int overlay,
                                 int color) {
        root.render(matrices, vertices, light, overlay, color);
    }
}
