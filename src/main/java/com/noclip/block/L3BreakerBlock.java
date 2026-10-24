package com.noclip.block;

import com.mojang.serialization.MapCodec;
import com.noclip.levels.Level3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Level 3: the main breaker. Pull it and every machine on the level stops, and every light goes out. */
public class L3BreakerBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape N = Block.box(2, 1, 0, 14, 15, 6);
    private static final VoxelShape S = Block.box(2, 1, 10, 14, 15, 16);
    private static final VoxelShape W = Block.box(0, 1, 2, 6, 15, 14);
    private static final VoxelShape E = Block.box(10, 1, 2, 16, 15, 14);

    public L3BreakerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> S;
            case WEST -> W;
            case EAST -> E;
            default -> N;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) Level3.pullBreaker(sl, pos, sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
