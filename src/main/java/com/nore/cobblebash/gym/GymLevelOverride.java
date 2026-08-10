package com.nore.cobblebash.gym;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Niveau d'arene choisi a la main pour la prochaine entree.
 *
 * <p>Ajout au mod d'origine, qui deduit toujours le niveau du nombre d'arenes
 * deja terminees. On ne touche pas a {@link GymLevelSystem} : le choix est pose
 * ici juste avant l'entree, consomme une fois, puis oublie. Une valeur oubliee
 * ne peut donc pas s'appliquer a une entree ulterieure.
 */
public final class GymLevelOverride {
   /** Bornes du systeme d'origine : premiere arene a 10, derniere a 100. */
   public static final int MIN = 10;
   public static final int MAX = 100;

   private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

   private GymLevelOverride() {
   }

   public static void set(ServerPlayer player, int level) {
      PENDING.put(player.getUUID(), Math.max(MIN, Math.min(MAX, level)));
   }

   /** Rend le choix en attente et l'efface, ou -1 s'il n'y en a pas. */
   public static int take(UUID playerId) {
      Integer level = PENDING.remove(playerId);
      return level == null ? -1 : level;
   }

   public static void clear(UUID playerId) {
      PENDING.remove(playerId);
   }

   /**
    * Niveaux des trois dresseurs : le choix manuel s'il y en a un, sinon la
    * progression habituelle. L'ecart entre les trois reste celui d'origine.
    */
   public static int[] trainerLevels(UUID playerId, int completedGyms) {
      int chosen = take(playerId);
      if (chosen < 0) {
         return GymLevelSystem.getTrainerLevels(completedGyms);
      }

      return new int[]{chosen, Math.min(chosen + 2, MAX), Math.min(chosen + 4, MAX)};
   }
}
