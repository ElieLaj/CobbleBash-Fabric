package com.nore.cobblebash.instance;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.UUID;

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
        return trainerStage;
    }

    public boolean advanceTrainerStage() {
        if (trainerStage >= 3) {
            return false;
        }

        trainerStage++;
        return true;
    }

    public int getSlotId() {
        return slotId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getGymType() {
        return gymType;
    }

    public boolean isRepeatClear() {
        return repeatClear;
    }

    public int[] getTrainerLevels() {
        return trainerLevels;
    }

    public ResourceKey<Level> getReturnDimension() {
        return returnDimension;
    }

    public double getReturnX() {
        return returnX;
    }

    public double getReturnY() {
        return returnY;
    }

    public double getReturnZ() {
        return returnZ;
    }

    public float getReturnYRot() {
        return returnYRot;
    }

    public float getReturnXRot() {
        return returnXRot;
    }

    public GameType getReturnGameMode() {
        return returnGameMode;
    }
}
