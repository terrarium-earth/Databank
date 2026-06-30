package com.cmdpro.databank;

import com.cmdpro.databank.dialogue.styles.DialogueStyleManager;
import com.cmdpro.databank.impact.ImpactFrameHandler;
import com.cmdpro.databank.misc.BasicAnimatedBlockItemRenderer;
import com.cmdpro.databank.misc.ResizeHelper;
import com.cmdpro.databank.model.DatabankModels;
import com.cmdpro.databank.music.MusicManager;
import com.cmdpro.databank.music.MusicSystem;
import com.cmdpro.databank.rendering.RenderProjectionUtil;
import com.cmdpro.databank.shaders.PostShaderInstance;
import com.cmdpro.databank.shaders.PostShaderManager;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(Dist.CLIENT)
public class DatabankClient {
    protected static void addResizeListeners() {
        // Impact Frames
        ResizeHelper.addListener(ImpactFrameHandler::resize);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DatabankClient.addResizeListeners();

            Minecraft.getInstance().getTextureManager().register(
                RenderProjectionUtil.PROJECTED_TARGET_TEXTURE,
                new AbstractTexture() {
                    @Override
                    public GpuTextureView getTextureView() {
                        return RenderProjectionUtil.getProjectedTarget().getColorTextureView();
                    }
                }
            );
        });
    }

    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Databank.locate("models"), DatabankModels::reload);
        event.addListener(Databank.locate("music"), MusicManager.getOrCreateInstance());
        event.addListener(Databank.locate("dialogue_styles"), DialogueStyleManager::reload);
    }

    @SubscribeEvent
    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(BasicAnimatedBlockItemRenderer.ID, BasicAnimatedBlockItemRenderer.Unbaked.CODEC);
    }
}
