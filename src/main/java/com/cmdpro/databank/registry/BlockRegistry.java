package com.cmdpro.databank.registry;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.megastructures.block.MegastructureSaveBlock;
import com.cmdpro.databank.multiblock.MultiblockPredicateSerializer;
import com.cmdpro.databank.multiblock.predicates.serializers.AnyMultiblockPredicateSerializer;
import com.cmdpro.databank.multiblock.predicates.serializers.BlockstateMultiblockPredicateSerializer;
import com.cmdpro.databank.multiblock.predicates.serializers.TagMultiblockPredicateSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Databank.MOD_ID);

    public static final DeferredBlock<MegastructureSaveBlock> MEGASTRUCTURE_SAVE = BLOCKS.registerBlock(
        "megastructure_save",
        MegastructureSaveBlock::new,
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRUCTURE_BLOCK)
    );
}
