package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Indoor Playground: plastic balls. You sink into them slowly, and nobody can see you down there. */
public class BallPitBlock extends Block {
    public BallPitBlock(Properties props) {
        super(props);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, new Vec3(0.75, 0.35, 0.75));
        if (!level.isClientSide && entity.getDeltaMovement().horizontalDistanceSqr() > 0.0004 && level.random.nextInt(25) == 0)
            level.playSound(null, pos, ModSounds.get("event.playground_balls"), SoundSource.BLOCKS, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
    }
}
