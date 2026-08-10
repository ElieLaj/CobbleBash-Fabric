package com.nore.cobblebash;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.beacon.ChampionBeaconBlock;
import com.nore.cobblebash.beacon.ChampionBeaconBlockEntity;
import com.nore.cobblebash.beacon.ChampionBeaconMenu;
import com.nore.cobblebash.block.EliteFourChampionBeamBlock;
import com.nore.cobblebash.block.EliteFourPlaqueBlock;
import com.nore.cobblebash.block.TrainingSimulatorBlock;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.elitefour.EliteFourChampionBeamBlockEntity;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.entity.GymLeaderEntity;
import com.nore.cobblebash.entity.GymTrainerEntity;
import com.nore.cobblebash.event.GymEventHandler;
import com.nore.cobblebash.event.LeagueRepresentativeEvents;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.item.ChampionRibbonItem;
import com.nore.cobblebash.item.EliteFourTrainingDiskItem;
import com.nore.cobblebash.item.TrainerRibbonItem;
import com.nore.cobblebash.item.TrainingDiskItem;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.util.DelayedTaskScheduler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Point d'entree Fabric.
 *
 * <p>NeoForge differe l'enregistrement via {@code DeferredRegister} parce que
 * ses registres ne sont ouverts que pendant un evenement precis. Fabric appelle
 * {@link #onInitialize()} alors qu'ils sont encore modifiables : on enregistre
 * donc directement, dans l'ordre des dependances — un bloc avant son item,
 * l'item avant l'onglet qui l'affiche.
 *
 * <p>Les champs ne sont donc plus des porteurs differes mais les objets
 * eux-memes, affectes au demarrage.
 */
public class CobbleBash implements ModInitializer {
   public static final String MODID = "cobblebash";
   public static final Logger LOGGER = LogUtils.getLogger();

   private static final SoundType CHAMPION_BEACON_SOUND = new SoundType(
      1.0F, 1.0F, SoundEvents.BEACON_DEACTIVATE, SoundEvents.GLASS_STEP,
      SoundEvents.GLASS_PLACE, SoundEvents.GLASS_HIT, SoundEvents.GLASS_FALL
   );

   public static final ResourceKey<PoiType> LEAGUE_REPRESENTATIVE_POI_KEY = ResourceKey.create(
      Registries.POINT_OF_INTEREST_TYPE, id("league_representative")
   );

   public static TrainingSimulatorBlock TRAINING_SIMULATOR;
   public static BlockItem TRAINING_SIMULATOR_ITEM;
   public static ChampionBeaconBlock CHAMPION_BEACON;
   public static BlockItem CHAMPION_BEACON_ITEM;
   public static EliteFourChampionBeamBlock ELITE_FOUR_CHAMPION_BEAM;
   public static EliteFourPlaqueBlock ELITE_FOUR_PLAQUE_ELECTRIC_GROUND;
   public static EliteFourPlaqueBlock ELITE_FOUR_PLAQUE_FIRE_FAIRY;
   public static EliteFourPlaqueBlock ELITE_FOUR_PLAQUE_GRASS_GHOST;
   public static EliteFourPlaqueBlock ELITE_FOUR_PLAQUE_WATER_STEEL;
   public static BlockEntityType<ChampionBeaconBlockEntity> CHAMPION_BEACON_BLOCK_ENTITY;
   public static BlockEntityType<EliteFourChampionBeamBlockEntity> ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY;
   public static MenuType<ChampionBeaconMenu> CHAMPION_BEACON_MENU;
   public static net.minecraft.world.inventory.MenuType<com.nore.cobblebash.gymlevel.GymLevelMenu> GYM_LEVEL_MENU;
   public static ChampionRibbonItem CHAMPION_RIBBON;
   public static TrainerRibbonItem TRAINER_RIBBON;
   public static EliteFourTrainingDiskItem ELITE_FOUR_TRAINING_DISK;
   public static SmithingTemplateItem CHAMPION_UPGRADE_SMITHING_TEMPLATE;
   public static PoiType LEAGUE_REPRESENTATIVE_POI;
   public static VillagerProfession LEAGUE_REPRESENTATIVE;
   public static EntityType<GymTrainerEntity> GYM_TRAINER;
   public static EntityType<GymLeaderEntity> GYM_LEADER;
   public static Map<GymType, TrainingDiskItem> TRAINING_DISKS = Collections.emptyMap();
   public static CreativeModeTab COBBLEBASH_TAB;

   @Override
   public void onInitialize() {
      Config.load();

      registerBlocks();
      registerItems();
      registerBlockEntitiesAndMenus();
      registerEntities();
      registerVillager();
      registerCreativeTab();

      CobbleBashCriteriaTriggers.bootstrap();
      CobbleBashStats.bootstrap();

      DelayedTaskScheduler.register();
      GymEventHandler.register();
      LeagueRepresentativeEvents.register();

      CommandRegistrationCallback.EVENT.register(
         (dispatcher, registryAccess, environment) -> GymCommand.register(dispatcher));

      // L'original branche RCT depuis FMLCommonSetupEvent. Attendre le
      // demarrage du serveur est plus sur sur Fabric : RCT a forcement fini de
      // s'initialiser, et l'appel est idempotent.
      ServerLifecycleEvents.SERVER_STARTING.register(server -> {
         RctApiProbe.logLoaded();
         GymEventHandler.registerRctListeners();
      });

      LOGGER.info("CobbleBash (Fabric) initialise.");
   }

   private static void registerBlocks() {
      TRAINING_SIMULATOR = registerBlock("training_simulator",
         new TrainingSimulatorBlock(Properties.of().mapColor(MapColor.METAL).strength(3.5F).noOcclusion()));

      CHAMPION_BEACON = registerBlock("champion_beacon",
         new ChampionBeaconBlock(Properties.of().mapColor(MapColor.DIAMOND).strength(3.0F)
            .sound(CHAMPION_BEACON_SOUND).lightLevel(state -> 15).noOcclusion()));

      ELITE_FOUR_CHAMPION_BEAM = registerBlock("elite_four_champion_beam",
         new EliteFourChampionBeamBlock(Properties.of().mapColor(MapColor.NONE)
            .strength(-1.0F, 3600000.0F).noCollission().noLootTable().noOcclusion()));

      ELITE_FOUR_PLAQUE_ELECTRIC_GROUND = registerPlaque("elite_four_plaque_electric_ground", EliteFourMember.ELECTRIC_GROUND);
      ELITE_FOUR_PLAQUE_FIRE_FAIRY = registerPlaque("elite_four_plaque_fire_fairy", EliteFourMember.FIRE_FAIRY);
      ELITE_FOUR_PLAQUE_GRASS_GHOST = registerPlaque("elite_four_plaque_grass_ghost", EliteFourMember.GRASS_GHOST);
      ELITE_FOUR_PLAQUE_WATER_STEEL = registerPlaque("elite_four_plaque_water_steel", EliteFourMember.WATER_STEEL);
   }

   private static void registerItems() {
      TRAINING_SIMULATOR_ITEM = registerBlockItem("training_simulator", TRAINING_SIMULATOR);
      CHAMPION_BEACON_ITEM = registerBlockItem("champion_beacon", CHAMPION_BEACON);
      registerBlockItem("elite_four_plaque_electric_ground", ELITE_FOUR_PLAQUE_ELECTRIC_GROUND);
      registerBlockItem("elite_four_plaque_fire_fairy", ELITE_FOUR_PLAQUE_FIRE_FAIRY);
      registerBlockItem("elite_four_plaque_grass_ghost", ELITE_FOUR_PLAQUE_GRASS_GHOST);
      registerBlockItem("elite_four_plaque_water_steel", ELITE_FOUR_PLAQUE_WATER_STEEL);

      CHAMPION_RIBBON = registerItem("champion_ribbon", new ChampionRibbonItem(new Item.Properties().stacksTo(1)));
      TRAINER_RIBBON = registerItem("trainer_ribbon", new TrainerRibbonItem(new Item.Properties().stacksTo(1)));
      ELITE_FOUR_TRAINING_DISK = registerItem("elite_four_training_disk",
         new EliteFourTrainingDiskItem(new Item.Properties().stacksTo(16)));
      CHAMPION_UPGRADE_SMITHING_TEMPLATE = registerItem("champion_upgrade_smithing_template",
         createChampionUpgradeTemplate());

      Map<GymType, TrainingDiskItem> disks = new EnumMap<>(GymType.class);
      for (GymType type : GymType.values()) {
         disks.put(type, registerItem(type.getId() + "_training_disk",
            new TrainingDiskItem(type, new Item.Properties().stacksTo(16))));
      }
      TRAINING_DISKS = Collections.unmodifiableMap(disks);
   }

   private static void registerBlockEntitiesAndMenus() {
      CHAMPION_BEACON_BLOCK_ENTITY = Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("champion_beacon"),
         BlockEntityType.Builder.of(ChampionBeaconBlockEntity::new, CHAMPION_BEACON).build(null));

      ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY = Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("elite_four_champion_beam"),
         BlockEntityType.Builder.of(EliteFourChampionBeamBlockEntity::new, ELITE_FOUR_CHAMPION_BEAM).build(null));

      CHAMPION_BEACON_MENU = Registry.register(
         BuiltInRegistries.MENU, id("champion_beacon"),
         new MenuType<>(ChampionBeaconMenu::new, FeatureFlags.DEFAULT_FLAGS));

      // Le type d'arene doit atteindre le client pour l'affichage : c'est ce
      // que permet le type de menu etendu de Fabric.
      GYM_LEVEL_MENU = Registry.register(
         BuiltInRegistries.MENU, id("gym_level"),
         new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(
            com.nore.cobblebash.gymlevel.GymLevelMenu::new,
            com.nore.cobblebash.gymlevel.GymLevelMenu.DATA_CODEC));
   }

   private static void registerEntities() {
      GYM_TRAINER = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("gym_trainer"),
         EntityType.Builder.of(GymTrainerEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(2)
            .build("cobblebash:gym_trainer"));

      GYM_LEADER = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("gym_leader"),
         EntityType.Builder.of(GymLeaderEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(2)
            .build("cobblebash:gym_leader"));

      // `EntityAttributeCreationEvent` cote NeoForge.
      FabricDefaultAttributeRegistry.register(GYM_TRAINER, GymTrainerEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(GYM_LEADER, GymLeaderEntity.createAttributes());
   }

   private static void registerVillager() {
      LEAGUE_REPRESENTATIVE_POI = Registry.register(
         BuiltInRegistries.POINT_OF_INTEREST_TYPE, id("league_representative"),
         new PoiType(ImmutableSet.copyOf(TRAINING_SIMULATOR.getStateDefinition().getPossibleStates()), 1, 1));

      LEAGUE_REPRESENTATIVE = Registry.register(
         BuiltInRegistries.VILLAGER_PROFESSION, id("league_representative"),
         new VillagerProfession(
            "cobblebash:league_representative",
            holder -> holder.is(LEAGUE_REPRESENTATIVE_POI_KEY),
            holder -> holder.is(LEAGUE_REPRESENTATIVE_POI_KEY),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_CARTOGRAPHER));
   }

   private static void registerCreativeTab() {
      // `withTabsBefore` est une extension NeoForge du constructeur d'onglet :
      // en vanilla, l'ordre suit celui de l'enregistrement.
      COBBLEBASH_TAB = Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB, id("cobblebash"),
         FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.cobblebash"))
            .icon(() -> TRAINING_SIMULATOR_ITEM.getDefaultInstance())
            .displayItems((parameters, output) -> {
               output.accept(TRAINING_SIMULATOR_ITEM);
               output.accept(CHAMPION_BEACON_ITEM);
               output.accept(CHAMPION_UPGRADE_SMITHING_TEMPLATE);
               output.accept(TRAINER_RIBBON);
               output.accept(CHAMPION_RIBBON);
               output.accept(ELITE_FOUR_TRAINING_DISK);
               TRAINING_DISKS.values().forEach(output::accept);
            })
            .build());
   }

   private static <T extends Block> T registerBlock(String name, T block) {
      return Registry.register(BuiltInRegistries.BLOCK, id(name), block);
   }

   private static EliteFourPlaqueBlock registerPlaque(String name, EliteFourMember member) {
      return registerBlock(name, new EliteFourPlaqueBlock(member,
         Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()));
   }

   private static BlockItem registerBlockItem(String name, Block block) {
      return registerItem(name, new BlockItem(block, new Item.Properties()));
   }

   private static <T extends Item> T registerItem(String name, T item) {
      return Registry.register(BuiltInRegistries.ITEM, id(name), item);
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(MODID, path);
   }

   private static SmithingTemplateItem createChampionUpgradeTemplate() {
      return new SmithingTemplateItem(
         Component.translatable("item.cobblebash.smithing_template.champion_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
         Component.translatable("item.cobblebash.smithing_template.champion_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
         Component.translatable("upgrade.cobblebash.champion_upgrade").withStyle(ChatFormatting.GRAY),
         Component.translatable("item.cobblebash.smithing_template.champion_upgrade.base_slot_description"),
         Component.translatable("item.cobblebash.smithing_template.champion_upgrade.additions_slot_description"),
         List.of(
            ResourceLocation.withDefaultNamespace("item/empty_slot_smithing_template_netherite_upgrade"),
            ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")
         ),
         List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")),
         new FeatureFlag[0]
      );
   }
}
