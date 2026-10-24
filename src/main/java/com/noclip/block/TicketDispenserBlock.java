package com.noclip.block;

import com.noclip.levels.WaitingRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** The Waiting Room's "TAKE A NUMBER" machine. Right-click for a ticket. */
public class TicketDispenserBlock extends ShapedBlock {
    public TicketDispenserBlock(Properties props) {
        super(props, List.of(new double[]{4, 0, 5, 12, 10, 11}, new double[]{6, 3, 4, 10, 4, 5},
                new double[]{5, 10, 6, 11, 12, 10}), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) WaitingRoom.takeTicket(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
