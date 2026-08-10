package com.nore.cobblebash.item;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.nore.cobblebash.CobbleBash;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

public final class RibbonAttributeManager {
   private static final Map<UUID, EnumMap<RibbonAttributeManager.RibbonKind, RibbonAttributeManager.RibbonGroup>> SESSIONS = new ConcurrentHashMap<>();

   private RibbonAttributeManager() {
   }

   public static void equip(RibbonSlot context, RibbonAttributeManager.RibbonKind kind) {
      if (context.entity() instanceof ServerPlayer serverplayer) {
         EnumMap<RibbonAttributeManager.RibbonKind, RibbonAttributeManager.RibbonGroup> enummap = SESSIONS.computeIfAbsent(
            serverplayer.getUUID(), id -> new EnumMap<>(RibbonAttributeManager.RibbonKind.class)
         );
         RibbonAttributeManager.RibbonGroup ribbonattributemanager$ribbongroup = enummap.computeIfAbsent(
            kind, ignored -> new RibbonAttributeManager.RibbonGroup(kind, subscribe(serverplayer, kind))
         );
         ribbonattributemanager$ribbongroup.sessions
            .put(RibbonAttributeManager.SlotKey.from(context), new RibbonAttributeManager.RibbonSession(RibbonAttributeManager.SlotKey.from(context)));
         apply(serverplayer, ribbonattributemanager$ribbongroup, true);
      }
   }

   public static void unequip(RibbonSlot context, RibbonAttributeManager.RibbonKind kind) {
      if (context.entity() instanceof ServerPlayer serverplayer) {
         EnumMap<RibbonAttributeManager.RibbonKind, RibbonAttributeManager.RibbonGroup> enummap = SESSIONS.get(serverplayer.getUUID());
         if (enummap == null) {
            removeModifiers(serverplayer, kind);
         } else {
            RibbonAttributeManager.RibbonGroup ribbonattributemanager$ribbongroup = enummap.get(kind);
            if (ribbonattributemanager$ribbongroup == null) {
               removeModifiers(serverplayer, kind);
            } else {
               ribbonattributemanager$ribbongroup.sessions.remove(RibbonAttributeManager.SlotKey.from(context));
               if (ribbonattributemanager$ribbongroup.sessions.isEmpty()) {
                  enummap.remove(kind);
                  ribbonattributemanager$ribbongroup.close(serverplayer);
               } else {
                  apply(serverplayer, ribbonattributemanager$ribbongroup, true);
               }

               if (enummap.isEmpty()) {
                  SESSIONS.remove(serverplayer.getUUID());
               }
            }
         }
      }
   }

   public static void handlePlayerLogout(ServerPlayer player) {
      EnumMap<RibbonAttributeManager.RibbonKind, RibbonAttributeManager.RibbonGroup> enummap = SESSIONS.remove(player.getUUID());
      if (enummap != null) {
         for (RibbonAttributeManager.RibbonGroup ribbonattributemanager$ribbongroup : enummap.values()) {
            ribbonattributemanager$ribbongroup.close(player);
         }
      }
   }

   public static List<RibbonAttributeManager.TooltipTypeBonus> calculateTooltipBonuses(Player player, RibbonAttributeManager.RibbonKind kind, double multiplier) {
      if (player != null && player.level() != null) {
         Map<String, RibbonAttributeManager.TooltipTypeBonus> map = new LinkedHashMap<>();

         try {
            PlayerPartyStore playerpartystore = getParty(player.getUUID(), player.level().registryAccess());
            if (kind == RibbonAttributeManager.RibbonKind.TRAINER) {
               addPokemonTooltip(map, playerpartystore.get(0), multiplier);
            } else {
               for (int i = 0; i < playerpartystore.size(); i++) {
                  addPokemonTooltip(map, playerpartystore.get(i), multiplier);
               }
            }
         } catch (Exception exception) {
            CobbleBash.LOGGER.warn("Failed to calculate Champion/Trainer Ribbon tooltip bonuses.", exception);
         }

         return new ArrayList<>(map.values());
      } else {
         return List.of();
      }
   }

