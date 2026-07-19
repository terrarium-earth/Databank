package com.cmdpro.databank.dev.client.renderer.block;

import com.cmdpro.databank.dev.DatabankDev;
import com.cmdpro.databank.dev.block.ModelTestBlockEntity;
import com.cmdpro.databank.model.DatabankModel;
import com.cmdpro.databank.model.DatabankModels;
import com.cmdpro.databank.model.animation.DatabankAnimationState;
import com.cmdpro.databank.model.blockentity.DatabankBlockEntityModel;
import com.cmdpro.databank.model.blockentity.DatabankBlockEntityRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public class ModelTestRenderer extends DatabankBlockEntityRenderer<ModelTestBlockEntity, ModelTestRenderer.RenderState> {
    public ModelTestRenderer(BlockEntityRendererProvider.Context rendererProvider) {
        super(new Model());
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(ModelTestBlockEntity blockEntity, RenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.animState = blockEntity.animState;
    }

    public static class RenderState extends BlockEntityRenderState {
        public DatabankAnimationState animState;
    }

    public static class Model extends DatabankBlockEntityModel<RenderState> {
        public DatabankModel model;
        public static AnimationDefinition idle;

        @Override
        public Identifier getTextureLocation() {
            return DatabankDev.locate("textures/entity/testguy.png");
        }

        @Override
        public void setupModelPose(RenderState obj) {
            obj.animState.updateAnimDefinitions(getModel());
            animate(obj.animState);
        }

        public DatabankModel getModel() {
            if (model == null) {
                model = DatabankModels.models.get(DatabankDev.locate("testguy"));
            }
            return model;
        }
    }
}

