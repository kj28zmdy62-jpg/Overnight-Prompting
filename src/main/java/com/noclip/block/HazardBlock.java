package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hurts whatever touches it: live wires, scalding pipes, rusted spikes. */
public class HazardBlock extends Block {
    private final float damage;
    private final boolean solid;

    public HazardBlock(Properties props, float damage, boolean solid) {
        super(props);
        this.damage = damage;
        this.solid = solid;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && level.getGameTime() % 10 == 0) {
            living.hurt(level.damageSources().hotFloor(), damage);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entityInside(state, level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return solid ? super.getCollisionShape(state, level, pos, ctx) : Shapes.empty();
    }
}
