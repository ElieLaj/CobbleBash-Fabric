package com.nore.cobblebash.command;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.beacon.ChampionBeaconAuras;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.gym.GymLevelOverride;
import com.nore.cobblebash.gym.GymLevelSystem;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.instance.GymSlotPosition;
import com.nore.cobblebash.integration.CobbleDollarsCompat;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.progress.GymCacheMigrationData;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.GymReturnData;
import com.nore.cobblebash.progress.GymRewardData;
import com.nore.cobblebash.progress.PlayerGymProgress;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.structure.EliteFourStructure;
import com.nore.cobblebash.structure.GymDoorController;
import com.nore.cobblebash.structure.GymPlatformBuilder;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootParams;

public class GymCommand {
   private static final Map<String, UUID> DEBUG_SLOT_RESERVATIONS = new HashMap<>();

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("cobblebash")
         .requires(source -> source.hasPermission(2));
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder1 = Commands.literal("gym");
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder2 = Commands.literal("enter");

      for (GymType gymtype : GymType.values()) {
         literalargumentbuilder2.then(Commands.literal(gymtype.getId()).executes(context -> enterGym((CommandSourceStack)context.getSource(), gymtype.getId())));
      }

      literalargumentbuilder2.then(Commands.literal("elite4").executes(context -> enterEliteFour((CommandSourceStack)context.getSource(), false)));
      literalargumentbuilder1.then(literalargumentbuilder2);
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder3 = Commands.literal("battle");
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder4 = Commands.literal("defeat");

      for (GymType gymtype1 : GymType.values()) {
         literalargumentbuilder3.then(trainerTarget(gymtype1, GymCommand::startTrainerBattle));
         literalargumentbuilder4.then(trainerTarget(gymtype1, GymCommand::defeatTrainer));
      }

      literalargumentbuilder1.then(literalargumentbuilder3);
      literalargumentbuilder1.then(literalargumentbuilder4);
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder5 = Commands.literal("complete");

      for (GymType gymtype2 : GymType.values()) {
         literalargumentbuilder5.then(
            Commands.literal(gymtype2.getId()).executes(context -> completeGym((CommandSourceStack)context.getSource(), gymtype2.getId()))
         );
      }

      literalargumentbuilder1.then(literalargumentbuilder5);
      literalargumentbuilder1.then(Commands.literal("advance").executes(context -> advanceGym((CommandSourceStack)context.getSource())));

      // Victoire sur la Ligue sans la rejouer : meme effets que la vraie, pour
      // tester ce qui en depend (avancement, quete, recompenses).
      literalargumentbuilder1.then(
         Commands.literal("league")
            .executes(context -> winLeague(context.getSource(), List.of(context.getSource().getPlayerOrException())))
            .then(Commands.argument("cibles", EntityArgument.players())
               .executes(context -> winLeague(context.getSource(), EntityArgument.getPlayers(context, "cibles"))))
      );

      literalargumentbuilder1.then(lootBagNode());

      // Accorde les dix-huit badges de type. Le palier de base de chaque badge
      // s'obtient en possedant l'avancement correspondant : on coche donc les
      // avancements, et CobbleBadges suit au tick suivant, par le meme chemin
      // qu'une arene reellement terminee.
      literalargumentbuilder1.then(
         Commands.literal("badges")
            .executes(context -> grantTypeBadges(context.getSource(), List.of(context.getSource().getPlayerOrException())))
            .then(Commands.argument("cibles", EntityArgument.players())
               .executes(context -> grantTypeBadges(context.getSource(), EntityArgument.getPlayers(context, "cibles"))))
      );

      com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> racine =
         dispatcher.register((LiteralArgumentBuilder)literalargumentbuilder.then(literalargumentbuilder1));

