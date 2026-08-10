package com.nore.cobblebash.integration;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nore.cobblebash.CobbleBash;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

public class RctTrainerDataLoader {
   private static final Gson GSON = new Gson();

   public static Optional<RctTrainerDataLoader.TrainerData> load(MinecraftServer server, String gymType, String trainerIdPart) {
      if (server == null) {
         return Optional.empty();
      }

      ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath("cobblebash", "gym_trainers/" + gymType + "/" + trainerIdPart + ".json");
      return server.getResourceManager()
         .getResource(resourcelocation)
         .flatMap(
            resource -> {
               try (BufferedReader bufferedreader = resource.openAsReader()) {
                  RctTrainerDataLoader.TrainerData rcttrainerdataloader$trainerdata = (RctTrainerDataLoader.TrainerData)GSON.fromJson(
                     bufferedreader, RctTrainerDataLoader.TrainerData.class
                  );
                  if (rcttrainerdataloader$trainerdata != null && rcttrainerdataloader$trainerdata.isValid()) {
                     return Optional.of(rcttrainerdataloader$trainerdata);
                  }

                  CobbleBash.LOGGER.warn("Invalid CobbleBash trainer JSON: {}", resourcelocation);
                  return Optional.empty();
               } catch (IOException | JsonParseException exception) {
                  CobbleBash.LOGGER.warn("Failed to read CobbleBash trainer JSON: {}", resourcelocation, exception);
                  return Optional.empty();
               }
            }
         );
   }

   public static class BuildData {
      private String id;
      private String name;
      private List<RctTrainerDataLoader.PokemonData> pokemon;

      public BuildData() {
      }

      public BuildData(String id, String name, List<RctTrainerDataLoader.PokemonData> pokemon) {
         this.id = id;
         this.name = name;
         this.pokemon = pokemon;
      }

      public String id() {
         return this.id != null && !this.id.isBlank() ? this.id : "build";
      }

      public String name() {
         return this.name != null && !this.name.isBlank() ? this.name : this.id();
      }

      public List<RctTrainerDataLoader.PokemonData> pokemon() {
         return this.pokemon == null ? List.of() : this.pokemon;
      }

      private boolean isValid() {
         return !this.pokemon().isEmpty() && this.pokemon().size() <= 6 && this.pokemon().stream().allMatch(RctTrainerDataLoader.PokemonData::isValid);
      }
   }

   public static class DialogueData {
      private List<String> lines;
      private String battle_text;
      private String cancel_text;

      public List<String> lines() {
         if (this.lines != null && !this.lines.isEmpty()) {
            List<String> list = this.lines.stream().filter(line -> line != null && !line.isBlank()).toList();
            return list.isEmpty() ? List.of("Care to battle?") : list;
         } else {
            return List.of("Care to battle?");
         }
      }

      public String battleText() {
         return this.battle_text != null && !this.battle_text.isBlank() ? this.battle_text : "Battle";
      }

      public String cancelText() {
         return this.cancel_text != null && !this.cancel_text.isBlank() ? this.cancel_text : "Cancel";
      }

      private static RctTrainerDataLoader.DialogueData empty() {
         return new RctTrainerDataLoader.DialogueData();
      }
   }

   public static class PokemonData {
      private static final Pattern NON_BATTLE_ID_CHARACTER = Pattern.compile("[^a-z0-9]");
      private static final Pattern NON_ASPECT_CHARACTER = Pattern.compile("[^a-z0-9_./-]");
      private static final Pattern NON_RESOURCE_ID_CHARACTER = Pattern.compile("[^a-z0-9_./-]");
      private static final Pattern REPEATED_UNDERSCORES = Pattern.compile("_+");
      private String species;
      private List<String> moves;
      private String ability;
      private String held_item;
      private List<String> aspects;
      private Integer level;

      public PokemonData() {
      }

      public PokemonData(String species, List<String> moves, String ability, String heldItem) {
         this.species = species;
         this.moves = moves;
         this.ability = ability;
         this.held_item = heldItem;
      }

      public String species() {
         return normalizeSpeciesId(this.species);
      }

      public List<String> moves() {
         return this.moves == null
            ? List.of()
            : this.moves.stream().map(RctTrainerDataLoader.PokemonData::normalizeBattleId).filter(move -> !move.isBlank()).toList();
      }

