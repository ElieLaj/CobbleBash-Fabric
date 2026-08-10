package com.nore.cobblebash.item;

import com.nore.cobblebash.CobbleBash;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

/**
 * Sac de butin remis a la sortie d'une arene.
 *
 * <p>Le niveau de l'arene est grave dedans a l'instant ou il est remis (voir
 * {@link #forLevel(int)}), puis relu a l'ouverture. Six paliers couvrent la
 * plage 1-100 : le contenu suit la difficulte reellement affrontee, pas la
 * progression du joueur au moment ou il tire la ficelle.
 *
 * <p>Le tirage lui-meme est delegue a une table de butin par palier, donc
 * modifiable en pack de donnees sans recompiler. Chaque table appelle une table
 * <em>extras</em> livree vide ici : c'est le point d'accroche prevu pour les
 * recompenses venues d'autres mods, qu'un mod de base ne peut pas nommer sans
 * casser au chargement si elles manquent.
 */
public class GymLootBagItem extends Item {
   /** Evite qu'un clic maintenu vide la pile d'un coup. */
   private static final int COOLDOWN_TICKS = 10;

   /** Bornes hautes des paliers ; au-dela du dernier seuil, palier 6. */
   private static final int[] TIER_CEILINGS = {15, 30, 50, 70, 90};

   /** Niveau a partir duquel le sac contient un oeuf du type de l'arene. */
   public static final int EGG_LEVEL = 50;

   public GymLootBagItem(Properties properties) {
      super(properties);
   }

   /** Un sac grave au niveau donne, sans type : pas d'oeuf a l'ouverture. */
   public static ItemStack forLevel(int gymLevel) {
      ItemStack stack = new ItemStack(CobbleBash.GYM_LOOT_BAG);
      stack.set(CobbleBashComponents.GYM_LEVEL, gymLevel);
      return stack;
   }

   /** Un sac grave au niveau et au type de l'arene dont il sort. */
   public static ItemStack forGym(int gymLevel, String gymType) {
      ItemStack stack = forLevel(gymLevel);
      if (gymType != null && !gymType.isBlank()) {
         stack.set(CobbleBashComponents.GYM_TYPE, gymType);
      }

      return stack;
   }

   /** Niveau grave, ou 1 pour un sac obtenu autrement (creatif, commande). */
   public static int levelOf(ItemStack stack) {
      Integer level = stack.get(CobbleBashComponents.GYM_LEVEL);
      return level == null ? 1 : level;
   }

   /** Type d'arene grave, ou vide si le sac n'en porte pas. */
   public static String typeOf(ItemStack stack) {
      String type = stack.get(CobbleBashComponents.GYM_TYPE);
      return type == null ? "" : type;
   }

   /** Palier 1 a 6 correspondant a un niveau d'arene. */
   public static int tierOf(int gymLevel) {
      for (int i = 0; i < TIER_CEILINGS.length; i++) {
         if (gymLevel <= TIER_CEILINGS[i]) {
            return i + 1;
         }
      }

      return TIER_CEILINGS.length + 1;
   }

   private static ResourceKey<LootTable> tableFor(int tier) {
      return table("rewards/gym_loot_bag/tier_" + tier);
   }

   private static ResourceKey<LootTable> table(String path) {
      return ResourceKey.create(Registries.LOOT_TABLE,
         ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, path));
   }

   /**
    * Le lot d'oeufs du type de l'arene, a partir du niveau {@link #EGG_LEVEL}.
    *
    * <p>Table separee et non pool du palier : le chemin depend du type grave
    * dans le sac, et une table de butin ne sait pas se brancher sur un
    * composant de l'objet qui la declenche. Livree vide par le mod, comme les
    * `extras` — les oeufs viennent d'un autre mod.
    */
   private static ResourceKey<LootTable> eggTableFor(ItemStack stack) {
      String type = typeOf(stack);
      if (type.isEmpty() || levelOf(stack) < EGG_LEVEL) {
         return null;
      }

      return table("rewards/gym_loot_bag/eggs/" + type);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      int level = levelOf(stack);
      String type = typeOf(stack);

      if (type.isEmpty()) {
         tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.level", level).withStyle(ChatFormatting.AQUA));
      } else {
         tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.level_type",
            level, Component.translatable("cobblebash.gym." + type)).withStyle(ChatFormatting.AQUA));
      }

      tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.tier", tierOf(level)).withStyle(ChatFormatting.DARK_GRAY));

      if (!type.isEmpty() && level >= EGG_LEVEL) {
         tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.egg").withStyle(ChatFormatting.GREEN));
      }

      tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.desc").withStyle(ChatFormatting.GRAY));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      }

      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.fail(stack);
      }

      ServerPlayer serverPlayer = (ServerPlayer)player;
      ServerLevel serverLevel = (ServerLevel)level;
      int gymLevel = levelOf(stack);

      LootParams params = new LootParams.Builder(serverLevel).withLuck(player.getLuck()).create(LootContextParamSets.EMPTY);
      List<ItemStack> rewards = new java.util.ArrayList<>(serverPlayer.server.reloadableRegistries()
         .getLootTable(tableFor(tierOf(gymLevel))).getRandomItems(params, player.getRandom()));

      ResourceKey<LootTable> eggs = eggTableFor(stack);
      if (eggs != null) {
         rewards.addAll(serverPlayer.server.reloadableRegistries()
            .getLootTable(eggs).getRandomItems(params, player.getRandom()));
      }

      // Table absente ou entierement vide : on ne consomme rien. Un sac qui
      // disparait sans rien rendre serait pire que le laisser fermer.
      if (rewards.isEmpty()) {
         serverPlayer.sendSystemMessage(
            Component.translatable("item.cobblebash.gym_loot_bag.empty").withStyle(ChatFormatting.RED));
         return InteractionResultHolder.fail(stack);
      }

      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      for (ItemStack reward : rewards) {
         // Le nom est releve AVANT le rangement : `Inventory.add` vide la pile
         // au fur et a mesure, et le nom d'une pile a zero est "Air".
         Component name = reward.getHoverName().copy();
         int count = reward.getCount();
         if (!player.getInventory().add(reward) && !reward.isEmpty()) {
            player.drop(reward, false);
         }

         serverPlayer.sendSystemMessage(
            Component.translatable("item.cobblebash.gym_loot_bag.opened", count, name));
      }

      player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
      serverLevel.playSound(null, player.blockPosition(),
         SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
      serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
         player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.4, 0.4, 0.0);

      return InteractionResultHolder.consume(stack);
   }
}
