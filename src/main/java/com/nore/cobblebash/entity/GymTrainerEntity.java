package com.nore.cobblebash.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GymTrainerEntity extends PathfinderMob {
   private static final EntityDataAccessor<Integer> DATA_MODEL_VARIANT = SynchedEntityData.defineId(GymTrainerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TEXTURE_VARIANT = SynchedEntityData.defineId(GymTrainerEntity.class, EntityDataSerializers.INT);
   private static final String MODEL_VARIANT_TAG = "CobbleBashTrainerModel";
   private static final String TEXTURE_VARIANT_TAG = "CobbleBashTrainerTexture";
   public static final int MODEL_VARIANT_COUNT = 4;
   public static final int TEXTURE_VARIANT_COUNT = 16;

   public GymTrainerEntity(EntityType<? extends GymTrainerEntity> entityType, Level level) {
      super(entityType, level);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_MODEL_VARIANT, 0);
      builder.define(DATA_TEXTURE_VARIANT, 0);
   }

   public int modelVariant() {
      return Mth.clamp((Integer)this.entityData.get(DATA_MODEL_VARIANT), 0, this.modelVariantCount() - 1);
   }

   public int textureVariant() {
      return Math.floorMod((Integer)this.entityData.get(DATA_TEXTURE_VARIANT), 16);
   }

   public void setVisual(int modelVariant, int textureVariant) {
      this.entityData.set(DATA_MODEL_VARIANT, Math.floorMod(modelVariant, this.modelVariantCount()));
      this.entityData.set(DATA_TEXTURE_VARIANT, Math.floorMod(textureVariant, 16));
   }

   protected int modelVariantCount() {
      return 4;
   }

   public void tick() {
      super.tick();
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      if (!this.level().isClientSide) {
         this.faceNearestPlayer();
      }
   }

   public boolean isPushable() {
      return false;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public void push(Entity entity) {
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putInt("CobbleBashTrainerModel", this.modelVariant());
      compound.putInt("CobbleBashTrainerTexture", this.textureVariant());
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.setVisual(compound.getInt("CobbleBashTrainerModel"), compound.getInt("CobbleBashTrainerTexture"));
   }

   private void faceNearestPlayer() {
      Player player = this.level().getNearestPlayer(this, 24.0);
      if (player != null) {
         double d0 = player.getX() - this.getX();
         double d1 = player.getZ() - this.getZ();
         double d2 = player.getEyeY() - this.getEyeY();
         double d3 = Math.sqrt(d0 * d0 + d1 * d1);
         float f = (float)(Mth.atan2(d1, d0) * 180.0F / (float)Math.PI) - 90.0F;
         float f1 = (float)(-(Mth.atan2(d2, d3) * 180.0F / (float)Math.PI));
         this.setYRot(f);
         this.setYBodyRot(f);
         this.setYHeadRot(f);
         this.setXRot(Mth.clamp(f1, -18.0F, 18.0F));
      }
   }
}
