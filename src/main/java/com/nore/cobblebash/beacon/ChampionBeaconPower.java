package com.nore.cobblebash.beacon;

import java.util.Arrays;

public enum ChampionBeaconPower {
   NONE(0, 0, false, false, false),
   REPEL(1, 1, true, true, false),
   LURE(2, 1, true, true, false),
   APRICORN(3, 2, true, false, true),
   BERRY(4, 2, true, false, true),
   DAYCARE(5, 3, true, false, true),
   EV(6, 4, true, false, true),
   SHINY(7, 5, false, true, false);

   private static final ChampionBeaconPower[] BY_ID = Arrays.stream(values())
      .sorted((left, right) -> Integer.compare(left.id, right.id))
      .toArray(ChampionBeaconPower[]::new);
   private final int id;
   private final int requiredLevel;
   private final boolean primary;
   private final boolean secondary;
   private final boolean upgradeable;

   ChampionBeaconPower(int id, int requiredLevel, boolean primary, boolean secondary, boolean upgradeable) {
      this.id = id;
      this.requiredLevel = requiredLevel;
      this.primary = primary;
      this.secondary = secondary;
      this.upgradeable = upgradeable;
   }

   public int id() {
      return this.id;
   }

   public int requiredLevel() {
      return this.requiredLevel;
   }

   public boolean isPrimary() {
      return this.primary;
   }

   public boolean isSecondary() {
      return this.secondary;
   }

   public boolean isUpgradeable() {
      return this.upgradeable;
   }

   public static ChampionBeaconPower byId(int id) {
      return id >= 0 && id < BY_ID.length ? BY_ID[id] : NONE;
   }
}
