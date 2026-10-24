package com.noclip.world;

import com.noclip.net.NoclipNet;
import com.noclip.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level-wide power cuts. While a blackout runs every player in the level sees
 * near-total darkness, and things that only come out in the dark can spawn
 * anywhere. Any level's behaviour can start one.
 */
public final class Blackouts {
    private static final Map<ResourceKey<Level>, Long> END = new ConcurrentHashMap<>();

    private Blackouts() {}

    public static void start(ServerLevel level, int ticks) {
        END.put(level.dimension(), level.getGameTime() + ticks);
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("blackout", ticks / 20f));
            level.playSound(null, p.blockPosition(), ModSounds.get("event.power_down"), SoundSource.AMBIENT, 1f, 1f);
        }
    }

    public static boolean active(Level level) {
        Long end = END.get(level.dimension());
        return end != null && level.getGameTime() < end;
    }

    /** Call when a player enters a level so late arrivals see an ongoing blackout too. */
    public static void sync(ServerPlayer p) {
        Long end = END.get(p.level().dimension());
        if (end != null && end > p.level().getGameTime()) {
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("blackout", (end - p.level().getGameTime()) / 20f));
        }
    }
}
