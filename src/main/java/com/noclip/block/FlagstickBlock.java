package com.noclip.block;

import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The pin standing in a cup. Pull it out and the cup is open. */
public class FlagstickBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(7, 0, 7, 9, 16, 9);

    public FlagstickBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).is(this)) bottom = bottom.below();
        BlockPos top = pos;
        while (level.getBlockState(top.above()).is(this)) top = top.above();
        for (BlockPos p = bottom.above(); p.getY() <= top.getY() + 1; p = p.above()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(bottom, ModBlocks.state("gc_cup"), Block.UPDATE_ALL);
        var clink = ModSounds.getOrNull("ambient.golf_course.additions");
        if (clink != null) level.playSound(null, bottom, clink, SoundSource.BLOCKS, 0.6f, 1.4f);
        return InteractionResult.CONSUME;
    }
}
