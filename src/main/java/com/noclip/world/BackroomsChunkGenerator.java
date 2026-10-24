package com.noclip.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.noclip.Noclip;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.util.Hash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * One chunk generator type for every level. The "level" field picks which
 * {@link com.noclip.level.LevelGenerator} draws the chunk. Generation happens in
 * {@link #buildSurface} because that is the first step that knows the world seed.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.STRING.fieldOf("level").forGetter(g -> g.levelId)
    ).apply(i, BackroomsChunkGenerator::new));

    private final String levelId;
    private BackroomsLevel level;

    public BackroomsChunkGenerator(BiomeSource biomeSource, String levelId) {
        super(biomeSource);
        this.levelId = levelId;
    }

    private BackroomsLevel level() {
        if (level == null) {
            level = Levels.get(levelId);
            if (level == null) throw new IllegalStateException("Unknown Backrooms level '" + levelId + "'");
        }
        return level;
    }

    public String levelId() {
        return levelId;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        long seed = Hash.hash(region.getSeed(), Hash.salt(levelId));
        try {
            level().generator.generate(new ChunkWriter(chunk, seed));
        } catch (RuntimeException e) {
            Noclip.LOG.error("Generator for {} failed at chunk {}", levelId, chunk.getPos(), e);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height, RandomState random) {
        return level().spawnMinY;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] states = new BlockState[height.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(height.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Backrooms: " + levelId);
    }
}
