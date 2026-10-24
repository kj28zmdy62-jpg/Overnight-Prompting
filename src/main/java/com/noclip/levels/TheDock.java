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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Dock. A long wooden dock across a black lake at night, lanterns every so often,
 * mist over the water. Every forty-eight boards a side pier runs out to a boathouse, and
 * every boathouse is number 4. Keep walking and keep counting; one of them is 13, and
 * its door is open. The water is very deep and very cold, and anything that falls in
 * is pulled under.
 */
public final class TheDock {
    static final String ID = "the_dock";
    static final int W = 40;
    static final int PZ = 96, SPAN = 48;

    private TheDock() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Dock").order(3120)
                .tagline("Every boathouse is number 4. Keep counting. Don't fall in.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Undertow())
                .visuals(new Visuals().tint(0.94f, 1.0f, 1.0f).saturation(0.6f).contrast(1.1f).brightness(1.12f)
                        .grain(0.07f).aberration(0.0015f).vignette(0.58f).warp(0.014f)
                        .fog(3f, 34f).fogColor(0x0E1A1C).bloom(0.7f))
                .sanityDrain(0.035f)
                .spawnY(W + 2, W + 2)
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.2f).dist(14, 28).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(24, 34).weight(1))
                .exit(ExitType.DOOR, "level_5", 1).exit(ExitType.DOOR, "motel", 2).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.WATER, "level_7", 3).exit(ExitType.WATER, "sewers", 1)
                .exit(ExitType.NOCLIP, "silent_beach", 2).exit(ExitType.NOCLIP, "level_7", 1).exit(ExitType.NOCLIP, "rain_city", 1)
                .build();
    }

    static int zl(int z) {
        return Math.floorMod(z + PZ / 2, PZ) - PZ / 2;
    }

    /** Finger pier n sticks out north (even n) or south (odd n) at x = n * SPAN + 24. */
    static boolean thirteen(long s, int n, int row) {
        return Hash.unit(s, n, row, 13) < 0.09f;
    }

    static final class Gen implements LevelGenerator {
        final BlockState boards = ModBlocks.state("td_boards");
        final BlockState boardsB = ModBlocks.state("td_boards_b");
        final BlockState mud = ModBlocks.state("td_mud");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState pile = ModBlocks.state("td_pile");
        final BlockState rail = ModBlocks.state("td_rail");
        final BlockState siding = ModBlocks.state("td_siding");
        final BlockState shingle = ModBlocks.state("td_shingle");
        final BlockState window = ModBlocks.state("td_window");
        final BlockState windowLit = ModBlocks.state("td_window_lit");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int floor = W - 18 + (int) (Hash.fbm(s ^ 0xB0DL, x / 30.0, z / 30.0, 2) * 6);
                    w.fill(x, 0, z, x, floor - 1, z, sub);
                    w.set(x, floor, z, mud);
                    w.fill(x, floor + 1, z, x, W, z, water);
                    int zl = zl(z);
                    if (Math.abs(zl) <= 1) mainDock(w, s, x, z, zl, floor);
                }
            int n0 = Math.floorDiv(w.minX - 24, SPAN) - 1, n1 = Math.floorDiv(w.maxX, SPAN) + 1;
            int r0 = Math.floorDiv(w.minZ + PZ / 2, PZ) - 1, r1 = Math.floorDiv(w.maxZ + PZ / 2, PZ) + 1;
            for (int r = r0; r <= r1; r++)
                for (int n = n0; n <= n1; n++) finger(w, s, n, r);
        }

        void mainDock(ChunkWriter w, long s, int x, int z, int zl, int floor) {
            boolean gap = Hash.unit(s, Math.floorDiv(x, 3), z - zl, 5) < 0.02f && Math.floorMod(x, 3) != 0;
            if (!gap) w.set(x, W + 1, z, Hash.unit(s, x, z, 6) < 0.3f ? boardsB : boards);
            if (Math.abs(zl) == 1 && Math.floorMod(x, 4) == 0) w.fill(x, floor + 1, z, x, W, z, pile);
            if (zl == 1 && Math.floorMod(x, 12) == 6 || zl == -1 && Math.floorMod(x, 12) == 0) {
                w.set(x, W + 2, z, ModBlocks.state("td_lamp_post"));
                boolean dead = Hash.unit(s, x, z, 7) < 0.25f;
                w.set(x, W + 3, z, OutsideKit.facing(dead ? "td_lantern_dead" : "td_lantern", Direction.NORTH));
            } else if (Math.abs(zl) == 1 && Hash.unit(s, x, z, 8) < 0.03f) {
                w.set(x, W + 2, z, OutsideKit.facing("td_cleat", zl > 0 ? Direction.NORTH : Direction.SOUTH));
            }
        }

        void finger(ChunkWriter w, long s, int n, int r) {
            int fx = n * SPAN + 24, zc = r * PZ;
            int dir = Math.floorMod(n, 2) == 0 ? -1 : 1;
            for (int k = 2; k <= 13; k++) {
                int z = zc + dir * k;
                for (int dx = 0; dx <= 1; dx++) {
                    int x = fx + dx;
                    if (!w.contains(x, z)) continue;
                    w.set(x, W + 1, z, boards);
                    if (k % 4 == 0) w.fill(x, W - 16, z, x, W, z, pile);
                }
                if (k == 6 && w.contains(fx + 2, z)) w.set(fx + 2, W + 1, z, OutsideKit.facing("td_life_ring", dir < 0 ? Direction.EAST : Direction.WEST));
            }
            boathouse(w, s, n, r, fx, zc + dir * 14, dir);
        }

        /** A boathouse at the end of the side pier, open to the water at the far end. */
        void boathouse(ChunkWriter w, long s, int n, int r, int fx, int z0, int dir) {
            boolean odd = thirteen(s, n, r);
            boolean lit = !odd && Hash.unit(s, n, r, 21) < 0.12f;
            for (int dx = -3; dx <= 4; dx++)
                for (int k = 0; k <= 8; k++) {
                    int x = fx + dx, z = z0 + dir * k;
                    if (!w.contains(x, z)) continue;
                    boolean side = dx == -3 || dx == 4;
                    boolean front = k == 0, back = k == 8;
                    if (!side && !back) w.set(x, W + 1, z, boards);
                    if (side || front) {
                        for (int y = W + 1; y <= W + 4; y++) {
                            BlockState b = siding;
                            if (side && y == W + 3 && (k == 3 || k == 6)) b = lit && k == 3 ? windowLit : window;
                            w.set(x, y, z, b);
                        }
                        if ((dx == -3 || dx == 4) && (k == 0 || k == 8)) w.fill(x, W - 16, z, x, W, z, pile);
                    }
                    if (back && !side) w.set(x, W + 4, z, siding);
                    // gabled roof, ridge along the boathouse
                    for (int t = 0; t <= 4; t++) {
                        int y = W + 5 + t;
                        int left = -3 + t, right = 4 - t;
                        if (dx == left) w.set(x, y, z, ModBlocks.state("td_roof").setValue(StairBlock.FACING, Direction.EAST));
                        else if (dx == right) w.set(x, y, z, ModBlocks.state("td_roof").setValue(StairBlock.FACING, Direction.WEST));
                        else if ((front || back) && dx > left && dx < right) w.set(x, y, z, siding);
                    }
                }
            // the front, facing the pier: a door and a number above it
            Direction face = dir < 0 ? Direction.SOUTH : Direction.NORTH;
            if (w.contains(fx, z0)) {
                if (odd) {
                    w.set(fx, W + 2, z0, ModBlocks.state("td_door_open"));
                    w.set(fx, W + 3, z0, ModBlocks.state("td_door_open"));
                } else {
                    w.set(fx, W + 2, z0, OutsideKit.facing("td_door", face));
                    w.set(fx, W + 3, z0, OutsideKit.facing("td_door", face));
                }
            }
            if (w.contains(fx + 1, z0)) w.set(fx + 1, W + 4, z0, OutsideKit.facing(odd ? "td_number_13" : "td_number_4", face));
            if (w.contains(fx - 1, z0 - dir)) {
                w.set(fx - 1, W + 2, z0 - dir, ModBlocks.state("td_lamp_post"));
                w.set(fx - 1, W + 3, z0 - dir, OutsideKit.facing(odd ? "td_lantern" : "td_lantern_dead", Direction.NORTH));
            }
            // a rowboat tied up inside, sometimes
            if (Hash.unit(s, n, r, 22) < 0.5f) {
                for (int k = 3; k <= 5; k++) {
                    int z = z0 + dir * k;
                    if (w.contains(fx + 1, z)) w.set(fx + 1, W + 1, z, OutsideKit.facing(k == 4 ? "sb_boat_mid" : "sb_boat_end",
                            k == 3 ? face.getOpposite() : face));
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Undertow implements LevelBehavior {
        private final Map<UUID, Integer> wet = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            wet.remove(p.getUUID());
            int zc = Math.floorDiv(p.getBlockZ() + PZ / 2, PZ) * PZ;
            int x = p.getBlockX();
            if (OutsideKit.autotest()) x = Math.floorDiv(x, SPAN) * SPAN + 18;
            level.getChunk(new BlockPos(x, W, zc));
            p.teleportTo(level, x + 0.5, W + 2, zc + 0.5, OutsideKit.autotest() ? 0f : 270f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isCreative() || p.isSpectator()) return;
            if (p.isInWater() && p.getY() < W + 0.6) {
                int t = wet.merge(p.getUUID(), 1, Integer::sum);
                if (t == 20) OutsideKit.sound(level, p.blockPosition(), "event.the_dock_pulled", 1.2f, 1f);
                if (t > 20) {
                    // something has hold of your ankle
                    p.setDeltaMovement(p.getDeltaMovement().multiply(0.6, 1, 0.6).add(0, -0.08, 0));
                    p.hurtMarked = true;
                }
                if (t == 30) ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.6f));
                if (t >= 70) {
                    wet.remove(p.getUUID());
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                    Travel.exit(p, ExitType.WATER, p.blockPosition());
                }
            } else {
                wet.remove(p.getUUID());
            }
        }
    }
}
