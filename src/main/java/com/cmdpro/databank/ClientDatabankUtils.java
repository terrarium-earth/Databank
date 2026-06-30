package com.cmdpro.databank;

import com.cmdpro.databank.config.DatabankClientConfig;
import com.cmdpro.databank.mixin.client.BufferSourceMixin;
import com.cmdpro.databank.mixin.client.RenderBuffersMixin;
import com.cmdpro.databank.rendering.ShaderHelper;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

import java.util.SequencedMap;

public class ClientDatabankUtils {
    public static void updateWorld() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        if (ShaderHelper.isSodiumOrSimilarActive() || DatabankClientConfig.forceAlternateChunkReload) {
            int viewDistance = Minecraft.getInstance().options.renderDistance().get();
            int max = Minecraft.getInstance().level.getMaxSectionY();
            int min = Minecraft.getInstance().level.getMinSectionY();
            ChunkPos playerChunkPos = Minecraft.getInstance().player.chunkPosition();
            for (int x = -viewDistance; x < viewDistance; x++) {
                for (int z = -viewDistance; z < viewDistance; z++) {
                    if (Minecraft.getInstance().level.hasChunk(playerChunkPos.x() + x, playerChunkPos.z() + z)) {
                        for (int y = min; y < max; y++) {
                            Minecraft.getInstance().levelRenderer.setSectionDirty(
                                playerChunkPos.x() + x,
                                y,
                                playerChunkPos.z() + z
                            );
                        }
                    }
                }
            }
        } else {
            for (SectionRenderDispatcher.RenderSection i : Minecraft.getInstance().levelRenderer.viewArea.sections) {
                i.setDirty(false);
            }
        }
    }

    public static void blitStretched(GuiGraphicsExtractor graphics, Identifier texture, int blitOffset, int x, int y, int u, int v, int width, int height, int screenWidth, int screenHeight, int textureWidth, int textureHeight) {
        int x2 = x + screenWidth;
        int y2 = y + screenHeight;
        float minU = (u + 0.0F) / (float) textureWidth;
        float maxU = (u + (float) width) / (float) textureWidth;
        float minV = (v + 0.0F) / (float) textureHeight;
        float maxV = (v + (float) height) / (float) textureHeight;
        graphics.blit(texture, x, y, x2, y2, maxU, minU, minV, maxV);
    }

    public static void blitStretched(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height, int screenWidth, int screenHeight) {
        blitStretched(graphics, texture, 0, x, y, u, v, width, height, screenWidth, screenHeight, 256, 256);
    }

    public static MultiBufferSource.BufferSource createBufferSourceCopy(BufferSourceCreation create, MultiBufferSource.BufferSource original) {
        BufferSourceMixin mixin = (BufferSourceMixin) original;
        SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = mixin.getFixedBuffers();
        ByteBufferBuilder sharedBuffer = mixin.getSharedBuffer();
        return create.create(fixedBuffers, sharedBuffer);
    }

    public static MultiBufferSource.BufferSource createBufferSourceCopyFrom(MultiBufferSource.BufferSource original) {
        return createBufferSourceCopy(MultiBufferSource::immediateWithBuffers, original);
    }

    public static MultiBufferSource.BufferSource createMainBufferSourceCopy() {
        RenderBuffers renderBuffers = Minecraft.getInstance().renderBuffers();
        return createBufferSourceCopyFrom(ShaderHelper.needsBufferWorkaround() ? ((RenderBuffersMixin) renderBuffers).getBufferSource() : renderBuffers.bufferSource());
    }

    public static MultiBufferSource.BufferSource createMainBufferSourceCopy(BufferSourceCreation create) {
        RenderBuffers renderBuffers = Minecraft.getInstance().renderBuffers();
        return createBufferSourceCopy(
            create,
            ShaderHelper.needsBufferWorkaround() ? ((RenderBuffersMixin) renderBuffers).getBufferSource() : renderBuffers.bufferSource()
        );
    }

    public interface BufferSourceCreation {
        MultiBufferSource.BufferSource create(SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder sharedBuffer);
    }
}
