package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A door in the Recursion sitting room. Opening it decides whether you leave, or go round again. */
public class RecursionDoorBlock extends Block {
    public RecursionDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) com.noclip.levels.Recursion.useDoor(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
