package com.noclip.block;

import com.noclip.levels.LockerRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Locker 1313. Something inside knocks when you look away. */
public class LockerOddBlock extends ShapedBlock {
    public LockerOddBlock(Properties props) {
        super(props, List.of(new double[]{0, 0, 0, 16, 16, 16}), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) LockerRooms.onOdd(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
