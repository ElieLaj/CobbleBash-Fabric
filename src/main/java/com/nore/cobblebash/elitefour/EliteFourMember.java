package com.nore.cobblebash.elitefour;

import java.util.Arrays;
import java.util.List;

public enum EliteFourMember {
   ELECTRIC_GROUND("electric_ground", "Electric/Ground"),
   WATER_STEEL("water_steel", "Water/Steel"),
   GRASS_GHOST("grass_ghost", "Grass/Ghost"),
   FIRE_FAIRY("fire_fairy", "Fire/Fairy");

   private static final List<EliteFourMember> ORDERED = List.of(values());
   private final String id;
   private final String displayName;

   EliteFourMember(String id, String displayName) {
      this.id = id;
      this.displayName = displayName;
   }

   public String getId() {
      return this.id;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getTrainerGymType() {
      return "elite4_" + this.id;
   }

   public static List<EliteFourMember> ordered() {
      return ORDERED;
   }

   public static EliteFourMember fromId(String id) {
      return Arrays.stream(values()).filter(member -> member.id.equals(id)).findFirst().orElse(null);
   }

   public static EliteFourMember fromTrainerGymType(String gymType) {
      return Arrays.stream(values()).filter(member -> member.getTrainerGymType().equals(gymType)).findFirst().orElse(null);
   }
}
