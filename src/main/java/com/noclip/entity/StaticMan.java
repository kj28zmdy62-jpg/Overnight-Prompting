package com.noclip.entity;

import com.noclip.net.NoclipNet;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * The Broadcast. It is only ever seen on a television screen, and then it is
 * standing behind you. Turn around and hold its gaze until it comes apart into
 * static. Keep walking, and it closes the distance.
 */
public class StaticMan extends PathfinderMob implements Sanity.Frightening {
    public static EntityType<StaticMan> TYPE;
    /** Called with (player, faced) when the encounter ends. */
    public static BiConsumer<ServerPlayer, Boolean> onResolved = (p, f) -> {};

    private UUID victim;
    private int stared, unseen;

    public StaticMan(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("static_man", StaticMan::new, MobCategory.MONSTER, 0.6f, 2.3f, createAttributes(), 0xD8D8D0, 0x0A0A0C);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void haunt(ServerPlayer p) {
        victim = p.getUUID();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isNoAi()) return;
        Player p = victim != null ? level().getPlayerByUUID(victim) : level().getNearestPlayer(this, 32);
        if (!(p instanceof ServerPlayer sp) || p.distanceToSqr(this) > 40 * 40 || tickCount > 20 * 60) {
            discard();
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        boolean seen = Mannequin.seenByAnyone(level(), this, 0.82);
        if (seen) {
            stared++;
            unseen = 0;
            if (stared % 10 == 1) ServerPlayNetworking.send(sp, new NoclipNet.EventPayload("pulse", 0.5f));
            if (stared > 50) {
                level().playSound(null, blockPosition(), ModSounds.get("event.static_fields_burst"), SoundSource.HOSTILE, 1.4f, 0.7f);
                Sanity.add(sp, -6f);
                onResolved.accept(sp, true);
                discard();
            }
            return;
        }
        stared = Math.max(0, stared - 2);
        unseen++;
        // while you keep your back turned it closes the gap, a step at a time, never where you can see
        if (unseen % 25 == 0) {
            Vec3 to = p.position().subtract(position());
            double d = to.horizontalDistance();
            if (d > 1.4) {
                Vec3 step = to.multiply(1, 0, 1).normalize().scale(Math.min(1.0, d - 1.2));
                moveTo(getX() + step.x, getY(), getZ() + step.z, yaw, 0f);
                level().playSound(null, blockPosition(), ModSounds.get("event.static_fields_tv"), SoundSource.HOSTILE, 0.5f, 1.4f);
            }
        }
        if (distanceToSqr(p) < 2.2 * 2.2 && unseen > 60) {
            level().playSound(null, blockPosition(), ModSounds.get("event.static_fields_burst"), SoundSource.HOSTILE, 2.0f, 0.5f);
            sp.hurt(damageSources().mobAttack(this), 6f);
            Sanity.add(sp, -20f);
            ServerPlayNetworking.send(sp, new NoclipNet.EventPayload("flash", 1f));
            onResolved.accept(sp, false);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public float fear() {
        return 1.2f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }
}
