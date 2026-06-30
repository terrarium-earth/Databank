package com.cmdpro.databank.registry;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.worldgui.WorldGui;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

public class EntityDataSerializerRegistry {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Databank.MOD_ID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<WorldGui>>> WORLD_GUI = ENTITY_DATA_SERIALIZERS.register(
        "world_gui",
        () -> EntityDataSerializer.forValueType(ByteBufCodecs.optional(WorldGui.STREAM_CODEC))
    );
}
