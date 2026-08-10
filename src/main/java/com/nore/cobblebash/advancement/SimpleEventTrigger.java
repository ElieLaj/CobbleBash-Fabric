package com.nore.cobblebash.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger.SimpleInstance;
import net.minecraft.server.level.ServerPlayer;

public class SimpleEventTrigger extends SimpleCriterionTrigger<SimpleEventTrigger.TriggerInstance> {
   private static final Codec<SimpleEventTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SimpleEventTrigger.TriggerInstance::player))
         .apply(instance, SimpleEventTrigger.TriggerInstance::new)
   );

   public Codec<SimpleEventTrigger.TriggerInstance> codec() {
      return CODEC;
   }

   public void trigger(ServerPlayer player) {
      this.trigger(player, instance -> true);
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
   }
}
