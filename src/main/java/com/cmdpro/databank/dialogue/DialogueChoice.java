package com.cmdpro.databank.dialogue;

import com.cmdpro.databank.DatabankRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DialogueChoice {
    public static final Codec<DialogueChoice> CODEC = RecordCodecBuilder.create((builder) -> builder.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter((obj) -> obj.text),
            DialogueChoiceAction.CODEC.listOf().fieldOf("actions").forGetter((obj) -> obj.actions)
    ).apply(builder, DialogueChoice::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueChoice> STREAM_CODEC = StreamCodec.composite(
        ComponentSerialization.STREAM_CODEC,
        (obj) -> obj.text,

        ByteBufCodecs
            .registry(DatabankRegistries.DIALOGUE_CHOICE_ACTION_REGISTRY.key())
            .dispatch(DialogueChoiceAction::getCodecs, DialogueChoiceAction.Codecs::streamCodec)
            .apply(ByteBufCodecs.list()),
        (obj) -> obj.actions,

        DialogueChoice::new
    );

    public DialogueChoice(Component text, List<DialogueChoiceAction> actions) {
        this.text = text;
        this.actions = actions;
    }
    public DialogueEntry entry;
    public Component text;
    public List<DialogueChoiceAction> actions;
    public void onClick(Player player, DialogueInstance instance, DialogueChoice choice) {
        for (DialogueChoiceAction i : actions) {
            i.onClick(player, instance, choice);
        }
    }
}
