package com.nore.cobblebash.instance;

import net.minecraft.core.BlockPos;

public class GymSlotPosition {
    private static final int SLOTS_PER_ROW = 32;
    private static final int SLOT_SPACING = 2000;
    private static final int BASE_Y = 80;

    public static BlockPos getOriginForSlot(int slotId) {
        int slotX = slotId % SLOTS_PER_ROW;
        int slotZ = slotId / SLOTS_PER_ROW;

        return new BlockPos(
                slotX * SLOT_SPACING,
                BASE_Y,
                slotZ * SLOT_SPACING
        );
    }
}