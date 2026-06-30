package com.cmdpro.databank.worldgui.components.types;

import com.cmdpro.databank.worldgui.components.WorldGuiComponentType;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NonNull;

public abstract class WorldGuiPositionedComponentType extends WorldGuiComponentType {
    protected static <T extends WorldGuiPositionedComponent> Products.@NonNull P2<RecordCodecBuilder.Mu<T>, Integer, Integer> positionedCodecParts() {
        return new Products.P2<>(
            Codec.INT.optionalFieldOf("x", 0).forGetter((component) -> component.x),
            Codec.INT.optionalFieldOf("y", 0).forGetter((component) -> component.y)
        );
    }
}
