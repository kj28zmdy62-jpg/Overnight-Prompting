package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * null. Something that was meant to be a person and never got its texture.
 * It does not walk. While you are not looking it skips toward you, a few
 * blocks at a time, like a frame that failed to draw.
 */
public class MtNull extends BackroomsMonster {
    public static EntityType<MtNull> TYPE;
    private int cooldown;

    public MtNull(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("mt_null", MtNull::new, MobCategory.MONSTER, 0.6f, 2.1f, createAttributes(), 0xF800F8, 0x000000);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 40);
        if (p == null || p.isCreative() || p.isSpectator()) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        if (cooldown > 0) cooldown--;
        if (distanceToSqr(p) < 2.2 && cooldown == 0) {
            doHurtTarget(p);
            if (p instanceof ServerPlayer sp) Sanity.add(sp, -6f);
            cooldown = 30;
            return;
        }
        if (tickCount % 30 != 0 || Mannequin.seenByAnyone(level(), this, 0.5)) return;
        Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        double d = Math.min(3.0, to.length() - 1.0);
        if (d <= 0) return;
        Vec3 step = to.normalize().scale(d);
        double ox = getX(), oy = getY(), oz = getZ();
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos at = BlockPos.containing(ox + step.x, oy + dy, oz + step.z);
            if (level().getBlockState(at.below()).isSolidRender(level(), at.below())) {
                moveTo(ox + step.x, at.getY(), oz + step.z, yaw, 0f);
                if (level().noCollision(this)) {
                    level().playSound(null, blockPosition(), ModSounds.get("entity.mt_null.blink"), SoundSource.HOSTILE, 0.8f, 0.8f + random.nextFloat() * 0.4f);
                    return;
                }
                moveTo(ox, oy, oz, yaw, 0f);
            }
        }
    }

    @Override
    public float fear() {
        return 0.35f;
    }
}
