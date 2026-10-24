package com.noclip.util;

/** Stateless, deterministic hashing used by every level generator. */
public final class Hash {
    private Hash() {}

    public static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    public static long hash(long seed, long a) {
        return mix(seed ^ mix(a * 0x9E3779B97F4A7C15L));
    }

    public static long hash(long seed, long a, long b) {
        return mix(hash(seed, a) ^ mix(b * 0xC2B2AE3D27D4EB4FL + 0x165667B19E3779F9L));
    }

    public static long hash(long seed, long a, long b, long c) {
        return mix(hash(seed, a, b) ^ mix(c * 0x27D4EB2F165667C5L + 0x85EBCA77C2B2AE63L));
    }

    /** Uniform float in [0,1). */
    public static float unit(long h) {
        return (h >>> 40) * 0x1.0p-24f;
    }

    public static float unit(long seed, long a, long b) {
        return unit(hash(seed, a, b));
    }

    public static float unit(long seed, long a, long b, long c) {
        return unit(hash(seed, a, b, c));
    }

    public static int range(long h, int bound) {
        return (int) Math.floorMod(h >>> 1, (long) bound);
    }

    public static int range(long seed, long a, long b, int bound) {
        return range(hash(seed, a, b), bound);
    }

    public static int range(long seed, long a, long b, long c, int bound) {
        return range(hash(seed, a, b, c), bound);
    }

    public static boolean chance(long seed, long a, long b, float p) {
        return unit(seed, a, b) < p;
    }

    public static boolean chance(long seed, long a, long b, long c, float p) {
        return unit(seed, a, b, c) < p;
    }

    public static long salt(String s) {
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return mix(h);
    }

    /** Smooth 2D value noise in [0,1]. */
    public static float noise(long seed, double x, double z) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        double fx = x - xi, fz = z - zi;
        double u = fx * fx * (3 - 2 * fx), v = fz * fz * (3 - 2 * fz);
        float a = unit(seed, xi, zi), b = unit(seed, xi + 1, zi);
        float c = unit(seed, xi, zi + 1), d = unit(seed, xi + 1, zi + 1);
        return (float) (a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v);
    }

    /** Fractal value noise in [0,1]. */
    public static float fbm(long seed, double x, double z, int octaves) {
        float sum = 0, amp = 0.5f, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise(seed + i * 7919L, x, z);
            norm += amp;
            x *= 2.03;
            z *= 2.03;
            amp *= 0.5f;
        }
        return sum / norm;
    }
}
