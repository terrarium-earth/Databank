package com.cmdpro.databank.model.blockentity;

import com.cmdpro.databank.model.BaseDatabankModel;
import com.cmdpro.databank.model.ModelPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;

public abstract class DatabankBlockEntityModel<T extends BlockEntityRenderState> extends BaseDatabankModel<T> {
    public void submit(T pEntity, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedOverlay, int pColor, Vec3 normalMult) {
        submitModel(pEntity, pPoseStack, submitNodeCollector, pPackedOverlay, pColor, normalMult);
    }
    public void submitModel(T pEntity, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedOverlay, int pColor, Vec3 normalMult) {
        for (ModelPose.ModelPosePart i : modelPose.parts) {
            submitPartAndChildren(pEntity, pPoseStack, submitNodeCollector, pPackedOverlay, pEntity.lightCoords, pColor, i, normalMult);
        }
    }
}