   private static Object subscribe(ServerPlayer player, RibbonAttributeManager.RibbonKind kind) {
      try {
         PlayerPartyStore playerpartystore = getParty(player);
         Object object = playerpartystore.getClass().getMethod("getAnyChangeObservable").invoke(playerpartystore);
         return object.getClass().getMethod("subscribe", Consumer.class).invoke(object, (Consumer<Object>)ignored -> {
            RibbonAttributeManager.RibbonGroup ribbonattributemanager$ribbongroup = getGroup(player, kind);
            if (ribbonattributemanager$ribbongroup != null) {
               apply(player, ribbonattributemanager$ribbongroup, false);
            }
         });
      } catch (ReflectiveOperationException | RuntimeException exception) {
         CobbleBash.LOGGER.warn("Failed to subscribe Champion/Trainer Ribbon to Cobblemon party changes.", exception);
         return null;
      }
   }

   private static RibbonAttributeManager.RibbonGroup getGroup(ServerPlayer player, RibbonAttributeManager.RibbonKind kind) {
      EnumMap<RibbonAttributeManager.RibbonKind, RibbonAttributeManager.RibbonGroup> enummap = SESSIONS.get(player.getUUID());
      return enummap == null ? null : enummap.get(kind);
   }

   private static void apply(ServerPlayer player, RibbonAttributeManager.RibbonGroup group, boolean force) {
      RibbonAttributeManager.RibbonModifiers ribbonattributemanager$ribbonmodifiers = calculateAggregateModifiers(player, group);
      if (force || !ribbonattributemanager$ribbonmodifiers.signature().equals(group.lastSignature)) {
         removeModifiers(player, group.kind);
         ribbonattributemanager$ribbonmodifiers.values().forEach((key, amount) -> addModifier(player, group.kind, key, amount));
         group.lastSignature = ribbonattributemanager$ribbonmodifiers.signature();
         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }

   private static RibbonAttributeManager.RibbonModifiers calculateAggregateModifiers(ServerPlayer player, RibbonAttributeManager.RibbonGroup group) {
      EnumMap<RibbonAttributeManager.AttributeKey, Double> enummap = new EnumMap<>(RibbonAttributeManager.AttributeKey.class);
      StringBuilder stringbuilder = new StringBuilder(group.kind.name());

      try {
         PlayerPartyStore playerpartystore = getParty(player);
         List<RibbonAttributeManager.RibbonSession> list = new ArrayList<>(group.sessions.values());
         list.sort(Comparator.comparing(session -> session.slotKey));

         for (int i = 0; i < list.size(); i++) {
            double d0 = Math.pow(0.5, i);
            stringbuilder.append("|copy").append(i).append('@').append(list.get(i).slotKey).append('=').append(d0);
            if (group.kind == RibbonAttributeManager.RibbonKind.TRAINER) {
               addPokemon(enummap, stringbuilder, playerpartystore.get(0), d0);
            } else {
               for (int j = 0; j < playerpartystore.size(); j++) {
                  addPokemon(enummap, stringbuilder, playerpartystore.get(j), d0);
               }
            }
         }
      } catch (Exception exception) {
         CobbleBash.LOGGER.warn("Failed to calculate Champion/Trainer Ribbon modifiers.", exception);
      }

      return new RibbonAttributeManager.RibbonModifiers(enummap, stringbuilder.toString());
   }

   private static PlayerPartyStore getParty(ServerPlayer player) {
      return Cobblemon.INSTANCE.getStorage().getParty(player);
   }

   private static PlayerPartyStore getParty(UUID playerId, RegistryAccess registryAccess) {
      return Cobblemon.INSTANCE.getStorage().getParty(playerId, registryAccess);
   }

   private static void addPokemon(EnumMap<RibbonAttributeManager.AttributeKey, Double> values, StringBuilder signature, Pokemon pokemon, double multiplier) {
      if (pokemon == null) {
         signature.append("|empty");
      } else {
         Map<String, ElementalType> map = new LinkedHashMap<>();

         for (ElementalType elementaltype : pokemon.getTypes()) {
            map.put(elementaltype.getName().toLowerCase(Locale.ROOT), elementaltype);
         }

         if (map.isEmpty()) {
            signature.append("|typeless");
         } else {
            double d1 = 1.0 / map.size();

            for (String s : map.keySet()) {
               double d0 = d1 * multiplier;
               signature.append('|').append(s).append(':').append(d0);
               addType(values, s, d0);
            }
         }
      }
   }

   private static void addPokemonTooltip(Map<String, RibbonAttributeManager.TooltipTypeBonus> bonuses, Pokemon pokemon, double multiplier) {
      if (pokemon != null) {
         Map<String, ElementalType> map = new LinkedHashMap<>();

         for (ElementalType elementaltype : pokemon.getTypes()) {
            map.put(elementaltype.getName().toLowerCase(Locale.ROOT), elementaltype);
         }

         if (!map.isEmpty()) {
            double d0 = 1.0 / map.size();

            for (Entry<String, ElementalType> entry : map.entrySet()) {
               EnumMap<RibbonAttributeManager.AttributeKey, Double> enummap = new EnumMap<>(RibbonAttributeManager.AttributeKey.class);
               addType(enummap, entry.getKey(), d0 * multiplier);
               RibbonAttributeManager.TooltipTypeBonus ribbonattributemanager$tooltiptypebonus = bonuses.computeIfAbsent(
                  entry.getKey(),
                  ignored -> new RibbonAttributeManager.TooltipTypeBonus(
                     entry.getKey(), entry.getValue().getTextureXMultiplier(), new EnumMap<>(RibbonAttributeManager.AttributeKey.class)
                  )
               );
               enummap.forEach((key, amount) -> ribbonattributemanager$tooltiptypebonus.attributes.merge(key, amount, Double::sum));
            }
         }
      }
   }

   private static void addType(EnumMap<RibbonAttributeManager.AttributeKey, Double> values, String typeName, double levels) {
      switch (typeName) {
         case "fighting":
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_DAMAGE, 0.5 * levels);
            break;
         case "steel":
            add(values, RibbonAttributeManager.AttributeKey.ARMOR, 1.0 * levels);
            break;
         case "rock":
            add(values, RibbonAttributeManager.AttributeKey.MINING_EFFICIENCY, 1.0 * levels);
            break;
         case "ground":
            add(values, RibbonAttributeManager.AttributeKey.MAX_HEALTH, 1.5 * levels);
            break;
         case "fairy":
            add(values, RibbonAttributeManager.AttributeKey.LUCK, 0.5 * levels);
            break;
         case "electric":
            add(values, RibbonAttributeManager.AttributeKey.MOVEMENT_SPEED, 0.005 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_SPEED, 0.05 * levels);
            break;
         case "flying":
            add(values, RibbonAttributeManager.AttributeKey.SAFE_FALL_DISTANCE, 1.0 * levels);
            add(values, RibbonAttributeManager.AttributeKey.FALL_DAMAGE_MULTIPLIER, -0.1 * levels);
            break;
         case "psychic":
            add(values, RibbonAttributeManager.AttributeKey.BLOCK_INTERACTION_RANGE, 0.5 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ENTITY_INTERACTION_RANGE, 0.25 * levels);
            break;
         case "water":
            add(values, RibbonAttributeManager.AttributeKey.WATER_MOVEMENT_EFFICIENCY, 0.15 * levels);
            add(values, RibbonAttributeManager.AttributeKey.OXYGEN_BONUS, 1.0 * levels);
            break;
         case "fire":
            add(values, RibbonAttributeManager.AttributeKey.BURNING_TIME, -0.1 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_DAMAGE, 0.25 * levels);
            break;
         case "dragon":
            add(values, RibbonAttributeManager.AttributeKey.EXPLOSION_KNOCKBACK_RESISTANCE, 0.1 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_DAMAGE, 0.25 * levels);
            break;
         case "normal":
            add(values, RibbonAttributeManager.AttributeKey.MAX_HEALTH, 0.5 * levels);
            add(values, RibbonAttributeManager.AttributeKey.MOVEMENT_SPEED, 0.002 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_DAMAGE, 0.15 * levels);
            break;
         case "grass":
            add(values, RibbonAttributeManager.AttributeKey.MOVEMENT_EFFICIENCY, 0.1 * levels);
            add(values, RibbonAttributeManager.AttributeKey.SWEEPING_DAMAGE_RATIO, 0.1 * levels);
            add(values, RibbonAttributeManager.AttributeKey.MOVEMENT_SPEED, 0.003 * levels);
            break;
         case "ghost":
            add(values, RibbonAttributeManager.AttributeKey.FOLLOW_RANGE, -2.0 * levels);
            add(values, RibbonAttributeManager.AttributeKey.MAX_HEALTH, 1.0 * levels);
            break;
         case "dark":
            add(values, RibbonAttributeManager.AttributeKey.SNEAKING_SPEED, 0.05 * levels);
            add(values, RibbonAttributeManager.AttributeKey.FOLLOW_RANGE, -2.0 * levels);
            break;
         case "bug":
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_SPEED, 0.2 * levels);
            break;
         case "ice":
            add(values, RibbonAttributeManager.AttributeKey.ARMOR_TOUGHNESS, 0.5 * levels);
            add(values, RibbonAttributeManager.AttributeKey.KNOCKBACK_RESISTANCE, 0.05 * levels);
            break;
         case "poison":
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_DAMAGE, 0.25 * levels);
            add(values, RibbonAttributeManager.AttributeKey.ATTACK_KNOCKBACK, 0.25 * levels);
      }
   }

   private static void add(EnumMap<RibbonAttributeManager.AttributeKey, Double> values, RibbonAttributeManager.AttributeKey key, double amount) {
      values.merge(key, amount, Double::sum);
   }

   private static void addModifier(ServerPlayer player, RibbonAttributeManager.RibbonKind kind, RibbonAttributeManager.AttributeKey key, double amount) {
      if (!(Math.abs(amount) < 1.0E-6)) {
         AttributeInstance attributeinstance = player.getAttribute(key.attribute());
         if (attributeinstance != null) {
            attributeinstance.addOrUpdateTransientModifier(new AttributeModifier(modifierId(kind, key), amount, Operation.ADD_VALUE));
         }
      }
   }

   private static void removeModifiers(ServerPlayer player, RibbonAttributeManager.RibbonKind kind) {
      for (RibbonAttributeManager.AttributeKey ribbonattributemanager$attributekey : RibbonAttributeManager.AttributeKey.values()) {
         AttributeInstance attributeinstance = player.getAttribute(ribbonattributemanager$attributekey.attribute());
         if (attributeinstance != null) {
            attributeinstance.removeModifier(modifierId(kind, ribbonattributemanager$attributekey));
         }
      }
   }

   private static ResourceLocation modifierId(RibbonAttributeManager.RibbonKind kind, RibbonAttributeManager.AttributeKey key) {
      return ResourceLocation.fromNamespaceAndPath("cobblebash", "ribbon/" + kind.name().toLowerCase(Locale.ROOT) + "/" + key.path);
   }

   public enum AttributeKey {
      ARMOR("armor", "armor", Attributes.ARMOR),
      ARMOR_TOUGHNESS("armor_toughness", "toughness", Attributes.ARMOR_TOUGHNESS),
      ATTACK_DAMAGE("attack_damage", "damage", Attributes.ATTACK_DAMAGE),
      ATTACK_KNOCKBACK("attack_knockback", "knockback", Attributes.ATTACK_KNOCKBACK),
      ATTACK_SPEED("attack_speed", "attack speed", Attributes.ATTACK_SPEED),
      BLOCK_INTERACTION_RANGE("block_interaction_range", "block reach", Attributes.BLOCK_INTERACTION_RANGE),
      BURNING_TIME("burning_time", "burn time", Attributes.BURNING_TIME),
      EXPLOSION_KNOCKBACK_RESISTANCE("explosion_knockback_resistance", "explosion resist", Attributes.EXPLOSION_KNOCKBACK_RESISTANCE),
      ENTITY_INTERACTION_RANGE("entity_interaction_range", "entity reach", Attributes.ENTITY_INTERACTION_RANGE),
      FALL_DAMAGE_MULTIPLIER("fall_damage_multiplier", "fall damage", Attributes.FALL_DAMAGE_MULTIPLIER),
      FOLLOW_RANGE("follow_range", "mob vision", Attributes.FOLLOW_RANGE),
      KNOCKBACK_RESISTANCE("knockback_resistance", "kb resist", Attributes.KNOCKBACK_RESISTANCE),
      LUCK("luck", "luck", Attributes.LUCK),
      MAX_HEALTH("max_health", "health", Attributes.MAX_HEALTH),
      MINING_EFFICIENCY("mining_efficiency", "mining", Attributes.MINING_EFFICIENCY),
      MOVEMENT_EFFICIENCY("movement_efficiency", "move eff", Attributes.MOVEMENT_EFFICIENCY),
      MOVEMENT_SPEED("movement_speed", "speed", Attributes.MOVEMENT_SPEED),
      OXYGEN_BONUS("oxygen_bonus", "oxygen", Attributes.OXYGEN_BONUS),
      SAFE_FALL_DISTANCE("safe_fall_distance", "safe fall", Attributes.SAFE_FALL_DISTANCE),
      SNEAKING_SPEED("sneaking_speed", "sneak speed", Attributes.SNEAKING_SPEED),
      SWEEPING_DAMAGE_RATIO("sweeping_damage_ratio", "sweep", Attributes.SWEEPING_DAMAGE_RATIO),
      WATER_MOVEMENT_EFFICIENCY("water_movement_efficiency", "water move", Attributes.WATER_MOVEMENT_EFFICIENCY);

      private final String path;
      private final String displayName;
      private final Holder<Attribute> attribute;

      AttributeKey(String path, String displayName, Holder<Attribute> attribute) {
         this.path = path;
         this.displayName = displayName;
         this.attribute = attribute;
      }

      public String displayName() {
         return this.displayName;
      }

      private Holder<Attribute> attribute() {
         return this.attribute;
      }
   }

   private static final class RibbonGroup {
      private final RibbonAttributeManager.RibbonKind kind;
      private final Object subscription;
      private final Map<RibbonAttributeManager.SlotKey, RibbonAttributeManager.RibbonSession> sessions = new LinkedHashMap<>();
      private String lastSignature = "";

      private RibbonGroup(RibbonAttributeManager.RibbonKind kind, Object subscription) {
         this.kind = kind;
         this.subscription = subscription;
      }

      private void close(ServerPlayer player) {
         if (this.subscription != null) {
            try {
               this.subscription.getClass().getMethod("unsubscribe").invoke(this.subscription);
            } catch (ReflectiveOperationException reflectiveoperationexception) {
               CobbleBash.LOGGER.warn("Failed to unsubscribe Champion/Trainer Ribbon from Cobblemon party changes.", reflectiveoperationexception);
            }
         }

         RibbonAttributeManager.removeModifiers(player, this.kind);
      }
   }

   public enum RibbonKind {
      TRAINER,
      CHAMPION;
   }

   private record RibbonModifiers(EnumMap<RibbonAttributeManager.AttributeKey, Double> values, String signature) {
   }

   private record RibbonSession(RibbonAttributeManager.SlotKey slotKey) {
   }

   private record SlotKey(String identifier, int index, boolean cosmetic) implements Comparable<RibbonAttributeManager.SlotKey> {
      private static RibbonAttributeManager.SlotKey from(RibbonSlot context) {
         return new RibbonAttributeManager.SlotKey(context.identifier(), context.index(), context.cosmetic());
      }

      public int compareTo(RibbonAttributeManager.SlotKey other) {
         int i = this.identifier.compareTo(other.identifier);
         if (i != 0) {
            return i;
         }

         int j = Integer.compare(this.index, other.index);
         return j != 0 ? j : Boolean.compare(this.cosmetic, other.cosmetic);
      }
   }

   public record TooltipTypeBonus(String typeName, int textureIndex, EnumMap<RibbonAttributeManager.AttributeKey, Double> attributes) {
      public TooltipTypeBonus {
         Objects.requireNonNull(typeName);
         Objects.requireNonNull(attributes);
      }
   }
}
