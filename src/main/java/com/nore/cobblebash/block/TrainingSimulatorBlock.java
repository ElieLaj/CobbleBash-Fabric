package com.nore.cobblebash.block;

import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.item.TrainingDiskItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TrainingSimulatorBlock extends Block {
    public TrainingSimulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(stack.getItem() instanceof TrainingDiskItem trainingDisk)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer
                && GymCommand.enterGym(serverPlayer, trainingDisk.getGymType().getId())) {
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.FAIL;
    }
}
