package com.noclip.entity;

import com.noclip.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Hounds hunt by sound. Sprint, jump, or fight and they will find you. Crouch,
 * stay still, and they lose the trail.
 */
public class Hound extends BackroomsMonster {
    private int quietTicks;

    public Hound(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.45, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new ListenGoal());
    }

    /** Noise a player is making, 0 = silent. */
    public static float noise(Player p) {
        if (p.isSpectator() || p.isCreative()) return 0;
        float n = 0;
        if (p.isSprinting()) n += 1.0f;
        if (!p.onGround() && p.getDeltaMovement().y > 0.1) n += 0.6f;
        if (p.swinging) n += 0.8f;
        if (p.getDeltaMovement().horizontalDistanceSqr() > 0.001 && !p.isShiftKeyDown()) n += 0.35f;
        if (p.hurtTime > 0) n += 0.5f;
        return n;
    }

    class ListenGoal extends Goal {
        ListenGoal() {
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (tickCount % 10 != 0) return false;
            for (Player p : level().players()) {
                float n = noise(p);
                double range = 8 + n * 22;
                if (n > 0.3f && p.distanceToSqr(Hound.this) < range * range) {
                    setTarget(p);
                    quietTicks = 0;
                    if (p instanceof ServerPlayer) playSound(ModSounds.get("entity.hound.howl"), 1.4f, 0.9f + random.nextFloat() * 0.2f);
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && getTarget() instanceof Player p) {
            boolean quiet = noise(p) < 0.3f && distanceToSqr(p) > 36;
            quietTicks = quiet ? quietTicks + 1 : 0;
            if (quietTicks > 80) {
                setTarget(null);
                quietTicks = 0;
            }
        }
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.45f : 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.hound.ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.hound.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.hound.hurt");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }
}
