package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Level 3: bare cable, still carrying current. Walk through it and it bites. It spits sparks. */
public class L3LiveWireBlock extends HazardBlock {
    private static final VoxelShape OUTLINE = Block.box(3, 0, 3, 13, 16, 13);

    public L3LiveWireBlock(Properties props) {
        super(props, 3.0f, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        int n = 2 + random.nextInt(6);
        double y = pos.getY() + 0.1 + random.nextDouble() * 0.5;
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.35 + random.nextDouble() * 0.3, y,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4,
                    random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.4);
        }
        if (random.nextInt(3) == 0) {
            level.playLocalSound(pos.getX() + 0.5, y, pos.getZ() + 0.5, ModSounds.get("event.level_3_zap"), SoundSource.BLOCKS,
                    0.35f, 0.8f + random.nextFloat() * 0.5f, false);
        }
    }
}
