package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Level 94's clay people. They are animated one frame at a time, and the frames
 * only advance when you move. Stand still and so do they. Walk, and with every
 * couple of steps you take, each of them takes one toward you.
 */
public class MotionClayFigure extends BackroomsMonster {
    public static EntityType<MotionClayFigure> TYPE;
    private Vec3 lastSeen;
    private double film;
    private int cooldown;

    public MotionClayFigure(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("motion_clay_figure", MotionClayFigure::new, MobCategory.MONSTER, 0.7f, 2.2f, createAttributes(), 0xE8B890, 0xE0C030);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        Player p = level().getNearestPlayer(this, 64);
        if (p == null || p.isSpectator()) return;
        Vec3 now = p.position();
        if (lastSeen != null && !p.isCreative()) film += Math.min(2.0, now.subtract(lastSeen).horizontalDistance());
        lastSeen = now;
        if (cooldown > 0) cooldown--;
        // one frame for every 1.6 blocks the player moves
        while (film >= 1.6) {
            film -= 1.6;
            frame(p);
        }
    }

    private void frame(Player p) {
        Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        double dist = to.length();
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z)) + (random.nextFloat() - 0.5f) * 20f;
        if (dist < 1.8) {
            if (cooldown == 0 && p instanceof ServerPlayer sp) {
                sp.hurt(damageSources().mobAttack(this), 3f);
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
                Sanity.add(sp, -6f);
                cooldown = 20;
            }
            return;
        }
        Vec3 step = to.normalize().scale(Math.min(1.1, dist - 1.2));
        int nx = (int) Math.floor(getX() + step.x), nz = (int) Math.floor(getZ() + step.z);
        int ny = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nx, nz);
        if (Math.abs(ny - getY()) > 2.5) return;
        if (!level().getBlockState(new BlockPos(nx, ny + 1, nz)).getCollisionShape(level(), new BlockPos(nx, ny + 1, nz)).isEmpty()) return;
        moveTo(getX() + step.x, ny, getZ() + step.z, yaw, 0f);
        setYHeadRot(yaw + (random.nextFloat() - 0.5f) * 30f);
        yBodyRot = yaw;
        level().playSound(null, blockPosition(), ModSounds.get("entity.motion_clay_figure.hop"), SoundSource.HOSTILE, 0.8f, 0.8f + random.nextFloat() * 0.4f);
    }

    @Override
    public float fear() {
        return 0.25f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
