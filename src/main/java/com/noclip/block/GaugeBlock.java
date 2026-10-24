package com.noclip.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Boiler Room pressure gauge. AGE 0..3 is the needle position; the level behaviour moves it. */
public class GaugeBlock extends BoxFacingBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public GaugeBlock(Properties props) {
        super(props.noCollission(), new double[][]{{2, 2, 0, 14, 14, 1.5}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }
}
