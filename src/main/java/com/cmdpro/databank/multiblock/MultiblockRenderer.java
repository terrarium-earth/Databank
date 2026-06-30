package com.cmdpro.databank.multiblock;

import com.cmdpro.databank.ClientDatabankUtils;
import com.cmdpro.databank.Databank;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
public class MultiblockRenderer {
    public static Rotation multiblockRotation;
    public static BlockPos multiblockPos;
    public static Multiblock multiblock;
    public static MultiBufferSource.BufferSource buffers;
    public static SubmitNodeStorage submitNodeStorage;
    public static FeatureRenderDispatcher featureRenderDispatcher;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void submitGeometry(SubmitCustomGeometryEvent event) {
        MultiblockRenderState renderState = event.getLevelRenderState().getRenderData(MultiblockRenderState.KEY);

        if (renderState != null) {
            CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
            event.getPoseStack().pushPose();

            event.getPoseStack().translate(
                -camera.pos.x(),
                -camera.pos.y(),
                -camera.pos.z()
            );

            MultiblockRenderer.submitMultiblocks(event.getPoseStack(), renderState, camera);

            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (featureRenderDispatcher != null) {
            featureRenderDispatcher.renderAllFeatures();
            buffers.endBatch();
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (MultiblockRenderer.multiblock != null) {
            if (MultiblockRenderer.multiblockPos != null) {
                if (MultiblockRenderer.multiblock.checkMultiblock(
                    mc.level,
                    MultiblockRenderer.multiblockPos,
                    MultiblockRenderer.multiblockRotation
                )) {
                    MultiblockRenderer.multiblock = null;
                    MultiblockRenderer.multiblockPos = null;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            if (MultiblockRenderer.multiblock != null) {
                if (MultiblockRenderer.multiblockPos == null) {
                    MultiblockRenderer.multiblockPos = event.getHitVec().getBlockPos().relative(event.getHitVec().getDirection());
                    MultiblockRenderer.multiblockRotation = MultiblockRenderer.getRotation();
                }
            }
        }
    }

    public static void submitMultiblocks(PoseStack stack, MultiblockRenderState state, CameraRenderState camera) {
        if (MultiblockRenderer.submitNodeStorage == null) {
            Minecraft minecraft = Minecraft.getInstance();
            RenderBuffers renderBuffers = minecraft.renderBuffers();

            submitNodeStorage = new SubmitNodeStorage();
            buffers = initBuffers(renderBuffers.bufferSource());

            featureRenderDispatcher = new FeatureRenderDispatcher(
                submitNodeStorage,
                minecraft.getModelManager(),
                buffers,
                minecraft.getAtlasManager(),
                renderBuffers.outlineBufferSource(),
                renderBuffers.crumblingBufferSource(),
                minecraft.font,
                minecraft.gameRenderer.getGameRenderState()
            );
        }

        var blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        for (MovingBlockRenderState movingBlockRenderState : state.movingBlockRenderStates) {
            submitNodeStorage.submitMovingBlock(stack, movingBlockRenderState);
        }

        for (BlockEntityRenderState blockEntityRenderState : state.blockEntityRenderStates) {
            blockEntityRenderDispatcher.submit(blockEntityRenderState, new PoseStack(), submitNodeStorage, camera);
        }
    }

    @NotNull
    public static Rotation getRotation() {
        Rotation rot = Rotation.NONE;
        if (Minecraft.getInstance().player.getDirection().equals(Direction.EAST)) {
            rot = Rotation.CLOCKWISE_90;
        }
        if (Minecraft.getInstance().player.getDirection().equals(Direction.SOUTH)) {
            rot = Rotation.CLOCKWISE_180;
        }
        if (Minecraft.getInstance().player.getDirection().equals(Direction.WEST)) {
            rot = Rotation.COUNTERCLOCKWISE_90;
        }
        return rot;
    }

    private static void extractMultiblockState(Multiblock multiblock, MultiblockRenderState renderState, ClientLevel level, BlockPos pos, float partialTicks, Rotation rotation) {
        for (List<List<Multiblock.PredicateAndPos>> i : multiblock.getStates()) {
            for (List<Multiblock.PredicateAndPos> j : i) {
                for (Multiblock.PredicateAndPos k : j) {
                    if (pos != null) {
                        BlockState state = Minecraft.getInstance().level.getBlockState(k.offset.rotate(rotation).offset(
                            pos));
                        boolean stateMatches = k.predicate.isSame(state, rotation);
                        if (!stateMatches) {
                            extractBlockState(
                                multiblock,
                                renderState,
                                level,
                                partialTicks,
                                k.predicate.getVisual().rotate(Minecraft.getInstance().level, k.offset, rotation),
                                k.offset,
                                k.offset.rotate(rotation).offset(pos)
                            );
                        }
                    } else {
                        extractBlockState(
                            multiblock,
                            renderState,
                            level,
                            partialTicks,
                            k.predicate.getVisual().rotate(Minecraft.getInstance().level, k.offset, rotation),
                            k.offset,
                            k.offset.rotate(rotation)
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        var state = new MultiblockRenderState();
        var level = event.getLevel();
        var partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        if (multiblock != null) {
            if (multiblockPos == null) {
                if (Minecraft.getInstance().hitResult instanceof BlockHitResult result) {
                    extractMultiblockState(
                        multiblock,
                        state,
                        level,
                        result.getBlockPos().relative(result.getDirection()),
                        partialTick,
                        getRotation()
                    );
                }
            } else {
                extractMultiblockState(multiblock, state, level, multiblockPos, partialTick, multiblockRotation);
            }
        }

        event.getRenderState().setRenderData(MultiblockRenderState.KEY, state);
    }

    private static void extractBlockState(Multiblock multiblock, MultiblockRenderState state, ClientLevel level, float partialTicks, BlockState block, BlockPos pos, BlockPos worldPos) {
        var blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        // TODO Render fluids
        //  Fluids are rendered with the chunk and not with moving blocks, so we'd need to change some of this handling

        if (block.getRenderShape() == RenderShape.MODEL) {
            MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
            movingBlockRenderState.randomSeedPos = pos;
            movingBlockRenderState.blockPos = pos;
            movingBlockRenderState.blockState = block;
            movingBlockRenderState.biome = level.getBiome(pos);
            movingBlockRenderState.cardinalLighting = level.cardinalLighting();
            movingBlockRenderState.lightEngine = level.getLightEngine();

            state.movingBlockRenderStates.add(movingBlockRenderState);
        }

        if (block.getBlock() instanceof EntityBlock entityBlock) {
            var be = multiblock.blockEntityCache.computeIfAbsent(
                pos.immutable(),
                p -> entityBlock.newBlockEntity(p, block)
            );

            if (be != null) {
                be.setLevel(Minecraft.getInstance().level);

                // fake cached state in case the renderer checks it as we don't want to query the actual world
                be.setBlockState(block);

                var blockEntityRenderState = blockEntityRenderDispatcher.tryExtractRenderState(
                    be,
                    partialTicks,
                    null,
                    null
                );

                if (blockEntityRenderState != null) {
                    state.blockEntityRenderStates.add(blockEntityRenderState);
                }
            }
        }
    }

    private static MultiBufferSource.BufferSource initBuffers(MultiBufferSource.BufferSource original) {
        return ClientDatabankUtils.createMainBufferSourceCopy((fixedBuffers, sharedBuffer) -> {
            SequencedMap<RenderType, ByteBufferBuilder> remapped = new Object2ObjectLinkedOpenHashMap<>();
            for (Map.Entry<RenderType, ByteBufferBuilder> e : fixedBuffers.entrySet()) {
                remapped.put(HologramRenderType.remap(e.getKey()), e.getValue());
            }
            return new HologramBuffers(sharedBuffer, remapped);
        });
    }

    public static class MultiblockRenderState {
        public static final ContextKey<MultiblockRenderState> KEY = new ContextKey<>(Databank.locate("multiblocks"));

        public List<MovingBlockRenderState> movingBlockRenderStates;
        public List<BlockEntityRenderState> blockEntityRenderStates;
    }

    private static class HologramBuffers extends MultiBufferSource.BufferSource {
        protected HologramBuffers(ByteBufferBuilder fallback, SequencedMap<RenderType, ByteBufferBuilder> layerBuffers) {
            super(fallback, layerBuffers);
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return super.getBuffer(HologramRenderType.remap(type));
        }
    }

    public static class HologramRenderType extends RenderType {
        private static final Map<RenderPipeline, RenderPipeline> remappedPipelines = new IdentityHashMap<>();
        private static final Map<RenderType, RenderType> remappedTypes = new IdentityHashMap<>();

        private HologramRenderType(RenderType original) {
            super(
                String.format("%s_%s_hologram", original.toString(), Databank.MOD_ID),
                remapSetup(original)
            );
        }

        public static RenderType remap(RenderType in) {
            if (in instanceof HologramRenderType) {
                return in;
            } else {
                return remappedTypes.computeIfAbsent(in, HologramRenderType::new);
            }
        }

        private static RenderSetup remapSetup(RenderType original) {
            RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(tryRemap(original.pipeline()));

            var state = original.state;

            if (state.useLightmap) {
                builder.useLightmap();
            }

            if (state.useOverlay) {
                builder.useOverlay();
            }

            for (Map.Entry<String, RenderSetup.TextureBinding> entry : state.textures.entrySet()) {
                builder.withTexture(entry.getKey(), entry.getValue().location(), entry.getValue().sampler());
            }

            if (original.affectsCrumbling()) {
                builder.affectsCrumbling();
            }

            return builder
                .setTextureTransform(state.textureTransform)
                .setOutputTarget(original.outputTarget())
                .setOutline(RenderSetup.OutlineProperty.NONE)
                .sortOnUpload()
                .bufferSize(original.bufferSize())
                .setLayeringTransform(state.layeringTransform)
                .createRenderSetup();
        }

        private static RenderPipeline remap(RenderPipeline in) {
            return remappedPipelines.computeIfAbsent(
                in,
                (pipeline) -> RenderPipeline.builder(pipeline.toBuilder().buildSnippet())
                    .withDepthStencilState(Optional.empty())
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .build()
            );
        }

        private static RenderPipeline tryRemap(RenderPipeline in) {
            if (in.getDepthStencilState() != null) {
                return remap(in);
            }

            if (in.getColorTargetState().blendFunction().isEmpty()) {
                return remap(in);
            }

            return in;
        }
    }
}
