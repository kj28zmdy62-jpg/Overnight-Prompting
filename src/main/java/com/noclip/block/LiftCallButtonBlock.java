package com.noclip.block;

import com.noclip.levels.LiftLobby;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** The brass call plate beside every lift in the Lift Lobby. Something always answers. */
public class LiftCallButtonBlock extends ShapedBlock {
    public LiftCallButtonBlock(Properties props) {
        super(props, List.of(new double[]{4, 3, 0, 12, 13, 1}), false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (player instanceof ServerPlayer sp) LiftLobby.onCall(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
