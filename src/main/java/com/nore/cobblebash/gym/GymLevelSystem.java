package com.nore.cobblebash.gym;

public class GymLevelSystem {

    private static final int TOTAL_GYMS = 18;
    private static final int MIN_LEVEL = 10;
    private static final int MAX_LEVEL = 100;
    private static final int TRAINER_SPACING = 2;

    public static int[] getTrainerLevels(int completedGyms) {
        int gymIndex = Math.max(0, Math.min(completedGyms, TOTAL_GYMS - 1));

        double step = (double)(MAX_LEVEL - 4 - MIN_LEVEL) / (TOTAL_GYMS - 1);

        int baseLevel = (int)Math.round(MIN_LEVEL + (gymIndex * step));

        int t1 = baseLevel;
        int t2 = baseLevel + TRAINER_SPACING;
        int t3 = Math.min(baseLevel + (TRAINER_SPACING * 2), MAX_LEVEL);

        return new int[]{t1, t2, t3};
    }
}