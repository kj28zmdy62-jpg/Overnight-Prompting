package com.noclip.entity;

import com.noclip.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Tide Walker. On the Silent Beach the sea makes no sound, so it can hear yours. Run,
 * jump or fight near the water and one of these stands up in the surf and walks after
 * you, slowly, dripping. Go quiet for long enough and it loses you and wades back in.
 */
public class TideWalker extends BackroomsMonster {
    public static EntityType<TideWalker> TYPE;
    private UUID prey;
    private int quiet;
    private boolean leaving;
    private double seaX;

    public TideWalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("sb_tidewalker", TideWalker::new, MobCategory.MONSTER, 0.6f, 2.1f, createAttributes(), 0xA8B4B8, 0x1E2220);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public void hunt(Player p, double seaX) {
        this.prey = p.getUUID();
        this.seaX = seaX;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isNoAi()) return;
        if (leaving) {
            getNavigation().moveTo(seaX + 30, getY(), getZ(), 1.0);
            if (isInWater() && getX() > seaX + 8 || tickCount > 20 * 120) discard();
            return;
        }
        Player p = prey == null ? null : level().getPlayerByUUID(prey);
        if (p == null || p.isSpectator() || p.isCreative() || p.distanceToSqr(this) > 48 * 48) {
            leaving = true;
            return;
        }
        quiet = Hound.noise(p) < 0.3f ? quiet + 1 : 0;
        if (quiet > 20 * 9) {
            leaving = true;
            return;
        }
        if (tickCount % 10 == 0) getNavigation().moveTo(p, 1.0);
        Vec3 look = p.getEyePosition();
        getLookControl().setLookAt(look.x, look.y, look.z);
        if (distanceToSqr(p) < 2.4) doHurtTarget(p);
    }

    @Override
    public float fear() {
        return leaving ? 0.1f : 0.6f;
    }
}
