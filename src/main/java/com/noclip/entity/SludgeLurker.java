package com.noclip.entity;

import com.noclip.block.SludgeBlock;
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
import net.minecraft.world.phys.Vec3;

/**
 * Sewers. It lives under the sludge and moves along the channels. You see the
 * bubbles before you see the eyes. When you wade, or stand at the edge of the
 * ledge, it comes up for your ankles and pulls.
 */
public class SludgeLurker extends BackroomsMonster {
    public static EntityType<SludgeLurker> TYPE;
    private double surface;
    private boolean anchored;
    private int risen, cooldown;

    public SludgeLurker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static void register() {
        TYPE = ModEntities.register("sludge_lurker", SludgeLurker::new, MobCategory.MONSTER, 0.8f, 1.2f,
                createAttributes(), 0x2A2A1A, 0xE8E8C0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void anchor(double surfaceY) {
        surface = surfaceY;
        anchored = true;
    }

    @Override
    protected void registerGoals() {
    }

    private boolean sludge(double x, double z) {
        return level().getBlockState(BlockPos.containing(x, surface, z)).getBlock() instanceof SludgeBlock;
    }

    @Override
    public void tick() {
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (level().isClientSide) return;
        if (!anchored) anchor(Math.floor(getY()) + 0.8);
        if (cooldown > 0) cooldown--;
        Player prey = level().getNearestPlayer(this, 14);
        if (prey != null && (prey.isCreative() || prey.isSpectator())) prey = null;
        double tx = getX(), tz = getZ();
        boolean close = false;
        if (prey != null) {
            Vec3 to = prey.position().subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            close = d < 2.6;
            if (d > 0.8) {
                Vec3 step = to.normalize().scale(0.11);
                if (sludge(getX() + step.x, getZ() + step.z)) {
                    tx += step.x;
                    tz += step.z;
                } else if (sludge(getX() + step.x, getZ())) {
                    tx += step.x;
                } else if (sludge(getX(), getZ() + step.z)) {
                    tz += step.z;
                }
            }
            float yaw = (float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        if (close && risen == 0 && cooldown == 0) {
            risen = 40;
            level().playSound(null, blockPosition(), ModSounds.get("entity.sludge_lurker.surface"), SoundSource.HOSTILE, 1.2f, 0.9f);
        }
        if (risen > 0) {
            risen--;
            if (risen == 25 && prey != null && distanceToSqr(prey) < 3.5 * 3.5) {
                prey.hurt(damageSources().mobAttack(this), 3f);
                Vec3 pull = position().subtract(prey.position()).multiply(1, 0, 1).normalize().scale(0.5);
                prey.push(pull.x, 0, pull.z);
                prey.hurtMarked = true;
                prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
                level().playSound(null, prey.blockPosition(), ModSounds.get("event.sewers_grab"), SoundSource.HOSTILE, 1.3f, 1f);
                if (prey instanceof ServerPlayer sp) Sanity.add(sp, -5f);
            }
            if (risen == 0) cooldown = 80;
        }
        double y = risen > 0 ? surface - 0.45 : surface - 1.5;
        setPos(tx, getY() + (y - getY()) * 0.25, tz);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float fear() {
        return risen > 0 ? 0.5f : 0.05f;
    }
}
