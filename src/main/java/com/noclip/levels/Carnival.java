package com.noclip.levels;

import com.noclip.block.GondolaBlock;
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
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Closed Carnival. A midway under strings of coloured bulbs, game booths with prizes on
 * the walls, a carousel and a ferris wheel turning with nobody on them. The rides only
 * run while someone is near enough to see. There is one way out that the fair offers:
 * get in a car of the ferris wheel and ride it to the very top.
 */
public final class Carnival {
    static final String ID = "carnival";
    static final int G = 40;
    static final int LOT = 32, PATH = 5;
    static final int WHEEL_R = 11, WHEEL_UP = 13, CARS = 12, STEPS = 96;
    static final int CAR_R = 4;

    private Carnival() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Closed Carnival").order(3190)
                .tagline("The rides are still running. Nobody is on them. Yet.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Rides())
                .visuals(new Visuals().tint(1.04f, 0.98f, 1.02f).saturation(1.05f).contrast(1.08f).brightness(1.3f)
                        .grain(0.07f).aberration(0.0018f).vignette(0.55f).warp(0.015f)
                        .fog(4f, 56f).fogColor(0x120C14).bloom(0.85f).sky())
                .sanityDrain(0.035f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("partygoer"), 2, 0.35f).dark().dist(16, 32).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.SECRET, "level_fun", 2).exit(ExitType.SECRET, "sky_rooms", 1).exit(ExitType.SECRET, "the_hub", 1)
                .exit(ExitType.DOOR, "level_fun", 3).exit(ExitType.DOOR, "parking_lot", 2).exit(ExitType.DOOR, "mirror_level", 1)
                .exit(ExitType.NOCLIP, "level_fun", 2).exit(ExitType.NOCLIP, "drive_in", 1).exit(ExitType.NOCLIP, "parking_lot", 1)
                .build();
    }

    enum Kind { BOOTHS, WHEEL, CAROUSEL, FUNHOUSE, FOOD, EXIT, OPEN }

    static Kind kind(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 1);
        if (r < 0.13f) return Kind.WHEEL;
        if (r < 0.27f) return Kind.CAROUSEL;
        if (r < 0.33f) return Kind.FUNHOUSE;
        if (r < 0.43f) return Kind.FOOD;
        if (r < 0.46f) return Kind.EXIT;
        if (r < 0.88f) return Kind.BOOTHS;
        return Kind.OPEN;
    }

    static BlockPos center(int i, int k) {
        return new BlockPos(i * LOT + 18, G, k * LOT + 18);
    }

    // ---------------------------------------------------------------- the ferris wheel, as geometry

    /** Static wheel structure in the wheel's plane (dx across, dy up from the hub). */
    static String wheelAt(int dx, int dy) {
        if (dx == 0 && dy == 0) return "cv_hub";
        double r = Math.sqrt(dx * dx + dy * dy);
        if (Math.abs(r - WHEEL_R) < 0.55) return "cv_rim";
        if (r < WHEEL_R) {
            double a = Math.atan2(dy, dx);
            for (int n = 0; n < 8; n++) {
                double sa = Math.PI / 8 + n * Math.PI / 4;
                double perp = Math.abs(-Math.sin(sa) * dx + Math.cos(sa) * dy);
                double along = Math.cos(sa) * dx + Math.sin(sa) * dy;
                if (along > 0 && perp < 0.5) return "cv_truss";
            }
        }
        return null;
    }

    static int[] car(int n, int step) {
        double a = 2 * Math.PI * n / CARS + 2 * Math.PI * step / STEPS - Math.PI / 2;
        return new int[]{(int) Math.round(WHEEL_R * Math.cos(a)), (int) Math.round(WHEEL_R * Math.sin(a)) - 1};
    }

    static double carAngle(int n, int step) {
        double a = 360.0 * n / CARS + 360.0 * step / STEPS - 90;
        return ((a % 360) + 360) % 360;
    }

    // ---------------------------------------------------------------- the carousel, as geometry

    static final int RING = 16, HORSES = 8;

    static int[] horse(int n, int step) {
        double a = 2 * Math.PI * (n * 2 + step) / RING;
        return new int[]{(int) Math.round(CAR_R * 1.15 * Math.cos(a)), (int) Math.round(CAR_R * 1.15 * Math.sin(a))};
    }

    static Direction tangent(int n, int step) {
        double a = 2 * Math.PI * (n * 2 + step) / RING;
        double tx = -Math.sin(a), tz = Math.cos(a);
        if (Math.abs(tx) > Math.abs(tz)) return tx > 0 ? Direction.EAST : Direction.WEST;
        return tz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // ---------------------------------------------------------------- generator

    static final class Gen implements LevelGenerator {
        final BlockState sawdust = ModBlocks.state("cv_sawdust");
        final BlockState grass = ModBlocks.state("cv_grass");
        final BlockState dirt = ModBlocks.state("cv_dirt");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState wood = ModBlocks.state("cv_booth_wood");
        final BlockState pole = ModBlocks.state("cv_pole");
        final BlockState platform = ModBlocks.state("cv_platform");
        final BlockState mirror = ModBlocks.state("cv_mirror");
        final BlockState marquee = ModBlocks.state("cv_marquee");
        final BlockState trailer = ModBlocks.state("cv_trailer");
        final BlockState hay = ModBlocks.state("cv_hay");
        final BlockState concrete = ModBlocks.state("warehouse_concrete");
        final BlockState fence = ModBlocks.state("chain_link_fence");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int u = Math.floorMod(x, LOT), v = Math.floorMod(z, LOT);
                    w.fill(x, 0, z, x, G - 2, z, sub);
                    w.set(x, G - 1, z, dirt);
                    boolean path = u < PATH || v < PATH;
                    w.set(x, G, z, path ? sawdust : Hash.unit(s, x, z, 2) < 0.12f ? dirt : grass);
                    if (path && Hash.unit(s, x, z, 3) < 0.035f) {
                        w.set(x, G + 1, z, OutsideKit.facing("cv_litter_" + Hash.range(s, x, z, 4, 3), Direction.NORTH));
                    }
                    // bulb strings down the middle of every path, poles where paths cross
                    if (u == 2 && v == 2) {
                        w.fill(x, G + 1, z, x, G + 6, z, pole);
                    } else if (u == 2 && v >= PATH - 2) {
                        boolean sag = v > 11 && v < 25;
                        w.set(x, G + 6 - (sag ? 1 : 0), z, OutsideKit.facing(sag ? "cv_string_sag" : "cv_string_lights", Direction.EAST));
                    } else if (v == 2 && u >= PATH - 2) {
                        boolean sag = u > 11 && u < 25;
                        w.set(x, G + 6 - (sag ? 1 : 0), z, OutsideKit.facing(sag ? "cv_string_sag" : "cv_string_lights", Direction.NORTH));
                    }
                }
            int i0 = Math.floorDiv(w.minX, LOT) - 1, i1 = Math.floorDiv(w.maxX, LOT) + 1;
            int k0 = Math.floorDiv(w.minZ, LOT) - 1, k1 = Math.floorDiv(w.maxZ, LOT) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) {
                    int x0 = i * LOT, z0 = k * LOT;
                    if (x0 > w.maxX || x0 + LOT - 1 < w.minX || z0 > w.maxZ || z0 + LOT - 1 < w.minZ) continue;
                    switch (kind(s, i, k)) {
                        case WHEEL -> wheel(w, s, i, k);
                        case CAROUSEL -> carousel(w, s, i, k);
                        case FUNHOUSE -> funhouse(w, s, x0, z0);
                        case FOOD -> food(w, s, x0, z0);
                        case EXIT -> exitGate(w, s, x0, z0);
                        case BOOTHS -> booths(w, s, i, k, x0, z0);
                        default -> open(w, s, x0, z0);
                    }
                }
        }

        void wheel(ChunkWriter w, long s, int i, int k) {
            BlockPos c = center(i, k);
            int cx = c.getX(), cy = G + WHEEL_UP, cz = c.getZ();
            for (int dx = -9; dx <= 9; dx++)
                for (int dz = -4; dz <= 4; dz++) w.set(cx + dx, G, cz + dz, concrete);
            for (int dx = -WHEEL_R - 1; dx <= WHEEL_R + 1; dx++)
                for (int dy = -WHEEL_R - 1; dy <= WHEEL_R + 1; dy++) {
                    String b = wheelAt(dx, dy);
                    if (b != null && cy + dy > G) w.set(cx + dx, cy + dy, cz, ModBlocks.state(b));
                }
            // A-frame legs on both sides of the wheel
            for (int side : new int[]{-2, 2}) {
                w.set(cx, cy, cz + side, ModBlocks.state("cv_hub"));
                w.set(cx, cy, cz + side / 2, ModBlocks.state("cv_hub"));
                for (int leg : new int[]{-1, 1}) {
                    int fx = cx + leg * 8;
                    for (int t = 0; t <= 26; t++) {
                        double f = t / 26.0;
                        int x = (int) Math.round(fx + (cx - fx) * f);
                        int y = (int) Math.round(G + 1 + (cy - G - 1) * f);
                        w.set(x, y, cz + side, ModBlocks.state("cv_truss"));
                    }
                }
            }
            for (int n = 0; n < CARS; n++) {
                int[] p = car(n, 0);
                if (wheelAt(p[0], p[1]) == null) w.set(cx + p[0], cy + p[1], cz, ModBlocks.state("cv_gondola"));
            }
            // a ticket stand by the boarding gate
            w.set(cx + 2, G + 1, cz + 3, OutsideKit.facing("cv_counter_3", Direction.SOUTH));
        }

        void carousel(ChunkWriter w, long s, int i, int k) {
            BlockPos c = center(i, k);
            int cx = c.getX(), cz = c.getZ();
            for (int dx = -9; dx <= 9; dx++)
                for (int dz = -9; dz <= 9; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    int x = cx + dx, z = cz + dz;
                    if (d <= 7.2) w.set(x, G + 1, z, OutsideKit.facing(platform, Direction.NORTH));
                    if (d <= 1.5) w.fill(x, G + 2, z, x, G + 5, z, mirror);
                    double ang = Math.atan2(dz, dx);
                    boolean stripe = Math.floorMod((int) Math.floor((ang + Math.PI) / (Math.PI / 8)), 2) == 0;
                    BlockState can = ModBlocks.state(stripe ? "cv_canopy_a" : "cv_canopy_b");
                    if (d <= 8.5) w.set(x, G + 6, z, d > 7.6 ? marquee : can);
                    if (d <= 6.5) w.set(x, G + 7, z, can);
                    if (d <= 4.5) w.set(x, G + 8, z, can);
                    if (d <= 2.5) w.set(x, G + 9, z, can);
                }
            w.set(cx, G + 10, cz, pole);
            w.set(cx, G + 11, cz, ModBlocks.state("cv_marquee"));
            for (int n = 0; n < HORSES; n++) placeHorse(OutsideKit.Setter.of(w), cx, cz, n, 0);
        }

        void funhouse(ChunkWriter w, long s, int x0, int z0) {
            int fx = x0 + 11, fz = z0 + 8;
            for (int u = -3; u <= 7; u++)
                for (int d = 0; d <= 10; d++)
                    for (int y = 1; y <= 6; y++) {
                        boolean edge = u == -3 || u == 7 || d == 10 || y == 6;
                        if (d == 0 && u >= 0 && u <= 4 && y <= 4) continue;
                        if (edge || d == 0) w.set(fx + u, G + y, fz + d, ModBlocks.state("cv_funhouse_wall"));
                    }
            for (int c = 0; c < 5; c++)
                for (int r = 0; r < 4; r++)
                    w.set(fx + 4 - c, G + 4 - r, fz, ModBlocks.state("cv_clown_" + c + "_" + r));
            w.set(fx + 2, G + 1, fz, ModBlocks.state("cv_funhouse_mouth"));
            w.set(fx + 2, G + 2, fz, ModBlocks.state("cv_funhouse_mouth"));
            for (int y = 1; y <= 5; y++) {
                w.set(fx - 1, G + y, fz - 1, marquee);
                w.set(fx + 5, G + y, fz - 1, marquee);
            }
        }

        void food(ChunkWriter w, long s, int x0, int z0) {
            int fx = x0 + 13, fz = z0 + 12;
            for (int u = 0; u < 7; u++)
                for (int d = 0; d < 3; d++)
                    for (int y = 1; y <= 3; y++) {
                        boolean window = d == 0 && y == 2 && u >= 1 && u <= 5;
                        boolean inside = d == 1 && u > 0 && u < 6 && y < 3;
                        w.set(fx + u, G + y, fz + d, window || inside ? ModBlocks.AIR : trailer);
                    }
            w.set(fx + 3, G + 2, fz + 1, OutsideKit.light(13));
            for (int c = 0; c < 3; c++) w.set(fx + 2 + c, G + 4, fz, OutsideKit.facing("cv_food_sign_" + (2 - c), Direction.NORTH));
            for (int n = 0; n < 4; n++) {
                long h = Hash.hash(s, x0, z0, n);
                w.set(x0 + 8 + Hash.range(h, 16), G + 1, z0 + 18 + Hash.range(h >>> 8, 8), hay);
            }
        }

        void exitGate(ChunkWriter w, long s, int x0, int z0) {
            int z = z0 + 16;
            for (int u = PATH; u < LOT; u++) {
                int x = x0 + u;
                if (u >= 16 && u <= 18) {
                    w.set(x, G + 1, z, ModBlocks.state("cv_turnstile"));
                    continue;
                }
                BlockState f = fence.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
                w.set(x, G + 1, z, f);
                w.set(x, G + 2, z, f);
            }
            for (int y = 1; y <= 4; y++) {
                w.set(x0 + 15, G + y, z, pole);
                w.set(x0 + 19, G + y, z, pole);
            }
            for (int c = 0; c < 3; c++) w.set(x0 + 16 + c, G + 4, z, OutsideKit.facing("cv_exit_sign_" + (2 - c), Direction.NORTH));
        }

        void booths(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            for (int row = 0; row < 2; row++) {
                Direction out = row == 0 ? Direction.NORTH : Direction.SOUTH;
                int front = row == 0 ? z0 + PATH : z0 + LOT - 1;
                int dz = row == 0 ? 1 : -1;
                for (int b = 0; b < 4; b++) {
                    int bx = x0 + 6 + b * 6;
                    long h = Hash.hash(s, i * 8L + row * 4 + b, k, 77);
                    if (Hash.unit(h) < 0.12f) continue;
                    int game = Hash.range(h >>> 4, 4);
                    for (int u = 0; u <= 5; u++)
                        for (int d = 0; d <= 3; d++) {
                            int x = bx + u, z = front + dz * d;
                            boolean side = u == 0 || u == 5;
                            if (d == 3 || side) {
                                for (int y = 1; y <= 4; y++) {
                                    BlockState wb = d == 3 && !side && y <= 3 ? ModBlocks.state("cv_prizes_" + game) : wood;
                                    w.set(x, G + y, z, wb);
                                }
                            }
                            w.set(x, G + 5, z, wood);
                            if (d == 0 && !side) w.set(x, G + 1, z, OutsideKit.facing("cv_counter_" + game, out));
                            if (d == 2 && !side && Hash.unit(h, u, 5) < 0.6f) {
                                String[] p = {"cv_plush_pink", "cv_plush_blue", "cv_plush_yellow"};
                                w.set(x, G + 3, z, ModBlocks.state(p[Hash.range(h, u, 6, 3)]));
                            }
                        }
                    for (int u = 1; u <= 4; u++) w.set(bx + u, G + 4, front - dz, OutsideKit.facing("cv_awning_" + game, out));
                    for (int c = 0; c < 3; c++) {
                        int u = out == Direction.NORTH ? 3 - c : 1 + c;
                        w.set(bx + u, G + 5, front, OutsideKit.facing("cv_sign_" + game + "_" + c, out));
                    }
                }
            }
            open(w, s, x0, z0);
        }

        void open(ChunkWriter w, long s, int x0, int z0) {
            for (int n = 0; n < 6; n++) {
                long h = Hash.hash(s, x0, z0, 100 + n);
                int x = x0 + 8 + Hash.range(h, 20), z = z0 + 11 + Hash.range(h >>> 8, 12);
                if (!w.contains(x, z) || !w.isAir(x, G + 1, z)) continue;
                float r = Hash.unit(h >>> 16);
                if (r < 0.35f) w.set(x, G + 1, z, hay);
                else if (r < 0.6f) w.set(x, G + 1, z, OutsideKit.facing("cv_litter_" + Hash.range(h, 3), Direction.NORTH));
                else if (r < 0.75f) {
                    w.set(x, G + 1, z, OutsideKit.facing("sf_fence_post", Direction.NORTH));
                    w.set(x, G + 2, z, ModBlocks.state("cv_balloon"));
                } else if (r < 0.85f) w.set(x, G + 1, z, ModBlocks.state("cv_plush_pink"));
            }
        }
    }

    static void placeHorse(OutsideKit.Setter w, int cx, int cz, int n, int step) {
        int[] p = horse(n, step);
        int x = cx + p[0], z = cz + p[1];
        int bob = ((n + step) % 2 == 0) ? 3 : 2;
        String colour = n % 2 == 0 ? "white" : "black";
        for (int y = 2; y <= 5; y++) {
            BlockState b = y == bob ? OutsideKit.facing("cv_horse_" + colour, tangent(n, step))
                    : OutsideKit.facing("cv_brass_pole", Direction.NORTH);
            w.set(x, G + y, z, b);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Runs the rides near players, carries riders round the wheel, and lets them off at the top. */
    static final class Rides implements LevelBehavior {
        private final Map<Long, Integer> wheelStep = new ConcurrentHashMap<>();
        private final Map<Long, Integer> carouselStep = new ConcurrentHashMap<>();
        /** rider -> (wheel key, car index) */
        private final Map<UUID, long[]> riders = new ConcurrentHashMap<>();

        Rides() {
            GondolaBlock.onBoard = (p, pos) -> {
                ServerLevel level = p.serverLevel();
                long s = OutsideKit.seed(level, ID);
                int i = Math.floorDiv(pos.getX(), LOT), k = Math.floorDiv(pos.getZ(), LOT);
                if (kind(s, i, k) != Kind.WHEEL) return;
                long key = key(i, k);
                int step = wheelStep.getOrDefault(key, 0);
                BlockPos c = center(i, k);
                for (int n = 0; n < CARS; n++) {
                    int[] q = car(n, step);
                    if (c.getX() + q[0] == pos.getX() && G + WHEEL_UP + q[1] == pos.getY()) {
                        riders.put(p.getUUID(), new long[]{key, n});
                        p.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
                        OutsideKit.sound(level, pos, "event.carnival_creak", 1.2f, 1.2f);
                        return;
                    }
                }
            };
        }

        static long key(int i, int k) {
            return ((long) i << 32) ^ (k & 0xffffffffL);
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            riders.remove(p.getUUID());
            if (!OutsideKit.autotest()) return;
            long s = OutsideKit.seed(level, ID);
            int pi = Math.floorDiv(p.getBlockX(), LOT), pk = Math.floorDiv(p.getBlockZ(), LOT);
            for (int r = 0; r < 6; r++)
                for (int di = -r; di <= r; di++)
                    for (int dk = -r; dk <= r; dk++)
                        if (kind(s, pi + di, pk + dk) == Kind.WHEEL) {
                            BlockPos c = center(pi + di, pk + dk);
                            level.getChunk(c);
                            p.teleportTo(level, c.getX() - 6.5, G + 1, c.getZ() + 19.5, 90f, -12f);
                            return;
                        }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long[] ride = riders.get(p.getUUID());
            if (ride != null && (p.isShiftKeyDown() || p.isRemoved())) {
                riders.remove(p.getUUID());
                ride = null;
            }
            if (p.tickCount % 2 != 0) return;
            long s = OutsideKit.seed(level, ID);
            int pi = Math.floorDiv(p.getBlockX(), LOT), pk = Math.floorDiv(p.getBlockZ(), LOT);
            long now = level.getGameTime();
            for (int di = -2; di <= 2; di++)
                for (int dk = -2; dk <= 2; dk++) {
                    int i = pi + di, k = pk + dk;
                    Kind kind = kind(s, i, k);
                    BlockPos c = center(i, k);
                    if (!level.isLoaded(c)) continue;
                    double dist = Math.sqrt(c.distSqr(p.blockPosition()));
                    if (kind == Kind.WHEEL && dist < 72 && now % 10 == 0) stepWheel(level, i, k, c);
                    if (kind == Kind.CAROUSEL && dist < 56 && now % 6 == 0) stepCarousel(level, i, k, c);
                    if (kind == Kind.CAROUSEL && dist < 40 && now % 104 == 0) OutsideKit.sound(level, c.above(4), "event.carnival_organ", 2.5f, 1f);
                    if (kind == Kind.WHEEL && dist < 48 && now % 50 == 0) OutsideKit.sound(level, c.above(WHEEL_UP), "event.carnival_creak", 2f, 0.8f);
                }
        }

        void stepWheel(ServerLevel level, int i, int k, BlockPos c) {
            long key = key(i, k);
            int step = (wheelStep.getOrDefault(key, 0) + 1) % STEPS;
            wheelStep.put(key, step);
            int cx = c.getX(), cy = G + WHEEL_UP, cz = c.getZ();
            Block gondola = ModBlocks.get("cv_gondola");
            // clear every car on the wheel's plane, then hang them one step further round
            for (int dx = -WHEEL_R - 1; dx <= WHEEL_R + 1; dx++)
                for (int dy = -WHEEL_R - 2; dy <= WHEEL_R; dy++) {
                    BlockPos q = new BlockPos(cx + dx, cy + dy, cz);
                    if (level.getBlockState(q).is(gondola)) level.setBlock(q, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                }
            for (int n = 0; n < CARS; n++) {
                int[] q = car(n, step);
                if (wheelAt(q[0], q[1]) == null && cy + q[1] > G) {
                    level.setBlock(new BlockPos(cx + q[0], cy + q[1], cz), ModBlocks.state("cv_gondola"), Block.UPDATE_CLIENTS);
                }
            }
            // carry riders with their car
            for (Map.Entry<UUID, long[]> e : riders.entrySet()) {
                if (e.getValue()[0] != key) continue;
                ServerPlayer rider = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (rider == null || rider.level() != level) continue;
                int n = (int) e.getValue()[1];
                int[] q = car(n, step);
                rider.teleportTo(level, cx + q[0] + 0.5, cy + q[1] + 0.15, cz + 0.5, rider.getYRot(), rider.getXRot());
                rider.resetFallDistance();
                double ang = carAngle(n, step);
                if (Math.abs(ang - 90) < 4) {
                    // the top of the wheel: you can see the whole fair, and then you can't
                    riders.remove(e.getKey());
                    OutsideKit.sound(level, rider.blockPosition(), "event.carnival_apex", 1.5f, 1f);
                    ServerPlayNetworking.send(rider, new NoclipNet.EventPayload("flash", 1f));
                    Travel.exit(rider, ExitType.SECRET, rider.blockPosition());
                }
            }
        }

        void stepCarousel(ServerLevel level, int i, int k, BlockPos c) {
            long key = key(i, k);
            int step = (carouselStep.getOrDefault(key, 0) + 1) % RING;
            carouselStep.put(key, step);
            int cx = c.getX(), cz = c.getZ();
            Block pole = ModBlocks.get("cv_brass_pole"), white = ModBlocks.get("cv_horse_white"), black = ModBlocks.get("cv_horse_black");
            for (int dx = -7; dx <= 7; dx++)
                for (int dz = -7; dz <= 7; dz++)
                    for (int y = 2; y <= 5; y++) {
                        BlockPos q = new BlockPos(cx + dx, G + y, cz + dz);
                        BlockState st = level.getBlockState(q);
                        if (st.is(pole) || st.is(white) || st.is(black)) level.setBlock(q, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                    }
            OutsideKit.Setter set = new OutsideKit.Setter() {
                public void set(int x, int y, int z, BlockState s) {
                    level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS);
                }

                public boolean air(int x, int y, int z) {
                    return level.getBlockState(new BlockPos(x, y, z)).isAir();
                }
            };
            for (int n = 0; n < HORSES; n++) placeHorse(set, cx, cz, n, step);
        }
    }
}
