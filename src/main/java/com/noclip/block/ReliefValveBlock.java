package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Boiler Room relief valve. It always leaks a little; when the pressure peaks it blows. */
public class ReliefValveBlock extends BoxFacingBlock {
    public ReliefValveBlock(Properties props) {
        super(props, new double[][]{{4, 0, 4, 12, 10, 12}, {6.5, 10, 6.5, 9.5, 16, 9.5}});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.35f) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    (random.nextFloat() - 0.5) * 0.02, 0.04 + random.nextFloat() * 0.03, (random.nextFloat() - 0.5) * 0.02);
        }
    }
}
