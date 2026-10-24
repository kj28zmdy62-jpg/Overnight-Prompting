package com.noclip.levels;

import com.noclip.entity.Seeker;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Foggy Playground. Fenced playgrounds one after another in a white fog, swing sets and
 * slides and a merry-go-round, faded paint, wood chips. The swings are swinging and the
 * merry-go-round is turning, and there is no wind and nobody on them. Every few minutes
 * a music box somewhere counts down from ten. Then something small in a red hood comes
 * looking for you. Hide, and crouch.
 */
public final class FoggyPlayground {
    static final String ID = "foggy_playground";
    static final int G = 40;
    static final int UNIT = 40;

    private FoggyPlayground() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Foggy Playground").order(3110)
                .tagline("The swings are moving. There is no wind. When the music box counts to ten, hide.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new HideAndSeek())
                .visuals(new Visuals().tint(1.0f, 1.0f, 1.02f).saturation(0.72f).contrast(0.92f).brightness(1.08f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.42f).warp(0.012f)
                        .fog(1f, 19f).fogColor(0xC4C8C6).bloom(0.35f))
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .exit(ExitType.DESCEND, "playground", 3).exit(ExitType.DESCEND, "daycare", 1).exit(ExitType.DESCEND, "water_park", 1)
                .exit(ExitType.SECRET, "level_fun", 2).exit(ExitType.SECRET, "daycare", 2).exit(ExitType.SECRET, "playground", 1)
                .exit(ExitType.NOCLIP, "playground", 2).exit(ExitType.NOCLIP, "golden_hour", 1).exit(ExitType.NOCLIP, "silent_beach", 1)
                .build();
    }

    static CellGrid grid(long s) {
        return new CellGrid(s ^ 0xF09L, UNIT, 0);
    }

    static boolean playground(CellGrid g, int i, int k) {
        return g.unit(i, k, 1) < 0.78f;
    }

    static boolean tubeSlide(CellGrid g, int i, int k) {
        return g.unit(i, k, 2) < 0.14f;
    }

