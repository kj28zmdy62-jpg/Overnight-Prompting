package com.noclip.block;

import com.noclip.levels.ElevatorShaft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Elevator Shaft call button. It works. That is the problem. */
public class CallButtonBlock extends BoxFacingBlock {
    public CallButtonBlock(Properties props) {
        super(props.noCollission(), new double[][]{{5, 3, 0, 11, 13, 1}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) ElevatorShaft.summon(sl, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
