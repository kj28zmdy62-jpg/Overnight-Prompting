package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Level 2: a floor grate breathing steam in slow pulses. Standing on it scalds. */
public class L2SteamVentBlock extends HazardBlock {
    public L2SteamVentBlock(Properties props) {
        super(props, 1.5f, true);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        long phase = Math.floorMod(level.getGameTime() + pos.asLong() * 31, 120L);
        int n = phase < 30 ? 3 : (random.nextInt(4) == 0 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.02,
                    0.06 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
