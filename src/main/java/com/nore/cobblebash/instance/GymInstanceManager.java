package com.nore.cobblebash.instance;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class GymInstanceManager {
    private static final Map<UUID, GymInstance> ACTIVE_BY_PLAYER = new HashMap<>();
    private static final Map<Integer, GymInstance> ACTIVE_BY_SLOT = new HashMap<>();
    private static final Queue<Integer> FREE_SLOTS = new ArrayDeque<>();

    private static int nextSlotId = 0;

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
        GymInstance existing = ACTIVE_BY_PLAYER.get(playerId);
        if (existing != null) {
            return existing;
        }

        int slotId = FREE_SLOTS.isEmpty() ? nextSlotId++ : FREE_SLOTS.poll();

        GymInstance instance = new GymInstance(
                slotId,
                playerId,
                gymType,
                repeatClear,
                trainerLevels,
                returnDimension,
                returnX,
                returnY,
                returnZ,
                returnYRot,
                returnXRot,
                returnGameMode
        );

        ACTIVE_BY_PLAYER.put(playerId, instance);
        ACTIVE_BY_SLOT.put(slotId, instance);

        return instance;
    }

    public static GymInstance getActive(UUID playerId) {
        return ACTIVE_BY_PLAYER.get(playerId);
    }

    public static GymInstance clear(UUID playerId) {
        GymInstance instance = ACTIVE_BY_PLAYER.remove(playerId);

        if (instance != null) {
            ACTIVE_BY_SLOT.remove(instance.getSlotId());
            FREE_SLOTS.add(instance.getSlotId());
        }

        return instance;
    }

    public static int getActiveCount() {
        return ACTIVE_BY_PLAYER.size();
    }

    public static int getFreeSlotCount() {
        return FREE_SLOTS.size();
    }

    public static int getNextSlotId() {
        return nextSlotId;
    }
}
