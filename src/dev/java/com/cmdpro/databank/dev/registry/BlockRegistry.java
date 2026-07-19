package com.cmdpro.databank.dev.registry;

import com.cmdpro.databank.dev.DatabankDev;
import com.cmdpro.databank.dev.block.MegablockTestCore;
import com.cmdpro.databank.dev.block.ModelTestBlock;
import com.cmdpro.databank.dev.block.ModelTestBlockEntity;
import com.cmdpro.databank.megablock.BasicMegablockRouter;
import com.cmdpro.databank.megastructures.block.MegastructureSaveBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DatabankDev.MOD_ID);

    public static final DeferredBlock<ModelTestBlock> MODEL_TEST = BLOCKS.registerBlock(
        "model_test",
        ModelTestBlock::new,
        () -> BlockBehaviour.Properties
            .ofFullCopy(Blocks.STRUCTURE_BLOCK)
            .noCollision()
            .noOcclusion()
            .noLootTable()
            .noTerrainParticles()
    );

    public static final DeferredBlock<MegablockTestCore> MEGABLOCK_TEST_CORE = BLOCKS.registerBlock(
        "megablock_test_core",
        MegablockTestCore::new,
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRUCTURE_BLOCK).noLootTable()
    );

    public static final DeferredBlock<BasicMegablockRouter> MEGABLOCK_TEST_ROUTER = BLOCKS.registerBlock(
        "megablock_test_router",
        (properties) -> new BasicMegablockRouter(
            properties,
            MEGABLOCK_TEST_CORE
        ),
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRUCTURE_BLOCK).noLootTable()
    );
}
