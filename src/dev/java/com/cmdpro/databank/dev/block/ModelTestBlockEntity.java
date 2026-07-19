package com.cmdpro.databank.dev.block;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.dev.registry.BlockEntityRegistry;
import com.cmdpro.databank.model.animation.DatabankAnimationReference;
import com.cmdpro.databank.model.animation.DatabankAnimationState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Databank.MOD_ID)
public class ModelTestBlockEntity extends BlockEntity {
    public ModelTestBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.MODEL_TEST.get(), pos, blockState);
    }
    public DatabankAnimationState animState = new DatabankAnimationState("animation")
            .addAnim(new DatabankAnimationReference("animation", (state, anim) -> {}, (state, anim) -> {}))
            .addAnim(new DatabankAnimationReference("animation2", (state, anim) -> {}, (state, anim) -> {}));

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        animState.setLevel(level);
    }
}
