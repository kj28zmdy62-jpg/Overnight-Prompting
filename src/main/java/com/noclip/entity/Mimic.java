package com.noclip.entity;

import com.noclip.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Footsteps that are not yours. It walks exactly where you walked, a few seconds
 * behind, and it does not always stop when you do. There is nothing to see.
 */
public class Mimic extends PathfinderMob implements Sanity.Frightening {
    private final Deque<Vec3> trail = new ArrayDeque<>();
    private int extraSteps;
    private int stepTimer;

    public Mimic(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 64);
        if (p == null || tickCount > 3000) {
            discard();
            return;
        }
        boolean playerMoving = p.getDeltaMovement().horizontalDistanceSqr() > 0.002 && p.onGround();
        if (playerMoving) {
            trail.addLast(p.position());
            extraSteps = 2 + random.nextInt(3);
        }
        // stay a few seconds behind; when the player stops, take a couple more steps anyway
        boolean walk = false;
        if (trail.size() > 60 || (!playerMoving && extraSteps > 0 && !trail.isEmpty())) {
            Vec3 next = trail.pollFirst();
            if (next != null) {
                setPos(next);
                walk = true;
            }
        }
        if (walk && ++stepTimer % 7 == 0) {
            if (!playerMoving) extraSteps--;
            BlockPos below = blockPosition().below();
            SoundType st = level().getBlockState(below).getSoundType();
            level().playSound(null, getX(), getY(), getZ(), st.getStepSound(), SoundSource.HOSTILE,
                    st.getVolume() * 0.5f, st.getPitch() * 0.95f);
        }
        if (distanceToSqr(p) < 4) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float fear() {
        return 0.05f;
    }
}
