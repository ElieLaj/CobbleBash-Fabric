package com.nore.cobblebash.integration;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel.StatsModel;
import com.gitlab.srcmc.rctapi.api.util.JTO;
import com.gitlab.srcmc.rctapi.api.util.Text;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class RctGymTrainerFactory {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Optional<TrainerModel> createTrainer(MinecraftServer server, String gymType, String trainerIdPart, int level) {
      return createTrainer(server, gymType, trainerIdPart, level, null);
   }

   public static Optional<TrainerModel> createTrainer(MinecraftServer server, String gymType, String trainerIdPart, int level, String displayNameOverride) {
      return RctTrainerDataLoader.load(server, gymType, trainerIdPart).map(data -> {
         RctTrainerDataLoader.BuildData rcttrainerdataloader$builddata = selectBuild(data.builds());
         List<RctTrainerDataLoader.PokemonData> list = new ArrayList<>(rcttrainerdataloader$builddata.pokemon());
         Collections.shuffle(list);
         String s = displayNameOverride != null && !displayNameOverride.isBlank() ? displayNameOverride : data.displayName();
         return new TrainerModel(Text.literal(s), JTO.of(RCTBattleAI::new), List.of(), list.stream().map(pokemon -> createPokemon(pokemon, level)).toList());
      });
   }

   public static Optional<String> getTrainerDisplayName(MinecraftServer server, String gymType, String trainerIdPart) {
      return RctTrainerDataLoader.load(server, gymType, trainerIdPart).map(RctTrainerDataLoader.TrainerData::displayName);
   }

   private static PokemonModel createPokemon(RctTrainerDataLoader.PokemonData data, int level) {
      StatsModel statsmodel = new StatsModel(31, 31, 31, 31, 31, 31);
      StatsModel statsmodel1 = new StatsModel(0, 0, 0, 0, 0, 0);
      return new PokemonModel(
         resolveSpeciesForRct(data.species()),
         "MALE",
         data.levelOr(level),
         "hardy",
         data.ability(),
         new LinkedHashSet<>(data.moves()),
         statsmodel,
         statsmodel1,
         false,
         data.heldItem(),
         new LinkedHashSet<>(data.aspects())
      );
   }

   private static String resolveSpeciesForRct(String requestedSpecies) {
      String s = requestedSpecies == null ? "" : requestedSpecies.trim().toLowerCase(Locale.ROOT);
      if (s.isBlank()) {
         return "";
      }

      for (ResourceLocation resourcelocation : speciesIdCandidates(s)) {
         Species species = PokemonSpecies.getByIdentifier(resourcelocation);
         if (species != null) {
            return species.getResourceIdentifier().toString();
         }
      }

      String s2 = s.contains(":") ? s.substring(s.indexOf(58) + 1) : s;
      int i = s2.lastIndexOf(47);
      String s3 = s2;
      if (i >= 0) {
         s3 = s2.substring(i + 1);
      }

      String s1 = s3;
      Species species1 = PokemonSpecies.getByName(s1);
      if (species1 != null) {
         return species1.getResourceIdentifier().toString();
      }

      Species species2 = PokemonSpecies.getSpecies().stream().filter(species -> speciesMatches(species, s, s1)).findFirst().orElse(null);
      if (species2 != null) {
         return species2.getResourceIdentifier().toString();
      }

      LOGGER.warn("Could not resolve CobbleBash trainer species '{}'. Close loaded Cobblemon species: {}", requestedSpecies, describeCloseSpeciesMatches(s1));
      return s;
   }

   private static List<ResourceLocation> speciesIdCandidates(String requested) {
      List<ResourceLocation> list = new ArrayList<>();
      if (requested.contains(":")) {
         ResourceLocation resourcelocation = ResourceLocation.tryParse(requested);
         if (resourcelocation != null) {
            list.add(resourcelocation);
         }

         return list;
      } else {
         list.add(ResourceLocation.fromNamespaceAndPath("cobblemon", requested));
         list.add(ResourceLocation.fromNamespaceAndPath("cobblemon", "custom/" + requested));
         list.add(ResourceLocation.fromNamespaceAndPath("cobblemon_alatia", requested));
         return list;
      }
   }

   private static boolean speciesMatches(Species species, String requested, String nameCandidate) {
      ResourceLocation resourcelocation = species.getResourceIdentifier();
      String s = resourcelocation.getPath();
      return species.getName().equalsIgnoreCase(nameCandidate)
         || resourcelocation.toString().equalsIgnoreCase(requested)
         || s.equalsIgnoreCase(nameCandidate)
         || s.endsWith("/" + nameCandidate);
   }

   private static String describeCloseSpeciesMatches(String nameCandidate) {
      List<String> list = PokemonSpecies.getSpecies().stream().filter(species -> {
         String s = species.getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
         String s1 = species.getName().toLowerCase(Locale.ROOT);
         return s.contains(nameCandidate) || s1.contains(nameCandidate);
      }).limit(8L).map(species -> species.getResourceIdentifier() + " (" + species.getName() + ")").toList();
      return list.isEmpty() ? "none" : String.join(", ", list);
   }

   private static RctTrainerDataLoader.BuildData selectBuild(List<RctTrainerDataLoader.BuildData> builds) {
      return builds.get(ThreadLocalRandom.current().nextInt(builds.size()));
   }
}
