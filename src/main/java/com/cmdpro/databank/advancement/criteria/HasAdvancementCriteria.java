package com.cmdpro.databank.advancement.criteria;

import com.cmdpro.databank.registry.CriteriaTriggerRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class HasAdvancementCriteria extends SimpleCriterionTrigger<HasAdvancementCriteria.HasAdvancementCriteriaInstance> {
    public static Criterion<HasAdvancementCriteriaInstance> instance(ContextAwarePredicate player, Identifier advancement) {
        return CriteriaTriggerRegistry.HAS_ADVANCEMENT.get().createCriterion(new HasAdvancementCriteriaInstance(Optional.of(player), advancement));
    }
    public static final Codec<HasAdvancementCriteriaInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(HasAdvancementCriteriaInstance::player),
            Identifier.CODEC.fieldOf("advancement").forGetter(HasAdvancementCriteriaInstance::advancement)
    ).apply(instance, HasAdvancementCriteriaInstance::new));
    @Override
    public Codec<HasAdvancementCriteriaInstance> codec() {
        return CODEC;
    }

    public void trigger(ServerPlayer player, Identifier advancement) {
        this.trigger(player, (instance) -> advancement.equals(instance.advancement));
    }
    public record HasAdvancementCriteriaInstance(Optional<ContextAwarePredicate> player, Identifier advancement) implements SimpleCriterionTrigger.SimpleInstance {
    }
}
