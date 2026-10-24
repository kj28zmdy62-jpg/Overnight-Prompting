package com.noclip.entity;

import com.noclip.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** "Hello friend =) Are you here for the party? =)" */
public class Partygoer extends BackroomsMonster {
    private static final String[] LINES = {
            "Hello friend =)", "Are you here for the party? =)", "Stay a while =)", "Don't leave yet =)",
            "We have cake =)", "Everyone is here =)", "Why are you running? =)", "Join us =)", "=)", "=)",
            "You look lonely =)", "It's your birthday =)"
    };
    private int speechCooldown;

    public Partygoer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 36);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f, 1f));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || --speechCooldown > 0) return;
        if (getTarget() instanceof ServerPlayer p && distanceToSqr(p) < 14 * 14) {
            p.sendSystemMessage(Component.literal("<").append(getDisplayName()).append("> " + LINES[random.nextInt(LINES.length)])
                    .withStyle(ChatFormatting.YELLOW));
            playSound(ModSounds.get("entity.partygoer.giggle"), 1f, 0.9f + random.nextFloat() * 0.25f);
            speechCooldown = 100 + random.nextInt(120);
        }
    }

    @Override
    public float fear() {
        return 0.3f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.partygoer.giggle");
    }
}
