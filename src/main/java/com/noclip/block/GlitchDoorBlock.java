package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A torn doorway in Level ???. It goes wherever the level is calling itself at the moment you step through. */
public class GlitchDoorBlock extends Block {
    public GlitchDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ServerPlayer p) com.noclip.levels.GlitchLevel.enterDoor(p);
    }
}
