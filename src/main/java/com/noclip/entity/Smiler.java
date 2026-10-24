package com.noclip.entity;

import com.noclip.item.FlashlightItem;
import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Two eyes and a grin, floating in the dark. It edges closer while you are not
 * looking. Shine a light on it for too long and it stops being patient.
 */
public class Smiler extends BackroomsMonster {
    private int anger;
    private int unseenClose;
    private boolean charging;

    public Smiler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().scale(0.6));
        if (level().isClientSide) return;
        BlockPos pos = blockPosition();
        if (!com.noclip.world.Blackouts.active(level()) && level().getBrightness(LightLayer.BLOCK, pos) >= 9 && tickCount > 20) {
            vanish();
            return;
        }
        Player p = level().getNearestPlayer(this, 40);
        if (p == null || p.isCreative() || p.isSpectator()) return;
        getLookControl().setLookAt(p);
        setYRot((float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ())));
        boolean watched = watched(p);
        boolean lit = FlashlightItem.holding(p);
        Vec3 to = p.getEyePosition().subtract(position().add(0, 0.6, 0));
        double dist = to.length();

        if (charging) {
            setDeltaMovement(to.normalize().scale(0.55));
            if (dist < 1.2) {
                p.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                vanish();
            }
            return;
        }
        if (watched && lit) {
            if (++anger > 50) {
                charging = true;
                level().playSound(null, blockPosition(), ModSounds.get("entity.smiler.scream"), SoundSource.HOSTILE, 1.6f, 1f);
            }
        } else {
            anger = Math.max(0, anger - 1);
        }
        if (!watched && dist > 3.5) {
            setDeltaMovement(to.normalize().scale(0.07));
        }
        if (!watched && dist < 5) {
            if (++unseenClose > 120) charging = true;
        } else {
            unseenClose = 0;
        }
    }

    private boolean watched(Player p) {
        Vec3 to = position().add(0, 0.6, 0).subtract(p.getEyePosition()).normalize();
        if (p.getViewVector(1f).dot(to) < 0.93) return false;
        return level().clip(new ClipContext(p.getEyePosition(), position().add(0, 0.6, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    private void vanish() {
        level().playSound(null, blockPosition(), ModSounds.get("entity.smiler.vanish"), SoundSource.HOSTILE, 0.8f, 1f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player) {
            vanish();
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public float fear() {
        return charging ? 1.5f : 0.6f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }
}
