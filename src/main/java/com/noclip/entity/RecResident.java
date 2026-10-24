package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
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

/**
 * The Resident of the Recursion sitting room. He does not move. He is simply
 * standing somewhere new each time you open a door, and he is always looking at you.
 */
public class RecResident extends PathfinderMob implements Sanity.Frightening {
    public static EntityType<RecResident> TYPE;

    public RecResident(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("rec_resident", RecResident::new, MobCategory.MONSTER, 0.6f, 1.95f,
                createAttributes(), 0x24262A, 0xC8BEAC);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0)
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
        Player p = level().getNearestPlayer(this, 40);
        if (p == null || tickCount > 20 * 600) {
            discard();
            return;
        }
        lookAtPlayer(p);
        getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ());
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) {
            level().playSound(null, blockPosition(), ModSounds.get("entity.rec_resident.breath"), SoundSource.HOSTILE, 0.8f, 0.7f);
            discard();
        }
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
        return 0.3f;
    }
}
