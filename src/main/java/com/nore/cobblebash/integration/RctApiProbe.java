package com.nore.cobblebash.integration;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.regex.Pattern;

public class RctApiProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern GYM_TRAINER_ID_PATTERN = Pattern.compile("^cobblebash_([a-z]+)_slot_([0-9]+)_(trainer_1|trainer_2|boss)$");

    public static void logLoaded() {
        RCTApi.initInstance("cobblebash");

        LOGGER.info("CobbleBash RCT API instance initialized.");
    }

    public static void registerTestTrainer(net.minecraft.server.MinecraftServer server) {
        registerGymTrainer(server, "bug", 0, "trainer_1", 10);
    }

    public static boolean registerGymTrainer(net.minecraft.server.MinecraftServer server, String gymType, int slotId, String trainerIdPart, int level) {
        var api = RCTApi.getInstance("cobblebash");

        if (api == null) {
            LOGGER.error("RCT API instance not found.");
            return false;
        }

        var trainer = RctGymTrainerFactory.createTrainer(server, gymType, trainerIdPart, level);
        if (trainer.isEmpty()) {
            LOGGER.error("No JSON RCT trainer data exists for {} {}.", gymType, trainerIdPart);
            return false;
        }

        var registry = api.getTrainerRegistry();
        registry.init(server);

        String trainerId = getTrainerId(gymType, slotId, trainerIdPart);
        registry.unregisterById(trainerId);

        try {
            registry.registerNPC(trainerId, trainer.get());
        } catch (RuntimeException exception) {
            registry.unregisterById(trainerId);
            LOGGER.error("Failed to register RCT trainer {} from JSON.", trainerId, exception);
            return false;
        }

        LOGGER.info("Registered RCT trainer: {}", trainerId);
        return true;
    }

    public static TrainerNPC getGymTrainer(String gymType, int slotId, String trainerIdPart) {
        var api = RCTApi.getInstance("cobblebash");

        if (api == null) {
            LOGGER.error("RCT API instance not found.");
            return null;
        }

        return api.getTrainerRegistry().getById(getTrainerId(gymType, slotId, trainerIdPart), TrainerNPC.class);
    }

    public static TrainerNPC getTestTrainer() {
        return getGymTrainer("bug", 0, "trainer_1");
    }

    public static boolean startTestBattle(ServerPlayer player) {
        return startGymBattle(player, "bug", 0, "trainer_1");
    }

    public static boolean startGymBattle(ServerPlayer player, String gymType, int slotId, String trainerIdPart) {
        var api = RCTApi.getInstance("cobblebash");

        if (api == null) {
            LOGGER.error("RCT API instance not found.");
            return false;
        }

        var registry = api.getTrainerRegistry();
        String trainerId = getTrainerId(gymType, slotId, trainerIdPart);

        var trainerNpc = registry.getById(trainerId, TrainerNPC.class);

        if (trainerNpc == null) {
            LOGGER.error("Trainer NPC not found: {}", trainerId);
            return false;
        }

        if (trainerNpc.getEntity() == null) {
            LOGGER.error("Trainer NPC is not attached to an entity: {}", trainerId);
            return false;
        }

        var trainerPlayer = registry.registerPlayer("cobblebash_player_" + player.getUUID(), player);

        return api.getBattleManager().startSingle(trainerPlayer, trainerNpc);
    }

    public static void unregisterGymTrainers(String gymType, int slotId) {
        var api = RCTApi.getInstance("cobblebash");

        if (api == null) {
            return;
        }

        var registry = api.getTrainerRegistry();
        registry.unregisterById(getTrainerId(gymType, slotId, "trainer_1"));
        registry.unregisterById(getTrainerId(gymType, slotId, "trainer_2"));
        registry.unregisterById(getTrainerId(gymType, slotId, "boss"));
    }

    public static String getTrainerId(String gymType, int slotId, String trainerIdPart) {
        return "cobblebash_" + gymType + "_slot_" + slotId + "_" + trainerIdPart;
    }

    public static String getTrainerDisplayName(net.minecraft.server.MinecraftServer server, String gymType, String trainerIdPart) {
        return RctGymTrainerFactory.getTrainerDisplayName(server, gymType, trainerIdPart).orElse("Gym Trainer");
    }

    public static GymTrainerRef getGymTrainerRef(LivingEntity entity) {
        if (entity == null) {
            return null;
        }

        for (String tag : entity.getTags()) {
            GymTrainerRef ref = parseGymTrainerId(tag);
            if (ref != null) {
                return ref;
            }
        }

        return null;
    }

    private static GymTrainerRef parseGymTrainerId(String trainerId) {
        var matcher = GYM_TRAINER_ID_PATTERN.matcher(trainerId);
        if (!matcher.matches()) {
            return null;
        }

        return new GymTrainerRef(matcher.group(1), Integer.parseInt(matcher.group(2)), matcher.group(3));
    }

    public record GymTrainerRef(String gymType, int slotId, String trainerIdPart) {
    }
}
