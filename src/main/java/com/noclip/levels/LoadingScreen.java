package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loading Screen. Islands of options-menu dirt hanging in the dark, each with
 * a sign that says LOADING TERRAIN... and a progress bar. The bridges between
 * the islands are terrain that is still streaming in: they appear a slice at
 * a time as the bars fill, sit complete for a few seconds at a hundred
 * percent, and then the whole lot unloads and the bars start again from
 * nothing. Cross while it is loaded. Fall, and you fall out of the level.
 * Here and there, at the foot of a sign, is a button marked DONE.
 */
public final class LoadingScreen {
    static final int FLOOR = 24, CELL = 24, PLAT = 10, BRIDGE = CELL - PLAT - 1;
    static final int CYCLE = 340, LOADING = 230, HOLD = 60;

    private LoadingScreen() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("loading_screen")
                .number("").title("Loading Screen").order(4110)
                .tagline("It never finishes loading. Cross while it is loaded.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Streaming())
                .visuals(new Visuals().tint(1.02f, 0.98f, 0.94f).saturation(0.85f).contrast(1.08f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0014f).vignette(0.55f).fog(8f, 52f).fogColor(0x0A0806).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44))
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "level_1", 1).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.FALL, "level_0", 2).exit(ExitType.FALL, "missing_textures", 1).exit(ExitType.FALL, "glitch_level", 1)
                .exit(ExitType.NOCLIP, "missing_textures", 1).exit(ExitType.NOCLIP, "glitch_level", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .build();
    }

    /** 0..1 for the whole level, from the world clock: everyone sees the same bar. */
    static float progress(long time) {
        int t = (int) Math.floorMod(time, (long) CYCLE);
        if (t < LOADING) return t / (float) LOADING;
        if (t < LOADING + HOLD) return 1f;
        return 0f;
    }

    static boolean bridged(long s, int i, int k, int axis) {
        return Hash.chance(s, i, k, 0xB1 + axis, 0.78f);
    }

    // ---------------------------------------------------------------- the streaming

    static final class Streaming implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 2 != 0) return;
            long s = level.getSeed();
            float pr = progress(level.getGameTime());
            int segs = (int) Math.floor(pr * BRIDGE + 0.001);
            int bars = (int) Math.ceil(pr * 5 - 0.001);
            int ci = Math.floorDiv(p.getBlockX(), CELL), ck = Math.floorDiv(p.getBlockZ(), CELL);
            BlockState chunk = ModBlocks.state("ls_chunk");
            boolean popped = false, dropped = false;
            for (int i = ci - 1; i <= ci + 1; i++)
                for (int k = ck - 1; k <= ck + 1; k++) {
                    int x0 = i * CELL, z0 = k * CELL;
                    for (int axis = 0; axis < 2; axis++) {
                        if (!bridged(s, i, k, axis)) continue;
                        for (int j = 0; j < BRIDGE; j++) {
                            boolean want = j < segs;
                            for (int wdt = 4; wdt <= 6; wdt++) {
                                BlockPos b = axis == 0 ? new BlockPos(x0 + PLAT + 1 + j, FLOOR, z0 + wdt) : new BlockPos(x0 + wdt, FLOOR, z0 + PLAT + 1 + j);
                                if (!level.isLoaded(b)) continue;
                                boolean has = level.getBlockState(b).is(chunk.getBlock());
                                if (want && !has && level.getBlockState(b).isAir()) {
                                    level.setBlock(b, chunk, Block.UPDATE_CLIENTS);
                                    if (!popped && wdt == 5 && b.distSqr(p.blockPosition()) < 400) {
                                        level.playSound(null, b, ModSounds.get("event.loading_screen_pop"), SoundSource.BLOCKS, 0.35f, 0.8f + p.getRandom().nextFloat() * 0.4f);
                                        popped = true;
                                    }
                                } else if (!want && has) {
                                    level.setBlock(b, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                                    dropped = true;
                                }
                            }
                        }
                    }
                    bars(level, x0, z0, bars);
                }
            if (dropped && p.tickCount % 4 == 0) level.playSound(null, p.blockPosition(), ModSounds.get("event.loading_screen_unload"), SoundSource.AMBIENT, 0.8f, 1f);
        }

        void bars(ServerLevel level, int x0, int z0, int n) {
            for (int t = 0; t < 5; t++) {
                for (int side = 0; side < 2; side++) {
                    int x = side == 0 ? x0 + 3 + t : x0 + 7 - t;
                    BlockPos b = new BlockPos(x, FLOOR + 3, z0 + (side == 0 ? 6 : 4));
                    if (!level.isLoaded(b)) continue;
                    BlockState cur = level.getBlockState(b);
                    boolean full = t < n;
                    String want = full ? "ls_bar_full" : "ls_bar_empty";
                    if (!cur.is(ModBlocks.get("ls_bar_full")) && !cur.is(ModBlocks.get("ls_bar_empty"))) continue;
                    if (!cur.is(ModBlocks.get(want)))
                        level.setBlock(b, ModBlocks.state(want).setValue(HorizontalDirectionalBlock.FACING, side == 0 ? Direction.NORTH : Direction.SOUTH), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ---------------------------------------------------------------- islands

    static final class Gen implements LevelGenerator {
        final BlockState dirt = ModBlocks.state("ls_dirt");
        final BlockState floor = ModBlocks.state("ls_dirt_dark");
        final BlockState pit = ModBlocks.state("bottomless_pit");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(1, pit);
            CellGrid g = new CellGrid(s, CELL, 0);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> island(w, s, i, k, x0, z0));
        }

        BlockState f(String n, Direction d) {
            return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void island(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int F = FLOOR;
            for (int u = 0; u <= PLAT; u++)
                for (int v = 0; v <= PLAT; v++) {
                    int x = x0 + u, z = z0 + v;
                    // ragged underside: the island was cut out of something larger
                    int depth = 1 + Hash.range(s, x, z, 0x15, 3);
                    for (int y = F - depth; y <= F; y++) w.set(x, y, z, y == F ? floor : dirt);
                    boolean edge = u == 0 || v == 0 || u == PLAT || v == PLAT;
                    boolean gap = (u >= 4 && u <= 6) || (v >= 4 && v <= 6);
                    if (edge && !gap) {
                        w.set(x, F + 1, z, dirt);
                        if (Hash.chance(s, x, z, 0x16, 0.5f)) w.set(x, F + 2, z, dirt);
                    }
                }
            // the sign: a slab of background with its caption and bar on both faces
            String[] caps = {"load", "load", "build", "save"};
            String cap = caps[Hash.range(s, i, k, 0xCA, caps.length)];
            boolean done = Hash.chance(s, i, k, 0xD0E, 0.08f);
            for (int t = 0; t < 5; t++) {
                int x = x0 + 3 + t;
                for (int y = F + 1; y <= F + 4; y++) w.set(x, y, z0 + 5, dirt);
                w.set(x, F + 4, z0 + 6, f("ls_cap_" + cap + "_" + t, Direction.NORTH));
                w.set(x, F + 3, z0 + 6, f("ls_bar_empty", Direction.NORTH));
                int xb = x0 + 7 - t;
                w.set(xb, F + 4, z0 + 4, f("ls_cap_" + cap + "_" + t, Direction.SOUTH));
                w.set(xb, F + 3, z0 + 4, f("ls_bar_empty", Direction.SOUTH));
            }
            if (done) {
                String[] q = {"l", "m", "r"};
                for (int t = 0; t < 3; t++) w.set(x0 + 4 + t, F + 1, z0 + 5, ModBlocks.state("ls_done_" + q[t]));
            }
        }
    }
}
