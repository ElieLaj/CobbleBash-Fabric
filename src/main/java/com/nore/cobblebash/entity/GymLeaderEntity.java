package com.nore.cobblebash.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GymLeaderEntity extends GymTrainerEntity {
   public static final int MODEL_VARIANT_COUNT = 9;

   public GymLeaderEntity(EntityType<? extends GymLeaderEntity> entityType, Level level) {
      super(entityType, level);
   }

   @Override
   protected int modelVariantCount() {
      return 9;
   }
}
