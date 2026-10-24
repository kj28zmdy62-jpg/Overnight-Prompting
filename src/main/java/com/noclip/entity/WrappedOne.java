package com.noclip.entity;

import com.noclip.levels.ColdStorage;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cold Storage. It hangs on an empty hook among the wrapped stock and looks
 * exactly like the rest of it. When you pass beneath with your eyes elsewhere,
 * it drops. Its touch takes the last of your warmth.
 */
public class WrappedOne extends BackroomsMonster {
    public static EntityType<WrappedOne> TYPE;
    private boolean hanging;
    private int loose;
    private float swayBase;

    public WrappedOne(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("wrapped_one", WrappedOne::new, MobCategory.MONSTER, 0.6f, 1.7f,
                createAttributes(), 0xD6DFDD, 0x4A3B38);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    /** Hang it from a hook; it stays still until somebody walks under it without looking. */
    public void hang() {
        hanging = true;
        swayBase = random.nextFloat() * 360f;
        setNoGravity(true);
        setNoAi(true);
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean hanging() {
        return hanging;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (hanging) {
            setDeltaMovement(Vec3.ZERO);
            float yaw = swayBase + (float) Math.sin(tickCount * 0.02) * 12f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
            if (tickCount % 5 != 0) return;
            for (Player p : level().players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                if (dx * dx + dz * dz > 4.5 * 4.5 || p.getY() > getY() + 0.5) continue;
                if (Mannequin.seenByAnyone(level(), this, 0.55)) continue;
                drop(p);
                return;
            }
            return;
        }
        loose++;
        if (loose > 900 && loose % 40 == 0 && !Mannequin.seenByAnyone(level(), this, 0.3)) discard();
    }

    private void drop(Player target) {
        hanging = false;
        setNoGravity(false);
        setNoAi(false);
        level().playSound(null, blockPosition(), ModSounds.get("entity.wrapped_one.drop"), SoundSource.HOSTILE, 1.5f, 1f);
        setTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer p) {
            ColdStorage.chill(p, 70f);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hanging && source.getEntity() instanceof Player p && !level().isClientSide) drop(p);
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hanging", hanging);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Hanging")) hang();
    }

    @Override
    public float fear() {
        return hanging ? 0f : 0.4f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return hanging ? null : ModSounds.get("entity.wrapped_one.breath");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.wrapped_one.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.wrapped_one.hurt");
    }
}
