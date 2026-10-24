package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Movie Theater: years of spilled soda. Your shoes stick to it. */
public class StickyFloorBlock extends Block {
    public StickyFloorBlock(Properties props) {
        super(props.speedFactor(0.55f).jumpFactor(0.6f));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
    }
}
