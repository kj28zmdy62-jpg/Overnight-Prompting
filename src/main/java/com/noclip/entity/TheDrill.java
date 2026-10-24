package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * The Bunker. A figure in a greatcoat and gas mask that only exists while the
 * alarm is sounding. It walks the corridors at a steady march and does not stop.
 * When the alarm ends it is simply not there any more.
 */
public class TheDrill extends BackroomsMonster {
    public static EntityType<TheDrill> TYPE;

    public TheDrill(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("the_drill", TheDrill::new, MobCategory.MONSTER, 0.6f, 2.0f, createAttributes(),
                0x3E4A32, 0x1A1C18);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 1400) discard();
    }

    @Override
    public float fear() {
        return 0.5f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.the_drill.breath");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.the_drill.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.the_drill.hurt");
    }
}
