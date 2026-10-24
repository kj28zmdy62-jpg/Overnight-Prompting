package com.noclip.levels;

import com.noclip.block.PoolSlideBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Level 37, "Sublimity", the Poolrooms. White tile rooms that run on forever,
 * flooded knee-deep with warm, still water and lit by daylight that comes from
 * nowhere. It is beautiful. It is calm. The water is kind to you while it is
 * shallow, and the dark pools want you to come down and see the drain.
 */
public final class Level37 {
    static final int F = 16;          // floor under the shallows
    static final int W = F + 1;       // the water plane: every surface in the level sits here
    static final String ID = "level_37";

    private Level37() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("37").title("Sublimity").order(370)
                .tagline("Warm water. White tile. Daylight from nowhere. Stay as long as you like.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Sublimity())
                .visuals(new Visuals().tint(0.95f, 1.02f, 1.05f).brightness(1.0f).saturation(0.92f).contrast(1.02f)
                        .grain(0.03f).aberration(0.0009f).vignette(0.28f).warp(0.012f).scanlines(0.015f).flicker(0f)
                        .bloom(0.6f).fog(12f, 72f).fogColor(0xDCECEE))
                .sanityDrain(0.02f)
                .spawnY(W, W + 1)
                .waterSpawn()
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.03f).seen().dist(30, 44))
                .exit(ExitType.WATER, "level_7", 4).exit(ExitType.WATER, "flooded_lobby", 1)
                .exit(ExitType.DOOR, "level_94", 2).exit(ExitType.DOOR, "level_188", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_7", 1).exit(ExitType.NOCLIP, "level_94", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Room { SHALLOW, DECK, POOL, HALL, TUNNEL, DARK, SLIDE }

    static CellGrid grid(long seed) {
        return new CellGrid(seed, 12, 5);
    }

    static Room room(CellGrid g, int i, int k) {
        float r = g.unit(i, k, 1);
        if (r < 0.44f) return Room.SHALLOW;
        if (r < 0.53f) return Room.DECK;
        if (r < 0.71f) return Room.POOL;
        if (r < 0.81f) return Room.HALL;
        if (r < 0.90f) return Room.TUNNEL;
        if (r < 0.935f) return Room.DARK;
        return g.width(i) >= 12 && g.depth(k) >= 9 ? Room.SLIDE : Room.POOL;
    }

    /** Ceiling block height above the water plane. */
    static int headroom(CellGrid g, int i, int k) {
        return switch (room(g, i, k)) {
            case HALL, SLIDE -> 12;
            case TUNNEL -> 3;
            case DARK -> 9;
            case POOL -> 6 + Hash.range(g.hash(i, k, 2), 4);
            default -> 4 + Hash.range(g.hash(i, k, 2), 4);
        };
    }

    static int poolDepth(CellGrid g, int i, int k) {
        Room r = room(g, i, k);
        if (r == Room.DARK) return 9;
        if (r == Room.SLIDE) return 6;
        return 3 + Hash.range(g.hash(i, k, 3), 4);
    }

    /** Inset of the basin from the cell's wall line. */
    static int ring(Room r) {
        return r == Room.SLIDE ? 2 : 3;
    }

    static boolean inBasin(CellGrid g, int i, int k, int u, int v) {
        Room r = room(g, i, k);
        if (r != Room.POOL && r != Room.DARK && r != Room.SLIDE) return false;
        int in = ring(r);
        return u >= in && v >= in && u <= g.width(i) - in && v <= g.depth(k) - in;
    }

    static final class Gen implements LevelGenerator {
        final BlockState tile = ModBlocks.state("pool_tile_white");
        final BlockState lime = ModBlocks.state("pool_tile_limescale");
        final BlockState band = ModBlocks.state("pool_tile_banded");
        final BlockState mosaic = ModBlocks.state("pool_mosaic");
        final BlockState lane = ModBlocks.state("pool_mosaic_lane");
        final BlockState deep = ModBlocks.state("pool_mosaic_deep");
        final BlockState deck = ModBlocks.state("pool_deck_tile");
        final BlockState deckWet = ModBlocks.state("pool_deck_tile_wet");
        final BlockState coping = ModBlocks.state("pool_coping");
        final BlockState ceiling = ModBlocks.state("pool_ceiling_tile");
        final BlockState sky = ModBlocks.state("pool_skylight");
        final BlockState poolLight = ModBlocks.state("pool_underwater_light");
        final BlockState window = ModBlocks.state("pool_window_nowhere");
        final BlockState drain = ModBlocks.state("pool_drain");
        final BlockState ladder = ModBlocks.state("pool_ladder").setValue(LadderBlock.WATERLOGGED, true);
        final BlockState ladderDry = ModBlocks.state("pool_ladder");
        final BlockState slide = ModBlocks.state("pool_slide");
        final BlockState ringFloat = ModBlocks.state("pool_float_ring");
        final BlockState lounger = ModBlocks.state("pool_lounger");
        final BlockState prints = ModBlocks.state("pool_wet_footprints");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState doorway = ModBlocks.state("dark_doorway");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState water = Blocks.WATER.defaultBlockState();

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 10, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = g.cellX(x), k = g.cellZ(z);
                    column(w, g, s, x, z, i, k, x - g.x0(i), z - g.z0(k));
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> features(w, g, s, i, k, x0, z0, wd, dp));
        }

        BlockState wallAt(long s, int x, int y, int z) {
            if (y <= W) return lime;
            if (y == W + 3) return band;
            return Hash.chance(s, x, y * 31 + z, 5, 0.03f) ? lime : tile;
        }

        void column(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int u, int v) {
            Room room = room(g, i, k);
            int top = W + headroom(g, i, k);
            boolean wallX = u == 0, wallZ = v == 0;
            if (wallX || wallZ) {
                // the wall stands as tall as the taller of the two rooms it divides
                int other = wallX && wallZ ? top : wallX ? W + headroom(g, i - 1, k) : W + headroom(g, i, k - 1);
                int wallTop = Math.max(top, other);
                w.fill(x, F - 9, z, x, F, z, tile);
                for (int y = W; y < wallTop; y++) w.set(x, y, z, wallAt(s, x, y, z));
                w.set(x, wallTop, z, ceiling);
                w.fill(x, wallTop + 1, z, x, w.maxY, z, filler);
                if (!(wallX && wallZ)) opening(w, g, s, x, z, i, k, u, v, wallX, Math.min(top, other));
                return;
            }
            int wd = g.width(i), dp = g.depth(k);
            // ceiling
            w.set(x, top, z, skylightAt(g, room, i, k, u, v, wd, dp) ? sky : ceiling);
            w.fill(x, top + 1, z, x, w.maxY, z, filler);
            switch (room) {
                case DECK -> {
                    w.fill(x, F - 9, z, x, F, z, sub);
                    w.set(x, W, z, Hash.fbm(s ^ 0x3E7, x / 9.0, z / 9.0, 2) > 0.6f ? deckWet : deck);
                }
                case POOL, DARK, SLIDE -> basinColumn(w, g, s, x, z, i, k, u, v, room);
                case TUNNEL -> {
                    w.fill(x, F - 9, z, x, F - 1, z, sub);
                    w.set(x, F, z, deck);
                    boolean passage = u <= 2 || v <= 2 || u >= wd - 2 || v >= dp - 2
                            || Math.abs(u - wd / 2) <= 1 || Math.abs(v - dp / 2) <= 1;
                    if (passage) {
                        w.set(x, W, z, water);
                    } else {
                        for (int y = W; y < top; y++) w.set(x, y, z, wallAt(s, x, y, z));
                    }
                }
                default -> {
                    // SHALLOW and HALL: knee-deep water over white tile
                    w.fill(x, F - 9, z, x, F - 1, z, sub);
                    boolean blue = Hash.unit(g.hash(i, k, 9)) < 0.3f;
                    w.set(x, F, z, blue ? (Math.floorMod(z - g.z0(k), 5) == 2 ? lane : mosaic) : deck);
                    w.set(x, W, z, water);
                    if (room == Room.HALL && u % 4 == 2 && v % 4 == 2 && u < wd - 1 && v < dp - 1) {
                        for (int y = W; y < top; y++) w.set(x, y, z, y == W + 3 ? band : tile);
                    }
                }
            }
        }

        boolean skylightAt(CellGrid g, Room room, int i, int k, int u, int v, int wd, int dp) {
            if (room == Room.DARK) return false;
            if (room == Room.TUNNEL) return (u == wd / 2 && v % 3 == 1) || (v == dp / 2 && u % 3 == 1);
            if (room == Room.HALL || room == Room.SLIDE) return u % 4 >= 1 && u % 4 <= 2 && v % 4 >= 1 && v % 4 <= 2;
            return u % 4 == 2 && v % 3 == 1 && u < wd - 1 && v < dp - 1;
        }

        void basinColumn(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int u, int v, Room room) {
            int in = ring(room), wd = g.width(i), dp = g.depth(k);
            int depth = poolDepth(g, i, k);
            int bottom = F - depth + 1;     // lowest water block
            boolean dark = room == Room.DARK;
            BlockState face = dark ? deep : mosaic;
            if (inBasin(g, i, k, u, v)) {
                w.fill(x, F - 10, z, x, bottom - 2, z, sub);
                boolean laneLine = Math.floorMod(v - in, 4) == 2;
                w.set(x, bottom - 1, z, dark ? deep : laneLine ? lane : mosaic);
                w.fill(x, bottom, z, x, W, z, water);
                return;
            }
            // deck ring around the basin; the basin's walls are mosaic underneath
            w.fill(x, F - 10, z, x, F, z, sub);
            boolean edge = u == in - 1 || v == in - 1 || u == wd - in + 1 || v == dp - in + 1;
            if (edge) {
                w.fill(x, bottom - 1, z, x, F, z, face);
                // round lamps set into the pool wall, glowing through the water
                if (!dark && (u + v) % 3 == 0) w.set(x, F - 1, z, poolLight);
                w.set(x, W, z, coping);
            } else {
                w.set(x, W, z, Hash.fbm(s ^ 0x77A, x / 7.0, z / 7.0, 2) > 0.62f ? deckWet : deck);
            }
        }

        /** Arched openings in the walls between rooms, and the rare things set into them. */
        void opening(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int u, int v, boolean alongX, int lowTop) {
            int len = alongX ? g.depth(k) : g.width(i);
            int along = alongX ? v : u;
            long h = g.hash(i, k, alongX ? 40 : 41);
            Room here = room(g, i, k), there = alongX ? room(g, i - 1, k) : room(g, i, k - 1);
            boolean tunnel = here == Room.TUNNEL || there == Room.TUNNEL;
            int width = tunnel ? 2 : 2 + Hash.range(h, Math.max(1, Math.min(5, len - 4)));
            int start = tunnel ? 1 : 1 + Hash.range(h >>> 9, Math.max(1, len - width - 1));
            if (Hash.unit(h >>> 20) < 0.12f && !tunnel) {
                // no way through here. Sometimes a window onto a sky that is not there instead.
                if (Hash.unit(h >>> 30) < 0.18f && along >= start && along < start + Math.min(width, 2)) {
                    w.set(x, W + 2, z, window);
                    w.set(x, W + 3, z, window);
                } else if (Hash.unit(h >>> 30) > 0.93f && along == start + 1) {
                    // a doorway at the wrong height, half above the water
                    w.set(x, W + 1, z, doorway);
                    w.set(x, W + 2, z, doorway);
                }
                return;
            }
            if (tunnel && along > 2 && along < len - 2 && Math.abs(along - len / 2) > 1) return;
            if (!tunnel && (along < start || along >= start + width)) return;
            int open = tunnel ? 2 : Math.min(lowTop - W - 1, 3 + (int) ((h >>> 40) & 1));
            boolean wetHere = !(here == Room.DECK || ((here == Room.POOL || here == Room.DARK || here == Room.SLIDE)));
            boolean wetThere = !(there == Room.DECK || ((there == Room.POOL || there == Room.DARK || there == Room.SLIDE)));
            w.set(x, W, z, wetHere || wetThere ? water : deck);
            for (int y = W + 1; y <= W + open; y++) w.set(x, y, z, Blocks.AIR.defaultBlockState());
            // round off the top corners of wide openings into an arch
            if (!tunnel && width >= 3 && (along == start || along == start + width - 1)) w.set(x, W + open, z, wallAt(s, x, W + open, z));
        }

        void features(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            Room room = room(g, i, k);
            long h = g.hash(i, k, 77);
            float r = Hash.unit(h);
            int cx = x0 + wd / 2, cz = z0 + dp / 2;
            switch (room) {
                case POOL, DARK -> {
                    int in = ring(room), depth = poolDepth(g, i, k);
                    // a chrome ladder down into the basin
                    for (int y = F - depth + 1; y <= W; y++)
                        w.set(x0 + in, y, cz, ladder.setValue(LadderBlock.FACING, Direction.EAST));
                    if (room == Room.DARK) {
                        for (int dx = 0; dx < 2; dx++)
                            for (int dz = 0; dz < 2; dz++) w.set(cx + dx, F - depth, cz + dz, drain);
                    } else if (r < 0.25f) {
                        w.set(cx + 1, W + 1, cz - 1, ringFloat.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    }
                    if (r > 0.6f) w.set(x0 + 1, W + 1, z0 + 2, lounger.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                    if (r > 0.8f) w.set(x0 + 1, W + 1, z0 + 4, lounger.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                }
                case SLIDE -> slide(w, g, i, k, x0, z0, wd, dp);
                case DECK -> {
                    if (r < 0.3f) {
                        for (int n = 0; n < 3; n++)
                            w.set(x0 + 2 + n * 2, W + 1, z0 + 1, lounger.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    } else if (r < 0.42f) {
                        // footprints that walk straight into the wall, and do not come back
                        for (int n = 1; n < wd - 1; n++) w.set(x0 + n, W + 1, cz, prints);
                    } else if (r < 0.5f) {
                        w.set(cx, W + 1, cz, bottle.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    }
                }
                case SHALLOW, HALL -> {
                    if (r < 0.07f) w.set(cx, W + 1, cz, ringFloat.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                }
                default -> {
                }
            }
        }

        /** A tiled tower with a ladder, and a long blue slide down into a deep pool. */
        void slide(ChunkWriter w, CellGrid g, int i, int k, int x0, int z0, int wd, int dp) {
            int depth = poolDepth(g, i, k);
            int topY = W + 6;
            for (int u = 1; u <= 3; u++)
                for (int v = 1; v <= 3; v++) {
                    w.fill(x0 + u, F - depth, z0 + v, x0 + u, topY, z0 + v, tile);
                    w.set(x0 + u, topY, z0 + v, deck);
                }
            for (int y = F - depth + 1; y <= topY; y++) {
                BlockState l = y <= W ? ladder : ladderDry;
                w.set(x0 + 2, y, z0 + 4, l.setValue(LadderBlock.FACING, Direction.SOUTH));
            }
            int len = Math.min(7, wd - 5);
            for (int n = 0; n < len; n++) {
                int x = x0 + 4 + n, y = topY - n;
                if (y <= W) break;
                w.set(x, y, z0 + 2, slide.setValue(StairBlock.FACING, Direction.WEST));
                if (n % 3 == 1) w.fill(x, F - depth, z0 + 2, x, y - 1, z0 + 2, tile);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * The shallows are warm and the warmth gives back what the Backrooms take.
     * The dark pools pull. Swim down far enough and the drain takes you.
     */
    static final class Sublimity implements LevelBehavior {
        private final Map<UUID, Boolean> wasInWater = new HashMap<>();
        private final Map<UUID, Integer> onSlide = new HashMap<>();

        static long seed(ServerLevel level) {
            return Hash.hash(level.getSeed(), Hash.salt(ID));
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            boolean inWater = p.isInWater();
            UUID id = p.getUUID();
            BlockPos feet = p.blockPosition();

            // the slide: remember that we were on it, and splash when we land
            if (level.getBlockState(feet.below()).getBlock() instanceof PoolSlideBlock
                    || level.getBlockState(feet).getBlock() instanceof PoolSlideBlock) onSlide.put(id, 30);
            int slideT = onSlide.getOrDefault(id, 0);
            if (slideT > 0) {
                onSlide.put(id, slideT - 1);
                if (inWater && !wasInWater.getOrDefault(id, false)) {
                    PoolSlideBlock.splash(level, feet);
                    level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.8, p.getZ(), 40, 0.6, 0.2, 0.6, 0.2);
                    onSlide.put(id, 0);
                }
            }
            wasInWater.put(id, inWater);

            // warmth: shallow water soothes
            if (inWater && !p.isUnderWater() && p.tickCount % 20 == 0) Sanity.add(p, 0.14f);

            // undertow over the dark pools
            if (inWater && p.getY() < W + 0.6 && !p.isCreative()) {
                CellGrid g = grid(seed(level));
                int i = g.cellX(feet.getX()), k = g.cellZ(feet.getZ());
                if (room(g, i, k) == Room.DARK && inBasin(g, i, k, feet.getX() - g.x0(i), feet.getZ() - g.z0(k))) {
                    if (p.tickCount % 3 == 0) {
                        Vec3 v = p.getDeltaMovement();
                        double cx = g.x0(i) + g.width(i) / 2.0 + 1, cz = g.z0(k) + g.depth(k) / 2.0 + 1;
                        double dx = Math.signum(cx - p.getX()) * 0.02, dz = Math.signum(cz - p.getZ()) * 0.02;
                        p.setDeltaMovement(v.x + dx, Math.max(v.y - 0.075, -0.35), v.z + dz);
                        p.hurtMarked = true;
                    }
                    if (p.tickCount % 50 == 0) {
                        level.playSound(null, feet, ModSounds.get("event.level_37_undertow"), SoundSource.AMBIENT, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.3f);
                        level.sendParticles(ParticleTypes.BUBBLE, p.getX(), p.getY() - 1.5, p.getZ(), 12, 0.6, 0.6, 0.6, 0.05);
                    }
                }
            }

            // something breathes at the bottom of a pool you are not looking at
            if (p.tickCount % 400 == 0 && p.getRandom().nextFloat() < 0.4f) {
                for (int attempt = 0; attempt < 12; attempt++) {
                    double ang = p.getRandom().nextDouble() * Math.PI * 2;
                    double d = 7 + p.getRandom().nextDouble() * 9;
                    BlockPos at = BlockPos.containing(p.getX() + Math.cos(ang) * d, W - 2, p.getZ() + Math.sin(ang) * d);
                    if (!level.getBlockState(at).is(Blocks.WATER) || !level.getBlockState(at.above(2)).is(Blocks.WATER)) continue;
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, at.getX() + 0.5, at.getY() - 1, at.getZ() + 0.5, 30, 0.25, 0.4, 0.25, 0.05);
                    level.sendParticles(ParticleTypes.BUBBLE_POP, at.getX() + 0.5, W + 0.95, at.getZ() + 0.5, 14, 0.3, 0.0, 0.3, 0.02);
                    level.playSound(null, at, ModSounds.get("event.level_37_undertow"), SoundSource.AMBIENT, 0.5f, 0.7f);
                    break;
                }
            }
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            wasInWater.put(p.getUUID(), true);
        }
    }
}
