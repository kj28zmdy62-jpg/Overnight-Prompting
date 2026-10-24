package com.noclip.travel;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.Levels;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ways in. Suffocating inside a wall in the real world can push you through
 * it, and pressing against the walls of the Backrooms long enough can make them
 * give way.
 */
public final class NoclipTriggers {
    private static final Map<UUID, Integer> wallPush = new ConcurrentHashMap<>();
    /** Chance per suffocation hit in the overworld to slip into Level 0. */
    public static float suffocationChance = 0.35f;

    private NoclipTriggers() {}

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer p && source.is(DamageTypes.IN_WALL) && !Travel.onCooldown(p)) {
                boolean backrooms = Levels.isBackrooms(p.level().dimension());
                float chance = backrooms ? 0.5f : (p.level().dimension() == Level.OVERWORLD ? suffocationChance : 0f);
                if (p.getRandom().nextFloat() < chance) {
                    if (backrooms) Travel.exit(p, ExitType.NOCLIP, p.blockPosition());
                    else Travel.sendTo(p, "level_0");
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * The client reports that it has been leaning into a wall. We check that the
     * player really is against something solid, then roll the dice.
     */
    public static void onWallPush(ServerPlayer p, int ticks) {
        if (p.isSpectator() || Travel.onCooldown(p) || ticks < 100) return;
        net.minecraft.core.BlockPos ahead = net.minecraft.core.BlockPos.containing(
                p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.8)).add(0, 0.5, 0));
        if (p.level().getBlockState(ahead).getCollisionShape(p.level(), ahead).isEmpty()) return;
        wallPush.put(p.getUUID(), ticks);
        BackroomsLevel def = Levels.get(p.level().dimension());
        if (def != null) {
            // Reports arrive once a second; after five seconds each has a 1 in 12 chance to give way.
            if (p.getRandom().nextInt(12) == 0) Travel.exit(p, ExitType.NOCLIP, p.blockPosition());
        } else if (p.level().dimension() == Level.OVERWORLD && p.getRandom().nextInt(300) == 0) {
            Travel.sendTo(p, "level_0");
        }
    }

    public static int pushTicks(ServerPlayer p) {
        return wallPush.getOrDefault(p.getUUID(), 0);
    }

    public static void forget(ServerPlayer p) {
        wallPush.remove(p.getUUID());
    }
}
