package com.noclip.block;

import com.noclip.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Level 188's front doors. They never open. Sometimes someone on the other side holds the handle. */
public class CourtyardDoorBlock extends Block {
    private static final String[] LINES = {
            "The handle will not turn.", "Locked.", "The handle turns a little, then is held from the other side.",
            "Through the door, very close, somebody breathes in.", "Locked. You hear a chain being slid into place.",
            "Locked. A light goes out behind the little window."};

    public CourtyardDoorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.playSound(null, pos, ModSounds.get("event.level_188_locked"), SoundSource.BLOCKS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
            player.displayClientMessage(Component.literal(LINES[level.random.nextInt(LINES.length)])
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
