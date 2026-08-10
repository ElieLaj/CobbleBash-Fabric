package com.nore.cobblebash.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class GymDoorController {
    private static final int CLEAR_GATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int PRESERVE_GATE_SHAPE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public static void buildClosedTestDoors(ServerLevel level, BlockPos origin) {
        closeDoor(level, getDoorOnePos(origin));
        closeDoor(level, getDoorTwoPos(origin));
    }

    public static void openDoorForStage(ServerLevel level, BlockPos origin, int stage) {
        openDoorForStage(level, origin, "bug", stage);
    }

    public static void openDoorForStage(ServerLevel level, BlockPos origin, String gymType, int stage) {
        GymStructureDefinition definition = GymStructureDefinition.get(gymType);
        if (definition != null) {
            openStructureGateForStage(level, origin, definition, stage);
            return;
        }

        if (stage >= 1) {
            openDoor(level, getDoorOnePos(origin));
        }

        if (stage >= 2) {
            openDoor(level, getDoorTwoPos(origin));
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
        BlockPos playerSpawn = origin.offset(definition.playerSpawnOffset());
        int clearFlags = definition.preservesGateNeighborShapes() ? PRESERVE_GATE_SHAPE_FLAGS : CLEAR_GATE_FLAGS;
        for (GymStructureDefinition.GateBox gate : gates) {
            clearBox(level, playerSpawn.offset(gate.min()), playerSpawn.offset(gate.max()), clearFlags);
        }
    }

    private static void clearBox(ServerLevel level, BlockPos first, BlockPos second, int clearFlags) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), clearFlags);
                }
            }
        }
    }
}
