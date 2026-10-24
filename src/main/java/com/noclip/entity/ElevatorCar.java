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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Elevator Shaft. The car: a steel cab that drops or climbs its hoistway at
 * fourteen metres a second and passes through anything in its way. Anyone in the
 * lane when it passes does not keep climbing.
 */
public class ElevatorCar extends Mob implements Sanity.Frightening {
    public static EntityType<ElevatorCar> TYPE;
    private double vy = -0.8;
    private int life;

    public ElevatorCar(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static void register() {
        TYPE = ModEntities.register("elevator_car", ElevatorCar::new, MobCategory.MISC, 3.75f, 3.0f,
                createAttributes(), 0x5A6066, 0xFFF0C0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void launch(double speed) {
        vy = speed;
    }

    @Override
    public void tick() {
        noPhysics = true;
        setDeltaMovement(0, vy, 0);
        super.tick();
        if (level().isClientSide) return;
        if (++life > 260) {
            discard();
            return;
        }
        if (life % 9 == 0) {
            level().playSound(null, getX(), getY() + 1.5, getZ(), ModSounds.get("entity.elevator_car.rush"), SoundSource.HOSTILE, 2.4f,
                    0.85f + random.nextFloat() * 0.1f);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.15))) {
            if (p.isCreative() || p.isSpectator() || p.invulnerableTime > 0) continue;
            p.hurt(level().damageSources().flyIntoWall(), 26f);
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
            p.push(away.normalize().x * 0.8, vy * 0.6, away.normalize().z * 0.8);
            p.hurtMarked = true;
            if (p instanceof ServerPlayer sp) Sanity.add(sp, -15f);
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
    protected void doPush(net.minecraft.world.entity.Entity e) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public float fear() {
        return 0.6f;
    }
}
