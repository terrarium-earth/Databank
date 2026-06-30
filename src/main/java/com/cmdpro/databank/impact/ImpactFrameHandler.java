package com.cmdpro.databank.impact;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.config.DatabankClientConfig;
import com.cmdpro.databank.misc.FloatGradient;
import com.cmdpro.databank.mixin.client.BufferSourceMixin;
import com.cmdpro.databank.shaders.PostShaderManager;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

public class ImpactFrameHandler {
    public static final ImpactShader defaultShader = new ImpactShader();
    private static final List<ImpactData> impactData = new ArrayList<>();

    public static ImpactFrame impactFrame;

    // TODO Migrate these external post pass targets to internal ones
    private static RenderTarget impactTarget;
    private static RenderTarget frozenImpactTarget;

    private static MultiBufferSource.BufferSource bufferSource;

    private static boolean reset;

    public static void resize(int width, int height) {
        getImpactTarget().resize(width, height);
        getFrozenImpactTarget().resize(width, height);
    }

    public static ImpactFrame addImpact(int ticks, ImpactRender frozenRender, ImpactRender dynamicRender, FloatGradient alpha, boolean merge, ImpactShader shader) {
        impactData.add(new ImpactData(frozenRender, dynamicRender, merge));
        reset = false;
        if (impactFrame != null) {
            impactFrame.shader.setActive(false);
        }
        impactFrame = new ImpactFrame(ticks, shader, alpha);
        shader.setActive(true);
        return impactFrame;
    }

    public static ImpactFrame addImpact(int ticks, ImpactRender frozenRender, ImpactRender dynamicRender, FloatGradient alpha) {
        return addImpact(ticks, frozenRender, dynamicRender, alpha, false, defaultShader);
    }

    public static ImpactFrame addImpact(int ticks, ImpactRender frozenRender, ImpactRender dynamicRender, FloatGradient alpha, boolean merge) {
        return addImpact(ticks, frozenRender, dynamicRender, alpha, merge, defaultShader);
    }

    public static ImpactFrame addImpact(int ticks, ImpactRender frozenRender, ImpactRender dynamicRender, FloatGradient alpha, ImpactShader shader) {
        return addImpact(ticks, frozenRender, dynamicRender, alpha, false, shader);
    }

    public static ImpactFrame withFlashes(ImpactFrame original, float[] flashes, float flashTime) {
        withFlashes(original.alpha, original.startTicks, flashes, flashTime);
        return original;
    }

    public static FloatGradient withFlashes(FloatGradient original, float seconds, float[] flashes, float flashTime) {
        if (!DatabankClientConfig.allowFlashOnImpactVisuals) {
            return original;
        }

        float flash = flashTime / seconds;
        for (float i : flashes) {
            float progress = i / seconds;
            float alpha = original.getValue(progress);
            original.addPoint(0f, progress, true);
            original.addPoint(alpha, progress + flash, true);
        }

        return original;
    }

    public static FloatGradient withFlashes(FloatGradient original, int ticks, float[] flashes, float flashTime) {
        return withFlashes(original, (float) ticks / 20f, flashes, flashTime);
    }

