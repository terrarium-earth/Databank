package com.cmdpro.databank.instanceddimension;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.misc.PlayerDataUtil;
import com.cmdpro.databank.mixin.MinecraftServerAccessor;
import com.cmdpro.databank.mixin.WorldBorderAccessor;
import com.cmdpro.databank.networking.ModMessages;
import com.cmdpro.databank.networking.packet.AddDimensionS2CPacket;
import com.cmdpro.databank.networking.packet.RemoveDimensionS2CPacket;
import com.cmdpro.databank.registry.AttachmentTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.apache.commons.io.FileUtils;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class InstancedDimension {
    public static final Codec<InstancedDimension> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        LevelStem.CODEC.fieldOf("stem").forGetter((obj) -> obj.stem),
        Codec.BOOL.fieldOf("saves").forGetter((obj) -> obj.saves),
        Codec.BOOL.optionalFieldOf("deletesWhenNoPlayers", false).forGetter((obj) -> obj.deletesWhenNoPlayers)
    ).apply(instance, InstancedDimension::new));

    public Identifier id;
    public LevelStem stem;
    public boolean saves;
    public boolean deletesWhenNoPlayers;

    public InstancedDimension(LevelStem stem, boolean saves, boolean deletesWhenNoPlayers) {
        this.stem = stem;
        this.saves = saves;
        this.deletesWhenNoPlayers = deletesWhenNoPlayers;
    }

    public Instance create(String identifier) {
        ResourceKey<Level> key = ResourceKey.create(
            Registries.DIMENSION,
            Databank.locate(id.getNamespace() + "-" + id.getPath() + "-" + identifier)
        );

        return new Instance(id, key);
    }

    public Instance create() {
        return create(UUID.randomUUID().toString());
    }

    protected Supplier<AttachmentType<ArrayList<Instance>>> getAttachmentType() {
        return saves ? AttachmentTypeRegistry.INSTANCED_DIMENSIONS : AttachmentTypeRegistry.TEMP_INSTANCED_DIMENSIONS;
    }

    public static class Instance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter((obj) -> obj.dimension),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("key").forGetter((obj) -> obj.key)
        ).apply(instance, Instance::new));

        public Identifier dimension;
        public ResourceKey<Level> key;
        public ServerLevel level;

        public Instance(Identifier dimension, ResourceKey<Level> key) {
            this.dimension = dimension;
            this.key = key;
        }

        public InstancedDimension getInstancedDimension() {
            return InstancedDimensionManager.instanceddimensions.get(dimension);
        }

        public ServerLevel getOrCreateDimension(MinecraftServer server) {
            if (level == null) {
                level = createDimension(server);
            }
            return level;
        }

        protected ServerLevel createDimension(MinecraftServer server) {
            LevelStem stem = getInstancedDimension().stem;
            MinecraftServerAccessor accessor = ((MinecraftServerAccessor) server);
            WorldOptions worldoptions = server.getWorldGenSettings().options();
            long i = worldoptions.seed();
            long j = BiomeManager.obfuscateSeed(i);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, key.identifier());
            DerivedLevelData derivedleveldata = new DerivedLevelData(
                server.getWorldData(),
                server.getWorldData().overworldData()
            );
            ServerLevel instancedLevel = new ServerLevel(
                server,
                accessor.getExecutor(),
                accessor.getStorageSource(),
                derivedleveldata,
                dimensionKey,
                stem,
                server.getWorldData().isDebugWorld(),
                j,
                ImmutableList.of(),
                false
            );
            accessor.getLevels().put(dimensionKey, instancedLevel);
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(accessor.getLevels().get(key)));
            server.markWorldsDirty();
            ModMessages.sendToAllPlayers(new AddDimensionS2CPacket(key));
            var list = server.overworld().getData(getInstancedDimension().getAttachmentType());
            if (list.stream().noneMatch((k) -> k.key.equals(key))) {
                list.add(this);
            }
            server.overworld().setData(getInstancedDimension().getAttachmentType(), list);
            return instancedLevel;
        }

        public void removeDimension(MinecraftServer server) {
            MinecraftServerAccessor accessor = ((MinecraftServerAccessor) server);
            for (Player o : level.players()) {
                InstancedDimensionManager.teleportPlayerOut(server, o);
            }

            // TODO Should probably remove this;
            //  What it does is when a dimension is removed(by being temporary or deletesWhenNoPlayers), it moves all the players that were in it to the last dimension they were in
            //  Editing the players' data on disk is unsafe, and very error prone (especially parsing the position and dimensional data manually)\
            //  Relevant codecs: SavedPosition.MAP_CODEC and PlayerLastLocationData.CODEC
            for (String o : PlayerDataUtil.getAllOfflineUUIDS(server)) {
                PlayerDataUtil.modifyOfflinePlayerData(
                    server, o, (data) -> {
                        var dimensionTag = data.getString("Dimension");

                        if (dimensionTag.isPresent()) {
                            ResourceKey<Level> resourcekey = ResourceKey.create(
                                Registries.DIMENSION,
                                Identifier.parse(dimensionTag.get())
                            );
                            if (resourcekey.equals(key)) {
                                String attachmentsKey = Identifier.fromNamespaceAndPath(
                                    "neoforge",
                                    "attachments"
                                ).toString();
                                var attachments = data.getCompound(attachmentsKey);
                                if (attachments.isPresent()) {
                                    Identifier lastLocationDataKey = Identifier.fromNamespaceAndPath(
                                        Databank.MOD_ID,
                                        "last_location_data"
                                    );
                                    var lastLocationData = attachments.get().getCompound(lastLocationDataKey.toString());
                                    if (lastLocationData.isPresent()) {
                                        String level = lastLocationData.get().getString("level").get();
                                        ListTag pos = lastLocationData.get().getList("pos").get();
                                        data.putString("Dimension", level);
                                        data.put("Pos", pos);
                                        return Optional.of(data);
                                    }
                                }
                            }
                        }
                        return Optional.empty();
                    }
                );
            }
            if (accessor.getLevels().containsKey(key)) {
                try {
                    ServerLevel level = accessor.getLevels().get(key);
                    NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
                    accessor.getLevels().remove(key);
                    level.close();
                    server.markWorldsDirty();
                    Path path = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(key).toRealPath();
                    FileUtils.deleteDirectory(new File(path.toString()));
                    ModMessages.sendToAllPlayers(new RemoveDimensionS2CPacket(key));
                    var list = server.overworld().getData(getInstancedDimension().getAttachmentType());
                    list.removeIf((i) -> i.key.equals(key));
                    server.overworld().setData(getInstancedDimension().getAttachmentType(), list);
                } catch (Exception e) {
                }
            }
        }
    }
}
