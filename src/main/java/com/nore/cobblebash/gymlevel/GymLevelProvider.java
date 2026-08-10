package com.nore.cobblebash.gymlevel;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Ouvre le choix de niveau en transmettant le type d'arene au client.
 *
 * <p>NeoForge passe ces donnees par un {@code Consumer<FriendlyByteBuf>} au
 * moment de l'ouverture ; Fabric attend a la place une fabrique qui les fournit,
 * d'ou cette classe plutot qu'un {@code SimpleMenuProvider}.
 */
public record GymLevelProvider(String gymType) implements ExtendedScreenHandlerFactory<String>, MenuProvider {

   @Override
   public String getScreenOpeningData(ServerPlayer player) {
      return this.gymType;
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("gui.cobblebash.gym_level");
   }

   @Override
   public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
      return new GymLevelMenu(containerId, inventory, this.gymType);
   }
}
