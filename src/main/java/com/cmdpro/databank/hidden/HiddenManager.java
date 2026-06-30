package com.cmdpro.databank.hidden;

import com.cmdpro.databank.Databank;
import com.google.gson.*;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HiddenManager extends SimpleJsonResourceReloadListener<JsonElement> {
    public static HiddenManager instance;
    protected HiddenManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("databank/hidden"));
    }
    public static HiddenManager getOrCreateInstance() {
        if (instance == null) {
            instance = new HiddenManager();
        }
        return instance;
    }
    public static Map<Identifier, Hidden> hidden = new HashMap<>();
    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<Identifier, Hidden> hidden = new HashMap<>();
        Databank.LOGGER.info("[DATABANK] Adding Databank Hidden Entries");
        for (Map.Entry<Identifier, JsonElement> i : pObject.entrySet()) {
            Identifier location = i.getKey();
            if (location.getPath().startsWith("_")) {
                continue;
            }

            try {
                JsonObject obj = i.getValue().getAsJsonObject();
                Hidden value = serializer.read(i.getKey(), obj);
                if (value == null) {
                    continue;
                }
                value.id = i.getKey();
                hidden.put(i.getKey(), value);
            } catch (Exception e) {
                Databank.LOGGER.error("[DATABANK ERROR] Parsing error loading hidden entry {}", location, e);
            }
        }
        HiddenManager.hidden = hidden;
        Databank.LOGGER.info("[DATABANK] Loaded {} hidden entries", hidden.size());
    }
    public static HiddenSerializer serializer = new HiddenSerializer();
}
