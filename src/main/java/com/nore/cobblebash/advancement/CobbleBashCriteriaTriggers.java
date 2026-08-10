package com.nore.cobblebash.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CobbleBashCriteriaTriggers {
   // Enregistrement direct : Fabric appelle l'initialiseur du mod pendant que
   // les registres sont encore modifiables.
   public static GymBossDefeatedTrigger GYM_BOSS_DEFEATED;
   public static SimpleEventTrigger LEAGUE_REPRESENTATIVE_MET;
   public static SimpleEventTrigger LEAGUE_REPRESENTATIVE_TRADED;
   public static SimpleEventTrigger GYM_ENTERED;

   public static void bootstrap() {
      GYM_BOSS_DEFEATED = register("gym_boss_defeated", new GymBossDefeatedTrigger());
      LEAGUE_REPRESENTATIVE_MET = register("league_representative_met", new SimpleEventTrigger());
      LEAGUE_REPRESENTATIVE_TRADED = register("league_representative_traded", new SimpleEventTrigger());
      GYM_ENTERED = register("gym_entered", new SimpleEventTrigger());
   }

   private static <T extends net.minecraft.advancements.CriterionTrigger<?>> T register(String name, T trigger) {
      return Registry.register(
         BuiltInRegistries.TRIGGER_TYPES,
         ResourceLocation.fromNamespaceAndPath("cobblebash", name),
         trigger
      );
   }

   public static void triggerGymBossDefeated(ServerPlayer player, String gymType) {
      GYM_BOSS_DEFEATED.trigger(player, gymType);
   }

   public static void triggerLeagueRepresentativeMet(ServerPlayer player) {
      LEAGUE_REPRESENTATIVE_MET.trigger(player);
   }

   public static void triggerLeagueRepresentativeTraded(ServerPlayer player) {
      LEAGUE_REPRESENTATIVE_TRADED.trigger(player);
   }

   public static void triggerGymEntered(ServerPlayer player) {
      GYM_ENTERED.trigger(player);
   }
}
