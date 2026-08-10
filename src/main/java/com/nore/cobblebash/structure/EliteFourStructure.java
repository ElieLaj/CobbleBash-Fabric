package com.nore.cobblebash.structure;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.block.EliteFourChampionBeamBlock;
import com.nore.cobblebash.block.EliteFourPlaqueBlock;
import com.nore.cobblebash.elitefour.EliteFourMember;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

public class EliteFourStructure {
   public static final String GYM_TYPE = "elite4";
   private static final ResourceLocation TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath("cobblebash", "elite4/elite4");
   private static final int CLEAR_FLAGS = 35;
   private static final int PLACE_FLAGS = 3;
   private static final int PRESERVE_CONNECTION_FLAGS = 50;
   private static final int CLEANUP_PADDING = 8;
   private static final BlockPos PLAYER_SPAWN_OFFSET = new BlockPos(68, 25, 49);
   private static final String ELITE_FOUR_TRAINER_ID_PART = "boss";
   public static final String CHAMPION_TRAINER_GYM_TYPE = "elite4_champion";
   private static final int ELITE_FOUR_BASE_LEVEL = 95;
   private static final int CHAMPION_LEVEL = 100;
   private static final EliteFourStructure.GateBox CHAMPION_GATE = new EliteFourStructure.GateBox(new BlockPos(67, 24, 48), new BlockPos(69, 24, 50));
   private static final EliteFourStructure.GateBox CHAMPION_SLOW_FALL_FIELD = new EliteFourStructure.GateBox(new BlockPos(67, 21, 48), new BlockPos(69, 21, 50));
   private static final BlockPos LEGACY_CHAMPION_BEAM_MIN = new BlockPos(68, 4, 49);
   private static final BlockPos CHAMPION_BEAM_MIN = new BlockPos(68, 5, 49);
   private static final BlockPos CHAMPION_BEAM_MAX = new BlockPos(68, 30, 49);
   public static final int CHAMPION_BEAM_HEIGHT = CHAMPION_BEAM_MAX.getY() - CHAMPION_BEAM_MIN.getY() + 1;
   private static final EliteFourStructure.PlaquePlacement[] PLAQUES = new EliteFourStructure.PlaquePlacement[]{
      new EliteFourStructure.PlaquePlacement(
         EliteFourMember.ELECTRIC_GROUND, new BlockPos(55, 29, 49), Direction.WEST, () -> CobbleBash.ELITE_FOUR_PLAQUE_ELECTRIC_GROUND
      ),
      new EliteFourStructure.PlaquePlacement(
         EliteFourMember.WATER_STEEL, new BlockPos(68, 29, 62), Direction.SOUTH, () -> CobbleBash.ELITE_FOUR_PLAQUE_WATER_STEEL
      ),
      new EliteFourStructure.PlaquePlacement(
         EliteFourMember.GRASS_GHOST, new BlockPos(81, 29, 49), Direction.EAST, () -> CobbleBash.ELITE_FOUR_PLAQUE_GRASS_GHOST
      ),
      new EliteFourStructure.PlaquePlacement(
         EliteFourMember.FIRE_FAIRY, new BlockPos(68, 29, 36), Direction.NORTH, () -> CobbleBash.ELITE_FOUR_PLAQUE_FIRE_FAIRY
      )
   };
   private static final EliteFourStructure.TrainerPlacement[] TRAINERS = new EliteFourStructure.TrainerPlacement[]{
      new EliteFourStructure.TrainerPlacement(EliteFourMember.ELECTRIC_GROUND.getTrainerGymType(), new BlockPos(33, 28, 49), -90.0F, 95),
      new EliteFourStructure.TrainerPlacement(EliteFourMember.FIRE_FAIRY.getTrainerGymType(), new BlockPos(68, 28, 14), 0.0F, 95),
      new EliteFourStructure.TrainerPlacement(EliteFourMember.GRASS_GHOST.getTrainerGymType(), new BlockPos(103, 28, 49), 90.0F, 95),
      new EliteFourStructure.TrainerPlacement(EliteFourMember.WATER_STEEL.getTrainerGymType(), new BlockPos(68, 28, 84), 180.0F, 95),
      new EliteFourStructure.TrainerPlacement("elite4_champion", new BlockPos(68, 3, 33), 0.0F, 100)
   };

   private EliteFourStructure() {
   }

