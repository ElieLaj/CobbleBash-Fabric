package com.nore.cobblebash.event;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent.Pre;
import com.cobblemon.mod.common.api.pokemon.experience.BattleExperienceSource;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.gitlab.srcmc.rctapi.api.events.Event;
import com.gitlab.srcmc.rctapi.api.events.Events;
import com.gitlab.srcmc.rctapi.api.trainer.Trainer;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.beacon.ChampionBeaconAuras;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.dialogue.GymTrainerDialogue;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.instance.GymSlotPosition;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.item.RibbonAttributeManager;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.structure.EliteFourStructure;
import fr.harmex.cobblebadges.common.api.point.Point;
import fr.harmex.cobblebadges.common.api.point.Points;
import fr.harmex.cobblebadges.common.utils.extensions.PlayerExtensionKt;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GymEventHandler {
   private static final double GYM_VOID_FAIL_Y = 0.0;
   private static final String TRAINER_ENTITY_TAG = "cobblebash_rct_trainer";
   private static final SoundEvent TRAINER_WIN_SOUND = SoundEvent.createVariableRangeEvent(
      ResourceLocation.fromNamespaceAndPath("cobblemon", "status.up.actor")
   );
   private static final SoundEvent TRAINER_LOSS_SOUND = SoundEvent.createVariableRangeEvent(
      ResourceLocation.fromNamespaceAndPath("cobblemon", "status.down.actor")
   );
   private static final String FLYING_GYM_TYPE = "flying";
   private static final BlockPos FLYING_PLAYER_SPAWN_OFFSET = new BlockPos(40, 9, 28);
   private static final BlockPos FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET = new BlockPos(-5, 8, -14);
   private static final BlockPos FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET = new BlockPos(-17, 18, -7);
   private static final String LAUNCH_PAD_SPAWN = "spawn";
   private static final String LAUNCH_PAD_TRAINER_ONE = "trainer_1";
   private static final String LAUNCH_PAD_TRAINER_TWO = "trainer_2";
   private static final GymEventHandler.LaunchPadSettings SPAWN_LAUNCH_PAD_SETTINGS = new GymEventHandler.LaunchPadSettings(0.8, 0.2, 12, 20, 140);
   private static final GymEventHandler.LaunchPadSettings TRAINER_ONE_LAUNCH_PAD_SETTINGS = new GymEventHandler.LaunchPadSettings(0.85, 0.2, 12, 20, 140);
   private static final GymEventHandler.LaunchPadSettings TRAINER_TWO_LAUNCH_PAD_SETTINGS = new GymEventHandler.LaunchPadSettings(0.9, 0.28, 14, 20, 140);
   private static final TagKey<Block> LAUNCH_PADS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("cobblebash", "launch_pads"));
   private static final double LAUNCH_PAD_VERTICAL_BOOST = 0.85;
   private static final double LAUNCH_PAD_MIN_HORIZONTAL_SPEED_SQR = 0.001;
   private static final double MAX_GYM_NORMAL_UPWARD_SPEED = 0.42;
   private static final int LAUNCH_PAD_AMBIENT_INTERVAL_TICKS = 12;
   private static final int LAUNCH_PAD_AMBIENT_RADIUS = 6;
   private static final int LAUNCH_PAD_AMBIENT_MAX_PADS = 4;
   private static final double LAUNCH_PAD_AMBIENT_POSITION_SPREAD = 0.35;
   private static final double LAUNCH_PAD_AMBIENT_HORIZONTAL_SPEED = 0.012;
   private static final double LAUNCH_PAD_AMBIENT_MIN_UPWARD_SPEED = 0.012;
   private static final double LAUNCH_PAD_AMBIENT_UPWARD_SPEED_VARIANCE = 0.018;
   private static boolean rctListenersRegistered = false;
   private static final Map<UUID, GymEventHandler.NpcBattleLevelState> NPC_BATTLE_LEVELS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> LAUNCH_PAD_ASCENT = new ConcurrentHashMap<>();
   private static final Map<UUID, Double> LAUNCH_PAD_VERTICAL_MOTION = new ConcurrentHashMap<>();
   private static final Map<UUID, Vec3> LAUNCH_PAD_HORIZONTAL_MOTION = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> LAUNCH_PAD_COOLDOWNS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> LAUNCH_PAD_FALL_PROTECTION = new ConcurrentHashMap<>();
   private static final Map<UUID, MobEffectInstance> SUPPRESSED_JUMP_BOOST = new ConcurrentHashMap<>();

   public static void registerRctListeners() {
      if (!rctListenersRegistered) {
         RCTApi rctapi = RCTApi.getInstance("cobblebash");
         if (rctapi != null) {
            registerCobblemonBattleFaintedListener();
            registerCobblemonExperienceListener();
            registerCobblemonSpawnBlocker();
            rctapi.getEventContext().register(Events.BATTLE_STARTED, event -> handleBattleStarted((BattleState)event.getValue()));
            rctapi.getEventContext().register(Events.BATTLE_ENDED, event -> handleBattleEnded((BattleState)event.getValue()));
            rctListenersRegistered = true;
         }
      }
   }

   private static void registerCobblemonBattleFaintedListener() {
      try {
         Object object = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents").getField("BATTLE_FAINTED").get(null);
         object.getClass().getMethod("subscribe", Consumer.class).invoke(object, (Consumer<Object>)event -> {
            if (event instanceof BattleFaintedEvent battlefaintedevent) {
               handleBattleFainted(battlefaintedevent);
            }
         });
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         CobbleBash.LOGGER.warn("Failed to register Cobblemon battle fainted listener.", reflectiveoperationexception);
      }
   }

   private static void registerCobblemonExperienceListener() {
      try {
         Object object = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents").getField("EXPERIENCE_GAINED_EVENT_PRE").get(null);
         object.getClass().getMethod("subscribe", Consumer.class).invoke(object, (Consumer<Object>)event -> {
            if (event instanceof Pre pre) {
               handleExperienceGained(pre);
            }
         });
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         CobbleBash.LOGGER.warn("Failed to register Cobblemon experience listener.", reflectiveoperationexception);
      }
   }

   private static void registerCobblemonSpawnBlocker() {
      try {
         Object object = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents").getField("POKEMON_ENTITY_SPAWN").get(null);
         Consumer<Object> consumer = GymEventHandler::handleCobblemonPokemonSpawn;
         object.getClass().getMethod("subscribe", Consumer.class).invoke(object, consumer);
      } catch (ReflectiveOperationException reflectiveoperationexception) {
         CobbleBash.LOGGER.warn("Failed to register Cobblemon void spawn blocker.", reflectiveoperationexception);
      }
   }

   private static void handleCobblemonPokemonSpawn(Object event) {
      if (event instanceof SpawnEvent<?> spawnevent && spawnevent.getEntity() instanceof PokemonEntity pokemonentity) {
         handleCobblemonPokemonSpawn(spawnevent, pokemonentity);
      } else {
         try {
            Object object = event.getClass().getMethod("getSpawnablePosition").invoke(event);
            if (object.getClass().getMethod("getWorld").invoke(object) instanceof ServerLevel serverlevel
               && serverlevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
               event.getClass().getMethod("cancel").invoke(event);
            }
         } catch (ReflectiveOperationException reflectiveoperationexception) {
            CobbleBash.LOGGER.warn("Failed to handle Cobblemon void spawn event.", reflectiveoperationexception);
         }
      }
   }

   private static void handleCobblemonPokemonSpawn(SpawnEvent<?> event, PokemonEntity pokemon) {
      ServerLevel serverlevel = event.getSpawnablePosition().getWorld();
      BlockPos blockpos = event.getSpawnablePosition().getPosition();
      if (serverlevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
         event.cancel();
      } else if (ChampionBeaconAuras.shouldRepel(serverlevel, blockpos)) {
         event.cancel();
      } else {
         ChampionBeaconAuras.tryApplyShinyAura(pokemon, serverlevel, blockpos);
      }
   }

   /**
    * Branchement des evenements.
    *
    * <p>Onze des quatorze crochets ont un equivalent direct dans l'API Fabric.
    * Les trois autres — maintien d'un item, arrivee d'entite annulable,
    * obtention d'un avancement — passent par des mixins, qui appellent les
    * methodes publiques en fin de classe.
    */
   public static void register() {
      ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
         ServerPlayer player = handler.player;
         RibbonAttributeManager.handlePlayerLogout(player);
         restoreSuppressedJumpBoost(player);
         GymCommand.clearActiveGym(player, false, false);
      });

      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
         ServerPlayer player = handler.player;
         if (player.level().dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            GymCommand.clearActiveGym(player, false, false);
            GymCommand.teleportToSavedReturnOrSpawn(player);
         }

         CobbleBashStats.syncGymsCompleted(player);
      });

      UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
         if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverplayer)) {
            return InteractionResult.PASS;
         }

         if (!(entity instanceof LivingEntity livingentity)) {
            return InteractionResult.PASS;
         }

         RctApiProbe.GymTrainerRef ref = RctApiProbe.getGymTrainerRef(livingentity);
         if (ref == null) {
            return InteractionResult.PASS;
         }

         return GymTrainerDialogue.open(serverplayer, ref) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      });

      UseItemCallback.EVENT.register((player, level, hand) -> {
         ItemStack stack = player.getItemInHand(hand);
         return cancelBlacklistedGymItem(player, stack)
            ? InteractionResultHolder.fail(stack)
            : InteractionResultHolder.pass(stack);
      });

      UseBlockCallback.EVENT.register(
         (player, level, hand, hitResult) -> cancelBlacklistedGymItem(player, player.getItemInHand(hand))
            ? InteractionResult.FAIL
            : InteractionResult.PASS
      );

      // `PlayerTickEvent.Post` n'a pas d'equivalent : on balaie les joueurs
      // connectes a la fin de chaque tick serveur, ce qui revient au meme.
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickGymPlayer(player);
         }
      });

      // Couvre `LivingIncomingDamageEvent` et `LivingDamageEvent.Pre` : tous
      // deux ne faisaient qu'annuler des degats.
      ServerLivingEntityEvents.ALLOW_DAMAGE.register(GymEventHandler::allowDamage);

      // `BlockEvent.BreakEvent`. La pose passe par un mixin : Fabric n'a pas de
      // crochet de pose de bloc en API de base.
      PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
         if (level instanceof ServerLevel serverlevel) {
            ChampionBeaconAuras.handleBlockChange(serverlevel, pos, state, Blocks.AIR.defaultBlockState());
         }

         return true;
      });
   }

   private static void tickGymPlayer(ServerPlayer serverplayer) {
      ChampionBeaconAuras.tickPlayer(serverplayer);
      if (!isInGymVoid(serverplayer)) {
         clearLaunchPadState(serverplayer);
         restoreSuppressedJumpBoost(serverplayer);
      } else {
         tickLaunchPadState(serverplayer);
         suppressJumpBoost(serverplayer);
         tickSuppressedJumpBoost(serverplayer);
         if (serverplayer.isFallFlying()) {
            serverplayer.stopFallFlying();
         }

         if (serverplayer.isPassenger()) {
            serverplayer.stopRiding();
         }

         applyLaunchPadAscent(serverplayer);
         clampGymJumpBoost(serverplayer);
         spawnAmbientLaunchPadParticles(serverplayer);
         tickEliteFourChampionTransition(serverplayer);
         if (serverplayer.getY() < 0.0) {
            failGymSafely(serverplayer, "You fell out of the gym.");
         } else {
            tryLaunchFromPad(serverplayer);
         }
      }
   }

   private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
      if (isGymTrainerEntity(entity)) {
         return false;
      }

      if (!(entity instanceof ServerPlayer serverplayer) || !isInGymVoid(serverplayer)) {
         return true;
      }

      if (isLaunchPadFallProtected(serverplayer) && source.is(DamageTypeTags.IS_FALL)) {
         serverplayer.resetFallDistance();
         return false;
      }

      // L'original ramenait les degats a zero puis declenchait l'echec ;
      // annuler entierement produit le meme etat : le joueur survit et sort.
      if (amount >= serverplayer.getHealth()) {
         failGymSafely(serverplayer, "You were defeated in the gym.");
         return false;
      }

      return true;
   }

   private static void tickEliteFourChampionTransition(ServerPlayer player) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance != null && "elite4".equals(gyminstance.getGymType()) && gyminstance.isEliteFourChampionUnlocked()) {
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         if (!gyminstance.hasEliteFourSlowFallingApplied() && EliteFourStructure.isInsideSlowFallField(blockpos, player.blockPosition())) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
            player.resetFallDistance();
            gyminstance.markEliteFourSlowFallingApplied();
            gyminstance.setEliteFourChampionBeamTicks(200);
         }

         if (gyminstance.isEliteFourChampionBeamActive()) {
            EliteFourStructure.startChampionBeam(player.serverLevel(), blockpos);
            gyminstance.tickEliteFourChampionBeam();
            if (!gyminstance.isEliteFourChampionBeamActive()) {
               EliteFourStructure.stopChampionBeam(player.serverLevel(), blockpos);
            }
         }
      }
   }

   // ------------------------------------------------------------------
   // Appeles depuis les mixins, faute de crochet Fabric equivalent.
   // ------------------------------------------------------------------

   /** {@code EntityJoinLevelEvent}, dont Fabric n'a pas de version annulable. */
   public static boolean blockEntityJoin(Level level, Entity entity) {
      if (!(level instanceof ServerLevel serverlevel) || !serverlevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
         return false;
      }

      return !(entity instanceof PokemonEntity) && isBlockedGymVoidCategory(entity.getType().getCategory());
   }

   /** {@code LivingEntityUseItemEvent.Start} et {@code .Tick}. */
   public static boolean blockItemUse(LivingEntity entity, ItemStack stack) {
      return entity instanceof Player player && cancelBlacklistedGymItem(player, stack);
   }

   /** {@code BlockEvent.EntityPlaceEvent}. */
   public static void onBlockPlaced(Level level, BlockPos pos, BlockState placedOver, BlockState placed) {
      if (level instanceof ServerLevel serverlevel) {
         ChampionBeaconAuras.handleBlockChange(serverlevel, pos, placedOver, placed);
      }
   }

   /** {@code AdvancementEvent.AdvancementEarnEvent}. */
   public static void onAdvancementEarned(ServerPlayer serverplayer, AdvancementHolder advancement) {
      String path = advancement.id().getPath();
      if (!advancement.id().getNamespace().equals("cobblebash") || !path.startsWith("gym/")) {
         return;
      }

      String s = path.substring("gym/".length());
      if (s.startsWith("complete_gym/")) {
         s = s.substring("complete_gym/".length());
      }

      if (isElementalGymType(s)) {
         Point point = Points.getById(ResourceLocation.fromNamespaceAndPath("cobblebadges", s));
         if (point != null) {
            PlayerExtensionKt.getCobbleBadgesData(serverplayer).setPoints(serverplayer, point, 0);
         }

         CobbleBashStats.syncGymsCompleted(serverplayer);
      }
   }

   private static void handleBattleStarted(BattleState battleState) {
      PokemonBattle pokemonbattle = battleState.getBattle();
      if (pokemonbattle != null) {
         for (Trainer trainer : battleState.getParticipants2()) {
            if (trainer instanceof TrainerNPC trainernpc) {
               RctApiProbe.GymTrainerRef rctapiprobe$gymtrainerref = RctApiProbe.getGymTrainerRef(trainernpc.getEntity());
               GymTrainerUnit gymtrainerunit = rctapiprobe$gymtrainerref == null
                  ? null
                  : GymTrainerUnit.fromTrainerIdPart(rctapiprobe$gymtrainerref.trainerIdPart());
               if (gymtrainerunit != null && trainernpc.getTeam().length != 0) {
                  LivingEntity livingentity = trainernpc.getEntity(pokemonbattle);
                  if (livingentity == null) {
                     livingentity = trainernpc.getEntity();
                  }

                  if (livingentity != null) {
                     UUID uuid = livingentity.getUUID();
                     int i = trainernpc.getTeam()[0].getLevel();
                     GymEventHandler.NpcBattleLevelState gymeventhandler$npcbattlelevelstate = new GymEventHandler.NpcBattleLevelState(
                        uuid, gymtrainerunit, i, trainernpc.getTeam().length
                     );
                     NPC_BATTLE_LEVELS.put(pokemonbattle.getBattleId(), gymeventhandler$npcbattlelevelstate);
                     applyNpcBattleLevels(pokemonbattle, gymeventhandler$npcbattlelevelstate);
                  }
               }
            }
         }
      }
   }

   private static void handleBattleFainted(BattleFaintedEvent event) {
      GymEventHandler.NpcBattleLevelState gymeventhandler$npcbattlelevelstate = NPC_BATTLE_LEVELS.get(event.getBattle().getBattleId());
      if (gymeventhandler$npcbattlelevelstate != null && event.getKilled().getActor() != null) {
         if (gymeventhandler$npcbattlelevelstate.actorId().equals(event.getKilled().getActor().getUuid())) {
            gymeventhandler$npcbattlelevelstate.incrementFaintedCount();
            applyNpcBattleLevels(event.getBattle(), gymeventhandler$npcbattlelevelstate);
         }
      }
   }

   private static void handleExperienceGained(Pre event) {
      if (event.getSource() instanceof BattleExperienceSource battleexperiencesource && event.getExperience() > 0) {
         GymEventHandler.NpcBattleLevelState gymeventhandler$npcbattlelevelstate = NPC_BATTLE_LEVELS.get(battleexperiencesource.getBattle().getBattleId());
         if (gymeventhandler$npcbattlelevelstate != null) {
            ServerPlayer serverplayer = event.getPokemon().getOwnerPlayer();
            if (serverplayer != null && serverplayer.level().dimension().equals(CobbleBashDimensions.GYM_VOID)) {
               GymInstance gyminstance = GymInstanceManager.getActive(serverplayer.getUUID());
               if (gyminstance != null && (gyminstance.isRepeatClear() || "elite4".equals(gyminstance.getGymType()))) {
                  double d0 = gymeventhandler$npcbattlelevelstate.unit() == GymTrainerUnit.BOSS
                     ? Config.repeatClearBossXpMultiplier()
                     : Config.repeatClearTrainerXpMultiplier();
                  event.setExperience(scaleExperience(event.getExperience(), d0));
               }
            }
         }
      }
   }

   private static int scaleExperience(int experience, double multiplier) {
      long i = Math.round(experience * multiplier);
      if (i <= experience && multiplier > 1.0) {
         i = experience + 1L;
      }

      return (int)Math.min(2147483647L, i);
   }

   private static void handleBattleEnded(BattleState battleState) {
      if (battleState.getBattle() != null) {
         NPC_BATTLE_LEVELS.remove(battleState.getBattle().getBattleId());
      }

      for (Trainer trainer : battleState.getWinners()) {
         if (trainer instanceof TrainerPlayer trainerplayer) {
            handlePlayerWon(trainerplayer.getPlayer(), battleState.getLosers());
         }
      }

      for (Trainer trainer1 : battleState.getLosers()) {
         if (trainer1 instanceof TrainerPlayer trainerplayer1) {
            handlePlayerLost(trainerplayer1.getPlayer(), battleState.getWinners());
         }
      }
   }

   private static void applyNpcBattleLevels(PokemonBattle battle, GymEventHandler.NpcBattleLevelState levelState) {
      int i = levelState.currentLevel();

      for (BattleActor battleactor : battle.getActors()) {
         if (levelState.actorId().equals(battleactor.getUuid())) {
            for (BattlePokemon battlepokemon : battleactor.getPokemonList()) {
               if (!battlepokemon.getEffectedPokemon().isFainted()) {
                  battlepokemon.getEffectedPokemon().setLevel(i);
                  battlepokemon.sendUpdate();
               }
            }
         }
      }
   }

   private static void handlePlayerWon(ServerPlayer player, Iterable<Trainer> losers) {
      for (Trainer trainer : losers) {
         if (trainer instanceof TrainerNPC trainernpc) {
            RctApiProbe.GymTrainerRef rctapiprobe$gymtrainerref = RctApiProbe.getGymTrainerRef(trainernpc.getEntity());
            if (rctapiprobe$gymtrainerref != null) {
               GymTrainerUnit gymtrainerunit = GymTrainerUnit.fromTrainerIdPart(rctapiprobe$gymtrainerref.trainerIdPart());
               if (gymtrainerunit != null) {
                  if (gymtrainerunit != GymTrainerUnit.BOSS) {
                     playTrainerResultSound(player, TRAINER_WIN_SOUND);
                  }

                  GymCommand.handleTrainerVictory(player, rctapiprobe$gymtrainerref.gymType(), rctapiprobe$gymtrainerref.slotId(), gymtrainerunit);
               }
            }
         }
      }
   }

   private static void handlePlayerLost(ServerPlayer player, Iterable<Trainer> winners) {
      for (Trainer trainer : winners) {
         if (trainer instanceof TrainerNPC trainernpc) {
            RctApiProbe.GymTrainerRef rctapiprobe$gymtrainerref = RctApiProbe.getGymTrainerRef(trainernpc.getEntity());
            if (rctapiprobe$gymtrainerref != null) {
               playTrainerResultSound(player, TRAINER_LOSS_SOUND);
               GymCommand.clearActiveGym(player, true);
               return;
            }
         }
      }
   }

   private static void playTrainerResultSound(ServerPlayer player, SoundEvent sound) {
      player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   private static boolean isElementalGymType(String gymType) {
      for (GymType gymtype : GymType.values()) {
         if (gymtype.getId().equals(gymType)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isBlockedGymVoidCategory(MobCategory category) {
      return category == MobCategory.CREATURE
         || category == MobCategory.MONSTER
         || category == MobCategory.AMBIENT
         || category == MobCategory.AXOLOTLS
         || category == MobCategory.UNDERGROUND_WATER_CREATURE
         || category == MobCategory.WATER_CREATURE
         || category == MobCategory.WATER_AMBIENT;
   }

   private static boolean cancelBlacklistedGymItem(Player player, ItemStack stack) {
      return player instanceof ServerPlayer serverplayer && isInGymVoid(serverplayer) && Config.isGymBlacklisted(stack);
   }

   private static boolean isInGymVoid(ServerPlayer player) {
      return player.level().dimension().equals(CobbleBashDimensions.GYM_VOID);
   }

   private static boolean isGymTrainerEntity(LivingEntity entity) {
      return entity.level().dimension().equals(CobbleBashDimensions.GYM_VOID) && entity.getTags().contains("cobblebash_rct_trainer");
   }

   private static void tryLaunchFromPad(ServerPlayer player) {
      if (isFlyingGymActive(player) && player.onGround() && LAUNCH_PAD_COOLDOWNS.getOrDefault(player.getUUID(), 0) <= 0) {
         BlockPos blockpos = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
         if (player.level().getBlockState(blockpos).is(LAUNCH_PADS)) {
            if (isLaunchPadActiveForStage(player, blockpos)) {
               String s = getLaunchPadId(player, blockpos);
               GymEventHandler.LaunchPadSettings gymeventhandler$launchpadsettings = getLaunchPadSettings(s);
               Vec3 vec3 = getLaunchPadHorizontalBoost(player, blockpos, gymeventhandler$launchpadsettings.horizontal());
               player.setDeltaMovement(vec3.x(), gymeventhandler$launchpadsettings.vertical(), vec3.z());
               player.hasImpulse = true;
               player.hurtMarked = true;
               player.resetFallDistance();
               player.connection.send(new ClientboundSetEntityMotionPacket(player));
               playLaunchPadEffects(player.serverLevel(), blockpos);
               LAUNCH_PAD_ASCENT.put(player.getUUID(), gymeventhandler$launchpadsettings.ticks());
               LAUNCH_PAD_VERTICAL_MOTION.put(player.getUUID(), gymeventhandler$launchpadsettings.vertical());
               LAUNCH_PAD_HORIZONTAL_MOTION.put(player.getUUID(), vec3);
               LAUNCH_PAD_COOLDOWNS.put(player.getUUID(), gymeventhandler$launchpadsettings.cooldown());
               LAUNCH_PAD_FALL_PROTECTION.put(player.getUUID(), gymeventhandler$launchpadsettings.fallProtection());
            }
         }
      }
   }

   private static void spawnAmbientLaunchPadParticles(ServerPlayer player) {
      if (isFlyingGymActive(player) && player.tickCount % 12 == 0) {
         ServerLevel serverlevel = player.serverLevel();
         BlockPos blockpos = player.blockPosition();
         BlockPos blockpos1 = blockpos.offset(-6, -2, -6);
         BlockPos blockpos2 = blockpos.offset(6, 1, 6);
         int i = 0;

         for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos1, blockpos2)) {
            if (serverlevel.getBlockState(blockpos3).is(LAUNCH_PADS) && isLaunchPadActiveForStage(player, blockpos3)) {
               double d0 = blockpos3.getX() + 0.5 + (player.getRandom().nextDouble() - 0.5) * 0.35;
               double d1 = blockpos3.getY() + 1.08;
               double d2 = blockpos3.getZ() + 0.5 + (player.getRandom().nextDouble() - 0.5) * 0.35;
               double d3 = (player.getRandom().nextDouble() - 0.5) * 0.012;
               double d4 = 0.012 + player.getRandom().nextDouble() * 0.018;
               double d5 = (player.getRandom().nextDouble() - 0.5) * 0.012;
               serverlevel.sendParticles(ParticleTypes.CLOUD, d0, d1, d2, 0, d3, d4, d5, 1.0);
               if (++i >= 4) {
                  return;
               }
            }
         }
      }
   }

   private static void playLaunchPadEffects(ServerLevel level, BlockPos pos) {
      double d0 = pos.getX() + 0.5;
      double d1 = pos.getY() + 1.05;
      double d2 = pos.getZ() + 0.5;
      level.playSound(null, d0, d1, d2, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.9F, 1.1F);
      level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, d0, d1, d2, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, d0, d1 + 0.08, d2, 12, 0.22, 0.08, 0.22, 0.06);
   }

   private static boolean isFlyingGymActive(ServerPlayer player) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      return gyminstance != null && "flying".equals(gyminstance.getGymType());
   }

   private static boolean isLaunchPadActiveForStage(ServerPlayer player, BlockPos padPos) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance != null && "flying".equals(gyminstance.getGymType())) {
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         BlockPos blockpos1 = blockpos.offset(FLYING_PLAYER_SPAWN_OFFSET);
         if (padPos.equals(blockpos1.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return gyminstance.getTrainerStage() >= 1;
         } else {
            return padPos.equals(blockpos1.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET)) ? gyminstance.getTrainerStage() >= 2 : true;
         }
      } else {
         return false;
      }
   }

   private static Vec3 getLaunchPadHorizontalBoost(ServerPlayer player, BlockPos padPos, double horizontalBoost) {
      Vec3 vec3 = getConfiguredLaunchPadDirection(player, padPos);
      if (vec3.lengthSqr() <= 0.001) {
         Vec3 vec31 = player.getLookAngle();
         vec3 = new Vec3(vec31.x(), 0.0, vec31.z());
      }

      if (vec3.lengthSqr() <= 0.001) {
         Vec3 vec32 = player.getDeltaMovement();
         vec3 = new Vec3(vec32.x(), 0.0, vec32.z());
      }

      return vec3.lengthSqr() > 0.001 ? vec3.normalize().scale(horizontalBoost) : Vec3.ZERO;
   }

   private static String getLaunchPadId(ServerPlayer player, BlockPos padPos) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance != null && "flying".equals(gyminstance.getGymType())) {
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         BlockPos blockpos1 = blockpos.offset(FLYING_PLAYER_SPAWN_OFFSET);
         if (padPos.equals(blockpos1.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return "trainer_1";
         } else {
            return padPos.equals(blockpos1.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET)) ? "trainer_2" : "spawn";
         }
      } else {
         return "spawn";
      }
   }

   private static GymEventHandler.LaunchPadSettings getLaunchPadSettings(String padId) {
      return switch (padId) {
         case "trainer_1" -> TRAINER_ONE_LAUNCH_PAD_SETTINGS;
         case "trainer_2" -> TRAINER_TWO_LAUNCH_PAD_SETTINGS;
         default -> SPAWN_LAUNCH_PAD_SETTINGS;
      };
   }

   private static Vec3 getConfiguredLaunchPadDirection(ServerPlayer player, BlockPos padPos) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance != null && "flying".equals(gyminstance.getGymType())) {
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         BlockPos blockpos1 = blockpos.offset(FLYING_PLAYER_SPAWN_OFFSET);
         if (padPos.equals(blockpos1.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return new Vec3(-1.0, 0.0, 0.0);
         } else {
            return padPos.equals(blockpos1.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET)) ? new Vec3(0.0, 0.0, 1.0) : Vec3.ZERO;
         }
      } else {
         return Vec3.ZERO;
      }
   }

   private static void suppressJumpBoost(ServerPlayer player) {
      MobEffectInstance mobeffectinstance = player.getEffect(MobEffects.JUMP);
      if (mobeffectinstance != null) {
         SUPPRESSED_JUMP_BOOST.putIfAbsent(player.getUUID(), new MobEffectInstance(mobeffectinstance));
         player.removeEffect(MobEffects.JUMP);
      }
   }

   private static void restoreSuppressedJumpBoost(ServerPlayer player) {
      MobEffectInstance mobeffectinstance = SUPPRESSED_JUMP_BOOST.remove(player.getUUID());
      if (mobeffectinstance != null && (mobeffectinstance.isInfiniteDuration() || mobeffectinstance.getDuration() > 0)) {
         player.addEffect(mobeffectinstance);
      }
   }

   private static void tickSuppressedJumpBoost(ServerPlayer player) {
      MobEffectInstance mobeffectinstance = SUPPRESSED_JUMP_BOOST.get(player.getUUID());
      if (mobeffectinstance != null && !mobeffectinstance.tick(player, () -> {})) {
         SUPPRESSED_JUMP_BOOST.remove(player.getUUID());
      }
   }

   private static void clampGymJumpBoost(ServerPlayer player) {
      if (!isLaunchPadMovementAllowed(player)) {
         Vec3 vec3 = player.getDeltaMovement();
         if (!(vec3.y() <= 0.42)) {
            player.setDeltaMovement(vec3.x(), 0.42, vec3.z());
            player.hasImpulse = true;
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
         }
      }
   }

   private static void tickLaunchPadState(ServerPlayer player) {
      decrementOrRemove(LAUNCH_PAD_COOLDOWNS, player.getUUID());
      if (decrementOrRemove(LAUNCH_PAD_FALL_PROTECTION, player.getUUID())) {
         player.resetFallDistance();
      }
   }

   private static void applyLaunchPadAscent(ServerPlayer player) {
      Integer integer = LAUNCH_PAD_ASCENT.get(player.getUUID());
      if (integer != null) {
         Vec3 vec3 = player.getDeltaMovement();
         double d0 = LAUNCH_PAD_VERTICAL_MOTION.getOrDefault(player.getUUID(), 0.85);
         Vec3 vec31 = LAUNCH_PAD_HORIZONTAL_MOTION.getOrDefault(player.getUUID(), Vec3.ZERO);
         double d1 = vec31.lengthSqr() > 0.0 ? vec31.x() : vec3.x();
         double d2 = vec31.lengthSqr() > 0.0 ? vec31.z() : vec3.z();
         player.setDeltaMovement(d1, d0, d2);
         player.hasImpulse = true;
         player.hurtMarked = true;
         player.resetFallDistance();
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
         if (integer > 1 && !player.horizontalCollision) {
            LAUNCH_PAD_ASCENT.put(player.getUUID(), integer - 1);
         } else {
            LAUNCH_PAD_ASCENT.remove(player.getUUID());
            LAUNCH_PAD_VERTICAL_MOTION.remove(player.getUUID());
            LAUNCH_PAD_HORIZONTAL_MOTION.remove(player.getUUID());
         }
      }
   }

   private static boolean decrementOrRemove(Map<UUID, Integer> map, UUID playerId) {
      Integer integer = map.get(playerId);
      if (integer == null) {
         return false;
      }

      if (integer <= 1) {
         map.remove(playerId);
      } else {
         map.put(playerId, integer - 1);
      }

      return true;
   }

   private static boolean isLaunchPadFallProtected(ServerPlayer player) {
      return LAUNCH_PAD_FALL_PROTECTION.getOrDefault(player.getUUID(), 0) > 0;
   }

   private static boolean isLaunchPadMovementAllowed(ServerPlayer player) {
      return LAUNCH_PAD_ASCENT.containsKey(player.getUUID()) || isLaunchPadFallProtected(player);
   }

   private static void clearLaunchPadState(ServerPlayer player) {
      LAUNCH_PAD_ASCENT.remove(player.getUUID());
      LAUNCH_PAD_VERTICAL_MOTION.remove(player.getUUID());
      LAUNCH_PAD_HORIZONTAL_MOTION.remove(player.getUUID());
      LAUNCH_PAD_COOLDOWNS.remove(player.getUUID());
      LAUNCH_PAD_FALL_PROTECTION.remove(player.getUUID());
   }

   private static void failGymSafely(ServerPlayer player, String message) {
      clearLaunchPadState(player);
      player.setHealth(Math.max(1.0F, player.getHealth()));
      player.sendSystemMessage(Component.literal(message));
      GymCommand.clearActiveGym(player, true);
      restoreSuppressedJumpBoost(player);
   }

   private record LaunchPadSettings(double vertical, double horizontal, int ticks, int cooldown, int fallProtection) {
   }

   private static class NpcBattleLevelState {
      private final UUID actorId;
      private final GymTrainerUnit unit;
      private final int baseLevel;
      private final int teamSize;
      private int faintedCount;

      private NpcBattleLevelState(UUID actorId, GymTrainerUnit unit, int baseLevel, int teamSize) {
         this.actorId = actorId;
         this.unit = unit;
         this.baseLevel = baseLevel;
         this.teamSize = teamSize;
      }

      private UUID actorId() {
         return this.actorId;
      }

      private void incrementFaintedCount() {
         this.faintedCount++;
      }

      private int currentLevel() {
         if (this.unit == GymTrainerUnit.TRAINER_TWO && this.faintedCount >= this.teamSize - 1) {
            return this.baseLevel + 3;
         }

         if (this.unit == GymTrainerUnit.BOSS) {
            if (this.faintedCount >= this.teamSize - 1) {
               return this.baseLevel + 5;
            }

            if (this.faintedCount >= this.teamSize - 2) {
               return this.baseLevel + 3;
            }
         }

         return this.baseLevel;
      }

      private GymTrainerUnit unit() {
         return this.unit;
      }
   }
}
