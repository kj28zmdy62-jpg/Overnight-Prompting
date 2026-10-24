package com.noclip.block;

import com.noclip.levels.BoilerRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Boiler Room bleed valve: turn it to vent pressure, and stand back from the steam. */
public class ValveWheelBlock extends BoxFacingBlock {
    public ValveWheelBlock(Properties props) {
        super(props, new double[][]{{2, 2, 9, 14, 14, 12}, {7, 7, 0, 9, 9, 9}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) BoilerRoom.bleed(sl, pos, state.getValue(FACING), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
