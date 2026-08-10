package com.nore.cobblebash.item;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ruban de trainer.
 *
 * <p>La version NeoForge implemente {@code ICurioItem} de Curios. Sur Fabric
 * l'equivalent est {@code Accessory} d'Accessories, dont les deux crochets
 * correspondent un pour un ; seul l'ordre des parametres change.
 */
public class TrainerRibbonItem extends Item implements Accessory {
   public TrainerRibbonItem(Properties properties) {
      super(properties);
   }

   @Override
   public void onEquip(ItemStack stack, SlotReference reference) {
      RibbonAttributeManager.equip(slot(reference), RibbonAttributeManager.RibbonKind.TRAINER);
   }

   @Override
   public void onUnequip(ItemStack stack, SlotReference reference) {
      RibbonAttributeManager.unequip(slot(reference), RibbonAttributeManager.RibbonKind.TRAINER);
   }

   private static RibbonSlot slot(SlotReference reference) {
      return RibbonSlot.of(reference.entity(), reference.slotName(), reference.slot());
   }
}
