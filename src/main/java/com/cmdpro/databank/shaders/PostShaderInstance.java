package com.cmdpro.databank.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PostShaderInstance implements PostChain.TargetBundle, Closeable {
    private static RenderTarget depthBackupTarget;
    private final Map<String, UniformBufferBuilder> uniformBuilders;

    private @Nullable PostChain postChain;

    public float time;
    public List<PostPass> passes;
    private boolean active;

    public final Map<Identifier, @Nullable ResourceHandle<RenderTarget>> targets = new HashMap<>();

    public PostShaderInstance(Map<String, UniformBufferInfo> uniformBuilders, Identifier... targets) {
        this.uniformBuilders = uniformBuilders
            .entrySet()
            .stream()
            .map((entry) -> Pair.of(
                entry.getKey(),
                new UniformBufferBuilder(entry::getKey, entry.getValue())
            ))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        this.targets.put(LevelTargetBundle.MAIN_TARGET_ID, null);

        for (Identifier targetId : targets) {
            this.targets.put(targetId, null);
        }
    }

    public abstract Identifier getShaderLocation();

    @Override
    public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
        if (targets.containsKey(id)) {
            targets.put(id, handle);
        } else {
            throw new IllegalArgumentException("No target with ID " + id);
        }
    }

    @Override
    public @Nullable ResourceHandle<RenderTarget> get(Identifier id) {
        return targets.get(id);
    }

    public Set<Identifier> targets() {
        return targets.keySet();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (!active) {
            time = 0;
        }
        this.active = active;
    }

    public void queueRemoval() {
        if (!PostShaderManager.removalQueue.contains(this)) {
            PostShaderManager.removalQueue.add(this);
        }
    }

    public void process() {
        if (postChain == null) {
            postChain = Minecraft.getInstance().getShaderManager().getPostChain(
                getShaderLocation(),
                Stream.concat(LevelTargetBundle.MAIN_TARGETS.stream(), targets().stream()).collect(Collectors.toSet())
            );
        }

        if (postChain != null) {
            passes = postChain.passes;

            beforeProcess();

            if (active) {
                int index = 0;

                for (PostPass pass : passes) {
                    Map<String, GpuBuffer> uniformOverrides = new HashMap<>();

                    for (Map.Entry<String, GpuBuffer> entry : pass.customUniforms.entrySet()) {
                        var builder = uniformBuilders.get(entry.getKey());

                        if (builder != null) {
                            var buffers = builder.buffers(postChain);
                            var buffer = buffers.get(index);

                            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                            try (GpuBuffer.MappedView view = commandEncoder.mapBuffer(buffer.currentBuffer(), false, true)) {
                                builder.builder().accept(this, Std140Builder.intoBuffer(view.data()));
                                uniformOverrides.put(entry.getKey(), buffer.currentBuffer());
                            }
                        }
                    }

                    pass.customUniforms.putAll(uniformOverrides);

                    ++index;
                }

                processPostChain();
                afterProcess();
            }
        }
    }

    public void processPostChain() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        getDepthBackupTarget().copyDepthFrom(mainTarget);
 /*       RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();*/

        FrameGraphBuilder frame = new FrameGraphBuilder();
        targets.put(LevelTargetBundle.MAIN_TARGET_ID, frame.importExternal("main", mainTarget));

        setupFrameGraph(frame);

        postChain.addToFrame(frame, mainTarget.width, mainTarget.height, this);
        frame.execute(minecraft.gameRenderer.resourcePool);
        mainTarget.copyDepthFrom(getDepthBackupTarget());
    }

    protected static RenderTarget getDepthBackupTarget() {
        if (depthBackupTarget == null) {
            depthBackupTarget = new MainTarget(
                Minecraft.getInstance().getMainRenderTarget().width,
                Minecraft.getInstance().getMainRenderTarget().height
            );
        }
        return depthBackupTarget;
    }

    public void tick() {
        time += 1 / 20f;
    }

    public void beforeProcess() {
    }

    public void afterProcess() {
    }

    public float getTime() {
        return time + (Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true) / 20f);
    }

    public void setupFrameGraph(FrameGraphBuilder frameGraphBuilder) {
    }

    @Override
    public void close() {
        for (Map.Entry<String, UniformBufferBuilder> entry : uniformBuilders.entrySet()) {
            entry.getValue().close();
        }
    }

    public record UniformBufferInfo(
        BiConsumer<PostShaderInstance, Std140Builder> builder,
        Std140SizeCalculator sizeCalculator
    ) {
        public MappableRingBuffer ringBuffer(Supplier<String> label) {
            return new MappableRingBuffer(
                label,
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM,
                sizeCalculator().get()
            );
        }
    }

    public static final class UniformBufferBuilder implements Closeable {
        private final Supplier<String> label;
        private final UniformBufferInfo builder;
        private List<MappableRingBuffer> buffers;

        public UniformBufferBuilder(Supplier<String> label, UniformBufferInfo builder) {
            this.label = label;
            this.builder = builder;
        }

        public BiConsumer<PostShaderInstance, Std140Builder> builder() {
            return builder.builder();
        }

        public List<MappableRingBuffer> buffers(PostChain chain) {
            if (buffers == null) {
                buffers = chain.passes
                    .stream()
                    .map((p) -> builder.ringBuffer(label))
                    .toList();
            }

            return buffers;
        }

        @Override
        public void close() {
            if (buffers == null) {
                return;
            }

            for (MappableRingBuffer buffer : buffers) {
                buffer.close();
            }
        }
    }
}
