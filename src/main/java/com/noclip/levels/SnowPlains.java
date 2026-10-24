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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snow Plains. A blizzard over an endless snowfield, and rows of streetlights standing
 * in it as if they were lighting a road that is not there. Out of their light you get
 * colder, and the longer you stay, the faster it happens; under them you thaw. There
 * are old tracks in the snow, and every now and then fresh ones, right beside you,
 * heading off into the white. They are not yours.
 */
public final class SnowPlains {
    static final String ID = "snow_plains";
    static final int G = 40;
    static final int ROW = 46, SPACING = 18;

    private SnowPlains() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Snow Plains").order(3080)
                .tagline("Streetlights in a blizzard, and no road. Stay in the light. Those tracks aren't yours.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Cold())
                .visuals(new Visuals().tint(0.92f, 0.98f, 1.08f).saturation(0.5f).contrast(0.96f).brightness(1.12f)
                        .grain(0.08f).aberration(0.0014f).vignette(0.52f).warp(0.015f)
                        .fog(1f, 24f).fogColor(0x8A96A4).bloom(0.6f))
                .sanityDrain(0.035f)
                .spawnY(G - 3, G + 4)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(16, 22))
                .exit(ExitType.DESCEND, "cold_storage", 2).exit(ExitType.DESCEND, "bunker", 2).exit(ExitType.DESCEND, "level_1", 1)
                .exit(ExitType.NOCLIP, "cold_storage", 2).exit(ExitType.NOCLIP, "salt_flats", 1)
                .exit(ExitType.NOCLIP, "pole_forest", 1)
                .build();
    }

    static int surface(long s, int x, int z) {
        return G + (int) Math.floor((Hash.fbm(s ^ 0x5A0L, x / 40.0, z / 40.0, 3) - 0.5f) * 5f);
    }

    static int rowZ(long s, int r) {
        return r * ROW + Hash.range(s, 3, r, 8);
    }

    static int lampX(long s, int r, int n) {
        return n * SPACING + Hash.range(s, r, n, 3);
    }

    /** 0 lit, 1 failing, 2 dead, 3 missing */
    static int lampState(long s, int r, int n) {
        float u = Hash.unit(s, r, n, 5);
        return u < 0.08f ? 3 : u < 0.2f ? 2 : u < 0.33f ? 1 : 0;
    }

    static CellGrid trails(long s) {
        return new CellGrid(s ^ 0x7A11L, 96, 24);
    }

    static final class Gen implements LevelGenerator {
        final BlockState snow = ModBlocks.state("sp_snow");
        final BlockState snowB = ModBlocks.state("sp_snow_b");
        final BlockState frozen = ModBlocks.state("sp_frozen");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState pole = ModBlocks.state("sp_pole");
        final BlockState post = ModBlocks.state("sp_post");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int h = surface(s, x, z);
                    w.fill(x, 0, z, x, h - 3, z, sub);
                    w.fill(x, h - 2, z, x, h - 1, z, frozen);
                    w.set(x, h, z, Hash.unit(s, x, z, 2) < 0.5f ? snow : snowB);
                }
            int r0 = Math.floorDiv(w.minZ, ROW) - 1, r1 = Math.floorDiv(w.maxZ, ROW) + 1;
            for (int r = r0; r <= r1; r++) {
                int z = rowZ(s, r);
                if (z < w.minZ || z > w.maxZ) continue;
                int n0 = Math.floorDiv(w.minX, SPACING) - 1, n1 = Math.floorDiv(w.maxX, SPACING) + 1;
                for (int n = n0; n <= n1; n++) lamp(w, s, r, n, z);
            }
            drawTrails(w, s);
        }

        void lamp(ChunkWriter w, long s, int r, int n, int z) {
            int x = lampX(s, r, n), st = lampState(s, r, n);
            if (!w.contains(x, z) || st == 3) return;
            int h = surface(s, x, z);
            for (int y = h + 1; y <= h + 6; y++) w.set(x, y, z, pole);
            w.set(x, h + 7, z, OutsideKit.facing("sp_pole_top", Direction.NORTH));
            String lamp = st == 0 ? "sp_lamp" : st == 1 ? "sp_lamp_failing" : "sp_lamp_dead";
            w.set(x, h + 7, z - 1, ModBlocks.state(lamp));
            if (st < 2) {
                w.set(x, h + 3, z - 1, OutsideKit.light(14));
                w.set(x + 3, h + 2, z - 1, OutsideKit.light(11));
                w.set(x - 3, h + 2, z - 1, OutsideKit.light(11));
                w.set(x, h + 2, z + 2, OutsideKit.light(11));
                w.set(x, h + 2, z - 4, OutsideKit.light(11));
            }
            // a stop sign at a crossing that is not there
            if (Hash.unit(s, r, n, 9) < 0.04f) w.set(x + 2, h + 1, z + 1, OutsideKit.facing("sp_road_sign", Direction.SOUTH));
        }

        /** Old tracks wandering across the snow: most give out, some end at a cellar hatch. */
        void drawTrails(ChunkWriter w, long s) {
            CellGrid g = trails(s);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                long h = g.hash(i, k, 1);
                if (Hash.unit(h) > 0.7f) return;
                double x = x0 + 10 + Hash.range(h >>> 4, wd - 20), z = z0 + 10 + Hash.range(h >>> 12, dp - 20);
                double ang = Hash.unit(h >>> 20) * Math.PI * 2;
                int len = 30 + Hash.range(h >>> 28, 50);
                boolean cellar = Hash.unit(h, 3, 3) < 0.35f;
                for (int n = 0; n < len; n++) {
                    ang += (Hash.unit(h, n, 4) - 0.5f) * 0.35f;
                    x += Math.cos(ang) * 1.6;
                    z += Math.sin(ang) * 1.6;
                    int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
                    if (!w.contains(bx, bz)) continue;
                    int hh = surface(s, bx, bz);
                    if (w.isAir(bx, hh + 1, bz)) w.set(bx, hh + 1, bz, OutsideKit.facing("sp_tracks", facing(ang)));
                    if (n == len / 2 && Hash.unit(h, 5, 5) < 0.25f) w.set(bx + 1, hh + 1, bz, OutsideKit.facing("sp_mitten", facing(ang)));
                }
                int ex = (int) Math.floor(x), ez = (int) Math.floor(z);
                if (cellar && w.contains(ex, ez)) {
                    int hh = surface(s, ex, ez);
                    w.set(ex, hh, ez, ModBlocks.state("sp_cellar"));
                    w.set(ex + 1, hh + 1, ez, post);
                    w.set(ex + 1, hh + 2, ez, OutsideKit.light(9));
                }
            });
        }
    }

    static Direction facing(double ang) {
        double dx = Math.cos(ang), dz = Math.sin(ang);
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // ---------------------------------------------------------------- behaviour

    static final class Cold implements LevelBehavior {
        static final class St {
            long entered;
            int nextTracks = 600;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.entered = level.getGameTime();
            st.nextTracks = 500 + p.getRandom().nextInt(600);
            states.put(p.getUUID(), st);
            long s = OutsideKit.seed(level, ID);
            // you arrive under a streetlight
            int r = Math.floorDiv(p.getBlockZ(), ROW);
            int n = Math.floorDiv(p.getBlockX(), SPACING);
            for (int k = 0; k < 6 && lampState(s, r, n + k) >= 2; k++) n++;
            int x = lampX(s, r, n) + 1, z = rowZ(s, r) + 2;
            level.getChunk(new BlockPos(x, G, z));
            p.teleportTo(level, x + 0.5, surface(s, x, z) + 1, z + 0.5, OutsideKit.autotest() ? 120f : p.getYRot(), 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> {
                St n = new St();
                n.entered = level.getGameTime();
                return n;
            });
            if (p.isCreative() || p.isSpectator()) return;
            int light = level.getBrightness(LightLayer.BLOCK, p.blockPosition().above());
            boolean warm = light >= 10;
            if (!warm) {
                // vanilla thaws 2 a tick; we freeze a little faster than that, and faster the longer you stay
                int minutes = (int) ((level.getGameTime() - st.entered) / 1200);
                int extra = (p.tickCount % 20 == 0) ? 1 + Math.min(4, minutes / 2) : 0;
                int f = p.getTicksFrozen();
                p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, f + 2 + extra));
                if (p.isFullyFrozen() && p.tickCount % 60 == 0) Sanity.add(p, -2f);
            }
            if (--st.nextTracks <= 0) {
                st.nextTracks = 700 + p.getRandom().nextInt(900);
                freshTracks(p, level);
            }
        }

        /** Fresh footprints cross the snow right beside you, coming from behind, heading off into the blizzard. */
        void freshTracks(ServerPlayer p, ServerLevel level) {
            long s = OutsideKit.seed(level, ID);
            double side = p.getRandom().nextBoolean() ? 1 : -1;
            double yaw = Math.toRadians(p.getYRot());
            Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x).scale(side);
            Vec3 start = p.position().subtract(fwd.scale(12)).add(right.scale(3));
            double ang = Math.atan2(fwd.z + right.z * 0.4, fwd.x + right.x * 0.4);
            BlockState fresh = ModBlocks.state("sp_tracks_fresh");
            int placed = 0;
            for (int n = 0; n < 28; n++) {
                ang += (p.getRandom().nextFloat() - 0.5f) * 0.2f;
                Vec3 at = start.add(Math.cos(ang) * 1.6 * n, 0, Math.sin(ang) * 1.6 * n);
                int bx = (int) Math.floor(at.x), bz = (int) Math.floor(at.z);
                BlockPos pos = new BlockPos(bx, surface(s, bx, bz) + 1, bz);
                if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) continue;
                if (pos.distSqr(p.blockPosition()) < 4) continue;
                if (OutsideKit.seen(p, Vec3.atCenterOf(pos), 0.75) && pos.distSqr(p.blockPosition()) < 100) continue;
                level.setBlock(pos, OutsideKit.facing(fresh, facing(ang)), Block.UPDATE_ALL);
                placed++;
            }
            if (placed > 0) {
                OutsideKit.sound(level, BlockPos.containing(start.add(fwd.scale(6))), "event.snow_plains_steps", 1.2f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.2f));
            }
        }
    }
}
