package com.nore.cobblebash.beacon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.PlayerSpawnerAccessor;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.block.ApricornBlock;
import com.cobblemon.mod.common.block.ApricornSaplingBlock;
import com.cobblemon.mod.common.block.BerryBlock;
import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity.Tethering;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChampionBeaconAuras {
   public static final double SHINY_EXTRA_CHANCE = 1.8311664530305805E-4;
   public static final float LURE_EXTRA_TIMER_PROGRESS_PER_TICK = 0.5F;
   private static final long CROP_GROWTH_INTERVAL_TICKS = 2400L;
   private static final long CROP_GROWTH_INTERVAL_TICKS_BASE = 4800L;
   private static final long DAYCARE_INTERVAL_TICKS = 1200L;
   private static final long EV_INTERVAL_TICKS = 4800L;
   private static final long PASTURE_DISCOVERY_INTERVAL_TICKS = 12000L;
   private static final int CROP_DISCOVERY_SCAN_INTERVAL_PULSES = 5;
   private static final int CROP_DISCOVERY_CHUNKS_PER_SCAN = 8;
   private static final int CROP_DISCOVERY_VERTICAL_RANGE = 16;
   private static final int DAYCARE_XP_PER_MINUTE = 60;
   private static final int DAYCARE_XP_PER_MINUTE_UPGRADED = 120;
   private static final int EV_AMOUNT = 1;
   private static final int EV_AMOUNT_UPGRADED = 2;
   private static final SidemodExperienceSource DAYCARE_EXPERIENCE_SOURCE = new SidemodExperienceSource("cobblebash");
   private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKED_BEACONS = new ConcurrentHashMap<>();
   private static final Map<ChampionBeaconAuras.BeaconKey, ChampionBeaconAuras.AuraCache> AURA_CACHES = new ConcurrentHashMap<>();
   private static final Set<UUID> DEBUG_PULSE_PLAYERS = ConcurrentHashMap.newKeySet();

   private ChampionBeaconAuras() {
   }

   public static void track(ChampionBeaconBlockEntity beacon) {
      if (beacon.getLevel() instanceof ServerLevel serverlevel) {
         Set positions = TRACKED_BEACONS.computeIfAbsent(serverlevel.dimension(), ignored -> ConcurrentHashMap.newKeySet());
         if (isPotentiallyActive(beacon)) {
            positions.add(beacon.getBlockPos().immutable());
            if (hasCacheableAura(beacon)) {
               prepareAuraCache(serverlevel, beacon);
            }
         } else {
            positions.remove(beacon.getBlockPos());
            AURA_CACHES.remove(new ChampionBeaconAuras.BeaconKey(serverlevel.dimension(), beacon.getBlockPos().immutable()));
         }
      }
   }

   public static void untrack(Level level, BlockPos pos) {
      if (level != null && !level.isClientSide) {
         Set<BlockPos> set = TRACKED_BEACONS.get(level.dimension());
         if (set != null) {
            set.remove(pos);
         }

         AURA_CACHES.remove(new ChampionBeaconAuras.BeaconKey(level.dimension(), pos.immutable()));
      }
   }

   public static void tickBeacon(ChampionBeaconBlockEntity beacon) {
      if (beacon.getLevel() instanceof ServerLevel serverlevel) {
         if (!isPotentiallyActive(beacon)) {
            AURA_CACHES.remove(new ChampionBeaconAuras.BeaconKey(serverlevel.dimension(), beacon.getBlockPos().immutable()));
         } else {
            if (hasPastureAura(beacon) && isBeaconPulse(serverlevel, beacon.getBlockPos(), 12000L)) {
               discoverAllLoadedPastures(serverlevel, beacon, prepareAuraCache(serverlevel, beacon));
            }

            if (hasCropAura(beacon) && isBeaconPulse(serverlevel, beacon.getBlockPos(), getCropGrowthInterval(beacon))) {
               ChampionBeaconAuras.AuraCache championbeaconauras$auracache = prepareAuraCache(serverlevel, beacon);
               pulseCrops(serverlevel, beacon, championbeaconauras$auracache);
            }

            if (hasPower(beacon, ChampionBeaconPower.DAYCARE) && isBeaconPulse(serverlevel, beacon.getBlockPos(), 1200L)) {
               pulseDaycare(serverlevel, beacon);
            }

            if (hasPower(beacon, ChampionBeaconPower.EV) && isBeaconPulse(serverlevel, beacon.getBlockPos(), 4800L)) {
               pulseEvAura(serverlevel, beacon);
            }
         }
      }
   }

   public static void handleBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
      if (isCacheRelevantBlock(oldState) || isCacheRelevantBlock(newState)) {
         Set<BlockPos> set = TRACKED_BEACONS.get(level.dimension());
         if (set != null && !set.isEmpty()) {
            for (BlockPos blockpos : set) {
               ChampionBeaconBlockEntity championbeaconblockentity = getActiveBeacon(level, blockpos);
               if (championbeaconblockentity != null
                  && isInHorizontalRange(blockpos, pos, championbeaconblockentity.getRadius())
                  && hasCacheableAura(championbeaconblockentity)) {
                  updateCacheForBlock(prepareAuraCache(level, championbeaconblockentity), pos, oldState, newState);
               }
            }
         }
      }
   }

   public static boolean shouldRepel(ServerLevel level, BlockPos pos) {
      return hasAura(level, pos, ChampionBeaconPower.REPEL);
   }

   public static boolean tryApplyShinyAura(PokemonEntity pokemon, ServerLevel level, BlockPos pos) {
      if (pokemon.getPokemon().getShiny()) {
         return false;
      }

      if (!hasAura(level, pos, ChampionBeaconPower.SHINY)) {
         return false;
      }

      if (level.random.nextDouble() >= 1.8311664530305805E-4) {
         return false;
      }

      pokemon.getPokemon().setShiny(true);
      pokemon.getPokemon().updateAspects();
      return true;
   }

   public static void tickPlayer(ServerPlayer player) {
      if (hasAura(player.serverLevel(), player.blockPosition(), ChampionBeaconPower.LURE)) {
         if (player instanceof PlayerSpawnerAccessor playerspawneraccessor) {
            PlayerSpawner playerspawner = playerspawneraccessor.getPlayerSpawner();
            if (playerspawner != null && playerspawner.getActive()) {
               float f = playerspawner.getTicksUntilNextSpawn();
               if (f > 1.0F) {
                  playerspawner.setTicksUntilNextSpawn(Math.max(1.0F, f - 0.5F));
               }
            }
         }
      }
   }

   public static ChampionBeaconAuras.DebugInfo debugAt(ServerLevel level, BlockPos pos) {
      EnumSet<ChampionBeaconPower> enumset = EnumSet.noneOf(ChampionBeaconPower.class);
      int i = 0;
      Set<BlockPos> set = TRACKED_BEACONS.get(level.dimension());
      if (set == null) {
         return new ChampionBeaconAuras.DebugInfo(0, enumset);
      }

      for (BlockPos blockpos : set) {
         ChampionBeaconBlockEntity championbeaconblockentity = getActiveBeacon(level, blockpos);
         if (championbeaconblockentity != null && isInHorizontalRange(blockpos, pos, championbeaconblockentity.getRadius())) {
            i++;
            addPower(enumset, championbeaconblockentity.getPrimaryPower());
            addPower(enumset, championbeaconblockentity.getSecondaryPower());
         }
      }

      return new ChampionBeaconAuras.DebugInfo(i, enumset);
   }

   public static ChampionBeaconAuras.PlayerDebugInfo debugForPlayer(ServerPlayer player) {
      ChampionBeaconAuras.DebugInfo championbeaconauras$debuginfo = debugAt(player.serverLevel(), player.blockPosition());
      boolean flag = championbeaconauras$debuginfo.powers().contains(ChampionBeaconPower.LURE);
      PlayerSpawner playerspawner = player instanceof PlayerSpawnerAccessor playerspawneraccessor ? playerspawneraccessor.getPlayerSpawner() : null;
      if (playerspawner == null) {
         return new ChampionBeaconAuras.PlayerDebugInfo(
            championbeaconauras$debuginfo,
            false,
            flag,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            getCobblemonShinyRate(),
            championbeaconauras$debuginfo.powers().contains(ChampionBeaconPower.SHINY) ? 1.8311664530305805E-4 : 0.0
         );
      }

      float f4 = playerspawner.getTickTimerMultiplier();
      float f = flag ? 0.5F : 0.0F;
      float f1 = f4 + f;
      float f2 = playerspawner.getTicksBetweenSpawns();
      float f3 = f1 <= 0.0F ? Float.POSITIVE_INFINITY : f2 / f1;
      return new ChampionBeaconAuras.PlayerDebugInfo(
         championbeaconauras$debuginfo,
         playerspawner.getActive(),
         flag,
         playerspawner.getTicksUntilNextSpawn(),
         f2,
         f4,
         f,
         f3,
         getCobblemonShinyRate(),
         championbeaconauras$debuginfo.powers().contains(ChampionBeaconPower.SHINY) ? 1.8311664530305805E-4 : 0.0
      );
   }

   public static boolean togglePulseDebug(ServerPlayer player) {
      UUID uuid = player.getUUID();
      if (DEBUG_PULSE_PLAYERS.remove(uuid)) {
         return false;
      }

      DEBUG_PULSE_PLAYERS.add(uuid);
      return true;
   }

   public static ChampionBeaconAuras.VisualizationInfo visualizeForPlayer(ServerPlayer player) {
      ServerLevel serverlevel = player.serverLevel();
      Set<BlockPos> set = TRACKED_BEACONS.get(serverlevel.dimension());
      if (set != null && !set.isEmpty()) {
         int i = 0;
         int j = 0;
         int k = 0;
         int l = 0;

         for (BlockPos blockpos : set) {
            ChampionBeaconBlockEntity championbeaconblockentity = getActiveBeacon(serverlevel, blockpos);
            if (championbeaconblockentity != null && isInHorizontalRange(blockpos, player.blockPosition(), championbeaconblockentity.getRadius())) {
               i++;
               if (hasCropAura(championbeaconblockentity)) {
                  ChampionBeaconAuras.AuraCache championbeaconauras$auracache = prepareAuraCache(serverlevel, championbeaconblockentity);
                  discoverAllLoadedCrops(serverlevel, championbeaconblockentity, championbeaconauras$auracache);
                  j += visualizeCachedPositions(serverlevel, championbeaconauras$auracache.apricornPositions, ChampionBeaconAuras::isApricornCropBlock);
                  k += visualizeCachedPositions(serverlevel, championbeaconauras$auracache.berryPositions, ChampionBeaconAuras::isBerryCropBlock);
               }

               ChampionBeaconAuras.PastureVisualizationCounter championbeaconauras$pasturevisualizationcounter = new ChampionBeaconAuras.PastureVisualizationCounter();
               forEachLoadedBlockEntityInHorizontalRange(
                  serverlevel, championbeaconblockentity.getBlockPos(), championbeaconblockentity.getRadius(), blockEntity -> {
                     if (blockEntity instanceof PokemonPastureBlockEntity) {
                        championbeaconauras$pasturevisualizationcounter.count++;
                        emitDebugParticle(serverlevel, blockEntity.getBlockPos());
                     }
                  }
               );
               l += championbeaconauras$pasturevisualizationcounter.count;
            }
         }

         return new ChampionBeaconAuras.VisualizationInfo(i, j, k, l);
      } else {
         return new ChampionBeaconAuras.VisualizationInfo(0, 0, 0, 0);
      }
   }

   private static boolean hasAura(ServerLevel level, BlockPos pos, ChampionBeaconPower power) {
      Set<BlockPos> set = TRACKED_BEACONS.get(level.dimension());
      if (set != null && !set.isEmpty()) {
         for (BlockPos blockpos : set) {
            ChampionBeaconBlockEntity championbeaconblockentity = getActiveBeacon(level, blockpos);
            if (championbeaconblockentity == null) {
               set.remove(blockpos);
            } else if (hasPower(championbeaconblockentity, power) && isInHorizontalRange(blockpos, pos, championbeaconblockentity.getRadius())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static ChampionBeaconAuras.AuraCache prepareAuraCache(ServerLevel level, ChampionBeaconBlockEntity beacon) {
      ChampionBeaconAuras.BeaconKey championbeaconauras$beaconkey = new ChampionBeaconAuras.BeaconKey(level.dimension(), beacon.getBlockPos().immutable());
      ChampionBeaconAuras.AuraCache championbeaconauras$auracache = AURA_CACHES.computeIfAbsent(
         championbeaconauras$beaconkey, ignored -> new ChampionBeaconAuras.AuraCache()
      );
      if (championbeaconauras$auracache.needsRefresh(beacon)) {
         championbeaconauras$auracache.clear();
         championbeaconauras$auracache.capture(beacon);
         if (hasCropAura(beacon)) {
            discoverAllLoadedCrops(level, beacon, championbeaconauras$auracache);
         }

         if (hasPastureAura(beacon)) {
            discoverAllLoadedPastures(level, beacon, championbeaconauras$auracache);
         }
      }

      return championbeaconauras$auracache;
   }

   private static void pulseCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache) {
      int i = 1;
      ChampionBeaconAuras.CropPulseStats championbeaconauras$croppulsestats = ChampionBeaconAuras.CropPulseStats.empty();
      ChampionBeaconAuras.CropPulseStats championbeaconauras$croppulsestats1 = ChampionBeaconAuras.CropPulseStats.empty();
      if (hasPower(beacon, ChampionBeaconPower.APRICORN)) {
         championbeaconauras$croppulsestats = growCachedCrops(level, cache.apricornPositions, ChampionBeaconAuras::isApricornCropBlock, i);
      }

      if (hasPower(beacon, ChampionBeaconPower.BERRY)) {
         championbeaconauras$croppulsestats1 = growCachedCrops(level, cache.berryPositions, ChampionBeaconAuras::isBerryCropBlock, i);
      }

      cache.cropPulseCount++;
      if (cache.cropPulseCount % 5 == 0) {
         discoverNextLoadedCropChunks(level, beacon, cache);
      }

      sendPulseDebug(
         level,
         beacon,
         Component.literal(
            "Champion Beacon crop pulse at "
               + formatPos(beacon.getBlockPos())
               + ": attempts/crop="
               + i
               + ", apricorns "
               + championbeaconauras$croppulsestats.describe()
               + ", berries "
               + championbeaconauras$croppulsestats1.describe()
         )
      );
   }

   private static ChampionBeaconAuras.CropPulseStats growCachedCrops(
      ServerLevel level, Set<BlockPos> positions, ChampionBeaconAuras.CropMatcher cropMatcher, int attempts
   ) {
      ChampionBeaconAuras.CropPulseStats championbeaconauras$croppulsestats = new ChampionBeaconAuras.CropPulseStats(positions.size());
      Iterator<BlockPos> iterator = positions.iterator();

      while (iterator.hasNext()) {
         BlockPos blockpos = iterator.next();
         if (shouldRemoveCachedCrop(level, blockpos, cropMatcher, attempts, championbeaconauras$croppulsestats)) {
            iterator.remove();
         }
      }

      return championbeaconauras$croppulsestats;
   }

   private static boolean shouldRemoveCachedCrop(
      ServerLevel level, BlockPos pos, ChampionBeaconAuras.CropMatcher cropMatcher, int attempts, ChampionBeaconAuras.CropPulseStats stats
   ) {
      BlockState blockstate = level.getBlockState(pos);
      if (cropMatcher.matches(blockstate) && blockstate.getBlock() instanceof BonemealableBlock bonemealableblock) {
         for (int i = 0; i < attempts; i++) {
            if (!bonemealableblock.isValidBonemealTarget(level, pos, blockstate)) {
               stats.matureOrSkipped++;
               return false;
            }

            if (bonemealableblock.isBonemealSuccess(level, level.random, pos, blockstate)) {
               bonemealableblock.performBonemeal(level, level.random, pos, blockstate);
               stats.growthAttemptsPerformed++;
            }

            blockstate = level.getBlockState(pos);
            if (!(cropMatcher.matches(blockstate) && blockstate.getBlock() instanceof BonemealableBlock bonemealableblock1)) {
               stats.removedInvalid++;
               return true;
            }

            bonemealableblock = bonemealableblock1;
         }

         return false;
      } else {
         stats.removedInvalid++;
         return true;
      }
   }

   private static void discoverAllLoadedCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache) {
      BlockPos blockpos = beacon.getBlockPos();
      int i = beacon.getRadius();
      int j = Math.floorDiv(blockpos.getX() - i, 16);
      int k = Math.floorDiv(blockpos.getX() + i, 16);
      int l = Math.floorDiv(blockpos.getZ() - i, 16);
      int i1 = Math.floorDiv(blockpos.getZ() + i, 16);

      for (int j1 = j; j1 <= k; j1++) {
         for (int k1 = l; k1 <= i1; k1++) {
            scanLoadedChunkForCrops(level, beacon, cache, j1, k1);
         }
      }
   }

   private static void discoverNextLoadedCropChunks(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache) {
      BlockPos blockpos = beacon.getBlockPos();
      int i = Math.max(1, Math.ceilDiv(beacon.getRadius(), 16));
      int j = i * 2 + 1;
      int k = j * j;
      int l = Math.floorDiv(blockpos.getX(), 16);
      int i1 = Math.floorDiv(blockpos.getZ(), 16);

      for (int j1 = 0; j1 < 8; j1++) {
         int k1 = Math.floorMod(cache.discoveryCursor++, k);
         int l1 = k1 % j - i;
         int i2 = k1 / j - i;
         scanLoadedChunkForCrops(level, beacon, cache, l + l1, i1 + i2);
      }
   }

   private static void scanLoadedChunkForCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache, int chunkX, int chunkZ) {
      ServerChunkCache serverchunkcache = level.getChunkSource();
      LevelChunk levelchunk = serverchunkcache.getChunkNow(chunkX, chunkZ);
      if (levelchunk != null) {
         BlockPos blockpos = beacon.getBlockPos();
         int i = beacon.getRadius();
         int j = Math.max(chunkX << 4, blockpos.getX() - i);
         int k = Math.min((chunkX << 4) + 15, blockpos.getX() + i);
         int l = Math.max(chunkZ << 4, blockpos.getZ() - i);
         int i1 = Math.min((chunkZ << 4) + 15, blockpos.getZ() + i);
         int j1 = Math.max(level.getMinBuildHeight(), blockpos.getY() - 16);
         int k1 = Math.min(level.getMaxBuildHeight() - 1, blockpos.getY() + 16);
         MutableBlockPos mutableblockpos = new MutableBlockPos();

         for (int l1 = j; l1 <= k; l1++) {
            for (int i2 = l; i2 <= i1; i2++) {
               mutableblockpos.set(l1, blockpos.getY(), i2);
               if (isInHorizontalRange(blockpos, mutableblockpos, i)) {
                  for (int j2 = j1; j2 <= k1; j2++) {
                     mutableblockpos.set(l1, j2, i2);
                     cacheCropPosition(cache, level.getBlockState(mutableblockpos), mutableblockpos);
                  }
               }
            }
         }
      }
   }

   private static void cacheCropPosition(ChampionBeaconAuras.AuraCache cache, BlockState state, BlockPos pos) {
      if (isApricornCropBlock(state)) {
         cache.apricornPositions.add(pos.immutable());
      } else if (isBerryCropBlock(state)) {
         cache.berryPositions.add(pos.immutable());
      }
   }

   private static void updateCacheForBlock(ChampionBeaconAuras.AuraCache cache, BlockPos pos, BlockState oldState, BlockState newState) {
      if (isApricornCropBlock(oldState)) {
         cache.apricornPositions.remove(pos);
      }

      if (isBerryCropBlock(oldState)) {
         cache.berryPositions.remove(pos);
      }

      if (isPastureBlock(oldState)) {
         cache.pasturePositions.remove(pos);
      }

      if (isApricornCropBlock(newState)) {
         cache.apricornPositions.add(pos.immutable());
      }

      if (isBerryCropBlock(newState)) {
         cache.berryPositions.add(pos.immutable());
      }

      if (isPastureBlock(newState)) {
         cache.pasturePositions.add(pos.immutable());
      }
   }

   private static boolean isCacheRelevantBlock(BlockState state) {
      return isApricornCropBlock(state) || isBerryCropBlock(state) || isPastureBlock(state);
   }

   private static boolean isApricornCropBlock(BlockState state) {
      return state.getBlock() instanceof ApricornBlock || state.getBlock() instanceof ApricornSaplingBlock;
   }

   private static boolean isBerryCropBlock(BlockState state) {
      return state.getBlock() instanceof BerryBlock;
   }

   private static boolean isPastureBlock(BlockState state) {
      return state.getBlock() instanceof PastureBlock;
   }

   private static int visualizeCachedPositions(ServerLevel level, Set<BlockPos> positions, ChampionBeaconAuras.CropMatcher cropMatcher) {
      int i = 0;

      for (BlockPos blockpos : positions) {
         if (cropMatcher.matches(level.getBlockState(blockpos))) {
            i++;
            emitDebugParticle(level, blockpos);
         }
      }

      return i;
   }

   private static void emitDebugParticle(ServerLevel level, BlockPos pos) {
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.0);
   }

   private static void pulseDaycare(ServerLevel level, ChampionBeaconBlockEntity beacon) {
      int i = isPrimaryUpgraded(beacon, ChampionBeaconPower.DAYCARE) ? 120 : 60;
      LinkedHashMap<UUID, Pokemon> linkedhashmap = collectPasturePokemon(level, beacon, prepareAuraCache(level, beacon));
      linkedhashmap.entrySet().removeIf(entry -> !entry.getValue().canLevelUpFurther());
      if (linkedhashmap.isEmpty()) {
         sendPulseDebug(
            level, beacon, Component.literal("Champion Beacon daycare pulse at " + formatPos(beacon.getBlockPos()) + ": no eligible pasture Pokemon found.")
         );
      } else {
         int j = i / linkedhashmap.size();
         int k = i % linkedhashmap.size();
         int l = 0;
         List<String> list = new ArrayList<>();

         for (Pokemon pokemon : linkedhashmap.values()) {
            int i1 = j + (l < k ? 1 : 0);
            l++;
            if (i1 > 0) {
               int j1 = pokemon.getLevel();
               int k1 = pokemon.getExperience();
               pokemon.addExperience(DAYCARE_EXPERIENCE_SOURCE, i1);
               list.add(
                  pokemon.getDisplayName(false).getString()
                     + " "
                     + pokemon.getUuid()
                     + " +"
                     + i1
                     + " XP "
                     + k1
                     + "->"
                     + pokemon.getExperience()
                     + ", Lv "
                     + j1
                     + "->"
                     + pokemon.getLevel()
               );
            }
         }

         sendPulseDebug(
            level,
            beacon,
            Component.literal(
               "Champion Beacon daycare pulse at "
                  + formatPos(beacon.getBlockPos())
                  + ": pool="
                  + i
                  + ", pokemon="
                  + linkedhashmap.size()
                  + ", "
                  + String.join("; ", list)
            )
         );
      }
   }

   private static void pulseEvAura(ServerLevel level, ChampionBeaconBlockEntity beacon) {
      Stat stat = getEvStat(beacon.getPaymentItem());
      int i = isPrimaryUpgraded(beacon, ChampionBeaconPower.EV) ? 2 : 1;
      LinkedHashMap<UUID, Pokemon> linkedhashmap = collectPasturePokemon(level, beacon, prepareAuraCache(level, beacon));
      if (linkedhashmap.isEmpty()) {
         sendPulseDebug(
            level,
            beacon,
            Component.literal(
               "Champion Beacon EV pulse at "
                  + formatPos(beacon.getBlockPos())
                  + ": no eligible pasture Pokemon found. Stat="
                  + stat.getDisplayName().getString()
            )
         );
      } else {
         List<Pokemon> list = new ArrayList<>(linkedhashmap.values());
         Pokemon pokemon = list.get(level.random.nextInt(list.size()));
         List<String> list1 = new ArrayList<>();
         int j = getEvValue(pokemon, stat);
         int k = getEvTotal(pokemon);
         addEv(pokemon, stat, i);
         int l = getEvValue(pokemon, stat);
         int i1 = getEvTotal(pokemon);
         int j1 = Math.max(0, l - j);
         list1.add(
            pokemon.getDisplayName(false).getString()
               + " "
               + pokemon.getUuid()
               + " +"
               + j1
               + " "
               + stat.getDisplayName().getString()
               + " EV "
               + j
               + "->"
               + l
               + ", total "
               + k
               + "->"
               + i1
         );
         sendPulseDebug(
            level,
            beacon,
            Component.literal(
               "Champion Beacon EV pulse at "
                  + formatPos(beacon.getBlockPos())
                  + ": stat="
                  + stat.getDisplayName().getString()
                  + ", amount="
                  + i
                  + ", candidates="
                  + linkedhashmap.size()
                  + ", selected=1, "
                  + String.join("; ", list1)
            )
         );
      }
   }

   private static LinkedHashMap<UUID, Pokemon> collectPasturePokemon(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache) {
      LinkedHashMap<UUID, Pokemon> linkedhashmap = new LinkedHashMap<>();
      Iterator<BlockPos> iterator = cache.pasturePositions.iterator();

      while (iterator.hasNext()) {
         BlockPos blockpos = iterator.next();
         if (!isInHorizontalRange(beacon.getBlockPos(), blockpos, beacon.getRadius())) {
            iterator.remove();
         } else if (level.getBlockEntity(blockpos) instanceof PokemonPastureBlockEntity pokemonpastureblockentity) {
            for (Tethering tethering : pokemonpastureblockentity.getTetheredPokemon()) {
               Pokemon pokemon = tethering.getPokemon();
               if (pokemon != null) {
                  linkedhashmap.putIfAbsent(pokemon.getUuid(), pokemon);
               }
            }
         } else if (!isPastureBlock(level.getBlockState(blockpos))) {
            iterator.remove();
         }
      }

      return linkedhashmap;
   }

   private static void discoverAllLoadedPastures(ServerLevel level, ChampionBeaconBlockEntity beacon, ChampionBeaconAuras.AuraCache cache) {
      forEachLoadedBlockEntityInHorizontalRange(level, beacon.getBlockPos(), beacon.getRadius(), blockEntity -> {
         if (blockEntity instanceof PokemonPastureBlockEntity) {
            cache.pasturePositions.add(blockEntity.getBlockPos().immutable());
         }
      });
   }

   private static int getEvValue(Pokemon pokemon, Stat stat) {
      try {
         Object object = getEvs(pokemon);
         Method method = object.getClass().getMethod("getOrDefault", Stat.class);
         return (Integer)method.invoke(object, stat);
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         return 0;
      }
   }

   private static int getEvTotal(Pokemon pokemon) {
      try {
         Object object = getEvs(pokemon);
         Method method = object.getClass().getMethod("total");
         return (Integer)method.invoke(object);
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         return 0;
      }
   }

   private static int addEv(Pokemon pokemon, Stat stat, int amount) {
      try {
         Object object = getEvs(pokemon);
         Class<?> oclass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.EvSource");
         Method method = object.getClass().getMethod("add", Stat.class, int.class, oclass);
         return (Integer)method.invoke(object, stat, amount, new SidemodEvSource("cobblebash", pokemon));
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         return 0;
      }
   }

   private static Object getEvs(Pokemon pokemon) throws ReflectiveOperationException {
      Method method = Pokemon.class.getMethod("getEvs");
      return method.invoke(pokemon);
   }

   private static void forEachLoadedBlockEntityInHorizontalRange(ServerLevel level, BlockPos center, int radius, Consumer<BlockEntity> consumer) {
      int i = Math.floorDiv(center.getX() - radius, 16);
      int j = Math.floorDiv(center.getX() + radius, 16);
      int k = Math.floorDiv(center.getZ() - radius, 16);
      int l = Math.floorDiv(center.getZ() + radius, 16);
      ServerChunkCache serverchunkcache = level.getChunkSource();

      for (int i1 = i; i1 <= j; i1++) {
         for (int j1 = k; j1 <= l; j1++) {
            LevelChunk levelchunk = serverchunkcache.getChunkNow(i1, j1);
            if (levelchunk != null) {
               for (BlockEntity blockentity : levelchunk.getBlockEntities().values()) {
                  if (isInHorizontalRange(center, blockentity.getBlockPos(), radius)) {
                     consumer.accept(blockentity);
                  }
               }
            }
         }
      }
   }

   private static boolean isBeaconPulse(ServerLevel level, BlockPos pos, long interval) {
      return Math.floorMod(level.getGameTime() + pos.asLong(), interval) == 0L;
   }

   private static long getCropGrowthInterval(ChampionBeaconBlockEntity beacon) {
      return beacon.isUpgraded() && beacon.getPrimaryPower().isUpgradeable() ? 2400L : 4800L;
   }

   private static Stat getEvStat(ResourceLocation paymentItem) {
      String s = paymentItem == null ? "" : paymentItem.getPath();

      return switch (s) {
         case "fire_stone", "shiny_stone" -> Stats.ATTACK;
         case "water_stone", "leaf_stone" -> Stats.HP;
         case "thunder_stone" -> Stats.SPEED;
         case "dawn_stone", "sun_stone" -> Stats.SPECIAL_ATTACK;
         case "ice_stone", "dusk_stone" -> Stats.DEFENCE;
         case "moon_stone" -> Stats.SPECIAL_DEFENCE;
         default -> Stats.HP;
      };
   }

   private static void sendPulseDebug(ServerLevel level, ChampionBeaconBlockEntity beacon, Component message) {
      if (!DEBUG_PULSE_PLAYERS.isEmpty()) {
         for (ServerPlayer serverplayer : level.players()) {
            if (DEBUG_PULSE_PLAYERS.contains(serverplayer.getUUID())
               && isInHorizontalRange(beacon.getBlockPos(), serverplayer.blockPosition(), beacon.getRadius())) {
               serverplayer.sendSystemMessage(message);
            }
         }
      }
   }

   private static ChampionBeaconBlockEntity getActiveBeacon(ServerLevel level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof ChampionBeaconBlockEntity championbeaconblockentity && isPotentiallyActive(championbeaconblockentity)
         ? championbeaconblockentity
         : null;
   }

   private static boolean isPotentiallyActive(ChampionBeaconBlockEntity beacon) {
      return beacon.hasBeam() && beacon.getPrimaryPower() != ChampionBeaconPower.NONE;
   }

   private static boolean hasPower(ChampionBeaconBlockEntity beacon, ChampionBeaconPower power) {
      return beacon.getPrimaryPower() == power || beacon.getSecondaryPower() == power;
   }

   private static boolean hasCropAura(ChampionBeaconBlockEntity beacon) {
      return hasPower(beacon, ChampionBeaconPower.APRICORN) || hasPower(beacon, ChampionBeaconPower.BERRY);
   }

   private static boolean hasPastureAura(ChampionBeaconBlockEntity beacon) {
      return hasPower(beacon, ChampionBeaconPower.DAYCARE) || hasPower(beacon, ChampionBeaconPower.EV);
   }

   private static boolean hasCacheableAura(ChampionBeaconBlockEntity beacon) {
      return hasCropAura(beacon) || hasPastureAura(beacon);
   }

   private static boolean isPrimaryUpgraded(ChampionBeaconBlockEntity beacon, ChampionBeaconPower power) {
      return beacon.getPrimaryPower() == power && beacon.isUpgraded();
   }

   private static void addPower(EnumSet<ChampionBeaconPower> powers, ChampionBeaconPower power) {
      if (power != ChampionBeaconPower.NONE) {
         powers.add(power);
      }
   }

   private static boolean isInHorizontalRange(BlockPos beaconPos, BlockPos targetPos, int radius) {
      double d0 = targetPos.getX() - beaconPos.getX();
      double d1 = targetPos.getZ() - beaconPos.getZ();
      return d0 * d0 + d1 * d1 <= radius * radius;
   }

   private static float getCobblemonShinyRate() {
      return Cobblemon.INSTANCE.getConfig().getShinyRate();
   }

   private static String formatPos(BlockPos pos) {
      return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
   }

   private static final class AuraCache {
      private final Set<BlockPos> apricornPositions = new HashSet<>();
      private final Set<BlockPos> berryPositions = new HashSet<>();
      private final Set<BlockPos> pasturePositions = new HashSet<>();
      private int radius = -1;
      private int cropPulseCount;
      private int discoveryCursor;
      private ChampionBeaconPower primaryPower = ChampionBeaconPower.NONE;
      private ChampionBeaconPower secondaryPower = ChampionBeaconPower.NONE;
      private boolean upgraded;

      private boolean needsRefresh(ChampionBeaconBlockEntity beacon) {
         return this.radius != beacon.getRadius()
            || this.primaryPower != beacon.getPrimaryPower()
            || this.secondaryPower != beacon.getSecondaryPower()
            || this.upgraded != beacon.isUpgraded();
      }

      private void capture(ChampionBeaconBlockEntity beacon) {
         this.radius = beacon.getRadius();
         this.primaryPower = beacon.getPrimaryPower();
         this.secondaryPower = beacon.getSecondaryPower();
         this.upgraded = beacon.isUpgraded();
      }

      private void clear() {
         this.apricornPositions.clear();
         this.berryPositions.clear();
         this.pasturePositions.clear();
         this.cropPulseCount = 0;
         this.discoveryCursor = 0;
      }
   }

   private record BeaconKey(ResourceKey<Level> dimension, BlockPos pos) {
   }

   private interface CropMatcher {
      boolean matches(BlockState var1);
   }

   private static final class CropPulseStats {
      private final int cachedBefore;
      private int growthAttemptsPerformed;
      private int matureOrSkipped;
      private int removedInvalid;

      private CropPulseStats(int cachedBefore) {
         this.cachedBefore = cachedBefore;
      }

      private static ChampionBeaconAuras.CropPulseStats empty() {
         return new ChampionBeaconAuras.CropPulseStats(0);
      }

      private String describe() {
         return "cached="
            + this.cachedBefore
            + ", growthAttempts="
            + this.growthAttemptsPerformed
            + ", mature/skipped="
            + this.matureOrSkipped
            + ", removed="
            + this.removedInvalid;
      }
   }

   public record DebugInfo(int activeBeacons, EnumSet<ChampionBeaconPower> powers) {
   }

   private static final class PastureVisualizationCounter {
      private int count;
   }

   public record PlayerDebugInfo(
      ChampionBeaconAuras.DebugInfo auraInfo,
      boolean spawnerActive,
      boolean lureActive,
      float ticksUntilNextSpawn,
      float ticksBetweenSpawnAttempts,
      float baseProgressPerTick,
      float lureProgressPerTick,
      float effectiveTicksBetweenSpawnAttempts,
      float cobblemonShinyRate,
      double activeExtraShinyChance
   ) {
   }

   public record VisualizationInfo(int activeBeacons, int apricorns, int berries, int pastures) {
   }
}
