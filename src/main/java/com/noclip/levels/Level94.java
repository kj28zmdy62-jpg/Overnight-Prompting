package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 94, "Motion". Green rolling hills of modelling clay under a cartoon sky.
 * Every texture boils from frame to frame like stop-motion animation. Somewhere
 * on the hills stands a single house with its windows lit and a television on.
 * The clay people move only when you do.
 */
public final class Level94 {
    static final String ID = "level_94";
    static final int REGION = 160;

    private Level94() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("94").title("Motion").order(940)
                .tagline("Everything here is animated one frame at a time. Including you, now.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Chimney())
                .visuals(new Visuals().tint(1.04f, 1.0f, 0.96f).saturation(1.3f).contrast(1.1f).brightness(1.08f)
                        .grain(0.035f).aberration(0.0008f).vignette(0.35f).warp(0.01f).scanlines(0f).flicker(0.05f)
                        .bloom(0.35f).fog(40f, 170f).fogColor(0xBFE0F8).sky().clouds())
                .sanityDrain(0.025f)
                .spawnY(20, 70)
                .spawn(SpawnRule.of(ModEntities.lazy("motion_clay_figure"), 5, 0.35f).dist(18, 36).weight(1))
                .exit(ExitType.SECRET, "level_37", 2).exit(ExitType.SECRET, "level_188", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "level_37", 1)
                .build();
    }

    static int height(long s, int x, int z) {
        double n = Hash.fbm(s ^ 0x94, x / 74.0, z / 74.0, 3);
        double m = Hash.noise(s ^ 0x95, x / 23.0, z / 23.0);
        return 16 + (int) Math.round(n * 34 + m * 3);
    }

    /** Centre of the one house in a region, and the height of its floor. */
    static int[] house(long s, int rx, int rz) {
        long h = Hash.hash(s, rx, rz, 94);
        int x = rx * REGION + 30 + Hash.range(h, REGION - 60);
        int z = rz * REGION + 30 + Hash.range(h >>> 16, REGION - 60);
        return new int[]{x, z, height(s, x, z) + 1};
    }

