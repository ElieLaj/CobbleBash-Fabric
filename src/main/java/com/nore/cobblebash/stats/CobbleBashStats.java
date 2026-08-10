package com.nore.cobblebash.stats;

import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.PlayerGymProgress;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import java.util.HashSet;
import java.util.Set;

public class CobbleBashStats {
   public static final ResourceLocation GYMS_COMPLETED_ID =
      ResourceLocation.fromNamespaceAndPath("cobblebash", "gyms_completed");

   /** Une statistique personnalisee est un registre de ResourceLocation ou la
    * cle est aussi la valeur. Fabric enregistre directement, sans differer. */
   public static ResourceLocation GYMS_COMPLETED;

   public static void bootstrap() {
      GYMS_COMPLETED = Registry.register(BuiltInRegistries.CUSTOM_STAT, GYMS_COMPLETED_ID, GYMS_COMPLETED_ID);
      Stats.CUSTOM.get(GYMS_COMPLETED, StatFormatter.DEFAULT);
   }

   public static void syncGymsCompleted(ServerPlayer player) {
      PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
      progress.markCompletedGyms(getCompletedGymAdvancements(player));
      setStatValue(player, progress.getCompletedGymCount());
   }

   private static Set<String> getCompletedGymAdvancements(ServerPlayer player) {
      Set<String> completed = new HashSet<>();

      for (GymType type : GymType.values()) {
         AdvancementHolder advancement = player.server
            .getAdvancements()
            .get(ResourceLocation.fromNamespaceAndPath("cobblebash", "gym/complete_gym/" + type.getId()));
         if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            completed.add(type.getId());
         }
      }

      return completed;
   }

   private static void setStatValue(ServerPlayer player, int value) {
      Stat<ResourceLocation> stat = Stats.CUSTOM.get(GYMS_COMPLETED);
      int current = player.getStats().getValue(stat);
      if (current != value) {
         player.resetStat(stat);
         if (value > 0) {
            player.awardStat(stat, value);
         }

         player.getStats().sendStats(player);
      }
   }
}
