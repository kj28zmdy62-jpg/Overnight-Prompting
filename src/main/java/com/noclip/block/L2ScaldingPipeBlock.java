package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Level 2: a vertical steam riser glowing with heat. Its collision box is a little
 * narrower than the block, so brushing against it means touching it, and it burns.
 */
public class L2ScaldingPipeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape OUTLINE = Block.box(2, 0, 2, 14, 16, 14);

    public L2ScaldingPipeBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        if (living.getBoundingBox().intersects(SHAPE.bounds().move(pos).inflate(0.08)) && level.getGameTime() % 10 == 0) {
            if (living.hurt(level.damageSources().hotFloor(), 2.5f)) {
                level.playSound(null, pos, ModSounds.get("event.level_2_scald"), SoundSource.BLOCKS, 0.8f,
                        0.9f + level.random.nextFloat() * 0.2f);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 + Math.cos(a) * 0.4, pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.5 + Math.sin(a) * 0.4, 0, 0.02, 0);
        }
    }
}
