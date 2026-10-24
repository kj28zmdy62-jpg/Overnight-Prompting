package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sky Rooms. Fragments of the Lobby hang in clear air above a floor of cloud: a room
 * with three walls, a carpet with no room, a light still buzzing with nothing around it.
 * One-block strips of corridor join them. The wind comes in gusts; crouch when you hear
 * it rise. Some of the floor will not hold you. Falling is a way out, into somewhere
 * worse.
 */
public final class SkyRooms {
    static final String ID = "sky_rooms";
    static final int BASE = 206;
    static final int FALL_Y = 172;

    private SkyRooms() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Sky Rooms").order(3130)
                .tagline("Pieces of the Lobby, high above the clouds. Mind the gap.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Wind())
                .visuals(new Visuals().tint(0.98f, 1.0f, 1.06f).saturation(0.95f).contrast(1.05f).brightness(1.12f)
                        .grain(0.04f).aberration(0.001f).vignette(0.38f).warp(0.012f).scanlines(0.02f)
                        .fog(40f, 150f).fogColor(0xBCD6EE).bloom(0.5f).sky().clouds())
                .sanityDrain(0.035f)
                .spawnY(BASE - 4, BASE + 4)
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.25f).dist(14, 30).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(10, 24).weight(2))
                .exit(ExitType.FALL, "the_abyss", 2).exit(ExitType.FALL, "level_6", 2).exit(ExitType.FALL, "level_0", 1)
                .exit(ExitType.DOOR, "level_0", 3).exit(ExitType.DOOR, "level_1", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "rooftops", 1).exit(ExitType.NOCLIP, "the_abyss", 1)
                .build();
    }

    enum Kind { OPEN, ROOM, DARK, DOORWAY, LAMP }

    record Island(int cx, int cz, int hx, int hz, int fy, Kind kind, long h) {
        boolean inside(int x, int z) {
            return Math.abs(x - cx) <= hx && Math.abs(z - cz) <= hz;
        }

        int edge(int x, int z) {
            return Math.min(hx - Math.abs(x - cx), hz - Math.abs(z - cz));
        }
    }

    static CellGrid grid(long s) {
        return new CellGrid(s ^ 0x5C1L, 26, 8);
    }

    static Island island(CellGrid g, int i, int k) {
        long h = g.hash(i, k, 1);
        if (Hash.unit(h) > 0.8f) return null;
        int cx = g.x0(i) + g.width(i) / 2 + Hash.range(h >>> 4, 5) - 2;
        int cz = g.z0(k) + g.depth(k) / 2 + Hash.range(h >>> 8, 5) - 2;
        int hx = 3 + Hash.range(h >>> 12, 4), hz = 3 + Hash.range(h >>> 16, 4);
        int fy = BASE + 3 * (Hash.range(h >>> 20, 3) - 1);
        float r = Hash.unit(h >>> 24 ^ h);
        Kind kind = r < 0.04f ? Kind.LAMP : r < 0.08f ? Kind.DOORWAY : r < 0.3f ? Kind.ROOM : r < 0.42f ? Kind.DARK : Kind.OPEN;
        return new Island(cx, cz, hx, hz, fy, kind, h);
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("moist_carpet");
        final BlockState damp = ModBlocks.state("damp_carpet");
        final BlockState crumble = ModBlocks.state("sr_crumbling_carpet");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState torn = ModBlocks.state("sr_torn_concrete");
        final BlockState upper = ModBlocks.state("sr_upper_slab");
        final BlockState paper = ModBlocks.state("yellow_wallpaper");
        final BlockState stained = ModBlocks.state("yellow_wallpaper_stained");
        final BlockState base = ModBlocks.state("yellow_wallpaper_baseboard");
        final BlockState peeling = ModBlocks.state("yellow_wallpaper_peeling");
        final BlockState ceiling = ModBlocks.state("ceiling_tile");
        final BlockState missing = ModBlocks.state("missing_ceiling_tile");
        final BlockState light = ModBlocks.state("fluorescent_light");
        final BlockState flicker = ModBlocks.state("flickering_fluorescent_light");
        final BlockState dead = ModBlocks.state("dead_fluorescent_light");
        final BlockState doorway = ModBlocks.state("dark_doorway");
        final BlockState wires = ModBlocks.state("sr_dangling_wires");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                Island a = island(g, i, k);
                if (a == null) return;
                if (a.kind == Kind.LAMP) {
                    lamp(w, s, a);
                    return;
                }
                draw(w, s, a);
                Island east = island(g, i + 1, k), south = island(g, i, k + 1);
                if (east != null && east.kind != Kind.LAMP && Hash.unit(a.h, 5, 1) < 0.9f) walkway(w, s, a, east, true);
                if (south != null && south.kind != Kind.LAMP && Hash.unit(a.h, 5, 2) < 0.9f) walkway(w, s, a, south, false);
            });
        }

        boolean present(Island a, int x, int z) {
            if (!a.inside(x, z)) return false;
            int e = a.edge(x, z);
            if (e > 0) return true;
            // ragged, broken-off edges (but the middle of each side, where corridors meet, holds)
            if (Math.abs(x - a.cx) <= 1 || Math.abs(z - a.cz) <= 1) return true;
            return Hash.unit(a.h, x, z) > 0.4f;
        }

        void draw(ChunkWriter w, long s, Island a) {
            boolean walled = a.kind == Kind.ROOM || a.kind == Kind.DARK;
            for (int x = a.cx - a.hx; x <= a.cx + a.hx; x++)
                for (int z = a.cz - a.hz; z <= a.cz + a.hz; z++) {
                    if (!w.contains(x, z) || !present(a, x, z)) continue;
                    int e = a.edge(x, z);
                    boolean wet = Hash.fbm(s ^ 0x77, x / 10.0, z / 10.0, 2) > 0.62f;
                    w.set(x, a.fy, z, Hash.unit(s, x, z, 41) < 0.025f && e > 0 ? crumble : wet ? damp : carpet);
                    w.set(x, a.fy - 1, z, sub);
                    // the torn underside hangs lower toward the middle
                    int hang = 1 + (int) (Hash.noise(s ^ 0x4A, x / 3.0, z / 3.0) * Math.min(6, e * 2 + 1));
                    w.fill(x, a.fy - 1 - hang, z, x, a.fy - 2, z, torn);
                    boolean perim = e == 0;
                    boolean opening = Math.abs(x - a.cx) <= 1 || Math.abs(z - a.cz) <= 1;
                    if (perim && !opening) {
                        int top = walled ? 4 : (int) (Hash.noise(s ^ 0x3A11, x / 2.5, z / 2.5) * 6) - 1;
                        for (int y = 1; y <= Math.min(4, top); y++) w.set(x, a.fy + y, z, wall(s, x, a.fy + y, z, y));
                    }
                    if (walled) {
                        boolean lamp = Math.floorMod(x - a.cx, 4) == 0 && Math.floorMod(z - a.cz, 4) == 2;
                        BlockState c = lamp ? (a.kind == Kind.DARK ? dead : Hash.unit(s, x, z, 9) < 0.2f ? flicker : light)
                                : Hash.unit(s, x, z, 10) < 0.05f ? missing : ceiling;
                        if (perim && Hash.unit(s, x, z, 11) < 0.3f) {
                            w.set(x, a.fy + 4, z, wires);
                        } else {
                            w.set(x, a.fy + 5, z, c);
                            w.set(x, a.fy + 6, z, upper);
                        }
                    }
                }
            // props
            long h = a.h;
            float r = Hash.unit(h, 7, 7);
            if (r < 0.15f) w.set(a.cx + 1, a.fy + 1, a.cz - 1, OutsideKit.facing("office_chair", Direction.from2DDataValue(Hash.range(h, 7, 8, 4))));
            else if (r < 0.22f) w.set(a.cx - 1, a.fy + 1, a.cz + 1, OutsideKit.facing("almond_water_bottle", Direction.NORTH));
            if (a.kind == Kind.DOORWAY) {
                // a piece of wall at the edge of the island with a doorway in it, and nothing on the far side
                int x = a.cx + a.hx;
                for (int dz = -1; dz <= 1; dz++)
                    for (int y = 1; y <= 4; y++) w.set(x, a.fy + y, a.cz + 3 + dz, y == 1 ? base : paper);
                w.set(x, a.fy + 1, a.cz + 3, doorway);
                w.set(x, a.fy + 2, a.cz + 3, doorway);
            }
        }

        BlockState wall(long s, int x, int y, int z, int dy) {
            if (dy == 1) return base;
            if (Hash.unit(s, x, y, z) < 0.04f) return peeling;
            return Hash.fbm(s ^ 0x5151, x / 9.0 + y * 0.3, z / 9.0, 2) > 0.63f ? stained : paper;
        }

        /** A single ceiling panel with its light still on, floating by itself. */
        void lamp(ChunkWriter w, long s, Island a) {
            int y = a.fy + 4;
            for (int dx = -1; dx <= 2; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int x = a.cx + dx, z = a.cz + dz;
                    w.set(x, y, z, (dx == 0 || dx == 1) && dz == 0 ? light : ceiling);
                    w.set(x, y + 1, z, upper);
                }
            w.set(a.cx - 1, y - 1, a.cz + 1, wires);
        }

        /** A Z-shaped strip of corridor floor, one block wide, from one island to the next. */
        void walkway(ChunkWriter w, long s, Island a, Island b, boolean alongX) {
            List<int[]> path = new ArrayList<>();
            if (alongX) {
                int xs = a.cx + a.hx + 1, xe = b.cx - b.hx - 1;
                if (xe < xs) return;
                int mid = (xs + xe) / 2;
                for (int x = xs; x <= mid; x++) path.add(new int[]{x, a.cz});
                int dz = Integer.signum(b.cz - a.cz);
                for (int z = a.cz + dz; dz != 0 && z != b.cz + dz; z += dz) path.add(new int[]{mid, z});
                for (int x = mid + 1; x <= xe; x++) path.add(new int[]{x, b.cz});
            } else {
                int zs = a.cz + a.hz + 1, ze = b.cz - b.hz - 1;
                if (ze < zs) return;
                int mid = (zs + ze) / 2;
                for (int z = zs; z <= mid; z++) path.add(new int[]{a.cx, z});
                int dx = Integer.signum(b.cx - a.cx);
                for (int x = a.cx + dx; dx != 0 && x != b.cx + dx; x += dx) path.add(new int[]{x, mid});
                for (int z = mid + 1; z <= ze; z++) path.add(new int[]{b.cx, z});
            }
            int dy = b.fy - a.fy;
            int n = path.size();
            if (n < Math.abs(dy) + 1) return;
            int y = a.fy;
            long h = Hash.hash(a.h, b.h);
            int wallFrom = Hash.range(h, Math.max(1, n)), wallLen = Hash.unit(h, 3, 3) < 0.35f ? 3 + Hash.range(h, 4, 4, 4) : 0;
            for (int idx = 0; idx < n; idx++) {
                int[] p = path.get(idx);
                int[] next = idx + 1 < n ? path.get(idx + 1) : new int[]{p[0] + (alongX ? 1 : 0), p[1] + (alongX ? 0 : 1)};
                Direction travel = Direction.fromDelta(Integer.signum(next[0] - p[0]), 0, Integer.signum(next[1] - p[1]));
                if (travel == null) travel = alongX ? Direction.EAST : Direction.SOUTH;
                int remaining = n - idx;
                boolean step = remaining <= Math.abs(dy) && y != b.fy;
                if (step) {
                    int ny = y + Integer.signum(dy);
                    BlockState st = ModBlocks.state("concrete_stairs").setValue(StairBlock.FACING, dy > 0 ? travel : travel.getOpposite());
                    int sy = dy > 0 ? ny : y;
                    w.set(p[0], sy, p[1], st);
                    w.set(p[0], sy - 1, p[1], sub);
                    y = ny;
                } else {
                    boolean weak = Hash.unit(s, p[0], p[1], 43) < 0.07f && idx > 1 && remaining > 2;
                    w.set(p[0], y, p[1], weak ? crumble : carpet);
                    w.set(p[0], y - 1, p[1], sub);
                    if (Hash.unit(s, p[0], p[1], 44) < 0.4f) w.set(p[0], y - 2, p[1], torn);
                    if (wallLen > 0 && idx >= wallFrom && idx < wallFrom + wallLen) {
                        Direction side = travel.getClockWise();
                        int top = 1 + Hash.range(s, p[0], p[1], 3);
                        for (int yy = 1; yy <= top; yy++)
                            w.set(p[0] + side.getStepX(), y + yy, p[1] + side.getStepZ(), yy == 1 ? base : paper);
                        w.set(p[0] + side.getStepX(), y, p[1] + side.getStepZ(), sub);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Wind implements LevelBehavior {
        static final class St {
            int next = 600, gust;
            double dx, dz;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();
        private final Map<BlockPos, Long> cracking = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.next = 500 + p.getRandom().nextInt(700);
            states.put(p.getUUID(), st);
            if (OutsideKit.autotest()) {
                long s = OutsideKit.seed(level, ID);
                CellGrid g = grid(s);
                int ci = g.cellX(p.getBlockX()), ck = g.cellZ(p.getBlockZ());
                for (int r = 0; r < 4; r++)
                    for (int di = -r; di <= r; di++)
                        for (int dk = -r; dk <= r; dk++) {
                            Island a = island(g, ci + di, ck + dk);
                            if (a == null || a.kind == Kind.LAMP || island(g, ci + di + 1, ck + dk) == null) continue;
                            level.getChunk(new BlockPos(a.cx, a.fy, a.cz));
                            p.teleportTo(level, a.cx + a.hx + 0.5, a.fy + 1, a.cz + 0.5, 270f, 0f);
                            return;
                        }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.getY() < FALL_Y && !p.isCreative() && !p.isSpectator()) {
                Travel.exit(p, ExitType.FALL, p.blockPosition());
                return;
            }
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            // crumbling floor: it holds for a moment after you step on it
            if (p.onGround()) {
                BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
                if (level.getBlockState(below).is(ModBlocks.get("sr_crumbling_carpet")) && !cracking.containsKey(below)) {
                    cracking.put(below.immutable(), level.getGameTime() + 14);
                    OutsideKit.sound(level, below, "event.sky_rooms_crack", 1f, 1f);
                }
            }
            if (p.tickCount % 2 == 0 && !cracking.isEmpty()) {
                long now = level.getGameTime();
                cracking.entrySet().removeIf(e -> {
                    if (now < e.getValue()) return false;
                    BlockPos pos = e.getKey();
                    if (level.isLoaded(pos)) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)),
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.05);
                        for (int y = 0; y >= -3; y--) {
                            BlockPos q = pos.offset(0, y, 0);
                            if (!level.getBlockState(q).isAir() && !level.getBlockState(q).is(Blocks.LIGHT)) level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    return true;
                });
            }
            // gusts
            if (st.gust > 0) {
                st.gust--;
                if (!p.isShiftKeyDown() && !p.isCreative() && !p.isSpectator()) {
                    float k = st.gust > 40 ? 0.035f : 0.055f;
                    p.push(st.dx * k, 0, st.dz * k);
                    p.hurtMarked = true;
                }
                return;
            }
            if (--st.next == 60) {
                OutsideKit.sound(level, p.blockPosition(), "event.sky_rooms_gust", 1.2f, 0.9f + p.getRandom().nextFloat() * 0.2f);
            }
            if (st.next <= 0) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2;
                st.dx = Math.cos(ang);
                st.dz = Math.sin(ang);
                st.gust = 60;
                st.next = 700 + p.getRandom().nextInt(900);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.25f));
            }
        }
    }
}
