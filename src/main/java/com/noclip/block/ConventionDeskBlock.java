package com.noclip.block;

import com.noclip.levels.ConventionCenter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The information desk the PA keeps sending you to. Ring the bell. */
public class ConventionDeskBlock extends Block {
    public ConventionDeskBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) ConventionCenter.onDesk(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
