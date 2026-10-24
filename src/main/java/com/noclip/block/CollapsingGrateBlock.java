package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Water Treatment corroded grating: stand on it and it groans, then drops out from under you. */
public class CollapsingGrateBlock extends Block {
    public CollapsingGrateBlock(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player p && !p.isCreative() && !p.isSpectator()
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 12);
            level.playSound(null, pos, ModSounds.get("event.water_treatment_creak"), SoundSource.BLOCKS, 1.0f, 1.3f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.levelEvent(2001, pos, Block.getId(state));
        level.removeBlock(pos, false);
        level.playSound(null, pos, ModSounds.get("event.water_treatment_collapse"), SoundSource.BLOCKS, 1.2f, 1f);
    }
}
