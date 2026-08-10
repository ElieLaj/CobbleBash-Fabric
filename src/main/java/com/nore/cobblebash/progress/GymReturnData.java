package com.nore.cobblebash.progress;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;
import java.util.UUID;

public class GymReturnData extends SavedData {
    private static final String DATA_NAME = "cobblebash_gym_returns";
    // Le troisieme parametre est le type de DataFixer. NeoForge ajoute une
    // surcharge a deux arguments qui le met a null ; en vanilla il faut
    // l'ecrire, et null convient : ces donnees ne sont pas versionnees.
    private static final SavedData.Factory<GymReturnData> FACTORY = new SavedData.Factory<>(
            GymReturnData::new,
            GymReturnData::load,
            null
    );

    private final CompoundTag returns = new CompoundTag();

    public static GymReturnData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static GymReturnData load(CompoundTag tag, HolderLookup.Provider provider) {
        GymReturnData data = new GymReturnData();
        if (tag.contains("returns", Tag.TAG_COMPOUND)) {
            data.returns.merge(tag.getCompound("returns"));
        }
        return data;
    }

    public void put(UUID playerId, ReturnLocation location) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", location.dimension().location().toString());
        tag.putDouble("x", location.x());
        tag.putDouble("y", location.y());
        tag.putDouble("z", location.z());
        tag.putFloat("yRot", location.yRot());
        tag.putFloat("xRot", location.xRot());

        returns.put(playerId.toString(), tag);
        setDirty();
    }

    public Optional<ReturnLocation> get(UUID playerId) {
        String key = playerId.toString();
        if (!returns.contains(key, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag tag = returns.getCompound(key);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimensionId == null) {
            return Optional.empty();
        }

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return Optional.of(new ReturnLocation(
                dimension,
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getFloat("yRot"),
                tag.getFloat("xRot")
        ));
    }

    public Optional<ReturnLocation> remove(UUID playerId) {
        Optional<ReturnLocation> location = get(playerId);
        if (returns.contains(playerId.toString())) {
            returns.remove(playerId.toString());
            setDirty();
        }
        return location;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("returns", returns.copy());
        return tag;
    }

    public record ReturnLocation(
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
        public static ReturnLocation from(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
            return new ReturnLocation(level.dimension(), x, y, z, yRot, xRot);
        }
    }
}
