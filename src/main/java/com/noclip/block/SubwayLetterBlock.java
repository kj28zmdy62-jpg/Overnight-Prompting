package com.noclip.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** One mosaic tile of a station name: blank or a letter A-Z. */
public class SubwayLetterBlock extends Block {
    public static final IntegerProperty LETTER = IntegerProperty.create("letter", 0, 26);

    public SubwayLetterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LETTER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LETTER);
    }

    public static int index(char c) {
        c = Character.toUpperCase(c);
        return c >= 'A' && c <= 'Z' ? c - 'A' + 1 : 0;
    }
}
