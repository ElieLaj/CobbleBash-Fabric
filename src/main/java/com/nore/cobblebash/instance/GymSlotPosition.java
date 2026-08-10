package com.nore.cobblebash.instance;

import net.minecraft.core.BlockPos;

public class GymSlotPosition {
   private static final int SLOTS_PER_ROW = 32;
   private static final int SLOT_SPACING = 2000;
   private static final int BASE_Y = 80;

   public static BlockPos getOriginForSlot(int slotId) {
      int i = slotId % 32;
      int j = slotId / 32;
      return new BlockPos(i * 2000, 80, j * 2000);
   }
}
