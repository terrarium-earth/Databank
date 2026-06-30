package com.cmdpro.databank.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class DatabankLivingEntityModel<T extends LivingEntityRenderState> extends DatabankEntityModel<T> {
    @Override
    public void submitModel(T pEntity, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedOverlay, int pColor, Vec3 normalMult) {
        pPoseStack.pushPose();
        float scale = pEntity.scale;
        pPoseStack.scale(scale, scale, scale);
        setupRotations(pEntity, pPoseStack, scale);
        super.submitModel(pEntity, pPoseStack, submitNodeCollector, pPackedOverlay, pColor, normalMult);
        pPoseStack.popPose();
    }

    public Vec2 getHeadRot(T entity) {
        float yRot = entity.yRot;
        float xRot = entity.xRot;
        if (isEntityUpsideDown(entity)) {
            xRot *= -1.0F;
            yRot *= -1.0F;
        }

        yRot = Mth.wrapDegrees(yRot);
        return new Vec2(xRot, yRot);
    }

    public float getYBodyRot(T entity) {
        float bodyRot = entity.bodyRot;
        if (this.isShaking(entity)) {
            bodyRot += (float)(Math.cos((double)entity.ageInTicks * 3.25) * Math.PI * 0.4F);
        }
        return bodyRot;
    }

    public boolean isEntityUpsideDown(T entity) {
        return entity.isUpsideDown;
    }

    protected boolean isShaking(T entity) {
        return entity.isFullyFrozen;
    }

    protected void setupRotations(T entity, PoseStack poseStack, float scale) {
        float yBodyRot = getYBodyRot(entity);
        if (!entity.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
        }

        if (entity.deathTime > 0) {
            float f = (entity.deathTime - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            poseStack.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees(entity)));
        } else if (entity.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - entity.xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.ageInTicks * -75.0F));
        } else if (entity.hasPose(Pose.SLEEPING)) {
            Direction direction = entity.bedOrientation;
            float f1 = direction != null ? sleepDirectionToRotation(direction) : yBodyRot;
            poseStack.mulPose(Axis.YP.rotationDegrees(f1));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(entity)));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (isEntityUpsideDown(entity)) {
            poseStack.translate(0.0F, (entity.boundingBoxHeight + 0.1F) / scale, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }
    private static float sleepDirectionToRotation(Direction facing) {
        switch (facing) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }
    protected float getFlipDegrees(T livingEntity) {
        return 90.0F;
    }
}