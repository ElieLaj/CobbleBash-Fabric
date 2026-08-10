package com.nore.cobblebash.structure;

import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.entity.GymLeaderEntity;
import com.nore.cobblebash.entity.GymTrainerEntity;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GymPlatformBuilder {
   private static final String TRAINER_ENTITY_TAG = "cobblebash_rct_trainer";
   private static final int CLEAR_FLAGS = 35;
   private static final int PLACE_STRUCTURE_FLAGS = 3;
   private static final int PRESERVE_CONNECTION_FLAGS = 50;
   private static final int STRUCTURE_CLEANUP_PADDING = 8;
   private static final int ELITE_FOUR_ELECTRIC_GROUND_MODEL = 4;
   private static final int ELITE_FOUR_FIRE_FAIRY_MODEL = 5;
   private static final int ELITE_FOUR_GRASS_GHOST_MODEL = 6;
   private static final int ELITE_FOUR_WATER_STEEL_MODEL = 7;
   private static final int ELITE_FOUR_CHAMPION_MODEL = 8;
   private static final String[] MALE_TRAINER_NAMES = new String[]{
      "Aiden", "Ben", "Caleb", "Dante", "Eli", "Felix", "Grant", "Hugo", "Ivan", "Jasper", "Kai", "Leo", "Miles", "Nolan", "Owen", "Theo"
   };
   private static final String[] FEMALE_TRAINER_NAMES = new String[]{
      "Ava", "Bianca", "Clara", "Daphne", "Elena", "Freya", "Gwen", "Iris", "Jade", "Kira", "Lena", "Maya", "Nora", "Piper", "Rhea", "Talia"
   };

   public static void buildTestPlatform(ServerLevel level, BlockPos origin) {
      buildTestPlatform(level, origin, "bug", 0, new int[]{10, 12, 14});
   }

   public static void buildTestPlatform(ServerLevel level, BlockPos origin, int[] trainerLevels) {
      buildTestPlatform(level, origin, "bug", 0, trainerLevels);
   }

   public static void buildGym(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      if (gymstructuredefinition != null) {
         buildStructureGym(level, origin, gymstructuredefinition, slotId, trainerLevels);
      } else {
         buildTestPlatform(level, origin, gymType, slotId, trainerLevels);
      }
   }

   public static void buildTestPlatform(ServerLevel level, BlockPos origin, String gymType, int slotId, int[] trainerLevels) {
      clearTestPlatform(level, origin);
      int i = origin.getY() - 1;

      for (int j = -1; j <= 1; j++) {
         for (int k = 0; k < 12; k++) {
            level.setBlock(origin.offset(j, -1, k), Blocks.STONE.defaultBlockState(), 3);
         }
      }

      level.setBlock(new BlockPos(origin.getX(), i, origin.getZ() + 1), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
      level.setBlock(new BlockPos(origin.getX(), i, origin.getZ() + 4), Blocks.IRON_BLOCK.defaultBlockState(), 3);
      level.setBlock(new BlockPos(origin.getX(), i, origin.getZ() + 7), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
      level.setBlock(new BlockPos(origin.getX(), i, origin.getZ() + 10), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
      GymDoorController.buildClosedTestDoors(level, origin);
      GymPlatformBuilder.GymVisualPlan gymplatformbuilder$gymvisualplan = createGymVisualPlan(level, gymType, slotId);
      spawnTrainer(level, origin, gymType, slotId, "trainer_1", trainerLevels[0], origin.offset(0, 0, 4), gymplatformbuilder$gymvisualplan.trainerOne());
      spawnTrainer(level, origin, gymType, slotId, "trainer_2", trainerLevels[1], origin.offset(0, 0, 7), gymplatformbuilder$gymvisualplan.trainerTwo());
      spawnTrainer(level, origin, gymType, slotId, "boss", trainerLevels[2], origin.offset(0, 0, 10), gymplatformbuilder$gymvisualplan.boss());
   }

   public static void clearGym(ServerLevel level, BlockPos origin, String gymType) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      if (gymstructuredefinition != null) {
         clearStructureGym(level, origin, gymstructuredefinition);
      } else {
         clearTestPlatform(level, origin);
      }
   }

   public static void clearCachedGymBlocks(ServerLevel level, BlockPos origin, String gymType) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      if (gymstructuredefinition == null) {
         clearTestPlatform(level, origin);
      } else {
         AABB aabb = getCachedSlotCleanupBox(level, origin);
         clearSlotEntities(level, aabb);
         clearDroppedItems(level, aabb);
         clearBlocks(level, aabb);
         clearSlotEntities(level, aabb);
         clearDroppedItems(level, aabb);
      }
   }

   public static void clearTestPlatform(ServerLevel level, BlockPos origin) {
      clearSlotEntities(level, origin);

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 4; j++) {
            for (int k = 0; k < 12; k++) {
               level.setBlock(origin.offset(i, j, k), Blocks.AIR.defaultBlockState(), 3);
            }
         }
      }

      clearSlotEntities(level, origin);
   }

   public static BlockPos getPlayerSpawn(BlockPos origin, String gymType) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      return gymstructuredefinition != null ? origin.offset(gymstructuredefinition.playerSpawnOffset()) : origin.offset(0, 0, 1);
   }

   public static float getPlayerSpawnYaw(String gymType, float fallbackYaw) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      return gymstructuredefinition == null ? fallbackYaw : gymstructuredefinition.playerYaw();
   }

   public static float getPlayerSpawnPitch(String gymType, float fallbackPitch) {
      return GymStructureDefinition.get(gymType) == null ? fallbackPitch : 0.0F;
   }

   public static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
      return attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, null);
   }

   private static boolean attachTrainerEntity(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos) {
      return attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, null);
   }

   private static boolean attachTrainerEntity(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos, LivingEntity preferredEntity
   ) {
      TrainerNPC trainernpc = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
      if (trainernpc == null) {
         return false;
      }

      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      LivingEntity livingentity = pos == null
         ? keepSingleTrainerEntity(findTrainerEntities(level, origin, s))
         : cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, preferredEntity).keeper();
      if (livingentity == null) {
         return false;
      }

      trainernpc.setEntity(livingentity);
      return true;
   }

   private static void buildStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition, int slotId, int[] trainerLevels) {
      StructureTemplate structuretemplate = getStructureTemplate(level, definition);
      if (structuretemplate == null) {
         CobbleBash.LOGGER.warn("Could not build {} gym because structure template {} was not found.", definition.gymType(), definition.templateId());
      } else {
         StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setIgnoreEntities(true);
         clearStructureGym(level, origin, definition);
         structuretemplate.placeInWorld(level, origin, origin, structureplacesettings, level.getRandom(), 3);
         restoreSavedConnectionStates(level, origin, structureplacesettings, structuretemplate);
         restoreDecorativeEntities(level, origin, structureplacesettings, structuretemplate);
         paintStructureBiome(level, origin, definition);
         BlockPos blockpos = getPlayerSpawn(origin, definition.gymType());
         GymPlatformBuilder.GymVisualPlan gymplatformbuilder$gymvisualplan = createGymVisualPlan(level, definition.gymType(), slotId);
         boolean flag = spawnTrainer(
            level,
            origin,
            definition.gymType(),
            slotId,
            "trainer_1",
            trainerLevels[0],
            blockpos.offset(definition.trainerOneOffset()),
            definition.trainerOneYaw(),
            gymplatformbuilder$gymvisualplan.trainerOne()
         );
         boolean flag1 = spawnTrainer(
            level,
            origin,
            definition.gymType(),
            slotId,
            "trainer_2",
            trainerLevels[1],
            blockpos.offset(definition.trainerTwoOffset()),
            definition.trainerTwoYaw(),
            gymplatformbuilder$gymvisualplan.trainerTwo()
         );
         boolean flag2 = spawnTrainer(
            level,
            origin,
            definition.gymType(),
            slotId,
            "boss",
            trainerLevels[2],
            blockpos.offset(definition.bossOffset()),
            definition.bossYaw(),
            gymplatformbuilder$gymvisualplan.boss()
         );
         verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_1", flag);
         verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "trainer_2", flag1);
         verifySpawnedTrainer(level, origin, definition.gymType(), slotId, "boss", flag2);
         scheduleTrainerRepair(
            level,
            origin,
            definition.gymType(),
            slotId,
            "trainer_1",
            trainerLevels[0],
            blockpos.offset(definition.trainerOneOffset()),
            definition.trainerOneYaw(),
            gymplatformbuilder$gymvisualplan.trainerOne()
         );
         scheduleTrainerRepair(
            level,
            origin,
            definition.gymType(),
            slotId,
            "trainer_2",
            trainerLevels[1],
            blockpos.offset(definition.trainerTwoOffset()),
            definition.trainerTwoYaw(),
            gymplatformbuilder$gymvisualplan.trainerTwo()
         );
         scheduleTrainerRepair(
            level,
            origin,
            definition.gymType(),
            slotId,
            "boss",
            trainerLevels[2],
            blockpos.offset(definition.bossOffset()),
            definition.bossYaw(),
            gymplatformbuilder$gymvisualplan.boss()
         );
      }
   }

   private static void clearStructureGym(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
      AABB aabb = getStructureCleanupBox(level, origin, definition);
      clearSlotEntities(level, aabb);
      clearDroppedItems(level, aabb);
      clearSlotEntities(level, aabb);
      clearDroppedItems(level, aabb);
   }

   private static void clearBlocks(ServerLevel level, AABB box) {
      BlockPos blockpos = BlockPos.containing(box.minX, box.minY, box.minZ);
      BlockPos blockpos1 = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
      MutableBlockPos mutableblockpos = new MutableBlockPos();

      for (int i = blockpos.getX(); i <= blockpos1.getX(); i++) {
         for (int j = blockpos.getY(); j <= blockpos1.getY(); j++) {
            for (int k = blockpos.getZ(); k <= blockpos1.getZ(); k++) {
               mutableblockpos.set(i, j, k);
               if (!level.getBlockState(mutableblockpos).isAir()) {
                  level.setBlock(mutableblockpos, Blocks.AIR.defaultBlockState(), 35);
               }
            }
         }
      }
   }

   private static StructureTemplate getStructureTemplate(ServerLevel level, GymStructureDefinition definition) {
      return (StructureTemplate)level.getStructureManager().get(definition.templateId()).orElse(null);
   }

   private static void paintStructureBiome(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
      Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(definition.biomeKey());
      AABB aabb = getStructureCleanupBox(level, origin, definition);
      int i = SectionPos.blockToSectionCoord(BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ).getX());
      int j = SectionPos.blockToSectionCoord(BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ).getX());
      int k = SectionPos.blockToSectionCoord(BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ).getZ());
      int l = SectionPos.blockToSectionCoord(BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ).getZ());
      List<ChunkAccess> list = new ArrayList<>();

      for (int i1 = i; i1 <= j; i1++) {
         for (int j1 = k; j1 <= l; j1++) {
            LevelChunk levelchunk = level.getChunk(i1, j1);
            levelchunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, null);
            levelchunk.setUnsaved(true);
            list.add(levelchunk);
         }
      }

      if (!list.isEmpty()) {
         level.getChunkSource().chunkMap.resendBiomesForChunks(list);
      }
   }

   private static List<ChunkPos> getChunks(AABB box) {
      BlockPos blockpos = BlockPos.containing(box.minX, box.minY, box.minZ);
      BlockPos blockpos1 = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
      int i = SectionPos.blockToSectionCoord(blockpos.getX());
      int j = SectionPos.blockToSectionCoord(blockpos1.getX());
      int k = SectionPos.blockToSectionCoord(blockpos.getZ());
      int l = SectionPos.blockToSectionCoord(blockpos1.getZ());
      List<ChunkPos> list = new ArrayList<>();

      for (int i1 = i; i1 <= j; i1++) {
         for (int j1 = k; j1 <= l; j1++) {
            list.add(new ChunkPos(i1, j1));
         }
      }

      return list;
   }

   private static void restoreSavedConnectionStates(ServerLevel level, BlockPos origin, StructurePlaceSettings settings, StructureTemplate template) {
      for (Block block : BuiltInRegistries.BLOCK) {
         if (isSavedStateBlock(block)) {
            for (StructureBlockInfo structureblockinfo : template.filterBlocks(origin, settings, block)) {
               BlockPos blockpos = structureblockinfo.pos();
               BlockState blockstate = level.getBlockState(blockpos);
               if (blockstate.is(block) && !blockstate.equals(structureblockinfo.state())) {
                  level.setBlock(blockpos, structureblockinfo.state(), 50);
               }
            }
         }
      }
   }

   private static boolean isSavedStateBlock(Block block) {
      return block instanceof FenceBlock
         || block instanceof FenceGateBlock
         || block instanceof IronBarsBlock
         || block instanceof SlabBlock
         || block instanceof StairBlock
         || block instanceof WallBlock;
   }

   private static void restoreDecorativeEntities(ServerLevel level, BlockPos origin, StructurePlaceSettings settings, StructureTemplate template) {
      for (StructureEntityInfo structureentityinfo : getTemplateEntityInfos(template)) {
         if (settings.getBoundingBox() == null || settings.getBoundingBox().isInside(structureentityinfo.blockPos)) {
            CompoundTag compoundtag = structureentityinfo.nbt.copy();
            if (isRestorableDecorativeEntityId(compoundtag)) {
               ListTag listtag = new ListTag();
               listtag.add(DoubleTag.valueOf(structureentityinfo.pos.x));
               listtag.add(DoubleTag.valueOf(structureentityinfo.pos.y));
               listtag.add(DoubleTag.valueOf(structureentityinfo.pos.z));
               compoundtag.put("Pos", listtag);
               compoundtag.remove("UUID");
               EntityType.create(compoundtag, level).ifPresent(entity -> {
                  if (isRestorableDecorativeEntity(entity)) {
                     float f = entity.rotate(settings.getRotation());
                     f += entity.mirror(settings.getMirror()) - entity.getYRot();
                     entity.moveTo(structureentityinfo.pos.x, structureentityinfo.pos.y, structureentityinfo.pos.z, f, entity.getXRot());
                     level.addFreshEntityWithPassengers(entity);
                  }
               });
            }
         }
      }
   }

   private static List<StructureEntityInfo> getTemplateEntityInfos(StructureTemplate template) {
      CompoundTag compoundtag = template.save(new CompoundTag());
      ListTag listtag = compoundtag.getList("entities", 10);
      List<StructureEntityInfo> list = new ArrayList<>();

      for (int i = 0; i < listtag.size(); i++) {
         CompoundTag compoundtag1 = listtag.getCompound(i);
         if (compoundtag1.contains("nbt")) {
            ListTag listtag1 = compoundtag1.getList("pos", 6);
            ListTag listtag2 = compoundtag1.getList("blockPos", 3);
            Vec3 vec3 = new Vec3(listtag1.getDouble(0), listtag1.getDouble(1), listtag1.getDouble(2));
            BlockPos blockpos = new BlockPos(listtag2.getInt(0), listtag2.getInt(1), listtag2.getInt(2));
            list.add(new StructureEntityInfo(vec3, blockpos, compoundtag1.getCompound("nbt")));
         }
      }

      return list;
   }

   private static boolean isRestorableDecorativeEntityId(CompoundTag tag) {
      String s = tag.getString("id");
      return "minecraft:armor_stand".equals(s) || "minecraft:glow_item_frame".equals(s) || "minecraft:item_frame".equals(s) || "minecraft:painting".equals(s);
   }

   private static boolean isRestorableDecorativeEntity(Entity entity) {
      return entity instanceof ArmorStand || entity instanceof GlowItemFrame || entity instanceof ItemFrame || entity instanceof Painting;
   }

   private static boolean spawnTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos) {
      return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, 0.0F);
   }

   private static boolean spawnTrainer(
      ServerLevel level,
      BlockPos origin,
      String gymType,
      int slotId,
      String trainerIdPart,
      int trainerLevel,
      BlockPos pos,
      GymPlatformBuilder.TrainerVisual visual
   ) {
      return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, 0.0F, visual);
   }

   public static boolean spawnTrainerEntity(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw
   ) {
      boolean flag = spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw);
      verifySpawnedTrainer(level, origin, gymType, slotId, trainerIdPart, flag);
      scheduleTrainerRepair(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw);
      return flag;
   }

   private static boolean spawnTrainer(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw
   ) {
      return spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, null);
   }

   private static boolean spawnTrainer(
      ServerLevel level,
      BlockPos origin,
      String gymType,
      int slotId,
      String trainerIdPart,
      int trainerLevel,
      BlockPos pos,
      float yaw,
      GymPlatformBuilder.TrainerVisual visual
   ) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      discardTrainerEntities(level, origin, s);
      discardNearbyTrainerDisplays(level, pos);
      CobbleBash.LOGGER
         .debug(
            "Spawning gym trainer {} at {} in {} gym slot {} with level {} and yaw {}.",
            new Object[]{s, pos.toShortString(), gymType, slotId, trainerLevel, yaw}
         );
      String s1 = getTrainerDisplayName(level, gymType, trainerIdPart, visual);
      boolean flag = RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel, s1);
      if (!flag) {
         CobbleBash.LOGGER.error("RCT trainer registration failed for {}; spawning visible trainer entity anyway.", s);
      }

      Mob mob = createTrainerDisplayEntity(level, gymType, slotId, trainerIdPart, trainerLevel, visual);
      if (mob == null) {
         CobbleBash.LOGGER.error("Skipping entity spawn for {} because trainer entity creation returned null.", s);
         return false;
      } else {
         mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         mob.setYRot(yaw);
         mob.setYHeadRot(yaw);
         mob.setYBodyRot(yaw);
         mob.setCustomName(Component.literal(s1));
         mob.setCustomNameVisible(true);
         mob.setNoAi(true);
         mob.setNoGravity(true);
         mob.setPersistenceRequired();
         mob.setInvulnerable(true);
         mob.addTag("cobblebash_rct_trainer");
         mob.addTag(s);
         if (!level.addFreshEntity(mob)) {
            CobbleBash.LOGGER.error("Failed to add trainer entity {} to the world at {}.", s, pos.toShortString());
            return false;
         } else {
            boolean flag1 = flag && attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, mob);
            if (!flag1) {
               CobbleBash.LOGGER.error("Spawned trainer entity {} but failed to attach it to the RCT trainer.", s);
               return true;
            } else {
               scheduleTrainerCleanup(level, origin, gymType, slotId, trainerIdPart, pos);
               CobbleBash.LOGGER.debug("Spawned and attached trainer entity {} with entity UUID {}.", s, mob.getUUID());
               return true;
            }
         }
      }
   }

   private static Mob createTrainerDisplayEntity(
      ServerLevel level, String gymType, int slotId, String trainerIdPart, int trainerLevel, GymPlatformBuilder.TrainerVisual visual
   ) {
      if (usesGymTrainerVisual(trainerIdPart)) {
         GymTrainerEntity gymtrainerentity = (GymTrainerEntity)((EntityType)CobbleBash.GYM_TRAINER).create(level);
         if (gymtrainerentity != null) {
            GymPlatformBuilder.TrainerVisual gymplatformbuilder$trainervisual1 = visual != null
               ? visual
               : selectGymTrainerVisual(gymType, slotId, trainerIdPart, trainerLevel);
            gymtrainerentity.setVisual(gymplatformbuilder$trainervisual1.modelVariant(), gymplatformbuilder$trainervisual1.textureVariant());
         }

         return gymtrainerentity;
      } else if (usesGymLeaderVisual(gymType, trainerIdPart)) {
         GymLeaderEntity gymleaderentity = (GymLeaderEntity)((EntityType)CobbleBash.GYM_LEADER).create(level);
         if (gymleaderentity != null) {
            GymPlatformBuilder.TrainerVisual gymplatformbuilder$trainervisual = visual != null ? visual : selectGymLeaderVisual(gymType, slotId, trainerLevel);
            gymleaderentity.setVisual(gymplatformbuilder$trainervisual.modelVariant(), gymplatformbuilder$trainervisual.textureVariant());
         }

         return gymleaderentity;
      } else {
         return (Mob)EntityType.VILLAGER.create(level);
      }
   }

   private static boolean usesGymTrainerVisual(String trainerIdPart) {
      return "trainer_1".equals(trainerIdPart) || "trainer_2".equals(trainerIdPart);
   }

   private static boolean usesGymLeaderVisual(String gymType, String trainerIdPart) {
      return "boss".equals(trainerIdPart) && !"elite4".equals(gymType);
   }

   private static GymPlatformBuilder.GymVisualPlan createGymVisualPlan(ServerLevel level, String gymType, int slotId) {
      List<Integer> list = new ArrayList<>();
      List<String> list1 = new ArrayList<>();
      int i = Math.min(4, 9);

      for (int j = 0; j < i; j++) {
         list.add(j);
      }

      int i1 = takeRandomModel(level, list);
      int k = takeRandomModel(level, list);
      int l = takeRandomModel(level, list);
      GymPlatformBuilder.GymVisualPlan gymplatformbuilder$gymvisualplan = new GymPlatformBuilder.GymVisualPlan(
         new GymPlatformBuilder.TrainerVisual(i1, level.getRandom().nextInt(16), createRolledDisplayName(level, "Trainer", i1, list1)),
         new GymPlatformBuilder.TrainerVisual(k, level.getRandom().nextInt(16), createRolledDisplayName(level, "Trainer", k, list1)),
         new GymPlatformBuilder.TrainerVisual(l, level.getRandom().nextInt(16), createRolledDisplayName(level, "Gym Leader", l, list1))
      );
      CobbleBash.LOGGER
         .debug(
            "Gym visual roll for {} slot {}: trainer_1={} model {}, texture {}; trainer_2={} model {}, texture {}; boss={} model {}, texture {}.",
            new Object[]{
               gymType,
               slotId,
               gymplatformbuilder$gymvisualplan.trainerOne().displayName(),
               gymplatformbuilder$gymvisualplan.trainerOne().modelVariant(),
               gymplatformbuilder$gymvisualplan.trainerOne().textureVariant(),
               gymplatformbuilder$gymvisualplan.trainerTwo().displayName(),
               gymplatformbuilder$gymvisualplan.trainerTwo().modelVariant(),
               gymplatformbuilder$gymvisualplan.trainerTwo().textureVariant(),
               gymplatformbuilder$gymvisualplan.boss().displayName(),
               gymplatformbuilder$gymvisualplan.boss().modelVariant(),
               gymplatformbuilder$gymvisualplan.boss().textureVariant()
            }
         );
      return gymplatformbuilder$gymvisualplan;
   }

   private static String getTrainerDisplayName(ServerLevel level, String gymType, String trainerIdPart, GymPlatformBuilder.TrainerVisual visual) {
      return visual != null && visual.displayName() != null && !visual.displayName().isBlank()
         ? visual.displayName()
         : RctApiProbe.getTrainerDisplayName(level.getServer(), gymType, trainerIdPart);
   }

   private static int takeRandomModel(ServerLevel level, List<Integer> models) {
      return models.remove(level.getRandom().nextInt(models.size()));
   }

   private static String createRolledDisplayName(ServerLevel level, String title, int modelVariant, List<String> usedNames) {
      String[] astring = isFemaleTrainerModel(modelVariant) ? FEMALE_TRAINER_NAMES : MALE_TRAINER_NAMES;
      List<String> list = new ArrayList<>();

      for (String s : astring) {
         String s1 = title + " " + s;
         if (!usedNames.contains(s1)) {
            list.add(s);
         }
      }

      String s2 = list.isEmpty() ? astring[level.getRandom().nextInt(astring.length)] : list.get(level.getRandom().nextInt(list.size()));
      String s3 = title + " " + s2;
      usedNames.add(s3);
      return s3;
   }

   private static boolean isFemaleTrainerModel(int modelVariant) {
      return modelVariant == 2 || modelVariant == 3;
   }

   private static GymPlatformBuilder.TrainerVisual selectGymTrainerVisual(String gymType, int slotId, String trainerIdPart, int trainerLevel) {
      return new GymPlatformBuilder.TrainerVisual(
         selectGymTrainerModel(gymType, slotId, trainerIdPart), selectGymTrainerTexture(gymType, slotId, trainerIdPart, trainerLevel), null
      );
   }

   private static GymPlatformBuilder.TrainerVisual selectGymLeaderVisual(String gymType, int slotId, int trainerLevel) {
      GymPlatformBuilder.TrainerVisual gymplatformbuilder$trainervisual = selectEliteFourVisual(gymType);
      return gymplatformbuilder$trainervisual != null
         ? gymplatformbuilder$trainervisual
         : new GymPlatformBuilder.TrainerVisual(selectGymLeaderModel(gymType, slotId), selectGymLeaderTexture(gymType, slotId, trainerLevel), null);
   }

   private static GymPlatformBuilder.TrainerVisual selectEliteFourVisual(String gymType) {
      if ("elite4_champion".equals(gymType)) {
         return new GymPlatformBuilder.TrainerVisual(8, 0, null);
      }

      EliteFourMember elitefourmember = EliteFourMember.fromTrainerGymType(gymType);
      if (elitefourmember == null) {
         return null;
      }

      return switch (elitefourmember) {
         case ELECTRIC_GROUND -> new GymPlatformBuilder.TrainerVisual(4, 0, null);
         case FIRE_FAIRY -> new GymPlatformBuilder.TrainerVisual(5, 0, null);
         case GRASS_GHOST -> new GymPlatformBuilder.TrainerVisual(6, 0, null);
         case WATER_STEEL -> new GymPlatformBuilder.TrainerVisual(7, 0, null);
      };
   }

   private static int selectGymTrainerModel(String gymType, int slotId, String trainerIdPart) {
      int i = Math.floorMod(Objects.hash(gymType, slotId), 4);
      return "trainer_1".equals(trainerIdPart) ? i : (i + 1 + Math.floorMod(Objects.hash(gymType, slotId, trainerIdPart), 3)) % 4;
   }

   private static int selectGymTrainerTexture(String gymType, int slotId, String trainerIdPart, int trainerLevel) {
      return Math.floorMod(Objects.hash(gymType, slotId, trainerIdPart, trainerLevel), 16);
   }

   private static int selectGymLeaderModel(String gymType, int slotId) {
      int i = selectGymTrainerModel(gymType, slotId, "trainer_1");
      int j = selectGymTrainerModel(gymType, slotId, "trainer_2");
      int k = Math.floorMod(Objects.hash(gymType, slotId, "leader"), 9);

      for (int l = 0; l < 9; l++) {
         int i1 = (k + l) % 9;
         if (i1 != i && i1 != j) {
            return i1;
         }
      }

      return k;
   }

   private static int selectGymLeaderTexture(String gymType, int slotId, int trainerLevel) {
      return Math.floorMod(Objects.hash(gymType, slotId, trainerLevel, "leader"), 16);
   }

   private static void verifySpawnedTrainer(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, boolean spawnResult) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      List<LivingEntity> list = findTrainerEntities(level, origin, s);
      LivingEntity livingentity = keepSingleTrainerEntity(list);
      TrainerNPC trainernpc = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
      if (livingentity != null && trainernpc != null && trainernpc.getEntity() != null) {
         CobbleBash.LOGGER.debug("Gym trainer verification passed for {} at {}.", s, livingentity.blockPosition().toShortString());
      } else {
         CobbleBash.LOGGER
            .error(
               "Gym trainer verification failed for {}. spawnResult={}, entityFound={}, rctTrainerFound={}, rctAttached={}.",
               new Object[]{s, spawnResult, livingentity != null, trainernpc != null, trainernpc != null && trainernpc.getEntity() != null}
            );
      }
   }

   private static void scheduleTrainerRepair(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, int trainerLevel, BlockPos pos, float yaw
   ) {
      scheduleTrainerRepair(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, null);
   }

   private static void scheduleTrainerRepair(
      ServerLevel level,
      BlockPos origin,
      String gymType,
      int slotId,
      String trainerIdPart,
      int trainerLevel,
      BlockPos pos,
      float yaw,
      GymPlatformBuilder.TrainerVisual visual
   ) {
      DelayedTaskScheduler.schedule(2, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual));
      DelayedTaskScheduler.schedule(20, () -> repairMissingTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual));
      DelayedTaskScheduler.schedule(60, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
   }

   private static void repairMissingTrainer(
      ServerLevel level,
      BlockPos origin,
      String gymType,
      int slotId,
      String trainerIdPart,
      int trainerLevel,
      BlockPos pos,
      float yaw,
      GymPlatformBuilder.TrainerVisual visual
   ) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      TrainerNPC trainernpc = RctApiProbe.getGymTrainer(gymType, slotId, trainerIdPart);
      LivingEntity livingentity = trainernpc == null ? null : trainernpc.getEntity();
      LivingEntity livingentity1 = cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, livingentity).keeper();
      if (livingentity1 == null || trainernpc == null || trainernpc.getEntity() != livingentity1) {
         CobbleBash.LOGGER
            .warn(
               "Repairing missing gym trainer {}. entityFound={}, rctTrainerFound={}, rctAttached={}.",
               new Object[]{s, livingentity1 != null, trainernpc != null, trainernpc != null && trainernpc.getEntity() != null}
            );
         if (livingentity1 != null) {
            if (trainernpc == null && !RctApiProbe.registerGymTrainer(level.getServer(), gymType, slotId, trainerIdPart, trainerLevel)) {
               CobbleBash.LOGGER.error("Failed to repair {} RCT attachment because RCT registration failed. Visible entity remains in world.", s);
            } else if (!attachTrainerEntity(level, origin, gymType, slotId, trainerIdPart, pos, livingentity1)) {
               CobbleBash.LOGGER.error("Failed to repair {} because the existing entity could not be attached.", s);
            } else {
               CobbleBash.LOGGER.info("Reattached existing gym trainer entity {}.", s);
            }
         } else {
            spawnTrainer(level, origin, gymType, slotId, trainerIdPart, trainerLevel, pos, yaw, visual);
            verifySpawnedTrainer(level, origin, gymType, slotId, trainerIdPart, true);
         }
      }
   }

   private static void clearTrainerEntities(ServerLevel level, BlockPos origin) {
      clearSlotEntities(level, getEntityCleanupBox(origin));
   }

   public static List<GymPlatformBuilder.TrainerEntityDebug> debugTrainerEntities(ServerLevel level, BlockPos origin, String gymType, int slotId) {
      List<GymPlatformBuilder.TrainerEntityDebug> list = new ArrayList<>();

      for (String s : new String[]{"trainer_1", "trainer_2", "boss"}) {
         String s1 = RctApiProbe.getTrainerId(gymType, slotId, s);
         BlockPos blockpos = getExpectedTrainerPos(origin, gymType, s);
         List<LivingEntity> list1 = findTrainerEntities(level, origin, s1);
         List<LivingEntity> list2 = findNearbyTrainerDisplays(level, blockpos);
         List<LivingEntity> list3 = mergeEntities(list1, list2);
         List<String> list4 = list3.stream().map(entity -> describeTrainerEntity(entity, s1, blockpos)).toList();
         list.add(new GymPlatformBuilder.TrainerEntityDebug(s, s1, list3.size(), list1.size(), list2.size(), list4));
      }

      return list;
   }

   public static int cleanupTrainerEntities(ServerLevel level, BlockPos origin, String gymType, int slotId) {
      int i = 0;

      for (String s : new String[]{"trainer_1", "trainer_2", "boss"}) {
         GymPlatformBuilder.TrainerCleanupResult gymplatformbuilder$trainercleanupresult = cleanupTrainerStack(
            level, origin, gymType, slotId, s, getExpectedTrainerPos(origin, gymType, s)
         );
         i += gymplatformbuilder$trainercleanupresult.removed();
      }

      return i;
   }

   public static int discardOneTrainerDisplay(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      BlockPos blockpos = getExpectedTrainerPos(origin, gymType, trainerIdPart);
      List<LivingEntity> list = mergeEntities(findTrainerEntities(level, origin, s), findNearbyTrainerDisplays(level, blockpos));
      if (list.isEmpty()) {
         return 0;
      }

      LivingEntity livingentity = list.stream()
         .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5)))
         .orElse(list.get(0));
      livingentity.discard();
      CobbleBash.LOGGER
         .warn(
            "Debug discarded one trainer display for {} at {}: {}", new Object[]{s, blockpos.toShortString(), describeTrainerEntity(livingentity, s, blockpos)}
         );
      return mergeEntities(findTrainerEntities(level, origin, s), findNearbyTrainerDisplays(level, blockpos)).size();
   }

   public static int discardTrainerDisplays(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      BlockPos blockpos = getExpectedTrainerPos(origin, gymType, trainerIdPart);
      List<LivingEntity> list = mergeEntities(findTrainerEntities(level, origin, s), findNearbyTrainerDisplays(level, blockpos));
      list.forEach(Entity::discard);
      CobbleBash.LOGGER.warn("Debug discarded {} trainer displays for {} at {}.", new Object[]{list.size(), s, blockpos.toShortString()});
      return list.size();
   }

   private static void clearSlotEntities(ServerLevel level, BlockPos origin) {
      clearSlotEntities(level, getEntityCleanupBox(origin));
   }

   private static void clearSlotEntities(ServerLevel level, AABB box) {
      level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof Player)).forEach(entity -> entity.discard());
   }

   private static void clearDroppedItems(ServerLevel level, AABB box) {
      level.getEntitiesOfClass(ItemEntity.class, box).forEach(entity -> entity.discard());
   }

   private static LivingEntity findTrainerEntity(ServerLevel level, BlockPos origin, String trainerId) {
      return findTrainerEntities(level, origin, trainerId).stream().findFirst().orElse(null);
   }

   private static List<LivingEntity> findTrainerEntities(ServerLevel level, BlockPos origin, String trainerId) {
      AABB aabb = getTrainerSearchBox(level, origin, trainerId);
      return level.getEntitiesOfClass(LivingEntity.class, aabb, entity -> entity.getTags().contains(trainerId));
   }

   private static LivingEntity keepSingleTrainerEntity(List<LivingEntity> entities) {
      if (entities.isEmpty()) {
         return null;
      }

      LivingEntity livingentity = entities.get(0);
      discardDuplicateTrainerEntities(entities, livingentity);
      return livingentity;
   }

   private static void scheduleTrainerCleanup(ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos) {
      DelayedTaskScheduler.schedule(1, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
      DelayedTaskScheduler.schedule(5, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
      DelayedTaskScheduler.schedule(20, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
      DelayedTaskScheduler.schedule(60, () -> cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos));
   }

   private static GymPlatformBuilder.TrainerCleanupResult cleanupTrainerStack(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos
   ) {
      return cleanupTrainerStack(level, origin, gymType, slotId, trainerIdPart, pos, null);
   }

   private static GymPlatformBuilder.TrainerCleanupResult cleanupTrainerStack(
      ServerLevel level, BlockPos origin, String gymType, int slotId, String trainerIdPart, BlockPos pos, LivingEntity preferredKeeper
   ) {
      String s = RctApiProbe.getTrainerId(gymType, slotId, trainerIdPart);
      int i = discardNearbyTrainerDisplaysExcept(level, pos, s);
      List<LivingEntity> list = findTrainerEntities(level, origin, s);
      LivingEntity livingentity = keepTrainerEntity(list, pos, preferredKeeper);
      int j = Math.max(0, list.size() - (livingentity == null ? 0 : 1));
      return new GymPlatformBuilder.TrainerCleanupResult(livingentity, i + j);
   }

   private static LivingEntity keepClosestTrainerEntity(List<LivingEntity> entities, BlockPos pos) {
      return keepTrainerEntity(entities, pos, null);
   }

   private static LivingEntity keepTrainerEntity(List<LivingEntity> entities, BlockPos pos, LivingEntity preferredKeeper) {
      if (entities.isEmpty()) {
         return null;
      }

      LivingEntity livingentity = findPreferredTrainerEntity(entities, preferredKeeper);
      if (livingentity == null) {
         livingentity = entities.stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)))
            .orElse(entities.get(0));
      }

      discardDuplicateTrainerEntities(entities, livingentity);
      return livingentity;
   }

   private static LivingEntity findPreferredTrainerEntity(List<LivingEntity> entities, LivingEntity preferredKeeper) {
      if (preferredKeeper != null && !preferredKeeper.isRemoved()) {
         for (LivingEntity livingentity : entities) {
            if (livingentity.getUUID().equals(preferredKeeper.getUUID())) {
               return livingentity;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static void discardDuplicateTrainerEntities(List<LivingEntity> entities, LivingEntity keeper) {
      int i = 0;

      for (int j = 0; j < entities.size(); j++) {
         LivingEntity livingentity = entities.get(j);
         if (livingentity != keeper) {
            livingentity.discard();
            i++;
         }
      }

      if (i > 0) {
         CobbleBash.LOGGER.warn("Discarded {} duplicate CobbleBash trainer display entities.", i);
      }
   }

   private static void discardTrainerEntities(ServerLevel level, BlockPos origin, String trainerId) {
      findTrainerEntities(level, origin, trainerId).forEach(Entity::discard);
   }

   private static void discardNearbyTrainerDisplays(ServerLevel level, BlockPos pos) {
      AABB aabb = getTrainerSpawnBox(pos);
      level.getEntitiesOfClass(LivingEntity.class, aabb, GymPlatformBuilder::isTrainerDisplayEntity).forEach(Entity::discard);
   }

   private static int discardNearbyTrainerDisplaysExcept(ServerLevel level, BlockPos pos, String trainerId) {
      List<LivingEntity> list = level.getEntitiesOfClass(
         LivingEntity.class, getTrainerSpawnBox(pos), entity -> isTrainerDisplayEntity(entity) && !entity.getTags().contains(trainerId)
      );
      list.forEach(Entity::discard);
      if (!list.isEmpty()) {
         CobbleBash.LOGGER
            .warn("Discarded {} stale CobbleBash trainer display entities near {} while keeping {}.", new Object[]{list.size(), pos.toShortString(), trainerId});
      }

      return list.size();
   }

   private static List<LivingEntity> findNearbyTrainerDisplays(ServerLevel level, BlockPos pos) {
      return level.getEntitiesOfClass(LivingEntity.class, getTrainerSpawnBox(pos), GymPlatformBuilder::isTrainerDisplayEntity);
   }

   private static boolean isTrainerDisplayEntity(LivingEntity entity) {
      return entity instanceof GymTrainerEntity || entity.getTags().contains("cobblebash_rct_trainer");
   }

   private static List<LivingEntity> mergeEntities(List<LivingEntity> first, List<LivingEntity> second) {
      List<LivingEntity> list = new ArrayList<>(first);

      for (LivingEntity livingentity : second) {
         boolean flag = false;

         for (LivingEntity livingentity1 : list) {
            if (livingentity1.getUUID().equals(livingentity.getUUID())) {
               flag = true;
               break;
            }
         }

         if (!flag) {
            list.add(livingentity);
         }
      }

      return list;
   }

   private static String describeTrainerEntity(LivingEntity entity, String trainerId, BlockPos expectedPos) {
      String s = entity instanceof GymTrainerEntity gymtrainerentity
         ? ", model=" + gymtrainerentity.modelVariant() + ", texture=" + gymtrainerentity.textureVariant()
         : "";
      String s1 = entity.getTags().contains(trainerId) ? ", exactTag=true" : ", exactTag=false";
      return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
         + " uuid="
         + entity.getUUID()
         + " pos=("
         + String.format(Locale.ROOT, "%.2f", entity.getX())
         + ", "
         + String.format(Locale.ROOT, "%.2f", entity.getY())
         + ", "
         + String.format(Locale.ROOT, "%.2f", entity.getZ())
         + ") d2="
         + String.format(Locale.ROOT, "%.3f", entity.distanceToSqr(expectedPos.getX() + 0.5, expectedPos.getY(), expectedPos.getZ() + 0.5))
         + s1
         + s;
   }

   private static BlockPos getExpectedTrainerPos(BlockPos origin, String gymType, String trainerIdPart) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      if (gymstructuredefinition != null) {
         BlockPos blockpos = getPlayerSpawn(origin, gymType);
         if ("trainer_1".equals(trainerIdPart)) {
            return blockpos.offset(gymstructuredefinition.trainerOneOffset());
         } else {
            return "trainer_2".equals(trainerIdPart)
               ? blockpos.offset(gymstructuredefinition.trainerTwoOffset())
               : blockpos.offset(gymstructuredefinition.bossOffset());
         }
      } else if ("trainer_1".equals(trainerIdPart)) {
         return origin.offset(0, 0, 4);
      } else {
         return "trainer_2".equals(trainerIdPart) ? origin.offset(0, 0, 7) : origin.offset(0, 0, 10);
      }
   }

   private static AABB getTrainerSpawnBox(BlockPos pos) {
      return new AABB(pos.getX() - 1.25, pos.getY() - 0.5, pos.getZ() - 1.25, pos.getX() + 2.25, pos.getY() + 3.0, pos.getZ() + 2.25);
   }

   private static AABB getEntityCleanupBox(BlockPos origin) {
      return new AABB(origin.getX() - 16, origin.getY() - 8, origin.getZ() - 8, origin.getX() + 16, origin.getY() + 12, origin.getZ() + 24);
   }

   private static AABB getTrainerSearchBox(ServerLevel level, BlockPos origin, String trainerId) {
      if (trainerId.startsWith("cobblebash_elite4_")) {
         return EliteFourStructure.getStructureBox(level, origin);
      }

      for (GymStructureDefinition gymstructuredefinition : GymStructureDefinition.values()) {
         if (trainerId.startsWith("cobblebash_" + gymstructuredefinition.gymType() + "_slot_")) {
            return getStructureCleanupBox(level, origin, gymstructuredefinition);
         }
      }

      return getEntityCleanupBox(origin);
   }

   private static AABB getStructureCleanupBox(ServerLevel level, BlockPos origin, GymStructureDefinition definition) {
      BlockPos blockpos = origin.offset(-8, -8, -8);
      BlockPos blockpos1 = origin.offset(8, 8, 8);
      StructureTemplate structuretemplate = getStructureTemplate(level, definition);
      if (structuretemplate != null) {
         BlockPos blockpos2 = new BlockPos(structuretemplate.getSize());
         blockpos1 = max(blockpos1, origin.offset(blockpos2.getX(), blockpos2.getY(), blockpos2.getZ()));
      }

      blockpos = min(blockpos, origin.offset(definition.playerSpawnOffset()));
      blockpos1 = max(blockpos1, origin.offset(definition.playerSpawnOffset()));
      blockpos = includePlayerRelativeMin(origin, definition, blockpos, definition.trainerOneOffset());
      blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, definition.trainerOneOffset());
      blockpos = includePlayerRelativeMin(origin, definition, blockpos, definition.trainerTwoOffset());
      blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, definition.trainerTwoOffset());
      blockpos = includePlayerRelativeMin(origin, definition, blockpos, definition.bossOffset());
      blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, definition.bossOffset());

      for (GymStructureDefinition.GateBox gymstructuredefinition$gatebox : definition.stageOneGates()) {
         blockpos = includePlayerRelativeMin(origin, definition, blockpos, gymstructuredefinition$gatebox.min());
         blockpos = includePlayerRelativeMin(origin, definition, blockpos, gymstructuredefinition$gatebox.max());
         blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, gymstructuredefinition$gatebox.min());
         blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, gymstructuredefinition$gatebox.max());
      }

      for (GymStructureDefinition.GateBox gymstructuredefinition$gatebox1 : definition.stageTwoGates()) {
         blockpos = includePlayerRelativeMin(origin, definition, blockpos, gymstructuredefinition$gatebox1.min());
         blockpos = includePlayerRelativeMin(origin, definition, blockpos, gymstructuredefinition$gatebox1.max());
         blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, gymstructuredefinition$gatebox1.min());
         blockpos1 = includePlayerRelativeMax(origin, definition, blockpos1, gymstructuredefinition$gatebox1.max());
      }

      return new AABB(blockpos.getX() - 8, blockpos.getY() - 8, blockpos.getZ() - 8, blockpos1.getX() + 8, blockpos1.getY() + 8, blockpos1.getZ() + 8);
   }

   private static AABB getCachedSlotCleanupBox(ServerLevel level, BlockPos origin) {
      BlockPos blockpos = origin.offset(-8, -8, -8);
      BlockPos blockpos1 = origin.offset(8, 8, 8);

      for (GymStructureDefinition gymstructuredefinition : GymStructureDefinition.values()) {
         AABB aabb = getStructureCleanupBox(level, origin, gymstructuredefinition);
         blockpos = min(blockpos, BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ));
         blockpos1 = max(blockpos1, BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ));
      }

      return new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos1.getX(), blockpos1.getY(), blockpos1.getZ());
   }

   private static BlockPos includePlayerRelativeMin(BlockPos origin, GymStructureDefinition definition, BlockPos currentMin, BlockPos offset) {
      return min(currentMin, origin.offset(definition.playerRelative(offset)));
   }

   private static BlockPos includePlayerRelativeMax(BlockPos origin, GymStructureDefinition definition, BlockPos currentMax, BlockPos offset) {
      return max(currentMax, origin.offset(definition.playerRelative(offset)));
   }

   private static BlockPos min(BlockPos first, BlockPos second) {
      return new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
   }

   private static BlockPos max(BlockPos first, BlockPos second) {
      return new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
   }

   private record GymVisualPlan(GymPlatformBuilder.TrainerVisual trainerOne, GymPlatformBuilder.TrainerVisual trainerTwo, GymPlatformBuilder.TrainerVisual boss) {
   }

   private record TrainerCleanupResult(LivingEntity keeper, int removed) {
   }

   public record TrainerEntityDebug(String trainerIdPart, String trainerId, int total, int exactTagged, int nearbyDisplays, List<String> entries) {
   }

   private record TrainerVisual(int modelVariant, int textureVariant, String displayName) {
   }
}
