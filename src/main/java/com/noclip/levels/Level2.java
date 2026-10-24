package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level 2, "Pipe Dreams". Narrow maintenance tunnels, two or three blocks wide,
 * lined with pipes and lit by caged bulbs. It is hot and it gets hotter. The
 * pipes knock before they burst: when you hear them, get down.
 */
public final class Level2 {
    static final int FLOOR = 6, TOP = FLOOR + 3, LOW_TOP = FLOOR + 2, CH_TOP = FLOOR + 5, ROCK = FLOOR + 7;

    private Level2() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_2")
                .number("2").title("Pipe Dreams").order(20)
                .tagline("Hot, narrow, and full of pipes. When they knock, get down.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Pressure())
                .visuals(new Visuals().tint(1.12f, 0.96f, 0.82f).saturation(0.82f).contrast(1.12f).brightness(1.3f)
                        .grain(0.09f).aberration(0.0018f).vignette(0.62f).warp(0.035f).fog(3f, 34f).fogColor(0x3A2A1E)
                        .bloom(0.55f).flicker(0.03f))
                .sanityDrain(0.04f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.18f).dist(20, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.45f).dark().dist(10, 24).weight(3))
                .exit(ExitType.NOCLIP, "level_3", 5).exit(ExitType.NOCLIP, "level_1", 1).exit(ExitType.NOCLIP, "level_4", 1)
                .exit(ExitType.DOOR, "level_3", 4).exit(ExitType.DOOR, "level_4", 1)
                .exit(ExitType.FALL, "level_5", 2).exit(ExitType.FALL, "boiler_room", 1)
                .exit(ExitType.ASCEND, "level_1", 2).exit(ExitType.ASCEND, "level_4", 1)
                .build();
    }

    // ---------------------------------------------------------------- topology

    /**
     * Tunnels run along jittered grid lines. Each grid node may connect to its
     * neighbours; some nodes swell into valve chambers, some sprout dead-end stubs.
     */
    static final class Net {
        final CellGrid g;
        final long s;

        Net(long s) {
            this.s = s;
            this.g = new CellGrid(s, 12, 4);
        }

        boolean segX(int i, int k) { return g.unit(i, k, 1) < 0.64f; }
        boolean segZ(int i, int k) { return g.unit(i, k, 2) < 0.64f; }
        boolean seg(int axis, int i, int k) { return axis == 0 ? segX(i, k) : segZ(i, k); }

        boolean node(int i, int k) {
            return segX(i, k) || segX(i - 1, k) || segZ(i, k) || segZ(i, k - 1);
        }

        boolean wide(int axis, int i, int k) { return g.unit(i, k, 3 + axis) < 0.45f; }
        boolean low(int axis, int i, int k) { return g.unit(i, k, 5 + axis) < 0.15f; }
        boolean chamber(int i, int k) { return node(i, k) && g.unit(i, k, 7) < 0.06f; }
        boolean frost(int axis, int i, int k) { return g.unit(i, k, 10 + axis) < 0.006f; }
        boolean leak(int axis, int i, int k) { return g.unit(i, k, 12 + axis) < 0.06f; }
        boolean dark(int axis, int i, int k) { return g.unit(i, k, 14 + axis) < 0.12f; }

        int lo(int axis, int i, int k) { return wide(axis, i, k) ? -1 : 0; }

        /** Stub direction 0:+x 1:-x 2:+z 3:-z, or -1. */
        int stub(int i, int k) {
            if (!node(i, k) || chamber(i, k) || g.unit(i, k, 8) >= 0.17f) return -1;
            int d = Hash.range(g.hash(i, k, 9), 4);
            boolean taken = switch (d) {
                case 0 -> segX(i, k);
                case 1 -> segX(i - 1, k);
                case 2 -> segZ(i, k);
                default -> segZ(i, k - 1);
            };
            return taken ? -1 : d;
        }

        static int sdx(int d) { return d == 0 ? 1 : d == 1 ? -1 : 0; }
        static int sdz(int d) { return d == 2 ? 1 : d == 3 ? -1 : 0; }

        boolean hot(int x, int z) {
            return Hash.fbm(s ^ 0x407L, x / 48.0, z / 48.0, 2) > 0.6f;
        }

        /** Highest air block of the column, or -1 if the column is solid rock. */
        int top(int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int best = -1;
            for (int ii = i; ii <= i + 1; ii++) {
                int dx = x - g.x0(ii);
                if (dx < -7 || dx > 7) continue;
                for (int kk = k; kk <= k + 1; kk++) {
                    int dz = z - g.z0(kk);
                    if (dz < -7 || dz > 7) continue;
                    if (!node(ii, kk)) continue;
                    boolean ch = chamber(ii, kk);
                    int r = ch ? 3 : 1;
                    if (Math.abs(dx) <= r && Math.abs(dz) <= r) best = Math.max(best, ch ? CH_TOP : TOP);
                    int sd = stub(ii, kk);
                    if (sd >= 0) {
                        int along = sd < 2 ? dx * Net.sdx(sd) : dz * Net.sdz(sd);
                        int across = sd < 2 ? dz : dx;
                        if (along >= 2 && along <= 6 && across >= 0 && across <= 1) best = Math.max(best, TOP);
                    }
                }
                // tunnel along z at line ii, between rows k and k+1
                if (segZ(ii, k) && z >= g.z0(k) && z <= g.z0(k + 1)) {
                    int lo = lo(1, ii, k);
                    if (dx >= lo && dx <= 1) best = Math.max(best, low(1, ii, k) ? LOW_TOP : TOP);
                }
            }
            for (int kk = k; kk <= k + 1; kk++) {
                int dz = z - g.z0(kk);
                if (segX(i, kk) && x >= g.x0(i) && x <= g.x0(i + 1)) {
                    int lo = lo(0, i, kk);
                    if (dz >= lo && dz <= 1) best = Math.max(best, low(0, i, kk) ? LOW_TOP : TOP);
                }
            }
            return best;
        }
    }

    // ---------------------------------------------------------------- generator

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("pipe_tunnel_wall");
        final BlockState lined = ModBlocks.state("pipe_tunnel_wall_lined");
        final BlockState risers = ModBlocks.state("pipe_tunnel_wall_risers");
        final BlockState base = ModBlocks.state("pipe_tunnel_wall_base");
        final BlockState scorched = ModBlocks.state("pipe_tunnel_wall_scorched");
        final BlockState frostWall = ModBlocks.state("pipe_tunnel_wall_frost");
        final BlockState[] graffiti = {ModBlocks.state("pipe_tunnel_graffiti_knock"), ModBlocks.state("pipe_tunnel_graffiti_hot"),
                ModBlocks.state("pipe_tunnel_graffiti_arrow")};
        final BlockState ceiling = ModBlocks.state("pipe_tunnel_ceiling");
        final BlockState floor = ModBlocks.state("pipe_tunnel_floor");
        final BlockState wet = ModBlocks.state("pipe_tunnel_floor_wet");
        final BlockState frostFloor = ModBlocks.state("pipe_tunnel_floor_frost");
        final BlockState grate = ModBlocks.state("pipe_tunnel_grate");
        final BlockState run = ModBlocks.state("pipe_run");
        final BlockState runHot = ModBlocks.state("pipe_run_hot");
        final BlockState runFrost = ModBlocks.state("pipe_run_frost");
        final BlockState runLow = ModBlocks.state("pipe_run_low");
        final BlockState runBroken = ModBlocks.state("pipe_run_broken");
        final BlockState crossLow = ModBlocks.state("pipe_cross_low");
        final BlockState bulb = ModBlocks.state("caged_bulb");
        final BlockState bulbDead = ModBlocks.state("caged_bulb_dead");
        final BlockState bulbCold = ModBlocks.state("caged_bulb_cold");
        final BlockState valve = ModBlocks.state("pressure_valve");
        final BlockState gauge = ModBlocks.state("pressure_gauge");
        final BlockState riser = ModBlocks.state("scalding_riser");
        final BlockState vent = ModBlocks.state("steam_vent_grate");
        final BlockState door = ModBlocks.state("maintenance_door");
        final BlockState shaft = ModBlocks.state("pipe_shaft_glow");
        final BlockState hatch = ModBlocks.state("ceiling_hatch");
        final BlockState ladder = ModBlocks.state("pipe_tunnel_ladder");
        final BlockState bedroll = ModBlocks.state("wanderer_bedroll");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            Net n = new Net(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, ROCK + 1, w.minZ, w.maxX, ROCK + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int t = n.top(x, z);
                    if (t < 0) {
                        boolean face = n.top(x + 1, z) >= 0 || n.top(x - 1, z) >= 0 || n.top(x, z + 1) >= 0 || n.top(x, z - 1) >= 0;
                        w.set(x, FLOOR, z, wall);
                        for (int y = FLOOR + 1; y <= ROCK; y++) w.set(x, y, z, face ? wallFace(n, s, x, y, z) : wall);
                    } else {
                        float r = Hash.unit(s, x, z, 21);
                        w.set(x, FLOOR, z, r < 0.1f ? grate : floor);
                        w.set(x, t + 1, z, ceiling);
                        for (int y = t + 2; y <= ROCK; y++) w.set(x, y, z, wall);
                    }
                }
            }
            n.g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (n.segX(i, k)) segment(w, n, s, 0, i, k);
                if (n.segZ(i, k)) segment(w, n, s, 1, i, k);
                if (n.chamber(i, k)) chamber(w, n, s, i, k);
                int sd = n.stub(i, k);
                if (sd >= 0) stubEnd(w, n, s, i, k, sd);
            });
        }

        BlockState wallFace(Net n, long s, int x, int y, int z) {
            if (y == FLOOR + 1) return base;
            if (y == FLOOR + 2) {
                if (n.hot(x, z)) return scorched;
                return Hash.chance(s, x, z, 31, 0.22f) ? risers : lined;
            }
            if (y == FLOOR + 3 && Hash.chance(s, x, z, 32, 0.3f)) return risers;
            return wall;
        }

        /** Map (along, across) on a segment to world x/z. axis 0 = runs along x. */
        static int wx(int axis, int line, int along, int across) { return axis == 0 ? along : line + across; }
        static int wz(int axis, int line, int along, int across) { return axis == 0 ? line + across : along; }

        void segment(ChunkWriter w, Net n, long s, int axis, int i, int k) {
            int line = axis == 0 ? n.g.z0(k) : n.g.x0(i);
            int a0 = axis == 0 ? n.g.x0(i) : n.g.z0(k);
            int a1 = axis == 0 ? n.g.x0(i + 1) : n.g.z0(k + 1);
            int ni = axis == 0 ? i + 1 : i, nk = axis == 0 ? k : k + 1;
            int start = a0 + (n.chamber(i, k) ? 4 : 2), end = a1 - (n.chamber(ni, nk) ? 4 : 2);
            int lo = n.lo(axis, i, k), hi = 1;
            boolean low = n.low(axis, i, k), frost = n.frost(axis, i, k), leak = n.leak(axis, i, k), dark = n.dark(axis, i, k);
            Direction loWall = axis == 0 ? Direction.NORTH : Direction.WEST;
            Direction hiWall = loWall.getOpposite();
            long h = n.g.hash(i, k, 40 + axis);
            int top = low ? LOW_TOP : TOP;
            int len = end - start;
            boolean hot = n.hot(wx(axis, line, (start + end) / 2, 0), wz(axis, line, (start + end) / 2, 0));

            for (int a = start; a <= end; a++) {
                // pipe runs along both walls, just under the ceiling
                if (!low) {
                    BlockState r = frost ? runFrost : (hot && Hash.chance(s, a, line, 51, 0.5f) ? runHot : run);
                    w.set(wx(axis, line, a, lo), TOP, wz(axis, line, a, lo), ClassicsKit.facing(r, loWall));
                    w.set(wx(axis, line, a, hi), TOP, wz(axis, line, a, hi), ClassicsKit.facing(r, hiWall));
                }
                if (frost) {
                    for (int c = lo; c <= hi; c++) w.set(wx(axis, line, a, c), FLOOR, wz(axis, line, a, c), frostFloor);
                    for (int y = FLOOR + 1; y <= top; y++) {
                        w.set(wx(axis, line, a, lo - 1), y, wz(axis, line, a, lo - 1), frostWall);
                        w.set(wx(axis, line, a, hi + 1), y, wz(axis, line, a, hi + 1), frostWall);
                    }
                }
                if (leak) {
                    for (int c = lo; c <= hi; c++) {
                        w.set(wx(axis, line, a, c), FLOOR, wz(axis, line, a, c), wet);
                        if (Hash.chance(s, a, line + c, 52, 0.35f)) w.set(wx(axis, line, a, c), FLOOR + 1, wz(axis, line, a, c), puddle);
                    }
                }
                // caged bulbs every six blocks down the middle
                if (Math.floorMod(a - a0, 5) == 2) {
                    BlockState b = frost ? bulbCold : dark ? bulbDead : (Hash.chance(s, a, line, 53, 0.08f) ? bulbDead : bulb);
                    w.set(wx(axis, line, a, 0), top, wz(axis, line, a, 0), ClassicsKit.facing(b, loWall));
                }
            }
            if (len < 6) return;
            // a burst pipe dripping into the tunnel
            if (leak && !low) {
                int a = start + 2 + Hash.range(h >>> 3, len - 3);
                w.set(wx(axis, line, a, lo), TOP, wz(axis, line, a, lo), ClassicsKit.facing(runBroken, loWall));
            }
            // low pipes along the floor on one side
            if (!low && !frost && Hash.unit(h >>> 5) < 0.3f) {
                int c = (h & 1) == 0 ? lo : hi;
                Direction d = c == lo ? loWall : hiWall;
                int from = start + Hash.range(h >>> 9, Math.max(1, len / 2));
                int to = Math.min(end, from + 3 + Hash.range(h >>> 13, 6));
                for (int a = from; a <= to; a++) w.set(wx(axis, line, a, c), FLOOR + 1, wz(axis, line, a, c), ClassicsKit.facing(runLow, d));
            }
            // a pipe crossing at chest height: crouch under it
            if (!low && !frost && Hash.unit(h >>> 49) < 0.3f) {
                int a = start + 1 + Hash.range(h >>> 51, Math.max(1, len - 2));
                Direction across = axis == 0 ? Direction.EAST : Direction.NORTH;
                for (int c = lo; c <= hi; c++) {
                    int x = wx(axis, line, a, c), z = wz(axis, line, a, c);
                    if (w.get(x, FLOOR + 1, z).isAir()) w.set(x, FLOOR + 2, z, ClassicsKit.facing(crossLow, across));
                }
            }
            // scalding risers narrow the wide tunnels (more of them where it is hot)
            if (lo == -1 && !low && !frost && Hash.unit(h >>> 17) < (hot ? 0.75f : 0.25f)) {
                int count = hot ? 2 : 1;
                for (int q = 0; q < count; q++) {
                    long hq = Hash.hash(h, q);
                    int a = start + 1 + Hash.range(hq, Math.max(1, len - 2));
                    int c = (hq & 2) == 0 ? lo : hi;
                    for (int y = FLOOR + 1; y <= TOP; y++) w.set(wx(axis, line, a, c), y, wz(axis, line, a, c), riser);
                }
            }
            // steam vents in the floor where it is hot
            if (hot && !frost && Hash.unit(h >>> 21) < 0.55f) {
                int a = start + 1 + Hash.range(h >>> 25, Math.max(1, len - 2));
                int c = lo + Hash.range(h >>> 29, hi - lo + 1);
                w.set(wx(axis, line, a, c), FLOOR, wz(axis, line, a, c), vent);
            }
            // a warning someone left on the wall
            if (Hash.unit(h >>> 33) < 0.05f) {
                int a = start + Hash.range(h >>> 37, Math.max(1, len));
                int c = (h & 4) == 0 ? lo - 1 : hi + 1;
                w.set(wx(axis, line, a, c), FLOOR + 2, wz(axis, line, a, c), graffiti[Hash.range(h >>> 41, 3)]);
            }
            // a missing floor grate: heat and orange light far below
            if (lo == -1 && !low && !frost && Hash.unit(h >>> 45) < 0.018f) {
                int a = start + len / 2;
                for (int da = 0; da <= 1; da++)
                    for (int c = -1; c <= 0; c++) {
                        int x = wx(axis, line, a + da, c), z = wz(axis, line, a + da, c);
                        for (int y = FLOOR - 4; y <= FLOOR; y++) w.set(x, y, z, y <= FLOOR - 3 ? shaft : ModBlocks.AIR);
                    }
            }
        }

        void chamber(ChunkWriter w, Net n, long s, int i, int k) {
            int cx = n.g.x0(i), cz = n.g.z0(k);
            long h = n.g.hash(i, k, 60);
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) w.set(cx + dx, FLOOR, cz + dz, grate);
            // scalding risers in the corners, up to the ceiling
            for (int sx = -3; sx <= 3; sx += 6)
                for (int sz = -3; sz <= 3; sz += 6)
                    for (int y = FLOOR + 1; y <= CH_TOP; y++) w.set(cx + sx, y, cz + sz, riser);
            // valves and gauges on the walls between the corners
            int[][] spots = {{-2, -3, 2}, {2, -3, 2}, {-2, 3, 3}, {2, 3, 3}, {-3, -2, 1}, {-3, 2, 1}, {3, -2, 0}, {3, 2, 0}};
            for (int q = 0; q < spots.length; q++) {
                int x = cx + spots[q][0], z = cz + spots[q][1];
                Direction d = switch (spots[q][2]) {
                    case 0 -> Direction.EAST;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.NORTH;
                    default -> Direction.SOUTH;
                };
                if (n.top(x + d.getStepX(), z + d.getStepZ()) >= 0) continue;
                float r = Hash.unit(Hash.hash(h, q));
                if (r < 0.35f) w.set(x, FLOOR + 2, z, ClassicsKit.facing(valve, d));
                else if (r < 0.75f) w.set(x, FLOOR + 2, z, ClassicsKit.facing(gauge, d));
                if (Hash.unit(Hash.hash(h, q + 20)) < 0.5f) w.set(x, FLOOR + 3, z, ClassicsKit.facing(gauge, d));
                w.set(x, FLOOR + 4, z, ClassicsKit.facing(run, d));
            }
            w.set(cx, CH_TOP, cz, bulb);
            w.set(cx + 1 - Hash.range(h >>> 7, 3), FLOOR, cz + 1 - Hash.range(h >>> 9, 3), vent);
            if (Hash.unit(h >>> 11) < 0.5f) w.set(cx - 1 + Hash.range(h >>> 13, 3), FLOOR, cz + 2, vent);
        }

        void stubEnd(ChunkWriter w, Net n, long s, int i, int k, int d) {
            int cx = n.g.x0(i), cz = n.g.z0(k);
            int ax = Net.sdx(d), az = Net.sdz(d);
            // across axis: +z for x stubs, +x for z stubs
            int cxs = d < 2 ? 0 : 1, czs = d < 2 ? 1 : 0;
            Direction out = ClassicsKit.dir(ax, az);
            int ex = cx + ax * 6, ez = cz + az * 6;       // last open column (across = 0)
            int wxp = cx + ax * 7, wzp = cz + az * 7;     // the end wall
            long h = n.g.hash(i, k, 70);
            float r = Hash.unit(h);
            if (r < 0.25f) {
                w.set(wxp, FLOOR + 1, wzp, door);
                w.set(wxp, FLOOR + 2, wzp, door);
                w.set(ex, TOP, ez, ClassicsKit.facing(bulb, out));
            } else if (r < 0.45f) {
                w.set(ex - ax, FLOOR + 1, ez - az, bedroll);
                w.set(ex - 2 * ax, FLOOR + 1, ez - 2 * az, bedroll);
                w.set(ex + cxs, FLOOR + 1, ez + czs, ClassicsKit.facing(supply, out));
                w.set(ex - ax + cxs, FLOOR + 1, ez - az + czs, ClassicsKit.facing(bottle, out));
                w.set(wxp, FLOOR + 2, wzp, graffiti[1]);
                w.set(ex, TOP, ez, ClassicsKit.facing(bulb, out));
            } else if (r < 0.62f) {
                for (int y = FLOOR + 1; y <= TOP; y++)
                    w.set(ex, y, ez, ladder.setValue(LadderBlock.FACING, out.getOpposite()));
                w.set(ex, TOP + 1, ez, hatch);
                w.set(ex + cxs, TOP, ez + czs, ClassicsKit.facing(bulb, out));
            } else if (r < 0.75f) {
                w.set(wxp, FLOOR + 2, wzp, graffiti[2]);
                w.set(ex, TOP, ez, ClassicsKit.facing(bulbDead, out));
            } else {
                w.set(ex, TOP, ez, ClassicsKit.facing(bulbDead, out));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Every so often the pipes near a player start to knock, faster and faster,
     * and two seconds later a pipe bursts in a cloud of scalding steam. Anyone
     * close who is standing up gets burned and blinded. Crouch and it passes over you.
     */
    static final class Pressure implements LevelBehavior {
        private final Map<UUID, long[]> state = new ConcurrentHashMap<>();   // nextAt, burstAt, x, y, z

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            state.put(p.getUUID(), new long[]{level.getGameTime() + 300 + p.getRandom().nextInt(300), 0, 0, 0, 0});
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            long now = level.getGameTime();
            long[] st = state.computeIfAbsent(p.getUUID(), u -> new long[]{now + 400, 0, 0, 0, 0});
            if (st[1] > 0) {
                if (now >= st[1]) {
                    burst(level, new BlockPos((int) st[2], (int) st[3], (int) st[4]));
                    st[1] = 0;
                    boolean hot = new Net(Hash.hash(level.getSeed(), Hash.salt("level_2"))).hot(p.getBlockX(), p.getBlockZ());
                    st[0] = now + (hot ? 300 : 500) + p.getRandom().nextInt(hot ? 300 : 600);
                }
                return;
            }
            if (now < st[0]) return;
            BlockPos spot = pickSpot(p, level);
            if (spot == null) {
                st[0] = now + 40;
                return;
            }
            ClassicsKit.sound(level, spot, "event.level_2_knock", 2.0f, 0.95f + p.getRandom().nextFloat() * 0.1f);
            st[1] = now + 42;
            st[2] = spot.getX();
            st[3] = spot.getY();
            st[4] = spot.getZ();
        }

        private static BlockPos pickSpot(ServerPlayer p, ServerLevel level) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2;
                double d = 2.5 + p.getRandom().nextDouble() * 4.5;
                BlockPos pos = BlockPos.containing(p.getX() + Math.cos(ang) * d, p.getY(), p.getZ() + Math.sin(ang) * d);
                if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        && !level.getBlockState(pos.below()).isAir()) return pos.above();
            }
            return null;
        }

        private static void burst(ServerLevel level, BlockPos pos) {
            ClassicsKit.sound(level, pos, "event.level_2_burst", 2.2f, 0.9f + level.random.nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 160, 1.3, 0.6, 1.3, 0.06);
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.6, 0.4, 0.6, 0.12);
            for (ServerPlayer q : level.players()) {
                double d2 = q.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                if (d2 > 4.5 * 4.5 || q.isSpectator()) continue;
                if (q.isShiftKeyDown()) {
                    ClassicsKit.event(q, "flash", 0.3f);
                } else {
                    ClassicsKit.event(q, "flash", 0.85f);
                    if (!q.isCreative()) {
                        q.hurt(level.damageSources().hotFloor(), d2 < 6.25 ? 5f : 3f);
                        Sanity.add(q, -5f);
                    }
                }
            }
        }
    }
}
