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
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storm Drains. Culverts big enough to drive a truck through, a channel of black
 * water down the middle, a ledge on each side. Every forty-eight metres the
 * tunnels meet under a shaft thirty metres tall, and daylight falls down it
 * through a grate you can climb up to but never through. Every few minutes the
 * water surges: the roar comes first, then it rises over the ledges and pulls.
 * Your footsteps come back to you a little late. Sometimes they keep going.
 */
public final class StormDrains {
    static final int FLOOR = 20, SHAFT_TOP = FLOOR + 34;
    static final int GRID = 48;

    private StormDrains() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("storm_drains")
                .number("").title("Storm Drains").order(2010)
                .tagline("When you hear the roar, get off the ledge. When you stop walking, listen.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Surge())
                .visuals(new Visuals().darkness(0.12f).tint(0.92f, 1.0f, 0.98f).saturation(0.62f).contrast(1.08f)
                        .brightness(1.3f).grain(0.08f).aberration(0.0014f).vignette(0.6f).fog(3f, 44f).fogColor(0x1A1E1C)
                        .bloom(0.75f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.15f).dist(30, 44).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(22, 36).weight(1))
                .exit(ExitType.NOCLIP, "sewers", 3).exit(ExitType.NOCLIP, "level_7", 1).exit(ExitType.NOCLIP, "water_treatment", 1)
                .exit(ExitType.WATER, "sewers", 3).exit(ExitType.WATER, "level_7", 1).exit(ExitType.WATER, "water_treatment", 1)
                .exit(ExitType.DOOR, "sewers", 2).exit(ExitType.DOOR, "dam", 1).exit(ExitType.DOOR, "water_treatment", 1)
                .exit(ExitType.ASCEND, "level_11", 2).exit(ExitType.ASCEND, "rain_city", 2).exit(ExitType.ASCEND, "parking_lot", 1)
                .build();
    }

    /** Offset from the nearest tunnel centre line (-24..23). */
    static int off(int c) {
        return Math.floorMod(c + GRID / 2, GRID) - GRID / 2;
    }

    static boolean tunnelX(int z) {
        return Math.abs(off(z)) <= 6;
    }

    static boolean tunnelZ(int x) {
        return Math.abs(off(x)) <= 6;
    }

