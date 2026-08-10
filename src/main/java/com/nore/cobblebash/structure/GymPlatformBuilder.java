package com.nore.cobblebash.structure;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class GymPlatformBuilder {
    private static final String TRAINER_ENTITY_TAG = "cobblebash_rct_trainer";
    private static final int CLEAR_STRUCTURE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int PLACE_STRUCTURE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int STRUCTURE_CLEANUP_PADDING = 8;
    private static final int STRUCTURE_RELIGHT_PADDING = 0;
    private static final int STRUCTURE_RELIGHT_CHUNK_PADDING = 1;
    private static final int[] STRUCTURE_RELIGHT_DELAYS = {1, 20};

    public static void buildTestPlatform(ServerLevel level, BlockPos origin) {
        buildTestPlatform(level, origin, "bug", 0, new int[]{10, 12, 14});
    }

    public static void buildTestPlatform(ServerLevel level, BlockPos origin, int[] trainerLevels) {
        buildTestPlatform(level, origin, "bug", 0, trainerLevels);
    }

    public static void buildGym(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            buildStructureGym(level, origin, definition, slotId, trainerLevels);
            return;
        }

        buildTestPlatform(level, origin, gymType, slotId, trainerLevels);
    }

    public static void buildTestPlatform(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
        clearTestPlatform(level, origin);

        int blockY = origin.getY() - 1;

        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z < 12; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 1), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 4), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 7), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(origin.getX(), blockY, origin.getZ() + 10), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);

        GymDoorController.buildClosedTestDoors(level, origin);
        spawnTrainer(level, origin, gymType, slotId, "trainer_1", trainerLevels[0], origin.offset(0, 0, 4));
        spawnTrainer(level, origin, gymType, slotId, "trainer_2", trainerLevels[1], origin.offset(0, 0, 7));
        spawnTrainer(level, origin, gymType, slotId, "boss", trainerLevels[2], origin.offset(0, 0, 10));
    }

    public static void clearGym(ServerLevel level, BlockPos origin, String gymType) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            clearStructureGym(level, origin, definition);
            return;
        }

        clearTestPlatform(level, origin);
    }

    public static void clearTestPlatform(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, origin);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = 0; z < 12; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        clearSlotEntities(level, origin);
    }

    public static BlockPos getPlayerSpawn(BlockPos origin, String gymType) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            return origin.offset(definition.playerSpawnOffset());
        }

        return origin.offset(0, 0, 1);
    }

    public static float getPlayerSpawnYaw(String gymType, float fallbackYaw) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        return definition == null ? fallbackYaw : definition.playerYaw();
    }

    public static float getPlayerSpawnPitch(String gymType, float fallbackPitch) {
        return GymStructureDefinition.get(gymType) == null ? fallbackPitch : 0.0F;
    }

    public static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
        if (trainer == null) return false;

        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        LivingEntity entity = findTrainerEntity(level, origin, trainerId);
        if (entity == null) return false;

        trainer.setEntity(entity);
        return true;
    }

    private static void buildStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition, int slotId, int[] trainerLevels) {
        clearStructureGym(level, origin, definition);

        StructureTemplate template = getStructureTemplate(level, definition);
        if (template == null) {
            CobbleBash.LOGGER.warn(
                    "Could not build {} gym because structure template {} was not found.",
                    definition.gymType(),
                    definition.templateId()
            );
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setKnownShape(true)
                .setIgnoreEntities(false);
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), PLACE_STRUCTURE_FLAGS);
        paintStructureBiome(level, origin, definition);
        queueStructureRelight(level, origin, definition);

        BlockPos playerSpawn = getPlayerSpawn(origin, definition.gymType());
        boolean trainerOneSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "trainer_1", trainerLevels[0],
                playerSpawn.offset(definition.trainerOneOffset()), definition.trainerOneYaw());
        boolean trainerTwoSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "trainer_2", trainerLevels[1],
                playerSpawn.offset(definition.trainerTwoOffset()), definition.trainerTwoYaw());
        boolean bossSpawned = spawnTrainer(level, origin, definition.gymType(), slotId, "boss", trainerLevels[2],
                playerSpawn.offset(definition.bossOffset()), definition.bossYaw());

        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_1", trainerOneSpawned);
        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_2", trainerTwoSpawned);
        verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "boss", bossSpawned);
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "trainer_1", trainerLevels[0],
                playerSpawn.offset(definition.trainerOneOffset()), definition.trainerOneYaw());
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "trainer_2", trainerLevels[1],
                playerSpawn.offset(definition.trainerTwoOffset()), definition.trainerTwoYaw());
        scheduleTrainerRepair(level, origin, definition.gymType(), slotId, "boss", trainerLevels[2],
                playerSpawn.offset(definition.bossOffset()), definition.bossYaw());
    }

    private static void clearStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        AABB cleanupBox = getStructureCleanupBox(level, origin, definition);
        clearSlotEntities(level, cleanupBox);
        clearDroppedItems(level, cleanupBox);
        clearBlocksNoDrops(level, cleanupBox);
        clearSlotEntities(level, cleanupBox);
        clearDroppedItems(level, cleanupBox);
    }

    private static void clearBlockNoDrops(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), CLEAR_STRUCTURE_FLAGS);
    }

    private static void clearBlocksNoDrops(ServerLevel level, AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        clearBlockNoDrops(level, pos);
                    }
                }
            }
        }
    }

    private static StructureTemplate getStructureTemplate(ServerLevel level, GymStructureDefinition definition) {
        return level.getStructureManager().get(definition.templateId()).orElse(null);
    }

    private static void queueStructureRelight(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        AABB relightBox = getStructureCleanupBox(level, origin, definition).inflate(STRUCTURE_RELIGHT_PADDING);

        for (int delay : STRUCTURE_RELIGHT_DELAYS) {
            DelayedTaskScheduler.schedule(delay, () -> relightStructureVolume(level, relightBox));
        }
    }

    private static void paintStructureBiome(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        Holder<Biome> biome = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(definition.biomeKey());
        AABB box = getStructureCleanupBox(level, origin, definition);

        int minChunkX = SectionPos.blockToSectionCoord(BlockPos.containing(box.minX, box.minY, box.minZ).getX());
        int maxChunkX = SectionPos.blockToSectionCoord(BlockPos.containing(box.maxX, box.maxY, box.maxZ).getX());
        int minChunkZ = SectionPos.blockToSectionCoord(BlockPos.containing(box.minX, box.minY, box.minZ).getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(BlockPos.containing(box.maxX, box.maxY, box.maxZ).getZ());
        List<ChunkAccess> changedChunks = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, null);
                chunk.setUnsaved(true);
                changedChunks.add(chunk);
            }
        }

        if (!changedChunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(changedChunks);
        }
    }

    private static void relightStructureVolume(ServerLevel level, AABB box) {
        BlockPos min = BlockPos.containing(
                box.minX,
                Math.max(box.minY, level.getMinBuildHeight()),
                box.minZ
        );
        BlockPos max = BlockPos.containing(
                box.maxX,
                Math.min(box.maxY, level.getMaxBuildHeight() - 1),
                box.maxZ
        );

        var lightEngine = level.getChunkSource().getLightEngine();
        int minChunkX = SectionPos.blockToSectionCoord(min.getX()) - STRUCTURE_RELIGHT_CHUNK_PADDING;
        int maxChunkX = SectionPos.blockToSectionCoord(max.getX()) + STRUCTURE_RELIGHT_CHUNK_PADDING;
        int minChunkZ = SectionPos.blockToSectionCoord(min.getZ()) - STRUCTURE_RELIGHT_CHUNK_PADDING;
        int maxChunkZ = SectionPos.blockToSectionCoord(max.getZ()) + STRUCTURE_RELIGHT_CHUNK_PADDING;
        int minSectionY = SectionPos.blockToSectionCoord(min.getY());
        int maxSectionY = SectionPos.blockToSectionCoord(max.getY());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                resetAndRelightChunk(level, lightEngine, chunkX, chunkZ, minSectionY, maxSectionY);
            }
        }

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    lightEngine.checkBlock(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static void resetAndRelightChunk(
            ServerLevel level,
            ThreadedLevelLightEngine lightEngine,
            int chunkX,
            int chunkZ,
            int minSectionY,
            int maxSectionY
    ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        var chunk = level.getChunk(chunkX, chunkZ);

        lightEngine.retainData(chunkPos, false);
        lightEngine.setLightEnabled(chunkPos, false);

        for (int sectionY = lightEngine.getMinLightSection(); sectionY < lightEngine.getMaxLightSection(); sectionY++) {
            SectionPos sectionPos = SectionPos.of(chunkPos, sectionY);
            lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, null);
            lightEngine.queueSectionData(LightLayer.SKY, sectionPos, null);
        }

        for (int sectionY = level.getMinSection(); sectionY < level.getMaxSection(); sectionY++) {
            lightEngine.updateSectionStatus(SectionPos.of(chunkPos, sectionY), true);
        }

        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            lightEngine.updateSectionStatus(SectionPos.of(chunkPos, sectionY), false);
        }

        lightEngine.initializeLight(chunk, true);
        lightEngine.lightChunk(chunk, false);
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos) {
        return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, 0.0F);
    }

    private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        CobbleBash.LOGGER.info(
                "Spawning gym trainer {} at {} in {} gym slot {} with level {} and yaw {}.",
                trainerId,
                pos.toShortString(),
                gymType,
                slotId,
                trainerLevel,
                yaw
        );

        boolean registered = RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel);
        if (!registered) {
            CobbleBash.LOGGER.error("RCT trainer registration failed for {}; spawning visible trainer entity anyway.", trainerId);
        }

        var entity = EntityType.VILLAGER.create(level);
        if (entity == null) {
            CobbleBash.LOGGER.error("Skipping entity spawn for {} because villager entity creation returned null.", trainerId);
            return false;
        }

        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setCustomName(Component.literal(RctApiProbe.getTrainerDisplayName(level.getServer(), gymType, trainerIdPart)));
        entity.setCustomNameVisible(true);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        entity.setPersistenceRequired();
        entity.setInvulnerable(true);
        entity.addTag(TRAINER_ENTITY_TAG);
        entity.addTag(trainerId);

        if (!level.addFreshEntity(entity)) {
            CobbleBash.LOGGER.error("Failed to add trainer entity {} to the world at {}.", trainerId, pos.toShortString());
            return false;
        }

        boolean attached = registered && attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart);
        if (!attached) {
            CobbleBash.LOGGER.error("Spawned trainer entity {} but failed to attach it to the RCT trainer.", trainerId);
            return true;
        }

        CobbleBash.LOGGER.info("Spawned and attached trainer entity {} with entity UUID {}.", trainerId, entity.getUUID());
        return true;
    }

    private static void verifySpawnedTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, boolean spawnResult) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        LivingEntity entity = findTrainerEntity(level, origin, trainerId);
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);

        if (entity == null || trainer == null || trainer.getEntity() == null) {
            CobbleBash.LOGGER.error(
                    "Gym trainer verification failed for {}. spawnResult={}, entityFound={}, rctTrainerFound={}, rctAttached={}.",
                    trainerId,
                    spawnResult,
                    entity != null,
                    trainer != null,
                    trainer != null && trainer.getEntity() != null
            );
            return;
        }

        CobbleBash.LOGGER.info(
                "Gym trainer verification passed for {} at {}.",
                trainerId,
                entity.blockPosition().toShortString()
        );
    }

    private static void scheduleTrainerRepair(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            BlockPos pos,
            float yaw
    ) {
        DelayedTaskScheduler.schedule(2, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw));
        DelayedTaskScheduler.schedule(20, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw));
    }

    private static void repairMissingTrainer(
            ServerLevel level,
            BlockPos origin,
            String gymType,
            int slotId,
            String trainerIdPart,
            int trainerLevel,
            BlockPos pos,
            float yaw
    ) {
        String trainerId = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
        LivingEntity entity = findTrainerEntity(level, origin, trainerId);
        TrainerNPC trainer = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);

        if (entity != null && trainer != null && trainer.getEntity() != null) {
            return;
        }

        CobbleBash.LOGGER.warn(
                "Repairing missing gym trainer {}. entityFound={}, rctTrainerFound={}, rctAttached={}.",
                trainerId,
                entity != null,
                trainer != null,
                trainer != null && trainer.getEntity() != null
        );

        if (entity != null) {
            if (trainer == null && !RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel)) {
                CobbleBash.LOGGER.error("Failed to repair {} RCT attachment because RCT registration failed. Visible entity remains in world.", trainerId);
                return;
            }

            if (!attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart)) {
                CobbleBash.LOGGER.error("Failed to repair {} because the existing entity could not be attached.", trainerId);
                return;
            }

            CobbleBash.LOGGER.info("Reattached existing gym trainer entity {}.", trainerId);
            return;
        }

        spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw);
        verifySpawnedTrainer(level, origin, gymType, slotId, trainerIdPart, true);
    }

    private static void clearTrainerEntities(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, getEntityCleanupBox(origin));
    }

    private static void clearSlotEntities(ServerLevel level, BlockPos origin) {
        clearSlotEntities(level, getEntityCleanupBox(origin));
    }

    private static void clearSlotEntities(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class,
                box,
                entity -> !(entity instanceof Player)
        ).forEach(entity -> entity.discard());
    }

    private static void clearDroppedItems(ServerLevel level, AABB box) {
        level.getEntitiesOfClass(ItemEntity.class, box).forEach(entity -> entity.discard());
    }

    private static LivingEntity findTrainerEntity(ServerLevel level, BlockPos origin, String trainerId) {
        AABB box = getTrainerSearchBox(level, origin, trainerId);

        return level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.getTags().contains(trainerId)
        ).stream().findFirst().orElse(null);
    }

    private static AABB getEntityCleanupBox(BlockPos origin) {
        return new AABB(
                origin.getX() - 16,
                origin.getY() - 8,
                origin.getZ() - 8,
                origin.getX() + 16,
                origin.getY() + 12,
                origin.getZ() + 24
        );
    }

    private static AABB getTrainerSearchBox(ServerLevel level, BlockPos origin, String trainerId) {
        for (GymStructureDefinition definition : GymStructureDefinition.values()) {
            if (trainerId.startsWith("cobblebash_" + definition.gymType() + "_slot_")) {
                return getStructureCleanupBox(level, origin, definition);
            }
        }

        return getEntityCleanupBox(origin);
    }

    private static AABB getStructureCleanupBox(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
        BlockPos min = origin.offset(-STRUCTURE_CLEANUP_PADDING, -STRUCTURE_CLEANUP_PADDING, -STRUCTURE_CLEANUP_PADDING);
        BlockPos max = origin.offset(STRUCTURE_CLEANUP_PADDING, STRUCTURE_CLEANUP_PADDING, STRUCTURE_CLEANUP_PADDING);

        StructureTemplate template = getStructureTemplate(level, definition);
        if (template != null) {
            BlockPos size = new BlockPos(template.getSize());
            max = max(max, origin.offset(size.getX(), size.getY(), size.getZ()));
        }

        min = min(min, origin.offset(definition.playerSpawnOffset()));
        max = max(max, origin.offset(definition.playerSpawnOffset()));

        min = includePlayerRelativeMin(origin, definition, min, definition.trainerOneOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.trainerOneOffset());
        min = includePlayerRelativeMin(origin, definition, min, definition.trainerTwoOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.trainerTwoOffset());
        min = includePlayerRelativeMin(origin, definition, min, definition.bossOffset());
        max = includePlayerRelativeMax(origin, definition, max, definition.bossOffset());

        for (GymStructureDefinition.GateBox gate : definition.stageOneGates()) {
            min = includePlayerRelativeMin(origin, definition, min, gate.min());
            min = includePlayerRelativeMin(origin, definition, min, gate.max());
            max = includePlayerRelativeMax(origin, definition, max, gate.min());
            max = includePlayerRelativeMax(origin, definition, max, gate.max());
        }

        for (GymStructureDefinition.GateBox gate : definition.stageTwoGates()) {
            min = includePlayerRelativeMin(origin, definition, min, gate.min());
            min = includePlayerRelativeMin(origin, definition, min, gate.max());
            max = includePlayerRelativeMax(origin, definition, max, gate.min());
            max = includePlayerRelativeMax(origin, definition, max, gate.max());
        }

        return new AABB(
                min.getX() - STRUCTURE_CLEANUP_PADDING,
                min.getY() - STRUCTURE_CLEANUP_PADDING,
                min.getZ() - STRUCTURE_CLEANUP_PADDING,
                max.getX() + STRUCTURE_CLEANUP_PADDING,
                max.getY() + STRUCTURE_CLEANUP_PADDING,
                max.getZ() + STRUCTURE_CLEANUP_PADDING
        );
    }

    private static BlockPos includePlayerRelativeMin(BlockPos origin, GymStructureDefinition definition, BlockPos currentMin, BlockPos offset) {
        return min(currentMin, origin.offset(definition.playerRelative(offset)));
    }

    private static BlockPos includePlayerRelativeMax(BlockPos origin, GymStructureDefinition definition, BlockPos currentMax, BlockPos offset) {
        return max(currentMax, origin.offset(definition.playerRelative(offset)));
    }

    private static BlockPos min(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
    }

    private static BlockPos max(BlockPos first, BlockPos second) {
        return new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
    }
}
