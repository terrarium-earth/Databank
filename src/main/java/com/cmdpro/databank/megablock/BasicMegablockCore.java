package com.cmdpro.databank.megablock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BasicMegablockCore extends Block implements MegablockCore {
    public BasicMegablockCore(Properties properties) {
        super(properties);
    }
    public Rotation getRotation(BlockState state) {
        return Rotation.NONE;
    }
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return checkPlacement(context, super.getStateForPlacement(context));
    }
    public BlockState checkPlacement(BlockPlaceContext context, BlockState state) {
        if (!MegablockCoreUtil.ableToPlace(this, getRotation(state), context)) {
            return null;
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        MegablockCoreUtil.placeRouters(this, getRotation(state), level, pos);
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        MegablockCoreUtil.removeRouters(this, getRotation(state), level, pos);
    }
}
