package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;

public class LeagueRepresentativeVillagerModel<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("cobblebash", "league_rep_villager"), "main"
   );
   private final ModelPart head;
   private final ModelPart nose;
   private final ModelPart body;
   private final ModelPart arms;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private final ModelPart rightShoe;
   private final ModelPart leftShoe;
   private final ModelPart belt;
   private final ModelPart hat;

   public LeagueRepresentativeVillagerModel(ModelPart root) {
      this.head = root.getChild("head");
      this.nose = this.head.getChild("nose");
      this.body = root.getChild("body");
      this.arms = root.getChild("arms");
      this.rightLeg = root.getChild("right_leg");
      this.leftLeg = root.getChild("left_leg");
      this.rightShoe = this.rightLeg.getChild("right_shoe");
      this.leftShoe = this.leftLeg.getChild("left_shoe");
      this.belt = root.getChild("belt");
      this.hat = this.head.getChild("hat");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "nose",
         CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -2.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "hat",
         CubeListBuilder.create()
            .texOffs(0, 38)
            .addBox(-4.0F, -2.4F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.1F))
            .texOffs(0, 51)
            .addBox(-4.0F, 1.8F, -8.0F, 8.0F, 1.0F, 12.0F, new CubeDeformation(0.1F)),
         PartPose.offset(0.0F, -8.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(16, 18).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
         "arms",
         CubeListBuilder.create()
            .texOffs(40, 33)
            .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(48, 21)
            .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 2.95F, -1.05F, -0.7505F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "mirrored",
         CubeListBuilder.create().texOffs(48, 9).mirror().addBox(4.0F, -23.05F, -3.05F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 21.05F, 1.05F)
      );
      PartDefinition partdefinition3 = partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(47, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      PartDefinition partdefinition4 = partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      partdefinition3.addOrReplaceChild(
         "right_shoe",
         CubeListBuilder.create()
            .texOffs(52, 0)
            .addBox(-5.8F, -1.0F, 0.6F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F))
            .texOffs(52, 3)
            .addBox(-5.8F, -1.0F, 3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F))
            .texOffs(52, 6)
            .addBox(-5.8F, -2.4F, 3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F)),
         PartPose.offset(3.9F, 12.0F, -3.0F)
      );
      partdefinition4.addOrReplaceChild(
         "left_shoe",
         CubeListBuilder.create()
            .texOffs(40, 0)
            .addBox(-2.0F, -1.0F, 0.6F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F))
            .texOffs(40, 3)
            .addBox(-2.0F, -1.0F, 3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F))
            .texOffs(40, 6)
            .addBox(-2.0F, -2.4F, 3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.2F)),
         PartPose.offset(-0.1F, 12.0F, -3.0F)
      );
      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild(
         "belt",
         CubeListBuilder.create()
            .texOffs(36, 41)
            .addBox(-1.0F, -3.5F, -2.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.1F))
            .texOffs(0, 0)
            .addBox(2.0F, -3.5F, -2.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.2F)),
         PartPose.offset(-3.0F, 14.0F, -1.0F)
      );
      PartDefinition partdefinition6 = partdefinition5.addOrReplaceChild(
         "pokeball_satchel_left",
         CubeListBuilder.create()
            .texOffs(32, 0)
            .addBox(-1.3F, -3.8F, 0.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(32, 4)
            .addBox(-1.1F, -1.6F, -0.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.2F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition6.addOrReplaceChild(
         "pokeball_holder_left",
         CubeListBuilder.create()
            .texOffs(18, 18)
            .addBox(-1.3F, -0.9F, 1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 18)
            .addBox(-1.3F, -0.9F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 18)
            .addBox(-1.3F, -2.7F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 18)
            .addBox(-1.4F, -1.5F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition6.addOrReplaceChild(
         "pokeball_left",
         CubeListBuilder.create()
            .texOffs(0, 2)
            .addBox(-6.3F, -2.7F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
            .texOffs(0, 2)
            .addBox(-6.3F, -0.9F, 0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
            .texOffs(0, 2)
            .addBox(-6.3F, -0.9F, -1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),
         PartPose.offset(4.0F, 0.0F, 1.0F)
      );
      PartDefinition partdefinition7 = partdefinition5.addOrReplaceChild(
         "pokeball_satchel_right",
         CubeListBuilder.create()
            .texOffs(32, 9)
            .addBox(-1.7F, -3.8F, 0.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(32, 13)
            .addBox(-1.9F, -1.6F, -0.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.2F)),
         PartPose.offset(8.0F, 0.0F, 0.0F)
      );
      partdefinition7.addOrReplaceChild(
         "pokeball_holder_right",
         CubeListBuilder.create()
            .texOffs(18, 20)
            .addBox(-1.7F, -0.9F, 1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 20)
            .addBox(-1.7F, -0.9F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 20)
            .addBox(-1.7F, -2.7F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
            .texOffs(18, 20)
            .addBox(-1.6F, -1.5F, 0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition7.addOrReplaceChild(
         "pokeball_right",
         CubeListBuilder.create()
            .texOffs(0, 4)
            .addBox(-5.7F, -2.7F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
            .texOffs(0, 4)
            .addBox(-5.7F, -0.9F, 0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
            .texOffs(0, 4)
            .addBox(-5.7F, -0.9F, -1.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),
         PartPose.offset(5.0F, 0.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      boolean flag = entity instanceof AbstractVillager abstractvillager && abstractvillager.getUnhappyCounter() > 0;
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
      this.head.xRot = headPitch * (float) (Math.PI / 180.0);
      this.head.zRot = flag ? 0.3F * Mth.sin(0.45F * ageInTicks) : 0.0F;
      if (flag) {
         this.head.xRot = 0.4F;
      }

      this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
      this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
      this.rightLeg.yRot = 0.0F;
      this.leftLeg.yRot = 0.0F;
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.arms.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
      this.belt.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
