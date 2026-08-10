package com.nore.cobblebash.client.render;

import com.nore.cobblebash.client.model.EliteFourChampionModel;
import com.nore.cobblebash.client.model.EliteFourElectricGroundModel;
import com.nore.cobblebash.client.model.EliteFourFireFairyModel;
import com.nore.cobblebash.client.model.EliteFourGrassGhostModel;
import com.nore.cobblebash.client.model.EliteFourWaterSteelModel;
import com.nore.cobblebash.client.model.GymLeaderFemaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderFemaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV1Model;
import com.nore.cobblebash.client.model.GymLeaderMaleV2Model;
import com.nore.cobblebash.client.model.GymLeaderOperativeModel;
import com.nore.cobblebash.entity.GymLeaderEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class GymLeaderRenderer extends MobRenderer<GymLeaderEntity, GymLeaderOperativeModel> {
   private static final String[] VARIANT_PREFIXES = new String[]{"male_v1", "male_v2", "female_v1", "female_v2"};
   private static final ResourceLocation[] ELITE_TEXTURES = new ResourceLocation[]{
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/entity/elite_four/electric_ground.png"),
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/entity/elite_four/fire_fairy.png"),
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/entity/elite_four/grass_ghost.png"),
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/entity/elite_four/water_steel.png"),
      ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/entity/elite_four/champion.png")
   };
   private static final ResourceLocation[][] TEXTURES = createTextures();

   public GymLeaderRenderer(Context context) {
      super(
         context,
         new GymLeaderOperativeModel(
            context.bakeLayer(GymLeaderMaleV1Model.LAYER_LOCATION),
            context.bakeLayer(GymLeaderMaleV2Model.LAYER_LOCATION),
            context.bakeLayer(GymLeaderFemaleV1Model.LAYER_LOCATION),
            context.bakeLayer(GymLeaderFemaleV2Model.LAYER_LOCATION),
            context.bakeLayer(EliteFourElectricGroundModel.LAYER_LOCATION),
            context.bakeLayer(EliteFourFireFairyModel.LAYER_LOCATION),
            context.bakeLayer(EliteFourGrassGhostModel.LAYER_LOCATION),
            context.bakeLayer(EliteFourWaterSteelModel.LAYER_LOCATION),
            context.bakeLayer(EliteFourChampionModel.LAYER_LOCATION)
         ),
         0.35F
      );
   }

   public ResourceLocation getTextureLocation(GymLeaderEntity leader) {
      int i = leader.modelVariant();
      return i >= VARIANT_PREFIXES.length
         ? ELITE_TEXTURES[Math.min(i - VARIANT_PREFIXES.length, ELITE_TEXTURES.length - 1)]
         : TEXTURES[i][leader.textureVariant()];
   }

   private static ResourceLocation[][] createTextures() {
      ResourceLocation[][] aresourcelocation = new ResourceLocation[VARIANT_PREFIXES.length][16];

      for (int i = 0; i < VARIANT_PREFIXES.length; i++) {
         for (int j = 0; j < 16; j++) {
            int k = j / 8 + 1;
            int l = j % 8 + 1;
            aresourcelocation[i][j] = ResourceLocation.fromNamespaceAndPath(
               "cobblebash", "textures/entity/gym_leader/" + VARIANT_PREFIXES[i] + "_" + k + "_" + l + ".png"
            );
         }
      }

      return aresourcelocation;
   }
}
