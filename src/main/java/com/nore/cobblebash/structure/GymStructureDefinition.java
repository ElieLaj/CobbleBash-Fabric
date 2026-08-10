package com.nore.cobblebash.structure;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record GymStructureDefinition(
        String gymType,
        ResourceLocation templateId,
        BlockPos playerSpawnOffset,
        float playerYaw,
        BlockPos trainerOneOffset,
        float trainerOneYaw,
        BlockPos trainerTwoOffset,
        float trainerTwoYaw,
        BlockPos bossOffset,
        float bossYaw,
        List<GateBox> stageOneGates,
        List<GateBox> stageTwoGates
) {
    private static final float NORTH_YAW = 180.0F;
    private static final float SOUTH_YAW = 0.0F;
    private static final float EAST_YAW = -90.0F;
    private static final float WEST_YAW = 90.0F;

    private static final Map<String, GymStructureDefinition> DEFINITIONS = Map.ofEntries(
            Map.entry("grass", new GymStructureDefinition(
                    "grass",
                    template("grass"),
                    new BlockPos(19, 4, 76),
                    NORTH_YAW,
                    new BlockPos(0, 0, -7),
                    SOUTH_YAW,
                    new BlockPos(0, -2, -27),
                    SOUTH_YAW,
                    new BlockPos(0, -2, -57),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-1, 0, -15), new BlockPos(1, 2, -15))),
                    List.of(new GateBox(new BlockPos(-1, -2, -39), new BlockPos(1, 0, -39)))
            )),
            Map.entry("flying", new GymStructureDefinition(
                    "flying",
                    template("flying"),
                    new BlockPos(40, 9, 28),
                    NORTH_YAW,
                    new BlockPos(0, 9, -14),
                    SOUTH_YAW,
                    new BlockPos(-17, 18, -14),
                    EAST_YAW,
                    new BlockPos(-17, 30, 20),
                    NORTH_YAW,
                    List.of(),
                    List.of(new GateBox(new BlockPos(-16, 30, 11), new BlockPos(-18, 33, 11)))
            )),
            Map.entry("electric", new GymStructureDefinition(
                    "electric",
                    template("electric"),
                    new BlockPos(40, 3, 38),
                    NORTH_YAW,
                    new BlockPos(0, -2, -12),
                    SOUTH_YAW,
                    new BlockPos(0, 7, -12),
                    NORTH_YAW,
                    new BlockPos(0, 17, -12),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-1, -1, -23), new BlockPos(1, 2, -23))),
                    List.of(new GateBox(new BlockPos(-1, 16, -1), new BlockPos(1, 18, -1)))
            )),
            Map.entry("normal", new GymStructureDefinition(
                    "normal",
                    template("normal"),
                    new BlockPos(24, 3, 45),
                    NORTH_YAW,
                    new BlockPos(-13, -2, -14),
                    EAST_YAW,
                    new BlockPos(5, 0, -34),
                    SOUTH_YAW,
                    new BlockPos(29, 2, -7),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(4, 0, -23), new BlockPos(6, 3, -23))),
                    List.of(new GateBox(new BlockPos(11, 1, -8), new BlockPos(11, 4, -6)))
            )),
            Map.entry("poison", new GymStructureDefinition(
                    "poison",
                    template("poison"),
                    new BlockPos(53, 12, 89),
                    NORTH_YAW,
                    new BlockPos(-2, 1, -20),
                    SOUTH_YAW,
                    new BlockPos(11, 2, -38),
                    SOUTH_YAW,
                    new BlockPos(11, 2, -60),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(10, 2, -31), new BlockPos(12, 4, -31))),
                    List.of(new GateBox(new BlockPos(10, 3, -51), new BlockPos(12, 6, -51)))
            )),
            Map.entry("rock", new GymStructureDefinition(
                    "rock",
                    template("rock"),
                    new BlockPos(23, 2, 38),
                    NORTH_YAW,
                    new BlockPos(-12, 0, -10),
                    EAST_YAW,
                    new BlockPos(-12, 2, -29),
                    SOUTH_YAW,
                    new BlockPos(9, 2, -29),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(-13, 0, -17), new BlockPos(-11, 2, -17))),
                    List.of(new GateBox(new BlockPos(-4, 2, -30), new BlockPos(-4, 4, -28)))
            )),
            Map.entry("steel", new GymStructureDefinition(
                    "steel",
                    template("steel"),
                    new BlockPos(16, 73, 34),
                    NORTH_YAW,
                    new BlockPos(0, 0, -16),
                    SOUTH_YAW,
                    new BlockPos(19, 0, -16),
                    WEST_YAW,
                    new BlockPos(18, 11, 7),
                    NORTH_YAW,
                    List.of(
                            new GateBox(new BlockPos(8, 0, -16), new BlockPos(8, 0, -16)),
                            new GateBox(new BlockPos(8, 1, -17), new BlockPos(8, 2, -15))
                    ),
                    List.of(
                            new GateBox(new BlockPos(28, 2, -10), new BlockPos(27, 2, -10)),
                            new GateBox(new BlockPos(29, 3, -10), new BlockPos(27, 4, -10))
                    )
            )),
            Map.entry("water", new GymStructureDefinition(
                    "water",
                    template("water"),
                    new BlockPos(40, 35, 70),
                    NORTH_YAW,
                    new BlockPos(9, -15, -9),
                    WEST_YAW,
                    new BlockPos(-22, -22, -18),
                    EAST_YAW,
                    new BlockPos(-3, -18, -45),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-16, -22, -17), new BlockPos(-16, -18, -19))),
                    List.of(new GateBox(new BlockPos(-4, -17, -37), new BlockPos(-2, -13, -37)))
            )),
            Map.entry("psychic", new GymStructureDefinition(
                    "psychic",
                    template("psychic"),
                    new BlockPos(44, 14, 41),
                    NORTH_YAW,
                    new BlockPos(0, 0, -8),
                    SOUTH_YAW,
                    new BlockPos(0, 0, -25),
                    SOUTH_YAW,
                    new BlockPos(-28, 0, -25),
                    EAST_YAW,
                    List.of(
                            new GateBox(new BlockPos(-1, 0, -16), new BlockPos(1, 4, -16)),
                            new GateBox(new BlockPos(-2, 1, -16), new BlockPos(2, 3, -16))
                    ),
                    List.of(new GateBox(new BlockPos(-11, 0, -24), new BlockPos(-11, 2, -26)))
            )),
            Map.entry("ice", new GymStructureDefinition(
                    "ice",
                    template("ice"),
                    new BlockPos(14, 28, 15),
                    NORTH_YAW,
                    new BlockPos(1, -6, 0),
                    WEST_YAW,
                    new BlockPos(1, -14, 0),
                    EAST_YAW,
                    new BlockPos(1, -25, 0),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(2, -14, 1), new BlockPos(0, -11, -2))),
                    List.of(new GateBox(new BlockPos(3, -25, 1), new BlockPos(-1, -22, -2)))
            )),
            Map.entry("dark", new GymStructureDefinition(
                    "dark",
                    template("dark"),
                    new BlockPos(13, 5, 58),
                    NORTH_YAW,
                    new BlockPos(0, -2, -6),
                    SOUTH_YAW,
                    new BlockPos(0, -3, -22),
                    SOUTH_YAW,
                    new BlockPos(0, -2, -45),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-1, -2, -13), new BlockPos(1, 0, -13))),
                    List.of(new GateBox(new BlockPos(-1, -2, -31), new BlockPos(1, 0, -31)))
            )),
            Map.entry("dragon", new GymStructureDefinition(
                    "dragon",
                    template("dragon"),
                    new BlockPos(46, 14, 86),
                    NORTH_YAW,
                    new BlockPos(-31, -1, -31),
                    EAST_YAW,
                    new BlockPos(-6, 1, -38),
                    WEST_YAW,
                    new BlockPos(-17, -4, -73),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-4, 1, -36), new BlockPos(-8, 6, -40))),
                    List.of(new GateBox(new BlockPos(-19, -4, -71), new BlockPos(-15, 1, -75)))
            )),
            Map.entry("bug", new GymStructureDefinition(
                    "bug",
                    template("bug"),
                    new BlockPos(26, 2, 28),
                    NORTH_YAW,
                    new BlockPos(-16, 0, -10),
                    EAST_YAW,
                    new BlockPos(0, 0, -19),
                    SOUTH_YAW,
                    new BlockPos(16, 0, -10),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(-1, 0, -10), new BlockPos(1, 2, -11))),
                    List.of(new GateBox(new BlockPos(7, 0, -7), new BlockPos(9, 2, -5)))
            )),
            Map.entry("ground", new GymStructureDefinition(
                    "ground",
                    template("ground"),
                    new BlockPos(35, 8, 69),
                    NORTH_YAW,
                    new BlockPos(-10, 0, -18),
                    EAST_YAW,
                    new BlockPos(18, 2, -18),
                    WEST_YAW,
                    new BlockPos(4, 1, -42),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(9, 2, -19), new BlockPos(9, 4, -17))),
                    List.of(new GateBox(new BlockPos(3, 4, -28), new BlockPos(5, 6, -28)))
            )),
            Map.entry("fire", new GymStructureDefinition(
                    "fire",
                    template("fire"),
                    new BlockPos(7, 3, 37),
                    NORTH_YAW,
                    new BlockPos(0, 1, -8),
                    SOUTH_YAW,
                    new BlockPos(21, 1, -5),
                    WEST_YAW,
                    new BlockPos(50, 5, -7),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(11, 1, -6), new BlockPos(11, 3, -4))),
                    List.of(new GateBox(new BlockPos(39, 5, -8), new BlockPos(39, 7, -6)))
            )),
            Map.entry("fighting", new GymStructureDefinition(
                    "fighting",
                    template("fighting"),
                    new BlockPos(11, 2, 46),
                    NORTH_YAW,
                    new BlockPos(0, 0, -6),
                    SOUTH_YAW,
                    new BlockPos(0, 0, -24),
                    SOUTH_YAW,
                    new BlockPos(38, 3, -12),
                    WEST_YAW,
                    List.of(new GateBox(new BlockPos(-2, 0, -13), new BlockPos(2, 4, -13))),
                    List.of(
                            new GateBox(new BlockPos(22, 2, -13), new BlockPos(22, 4, -11)),
                            new GateBox(new BlockPos(33, 3, -12), new BlockPos(33, 4, -12))
                    )
            )),
            Map.entry("ghost", new GymStructureDefinition(
                    "ghost",
                    template("ghost"),
                    new BlockPos(27, 2, 41),
                    NORTH_YAW,
                    new BlockPos(18, 1, -3),
                    WEST_YAW,
                    new BlockPos(-18, 1, -9),
                    SOUTH_YAW,
                    new BlockPos(0, 5, -28),
                    SOUTH_YAW,
                    List.of(new GateBox(new BlockPos(-10, 1, -2), new BlockPos(-10, 5, -4))),
                    List.of(new GateBox(new BlockPos(-1, 6, -17), new BlockPos(1, 10, -17)))
            )),
            Map.entry("fairy", new GymStructureDefinition(
                    "fairy",
                    template("fairy"),
                    new BlockPos(25, 26, 60),
                    NORTH_YAW,
                    new BlockPos(-4, 3, -13),
                    SOUTH_YAW,
                    new BlockPos(23, 7, -17),
                    WEST_YAW,
                    new BlockPos(18, 2, 18),
                    NORTH_YAW,
                    List.of(new GateBox(new BlockPos(16, 7, -18), new BlockPos(16, 9, -16))),
                    List.of(new GateBox(new BlockPos(19, 2, 7), new BlockPos(17, 4, 7)))
            ))
    );

    public static GymStructureDefinition get(String gymType) {
        return DEFINITIONS.get(gymType);
    }

    public static Collection<GymStructureDefinition> values() {
        return DEFINITIONS.values();
    }

    public BlockPos playerRelative(BlockPos offset) {
        return playerSpawnOffset.offset(offset);
    }

    public boolean preservesGateNeighborShapes() {
        return "poison".equals(gymType);
    }

    public ResourceKey<Biome> biomeKey() {
        return switch (gymType) {
            case "dragon" -> Biomes.LUSH_CAVES;
            case "ground" -> Biomes.DESERT;
            case "poison" -> Biomes.SWAMP;
            case "water" -> Biomes.LUKEWARM_OCEAN;
            default -> Biomes.THE_VOID;
        };
    }

    private static ResourceLocation template(String gymType) {
        return ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gym/cobblebash_gym_" + gymType);
    }

    public record GateBox(BlockPos min, BlockPos max) {
    }
}
