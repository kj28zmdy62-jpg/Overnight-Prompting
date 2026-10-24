package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A door painted onto the wall of the Long Hallway. Try the handle: your knuckles meet plaster. */
public class PaintedDoorBlock extends Block {
    public PaintedDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.playSound(null, pos, ModSounds.get("event.long_hallway_thud"), SoundSource.BLOCKS, 0.8f,
                    0.9f + level.random.nextFloat() * 0.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
