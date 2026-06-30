package com.cmdpro.databank.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;


public abstract class DatabankEntityRenderer<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T, S> {
    private final DatabankEntityModel<S> model;

    public DatabankEntityRenderer(EntityRendererProvider.Context context, DatabankEntityModel<S> model, float shadowRadius) {
        super(context);
        this.shadowRadius = shadowRadius;
        this.model = model;
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        DatabankEntityModel<S> model = getModel(state);
        model.setupModelPose(state);
        model.submit(state, poseStack, submitNodeCollector, getOverlayCoords(state), 0xFFFFFFFF, new Vec3(1, 1, 1));
        poseStack.popPose();
    }

    public int getOverlayCoords(S state) {
        return OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(false));
    }

    public DatabankEntityModel<S> getModel() {
        return model;
    }

    public DatabankEntityModel<S> getModel(S state) {
        return getModel();
    }
}
