package com.cmdpro.databank.registry;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.megastructures.block.MegastructureSaveBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Databank.MOD_ID);

    public static final DeferredItem<GameMasterBlockItem> MEGASTRUCTURE_SAVE = ITEMS.registerItem(
        "megastructure_save",
        (properties) -> new GameMasterBlockItem(BlockRegistry.MEGASTRUCTURE_SAVE.get(), properties)
    );
}
