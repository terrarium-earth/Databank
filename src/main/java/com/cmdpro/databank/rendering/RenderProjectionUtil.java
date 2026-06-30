package com.cmdpro.databank.rendering;

import com.cmdpro.databank.ClientDatabankUtils;
import com.cmdpro.databank.Databank;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
public class RenderProjectionUtil {
    private static final RenderTargetPool pool = new RenderTargetPool();
    private static RenderTarget projectedTarget;
    private static RenderTarget guiTargetOverride;
    private static final List<ProjectionRender> queued = new ArrayList<>();
    public static final Identifier PROJECTED_TARGET_TEXTURE = Databank.locate("internal/projected_target.png");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
        if (ShaderHelper.shouldUseAlternateRendering()) {
            RenderSystem.getModelViewStack().pushMatrix().set(RenderHandler.matrix4f);
            doEffectRendering(event);
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterWeather event) {
        if (!ShaderHelper.shouldUseAlternateRendering()) {
            doEffectRendering(event);
        }
    }

    private static void doEffectRendering(RenderLevelStageEvent event) {
        var camera = event.getLevelRenderState().cameraRenderState;

        event.getPoseStack().pushPose();

        event.getPoseStack().translate(
            -camera.pos.x(),
            -camera.pos.y(),
            -camera.pos.z()
        );

        for (ProjectionRender i : queued) {
            i.apply(event.getPoseStack());
        }

        queued.clear();
        event.getPoseStack().popPose();
    }

    // TODO Don't queue? kinda obsolete with the submit node collector + feature renderer, and probably unsafe.
    public static void queueProjectionRender(Consumer<GuiGraphicsExtractor> graphics, MultiBufferSource.BufferSource source, Vec3 topLeft, Vec3 topRight, Vec3 bottomRight, Vec3 bottomLeft, int width, int height, float viewScale) {
        ProjectionRender render = new ProjectionRender(
            graphics,
            (stack) -> {
            },
            (stack) -> {
            },
            source,
            topLeft,
            topRight,
            bottomRight,
            bottomLeft,
            width,
            height,
            viewScale
        );

        queued.add(render);
    }

    public static void renderTarget(RenderTarget target, MultiBufferSource.BufferSource source, PoseStack poseStack, Vec3 topLeft, Vec3 topRight, Vec3 bottomRight, Vec3 bottomLeft) {
        float tlX = (float) topLeft.x();
        float tlY = (float) topLeft.y();
        float tlZ = (float) topLeft.z();

        float trX = (float) topRight.x();
        float trY = (float) topRight.y();
        float trZ = (float) topRight.z();

        float brX = (float) bottomRight.x();
        float brY = (float) bottomRight.y();
        float brZ = (float) bottomRight.z();

        float blX = (float) bottomLeft.x();
        float blY = (float) bottomLeft.y();
        float blZ = (float) bottomLeft.z();
        VertexConsumer consumer = source.getBuffer(RenderTypeHandler.SCREEN_PROJECTION);

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, tlX, tlY, tlZ).setUv(0, 0);
        consumer.addVertex(pose, trX, trY, trZ).setUv(1, 0);
        consumer.addVertex(pose, brX, brY, brZ).setUv(1, 1);
        consumer.addVertex(pose, blX, blY, blZ).setUv(0, 1);
        poseStack.popPose();

        source.endBatch(RenderTypeHandler.SCREEN_PROJECTION);
    }

    static MultiBufferSource.BufferSource projectionBufferSource = null;

    private static MultiBufferSource.BufferSource createProjectionBufferSource() {
        if (projectionBufferSource == null) {
            projectionBufferSource = ClientDatabankUtils.createMainBufferSourceCopy();
        }
        return projectionBufferSource;
    }

    public static RenderTarget getGuiTargetOverride() {
        return guiTargetOverride;
    }

    public static RenderTarget getProjectedTarget() {
        return projectedTarget;
    }

    private static class ProjectionRender {
        Consumer<GuiGraphicsExtractor> graphics;
        Consumer<PoseStack> applyPoseStackTransformations;
        Consumer<PoseStack> undoPoseStackTransformations;
        MultiBufferSource.BufferSource source;
        Vec3 topLeft;
        Vec3 topRight;
        Vec3 bottomRight;
        Vec3 bottomLeft;
        int width;
        int height;
        float viewScale;

        public ProjectionRender(Consumer<GuiGraphicsExtractor> graphics, Consumer<PoseStack> applyPoseStackTransformations, Consumer<PoseStack> undoPoseStackTransformations, MultiBufferSource.BufferSource source, Vec3 topLeft, Vec3 topRight, Vec3 bottomRight, Vec3 bottomLeft, int width, int height, float viewScale) {
            this.graphics = graphics;
            this.applyPoseStackTransformations = applyPoseStackTransformations;
            this.undoPoseStackTransformations = undoPoseStackTransformations;
            this.source = source;
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomRight = bottomRight;
            this.bottomLeft = bottomLeft;
            this.width = width;
            this.height = height;
            this.viewScale = viewScale;
        }

        public void apply(PoseStack poseStack) {
            int viewWidth = (int) (width * viewScale);
            int viewHeight = (int) (height * viewScale);

            Supplier<RenderTarget> targetCreationSupplier = () -> (RenderTarget) new MainTarget(viewWidth, viewHeight);

            RenderTarget target = pool.getTarget((renderTarget) -> renderTarget.width == viewWidth && renderTarget.height == viewHeight, targetCreationSupplier);
            Identifier use = pool.generateRandomUseId(Databank.MOD_ID);
            pool.markUse(target, use);

            // TODO Calculate mouse positions
            GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
            GuiGraphicsExtractor guiGraphics = new GuiGraphicsExtractor(
                Minecraft.getInstance(),
                gameRenderer.guiRenderer.renderState,
                0,
                0
            );

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(viewScale, viewScale);
            graphics.accept(guiGraphics);
            guiGraphics.pose().popMatrix();

            projectedTarget = target;
            guiTargetOverride = target;

            RenderSystem.backupProjectionMatrix();
            gameRenderer.guiRenderer.render(gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            RenderSystem.restoreProjectionMatrix();

            guiTargetOverride = null;

            poseStack.pushPose();

            List<Vec3> vectors = new ArrayList<>();
            vectors.add(topLeft);
            vectors.add(topRight);
            vectors.add(bottomRight);
            vectors.add(bottomLeft);

            Vec3 combinedVec = new Vec3(0, 0, 0);
            for (Vec3 i : vectors) {
                combinedVec = combinedVec.add(i);
            }
            Vec3 middle = combinedVec.scale(1.0f / (float) vectors.size());
            Vec3 topLeft = middle.subtract(this.topLeft);
            Vec3 topRight = middle.subtract(this.topRight);
            Vec3 bottomRight = middle.subtract(this.bottomRight);
            Vec3 bottomLeft = middle.subtract(this.bottomLeft);

            poseStack.translate(middle.x(), middle.y(), middle.z());
            poseStack.pushPose();
            applyPoseStackTransformations.accept(poseStack);
            RenderProjectionUtil.renderTarget(target, source, poseStack, topLeft, topRight, bottomRight, bottomLeft);
            undoPoseStackTransformations.accept(poseStack);
            poseStack.popPose();
            poseStack.popPose();

            pool.unmarkUse(target, use);
        }
    }
}
