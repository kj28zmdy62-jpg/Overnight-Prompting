package com.noclip.item;

import com.noclip.registry.ModItems;
import com.noclip.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A cheap plastic flashlight. Right-click toggles it; sneak + right-click swaps
 * in a battery from your inventory. The beam itself is drawn by the client shader.
 */
public class FlashlightItem extends Item {
    public FlashlightItem(Properties props) {
        super(props);
    }

    public static boolean isOn(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModItems.LIGHT_ON));
    }

    public static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof FlashlightItem && isOn(stack) && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getDamageValue() > 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack b = player.getInventory().getItem(i);
                if (b.is(ModItems.BATTERY)) {
                    if (!level.isClientSide) {
                        if (!player.getAbilities().instabuild) b.shrink(1);
                        stack.setDamageValue(0);
                        level.playSound(null, player.blockPosition(), ModSounds.get("item.battery"), SoundSource.PLAYERS, 0.8f, 1f);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            boolean on = !isOn(stack);
            stack.set(ModItems.LIGHT_ON, on);
            level.playSound(null, player.blockPosition(), ModSounds.get("item.flashlight_click"), SoundSource.PLAYERS, 0.6f, on ? 1.1f : 0.9f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !isOn(stack)) return;
        boolean held = selected || p.getOffhandItem() == stack;
        if (!held || p.isCreative()) return;
        if (level.getGameTime() % 20 == 0) {
            if (stack.getDamageValue() < stack.getMaxDamage() - 1) {
                stack.setDamageValue(stack.getDamageValue() + 1);
            } else {
                stack.set(ModItems.LIGHT_ON, false);
                level.playSound(null, p.blockPosition(), ModSounds.get("item.flashlight_click"), SoundSource.PLAYERS, 0.5f, 0.6f);
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.noclip.flashlight.battery", left / 60, left % 60).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.noclip.flashlight.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    public static boolean holding(Player p) {
        return isLit(p.getItemBySlot(EquipmentSlot.MAINHAND)) || isLit(p.getItemBySlot(EquipmentSlot.OFFHAND));
    }
}
