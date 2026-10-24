package com.noclip.entity;

import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Groundskeeper. He comes out when the sprinklers stop, and he walks only on the
 * mown grass: fairways, greens and tees. Stand on the grass and he comes for you. Stand
 * in the rough, in a bunker, under the pines, and he stops at the edge of the fairway
 * and waits, raking, until it is time to go.
 */
public class Groundskeeper extends BackroomsMonster {
    public static EntityType<Groundskeeper> TYPE;
    private Vec3 lastMown;
    private int life = 20 * 45;

    public Groundskeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("gc_groundskeeper", Groundskeeper::new, MobCategory.MONSTER, 0.6f, 2.0f, createAttributes(), 0x2A3E5A, 0x3A5A3A);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean mown(Level level, BlockPos feet) {
        BlockState b = level.getBlockState(feet.below());
        if (b.isAir()) b = level.getBlockState(feet.below(2));
        return b.is(ModBlocks.get("gc_fairway")) || b.is(ModBlocks.get("gc_green")) || b.is(ModBlocks.get("gc_tee"));
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isNoAi()) return;
        if (--life <= 0) {
            discard();
            return;
        }
        if (mown(level(), blockPosition())) {
            lastMown = position();
        } else if (lastMown != null) {
            // he does not set foot off the grass
            getNavigation().stop();
            moveTo(lastMown.x, lastMown.y, lastMown.z, getYRot(), 0f);
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        Player p = level().getNearestPlayer(this, 64);
        if (p == null || p.isCreative() || p.isSpectator()) return;
        getLookControl().setLookAt(p, 30f, 30f);
        if (mown(level(), p.blockPosition())) {
            if (tickCount % 10 == 0) getNavigation().moveTo(p, 1.0);
            if (distanceToSqr(p) < 2.6) doHurtTarget(p);
        } else {
            getNavigation().stop();
            if (tickCount % 60 == 0) {
                level().playSound(null, blockPosition(), ModSounds.get("event.golf_course_rake"), SoundSource.HOSTILE, 1f, 0.9f + random.nextFloat() * 0.2f);
            }
        }
    }

    @Override
    public float fear() {
        return 0.5f;
    }
}
