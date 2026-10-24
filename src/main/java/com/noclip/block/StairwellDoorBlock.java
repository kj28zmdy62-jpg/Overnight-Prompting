package com.noclip.block;

import com.noclip.levels.Stairwell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** A steel fire door on a Stairwell landing. Locked, unless the number beside it is the right one. */
public class StairwellDoorBlock extends ShapedBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public StairwellDoorBlock(Properties props) {
        super(props, List.of(new double[]{0, 0, 0, 16, 16, 16}), true);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Stairwell.onDoor(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
