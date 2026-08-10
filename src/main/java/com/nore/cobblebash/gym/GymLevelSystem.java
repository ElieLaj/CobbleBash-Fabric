package com.nore.cobblebash.gym;

public class GymLevelSystem {
   private static final int TOTAL_GYMS = 18;
   private static final int MIN_LEVEL = 10;
   private static final int MAX_LEVEL = 100;
   private static final int TRAINER_SPACING = 2;

   public static int[] getTrainerLevels(int completedGyms) {
      int i = Math.max(0, Math.min(completedGyms, 17));
      double d0 = 5.0588235294117645;
      int j = (int)Math.round(10.0 + i * d0);
      int k = j;
      int l = j + 2;
      int i1 = Math.min(j + 4, 100);
      return new int[]{k, l, i1};
   }
}
