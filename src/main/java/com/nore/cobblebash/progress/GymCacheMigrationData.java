package com.nore.cobblebash.progress;

import com.nore.cobblebash.structure.EliteFourStructure;
import com.nore.cobblebash.structure.GymPlatformBuilder;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class GymCacheMigrationData extends SavedData {
   private static final String DATA_NAME = "cobblebash_gym_cache_migrations";
   private static final String MIGRATION_ID = "cached_slot_layout_0_1_3_full_slot";
   private static final String MIGRATED_SLOTS_KEY = "migratedSlots";
   private static final Factory<GymCacheMigrationData> FACTORY = new Factory<>(GymCacheMigrationData::new, GymCacheMigrationData::load, null);
   private final Set<String> migratedSlots = new HashSet<>();

   public static GymCacheMigrationData get(MinecraftServer server) {
      return (GymCacheMigrationData)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cobblebash_gym_cache_migrations");
   }

   private static GymCacheMigrationData load(CompoundTag tag, Provider provider) {
      GymCacheMigrationData gymcachemigrationdata = new GymCacheMigrationData();
      if (tag.contains("migratedSlots", 10)) {
         CompoundTag compoundtag = tag.getCompound("migratedSlots");

         for (String s : compoundtag.getAllKeys()) {
            if (compoundtag.getBoolean(s)) {
               gymcachemigrationdata.migratedSlots.add(s);
            }
         }
      }

      return gymcachemigrationdata;
   }

   private void migrateSlotIfNeeded(String gymType, int slotId, Runnable migration) {
      String s = "cached_slot_layout_0_1_3_full_slot:" + gymType + ":" + slotId;
      if (!this.migratedSlots.contains(s)) {
         migration.run();
         this.migratedSlots.add(s);
         this.setDirty();
      }
   }

   public void migrateGymSlotIfNeeded(ServerLevel level, String gymType, int slotId, BlockPos origin) {
      this.migrateSlotIfNeeded(gymType, slotId, () -> {
         if ("elite4".equals(gymType)) {
            EliteFourStructure.clearCachedBlocks(level, origin);
         } else {
            GymPlatformBuilder.clearCachedGymBlocks(level, origin, gymType);
         }
      });
   }

   public CompoundTag save(CompoundTag tag, Provider provider) {
      CompoundTag compoundtag = new CompoundTag();

      for (String s : this.migratedSlots) {
         compoundtag.putBoolean(s, true);
      }

      tag.put("migratedSlots", compoundtag);
      return tag;
   }
}
