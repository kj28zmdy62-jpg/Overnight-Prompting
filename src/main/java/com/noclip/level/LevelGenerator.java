package com.noclip.level;

import com.noclip.world.ChunkWriter;

/** Builds the blocks of one chunk of a Backrooms level. Must be deterministic per (seed, chunk). */
@FunctionalInterface
public interface LevelGenerator {
    void generate(ChunkWriter w);
}