    static boolean junction(int x, int z) {
        return Math.abs(off(x)) <= 8 && Math.abs(off(z)) <= 8;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("sd_culvert");
        final BlockState algae = ModBlocks.state("sd_culvert_algae");
        final BlockState tide = ModBlocks.state("sd_tide_line");
        final BlockState low = ModBlocks.state("sd_low_wall");
        final BlockState[] graffiti = {ModBlocks.state("sd_graffiti_arrow"), ModBlocks.state("sd_graffiti_count"),
                ModBlocks.state("sd_graffiti_listen")};
        final BlockState ruler = ModBlocks.state("sd_ruler");
        final BlockState ledge = ModBlocks.state("sd_floor");
        final BlockState bed = ModBlocks.state("sd_bed");
        final BlockState crown = ModBlocks.state("sd_ceiling");
        final BlockState shaft = ModBlocks.state("sd_shaft");
        final BlockState sky = ModBlocks.state("sd_sky_grate");
        final BlockState loose = ModBlocks.state("sd_loose_grate");
        final BlockState beam = ModBlocks.state("sd_beam");
        final BlockState rungs = ModBlocks.state("sd_rungs");
        final BlockState outfall = ModBlocks.state("sd_outfall");
        final BlockState door = ModBlocks.state("sd_door");
        final BlockState lamp = ModBlocks.state("sd_lamp");
        final BlockState trash = ModBlocks.state("sd_trash");
        final BlockState cart = ModBlocks.state("sd_cart");
        final BlockState cone = ModBlocks.state("sd_cone");
        final BlockState bridge = ModBlocks.state("sd_bridge");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        final BlockState fill = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 8);
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 3, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            }
            features(w, s);
        }

        BlockState wallAt(long s, int x, int y, int z) {
            if (y == FLOOR + 1) return low;
            if (y == FLOOR + 2) return tide;
            float r = Hash.unit(s, x, y, z);
            if (y == FLOOR + 3 && r < 0.004f) return graffiti[Hash.range(Hash.hash(s, x, z, 5), graffiti.length)];
            return Hash.fbm(s ^ 0xA16A, x / 14.0, (y * 2 + z) / 14.0, 2) > 0.6f ? algae : wall;
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int u = off(z), v = off(x);
            boolean j = junction(x, z);
            boolean tx = tunnelX(z), tz = tunnelZ(x);
            if (!j && !tx && !tz) {
                // solid ground between tunnels; side pipes are cut later
                w.fill(x, FLOOR - 2, z, x, FLOOR + 10, z, wall);
                w.fill(x, FLOOR + 11, z, x, FLOOR + 13, z, filler);
                return;
            }
            if (j) {
                junctionColumn(w, s, x, z, u, v);
                return;
            }
            int a = tx ? Math.abs(u) : Math.abs(v);
            int along = tx ? x : z;
            if (a == 6) {
                w.fill(x, FLOOR - 2, z, x, FLOOR, z, wall);
                for (int y = FLOOR + 1; y <= FLOOR + 6; y++) w.set(x, y, z, wallAt(s, x, y, z));
                w.fill(x, FLOOR + 7, z, x, FLOOR + 10, z, wall);
                return;
            }
            int top = a <= 2 ? FLOOR + 8 : FLOOR + 10 - a;
            if (a <= 2) {
                w.fill(x, FLOOR - 3, z, x, FLOOR - 2, z, bed);
                w.set(x, FLOOR - 1, z, water);
                boolean bridgeHere = Math.floorMod(along, 32) == 16;
                if (bridgeHere) w.set(x, FLOOR, z, bridge);
            } else {
                w.fill(x, FLOOR - 2, z, x, FLOOR - 1, z, wall);
                w.set(x, FLOOR, z, ledge);
            }
            w.set(x, top + 1, z, crown);
            w.fill(x, top + 2, z, x, FLOOR + 10, z, wall);
            if (a == 0 && Math.floorMod(along, 6) == 0) w.set(x, FLOOR + 6, z, fill);
        }

        void junctionColumn(ChunkWriter w, long s, int x, int z, int u, int v) {
            int au = Math.abs(u), av = Math.abs(v);
            int m = Math.max(au, av);
            boolean channel = (au <= 2 || av <= 2);
            if (m == 8) {
                // shaft wall: the tunnels' mouths are cut through it below
                boolean mouth = (au <= 5 || av <= 5) && Math.min(au, av) <= 5;
                w.fill(x, FLOOR - 2, z, x, FLOOR, z, wall);
                for (int y = FLOOR + 1; y < SHAFT_TOP; y++) {
                    int a = Math.min(au, av);
                    int mouthTop = a <= 2 ? FLOOR + 8 : FLOOR + 10 - a;
                    if (mouth && y <= mouthTop) continue;
                    w.set(x, y, z, y <= FLOOR + 6 ? wallAt(s, x, y, z) : shaft);
                }
                if (mouth && Math.min(au, av) <= 2) {
                    w.fill(x, FLOOR - 3, z, x, FLOOR - 2, z, bed);
                    w.set(x, FLOOR, z, ModBlocks.AIR);
                    w.set(x, FLOOR - 1, z, water);
                } else if (mouth) {
                    w.set(x, FLOOR, z, ledge);
                }
                w.set(x, SHAFT_TOP, z, shaft);
                return;
            }
            // chamber interior: the two channels cross in a plus, ledge quadrants between, bridges over
            if (channel) {
                w.fill(x, FLOOR - 3, z, x, FLOOR - 2, z, bed);
                w.set(x, FLOOR - 1, z, water);
                if ((au <= 2 && av == 5) || (av <= 2 && au == 5)) w.set(x, FLOOR, z, bridge);
            } else {
                w.fill(x, FLOOR - 2, z, x, FLOOR - 1, z, wall);
                w.set(x, FLOOR, z, ledge);
            }
            int gi = Math.floorDiv(x + GRID / 2, GRID), gk = Math.floorDiv(z + GRID / 2, GRID);
            // the grate at the top of the shaft, and the light that falls from it
            if (au <= 2 && av <= 2) {
                w.set(x, SHAFT_TOP, z, sky);
                if (au <= 1 && av <= 1) {
                    for (int y = FLOOR + 9; y < SHAFT_TOP; y += 1) w.set(x, y, z, beam);
                }
                if (u == 0 && v == 0) {
                    w.set(x, FLOOR + 5, z, light);
                    if (Hash.chance(s, gi, gk, 9, 0.25f)) {
                        w.set(x, FLOOR - 2, z, water);
                        w.set(x, FLOOR - 3, z, outfall);
                    }
                }
            } else {
                w.set(x, SHAFT_TOP, z, shaft);
            }
            w.fill(x, SHAFT_TOP + 1, z, x, SHAFT_TOP + 3, z, filler);
        }

        void features(ChunkWriter w, long s) {
            int gi0 = Math.floorDiv(w.minX + GRID / 2, GRID) - 1, gi1 = Math.floorDiv(w.maxX + GRID / 2, GRID) + 1;
            int gk0 = Math.floorDiv(w.minZ + GRID / 2, GRID) - 1, gk1 = Math.floorDiv(w.maxZ + GRID / 2, GRID) + 1;
            for (int gi = gi0; gi <= gi1; gi++) {
                for (int gk = gk0; gk <= gk1; gk++) {
                    int cx = gi * GRID, cz = gk * GRID;
                    shaftLadder(w, s, gi, gk, cx, cz);
                    // tunnel along +x from this junction, and along +z
                    tunnelDetails(w, s, gi, gk, cx, cz, true);
                    tunnelDetails(w, s, gi, gk, cx, cz, false);
                    sidePipe(w, s, gi, gk, cx, cz);
                }
            }
        }

        /** Rungs up one wall of the shaft to the grate. One grate in four is loose. */
        void shaftLadder(ChunkWriter w, long s, int gi, int gk, int cx, int cz) {
            int side = Hash.range(s, gi, gk, 12, 4);
            Direction d = Direction.from2DDataValue(side);
            // ladder column just inside the wall on side d, offset from the tunnel mouth
            int lx = cx + d.getStepX() * 7 + (d.getStepZ() != 0 ? 6 : 0);
            int lz = cz + d.getStepZ() * 7 + (d.getStepX() != 0 ? 6 : 0);
            BlockState r = rungs.setValue(LadderBlock.FACING, d.getOpposite());
            for (int y = FLOOR + 1; y < SHAFT_TOP; y++) w.set(lx, y, lz, r);
            boolean isLoose = Hash.chance(s, gi, gk, 13, 0.25f);
            w.set(lx, SHAFT_TOP, lz, isLoose ? loose : sky);
            w.set(lx, FLOOR + 1, lz, r);
        }

        void tunnelDetails(ChunkWriter w, long s, int gi, int gk, int cx, int cz, boolean alongX) {
            for (int t = 9; t < GRID - 9; t++) {
                int ax = alongX ? cx + t : cx, az = alongX ? cz : cz + t;
                long h = Hash.hash(s, ax, az, alongX ? 31 : 32);
                for (int side = -1; side <= 1; side += 2) {
                    int px = alongX ? ax : cx + side * 5, pz = alongX ? cz + side * 5 : az;
                    Direction toWall = alongX ? (side < 0 ? Direction.NORTH : Direction.SOUTH) : (side < 0 ? Direction.WEST : Direction.EAST);
                    float r = Hash.unit(Hash.hash(h, side));
                    if (Math.floorMod(t, 8) == 4 && (side > 0) == (Math.floorMod(t, 16) == 4)) {
                        if (r < 0.8f) w.set(px, FLOOR + 4, pz, lamp.setValue(HorizontalDirectionalBlock.FACING, toWall));
                    } else if (r < 0.006f) {
                        w.set(px - (alongX ? 0 : side), FLOOR + 1, pz - (alongX ? side : 0), trash.setValue(HorizontalDirectionalBlock.FACING, toWall));
                    } else if (r < 0.008f) {
                        w.set(px - (alongX ? 0 : side), FLOOR + 1, pz - (alongX ? side : 0), cone.setValue(HorizontalDirectionalBlock.FACING, toWall));
                    } else if (r < 0.0095f) {
                        w.set(px - (alongX ? 0 : side), FLOOR + 1, pz - (alongX ? side : 0), cart.setValue(HorizontalDirectionalBlock.FACING, toWall.getClockWise()));
                    }
                }
                // a raised maintenance niche every so often: a refuge from the water
                if (Math.floorMod(t, 13) == 0 && Hash.unit(h >>> 20) < 0.5f) niche(w, h, alongX, ax, az, cx, cz);
            }
        }

        void niche(ChunkWriter w, long h, boolean alongX, int ax, int az, int cx, int cz) {
            int side = (h & 1) == 0 ? 1 : -1;
            Direction out = alongX ? (side > 0 ? Direction.SOUTH : Direction.NORTH) : (side > 0 ? Direction.EAST : Direction.WEST);
            for (int a = -1; a <= 1; a++) {
                for (int dpt = 6; dpt <= 8; dpt++) {
                    int x = alongX ? ax + a : cx + side * dpt, z = alongX ? cz + side * dpt : az + a;
                    for (int y = FLOOR + 3; y <= FLOOR + 5; y++) w.set(x, y, z, ModBlocks.AIR);
                    w.set(x, FLOOR + 2, z, ledge);
                }
            }
            // rungs up from the ledge, a lamp inside, a ruler showing how high it gets
            int lx = alongX ? ax : cx + side * 5, lz = alongX ? cz + side * 5 : az;
            BlockState r = rungs.setValue(LadderBlock.FACING, out.getOpposite());
            w.set(lx, FLOOR + 1, lz, r);
            w.set(lx, FLOOR + 2, lz, r);
            int bx = alongX ? ax : cx + side * 9, bz = alongX ? cz + side * 9 : az;
            float r2 = Hash.unit(h >>> 30);
            if (r2 < 0.3f) {
                w.set(bx, FLOOR + 3, bz, door);
                w.set(bx, FLOOR + 4, bz, door);
            }
            int ix = alongX ? ax + 1 : cx + side * 8, iz = alongX ? cz + side * 8 : az + 1;
            w.set(alongX ? ax : cx + side * 8, FLOOR + 5, alongX ? cz + side * 8 : az,
                    lamp.setValue(HorizontalDirectionalBlock.FACING, out));
            if (r2 > 0.6f) w.set(ix, FLOOR + 3, iz, r2 > 0.85f ? supply : bottle);
            int rx = alongX ? ax + 2 : cx + side * 6, rz = alongX ? cz + side * 6 : az + 2;
            for (int y = FLOOR + 1; y <= FLOOR + 5; y++) w.set(rx, y, rz, ruler);
        }

        /** A smaller walk-through pipe running between two big tunnels, higher than the flood line. */
        void sidePipe(ChunkWriter w, long s, int gi, int gk, int cx, int cz) {
            if (!Hash.chance(s, gi, gk, 40, 0.55f)) return;
            int pz = cz + GRID / 2;
            for (int x = cx + 6; x <= cx + GRID - 6; x++) {
                for (int dz = -1; dz <= 1; dz++) {
                    w.set(x, FLOOR + 1, pz + dz, ledge);
                    for (int y = FLOOR + 2; y <= FLOOR + 4; y++) {
                        boolean corner = Math.abs(dz) == 1 && y == FLOOR + 4;
                        if (!corner) w.set(x, y, pz + dz, ModBlocks.AIR);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Water surges, current, and the footsteps that come back. */
    static final class Surge implements LevelBehavior {
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Map<ResourceKey<Level>, Set<BlockPos>> FLOODED = new ConcurrentHashMap<>();
        private final Map<UUID, double[]> walk = new ConcurrentHashMap<>();
        private final Map<UUID, Deque<double[]>> echoes = new ConcurrentHashMap<>();

        /** {phase, phaseEnd, nextSurge}. Phases: 0 idle, 1 roar, 2 rising, 3 high, 4 falling. */
        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 1500 + level.random.nextInt(1500)});
        }

        static int floodLevel(long phase) {
            return phase == 2 || phase == 4 ? 1 : phase == 3 ? 2 : 0;
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0) {
                if (now >= st[2]) {
                    st[0] = 1;
                    st[1] = now + 160;
                    for (ServerPlayer p : level.players()) {
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.storm_drains_surge"), SoundSource.AMBIENT, 2f, 1f);
                        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.5f));
                        p.displayClientMessage(Component.literal("A roar, somewhere upstream.").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), true);
                    }
                }
                return;
            }
            if (now < st[1]) return;
            int next = (int) st[0] + 1;
            if (next > 4) {
                st[0] = 0;
                st[2] = now + 2400 + level.random.nextInt(1800);
                drain(level, 0);
                return;
            }
            st[0] = next;
            st[1] = now + (next == 2 ? 100 : next == 3 ? 220 : 100);
            if (next == 4) drain(level, 1);
        }

        private static void drain(ServerLevel level, int keepUpTo) {
            Set<BlockPos> set = FLOODED.get(level.dimension());
            if (set == null) return;
            set.removeIf(p -> {
                if (p.getY() - FLOOR <= keepUpTo && keepUpTo > 0) return false;
                if (level.isLoaded(p) && level.getBlockState(p).is(Blocks.WATER)) level.setBlock(p, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                return true;
            });
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            int flood = floodLevel(st[0]);
            if (flood > 0 && (p.tickCount + p.getId()) % 10 == 0) flood(p, level, flood);
            if (flood > 0 && p.isInWater() && !p.isSpectator()) current(p);
            if (!p.isSpectator()) echo(p, level);
        }

        private static void flood(ServerPlayer p, ServerLevel level, int height) {
            Set<BlockPos> set = FLOODED.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            int r = 20;
            for (int x = p.getBlockX() - r; x <= p.getBlockX() + r; x++) {
                for (int z = p.getBlockZ() - r; z <= p.getBlockZ() + r; z++) {
                    if (!tunnelX(z) && !tunnelZ(x) && !junction(x, z)) continue;
                    for (int y = FLOOR; y <= FLOOR + height; y++) {
                        m.set(x, y, z);
                        if (!level.getBlockState(m).isAir()) continue;
                        level.setBlock(m, water, Block.UPDATE_CLIENTS);
                        set.add(m.immutable());
                    }
                }
            }
        }

        /** The water pulls along the tunnel you are in. */
        private static void current(ServerPlayer p) {
            int x = p.getBlockX(), z = p.getBlockZ();
            if (junction(x, z)) return;
            boolean alongX = tunnelX(z);
            int lane = alongX ? Math.floorDiv(z + GRID / 2, GRID) : Math.floorDiv(x + GRID / 2, GRID);
            int dir = Math.floorMod(lane, 2) == 0 ? 1 : -1;
            p.push(alongX ? dir * 0.028 : 0, 0, alongX ? 0 : dir * 0.028);
            p.hurtMarked = true;
        }

        /** Footsteps come back from behind, late. Sometimes they keep coming after you stop. */
        private void echo(ServerPlayer p, ServerLevel level) {
            double[] wk = walk.computeIfAbsent(p.getUUID(), k -> new double[]{p.getX(), p.getZ(), 0, 0});
            Deque<double[]> q = echoes.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            double dx = p.getX() - wk[0], dz = p.getZ() - wk[1];
            wk[0] = p.getX();
            wk[1] = p.getZ();
            double moved = Math.sqrt(dx * dx + dz * dz);
            long now = level.getGameTime();
            if (moved > 0.02 && p.onGround()) {
                wk[2] += moved;
                wk[3] = 0;
                if (wk[2] > 1.8) {
                    wk[2] = 0;
                    Vec3 back = p.position().subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(12));
                    q.add(new double[]{now + 14 + p.getRandom().nextInt(8), back.x, p.getY(), back.z});
                }
            } else {
                wk[3]++;
                if (wk[3] == 30 && p.getRandom().nextFloat() < 0.15f) {
                    // they do not stop when you do
                    Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
                    for (int n = 0; n < 4; n++) {
                        Vec3 at = p.position().subtract(dir.scale(14 - n * 2.2));
                        q.add(new double[]{now + 8 + n * 13, at.x, p.getY(), at.z});
                    }
                }
            }
            while (!q.isEmpty() && q.peek()[0] <= now) {
                double[] e = q.poll();
                level.playSound(null, e[1], e[2], e[3], ModSounds.get("event.storm_drains_step"), SoundSource.AMBIENT,
                        0.7f, 0.9f + p.getRandom().nextFloat() * 0.15f);
            }
        }
    }
}
