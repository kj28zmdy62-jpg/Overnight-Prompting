package com.noclip.entity;

import com.noclip.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * The thing in The Void. Pale, faceless and enormously tall, standing a very long
 * way off. It is positioned every tick by the level's behaviour; on its own it
 * does nothing at all, which is somehow worse.
 */
public class VoidFarThing extends BackroomsMonster {
    public static EntityType<VoidFarThing> TYPE;
    public UUID watcher;
    public double distance = 72;
    public float bearing;

    public VoidFarThing(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static void register() {
        TYPE = ModEntities.register("void_far_thing", VoidFarThing::new, MobCategory.MONSTER, 2.0f, 11.0f, createAttributes(), 0x000000, 0xE8E4DC);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (!level().isClientSide && tickCount > 40 && (watcher == null || level().getPlayerByUUID(watcher) == null)) discard();
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
    public boolean removeWhenFarAway(double distanceSq) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 260 * 260;
    }

    @Override
    public float fear() {
        return 0f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
