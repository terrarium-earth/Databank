package com.cmdpro.databank.model.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class DatabankItemRenderer implements SpecialModelRenderer<ItemStack> {
    private DatabankItemModel<ItemStack> model;

    public DatabankItemRenderer(DatabankItemModel<ItemStack> model) {
        this.model = model;
    }

    @Override
    public void submit(
        @Nullable ItemStack state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        int overlayCoords,
        boolean hasFoil,
        final int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        DatabankItemModel<ItemStack> model = getModel(state);
        model.setupModelPose(state);

        Vec3 normalMult = /* TODO pDisplayContext == ItemDisplayContext.GUI ? new Vec3(-1, -1, 1) :*/ new Vec3(1, 1, 1);

        model.submit(state, poseStack, submitNodeCollector, lightCoords, overlayCoords, 0xFFFFFFFF, normalMult);
        poseStack.popPose();
    }

    @Override
    public @Nullable ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    public DatabankItemModel<ItemStack> getModel() {
        return model;
    }
    public DatabankItemModel<ItemStack> getModel(ItemStack stack) {
        return getModel();
    }
}
