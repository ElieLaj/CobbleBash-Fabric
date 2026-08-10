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

public class EliteFourGrassGhostModel extends EntityModel<GymLeaderEntity> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("cobblebash", "elite_four_grass_ghost"), "main"
   );
   private final ModelPart Entity;
   private final ModelPart Leggs;
   private final ModelPart Left_Leg;
   private final ModelPart Shoe_Left;
   private final ModelPart Right_Leg;
   private final ModelPart Shoe_Right;
   private final ModelPart Waist;
   private final ModelPart Head;
   private final ModelPart PonyTail_Hair;
   private final ModelPart PonyTail;
   private final ModelPart Tie;
   private final ModelPart Bangs;
   private final ModelPart Body;
   private final ModelPart Belt;
   private final ModelPart Badge;
   private final ModelPart Right_Arm;
   private final ModelPart Left_Arm;
   private final ModelPart bb_main;

   public EliteFourGrassGhostModel(ModelPart root) {
      this.Entity = root.getChild("Entity");
      this.Leggs = this.Entity.getChild("Leggs");
      this.Left_Leg = this.Leggs.getChild("Left_Leg");
      this.Shoe_Left = this.Left_Leg.getChild("Shoe_Left");
      this.Right_Leg = this.Leggs.getChild("Right_Leg");
      this.Shoe_Right = this.Right_Leg.getChild("Shoe_Right");
      this.Waist = this.Entity.getChild("Waist");
      this.Head = this.Waist.getChild("Head");
      this.PonyTail_Hair = this.Head.getChild("PonyTail_Hair");
      this.PonyTail = this.PonyTail_Hair.getChild("PonyTail");
      this.Tie = this.PonyTail.getChild("Tie");
      this.Bangs = this.PonyTail_Hair.getChild("Bangs");
      this.Body = this.Waist.getChild("Body");
      this.Belt = this.Body.getChild("Belt");
      this.Badge = this.Body.getChild("Badge");
      this.Right_Arm = this.Waist.getChild("Right_Arm");
      this.Left_Arm = this.Waist.getChild("Left_Arm");
      this.bb_main = root.getChild("bb_main");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("Entity", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("Leggs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "Left_Leg",
         CubeListBuilder.create()
            .texOffs(0, 48)
            .addBox(-2.0F, 1.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(0, 32)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition4 = partdefinition3.addOrReplaceChild(
         "Shoe_Left",
         CubeListBuilder.create()
            .texOffs(0, 123)
            .addBox(-2.0F, -4.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.15F))
            .texOffs(0, 116)
            .addBox(-2.0F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
            .texOffs(12, 125)
            .addBox(-1.0F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, -1.0F)
      );
      PartDefinition partdefinition5 = partdefinition2.addOrReplaceChild(
         "Right_Leg",
         CubeListBuilder.create()
            .texOffs(0, 58)
            .addBox(-2.0F, 1.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(0, 16)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-3.8F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition6 = partdefinition5.addOrReplaceChild(
         "Shoe_Right",
         CubeListBuilder.create()
            .texOffs(0, 123)
            .addBox(-1.8F, -4.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.15F))
            .texOffs(0, 116)
            .addBox(-1.8F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
            .texOffs(12, 125)
            .addBox(-0.8F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.2F, 9.0F, -1.0F)
      );
      PartDefinition partdefinition7 = partdefinition1.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(-1.9F, 0.0F, 0.0F));
      PartDefinition partdefinition8 = partdefinition7.addOrReplaceChild(
         "Head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -12.0F, 0.0F)
      );
      PartDefinition partdefinition9 = partdefinition8.addOrReplaceChild(
         "PonyTail_Hair",
         CubeListBuilder.create().texOffs(16, 48).addBox(-5.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition10 = partdefinition9.addOrReplaceChild(
         "PonyTail",
         CubeListBuilder.create()
            .texOffs(6, 81)
            .addBox(-4.0F, 0.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(4, 84)
            .addBox(-4.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 73)
            .addBox(-5.0F, 0.0F, 0.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 68)
            .addBox(-4.0F, 6.0F, 0.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 81)
            .addBox(-4.0F, 9.0F, 1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, -8.0F, 5.0F)
      );
      PartDefinition partdefinition11 = partdefinition10.addOrReplaceChild(
         "Tie",
         CubeListBuilder.create()
            .texOffs(2, 84)
            .addBox(0.5F, -1.6F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 84)
            .addBox(0.5F, 0.6F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 84)
            .addBox(-0.7F, 0.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 84)
            .addBox(-0.7F, -1.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 84)
            .addBox(1.7F, -1.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 84)
            .addBox(1.7F, 0.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-4.0F, 1.0F, -1.0F)
      );
      PartDefinition partdefinition12 = partdefinition9.addOrReplaceChild(
         "Bangs",
         CubeListBuilder.create()
            .texOffs(39, 14)
            .addBox(2.5F, -27.5F, -3.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(45, 14)
            .addBox(7.5F, -27.5F, -3.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(39, 11)
            .addBox(6.5F, -27.5F, -5.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(39, 8)
            .addBox(1.5F, -27.5F, -5.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(33, 13)
            .addBox(8.5F, -26.5F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(36, 10)
            .addBox(9.5F, -24.5F, -5.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(32, 7)
            .addBox(9.5F, -26.5F, -4.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(40, 5)
            .addBox(1.5F, -26.5F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(46, 5)
            .addBox(1.5F, -24.5F, -5.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(36, 3)
            .addBox(1.5F, -26.5F, -4.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-7.0F, 19.0F, 1.0F)
      );
      PartDefinition partdefinition13 = partdefinition7.addOrReplaceChild(
         "Body",
         CubeListBuilder.create()
            .texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(30, 32)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.15F))
            .texOffs(118, 116)
            .addBox(-7.0F, 0.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.05F))
            .texOffs(118, 122)
            .addBox(6.0F, 0.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.05F)),
         PartPose.offset(0.0F, -12.0F, 0.0F)
      );
      PartDefinition partdefinition14 = partdefinition13.addOrReplaceChild(
         "Belt",
         CubeListBuilder.create()
            .texOffs(0, 111)
            .addBox(-4.0F, -2.0F, 0.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.25F))
            .texOffs(17, 124)
            .addBox(-1.0F, -2.5F, -0.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 12.0F, -2.0F)
      );
      PartDefinition partdefinition15 = partdefinition13.addOrReplaceChild(
         "Badge",
         CubeListBuilder.create()
            .texOffs(0, 5)
            .addBox(-0.4F, -1.6F, -0.1F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(0, 3)
            .addBox(-0.4F, -1.6F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 17)
            .addBox(-0.4F, -0.9F, -0.2F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, 6.0F, -2.0F)
      );
      PartDefinition partdefinition16 = partdefinition7.addOrReplaceChild(
         "Right_Arm",
         CubeListBuilder.create()
            .texOffs(0, 105)
            .addBox(-2.05F, 5.25F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(40, 16)
            .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, -10.0F, 0.0F)
      );
      PartDefinition partdefinition17 = partdefinition7.addOrReplaceChild(
         "Left_Arm",
         CubeListBuilder.create()
            .texOffs(0, 105)
            .addBox(-0.95F, 5.25F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.1F))
            .texOffs(16, 32)
            .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(5.0F, -10.0F, 0.0F)
      );
      PartDefinition partdefinition18 = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition19 = partdefinition18.addOrReplaceChild(
         "Right_Leg_Cover_r1",
         CubeListBuilder.create().texOffs(23, 70).addBox(-1.6F, -3.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)),
         PartPose.offsetAndRotation(-3.0F, -10.0F, 0.0F, 0.0F, 3.1416F, 0.1309F)
      );
      PartDefinition partdefinition20 = partdefinition18.addOrReplaceChild(
         "Left_Leg_cover_r1",
         CubeListBuilder.create().texOffs(23, 70).addBox(-1.6F, -3.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)),
         PartPose.offsetAndRotation(3.0F, -10.0F, 0.0F, 0.0F, 0.0F, -0.1309F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(GymLeaderEntity leader, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.Head.xRot = Mth.clamp(headPitch, -18.0F, 18.0F) * (float) (Math.PI / 180.0);
      this.Head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (float) (Math.PI / 180.0);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Entity.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
