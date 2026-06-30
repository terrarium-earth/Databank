package com.cmdpro.databank.worldgui.components;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public abstract class WorldGuiComponentType {
    public abstract MapCodec<? extends WorldGuiComponent> codec();
    public abstract StreamCodec<RegistryFriendlyByteBuf, WorldGuiComponent> streamCodec();
}
