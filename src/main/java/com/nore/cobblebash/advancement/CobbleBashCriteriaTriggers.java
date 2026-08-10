package com.nore.cobblebash.advancement;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CobbleBashCriteriaTriggers {
    // Enregistrement direct, sans registre differe : Fabric appelle
    // l'initialiseur du mod avant le chargement des donnees.
    public static GymBossDefeatedTrigger GYM_BOSS_DEFEATED;

    public static void bootstrap() {
        GYM_BOSS_DEFEATED = Registry.register(
                BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gym_boss_defeated"),
                new GymBossDefeatedTrigger()
        );
    }

    public static void triggerGymBossDefeated(ServerPlayer player, String gymType) {
        GYM_BOSS_DEFEATED.trigger(player, gymType);
    }
}
