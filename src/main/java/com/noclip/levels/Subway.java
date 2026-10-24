package com.noclip.levels;

import com.noclip.block.SubwayLetterBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subway Station. Parallel lines of tiled platforms, each a station then a tunnel then
 * a station, forever. A train comes through every minute or so at full speed, lit,
 * full of people who do not move, and never stops. Step down onto the tracks and the
 * rails start to sing: one is already on its way. The third rail kills on touch. The
 * ways out are a stair that is not shuttered, or a service door in a tunnel refuge,
 * which means walking the tracks.
 */
public final class Subway {
    public static final String ID = "subway";
    static final int FLOOR = 10, CEIL = 16, P = 40, PX = 96, STATION = 64;
    static final int N0 = 14, S0 = 20, W = 5;      // track envelopes: lz N0..N0+4 and S0..S0+4
    static final int SPEED = 2, CAR = 15, CARS = 3, LEN = CAR * CARS - 1, REACH = 200;
    static final String[] NAMES = {"NOWHERE", "LAST STOP", "HALLWAY", "NO EXIT", "TERMINUS", "HOMEWARD", "YOUR STOP",
            "OUTBOUND", "FAREWELL", "LOBBY", "UNDERHILL", "ALMOND ST", "INBOUND", "GROUND", "NEXT"};

