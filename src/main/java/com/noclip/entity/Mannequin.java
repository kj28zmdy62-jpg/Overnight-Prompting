package com.noclip.entity;

import com.noclip.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Only moves when nobody is looking. Never turn your back on the displays. */
public class Mannequin extends BackroomsMonster {
    private boolean wasSeen = true;
    private float frozenYaw;

    public Mannequin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
    }

    public static boolean seenByAnyone(Level level, net.minecraft.world.entity.Entity e, double cone) {
        for (Player p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(e) > 64 * 64) continue;
            Vec3 eye = p.getEyePosition();
            for (double h : new double[]{0.3, 1.0, 1.7}) {
                Vec3 pt = e.position().add(0, h, 0);
                Vec3 to = pt.subtract(eye).normalize();
                if (p.getViewVector(1f).dot(to) < cone) continue;
                if (level.clip(new ClipContext(eye, pt, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS)
                    return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        boolean seen = seenByAnyone(level(), this, 0.45);
        if (seen) {
            if (!wasSeen) {
                frozenYaw = getYRot();
                level().playSound(null, blockPosition(), ModSounds.get("entity.mannequin.creak"), SoundSource.HOSTILE, 0.6f, 0.8f + random.nextFloat() * 0.4f);
            }
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            setYRot(frozenYaw);
            setYHeadRot(frozenYaw);
            yBodyRot = frozenYaw;
        } else {
            Player p = level().getNearestPlayer(this, 48);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                getNavigation().moveTo(p, 1.0);
                if (distanceToSqr(p) < 2.6) doHurtTarget(p);
            }
        }
        wasSeen = seen;
    }

    @Override
    public float fear() {
        return 0.15f;
    }
}
