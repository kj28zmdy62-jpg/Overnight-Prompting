package com.noclip.levels;

import com.noclip.block.NeonBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rain City. Neon streets at night in rain that never lets up, every puddle full of
 * reflected signs, every shop lit and empty. Something walks these streets. You cannot
 * see it; you can see the puddles splash where it steps and the signs go dark as it
 * passes, coming your way. Get under cover in a lit doorway and it walks on by.
 */
public final class RainCity {
    static final String ID = "rain_city";
    static final int G = 40;
    static final int B = 40, ROAD = 8;
    static final String[] REFLECT = {"rc_reflect_pink", "rc_reflect_cyan", "rc_reflect_amber"};

    private RainCity() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Rain City").order(3150)
                .tagline("It never stops raining. Watch the signs: they go dark when it walks past.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Walker())
                .visuals(new Visuals().tint(1.0f, 0.96f, 1.08f).saturation(1.15f).contrast(1.12f).brightness(1.25f)
                        .grain(0.06f).aberration(0.0022f).vignette(0.5f).warp(0.012f)
                        .fog(4f, 52f).fogColor(0x0E0C16).bloom(0.95f))
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .exit(ExitType.DESCEND, "subway", 3).exit(ExitType.DESCEND, "level_0", 1)
                .exit(ExitType.DOOR, "level_5", 3).exit(ExitType.DOOR, "motel", 1)
                .exit(ExitType.NOCLIP, "level_11", 3).exit(ExitType.NOCLIP, "rooftops", 2).exit(ExitType.NOCLIP, "the_dock", 1)
                .build();
    }

    static int height(long s, int x, int z) {
        int tx = Math.floorDiv(x, 7), tz = Math.floorDiv(z, 7);
        return G + 12 + Hash.range(s, tx, tz, 16);
    }

    static boolean hotel(long s, int bi, int bk) {
        return Hash.unit(s, bi, bk, 3) < 0.15f;
    }

    static boolean subway(long s, int bi, int bk) {
        return Hash.unit(s, bi, bk, 4) < 0.14f;
    }

    static final class Gen implements LevelGenerator {
        final BlockState asphalt = ModBlocks.state("rc_asphalt");
        final BlockState lane = ModBlocks.state("rc_asphalt_lane");
        final BlockState zebra = ModBlocks.state("rc_asphalt_zebra");
        final BlockState pave = ModBlocks.state("rc_pavement");
        final BlockState curb = ModBlocks.state("rc_curb");
        final BlockState puddle = ModBlocks.state("rc_puddle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState shop = ModBlocks.state("rc_shopfront");
        final BlockState shopLight = ModBlocks.state("rc_shop_ceiling");
        final BlockState shopFloor = ModBlocks.state("rc_shop_floor");
        final BlockState dark = ModBlocks.state("rc_window_dark");
        final BlockState warm = ModBlocks.state("rc_window_warm");
        final BlockState cool = ModBlocks.state("rc_window_cool");
        final BlockState pole = ModBlocks.state("rc_pole");
        final BlockState manhole = ModBlocks.state("rc_manhole");
        final String[] facades = {"rc_facade_grey", "rc_facade_brick", "rc_facade_tile"};

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int u = Math.floorMod(x, B), v = Math.floorMod(z, B);
                    int bi = Math.floorDiv(x, B), bk = Math.floorDiv(z, B);
                    w.fill(x, 0, z, x, G - 2, z, sub);
                    boolean road = u < ROAD || v < ROAD;
                    int d = road ? -1 : Math.min(Math.min(u - ROAD, B - 1 - u), Math.min(v - ROAD, B - 1 - v));
                    if (road) {
                        boolean cross = u < ROAD && v < ROAD;
                        BlockState top = asphalt;
                        if (cross && (u == 0 || u == ROAD - 1 || v == 0 || v == ROAD - 1)) top = asphalt;
                        else if (!cross && (u == ROAD - 1 || v == ROAD - 1) ) top = zebra;
                        else if (u < ROAD && !cross && u == 4 && Math.floorMod(z, 6) < 3) top = lane;
                        else if (v < ROAD && !cross && v == 4 && Math.floorMod(x, 6) < 3) top = lane;
                        w.set(x, G, z, top);
                        if ((u == 3 && Math.floorMod(z, 20) == 10) || (v == 3 && Math.floorMod(x, 20) == 10)) w.set(x, G, z, manhole);
                    } else if (d <= 2) {
                        w.set(x, G, z, d == 0 ? curb : pave);
                    } else {
                        building(w, s, x, z, u, v, bi, bk);
                        continue;
                    }
                    w.set(x, G - 1, z, sub);
                    if (Hash.fbm(s ^ 0x9D1L, x / 7.0, z / 7.0, 2) > 0.6f && w.get(x, G, z) != manhole) {
                        w.set(x, G, z, puddle);
                        w.set(x, G - 1, z, ModBlocks.state(REFLECT[Hash.range(s, Math.floorDiv(x, 9), Math.floorDiv(z, 9), 3)]));
                    }
                    if (d == 1 && (u == ROAD + 1 || u == B - 2) && Math.floorMod(v, 12) == 6) {
                        w.fill(x, G + 1, z, x, G + 4, z, pole);
                        w.set(x, G + 5, z, OutsideKit.facing("rc_street_lamp", u == ROAD + 1 ? Direction.WEST : Direction.EAST));
                    }
                    if (d == 1 && (v == ROAD + 1 || v == B - 2) && Math.floorMod(u, 12) == 6) {
                        w.fill(x, G + 1, z, x, G + 4, z, pole);
                        w.set(x, G + 5, z, OutsideKit.facing("rc_street_lamp", v == ROAD + 1 ? Direction.NORTH : Direction.SOUTH));
                    }
                    if (u == ROAD && v == ROAD) {
                        w.fill(x, G + 1, z, x, G + 3, z, pole);
                        w.set(x, G + 4, z, OutsideKit.facing("rc_traffic_light", Direction.NORTH));
                    }
                    if (d == 2 && Hash.unit(s, x, z, 9) < 0.006f && w.get(x, G, z) == pave) {
                        Direction f = u == ROAD + 2 ? Direction.WEST : u == B - 3 ? Direction.EAST : v == ROAD + 2 ? Direction.NORTH : Direction.SOUTH;
                        w.set(x, G + 1, z, OutsideKit.facing("rc_vending", f));
                    }
                    if (subway(s, bi, bk) && u >= ROAD && u <= ROAD + 2 && v >= 18 && v <= 26) subwayStairs(w, x, z, v);
                }
        }

