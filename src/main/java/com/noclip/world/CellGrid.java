package com.noclip.world;

import com.noclip.util.Hash;

/**
 * An infinite grid of rooms whose walls sit on jittered lines, so rooms come in
 * uneven sizes. Use it for any level built from rooms, halls or blocks.
 *
 * <pre>
 *   CellGrid g = new CellGrid(seed, 8, 3);    // cells 8 apart, lines jitter by 0..2
 *   int i = g.cellX(x), k = g.cellZ(z);        // which room a column is in
 *   g.x0(i), g.width(i)                        // room bounds (wall line is at x0)
 * </pre>
 */
public final class CellGrid {
    public final long seed;
    public final int size, jitter;

    public CellGrid(long seed, int size, int jitter) {
        this.seed = seed;
        this.size = size;
        this.jitter = jitter;
    }

    public int lineX(int i) {
        return i * size + (jitter > 0 ? Hash.range(seed, 1, i, jitter) : 0);
    }

    public int lineZ(int k) {
        return k * size + (jitter > 0 ? Hash.range(seed, 2, k, jitter) : 0);
    }

    public int cellX(int x) {
        int i = Math.floorDiv(x, size);
        return x < lineX(i) ? i - 1 : i;
    }

    public int cellZ(int z) {
        int k = Math.floorDiv(z, size);
        return z < lineZ(k) ? k - 1 : k;
    }

    public int x0(int i) {
        return lineX(i);
    }

    public int z0(int k) {
        return lineZ(k);
    }

    public int width(int i) {
        return lineX(i + 1) - lineX(i);
    }

    public int depth(int k) {
        return lineZ(k + 1) - lineZ(k);
    }

    /** Deterministic random value for a cell, distinguished by a salt. */
    public long hash(int i, int k, long salt) {
        return Hash.hash(seed, i, k, salt);
    }

    public float unit(int i, int k, long salt) {
        return Hash.unit(hash(i, k, salt));
    }

    /** Iterate every cell that overlaps the given chunk (with a one-cell margin). */
    public void forCells(ChunkWriter w, CellVisitor v) {
        int i0 = cellX(w.minX) - 1, i1 = cellX(w.maxX) + 1;
        int k0 = cellZ(w.minZ) - 1, k1 = cellZ(w.maxZ) + 1;
        for (int i = i0; i <= i1; i++)
            for (int k = k0; k <= k1; k++)
                v.visit(i, k, x0(i), z0(k), width(i), depth(k));
    }

    @FunctionalInterface
    public interface CellVisitor {
        void visit(int i, int k, int x0, int z0, int width, int depth);
    }
}
