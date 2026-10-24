package com.noclip.levels;

import com.noclip.util.Hash;
import com.noclip.world.CellGrid;

/**
 * The Lobby's floor plan, for the levels that are the Lobby gone wrong
 * (overgrown, flooded, inverted). Uneven rooms with doorways, half walls and
 * open sides, the same feel as Level 0, generated from its own seed.
 */
final class LobbyLayout {
    final CellGrid g;
    final long s;

    LobbyLayout(long seed) {
        this.s = seed;
        this.g = new CellGrid(seed, 8, 3);
    }

    int cellX(int x) {
        return g.cellX(x);
    }

    int cellZ(int z) {
        return g.cellZ(z);
    }

    /** True where a wall column stands. */
    boolean wall(int x, int z) {
        int i = g.cellX(x), k = g.cellZ(z);
        int u = x - g.x0(i), v = z - g.z0(k);
        if (u == 0 && v == 0) return true;
        if (u == 0) return edge(0, i, k, v, g.depth(k));
        if (v == 0) return edge(1, i, k, u, g.width(i));
        return false;
    }

    private boolean edge(int dir, int i, int k, int t, int len) {
        long h = g.hash(i, k, 100 + dir);
        float r = Hash.unit(h);
        if (r < 0.34f) return false;                       // open
        if (r < 0.56f) return true;                        // solid
        if (r < 0.66f) return t < len / 2;                 // half wall
        int gap = 1 + Hash.range(h >>> 8, Math.max(1, len - 3));
        int width = 1 + (int) ((h >>> 20) & 1);
        return t < gap || t >= gap + width;                // doorway
    }

    /** A per-room random value, for set pieces. */
    float room(int x, int z, long salt) {
        return g.unit(g.cellX(x), g.cellZ(z), salt);
    }
}
