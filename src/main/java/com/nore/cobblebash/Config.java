package com.nore.cobblebash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration.
 *
 * <p>L'original passe par {@code ModConfigSpec}, propre a NeoForge. Fabric n'a
 * pas d'equivalent en API de base : on lit un JSON, cree avec ses valeurs par
 * defaut au premier lancement. Les bornes de l'original sont conservees, et un
 * reglage hors bornes est ramene dedans plutot que de faire echouer le
 * chargement.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> DEFAULT_BLACKLIST = List.of(
            "minecraft:ender_pearl",
            "minecraft:chorus_fruit",
            "minecraft:firework_rocket"
    );

    private static Values values = new Values();

    private Config() {
    }

    /** Modele de serialisation : les memes cinq reglages que la version NeoForge. */
    private static final class Values {
        List<String> gymItemBlacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        int cobbleDollarsRepeatTrainerReward = 500;
        int cobbleDollarsRepeatBossReward = 1500;
        double repeatClearTrainerXpMultiplier = 1.2D;
        double repeatClearBossXpMultiplier = 1.5D;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CobbleBash.MODID + ".json");

        if (Files.notExists(path)) {
            write(path, new Values());
            values = new Values();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Values read = GSON.fromJson(reader, Values.class);
            values = read == null ? new Values() : read;
        } catch (IOException | RuntimeException exception) {
            CobbleBash.LOGGER.warn("Could not read {}, falling back to defaults.", path, exception);
            values = new Values();
        }

        if (values.gymItemBlacklist == null) {
            values.gymItemBlacklist = new ArrayList<>(DEFAULT_BLACKLIST);
        }
        values.cobbleDollarsRepeatTrainerReward = Math.max(0, values.cobbleDollarsRepeatTrainerReward);
        values.cobbleDollarsRepeatBossReward = Math.max(0, values.cobbleDollarsRepeatBossReward);
        values.repeatClearTrainerXpMultiplier = clamp(values.repeatClearTrainerXpMultiplier);
        values.repeatClearBossXpMultiplier = clamp(values.repeatClearBossXpMultiplier);

        // Une entree mal orthographiee ne bloquerait jamais rien : on le dit,
        // plutot que de laisser croire que l'item est interdit.
        for (String id : values.gymItemBlacklist) {
            if (!isKnownItem(id)) {
                CobbleBash.LOGGER.warn("gymItemBlacklist: unknown item '{}', it will never match.", id);
            }
        }
    }

    private static double clamp(double v) {
        return Math.min(100.0D, Math.max(1.0D, v));
    }

    private static void write(Path path, Values v) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(v, writer);
            }
        } catch (IOException exception) {
            CobbleBash.LOGGER.warn("Could not write {}.", path, exception);
        }
    }

    public static boolean isGymBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return values.gymItemBlacklist.contains(itemId.toString());
    }

    public static int cobbleDollarsRepeatTrainerReward() {
        return values.cobbleDollarsRepeatTrainerReward;
    }

    public static int cobbleDollarsRepeatBossReward() {
        return values.cobbleDollarsRepeatBossReward;
    }

    public static double repeatClearTrainerXpMultiplier() {
        return values.repeatClearTrainerXpMultiplier;
    }

    public static double repeatClearBossXpMultiplier() {
        return values.repeatClearBossXpMultiplier;
    }

    private static boolean isKnownItem(String itemName) {
        try {
            return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
