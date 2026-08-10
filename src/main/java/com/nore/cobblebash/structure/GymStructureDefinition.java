package com.nore.cobblebash.structure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

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
   List<GymStructureDefinition.GateBox> stageOneGates,
   List<GymStructureDefinition.GateBox> stageTwoGates
) {
   private static final float NORTH_YAW = 180.0F;
   private static final float SOUTH_YAW = 0.0F;
   private static final float EAST_YAW = -90.0F;
   private static final float WEST_YAW = 90.0F;
   private static final Map<String, GymStructureDefinition> DEFINITIONS = Map.ofEntries(
      Map.entry(
         "grass",
         new GymStructureDefinition(
            "grass",
            template("grass"),
            new BlockPos(19, 4, 76),
            180.0F,
            new BlockPos(0, 0, -7),
            0.0F,
            new BlockPos(0, -2, -27),
            0.0F,
            new BlockPos(0, -2, -57),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, 0, -15), new BlockPos(1, 2, -15))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, -2, -39), new BlockPos(1, 0, -39)))
         )
      ),
      Map.entry(
         "flying",
         new GymStructureDefinition(
            "flying",
            template("flying"),
            new BlockPos(40, 9, 28),
            180.0F,
            new BlockPos(0, 9, -14),
            0.0F,
            new BlockPos(-17, 18, -14),
            -90.0F,
            new BlockPos(-17, 30, 20),
            180.0F,
            List.of(),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-16, 30, 11), new BlockPos(-18, 33, 11)))
         )
      ),
      Map.entry(
         "electric",
         new GymStructureDefinition(
            "electric",
            template("electric"),
            new BlockPos(40, 3, 38),
            180.0F,
            new BlockPos(0, -2, -12),
            0.0F,
            new BlockPos(0, 7, -12),
            180.0F,
            new BlockPos(0, 17, -12),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, -1, -23), new BlockPos(1, 2, -23))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, 16, -1), new BlockPos(1, 18, -1)))
         )
      ),
      Map.entry(
         "normal",
         new GymStructureDefinition(
            "normal",
            template("normal"),
            new BlockPos(24, 3, 45),
            180.0F,
            new BlockPos(-13, -2, -14),
            -90.0F,
            new BlockPos(5, 0, -34),
            0.0F,
            new BlockPos(29, 2, -7),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(4, 0, -23), new BlockPos(6, 3, -23))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(11, 1, -8), new BlockPos(11, 4, -6)))
         )
      ),
      Map.entry(
         "poison",
         new GymStructureDefinition(
            "poison",
            template("poison"),
            new BlockPos(53, 12, 89),
            180.0F,
            new BlockPos(-2, 1, -20),
            0.0F,
            new BlockPos(11, 2, -38),
            0.0F,
            new BlockPos(11, 2, -60),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(10, 2, -31), new BlockPos(12, 4, -31))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(10, 3, -51), new BlockPos(12, 6, -51)))
         )
      ),
      Map.entry(
         "rock",
         new GymStructureDefinition(
            "rock",
            template("rock"),
            new BlockPos(23, 2, 38),
            180.0F,
            new BlockPos(-12, 0, -10),
            -90.0F,
            new BlockPos(-12, 2, -29),
            0.0F,
            new BlockPos(9, 2, -29),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-13, 0, -17), new BlockPos(-11, 2, -17))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-4, 2, -30), new BlockPos(-4, 4, -28)))
         )
      ),
      Map.entry(
         "steel",
         new GymStructureDefinition(
            "steel",
            template("steel"),
            new BlockPos(16, 73, 34),
            180.0F,
            new BlockPos(0, 0, -16),
            0.0F,
            new BlockPos(19, 0, -16),
            90.0F,
            new BlockPos(18, 11, 7),
            180.0F,
            List.of(
               new GymStructureDefinition.GateBox(new BlockPos(8, 0, -16), new BlockPos(8, 0, -16)),
               new GymStructureDefinition.GateBox(new BlockPos(8, 1, -17), new BlockPos(8, 2, -15))
            ),
            List.of(
               new GymStructureDefinition.GateBox(new BlockPos(28, 2, -10), new BlockPos(27, 2, -10)),
               new GymStructureDefinition.GateBox(new BlockPos(29, 3, -10), new BlockPos(27, 4, -10))
            )
         )
      ),
      Map.entry(
         "water",
         new GymStructureDefinition(
            "water",
            template("water"),
            new BlockPos(40, 35, 70),
            180.0F,
            new BlockPos(9, -15, -9),
            90.0F,
            new BlockPos(-22, -22, -18),
            -90.0F,
            new BlockPos(-3, -18, -45),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-16, -22, -17), new BlockPos(-16, -18, -19))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-4, -17, -37), new BlockPos(-2, -13, -37)))
         )
      ),
      Map.entry(
         "psychic",
         new GymStructureDefinition(
            "psychic",
            template("psychic"),
            new BlockPos(44, 14, 41),
            180.0F,
            new BlockPos(0, 0, -8),
            0.0F,
            new BlockPos(0, 0, -25),
            0.0F,
            new BlockPos(-28, 0, -25),
            -90.0F,
            List.of(
               new GymStructureDefinition.GateBox(new BlockPos(-1, 0, -16), new BlockPos(1, 4, -16)),
               new GymStructureDefinition.GateBox(new BlockPos(-2, 1, -16), new BlockPos(2, 3, -16))
            ),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-11, 0, -24), new BlockPos(-11, 2, -26)))
         )
      ),
      Map.entry(
         "ice",
         new GymStructureDefinition(
            "ice",
            template("ice"),
            new BlockPos(14, 28, 15),
            180.0F,
            new BlockPos(1, -6, 0),
            90.0F,
            new BlockPos(1, -14, 0),
            -90.0F,
            new BlockPos(1, -25, 0),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(2, -14, 1), new BlockPos(0, -11, -2))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(3, -25, 1), new BlockPos(-1, -22, -2)))
         )
      ),
      Map.entry(
         "dark",
         new GymStructureDefinition(
            "dark",
            template("dark"),
            new BlockPos(13, 5, 58),
            180.0F,
            new BlockPos(0, -2, -6),
            0.0F,
            new BlockPos(0, -3, -22),
            0.0F,
            new BlockPos(0, -2, -45),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, -2, -13), new BlockPos(1, 0, -13))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, -2, -31), new BlockPos(1, 0, -31)))
         )
      ),
      Map.entry(
         "dragon",
         new GymStructureDefinition(
            "dragon",
            template("dragon"),
            new BlockPos(46, 14, 86),
            180.0F,
            new BlockPos(-31, -1, -31),
            -90.0F,
            new BlockPos(-6, 1, -38),
            90.0F,
            new BlockPos(-17, -4, -73),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-4, 1, -36), new BlockPos(-8, 6, -40))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-19, -4, -71), new BlockPos(-15, 1, -75)))
         )
      ),
      Map.entry(
         "bug",
         new GymStructureDefinition(
            "bug",
            template("bug"),
            new BlockPos(26, 2, 28),
            180.0F,
            new BlockPos(-16, 0, -10),
            -90.0F,
            new BlockPos(0, 0, -19),
            0.0F,
            new BlockPos(16, 0, -10),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, 0, -10), new BlockPos(1, 2, -11))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(7, 0, -7), new BlockPos(9, 2, -5)))
         )
      ),
      Map.entry(
         "ground",
         new GymStructureDefinition(
            "ground",
            template("ground"),
            new BlockPos(35, 8, 69),
            180.0F,
            new BlockPos(-10, 0, -18),
            -90.0F,
            new BlockPos(18, 2, -18),
            90.0F,
            new BlockPos(4, 1, -42),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(9, 2, -19), new BlockPos(9, 4, -17))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(3, 4, -28), new BlockPos(5, 6, -28)))
         )
      ),
      Map.entry(
         "fire",
         new GymStructureDefinition(
            "fire",
            template("fire"),
            new BlockPos(7, 3, 37),
            180.0F,
            new BlockPos(0, 1, -8),
            0.0F,
            new BlockPos(21, 1, -5),
            90.0F,
            new BlockPos(50, 5, -7),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(11, 1, -6), new BlockPos(11, 3, -4))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(39, 5, -8), new BlockPos(39, 7, -6)))
         )
      ),
      Map.entry(
         "fighting",
         new GymStructureDefinition(
            "fighting",
            template("fighting"),
            new BlockPos(11, 2, 46),
            180.0F,
            new BlockPos(0, 0, -6),
            0.0F,
            new BlockPos(0, 0, -24),
            0.0F,
            new BlockPos(38, 3, -12),
            90.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-2, 0, -13), new BlockPos(2, 4, -13))),
            List.of(
               new GymStructureDefinition.GateBox(new BlockPos(22, 2, -13), new BlockPos(22, 4, -11)),
               new GymStructureDefinition.GateBox(new BlockPos(33, 3, -12), new BlockPos(33, 4, -12))
            )
         )
      ),
      Map.entry(
         "ghost",
         new GymStructureDefinition(
            "ghost",
            template("ghost"),
            new BlockPos(27, 2, 41),
            180.0F,
            new BlockPos(18, 1, -3),
            90.0F,
            new BlockPos(-18, 1, -9),
            0.0F,
            new BlockPos(0, 5, -28),
            0.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-10, 1, -2), new BlockPos(-10, 5, -4))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(-1, 6, -17), new BlockPos(1, 10, -17)))
         )
      ),
      Map.entry(
         "fairy",
         new GymStructureDefinition(
            "fairy",
            template("fairy"),
            new BlockPos(25, 26, 60),
            180.0F,
            new BlockPos(-4, 3, -13),
            0.0F,
            new BlockPos(23, 7, -17),
            90.0F,
            new BlockPos(18, 2, 18),
            180.0F,
            List.of(new GymStructureDefinition.GateBox(new BlockPos(16, 7, -18), new BlockPos(16, 9, -16))),
            List.of(new GymStructureDefinition.GateBox(new BlockPos(19, 2, 7), new BlockPos(17, 4, 7)))
         )
      )
   );

   public static GymStructureDefinition get(String gymType) {
      return DEFINITIONS.get(gymType);
   }

   public static Collection<GymStructureDefinition> values() {
      return DEFINITIONS.values();
   }

   public BlockPos playerRelative(BlockPos offset) {
      return this.playerSpawnOffset.offset(offset);
   }

   public boolean preservesGateNeighborShapes() {
      return "poison".equals(this.gymType);
   }

   public ResourceKey<Biome> biomeKey() {
      return switch (this.gymType) {
         case "dragon" -> Biomes.LUSH_CAVES;
         case "ground" -> Biomes.DESERT;
         case "poison" -> Biomes.SWAMP;
         case "water" -> Biomes.LUKEWARM_OCEAN;
         default -> Biomes.THE_VOID;
      };
   }

   private static ResourceLocation template(String gymType) {
      return ResourceLocation.fromNamespaceAndPath("cobblebash", "gym/cobblebash_gym_" + gymType);
   }

   public record GateBox(BlockPos min, BlockPos max) {
   }
}
