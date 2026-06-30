package com.cmdpro.databank.dialogue.styles;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.dialogue.DialogueStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DialogueStyleManager {
    public static HashMap<Identifier, DialogueStyle> styles = new HashMap<>();

    protected static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    public static CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState currentReload,
        Executor taskExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor reloadExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> currentReload.resourceManager().listResources("databank/dialogue/styles", name -> name.toString().endsWith(".json")), taskExecutor)
                .thenApply(resources -> {
                    Map<Identifier, CompletableFuture<DialogueStyle>> tasks = new HashMap<>();
                    for (Identifier i : resources.keySet()) {
                        tasks.put(i, CompletableFuture.supplyAsync(() -> {
                            JsonObject json;
                            try {
                                Resource resource = currentReload.resourceManager().getResourceOrThrow(i);
                                InputStream stream = resource.open();
                                json = GsonHelper.fromJson(GSON, IOUtils.toString(stream, Charset.defaultCharset()), JsonObject.class);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return DialogueStyle.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                        }));
                    }
                    return tasks;
                })
                .thenCompose(preparationBarrier::wait).thenAccept((tasks) -> {
                    HashMap<Identifier, DialogueStyle> styles = new HashMap<Identifier, DialogueStyle>();
                    for (Map.Entry<Identifier, CompletableFuture<DialogueStyle>> i : tasks.entrySet()) {
                        String path = i.getKey().getPath().replaceFirst("databank/dialogue/styles/", "");
                        path = path.substring(0, path.length()-5);
                        styles.put(i.getKey().withPath(path), i.getValue().join());
                    }
                    DialogueStyleManager.styles = styles;
                    Databank.LOGGER.info("[DATABANK] Loaded Databank Dialogue Styles");
                });
    }
}
