package com.cmdpro.databank;

import com.mojang.serialization.Codec;
import net.minecraft.util.Util;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

public class DatabankExtraCodecs {
    public static final Codec<Vector2ic> VECTOR2I = Codec.INT
            .listOf()
            .comapFlatMap(
                input -> Util.fixedSize(input, 2).map(d -> new Vector2i(d.get(0), d.get(1))),
                vec -> List.of(vec.x(), vec.y())
            );
}
