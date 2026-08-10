package com.nore.cobblebash.block;

import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EliteFourPlaqueBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE_SOUTH = Shapes.or(Block.box(-8.5, -4.5, 14.0, 24.5, 20.5, 16.0), new VoxelShape[0]);
   private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(SHAPE_SOUTH);
   private static final int CLEAR_FLAGS = 35;
   private static final int GATE_HALF_WIDTH = 1;
   private static final int GATE_MIN_HEIGHT_OFFSET = -1;
   private static final int GATE_MAX_HEIGHT_OFFSET = 2;
   private static final int GATE_DEPTH = 9;
   private final EliteFourMember member;

   public EliteFourPlaqueBlock(EliteFourMember member, Properties properties) {
      super(properties);
      this.member = member;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.getValue(FACING));
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         return level instanceof ServerLevel serverlevel && !this.tryOpenGate(serverlevel, pos, (Direction)state.getValue(FACING), player)
            ? InteractionResult.SUCCESS
            : InteractionResult.SUCCESS;
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   private boolean tryOpenGate(ServerLevel level, BlockPos pos, Direction facing, Player player) {
      if (player instanceof ServerPlayer serverplayer) {
         GymInstance gyminstance = GymInstanceManager.getActive(serverplayer.getUUID());
         if (gyminstance == null || !"elite4".equals(gyminstance.getGymType())) {
            serverplayer.sendSystemMessage(Component.literal("This Elite Four plaque is inactive."));
            return false;
         }

         if (gyminstance.hasDefeatedEliteFourMember(this.member.getId())) {
            serverplayer.sendSystemMessage(Component.literal(this.member.getDisplayName() + " is already defeated."));
            return false;
         }

         if (gyminstance.hasActiveEliteFourMember()) {
            if (!gyminstance.getActiveEliteFourMember().equals(this.member.getId())) {
               serverplayer.sendSystemMessage(Component.literal("Complete previous Elite Four member."));
               return false;
            } else {
               serverplayer.sendSystemMessage(Component.literal(this.member.getDisplayName() + " gate is already open."));
               return false;
            }
         } else if (!gyminstance.selectEliteFourMember(this.member.getId())) {
            serverplayer.sendSystemMessage(Component.literal("Complete previous Elite Four member."));
            return false;
         } else {
            openGate(level, pos, facing);
            level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverplayer.sendSystemMessage(Component.literal(this.member.getDisplayName() + " gate opened."));
            return true;
         }
      } else {
         return false;
      }
   }

   public static void openGate(ServerLevel level, BlockPos plaquePos, Direction facing) {
      Direction direction = facing.getAxis() == Axis.X ? Direction.NORTH : Direction.EAST;

      for (int i = 1; i <= 9; i++) {
         BlockPos blockpos = plaquePos.relative(facing, i);

         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 2; k++) {
               BlockPos blockpos1 = blockpos.relative(direction, j).offset(0, k, 0);
               level.setBlock(blockpos1, Blocks.AIR.defaultBlockState(), 35);
            }
         }
      }

      level.setBlock(plaquePos, Blocks.AIR.defaultBlockState(), 35);
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
