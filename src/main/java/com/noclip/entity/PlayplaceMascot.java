package com.noclip.entity;

import com.noclip.levels.Playground;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Indoor Playground. "It". Counts to ten, then comes looking. If it can see you
 * it has found you. It cannot see into the ball pits or the tubes.
 */
public class PlayplaceMascot extends BackroomsMonster {
    public static EntityType<PlayplaceMascot> TYPE;
    private int step;
    private Vec3 lastSeen;

    public PlayplaceMascot(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    public static void register() {
        TYPE = ModEntities.register("playplace_mascot", PlayplaceMascot::new, MobCategory.MONSTER, 0.8f, 2.4f,
                createAttributes(), 0x8A4AB0, 0xE8C020);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (!Playground.seeking() && tickCount > 20) {
            discard();
            return;
        }
        if (tickCount % 5 == 0) {
            Player target = getTarget() instanceof Player p ? p : null;
            if (target != null && (Playground.hidden(target) || target.isCreative() || target.isSpectator())) {
                lastSeen = target.position();
                setTarget(null);
                getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 0.8);
            }
            if (getTarget() == null) {
                for (ServerPlayer p : sl.players()) {
                    if (p.isCreative() || p.isSpectator() || Playground.hidden(p) || p.distanceToSqr(this) > 30 * 30) continue;
                    Vec3 to = p.getEyePosition().subtract(getEyePosition()).normalize();
                    if (getViewVector(1f).dot(to) < 0.2 || !hasLineOfSight(p)) continue;
                    setTarget(p);
                    level().playSound(null, blockPosition(), ModSounds.get("event.playground_found"), SoundSource.HOSTILE, 1.8f, 1f);
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("§d§oFound you!"), true);
                    Sanity.add(p, -6f);
                    break;
                }
            }
            if (getTarget() == null && getNavigation().isDone() && tickCount % 80 == 0) {
                // seeking: drift toward the nearest player's general area
                Player p = sl.getNearestPlayer(this, 64);
                if (p != null) getNavigation().moveTo(p.getX() + random.nextInt(17) - 8, p.getY(), p.getZ() + random.nextInt(17) - 8, 0.75);
            }
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.001 && ++step > 30) {
            step = 0;
            level().playSound(null, blockPosition(), ModSounds.get("entity.playplace_mascot.step"), SoundSource.HOSTILE, 0.9f, 1f);
        }
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.5f : 0.2f;
    }
}
