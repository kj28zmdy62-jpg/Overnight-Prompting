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
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telephone Pole Forest. No trees, only poles: rows and rows of creosote poles strung
 * with wire, standing in fog over a floor of needles and fern. The wires hum all the
 * time. Every so often the hum climbs into a whine, and then current comes down the
 * poles. When the wires sing, get away from the poles.
 */
public final class PoleForest {
    static final String ID = "pole_forest";
    static final int BASE = 40;
    static final int RZ = 9, RX = 11;

    private PoleForest() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Telephone Pole Forest").order(3070)
                .tagline("A forest of poles in the fog. When the wires sing, get away from them.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Surge())
                .visuals(new Visuals().tint(0.92f, 0.98f, 1.05f).saturation(0.6f).contrast(1.06f).brightness(1.08f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.55f).warp(0.015f)
                        .fog(2f, 30f).fogColor(0x46525C).bloom(0.4f))
                .sanityDrain(0.04f)
                .spawnY(BASE - 6, BASE + 6)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(18, 28))
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.08f).dist(26, 40))
                .exit(ExitType.DOOR, "level_3", 3).exit(ExitType.DOOR, "power_plant", 1)
                .exit(ExitType.DESCEND, "level_2", 2).exit(ExitType.DESCEND, "sewers", 1)
                .exit(ExitType.NOCLIP, "level_3", 2).exit(ExitType.NOCLIP, "static_fields", 1)
                .exit(ExitType.NOCLIP, "snow_plains", 1)
                .build();
    }

    static int surface(long s, int x, int z) {
        return BASE + (int) Math.floor((Hash.fbm(s ^ 0x9F0L, x / 52.0, z / 52.0, 3) - 0.5f) * 7f);
    }

    static int rowZ(long s, int r) {
        return r * RZ + Hash.range(s, 31, r, 3);
    }

    static int poleX(long s, int r, int n) {
        return n * RX + Hash.range(s, r, n, 5);
    }

    /** 0 standing, 1 missing, 2 snapped. */
    static int state(long s, int r, int n) {
        float u = Hash.unit(s, r, n, 7);
        return u < 0.05f ? 1 : u < 0.08f ? 2 : 0;
    }

    static int top(long s, int r) {
        return BASE + 11 + (Math.floorMod(r, 3) == 1 ? 2 : 0);
    }

    static CellGrid sites(long s) {
        return new CellGrid(s ^ 0x5B5L, 72, 16);
    }

    /** Substations and cable vaults sit in clearings; poles are not planted there. */
    static int site(long s, CellGrid g, int i, int k) {
        float u = g.unit(i, k, 1);
        return u < 0.22f ? 1 : u < 0.4f ? 2 : 0;
    }

    static int siteX(CellGrid g, int i, int k) {
        return g.x0(i) + 20 + Hash.range(g.hash(i, k, 2), 30);
    }

    static int siteZ(CellGrid g, int i, int k) {
        return g.z0(k) + 20 + Hash.range(g.hash(i, k, 3), 30);
    }

    static boolean inClearing(long s, int x, int z) {
        CellGrid g = sites(s);
        int i = g.cellX(x), k = g.cellZ(z);
        if (site(s, g, i, k) == 0) return false;
        return Math.abs(x - siteX(g, i, k)) <= 7 && Math.abs(z - siteZ(g, i, k)) <= 7;
    }

    static final class Gen implements LevelGenerator {
        final BlockState litter = ModBlocks.state("pf_litter");
        final BlockState moss = ModBlocks.state("pf_moss");
        final BlockState soil = ModBlocks.state("pf_soil");
        final BlockState fern = ModBlocks.state("pf_fern");
        final BlockState brush = ModBlocks.state("pf_brush");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState live = ModBlocks.state("pf_live_wire");
        final BlockState wall = ModBlocks.state("pf_substation_wall");
        final BlockState fence = ModBlocks.state("chain_link_fence");
        final BlockState concrete = ModBlocks.state("warehouse_concrete");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int h = surface(s, x, z);
                    w.fill(x, 0, z, x, h - 4, z, sub);
                    w.fill(x, h - 3, z, x, h - 1, z, soil);
                    boolean m = Hash.fbm(s ^ 0x3055L, x / 18.0, z / 18.0, 3) > 0.58f;
                    w.set(x, h, z, m ? moss : litter);
                    float r = Hash.unit(s, x, z, 3);
                    if (r < 0.13f) w.set(x, h + 1, z, fern);
                    else if (r < 0.17f) w.set(x, h + 1, z, brush);
                }
            int r0 = Math.floorDiv(w.minZ - 4, RZ) - 1, r1 = Math.floorDiv(w.maxZ, RZ) + 1;
            for (int r = r0; r <= r1; r++) {
                int z = rowZ(s, r);
                if (z < w.minZ - 6 || z > w.maxZ + 6) continue;
                int n0 = Math.floorDiv(w.minX, RX) - 2, n1 = Math.floorDiv(w.maxX, RX) + 2;
                for (int n = n0; n <= n1; n++) {
                    pole(w, s, r, n, z);
                    span(w, s, r, n, z);
                }
            }
            drawSites(w, s);
        }

        void pole(ChunkWriter w, long s, int r, int n, int z) {
            int x = poleX(s, r, n);
            int st = state(s, r, n);
            if (st == 1 || inClearing(s, x, z)) return;
            int h = surface(s, x, z), top = top(s, r);
            if (st == 2) {
                w.set(x, h + 1, z, OutsideKit.facing("pf_pole", Direction.NORTH));
                w.set(x, h + 2, z, OutsideKit.facing("pf_pole_stump", Direction.NORTH));
                // the rest of it lies in the ferns
                for (int k = 1; k <= 7; k++) {
                    int zz = z + k;
                    if (w.contains(x, zz)) {
                        int hh = surface(s, x, zz);
                        w.set(x, hh + 1, zz, OutsideKit.facing("pf_pole_fallen", Direction.EAST));
                    }
                }
                return;
            }
            float u = Hash.unit(s, r, n, 11);
            for (int y = h + 1; y < top; y++) {
                String b = y == h + 3 && u < 0.08f ? "pf_pole_tagged" : y == h + 3 && u < 0.1f ? "pf_pole_tagged2" : "pf_pole";
                if (y == top - 3 && u > 0.9f) b = "pf_transformer";
                if (y == top - 3 && u <= 0.9f && Math.floorMod(r, 2) == 0) b = "pf_crossarm";
                w.set(x, y, z, OutsideKit.facing(b, Direction.EAST));
            }
            w.set(x, top, z, OutsideKit.facing("pf_crossarm", Direction.EAST));
        }

        /** Wire from pole n to pole n+1 in row r, sagging in the middle, or dragged to the ground by a snapped pole. */
        void span(ChunkWriter w, long s, int r, int n, int z) {
            int xa = poleX(s, r, n), xb = poleX(s, r, n + 1);
            int sa = state(s, r, n), sb = state(s, r, n + 1);
            if (sa == 1) {
                return;
            }
            if (sb == 1) xb = poleX(s, r, n + 2);
            if (inClearing(s, xa, z) || inClearing(s, xb, z)) return;
            int top = top(s, r);
            boolean twoTier = Math.floorMod(r, 2) == 0;
            int len = xb - xa;
            for (int x = xa + 1; x < xb; x++) {
                if (!w.contains(x, z)) continue;
                int i = x - xa;
                if (sa == 2 || sb == 2) {
                    // one end is down: the line slopes into the undergrowth and sparks there
                    float t = sa == 2 ? 1f - (float) i / len : (float) i / len;
                    int ground = surface(s, x, z);
                    int y = Math.round(top - t * (top - ground - 1));
                    if (y <= ground + 1) {
                        w.set(x, ground, z, live);
                        if (!w.isAir(x, ground + 1, z)) w.set(x, ground + 1, z, ModBlocks.AIR);
                    } else {
                        w.set(x, y, z, OutsideKit.facing("pf_wire", Direction.EAST));
                    }
                    continue;
                }
                boolean sag = len > 6 && i > len / 3 && i < len - len / 3;
                String b = sag ? "pf_wire_sag" : "pf_wire";
                w.set(x, top, z, OutsideKit.facing(b, Direction.EAST));
                if (twoTier) w.set(x, top - 3, z, OutsideKit.facing(b, Direction.EAST));
            }
        }

        void drawSites(ChunkWriter w, long s) {
            CellGrid g = sites(s);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                int type = site(s, g, i, k);
                if (type == 0) return;
                int cx = siteX(g, i, k), cz = siteZ(g, i, k);
                int y = surface(s, cx, cz);
                // level the clearing
                for (int dx = -7; dx <= 7; dx++)
                    for (int dz = -7; dz <= 7; dz++) {
                        int x = cx + dx, z = cz + dz;
                        if (!w.contains(x, z)) continue;
                        w.fill(x, y + 1, z, x, y + 8, z, ModBlocks.AIR);
                        w.fill(x, y - 3, z, x, y - 1, z, soil);
                        w.set(x, y, z, litter);
                        if (Hash.unit(s, x, z, 3) < 0.08f) w.set(x, y + 1, z, fern);
                    }
                if (type == 1) {
                    // a fenced substation hut, humming
                    for (int dx = -5; dx <= 5; dx++)
                        for (int dz = -5; dz <= 5; dz++) {
                            int x = cx + dx, z = cz + dz;
                            w.set(x, y, z, concrete);
                            w.set(x, y + 1, z, ModBlocks.AIR);
                            boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                            if (edge && !(dz == 5 && Math.abs(dx) <= 1)) {
                                w.set(x, y + 1, z, fence);
                                w.set(x, y + 2, z, fence);
                            }
                            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && dz <= 0) {
                                boolean hutEdge = Math.abs(dx) == 2 || dz == -2 || dz == 0;
                                for (int yy = 1; yy <= 3; yy++) w.set(x, y + yy, z, hutEdge ? wall : ModBlocks.AIR);
                                w.set(x, y + 4, z, wall);
                            }
                        }
                    w.set(cx, y + 1, cz, ModBlocks.state("pf_substation_door"));
                    w.set(cx, y + 2, cz, ModBlocks.state("pf_substation_door"));
                    w.set(cx + 3, y + 1, cz + 3, OutsideKit.facing("pf_transformer", Direction.NORTH));
                    w.set(cx - 3, y + 1, cz + 3, OutsideKit.facing("pf_transformer", Direction.SOUTH));
                } else {
                    // a cable vault: a concrete pad with an iron cover
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++) {
                            w.set(cx + dx, y, cz + dz, concrete);
                            w.set(cx + dx, y + 1, cz + dz, ModBlocks.AIR);
                        }
                    w.set(cx, y, cz, ModBlocks.state("pf_manhole"));
                    w.set(cx + 2, y + 1, cz, OutsideKit.facing("pf_pole_stump", Direction.NORTH));
                }
            });
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Surge implements LevelBehavior {
        static final class St {
            int next = 1200;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.next = 900 + p.getRandom().nextInt(900);
            states.put(p.getUUID(), st);
            if (OutsideKit.autotest()) {
                long s = OutsideKit.seed(level, ID);
                int r = Math.floorDiv(p.getBlockZ(), RZ);
                int z = rowZ(s, r) + 3, x = p.getBlockX();
                level.getChunk(new BlockPos(x, BASE, z));
                p.teleportTo(level, x + 0.5, surface(s, x, z) + 1, z + 0.5, 180f + 35f, -8f);
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            long s = OutsideKit.seed(level, ID);
            st.next--;
            if (st.next == 80) {
                OutsideKit.sound(level, p.blockPosition(), "event.pole_forest_rise", 1.6f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.3f));
            }
            if (st.next > 0 && st.next < 80 && st.next % 10 == 0) sparkNear(level, s, p, 4);
            if (st.next <= 0) {
                st.next = 1000 + p.getRandom().nextInt(1400);
                BlockPos pole = nearestPole(s, p, 3);
                double d = pole == null ? 99 : Math.sqrt(Math.pow(pole.getX() + 0.5 - p.getX(), 2) + Math.pow(pole.getZ() + 0.5 - p.getZ(), 2));
                sparkNear(level, s, p, 12);
                if (d < 3.2 && !p.isCreative() && !p.isSpectator()) {
                    OutsideKit.sound(level, pole, "event.pole_forest_zap", 2f, 1f);
                    p.hurt(level.damageSources().lightningBolt(), 6f);
                    Sanity.add(p, -10f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.9f));
                } else {
                    OutsideKit.sound(level, p.blockPosition(), "event.pole_forest_zap", 0.8f, 0.7f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.25f));
                }
            }
            if (p.tickCount % 80 == 0) {
                // transformers hum when you are near
                BlockPos t = nearestPole(s, p, 2);
                if (t != null && Hash.unit(s, Math.floorDiv(t.getZ(), RZ), Math.floorDiv(t.getX(), RX), 11) > 0.9f) {
                    OutsideKit.sound(level, t.above(top(s, Math.floorDiv(t.getZ(), RZ)) - 3 - t.getY()), "event.pole_forest_hum", 1.2f, 1f);
                }
            }
        }

        /** Sparks fly from the crossarms of the poles around the player. */
        static void sparkNear(ServerLevel level, long s, ServerPlayer p, int count) {
            int pr = Math.floorDiv(p.getBlockZ(), RZ);
            for (int r = pr - 2; r <= pr + 2; r++) {
                int z = rowZ(s, r);
                int pn = Math.floorDiv(p.getBlockX(), RX);
                for (int n = pn - 2; n <= pn + 2; n++) {
                    if (state(s, r, n) != 0) continue;
                    int x = poleX(s, r, n);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, top(s, r) + 0.8, z + 0.5, count, 0.6, 0.2, 0.6, 0.2);
                    if (count > 6) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, surface(s, x, z) + 2, z + 0.5, count, 0.2, 1.5, 0.2, 0.1);
                }
            }
        }

        /** Nearest standing pole (its base), searching a few rows and columns around the player. */
        static BlockPos nearestPole(long s, ServerPlayer p, int reach) {
            int pr = Math.floorDiv(p.getBlockZ(), RZ), pn = Math.floorDiv(p.getBlockX(), RX);
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (int r = pr - reach; r <= pr + reach; r++) {
                int z = rowZ(s, r);
                for (int n = pn - reach; n <= pn + reach; n++) {
                    if (state(s, r, n) != 0) continue;
                    int x = poleX(s, r, n);
                    if (inClearing(s, x, z)) continue;
                    double d = Math.pow(x + 0.5 - p.getX(), 2) + Math.pow(z + 0.5 - p.getZ(), 2);
                    if (d < bd) {
                        bd = d;
                        best = new BlockPos(x, surface(s, x, z) + 1, z);
                    }
                }
            }
            return best;
        }
    }
}
