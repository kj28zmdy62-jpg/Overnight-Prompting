package com.noclip.block;

import com.noclip.level.ExitType;
import com.noclip.travel.Travel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** A framed photograph of an open door, in the Photo Gallery. Reach into it. */
public class PhotoExitBlock extends ShapedBlock {
    public PhotoExitBlock(Properties props) {
        super(props, List.of(new double[]{0, 0, 0, 16, 16, 2}), false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Travel.exit(sp, ExitType.DOOR, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
