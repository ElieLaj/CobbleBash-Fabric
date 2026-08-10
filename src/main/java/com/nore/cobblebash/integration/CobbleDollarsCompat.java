package com.nore.cobblebash.integration;

import com.nore.cobblebash.CobbleBash;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

public final class CobbleDollarsCompat {
   private static final String MOD_ID = "cobbledollars";
   private static final String PLAYER_EXTENSIONS = "fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt";
   private static Method getCobbleDollarsMethod;
   private static Method setCobbleDollarsMethod;
   private static boolean unavailableLogged;

   private CobbleDollarsCompat() {
   }

   public static boolean award(ServerPlayer player, int amount) {
      if (amount > 0 && FabricLoader.getInstance().isModLoaded("cobbledollars") && ensureMethodsLoaded()) {
         try {
            BigInteger biginteger = (BigInteger)getCobbleDollarsMethod.invoke(null, player);
            BigInteger biginteger1 = BigInteger.valueOf(amount);
            setCobbleDollarsMethod.invoke(null, player, biginteger.add(biginteger1));
            return true;
         } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            logUnavailable(exception);
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean ensureMethodsLoaded() {
      if (getCobbleDollarsMethod != null && setCobbleDollarsMethod != null) {
         return true;
      }

      try {
         Class<?> oclass = Class.forName("fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt");
         getCobbleDollarsMethod = oclass.getMethod("getCobbleDollars", Player.class);
         setCobbleDollarsMethod = oclass.getMethod("setCobbleDollars", Player.class, BigInteger.class);
         return true;
      } catch (ClassNotFoundException | NoSuchMethodException reflectiveoperationexception) {
         logUnavailable(reflectiveoperationexception);
         return false;
      }
   }

   private static void logUnavailable(Exception exception) {
      if (!unavailableLogged) {
         unavailableLogged = true;
         CobbleBash.LOGGER.warn("Cobble Dollars is loaded, but CobbleBash could not access its balance API.", exception);
      }
   }
}