        void subwayStairs(ChunkWriter w, int x, int z, int v) {
            int step = v - 18;
            if (step < 6) {
                int y = G - step;
                w.fill(x, y, z, x, G + 2, z, ModBlocks.AIR);
                w.set(x, y, z, ModBlocks.state("rc_stairs").setValue(StairBlock.FACING, Direction.NORTH));
                w.fill(x, y - 3, z, x, y - 1, z, sub);
            } else {
                w.fill(x, G - 6, z, x, G + 2, z, ModBlocks.AIR);
                w.fill(x, G - 6, z, x, G - 4, z, ModBlocks.state("rc_subway_dark"));
                w.fill(x, G - 3, z, x, G, z, sub);
            }
            if (v == 18 && Math.floorMod(x, B) == ROAD) w.set(x, G + 1, z - 1, OutsideKit.facing("rc_subway_sign", Direction.NORTH));
        }

        void building(ChunkWriter w, long s, int x, int z, int u, int v, int bi, int bk) {
            int h = height(s, x, z);
            BlockState facade = ModBlocks.state(facades[Hash.range(s, Math.floorDiv(x, 7), Math.floorDiv(z, 7), 9, 3)]);
            int e = Math.min(Math.min(u - ROAD - 3, B - 1 - 3 - u), Math.min(v - ROAD - 3, B - 1 - 3 - v));
            w.set(x, G, z, shopFloor);
            if (e > 1) {
                w.fill(x, G + 1, z, x, h, z, facade);
                return;
            }
            Direction out = u - ROAD - 3 == e ? Direction.WEST : B - 4 - u == e ? Direction.EAST : v - ROAD - 3 == e ? Direction.NORTH : Direction.SOUTH;
            boolean corner = (u == ROAD + 3 || u == B - 4) && (v == ROAD + 3 || v == B - 4);
            if (e == 1) {
                // just inside the shopfront: a lit room you can step into, out of the rain
                w.fill(x, G + 1, z, x, G + 3, z, ModBlocks.AIR);
                w.set(x, G + 4, z, shopLight);
                w.fill(x, G + 5, z, x, h, z, facade);
                return;
            }
            // the outer skin
            boolean doorway = Math.floorMod((out.getAxis() == Direction.Axis.X ? z : x), 9) == 4;
            for (int y = G + 1; y <= G + 3; y++) w.set(x, y, z, corner ? facade : doorway ? ModBlocks.AIR
                    : shop.setValue(IronBarsBlock.NORTH, out.getAxis() == Direction.Axis.X).setValue(IronBarsBlock.SOUTH, out.getAxis() == Direction.Axis.X)
                    .setValue(IronBarsBlock.EAST, out.getAxis() == Direction.Axis.Z).setValue(IronBarsBlock.WEST, out.getAxis() == Direction.Axis.Z));
            w.set(x, G + 4, z, facade);
            for (int y = G + 5; y <= h; y++) {
                boolean win = Math.floorMod(y - G, 3) == 1 && Math.floorMod(out.getAxis() == Direction.Axis.X ? z : x, 2) == 0 && !corner;
                float r = Hash.unit(s, x * 31L + y, z, 5);
                w.set(x, y, z, win ? (r < 0.12f ? warm : r < 0.2f ? cool : dark) : facade);
            }
            // an awning over the sidewalk, and a sign above it
            int ax = x + out.getStepX(), az = z + out.getStepZ();
            if (!corner && w.contains(ax, az)) {
                long seg = Hash.hash(s, Math.floorDiv(x, 5), Math.floorDiv(z, 5), 6);
                w.set(ax, G + 4, az, OutsideKit.facing(Hash.unit(seg) < 0.5f ? "rc_awning_red" : "rc_awning_green", out));
                int pos = Math.floorMod(out.getAxis() == Direction.Axis.X ? z : x, 5);
                if (pos == 2 && Hash.unit(seg, 1, 1) < 0.75f) {
                    w.set(ax, G + 5, az, ModBlocks.state("rc_neon_" + Hash.range(seg, 2, 2, 8)).setValue(NeonBlock.FACING, out));
                }
            }
            if (hotel(s, bi, bk) && u == ROAD + 3 && v == ROAD + 6) {
                w.set(x, G + 1, z, ModBlocks.state("rc_hotel_door"));
                w.set(x, G + 2, z, ModBlocks.state("rc_hotel_door"));
            }
            if (hotel(s, bi, bk) && u == ROAD + 3 && v == ROAD + 3) {
                String word = "hotel";
                for (int n = 0; n < word.length(); n++) {
                    w.set(x - 1, G + 10 - n, z - 1, ModBlocks.state("rc_letter_pink_" + word.charAt(n)).setValue(NeonBlock.FACING, Direction.WEST));
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Walker implements LevelBehavior {
        static final class St {
            Vec3 pos;
            int age;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            states.put(p.getUUID(), st);
            int bx = Math.floorDiv(p.getBlockX(), B) * B, bz = Math.floorDiv(p.getBlockZ(), B) * B;
            level.getChunk(new BlockPos(bx, G, bz));
            p.teleportTo(level, bx + 4.5, G + 1, bz + 20.5, OutsideKit.autotest() ? 90f : 0f, 0f);
            reset(p, st);
        }

        void reset(ServerPlayer p, St st) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2;
            st.pos = new Vec3(p.getX() + Math.cos(ang) * 44, G + 1, p.getZ() + Math.sin(ang) * 44);
            st.age = 0;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            if (st.pos == null) reset(p, st);
            if (p.isCreative() || p.isSpectator()) return;
            st.age++;
            BlockPos head = p.blockPosition().above();
            boolean sheltered = !level.canSeeSky(head) && level.getBrightness(LightLayer.BLOCK, head) >= 10;
            Vec3 to = new Vec3(p.getX() - st.pos.x, 0, p.getZ() - st.pos.z);
            double dist = to.length();
            Vec3 step = to.normalize().scale(sheltered ? -0.08 : 0.11);
            st.pos = st.pos.add(step);
            if (dist > 80 || st.age > 20 * 90) {
                reset(p, st);
                return;
            }
            if (st.age % 8 == 0 && dist < 48) {
                BlockPos at = BlockPos.containing(st.pos.x, G + 1, st.pos.z);
                level.sendParticles(ParticleTypes.SPLASH, st.pos.x, G + 1.05, st.pos.z, 14, 0.25, 0.02, 0.25, 0.15);
                OutsideKit.sound(level, at, "event.rain_city_step", 1.3f, 0.9f + p.getRandom().nextFloat() * 0.2f);
                douse(level, at);
            }
            if (dist < 1.3 && !sheltered) {
                p.hurt(level.damageSources().magic(), 6f);
                Sanity.add(p, -15f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                reset(p, st);
            }
        }

        /** Every sign near it goes dark for a few seconds. */
        static void douse(ServerLevel level, BlockPos at) {
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-5, 2, -5), at.offset(5, 12, 5))) {
                BlockState st = level.getBlockState(q);
                if (st.getBlock() instanceof NeonBlock && st.getValue(NeonBlock.LIT)) {
                    level.setBlock(q, st.setValue(NeonBlock.LIT, false), Block.UPDATE_CLIENTS);
                    level.scheduleTick(q.immutable(), st.getBlock(), 50 + level.random.nextInt(30));
                }
            }
        }
    }
}
