package com.noclip.entity;

import com.noclip.levels.School;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * School After Hours. While class is in session it walks the halls looking for
 * anyone out of class. It does not go into classrooms, and it cannot open doors.
 * When the second bell rings it is simply not there any more.
 */
public class HallMonitor extends BackroomsMonster {
    public static EntityType<HallMonitor> TYPE;
    private int stepTimer;

    public HallMonitor(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    public static void register() {
        TYPE = ModEntities.register("hall_monitor", HallMonitor::new, MobCategory.MONSTER, 0.6f, 2.3f,
                createAttributes(), 0x5A4630, 0xE86A1A);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55, 0.01f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (!School.inSession() && tickCount > 40) {
            discard();
            return;
        }
        if (tickCount % 10 == 0) {
            Player best = null;
            double bd = 40 * 40;
            for (Player p : sl.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                if (!School.inHallway(p.getBlockX(), p.getBlockZ())) continue;
                double d = p.distanceToSqr(this);
                if (d < bd) {
                    bd = d;
                    best = p;
                }
            }
            if (best != getTarget()) {
                if (best != null)
                    level().playSound(null, blockPosition(), ModSounds.get("entity.hall_monitor.whistle"), SoundSource.HOSTILE, 1.6f, 1f);
                else getNavigation().stop();
                setTarget(best);
            }
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.002 && ++stepTimer > 46) {
            stepTimer = 0;
            level().playSound(null, blockPosition(), ModSounds.get("entity.hall_monitor.step"), SoundSource.HOSTILE, 0.9f, 0.95f);
        }
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.45f : 0.15f;
    }
}
