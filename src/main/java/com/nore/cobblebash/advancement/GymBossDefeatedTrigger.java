package com.nore.cobblebash.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger.SimpleInstance;
import net.minecraft.server.level.ServerPlayer;

public class GymBossDefeatedTrigger extends SimpleCriterionTrigger<GymBossDefeatedTrigger.TriggerInstance> {
   private static final Codec<GymBossDefeatedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(GymBossDefeatedTrigger.TriggerInstance::player),
            Codec.STRING.optionalFieldOf("gym_type").forGetter(GymBossDefeatedTrigger.TriggerInstance::gymType)
         )
         .apply(instance, GymBossDefeatedTrigger.TriggerInstance::new)
   );

   public Codec<GymBossDefeatedTrigger.TriggerInstance> codec() {
      return CODEC;
   }

   public void trigger(ServerPlayer player, String gymType) {
      String s = gymType.toLowerCase(Locale.ROOT);
      this.trigger(player, instance -> instance.matches(s));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> gymType) implements SimpleInstance {
      public boolean matches(String completedGymType) {
         return this.gymType.isEmpty() || this.gymType.get().equals(completedGymType);
      }
   }
}
