package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.DatabankUtils;
import com.cmdpro.databank.hidden.types.BlockHiddenType;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {

    @Shadow
    public abstract Set<Property<?>> getColoringProperties(Block block);

    @Shadow
    public abstract List<BlockTintSource> getTintSources(BlockState state);

    @Inject(method = "getTintSources", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void getTintSources(BlockState state, CallbackInfoReturnable<List<BlockTintSource>> cir) {
        if (state != null) {
            Block hiddenBlock = BlockHiddenType.getHiddenBlockClient(state);
            if ((hiddenBlock != null) && (hiddenBlock != state.getBlock())) {
                BlockState newState = DatabankUtils.changeBlockType(state, hiddenBlock);
                cir.setReturnValue(getTintSources(newState));
            }
        }
    }

    @Inject(method = "getColoringProperties", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void getColoringProperties(Block block, CallbackInfoReturnable<Set<Property<?>>> cir) {
        if (block != null) {
            Block hiddenBlock = BlockHiddenType.getHiddenBlockClient(block);
            if ((hiddenBlock != null) && (hiddenBlock != block)) {
                cir.setReturnValue(getColoringProperties(hiddenBlock));
            }
        }
    }
}
