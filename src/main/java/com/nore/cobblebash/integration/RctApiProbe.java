package com.nore.cobblebash.integration;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerRegistry;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class RctApiProbe {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Pattern GYM_TRAINER_ID_PATTERN = Pattern.compile("^cobblebash_([a-z0-9_]+)_slot_([0-9]+)_(trainer_1|trainer_2|boss)$");

   public static void logLoaded() {
      RCTApi.initInstance("cobblebash");
      LOGGER.info("CobbleBash RCT API instance initialized.");
   }

   public static void registerTestTrainer(MinecraftServer server) {
      registerGymTrainer(server, "bug", 0, "trainer_1", 10);
   }

   public static boolean registerGymTrainer(MinecraftServer server, String gymType, int slotId, String trainerIdPart, int level) {
      return registerGymTrainer(server, gymType, slotId, trainerIdPart, level, null);
   }

   public static boolean registerGymTrainer(MinecraftServer server, String gymType, int slotId, String trainerIdPart, int level, String displayNameOverride) {
      RCTApi rctapi = RCTApi.getInstance("cobblebash");
      if (rctapi == null) {
         LOGGER.error("RCT API instance not found.");
         return false;
      }

      Optional<TrainerModel> optional = RctGymTrainerFactory.createTrainer(server, gymType, trainerIdPart, level, displayNameOverride);
      if (optional.isEmpty()) {
         LOGGER.error("No JSON RCT trainer data exists for {} {}.", gymType, trainerIdPart);
         return false;
      }

      TrainerRegistry trainerregistry = rctapi.getTrainerRegistry();
      trainerregistry.init(server);
      String s = getTrainerId(gymType, slotId, trainerIdPart);
      trainerregistry.unregisterById(s);
      TrainerModel trainermodel = optional.get();

      try {
         trainerregistry.registerNPC(s, trainermodel);
      } catch (RuntimeException runtimeexception) {
         trainerregistry.unregisterById(s);
         LOGGER.error("Failed to register RCT trainer {} from JSON. Team: {}", new Object[]{s, describeTeam(trainermodel), runtimeexception});
         return false;
      }

      LOGGER.info("Registered RCT trainer: {}", s);
      return true;
   }

   public static TrainerNPC getGymTrainer(String gymType, int slotId, String trainerIdPart) {
      RCTApi rctapi = RCTApi.getInstance("cobblebash");
      if (rctapi == null) {
         LOGGER.error("RCT API instance not found.");
         return null;
      } else {
         return (TrainerNPC)rctapi.getTrainerRegistry().getById(getTrainerId(gymType, slotId, trainerIdPart), TrainerNPC.class);
      }
   }

   public static TrainerNPC getTestTrainer() {
      return getGymTrainer("bug", 0, "trainer_1");
   }

   public static boolean startTestBattle(ServerPlayer player) {
      return startGymBattle(player, "bug", 0, "trainer_1");
   }

   public static boolean startGymBattle(ServerPlayer player, String gymType, int slotId, String trainerIdPart) {
      RCTApi rctapi = RCTApi.getInstance("cobblebash");
      if (rctapi == null) {
         LOGGER.error("RCT API instance not found.");
         return false;
      } else {
         TrainerRegistry trainerregistry = rctapi.getTrainerRegistry();
         String s = getTrainerId(gymType, slotId, trainerIdPart);
         TrainerNPC trainernpc = (TrainerNPC)trainerregistry.getById(s, TrainerNPC.class);
         if (trainernpc == null) {
            LOGGER.error("Trainer NPC not found: {}", s);
            return false;
         } else if (trainernpc.getEntity() == null) {
            LOGGER.error("Trainer NPC is not attached to an entity: {}", s);
            return false;
         } else {
            TrainerPlayer trainerplayer = trainerregistry.registerPlayer("cobblebash_player_" + player.getUUID(), player);
            return rctapi.getBattleManager().startSingle(trainerplayer, trainernpc);
         }
      }
   }

   public static void unregisterGymTrainers(String gymType, int slotId) {
      RCTApi rctapi = RCTApi.getInstance("cobblebash");
      if (rctapi != null) {
         TrainerRegistry trainerregistry = rctapi.getTrainerRegistry();
         trainerregistry.unregisterById(getTrainerId(gymType, slotId, "trainer_1"));
         trainerregistry.unregisterById(getTrainerId(gymType, slotId, "trainer_2"));
         trainerregistry.unregisterById(getTrainerId(gymType, slotId, "boss"));
      }
   }

   public static String getTrainerId(String gymType, int slotId, String trainerIdPart) {
      return "cobblebash_" + gymType + "_slot_" + slotId + "_" + trainerIdPart;
   }

   public static String getTrainerDisplayName(MinecraftServer server, String gymType, String trainerIdPart) {
      return RctGymTrainerFactory.getTrainerDisplayName(server, gymType, trainerIdPart).orElse("Gym Trainer");
   }

   private static String describeTeam(TrainerModel trainer) {
      if (trainer != null && trainer.getTeam() != null) {
         StringBuilder stringbuilder = new StringBuilder();

         for (PokemonModel pokemonmodel : trainer.getTeam()) {
            if (!stringbuilder.isEmpty()) {
               stringbuilder.append("; ");
            }

            stringbuilder.append(pokemonmodel.getSpecies())
               .append(" lv")
               .append(pokemonmodel.getLevel())
               .append(" ability=")
               .append(pokemonmodel.getAbility())
               .append(" heldItems=")
               .append(Arrays.toString(pokemonmodel.getHeldItems()))
               .append(" aspects=")
               .append(pokemonmodel.getAspects())
               .append(" moves=")
               .append(pokemonmodel.getMoveset());
         }

         return stringbuilder.toString();
      } else {
         return "empty";
      }
   }

   public static RctApiProbe.GymTrainerRef getGymTrainerRef(LivingEntity entity) {
      if (entity == null) {
         return null;
      }

      for (String s : entity.getTags()) {
         RctApiProbe.GymTrainerRef rctapiprobe$gymtrainerref = parseGymTrainerId(s);
         if (rctapiprobe$gymtrainerref != null) {
            return rctapiprobe$gymtrainerref;
         }
      }

      return null;
   }

   private static RctApiProbe.GymTrainerRef parseGymTrainerId(String trainerId) {
      Matcher matcher = GYM_TRAINER_ID_PATTERN.matcher(trainerId);
      return !matcher.matches() ? null : new RctApiProbe.GymTrainerRef(matcher.group(1), Integer.parseInt(matcher.group(2)), matcher.group(3));
   }

   public record GymTrainerRef(String gymType, int slotId, String trainerIdPart) {
   }
}
