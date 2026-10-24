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
import com.noclip.world.Blackouts;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 1, "Habitable Zone". A concrete warehouse that never ends, lit by
 * hanging strips, misted with fog, stocked with crates nobody delivered. The
 * power fails every few minutes, and the dark brings company.
 */
public final class Level1 {
    static final int FLOOR = 8, CEIL = 16;

    private Level1() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_1")
                .number("1").title("Habitable Zone").order(10)
                .tagline("Concrete, fog, and crates nobody delivered. Hold on to your flashlight.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Power())
                .visuals(new Visuals().tint(0.93f, 0.98f, 1.03f).saturation(0.68f).contrast(1.08f).brightness(1.1f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.55f).fog(4f, 40f).fogColor(0x5F6468).bloom(0.45f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 3, 0.55f).dark().dist(12, 26).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.25f).dist(20, 40).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.12f).dist(30, 44).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("lurker"), 1, 0.06f).dist(6, 10).weight(1))
                .exit(ExitType.NOCLIP, "level_2", 5).exit(ExitType.NOCLIP, "level_3", 2).exit(ExitType.NOCLIP, "level_4", 1)
                .exit(ExitType.DESCEND, "level_2", 1)
                .exit(ExitType.DOOR, "level_3", 2).exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "level_5", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("warehouse_concrete");
        final BlockState painted = ModBlocks.state("painted_concrete");
        final BlockState wet = ModBlocks.state("wet_concrete");
        final BlockState wall = ModBlocks.state("concrete_wall");
        final BlockState pillar = ModBlocks.state("concrete_pillar");
        final BlockState hazard = ModBlocks.state("hazard_pillar");
        final BlockState ceiling = ModBlocks.state("warehouse_ceiling");
        final BlockState strip = ModBlocks.state("strip_light");
        final BlockState stripOff = ModBlocks.state("strip_light_off");
        final BlockState crate = ModBlocks.state("wooden_crate");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState shelf = ModBlocks.state("metal_shelf");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState fence = ModBlocks.state("chain_link_fence");
        final BlockState door = ModBlocks.state("metal_service_door");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState stairs = ModBlocks.state("concrete_stairs");
        final BlockState dark = ModBlocks.state("stairwell_dark");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 12, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL + 1, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    float damp = Hash.fbm(s ^ 0xBEEF, x / 18.0, z / 18.0, 3);
                    BlockState f = damp > 0.64f ? wet : (Math.floorMod(x, 24) == 6 ? painted : floor);
                    w.set(x, FLOOR, z, f);
                    if (damp > 0.69f && Hash.chance(s, x, z, 5, 0.5f)) w.set(x, FLOOR + 1, z, puddle);
                    w.set(x, CEIL, z, ceiling);
                    if (Math.floorMod(x, 6) == 3 && Math.floorMod(z, 6) == 3) {
                        w.set(x, CEIL - 1, z, Hash.chance(s, x, z, 9, 0.13f) ? stripOff : strip);
                    }
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, g, s, i, k, x0, z0));
        }

        void cell(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0) {
            // 2x2 pillar at every grid corner, hazard-striped at the base
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) {
                    w.set(x0 + dx, FLOOR + 1, z0 + dz, hazard);
                    w.fill(x0 + dx, FLOOR + 2, z0 + dz, x0 + dx, CEIL - 1, z0 + dz, pillar);
                }
            // long dividing walls on some grid lines, with a wide gap
            if (Hash.chance(s, i, k, 31, 0.16f)) {
                int gap = 3 + Hash.range(s, i, k, 32, 5);
                for (int dz = 2; dz < 12; dz++) {
                    if (dz >= gap && dz < gap + 3) continue;
                    w.fill(x0, FLOOR + 1, z0 + dz, x0, CEIL - 1, z0 + dz, wall);
                }
            }
            long h = g.hash(i, k, 77);
            float r = Hash.unit(h);
            int cx = x0 + 6, cz = z0 + 6;
            if (r < 0.20f) {
                // crate stacks
                for (int n = 0; n < 5; n++) {
                    long hh = Hash.hash(h, n);
                    int ox = x0 + 3 + Hash.range(hh, 8), oz = z0 + 3 + Hash.range(hh >>> 8, 8);
                    int height = 1 + Hash.range(hh >>> 16, 3);
                    for (int y = 0; y < height; y++) w.set(ox, FLOOR + 1 + y, oz, crate);
                    if (Hash.unit(hh >>> 24) < 0.4f) w.set(ox + 1, FLOOR + 1, oz, crate);
                }
            } else if (r < 0.36f) {
                // aisles of shelving
                for (int row = 0; row < 3; row++) {
                    int x = x0 + 3 + row * 3;
                    for (int z = z0 + 3; z <= z0 + 9; z++) {
                        BlockState sh = shelf.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                        w.set(x, FLOOR + 1, z, sh);
                        w.set(x, FLOOR + 2, z, sh);
                    }
                }
            } else if (r < 0.44f) {
                for (int n = 0; n < 4; n++) {
                    long hh = Hash.hash(h, n + 9);
                    w.set(x0 + 3 + Hash.range(hh, 7), FLOOR + 1, z0 + 3 + Hash.range(hh >>> 8, 7),
                            boxes.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(hh >>> 16, 4))));
                }
            } else if (r < 0.49f) {
                // a fenced cage with something inside
                for (int d = 0; d <= 6; d++) {
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                        w.set(cx - 3 + d, y, cz - 3, fence);
                        w.set(cx - 3 + d, y, cz + 3, fence);
                        w.set(cx - 3, y, cz - 3 + d, fence);
                        if (d != 3) w.set(cx + 3, y, cz - 3 + d, fence);
                    }
                }
                w.set(cx, FLOOR + 1, cz, Hash.unit(h >>> 30) < 0.45f ? supply : crate);
                w.set(cx - 1, FLOOR + 1, cz + 1, crate);
            } else if (r < 0.52f) {
                w.set(cx, FLOOR + 1, cz, supply);
            } else if (r < 0.535f) {
                // a stairwell going down into the dark
                for (int dx = -1; dx <= 1; dx++) {
                    for (int step = 0; step < 6; step++) {
                        int y = FLOOR - step;
                        w.set(cx + dx, y, cz - step, stairs.setValue(StairBlock.FACING, Direction.SOUTH));
                        for (int yy = y + 1; yy <= FLOOR; yy++) w.set(cx + dx, yy, cz - step, ModBlocks.AIR);
                    }
                    for (int step = 6; step < 8; step++) {
                        w.fill(cx + dx, FLOOR - 7, cz - step, cx + dx, FLOOR, cz - step, ModBlocks.AIR);
                        w.set(cx + dx, FLOOR - 7, cz - step, dark);
                        w.set(cx + dx, FLOOR - 6, cz - step, dark);
                    }
                    w.set(cx + dx, FLOOR + 1, cz + 1, fence);
                }
                for (int step = -1; step < 8; step++) {
                    w.set(cx - 2, FLOOR + 1, cz - step, fence);
                    w.set(cx + 2, FLOOR + 1, cz - step, fence);
                }
            } else if (r < 0.555f) {
                // a free-standing wall with a door in it
                for (int d = -3; d <= 3; d++) w.fill(cx + d, FLOOR + 1, cz, cx + d, CEIL - 1, cz, wall);
                w.set(cx, FLOOR + 1, cz, door);
                w.set(cx, FLOOR + 2, cz, door);
            } else if (r < 0.58f) {
                w.set(cx + 2, FLOOR + 1, cz - 1, bottle.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
        }
    }

    /** The power fails every few minutes. Something always notices. */
    static final class Power implements LevelBehavior {
        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            if (Blackouts.active(level)) return;
            if (level.random.nextInt(20 * 210) == 0) Blackouts.start(level, 20 * (25 + level.random.nextInt(25)));
        }
    }
}
