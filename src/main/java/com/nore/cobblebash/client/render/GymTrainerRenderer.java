package com.nore.cobblebash.client.render;

import com.nore.cobblebash.client.model.GymTrainerFemaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerOperativeModel;
import com.nore.cobblebash.entity.GymTrainerEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class GymTrainerRenderer extends MobRenderer<GymTrainerEntity, GymTrainerOperativeModel> {
   private static final String[] VARIANT_PREFIXES = new String[]{"male_v1", "male_v2", "female_v1", "female_v2"};
   private static final ResourceLocation[][] TEXTURES = createTextures();

   public GymTrainerRenderer(Context context) {
      super(
         context,
         new GymTrainerOperativeModel(
            context.bakeLayer(GymTrainerMaleV1Model.LAYER_LOCATION),
            context.bakeLayer(GymTrainerMaleV2Model.LAYER_LOCATION),
            context.bakeLayer(GymTrainerFemaleV1Model.LAYER_LOCATION),
            context.bakeLayer(GymTrainerFemaleV2Model.LAYER_LOCATION)
         ),
         0.35F
      );
   }

   public ResourceLocation getTextureLocation(GymTrainerEntity trainer) {
      return TEXTURES[trainer.modelVariant()][trainer.textureVariant()];
   }

   private static ResourceLocation[][] createTextures() {
      ResourceLocation[][] aresourcelocation = new ResourceLocation[VARIANT_PREFIXES.length][16];

      for (int i = 0; i < VARIANT_PREFIXES.length; i++) {
         for (int j = 0; j < 16; j++) {
            int k = j / 8 + 1;
            int l = j % 8 + 1;
            aresourcelocation[i][j] = ResourceLocation.fromNamespaceAndPath(
               "cobblebash", "textures/entity/gym_trainer/" + VARIANT_PREFIXES[i] + "_" + k + "_" + l + ".png"
            );
         }
      }

      return aresourcelocation;
   }
}
