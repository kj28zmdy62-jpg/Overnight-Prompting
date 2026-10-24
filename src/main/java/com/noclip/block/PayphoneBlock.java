package com.noclip.block;

import com.mojang.serialization.MapCodec;
import com.noclip.level.ExitType;
import com.noclip.registry.ModSounds;
import com.noclip.travel.Travel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A payphone on a wall. Usually it gives a dial tone. When it rings, picking it up
 * puts you on the line with yourself, and then somewhere else.
 */
public class PayphoneBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public PayphoneBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RINGING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RINGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (state.getValue(RINGING) && player instanceof ServerPlayer sp) {
            level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.get("event.gas_station_breath"), SoundSource.BLOCKS, 1.0f, 1.0f);
            Travel.exit(sp, ExitType.SECRET, pos);
        } else {
            level.playSound(null, pos, ModSounds.get("event.gas_station_dial"), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.CONSUME;
    }
}
