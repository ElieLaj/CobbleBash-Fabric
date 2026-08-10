package com.nore.cobblebash.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GymProgressManager {
    private static final Map<UUID, PlayerGymProgress> PLAYER_PROGRESS = new HashMap<>();

    public static PlayerGymProgress get(UUID playerId) {
        return PLAYER_PROGRESS.computeIfAbsent(playerId, id -> new PlayerGymProgress());
    }
}