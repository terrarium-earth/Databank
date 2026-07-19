package com.cmdpro.databank.megablock;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class BasicMegablockRouter extends MegablockRouter {
    public Supplier<? extends Block> core;

    public BasicMegablockRouter(Properties properties, Supplier<? extends Block> core) {
        super(properties);
        this.core = core;
    }

    @Override
    public Block getCore() {
        return core.get();
    }
}
