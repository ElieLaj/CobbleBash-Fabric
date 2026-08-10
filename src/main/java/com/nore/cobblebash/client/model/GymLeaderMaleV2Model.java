package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.cobblebash.entity.GymLeaderEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GymLeaderMaleV2Model extends EntityModel<GymLeaderEntity> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("cobblebash", "gym_leader_male_v2"), "main"
   );
   private final ModelPart root;
   private final ModelPart bodyRoot;
   private final ModelPart waist;
   private final ModelPart head;
   private final ModelPart rightArm;
   private final ModelPart leftArm;

   public GymLeaderMaleV2Model(ModelPart root) {
      this.root = root;
      this.bodyRoot = root;
      this.waist = this.bodyRoot.getChild("Waist");
      this.head = this.waist.getChild("Head");
      this.rightArm = this.waist.getChild("Right_Arm");
      this.leftArm = this.waist.getChild("Left_Arm");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "Head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -12.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "_3D_Hair",
         CubeListBuilder.create()
            .texOffs(1, 70)
            .addBox(-4.5F, -6.0F, 0.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(17, 68)
            .addBox(-4.5F, -5.0F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(37, 68)
            .addBox(-4.5F, -7.0F, 0.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(21, 64)
            .addBox(-4.5F, -7.5F, -1.5F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(29, 65)
            .addBox(-4.5F, -8.5F, -2.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(10, 75)
            .addBox(-4.5F, -8.5F, -4.5F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(23, 78)
            .addBox(-3.5F, -8.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(32, 75)
            .addBox(-2.5F, -8.5F, 2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(30, 75)
            .addBox(-3.5F, -9.0F, -4.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(14, 74)
            .addBox(-2.5F, -9.0F, -4.5F, 1.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(13, 77)
            .addBox(-1.5F, -9.0F, -4.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(23, 71)
            .addBox(-3.5F, -8.0F, -4.5F, 5.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(38, 78)
            .addBox(-1.5F, -8.5F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(23, 75)
            .addBox(-0.5F, -9.0F, -4.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(38, 75)
            .addBox(-0.5F, -8.5F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(37, 71)
            .addBox(0.5F, -8.5F, 1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 75)
            .addBox(0.5F, -9.0F, -4.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(29, 68)
            .addBox(1.5F, -8.5F, -3.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(15, 74)
            .addBox(1.5F, -8.5F, -4.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(30, 77)
            .addBox(2.5F, -8.5F, -3.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(17, 70)
            .addBox(3.5F, -7.5F, -4.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(4, 69)
            .addBox(3.5F, -7.0F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(21, 64)
            .addBox(3.5F, -6.0F, -2.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(11, 68)
            .addBox(3.5F, -5.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition4 = partdefinition1.addOrReplaceChild(
         "Body",
         CubeListBuilder.create()
            .texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(32, 32)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.15F))
            .texOffs(118, 122)
            .addBox(7.0F, 0.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.05F))
            .texOffs(118, 116)
            .addBox(-8.0F, 0.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.05F)),
         PartPose.offset(0.0F, -12.0F, 0.0F)
      );
      PartDefinition partdefinition5 = partdefinition4.addOrReplaceChild(
         "Belt",
         CubeListBuilder.create()
            .texOffs(0, 111)
            .addBox(-4.0F, -2.0F, 0.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.25F))
            .texOffs(17, 125)
            .addBox(-1.0F, -2.5F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 12.0F, -2.0F)
      );
      PartDefinition partdefinition6 = partdefinition4.addOrReplaceChild(
         "Badge",
         CubeListBuilder.create()
            .texOffs(0, 5)
            .addBox(-0.4F, -1.6F, -0.1F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(0, 3)
            .addBox(-0.4F, -1.6F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, 5.0F, -2.0F)
      );
      PartDefinition partdefinition7 = partdefinition1.addOrReplaceChild(
         "Right_Arm",
         CubeListBuilder.create()
            .texOffs(40, 16)
            .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 105)
            .addBox(-3.05F, 5.25F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)),
         PartPose.offset(-5.0F, -10.0F, 0.0F)
      );
      PartDefinition partdefinition8 = partdefinition1.addOrReplaceChild(
         "Left_Arm",
         CubeListBuilder.create()
            .texOffs(16, 32)
            .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 105)
            .addBox(-0.95F, 5.25F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)),
         PartPose.offset(5.0F, -10.0F, 0.0F)
      );
      PartDefinition partdefinition9 = partdefinition.addOrReplaceChild("Legs", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
      PartDefinition partdefinition10 = partdefinition9.addOrReplaceChild(
         "Left_Leg",
         CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.8F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition11 = partdefinition10.addOrReplaceChild(
         "Left_Leg_cover_r1",
         CubeListBuilder.create().texOffs(0, 48).addBox(-1.6F, -3.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)),
         PartPose.offsetAndRotation(1.1F, 2.0F, 0.0F, 0.0F, 0.0F, -0.1309F)
      );
      PartDefinition partdefinition12 = partdefinition10.addOrReplaceChild(
         "Shoe_Left",
         CubeListBuilder.create()
            .texOffs(0, 123)
            .addBox(-2.0F, -2.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(0, 116)
            .addBox(-2.0F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
            .texOffs(12, 125)
            .addBox(-1.0F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, -1.0F)
      );
      PartDefinition partdefinition13 = partdefinition9.addOrReplaceChild(
         "Right_Leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition14 = partdefinition13.addOrReplaceChild(
         "Right_Leg_Cover_r1",
         CubeListBuilder.create().texOffs(0, 48).addBox(-1.6F, -3.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)),
         PartPose.offsetAndRotation(-1.1F, 2.0F, 0.0F, 0.0F, 3.1416F, 0.1309F)
      );
      PartDefinition partdefinition15 = partdefinition13.addOrReplaceChild(
         "Shoe_Right",
         CubeListBuilder.create()
            .texOffs(0, 123)
            .addBox(-1.8F, -2.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(0, 116)
            .addBox(-1.8F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
            .texOffs(12, 125)
            .addBox(-0.8F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.2F, 9.0F, -1.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(GymLeaderEntity leader, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float f = ageInTicks * 0.055F;
      float f1 = ageInTicks * 0.09F;
      this.bodyRoot.xRot = 0.0F;
      this.bodyRoot.yRot = 0.0F;
      this.bodyRoot.zRot = 0.0F;
      this.waist.xRot = 0.0F;
      this.waist.yRot = 0.0F;
      this.waist.zRot = 0.0F;
      this.head.xRot = Mth.clamp(headPitch, -18.0F, 18.0F) * (float) (Math.PI / 180.0) + Mth.sin(f1) * 0.012F;
      this.head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (float) (Math.PI / 180.0);
      this.head.zRot = Mth.sin(f + 2.0F) * 0.01F;
      this.rightArm.xRot = Mth.sin(f + 0.3F) * 0.02F;
      this.leftArm.xRot = -Mth.sin(f + 0.3F) * 0.02F;
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