    static final int[] SWINGS = {10, 12, 14};
    static final int SWING_Z = 10;
    static final int MGR_X = 27, MGR_Z = 11;
    static final int SAW_X = 28, SAW_Z = 25;
    static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    static final class Gen implements LevelGenerator {
        final BlockState grass = ModBlocks.state("fp_grass");
        final BlockState soil = ModBlocks.state("fp_soil");
        final BlockState chips = ModBlocks.state("fp_chips");
        final BlockState rubber = ModBlocks.state("fp_rubber");
        final BlockState path = ModBlocks.state("fp_path");
        final BlockState sand = ModBlocks.state("fp_sand");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState fence = ModBlocks.state("fp_fence");
        final BlockState trunk = ModBlocks.state("fp_trunk");
        final BlockState branches = ModBlocks.state("fp_branches");
        final BlockState climber = ModBlocks.state("fp_climber");
        final BlockState tube = ModBlocks.state("fp_tube");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.fill(x, 0, z, x, G - 3, z, sub);
                    w.fill(x, G - 2, z, x, G - 1, z, soil);
                    int i = g.cellX(x), k = g.cellZ(z);
                    int u = x - g.x0(i), v = z - g.z0(k);
                    boolean pg = playground(g, i, k);
                    BlockState top = grass;
                    if (u == 1 || v == 1 || u == 2 || v == 2) top = path;
                    if (pg && u >= 4 && u <= 35 && v >= 4 && v <= 35) {
                        top = chips;
                        if (u >= 8 && u <= 16 && v >= 7 && v <= 13) top = rubber;
                        if (u >= 6 && u <= 9 && v >= 29 && v <= 32) top = sand;
                    }
                    w.set(x, G, z, top);
                    if (pg && (u == 4 || u == 35 || v == 4 || v == 35) && u >= 4 && u <= 35 && v >= 4 && v <= 35) {
                        boolean gate = (v == 35 && (u == 19 || u == 20)) || (v == 4 && (u == 19 || u == 20));
                        boolean tubeGap = tubeSlide(g, i, k) && v == 35 && u == 12;
                        if (!gate && !tubeGap) {
                            boolean alongX = v == 4 || v == 35;
                            w.set(x, G + 1, z, fence.setValue(IronBarsBlock.EAST, alongX).setValue(IronBarsBlock.WEST, alongX)
                                    .setValue(IronBarsBlock.NORTH, !alongX).setValue(IronBarsBlock.SOUTH, !alongX));
                        }
                    }
                    if (top == grass && Hash.unit(s, x, z, 7) < 0.004f) tree(w, x, z);
                }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (playground(g, i, k)) equipment(w, s, g, i, k, x0, z0);
                else {
                    w.set(x0 + 20, G + 1, z0 + 2, OutsideKit.facing("fp_bench", Direction.SOUTH));
                }
            });
        }

        void tree(ChunkWriter w, int x, int z) {
            w.fill(x, G + 1, z, x, G + 4, z, trunk);
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int y = G + 4; y <= G + 6; y++)
                        if ((dx != 0 || dz != 0 || y > G + 4) && w.isAir(x + dx, y, z + dz)) w.set(x + dx, y, z + dz, branches);
        }

        void equipment(ChunkWriter w, long s, CellGrid g, int i, int k, int x0, int z0) {
            // swing set: A-frame legs, a bar along x, three swings
            for (int y = G + 1; y <= G + 2; y++) {
                w.set(x0 + 8, y, z0 + SWING_Z, OutsideKit.facing("fp_swing_leg", Direction.NORTH));
                w.set(x0 + 16, y, z0 + SWING_Z, OutsideKit.facing("fp_swing_leg", Direction.NORTH));
            }
            w.set(x0 + 8, G + 3, z0 + SWING_Z, OutsideKit.facing("fp_swing_apex", Direction.NORTH));
            w.set(x0 + 16, G + 3, z0 + SWING_Z, OutsideKit.facing("fp_swing_apex", Direction.NORTH));
            for (int x = x0 + 9; x <= x0 + 15; x++) w.set(x, G + 3, z0 + SWING_Z, OutsideKit.facing("fp_swing_bar", Direction.NORTH));
            for (int sx : SWINGS) w.set(x0 + sx, G + 2, z0 + SWING_Z, ModBlocks.state("fp_swing_mid"));
            // merry-go-round
            w.set(x0 + MGR_X, G + 1, z0 + MGR_Z, OutsideKit.facing("fp_mgr_hub", Direction.NORTH));
            for (int n = 0; n < 8; n++) {
                int[] r = RING[n];
                w.set(x0 + MGR_X + r[0], G + 1, z0 + MGR_Z + r[1], OutsideKit.facing(n % 2 == 0 ? "fp_mgr_handle" : "fp_mgr_deck", Direction.NORTH));
            }
            // play structure with a slide and a climber
            for (int dx = 0; dx <= 4; dx += 4)
                for (int dz = 0; dz <= 4; dz += 4)
                    for (int y = G + 1; y <= G + 4; y++) w.set(x0 + 11 + dx, y, z0 + 23 + dz, OutsideKit.facing("fp_post", Direction.NORTH));
            for (int dx = 0; dx <= 4; dx++)
                for (int dz = 0; dz <= 4; dz++) {
                    boolean corner = (dx == 0 || dx == 4) && (dz == 0 || dz == 4);
                    if (!corner) w.set(x0 + 11 + dx, G + 2, z0 + 23 + dz, OutsideKit.facing("fp_deck", Direction.NORTH));
                    if ((dz == 0 || dz == 4) && !corner && dx != 2) w.set(x0 + 11 + dx, G + 3, z0 + 23 + dz, OutsideKit.facing("fp_rail", Direction.NORTH));
                }
            for (int n = 0; n < 3; n++) w.set(x0 + 16 + n, G + 2 - n, z0 + 25, OutsideKit.facing("fp_slide", Direction.WEST));
            for (int y = G + 1; y <= G + 2; y++) w.set(x0 + 10, y, z0 + 25, climber);
            for (int dx = 0; dx < 3; dx++)
                for (int dz = 0; dz < 2; dz++)
                    for (int y = G + 1; y <= G + 2; y++) if ((dx + dz + y) % 2 == 0) w.set(x0 + 21 + dx, y, z0 + 29 + dz, climber);
            if (tubeSlide(g, i, k)) {
                int[][] tubeCells = {{13, 2, 28}, {13, 2, 29}, {13, 2, 30}, {13, 1, 31}, {13, 1, 32}, {12, 1, 33}, {12, 1, 34}};
                for (int[] c : tubeCells) w.set(x0 + c[0], G + c[1], z0 + c[2], OutsideKit.facing(tube, Direction.NORTH));
                w.set(x0 + 12, G + 1, z0 + 35, ModBlocks.state("fp_tube_end"));
                w.set(x0 + 12, G + 2, z0 + 35, ModBlocks.state("fp_tube_end"));
            }
            // seesaw, spring riders, sandbox
            w.set(x0 + SAW_X, G + 1, z0 + SAW_Z, OutsideKit.facing("fp_seesaw_mid", Direction.NORTH));
            w.set(x0 + SAW_X, G + 1, z0 + SAW_Z - 1, OutsideKit.facing("fp_seesaw_up", Direction.NORTH));
            w.set(x0 + SAW_X, G + 1, z0 + SAW_Z + 1, OutsideKit.facing("fp_seesaw_down", Direction.NORTH));
            w.set(x0 + 31, G + 1, z0 + 29, OutsideKit.facing("fp_spring_duck", Direction.WEST));
            w.set(x0 + 27, G + 1, z0 + 31, OutsideKit.facing("fp_spring_duck", Direction.NORTH));
            w.set(x0 + 7, G + 1, z0 + 30, OutsideKit.facing("fp_pail", Direction.from2DDataValue(Hash.range(g.hash(i, k, 5), 4))));
            w.set(x0 + 22, G + 1, z0 + 37, OutsideKit.facing("fp_bench", Direction.NORTH));
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class HideAndSeek implements LevelBehavior {
        static final class St {
            int timer = 1500;
            int phase;
            UUID seeker;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        HideAndSeek() {
            Seeker.onTag = p -> {
                OutsideKit.sound(p.serverLevel(), p.blockPosition(), "event.foggy_playground_found", 1.6f, 0.8f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                Sanity.add(p, -15f);
                Travel.exit(p, ExitType.SECRET, p.blockPosition());
            };
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.timer = 1200 + p.getRandom().nextInt(1200);
            states.put(p.getUUID(), st);
            long s = OutsideKit.seed(level, ID);
            CellGrid g = grid(s);
            int i = g.cellX(p.getBlockX()), k = g.cellZ(p.getBlockZ());
            for (int r = 0; r < 3; r++)
                for (int di = -r; di <= r; di++)
                    for (int dk = -r; dk <= r; dk++)
                        if (playground(g, i + di, k + dk)) {
                            int x0 = g.x0(i + di), z0 = g.z0(k + dk);
                            level.getChunk(new BlockPos(x0, G, z0));
                            p.teleportTo(level, x0 + 20.5, G + 1, z0 + 19.5, OutsideKit.autotest() ? 60f : p.getYRot(), 0f);
                            return;
                        }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long s = OutsideKit.seed(level, ID);
            if (p.tickCount % 5 == 0) animate(p, level, s);
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            if (p.isCreative() || p.isSpectator()) return;
            st.timer--;
            switch (st.phase) {
                case 0 -> {
                    if (st.timer <= 0) {
                        st.phase = 1;
                        st.timer = 200;
                        BlockPos c = nearestPlayground(s, p);
                        OutsideKit.sound(level, c == null ? p.blockPosition() : c, "event.foggy_playground_count", 3.5f, 1f);
                    }
                }
                case 1 -> {
                    if (st.timer <= 0) {
                        st.phase = 2;
                        st.timer = 20 * 52;
                        spawnSeeker(p, level, st);
                    }
                }
                default -> {
                    boolean gone = st.seeker == null || !(level.getEntity(st.seeker) instanceof Seeker sk) || sk.isRemoved();
                    if (gone || st.timer <= 0) {
                        if (!gone && level.getEntity(st.seeker) instanceof Seeker sk) {
                            OutsideKit.sound(level, sk.blockPosition(), "ambient.foggy_playground.additions", 1.5f, 1.3f);
                            sk.discard();
                        }
                        if (st.timer <= 0) Sanity.add(p, 10f);
                        st.phase = 0;
                        st.timer = 1500 + p.getRandom().nextInt(1500);
                    }
                }
            }
        }

        BlockPos nearestPlayground(long s, ServerPlayer p) {
            CellGrid g = grid(s);
            int i = g.cellX(p.getBlockX()), k = g.cellZ(p.getBlockZ());
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (int di = -1; di <= 1; di++)
                for (int dk = -1; dk <= 1; dk++) {
                    if (!playground(g, i + di, k + dk)) continue;
                    BlockPos c = new BlockPos(g.x0(i + di) + 20, G + 2, g.z0(k + dk) + 20);
                    double d = c.distSqr(p.blockPosition());
                    if (d < bd && d > 12 * 12) {
                        bd = d;
                        best = c;
                    }
                }
            return best;
        }

        void spawnSeeker(ServerPlayer p, ServerLevel level, St st) {
            for (int attempt = 0; attempt < 20; attempt++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2, d = 18 + p.getRandom().nextDouble() * 8;
                BlockPos at = BlockPos.containing(p.getX() + Math.cos(ang) * d, G + 1, p.getZ() + Math.sin(ang) * d);
                if (!level.isLoaded(at) || !level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                Seeker sk = Seeker.TYPE.create(level);
                if (sk == null) return;
                sk.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                sk.seek(p);
                level.addFreshEntity(sk);
                st.seeker = sk.getUUID();
                return;
            }
        }

        /** Swings swing, the merry-go-round turns, the seesaw rocks: nobody on any of them. */
        void animate(ServerPlayer p, ServerLevel level, long s) {
            CellGrid g = grid(s);
            int ci = g.cellX(p.getBlockX()), ck = g.cellZ(p.getBlockZ());
            long t = level.getGameTime() / 5;
            String[] cycle = {"fp_swing_mid", "fp_swing_fwd", "fp_swing_mid", "fp_swing_back"};
            for (int i = ci - 1; i <= ci + 1; i++)
                for (int k = ck - 1; k <= ck + 1; k++) {
                    if (!playground(g, i, k)) continue;
                    int x0 = g.x0(i), z0 = g.z0(k);
                    if (new BlockPos(x0 + 20, G, z0 + 20).distSqr(p.blockPosition()) > 48 * 48) continue;
                    for (int n = 0; n < SWINGS.length; n++) {
                        long ph = t + n * (1 + Hash.range(g.hash(i, k, 30 + n), 3));
                        if (Hash.unit(g.hash(i, k, 40 + n)) < 0.3f) ph = 0;
                        put(level, x0 + SWINGS[n], G + 2, z0 + SWING_Z, ModBlocks.state(cycle[(int) Math.floorMod(ph, 4)]));
                    }
                    int step = (int) Math.floorMod(t / 2, 8);
                    for (int n = 0; n < 8; n++) {
                        int[] r = RING[n];
                        boolean handle = Math.floorMod(n - step, 2) == 0;
                        put(level, x0 + MGR_X + r[0], G + 1, z0 + MGR_Z + r[1], OutsideKit.facing(handle ? "fp_mgr_handle" : "fp_mgr_deck", Direction.NORTH));
                    }
                    boolean tilt = Math.floorMod(t / 8, 2) == 0;
                    put(level, x0 + SAW_X, G + 1, z0 + SAW_Z - 1, OutsideKit.facing(tilt ? "fp_seesaw_up" : "fp_seesaw_down", Direction.NORTH));
                    put(level, x0 + SAW_X, G + 1, z0 + SAW_Z + 1, OutsideKit.facing(tilt ? "fp_seesaw_down" : "fp_seesaw_up", Direction.NORTH));
                }
        }

        static void put(ServerLevel level, int x, int y, int z, BlockState st) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.isLoaded(pos) && level.getBlockState(pos) != st) level.setBlock(pos, st, Block.UPDATE_CLIENTS);
        }
    }
}
