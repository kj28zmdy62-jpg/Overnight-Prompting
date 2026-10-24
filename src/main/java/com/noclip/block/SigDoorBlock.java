package com.noclip.block;

import com.noclip.levels.TheSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Signal: the transmitter room door. It opens only for someone who has found the frequency. */
public class SigDoorBlock extends Block {
    public SigDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) TheSignal.door(sl, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
