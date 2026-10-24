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
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endless Highway. A divided highway at night, lit in pools of sodium orange with long
 * dark stretches between. There are no cars. Mile markers count down toward an exit
 * that is always a few miles ahead; at mile zero a ramp drops away into an underpass.
 * Every so often you hear something coming up the road behind you, fast. You never see
 * it. Be off the lanes when it passes.
 */
public final class EndlessHighway {
    static final String ID = "endless_highway";
    static final int G = 40;
    /** Highways run along x and repeat every PZ blocks along z, so you always arrive near one. */
    static final int PZ = 128;
    static final int MILE = 96;

    private EndlessHighway() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Endless Highway").order(3020)
                .tagline("No cars. The mile markers count down. Something is coming up behind you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Traffic())
                .visuals(new Visuals().tint(1.16f, 0.93f, 0.68f).saturation(0.42f).contrast(1.1f).brightness(1.4f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.6f).warp(0.012f)
                        .fog(3f, 46f).fogColor(0x140C06).bloom(0.75f))
                .sanityDrain(0.035f)
                .spawnY(G - 4, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(34, 46))
                .exit(ExitType.DESCEND, "storm_drains", 2).exit(ExitType.DESCEND, "parking_garage", 2)
                .exit(ExitType.DESCEND, "subway", 1)
                .exit(ExitType.NOCLIP, "gas_station", 3).exit(ExitType.NOCLIP, "parking_lot", 2)
                .exit(ExitType.NOCLIP, "rain_city", 1).exit(ExitType.NOCLIP, "motel", 1)
                .build();
    }

    static int zl(int z) {
        return Math.floorMod(z + PZ / 2, PZ) - PZ / 2;
    }

    /** 9 down to 0 travelling east; the exit is in the mile-0 stretch. */
    static int mile(int x) {
        return 9 - Math.floorMod(Math.floorDiv(x, MILE), 10);
    }

    static int ground(int zl) {
        int a = Math.abs(zl);
        if (a <= 13) return G;
        if (a <= 19) return G - Math.min(3, (a - 12) / 2);
        if (a <= 24) return G - 3 + (a - 19) / 2;
        return G - 1;
    }

    static boolean lampAt(int x, int zl) {
        return zl == 13 && Math.floorMod(x, 32) == 0 || zl == -13 && Math.floorMod(x, 32) == 16;
    }

