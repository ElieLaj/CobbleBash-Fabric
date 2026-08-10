package com.nore.cobblebash.event;

import com.cobblemon.mod.common.CobblemonItems;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.gym.GymType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.npc.VillagerTrades.ItemsForEmeralds;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.InteractionResult;

public class LeagueRepresentativeEvents {
   private static final ResourceLocation END_CITY_TREASURE = ResourceLocation.withDefaultNamespace("chests/end_city_treasure");
   private static final Map<ResourceLocation, List<LeagueRepresentativeEvents.LootInjection>> LOOT_INJECTIONS = buildLootInjections();

   /**
    * Branchement des evenements.
    *
    * <p>Les offres de villageois et l'injection dans les tables de butin ont
    * chacune leur equivalent Fabric. Les interactions se replient toutes deux
    * sur {@code UseEntityCallback} — NeoForge distingue le clic sur l'entite et
    * le clic sur une partie precise, Fabric non, et cette distinction n'importe
    * pas ici. L'echange lui-meme passe par un mixin, faute de crochet.
    */
   public static void register() {
      TradeOfferHelper.registerVillagerOffers(CobbleBash.LEAGUE_REPRESENTATIVE, 1, factories -> {
         factories.add(item(CobblemonItems.POKE_BALL, 2, 1, 16, 1));
         factories.add(item(CobblemonItems.HEAL_BALL, 4, 1, 12, 1));
         factories.add(item(CobblemonItems.NEST_BALL, 5, 1, 12, 1));
         factories.add(item(CobblemonItems.X_ATTACK, 3, 1, 12, 2));
         factories.add(item(CobblemonItems.X_DEFENSE, 3, 1, 12, 2));
         factories.add(item(CobblemonItems.X_SPEED, 3, 1, 12, 2));
      });

      LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
         ResourceLocation name = key.location();

         List<LeagueRepresentativeEvents.LootInjection> list = LOOT_INJECTIONS.get(name);
         if (list != null) {
            for (LeagueRepresentativeEvents.LootInjection injection : list) {
               tableBuilder.pool(
                  LootPool.lootPool()
                     .setRolls(ConstantValue.exactly(1.0F))
                     .add(LootItem.lootTableItem((ItemLike)CobbleBash.TRAINING_DISKS.get(injection.gymType())).setWeight(1))
                     .add(EmptyLootItem.emptyItem().setWeight(injection.emptyWeight()))
                     .build()
               );
            }
         }

         if (END_CITY_TREASURE.equals(name)) {
            tableBuilder.pool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem((ItemLike)CobbleBash.ELITE_FOUR_TRAINING_DISK).setWeight(1))
                  .add(EmptyLootItem.emptyItem().setWeight(23))
                  .build()
            );
         }
      });

      UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
         handleRepresentativeInteraction(player, entity);
         return InteractionResult.PASS;
      });
   }

   /** {@code TradeWithVillagerEvent}, appele depuis le mixin du comptoir. */
   public static void onTradeWithVillager(ServerPlayer player, Entity merchant) {
      if (merchant instanceof Villager villager
         && villager.getVillagerData().getProfession() == CobbleBash.LEAGUE_REPRESENTATIVE) {
         ensureMasterTrainingDiskOffer(villager);
         CobbleBashCriteriaTriggers.triggerLeagueRepresentativeTraded(player);
      }
   }

   private static void handleRepresentativeInteraction(Entity playerEntity, Entity target) {
      if (playerEntity instanceof ServerPlayer serverplayer
         && target instanceof Villager villager
         && villager.getVillagerData().getProfession() == CobbleBash.LEAGUE_REPRESENTATIVE) {
         ensureMasterTrainingDiskOffer(villager);
         CobbleBashCriteriaTriggers.triggerLeagueRepresentativeMet(serverplayer);
      }
   }


   private static ItemListing item(Item item, int emeraldCost, int count, int maxUses, int xp) {
      return new ItemsForEmeralds(item, emeraldCost, count, maxUses, xp);
   }

   private static ItemListing disk(int emeraldCost, int maxUses, int xp) {
      return new LeagueRepresentativeEvents.RandomTrainingDiskForEmeralds(emeraldCost, maxUses, xp);
   }

   private static ItemListing eliteFourDisk(int emeraldCost, int maxUses, int xp) {
      return (trader, random) -> new MerchantOffer(
         new ItemCost(Items.EMERALD, emeraldCost), new ItemStack((ItemLike)CobbleBash.ELITE_FOUR_TRAINING_DISK), maxUses, xp, 0.05F
      );
   }

   private static void ensureMasterTrainingDiskOffer(Villager villager) {
      if (villager.getVillagerData().getLevel() >= 5 && !hasTrainingDiskOffer(villager)) {
         MerchantOffer merchantoffer = new LeagueRepresentativeEvents.RandomTrainingDiskForEmeralds(18, 6, 30).getOffer(villager, villager.getRandom());
         if (merchantoffer != null) {
            villager.getOffers().add(merchantoffer);
         }
      }
   }

   private static boolean hasTrainingDiskOffer(Villager villager) {
      for (MerchantOffer merchantoffer : villager.getOffers()) {
         if (isTrainingDisk(merchantoffer.getResult())) {
            return true;
         }
      }

      return false;
   }

   private static boolean isTrainingDisk(ItemStack stack) {
      for (GymType gymtype : GymType.values()) {
         if (stack.is((Item)CobbleBash.TRAINING_DISKS.get(gymtype))) {
            return true;
         }
      }

      return stack.is((Item)CobbleBash.ELITE_FOUR_TRAINING_DISK);
   }

   private static List<ItemLike> buildTrainingDiskList() {
      List<ItemLike> list = new ArrayList<>();

      for (GymType gymtype : GymType.values()) {
         list.add((ItemLike)CobbleBash.TRAINING_DISKS.get(gymtype));
      }

      return List.copyOf(list);
   }

   private static Map<ResourceLocation, List<LeagueRepresentativeEvents.LootInjection>> buildLootInjections() {
      Map<ResourceLocation, List<LeagueRepresentativeEvents.LootInjection>> map = new HashMap<>();
      add(map, GymType.NORMAL, 7, "village/village_plains_house", "simple_dungeon");
      add(map, GymType.FIGHTING, 7, "pillager_outpost", "trial_chambers/reward");
      add(map, GymType.FLYING, 7, "pillager_outpost", "shipwreck_map", "trial_chambers/supply");
      add(map, GymType.POISON, 7, "jungle_temple", "abandoned_mineshaft");
      add(map, GymType.GROUND, 7, "desert_pyramid", "village/village_desert_house");
      add(map, GymType.ROCK, 7, "abandoned_mineshaft", "trial_chambers/corridor");
      add(map, GymType.BUG, 7, "jungle_temple", "abandoned_mineshaft", "village/village_taiga_house");
      add(map, GymType.GHOST, 7, "woodland_mansion", "ancient_city");
      add(map, GymType.STEEL, 7, "village/village_armorer", "village/village_weaponsmith", "trial_chambers/reward");
      add(map, GymType.FIRE, 7, "ruined_portal", "nether_bridge", "bastion_treasure");
      add(map, GymType.WATER, 7, "shipwreck_treasure", "shipwreck_supply", "buried_treasure", "underwater_ruin_big", "underwater_ruin_small");
      add(map, GymType.GRASS, 7, "jungle_temple", "village/village_plains_house", "village/village_taiga_house");
      add(map, GymType.ELECTRIC, 7, "trial_chambers/reward", "ancient_city", "ruined_portal");
      add(map, GymType.PSYCHIC, 7, "stronghold_library", "ancient_city");
      add(map, GymType.ICE, 7, "igloo_chest", "ancient_city_ice_box");
      add(map, GymType.DRAGON, 7, "end_city_treasure", "stronghold_library", "stronghold_corridor");
      add(map, GymType.DARK, 7, "woodland_mansion", "ancient_city");
      add(map, GymType.FAIRY, 7, "woodland_mansion", "village/village_temple", "stronghold_library");
      return Map.copyOf(map);
   }

   private static void add(Map<ResourceLocation, List<LeagueRepresentativeEvents.LootInjection>> injections, GymType type, int emptyWeight, String... tables) {
      for (String s : tables) {
         ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("chests/" + s);
         injections.computeIfAbsent(resourcelocation, ignored -> new ArrayList<>()).add(new LeagueRepresentativeEvents.LootInjection(type, emptyWeight));
      }
   }

   private record LootInjection(GymType gymType, int emptyWeight) {
   }

   private static final class RandomTrainingDiskForEmeralds implements ItemListing {
      private final int emeraldCost;
      private final int maxUses;
      private final int xp;

      private RandomTrainingDiskForEmeralds(int emeraldCost, int maxUses, int xp) {
         this.emeraldCost = emeraldCost;
         this.maxUses = maxUses;
         this.xp = xp;
      }

      public MerchantOffer getOffer(Entity trader, RandomSource random) {
         List<ItemLike> list = LeagueRepresentativeEvents.buildTrainingDiskList();
         if (list.isEmpty()) {
            return null;
         }

         ItemLike itemlike = list.get(random.nextInt(list.size()));
         return new MerchantOffer(new ItemCost(Items.EMERALD, this.emeraldCost), new ItemStack(itemlike), this.maxUses, this.xp, 0.05F);
      }
   }
}
