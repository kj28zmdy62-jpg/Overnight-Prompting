package com.noclip.block;

import com.noclip.levels.Motel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Roadside Motel: the inside of room 6's door. It never lets you out where you came in. */
public class MotelExitDoorBlock extends Block {
    public MotelExitDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Motel.leaveRoom(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
