package com.nore.cobblebash.progress;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class GymReturnData extends SavedData {
   private static final String DATA_NAME = "cobblebash_gym_returns";
   private static final Factory<GymReturnData> FACTORY = new Factory<>(GymReturnData::new, GymReturnData::load, null);
   private final CompoundTag returns = new CompoundTag();

   public static GymReturnData get(MinecraftServer server) {
      return (GymReturnData)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cobblebash_gym_returns");
   }

   private static GymReturnData load(CompoundTag tag, Provider provider) {
      GymReturnData gymreturndata = new GymReturnData();
      if (tag.contains("returns", 10)) {
         gymreturndata.returns.merge(tag.getCompound("returns"));
      }

      return gymreturndata;
   }

   public void put(UUID playerId, GymReturnData.ReturnLocation location) {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("dimension", location.dimension().location().toString());
      compoundtag.putDouble("x", location.x());
      compoundtag.putDouble("y", location.y());
      compoundtag.putDouble("z", location.z());
      compoundtag.putFloat("yRot", location.yRot());
      compoundtag.putFloat("xRot", location.xRot());
      this.returns.put(playerId.toString(), compoundtag);
      this.setDirty();
   }

   public Optional<GymReturnData.ReturnLocation> get(UUID playerId) {
      String s = playerId.toString();
      if (!this.returns.contains(s, 10)) {
         return Optional.empty();
      }

      CompoundTag compoundtag = this.returns.getCompound(s);
      ResourceLocation resourcelocation = ResourceLocation.tryParse(compoundtag.getString("dimension"));
      if (resourcelocation == null) {
         return Optional.empty();
      }

      ResourceKey<Level> resourcekey = ResourceKey.create(Registries.DIMENSION, resourcelocation);
      return Optional.of(
         new GymReturnData.ReturnLocation(
            resourcekey,
            compoundtag.getDouble("x"),
            compoundtag.getDouble("y"),
            compoundtag.getDouble("z"),
            compoundtag.getFloat("yRot"),
            compoundtag.getFloat("xRot")
         )
      );
   }

   public Optional<GymReturnData.ReturnLocation> remove(UUID playerId) {
      Optional<GymReturnData.ReturnLocation> optional = this.get(playerId);
      if (this.returns.contains(playerId.toString())) {
         this.returns.remove(playerId.toString());
         this.setDirty();
      }

      return optional;
   }

   public CompoundTag save(CompoundTag tag, Provider provider) {
      tag.put("returns", this.returns.copy());
      return tag;
   }

   public record ReturnLocation(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
      public static GymReturnData.ReturnLocation from(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
         return new GymReturnData.ReturnLocation(level.dimension(), x, y, z, yRot, xRot);
      }
   }
}
