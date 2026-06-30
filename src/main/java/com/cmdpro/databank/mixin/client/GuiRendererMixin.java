package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.rendering.RenderProjectionUtil;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"), method = "draw")
    private RenderTarget redirectRenderTarget(Minecraft minecraft) {
        var override = RenderProjectionUtil.getGuiTargetOverride();

        if (override != null) {
            return override;
        }

        return minecraft.getMainRenderTarget();
    }
}
