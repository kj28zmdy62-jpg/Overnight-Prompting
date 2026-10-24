package com.noclip.block;

import com.noclip.levels.Laundromat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Laundromat: a dryer tumbling clothes that look a lot like yours. You can open it. */
public class LaundryDryerBlock extends ShapedBlock {
    public LaundryDryerBlock(Properties props) {
        super(props, List.of(new double[]{0, 0, 1, 16, 16, 16}), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Laundromat.openDryer(sp, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
