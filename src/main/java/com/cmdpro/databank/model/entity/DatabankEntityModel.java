package com.cmdpro.databank.model.entity;

import com.cmdpro.databank.model.BaseDatabankModel;
import com.cmdpro.databank.model.ModelPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public abstract class DatabankEntityModel<T extends EntityRenderState> extends BaseDatabankModel<T> {
    public void submit(T pEntity, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedOverlay, int pColor, Vec3 normalMult) {
        submitModel(pEntity, pPoseStack, submitNodeCollector, pPackedOverlay, pColor, normalMult);
    }

    public void submitModel(T pEntity, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedOverlay, int pColor, Vec3 normalMult) {
        for (ModelPose.ModelPosePart i : modelPose.parts) {
            submitPartAndChildren(pEntity, pPoseStack, submitNodeCollector, pEntity.lightCoords, pPackedOverlay, pColor, i, normalMult);
        }
    }

    @Override
    public RenderType getRenderType(T obj) {
        boolean bodyVisible = isBodyVisible(obj);
        boolean translucent = !bodyVisible && (!(obj instanceof LivingEntityRenderState state) || !state.isInvisibleToPlayer);
        boolean glowing = obj.appearsGlowing();
        Identifier resourcelocation = getTextureLocation();
        if (translucent) {
            return RenderTypes.itemTranslucent(resourcelocation);
        } else if (bodyVisible) {
            return super.getRenderType(obj);
        } else {
            return glowing ? RenderTypes.outline(resourcelocation) : null;
        }
    }
    protected boolean isBodyVisible(T livingEntity) {
        return !livingEntity.isInvisible;
    }
}