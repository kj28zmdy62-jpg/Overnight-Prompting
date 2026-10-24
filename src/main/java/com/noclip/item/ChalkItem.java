package com.noclip.item;

import com.noclip.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mark the walls so you know where you have been. Sneak to change the mark:
 * arrow, cross, tally, circle. (Marks have a habit of moving.)
 */
public class ChalkItem extends Item {
    public static final String[] MARKS = {"chalk_arrow", "chalk_cross", "chalk_tally", "chalk_circle"};

    public ChalkItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face.getAxis().isVertical()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(face);
        if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean sneaking = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();
            int idx = sneaking ? 1 + (int) (level.getGameTime() % (MARKS.length - 1)) : 0;
            BlockState mark = ModBlocks.state(MARKS[idx]).setValue(HorizontalDirectionalBlock.FACING, face);
            level.setBlockAndUpdate(pos, mark);
            level.playSound(null, pos, SoundEvents.CALCITE_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
            if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(),
                    ctx.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
