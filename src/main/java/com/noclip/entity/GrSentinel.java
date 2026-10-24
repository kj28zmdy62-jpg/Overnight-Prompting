package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A red wireframe figure that walks the Grid. It was drawn to find things that do not belong. */
public class GrSentinel extends BackroomsMonster {
    public static EntityType<GrSentinel> TYPE;

    public GrSentinel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("gr_sentinel", GrSentinel::new, MobCategory.MONSTER, 0.6f, 2.0f, createAttributes(),
                0x120206, 0xFF2A4A);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.gr_sentinel.derez");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.gr_sentinel.derez");
    }

    @Override
    public float fear() {
        return 0.25f;
    }
}
