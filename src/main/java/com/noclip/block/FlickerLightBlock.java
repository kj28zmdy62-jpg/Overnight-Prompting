package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A ceiling light that randomly cuts out for a moment and comes back. */
public class FlickerLightBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final float chance;

    public FlickerLightBlock(Properties props, float chance) {
        super(props.randomTicks());
        this.chance = chance;
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextFloat() < chance) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 2 + random.nextInt(random.nextFloat() < 0.15f ? 80 : 8));
        }
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) && random.nextFloat() < 0.35f) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    com.noclip.registry.ModSounds.get("block.light.zap"), net.minecraft.sounds.SoundSource.BLOCKS,
                    0.35f, 0.9f + random.nextFloat() * 0.2f, false);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
    }
}
