package com.nore.cobblebash.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class GymDoorController {
   private static final int CLEAR_GATE_FLAGS = 35;
   private static final int PRESERVE_GATE_SHAPE_FLAGS = 50;

   public static void buildClosedTestDoors(ServerLevel level, BlockPos origin) {
      closeDoor(level, getDoorOnePos(origin));
      closeDoor(level, getDoorTwoPos(origin));
   }

   public static void openDoorForStage(ServerLevel level, BlockPos origin, int stage) {
      openDoorForStage(level, origin, "bug", stage);
   }

   public static void openDoorForStage(ServerLevel level, BlockPos origin, String gymType, int stage) {
      GymStructureDefinition gymstructuredefinition = GymStructureDefinition.get(gymType);
      if (gymstructuredefinition != null) {
         openStructureGateForStage(level, origin, gymstructuredefinition, stage);
      } else {
         if (stage >= 1) {
            openDoor(level, getDoorOnePos(origin));
         }

         if (stage >= 2) {
            openDoor(level, getDoorTwoPos(origin));
         }
      }
   }

   private static BlockPos getDoorOnePos(BlockPos origin) {
      return origin.offset(0, 0, 5);
   }

   private static BlockPos getDoorTwoPos(BlockPos origin) {
      return origin.offset(0, 0, 8);
   }

   private static void closeDoor(ServerLevel level, BlockPos pos) {
      level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
      level.setBlock(pos.above(), Blocks.IRON_BARS.defaultBlockState(), 3);
   }

   private static void openDoor(ServerLevel level, BlockPos pos) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
   }

   private static void openStructureGateForStage(ServerLevel level, BlockPos origin, GymStructureDefinition definition, int stage) {
      if (stage >= 1) {
         clearBoxes(level, origin, definition, definition.stageOneGates());
      }

      if (stage >= 2) {
         clearBoxes(level, origin, definition, definition.stageTwoGates());
      }
   }

   private static void clearBoxes(ServerLevel level, BlockPos origin, GymStructureDefinition definition, Iterable<GymStructureDefinition.GateBox> gates) {
      BlockPos blockpos = origin.offset(definition.playerSpawnOffset());
      int i = definition.preservesGateNeighborShapes() ? 50 : 35;

      for (GymStructureDefinition.GateBox gymstructuredefinition$gatebox : gates) {
         clearBox(level, blockpos.offset(gymstructuredefinition$gatebox.min()), blockpos.offset(gymstructuredefinition$gatebox.max()), i);
      }
   }

   private static void clearBox(ServerLevel level, BlockPos first, BlockPos second, int clearFlags) {
      int i = Math.min(first.getX(), second.getX());
      int j = Math.min(first.getY(), second.getY());
      int k = Math.min(first.getZ(), second.getZ());
      int l = Math.max(first.getX(), second.getX());
      int i1 = Math.max(first.getY(), second.getY());
      int j1 = Math.max(first.getZ(), second.getZ());

      for (int k1 = i; k1 <= l; k1++) {
         for (int l1 = j; l1 <= i1; l1++) {
            for (int i2 = k; i2 <= j1; i2++) {
               level.setBlock(new BlockPos(k1, l1, i2), Blocks.AIR.defaultBlockState(), clearFlags);
            }
         }
      }
   }
}
