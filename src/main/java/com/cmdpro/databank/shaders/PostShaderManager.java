package com.cmdpro.databank.shaders;

import com.cmdpro.databank.Databank;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
public class PostShaderManager {
    public static List<PostShaderInstance> instances = new ArrayList<>();
    public static List<PostShaderInstance> removalQueue = new ArrayList<>();

    public static void addShader(PostShaderInstance instance) {
        instances.add(instance);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        for (PostShaderInstance i : instances) {
            i.tick();
        }
        for (PostShaderInstance i : removalQueue) {
            instances.remove(i);
        }
        removalQueue.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void setupFrameGraph(FrameGraphSetupEvent event) {
        for (PostShaderInstance i : instances) {
            i.setupFrameGraph(event.getFrameGrapBuilder());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
        for (PostShaderInstance i : instances) {
            i.process();
        }
    }
}
