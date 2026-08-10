package com.nore.cobblebash.block;

import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.item.EliteFourTrainingDiskItem;
import com.nore.cobblebash.item.TrainingDiskItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainingSimulatorBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   private static final VoxelShape LOWER_SHAPE_SOUTH = Shapes.or(
      Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 14.0),
      new VoxelShape[]{
         Block.box(1.0, 10.0, 1.0, 15.0, 13.0, 15.0),
         Block.box(1.0, 13.0, 1.0, 15.0, 15.0, 10.0),
         Block.box(2.0, 13.0, 11.0, 4.0, 14.0, 13.0),
         Block.box(5.0, 13.0, 11.0, 7.0, 14.0, 13.0),
         Block.box(8.0, 13.0, 11.0, 10.0, 14.0, 13.0),
         Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0),
         Block.box(1.0, 15.0, 1.0, 2.0, 16.0, 10.0),
         Block.box(2.0, 15.0, 1.0, 10.0, 16.0, 9.0),
         Block.box(10.0, 15.0, 1.0, 15.0, 16.0, 10.0)
      }
   );
   private static final VoxelShape UPPER_SHAPE_SOUTH = Shapes.or(
      Block.box(3.0, 0.0, 0.0, 13.0, 6.0, 1.0),
      new VoxelShape[]{
         Block.box(1.0, 0.0, 1.0, 2.0, 8.0, 10.0),
         Block.box(2.0, 0.0, 1.0, 10.0, 7.0, 9.0),
         Block.box(10.0, 0.0, 1.0, 15.0, 7.0, 10.0),
         Block.box(2.0, 7.0, 1.0, 15.0, 8.0, 10.0)
      }
   );
   private static final Map<Direction, VoxelShape> LOWER_SHAPES = makeHorizontalShapes(LOWER_SHAPE_SOUTH);
   private static final Map<Direction, VoxelShape> UPPER_SHAPES = makeHorizontalShapes(UPPER_SHAPE_SOUTH);

   public TrainingSimulatorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(HALF, DoubleBlockHalf.LOWER)
      );
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos blockpos = context.getClickedPos();
      Level level = context.getLevel();
      return blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)
         ? (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()))
            .setValue(HALF, DoubleBlockHalf.LOWER)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.setBlock(pos.above(), (BlockState)state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.isClientSide && !state.is(newState.getBlock())) {
         BlockPos blockpos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
         BlockState blockstate = level.getBlockState(blockpos);
         if (blockstate.is(this) && blockstate.getValue(HALF) != state.getValue(HALF)) {
            level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
         }
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShapeForState(state);
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShapeForState(state);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, HALF});
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
         pos = pos.below();
         state = level.getBlockState(pos);
         if (!state.is(this) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return ItemInteractionResult.FAIL;
         }
      }

      if (stack.getItem() instanceof EliteFourTrainingDiskItem) {
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         }

         if (player instanceof ServerPlayer serverplayer && GymCommand.enterEliteFour(serverplayer, false)) {
            if (!serverplayer.getAbilities().instabuild) {
               stack.shrink(1);
            }

            return ItemInteractionResult.SUCCESS;
         } else {
            return ItemInteractionResult.FAIL;
         }
      } else if (!(stack.getItem() instanceof TrainingDiskItem trainingdiskitem)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         }

         // Au lieu d'entrer tout de suite, on demande le niveau. Le disque
         // n'est consomme qu'a la confirmation, dans GymLevelMenu.
         if (player instanceof ServerPlayer serverplayer1) {
            String gymId = trainingdiskitem.getGymType().getId();
            serverplayer1.openMenu(new com.nore.cobblebash.gymlevel.GymLevelProvider(gymId));
            return ItemInteractionResult.SUCCESS;
         } else {
            return ItemInteractionResult.FAIL;
         }
      }
   }

   private static VoxelShape getShapeForState(BlockState state) {
      Direction direction = (Direction)state.getValue(FACING);
      return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES.get(direction) : LOWER_SHAPES.get(direction);
   }

   private static Map<Direction, VoxelShape> makeHorizontalShapes(VoxelShape southShape) {
      EnumMap<Direction, VoxelShape> enummap = new EnumMap<>(Direction.class);

      for (Direction direction : Plane.HORIZONTAL) {
         enummap.put(direction, rotateSouthShape(southShape, direction));
      }

      return enummap;
   }

   private static VoxelShape rotateSouthShape(VoxelShape shape, Direction direction) {
      if (direction == Direction.SOUTH) {
         return shape;
      }

      VoxelShape[] avoxelshape = new VoxelShape[]{Shapes.empty()};
      shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
         VoxelShape voxelshape = switch (direction) {
            case NORTH -> Block.box(16.0 - maxX * 16.0, minY * 16.0, 16.0 - maxZ * 16.0, 16.0 - minX * 16.0, maxY * 16.0, 16.0 - minZ * 16.0);
            case EAST -> Block.box(minZ * 16.0, minY * 16.0, 16.0 - maxX * 16.0, maxZ * 16.0, maxY * 16.0, 16.0 - minX * 16.0);
            case WEST -> Block.box(16.0 - maxZ * 16.0, minY * 16.0, minX * 16.0, 16.0 - minZ * 16.0, maxY * 16.0, maxX * 16.0);
            default -> throw new IllegalStateException("Unexpected horizontal direction: " + direction);
         };
         avoxelshape[0] = Shapes.or(avoxelshape[0], voxelshape);
      });
      return avoxelshape[0];
   }
}
