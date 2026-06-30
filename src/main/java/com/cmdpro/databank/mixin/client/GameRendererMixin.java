package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.misc.AfterHandRenderLevelStageEvent;
import com.cmdpro.databank.misc.ResizeHelper;
import com.cmdpro.databank.worldgui.WorldGuiEntity;
import com.cmdpro.databank.worldgui.WorldGuiHitResult;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "TAIL"), remap = false)
    private void Databank$renderLevel(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "modelViewMatrix") Matrix4f modelViewMatrix) {
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer levelRenderer = mc.levelRenderer;
        Profiler.get().push("databank_render_after_hand");
        NeoForge.EVENT_BUS.post(new AfterHandRenderLevelStageEvent(
            levelRenderer,
            levelRenderer.levelRenderState,
            null,
            modelViewMatrix,
            levelRenderer.getRenderableSections()
        ));
        Profiler.get().pop();
    }

    @Inject(method = "resize", at = @At(value = "TAIL"), remap = false)
    private void resize(int pWidth, int pHeight, CallbackInfo ci) {
        ResizeHelper.resize(pWidth, pHeight);
    }
}
