package com.nore.cobblebash.progress;

import java.util.HashSet;
import java.util.Collection;
import java.util.Set;

public class PlayerGymProgress {
    private final Set<String> completedGyms = new HashSet<>();
    private String activeGymType = "none";

    public int getCompletedGymCount() {
        return completedGyms.size();
    }

    public String getActiveGymType() {
        return activeGymType;
    }

    public void setActiveGymType(String activeGymType) {
        this.activeGymType = activeGymType;
    }

    public void completeGym(String gymType) {
        completedGyms.add(gymType);
        activeGymType = "none";
    }

    public void markCompletedGyms(Collection<String> gymTypes) {
        completedGyms.addAll(gymTypes);
    }

    public boolean hasCompleted(String gymType) {
        return completedGyms.contains(gymType);
    }

    public boolean isActiveGym(String gymType) {
        return activeGymType.equals(gymType);
    }
}