    static final class Gen implements LevelGenerator {
        final BlockState asphalt = ModBlocks.state("hw_asphalt");
        final BlockState dash = ModBlocks.state("hw_asphalt_dash");
        final BlockState edge = ModBlocks.state("hw_asphalt_edge");
        final BlockState yellow = ModBlocks.state("hw_asphalt_yellow");
        final BlockState gravel = ModBlocks.state("hw_gravel");
        final BlockState verge = ModBlocks.state("hw_verge");
        final BlockState grass = ModBlocks.state("hw_tall_grass");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState culvert = ModBlocks.state("hw_culvert");
        final BlockState underpass = ModBlocks.state("hw_underpass");
        final BlockState lampPole = ModBlocks.state("hw_lamp_pole");
        final BlockState truss = ModBlocks.state("hw_truss");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int zl = zl(z), a = Math.abs(zl);
                    int y = ground(zl);
                    w.fill(x, 0, z, x, y - 1, z, sub);
                    BlockState top;
                    if (a == 0) top = asphalt;
                    else if (a == 1) top = yellow;
                    else if (a <= 9) top = a == 5 && Math.floorMod(x, 8) < 4 ? dash : asphalt;
                    else if (a == 10) top = edge;
                    else if (a == 11) top = asphalt;
                    else if (a <= 13) top = gravel;
                    else top = verge;
                    w.set(x, y, z, top);
                    if (a == 0) w.set(x, G + 1, z, OutsideKit.facing("hw_barrier", Direction.EAST));
                    if (a == 12) {
                        boolean post = Math.floorMod(x, 4) == 0;
                        Direction face = zl > 0 ? Direction.NORTH : Direction.SOUTH;
                        w.set(x, G + 1, z, OutsideKit.facing(post ? "hw_guard_post" : "hw_guard_rail", face));
                    }
                    if (a > 14 && Hash.unit(s, x, z, 5) < 0.3f) w.set(x, y + 1, z, grass);
                    if (lampAt(x, zl)) lamp(w, s, x, z, zl);
                    if (zl == 11 && Math.floorMod(x, MILE) == 48) {
                        w.set(x, G + 1, z, OutsideKit.facing("hw_mile_" + mile(x), Direction.EAST));
                    }
                }
            int m0 = Math.floorDiv(w.minX, MILE) - 1, m1 = Math.floorDiv(w.maxX, MILE) + 1;
            int r0 = Math.floorDiv(w.minZ + PZ / 2, PZ) - 1, r1 = Math.floorDiv(w.maxZ + PZ / 2, PZ) + 1;
            for (int m = m0; m <= m1; m++)
                for (int r = r0; r <= r1; r++) {
                    int x0 = m * MILE, zc = r * PZ;
                    int n = mile(x0);
                    if (n == 1) gantry(w, x0 + 48, zc, "hw_sign_next");
                    if (n == 0) {
                        gantry(w, x0 + 6, zc, "hw_sign_exit");
                        ramp(w, x0, zc);
                    }
                    long h = Hash.hash(s, m, r, 3);
                    if (Hash.unit(h) < 0.25f && n != 0) {
                        // a car pulled onto the shoulder, flares burning behind it, nobody inside
                        int cx = x0 + 20 + Hash.range(h >>> 8, 50);
                        OutsideKit.car(w, cx, G + 1, zc + 10, Direction.EAST, (h & 1) == 0 ? "blue" : "red", false);
                        w.set(cx - 7, G + 1, zc + 8, ModBlocks.state("hw_flare"));
                        w.set(cx - 12, G + 1, zc + 7, ModBlocks.state("hw_flare"));
                    }
                }
        }

        void lamp(ChunkWriter w, long s, int x, int z, int zl) {
            int dir = zl > 0 ? -1 : 1;
            boolean dead = Hash.unit(s, x, z, 9) < 0.12f;
            for (int y = G + 1; y <= G + 9; y++) w.set(x, y, z, lampPole);
            for (int k = 0; k <= 5; k++) w.set(x, G + 10, z + dir * k, OutsideKit.facing("hw_lamp_arm", Direction.NORTH));
            Direction face = dir < 0 ? Direction.NORTH : Direction.SOUTH;
            w.set(x, G + 10, z + dir * 6, OutsideKit.facing(dead ? "hw_lamp_head_dead" : "hw_lamp_head", face));
            if (!dead) {
                // a pool of light on the lanes below
                w.set(x, G + 5, z + dir * 6, OutsideKit.light(15));
                w.set(x - 4, G + 3, z + dir * 6, OutsideKit.light(11));
                w.set(x + 4, G + 3, z + dir * 6, OutsideKit.light(11));
                w.set(x, G + 3, z + dir * 10, OutsideKit.light(10));
                w.set(x, G + 3, z + dir * 2, OutsideKit.light(10));
            }
        }

        /** Overhead sign across the eastbound lanes. */
        void gantry(ChunkWriter w, int x, int zc, String sign) {
            for (int y = G + 1; y <= G + 10; y++) {
                w.set(x, y, zc + 12, truss);
                w.set(x, y, zc + 1, truss);
            }
            for (int zl = 1; zl <= 12; zl++) w.set(x, G + 10, zc + zl, truss);
            for (int c = 0; c < 4; c++)
                for (int r = 0; r < 2; r++)
                    w.set(x - 1, G + 9 - r, zc + 4 + c, OutsideKit.facing(sign + "_" + c + "_" + r, Direction.WEST));
        }

        /** The exit: a ramp dropping off the right shoulder into a dark underpass. */
        void ramp(ChunkWriter w, int x0, int zc) {
            for (int dx = 0; dx <= 44; dx++) {
                int x = x0 + 20 + dx;
                int y = G - Math.min(16, dx / 2);
                for (int zl = 12; zl <= 18; zl++) {
                    int z = zc + zl;
                    boolean wall = zl == 12 || zl == 18;
                    if (wall && dx > 1) {
                        w.fill(x, y + 1, z, x, G + 1, z, culvert);
                        continue;
                    }
                    if (zl == 12) {
                        w.fill(x, G + 1, z, x, G + 3, z, ModBlocks.AIR);
                        continue;
                    }
                    w.fill(x, y + 1, z, x, G + 4, z, ModBlocks.AIR);
                    w.set(x, y, z, zl == 13 || zl == 17 ? edge : asphalt);
                    w.fill(x, y - 3, z, x, y - 1, z, sub);
                }
            }
            // the culvert mouth at the bottom
            int xe = x0 + 20 + 34;
            for (int dx = 0; dx <= 10; dx++)
                for (int zl = 12; zl <= 18; zl++) {
                    int x = xe + dx, z = zc + zl;
                    int y = G - 16;
                    boolean inside = zl > 12 && zl < 18;
                    w.fill(x, y + 1, z, x, G, z, inside ? ModBlocks.AIR : culvert);
                    if (inside) {
                        w.set(x, y + 5, z, culvert);
                        w.fill(x, y + 6, z, x, G, z, sub);
                        if (dx >= 6) w.fill(x, y + 1, z, x, y + 4, z, underpass);
                    }
                }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Traffic implements LevelBehavior {
        static final class St {
            int next = 900;
            int passing = -1;
            int fromX;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.next = 600 + p.getRandom().nextInt(900);
            states.put(p.getUUID(), st);
            // you always come to on the hard shoulder
            int zc = Math.floorDiv(p.getBlockZ() + PZ / 2, PZ) * PZ;
            int x = p.getBlockX();
            if (OutsideKit.autotest()) x = Math.floorDiv(x, 32) * 32 + 6;
            level.getChunk(new BlockPos(x, G, zc));
            p.teleportTo(level, x + 0.5, G + 1, zc + 11.5, 270f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            if (st.passing >= 0) {
                st.passing++;
                if (st.passing == 92) pass(p, level, st);
                if (st.passing > 110) st.passing = -1;
                return;
            }
            if (--st.next > 0) return;
            st.next = 900 + p.getRandom().nextInt(1300);
            int zl = zl(p.getBlockZ());
            // eastbound traffic on the south side, westbound on the north
            st.fromX = zl >= 0 ? -1 : 1;
            st.passing = 0;
            BlockPos src = BlockPos.containing(p.getX() + st.fromX * 26, G + 1, p.getZ());
            OutsideKit.sound(level, src, "event.endless_highway_approach", 3.5f, 1f);
        }

        void pass(ServerPlayer p, ServerLevel level, St st) {
            int a = Math.abs(zl(p.getBlockZ()));
            boolean onLanes = a >= 1 && a <= 11 && p.getY() > G - 1 && p.getY() < G + 3;
            if (onLanes && !p.isCreative() && !p.isSpectator()) {
                OutsideKit.sound(level, p.blockPosition(), "event.endless_highway_hit", 1.5f, 1f);
                p.hurt(level.damageSources().flyIntoWall(), 9f);
                Sanity.add(p, -12f);
                p.push(-st.fromX * 1.4, 0.45, 0);
                p.hurtMarked = true;
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
            } else {
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.4f));
                Vec3 wind = new Vec3(-st.fromX * 0.15, 0, 0);
                p.push(wind.x, 0, 0);
                p.hurtMarked = true;
            }
        }
    }
}
