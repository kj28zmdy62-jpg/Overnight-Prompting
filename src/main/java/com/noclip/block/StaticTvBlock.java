package com.noclip.block;

import com.noclip.level.ExitType;
import com.noclip.registry.ModSounds;
import com.noclip.travel.Travel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The television in Static Fields. Mode 0 plays static, 1 shows the man, 2 shows a
 * doorway (use it to go through the screen), 3 is switched off.
 */
public class StaticTvBlock extends Block {
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 3);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 3, 15, 5, 13), Block.box(1, 5, 2, 15, 15, 14.5));

    public StaticTvBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(MODE) == 3 ? 0 : (s.getValue(MODE) == 2 ? 10 : 7)));
        registerDefaultState(stateDefinition.any().setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int mode = state.getValue(MODE);
        if (mode == 2 && player instanceof ServerPlayer sp) {
            level.playSound(null, pos, ModSounds.get("event.static_fields_burst"), SoundSource.BLOCKS, 1.2f, 0.6f);
            Travel.exit(sp, ExitType.SECRET, pos);
        } else {
            // the knob turns, the channel does not change
            level.playSound(null, pos, ModSounds.get("event.static_fields_tv"), SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);
        }
        return InteractionResult.CONSUME;
    }
}
