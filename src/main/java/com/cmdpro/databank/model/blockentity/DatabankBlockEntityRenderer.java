package com.cmdpro.databank.model.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class DatabankBlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> implements BlockEntityRenderer<T, S> {
    private DatabankBlockEntityModel<S> model;

    public DatabankBlockEntityRenderer(DatabankBlockEntityModel<S> model) {
        this.model = model;
    }

    @Override
    public void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        DatabankBlockEntityModel<S> model = getModel(state);
        model.setupModelPose(state);
        model.submit(state, poseStack, submitNodeCollector, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, new Vec3(1, 1, 1));
        poseStack.popPose();
    }

    public DatabankBlockEntityModel<S> getModel() {
        return model;
    }
    public DatabankBlockEntityModel<S> getModel(S state) {
        return getModel();
    }
}
