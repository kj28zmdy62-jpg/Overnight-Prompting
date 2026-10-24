package com.noclip.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Writes blocks into a single chunk using world coordinates. Anything outside
 * the chunk is silently clipped, so generators can draw features that straddle
 * chunk borders without caring which chunk is being built.
 */
public final class ChunkWriter {
    public final ChunkAccess chunk;
    public final int minX, minZ, maxX, maxZ, minY, maxY;
    public final long seed;
    private final BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

    public ChunkWriter(ChunkAccess chunk, long seed) {
        this.chunk = chunk;
        this.seed = seed;
        this.minX = chunk.getPos().getMinBlockX();
        this.minZ = chunk.getPos().getMinBlockZ();
        this.maxX = minX + 15;
        this.maxZ = minZ + 15;
        this.minY = chunk.getMinBuildHeight();
        this.maxY = chunk.getMaxBuildHeight() - 1;
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(int x, int y, int z) {
        return contains(x, z) && y >= minY && y <= maxY;
    }

    public void set(int x, int y, int z, BlockState state) {
        if (!contains(x, y, z)) return;
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        section.setBlockState(x & 15, y & 15, z & 15, state, false);
    }

    public BlockState get(int x, int y, int z) {
        if (!contains(x, y, z)) return Blocks.AIR.defaultBlockState();
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    public boolean isAir(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    /** Fill an inclusive box, clipped to this chunk. */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int ax = Math.max(Math.min(x1, x2), minX), bx = Math.min(Math.max(x1, x2), maxX);
        int az = Math.max(Math.min(z1, z2), minZ), bz = Math.min(Math.max(z1, z2), maxZ);
        int ay = Math.max(Math.min(y1, y2), minY), by = Math.min(Math.max(y1, y2), maxY);
        for (int y = ay; y <= by; y++) {
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
            for (int x = ax; x <= bx; x++)
                for (int z = az; z <= bz; z++)
                    section.setBlockState(x & 15, y & 15, z & 15, state, false);
        }
    }

    /** Fill a whole horizontal layer of this chunk. */
    public void layer(int y, BlockState state) {
        fill(minX, y, minZ, maxX, y, maxZ, state);
    }

    /** Hollow box: walls only (floor and ceiling untouched). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        fill(x1, y1, z1, x2, y2, z1, state);
        fill(x1, y1, z2, x2, y2, z2, state);
        fill(x1, y1, z1, x1, y2, z2, state);
        fill(x2, y1, z1, x2, y2, z2, state);
    }

    /** Replace only blocks matching {@code from}. */
    public void replace(int x1, int y1, int z1, int x2, int y2, int z2, BlockState from, BlockState to) {
        int ax = Math.max(Math.min(x1, x2), minX), bx = Math.min(Math.max(x1, x2), maxX);
        int az = Math.max(Math.min(z1, z2), minZ), bz = Math.min(Math.max(z1, z2), maxZ);
        int ay = Math.max(Math.min(y1, y2), minY), by = Math.min(Math.max(y1, y2), maxY);
        for (int y = ay; y <= by; y++)
            for (int x = ax; x <= bx; x++)
                for (int z = az; z <= bz; z++)
                    if (get(x, y, z) == from) set(x, y, z, to);
    }

    public BlockPos.MutableBlockPos pos(int x, int y, int z) {
        return mut.set(x, y, z);
    }
}
