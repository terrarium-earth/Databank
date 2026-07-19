package com.cmdpro.databank.megastructures.block.renderers;

import com.cmdpro.databank.megastructures.block.MegastructureSaveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public class MegastructureSaveRenderer implements BlockEntityRenderer<MegastructureSaveBlockEntity, MegastructureSaveRenderer.RenderState> {
    public MegastructureSaveRenderer(BlockEntityRendererProvider.Context rendererProvider) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(MegastructureSaveBlockEntity blockEntity, RenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        if (blockEntity.corner1 != null && blockEntity.corner2 != null) {
            BlockEntityWithBoundingBoxRenderer.extract(blockEntity, state);
        }

        state.corner1 = blockEntity.corner1;
        state.corner2 = blockEntity.corner2;
        state.center = blockEntity.center;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockPos corner1 = state.corner1;
        BlockPos corner2 = state.corner2;
        BlockPos center = state.center;
        Color color = Color.GREEN;
        Color centerColor = Color.WHITE;
        boolean complete = true;
        if (corner1 == null) {
            corner1 = state.blockPos;
            color = Color.RED;
            complete = false;
        }
        if (corner2 == null) {
            corner2 = corner1;
            color = Color.YELLOW;
            complete = false;
        }
        BlockPos minBlock = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ())
        );
        BlockPos maxBlock = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ())
        );
        Vec3 min = minBlock.getCenter().add(-0.5f, -0.5f, -0.5f);
        Vec3 max = maxBlock.getCenter().add(0.5f, 0.5f, 0.5f);

        Gizmos.cuboid(
            new AABB(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()),
            GizmoStyle.stroke(ARGB.color(255, color.getRGB())),
            true
        ).setAlwaysOnTop();

        if (complete) {
            if (center != null) {
                Vec3 centerMin = center.getCenter().add(-0.5f, -0.5f, -0.5f);
                Vec3 centerMax = center.getCenter().add(0.5f, 0.5f, 0.5f);

                Gizmos.cuboid(
                    new AABB(centerMin.x(), centerMin.y(), centerMin.z(), centerMax.x(), centerMax.y(), centerMax.z()),
                    GizmoStyle.stroke(ARGB.color(255, centerColor.getRGB())),
                    true
                ).setAlwaysOnTop();
            }
        }

        renderInvisibleBlocks(state);
    }

    private void renderInvisibleBlocks(RenderState state) {
        if (state.invisibleBlocks == null) {
            return;
        }

        BoundingBoxRenderable.RenderableBox box = state.box;
        var size = box.size();
        var startingPos = box.localPos();

        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    int index = z * size.getX() * size.getY() + y * size.getX() + x;

                    BlockEntityWithBoundingBoxRenderState.InvisibleBlockType invisibleBlockType = state.invisibleBlocks[index];
                    if (invisibleBlockType == null) {
                        continue;
                    }

                    double renderX0 = startingPos.getX() + x + 0.45F;
                    double renderY0 = startingPos.getY() + y + 0.45F;
                    double renderZ0 = startingPos.getZ() + z + 0.45F;
                    double renderX1 = startingPos.getX() + x + 0.55F;
                    double renderY1 = startingPos.getY() + y + 0.55F;
                    double renderZ1 = startingPos.getZ() + z + 0.55F;
                    AABB aabb = new AABB(renderX0, renderY0, renderZ0, renderX1, renderY1, renderZ1);
                    if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.75F, 0.75F)));
                    } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-65536));
                    } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-256));
                    }
                }
            }
        }
    }

    public static class RenderState extends BlockEntityWithBoundingBoxRenderState {
        public BlockPos corner1;
        public BlockPos corner2;
        public BlockPos center;
    }
}
