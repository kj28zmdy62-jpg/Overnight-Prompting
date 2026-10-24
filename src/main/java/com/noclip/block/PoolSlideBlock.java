package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Level 37's water slide. Shaped like stairs, but slick: anything standing on it
 * is carried down the slope, faster and faster, into whatever pool is below.
 */
public class PoolSlideBlock extends StairBlock {
    public PoolSlideBlock(Properties props) {
        super(Blocks.QUARTZ_BLOCK.defaultBlockState(), props.friction(0.98f));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (entity.isShiftKeyDown()) return;
        // stairs facing F rise toward F, so "downhill" is the opposite way
        Direction down = state.getValue(FACING).getOpposite();
        Vec3 v = entity.getDeltaMovement();
        double along = v.x * down.getStepX() + v.z * down.getStepZ();
        // players move on their own client; everything else is moved by the server
        boolean owner = entity instanceof net.minecraft.world.entity.player.Player ? level.isClientSide : !level.isClientSide;
        if (owner && along < 0.62) {
            double push = 0.09;
            entity.setDeltaMovement(v.x + down.getStepX() * push, v.y, v.z + down.getStepZ() * push);
        }
        if (!level.isClientSide && level.random.nextInt(14) == 0) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.25f, 1.6f);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance, 0.0f, level.damageSources().fall());
    }

    /** Plays the big splash when something leaves the slide into water. Called by the level behaviour. */
    public static void splash(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.get("event.level_37_splash"), SoundSource.AMBIENT, 1f, 0.9f + level.random.nextFloat() * 0.2f);
    }
}
