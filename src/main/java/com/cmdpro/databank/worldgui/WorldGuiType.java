package com.cmdpro.databank.worldgui;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec2;

public abstract class WorldGuiType {
    public abstract MapCodec<? extends WorldGui> codec();
    public abstract StreamCodec<RegistryFriendlyByteBuf, ? extends WorldGui> streamCodec();

    public abstract Vec2 getMenuWorldSize(WorldGuiEntity entity);
    public abstract Vec2 getRenderSize();

    public boolean saves() {
        return false;
    }

    public float getViewScale() {
        return 1;
    }
}
