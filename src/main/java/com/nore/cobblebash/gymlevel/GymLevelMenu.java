package com.nore.cobblebash.gymlevel;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.gym.GymLevelOverride;
import com.nore.cobblebash.item.TrainingDiskItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Choix du niveau avant d'entrer dans une arene.
 *
 * <p>Ajout au mod d'origine, qui entre directement au clic droit. Le menu ne
 * porte aucun emplacement : le niveau remonte par le mecanisme de boutons de
 * conteneur, celui qu'utilise la balise vanilla, ce qui evite d'ajouter un
 * paquet reseau.
 *
 * <p>Le disque n'est consomme qu'a la confirmation, et il est reverifie a ce
 * moment-la : fermer la fenetre ne coute donc rien, et un disque pose entre
 * temps ne peut pas etre pris a tort.
 */
public class GymLevelMenu extends AbstractContainerMenu {
   /** Le type d'arene voyage jusqu'au client pour l'affichage. */
   public static final StreamCodec<RegistryFriendlyByteBuf, String> DATA_CODEC =
      ByteBufCodecs.STRING_UTF8.cast();

   private final String gymType;

   public GymLevelMenu(int containerId, Inventory inventory, String gymType) {
      super(CobbleBash.GYM_LEVEL_MENU, containerId);
      this.gymType = gymType;
   }

   public String gymType() {
      return this.gymType;
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return false;
      }

      if (id < GymLevelOverride.MIN || id > GymLevelOverride.MAX) {
         return false;
      }

      // Le disque est relu maintenant : entre l'ouverture et la confirmation,
      // le joueur a pu le poser, le jeter ou en changer.
      InteractionHand hand = findDisk(serverPlayer);
      if (hand == null) {
         serverPlayer.closeContainer();
         return false;
      }

      GymLevelOverride.set(serverPlayer, id);
      serverPlayer.closeContainer();

      if (GymCommand.enterGym(serverPlayer, this.gymType)) {
         ItemStack stack = serverPlayer.getItemInHand(hand);
         if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
         }
         return true;
      }

      // Entree refusee : le choix ne doit pas rester en attente.
      GymLevelOverride.clear(serverPlayer.getUUID());
      return false;
   }

   private InteractionHand findDisk(Player player) {
      for (InteractionHand hand : InteractionHand.values()) {
         ItemStack stack = player.getItemInHand(hand);
         if (stack.getItem() instanceof TrainingDiskItem disk
            && disk.getGymType().getId().equals(this.gymType)) {
            return hand;
         }
      }
      return null;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }
}
