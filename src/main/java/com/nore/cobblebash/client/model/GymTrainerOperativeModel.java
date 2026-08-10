package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.cobblebash.entity.GymTrainerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

public class GymTrainerOperativeModel extends EntityModel<GymTrainerEntity> {
   private final EntityModel<GymTrainerEntity>[] models;
   private int activeModel;

   public GymTrainerOperativeModel(ModelPart maleV1, ModelPart maleV2, ModelPart femaleV1, ModelPart femaleV2) {
      this.models = new EntityModel[]{
         new GymTrainerMaleV1Model(maleV1), new GymTrainerMaleV2Model(maleV2), new GymTrainerFemaleV1Model(femaleV1), new GymTrainerFemaleV2Model(femaleV2)
      };
   }

   public void setupAnim(GymTrainerEntity trainer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.activeModel = trainer.modelVariant();
      this.activeModel().setupAnim(trainer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.activeModel().renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   private EntityModel<GymTrainerEntity> activeModel() {
      return this.models[Math.max(0, Math.min(this.activeModel, this.models.length - 1))];
   }
}
