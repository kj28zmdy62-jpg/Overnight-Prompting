package com.noclip.entity;

import com.noclip.level.ExitType;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
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

/**
 * A resident of Level 188. It stands in a lit window and looks down at the
 * courtyard. Whenever nobody is watching it, it is in a different window, a
 * little lower, a little closer. On the ground floor it comes out.
 */
public class WindowWatcher extends BackroomsMonster {
    public static EntityType<WindowWatcher> TYPE;
    /** Window bookkeeping, set by Level 188's behaviour. outside = it has left the building. */
    public int side, column, floor;
    public boolean outside;
    private int knock;

    public WindowWatcher(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("window_watcher", WindowWatcher::new, MobCategory.MONSTER, 0.6f, 1.95f, createAttributes(), 0x16161A, 0xD8D4CC);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 72);
        if (p == null) {
            if (tickCount > 200) discard();
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        boolean seen = Mannequin.seenByAnyone(level(), this, 0.6);
        if (!outside) {
            // in the window: it only ever turns to face you
            setDeltaMovement(0, 0, 0);
            setYRot(yaw);
            setYHeadRot(yaw);
            yBodyRot = yaw;
            getLookControl().setLookAt(p);
            if (seen && distanceToSqr(p) < 14 * 14 && --knock <= 0) {
                level().playSound(null, blockPosition(), ModSounds.get("entity.window_watcher.knock"), SoundSource.HOSTILE, 0.8f, 0.9f + random.nextFloat() * 0.2f);
                knock = 120 + random.nextInt(160);
            }
            return;
        }
        if (p.isCreative() || p.isSpectator()) {
            getNavigation().stop();
            return;
        }
        // out: slow while you watch it, quick when you don't
        getNavigation().moveTo(p, seen ? 0.45 : 1.25);
        if (distanceToSqr(p) < 1.7 * 1.7 && p instanceof ServerPlayer sp) {
            sp.hurt(damageSources().mobAttack(this), 4f);
            Sanity.add(sp, -20f);
            level().playSound(null, blockPosition(), ModSounds.get("entity.window_watcher.break"), SoundSource.HOSTILE, 1f, 0.6f);
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("It takes your wrist and leads you inside.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
            Travel.exit(sp, ExitType.SECRET, blockPosition());
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!outside) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return outside;
    }

    @Override
    public boolean isNoGravity() {
        return !outside;
    }

    @Override
    public float fear() {
        return outside ? 0.5f : 0.12f;
    }
}
