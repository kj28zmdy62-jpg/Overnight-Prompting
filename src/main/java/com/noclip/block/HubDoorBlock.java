package com.noclip.block;

import com.noclip.level.Levels;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.travel.Travel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A door in The Hub. It goes wherever the brass plaque above it says. The plaque
 * is a block of its own ({@code hub_plaque_<level id>}), so the hall can quietly
 * change where its doors lead by swapping plaques while nobody is looking.
 */
public class HubDoorBlock extends Block {
    public static final String PLAQUE = "hub_plaque_";

    public HubDoorBlock(Properties props) {
        super(props);
    }

    /** The level a plaque block points at, or null if the block is not a plaque. */
    public static String target(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.startsWith(PLAQUE) ? path.substring(PLAQUE.length()) : null;
    }

    public static List<String> allPlaques() {
        List<String> out = new ArrayList<>();
        for (String name : ModBlocks.BLOCKS.keySet()) if (name.startsWith(PLAQUE)) out.add(name.substring(PLAQUE.length()));
        return out;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (Travel.onCooldown(sp)) return InteractionResult.SUCCESS;
        String dest = null;
        for (int dy = 1; dy <= 3 && dest == null; dy++) dest = target(level.getBlockState(pos.above(dy)));
        if (dest == null) dest = "unmarked";
        if (dest.equals("unmarked")) {
            List<String> open = new ArrayList<>();
            for (String id : allPlaques()) if (Levels.get(id) != null && !id.equals("the_hub")) open.add(id);
            if (open.isEmpty()) dest = null;
            else dest = open.get(level.random.nextInt(open.size()));
        }
        if (dest == null || Levels.get(dest) == null) {
            level.playSound(null, pos, ModSounds.get("event.the_hub_locked"), SoundSource.BLOCKS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
            sp.displayClientMessage(Component.literal(level.random.nextFloat() < 0.15f
                    ? "Locked. Something on the other side knocks back."
                    : "Locked. Whatever is behind this door has not been built yet.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, pos, ModSounds.get("event.the_hub_open"), SoundSource.BLOCKS, 1f, 1f);
        Travel.sendTo(sp, dest);
        return InteractionResult.SUCCESS;
    }
}
