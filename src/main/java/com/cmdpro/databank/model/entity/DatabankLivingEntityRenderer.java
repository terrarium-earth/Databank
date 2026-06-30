package com.cmdpro.databank.model.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

public abstract class DatabankLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> extends DatabankEntityRenderer<T, S> {
    public DatabankLivingEntityRenderer(EntityRendererProvider.Context context, DatabankEntityModel<S> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Override
    public int getOverlayCoords(S state) {
        return OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(state.hasRedOverlay));
    }

    @Override
    public float getShadowRadius(S state) {
        return super.getShadowRadius(state) * state.ageScale;
    }
}
