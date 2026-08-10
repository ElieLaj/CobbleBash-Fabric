package com.nore.cobblebash.integration;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nore.cobblebash.CobbleBash;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class RctTrainerDataLoader {
    private static final Gson GSON = new Gson();

    public static Optional<TrainerData> load(MinecraftServer server, String gymType, String trainerIdPart) {
        if (server == null) {
            return Optional.empty();
        }

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                CobbleBash.MODID,
                "gym_trainers/" + gymType + "/" + trainerIdPart + ".json"
        );

        return server.getResourceManager().getResource(location).flatMap(resource -> {
            try (BufferedReader reader = resource.openAsReader()) {
                TrainerData data = GSON.fromJson(reader, TrainerData.class);
                if (data == null || !data.isValid()) {
                    CobbleBash.LOGGER.warn("Invalid CobbleBash trainer JSON: {}", location);
                    return Optional.empty();
                }

                return Optional.of(data);
            } catch (IOException | JsonParseException exception) {
                CobbleBash.LOGGER.warn("Failed to read CobbleBash trainer JSON: {}", location, exception);
                return Optional.empty();
            }
        });
    }

    public static class TrainerData {
        private String display_name;
        private List<PokemonData> pokemon;
        private List<BuildData> builds;
        private DialogueData dialogue;

        public TrainerData() {
        }

        public TrainerData(String displayName, List<PokemonData> pokemon) {
            this.display_name = displayName;
            this.pokemon = pokemon;
        }

        public String displayName() {
            return display_name == null || display_name.isBlank() ? "Gym Trainer" : display_name;
        }

        public List<PokemonData> pokemon() {
            return pokemon == null ? List.of() : pokemon;
        }

        public List<BuildData> builds() {
            if (builds != null && !builds.isEmpty()) {
                return builds;
            }

            if (!pokemon().isEmpty()) {
                return List.of(new BuildData("default", "Default", pokemon()));
            }

            return List.of();
        }

        public DialogueData dialogue() {
            return dialogue == null ? DialogueData.empty() : dialogue;
        }

        private boolean isValid() {
            return !builds().isEmpty() && builds().stream().allMatch(BuildData::isValid);
        }
    }

    public static class BuildData {
        private String id;
        private String name;
        private List<PokemonData> pokemon;

        public BuildData() {
        }

        public BuildData(String id, String name, List<PokemonData> pokemon) {
            this.id = id;
            this.name = name;
            this.pokemon = pokemon;
        }

        public String id() {
            return id == null || id.isBlank() ? "build" : id;
        }

        public String name() {
            return name == null || name.isBlank() ? id() : name;
        }

        public List<PokemonData> pokemon() {
            return pokemon == null ? List.of() : pokemon;
        }

        private boolean isValid() {
            return !pokemon().isEmpty() && pokemon().size() <= 6 && pokemon().stream().allMatch(PokemonData::isValid);
        }
    }

    public static class DialogueData {
        private List<String> lines;
        private String battle_text;
        private String cancel_text;

        public DialogueData() {
        }

        public List<String> lines() {
            if (lines == null || lines.isEmpty()) {
                return List.of("Care to battle?");
            }

            List<String> filteredLines = lines.stream()
                    .filter(line -> line != null && !line.isBlank())
                    .toList();
            return filteredLines.isEmpty() ? List.of("Care to battle?") : filteredLines;
        }

        public String battleText() {
            return battle_text == null || battle_text.isBlank() ? "Battle" : battle_text;
        }

        public String cancelText() {
            return cancel_text == null || cancel_text.isBlank() ? "Cancel" : cancel_text;
        }

        private static DialogueData empty() {
            return new DialogueData();
        }
    }

    public static class PokemonData {
        private static final Pattern NON_BATTLE_ID_CHARACTER = Pattern.compile("[^a-z0-9]");
        private static final Pattern NON_RESOURCE_ID_CHARACTER = Pattern.compile("[^a-z0-9_./-]");
        private static final Pattern REPEATED_UNDERSCORES = Pattern.compile("_+");
        private String species;
        private List<String> moves;
        private String ability;
        private String held_item;

        public PokemonData() {
        }

        public PokemonData(String species, List<String> moves, String ability, String heldItem) {
            this.species = species;
            this.moves = moves;
            this.ability = ability;
            this.held_item = heldItem;
        }

        public String species() {
            return normalizeBattleId(species);
        }

        public List<String> moves() {
            if (moves == null) {
                return List.of();
            }

            return moves.stream()
                    .map(PokemonData::normalizeBattleId)
                    .filter(move -> !move.isBlank())
                    .toList();
        }

        public String ability() {
            return normalizeBattleId(ability);
        }

        public String heldItem() {
            return normalizeResourceId(held_item);
        }

        private boolean isValid() {
            return !species().isBlank()
                    && !moves().isEmpty()
                    && moves().size() <= 4
                    && isValidOptionalResourceId(held_item);
        }

        private static boolean isValidOptionalResourceId(String value) {
            String normalized = normalizeResourceId(value);
            return normalized.isEmpty() || ResourceLocation.tryParse(normalized) != null;
        }

        private static String normalizeBattleId(String value) {
            if (isEmptyOptionalValue(value)) {
                return "";
            }

            return NON_BATTLE_ID_CHARACTER.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("");
        }

        private static String normalizeResourceId(String value) {
            if (isEmptyOptionalValue(value)) {
                return "";
            }

            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            int namespaceSeparator = trimmed.indexOf(':');
            if (namespaceSeparator >= 0) {
                String namespace = normalizeResourcePath(trimmed.substring(0, namespaceSeparator));
                String path = normalizeResourcePath(trimmed.substring(namespaceSeparator + 1));
                return namespace + ":" + path;
            }

            return normalizeResourcePath(trimmed);
        }

        private static String normalizeResourcePath(String value) {
            String normalized = value
                    .replace(' ', '_')
                    .replace('-', '_');
            normalized = NON_RESOURCE_ID_CHARACTER.matcher(normalized).replaceAll("_");
            return REPEATED_UNDERSCORES.matcher(normalized).replaceAll("_");
        }

        private static boolean isEmptyOptionalValue(String value) {
            return value == null || value.isBlank() || value.trim().equalsIgnoreCase("none");
        }
    }
}