    static final class Gen implements LevelGenerator {
        final BlockState grass = ModBlocks.state("motion_clay_grass");
        final BlockState dirt = ModBlocks.state("motion_clay_dirt");
        final BlockState[] flowers = {ModBlocks.state("motion_clay_flower_red"), ModBlocks.state("motion_clay_flower_blue"),
                ModBlocks.state("motion_clay_flower_pink")};
        final BlockState leaves = ModBlocks.state("motion_clay_leaves");
        final BlockState trunk = ModBlocks.state("motion_clay_trunk");
        final BlockState wall = ModBlocks.state("motion_clay_wall");
        final BlockState roof = ModBlocks.state("motion_clay_roof");
        final BlockState window = ModBlocks.state("motion_clay_window");
        final BlockState floor = ModBlocks.state("motion_clay_floor");
        final BlockState rug = ModBlocks.state("motion_clay_rug");
        final BlockState tv = ModBlocks.state("motion_clay_tv");
        final BlockState chair = ModBlocks.state("motion_clay_armchair");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int h = height(s, x, z);
                    w.fill(x, 0, z, x, h - 5, z, sub);
                    w.fill(x, h - 4, z, x, h - 1, z, dirt);
                    w.set(x, h, z, grass);
                    if (Hash.chance(s, x, z, 7, 0.05f)) w.set(x, h + 1, z, flowers[Hash.range(s, x, z, 8, 3)]);
                }
            }
            CellGrid g = new CellGrid(s ^ 0x7EE, 11, 5);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                long h = g.hash(i, k, 3);
                if (Hash.unit(h) > 0.2f) return;
                int tx = x0 + 2 + Hash.range(h >>> 8, Math.max(1, wd - 4)), tz = z0 + 2 + Hash.range(h >>> 16, Math.max(1, dp - 4));
                int base = height(s, tx, tz);
                int tall = 3 + Hash.range(h >>> 24, 2);
                for (int y = base + 1; y <= base + tall; y++) w.set(tx, y, tz, trunk);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        for (int dy = 0; dy <= 2; dy++) {
                            if (Math.abs(dx) + Math.abs(dz) == 2 && dy != 1) continue;
                            if (dx == 0 && dz == 0 && dy == 0) continue;
                            w.set(tx + dx, base + tall + dy, tz + dz, leaves);
                        }
                w.set(tx, base + tall + 3, tz, leaves);
            });
            int rx0 = Math.floorDiv(w.minX - 8, REGION), rx1 = Math.floorDiv(w.maxX + 8, REGION);
            int rz0 = Math.floorDiv(w.minZ - 8, REGION), rz1 = Math.floorDiv(w.maxZ + 8, REGION);
            for (int rx = rx0; rx <= rx1; rx++)
                for (int rz = rz0; rz <= rz1; rz++) buildHouse(w, s, Level94.house(s, rx, rz));
        }

        /** The lone house: cream clay walls, a red roof, warm windows, and the TV on. */
        void buildHouse(ChunkWriter w, long s, int[] at) {
            int cx = at[0], cz = at[1], fy = at[2];
            if (cx + 8 < w.minX || cx - 8 > w.maxX || cz + 8 < w.minZ || cz - 8 > w.maxZ) return;
            for (int dx = -6; dx <= 6; dx++)
                for (int dz = -6; dz <= 6; dz++) {
                    int x = cx + dx, z = cz + dz;
                    int h = height(s, x, z);
                    for (int y = Math.min(h, fy - 1); y < fy; y++) w.set(x, y, z, y == fy - 1 ? grass : dirt);
                    w.fill(x, fy, z, x, fy + 12, z, ModBlocks.AIR);
                    if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) w.set(x, fy - 1, z, floor);
                }
            for (int dx = -4; dx <= 4; dx++)
                for (int dz = -4; dz <= 4; dz++) {
                    boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    if (!edge) continue;
                    for (int y = fy; y <= fy + 3; y++) w.set(cx + dx, y, cz + dz, wall);
                }
            // door, windows
            w.set(cx, fy, cz + 4, ModBlocks.AIR);
            w.set(cx, fy + 1, cz + 4, ModBlocks.AIR);
            for (int d : new int[]{-2, 2}) {
                w.set(cx + d, fy + 1, cz + 4, window);
                w.set(cx + d, fy + 1, cz - 4, window);
                w.set(cx - 4, fy + 1, cz + d, window);
                w.set(cx + 4, fy + 1, cz + d, window);
            }
            // gable roof in stepped clay
            for (int dz = -5; dz <= 5; dz++) {
                int y = fy + 4 + (5 - Math.abs(dz));
                for (int dx = -5; dx <= 5; dx++) {
                    w.set(cx + dx, y, cz + dz, roof);
                    if (Math.abs(dx) == 4 && Math.abs(dz) <= 4)
                        for (int yy = fy + 4; yy < y; yy++) w.set(cx + dx, yy, cz + dz, wall);
                }
            }
            for (int y = fy + 6; y <= fy + 11; y++) w.set(cx + 2, y, cz - 2, roof);
            // the living room
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) w.set(cx + dx, fy, cz + dz, rug);
            w.set(cx, fy, cz - 3, tv);
            w.set(cx, fy, cz + 1, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }
    }

    /** Smoke rises from the chimney, in jerky puffs, so you can find the house from a long way off. */
    static final class Chimney implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 6 != 0) return;
            long s = DeepEndUtil.seed(level, ID);
            int rx = Math.floorDiv(p.getBlockX(), REGION), rz = Math.floorDiv(p.getBlockZ(), REGION);
            for (int ox = -1; ox <= 1; ox++)
                for (int oz = -1; oz <= 1; oz++) {
                    int[] at = house(s, rx + ox, rz + oz);
                    double d2 = Math.pow(at[0] - p.getX(), 2) + Math.pow(at[1] - p.getZ(), 2);
                    if (d2 > 180 * 180) continue;
                    level.sendParticles(p, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, at[0] + 2.5, at[2] + 12.2, at[1] - 1.5,
                            2, 0.15, 0.1, 0.15, 0.01);
                }
        }
    }
}
