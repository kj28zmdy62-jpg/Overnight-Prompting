package com.noclip.block;

import com.noclip.level.ExitType;
import com.noclip.travel.Travel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A way out of the current level. Depending on its trigger it fires when you
 * walk into it, fall into it, or use it. The destination comes from the current
 * level's exit table for this block's {@link ExitType}.
 */
public class ExitBlock extends Block {
    public enum Trigger { INSIDE, USE }

    private final ExitType type;
    private final Trigger trigger;
    private final boolean solid;

    public ExitBlock(Properties props, ExitType type, Trigger trigger, boolean solid) {
        super(props);
        this.type = type;
        this.trigger = trigger;
        this.solid = solid;
    }

    public ExitType exitType() {
        return type;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (trigger == Trigger.INSIDE && entity instanceof ServerPlayer player) {
            Travel.exit(player, type, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (trigger != Trigger.USE) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) Travel.exit(sp, type, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return solid ? super.getCollisionShape(state, level, pos, ctx) : Shapes.empty();
    }
}
