package com.cmdpro.databank.instanceddimension;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.registry.AttachmentTypeRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = Databank.MOD_ID)
public class InstancedDimensionManager extends SimpleJsonResourceReloadListener<JsonElement> {
    public static HashMap<Identifier, InstancedDimension> instanceddimensions = new HashMap<>();

    public static InstancedDimensionManager instance;
    protected InstancedDimensionManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("databank/instanced_dimensions"));
    }
    public static InstancedDimensionManager getOrCreateInstance() {
        if (instance == null) {
            instance = new InstancedDimensionManager();
        }
        return instance;
    }
    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        instanceddimensions = new HashMap<>();
        Databank.LOGGER.info("[DATABANK] Adding Databank Instanced Dimensions");
        for (Map.Entry<Identifier, JsonElement> i : pObject.entrySet()) {
            Identifier location = i.getKey();
            if (location.getPath().startsWith("_")) {
                continue;
            }

            try {
                JsonObject obj = i.getValue().getAsJsonObject();
                InstancedDimension dimension = InstancedDimension.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, getRegistryLookup()), obj).getOrThrow();
                dimension.id = i.getKey();
                instanceddimensions.put(i.getKey(), dimension);
            } catch (IllegalArgumentException | JsonParseException e) {
                Databank.LOGGER.error("[DATABANK ERROR] Parsing error loading instanced dimension type {}", location, e);
            }
        }
        Databank.LOGGER.info("[DATABANK] Loaded {} Instanced Dimensions", instanceddimensions.size());
    }
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        List<InstancedDimension.Instance> instances = getInstances(overworld);
        List<InstancedDimension.Instance> toRemove = new ArrayList<>();
        for (InstancedDimension.Instance i : instances) {
            if (i.level != null) {
                if (i.getInstancedDimension().deletesWhenNoPlayers && i.level.players().isEmpty()) {
                    toRemove.add(i);
                }
            }
        }
        for (InstancedDimension.Instance i : toRemove) {
            i.removeDimension(event.getServer());
        }
    }
    @SubscribeEvent
    public static void onPlayerTravel(EntityTravelToDimensionEvent event) {
        Level level = event.getEntity().level();

        if (!(event.getEntity() instanceof Player) || level.isClientSide()) {
            return;
        }

        ResourceKey<Level> to = event.getDimension();
        ResourceKey<Level> from = level.dimension();
        ServerLevel overworld = level.getServer().overworld();
        List<InstancedDimension.Instance> instances = getInstances(overworld);
        boolean inDimension = false;
        boolean wasInDimension = false;
        for (InstancedDimension.Instance i : instances) {
            if (i.level != null && to.equals(i.key)) {
                inDimension = true;
            }
            if (i.level != null && from.equals(i.key)) {
                wasInDimension = true;
            }
        }
        if (inDimension && !wasInDimension) {
            event.getEntity().setData(AttachmentTypeRegistry.LAST_LOCATION_DATA, Optional.of(new PlayerLastLocationData(from, event.getEntity().position())));
        }
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Level overworld = event.getServer().overworld();
        List<InstancedDimension.Instance> instances = overworld.getData(AttachmentTypeRegistry.INSTANCED_DIMENSIONS);
        for (InstancedDimension.Instance i : instances.stream().toList()) {
            i.getOrCreateDimension(event.getServer());
        }
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Level overworld = event.getServer().overworld();
        List<InstancedDimension.Instance> instances = overworld.getData(AttachmentTypeRegistry.TEMP_INSTANCED_DIMENSIONS);
        for (InstancedDimension.Instance i : instances.stream().toList()) {
            i.removeDimension(event.getServer());
        }
    }
    @SubscribeEvent
    public static void playerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (server.isSingleplayerOwner(player.nameAndId())) {
            Level overworld = server.overworld();
            List<InstancedDimension.Instance> instances = overworld.getData(AttachmentTypeRegistry.TEMP_INSTANCED_DIMENSIONS);
            if (instances.stream().anyMatch((i) -> i.key.equals(player.level().dimension()))) {
                teleportPlayerOut(server, player);
            }
        }
    }
    public static void teleportPlayerOut(MinecraftServer server, Player player) {
        Optional<PlayerLastLocationData> data = player.getData(AttachmentTypeRegistry.LAST_LOCATION_DATA);
        ResourceKey<Level> outside = Level.OVERWORLD;
        Vec3 pos = Vec3.ZERO;
        if (data.isPresent()) {
            outside = data.get().level;
            pos = data.get().pos;
        }
        player.teleportTo(server.getLevel(outside), pos.x(), pos.y(), pos.z(), Set.of(), player.yRotO, player.xRotO, true);
    }
    public static List<InstancedDimension.Instance> getInstances(Level level) {
        List<InstancedDimension.Instance> instances = new ArrayList<>(level.getData(AttachmentTypeRegistry.TEMP_INSTANCED_DIMENSIONS));
        instances.addAll(level.getData(AttachmentTypeRegistry.INSTANCED_DIMENSIONS));
        return instances;
    }
}
