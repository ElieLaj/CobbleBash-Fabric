package com.nore.cobblebash.dimension;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class CobbleBashDimensions {
    public static final ResourceKey<Level> GYM_VOID = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gym_void")
    );
}