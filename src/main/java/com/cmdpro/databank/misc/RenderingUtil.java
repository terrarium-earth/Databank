package com.cmdpro.databank.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class RenderingUtil {
    /**
     * @deprecated Use BeaconRenderer.submitBeaconBeam
     */
    @Deprecated(forRemoval = true)
    public static void renderAdvancedBeaconBeam(PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, Identifier pBeamLocation, float pTextureScale, long pGameTime, Vec3 pointA, Vec3 pointB, Color color, float pBeamRadius, float pGlowRadius) {
        int height = (int) pointA.distanceTo(pointB);
        pPoseStack.pushPose();
        pPoseStack.translate(pointA.x, pointA.y, pointA.z);

        BeaconRenderer.submitBeaconBeam(
            pPoseStack,
            submitNodeCollector,
            pBeamLocation,
            pTextureScale,
            pGameTime,
            0,
            height,
            color.getRGB(),
            pBeamRadius,
            pGlowRadius
        );

        pPoseStack.popPose();
    }
}
