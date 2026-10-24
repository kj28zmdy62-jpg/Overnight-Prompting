package com.noclip.entity;

import com.noclip.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Seeker. When the music box in the Foggy Playground has counted to ten, it comes
 * looking: a small hooded figure walking from swing set to slide. It sees what is in
 * front of it and close; crouch behind something and it walks right past. If it sees
 * you it runs, and if it touches you, you're it.
 */
public class Seeker extends BackroomsMonster {
    public static EntityType<Seeker> TYPE;
    public static Consumer<ServerPlayer> onTag = p -> {};
    private UUID prey;
    private boolean spotted;
    private Vec3 wander;
    private int life = 20 * 50;

    public Seeker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("fp_seeker", Seeker::new, MobCategory.MONSTER, 0.5f, 1.45f, createAttributes(), 0x8A2A2A, 0x3A4A6A);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public void seek(Player p) {
        prey = p.getUUID();
    }

    public boolean expired() {
        return life <= 0;
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
        life--;
        Player p = prey == null ? null : level().getPlayerByUUID(prey);
        if (p == null || life <= 0 || p.distanceToSqr(this) > 64 * 64) {
            discard();
            return;
        }
        if (spotted) {
            if (tickCount % 5 == 0) getNavigation().moveTo(p, 2.1);
            if (distanceToSqr(p) < 1.6 && p instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()) {
                onTag.accept(sp);
                discard();
            }
            return;
        }
        if (wander == null || tickCount % 80 == 0 || distanceToSqr(wander) < 4) {
            // drift toward where the player probably is
            wander = p.position().add((random.nextDouble() - 0.5) * 18, 0, (random.nextDouble() - 0.5) * 18);
            getNavigation().moveTo(wander.x, wander.y, wander.z, 1.0);
        }
        if (tickCount % 5 == 0 && sees(p)) {
            spotted = true;
            playSound(com.noclip.registry.ModSounds.get("event.foggy_playground_found"), 1.5f, 1.2f);
        }
    }

    private boolean sees(Player p) {
        if (p.isCreative() || p.isSpectator()) return false;
        double range = p.isShiftKeyDown() ? 5 : 13;
        if (distanceToSqr(p) > range * range) return false;
        Vec3 eye = getEyePosition();
        Vec3 to = p.getEyePosition().subtract(eye);
        Vec3 look = getViewVector(1f);
        if (to.normalize().dot(look) < 0.25 && distanceToSqr(p) > 4) return false;
        return level().clip(new ClipContext(eye, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    @Override
    public float fear() {
        return spotted ? 0.8f : 0.3f;
    }
}
