package com.nore.cobblebash;

import com.nore.cobblebash.beacon.ChampionBeaconRenderer;
import com.nore.cobblebash.beacon.ChampionBeaconScreen;
import com.nore.cobblebash.client.model.ChampionRibbonModel;
import com.nore.cobblebash.client.model.EliteFourChampionModel;
import com.nore.cobblebash.client.model.EliteFourElectricGroundModel;
import com.nore.cobblebash.client.model.EliteFourFireFairyModel;
import com.nore.cobblebash.client.model.EliteFourGrassGhostModel;
import com.nore.cobblebash.client.model.EliteFourWaterSteelModel;
import com.nore.cobblebash.client.model.GymLeaderFemaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderFemaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV2Model;
import com.nore.cobblebash.client.model.LeagueRepresentativeVillagerModel;
import com.nore.cobblebash.client.model.TrainerRibbonModel;
import com.nore.cobblebash.client.render.GymLeaderRenderer;
import com.nore.cobblebash.client.render.GymTrainerRenderer;
import com.nore.cobblebash.client.render.LeagueRepresentativeVillagerRenderer;
import com.nore.cobblebash.client.render.RibbonAccessoryRenderer;
import com.nore.cobblebash.client.tooltip.ClientRibbonTooltipComponent;
import com.nore.cobblebash.client.tooltip.RibbonTooltipComponent;
import com.nore.cobblebash.elitefour.EliteFourChampionBeamRenderer;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Cote client.
 *
 * <p>NeoForge distribue ces enregistrements sur cinq evenements du bus de mod ;
 * Fabric les rassemble ici, chacun sur son registre.
 *
 * <p>L'ecran de configuration de l'original ({@code IConfigScreenFactory}) n'a
 * pas d'equivalent en API de base — la configuration se modifie dans
 * {@code config/cobblebash.json}.
 */
public class CobbleBashClient implements ClientModInitializer {
   private static final ResourceLocation TRAINER_RIBBON_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/item/trainer_ribbon.png");
   private static final ResourceLocation CHAMPION_RIBBON_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/item/champion_ribbon.png");

   @Override
   public void onInitializeClient() {
      registerLayerDefinitions();
      registerEntityRenderers();

      BlockEntityRenderers.register(CobbleBash.CHAMPION_BEACON_BLOCK_ENTITY, ChampionBeaconRenderer::new);
      BlockEntityRenderers.register(CobbleBash.ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY, EliteFourChampionBeamRenderer::new);

      // `RegisterMenuScreensEvent` cote NeoForge.
      MenuScreens.register(CobbleBash.CHAMPION_BEACON_MENU, ChampionBeaconScreen::new);
      MenuScreens.register(CobbleBash.GYM_LEVEL_MENU, com.nore.cobblebash.gymlevel.GymLevelScreen::new);

      registerRibbonRenderers();

      // `RegisterClientTooltipComponentFactoriesEvent` : Fabric convertit les
      // composants d'infobulle par un rappel plutot que par un registre.
      net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback.EVENT.register(data ->
         data instanceof RibbonTooltipComponent ribbon ? new ClientRibbonTooltipComponent(ribbon) : null);
   }

   private static void registerLayerDefinitions() {
      layer(TrainerRibbonModel.LAYER_LOCATION, TrainerRibbonModel::createBodyLayer);
      layer(ChampionRibbonModel.LAYER_LOCATION, ChampionRibbonModel::createBodyLayer);
      layer(GymTrainerMaleV1Model.LAYER_LOCATION, GymTrainerMaleV1Model::createBodyLayer);
      layer(GymTrainerMaleV2Model.LAYER_LOCATION, GymTrainerMaleV2Model::createBodyLayer);
      layer(GymTrainerFemaleV1Model.LAYER_LOCATION, GymTrainerFemaleV1Model::createBodyLayer);
      layer(GymTrainerFemaleV2Model.LAYER_LOCATION, GymTrainerFemaleV2Model::createBodyLayer);
      layer(GymLeaderMaleV1Model.LAYER_LOCATION, GymLeaderMaleV1Model::createBodyLayer);
      layer(GymLeaderMaleV2Model.LAYER_LOCATION, GymLeaderMaleV2Model::createBodyLayer);
      layer(GymLeaderFemaleV1Model.LAYER_LOCATION, GymLeaderFemaleV1Model::createBodyLayer);
      layer(GymLeaderFemaleV2Model.LAYER_LOCATION, GymLeaderFemaleV2Model::createBodyLayer);
      layer(EliteFourElectricGroundModel.LAYER_LOCATION, EliteFourElectricGroundModel::createBodyLayer);
      layer(EliteFourFireFairyModel.LAYER_LOCATION, EliteFourFireFairyModel::createBodyLayer);
      layer(EliteFourGrassGhostModel.LAYER_LOCATION, EliteFourGrassGhostModel::createBodyLayer);
      layer(EliteFourWaterSteelModel.LAYER_LOCATION, EliteFourWaterSteelModel::createBodyLayer);
      layer(EliteFourChampionModel.LAYER_LOCATION, EliteFourChampionModel::createBodyLayer);
      layer(LeagueRepresentativeVillagerModel.LAYER_LOCATION, LeagueRepresentativeVillagerModel::createBodyLayer);
   }

   private static void layer(net.minecraft.client.model.geom.ModelLayerLocation location,
                             EntityModelLayerRegistry.TexturedModelDataProvider provider) {
      EntityModelLayerRegistry.registerModelLayer(location, provider);
   }

   private static void registerEntityRenderers() {
      EntityRendererRegistry.register(EntityType.VILLAGER, LeagueRepresentativeVillagerRenderer::new);
      EntityRendererRegistry.register(CobbleBash.GYM_TRAINER, GymTrainerRenderer::new);
      EntityRendererRegistry.register(CobbleBash.GYM_LEADER, GymLeaderRenderer::new);
   }

   private static void registerRibbonRenderers() {
      AccessoriesRendererRegistry.registerRenderer(CobbleBash.TRAINER_RIBBON, () -> new RibbonAccessoryRenderer(
         new TrainerRibbonModel(Minecraft.getInstance().getEntityModels().bakeLayer(TrainerRibbonModel.LAYER_LOCATION)),
         TRAINER_RIBBON_TEXTURE));
      AccessoriesRendererRegistry.registerRenderer(CobbleBash.CHAMPION_RIBBON, () -> new RibbonAccessoryRenderer(
         new ChampionRibbonModel(Minecraft.getInstance().getEntityModels().bakeLayer(ChampionRibbonModel.LAYER_LOCATION)),
         CHAMPION_RIBBON_TEXTURE));
   }
}
