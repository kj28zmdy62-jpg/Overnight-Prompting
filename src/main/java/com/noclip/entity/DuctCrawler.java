package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Air Vents. A long pale thing that lives in the ducts and moves through them on
 * knees and elbows, knocking on the sheet metal as it comes. It hates light: it
 * will not follow you into a fan's glow, and after it bites it scuttles back into
 * the dark for a while.
 */
public class DuctCrawler extends BackroomsMonster {
    public static EntityType<DuctCrawler> TYPE;
    private int retreat;
    private int knock;

    public DuctCrawler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("duct_crawler", DuctCrawler::new, MobCategory.MONSTER, 0.7f, 0.6f,
                createAttributes(), 0xC8C4B6, 0x2A1210);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 36)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    /** Light in which it will not hunt. */
    public static boolean lit(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) >= 9;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, false) {
            @Override
            public boolean canUse() {
                return retreat <= 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return retreat <= 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> !lit(e.level(), e.blockPosition())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (retreat > 0) retreat--;
        LivingEntity t = getTarget();
        if (t != null && (lit(level(), t.blockPosition()) || lit(level(), blockPosition()))) {
            // the light hurts it: back off into the dark and wait
            setTarget(null);
            flee(t.position(), 80);
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.002 && --knock <= 0) {
            knock = 18 + random.nextInt(30);
            level().playSound(null, blockPosition(), ModSounds.get("entity.duct_crawler.knock"), SoundSource.HOSTILE,
                    1.2f, 0.8f + random.nextFloat() * 0.4f);
        }
    }

    private void flee(Vec3 from, int ticks) {
        retreat = ticks;
        Vec3 away = DefaultRandomPos.getPosAway(this, 14, 2, from);
        if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.5);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            level().playSound(null, blockPosition(), ModSounds.get("entity.duct_crawler.hiss"), SoundSource.HOSTILE, 1.4f, 1f);
            flee(target.position(), 60 + random.nextInt(60));
            setTarget(null);
        }
        return hit;
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.5f : 0.25f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.duct_crawler.knock");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.duct_crawler.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.duct_crawler.hiss");
    }
}
