package com.cmdpro.databank.model.item;

import com.cmdpro.databank.model.BaseDatabankModel;
import com.cmdpro.databank.model.ModelPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public abstract class DatabankItemModel<T> extends BaseDatabankModel<T> {
    public void submit(T pItem, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedLight, int pPackedOverlay, int pColor, Vec3 normalMult) {
        renderModel(pItem, pPoseStack, submitNodeCollector, pPackedLight, pPackedOverlay, pColor, normalMult);
    }

    public void renderModel(T pItem, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedLight, int pPackedOverlay, int pColor, Vec3 normalMult) {
        for (ModelPose.ModelPosePart i : modelPose.parts) {
            submitPartAndChildren(pItem, pPoseStack, submitNodeCollector, pPackedOverlay, pPackedLight, pColor, i, normalMult);
        }
    }
}