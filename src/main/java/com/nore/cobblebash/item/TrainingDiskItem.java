package com.nore.cobblebash.item;

import com.nore.cobblebash.gym.GymType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class TrainingDiskItem extends Item {
   private final GymType gymType;

   public TrainingDiskItem(GymType gymType, Properties properties) {
      super(properties);
      this.gymType = gymType;
   }

   public GymType getGymType() {
      return this.gymType;
   }
}
