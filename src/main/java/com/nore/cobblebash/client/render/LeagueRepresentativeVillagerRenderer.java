package com.nore.cobblebash.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.client.model.LeagueRepresentativeVillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

public class LeagueRepresentativeVillagerRenderer extends MobRenderer<Villager, LeagueRepresentativeVillagerModel<Villager>> {
   private static final ResourceLocation LEAGUE_REPRESENTATIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "cobblebash", "textures/entity/villager/league_rep_villager.png"
   );
   private final VillagerRenderer vanillaRenderer;

   public LeagueRepresentativeVillagerRenderer(Context context) {
      super(context, new LeagueRepresentativeVillagerModel(context.bakeLayer(LeagueRepresentativeVillagerModel.LAYER_LOCATION)), 0.5F);
      this.vanillaRenderer = new VillagerRenderer(context);
   }

   public void render(Villager villager, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      if (villager.getVillagerData().getProfession() != CobbleBash.LEAGUE_REPRESENTATIVE) {
         this.vanillaRenderer.render(villager, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      } else {
         super.render(villager, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      }
   }

   public ResourceLocation getTextureLocation(Villager villager) {
      return LEAGUE_REPRESENTATIVE_TEXTURE;
   }

   protected void scale(Villager villager, PoseStack poseStack, float partialTick) {
      float f = 0.9375F * villager.getAgeScale();
      poseStack.scale(f, f, f);
   }
}
