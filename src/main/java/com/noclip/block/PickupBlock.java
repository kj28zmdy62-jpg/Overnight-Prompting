package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Something left behind on the floor. Use it to take it. */
public class PickupBlock extends ShapedBlock {
    private final ResourceLocation itemId;
    private Item item;

    public PickupBlock(Properties props, List<double[]> boxes, String itemId) {
        super(props, boxes, false);
        this.itemId = ResourceLocation.parse(itemId);
    }

    private Item item() {
        if (item == null) item = BuiltInRegistries.ITEM.get(itemId);
        return item;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack stack = new ItemStack(item());
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(item());
    }

}
