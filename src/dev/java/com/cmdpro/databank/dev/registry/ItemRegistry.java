package com.cmdpro.databank.dev.registry;

import com.cmdpro.databank.dev.DatabankDev;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DatabankDev.MOD_ID);

    public static final DeferredItem<GameMasterBlockItem> MODEL_TEST_ITEM = ITEMS.registerItem("model_test", (properties) -> new GameMasterBlockItem(BlockRegistry.MODEL_TEST.get(), properties));
    public static final DeferredItem<BlockItem> MEGABLOCK_TEST_CORE_ITEM = ITEMS.registerSimpleBlockItem(BlockRegistry.MEGABLOCK_TEST_CORE);
}
