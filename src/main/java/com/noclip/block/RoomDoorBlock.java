package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One of the hundreds of freestanding doors in the Room of Doors. A door is
 * "clean" only when neither half carries a mark: no scratches, no handprints, no chalk.
 */
public class RoomDoorBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 6, 16, 16, 10);

    public RoomDoorBlock(Properties props) {
        super(props);
    }

    static String name(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            String me = name(state);
            BlockPos otherPos = me.contains("_low_") ? pos.above() : pos.below();
            BlockState other = level.getBlockState(otherPos);
            boolean clean = me.endsWith("_clean") && other.getBlock() instanceof RoomDoorBlock && name(other).endsWith("_clean");
            com.noclip.levels.RoomOfDoors.useDoor(sp, pos, clean);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
