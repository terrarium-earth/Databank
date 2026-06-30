package com.cmdpro.databank.worldgui.renderer;

import com.cmdpro.databank.rendering.RenderHandler;
import com.cmdpro.databank.rendering.RenderProjectionUtil;
import com.cmdpro.databank.worldgui.WorldGui;
import com.cmdpro.databank.worldgui.WorldGuiEntity;
import com.cmdpro.databank.worldgui.WorldGuiType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WorldGuiRenderer extends EntityRenderer<WorldGuiEntity, WorldGuiRenderer.RenderState> {
    public WorldGuiRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        if (state.gui != null) {
            MultiBufferSource.BufferSource source = RenderHandler.createBufferSource();
            Vec2 size = state.gui.getType().getRenderSize();
            RenderProjectionUtil.queueProjectionRender((graphics) -> {
                if (state.gui != null) {
                    state.gui.renderGui(graphics);
                }
            }, source, state.topLeft, state.topRight, state.bottomRight, state.bottomLeft, (int) size.x, (int) size.y, state.gui.getType().getViewScale());
        }
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(WorldGuiEntity entity, RenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        WorldGui gui = entity.getGuiData();
        if (gui != null) {
            state.gui = gui;

            state.topLeft = entity.getBoundsCorner(-1, 1);
            state.topRight = entity.getBoundsCorner(1, 1);
            state.bottomRight = entity.getBoundsCorner(1, -1);
            state.bottomLeft = entity.getBoundsCorner(-1, -1);
        }
    }

    @Override
    public boolean shouldRender(WorldGuiEntity pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    public static class RenderState extends EntityRenderState {
        public WorldGui gui;
        public Vec3 topLeft;
        public Vec3 topRight;
        public Vec3 bottomRight;
        public Vec3 bottomLeft;
    }
}
