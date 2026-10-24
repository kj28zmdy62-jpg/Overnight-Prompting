package com.noclip.block;

import com.noclip.levels.TheSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Signal: a radio set. Use to tune up the band, sneak-use to tune down. */
public class SigRadioBlock extends BoxFacingBlock {
    public SigRadioBlock(Properties props) {
        super(props, new double[][]{{0, 0, 0, 16, 9, 16}, {2, 9, 4, 14, 17, 12}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) TheSignal.tune(sl, pos, player, player.isShiftKeyDown());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
