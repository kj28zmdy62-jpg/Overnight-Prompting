package com.noclip.item;

import com.noclip.net.NoclipNet;
import com.noclip.sanity.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The wanderer's journal: every level you have survived, in your own handwriting. */
public class JournalItem extends Item {
    /** Set by the client entrypoint; opens the journal screen. */
    public static Runnable openScreen = () -> {};

    public JournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ServerPlayNetworking.send(sp, new NoclipNet.JournalPayload(String.join(",", PlayerData.visited(sp))));
        } else {
            openScreen.run();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
