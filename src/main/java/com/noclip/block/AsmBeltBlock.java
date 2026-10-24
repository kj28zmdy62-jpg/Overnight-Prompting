package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Assembly Line conveyor: anything standing on it is carried toward the back of the block (the way the belt runs). */
public class AsmBeltBlock extends BoxFacingBlock {
    public AsmBeltBlock(Properties props) {
        super(props, new double[][]{{0, 0, 0, 16, 16, 16}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The direction the belt carries things. */
    public static Direction runs(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Direction d = runs(state);
        Vec3 v = entity.getDeltaMovement();
        double along = v.x * d.getStepX() + v.z * d.getStepZ();
        if (along < 0.22) entity.setDeltaMovement(v.add(d.getStepX() * 0.05, 0, d.getStepZ() * 0.05));
        super.stepOn(level, pos, state, entity);
    }
}
