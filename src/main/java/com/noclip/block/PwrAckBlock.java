package com.noclip.block;

import com.noclip.levels.PowerPlant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Power Plant: the alarm acknowledge console. Press it before the turbines trip. */
public class PwrAckBlock extends BoxFacingBlock {
    public PwrAckBlock(Properties props) {
        super(props, new double[][]{{0, 0, 0, 16, 12, 16}, {5, 12, 5, 11, 15, 11}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) PowerPlant.acknowledge(sl, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
