package com.cmdpro.databank.megastructures;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.music.MusicController;
import com.cmdpro.databank.music.MusicSerializer;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.HashMap;
import java.util.Map;

public class MegastructureManager extends SimpleJsonResourceReloadListener<JsonElement> {
    public static HashMap<Identifier, Megastructure> megastructures = new HashMap<>();

    public static MegastructureManager instance;
    protected MegastructureManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("databank/megastructures"));
    }
    public static MegastructureManager getOrCreateInstance() {
        if (instance == null) {
            instance = new MegastructureManager();
        }
        return instance;
    }
    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        megastructures = new HashMap<>();
        Databank.LOGGER.info("[DATABANK] Adding Databank Megastructures");
        for (Map.Entry<Identifier, JsonElement> i : pObject.entrySet()) {
            Identifier location = i.getKey();
            if (location.getPath().startsWith("_")) {
                continue;
            }

            try {
                JsonObject obj = i.getValue().getAsJsonObject();
                Megastructure megastructure = ICondition.getWithWithConditionsCodec(Megastructure.CONDITION_CODEC, JsonOps.INSTANCE, obj).orElse(null);
                megastructures.put(i.getKey(), megastructure);
            } catch (IllegalArgumentException | JsonParseException e) {
                Databank.LOGGER.error("[DATABANK ERROR] Parsing error loading megastructure type {}", location, e);
            }
        }
        Databank.LOGGER.info("[DATABANK] Loaded {} Megastructures", megastructures.size());
    }
    public static MusicSerializer serializer = new MusicSerializer();
}
