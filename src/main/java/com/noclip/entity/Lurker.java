package com.noclip.entity;

import com.noclip.item.CamcorderItem;
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
import net.minecraft.world.phys.Vec3;

/**
 * You cannot see it. It stands very close, usually just behind you, and your
 * sanity bleeds while it does. Through a camcorder's viewfinder it is plain as
 * day. Hold it in frame long enough and it leaves.
 */
public class Lurker extends PathfinderMob implements Sanity.Frightening {
    private int filmed;

    public Lurker(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
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
        Player p = level().getNearestPlayer(this, 48);
        if (p == null || tickCount > 2400) {
            discard();
            return;
        }
        // drift to a spot just behind the player's shoulder
        double yaw = Math.toRadians(p.getYRot());
        Vec3 behind = p.position().add(Math.sin(yaw) * 2.2, 0, -Math.cos(yaw) * 2.2);
        Vec3 to = behind.subtract(position());
        if (to.length() > 0.2) setPos(position().add(to.scale(0.08)));
        float face = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(face);
        setYHeadRot(face);
        yBodyRot = face;
        boolean seen = CamcorderItem.viewing(p) && Mannequin.seenByAnyone(level(), this, 0.9);
        if (seen) {
            if (filmed == 0) level().playSound(null, blockPosition(), ModSounds.get("event.sting"), SoundSource.HOSTILE, 0.8f, 1.3f);
            if (++filmed > 50) {
                level().playSound(null, blockPosition(), ModSounds.get("entity.smiler.vanish"), SoundSource.HOSTILE, 0.8f, 0.7f);
                discard();
            }
        } else {
            filmed = Math.max(0, filmed - 1);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.get("halluc.whisper");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 280;
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
    public float fear() {
        return 0.25f;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return true;
    }
}
