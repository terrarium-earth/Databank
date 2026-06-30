package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.multiblock.MultiblockRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"), method = "draw")
    private GpuBufferSlice updateColor(DynamicUniforms instance, Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix) {
        if ((Object) this instanceof MultiblockRenderer.HologramRenderType) {
            // Half alpha in the color modulator for hologram rendering
            return instance.writeTransform(
                modelView,
                new Vector4f(colorModulator.x(), colorModulator.y(), colorModulator.z(), colorModulator.w() * 0.5f),
                modelOffset,
                textureMatrix
            );
        }

        return instance.writeTransform(modelView, colorModulator, modelOffset, textureMatrix);
    }
}