    protected static RenderTarget getImpactTarget() {
        if (impactTarget == null) {
            int width = Minecraft.getInstance().getMainRenderTarget().width;
            int height = Minecraft.getInstance().getMainRenderTarget().height;
            impactTarget = new MainTarget(width, height);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                impactTarget.getColorTexture(),
                0,
                impactTarget.getDepthTexture(),
                1.0
            );
        }
        return impactTarget;
    }

    protected static RenderTarget getFrozenImpactTarget() {
        if (frozenImpactTarget == null) {
            int width = Minecraft.getInstance().getMainRenderTarget().width;
            int height = Minecraft.getInstance().getMainRenderTarget().height;
            frozenImpactTarget = new MainTarget(width, height);
        }

        return frozenImpactTarget;
    }

    private static MultiBufferSource.BufferSource initBuffers(MultiBufferSource.BufferSource original) {
        BufferSourceMixin mixin = (BufferSourceMixin) original;
        var fallback = mixin.getSharedBuffer();
        var fixedBuffers = mixin.getFixedBuffers();
        return MultiBufferSource.immediateWithBuffers(fixedBuffers, fallback);
    }

    protected static void renderData(ImpactData data, boolean dynamic, RenderLevelStageEvent event) {
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        RenderTarget impactTarget = dynamic ? getImpactTarget() : getFrozenImpactTarget();
        if (!data.merge) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                impactTarget.getColorTexture(),
                0,
                impactTarget.getDepthTexture(),
                1.0
            );
        }
        event.getPoseStack().pushPose();
        Vec3 pos = camera.pos;
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-pos.x(), -pos.y(), -pos.z());
        Minecraft minecraft = Minecraft.getInstance();
        if (bufferSource == null) {
            bufferSource = initBuffers(minecraft.renderBuffers().bufferSource());
        }
        DeltaTracker deltaTracker = minecraft.getDeltaTracker();
        (dynamic ? data.renderDynamic : data.renderFrozen).renderFrame(
            impactTarget,
            bufferSource,
            event.getPoseStack(),
            deltaTracker,
            camera,
            (int) deltaTracker.getRealtimeDeltaTicks(),
            impactFrame.getProgress(deltaTracker.getGameTimeDeltaPartialTick(true))
        );
        bufferSource.endBatch();
        event.getPoseStack().popPose();
    }

    public static class ImpactFrame {
        public int startTicks;
        public int ticks;
        public ImpactShader shader;
        public FloatGradient alpha;
        protected List<ImpactData> impactData = new ArrayList<>();

        protected ImpactFrame(int startTicks, ImpactShader shader, FloatGradient alpha) {
            this.startTicks = startTicks;
            this.ticks = startTicks;
            this.shader = shader;
            this.alpha = alpha;
        }

        protected void tick() {
            ticks--;
        }

        public ImpactFrame withFlashes(float[] flashes, float flashTime) {
            ImpactFrameHandler.withFlashes(this, flashes, flashTime);
            return this;
        }

        public float getProgress(float partialTick) {
            float maxProgress = (float) ImpactFrameHandler.impactFrame.startTicks / 20f;
            return ((float) (startTicks - ticks) / 20f) / maxProgress;
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
    protected static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            if (impactFrame != null) {
                impactFrame.tick();
                if (impactFrame.ticks <= 0) {
                    impactFrame.shader.setActive(false);
                    impactFrame = null;
                    reset = false;
                }
            }
        }

        @SubscribeEvent
        public static void onRender(RenderLevelStageEvent.AfterWeather event) {
            if (impactFrame != null) {
                RenderTarget frozenTarget = getFrozenImpactTarget();
                if (!impactData.isEmpty()) {
                    RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                        frozenTarget.getColorTexture(),
                        0,
                        frozenTarget.getDepthTexture(),
                        1.0
                    );

                    for (ImpactData data : impactData) {
                        renderData(data, false, event);
                    }
                    impactFrame.impactData.addAll(impactData);
                    impactData.clear();
                }
                if (!impactFrame.shader.isActive()) {
                    impactFrame.shader.setActive(true);
                }
                if (impactFrame.alpha.getValue(impactFrame.shader.getTime()) <= 0.1) {
                    reset = true;
                } else if (reset) {
                    RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                        frozenTarget.getColorTexture(),
                        0,
                        frozenTarget.getDepthTexture(),
                        1.0
                    );

                    for (ImpactData data : impactData) {
                        renderData(data, false, event);
                    }
                    reset = false;
                }
                RenderTarget target = getImpactTarget();

                RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    target.getColorTexture(),
                    0,
                    target.getDepthTexture(),
                    1.0
                );
                target.copyDepthFrom(frozenTarget);
                for (ImpactData i : impactFrame.impactData) {
                    renderData(i, true, event);
                }
            }
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
    protected static class ModEvents {
        @SubscribeEvent
        public static void doSetup(FMLClientSetupEvent event) {
            PostShaderManager.addShader(defaultShader);
        }
    }

    public interface ImpactRender {
        void renderFrame(RenderTarget target, MultiBufferSource bufferSource, PoseStack poseStack, DeltaTracker tracker, CameraRenderState camera, int renderTicks, float progress);
    }

    protected record ImpactData(ImpactRender renderFrozen, ImpactRender renderDynamic, boolean merge) {
    }
}
