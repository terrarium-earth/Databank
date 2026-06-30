package com.cmdpro.databank.dialogue;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.megastructures.Megastructure;
import com.cmdpro.databank.music.MusicSerializer;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DialogueTreeManager extends SimpleJsonResourceReloadListener<JsonElement> {
    public static HashMap<Identifier, DialogueTree> trees = new HashMap<>();

    public static DialogueTreeManager instance;
    protected DialogueTreeManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("databank/dialogue/trees"));
    }
    public static DialogueTreeManager getOrCreateInstance() {
        if (instance == null) {
            instance = new DialogueTreeManager();
        }
        return instance;
    }
    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        trees = new HashMap<>();
        Databank.LOGGER.info("[DATABANK] Adding Databank Dialogue Trees");
        for (Map.Entry<Identifier, JsonElement> i : pObject.entrySet()) {
            Identifier location = i.getKey();
            if (location.getPath().startsWith("_")) {
                continue;
            }

            try {
                JsonObject obj = i.getValue().getAsJsonObject();
                DialogueTree tree = DialogueTree.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow();
                trees.put(i.getKey(), tree);
            } catch (IllegalArgumentException | JsonParseException e) {
                Databank.LOGGER.error("[DATABANK ERROR] Parsing error loading dialogue tree type {}", location, e);
            }
        }
        Databank.LOGGER.info("[DATABANK] Loaded {} Dialogue Trees", trees.size());
    }
    public static MusicSerializer serializer = new MusicSerializer();
}