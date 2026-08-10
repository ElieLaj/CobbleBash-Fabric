package com.nore.cobblebash.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.gym.GymLevelSystem;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.instance.GymSlotPosition;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.GymReturnData;
import com.nore.cobblebash.progress.PlayerGymProgress;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.structure.GymPlatformBuilder;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import com.nore.cobblebash.structure.GymDoorController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.nore.cobblebash.integration.RctApiProbe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GymCommand {
    private static final Map<String, UUID> DEBUG_SLOT_RESERVATIONS = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var gymRoot = Commands.literal("gym");

        // Keeper/admin commands
        gymRoot.then(Commands.literal("test")
                .executes(context -> {
                    context.getSource().sendSuccess(
                            () -> Component.literal("CobbleBash gym command works."),
                            false
                    );
                    return 1;
                })
        );

        var enterNode = Commands.literal("enter");
        for (GymType type : GymType.values()) {
            enterNode.then(
                    Commands.literal(type.getId())
                            .executes(context -> enterGym(context.getSource(), type.getId()))
            );
        }
        gymRoot.then(enterNode);

        var battleNode = Commands.literal("battle");
        var defeatNode = Commands.literal("defeat");
        for (GymType type : GymType.values()) {
            battleNode.then(trainerTarget(type, GymCommand::startTrainerBattle));
            defeatNode.then(trainerTarget(type, GymCommand::defeatTrainer));
        }
        gymRoot.then(battleNode);
        gymRoot.then(defeatNode);

        var completeNode = Commands.literal("complete");
        for (GymType type : GymType.values()) {
            completeNode.then(
                    Commands.literal(type.getId())
                            .executes(context -> completeGym(context.getSource(), type.getId()))
            );
        }
        gymRoot.then(completeNode);

        gymRoot.then(Commands.literal("exit")
                .executes(context -> exitGym(context.getSource()))
        );

        gymRoot.then(Commands.literal("leave")
                .executes(context -> leaveGym(context.getSource()))
        );

        // Temporary/debug commands
        gymRoot.then(Commands.literal("debug")
                .executes(context -> debugProgress(context.getSource()))
        );

        gymRoot.then(Commands.literal("advance")
                .executes(context -> advanceGym(context.getSource()))
        );

        gymRoot.then(Commands.literal("rct_debug")
                .executes(context -> debugRct(context.getSource()))
        );

        gymRoot.then(Commands.literal("rct_register")
                .executes(context -> registerRct(context.getSource()))
        );

        gymRoot.then(Commands.literal("rct_get")
                .executes(context -> getRct(context.getSource()))
        );

        gymRoot.then(Commands.literal("rct_battle")
                .executes(context -> startRctBattle(context.getSource()))
        );

        gymRoot.then(Commands.literal("slot_debug")
                .then(Commands.literal("status")
                        .executes(context -> debugSlotStatus(context.getSource())))
                .then(Commands.literal("reserve")
                        .then(Commands.argument("label", StringArgumentType.word())
                                .executes(context -> reserveDebugSlot(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "label")
                                ))))
                .then(Commands.literal("release")
                        .then(Commands.argument("label", StringArgumentType.word())
                                .executes(context -> releaseDebugSlot(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "label")
                                ))))
        );

        dispatcher.register(gymRoot);
    }

    private static int enterGym(CommandSourceStack source, String gymType) {
        return enterGym(source.getPlayer(), source, gymType);
    }

    public static boolean enterGym(ServerPlayer player, String gymType) {
        return enterGym(player, null, gymType) > 0;
    }

    private static int enterGym(ServerPlayer player, CommandSourceStack source, String gymType) {
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);

        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());

        clearActiveGym(player, true, true);

        boolean alreadyCompleted = progress.hasCompleted(gymType);
        GameType returnGameMode = player.gameMode.getGameModeForPlayer();

        int[] trainerLevels = GymLevelSystem.getTrainerLevels(progress.getCompletedGymCount());
        GymReturnData.ReturnLocation returnLocation = GymReturnData.ReturnLocation.from(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );

        GymInstance instance = GymInstanceManager.createOrGet(
                player.getUUID(),
                gymType,
                alreadyCompleted,
                trainerLevels,
                returnLocation.dimension(),
                returnLocation.x(),
                returnLocation.y(),
                returnLocation.z(),
                returnLocation.yRot(),
                returnLocation.xRot(),
                returnGameMode
        );
        GymReturnData.get(player.server).put(player.getUUID(), returnLocation);

        progress.setActiveGymType(instance.getGymType());

        String mode = instance.isRepeatClear() ? "REPEAT" : "FIRST CLEAR";
        int[] instanceLevels = instance.getTrainerLevels();
        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        BlockPos playerSpawn = GymPlatformBuilder.getPlayerSpawn(origin, instance.getGymType());

        GymPlatformBuilder.buildGym(gymLevel, origin, instance.getGymType(), instance.getSlotId(), instanceLevels);

        player.teleportTo(
                gymLevel,
                playerSpawn.getX() + 0.5,
                playerSpawn.getY(),
                playerSpawn.getZ() + 0.5,
                GymPlatformBuilder.getPlayerSpawnYaw(instance.getGymType(), player.getYRot()),
                GymPlatformBuilder.getPlayerSpawnPitch(instance.getGymType(), player.getXRot())
        );
        player.setGameMode(GameType.ADVENTURE);

        sendSuccess(
                player,
                source,
                "Entering " + instance.getGymType()
                        + " gym [" + mode + "]. Slot = "
                        + instance.getSlotId()
                        + ". Origin = "
                        + formatPos(origin)
                        + ". Trainer levels: {"
                        + instanceLevels[0] + ", "
                        + instanceLevels[1] + ", "
                        + instanceLevels[2] + "}"
        );

        return 1;
    }

    private static int completeGym(CommandSourceStack source, String gymType) {
        return completeGym(source.getPlayer(), gymType, source);
    }

    private static int completeGym(ServerPlayer player, String gymType, CommandSourceStack source) {

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());

        if (!progress.isActiveGym(gymType)) {
            sendFailure(player, source, "Cannot complete " + gymType + " gym because your active gym is " + progress.getActiveGymType() + ".");
            return 0;
        }

        boolean alreadyCompleted = progress.hasCompleted(gymType);
        progress.completeGym(gymType);
        CobbleBashStats.syncGymsCompleted(player);

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());
        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        String rewardMode = alreadyCompleted ? "repeat rewards" : "first clear rewards + badge";
        String slotText = clearedInstance == null ? "none" : String.valueOf(clearedInstance.getSlotId());

        sendSuccess(
                player,
                source,
                "Completed " + gymType
                        + " gym. Reward mode: " + rewardMode
                        + ". Freed slot: " + slotText + "."
        );

        return 1;
    }

    private static int exitGym(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        if (clearedInstance == null) {
            source.sendFailure(Component.literal("You do not have an active gym instance."));
            return 0;
        }

        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        source.sendSuccess(
                () -> Component.literal(
                        "Exited " + clearedInstance.getGymType()
                                + " gym. Freed slot: "
                                + clearedInstance.getSlotId()
                                + "."
                ),
                false
        );

        return 1;
    }

    private static int leaveGym(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        source.sendSuccess(
                () -> Component.literal("Returned to gym entry point."),
                false
        );

        return 1;
    }

    private static int advanceGym(CommandSourceStack source) {
        return advanceGym(source.getPlayer(), source);
    }

    private static int advanceGym(ServerPlayer player, CommandSourceStack source) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            sendFailure(player, source, "You do not have an active gym instance.");
            return 0;
        }

        boolean advanced = instance.advanceTrainerStage();

        if (!advanced) {
            sendFailure(player, source, "Gym instance is already cleared.");
            return 0;
        }

        sendSuccess(
                player,
                source,
                "Advanced " + instance.getGymType()
                        + " gym to stage "
                        + instance.getTrainerStage()
                        + "."
        );

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel != null) {
            BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
            GymDoorController.openDoorForStage(gymLevel, origin, instance.getGymType(), instance.getTrainerStage());
        }

        if (instance.getTrainerStage() == 3) {
            CobbleBashCriteriaTriggers.triggerGymBossDefeated(player, instance.getGymType());
            sendSuccess(player, source, "Boss defeated. Leaving gym in 5 seconds...");

            DelayedTaskScheduler.schedule(100, () -> {
                if (source != null) {
                    completeGym(source, instance.getGymType());
                } else {
                    completeGym(player, instance.getGymType(), null);
                }
            });
        }

        return 1;
    }

    private static int registerRct(CommandSourceStack source) {
        RctApiProbe.registerTestTrainer(source.getServer());

        source.sendSuccess(
                () -> Component.literal("Registered RCT trainer: cobblebash_bug_trainer_1"),
                false
        );

        return 1;
    }

    private static int getRct(CommandSourceStack source) {
        var trainerNpc = RctApiProbe.getTestTrainer();

        if (trainerNpc == null) {
            source.sendFailure(Component.literal("Trainer not found."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Found trainer: "
                                + trainerNpc.getName()
                                + ", team size = "
                                + trainerNpc.getTeam().length
                ),
                false
        );

        return 1;
    }

    private static int debugRct(CommandSourceStack source) {
        StringBuilder builder = new StringBuilder("RCT API instances: ");

        RCTApi.getInstances().forEach(entry ->
                builder.append("[")
                        .append(entry.getKey())
                        .append("] ")
        );

        source.sendSuccess(
                () -> Component.literal(builder.toString()),
                false
        );

        return 1;
    }

    private static int startRctBattle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        boolean started = RctApiProbe.startTestBattle(player);

        if (!started) {
            source.sendFailure(Component.literal("Failed to start RCT battle."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Started RCT test battle."),
                false
        );

        return 1;
    }

    private static int reserveDebugSlot(CommandSourceStack source, String label) {
        if (DEBUG_SLOT_RESERVATIONS.containsKey(label)) {
            UUID existingId = DEBUG_SLOT_RESERVATIONS.get(label);
            GymInstance existing = GymInstanceManager.getActive(existingId);
            if (existing != null) {
                source.sendFailure(Component.literal("Debug slot label '" + label + "' already reserves slot " + existing.getSlotId() + "."));
                return 0;
            }
        }

        ServerPlayer player = source.getPlayer();
        UUID reservationId = UUID.nameUUIDFromBytes(("cobblebash:slot_debug:" + label).getBytes(StandardCharsets.UTF_8));
        GymReturnData.ReturnLocation returnLocation = GymReturnData.ReturnLocation.from(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );

        GymInstance instance = GymInstanceManager.createOrGet(
                reservationId,
                "slot_debug",
                true,
                new int[]{0, 0, 0},
                returnLocation.dimension(),
                returnLocation.x(),
                returnLocation.y(),
                returnLocation.z(),
                returnLocation.yRot(),
                returnLocation.xRot(),
                player.gameMode.getGameModeForPlayer()
        );
        DEBUG_SLOT_RESERVATIONS.put(label, reservationId);

        source.sendSuccess(
                () -> Component.literal("Reserved debug slot " + instance.getSlotId() + " as '" + label + "'."),
                false
        );
        return 1;
    }

    private static int releaseDebugSlot(CommandSourceStack source, String label) {
        UUID reservationId = DEBUG_SLOT_RESERVATIONS.remove(label);
        if (reservationId == null) {
            source.sendFailure(Component.literal("No debug slot reservation exists for '" + label + "'."));
            return 0;
        }

        GymInstance cleared = GymInstanceManager.clear(reservationId);
        if (cleared == null) {
            source.sendFailure(Component.literal("Debug slot label '" + label + "' was tracked, but no active slot was reserved."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Released debug slot " + cleared.getSlotId() + " from '" + label + "'."),
                false
        );
        return 1;
    }

    private static int debugSlotStatus(CommandSourceStack source) {
        StringBuilder reservations = new StringBuilder();
        DEBUG_SLOT_RESERVATIONS.forEach((label, playerId) -> {
            GymInstance instance = GymInstanceManager.getActive(playerId);
            if (instance != null) {
                if (!reservations.isEmpty()) {
                    reservations.append(", ");
                }
                reservations.append(label).append("=").append(instance.getSlotId());
            }
        });

        source.sendSuccess(
                () -> Component.literal(
                        "Slot debug: active instances = "
                                + GymInstanceManager.getActiveCount()
                                + ", free slots = "
                                + GymInstanceManager.getFreeSlotCount()
                                + ", next slot id = "
                                + GymInstanceManager.getNextSlotId()
                                + ", reservations = "
                                + (reservations.isEmpty() ? "none" : reservations)
                ),
                false
        );
        return 1;
    }

    private static int startTrainerBattle(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
        return startTrainerBattle(source.getPlayer(), source, gymType.getId(), null, unit);
    }

    public static boolean startTrainerBattle(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
        return startTrainerBattle(player, null, gymType, slotId, unit) > 0;
    }

    private static int startTrainerBattle(ServerPlayer player, CommandSourceStack source, String gymType, Integer requiredSlotId, GymTrainerUnit unit) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            sendFailure(player, source, "You do not have an active gym instance.");
            return 0;
        }

        if (!instance.getGymType().equals(gymType)) {
            sendFailure(player, source, "Your active gym is " + instance.getGymType() + ", not " + gymType + ".");
            return 0;
        }

        if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
            sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
            return 0;
        }

        if (instance.getTrainerStage() != unit.getRequiredStage()) {
            GymTrainerUnit expectedUnit = getExpectedTrainerUnit(instance.getTrainerStage());
            String targetName = getTrainerDisplayName(player, gymType, unit);
            String expectedName = expectedUnit == null
                    ? "the previous trainer"
                    : getTrainerDisplayName(player, gymType, expectedUnit);
            sendFailure(
                    player,
                    source,
                    "Cannot battle " + targetName + ". Beat " + expectedName + " first."
            );
            return 0;
        }

        int level = instance.getTrainerLevels()[unit.getLevelIndex()];
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        if (RctApiProbe.getGymTrainer(gymType, instance.getSlotId(), unit.getTrainerIdPart()) == null
                && !RctApiProbe.registerGymTrainer(player.server, gymType, instance.getSlotId(), unit.getTrainerIdPart(), level)) {
            sendFailure(player, source, "Failed to register " + gymType + " " + unit.getDisplayName() + " trainer.");
            return 0;
        }

        GymPlatformBuilder.attachTrainerEntity(gymLevel, origin, gymType, instance.getSlotId(), unit.getTrainerIdPart());

        boolean started = RctApiProbe.startGymBattle(player, gymType, instance.getSlotId(), unit.getTrainerIdPart());

        if (!started) {
            sendFailure(player, source, "Failed to start " + gymType + " " + unit.getDisplayName() + " battle.");
            return 0;
        }

        sendSuccess(player, source, "Started " + gymType + " " + unit.getDisplayName() + " battle.");

        return 1;
    }

    private static int defeatTrainer(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
        ServerPlayer player = source.getPlayer();
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            source.sendFailure(Component.literal("You do not have an active gym instance."));
            return 0;
        }

        if (!instance.getGymType().equals(gymType.getId())) {
            source.sendFailure(Component.literal("Your active gym is " + instance.getGymType() + ", not " + gymType.getId() + "."));
            return 0;
        }

        if (instance.getTrainerStage() != unit.getRequiredStage()) {
            source.sendFailure(Component.literal(
                    "Cannot defeat " + unit.getDisplayName()
                            + " at stage " + instance.getTrainerStage()
                            + ". Expected " + getExpectedTrainerName(instance.getTrainerStage()) + "."
            ));
            return 0;
        }

        return advanceGym(source);
    }

    public static void handleTrainerVictory(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null
                || !instance.getGymType().equals(gymType)
                || instance.getSlotId() != slotId
                || instance.getTrainerStage() != unit.getRequiredStage()) {
            return;
        }

        advanceGym(player, null);
    }

    public static void clearActiveGym(ServerPlayer player, boolean teleport) {
        clearActiveGym(player, teleport, teleport);
    }

    public static void clearActiveGym(ServerPlayer player, boolean teleport, boolean consumeSavedReturn) {
        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        clearInstancePlatform(player, clearedInstance);

        if (teleport && player.level().dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            teleportToReturnLocation(player, clearedInstance);
        } else if (clearedInstance != null) {
            restoreReturnGameMode(player, clearedInstance);
        }

        if (consumeSavedReturn) {
            GymReturnData.get(player.server).remove(player.getUUID());
        }
    }

    private static int debugProgress(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        int[] trainerLevels = GymLevelSystem.getTrainerLevels(progress.getCompletedGymCount());

        String instanceText = instance == null
                ? "none"
                : "slot " + instance.getSlotId()
                + ", type " + instance.getGymType()
                + ", stage " + instance.getTrainerStage()
                + ", origin " + formatPos(GymSlotPosition.getOriginForSlot(instance.getSlotId()));

        source.sendSuccess(
                () -> Component.literal(
                        "CobbleBash debug: completed gyms = "
                                + progress.getCompletedGymCount()
                                + ", active gym = "
                                + progress.getActiveGymType()
                                + ", trainer levels = {"
                                + trainerLevels[0] + ", "
                                + trainerLevels[1] + ", "
                                + trainerLevels[2] + "}"
                                + ", active instance = "
                                + instanceText
                                + ", active instances = "
                                + GymInstanceManager.getActiveCount()
                                + ", free slots = "
                                + GymInstanceManager.getFreeSlotCount()
                                + ", next slot id = "
                                + GymInstanceManager.getNextSlotId()
                ),
                false
        );

        return 1;
    }

    private static void teleportToSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();

        player.teleportTo(
                overworld,
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
    }

    private static void teleportToReturnLocation(ServerPlayer player, GymInstance instance) {
        if (instance == null) {
            GymReturnData.get(player.server).remove(player.getUUID())
                    .ifPresentOrElse(
                            location -> teleportToReturnLocation(player, location),
                            () -> teleportToSpawn(player)
                    );
            return;
        }

        teleportToReturnLocation(player, new GymReturnData.ReturnLocation(
                instance.getReturnDimension(),
                instance.getReturnX(),
                instance.getReturnY(),
                instance.getReturnZ(),
                instance.getReturnYRot(),
                instance.getReturnXRot()
        ));
        restoreReturnGameMode(player, instance);
    }

    public static void teleportToSavedReturnOrSpawn(ServerPlayer player) {
        GymReturnData.get(player.server).remove(player.getUUID())
                .ifPresentOrElse(
                        location -> teleportToReturnLocation(player, location),
                        () -> teleportToSpawn(player)
                );
    }

    private static void teleportToReturnLocation(ServerPlayer player, GymReturnData.ReturnLocation location) {
        ServerLevel returnLevel = player.server.getLevel(location.dimension());
        if (returnLevel == null || returnLevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            teleportToSpawn(player);
            return;
        }

        player.teleportTo(
                returnLevel,
                location.x(),
                location.y(),
                location.z(),
                location.yRot(),
                location.xRot()
        );
    }

    private static String formatPos(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    private static void clearInstancePlatform(ServerPlayer player, GymInstance instance) {
        if (instance == null) {
            return;
        }

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            return;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        GymPlatformBuilder.clearGym(gymLevel, origin, instance.getGymType());
        RctApiProbe.unregisterGymTrainers(instance.getGymType(), instance.getSlotId());
    }

    private static void restoreReturnGameMode(ServerPlayer player, GymInstance instance) {
        if (instance.getReturnGameMode() != null) {
            player.setGameMode(instance.getReturnGameMode());
        }
    }

    private static void sendSuccess(ServerPlayer player, CommandSourceStack source, String message) {
        if (source != null) {
            source.sendSuccess(() -> Component.literal(message), false);
        } else {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static void sendFailure(ServerPlayer player, CommandSourceStack source, String message) {
        if (source != null) {
            source.sendFailure(Component.literal(message));
        } else {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trainerTarget(GymType type, TrainerCommandExecutor executor) {
        return Commands.literal(type.getId())
                .then(Commands.literal("trainer")
                        .then(Commands.literal("one").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.TRAINER_ONE)))
                        .then(Commands.literal("two").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.TRAINER_TWO))))
                .then(Commands.literal("boss").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.BOSS)));
    }

    private static String getExpectedTrainerName(int stage) {
        GymTrainerUnit unit = getExpectedTrainerUnit(stage);
        return unit == null ? "no remaining trainer" : unit.getDisplayName();
    }

    private static GymTrainerUnit getExpectedTrainerUnit(int stage) {
        return switch (stage) {
            case 0 -> GymTrainerUnit.TRAINER_ONE;
            case 1 -> GymTrainerUnit.TRAINER_TWO;
            case 2 -> GymTrainerUnit.BOSS;
            default -> null;
        };
    }

    private static String getTrainerDisplayName(ServerPlayer player, String gymType, GymTrainerUnit unit) {
        return RctApiProbe.getTrainerDisplayName(player.server, gymType, unit.getTrainerIdPart());
    }

    private interface TrainerCommandExecutor {
        int run(CommandSourceStack source, GymType gymType, GymTrainerUnit unit);
    }
}
