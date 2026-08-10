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
import net.minecraft.world.entity.Entity;

public class ChampionRibbonModel<T extends Entity> extends EntityModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("cobblebash", "champion_ribbon"), "main"
   );
   private final ModelPart championRibbon;

   public ChampionRibbonModel(ModelPart root) {
      this.championRibbon = root.getChild("champion_ribbon");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "champion_ribbon",
         CubeListBuilder.create()
            .texOffs(14, 24)
            .addBox(-1.9963F, 1.002F, -0.45F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(16, 24)
            .addBox(-0.4963F, 4.002F, -0.45F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-2.4963F, -3.498F, -0.45F, 5.0F, 6.0F, 1.0F, new CubeDeformation(0.01F))
            .texOffs(15, 3)
            .addBox(-1.5063F, -1.488F, -0.47F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
            .texOffs(7, 23)
            .addBox(1.2966F, -4.498F, -0.45F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 24)
            .addBox(-2.2963F, -4.498F, -0.45F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(3, 21)
            .addBox(-0.4963F, -4.998F, -0.45F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(4.0F, 7.2F, -7.0F)
      );
      partdefinition1.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(11, 14).addBox(0.0F, 0.0F, -0.01F, 1.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.2463F, 0.602F, 0.05F, 0.0F, 0.0F, 0.2182F)
      );
      partdefinition1.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(0, 13).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-0.7463F, 0.702F, 0.05F, 0.0F, 0.0F, 0.2182F)
      );
      partdefinition1.addOrReplaceChild(
         "cube_r3",
         CubeListBuilder.create().texOffs(17, 13).addBox(0.0F, 0.0F, -0.02F, 1.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.2537F, 0.802F, 0.05F, 0.0F, 0.0F, -0.2182F)
      );
      partdefinition1.addOrReplaceChild(
         "cube_r4",
         CubeListBuilder.create().texOffs(26, 10).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.7537F, 0.702F, 0.05F, 0.0F, 0.0F, -0.2182F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.championRibbon.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }
}
