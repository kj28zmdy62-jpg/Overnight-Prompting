package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.sanity.Sanity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Your reflection on the far side of the glass in the Mirror Level. It wears
 * your skin and copies you, a little late. The level moves it; it has no mind
 * of its own. Mostly.
 */
public class MrReflection extends PathfinderMob implements Sanity.Frightening {
    public static EntityType<MrReflection> TYPE;
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(MrReflection.class, EntityDataSerializers.STRING);

    public MrReflection(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static void register() {
        TYPE = ModEntities.register("mr_reflection", MrReflection::new, MobCategory.MISC, 0.6f, 1.8f, createAttributes(),
                0xA8AEB4, 0x2A2A30);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, "");
    }

    public String owner() {
        return entityData.get(OWNER);
    }

    public void setOwner(String uuid) {
        entityData.set(OWNER, uuid);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (owner().isEmpty() || tickCount > 20 * 60 * 30)) discard();
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
    protected void doPush(net.minecraft.world.entity.Entity e) {
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public float fear() {
        return 0.05f;
    }
}
