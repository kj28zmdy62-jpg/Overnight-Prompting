package com.noclip.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Level 5: a brass room-number plate. Three digit properties, so every door can carry its own number. */
public class L5PlaqueBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty HUNDREDS = IntegerProperty.create("hundreds", 0, 9);
    public static final IntegerProperty TENS = IntegerProperty.create("tens", 0, 9);
    public static final IntegerProperty UNITS = IntegerProperty.create("units", 0, 9);
    private static final VoxelShape N = Block.box(3.5, 4, 0, 12.5, 9, 1);
    private static final VoxelShape S = Block.box(3.5, 4, 15, 12.5, 9, 16);
    private static final VoxelShape W = Block.box(0, 4, 3.5, 1, 9, 12.5);
    private static final VoxelShape E = Block.box(15, 4, 3.5, 16, 9, 12.5);

    public L5PlaqueBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HUNDREDS, 0).setValue(TENS, 0).setValue(UNITS, 0));
    }

    public BlockState withNumber(int n) {
        n = Math.max(0, Math.min(999, n));
        return defaultBlockState().setValue(HUNDREDS, n / 100).setValue(TENS, (n / 10) % 10).setValue(UNITS, n % 10);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HUNDREDS, TENS, UNITS);
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }
}
