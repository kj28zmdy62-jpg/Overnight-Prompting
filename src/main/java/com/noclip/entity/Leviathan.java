package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Level 7: something very large under the black water. It circles below anyone
 * who swims, slowly tightening the circle the longer they stay in. Stay in too
 * long and it comes up. It cannot be hurt. It can only be avoided.
 */
public class Leviathan extends PathfinderMob implements com.noclip.sanity.Sanity.Frightening {
    public static EntityType<Leviathan> TYPE;
    /** Water surface of Level 7. */
    public static int sea = 40;

    private double angle;
    private int wet, cooldown, idle, nextMoan = 200;
    private boolean charging;

    public Leviathan(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        angle = level.random.nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static void register() {
        TYPE = ModEntities.register("leviathan", Leviathan::new, MobCategory.MONSTER, 5f, 4f, createAttributes(), 0x06080A, 0xD8E0C8);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        setAirSupply(getMaxAirSupply());
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 140);
        if (p == null || p.isSpectator()) {
            setPos(getX(), getY() - 0.15, getZ());
            if (++idle > 300 || getY() < 0) discard();
            return;
        }
        idle = 0;
        boolean swimming = p.isInWater() && !p.isCreative();
        wet = swimming ? wet + 1 : Math.max(0, wet - 3);
        if (cooldown > 0) cooldown--;
        if (!swimming && distanceToSqr(p) > 90 * 90) {
            if (++idle > 600) discard();
        }

        if (!charging && swimming && wet > 500 && cooldown == 0 && p.getY() < sea - 1) {
            charging = true;
            level().playSound(null, blockPosition(), ModSounds.get("entity.leviathan.surge"), SoundSource.HOSTILE, 4f, 0.8f);
        }
        if (charging && !swimming) {
            charging = false;
            cooldown = 200;
        }

        Vec3 target;
        double speed;
        if (charging) {
            target = p.position().add(0, 0.5, 0);
            speed = 0.95;
        } else {
            angle += swimming ? 0.012 + wet * 0.00002 : 0.006;
            double r = Math.max(9, 36 - wet * 0.05);
            double depth = swimming ? Math.max(8, 22 - wet * 0.025) : 30;
            double ty = Math.max(3, Math.min(p.getY() - depth, sea - 12));
            target = new Vec3(p.getX() + Math.cos(angle) * r, ty, p.getZ() + Math.sin(angle) * r);
            speed = 0.45;
        }
        Vec3 to = target.subtract(position());
        double d = to.length();
        if (d > 0.01) {
            Vec3 step = to.scale(Math.min(speed, d) / d);
            setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
            float yaw = (float) Math.toDegrees(Math.atan2(-step.x, step.z));
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            setXRot((float) Math.toDegrees(-Math.atan2(step.y, Math.sqrt(step.x * step.x + step.z * step.z))) * 0.5f);
        }

        if (charging && getBoundingBox().inflate(1.5).intersects(p.getBoundingBox())) {
            p.hurt(damageSources().mobAttack(this), 10f);
            Vec3 push = p.position().subtract(position()).normalize().scale(1.6);
            p.setDeltaMovement(push.x, 1.2, push.z);
            p.hurtMarked = true;
            if (p instanceof ServerPlayer sp) Sanity.add(sp, -20f);
            charging = false;
            cooldown = 700;
            wet = 150;
        }

        if (--nextMoan <= 0) {
            nextMoan = 260 + random.nextInt(300);
            level().playSound(null, blockPosition(), ModSounds.get("entity.leviathan.moan"), SoundSource.HOSTILE, 8f,
                    0.7f + random.nextFloat() * 0.3f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return distanceSq > 160 * 160;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    public float fear() {
        return charging ? 2.0f : 0.8f;
    }
}
