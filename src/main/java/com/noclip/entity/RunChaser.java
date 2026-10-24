package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Level !'s pursuer. It is behind you from the moment you arrive and it does not
 * stop, slow down, or go around things: it goes through them. It is a little
 * slower than you can sprint and a lot faster than you can walk.
 */
public class RunChaser extends BackroomsMonster {
    public static EntityType<RunChaser> TYPE;
    private UUID prey;
    private int age;
    private int stepTimer;

    public RunChaser(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static void register() {
        TYPE = ModEntities.register("run_chaser", RunChaser::new, MobCategory.MONSTER, 0.8f, 2.6f, createAttributes(), 0x0C0606, 0xFF2A1A);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 1000)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 128);
    }

    public void hunt(Player p) {
        this.prey = p.getUUID();
    }

    public UUID prey() {
        return prey;
    }

    @Override
    protected void registerGoals() {
    }

    /** Blocks per tick. Sprinting is about 0.28; walking about 0.21. */
    private double speed(double dist) {
        double base = 0.245 + Math.min(0.03, age / 20.0 / 600.0 * 0.03);
        if (dist > 34) return 0.55;      // it never falls far behind
        return base;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide) return;
        age++;
        Player p = prey != null ? level().getPlayerByUUID(prey) : level().getNearestPlayer(this, 128);
        if (p == null || p.level() != level()) {
            if (age > 100) discard();
            return;
        }
        if (prey == null) prey = p.getUUID();
        Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        double dist = to.length();
        // it holds back for a moment when you arrive: long enough to understand, not long enough to think
        boolean waiting = age < 50 || p.isCreative() || p.isSpectator();
        if (!waiting && dist > 0.6) {
            Vec3 step = to.normalize().scale(Math.min(dist, speed(dist)));
            double ny = getY() + (p.getY() - getY()) * 0.3;
            setPos(getX() + step.x, ny, getZ() + step.z);
            setDeltaMovement(step.x, 0, step.z);
            if (--stepTimer <= 0) {
                playSound(ModSounds.get("entity.run_chaser.step"), 1.4f, 0.8f + random.nextFloat() * 0.2f);
                stepTimer = 5;
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        setAggressive(true);
        if (p instanceof ServerPlayer sp && !waiting) {
            if (dist < 12 && tickCount % 15 == 0) {
                ServerPlayNetworking.send(sp, new com.noclip.net.NoclipNet.EventPayload("pulse", (float) (1.0 - dist / 12.0)));
            }
            if (dist < 9 && tickCount % 40 == 0) {
                level().playSound(null, blockPosition(), ModSounds.get("entity.run_chaser.roar"), SoundSource.HOSTILE, 1.6f, 0.9f + random.nextFloat() * 0.2f);
            }
            if (dist < 1.3 && Math.abs(p.getY() - getY()) < 2.5) {
                level().playSound(null, blockPosition(), ModSounds.get("entity.run_chaser.kill"), SoundSource.HOSTILE, 2f, 1f);
                sp.hurt(damageSources().mobAttack(this), 1000f);
            }
        }
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
    public float fear() {
        return 0.6f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (prey != null) tag.putUUID("Prey", prey);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Prey")) prey = tag.getUUID("Prey");
    }
}
