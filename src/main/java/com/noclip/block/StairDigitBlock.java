package com.noclip.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A floor number stencilled on the Stairwell wall. Sixteen glyphs: 0-9, blank, ?, -, B,
 * an eye, and a smear where someone painted over it. The Stairwell repaints these
 * whenever you are not looking.
 */
public class StairDigitBlock extends Block {
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 15);
    public static final int BLANK = 10, QUESTION = 11, DASH = 12, B = 13, EYE = 14, SMEAR = 15;

    public StairDigitBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(GLYPH, BLANK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLYPH);
    }
}
