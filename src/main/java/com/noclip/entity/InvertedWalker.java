package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Someone walking on the carpet overhead in the Inverted Lobby: upside down to
 * you, right way up to them. They follow you across the ceiling while you are
 * not looking up. Let one stand above you for too long and it lets go.
 */
public class InvertedWalker extends BackroomsMonster {
    public static EntityType<InvertedWalker> TYPE;
    private int above;
    private boolean dropping;

    public InvertedWalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static void register() {
        TYPE = ModEntities.register("inverted_walker", InvertedWalker::new, MobCategory.MONSTER, 0.6f, 1.95f, createAttributes(), 0xD8C890, 0x4A4438);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 6);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        noPhysics = !dropping;
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 48);
        if (p == null) {
            if (tickCount > 200) discard();
            return;
        }
        if (dropping) {
            if (onGround() || tickCount % 200 == 0) {
                if (distanceToSqr(p) < 2.5 * 2.5 && p instanceof ServerPlayer sp && !sp.isCreative()) {
                    sp.hurt(damageSources().mobAttack(this), 6f);
                    Sanity.add(sp, -15f);
                }
                discard();
            }
            return;
        }
        Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        double d = to.length();
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        boolean seen = Mannequin.seenByAnyone(level(), this, 0.8);
        if (!seen && d > 0.4) {
            Vec3 step = to.normalize().scale(Math.min(d, 0.11));
            setPos(getX() + step.x, getY(), getZ() + step.z);
            setDeltaMovement(step.x, 0, step.z);
            setYRot(yaw);
            yBodyRot = yaw;
            setYHeadRot(yaw);
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        above = d < 1.4 && !p.isCreative() ? above + 1 : 0;
        if (above > 30) {
            dropping = true;
            noPhysics = false;
            setNoGravity(false);
            level().playSound(null, blockPosition(), ModSounds.get("entity.inverted_walker.drop"), SoundSource.HOSTILE, 1.2f, 1f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return dropping && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float fear() {
        return 0.3f;
    }
}
