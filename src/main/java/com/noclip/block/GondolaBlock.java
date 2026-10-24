package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiConsumer;

/** A ferris wheel car. Use it to get in; the Closed Carnival's behaviour carries you round. */
public class GondolaBlock extends Block {
    public static BiConsumer<ServerPlayer, BlockPos> onBoard = (p, pos) -> {};
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(1, 2, 1, 15, 9, 2),
            Block.box(1, 2, 14, 15, 9, 15), Block.box(1, 2, 2, 2, 9, 14), Block.box(14, 2, 2, 15, 9, 14));

    public GondolaBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) onBoard.accept(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
