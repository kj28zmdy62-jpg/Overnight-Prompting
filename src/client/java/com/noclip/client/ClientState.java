package com.noclip.client;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.level.Visuals;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Everything the client knows about the player's trip through the Backrooms. */
public final class ClientState {
    public static float sanity = 100f;
    public static float shownSanity = 100f;
    public static BackroomsLevel level;
    public static long enteredAt;
    public static boolean firstVisit;
    public static int discovered;
    public static List<String> visited = new ArrayList<>();
    public static int wallPushTicks;
    public static float noclipFlash;
    public static float pulse;
    public static boolean camcorder;
    /** Wall-clock end of the current blackout, 0 if none. */
    public static long blackoutUntil;

    /** How much to remove ambient light from the lightmap (dark levels). Eases in and out. */
    private static float lightmapDark;

    public static float lightmapDarkness() {
        float target = level != null ? level.visuals.darkness : 0f;
        lightmapDark += (target - lightmapDark) * 0.05f;
        return lightmapDark < 0.005f ? 0f : lightmapDark;
    }

    /** 0..1 darkness from a level-wide blackout, flickering at its start and end. */
    public static float blackout() {
        long left = blackoutUntil - System.currentTimeMillis();
        if (left <= 0) return 0f;
        long t = System.currentTimeMillis();
        if (left < 1500) return ((t / 90) % 3 == 0) ? 0.2f : 0.95f;
        return 0.95f;
    }

    /** Blended visual profile; eases between levels instead of snapping. */
    public static final Visuals current = new Visuals();
    public static float effectStrength;

    private ClientState() {}

    public static float insanity() {
        return Math.max(0f, Math.min(1f, (65f - shownSanity) / 65f));
    }

    public static void enter(String id, boolean first, int count) {
        level = id.isEmpty() ? null : Levels.get(id);
        blackoutUntil = 0;
        enteredAt = System.currentTimeMillis();
        firstVisit = first;
        discovered = count;
        noclipFlash = 1f;
    }

    /** Called every client tick to keep the level in sync with the dimension we are actually in. */
    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            level = null;
            return;
        }
        BackroomsLevel byDim = Levels.get(mc.level.dimension());
        if (byDim != level) {
            level = byDim;
            if (level != null && enteredAt == 0) enteredAt = System.currentTimeMillis();
        }
        shownSanity += (sanity - shownSanity) * 0.08f;
    }

    public static long millisInLevel() {
        return System.currentTimeMillis() - enteredAt;
    }
}
