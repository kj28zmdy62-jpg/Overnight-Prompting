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

/** Roadside Motel: any door on the strip. They are all room 6. */
public class MotelDoorBlock extends Block {
    public MotelDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Motel.enterRoom(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
