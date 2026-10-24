package com.noclip.entity;

import com.noclip.registry.ModEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The striped sun on the Grid's horizon. The level keeps it the same distance away, always. */
public class GrHorizon extends PathfinderMob {
    public static EntityType<GrHorizon> TYPE;

    public GrHorizon(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static void register() {
        TYPE = ModEntities.register("gr_horizon", GrHorizon::new, MobCategory.MISC, 1.0f, 1.0f, createAttributes(),
                0xFFE860, 0xFF2A8A);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 160);
        if (p == null) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(30, 30, 4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 200 * 200;
    }
}
