package com.noclip.sanity;

import com.noclip.Noclip;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.net.NoclipNet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sanity drains while you are in the Backrooms: faster in the dark, faster near
 * entities, slower with company. At zero your mind starts to give out.
 */
public final class Sanity {
    public static final ResourceKey<DamageType> INSANITY = ResourceKey.create(Registries.DAMAGE_TYPE, Noclip.id("insanity"));
    private static final Map<UUID, Float> lastSent = new ConcurrentHashMap<>();
    private static final com.noclip.level.SpawnRule LOW_SANITY_FIGURE =
            com.noclip.level.SpawnRule.of(com.noclip.registry.ModEntities.lazy("shadow_figure"), 1, 1f).seen().dist(24, 40);

    private Sanity() {}

    /** Entities that frighten the player implement this. */
    public interface Frightening {
        /** Sanity lost per second while within 16 blocks and visible. */
        float fear();
    }

    public static void tick(ServerPlayer p) {
        if (p.tickCount % 20 != 0 || p.isCreative() || p.isSpectator()) {
            if (p.tickCount % 20 == 0) sync(p, false);
            return;
        }
        ServerLevel level = p.serverLevel();
        BackroomsLevel def = Levels.get(level.dimension());
        float s = PlayerData.sanity(p);
        if (def == null) {
            s += 0.5f;
        } else {
            float drain = def.sanityDrain;
            int light = level.getMaxLocalRawBrightness(p.blockPosition().above());
            if (light < 4) drain *= 2.5f;
            else if (light < 8) drain *= 1.5f;
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) {
                if (m instanceof Frightening f && p.hasLineOfSight(m)) drain += f.fear();
            }
            for (Player other : level.players()) {
                if (other != p && other.distanceToSqr(p) < 256) {
                    drain *= 0.6f;
                    break;
                }
            }
            s -= drain;
            // a fraying mind sees someone standing at the end of the hall
            if (s < 30 && p.getRandom().nextInt(70) == 0) {
                com.noclip.world.EntityDirector.trySpawn(p, level, def, LOW_SANITY_FIGURE);
            }
            if (s <= 0 && p.tickCount % 60 == 0) {
                p.hurt(insanity(level), 1.5f);
            }
        }
        PlayerData.setSanity(p, s);
        sync(p, false);
    }

    public static void add(ServerPlayer p, float amount) {
        PlayerData.setSanity(p, PlayerData.sanity(p) + amount);
        sync(p, true);
    }

    public static void sync(ServerPlayer p, boolean force) {
        float s = PlayerData.sanity(p);
        Float last = lastSent.get(p.getUUID());
        if (force || last == null || Math.abs(last - s) >= 0.25f || p.tickCount % 200 == 0) {
            lastSent.put(p.getUUID(), s);
            ServerPlayNetworking.send(p, new NoclipNet.SanityPayload(s));
        }
    }

    public static void forget(ServerPlayer p) {
        lastSent.remove(p.getUUID());
    }

    public static DamageSource insanity(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INSANITY));
    }
}
