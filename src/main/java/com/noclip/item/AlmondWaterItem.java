package com.noclip.item;

import com.noclip.sanity.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;

/** Faintly sweet, faintly nutty. The only thing in the Backrooms that keeps your head clear. */
public class AlmondWaterItem extends net.minecraft.world.item.Item {
    private final float sanity;

    public AlmondWaterItem(Properties props, float sanity) {
        super(props);
        this.sanity = sanity;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer p) {
            Sanity.add(p, sanity);
            p.getFoodData().eat(2, 0.4f);
            p.removeEffect(MobEffects.CONFUSION);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
        }
        if (user instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
