package com.noclip.block;

import com.noclip.levels.BingoHall;
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

import java.util.List;

/** A bingo card on a folding table. Somebody is playing it. Claim it and it is yours. */
public class BingoCardBlock extends ShapedBlock {
    public static final IntegerProperty DAUBS = IntegerProperty.create("daubs", 0, 4);

    public BingoCardBlock(Properties props) {
        super(props, List.of(new double[]{3, -4, 2, 13, -2, 14}), false);
        registerDefaultState(defaultBlockState().setValue(DAUBS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DAUBS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) BingoHall.onCard(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
