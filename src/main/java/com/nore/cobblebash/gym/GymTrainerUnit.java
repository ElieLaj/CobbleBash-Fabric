package com.nore.cobblebash.gym;

public enum GymTrainerUnit {
    TRAINER_ONE("trainer one", "trainer_1", 0, 0),
    TRAINER_TWO("trainer two", "trainer_2", 1, 1),
    BOSS("boss", "boss", 2, 2);

    private final String displayName;
    private final String trainerIdPart;
    private final int levelIndex;
    private final int requiredStage;

    GymTrainerUnit(String displayName, String trainerIdPart, int levelIndex, int requiredStage) {
        this.displayName = displayName;
        this.trainerIdPart = trainerIdPart;
        this.levelIndex = levelIndex;
        this.requiredStage = requiredStage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTrainerIdPart() {
        return trainerIdPart;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public int getRequiredStage() {
        return requiredStage;
    }

    public static GymTrainerUnit fromTrainerIdPart(String trainerIdPart) {
        for (GymTrainerUnit unit : values()) {
            if (unit.trainerIdPart.equals(trainerIdPart)) {
                return unit;
            }
        }

        return null;
    }
}
