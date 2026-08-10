package com.nore.cobblebash.instance;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class GymInstance {
   private final int slotId;
   private final UUID ownerId;
   private final String gymType;
   private final boolean repeatClear;
   private final int[] trainerLevels;
   private final ResourceKey<Level> returnDimension;
   private final double returnX;
   private final double returnY;
   private final double returnZ;
   private final float returnYRot;
   private final float returnXRot;
   private final GameType returnGameMode;
   private int trainerStage = 0;
   private final Set<String> defeatedEliteFourMembers = new LinkedHashSet<>();
   private String activeEliteFourMember = "";
   private boolean eliteFourChampionUnlocked = false;
   private boolean eliteFourSlowFallingApplied = false;
   private int eliteFourChampionBeamTicks = 0;

   public GymInstance(
      int slotId,
      UUID ownerId,
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
      this.slotId = slotId;
      this.ownerId = ownerId;
      this.gymType = gymType;
      this.repeatClear = repeatClear;
      this.trainerLevels = trainerLevels;
      this.returnDimension = returnDimension;
      this.returnX = returnX;
      this.returnY = returnY;
      this.returnZ = returnZ;
      this.returnYRot = returnYRot;
      this.returnXRot = returnXRot;
      this.returnGameMode = returnGameMode;
   }

   public int getTrainerStage() {
      return this.trainerStage;
   }

   public boolean advanceTrainerStage() {
      if (this.trainerStage >= 3) {
         return false;
      }

      this.trainerStage++;
      return true;
   }

   public boolean hasActiveEliteFourMember() {
      return !this.activeEliteFourMember.isBlank();
   }

   public String getActiveEliteFourMember() {
      return this.activeEliteFourMember;
   }

   public boolean selectEliteFourMember(String memberId) {
      if (memberId != null && !memberId.isBlank() && !this.hasActiveEliteFourMember() && !this.defeatedEliteFourMembers.contains(memberId)) {
         this.activeEliteFourMember = memberId;
         return true;
      } else {
         return false;
      }
   }

   public boolean completeActiveEliteFourMember() {
      if (!this.hasActiveEliteFourMember()) {
         return false;
      }

      this.defeatedEliteFourMembers.add(this.activeEliteFourMember);
      this.activeEliteFourMember = "";
      return true;
   }

   public boolean hasDefeatedEliteFourMember(String memberId) {
      return this.defeatedEliteFourMembers.contains(memberId);
   }

   public int getDefeatedEliteFourMemberCount() {
      return this.defeatedEliteFourMembers.size();
   }

   public Set<String> getDefeatedEliteFourMembers() {
      return Set.copyOf(this.defeatedEliteFourMembers);
   }

   public boolean isEliteFourChampionUnlocked() {
      return this.eliteFourChampionUnlocked;
   }

   public void unlockEliteFourChampion() {
      this.eliteFourChampionUnlocked = true;
   }

   public boolean hasEliteFourSlowFallingApplied() {
      return this.eliteFourSlowFallingApplied;
   }

   public void markEliteFourSlowFallingApplied() {
      this.eliteFourSlowFallingApplied = true;
   }

   public int getEliteFourChampionBeamTicks() {
      return this.eliteFourChampionBeamTicks;
   }

   public void setEliteFourChampionBeamTicks(int eliteFourChampionBeamTicks) {
      this.eliteFourChampionBeamTicks = eliteFourChampionBeamTicks;
   }

   public void tickEliteFourChampionBeam() {
      if (this.eliteFourChampionBeamTicks > 0) {
         this.eliteFourChampionBeamTicks--;
      }
   }

   public boolean isEliteFourChampionBeamActive() {
      return this.eliteFourChampionBeamTicks != 0;
   }

   public int getSlotId() {
      return this.slotId;
   }

   public UUID getOwnerId() {
      return this.ownerId;
   }

   public String getGymType() {
      return this.gymType;
   }

   public boolean isRepeatClear() {
      return this.repeatClear;
   }

   public int[] getTrainerLevels() {
      return this.trainerLevels;
   }

   public ResourceKey<Level> getReturnDimension() {
      return this.returnDimension;
   }

   public double getReturnX() {
      return this.returnX;
   }

   public double getReturnY() {
      return this.returnY;
   }

   public double getReturnZ() {
      return this.returnZ;
   }

   public float getReturnYRot() {
      return this.returnYRot;
   }

   public float getReturnXRot() {
      return this.returnXRot;
   }

   public GameType getReturnGameMode() {
      return this.returnGameMode;
   }
}
