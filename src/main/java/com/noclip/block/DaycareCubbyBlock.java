package com.noclip.block;

import com.noclip.levels.Daycare;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A cubby hole in the Daycare wall. Each has a name over it. One of them is yours. */
public class DaycareCubbyBlock extends Block {
    public static final IntegerProperty COAT = IntegerProperty.create("coat", 0, 2);

    public DaycareCubbyBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(COAT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COAT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Daycare.onCubby(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
