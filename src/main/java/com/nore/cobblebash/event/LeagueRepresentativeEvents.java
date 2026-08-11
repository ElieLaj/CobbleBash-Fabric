package com.nore.cobblebash.event;

import com.cobblemon.mod.common.CobblemonItems;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.gym.GymType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.InteractionResult;

public class LeagueRepresentativeEvents {

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
         factories.add(lazyItem(() -> CobblemonItems.POKE_BALL, 2, 1, 16, 1));
         factories.add(lazyItem(() -> CobblemonItems.HEAL_BALL, 4, 1, 12, 1));
         factories.add(lazyItem(() -> CobblemonItems.NEST_BALL, 5, 1, 12, 1));
         factories.add(lazyItem(() -> CobblemonItems.X_ATTACK, 3, 1, 12, 2));
         factories.add(lazyItem(() -> CobblemonItems.X_DEFENSE, 3, 1, 12, 2));
         factories.add(lazyItem(() -> CobblemonItems.X_SPEED, 3, 1, 12, 2));
      });

      // University : injection dans les coffres vanilla desactivee (10/08/2026).
      // Les Disques d'Entrainement restent vendus par le Representant de la Ligue
      // et le Disque du Conseil 4 est donne par awardEliteFourDiskIfEligible une
      // fois les 18 arenes battues, donc rien n'est rendu inaccessible.

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


   /**
    * Offre dont l'objet n'est resolu qu'a la premiere generation d'echange.
    *
    * <p>{@code registerVillagerOffers} execute son lambda des l'initialisation
    * du mod ; toucher {@code CobblemonItems} a ce moment declenche son
    * initialisation statique avant celle de Cobblemon, et le jeu s'arrete sur
    * un {@code lateinit property implementation has not been initialized}.
    * NeoForge n'avait pas ce souci : son evenement se declenche bien plus tard.
    */
   private static ItemListing lazyItem(Supplier<Item> item, int emeraldCost, int count, int maxUses, int xp) {
      return (entity, random) -> item(item.get(), emeraldCost, count, maxUses, xp).getOffer(entity, random);
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
