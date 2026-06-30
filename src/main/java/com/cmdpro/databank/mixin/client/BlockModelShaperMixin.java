package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.DatabankUtils;
import com.cmdpro.databank.hidden.types.BlockHiddenType;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelSet.class)
public class BlockModelShaperMixin {
    @Inject(method = "get", at = @At(value = "HEAD"), cancellable = true)
    public void getBlockModel(BlockState pState, CallbackInfoReturnable<BlockModel> cir) {
        Block block = BlockHiddenType.getHiddenBlockClient(pState);
        if ((block != null) && (block != pState.getBlock())) {
            var set = (BlockModelSet)(Object)this;
            cir.setReturnValue(set.get(DatabankUtils.changeBlockType(pState, block)));
        }
    }
}
