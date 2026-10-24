package com.noclip.sanity;

import com.mojang.serialization.Codec;
import com.noclip.Noclip;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Persistent per-player Backrooms state. */
@SuppressWarnings("UnstableApiUsage")
public final class PlayerData {
    public static final AttachmentType<Float> SANITY = AttachmentRegistry.<Float>builder()
            .persistent(Codec.FLOAT)
            .initializer(() -> 100f)
            .buildAndRegister(Noclip.id("sanity"));

    public static final AttachmentType<List<String>> VISITED = AttachmentRegistry.<List<String>>builder()
            .persistent(Codec.STRING.listOf())
            .copyOnDeath()
            .initializer(List::of)
            .buildAndRegister(Noclip.id("visited"));

    private PlayerData() {}

    public static void init() {
    }

    public static float sanity(Player p) {
        return p.getAttachedOrCreate(SANITY);
    }

    public static void setSanity(Player p, float v) {
        p.setAttached(SANITY, Math.max(0f, Math.min(100f, v)));
    }

    public static List<String> visited(Player p) {
        return p.getAttachedOrCreate(VISITED);
    }

    /** @return true if this was the first visit */
    public static boolean visit(Player p, String level) {
        List<String> v = visited(p);
        if (v.contains(level)) return false;
        List<String> copy = new ArrayList<>(v);
        copy.add(level);
        p.setAttached(VISITED, copy);
        return true;
    }
}
