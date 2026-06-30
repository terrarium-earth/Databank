package com.cmdpro.databank.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.IRenderableSection;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public class AfterHandRenderLevelStageEvent extends RenderLevelStageEvent {
    public AfterHandRenderLevelStageEvent(LevelRenderer levelRenderer, LevelRenderState levelRenderState, @Nullable PoseStack poseStack, Matrix4fc modelViewMatrix, Iterable<? extends IRenderableSection> renderableSections) {
        super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
    }
}