   public static void build(ServerLevel level, BlockPos origin, int slotId) {
      StructureTemplate structuretemplate = getTemplate(level);
      if (structuretemplate == null) {
         CobbleBash.LOGGER.warn("Could not build Elite Four because structure template {} was not found.", TEMPLATE_ID);
      } else {
         AABB aabb = getCleanupBox(origin, structuretemplate);
         clear(level, origin);
         StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setIgnoreEntities(true);
         structuretemplate.placeInWorld(level, origin, origin, structureplacesettings, level.getRandom(), 3);
         restoreSavedConnectionStates(level, origin, structureplacesettings, structuretemplate);
         restoreDecorativeEntities(level, origin, structureplacesettings, structuretemplate);
         paintStructureBiome(level, aabb);
         placePlaques(level, origin);
         spawnTrainers(level, origin, slotId);
      }
   }

   public static void build(ServerLevel level, BlockPos origin) {
      build(level, origin, 0);
   }

   public static void clear(ServerLevel level, BlockPos origin) {
      StructureTemplate structuretemplate = getTemplate(level);
      if (structuretemplate != null) {
         AABB aabb = getCleanupBox(origin, structuretemplate);
         clearEntities(level, aabb);
         clearEntities(level, aabb);
      }
   }

   public static void clearCachedBlocks(ServerLevel level, BlockPos origin) {
      StructureTemplate structuretemplate = getTemplate(level);
      if (structuretemplate != null) {
         AABB aabb = getCleanupBox(origin, structuretemplate);
         clearEntities(level, aabb);
         clearBlocks(level, aabb);
         clearEntities(level, aabb);
      }
   }

   public static BlockPos getPlayerSpawn(ServerLevel level, BlockPos origin) {
      StructureTemplate structuretemplate = getTemplate(level);
      return structuretemplate == null ? origin.offset(0, 1, 0) : origin.offset(PLAYER_SPAWN_OFFSET);
   }

   public static boolean openMemberGate(ServerLevel level, BlockPos origin, EliteFourMember member) {
      for (EliteFourStructure.PlaquePlacement elitefourstructure$plaqueplacement : PLAQUES) {
         if (elitefourstructure$plaqueplacement.member() == member) {
            EliteFourPlaqueBlock.openGate(level, origin.offset(elitefourstructure$plaqueplacement.offset()), elitefourstructure$plaqueplacement.facing());
            return true;
         }
      }

      return false;
   }

   public static void openChampionGate(ServerLevel level, BlockPos origin) {
      clearGateBox(level, origin, CHAMPION_GATE);
   }

   public static boolean isInsideSlowFallField(BlockPos origin, BlockPos pos) {
      return CHAMPION_SLOW_FALL_FIELD.contains(origin, pos);
   }

   public static BlockPos getChampionBeamMin(BlockPos origin) {
      return origin.offset(CHAMPION_BEAM_MIN);
   }

   public static BlockPos getChampionBeamMax(BlockPos origin) {
      return origin.offset(CHAMPION_BEAM_MAX);
   }

   public static void startChampionBeam(ServerLevel level, BlockPos origin) {
      stopChampionBeamAt(level, origin.offset(LEGACY_CHAMPION_BEAM_MIN));
      BlockPos blockpos = getChampionBeamMin(origin);
      BlockState blockstate = level.getBlockState(blockpos);
      if (!blockstate.is((Block)CobbleBash.ELITE_FOUR_CHAMPION_BEAM)) {
         level.setBlock(blockpos, ((EliteFourChampionBeamBlock)CobbleBash.ELITE_FOUR_CHAMPION_BEAM).defaultBlockState(), 3);
      }
   }

   public static void stopChampionBeam(ServerLevel level, BlockPos origin) {
      stopChampionBeamAt(level, origin.offset(LEGACY_CHAMPION_BEAM_MIN));
      stopChampionBeamAt(level, getChampionBeamMin(origin));
   }

