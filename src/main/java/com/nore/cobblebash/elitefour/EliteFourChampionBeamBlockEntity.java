package com.nore.cobblebash.elitefour;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EliteFourChampionBeamBlockEntity extends BlockEntity {
   public EliteFourChampionBeamBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)CobbleBash.ELITE_FOUR_CHAMPION_BEAM_BLOCK_ENTITY, pos, blockState);
   }
}
