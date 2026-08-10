package com.nore.cobblebash.beacon;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChampionBeaconRenderer implements BlockEntityRenderer<ChampionBeaconBlockEntity> {
   private static final int BEAM_COLOR = -1;

   public ChampionBeaconRenderer(Context context) {
   }

   public void render(
      ChampionBeaconBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      if (blockEntity.hasBeam() && blockEntity.getLevel() != null) {
         long i = blockEntity.getLevel().getGameTime();
         int j = 0;
         List<ChampionBeaconRenderer.BeamSection> list = collectBeamSections(blockEntity.getLevel(), blockEntity.getBlockPos());

         for (int k = 0; k < list.size(); k++) {
            ChampionBeaconRenderer.BeamSection championbeaconrenderer$beamsection = list.get(k);
            int l = k == list.size() - 1 ? 1024 : championbeaconrenderer$beamsection.height;
            BeaconRenderer.renderBeaconBeam(
               poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F, i, j, l, championbeaconrenderer$beamsection.color, 0.2F, 0.25F
            );
            j += championbeaconrenderer$beamsection.height;
         }
      }
   }

   private static List<ChampionBeaconRenderer.BeamSection> collectBeamSections(Level level, BlockPos beaconPos) {
      List<ChampionBeaconRenderer.BeamSection> list = new ArrayList<>();
      ChampionBeaconRenderer.BeamSection championbeaconrenderer$beamsection = new ChampionBeaconRenderer.BeamSection(-1);
      list.add(championbeaconrenderer$beamsection);
      int i = level.getHeight();

      for (BlockPos blockpos = beaconPos.above(); blockpos.getY() < i; blockpos = blockpos.above()) {
         BlockState blockstate = level.getBlockState(blockpos);
         Integer integer = beaconColor(blockstate);
         if (integer != null) {
            if (championbeaconrenderer$beamsection.color == -1 && championbeaconrenderer$beamsection.height == 1) {
               championbeaconrenderer$beamsection.color = integer;
            } else if (championbeaconrenderer$beamsection.color == integer) {
               championbeaconrenderer$beamsection.height++;
            } else {
               championbeaconrenderer$beamsection = new ChampionBeaconRenderer.BeamSection(ARGB32.average(championbeaconrenderer$beamsection.color, integer));
               list.add(championbeaconrenderer$beamsection);
            }
         } else {
            if (blockstate.getLightBlock(level, blockpos) >= 15 && !blockstate.is(Blocks.BEDROCK)) {
               return List.of();
            }

            championbeaconrenderer$beamsection.height++;
         }
      }

      return list;
   }

   public boolean shouldRenderOffScreen(ChampionBeaconBlockEntity blockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 256;
   }

   public boolean shouldRender(ChampionBeaconBlockEntity blockEntity, Vec3 cameraPos) {
      return Vec3.atCenterOf(blockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }

   public AABB getRenderBoundingBox(ChampionBeaconBlockEntity blockEntity) {
      BlockPos blockpos = blockEntity.getBlockPos();
      return new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos.getX() + 1.0, 1024.0, blockpos.getZ() + 1.0);
   }

   private static class BeamSection {
      private int color;
      private int height = 1;

      private BeamSection(int color) {
         this.color = color;
      }
   }

   /**
    * Couleur que ce bloc donne au faisceau, ou null s'il l'arrete.
    *
    * <p>Remplace {@code BlockState.getBeaconColorMultiplier}, ajoute a vanilla
    * par NeoForge. La balise vanilla fait le meme test : seul un
    * {@link BeaconBeamBlock} teinte le faisceau.
    */
   private static Integer beaconColor(BlockState state) {
      return state.getBlock() instanceof BeaconBeamBlock beam
         ? beam.getColor().getTextureDiffuseColor()
         : null;
   }

}
