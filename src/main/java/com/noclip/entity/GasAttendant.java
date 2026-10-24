package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Attendant of the repeating gas station. At first he only stands by the pumps
 * and watches you come back. After enough laps he stops waiting, and moves whenever
 * nobody is looking at him.
 */
public class GasAttendant extends BackroomsMonster {
    public static EntityType<GasAttendant> TYPE;
    private boolean hunting;
    private boolean wasSeen = true;

    public GasAttendant(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static void register() {
        TYPE = ModEntities.register("gs_attendant", GasAttendant::new, MobCategory.MONSTER, 0.6f, 1.95f, createAttributes(), 0xA42A22, 0xE8E4D8);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    public void setHunting(boolean h) {
        hunting = h;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isNoAi()) return;
        Player p = level().getNearestPlayer(this, 64);
        if (p == null) return;
        boolean seen = Mannequin.seenByAnyone(level(), this, 0.5);
        if (!hunting) {
            getNavigation().stop();
            face(p);
            // walk up to him and he is gone; the door chimes somewhere
            if (distanceToSqr(p) < 4 * 4) {
                level().playSound(null, blockPosition(), ModSounds.get("event.gas_station_chime"), SoundSource.HOSTILE, 1.0f, 0.8f);
                discard();
            }
            return;
        }
        if (seen) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!wasSeen) face(p);
        } else if (!p.isCreative() && !p.isSpectator()) {
            getNavigation().moveTo(p, 1.0);
            if (distanceToSqr(p) < 2.4) doHurtTarget(p);
        }
        wasSeen = seen;
    }

    private void face(Player p) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public float fear() {
        return hunting ? 0.5f : 0.2f;
    }

    @Override
    public void checkDespawn() {
    }
}
