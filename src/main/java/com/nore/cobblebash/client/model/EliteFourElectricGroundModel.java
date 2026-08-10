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

public class EliteFourElectricGroundModel extends EntityModel<GymLeaderEntity> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("cobblebash", "elite_four_electric_ground"), "main"
   );
   private final ModelPart Waist;
   private final ModelPart Head;
   private final ModelPart __3D_Haid;
   private final ModelPart Body;
   private final ModelPart Belt;
   private final ModelPart Badge;
   private final ModelPart Right_Arm;
   private final ModelPart Left_Arm;
   private final ModelPart Legs;
   private final ModelPart Left_Leg;
   private final ModelPart Shoe_Left;
   private final ModelPart Right_Leg;
   private final ModelPart Shoe_Right;

   public EliteFourElectricGroundModel(ModelPart root) {
      this.Waist = root.getChild("Waist");
      this.Head = this.Waist.getChild("Head");
      this.__3D_Haid = this.Head.getChild("__3D_Haid");
      this.Body = this.Waist.getChild("Body");
      this.Belt = this.Body.getChild("Belt");
      this.Badge = this.Body.getChild("Badge");
      this.Right_Arm = this.Waist.getChild("Right_Arm");
      this.Left_Arm = this.Waist.getChild("Left_Arm");
      this.Legs = root.getChild("Legs");
      this.Left_Leg = this.Legs.getChild("Left_Leg");
      this.Shoe_Left = this.Left_Leg.getChild("Shoe_Left");
      this.Right_Leg = this.Legs.getChild("Right_Leg");
      this.Shoe_Right = this.Right_Leg.getChild("Shoe_Right");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "Head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 64)
            .addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -12.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "__3D_Haid",
         CubeListBuilder.create()
            .texOffs(32, 6)
            .addBox(-0.5F, -3.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(36, 6)
            .addBox(-8.5F, -3.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(40, 4)
            .addBox(-8.5F, -4.5F, -4.5F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(50, 6)
            .addBox(-8.5F, -5.5F, -5.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(54, 6)
            .addBox(-8.5F, -5.5F, -7.5F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(32, 4)
            .addBox(-8.5F, -6.5F, -8.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(32, 10)
            .addBox(-7.5F, -7.5F, -8.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(32, 1)
            .addBox(-5.5F, -8.5F, -8.5F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(44, 1)
            .addBox(-0.5F, -7.5F, -8.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(60, 7)
            .addBox(-0.5F, -5.5F, -7.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(56, 2)
            .addBox(-0.5F, -3.5F, -6.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(65, 2)
            .addBox(-0.5F, -4.5F, -4.5F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(37, 4)
            .addBox(-0.5F, -6.5F, -7.5F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
            .texOffs(46, 3)
            .addBox(-4.5F, -8.5F, -6.5F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(40, 4)
            .addBox(-4.5F, -8.5F, -4.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(63, 1)
            .addBox(-2.5F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(75, 5)
            .addBox(-5.5F, -5.5F, -0.5F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(71, 0)
            .addBox(-7.5F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(4.0F, 0.0F, 4.0F)
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
            .addBox(-0.4F, -1.6F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 17)
            .addBox(-0.4F, -0.9F, -0.2F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
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
      this.Head.xRot = Mth.clamp(headPitch, -18.0F, 18.0F) * (float) (Math.PI / 180.0);
      this.Head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (float) (Math.PI / 180.0);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.Legs.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
