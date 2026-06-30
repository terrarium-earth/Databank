package com.cmdpro.databank.rendering;

import com.cmdpro.databank.Databank;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.SequencedMap;

@EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
public class RenderHandler {
    static MultiBufferSource.BufferSource bufferSource = null;

    public static Matrix4f matrix4f;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
        matrix4f = new Matrix4f(RenderSystem.getModelViewMatrix());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
        if (ShaderHelper.shouldUseAlternateRendering()) {
            RenderSystem.getModelViewStack().pushMatrix().set(RenderHandler.matrix4f);
            for (RenderType i : RenderTypeHandler.normalRenderTypes) {
                createBufferSource().endBatch(i);
            }
            for (RenderType i : RenderTypeHandler.particleRenderTypes) {
                createBufferSource().endBatch(i);
            }
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterWeather event) {
        if (!ShaderHelper.shouldUseAlternateRendering()) {
            for (RenderType i : RenderTypeHandler.normalRenderTypes) {
                createBufferSource().endBatch(i);
            }
            for (RenderType i : RenderTypeHandler.particleRenderTypes) {
                createBufferSource().endBatch(i);
            }
        }
    }

    public static MultiBufferSource.BufferSource createBufferSource() {
        if (bufferSource == null) {
            SequencedMap<RenderType, ByteBufferBuilder> buffers = new Object2ObjectLinkedOpenHashMap<>();
            for (RenderType i : RenderTypeHandler.renderTypes) {
                buffers.put(i, new ByteBufferBuilder(i.bufferSize()));
            }
            bufferSource = MultiBufferSource.immediateWithBuffers(buffers, new ByteBufferBuilder(256));
        }
        return bufferSource;
    }
}