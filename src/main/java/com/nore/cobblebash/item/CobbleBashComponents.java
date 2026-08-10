package com.nore.cobblebash.item;

import com.mojang.serialization.Codec;
import com.nore.cobblebash.CobbleBash;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Composants de donnees du mod.
 *
 * <p>Un seul pour l'instant : le niveau d'arene grave dans le sac de butin. Il
 * doit etre porte par l'objet et non deduit a l'ouverture, parce que le sac se
 * garde : gagne a l'arene Insecte niveau 12, il doit encore payer le tarif du
 * niveau 12 le jour ou son proprietaire en est a la centieme.
 */
public final class CobbleBashComponents {
   public static DataComponentType<Integer> GYM_LEVEL;

   private CobbleBashComponents() {
   }

   public static void bootstrap() {
      GYM_LEVEL = Registry.register(
         BuiltInRegistries.DATA_COMPONENT_TYPE,
         CobbleBash.id("gym_level"),
         DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
      );
   }
}
