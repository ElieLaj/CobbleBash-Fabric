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

   public GymLootBagItem(Properties properties) {
      super(properties);
   }

   /** Un sac grave au niveau donne. */
   public static ItemStack forLevel(int gymLevel) {
      ItemStack stack = new ItemStack(CobbleBash.GYM_LOOT_BAG);
      stack.set(CobbleBashComponents.GYM_LEVEL, gymLevel);
      return stack;
   }

   /** Niveau grave, ou 1 pour un sac obtenu autrement (creatif, commande). */
   public static int levelOf(ItemStack stack) {
      Integer level = stack.get(CobbleBashComponents.GYM_LEVEL);
      return level == null ? 1 : level;
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
      return ResourceKey.create(Registries.LOOT_TABLE,
         ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "rewards/gym_loot_bag/tier_" + tier));
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      int level = levelOf(stack);
      tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.level", level).withStyle(ChatFormatting.AQUA));
      tooltip.add(Component.translatable("item.cobblebash.gym_loot_bag.tier", tierOf(level)).withStyle(ChatFormatting.DARK_GRAY));
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

      LootTable table = serverPlayer.server.reloadableRegistries().getLootTable(tableFor(tierOf(gymLevel)));
      LootParams params = new LootParams.Builder(serverLevel).withLuck(player.getLuck()).create(LootContextParamSets.EMPTY);
      List<ItemStack> rewards = table.getRandomItems(params, player.getRandom());

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
