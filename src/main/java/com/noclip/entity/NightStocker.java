package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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
 * Night Supermarket. Comes when the intercom announces a clean-up, to the aisle
 * it named. Red vest, blank name tag, a price sticker where its face should be.
 * It finishes its shift after a minute.
 */
public class NightStocker extends BackroomsMonster {
    public static EntityType<NightStocker> TYPE;

    public NightStocker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    public static void register() {
        TYPE = ModEntities.register("night_stocker", NightStocker::new, MobCategory.MONSTER, 0.6f, 2.0f,
                createAttributes(), 0xB01E24, 0xF08A1A);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.01f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel)) return;
        if (tickCount > 1400) {
            discard();
            return;
        }
        if (getTarget() == null && tickCount % 10 == 0) {
            Player p = level().getNearestPlayer(this, 9);
            if (p != null && !p.isCreative() && !p.isSpectator() && hasLineOfSight(p)) setTarget(p);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        level().playSound(null, blockPosition(), ModSounds.get("entity.night_stocker.gun"), SoundSource.HOSTILE, 1f, 1f);
        return super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.night_stocker.gun");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.4f : 0.15f;
    }
}
