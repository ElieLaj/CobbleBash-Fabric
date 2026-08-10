package com.nore.cobblebash.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Rendu du ruban sur le porteur.
 *
 * <p>Remplace le {@code RibbonCurioRenderer} de la version NeoForge. Accessories
 * passe directement le modele du porteur la ou Curios passait le
 * {@code RenderLayerParent}, et fournit {@code followBodyRotations} pour aligner
 * un modele accessoire sur le buste — ce que l'original faisait a la main.
 */
public class RibbonAccessoryRenderer implements AccessoryRenderer {
   private final EntityModel<LivingEntity> model;
   private final ResourceLocation texture;

   @SuppressWarnings("unchecked")
   public RibbonAccessoryRenderer(EntityModel<? extends LivingEntity> model, ResourceLocation texture) {
      this.model = (EntityModel<LivingEntity>) model;
      this.texture = texture;
   }

   @Override
   public <M extends LivingEntity> void render(
      ItemStack stack,
      SlotReference reference,
      PoseStack poseStack,
      net.minecraft.client.model.EntityModel<M> parentModel,
      MultiBufferSource buffer,
      int packedLight,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      LivingEntity wearer = reference.entity();
      if (wearer == null) {
         return;
      }

      if (parentModel instanceof HumanoidModel<?> humanoid && this.model instanceof HumanoidModel<?> ribbon) {
         // Aligne le ruban sur le buste : sans ca il flotte quand le porteur
         // se penche ou s'accroupit.
         AccessoryRenderer.followBodyRotations(wearer, (HumanoidModel<LivingEntity>) ribbon);
      }

      this.model.setupAnim(wearer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.texture));
      this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
   }
}
