package com.noclip.levels;

import com.noclip.entity.Groundskeeper;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Golf Course at Dusk. Hole after hole under a violet sky, pines between them, a flag in
 * every cup, and somewhere a clubhouse with its windows lit. The flags all stream toward
 * it. The sprinklers tick across the fairways all evening; when every one of them stops
 * at once, the Groundskeeper is out, and he will come for anyone standing on his grass.
 * Pull a flag and the cup is open.
 */
public final class GolfCourse {
    static final String ID = "golf_course";
    static final int G = 40;
    static final int HOLE = 72;

    private GolfCourse() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Golf Course at Dusk").order(3090)
                .tagline("Sprinklers tick. A flag in every hole. When the ticking stops, get off the grass.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Keeper())
                .visuals(new Visuals().tint(0.96f, 0.95f, 1.08f).saturation(0.78f).contrast(1.06f).brightness(1.14f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.48f).warp(0.01f)
                        .fog(8f, 76f).fogColor(0x3A3A5A).bloom(0.55f).sky())
                .sanityDrain(0.03f)
                .spawnY(G - 4, G + 5)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(30, 46))
                .exit(ExitType.FALL, "level_0", 2).exit(ExitType.FALL, "the_abyss", 1).exit(ExitType.FALL, "storm_drains", 1)
                .exit(ExitType.DOOR, "ballroom", 2).exit(ExitType.DOOR, "level_5", 2).exit(ExitType.DOOR, "bowling_alley", 1)
                .exit(ExitType.NOCLIP, "golden_hour", 2).exit(ExitType.NOCLIP, "foggy_playground", 1)
                .exit(ExitType.NOCLIP, "level_9", 1)
                .build();
    }

    static CellGrid grid(long s) {
        return new CellGrid(s ^ 0x601FL, HOLE, 0);
    }

    static boolean club(CellGrid g, int i, int k) {
        return g.unit(i, k, 1) < 0.07f;
    }

    static boolean alongX(CellGrid g, int i, int k) {
        return (g.hash(i, k, 2) & 1) == 0;
    }

    /** Lateral centre of the fairway at distance a along the hole. */
    static double centre(CellGrid g, int i, int k, double a) {
        long h = g.hash(i, k, 3);
        double amp = 2 + Hash.unit(h) * 7, ph = Hash.unit(h >>> 20) * 6.28;
        return 36 + Math.sin(a / 72.0 * Math.PI * 1.3 + ph) * amp;
    }

    static double width(CellGrid g, int i, int k, double a) {
        return 10 + 3 * Math.sin(a * 0.13 + g.unit(i, k, 4) * 6.28);
    }

    static int surface(long s, int x, int z) {
        return G + (int) Math.floor((Hash.fbm(s ^ 0x5F0L, x / 48.0, z / 48.0, 3) - 0.5f) * 7f);
    }

    static BlockPos toWorld(CellGrid g, int i, int k, double a, double b) {
        int x0 = g.x0(i), z0 = g.z0(k);
        return alongX(g, i, k) ? new BlockPos(x0 + (int) Math.round(a), 0, z0 + (int) Math.round(b))
                : new BlockPos(x0 + (int) Math.round(b), 0, z0 + (int) Math.round(a));
    }

    static BlockPos cup(long s, CellGrid g, int i, int k) {
        BlockPos p = toWorld(g, i, k, 62, centre(g, i, k, 62));
        return new BlockPos(p.getX(), surface(s, p.getX(), p.getZ()), p.getZ());
    }

    static BlockPos clubhouse(CellGrid g, int i, int k) {
        return new BlockPos(g.x0(i) + 36, G, g.z0(k) + 36);
    }

    static BlockPos nearestClub(CellGrid g, int i, int k) {
        for (int r = 1; r <= 7; r++)
            for (int di = -r; di <= r; di++)
                for (int dk = -r; dk <= r; dk++)
                    if ((Math.abs(di) == r || Math.abs(dk) == r) && club(g, i + di, k + dk)) return clubhouse(g, i + di, k + dk);
        return null;
    }

    static List<BlockPos> sprinklers(long s, CellGrid g, int i, int k) {
        List<BlockPos> out = new ArrayList<>();
        if (club(g, i, k)) return out;
        for (int a = 14; a <= 56; a += 14) {
            BlockPos p = toWorld(g, i, k, a, centre(g, i, k, a));
            out.add(new BlockPos(p.getX(), surface(s, p.getX(), p.getZ()) + 1, p.getZ()));
        }
        return out;
    }

    enum Turf { ROUGH, FAIRWAY, GREEN, TEE, SAND, POND }

    static final class Gen implements LevelGenerator {
        final BlockState fairway = ModBlocks.state("gc_fairway");
        final BlockState green = ModBlocks.state("gc_green");
        final BlockState tee = ModBlocks.state("gc_tee");
        final BlockState rough = ModBlocks.state("gc_rough");
        final BlockState sand = ModBlocks.state("gc_sand");
        final BlockState soil = ModBlocks.state("gc_soil");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState grass = ModBlocks.state("gc_long_grass");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState trunk = ModBlocks.state("gc_trunk").setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        final BlockState needles = ModBlocks.state("gc_needles");
        final BlockState wall = ModBlocks.state("gc_club_wall");
        final BlockState window = ModBlocks.state("gc_club_window");
        final BlockState roof = ModBlocks.state("gc_club_roof");
        final BlockState path = ModBlocks.state("warehouse_concrete");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = g.cellX(x), k = g.cellZ(z);
                    int h = surface(s, x, z);
                    Turf t = club(g, i, k) ? Turf.ROUGH : turf(s, g, i, k, x, z);
                    if (t == Turf.GREEN || t == Turf.TEE) h = flat(s, g, i, k, t);
                    w.fill(x, 0, z, x, h - 4, z, sub);
                    w.fill(x, h - 3, z, x, h - 1, z, soil);
                    switch (t) {
                        case FAIRWAY -> w.set(x, h, z, fairway);
                        case GREEN -> w.set(x, h, z, green);
                        case TEE -> w.set(x, h, z, tee);
                        case SAND -> {
                            w.set(x, h, z, ModBlocks.AIR);
                            w.set(x, h - 1, z, sand);
                        }
                        case POND -> {
                            w.fill(x, h - 2, z, x, h, z, water);
                            w.set(x, h - 3, z, soil);
                        }
                        default -> {
                            w.set(x, h, z, rough);
                            if (Hash.unit(s, x, z, 5) < 0.2f) w.set(x, h + 1, z, grass);
                        }
                    }
                }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (club(g, i, k)) clubhouseAt(w, s, g, i, k);
                else hole(w, s, g, i, k);
            });
            trees(w, s, g);
        }

        int flat(long s, CellGrid g, int i, int k, Turf t) {
            BlockPos c = t == Turf.GREEN ? toWorld(g, i, k, 62, centre(g, i, k, 62)) : toWorld(g, i, k, 10, centre(g, i, k, 10));
            return surface(s, c.getX(), c.getZ()) + (t == Turf.TEE ? 1 : 0);
        }

        Turf turf(long s, CellGrid g, int i, int k, int x, int z) {
            boolean ax = alongX(g, i, k);
            double a = ax ? x - g.x0(i) : z - g.z0(k);
            double b = ax ? z - g.z0(k) : x - g.x0(i);
            double cg = centre(g, i, k, 62);
            double dg = Math.hypot(a - 62, b - cg);
            if (dg <= 6.5) return Turf.GREEN;
            long h = g.hash(i, k, 6);
            // bunkers guarding the green, one out on the fairway
            if (Math.hypot(a - 57, b - cg - 8) < 2.8 || Math.hypot(a - 66, b - cg + 8) < 2.4) return Turf.SAND;
            double cb = centre(g, i, k, 40);
            if (Hash.unit(h) < 0.6f && Math.hypot(a - 40, b - cb - 7) < 2.6) return Turf.SAND;
            double ct = centre(g, i, k, 10);
            if (Math.abs(a - 10) <= 2 && Math.abs(b - ct) <= 2) return Turf.TEE;
            if (Hash.unit(h >>> 10) < 0.3f) {
                double cp = centre(g, i, k, 30);
                if (Math.pow((a - 30) / 5.5, 2) + Math.pow((b - cp + 1) / 9.0, 2) < 1) return Turf.POND;
            }
            if (a >= 13 && a <= 58 && Math.abs(b - centre(g, i, k, a)) <= width(g, i, k, a) / 2) return Turf.FAIRWAY;
            return Turf.ROUGH;
        }

        void hole(ChunkWriter w, long s, CellGrid g, int i, int k) {
            BlockPos cup = cup(s, g, i, k);
            BlockPos club = nearestClub(g, i, k);
            Direction toClub = Direction.NORTH;
            if (club != null) {
                int dx = club.getX() - cup.getX(), dz = club.getZ() - cup.getZ();
                toClub = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            }
            BlockState stick = ModBlocks.state("gc_flagstick");
            w.set(cup.getX(), cup.getY(), cup.getZ(), stick);
            w.set(cup.getX(), cup.getY() + 1, cup.getZ(), stick);
            w.set(cup.getX(), cup.getY() + 2, cup.getZ(), stick);
            w.set(cup.getX(), cup.getY() + 3, cup.getZ(), OutsideKit.facing("gc_flag", toClub));
            for (BlockPos sp : sprinklers(s, g, i, k)) {
                if (w.isAir(sp.getX(), sp.getY(), sp.getZ())) w.set(sp.getX(), sp.getY(), sp.getZ(), OutsideKit.facing("gc_sprinkler", Direction.NORTH));
            }
            double ct = centre(g, i, k, 10);
            for (int side : new int[]{-2, 2}) {
                BlockPos m = toWorld(g, i, k, 8, ct + side);
                w.set(m.getX(), surface(s, m.getX(), m.getZ()) + 1, m.getZ(), OutsideKit.facing("gc_tee_marker", Direction.NORTH));
            }
            boolean ax = alongX(g, i, k);
            BlockPos b = toWorld(g, i, k, 7, ct + 5);
            w.set(b.getX(), surface(s, b.getX(), b.getZ()) + 1, b.getZ(), OutsideKit.facing("gc_bench", ax ? Direction.WEST : Direction.NORTH));
            long h = g.hash(i, k, 8);
            if (Hash.unit(h) < 0.35f) {
                BlockPos sign = toWorld(g, i, k, 54, centre(g, i, k, 62) + 9);
                w.set(sign.getX(), surface(s, sign.getX(), sign.getZ()) + 1, sign.getZ(), OutsideKit.facing("gc_sign", ax ? Direction.WEST : Direction.NORTH));
            }
            if (Hash.unit(h >>> 12) < 0.3f) {
                BlockPos c = toWorld(g, i, k, 12, ct - 6);
                Direction d = ax ? Direction.EAST : Direction.SOUTH;
                int y = surface(s, c.getX(), c.getZ()) + 1;
                w.set(c.getX(), y, c.getZ(), OutsideKit.facing("gc_cart_front", d));
                w.set(c.getX() - d.getStepX(), y, c.getZ() - d.getStepZ(), OutsideKit.facing("gc_cart_back", d));
            }
        }

        void trees(ChunkWriter w, long s, CellGrid g) {
            for (int x = w.minX - 3; x <= w.maxX + 3; x++)
                for (int z = w.minZ - 3; z <= w.maxZ + 3; z++) {
                    if (Hash.unit(s, x, z, 21) > 0.035f) continue;
                    if (Hash.fbm(s ^ 0x7EE5L, x / 30.0, z / 30.0, 2) < 0.48f) continue;
                    int i = g.cellX(x), k = g.cellZ(z);
                    if (club(g, i, k) || turf(s, g, i, k, x, z) != Turf.ROUGH) continue;
                    boolean ax = alongX(g, i, k);
                    double a = ax ? x - g.x0(i) : z - g.z0(k), b = ax ? z - g.z0(k) : x - g.x0(i);
                    if (Math.abs(b - centre(g, i, k, a)) < width(g, i, k, a) / 2 + 4 || Math.hypot(a - 62, b - centre(g, i, k, 62)) < 11) continue;
                    int h = surface(s, x, z), top = h + 9 + Hash.range(s, x, z, 22, 4);
                    w.fill(x, h + 1, z, x, top, z, trunk);
                    for (int y = h + 4; y <= top + 1; y++) {
                        int r = Math.max(0, (top + 2 - y) / 2);
                        for (int dx = -r; dx <= r; dx++)
                            for (int dz = -r; dz <= r; dz++)
                                if ((dx != 0 || dz != 0 || y > top) && dx * dx + dz * dz <= r * r + 1 && w.isAir(x + dx, y, z + dz))
                                    w.set(x + dx, y, z + dz, needles);
                    }
                }
        }

        void clubhouseAt(ChunkWriter w, long s, CellGrid g, int i, int k) {
            BlockPos c = clubhouse(g, i, k);
            int cx = c.getX(), cz = c.getZ();
            int y0 = surface(s, cx, cz);
            for (int dx = -10; dx <= 10; dx++)
                for (int dz = -8; dz <= 10; dz++) {
                    int x = cx + dx, z = cz + dz;
                    if (!w.contains(x, z)) continue;
                    w.fill(x, y0 + 1, z, x, y0 + 9, z, ModBlocks.AIR);
                    w.fill(x, y0 - 3, z, x, y0 - 1, z, soil);
                    w.set(x, y0, z, Math.abs(dx) <= 1 && dz > 5 ? path : ModBlocks.state("gc_tee"));
                    boolean in = Math.abs(dx) <= 7 && Math.abs(dz) <= 5;
                    if (!in) continue;
                    boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 5;
                    for (int y = 1; y <= 4; y++) {
                        BlockState b = ModBlocks.AIR;
                        if (edge) {
                            boolean win = (y == 2 || y == 3) && (Math.abs(dx) == 7 ? Math.floorMod(dz, 3) == 1 : Math.floorMod(dx, 3) == 1);
                            b = win ? window : wall;
                        }
                        w.set(x, y0 + y, z, b);
                    }
                    w.set(x, y0 + 5, z, roof);
                    if (edge) {
                        Direction out = Math.abs(dz) == 5 ? (dz > 0 ? Direction.SOUTH : Direction.NORTH) : (dx > 0 ? Direction.EAST : Direction.WEST);
                        w.set(x, y0 + 6, z, ModBlocks.state("gc_club_roof_stairs").setValue(StairBlock.FACING, out.getOpposite()));
                    } else if (Math.abs(dx) <= 5 && Math.abs(dz) <= 3) {
                        w.set(x, y0 + 6, z, roof);
                        if (Math.abs(dx) <= 3 && Math.abs(dz) <= 1) w.set(x, y0 + 7, z, roof);
                    }
                    if (!edge) w.set(x, y0 + 4, z, Math.floorMod(dx, 3) == 0 && Math.floorMod(dz, 3) == 0 ? OutsideKit.light(13) : ModBlocks.AIR);
                }
            w.set(cx, y0 + 1, cz + 5, ModBlocks.state("gc_club_door"));
            w.set(cx, y0 + 2, cz + 5, ModBlocks.state("gc_club_door"));
            for (int c2 = 0; c2 < 4; c2++) w.set(cx - 2 + c2, y0 + 4, cz + 6, OutsideKit.facing("gc_club_sign_" + c2, Direction.SOUTH));
            w.set(cx - 2, y0 + 3, cz + 6, OutsideKit.facing("gc_porch_lamp", Direction.NORTH));
            w.set(cx + 2, y0 + 3, cz + 6, OutsideKit.facing("gc_porch_lamp", Direction.NORTH));
            for (int n = 0; n < 3; n++) {
                int x = cx + 4 + n * 2, z = cz + 8;
                w.set(x, y0 + 1, z, OutsideKit.facing("gc_cart_front", Direction.NORTH));
                w.set(x, y0 + 1, z + 1, OutsideKit.facing("gc_cart_back", Direction.NORTH));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Keeper implements LevelBehavior {
        static final class St {
            int timer = 1800;
            boolean silent;
            UUID keeper;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.timer = 1500 + p.getRandom().nextInt(1500);
            states.put(p.getUUID(), st);
            long s = OutsideKit.seed(level, ID);
            CellGrid g = grid(s);
            int i = g.cellX(p.getBlockX()), k = g.cellZ(p.getBlockZ());
            for (int r = 0; r < 3; r++)
                for (int di = -r; di <= r; di++)
                    for (int dk = -r; dk <= r; dk++) {
                        if (club(g, i + di, k + dk)) continue;
                        BlockPos t = toWorld(g, i + di, k + dk, 10, centre(g, i + di, k + dk, 10));
                        level.getChunk(t);
                        float yaw = alongX(g, i + di, k + dk) ? 180f : 270f;
                        p.teleportTo(level, t.getX() + 0.5, surface(s, t.getX(), t.getZ()) + 2, t.getZ() + 0.5, yaw, 0f);
                        return;
                    }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            long s = OutsideKit.seed(level, ID);
            st.timer--;
            if (!st.silent) {
                if (p.tickCount % 4 == 0) sprinkle(p, level, s, p.tickCount % 52 == 0);
                if (st.timer <= 0 && !p.isCreative() && !p.isSpectator()) {
                    st.silent = true;
                    st.timer = 20 * 50;
                    OutsideKit.sound(level, p.blockPosition(), "event.golf_course_silence", 1.2f, 1f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.4f));
                }
                return;
            }
            if (st.timer == 20 * 45) spawnKeeper(p, level, s, st);
            if (st.timer <= 0) {
                st.silent = false;
                st.timer = 1600 + p.getRandom().nextInt(1600);
                if (st.keeper != null && level.getEntity(st.keeper) instanceof Groundskeeper gk) gk.discard();
            }
        }

        void sprinkle(ServerPlayer p, ServerLevel level, long s, boolean tick) {
            CellGrid g = grid(s);
            int ci = g.cellX(p.getBlockX()), ck = g.cellZ(p.getBlockZ());
            BlockPos nearest = null;
            double nd = 24 * 24;
            for (int i = ci - 1; i <= ci + 1; i++)
                for (int k = ck - 1; k <= ck + 1; k++)
                    for (BlockPos sp : sprinklers(s, g, i, k)) {
                        double d = sp.distSqr(p.blockPosition());
                        if (d < nd) {
                            nd = d;
                            nearest = sp;
                        }
                        if (d < 20 * 20) {
                            double ang = level.getGameTime() * 0.09 + (sp.getX() * 7 + sp.getZ() * 13);
                            double dx = Math.cos(ang), dz = Math.sin(ang);
                            for (int r = 1; r <= 5; r++) {
                                level.sendParticles(ParticleTypes.SPLASH, sp.getX() + 0.5 + dx * r, sp.getY() + 0.6 + (r < 3 ? r * 0.3 : (5 - r) * 0.3),
                                        sp.getZ() + 0.5 + dz * r, 2, 0.1, 0.05, 0.1, 0.1);
                            }
                        }
                    }
            if (tick && nearest != null) OutsideKit.sound(level, nearest, "event.golf_course_sprinkler", 1.4f, 0.95f + p.getRandom().nextFloat() * 0.1f);
        }

        void spawnKeeper(ServerPlayer p, ServerLevel level, long s, St st) {
            for (int attempt = 0; attempt < 30; attempt++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2;
                double d = 20 + p.getRandom().nextDouble() * 12;
                int x = (int) Math.floor(p.getX() + Math.cos(ang) * d), z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
                BlockPos at = new BlockPos(x, surface(s, x, z) + 1, z);
                if (!level.isLoaded(at)) continue;
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos q = at.above(dy);
                    if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir() && Groundskeeper.mown(level, q)
                            && !OutsideKit.seen(p, q, 0.5)) {
                        Groundskeeper gk = Groundskeeper.TYPE.create(level);
                        if (gk == null) return;
                        gk.moveTo(x + 0.5, q.getY(), z + 0.5, 0f, 0f);
                        level.addFreshEntity(gk);
                        st.keeper = gk.getUUID();
                        return;
                    }
                }
            }
        }
    }
}
