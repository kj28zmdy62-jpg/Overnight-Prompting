package com.noclip.block;

import com.noclip.levels.CallCenter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** A desk phone in the Call Center. Pick it up. It is for you. */
public class CallPhoneBlock extends ShapedBlock {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");

    public CallPhoneBlock(Properties props) {
        super(props, List.of(new double[]{3, -4, 3, 13, 0, 13}), false);
        registerDefaultState(defaultBlockState().setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RINGING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) CallCenter.onPhone(sp, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
