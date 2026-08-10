package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.cobblebash.entity.GymLeaderEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

public class GymLeaderOperativeModel extends EntityModel<GymLeaderEntity> {
   private final EntityModel<GymLeaderEntity>[] models;
   private int activeModel;

   public GymLeaderOperativeModel(
      ModelPart maleV1,
      ModelPart maleV2,
      ModelPart femaleV1,
      ModelPart femaleV2,
      ModelPart eliteElectricGround,
      ModelPart eliteFireFairy,
      ModelPart eliteGrassGhost,
      ModelPart eliteWaterSteel,
      ModelPart eliteChampion
   ) {
      this.models = new EntityModel[]{
         new GymLeaderMaleV1Model(maleV1),
         new GymLeaderMaleV2Model(maleV2),
         new GymLeaderFemaleV1Model(femaleV1),
         new GymLeaderFemaleV2Model(femaleV2),
         new EliteFourElectricGroundModel(eliteElectricGround),
         new EliteFourFireFairyModel(eliteFireFairy),
         new EliteFourGrassGhostModel(eliteGrassGhost),
         new EliteFourWaterSteelModel(eliteWaterSteel),
         new EliteFourChampionModel(eliteChampion)
      };
   }

   public void setupAnim(GymLeaderEntity leader, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.activeModel = leader.modelVariant();
      this.activeModel().setupAnim(leader, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.activeModel().renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   private EntityModel<GymLeaderEntity> activeModel() {
      return this.models[Math.max(0, Math.min(this.activeModel, this.models.length - 1))];
   }
}
