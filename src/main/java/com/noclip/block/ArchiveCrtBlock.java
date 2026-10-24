package com.noclip.block;

import com.noclip.levels.Archive;
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

/** A viewing-booth television in the Archive. tape: 0 idle, 1 static, 2 playing you. */
public class ArchiveCrtBlock extends ShapedBlock {
    public static final IntegerProperty TAPE = IntegerProperty.create("tape", 0, 2);

    public ArchiveCrtBlock(Properties props) {
        super(props, List.of(new double[]{2, -4, 3, 14, 9, 14}, new double[]{4, -2, 14, 12, 7, 16}), true);
        registerDefaultState(defaultBlockState().setValue(TAPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TAPE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Archive.onCrt(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
