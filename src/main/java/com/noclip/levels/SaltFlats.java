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
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Salt Flats. White crust in every direction, patches of brine so still they hold the
 * sky. Somewhere out here a single door stands in its frame with nothing behind it.
 * You will see other doors too; walk up to them and they are not there. Only the real
 * door casts a shadow.
 */
public final class SaltFlats {
    static final String ID = "salt_flats";
    static final int G = 40;
    static final int REGION = 192;
    static final int MIRAGES = 6;

    private SaltFlats() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Salt Flats").order(3100)
                .tagline("White ground forever. One door. Only one of them is real.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Mirages())
                .visuals(new Visuals().tint(1.02f, 1.02f, 1.05f).saturation(0.82f).contrast(1.12f).brightness(1.1f)
                        .grain(0.04f).aberration(0.0012f).vignette(0.32f).warp(0.035f).scanlines(0.02f)
                        .fog(18f, 110f).fogColor(0xE6EEF4).bloom(0.9f).sky())
                .sanityDrain(0.045f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(30, 46))
                .exit(ExitType.DOOR, "zero_zero", 2).exit(ExitType.DOOR, "the_hub", 1).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.WATER, "level_7", 2).exit(ExitType.WATER, "the_dock", 1)
                .exit(ExitType.NOCLIP, "zero_zero", 2).exit(ExitType.NOCLIP, "static_fields", 1)
                .exit(ExitType.NOCLIP, "silent_beach", 1).exit(ExitType.NOCLIP, "snow_plains", 1)
                .build();
    }

    static CellGrid regions(long s) {
        return new CellGrid(s ^ 0xD00L, REGION, 0);
    }

    static BlockPos realDoor(long s, CellGrid g, int i, int k) {
        long h = g.hash(i, k, 1);
        return new BlockPos(g.x0(i) + 40 + Hash.range(h, REGION - 80), G + 1, g.z0(k) + 40 + Hash.range(h >>> 20, REGION - 80));
    }

    static BlockPos mirage(long s, CellGrid g, int i, int k, int n) {
        long h = g.hash(i, k, 100 + n);
        BlockPos real = realDoor(s, g, i, k);
        int x = g.x0(i) + 10 + Hash.range(h, REGION - 20), z = g.z0(k) + 10 + Hash.range(h >>> 20, REGION - 20);
        if (Math.abs(x - real.getX()) < 30 && Math.abs(z - real.getZ()) < 30) return null;
        return new BlockPos(x, G + 1, z);
    }

    static boolean wet(long s, int x, int z) {
        return Hash.fbm(s ^ 0xB121EL, x / 46.0, z / 46.0, 3) > 0.6f;
    }

    static final class Gen implements LevelGenerator {
        final BlockState crust = ModBlocks.state("sl_crust");
        final BlockState crustB = ModBlocks.state("sl_crust_b");
        final BlockState shadow = ModBlocks.state("sl_crust_shadow");
        final BlockState brine = ModBlocks.state("sl_brine");
        final BlockState sky = ModBlocks.state("sl_mirror_sky");
        final BlockState salt = ModBlocks.state("sl_salt");
        final BlockState track = ModBlocks.state("sl_track");
        final BlockState line = ModBlocks.state("sl_line");
        final BlockState prints = ModBlocks.state("sl_footprints");
        final BlockState poolWall = ModBlocks.state("sl_pool_wall");
        final BlockState poolBottom = ModBlocks.state("sl_pool_bottom");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = regions(s);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.fill(x, 0, z, x, G - 6, z, sub);
                    w.fill(x, G - 5, z, x, G - 1, z, salt);
                    if (Math.floorMod(z, 256) == 128) {
                        w.set(x, G, z, line);
                    } else if (wet(s, x, z) && !nearDoor(s, g, x, z)) {
                        w.set(x, G, z, brine);
                        w.set(x, G - 1, z, sky);
                    } else {
                        w.set(x, G, z, Hash.unit(s, x, z, 2) < 0.5f ? crust : crustB);
                    }
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                BlockPos d = realDoor(s, g, i, k);
                door(w, d, "sl_door_");
                // the only shadow on the flats
                for (int n = 1; n <= 3; n++) w.set(d.getX() + n, G, d.getZ(), shadow);
                for (int n = 0; n < MIRAGES; n++) {
                    BlockPos m = mirage(s, g, i, k, n);
                    if (m != null) door(w, m, "sl_mirage_");
                }
            });
            pieces(w, s);
        }

        boolean nearDoor(long s, CellGrid g, int x, int z) {
            BlockPos d = realDoor(s, g, g.cellX(x), g.cellZ(z));
            return Math.abs(d.getX() - x) <= 5 && Math.abs(d.getZ() - z) <= 5;
        }

        void door(ChunkWriter w, BlockPos p, String prefix) {
            BlockState lower = ModBlocks.state(prefix + "lower"), upper = ModBlocks.state(prefix + "upper"), top = ModBlocks.state(prefix + "top");
            if (lower.hasProperty(HorizontalDirectionalBlock.FACING)) {
                lower = OutsideKit.facing(lower, Direction.NORTH);
                upper = OutsideKit.facing(upper, Direction.NORTH);
                top = OutsideKit.facing(top, Direction.NORTH);
            }
            w.set(p.getX(), G, p.getZ(), crust);
            w.set(p.getX(), G + 1, p.getZ(), lower);
            w.set(p.getX(), G + 2, p.getZ(), upper);
            w.set(p.getX(), G + 3, p.getZ(), top);
        }

        void pieces(ChunkWriter w, long s) {
            CellGrid g = new CellGrid(s ^ 0x5A17L, 48, 8);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                long h = g.hash(i, k, 9);
                float r = Hash.unit(h);
                int x = x0 + 8 + Hash.range(h >>> 8, Math.max(1, wd - 16));
                int z = z0 + 8 + Hash.range(h >>> 16, Math.max(1, dp - 16));
                Direction d = Direction.from2DDataValue(Hash.range(h >>> 24, 4));
                if (r < 0.025f) {
                    // someone set up a table to wait at, and left
                    w.set(x, G, z, crust);
                    w.set(x, G + 1, z, OutsideKit.facing("sl_folding_table", d));
                    w.set(x, G + 2, z, OutsideKit.facing("almond_water_bottle", d));
                    w.set(x + d.getStepX() * 2, G + 1, z + d.getStepZ() * 2, OutsideKit.facing("sl_folding_chair", d.getOpposite()));
                } else if (r < 0.05f) {
                    w.fill(x, G + 1, z, x, G + 2, z, OutsideKit.facing("sl_survey_pole", Direction.NORTH));
                    w.set(x, G + 3, z, OutsideKit.facing("sl_survey_flag", d));
                } else if (r < 0.075f) {
                    // tyre tracks that start and stop in the middle of nowhere
                    int len = 30 + Hash.range(h >>> 30, 30);
                    boolean alongX = (h & 1) == 0;
                    for (int n = 0; n < len; n++) {
                        int xx = alongX ? x + n : x, zz = alongX ? z : z + n;
                        if (w.contains(xx, zz) && !w.get(xx, G, zz).is(ModBlocks.get("sl_brine"))) w.set(xx, G, zz, track);
                    }
                } else if (r < 0.09f) {
                    // footprints walking away, then none
                    for (int n = 0; n < 9; n++) {
                        int zz = z + n * 2;
                        if (w.contains(x, zz) && !w.get(x, G, zz).is(ModBlocks.get("sl_brine"))) w.set(x, G, zz, prints);
                    }
                } else if (r < 0.1f) {
                    w.set(x, G + 1, z, OutsideKit.facing("sl_bones", d));
                } else if (r < 0.108f) {
                    OutsideKit.car(w, x, G, z, d, "red", false);
                } else if (r < 0.122f) {
                    // a brine pool, much deeper than it should be
                    for (int dx = -4; dx <= 4; dx++)
                        for (int dz = -4; dz <= 4; dz++) {
                            int dd = dx * dx + dz * dz;
                            if (dd > 16) continue;
                            int xx = x + dx, zz = z + dz;
                            if (!w.contains(xx, zz)) continue;
                            if (dd > 9) {
                                w.fill(xx, G - 14, zz, xx, G, zz, poolWall);
                            } else {
                                w.fill(xx, G - 14, zz, xx, G - 14, zz, poolWall);
                                w.set(xx, G - 13, zz, poolBottom);
                                w.fill(xx, G - 12, zz, xx, G, zz, water);
                            }
                        }
                }
            });
        }
    }

    /** Mirage doors melt away when you get close. The real one hums. */
    static final class Mirages implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (!OutsideKit.autotest()) return;
            long s = OutsideKit.seed(level, ID);
            CellGrid g = regions(s);
            BlockPos d = realDoor(s, g, g.cellX(p.getBlockX()), g.cellZ(p.getBlockZ()));
            level.getChunk(d);
            p.teleportTo(level, d.getX() - 5.5, G + 1, d.getZ() + 7.5, 145f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0) return;
            long s = OutsideKit.seed(level, ID);
            CellGrid g = regions(s);
            int ci = g.cellX(p.getBlockX()), ck = g.cellZ(p.getBlockZ());
            Block mirage = ModBlocks.get("sl_mirage_lower");
            for (int i = ci - 1; i <= ci + 1; i++)
                for (int k = ck - 1; k <= ck + 1; k++) {
                    for (int n = 0; n < MIRAGES; n++) {
                        BlockPos m = mirage(s, g, i, k, n);
                        if (m == null) continue;
                        double dx = m.getX() + 0.5 - p.getX(), dz = m.getZ() + 0.5 - p.getZ();
                        if (dx * dx + dz * dz > 15 * 15 || !level.isLoaded(m)) continue;
                        if (!level.getBlockState(m).is(mirage)) continue;
                        for (int y = 0; y < 3; y++) level.setBlock(m.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        OutsideKit.sound(level, m, "event.salt_flats_shimmer", 1.2f, 1f);
                        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.35f));
                    }
                    if (p.tickCount % 120 == 0) {
                        BlockPos d = realDoor(s, g, i, k);
                        if (d.distSqr(p.blockPosition()) < 56 * 56) OutsideKit.sound(level, d, "event.salt_flats_tone", 3f, 1f);
                    }
                }
        }
    }
}
