package com.nore.cobblebash.event;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.events.Events;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;
import com.gitlab.srcmc.rctapi.api.trainer.Trainer;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.dialogue.GymTrainerDialogue;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.instance.GymSlotPosition;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.stats.CobbleBashStats;
import fr.harmex.cobblebadges.common.api.point.Point;
import fr.harmex.cobblebadges.common.api.point.Points;
import fr.harmex.cobblebadges.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GymEventHandler {
    private static final double GYM_VOID_FAIL_Y = 0.0D;
    private static final String TRAINER_ENTITY_TAG = "cobblebash_rct_trainer";
    private static final String FLYING_GYM_TYPE = "flying";
    private static final BlockPos FLYING_PLAYER_SPAWN_OFFSET = new BlockPos(40, 9, 28);
    private static final BlockPos FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET = new BlockPos(-5, 8, -14);
    private static final BlockPos FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET = new BlockPos(-17, 18, -7);
    private static final String LAUNCH_PAD_SPAWN = "spawn";
    private static final String LAUNCH_PAD_TRAINER_ONE = "trainer_1";
    private static final String LAUNCH_PAD_TRAINER_TWO = "trainer_2";
    private static final LaunchPadSettings SPAWN_LAUNCH_PAD_SETTINGS = new LaunchPadSettings(0.8D, 0.2D, 12, 20, 140);
    private static final LaunchPadSettings TRAINER_ONE_LAUNCH_PAD_SETTINGS = new LaunchPadSettings(0.85D, 0.2D, 12, 20, 140);
    private static final LaunchPadSettings TRAINER_TWO_LAUNCH_PAD_SETTINGS = new LaunchPadSettings(0.9D, 0.28D, 14, 20, 140);
    private static final TagKey<Block> LAUNCH_PADS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "launch_pads")
    );
    private static final double LAUNCH_PAD_VERTICAL_BOOST = 0.85D;
    private static final double LAUNCH_PAD_MIN_HORIZONTAL_SPEED_SQR = 0.001D;
    private static final double MAX_GYM_NORMAL_UPWARD_SPEED = 0.42D;
    private static final int LAUNCH_PAD_AMBIENT_INTERVAL_TICKS = 12;
    private static final int LAUNCH_PAD_AMBIENT_RADIUS = 6;
    private static final int LAUNCH_PAD_AMBIENT_MAX_PADS = 4;
    private static final double LAUNCH_PAD_AMBIENT_POSITION_SPREAD = 0.35D;
    private static final double LAUNCH_PAD_AMBIENT_HORIZONTAL_SPEED = 0.012D;
    private static final double LAUNCH_PAD_AMBIENT_MIN_UPWARD_SPEED = 0.012D;
    private static final double LAUNCH_PAD_AMBIENT_UPWARD_SPEED_VARIANCE = 0.018D;
    private static boolean rctListenersRegistered = false;
    private static final Map<UUID, NpcBattleLevelState> NPC_BATTLE_LEVELS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAUNCH_PAD_ASCENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> LAUNCH_PAD_VERTICAL_MOTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAUNCH_PAD_HORIZONTAL_MOTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAUNCH_PAD_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAUNCH_PAD_FALL_PROTECTION = new ConcurrentHashMap<>();
    private static final Map<UUID, MobEffectInstance> SUPPRESSED_JUMP_BOOST = new ConcurrentHashMap<>();

    public static void registerRctListeners() {
        if (rctListenersRegistered) {
            return;
        }

        var api = RCTApi.getInstance("cobblebash");
        if (api == null) {
            return;
        }

        registerCobblemonBattleFaintedListener();
        registerCobblemonSpawnBlocker();
        api.getEventContext().register(Events.BATTLE_STARTED, event -> handleBattleStarted(event.getValue()));
        api.getEventContext().register(Events.BATTLE_ENDED, event -> handleBattleEnded(event.getValue()));
        rctListenersRegistered = true;
    }

    private static void registerCobblemonBattleFaintedListener() {
        try {
            Object observable = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")
                    .getField("BATTLE_FAINTED")
                    .get(null);
            observable.getClass()
                    .getMethod("subscribe", Consumer.class)
                    .invoke(observable, (Consumer<Object>) event -> {
                        if (event instanceof com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent faintedEvent) {
                            handleBattleFainted(faintedEvent);
                        }
                    });
        } catch (ReflectiveOperationException exception) {
            CobbleBash.LOGGER.warn("Failed to register Cobblemon battle fainted listener.", exception);
        }
    }

    private static void registerCobblemonSpawnBlocker() {
        try {
            Object observable = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents")
                    .getField("POKEMON_ENTITY_SPAWN")
                    .get(null);
            observable.getClass()
                    .getMethod("subscribe", Consumer.class)
                    .invoke(observable, (Consumer<Object>) GymEventHandler::handleCobblemonPokemonSpawn);
        } catch (ReflectiveOperationException exception) {
            CobbleBash.LOGGER.warn("Failed to register Cobblemon void spawn blocker.", exception);
        }
    }

    private static void handleCobblemonPokemonSpawn(Object event) {
        try {
            Object spawnablePosition = event.getClass().getMethod("getSpawnablePosition").invoke(event);
            Object world = spawnablePosition.getClass().getMethod("getWorld").invoke(spawnablePosition);

            if (world instanceof ServerLevel serverLevel && serverLevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
                event.getClass().getMethod("cancel").invoke(event);
            }
        } catch (ReflectiveOperationException exception) {
            CobbleBash.LOGGER.warn("Failed to handle Cobblemon void spawn event.", exception);
        }
    }

    /**
     * Branchement des evenements.
     *
     * <p>Neuf des douze crochets de l'original ont un equivalent direct dans
     * l'API Fabric. Les trois autres — debut et tick d'utilisation d'un item,
     * arrivee d'une entite annulable, obtention d'un avancement — n'existent
     * pas : ils passent par des mixins, qui appellent les methodes publiques
     * plus bas.
     */
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
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
            if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof LivingEntity target)) {
                return InteractionResult.PASS;
            }

            RctApiProbe.GymTrainerRef trainerRef = RctApiProbe.getGymTrainerRef(target);
            if (trainerRef == null) {
                return InteractionResult.PASS;
            }

            return GymTrainerDialogue.open(serverPlayer, trainerRef)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            return cancelBlacklistedGymItem(player, stack)
                    ? InteractionResultHolder.fail(stack)
                    : InteractionResultHolder.pass(stack);
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
                cancelBlacklistedGymItem(player, player.getItemInHand(hand))
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);

        // `PlayerTickEvent.Post` n'a pas d'equivalent : on balaie les joueurs
        // connectes a la fin de chaque tick serveur, ce qui revient au meme.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickGymPlayer(player);
            }
        });

        // Couvre `LivingIncomingDamageEvent` et `LivingDamageEvent.Pre` : tous
        // deux ne faisaient qu'annuler des degats, ce que ce crochet fait aussi.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GymEventHandler::allowDamage);
    }

    private static void tickGymPlayer(ServerPlayer player) {
        if (!isInGymVoid(player)) {
            clearLaunchPadState(player);
            restoreSuppressedJumpBoost(player);
            return;
        }

        tickLaunchPadState(player);
        suppressJumpBoost(player);
        tickSuppressedJumpBoost(player);

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        if (player.isPassenger()) {
            player.stopRiding();
        }

        applyLaunchPadAscent(player);
        clampGymJumpBoost(player);

        spawnAmbientLaunchPadParticles(player);

        if (player.getY() < GYM_VOID_FAIL_Y) {
            failGymSafely(player, "You fell out of the gym.");
            return;
        }

        tryLaunchFromPad(player);
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (isGymTrainerEntity(entity)) {
            return false;
        }

        if (!(entity instanceof ServerPlayer player) || !isInGymVoid(player)) {
            return true;
        }

        if (isLaunchPadFallProtected(player) && source.is(DamageTypeTags.IS_FALL)) {
            player.resetFallDistance();
            return false;
        }

        // L'original ramenait les degats a zero puis declenchait l'echec ;
        // annuler entierement produit le meme etat : le joueur survit et sort.
        if (amount >= player.getHealth()) {
            failGymSafely(player, "You were defeated in the gym.");
            return false;
        }

        return true;
    }

    // ------------------------------------------------------------------
    // Appeles depuis les mixins, faute de crochet Fabric equivalent.
    // ------------------------------------------------------------------

    /** {@code LivingEntityUseItemEvent.Start} et {@code .Tick}. */
    public static boolean blockItemUse(LivingEntity entity, ItemStack stack) {
        return entity instanceof Player player && cancelBlacklistedGymItem(player, stack);
    }

    /** {@code EntityJoinLevelEvent}, dont Fabric n'a pas de version annulable. */
    public static boolean blockEntityJoin(Level level, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            return false;
        }

        return entity instanceof PokemonEntity pokemon && !isGymBattlePokemon(pokemon);
    }

    /** {@code AdvancementEvent.AdvancementEarnEvent}. */
    public static void onAdvancementEarned(ServerPlayer player, ResourceLocation advancementId) {
        String path = advancementId.getPath();
        if (!advancementId.getNamespace().equals(CobbleBash.MODID) || !path.startsWith("gym/")) {
            return;
        }

        String gymType = path.substring("gym/".length());
        if (!isElementalGymType(gymType)) {
            return;
        }

        Point point = Points.getById(ResourceLocation.fromNamespaceAndPath("cobblebadges", gymType));
        if (point != null) {
            PlayerExtensionKt.getCobbleBadgesData(player).setPoints(player, point, 0);
        }

        CobbleBashStats.syncGymsCompleted(player);
    }

    private static void handleBattleStarted(BattleState battleState) {
        PokemonBattle battle = battleState.getBattle();
        if (battle == null) {
            return;
        }

        for (Trainer trainer : battleState.getParticipants2()) {
            if (!(trainer instanceof TrainerNPC trainerNpc)) {
                continue;
            }

            RctApiProbe.GymTrainerRef trainerRef = RctApiProbe.getGymTrainerRef(trainerNpc.getEntity());
            GymTrainerUnit unit = trainerRef == null ? null : GymTrainerUnit.fromTrainerIdPart(trainerRef.trainerIdPart());
            if (unit == null || unit == GymTrainerUnit.TRAINER_ONE || trainerNpc.getTeam().length == 0) {
                continue;
            }

            LivingEntity trainerEntity = trainerNpc.getEntity(battle);
            if (trainerEntity == null) {
                trainerEntity = trainerNpc.getEntity();
            }

            if (trainerEntity == null) {
                continue;
            }

            UUID actorId = trainerEntity.getUUID();
            int baseLevel = trainerNpc.getTeam()[0].getLevel();
            NpcBattleLevelState levelState = new NpcBattleLevelState(actorId, unit, baseLevel, trainerNpc.getTeam().length);
            NPC_BATTLE_LEVELS.put(battle.getBattleId(), levelState);
            applyNpcBattleLevels(battle, levelState);
        }
    }

    private static void handleBattleFainted(com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent event) {
        NpcBattleLevelState levelState = NPC_BATTLE_LEVELS.get(event.getBattle().getBattleId());
        if (levelState == null || event.getKilled().getActor() == null) {
            return;
        }

        if (!levelState.actorId().equals(event.getKilled().getActor().getUuid())) {
            return;
        }

        levelState.incrementFaintedCount();
        applyNpcBattleLevels(event.getBattle(), levelState);
    }

    private static void handleBattleEnded(BattleState battleState) {
        if (battleState.getBattle() != null) {
            NPC_BATTLE_LEVELS.remove(battleState.getBattle().getBattleId());
        }

        for (Trainer winner : battleState.getWinners()) {
            if (winner instanceof TrainerPlayer playerWinner) {
                handlePlayerWon(playerWinner.getPlayer(), battleState.getLosers());
            }
        }

        for (Trainer loser : battleState.getLosers()) {
            if (loser instanceof TrainerPlayer playerLoser) {
                handlePlayerLost(playerLoser.getPlayer(), battleState.getWinners());
            }
        }
    }

    private static void applyNpcBattleLevels(PokemonBattle battle, NpcBattleLevelState levelState) {
        int level = levelState.currentLevel();
        for (BattleActor actor : battle.getActors()) {
            if (!levelState.actorId().equals(actor.getUuid())) {
                continue;
            }

            for (BattlePokemon pokemon : actor.getPokemonList()) {
                if (!pokemon.getEffectedPokemon().isFainted()) {
                    pokemon.getEffectedPokemon().setLevel(level);
                    pokemon.sendUpdate();
                }
            }
        }
    }

    private static void handlePlayerWon(ServerPlayer player, Iterable<Trainer> losers) {
        for (Trainer loser : losers) {
            if (!(loser instanceof TrainerNPC trainerNpc)) {
                continue;
            }

            RctApiProbe.GymTrainerRef trainerRef = RctApiProbe.getGymTrainerRef(trainerNpc.getEntity());
            if (trainerRef == null) {
                continue;
            }

            GymTrainerUnit unit = GymTrainerUnit.fromTrainerIdPart(trainerRef.trainerIdPart());
            if (unit != null) {
                GymCommand.handleTrainerVictory(player, trainerRef.gymType(), trainerRef.slotId(), unit);
            }
        }
    }

    private static void handlePlayerLost(ServerPlayer player, Iterable<Trainer> winners) {
        for (Trainer winner : winners) {
            if (!(winner instanceof TrainerNPC trainerNpc)) {
                continue;
            }

            RctApiProbe.GymTrainerRef trainerRef = RctApiProbe.getGymTrainerRef(trainerNpc.getEntity());
            if (trainerRef != null) {
                GymCommand.clearActiveGym(player, true);
                return;
            }
        }
    }

    private static boolean isElementalGymType(String gymType) {
        for (GymType type : GymType.values()) {
            if (type.getId().equals(gymType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGymBattlePokemon(PokemonEntity pokemon) {
        return pokemon.isBattling() || pokemon.isBattleClone() || pokemon.getBattleId() != null;
    }

    private static boolean cancelBlacklistedGymItem(Player player, ItemStack stack) {
        return player instanceof ServerPlayer serverPlayer && isInGymVoid(serverPlayer) && Config.isGymBlacklisted(stack);
    }

    private static boolean isInGymVoid(ServerPlayer player) {
        return player.level().dimension().equals(CobbleBashDimensions.GYM_VOID);
    }

    private static boolean isGymTrainerEntity(LivingEntity entity) {
        return entity.level().dimension().equals(CobbleBashDimensions.GYM_VOID)
                && entity.getTags().contains(TRAINER_ENTITY_TAG);
    }

    private static void tryLaunchFromPad(ServerPlayer player) {
        if (!isFlyingGymActive(player)
                || !player.onGround()
                || LAUNCH_PAD_COOLDOWNS.getOrDefault(player.getUUID(), 0) > 0) {
            return;
        }

        BlockPos standingOn = BlockPos.containing(player.getX(), player.getY() - 0.05D, player.getZ());
        if (!player.level().getBlockState(standingOn).is(LAUNCH_PADS)) {
            return;
        }

        if (!isLaunchPadActiveForStage(player, standingOn)) {
            return;
        }

        String padId = getLaunchPadId(player, standingOn);
        LaunchPadSettings tuning = getLaunchPadSettings(padId);
        Vec3 horizontalBoost = getLaunchPadHorizontalBoost(player, standingOn, tuning.horizontal());

        player.setDeltaMovement(horizontalBoost.x(), tuning.vertical(), horizontalBoost.z());
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.resetFallDistance();
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        playLaunchPadEffects(player.serverLevel(), standingOn);

        LAUNCH_PAD_ASCENT.put(player.getUUID(), tuning.ticks());
        LAUNCH_PAD_VERTICAL_MOTION.put(player.getUUID(), tuning.vertical());
        LAUNCH_PAD_HORIZONTAL_MOTION.put(player.getUUID(), horizontalBoost);
        LAUNCH_PAD_COOLDOWNS.put(player.getUUID(), tuning.cooldown());
        LAUNCH_PAD_FALL_PROTECTION.put(player.getUUID(), tuning.fallProtection());
    }

    private static void spawnAmbientLaunchPadParticles(ServerPlayer player) {
        if (!isFlyingGymActive(player) || player.tickCount % LAUNCH_PAD_AMBIENT_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-LAUNCH_PAD_AMBIENT_RADIUS, -2, -LAUNCH_PAD_AMBIENT_RADIUS);
        BlockPos max = center.offset(LAUNCH_PAD_AMBIENT_RADIUS, 1, LAUNCH_PAD_AMBIENT_RADIUS);
        int spawnedPads = 0;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).is(LAUNCH_PADS)) {
                continue;
            }

            if (!isLaunchPadActiveForStage(player, pos)) {
                continue;
            }

            double x = pos.getX() + 0.5D + (player.getRandom().nextDouble() - 0.5D) * LAUNCH_PAD_AMBIENT_POSITION_SPREAD;
            double y = pos.getY() + 1.08D;
            double z = pos.getZ() + 0.5D + (player.getRandom().nextDouble() - 0.5D) * LAUNCH_PAD_AMBIENT_POSITION_SPREAD;
            double xSpeed = (player.getRandom().nextDouble() - 0.5D) * LAUNCH_PAD_AMBIENT_HORIZONTAL_SPEED;
            double ySpeed = LAUNCH_PAD_AMBIENT_MIN_UPWARD_SPEED
                    + player.getRandom().nextDouble() * LAUNCH_PAD_AMBIENT_UPWARD_SPEED_VARIANCE;
            double zSpeed = (player.getRandom().nextDouble() - 0.5D) * LAUNCH_PAD_AMBIENT_HORIZONTAL_SPEED;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0D);

            spawnedPads++;
            if (spawnedPads >= LAUNCH_PAD_AMBIENT_MAX_PADS) {
                return;
            }
        }
    }

    private static void playLaunchPadEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.05D;
        double z = pos.getZ() + 0.5D;

        level.playSound(null, x, y, z, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.9F, 1.1F);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.08D, z, 12, 0.22D, 0.08D, 0.22D, 0.06D);
    }

    private static boolean isFlyingGymActive(ServerPlayer player) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        return instance != null && FLYING_GYM_TYPE.equals(instance.getGymType());
    }

    private static boolean isLaunchPadActiveForStage(ServerPlayer player, BlockPos padPos) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        if (instance == null || !FLYING_GYM_TYPE.equals(instance.getGymType())) {
            return false;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        BlockPos playerSpawn = origin.offset(FLYING_PLAYER_SPAWN_OFFSET);

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return instance.getTrainerStage() >= 1;
        }

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET))) {
            return instance.getTrainerStage() >= 2;
        }

        return true;
    }

    private static Vec3 getLaunchPadHorizontalBoost(ServerPlayer player, BlockPos padPos, double horizontalBoost) {
        Vec3 direction = getConfiguredLaunchPadDirection(player, padPos);
        if (direction.lengthSqr() <= LAUNCH_PAD_MIN_HORIZONTAL_SPEED_SQR) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x(), 0.0D, look.z());
        }

        if (direction.lengthSqr() <= LAUNCH_PAD_MIN_HORIZONTAL_SPEED_SQR) {
            Vec3 movement = player.getDeltaMovement();
            direction = new Vec3(movement.x(), 0.0D, movement.z());
        }

        return direction.lengthSqr() > LAUNCH_PAD_MIN_HORIZONTAL_SPEED_SQR
                ? direction.normalize().scale(horizontalBoost)
                : Vec3.ZERO;
    }

    private static String getLaunchPadId(ServerPlayer player, BlockPos padPos) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        if (instance == null || !FLYING_GYM_TYPE.equals(instance.getGymType())) {
            return LAUNCH_PAD_SPAWN;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        BlockPos playerSpawn = origin.offset(FLYING_PLAYER_SPAWN_OFFSET);

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return LAUNCH_PAD_TRAINER_ONE;
        }

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET))) {
            return LAUNCH_PAD_TRAINER_TWO;
        }

        return LAUNCH_PAD_SPAWN;
    }

    private static LaunchPadSettings getLaunchPadSettings(String padId) {
        return switch (padId) {
            case LAUNCH_PAD_TRAINER_ONE -> TRAINER_ONE_LAUNCH_PAD_SETTINGS;
            case LAUNCH_PAD_TRAINER_TWO -> TRAINER_TWO_LAUNCH_PAD_SETTINGS;
            default -> SPAWN_LAUNCH_PAD_SETTINGS;
        };
    }

    private static Vec3 getConfiguredLaunchPadDirection(ServerPlayer player, BlockPos padPos) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        if (instance == null || !FLYING_GYM_TYPE.equals(instance.getGymType())) {
            return Vec3.ZERO;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        BlockPos playerSpawn = origin.offset(FLYING_PLAYER_SPAWN_OFFSET);

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_ONE_LAUNCH_PAD_OFFSET))) {
            return new Vec3(-1.0D, 0.0D, 0.0D);
        }

        if (padPos.equals(playerSpawn.offset(FLYING_TRAINER_TWO_LAUNCH_PAD_OFFSET))) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return Vec3.ZERO;
    }

    private static void suppressJumpBoost(ServerPlayer player) {
        MobEffectInstance jumpBoost = player.getEffect(MobEffects.JUMP);
        if (jumpBoost == null) {
            return;
        }

        SUPPRESSED_JUMP_BOOST.putIfAbsent(player.getUUID(), new MobEffectInstance(jumpBoost));
        player.removeEffect(MobEffects.JUMP);
    }

    private static void restoreSuppressedJumpBoost(ServerPlayer player) {
        MobEffectInstance jumpBoost = SUPPRESSED_JUMP_BOOST.remove(player.getUUID());
        if (jumpBoost != null && (jumpBoost.isInfiniteDuration() || jumpBoost.getDuration() > 0)) {
            player.addEffect(jumpBoost);
        }
    }

    private static void tickSuppressedJumpBoost(ServerPlayer player) {
        MobEffectInstance jumpBoost = SUPPRESSED_JUMP_BOOST.get(player.getUUID());
        if (jumpBoost != null && !jumpBoost.tick(player, () -> { })) {
            SUPPRESSED_JUMP_BOOST.remove(player.getUUID());
        }
    }

    private static void clampGymJumpBoost(ServerPlayer player) {
        if (isLaunchPadMovementAllowed(player)) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y() <= MAX_GYM_NORMAL_UPWARD_SPEED) {
            return;
        }

        player.setDeltaMovement(movement.x(), MAX_GYM_NORMAL_UPWARD_SPEED, movement.z());
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void tickLaunchPadState(ServerPlayer player) {
        decrementOrRemove(LAUNCH_PAD_COOLDOWNS, player.getUUID());
        if (decrementOrRemove(LAUNCH_PAD_FALL_PROTECTION, player.getUUID())) {
            player.resetFallDistance();
        }
    }

    private static void applyLaunchPadAscent(ServerPlayer player) {
        Integer ticks = LAUNCH_PAD_ASCENT.get(player.getUUID());
        if (ticks == null) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        double vertical = LAUNCH_PAD_VERTICAL_MOTION.getOrDefault(player.getUUID(), LAUNCH_PAD_VERTICAL_BOOST);
        Vec3 horizontal = LAUNCH_PAD_HORIZONTAL_MOTION.getOrDefault(player.getUUID(), Vec3.ZERO);
        double xMovement = horizontal.lengthSqr() > 0.0D ? horizontal.x() : movement.x();
        double zMovement = horizontal.lengthSqr() > 0.0D ? horizontal.z() : movement.z();
        player.setDeltaMovement(xMovement, vertical, zMovement);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.resetFallDistance();
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        if (ticks <= 1 || player.horizontalCollision) {
            LAUNCH_PAD_ASCENT.remove(player.getUUID());
            LAUNCH_PAD_VERTICAL_MOTION.remove(player.getUUID());
            LAUNCH_PAD_HORIZONTAL_MOTION.remove(player.getUUID());
        } else {
            LAUNCH_PAD_ASCENT.put(player.getUUID(), ticks - 1);
        }
    }

    private static boolean decrementOrRemove(Map<UUID, Integer> map, UUID playerId) {
        Integer ticks = map.get(playerId);
        if (ticks == null) {
            return false;
        }

        if (ticks <= 1) {
            map.remove(playerId);
        } else {
            map.put(playerId, ticks - 1);
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
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        GymCommand.clearActiveGym(player, true);
        restoreSuppressedJumpBoost(player);
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
            return actorId;
        }

        private void incrementFaintedCount() {
            faintedCount++;
        }

        private int currentLevel() {
            if (unit == GymTrainerUnit.TRAINER_TWO && faintedCount >= teamSize - 1) {
                return baseLevel + 3;
            }

            if (unit == GymTrainerUnit.BOSS) {
                if (faintedCount >= teamSize - 1) {
                    return baseLevel + 5;
                }

                if (faintedCount >= teamSize - 2) {
                    return baseLevel + 3;
                }
            }

            return baseLevel;
        }
    }

    private record LaunchPadSettings(double vertical, double horizontal, int ticks, int cooldown, int fallProtection) {
    }
}
