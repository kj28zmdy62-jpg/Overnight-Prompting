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
    private final String spec;
    private List<ResourceLocation> ids;
    private int[] weights;

    /**
     * @param spec one item id, or a weighted list "noclip:almond_water*3,noclip:battery*2" from which
     *             1-3 items are drawn when the block is taken
     */
    public PickupBlock(Properties props, List<double[]> boxes, String spec) {
        super(props, boxes, false);
        this.spec = spec;
    }

    private void parse() {
        if (ids != null) return;
        String[] parts = spec.split(",");
        ids = new java.util.ArrayList<>();
        weights = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].trim().split("\\*");
            ids.add(ResourceLocation.parse(kv[0]));
            weights[i] = kv.length > 1 ? Integer.parseInt(kv[1]) : 1;
        }
    }

    private Item item() {
        parse();
        return BuiltInRegistries.ITEM.get(ids.get(0));
    }

    private Item roll(net.minecraft.util.RandomSource r) {
        parse();
        int total = 0;
        for (int w : weights) total += w;
        int x = r.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x < 0) return BuiltInRegistries.ITEM.get(ids.get(i));
        }
        return item();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            parse();
            int count = ids.size() == 1 ? 1 : 1 + level.random.nextInt(3);
            for (int i = 0; i < count; i++) {
                ItemStack stack = new ItemStack(roll(level.random));
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
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
