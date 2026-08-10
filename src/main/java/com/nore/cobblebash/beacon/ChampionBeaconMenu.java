package com.nore.cobblebash.beacon;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ChampionBeaconMenu extends AbstractContainerMenu {
   public static final int PAYMENT_SLOT = 0;
   public static final int SLOT_COUNT = 1;
   public static final int DATA_LEVELS = 0;
   public static final int DATA_PRIMARY = 1;
   public static final int DATA_SECONDARY = 2;
   public static final int DATA_UPGRADED = 3;
   public static final int DATA_COUNT = 4;
   public static final int PRIMARY_BUTTON_OFFSET = 10;
   public static final int SECONDARY_BUTTON_OFFSET = 30;
   public static final int UPGRADE_BUTTON_ID = 50;
   public static final int CONFIRM_BUTTON_ID = 60;
   public static final TagKey<Item> PAYMENT_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("cobblemon", "evolution_stones"));
   private static final int INV_SLOT_START = 1;
   private static final int INV_SLOT_END = 28;
   private static final int HOTBAR_SLOT_START = 28;
   private static final int HOTBAR_SLOT_END = 37;
   private final Container paymentContainer = new SimpleContainer(1) {
      public boolean canPlaceItem(int slot, ItemStack stack) {
         return ChampionBeaconMenu.isPaymentItem(stack);
      }

      public int getMaxStackSize() {
         return 1;
      }
   };
   private final ChampionBeaconMenu.PaymentSlot paymentSlot;
   private final ContainerLevelAccess access;
   private final ChampionBeaconBlockEntity blockEntity;
   private int levels;
   private int primaryPower;
   private int secondaryPower;
   private int upgraded;

   public ChampionBeaconMenu(int containerId, Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL, null);
   }

   public ChampionBeaconMenu(int containerId, Inventory inventory, ContainerLevelAccess access, ChampionBeaconBlockEntity blockEntity) {
      super((MenuType)CobbleBash.CHAMPION_BEACON_MENU, containerId);
      this.access = access;
      this.blockEntity = blockEntity;
      if (blockEntity != null) {
         this.levels = blockEntity.getLevels();
         this.primaryPower = blockEntity.getPrimaryPower().id();
         this.secondaryPower = blockEntity.getSecondaryPower().id();
         this.upgraded = blockEntity.isUpgraded() ? 1 : 0;
      }

      this.paymentSlot = new ChampionBeaconMenu.PaymentSlot(this.paymentContainer, 0, 80, 133);
      this.addSlot(this.paymentSlot);
      this.addDataSlot(new DataSlot() {
         public int get() {
            return ChampionBeaconMenu.this.levels;
         }

         public void set(int value) {
            ChampionBeaconMenu.this.levels = value;
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return ChampionBeaconMenu.this.primaryPower;
         }

         public void set(int value) {
            ChampionBeaconMenu.this.primaryPower = value;
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return ChampionBeaconMenu.this.secondaryPower;
         }

         public void set(int value) {
            ChampionBeaconMenu.this.secondaryPower = value;
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return ChampionBeaconMenu.this.upgraded;
         }

         public void set(int value) {
            ChampionBeaconMenu.this.upgraded = value;
         }
      });

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j + i * 9 + 9, 36 + j * 18, 160 + i * 18));
         }
      }

      for (int k = 0; k < 9; k++) {
         this.addSlot(new Slot(inventory, k, 36 + k * 18, 218));
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id >= 10 && id < 30) {
         ChampionBeaconPower championbeaconpower2 = ChampionBeaconPower.byId(id - 10);
         if (!this.canSelectPrimary(championbeaconpower2)) {
            return false;
         }

         this.primaryPower = championbeaconpower2.id();
         ChampionBeaconPower championbeaconpower1 = ChampionBeaconPower.byId(this.secondaryPower);
         if (championbeaconpower1 == championbeaconpower2 || conflicts(championbeaconpower2, championbeaconpower1)) {
            this.secondaryPower = ChampionBeaconPower.NONE.id();
         }

         if (!championbeaconpower2.isUpgradeable()) {
            this.upgraded = 0;
         }

         this.broadcastChanges();
         return true;
      } else if (id >= 30 && id < 50) {
         ChampionBeaconPower championbeaconpower = ChampionBeaconPower.byId(id - 30);
         if (this.canSelectSecondary(championbeaconpower)) {
            this.secondaryPower = championbeaconpower.id();
            this.upgraded = 0;
            this.broadcastChanges();
            return true;
         } else {
            return false;
         }
      } else if (id == 50) {
         if (this.canUpgradePrimary()) {
            this.upgraded = this.upgraded == 0 ? 1 : 0;
            if (this.upgraded != 0) {
               this.secondaryPower = ChampionBeaconPower.NONE.id();
            }

            this.broadcastChanges();
            return true;
         } else {
            return false;
         }
      } else {
         return id == 60 ? this.confirmSelection(player) : false;
      }
   }

   private boolean confirmSelection(Player player) {
      ChampionBeaconPower championbeaconpower = this.getPrimaryPower();
      ChampionBeaconPower championbeaconpower1 = this.getSecondaryPower();
      boolean flag = this.isUpgraded();
      if (this.blockEntity == null || !this.paymentSlot.hasItem() || !this.canSelectPrimary(championbeaconpower)) {
         return false;
      }

      if (championbeaconpower1 != ChampionBeaconPower.NONE && !this.canSelectSecondary(championbeaconpower1)) {
         return false;
      }

      if (flag && !this.canUpgradePrimary()) {
         return false;
      }

      ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(this.paymentSlot.getItem().getItem());
      this.paymentSlot.remove(1);
      this.blockEntity.applyPowers(championbeaconpower, championbeaconpower1, flag, resourcelocation);
      this.access.execute(Level::blockEntityChanged);
      this.broadcastChanges();
      return true;
   }

   public void removed(Player player) {
      super.removed(player);
      if (!player.level().isClientSide) {
         ItemStack itemstack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
         if (!itemstack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(itemstack);
         }
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(this.access, player, (Block)CobbleBash.CHAMPION_BEACON);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index == 0) {
            if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else {
            if (isPaymentItem(itemstack1) && this.moveItemStackTo(itemstack1, 0, 1, false)) {
               return ItemStack.EMPTY;
            }

            if (index >= 1 && index < 28) {
               if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= 28 && index < 37) {
               if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, itemstack1);
      }

      return itemstack;
   }

   public int getLevels() {
      return this.levels;
   }

   public ChampionBeaconPower getPrimaryPower() {
      return ChampionBeaconPower.byId(this.primaryPower);
   }

   public ChampionBeaconPower getSecondaryPower() {
      return ChampionBeaconPower.byId(this.secondaryPower);
   }

   public boolean isUpgraded() {
      return this.upgraded != 0;
   }

   public boolean hasPayment() {
      return this.paymentSlot.hasItem();
   }

   public boolean canSelectPrimary(ChampionBeaconPower power) {
      return power.isPrimary() && this.levels >= power.requiredLevel();
   }

   public boolean canSelectSecondary(ChampionBeaconPower power) {
      return power.isSecondary() && this.levels >= 5 && power != this.getPrimaryPower() && !conflicts(this.getPrimaryPower(), power);
   }

   public boolean canUpgradePrimary() {
      return this.levels >= 5 && this.getPrimaryPower().isUpgradeable();
   }

   private static boolean isPaymentItem(ItemStack stack) {
      return stack.is(PAYMENT_ITEMS);
   }

   private static boolean conflicts(ChampionBeaconPower primary, ChampionBeaconPower secondary) {
      return primary == ChampionBeaconPower.REPEL && secondary == ChampionBeaconPower.LURE
         || primary == ChampionBeaconPower.LURE && secondary == ChampionBeaconPower.REPEL;
   }

   private static class PaymentSlot extends Slot {
      PaymentSlot(Container container, int containerIndex, int xPosition, int yPosition) {
         super(container, containerIndex, xPosition, yPosition);
      }

      public boolean mayPlace(ItemStack stack) {
         return ChampionBeaconMenu.isPaymentItem(stack);
      }

      public int getMaxStackSize() {
         return 1;
      }
   }
}
