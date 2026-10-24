package com.noclip.block;

import net.minecraft.world.level.block.Block;

/** Cold Storage floor ice: as slippery as packed ice. */
public class SlickIceBlock extends Block {
    public SlickIceBlock(Properties props) {
        super(props.friction(0.98f));
    }
}