      // `/gym ...` existait avant la 0.1.3, qui a tout deplace sous
      // `/cobblebash`. Une redirection rend l'ancienne forme sans dupliquer
      // l'arbre : les deux pointent sur le meme noeud.
      dispatcher.register(
         Commands.literal("gym")
            .requires(source -> source.hasPermission(2))
            .redirect(racine.getChild("gym"))
      );
   }

   /**
    * Accorde les dix-huit badges de type, sans toucher au Conseil 4.
    *
    * <p>La boucle ne connait que {@link GymType} : tout autre badge est hors
    * d'atteinte par construction, y compris ceux qui viendraient plus tard.
    */
   private static int grantTypeBadges(CommandSourceStack source, java.util.Collection<ServerPlayer> targets) {
      net.minecraft.server.ServerAdvancementManager manager = source.getServer().getAdvancements();
      int total = 0;

      for (ServerPlayer player : targets) {
         int granted = 0;
         int alreadyOwned = 0;

         awardEveryCriterion(player, manager.get(ResourceLocation.fromNamespaceAndPath("cobblebash", "gym/root")));

         for (GymType type : GymType.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cobblebash", "gym/complete_gym/" + type.getId());
            net.minecraft.advancements.AdvancementHolder holder = manager.get(id);

            if (holder == null) {
               source.sendFailure(Component.literal("Advancement missing: " + id));
               continue;
            }

            if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
               alreadyOwned++;
               continue;
            }

            awardEveryCriterion(player, holder);
            granted++;
         }

         CobbleBashStats.syncGymsCompleted(player);
         total += granted;

         String name = player.getGameProfile().getName();
         int g = granted;
         int o = alreadyOwned;
         source.sendSuccess(
            () -> Component.literal(name + ": granted " + g + " type badge(s), " + o + " already owned. Elite Four untouched."),
            true
         );
      }

      return total;
   }

   /** Un niveau par palier du sac de butin : de quoi tous les voir d'un coup. */
   private static final int[] PALIERS_TEMOINS = {10, 25, 45, 65, 85, 100};

   /**
    * `/cobblebash gym lootbag` : des sacs de butin sans passer par l'arene.
    *
    * <p>Le type est un litteral par valeur plutot qu'une chaine libre : la
    * completion le propose, et un type invalide ne franchit pas l'analyse.
    */
   private static LiteralArgumentBuilder<CommandSourceStack> lootBagNode() {
      LiteralArgumentBuilder<CommandSourceStack> tous = Commands.literal("all")
         .executes(context -> giveBags(context.getSource(), PALIERS_TEMOINS, ""));

      RequiredArgumentBuilder<CommandSourceStack, Integer> niveau =
         Commands.argument("niveau", IntegerArgumentType.integer(1, GymLevelOverride.MAX))
            .executes(context -> giveBags(context.getSource(),
               new int[]{IntegerArgumentType.getInteger(context, "niveau")}, ""));

      for (GymType type : GymType.values()) {
         String id = type.getId();
         tous.then(Commands.literal(id).executes(context -> giveBags(context.getSource(), PALIERS_TEMOINS, id)));
         niveau.then(Commands.literal(id).executes(context -> giveBags(context.getSource(),
            new int[]{IntegerArgumentType.getInteger(context, "niveau")}, id)));
      }

      return Commands.literal("lootbag").then(tous).then(niveau);
   }

   private static int giveBags(CommandSourceStack source, int[] niveaux, String type) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();

      // Sans type precise, on en tire un au hasard plutot que d'en laisser le
      // sac depourvu : aucun sac obtenu en jeu n'est sans type, et un sac nu
      // ne contiendrait jamais d'oeuf — de quoi croire a une panne.
      GymType[] types = GymType.values();
      String choisi = type.isEmpty() ? types[player.getRandom().nextInt(types.length)].getId() : type;

      for (int niveau : niveaux) {
         giveOrDrop(player, com.nore.cobblebash.item.GymLootBagItem.forGym(niveau, choisi));
      }

      String resume = niveaux.length + " gym loot bag(s), type: " + choisi
         + (type.isEmpty() ? " (picked at random)" : "");
      source.sendSuccess(() -> Component.literal("Gave " + resume + "."), false);
      return niveaux.length;
   }

   /**
    * `/cobblebash gym league` : la victoire sur la Ligue sans la rejouer.
    *
    * <p>Passe par le declencheur d'avancement, comme la vraie victoire, puis
    * coche ce qui resterait : c'est l'avancement que la quete du modpack
    * observe, pas le combat.
    */
   private static int winLeague(CommandSourceStack source, java.util.Collection<ServerPlayer> targets) {
      net.minecraft.server.ServerAdvancementManager manager = source.getServer().getAdvancements();
      net.minecraft.advancements.AdvancementHolder holder =
         manager.get(ResourceLocation.fromNamespaceAndPath("cobblebash", "gym/complete_elite_four"));

      if (holder == null) {
         source.sendFailure(Component.literal("Advancement missing: cobblebash:gym/complete_elite_four"));
         return 0;
      }

      for (ServerPlayer player : targets) {
         boolean deja = player.getAdvancements().getOrStartProgress(holder).isDone();
         CobbleBashCriteriaTriggers.triggerEliteFourCompleted(player);
         awardEveryCriterion(player, holder);

         if (!deja) {
            giveOrDrop(player, new ItemStack((ItemLike)CobbleBash.CHAMPION_UPGRADE_SMITHING_TEMPLATE));
         }

         String name = player.getGameProfile().getName();
         boolean d = deja;
         source.sendSuccess(
            () -> Component.literal(name + ": league cleared" + (d ? " (already owned, no template given)." : " + Champion Upgrade Smithing Template.")),
            true
         );
      }

      return targets.size();
   }

   /** Coche tous les criteres restants : c'est ce qui valide l'avancement. */
   private static void awardEveryCriterion(ServerPlayer player, net.minecraft.advancements.AdvancementHolder holder) {
      if (holder == null) {
         return;
      }

      net.minecraft.server.PlayerAdvancements advancements = player.getAdvancements();
      // Copie d'abord : `award` modifie la progression qu'on est en train de lire.
      List<String> remaining = new ArrayList<>();
      advancements.getOrStartProgress(holder).getRemainingCriteria().forEach(remaining::add);

      for (String criterion : remaining) {
         advancements.award(holder, criterion);
      }
   }

   private static int enterGym(CommandSourceStack source, String gymType) {
      return enterGym(source.getPlayer(), source, gymType);
   }

   public static boolean enterGym(ServerPlayer player, String gymType) {
      return enterGym(player, null, gymType) > 0;
   }

   public static boolean enterEliteFour(ServerPlayer player, boolean bypassRequirements) {
      return enterEliteFour(player, null, bypassRequirements) > 0;
   }

   private static int enterGym(ServerPlayer player, CommandSourceStack source, String gymType) {
      ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
      if (serverlevel == null) {
         sendFailure(player, source, "CobbleBash gym dimension was not found.");
         return 0;
      } else {
         PlayerGymProgress playergymprogress = GymProgressManager.get(player.getUUID());
         clearActiveGym(player, true, true);
         boolean flag = playergymprogress.hasCompleted(gymType);
         GameType gametype = player.gameMode.getGameModeForPlayer();
         int[] aint = com.nore.cobblebash.gym.GymLevelOverride.trainerLevels(
            player.getUUID(), playergymprogress.getCompletedGymCount());
         GymReturnData.ReturnLocation gymreturndata$returnlocation = GymReturnData.ReturnLocation.from(
            (ServerLevel)player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
         );
         GymInstance gyminstance = GymInstanceManager.createOrGet(
            player.getUUID(),
            gymType,
            flag,
            aint,
            gymreturndata$returnlocation.dimension(),
            gymreturndata$returnlocation.x(),
            gymreturndata$returnlocation.y(),
            gymreturndata$returnlocation.z(),
            gymreturndata$returnlocation.yRot(),
            gymreturndata$returnlocation.xRot(),
            gametype
         );
         GymReturnData.get(player.server).put(player.getUUID(), gymreturndata$returnlocation);
         playergymprogress.setActiveGymType(gyminstance.getGymType());
         int[] aint1 = gyminstance.getTrainerLevels();
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         BlockPos blockpos1 = GymPlatformBuilder.getPlayerSpawn(blockpos, gyminstance.getGymType());
         GymCacheMigrationData.get(player.server).migrateGymSlotIfNeeded(serverlevel, gyminstance.getGymType(), gyminstance.getSlotId(), blockpos);
         GymPlatformBuilder.buildGym(serverlevel, blockpos, gyminstance.getGymType(), gyminstance.getSlotId(), aint1);
         player.teleportTo(
            serverlevel,
            blockpos1.getX() + 0.5,
            blockpos1.getY(),
            blockpos1.getZ() + 0.5,
            GymPlatformBuilder.getPlayerSpawnYaw(gyminstance.getGymType(), player.getYRot()),
            GymPlatformBuilder.getPlayerSpawnPitch(gyminstance.getGymType(), player.getXRot())
         );
         player.setGameMode(GameType.ADVENTURE);
         CobbleBashCriteriaTriggers.triggerGymEntered(player);
         return 1;
      }
   }

   private static int enterEliteFour(CommandSourceStack source, boolean bypassRequirements) {
      return enterEliteFour(source.getPlayer(), source, bypassRequirements);
   }

   private static int enterEliteFour(ServerPlayer player, CommandSourceStack source, boolean bypassRequirements) {
      ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
      if (serverlevel == null) {
         sendFailure(player, source, "CobbleBash gym dimension was not found.");
         return 0;
      } else {
         PlayerGymProgress playergymprogress = GymProgressManager.get(player.getUUID());
         if (!bypassRequirements && !hasCompletedAllElementalGyms(playergymprogress)) {
            sendFailure(player, source, "The Elite Four requires all 18 type gyms to be completed first.");
            return 0;
         } else {
            clearActiveGym(player, true, true);
            GameType gametype = player.gameMode.getGameModeForPlayer();
            GymReturnData.ReturnLocation gymreturndata$returnlocation = GymReturnData.ReturnLocation.from(
               (ServerLevel)player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
            );
            GymInstance gyminstance = GymInstanceManager.createOrGet(
               player.getUUID(),
               "elite4",
               false,
               new int[]{0, 0, 0},
               gymreturndata$returnlocation.dimension(),
               gymreturndata$returnlocation.x(),
               gymreturndata$returnlocation.y(),
               gymreturndata$returnlocation.z(),
               gymreturndata$returnlocation.yRot(),
               gymreturndata$returnlocation.xRot(),
               gametype
            );
            GymReturnData.get(player.server).put(player.getUUID(), gymreturndata$returnlocation);
            playergymprogress.setActiveGymType("elite4");
            BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
            GymCacheMigrationData.get(player.server).migrateGymSlotIfNeeded(serverlevel, "elite4", gyminstance.getSlotId(), blockpos);
            EliteFourStructure.build(serverlevel, blockpos, gyminstance.getSlotId());
            BlockPos blockpos1 = EliteFourStructure.getPlayerSpawn(serverlevel, blockpos);
            player.teleportTo(serverlevel, blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5, 180.0F, 0.0F);
            player.setGameMode(GameType.ADVENTURE);
            return 1;
         }
      }
   }

   private static int completeGym(CommandSourceStack source, String gymType) {
      return completeGym(source.getPlayer(), gymType, source);
   }

   private static int completeGym(ServerPlayer player, String gymType, CommandSourceStack source) {
      PlayerGymProgress playergymprogress = GymProgressManager.get(player.getUUID());
      if (!playergymprogress.isActiveGym(gymType)) {
         sendFailure(player, source, "Cannot complete " + gymType + " gym because your active gym is " + playergymprogress.getActiveGymType() + ".");
         return 0;
      } else {
         boolean flag = playergymprogress.hasCompleted(gymType);
         playergymprogress.completeGym(gymType);
         CobbleBashStats.syncGymsCompleted(player);
         awardTrainerRibbonIfEligible(player, gymType);
         awardEliteFourDiskIfEligible(player, playergymprogress);
         awardGymLootBag(player);
         GymInstance gyminstance = GymInstanceManager.clear(player.getUUID());
         clearInstancePlatform(player, gyminstance);
         teleportToReturnLocation(player, gyminstance);
         GymReturnData.get(player.server).remove(player.getUUID());
         String s = flag ? "repeat rewards" : "first clear rewards + badge";
         String s1 = gyminstance == null ? "none" : String.valueOf(gyminstance.getSlotId());
         sendSuccess(player, source, "Completed " + gymType + " gym. Reward mode: " + s + ". Freed slot: " + s1 + ".");
         return 1;
      }
   }

   private static void awardTrainerRibbonIfEligible(ServerPlayer player, String completedGymType) {
      GymRewardData gymrewarddata = GymRewardData.get(player.server);
      String s = gymrewarddata.getOrSetTrainerRibbonGym(player.getUUID(), completedGymType);
      if (s.equals(completedGymType)) {
         giveOrDrop(player, new ItemStack((ItemLike)CobbleBash.TRAINER_RIBBON));
         player.sendSystemMessage(Component.literal("Received a Trainer Ribbon from the " + completedGymType + " Gym Leader."));
      }
   }

   private static void awardEliteFourDiskIfEligible(ServerPlayer player, PlayerGymProgress progress) {
      if (hasCompletedAllElementalGyms(progress)) {
         GymRewardData gymrewarddata = GymRewardData.get(player.server);
         if (gymrewarddata.markEliteFourDiskAwarded(player.getUUID())) {
            giveOrDrop(player, new ItemStack((ItemLike)CobbleBash.ELITE_FOUR_TRAINING_DISK));
            player.sendSystemMessage(Component.literal("Received an Elite Four Training Disk for conquering all 18 gyms."));
         }
      }
   }

   /**
    * Remet le sac de butin, grave au niveau de l'arene qui vient d'etre battue.
    *
    * <p>Le niveau retenu est celui du dernier dresseur, c'est-a-dire le plus
    * haut affronte. L'instance est encore vivante ici — elle n'est liberee que
    * juste apres — donc le niveau reellement joue est encore lisible, y compris
    * quand il vient d'un choix manuel et non de la progression.
    */
   private static void awardGymLootBag(ServerPlayer player) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      int[] aint = gyminstance == null ? null : gyminstance.getTrainerLevels();

      int level = GymLevelOverride.MIN;
      if (aint != null) {
         for (int niveau : aint) {
            level = Math.max(level, niveau);
         }
      }

      String type = gyminstance == null ? "" : gyminstance.getGymType();
      giveOrDrop(player, com.nore.cobblebash.item.GymLootBagItem.forGym(level, type));
   }

   private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty()) {
         // Le booleen de `add` vaut vrai des qu'un seul exemplaire est case :
         // s'y fier faisait disparaitre le reste sur un inventaire presque
         // plein. Seul l'etat de la pile apres coup fait foi.
         player.getInventory().add(stack);
         if (!stack.isEmpty()) {
            player.drop(stack, false);
         }
      }
   }

   private static int exitGym(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      GymInstance gyminstance = GymInstanceManager.clear(serverplayer.getUUID());
      PlayerGymProgress playergymprogress = GymProgressManager.get(serverplayer.getUUID());
      playergymprogress.setActiveGymType("none");
      if (gyminstance == null) {
         source.sendFailure(Component.literal("You do not have an active gym instance."));
         return 0;
      } else {
         clearInstancePlatform(serverplayer, gyminstance);
         teleportToReturnLocation(serverplayer, gyminstance);
         GymReturnData.get(serverplayer.server).remove(serverplayer.getUUID());
         source.sendSuccess(() -> Component.literal("Exited " + gyminstance.getGymType() + " gym. Freed slot: " + gyminstance.getSlotId() + "."), false);
         return 1;
      }
   }

   private static int leaveGym(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      GymInstance gyminstance = GymInstanceManager.clear(serverplayer.getUUID());
      PlayerGymProgress playergymprogress = GymProgressManager.get(serverplayer.getUUID());
      playergymprogress.setActiveGymType("none");
      clearInstancePlatform(serverplayer, gyminstance);
      teleportToReturnLocation(serverplayer, gyminstance);
      GymReturnData.get(serverplayer.server).remove(serverplayer.getUUID());
      source.sendSuccess(() -> Component.literal("Returned to gym entry point."), false);
      return 1;
   }

   private static int advanceGym(CommandSourceStack source) {
      return advanceGym(source.getPlayer(), source);
   }

   private static int advanceGym(ServerPlayer player, CommandSourceStack source) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance == null) {
         sendFailure(player, source, "You do not have an active gym instance.");
         return 0;
      }

      if ("elite4".equals(gyminstance.getGymType())) {
         return advanceEliteFour(player, source, gyminstance);
      }

      boolean flag = gyminstance.advanceTrainerStage();
      if (!flag) {
         sendFailure(player, source, "Gym instance is already cleared.");
         return 0;
      }

      sendSuccess(player, source, "Advanced " + gyminstance.getGymType() + " gym to stage " + gyminstance.getTrainerStage() + ".");
      ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
      if (serverlevel != null) {
         BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
         GymDoorController.openDoorForStage(serverlevel, blockpos, gyminstance.getGymType(), gyminstance.getTrainerStage());
      }

      if (gyminstance.getTrainerStage() == 3) {
         CobbleBashCriteriaTriggers.triggerGymBossDefeated(player, gyminstance.getGymType());
         sendSuccess(player, source, "Boss defeated. Leaving gym in 5 seconds...");
         DelayedTaskScheduler.schedule(100, () -> {
            if (source != null) {
               completeGym(source, gyminstance.getGymType());
            } else {
               completeGym(player, gyminstance.getGymType(), null);
            }
         });
      }

      return 1;
   }

   private static int advanceEliteFour(ServerPlayer player, CommandSourceStack source, GymInstance instance) {
      if (!instance.hasActiveEliteFourMember()) {
         sendFailure(player, source, "Choose an Elite Four plaque first.");
         return 0;
      }

      EliteFourMember elitefourmember = EliteFourMember.fromId(instance.getActiveEliteFourMember());
      if (elitefourmember != null && instance.completeActiveEliteFourMember()) {
         sendSuccess(player, source, "Defeated " + elitefourmember.getDisplayName() + ".");
         if (instance.getDefeatedEliteFourMemberCount() >= EliteFourMember.ordered().size()) {
            instance.unlockEliteFourChampion();
            instance.setEliteFourChampionBeamTicks(-1);
            ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
            if (serverlevel != null) {
               BlockPos blockpos1 = GymSlotPosition.getOriginForSlot(instance.getSlotId());
               EliteFourStructure.openChampionGate(serverlevel, blockpos1);
            }

            sendSuccess(player, source, "All Elite Four members defeated. Champion gate opened.");
            return 1;
         } else {
            List<EliteFourMember> list = new ArrayList<>();

            for (EliteFourMember elitefourmember1 : EliteFourMember.ordered()) {
               if (!instance.hasDefeatedEliteFourMember(elitefourmember1.getId())) {
                  list.add(elitefourmember1);
               }
            }

            EliteFourMember elitefourmember2 = list.get(player.getRandom().nextInt(list.size()));
            instance.selectEliteFourMember(elitefourmember2.getId());
            ServerLevel serverlevel1 = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
            if (serverlevel1 != null) {
               BlockPos blockpos = GymSlotPosition.getOriginForSlot(instance.getSlotId());
               EliteFourStructure.openMemberGate(serverlevel1, blockpos, elitefourmember2);
            }

            sendSuccess(player, source, "Next Elite Four member unlocked: " + elitefourmember2.getDisplayName() + ".");
            return 1;
         }
      } else {
         sendFailure(player, source, "Could not complete the active Elite Four member.");
         return 0;
      }
   }

   private static int registerRct(CommandSourceStack source) {
      RctApiProbe.registerTestTrainer(source.getServer());
      source.sendSuccess(() -> Component.literal("Registered RCT trainer: cobblebash_bug_trainer_1"), false);
      return 1;
   }

   private static int getRct(CommandSourceStack source) {
      TrainerNPC trainernpc = RctApiProbe.getTestTrainer();
      if (trainernpc == null) {
         source.sendFailure(Component.literal("Trainer not found."));
         return 0;
      } else {
         source.sendSuccess(() -> Component.literal("Found trainer: " + trainernpc.getName() + ", team size = " + trainernpc.getTeam().length), false);
         return 1;
      }
   }

   private static int debugRct(CommandSourceStack source) {
      StringBuilder stringbuilder = new StringBuilder("RCT API instances: ");
      RCTApi.getInstances().forEach(entry -> stringbuilder.append("[").append((String)entry.getKey()).append("] "));
      source.sendSuccess(() -> Component.literal(stringbuilder.toString()), false);
      return 1;
   }

   private static int startRctBattle(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      boolean flag = RctApiProbe.startTestBattle(serverplayer);
      if (!flag) {
         source.sendFailure(Component.literal("Failed to start RCT battle."));
         return 0;
      } else {
         source.sendSuccess(() -> Component.literal("Started RCT test battle."), false);
         return 1;
      }
   }

   private static int reserveDebugSlot(CommandSourceStack source, String label) {
      if (DEBUG_SLOT_RESERVATIONS.containsKey(label)) {
         UUID uuid = DEBUG_SLOT_RESERVATIONS.get(label);
         GymInstance gyminstance = GymInstanceManager.getActive(uuid);
         if (gyminstance != null) {
            source.sendFailure(Component.literal("Debug slot label '" + label + "' already reserves slot " + gyminstance.getSlotId() + "."));
            return 0;
         }
      }

      ServerPlayer serverplayer = source.getPlayer();
      UUID uuid1 = UUID.nameUUIDFromBytes(("cobblebash:slot_debug:" + label).getBytes(StandardCharsets.UTF_8));
      GymReturnData.ReturnLocation gymreturndata$returnlocation = GymReturnData.ReturnLocation.from(
         (ServerLevel)serverplayer.level(), serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), serverplayer.getYRot(), serverplayer.getXRot()
      );
      GymInstance gyminstance1 = GymInstanceManager.createOrGet(
         uuid1,
         "slot_debug",
         true,
         new int[]{0, 0, 0},
         gymreturndata$returnlocation.dimension(),
         gymreturndata$returnlocation.x(),
         gymreturndata$returnlocation.y(),
         gymreturndata$returnlocation.z(),
         gymreturndata$returnlocation.yRot(),
         gymreturndata$returnlocation.xRot(),
         serverplayer.gameMode.getGameModeForPlayer()
      );
      DEBUG_SLOT_RESERVATIONS.put(label, uuid1);
      source.sendSuccess(() -> Component.literal("Reserved debug slot " + gyminstance1.getSlotId() + " as '" + label + "'."), false);
      return 1;
   }

   private static int releaseDebugSlot(CommandSourceStack source, String label) {
      UUID uuid = DEBUG_SLOT_RESERVATIONS.remove(label);
      if (uuid == null) {
         source.sendFailure(Component.literal("No debug slot reservation exists for '" + label + "'."));
         return 0;
      } else {
         GymInstance gyminstance = GymInstanceManager.clear(uuid);
         if (gyminstance == null) {
            source.sendFailure(Component.literal("Debug slot label '" + label + "' was tracked, but no active slot was reserved."));
            return 0;
         } else {
            source.sendSuccess(() -> Component.literal("Released debug slot " + gyminstance.getSlotId() + " from '" + label + "'."), false);
            return 1;
         }
      }
   }

   private static int debugSlotStatus(CommandSourceStack source) {
      StringBuilder stringbuilder = new StringBuilder();
      DEBUG_SLOT_RESERVATIONS.forEach((label, playerId) -> {
         GymInstance gyminstance = GymInstanceManager.getActive(playerId);
         if (gyminstance != null) {
            if (!stringbuilder.isEmpty()) {
               stringbuilder.append(", ");
            }

            stringbuilder.append(label).append("=").append(gyminstance.getSlotId());
         }
      });
      source.sendSuccess(
         () -> Component.literal(
            "Slot debug: active instances = "
               + GymInstanceManager.getActiveCount()
               + ", free slots = "
               + GymInstanceManager.getFreeSlotCount()
               + ", next slot id = "
               + GymInstanceManager.getNextSlotId()
               + ", reservations = "
               + (stringbuilder.isEmpty() ? "none" : stringbuilder)
         ),
         false
      );
      return 1;
   }

   private static int startTrainerBattle(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
      return startTrainerBattle(source.getPlayer(), source, gymType.getId(), null, unit);
   }

   public static boolean startTrainerBattle(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
      return startTrainerBattle(player, null, gymType, slotId, unit) > 0;
   }

   private static int startTrainerBattle(ServerPlayer player, CommandSourceStack source, String gymType, Integer requiredSlotId, GymTrainerUnit unit) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance == null) {
         sendFailure(player, source, "You do not have an active gym instance.");
         return 0;
      } else if ("elite4".equals(gyminstance.getGymType())) {
         return startEliteFourTrainerBattle(player, source, gymType, requiredSlotId, unit, gyminstance);
      } else if (!gyminstance.getGymType().equals(gymType)) {
         sendFailure(player, source, "Your active gym is " + gyminstance.getGymType() + ", not " + gymType + ".");
         return 0;
      } else if (requiredSlotId != null && gyminstance.getSlotId() != requiredSlotId) {
         sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + gyminstance.getSlotId() + ".");
         return 0;
      } else if (gyminstance.getTrainerStage() != unit.getRequiredStage()) {
         GymTrainerUnit gymtrainerunit = getExpectedTrainerUnit(gyminstance.getTrainerStage());
         String s = getTrainerDisplayName(player, gymType, unit);
         String s1 = gymtrainerunit == null ? "the previous trainer" : getTrainerDisplayName(player, gymType, gymtrainerunit);
         sendFailure(player, source, "Cannot battle " + s + ". Beat " + s1 + " first.");
         return 0;
      } else {
         int i = gyminstance.getTrainerLevels()[unit.getLevelIndex()];
         ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
         if (serverlevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
         } else {
            BlockPos blockpos = GymSlotPosition.getOriginForSlot(gyminstance.getSlotId());
            if (RctApiProbe.getGymTrainer(gymType, gyminstance.getSlotId(), unit.getTrainerIdPart()) == null
               && !RctApiProbe.registerGymTrainer(player.server, gymType, gyminstance.getSlotId(), unit.getTrainerIdPart(), i)) {
               sendFailure(player, source, "Failed to register " + gymType + " " + unit.getDisplayName() + " trainer.");
               return 0;
            } else {
               GymPlatformBuilder.attachTrainerEntity(serverlevel, blockpos, gymType, gyminstance.getSlotId(), unit.getTrainerIdPart());
               boolean flag = RctApiProbe.startGymBattle(player, gymType, gyminstance.getSlotId(), unit.getTrainerIdPart());
               if (!flag) {
                  sendFailure(player, source, "Failed to start " + gymType + " " + unit.getDisplayName() + " battle.");
                  return 0;
               } else {
                  sendSuccess(player, source, "Started " + gymType + " " + unit.getDisplayName() + " battle.");
                  return 1;
               }
            }
         }
      }
   }

   private static int startEliteFourTrainerBattle(
      ServerPlayer player, CommandSourceStack source, String gymType, Integer requiredSlotId, GymTrainerUnit unit, GymInstance instance
   ) {
      EliteFourMember elitefourmember = EliteFourMember.fromTrainerGymType(gymType);
      if ("elite4_champion".equals(gymType)) {
         return startEliteFourChampionBattle(player, source, requiredSlotId, unit, instance);
      }

      if (elitefourmember != null && unit == GymTrainerUnit.BOSS) {
         if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
            sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
            return 0;
         } else if (!instance.getActiveEliteFourMember().equals(elitefourmember.getId())) {
            sendFailure(player, source, "Complete previous Elite Four member.");
            return 0;
         } else {
            int i = elitefourmember != EliteFourMember.FIRE_FAIRY && elitefourmember != EliteFourMember.WATER_STEEL ? 95 : 98;
            ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
            if (serverlevel == null) {
               sendFailure(player, source, "CobbleBash gym dimension was not found.");
               return 0;
            } else {
               BlockPos blockpos = GymSlotPosition.getOriginForSlot(instance.getSlotId());
               if (RctApiProbe.getGymTrainer(gymType, instance.getSlotId(), unit.getTrainerIdPart()) == null
                  && !RctApiProbe.registerGymTrainer(player.server, gymType, instance.getSlotId(), unit.getTrainerIdPart(), i)) {
                  sendFailure(player, source, "Failed to register " + elitefourmember.getDisplayName() + " Elite Four trainer.");
                  return 0;
               } else {
                  GymPlatformBuilder.attachTrainerEntity(serverlevel, blockpos, gymType, instance.getSlotId(), unit.getTrainerIdPart());
                  boolean flag = RctApiProbe.startGymBattle(player, gymType, instance.getSlotId(), unit.getTrainerIdPart());
                  if (!flag) {
                     sendFailure(player, source, "Failed to start " + elitefourmember.getDisplayName() + " Elite Four battle.");
                     return 0;
                  } else {
                     sendSuccess(player, source, "Started " + elitefourmember.getDisplayName() + " Elite Four battle.");
                     return 1;
                  }
               }
            }
         }
      } else {
         sendFailure(player, source, "That trainer is not part of the active Elite Four challenge.");
         return 0;
      }
   }

   private static int startEliteFourChampionBattle(
      ServerPlayer player, CommandSourceStack source, Integer requiredSlotId, GymTrainerUnit unit, GymInstance instance
   ) {
      if (unit != GymTrainerUnit.BOSS) {
         sendFailure(player, source, "That trainer is not the Elite Four Champion.");
         return 0;
      } else if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
         sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
         return 0;
      } else if (!instance.isEliteFourChampionUnlocked()) {
         sendFailure(player, source, "Defeat all four Elite Four members first.");
         return 0;
      } else {
         ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
         if (serverlevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
         } else {
            BlockPos blockpos = GymSlotPosition.getOriginForSlot(instance.getSlotId());
            if (RctApiProbe.getGymTrainer("elite4_champion", instance.getSlotId(), unit.getTrainerIdPart()) == null
               && !RctApiProbe.registerGymTrainer(player.server, "elite4_champion", instance.getSlotId(), unit.getTrainerIdPart(), 100)) {
               sendFailure(player, source, "Failed to register Elite Four Champion trainer.");
               return 0;
            } else {
               GymPlatformBuilder.attachTrainerEntity(serverlevel, blockpos, "elite4_champion", instance.getSlotId(), unit.getTrainerIdPart());
               boolean flag = RctApiProbe.startGymBattle(player, "elite4_champion", instance.getSlotId(), unit.getTrainerIdPart());
               if (!flag) {
                  sendFailure(player, source, "Failed to start Elite Four Champion battle.");
                  return 0;
               } else {
                  sendSuccess(player, source, "Started Elite Four Champion battle.");
                  return 1;
               }
            }
         }
      }
   }

   private static int defeatTrainer(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
      ServerPlayer serverplayer = source.getPlayer();
      GymInstance gyminstance = GymInstanceManager.getActive(serverplayer.getUUID());
      if (gyminstance == null) {
         source.sendFailure(Component.literal("You do not have an active gym instance."));
         return 0;
      } else if (!gyminstance.getGymType().equals(gymType.getId())) {
         source.sendFailure(Component.literal("Your active gym is " + gyminstance.getGymType() + ", not " + gymType.getId() + "."));
         return 0;
      } else if (gyminstance.getTrainerStage() != unit.getRequiredStage()) {
         source.sendFailure(
            Component.literal(
               "Cannot defeat "
                  + unit.getDisplayName()
                  + " at stage "
                  + gyminstance.getTrainerStage()
                  + ". Expected "
                  + getExpectedTrainerName(gyminstance.getTrainerStage())
                  + "."
            )
         );
         return 0;
      } else {
         return advanceGym(source);
      }
   }

   public static void handleTrainerVictory(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
      GymInstance gyminstance = GymInstanceManager.getActive(player.getUUID());
      if (gyminstance != null && "elite4".equals(gyminstance.getGymType())) {
         handleEliteFourTrainerVictory(player, gymType, slotId, unit, gyminstance);
      } else if (gyminstance != null
         && gyminstance.getGymType().equals(gymType)
         && gyminstance.getSlotId() == slotId
         && gyminstance.getTrainerStage() == unit.getRequiredStage()) {
         awardCobbleDollarsForTrainerVictory(player, gyminstance, unit);
         advanceGym(player, null);
      }
   }

   private static void handleEliteFourTrainerVictory(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit, GymInstance instance) {
      if ("elite4_champion".equals(gymType) && unit == GymTrainerUnit.BOSS && instance.getSlotId() == slotId && instance.isEliteFourChampionUnlocked()) {
         giveOrDrop(player, new ItemStack((ItemLike)CobbleBash.CHAMPION_UPGRADE_SMITHING_TEMPLATE));
         player.sendSystemMessage(Component.literal("Received a Champion Upgrade Smithing Template."));
         // Seul endroit ou le Conseil 4 est reellement termine.
         CobbleBashCriteriaTriggers.triggerEliteFourCompleted(player);
      } else {
         EliteFourMember elitefourmember = EliteFourMember.fromTrainerGymType(gymType);
         if (elitefourmember != null
            && unit == GymTrainerUnit.BOSS
            && instance.getSlotId() == slotId
            && instance.getActiveEliteFourMember().equals(elitefourmember.getId())) {
            playEliteFourVictoryAdvance(player);
         }
      }
   }

   private static void playEliteFourVictoryAdvance(ServerPlayer player) {
      advanceGym(player, null);
   }

   private static void awardCobbleDollarsForTrainerVictory(ServerPlayer player, GymInstance instance, GymTrainerUnit unit) {
      if (instance.isRepeatClear()) {
         int i = unit == GymTrainerUnit.BOSS
            ? Config.cobbleDollarsRepeatBossReward()
            : Config.cobbleDollarsRepeatTrainerReward();
         if (CobbleDollarsCompat.award(player, i)) {
            player.sendSystemMessage(Component.translatable("message.cobblebash.cobble_dollars_reward", new Object[]{i}));
         }
      }
   }

   public static void clearActiveGym(ServerPlayer player, boolean teleport) {
      clearActiveGym(player, teleport, teleport);
   }

   public static void clearActiveGym(ServerPlayer player, boolean teleport, boolean consumeSavedReturn) {
      GymInstance gyminstance = GymInstanceManager.clear(player.getUUID());
      PlayerGymProgress playergymprogress = GymProgressManager.get(player.getUUID());
      playergymprogress.setActiveGymType("none");
      clearInstancePlatform(player, gyminstance);
      if (teleport && player.level().dimension().equals(CobbleBashDimensions.GYM_VOID)) {
         teleportToReturnLocation(player, gyminstance);
      } else if (gyminstance != null) {
         restoreReturnGameMode(player, gyminstance);
      }

      if (consumeSavedReturn) {
         GymReturnData.get(player.server).remove(player.getUUID());
      }
   }

   private static int debugProgress(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      PlayerGymProgress playergymprogress = GymProgressManager.get(serverplayer.getUUID());
      GymInstance gyminstance = GymInstanceManager.getActive(serverplayer.getUUID());
      int[] aint = GymLevelSystem.getTrainerLevels(playergymprogress.getCompletedGymCount());
      String s = gyminstance == null
         ? "none"
         : "slot "
            + gyminstance.getSlotId()
            + ", type "
            + gyminstance.getGymType()
            + ", stage "
            + gyminstance.getTrainerStage()
            + ", origin "
            + formatPos(GymSlotPosition.getOriginForSlot(gyminstance.getSlotId()));
      source.sendSuccess(
         () -> Component.literal(
            "CobbleBash debug: completed gyms = "
               + playergymprogress.getCompletedGymCount()
               + ", active gym = "
               + playergymprogress.getActiveGymType()
               + ", trainer levels = {"
               + aint[0]
               + ", "
               + aint[1]
               + ", "
               + aint[2]
               + "}, active instance = "
               + s
               + ", active instances = "
               + GymInstanceManager.getActiveCount()
               + ", free slots = "
               + GymInstanceManager.getFreeSlotCount()
               + ", next slot id = "
               + GymInstanceManager.getNextSlotId()
         ),
         false
      );
      return 1;
   }

   private static int debugBeaconAuras(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      ChampionBeaconAuras.PlayerDebugInfo championbeaconauras$playerdebuginfo = ChampionBeaconAuras.debugForPlayer(serverplayer);
      String s = championbeaconauras$playerdebuginfo.auraInfo().powers().isEmpty()
         ? "none"
         : championbeaconauras$playerdebuginfo.auraInfo().powers().stream().map(Enum::name).reduce((left, right) -> left + ", " + right).orElse("none");
      String s1 = championbeaconauras$playerdebuginfo.activeExtraShinyChance() <= 0.0
         ? "inactive"
         : "active +" + formatOneIn(championbeaconauras$playerdebuginfo.activeExtraShinyChance());
      String s2 = championbeaconauras$playerdebuginfo.activeExtraShinyChance() <= 0.0
         ? "base only"
         : formatEstimatedCombinedShiny(championbeaconauras$playerdebuginfo.cobblemonShinyRate(), championbeaconauras$playerdebuginfo.activeExtraShinyChance());
      source.sendSuccess(
         () -> Component.literal(
            "Champion Beacon aura debug at "
               + formatPos(serverplayer.blockPosition())
               + ": active beacons in range = "
               + championbeaconauras$playerdebuginfo.auraInfo().activeBeacons()
               + ", powers = "
               + s
               + ", player spawner active = "
               + championbeaconauras$playerdebuginfo.spawnerActive()
               + ", spawn timer = "
               + formatFloat(championbeaconauras$playerdebuginfo.ticksUntilNextSpawn())
               + "/"
               + formatFloat(championbeaconauras$playerdebuginfo.ticksBetweenSpawnAttempts())
               + " ticks, base progress/tick = "
               + formatFloat(championbeaconauras$playerdebuginfo.baseProgressPerTick())
               + ", lure active = "
               + championbeaconauras$playerdebuginfo.lureActive()
               + ", lure bonus progress/tick = "
               + formatFloat(championbeaconauras$playerdebuginfo.lureProgressPerTick())
               + ", effective interval = "
               + formatFloat(championbeaconauras$playerdebuginfo.effectiveTicksBetweenSpawnAttempts())
               + " ticks ("
               + formatFloat(championbeaconauras$playerdebuginfo.effectiveTicksBetweenSpawnAttempts() / 20.0F)
               + "s), Cobblemon shinyRate = "
               + formatFloat(championbeaconauras$playerdebuginfo.cobblemonShinyRate())
               + ", shiny aura = "
               + s1
               + ", estimated combined shiny = "
               + s2
         ),
         false
      );
      return 1;
   }

   private static int toggleBeaconPulseDebug(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      boolean flag = ChampionBeaconAuras.togglePulseDebug(serverplayer);
      source.sendSuccess(() -> Component.literal("Champion Beacon pulse debug " + (flag ? "enabled" : "disabled") + "."), false);
      return 1;
   }

   private static int visualizeBeaconAuras(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      ChampionBeaconAuras.VisualizationInfo championbeaconauras$visualizationinfo = ChampionBeaconAuras.visualizeForPlayer(serverplayer);
      source.sendSuccess(
         () -> Component.literal(
            "Champion Beacon visualization: active beacons in range = "
               + championbeaconauras$visualizationinfo.activeBeacons()
               + ", cached apricorns = "
               + championbeaconauras$visualizationinfo.apricorns()
               + ", cached berries = "
               + championbeaconauras$visualizationinfo.berries()
               + ", pastures = "
               + championbeaconauras$visualizationinfo.pastures()
               + ". Green particles mark cached crops and detected pastures."
         ),
         false
      );
      return 1;
   }

   private static void teleportToSpawn(ServerPlayer player) {
      ServerLevel serverlevel = player.server.overworld();
      BlockPos blockpos = serverlevel.getSharedSpawnPos();
      player.teleportTo(serverlevel, blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, player.getYRot(), player.getXRot());
   }

   private static void teleportToReturnLocation(ServerPlayer player, GymInstance instance) {
      if (instance == null) {
         GymReturnData.get(player.server)
            .remove(player.getUUID())
            .ifPresentOrElse(location -> teleportToReturnLocation(player, location), () -> teleportToSpawn(player));
      } else {
         teleportToReturnLocation(
            player,
            new GymReturnData.ReturnLocation(
               instance.getReturnDimension(),
               instance.getReturnX(),
               instance.getReturnY(),
               instance.getReturnZ(),
               instance.getReturnYRot(),
               instance.getReturnXRot()
            )
         );
         restoreReturnGameMode(player, instance);
      }
   }

   public static void teleportToSavedReturnOrSpawn(ServerPlayer player) {
      GymReturnData.get(player.server)
         .remove(player.getUUID())
         .ifPresentOrElse(location -> teleportToReturnLocation(player, location), () -> teleportToSpawn(player));
   }

   private static void teleportToReturnLocation(ServerPlayer player, GymReturnData.ReturnLocation location) {
      ServerLevel serverlevel = player.server.getLevel(location.dimension());
      if (serverlevel != null && !serverlevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
         player.teleportTo(serverlevel, location.x(), location.y(), location.z(), location.yRot(), location.xRot());
      } else {
         teleportToSpawn(player);
      }
   }

   private static String formatPos(BlockPos pos) {
      return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
   }

   private static String formatFloat(float value) {
      return Float.isInfinite(value) ? "infinite" : String.format(Locale.ROOT, "%.2f", value);
   }

   private static String formatOneIn(double chance) {
      return chance <= 0.0 ? "0" : "1/" + Math.round(1.0 / chance);
   }

   private static String formatEstimatedCombinedShiny(float cobblemonShinyRate, double extraChance) {
      if (cobblemonShinyRate <= 0.0F) {
         return formatOneIn(extraChance);
      }

      double d0 = 1.0 / cobblemonShinyRate;
      double d1 = d0 + (1.0 - d0) * extraChance;
      return formatOneIn(d1);
   }

   private static boolean hasCompletedAllElementalGyms(PlayerGymProgress progress) {
      for (GymType gymtype : GymType.values()) {
         if (!progress.hasCompleted(gymtype.getId())) {
            return false;
         }
      }

      return true;
   }

   private static void clearInstancePlatform(ServerPlayer player, GymInstance instance) {
      if (instance != null) {
         ServerLevel serverlevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
         if (serverlevel != null) {
            BlockPos blockpos = GymSlotPosition.getOriginForSlot(instance.getSlotId());
            if ("elite4".equals(instance.getGymType())) {
               EliteFourStructure.clear(serverlevel, blockpos);
            } else {
               GymPlatformBuilder.clearGym(serverlevel, blockpos, instance.getGymType());
               RctApiProbe.unregisterGymTrainers(instance.getGymType(), instance.getSlotId());
            }
         }
      }
   }

   private static void restoreReturnGameMode(ServerPlayer player, GymInstance instance) {
      if (instance.getReturnGameMode() != null) {
         player.setGameMode(instance.getReturnGameMode());
      }
   }

   private static void sendSuccess(ServerPlayer player, CommandSourceStack source, String message) {
      if (source != null) {
         source.sendSuccess(() -> Component.literal(message), false);
      } else {
         player.sendSystemMessage(Component.literal(message));
      }
   }

   private static void sendFailure(ServerPlayer player, CommandSourceStack source, String message) {
      if (source != null) {
         source.sendFailure(Component.literal(message));
      } else {
         player.sendSystemMessage(Component.literal(message));
      }
   }

   private static int debugTrainerEntities(CommandSourceStack source) {
      GymCommand.ActiveGymDebugContext gymcommand$activegymdebugcontext = getActiveGymDebugContext(source);
      if (gymcommand$activegymdebugcontext == null) {
         return 0;
      }

      int i = GymPlatformBuilder.cleanupTrainerEntities(
         gymcommand$activegymdebugcontext.gymLevel(),
         gymcommand$activegymdebugcontext.origin(),
         gymcommand$activegymdebugcontext.gymType(),
         gymcommand$activegymdebugcontext.instance().getSlotId()
      );
      if (i > 0) {
         source.sendSuccess(() -> Component.literal("Cleaned " + i + " stale trainer display entities before counting."), false);
         CobbleBash.LOGGER
            .warn(
               "Trainer debug count normalized {} gym {} slot {} before reporting; removed {} entities.",
               new Object[]{
                  gymcommand$activegymdebugcontext.player().getGameProfile().getName(),
                  gymcommand$activegymdebugcontext.gymType(),
                  gymcommand$activegymdebugcontext.instance().getSlotId(),
                  i
               }
            );
      }

      List<GymPlatformBuilder.TrainerEntityDebug> list = GymPlatformBuilder.debugTrainerEntities(
         gymcommand$activegymdebugcontext.gymLevel(),
         gymcommand$activegymdebugcontext.origin(),
         gymcommand$activegymdebugcontext.gymType(),
         gymcommand$activegymdebugcontext.instance().getSlotId()
      );
      int j = 0;

      for (GymPlatformBuilder.TrainerEntityDebug gymplatformbuilder$trainerentitydebug : list) {
         j += gymplatformbuilder$trainerentitydebug.total();
         source.sendSuccess(
            () -> Component.literal(
               gymplatformbuilder$trainerentitydebug.trainerIdPart()
                  + ": total="
                  + gymplatformbuilder$trainerentitydebug.total()
                  + ", exactTagged="
                  + gymplatformbuilder$trainerentitydebug.exactTagged()
                  + ", nearbyDisplays="
                  + gymplatformbuilder$trainerentitydebug.nearbyDisplays()
            ),
            false
         );
         CobbleBash.LOGGER
            .info(
               "Trainer debug for {} gym {} slot {} {}: total={}, exactTagged={}, nearbyDisplays={}, trainerId={}, entries={}",
               new Object[]{
                  gymcommand$activegymdebugcontext.player().getGameProfile().getName(),
                  gymcommand$activegymdebugcontext.gymType(),
                  gymcommand$activegymdebugcontext.instance().getSlotId(),
                  gymplatformbuilder$trainerentitydebug.trainerIdPart(),
                  gymplatformbuilder$trainerentitydebug.total(),
                  gymplatformbuilder$trainerentitydebug.exactTagged(),
                  gymplatformbuilder$trainerentitydebug.nearbyDisplays(),
                  gymplatformbuilder$trainerentitydebug.trainerId(),
                  gymplatformbuilder$trainerentitydebug.entries()
               }
            );

         for (String s : gymplatformbuilder$trainerentitydebug.entries()) {
            source.sendSuccess(() -> Component.literal("  " + s), false);
         }
      }

      return Math.max(1, j);
   }

   private static int cleanupTrainerEntities(CommandSourceStack source) {
      GymCommand.ActiveGymDebugContext gymcommand$activegymdebugcontext = getActiveGymDebugContext(source);
      if (gymcommand$activegymdebugcontext == null) {
         return 0;
      }

      int i = GymPlatformBuilder.cleanupTrainerEntities(
         gymcommand$activegymdebugcontext.gymLevel(),
         gymcommand$activegymdebugcontext.origin(),
         gymcommand$activegymdebugcontext.gymType(),
         gymcommand$activegymdebugcontext.instance().getSlotId()
      );
      source.sendSuccess(() -> Component.literal("Removed " + i + " stale or duplicate trainer display entities."), false);
      CobbleBash.LOGGER
         .warn(
            "Trainer debug cleanup for {} gym {} slot {} removed {} entities.",
            new Object[]{
               gymcommand$activegymdebugcontext.player().getGameProfile().getName(),
               gymcommand$activegymdebugcontext.gymType(),
               gymcommand$activegymdebugcontext.instance().getSlotId(),
               i
            }
         );
      return Math.max(1, i);
   }

   private static int discardOneTrainerDisplay(CommandSourceStack source, GymTrainerUnit unit) {
      GymCommand.ActiveGymDebugContext gymcommand$activegymdebugcontext = getActiveGymDebugContext(source);
      if (gymcommand$activegymdebugcontext == null) {
         return 0;
      }

      int i = GymPlatformBuilder.discardOneTrainerDisplay(
         gymcommand$activegymdebugcontext.gymLevel(),
         gymcommand$activegymdebugcontext.origin(),
         gymcommand$activegymdebugcontext.gymType(),
         gymcommand$activegymdebugcontext.instance().getSlotId(),
         unit.getTrainerIdPart()
      );
      source.sendSuccess(
         () -> Component.literal("Discarded one " + unit.getDisplayName() + " display entity. Remaining nearby/tagged displays: " + i + "."), false
      );
      return 1;
   }

   private static int discardTrainerDisplays(CommandSourceStack source, GymTrainerUnit unit) {
      GymCommand.ActiveGymDebugContext gymcommand$activegymdebugcontext = getActiveGymDebugContext(source);
      if (gymcommand$activegymdebugcontext == null) {
         return 0;
      }

      int i = GymPlatformBuilder.discardTrainerDisplays(
         gymcommand$activegymdebugcontext.gymLevel(),
         gymcommand$activegymdebugcontext.origin(),
         gymcommand$activegymdebugcontext.gymType(),
         gymcommand$activegymdebugcontext.instance().getSlotId(),
         unit.getTrainerIdPart()
      );
      source.sendSuccess(() -> Component.literal("Discarded " + i + " " + unit.getDisplayName() + " display entities."), false);
      return Math.max(1, i);
   }

   private static GymCommand.ActiveGymDebugContext getActiveGymDebugContext(CommandSourceStack source) {
      ServerPlayer serverplayer = source.getPlayer();
      GymInstance gyminstance = GymInstanceManager.getActive(serverplayer.getUUID());
      if (gyminstance == null) {
         source.sendFailure(Component.literal("You do not have an active gym instance."));
         return null;
      } else if ("elite4".equals(gyminstance.getGymType())) {
         source.sendFailure(Component.literal("Trainer debug currently targets regular gyms. Use a regular gym instance."));
         return null;
      } else {
         ServerLevel serverlevel = serverplayer.server.getLevel(CobbleBashDimensions.GYM_VOID);
         if (serverlevel == null) {
            source.sendFailure(Component.literal("Gym void dimension is not loaded."));
            return null;
         } else {
            return new GymCommand.ActiveGymDebugContext(
               serverplayer, serverlevel, gyminstance, GymSlotPosition.getOriginForSlot(gyminstance.getSlotId()), gyminstance.getGymType()
            );
         }
      }
   }

   private static LiteralArgumentBuilder<CommandSourceStack> trainerDebugTarget(GymCommand.TrainerDebugCommandExecutor executor) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("trainer")
            .then(Commands.literal("one").executes(context -> executor.run((CommandSourceStack)context.getSource(), GymTrainerUnit.TRAINER_ONE))))
         .then(Commands.literal("two").executes(context -> executor.run((CommandSourceStack)context.getSource(), GymTrainerUnit.TRAINER_TWO)));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> trainerTarget(GymType type, GymCommand.TrainerCommandExecutor executor) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(type.getId())
            .then(
               ((LiteralArgumentBuilder)Commands.literal("trainer")
                     .then(Commands.literal("one").executes(context -> executor.run((CommandSourceStack)context.getSource(), type, GymTrainerUnit.TRAINER_ONE))))
                  .then(Commands.literal("two").executes(context -> executor.run((CommandSourceStack)context.getSource(), type, GymTrainerUnit.TRAINER_TWO)))
            ))
         .then(Commands.literal("boss").executes(context -> executor.run((CommandSourceStack)context.getSource(), type, GymTrainerUnit.BOSS)));
   }

   private static String getExpectedTrainerName(int stage) {
      GymTrainerUnit gymtrainerunit = getExpectedTrainerUnit(stage);
      return gymtrainerunit == null ? "no remaining trainer" : gymtrainerunit.getDisplayName();
   }

   private static GymTrainerUnit getExpectedTrainerUnit(int stage) {
      return switch (stage) {
         case 0 -> GymTrainerUnit.TRAINER_ONE;
         case 1 -> GymTrainerUnit.TRAINER_TWO;
         case 2 -> GymTrainerUnit.BOSS;
         default -> null;
      };
   }

   private static String getTrainerDisplayName(ServerPlayer player, String gymType, GymTrainerUnit unit) {
      return RctApiProbe.getTrainerDisplayName(player.server, gymType, unit.getTrainerIdPart());
   }

   private record ActiveGymDebugContext(ServerPlayer player, ServerLevel gymLevel, GymInstance instance, BlockPos origin, String gymType) {
   }

   private interface TrainerCommandExecutor {
      int run(CommandSourceStack var1, GymType var2, GymTrainerUnit var3);
   }

   private interface TrainerDebugCommandExecutor {
      int run(CommandSourceStack var1, GymTrainerUnit var2);
   }
}
