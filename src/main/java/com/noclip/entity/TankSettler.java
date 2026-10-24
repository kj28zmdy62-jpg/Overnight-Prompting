package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
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

/**
 * Water Treatment. A long dark shape that circles just under the surface of a
 * settling tank. From the catwalk you only see it pass. In the water with it,
 * it comes straight for you and drags you down.
 */
public class TankSettler extends BackroomsMonster {
    public static EntityType<TankSettler> TYPE;
    private double cx, cz, surface, radius = 8, angle;
    private boolean anchored;
    private int cooldown, wake;

    public TankSettler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static void register() {
        TYPE = ModEntities.register("tank_settler", TankSettler::new, MobCategory.MONSTER, 1.6f, 0.6f,
                createAttributes(), 0x0A0E0A, 0xC8D0B0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Circle the tank centred here, this far out, this far below the water surface. */
    public void anchor(double cx, double cz, double surface, double radius) {
        this.cx = cx;
        this.cz = cz;
        this.surface = surface;
        this.radius = radius;
        this.angle = random.nextDouble() * Math.PI * 2;
        this.anchored = true;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (level().isClientSide) return;
        if (!anchored) {
            anchor(getX(), getZ(), getY() + 2.5, 8);
        }
        if (cooldown > 0) cooldown--;
        Player prey = null;
        for (Player p : level().players()) {
            if (p.isCreative() || p.isSpectator() || !p.isInWater()) continue;
            if (p.distanceToSqr(cx, p.getY(), cz) < (radius + 6) * (radius + 6)) {
                prey = p;
                break;
            }
        }
        Vec3 next;
        if (prey != null) {
            Vec3 to = prey.position().add(0, 0.2, 0).subtract(position());
            next = position().add(to.normalize().scale(Math.min(0.16, to.length())));
            if (to.lengthSqr() < 3.2 && cooldown == 0) {
                cooldown = 30;
                prey.hurt(damageSources().mobAttack(this), 4f);
                prey.push(0, -0.45, 0);
                prey.hurtMarked = true;
                if (prey instanceof ServerPlayer sp) Sanity.add(sp, -6f);
            }
        } else {
            angle += 0.012;
            next = new Vec3(cx + Math.cos(angle) * radius, surface - 2.5, cz + Math.sin(angle) * radius);
            next = position().add(next.subtract(position()).scale(0.08));
        }
        Vec3 d = next.subtract(position());
        if (d.horizontalDistanceSqr() > 1e-5) {
            float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        setPos(next.x, next.y, next.z);
        if (++wake > 140 + random.nextInt(60)) {
            wake = 0;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.get("entity.tank_settler.wake"), SoundSource.HOSTILE, 1.2f, 0.8f);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount * 0.5f);
    }

    @Override
    public float fear() {
        return 0.35f;
    }
}
