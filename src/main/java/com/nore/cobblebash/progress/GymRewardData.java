package com.nore.cobblebash.progress;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class GymRewardData extends SavedData {
   private static final String DATA_NAME = "cobblebash_gym_rewards";
   private static final String TRAINER_RIBBON_GYMS_KEY = "trainerRibbonGyms";
   private static final String ELITE_FOUR_DISK_AWARDED_KEY = "eliteFourDiskAwarded";
   private static final Factory<GymRewardData> FACTORY = new Factory<>(GymRewardData::new, GymRewardData::load, null);
   private final CompoundTag trainerRibbonGyms = new CompoundTag();
   private final CompoundTag eliteFourDiskAwarded = new CompoundTag();

   public static GymRewardData get(MinecraftServer server) {
      return (GymRewardData)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cobblebash_gym_rewards");
   }

   private static GymRewardData load(CompoundTag tag, Provider provider) {
      GymRewardData gymrewarddata = new GymRewardData();
      if (tag.contains("trainerRibbonGyms", 10)) {
         gymrewarddata.trainerRibbonGyms.merge(tag.getCompound("trainerRibbonGyms"));
      }

      if (tag.contains("eliteFourDiskAwarded", 10)) {
         gymrewarddata.eliteFourDiskAwarded.merge(tag.getCompound("eliteFourDiskAwarded"));
      }

      return gymrewarddata;
   }

   public Optional<String> getTrainerRibbonGym(UUID playerId) {
      String s = playerId.toString();
      return !this.trainerRibbonGyms.contains(s, 8) ? Optional.empty() : Optional.of(this.trainerRibbonGyms.getString(s));
   }

   public String getOrSetTrainerRibbonGym(UUID playerId, String gymType) {
      Optional<String> optional = this.getTrainerRibbonGym(playerId);
      if (optional.isPresent()) {
         return optional.get();
      }

      this.trainerRibbonGyms.putString(playerId.toString(), gymType);
      this.setDirty();
      return gymType;
   }

   public boolean hasEliteFourDiskAwarded(UUID playerId) {
      return this.eliteFourDiskAwarded.getBoolean(playerId.toString());
   }

   public boolean markEliteFourDiskAwarded(UUID playerId) {
      if (this.hasEliteFourDiskAwarded(playerId)) {
         return false;
      }

      this.eliteFourDiskAwarded.putBoolean(playerId.toString(), true);
      this.setDirty();
      return true;
   }

   public CompoundTag save(CompoundTag tag, Provider provider) {
      tag.put("trainerRibbonGyms", this.trainerRibbonGyms.copy());
      tag.put("eliteFourDiskAwarded", this.eliteFourDiskAwarded.copy());
      return tag;
   }
}
