package com.cmdpro.databank.hidden.types;

import com.cmdpro.databank.ClientDatabankUtils;
import com.cmdpro.databank.DatabankRegistries;
import com.cmdpro.databank.DatabankUtils;
import com.cmdpro.databank.config.DatabankClientConfig;
import com.cmdpro.databank.hidden.*;
import com.cmdpro.databank.hidden.conditions.ActualPlayerCondition;
import com.cmdpro.databank.mixin.client.BlockColorsAccessor;
import com.cmdpro.databank.registry.HiddenTypeRegistry;
import com.cmdpro.databank.rendering.ShaderHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

public class BlockHiddenType extends HiddenTypeInstance.HiddenType<BlockHiddenType.BlockHiddenTypeInstance> {
    public static final BlockHiddenType INSTANCE = new BlockHiddenType();
    public static final MapCodec<BlockHiddenTypeInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("original").forGetter((type) -> type.original),
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hidden_as").forGetter((type) -> type.hiddenAs),
        ComponentSerialization.CODEC.optionalFieldOf("name_override").forGetter((type) -> type.nameOverride),
        HiddenSerializer.HIDDEN_CONDITION_CODEC.optionalFieldOf(
            "drop_original_loot_condition",
            new ActualPlayerCondition()
        ).forGetter((type) -> type.dropOriginalLootCondition),
        Codec.BOOL.optionalFieldOf(
            "should_overwrite_loot_if_hidden",
            true
        ).forGetter((type) -> type.shouldOverwriteLootIfHidden),
        StatePropertiesPredicate.CODEC.optionalFieldOf("should_apply_predicate").forGetter((type) -> type.shouldApplyPredicate),
        BlockHiddenOverride.CODEC.codec().listOf().optionalFieldOf(
            "overrides",
            new ArrayList<>()
        ).forGetter((type) -> type.overrides)
    ).apply(instance, BlockHiddenTypeInstance::new));


    @Override
    public MapCodec<BlockHiddenTypeInstance> codec() {
        return CODEC;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockHiddenTypeInstance> STREAM_CODEC = StreamCodec.of(
        (buf, val) -> {
            buf.writeResourceKey(BuiltInRegistries.BLOCK.getResourceKey(val.original).orElseThrow());
            buf.writeResourceKey(BuiltInRegistries.BLOCK.getResourceKey(val.hiddenAs).orElseThrow());
            buf.writeOptional(
                val.nameOverride,
                (buf2, val2) -> ComponentSerialization.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf2, val2)
            );
            buf.writeResourceKey(DatabankRegistries.HIDDEN_CONDITION_REGISTRY.getResourceKey(val.dropOriginalLootCondition.getSerializer()).orElseThrow());
            val.dropOriginalLootCondition.getSerializer().streamCodec().encode(buf, val.dropOriginalLootCondition);
            buf.writeBoolean(val.shouldOverwriteLootIfHidden);
            buf.writeOptional(val.shouldApplyPredicate, StatePropertiesPredicate.STREAM_CODEC);
            buf.writeCollection(
                val.overrides,
                (buf2, override) -> BlockHiddenOverride.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf2, override)
            );
        }, (buf) -> {
            ResourceKey<Block> originalKey = buf.readResourceKey(Registries.BLOCK);
            ResourceKey<Block> hiddenAsKey = buf.readResourceKey(Registries.BLOCK);
            Block original = BuiltInRegistries.BLOCK.getValueOrThrow(originalKey);
            Block hiddenAs = BuiltInRegistries.BLOCK.getValueOrThrow(hiddenAsKey);
            Optional<Component> nameOverride = buf.readOptional((buf2) -> ComponentSerialization.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf2));
            ResourceKey<HiddenCondition.Serializer<?>> dropConditionKey = buf.readResourceKey(DatabankRegistries.HIDDEN_CONDITION_REGISTRY_KEY);
            HiddenCondition.Serializer<?> dropConditionSerializer = DatabankRegistries.HIDDEN_CONDITION_REGISTRY.getValueOrThrow(
                dropConditionKey);
            HiddenCondition dropCondition = dropConditionSerializer.streamCodec().decode(buf);
            boolean shouldOverwriteLootIfHidden = buf.readBoolean();
            Optional<StatePropertiesPredicate> shouldApplyPredicate = buf.readOptional(StatePropertiesPredicate.STREAM_CODEC);
            List<BlockHiddenOverride> overrides = buf.readList((buf2) -> BlockHiddenOverride.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf2));
            return new BlockHiddenTypeInstance(
                original,
                hiddenAs,
                nameOverride,
                dropCondition,
                shouldOverwriteLootIfHidden,
                shouldApplyPredicate,
                overrides
            );
        }
    );

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BlockHiddenTypeInstance> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public void updateClient() {
        cache.clear();
        ClientDatabankUtils.updateWorld();
    }

    @Override
    public void onRecieveClient() {
        ClientHandler.updateBlockColors();
    }

    public static boolean isVisible(Block block, Player player) {
        if (player.level().isClientSide()) {
            return isVisibleClient(block);
        }
        return getHiddenBlock(block, player) == null;
    }

    public static boolean isVisible(BlockState block, Player player) {
        if (player.level().isClientSide()) {
            return isVisibleClient(block);
        }
        return getHiddenBlock(block, player) == null;
    }

    public static boolean isVisibleClient(Block block) {
        return getHiddenBlockClient(block) == null;
    }

    public static boolean isVisibleClient(BlockState block) {
        return getHiddenBlockClient(block) == null;
    }

    public static Block getHiddenBlock(Block block, Player player) {
        if (player.level().isClientSide()) {
            return getHiddenBlockClient(block);
        }
        return getHiddenBlock(block.defaultBlockState(), player);
    }

    public static Block getHiddenBlock(BlockState block, Player player) {
        if (player.level().isClientSide()) {
            return getHiddenBlockClient(block);
        }
        for (Hidden i : HiddenManager.hidden.values()) {
            if (i.type instanceof BlockHiddenTypeInstance type) {
                if (type.original == null || type.hiddenAs == null || i.condition == null) {
                    continue;
                }
                if (type.isHidden(block.getBlock(), player)) {
                    boolean applies = true;
                    if (type.shouldApplyPredicate.isPresent()) {
                        applies = type.shouldApplyPredicate.get().matches(block);
                    }
                    if (applies) {
                        BlockHiddenOverride override = findOverride(type, block);
                        return override != null ? override.hiddenAs : type.hiddenAs;
                    }
                } else if (type.matches(block.getBlock())) {
                    break;
                }
            }
        }
        return null;
    }

    public static boolean shouldDropOriginalBlock(Block block, Player player) {
        return shouldDropOriginalBlock(block.defaultBlockState(), player);
    }

    public static boolean shouldDropOriginalBlock(BlockState block, Player player) {
        for (Hidden i : HiddenManager.hidden.values()) {
            if (i.type instanceof BlockHiddenTypeInstance type) {
                if (type.original == null || type.hiddenAs == null || i.condition == null) {
                    continue;
                }
                if (type.isHidden(block.getBlock(), player) && type.shouldOverwriteLootIfHidden) {
                    return false;
                } else if (type.matches(block.getBlock())) {
                    boolean applies = true;
                    if (type.shouldApplyPredicate.isPresent()) {
                        applies = type.shouldApplyPredicate.get().matches(block);
                    }
                    if (applies) {
                        BlockHiddenOverride override = findOverride(type, block);
                        return override !=
                            null ? override.dropOriginalLootCondition.isUnlocked(player) : type.dropOriginalLootCondition.isUnlocked(
                            player);
                    }
                }
            }
        }
        return false;
    }

    public static Block getHiddenBlock(Block block) {
        return getHiddenBlock(block.defaultBlockState());
    }

    public static Block getHiddenBlock(BlockState block) {
        for (Hidden i : HiddenManager.hidden.values()) {
            if (i.type instanceof BlockHiddenTypeInstance type) {
                if (type.original == null || type.hiddenAs == null || i.condition == null) {
                    continue;
                }
                if (type.matches(block.getBlock())) {
                    boolean applies = true;
                    if (type.shouldApplyPredicate.isPresent()) {
                        applies = type.shouldApplyPredicate.get().matches(block);
                    }
                    if (applies) {
                        BlockHiddenOverride override = findOverride(type, block);
                        return override != null ? override.hiddenAs : type.hiddenAs;
                    }
                }
            }
        }
        return null;
    }

    public static Optional<Component> getHiddenBlockNameOverride(Block block) {
        return getHiddenBlockNameOverride(block.defaultBlockState());
    }

    public static Optional<Component> getHiddenBlockNameOverride(BlockState block) {
        for (Hidden i : HiddenManager.hidden.values()) {
            if (i.type instanceof BlockHiddenTypeInstance type) {
                if (type.original == null) {
                    continue;
                }
                if (type.matches(block.getBlock())) {
                    boolean applies = true;
                    if (type.shouldApplyPredicate.isPresent()) {
                        applies = type.shouldApplyPredicate.get().matches(block);
                    }
                    if (applies) {
                        BlockHiddenOverride override = findOverride(type, block);
                        return override != null ? override.nameOverride : type.nameOverride;
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static BlockHiddenOverride findOverride(BlockHiddenTypeInstance instance, BlockState state) {
        for (BlockHiddenOverride j : instance.overrides) {
            if (j.predicate.matches(state)) {
                return j;
            }
        }
        return null;
    }

    public static Block getHiddenBlockClient(Block block) {
        return getHiddenBlockClient(block.defaultBlockState());
    }

    public static Block getHiddenBlockClient(BlockState block) {
        Hidden hidden = cache.get(block);
        if (!cache.containsKey(block)) {
            for (Map.Entry<Identifier, Hidden> i : new HashMap<>(HiddenManager.hidden).entrySet()) {
                if (i.getValue().type instanceof BlockHiddenTypeInstance type) {
                    if (type.original == null || type.hiddenAs == null) {
                        continue;
                    }
                    if (type.matches(block.getBlock())) {
                        hidden = i.getValue();
                        break;
                    }
                }
            }
            cache.put(block, hidden);
        }
        if (hidden != null) {
            if (hidden.type instanceof BlockHiddenTypeInstance type) {
                if (type.isHiddenClient(block.getBlock())) {
                    boolean applies = true;
                    if (type.shouldApplyPredicate.isPresent()) {
                        applies = type.shouldApplyPredicate.get().matches(block);
                    }
                    if (applies) {
                        BlockHiddenOverride override = findOverride(type, block);
                        return override != null ? override.hiddenAs : type.hiddenAs;
                    }
                }
            }
        }
        return null;
    }

    private static HashMap<BlockState, Hidden> cache = new HashMap<>();

    public static class BlockHiddenTypeInstance extends HiddenTypeInstance<Block> {
        public Block original;
        public Block hiddenAs;
        public Optional<Component> nameOverride;
        public HiddenCondition dropOriginalLootCondition;
        public boolean shouldOverwriteLootIfHidden;
        public Optional<StatePropertiesPredicate> shouldApplyPredicate;
        public List<BlockHiddenOverride> overrides;

        public BlockHiddenTypeInstance(Block original, Block hiddenAs, Optional<Component> nameOverride, HiddenCondition dropOriginalLootCondition, boolean shouldOverwiteLootIfHidden, Optional<StatePropertiesPredicate> shouldApplyPredicate, List<BlockHiddenOverride> overrides) {
            this.original = original;
            this.hiddenAs = hiddenAs;
            this.nameOverride = nameOverride;
            this.dropOriginalLootCondition = dropOriginalLootCondition;
            this.shouldOverwriteLootIfHidden = shouldOverwiteLootIfHidden;
            this.shouldApplyPredicate = shouldApplyPredicate;
            this.overrides = overrides;
        }

        @Override
        public boolean matches(Block obj) {
            return obj.equals(original);
        }

        @Override
        public HiddenType<? extends HiddenTypeInstance<Block>> getType() {
            return INSTANCE;
        }
    }

    public static class BlockHiddenOverride {
        public StatePropertiesPredicate predicate;
        public Block hiddenAs;
        public Optional<Component> nameOverride;
        public HiddenCondition dropOriginalLootCondition;
        public boolean shouldOverwriteLootIfHidden;

        public BlockHiddenOverride(StatePropertiesPredicate predicate, Block hiddenAs, Optional<Component> nameOverride, HiddenCondition dropOriginalLootCondition, boolean shouldOverwiteLootIfHidden) {
            this.predicate = predicate;
            this.hiddenAs = hiddenAs;
            this.nameOverride = nameOverride;
            this.dropOriginalLootCondition = dropOriginalLootCondition;
            this.shouldOverwriteLootIfHidden = shouldOverwiteLootIfHidden;
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, BlockHiddenOverride> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                StatePropertiesPredicate.STREAM_CODEC.encode(buf, val.predicate);
                buf.writeResourceKey(BuiltInRegistries.BLOCK.getResourceKey(val.hiddenAs).orElseThrow());
                buf.writeOptional(
                    val.nameOverride,
                    (buf2, val2) -> ComponentSerialization.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf2, val2)
                );
                buf.writeResourceKey(DatabankRegistries.HIDDEN_CONDITION_REGISTRY.getResourceKey(val.dropOriginalLootCondition.getSerializer()).orElseThrow());
                val.dropOriginalLootCondition.getSerializer().streamCodec().encode(buf, val.dropOriginalLootCondition);
                buf.writeBoolean(val.shouldOverwriteLootIfHidden);
            }, (buf) -> {
                StatePropertiesPredicate predicate = StatePropertiesPredicate.STREAM_CODEC.decode(buf);
                ResourceKey<Block> hiddenAsKey = buf.readResourceKey(Registries.BLOCK);
                Block hiddenAs = BuiltInRegistries.BLOCK.getValueOrThrow(hiddenAsKey);
                Optional<Component> nameOverride = buf.readOptional((buf2) -> ComponentSerialization.STREAM_CODEC.decode(
                    (RegistryFriendlyByteBuf) buf2));
                ResourceKey<HiddenCondition.Serializer<?>> dropConditionKey = buf.readResourceKey(DatabankRegistries.HIDDEN_CONDITION_REGISTRY_KEY);
                HiddenCondition.Serializer<?> dropConditionSerializer = DatabankRegistries.HIDDEN_CONDITION_REGISTRY.getValueOrThrow(
                    dropConditionKey);
                HiddenCondition dropCondition = dropConditionSerializer.streamCodec().decode(buf);
                boolean shouldOverwriteLootIfHidden = buf.readBoolean();
                return new BlockHiddenOverride(
                    predicate,
                    hiddenAs,
                    nameOverride,
                    dropCondition,
                    shouldOverwriteLootIfHidden
                );
            }
        );
        public static final MapCodec<BlockHiddenOverride> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StatePropertiesPredicate.CODEC.fieldOf("predicate").forGetter((type) -> type.predicate),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hidden_as").forGetter((type) -> type.hiddenAs),
            ComponentSerialization.CODEC.optionalFieldOf("name_override").forGetter((type) -> type.nameOverride),
            HiddenSerializer.HIDDEN_CONDITION_CODEC.optionalFieldOf(
                "drop_original_loot_condition",
                new ActualPlayerCondition()
            ).forGetter((type) -> type.dropOriginalLootCondition),
            Codec.BOOL.optionalFieldOf(
                "should_overwrite_loot_if_hidden",
                true
            ).forGetter((type) -> type.shouldOverwriteLootIfHidden)
        ).apply(instance, BlockHiddenOverride::new));
    }

    private static class ClientHandler {
        static HashMap<Block, List<BlockTintSource>> overriden = new HashMap<>();

        public static void updateBlockColors() {
            if (!ShaderHelper.isSodiumOrSimilarActive() && !DatabankClientConfig.forceAlternateHiddenColors) {
                return;
            }
            BlockColors colors = Minecraft.getInstance().getBlockColors();
            HashMap<Block, List<WrappingData>> wrappingData = new HashMap<>();
            for (Hidden i : HiddenTypeRegistry.BLOCK.get().getHiddenOfType().values()) {
                if (i.type instanceof BlockHiddenTypeInstance instance) {
                    Block block = instance.original;
                    WrappingData data = new WrappingData(instance, instance.shouldApplyPredicate);
                    List<WrappingData> wrapData = wrappingData.getOrDefault(block, new ArrayList<>());
                    wrapData.add(data);
                    wrappingData.put(block, wrapData);
                    if (!overriden.containsKey(block)) {
                        var color = ((BlockColorsAccessor) colors).getSources().get(block);
                        if (color != null) {
                            overriden.put(block, color);
                        }
                    }
                }
            }
            List<Block> blocksNotWrapped = new ArrayList<>(overriden.keySet());
            for (Map.Entry<Block, List<WrappingData>> i : wrappingData.entrySet()) {
                var originalSources = overriden.get(i.getKey());
                var wrapped = createWrapped(i.getKey(), originalSources, i.getValue());
                ((BlockColorsAccessor) colors).getSources().remove(i.getKey());
                colors.register(wrapped, i.getKey());
                blocksNotWrapped.remove(i.getKey());
            }
            for (Block i : blocksNotWrapped) {
                ((BlockColorsAccessor) colors).getSources().remove(i);
                colors.register(overriden.get(i), i);
                overriden.remove(i);
            }
            Minecraft.getInstance().levelRenderer.allChanged();
        }

        public static List<BlockTintSource> createWrapped(Block originalBlock, List<BlockTintSource> originalSources, List<WrappingData> wrappingData) {
            BlockColors colors = Minecraft.getInstance().getBlockColors();
            return new WrappedTintSources(originalBlock, colors, originalSources, wrappingData);
        }

        private record WrappingData(BlockHiddenTypeInstance instance, Optional<StatePropertiesPredicate> shouldApplyPredicate) {
        }

        private record TintSourceReplacement(BlockTintSource source, BlockState state) {}

        private static class WrappedTintSources extends AbstractList<BlockTintSource> {
            private final Block originalBlock;
            private final BlockColors colors;
            private final List<BlockTintSource> originalSources;
            private final List<ClientHandler.WrappingData> wrappingData;

            public WrappedTintSources(Block originalBlock, BlockColors colors, List<BlockTintSource> originalSources, List<ClientHandler.WrappingData> wrappingData) {
                this.originalBlock = originalBlock;
                this.colors = colors;
                this.originalSources = originalSources;
                this.wrappingData = wrappingData;
            }

            @Override
            public BlockTintSource get(int i) {
                return new BlockTintSource() {
                    private TintSourceReplacement replacement(BlockState state) {
                        BlockHiddenTypeInstance finalType = null;
                        for (WrappingData i : wrappingData) {
                            boolean apply = true;
                            if (i.shouldApplyPredicate.isPresent()) {
                                apply = i.shouldApplyPredicate.get().matches(state);
                            }
                            if (apply) {
                                finalType = i.instance;
                            }
                        }

                        if (finalType != null) {
                            if (finalType.isHiddenClient(finalType.original)) {
                                Block hiddenAs = finalType.hiddenAs;
                                for (BlockHiddenOverride i : finalType.overrides) {
                                    if (i.predicate.matches(state)) {
                                        hiddenAs = i.hiddenAs;
                                    }
                                }
                                if (hiddenAs != finalType.original) {
                                    BlockState hiddenState = DatabankUtils.changeBlockType(state, hiddenAs);
                                    BlockTintSource source = colors.getTintSource(hiddenState, i);

                                    if (source == null) {
                                        return null;
                                    }

                                    return new TintSourceReplacement(source, hiddenState);
                                }
                            }
                        }

                        if (i >= originalSources.size()) {
                            return null;
                        }

                        return new TintSourceReplacement(originalSources.get(i), state);
                    }

                    @Override
                    public int color(BlockState state) {
                        TintSourceReplacement replacement = replacement(state);

                        return replacement == null ? 0xFFFFFFFF : replacement.source.color(replacement.state);
                    }

                    @Override
                    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                        TintSourceReplacement replacement = replacement(state);

                        return replacement == null ? 0xFFFFFFFF : replacement.source.colorInWorld(replacement.state, level, pos);
                    }

                    @Override
                    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                        TintSourceReplacement replacement = replacement(state);

                        return replacement == null ? 0xFFFFFFFF : replacement.source.colorAsTerrainParticle(replacement.state, level, pos);
                    }

                    @Override
                    public Set<Property<?>> relevantProperties() {
                        return colors.getColoringProperties(getHiddenBlockClient(originalBlock));
                    }
                };
            }

            @Override
            public int size() {
                List<BlockTintSource> biggestTintSourceList = colors.getTintSources(getHiddenBlockClient(originalBlock).defaultBlockState());

                for (WrappingData wrappingData : wrappingData) {
                    BlockState hiddenState = DatabankUtils.changeBlockType(originalBlock.defaultBlockState(), wrappingData.instance.hiddenAs);

                    var sources = colors.getTintSources(hiddenState);

                    if (sources.size() > biggestTintSourceList.size()) {
                        biggestTintSourceList = sources;
                    }
                }

                return biggestTintSourceList.size();
            }
        }
    }
}
