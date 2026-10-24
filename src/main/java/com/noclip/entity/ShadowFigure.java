package com.noclip.entity;

import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Someone standing at the end of the hall, watching. Walk toward them, or stare
 * too long, and there was never anyone there.
 */
public class ShadowFigure extends PathfinderMob implements Sanity.Frightening {
    private int stared;

    public ShadowFigure(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
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
        Player p = level().getNearestPlayer(this, 64);
        if (p == null || tickCount > 1600) {
            discard();
            return;
        }
        getLookControl().setLookAt(p);
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        if (distanceToSqr(p) < 13 * 13) {
            vanish();
            return;
        }
        stared = Mannequin.seenByAnyone(level(), this, 0.985) ? stared + 1 : Math.max(0, stared - 1);
        if (stared > 45) vanish();
    }

    private void vanish() {
        level().playSound(null, blockPosition(), ModSounds.get("halluc.whisper"), SoundSource.HOSTILE, 0.5f, 0.8f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) vanish();
        return false;
    }

    @Override
    public float fear() {
        return 0.5f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
