package com.noclip.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

/** Airport: a moving walkway. Anything standing on it is carried along its facing, whether it wants to go or not. */
public class AirportWalkwayBlock extends HorizontalDirectionalBlock {
    public AirportWalkwayBlock(Properties props) {
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
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Direction d = state.getValue(FACING);
        Vec3 v = entity.getDeltaMovement();
        double along = v.x * d.getStepX() + v.z * d.getStepZ();
        if (along < 0.28) entity.setDeltaMovement(v.add(d.getStepX() * 0.045, 0, d.getStepZ() * 0.045));
        super.stepOn(level, pos, state, entity);
    }
}
