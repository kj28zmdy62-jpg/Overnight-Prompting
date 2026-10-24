package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The Guest of the Red Room. It only moves while nobody is looking at it, a
 * short step at a time, toward whoever is in the room. When the door starts to
 * open it hurries.
 */
public class RedGuest extends PathfinderMob implements Sanity.Frightening {
    public static EntityType<RedGuest> TYPE;
    public UUID owner;
    private int stepTimer;

    public RedGuest(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("red_guest", RedGuest::new, MobCategory.MONSTER, 0.6f, 2.3f,
                createAttributes(), 0x060404, 0x6E0A0C);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    public void lookAtPlayer(Player p) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        yRotO = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 20);
        if (p == null || tickCount > 20 * 900) {
            discard();
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (Mannequin.seenByAnyone(level(), this, 0.5) || p.isCreative() || p.isSpectator()) {
            lookAtPlayer(p);
            return;
        }
        boolean hurry = com.noclip.levels.RedRoom.doorOpening(p.getUUID());
        if (++stepTimer < (hurry ? 9 : 18)) return;
        stepTimer = 0;
        Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        double dist = to.length();
        if (dist < 1.5) {
            if (p instanceof ServerPlayer sp) com.noclip.levels.RedRoom.caught(sp, this);
            return;
        }
        Vec3 step = to.normalize().scale(0.7);
        double ox = getX(), oz = getZ();
        moveTo(ox + step.x, getY(), oz + step.z, getYRot(), 0f);
        if (!level().noCollision(this)) {
            moveTo(ox + step.x, getY(), oz, getYRot(), 0f);
            if (!level().noCollision(this)) {
                moveTo(ox, getY(), oz + step.z, getYRot(), 0f);
                if (!level().noCollision(this)) moveTo(ox, getY(), oz, getYRot(), 0f);
            }
        }
        lookAtPlayer(p);
        if (dist < 4 && random.nextInt(3) == 0) {
            level().playSound(null, blockPosition(), ModSounds.get("entity.rec_resident.breath"), SoundSource.HOSTILE, 0.9f, 0.6f);
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
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public float fear() {
        return 0.6f;
    }
}
