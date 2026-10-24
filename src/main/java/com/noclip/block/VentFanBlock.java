package com.noclip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Air Vents booster fan. While spinning (LIT) it strobes light through its blades
 * and cuts whatever crawls into it; when it stops it goes dark and can be crawled
 * through. The level behaviour decides when each fan spins.
 */
public class VentFanBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public VentFanBlock(Properties props) {
        super(props.lightLevel(s -> s.getValue(LIT) ? 12 : 2)
                .emissiveRendering((s, l, p) -> s.getValue(LIT))
                .noCollission());
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(LIT) || !(entity instanceof LivingEntity living)) return;
        if (level.getGameTime() % 8 == 0) {
            living.hurt(level.damageSources().generic(), 3f);
            Vec3 away = entity.position().subtract(Vec3.atCenterOf(pos)).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
            entity.push(away.normalize().x * 0.4, 0, away.normalize().z * 0.4);
            entity.hurtMarked = true;
        }
    }
}
