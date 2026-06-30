package com.cmdpro.databank.misc;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.model.DatabankModel;
import com.cmdpro.databank.model.DatabankModels;
import com.cmdpro.databank.model.animation.DatabankAnimationReference;
import com.cmdpro.databank.model.animation.DatabankAnimationState;
import com.cmdpro.databank.model.item.DatabankItemModel;
import com.cmdpro.databank.model.item.DatabankItemRenderer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class BasicAnimatedBlockItemRenderer extends DatabankItemRenderer {
    public static final Identifier ID = Databank.locate("model");

    public BasicAnimatedBlockItemRenderer(Model model) {
        super(model);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // TODO
    }

    public record Unbaked(Identifier textureLocation, Identifier modelLocation) implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::textureLocation),
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::modelLocation)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<ItemStack> bake(BakingContext context) {
            return new BasicAnimatedBlockItemRenderer(new Model(textureLocation, modelLocation));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }

    public static class Model extends DatabankItemModel<ItemStack> {
        public DatabankAnimationState animState = new DatabankAnimationState("hand")
                .addAnim(new DatabankAnimationReference("hand", (state, anim) -> {}, (state, anim) -> {}));

        public Identifier textureLocation;
        public Identifier modelLocation;
        public DatabankModel model;

        public Model(Identifier textureLocation, Identifier modelLocation) {
            this.textureLocation = textureLocation;
            this.modelLocation = modelLocation;
        }

        @Override
        public Identifier getTextureLocation() {
            return textureLocation;
        }

        @Override
        public void setupModelPose(ItemStack stack) {
            animate(animState);
        }

        public DatabankModel getModel() {
            if (model == null) {
                model = DatabankModels.models.get(modelLocation);
                animState.updateAnimDefinitions(model);
            }
            return model;
        }
    }
}