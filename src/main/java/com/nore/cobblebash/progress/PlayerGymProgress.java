package com.nore.cobblebash.progress;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PlayerGymProgress {
   private final Set<String> completedGyms = new HashSet<>();
   private String activeGymType = "none";

   public int getCompletedGymCount() {
      return this.completedGyms.size();
   }

   public String getActiveGymType() {
      return this.activeGymType;
   }

   public void setActiveGymType(String activeGymType) {
      this.activeGymType = activeGymType;
   }

   public void completeGym(String gymType) {
      this.completedGyms.add(gymType);
      this.activeGymType = "none";
   }

   public void markCompletedGyms(Collection<String> gymTypes) {
      this.completedGyms.addAll(gymTypes);
   }

   public boolean hasCompleted(String gymType) {
      return this.completedGyms.contains(gymType);
   }

   public boolean hasCompletedAll(Collection<String> gymTypes) {
      return this.completedGyms.containsAll(gymTypes);
   }

   public boolean isActiveGym(String gymType) {
      return this.activeGymType.equals(gymType);
   }
}
