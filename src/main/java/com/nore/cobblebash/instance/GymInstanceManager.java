package com.nore.cobblebash.instance;

import com.nore.cobblebash.gym.GymType;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class GymInstanceManager {
   private static final Map<UUID, GymInstance> ACTIVE_BY_PLAYER = new HashMap<>();
   private static final Map<Integer, GymInstance> ACTIVE_BY_SLOT = new HashMap<>();
   private static final Map<String, Queue<Integer>> FREE_SLOTS_BY_GYM = new HashMap<>();
   private static final Map<String, Integer> PRIMARY_SLOTS_BY_GYM = createPrimarySlots();
   private static int nextOverflowSlotId = PRIMARY_SLOTS_BY_GYM.size();

   public static GymInstance createOrGet(
      UUID playerId,
      String gymType,
      boolean repeatClear,
      int[] trainerLevels,
      ResourceKey<Level> returnDimension,
      double returnX,
      double returnY,
      double returnZ,
      float returnYRot,
      float returnXRot,
      GameType returnGameMode
   ) {
      GymInstance gyminstance = ACTIVE_BY_PLAYER.get(playerId);
      if (gyminstance != null) {
         return gyminstance;
      }

      int i = getReusableSlot(gymType);
      GymInstance gyminstance1 = new GymInstance(
         i, playerId, gymType, repeatClear, trainerLevels, returnDimension, returnX, returnY, returnZ, returnYRot, returnXRot, returnGameMode
      );
      ACTIVE_BY_PLAYER.put(playerId, gyminstance1);
      ACTIVE_BY_SLOT.put(i, gyminstance1);
      return gyminstance1;
   }

   public static GymInstance getActive(UUID playerId) {
      return ACTIVE_BY_PLAYER.get(playerId);
   }

   public static GymInstance clear(UUID playerId) {
      GymInstance gyminstance = ACTIVE_BY_PLAYER.remove(playerId);
      if (gyminstance != null) {
         ACTIVE_BY_SLOT.remove(gyminstance.getSlotId());
         FREE_SLOTS_BY_GYM.computeIfAbsent(gyminstance.getGymType(), ignored -> new ArrayDeque<>()).add(gyminstance.getSlotId());
      }

      return gyminstance;
   }

   public static int getActiveCount() {
      return ACTIVE_BY_PLAYER.size();
   }

   public static int getFreeSlotCount() {
      int i = 0;

      for (Queue<Integer> queue : FREE_SLOTS_BY_GYM.values()) {
         i += queue.size();
      }

      return i;
   }

   public static int getNextSlotId() {
      return nextOverflowSlotId;
   }

   private static int getReusableSlot(String gymType) {
      Queue<Integer> queue = FREE_SLOTS_BY_GYM.get(gymType);

      while (queue != null && !queue.isEmpty()) {
         int i = queue.poll();
         if (!ACTIVE_BY_SLOT.containsKey(i)) {
            return i;
         }
      }

      Integer integer = PRIMARY_SLOTS_BY_GYM.get(gymType);
      return integer != null && !ACTIVE_BY_SLOT.containsKey(integer) ? integer : nextOverflowSlotId++;
   }

   private static Map<String, Integer> createPrimarySlots() {
      Map<String, Integer> map = new LinkedHashMap<>();

      for (GymType gymtype : GymType.values()) {
         map.put(gymtype.getId(), map.size());
      }

      map.put("elite4", map.size());
      return Map.copyOf(map);
   }
}