      public String ability() {
         return normalizeBattleId(this.ability);
      }

      public String heldItem() {
         return normalizeResourceId(this.held_item);
      }

      public List<String> aspects() {
         return this.aspects == null
            ? List.of()
            : this.aspects.stream().map(RctTrainerDataLoader.PokemonData::normalizeAspect).filter(aspect -> !aspect.isBlank()).distinct().toList();
      }

      public int levelOr(int fallbackLevel) {
         return this.level == null ? fallbackLevel : Math.max(1, Math.min(100, this.level));
      }

      private boolean isValid() {
         return !this.species().isBlank()
            && !this.moves().isEmpty()
            && this.moves().size() <= 4
            && isValidOptionalResourceId(this.held_item)
            && isValidSpeciesId(this.species);
      }

      private static boolean isValidOptionalResourceId(String value) {
         String s = normalizeResourceId(value);
         return s.isEmpty() || ResourceLocation.tryParse(s) != null;
      }

      private static String normalizeBattleId(String value) {
         return isEmptyOptionalValue(value) ? "" : NON_BATTLE_ID_CHARACTER.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("");
      }

      private static boolean isValidSpeciesId(String value) {
         String s = normalizeSpeciesId(value);
         return !s.isEmpty() && (s.indexOf(58) < 0 || ResourceLocation.tryParse(s) != null);
      }

      private static String normalizeSpeciesId(String value) {
         if (isEmptyOptionalValue(value)) {
            return "";
         }

         String s = value.trim().toLowerCase(Locale.ROOT);
         int i = s.indexOf(58);
         return i >= 0 ? normalizeResourceId(s) : normalizeBattleId(s);
      }

      private static String normalizeAspect(String value) {
         if (isEmptyOptionalValue(value)) {
            return "";
         }

         String s = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
         s = NON_ASPECT_CHARACTER.matcher(s).replaceAll("_");
         return REPEATED_UNDERSCORES.matcher(s).replaceAll("_");
      }

      private static String normalizeResourceId(String value) {
         if (isEmptyOptionalValue(value)) {
            return "";
         } else {
            String s = value.trim().toLowerCase(Locale.ROOT);
            int i = s.indexOf(58);
            if (i >= 0) {
               String s1 = normalizeResourcePath(s.substring(0, i));
               String s2 = normalizeResourcePath(s.substring(i + 1));
               return s1 + ":" + s2;
            } else {
               return normalizeResourcePath(s);
            }
         }
      }

      private static String normalizeResourcePath(String value) {
         String s = value.replace(' ', '_').replace('-', '_');
         s = NON_RESOURCE_ID_CHARACTER.matcher(s).replaceAll("_");
         return REPEATED_UNDERSCORES.matcher(s).replaceAll("_");
      }

      private static boolean isEmptyOptionalValue(String value) {
         return value == null || value.isBlank() || value.trim().equalsIgnoreCase("none");
      }
   }

   public static class TrainerData {
      private String display_name;
      private List<RctTrainerDataLoader.PokemonData> pokemon;
      private List<RctTrainerDataLoader.BuildData> builds;
      private RctTrainerDataLoader.DialogueData dialogue;

      public TrainerData() {
      }

      public TrainerData(String displayName, List<RctTrainerDataLoader.PokemonData> pokemon) {
         this.display_name = displayName;
         this.pokemon = pokemon;
      }

      public String displayName() {
         return this.display_name != null && !this.display_name.isBlank() ? this.display_name : "Gym Trainer";
      }

      public List<RctTrainerDataLoader.PokemonData> pokemon() {
         return this.pokemon == null ? List.of() : this.pokemon;
      }

      public List<RctTrainerDataLoader.BuildData> builds() {
         if (this.builds != null && !this.builds.isEmpty()) {
            return this.builds;
         } else {
            return !this.pokemon().isEmpty() ? List.of(new RctTrainerDataLoader.BuildData("default", "Default", this.pokemon())) : List.of();
         }
      }

      public RctTrainerDataLoader.DialogueData dialogue() {
         return this.dialogue == null ? RctTrainerDataLoader.DialogueData.empty() : this.dialogue;
      }

      private boolean isValid() {
         return !this.builds().isEmpty() && this.builds().stream().allMatch(RctTrainerDataLoader.BuildData::isValid);
      }
   }
}