   private static void stopChampionBeamAt(ServerLevel level, BlockPos pos) {
      if (level.getBlockState(pos).is((Block)CobbleBash.ELITE_FOUR_CHAMPION_BEAM)) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
      }
   }

   public static AABB getStructureBox(ServerLevel level, BlockPos origin) {
      StructureTemplate structuretemplate = getTemplate(level);
      return structuretemplate == null
         ? new AABB(origin.getX() - 8, origin.getY() - 8, origin.getZ() - 8, origin.getX() + 132, origin.getY() + 122, origin.getZ() + 116)
         : getCleanupBox(origin, structuretemplate);
   }

   private static void placePlaques(ServerLevel level, BlockPos origin) {
      for (EliteFourStructure.PlaquePlacement elitefourstructure$plaqueplacement : PLAQUES) {
         BlockState blockstate = (BlockState)elitefourstructure$plaqueplacement.block()
            .get()
            .defaultBlockState()
            .setValue(EliteFourPlaqueBlock.FACING, elitefourstructure$plaqueplacement.facing());
         level.setBlock(origin.offset(elitefourstructure$plaqueplacement.offset()), blockstate, 3);
      }
   }

   private static void spawnTrainers(ServerLevel level, BlockPos origin, int slotId) {
      for (EliteFourStructure.TrainerPlacement elitefourstructure$trainerplacement : TRAINERS) {
         GymPlatformBuilder.spawnTrainerEntity(
            level,
            origin,
            elitefourstructure$trainerplacement.gymType(),
            slotId,
            "boss",
            elitefourstructure$trainerplacement.level(),
            origin.offset(elitefourstructure$trainerplacement.offset()),
            elitefourstructure$trainerplacement.yaw()
         );
      }
   }

   private static void restoreSavedConnectionStates(ServerLevel level, BlockPos origin, StructurePlaceSettings settings, StructureTemplate template) {
      for (Block block : BuiltInRegistries.BLOCK) {
         if (isSavedStateBlock(block)) {
            for (StructureBlockInfo structureblockinfo : template.filterBlocks(origin, settings, block)) {
               BlockPos blockpos = structureblockinfo.pos();
               if (level.getBlockState(blockpos).is(block)) {
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

   private static void paintStructureBiome(ServerLevel level, AABB box) {
      Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
      List<ChunkAccess> list = new ArrayList<>();

      for (ChunkPos chunkpos : getChunks(box)) {
         LevelChunk levelchunk = level.getChunk(chunkpos.x, chunkpos.z);
         levelchunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, null);
         levelchunk.setUnsaved(true);
         list.add(levelchunk);
      }

      if (!list.isEmpty()) {
         level.getChunkSource().chunkMap.resendBiomesForChunks(list);
      }
   }

   private static StructureTemplate getTemplate(ServerLevel level) {
      return (StructureTemplate)level.getStructureManager().get(TEMPLATE_ID).orElse(null);
   }

   private static AABB getCleanupBox(BlockPos origin, StructureTemplate template) {
      BlockPos blockpos = new BlockPos(template.getSize());
      return new AABB(
         origin.getX() - 8,
         origin.getY() - 8,
         origin.getZ() - 8,
         origin.getX() + blockpos.getX() + 8,
         origin.getY() + blockpos.getY() + 8,
         origin.getZ() + blockpos.getZ() + 8
      );
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

   private static void clearGateBox(ServerLevel level, BlockPos origin, EliteFourStructure.GateBox box) {
      BlockPos blockpos = box.min(origin);
      BlockPos blockpos1 = box.max(origin);

      for (int i = blockpos.getX(); i <= blockpos1.getX(); i++) {
         for (int j = blockpos.getY(); j <= blockpos1.getY(); j++) {
            for (int k = blockpos.getZ(); k <= blockpos1.getZ(); k++) {
               level.setBlock(new BlockPos(i, j, k), Blocks.AIR.defaultBlockState(), 35);
            }
         }
      }
   }

   private static void clearEntities(ServerLevel level, AABB box) {
      level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof Player) || entity instanceof ItemEntity).forEach(Entity::discard);
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

   private record GateBox(BlockPos first, BlockPos second) {
      private BlockPos min(BlockPos origin) {
         BlockPos blockpos = origin.offset(this.first);
         BlockPos blockpos1 = origin.offset(this.second);
         return new BlockPos(
            Math.min(blockpos.getX(), blockpos1.getX()), Math.min(blockpos.getY(), blockpos1.getY()), Math.min(blockpos.getZ(), blockpos1.getZ())
         );
      }

      private BlockPos max(BlockPos origin) {
         BlockPos blockpos = origin.offset(this.first);
         BlockPos blockpos1 = origin.offset(this.second);
         return new BlockPos(
            Math.max(blockpos.getX(), blockpos1.getX()), Math.max(blockpos.getY(), blockpos1.getY()), Math.max(blockpos.getZ(), blockpos1.getZ())
         );
      }

      private boolean contains(BlockPos origin, BlockPos pos) {
         BlockPos blockpos = this.min(origin);
         BlockPos blockpos1 = this.max(origin);
         return pos.getX() >= blockpos.getX()
            && pos.getX() <= blockpos1.getX()
            && pos.getY() >= blockpos.getY()
            && pos.getY() <= blockpos1.getY()
            && pos.getZ() >= blockpos.getZ()
            && pos.getZ() <= blockpos1.getZ();
      }
   }

   private record PlaquePlacement(EliteFourMember member, BlockPos offset, Direction facing, Supplier<Block> block) {
   }

   private record TrainerPlacement(String gymType, BlockPos offset, float yaw, int level) {
   }
}
