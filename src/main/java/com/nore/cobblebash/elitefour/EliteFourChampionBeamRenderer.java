package com.nore.cobblebash.elitefour;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nore.cobblebash.structure.EliteFourStructure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EliteFourChampionBeamRenderer implements BlockEntityRenderer<EliteFourChampionBeamBlockEntity> {
   private static final int BEAM_COLOR = -15720320;

   public EliteFourChampionBeamRenderer(Context context) {
   }

   public void render(
      EliteFourChampionBeamBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      if (blockEntity.getLevel() != null) {
         BeaconRenderer.renderBeaconBeam(
            poseStack,
            bufferSource,
            BeaconRenderer.BEAM_LOCATION,
            partialTick,
            1.0F,
            blockEntity.getLevel().getGameTime(),
            0,
            EliteFourStructure.CHAMPION_BEAM_HEIGHT,
            -15720320,
            0.2F,
            0.25F
         );
      }
   }

   public boolean shouldRenderOffScreen(EliteFourChampionBeamBlockEntity blockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 256;
   }

   public boolean shouldRender(EliteFourChampionBeamBlockEntity blockEntity, Vec3 cameraPos) {
      return Vec3.atCenterOf(blockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }

   public AABB getRenderBoundingBox(EliteFourChampionBeamBlockEntity blockEntity) {
      BlockPos blockpos = blockEntity.getBlockPos();
      return new AABB(
         blockpos.getX(),
         blockpos.getY(),
         blockpos.getZ(),
         blockpos.getX() + 1.0,
         blockpos.getY() + EliteFourStructure.CHAMPION_BEAM_HEIGHT,
         blockpos.getZ() + 1.0
      );
   }
}