    private Subway() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Subway Station").order(1160)
                .tagline("Stand behind the yellow line. It does not stop here.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Trains())
                .visuals(new Visuals().tint(0.92f, 1.03f, 0.98f).saturation(0.74f).contrast(1.1f).brightness(1.06f)
                        .grain(0.07f).aberration(0.0016f).vignette(0.5f).bloom(0.5f).fog(6f, 70f).fogColor(0x161A18))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 3, 0.3f).dist(16, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(14, 30).weight(2))
                .exit(ExitType.NOCLIP, "level_1", 2).exit(ExitType.NOCLIP, "storm_drains", 1).exit(ExitType.NOCLIP, "level_2", 1)
                .exit(ExitType.NOCLIP, "airport", 1)
                .exit(ExitType.ASCEND, "level_11", 2).exit(ExitType.ASCEND, "rain_city", 2).exit(ExitType.ASCEND, "dead_mall", 1)
                .exit(ExitType.ASCEND, "lift_lobby", 1).exit(ExitType.ASCEND, "airport", 1)
                .exit(ExitType.DOOR, "storm_drains", 2).exit(ExitType.DOOR, "sewers", 2).exit(ExitType.DOOR, "level_2", 1)
                .exit(ExitType.DOOR, "power_plant", 1).exit(ExitType.DOOR, "level_3", 1)
                .build();
    }

    static boolean station(int x) {
        return Math.floorMod(x, PX) < STATION;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState white = ModBlocks.state("sub_tile_white");
        final BlockState band = ModBlocks.state("sub_tile_band");
        final BlockState base = ModBlocks.state("sub_tile_base");
        final BlockState platform = ModBlocks.state("sub_platform");
        final BlockState edgeS = ModBlocks.state("sub_edge");
        final BlockState edgeN = ModBlocks.state("sub_edge_n");
        final BlockState ballast = ModBlocks.state("sub_ballast");
        final BlockState sleeper = ModBlocks.state("sub_sleeper");
        final BlockState rail = ModBlocks.state("sub_rail");
        final BlockState third = ModBlocks.state("sub_third_rail");
        final BlockState column = ModBlocks.state("sub_column");
        final BlockState ceiling = ModBlocks.state("sub_ceiling");
        final BlockState pipes = ModBlocks.state("sub_ceiling_pipes");
        final BlockState tunnel = ModBlocks.state("sub_tunnel_wall");
        final BlockState strip = ModBlocks.state("sub_strip");
        final BlockState stripOff = ModBlocks.state("sub_strip_off");
        final BlockState bench = ModBlocks.state("sub_bench");
        final BlockState bin = ModBlocks.state("sub_bin");
        final BlockState map = ModBlocks.state("sub_map");
        final BlockState shutter = ModBlocks.state("sub_shutter");
        final BlockState exitUp = ModBlocks.state("sub_exit_up");
        final BlockState door = ModBlocks.state("sub_service_door");
        final BlockState doorTop = ModBlocks.state("sub_service_door_top");
        final BlockState red = ModBlocks.state("sub_signal_red");
        final BlockState green = ModBlocks.state("sub_signal_green");
        final BlockState letter = ModBlocks.state("sub_letter");
        final BlockState steps = ModBlocks.state("concrete_stairs");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState[][] boards = {
                {ModBlocks.state("sub_board_a1"), ModBlocks.state("sub_board_a2")},
                {ModBlocks.state("sub_board_b1"), ModBlocks.state("sub_board_b2")},
                {ModBlocks.state("sub_board_c1"), ModBlocks.state("sub_board_c2")}};
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 2, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, w.maxY, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                boolean st = station(x);
                int lx = Math.floorMod(x, PX);
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lz = Math.floorMod(z, P);
                    column(w, s, x, z, lx, lz, st);
                }
            }
            // per-station fittings
            int i0 = Math.floorDiv(w.minX, PX), i1 = Math.floorDiv(w.maxX, PX);
            int k0 = Math.floorDiv(w.minZ, P), k1 = Math.floorDiv(w.maxZ, P);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) fittings(w, s, i, k);
        }

        void column(ChunkWriter w, long s, int x, int z, int lx, int lz, boolean st) {
            boolean trenchN = lz >= N0 && lz < N0 + W, trenchS = lz >= S0 && lz < S0 + W;
            if (lz < 8 || lz > 30) {
                w.fill(x, FLOOR - 1, z, x, CEIL, z, st ? white : tunnel);
                return;
            }
            if (trenchN || trenchS) {
                int t = trenchN ? lz - N0 : lz - S0;      // 0..4
                w.set(x, FLOOR - 1, z, (t >= 1 && t <= 3) ? sleeper : ballast);
                BlockState top = ModBlocks.AIR;
                if (t == 1 || t == 3) top = rail;
                if ((trenchN && t == 4) || (trenchS && t == 0)) top = third;
                w.set(x, FLOOR, z, top);
                for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
                w.set(x, CEIL, z, st ? pipes : tunnel);
                return;
            }
            if (lz == 19) {
                w.set(x, FLOOR - 1, z, ballast);
                if (st) {
                    w.set(x, FLOOR, z, platform);
                    boolean col = Math.floorMod(x, 4) == 0;
                    for (int y = FLOOR + 1; y < CEIL; y++)
                        w.set(x, y, z, col ? column.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : ModBlocks.AIR);
                    w.set(x, CEIL, z, pipes);
                } else {
                    w.fill(x, FLOOR, z, x, CEIL, z, tunnel);
                }
                return;
            }
            // platforms and walls
            if (!st) {
                w.fill(x, FLOOR - 1, z, x, CEIL, z, tunnel);
                return;
            }
            boolean wall = lz == 8 || lz == 30;
            if (wall) {
                w.set(x, FLOOR - 1, z, base);
                w.set(x, FLOOR, z, base);
                w.set(x, FLOOR + 1, z, base);
                w.set(x, FLOOR + 2, z, white);
                w.set(x, FLOOR + 3, z, band);
                w.set(x, FLOOR + 4, z, white);
                w.set(x, FLOOR + 5, z, white);
                w.set(x, CEIL, z, ceiling);
                return;
            }
            w.set(x, FLOOR - 1, z, base);
            BlockState f = platform;
            if (lz == 13) f = edgeS;
            if (lz == 25) f = edgeN;
            w.set(x, FLOOR, z, f);
            for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
            w.set(x, CEIL, z, ceiling);
            int pz = lz <= 13 ? lz - 9 : lz - 25;   // 0..4 across the platform
            if (pz == 2 && Math.floorMod(lx, 8) == 4) {
                for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, column.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            } else if (pz == 2 && Math.floorMod(lx, 4) != 0) {
                w.set(x, CEIL - 1, z, Hash.chance(s, x, z, 0x5719, 0.07f) ? stripOff : strip);
            }
        }

        void fittings(ChunkWriter w, long s, int i, int k) {
            int x0 = i * PX, z0 = k * P;
            long h = Hash.hash(s, i, k, 0x5AB);
            String name = NAMES[Hash.range(h, NAMES.length)];
            // name tiles on both back walls, twice per station
            for (int at : new int[]{10, 42}) {
                for (int c = 0; c < name.length(); c++) {
                    int idx = SubwayLetterBlock.index(name.charAt(c));
                    w.set(x0 + at + c, FLOOR + 3, z0 + 8, letter.setValue(SubwayLetterBlock.LETTER, idx));
                    // the south wall reads the other way round
                    w.set(x0 + at + name.length() - 1 - c, FLOOR + 3, z0 + 30, letter.setValue(SubwayLetterBlock.LETTER, idx));
                }
            }
            // benches, bins, maps along the back walls
            for (int lx = 2; lx < STATION - 2; lx++) {
                int m = lx % 16;
                if (m == 5 || m == 6) {
                    w.set(x0 + lx, FLOOR + 1, z0 + 9, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + lx, FLOOR + 1, z0 + 29, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
                if (m == 9) {
                    w.set(x0 + lx, FLOOR + 1, z0 + 9, bin);
                    w.set(x0 + lx, FLOOR + 1, z0 + 29, bin);
                }
                if (lx % 32 == 24) {
                    w.set(x0 + lx, FLOOR + 2, z0 + 8, map);
                    w.set(x0 + lx, FLOOR + 2, z0 + 30, map);
                }
            }
            // departure boards
            BlockState[] b = boards[Hash.range(h >>> 8, 3)];
            for (int at : new int[]{14, 46}) {
                for (int pz : new int[]{12, 26}) {
                    w.set(x0 + at, CEIL - 1, z0 + pz, b[0]);
                    w.set(x0 + at + 1, CEIL - 1, z0 + pz, b[1]);
                }
            }
            // the way up: open, or shuttered
            boolean open = Hash.unit(h >>> 12) < 0.4f;
            int sx = x0 + 29;
            for (int c = 0; c < 3; c++) {
                int x = sx + c;
                if (!open) {
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) w.set(x, y, z0 + 8, shutter);
                    continue;
                }
                for (int d = 0; d <= 4; d++) {
                    int z = z0 + 8 - d, y = FLOOR + 1 + d;
                    w.set(x, y, z, steps.setValue(StairBlock.FACING, Direction.NORTH));
                    for (int yy = y + 1; yy <= y + 3; yy++) w.set(x, yy, z, ModBlocks.AIR);
                    w.set(x, y + 4, z, ceiling);
                }
                w.set(x, FLOOR + 6, z0 + 3, exitUp);
                w.set(x, FLOOR + 7, z0 + 3, exitUp);
                w.set(x, FLOOR + 8, z0 + 3, exitUp);
                w.set(x, FLOOR + 9, z0 + 3, ceiling);
                w.set(x, FLOOR + 6, z0 + 2, filler);
            }
            if (open) {
                for (int d = -1; d <= 5; d++) {
                    int z = z0 + 8 - d;
                    for (int y = FLOOR + 1; y <= FLOOR + 9; y++) {
                        if (d >= 0) {
                            w.set(sx - 1, y, z, white);
                            w.set(sx + 3, y, z, white);
                        }
                    }
                }
                w.set(sx + 1, FLOOR + 4, z0 + 9, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
            // tunnel: refuge niches, signals, and maybe a service door
            boolean hasDoor = Hash.unit(h >>> 16) < 0.6f;
            boolean doorNorth = Hash.unit(h >>> 20) < 0.5f;
            for (int n = 0; n < 3; n++) {
                int x = x0 + STATION + 6 + n * 10;
                for (int side = 0; side < 2; side++) {
                    int nz = z0 + (side == 0 ? 13 : 25);
                    w.set(x, FLOOR, nz, platform);
                    w.set(x, FLOOR + 1, nz, ModBlocks.AIR);
                    w.set(x, FLOOR + 2, nz, ModBlocks.AIR);
                    if (hasDoor && n == 1 && (side == 0) == doorNorth) {
                        int bz = z0 + (side == 0 ? 12 : 26);
                        w.set(x, FLOOR + 1, bz, door);
                        w.set(x, FLOOR + 2, bz, doorTop);
                        w.set(x, FLOOR + 3, nz, green);
                    }
                }
            }
            for (int n = 0; n < 2; n++) {
                int x = x0 + STATION + 11 + n * 10;
                w.set(x, FLOOR + 3, z0 + 13, red);
                w.set(x, FLOOR + 3, z0 + 25, red);
            }
        }
    }

    // ---------------------------------------------------------------- the train

    static final class Train {
        final ResourceKey<Level> dim;
        final int z0, dir;        // band origin z; +1 east (north track), -1 west (south track)
        final boolean north;
        int front;
        final int end;
        Train(ResourceKey<Level> dim, int z0, boolean north, int front, int end) {
            this.dim = dim;
            this.z0 = z0;
            this.north = north;
            this.dir = north ? 1 : -1;
            this.front = front;
            this.end = end;
        }

        int zMin() {
            return z0 + (north ? N0 : S0);
        }

        int back() {
            return front - dir * LEN;
        }
    }

    static final List<Train> TRAINS = new ArrayList<>();
    static final Map<UUID, Long> NEXT = new HashMap<>();
    static final Map<UUID, Long> SWEEP = new HashMap<>();

    static boolean trainBlock(BlockState s) {
        return s.is(ModBlocks.get("sub_train_body")) || s.is(ModBlocks.get("sub_train_window"))
                || s.is(ModBlocks.get("sub_train_window_fig")) || s.is(ModBlocks.get("sub_train_roof"))
                || s.is(ModBlocks.get("sub_train_front"));
    }

    static boolean active(ServerLevel level, int z0, boolean north) {
        for (Train t : TRAINS) if (t.dim == level.dimension() && t.z0 == z0 && t.north == north) return true;
        return false;
    }

    /** Launch a train that will pass x = at in about five seconds. */
    static void launch(ServerLevel level, int z0, boolean north, double at) {
        if (active(level, z0, north)) return;
        int dir = north ? 1 : -1;
        int start = (int) Math.floor(at) - dir * REACH;
        Train t = new Train(level.dimension(), z0, north, start, (int) Math.floor(at) + dir * (REACH + LEN));
        TRAINS.add(t);
        for (ServerPlayer p : level.players()) {
            if (Math.floorDiv(p.getBlockZ(), P) * P != z0 || Math.abs(p.getX() - at) > 160) continue;
            level.playSound(null, p.getX(), p.getY() + 1, p.getZ(), ModSounds.get("event.subway_train"), SoundSource.AMBIENT, 1.0f, 1f);
        }
    }

    /** What each slice of the train looks like, from the leading face (k = 0) back. */
    static final int[] TYPE = new int[LEN];
    static final int[] CHANGED;
    static {
        List<Integer> changed = new ArrayList<>();
        for (int k = 0; k < LEN; k++) {
            int inCar = k % CAR;
            int t;
            if (k == 0 || k == LEN - 1) t = 0;
            else if (inCar == CAR - 1) t = 1;
            else t = 2 + (Hash.chance(0x7A1, k, 0, 0.09f) ? 1 : 0) + (Hash.chance(0x7A1, k, 1, 0.09f) ? 2 : 0);
            TYPE[k] = t;
            if (k == 0 || TYPE[k] != TYPE[k - 1]) changed.add(k);
        }
        changed.add(LEN - 1);
        CHANGED = changed.stream().mapToInt(Integer::intValue).toArray();
    }

    static void writeSlice(ServerLevel level, Train t, int x, int k) {
        int type = TYPE[k];
        BlockState body = ModBlocks.state("sub_train_body");
        BlockState roof = ModBlocks.state("sub_train_roof");
        BlockState front = ModBlocks.state("sub_train_front");
        int zm = t.zMin();
        for (int dz = 0; dz < W; dz++) {
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                BlockState s = ModBlocks.AIR;
                boolean side = dz == 0 || dz == W - 1;
                if (type == 1) {
                    if (dz == 2 && y <= FLOOR + 2) s = body;          // coupling
                } else if (type == 0) {
                    if (!(side && y == FLOOR + 4)) s = front;
                } else if (y == FLOOR + 4) {
                    if (!side) s = roof;
                } else if (y == FLOOR + 2 && side) {
                    boolean fig = dz == 0 ? (type & 1) == 1 : (type & 2) == 2;
                    s = ModBlocks.state(fig ? "sub_train_window_fig" : "sub_train_window")
                            .setValue(HorizontalDirectionalBlock.FACING, dz == 0 ? Direction.NORTH : Direction.SOUTH);
                } else {
                    s = body;
                }
                BlockPos p = new BlockPos(x, y, zm + dz);
                BlockState cur = level.getBlockState(p);
                if (cur != s && (cur.isAir() || trainBlock(cur))) level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    static void clearSlice(ServerLevel level, Train t, int x) {
        int zm = t.zMin();
        for (int dz = 0; dz < W; dz++)
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                BlockPos p = new BlockPos(x, y, zm + dz);
                if (trainBlock(level.getBlockState(p))) level.setBlock(p, ModBlocks.AIR, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
    }

    static boolean loaded(ServerLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    static void step(ServerLevel level, Train t) {
        for (int n = 0; n < SPEED; n++) {
            t.front += t.dir;
            for (int k : CHANGED) {
                int x = t.front - t.dir * k;
                if (loaded(level, x, t.zMin())) writeSlice(level, t, x, k);
            }
            int tail = t.front - t.dir * LEN;
            if (loaded(level, tail, t.zMin())) clearSlice(level, t, tail);
        }
        // anything in the way
        int lo = Math.min(t.front, t.back()) - 1, hi = Math.max(t.front, t.back()) + 1;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            boolean inBand = p.getZ() >= t.zMin() - 0.3 && p.getZ() <= t.zMin() + W + 0.3 && p.getY() < FLOOR + 5 && p.getY() > FLOOR - 1;
            if (inBand && p.getX() >= lo && p.getX() <= hi + 1) {
                if (!p.isCreative()) {
                    p.hurt(level.damageSources().flyIntoWall(), 200f);
                    p.setDeltaMovement(t.dir * 3.0, 0.6, 0);
                    p.hurtMarked = true;
                }
            }
            // the wind it drags along the platform
            double nearZ = Math.max(t.zMin() - p.getZ(), p.getZ() - (t.zMin() + W));
            double nearX = Math.abs(p.getX() - t.front);
            if (nearZ < 6 && nearZ > -0.5 && Math.abs(p.getY() - (FLOOR + 1)) < 4 && (nearX < 8 || (p.getX() - lo) * (p.getX() - hi) < 0)) {
                p.push(t.dir * 0.05, 0, 0);
                p.hurtMarked = true;
                if (level.getGameTime() % 4 == 0) ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.25f));
            }
        }
        if (level.getGameTime() % 2 == 0) {
            int fx = t.front + t.dir * 2;
            level.sendParticles(ParticleTypes.ASH, fx, FLOOR + 2, t.zMin() + 2.5, 12, 1.0, 1.0, 3.0, 0.02);
        }
    }

    static void finish(ServerLevel level, Train t) {
        for (int k = -SPEED; k <= LEN + SPEED; k++) {
            int x = t.front - t.dir * k;
            if (loaded(level, x, t.zMin())) clearSlice(level, t, x);
        }
    }

    static final class Trains implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            NEXT.put(p.getUUID(), level.getGameTime() + 20 * (20 + p.getRandom().nextInt(25)));
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            int z0 = Math.floorDiv(p.getBlockZ(), P) * P;
            int lz = p.getBlockZ() - z0;
            Long next = NEXT.get(p.getUUID());
            if (next == null) next = now + 20 * 30;
            // down on the tracks: something is already coming
            boolean onTracks = p.getY() < FLOOR + 0.9 && p.getY() > FLOOR - 1 && ((lz >= N0 && lz < N0 + W) || (lz >= S0 && lz < S0 + W));
            if (onTracks && !p.isSpectator() && next - now > 20 * 9) {
                boolean north = lz < 19;
                next = now + 20 * 3 + p.getRandom().nextInt(40);
                NEXT.put(p.getUUID(), next);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.get("event.subway_rumble"), SoundSource.AMBIENT, 1.2f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.5f));
                TRACK.put(p.getUUID(), north);
            }
            if (now >= next) {
                Boolean forced = TRACK.remove(p.getUUID());
                boolean north = forced != null ? forced : p.getRandom().nextBoolean();
                launch(level, z0, north, p.getX());
                NEXT.put(p.getUUID(), now + 20 * (45 + p.getRandom().nextInt(50)));
            } else {
                NEXT.put(p.getUUID(), next);
            }
            // leftover carriages from a train that ran out of loaded track
            Long sw = SWEEP.get(p.getUUID());
            if (sw == null || now >= sw) {
                SWEEP.put(p.getUUID(), now + 200);
                sweep(level, p, z0);
            }
        }

        void sweep(ServerLevel level, ServerPlayer p, int z0) {
            for (boolean north : new boolean[]{true, false}) {
                if (active(level, z0, north)) continue;
                int zm = z0 + (north ? N0 : S0);
                for (int x = p.getBlockX() - 64; x <= p.getBlockX() + 64; x++) {
                    if (!loaded(level, x, zm)) continue;
                    for (int dz = 0; dz < W; dz += 2) {
                        if (trainBlock(level.getBlockState(new BlockPos(x, FLOOR + 3, zm + dz)))
                                || trainBlock(level.getBlockState(new BlockPos(x, FLOOR + 1, zm + dz)))) {
                            for (int dd = 0; dd < W; dd++)
                                for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                                    BlockPos bp = new BlockPos(x, y, zm + dd);
                                    if (trainBlock(level.getBlockState(bp))) level.setBlock(bp, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                                }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            for (Iterator<Train> it = TRAINS.iterator(); it.hasNext(); ) {
                Train t = it.next();
                if (t.dim != level.dimension()) continue;
                step(level, t);
                if ((t.back() - t.end) * t.dir > 0) {
                    finish(level, t);
                    it.remove();
                }
            }
        }
    }

    static final Map<UUID, Boolean> TRACK = new HashMap<>();
}
