package com.cmdpro.databank.worldgui.components.types;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NonNull;

public abstract class WorldGuiRectComponentType extends WorldGuiPositionedComponentType {
    protected static <T extends WorldGuiRectComponent> Products.@NonNull P4<RecordCodecBuilder.Mu<T>, Integer, Integer, Integer, Integer> rectCodecParts() {
        return WorldGuiPositionedComponentType.<T>positionedCodecParts().and(
            new Products.P2<>(
                Codec.INT.optionalFieldOf("width", 0).forGetter((component) -> component.width),
                Codec.INT.optionalFieldOf("height", 0).forGetter((component) -> component.height)
            )
        );
    }
}
