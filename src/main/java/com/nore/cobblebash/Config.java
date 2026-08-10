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
 * pas d'equivalent en API de base, et tirer une bibliotheque de configuration
 * pour une seule liste d'items serait disproportionne : on lit un JSON ecrit a
 * la main, cree avec ses valeurs par defaut au premier lancement.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> DEFAULT_BLACKLIST = List.of(
            "minecraft:ender_pearl",
            "minecraft:chorus_fruit",
            "minecraft:firework_rocket"
    );

    private static List<String> gymItemBlacklist = DEFAULT_BLACKLIST;

    private Config() {
    }

    /** Modele de serialisation : un seul champ, comme la config d'origine. */
    private static final class Values {
        List<String> gymItemBlacklist = new ArrayList<>(DEFAULT_BLACKLIST);
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CobbleBash.MODID + ".json");

        if (Files.notExists(path)) {
            write(path, new Values());
            gymItemBlacklist = DEFAULT_BLACKLIST;
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Values values = GSON.fromJson(reader, Values.class);
            gymItemBlacklist = values == null || values.gymItemBlacklist == null
                    ? DEFAULT_BLACKLIST
                    : List.copyOf(values.gymItemBlacklist);
        } catch (IOException | RuntimeException exception) {
            CobbleBash.LOGGER.warn("Could not read {}, falling back to defaults.", path, exception);
            gymItemBlacklist = DEFAULT_BLACKLIST;
        }

        // Une entree mal orthographiee ne bloquerait jamais rien : on le dit,
        // plutot que de laisser le joueur croire que l'item est interdit.
        for (String id : gymItemBlacklist) {
            if (!isKnownItem(id)) {
                CobbleBash.LOGGER.warn("gymItemBlacklist: unknown item '{}', it will never match.", id);
            }
        }
    }

    private static void write(Path path, Values values) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(values, writer);
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
        return gymItemBlacklist.contains(itemId.toString());
    }

    private static boolean isKnownItem(String itemName) {
        try {
            return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
