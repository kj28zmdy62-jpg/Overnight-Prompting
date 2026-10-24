package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
 * Hospital Ward. Bandaged, barefoot, nearly blind. It finds you by your
 * heartbeat: keep calm and it wanders past; let your sanity drop and it hears
 * your pulse through the walls and comes straight for you.
 */
public class WardPatient extends BackroomsMonster {
    public static EntityType<WardPatient> TYPE;
    /** Below this sanity your heart is loud enough to hear from anywhere nearby. */
    public static final float LOUD = 50f;
    private int shuffle;

    public WardPatient(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    public static void register() {
        TYPE = ModEntities.register("ward_patient", WardPatient::new, MobCategory.MONSTER, 0.6f, 1.95f,
                createAttributes(), 0x9CC0D2, 0xD8D4C4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.45, 0.004f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (tickCount % 10 == 0) {
            Player best = null;
            double bd = Double.MAX_VALUE;
            for (Player p : sl.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                double d = p.distanceToSqr(this);
                boolean loud = PlayerData.sanity(p) < LOUD && d < 44 * 44;
                if ((loud || d < 6.5 * 6.5) && d < bd) {
                    bd = d;
                    best = p;
                }
            }
            if (best != getTarget()) {
                if (best == null) getNavigation().stop();
                setTarget(best);
            }
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.001 && ++shuffle > 50) {
            shuffle = 0;
            level().playSound(null, blockPosition(), ModSounds.get("entity.ward_patient.shuffle"), SoundSource.HOSTILE, 0.7f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.ward_patient.breath");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.35f : 0.12f;
    }
}
