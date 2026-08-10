package com.nore.cobblebash.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public class GymBossDefeatedTrigger extends SimpleCriterionTrigger<GymBossDefeatedTrigger.TriggerInstance> {
    private static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.STRING.optionalFieldOf("gym_type").forGetter(TriggerInstance::gymType)
    ).apply(instance, TriggerInstance::new));

    @Override
    public Codec<TriggerInstance> codec() {
        return CODEC;
    }

    public void trigger(ServerPlayer player, String gymType) {
        String normalizedGymType = gymType.toLowerCase(Locale.ROOT);
        trigger(player, instance -> instance.matches(normalizedGymType));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> gymType)
            implements SimpleCriterionTrigger.SimpleInstance {
        public boolean matches(String completedGymType) {
            return gymType.isEmpty() || gymType.get().equals(completedGymType);
        }
    }
}
