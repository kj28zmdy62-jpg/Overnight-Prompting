package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 6, "Lights Out". Narrow cinder-block corridors with no light source of
 * any kind. Your flashlight is the only thing between you and what lives here.
 * Somewhere a green EXIT sign glows.
 */
public final class Level6 {
    static final int FLOOR = 6, CEIL = 10;

    private Level6() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_6")
                .number("6").title("Lights Out").order(60)
                .tagline("There are no lights here. Only what you brought.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .visuals(new Visuals().darkness(1.0f).grain(0.12f).aberration(0.002f).vignette(0.7f)
                        .saturation(0.6f).fog(2f, 22f).fogColor(0x000000).bloom(0.6f).flicker(0.04f))
                .sanityDrain(0.09f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 4, 0.7f).dark().dist(10, 24).weight(5))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 3, 0.35f).dist(14, 28).weight(2))
                .exit(ExitType.NOCLIP, "level_7", 3).exit(ExitType.NOCLIP, "level_1", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.DOOR, "level_7", 3).exit(ExitType.DOOR, "level_8", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("cinder_block");
        final BlockState scratched = ModBlocks.state("scratched_cinder_block");
        final BlockState floor = ModBlocks.state("rough_concrete");
        final BlockState ceiling = ModBlocks.state("low_ceiling");
        final BlockState sign = ModBlocks.state("exit_sign");
        final BlockState door = ModBlocks.state("dark_exit_door");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            // a maze of 3-block cells: walls on cell edges, corridors two wide
            CellGrid g = new CellGrid(s, 3, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.fill(w.minX, CEIL, w.minZ, w.maxX, CEIL + 2, w.maxZ, ceiling);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (solid(g, s, x, z)) {
                        BlockState b = Hash.chance(s, x, z, 3, 0.04f) ? scratched : wall;
                        w.fill(x, FLOOR + 1, z, x, CEIL - 1, z, b);
                    }
                }
            }
            // rare exits: a door with a glowing sign above it, set into a wall
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (!Hash.chance(s, i, k, 66, 0.012f)) return;
                if (!solid(g, s, x0 + 1, z0) || solid(g, s, x0 + 1, z0 + 1)) return;
                w.set(x0 + 1, FLOOR + 1, z0, door);
                w.set(x0 + 1, FLOOR + 2, z0, door);
                w.set(x0 + 1, FLOOR + 3, z0 + 1, sign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            });
        }

        /** Corners always solid; each edge segment between corners is wall ~45% of the time. */
        boolean solid(CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int u = x - g.x0(i), v = z - g.z0(k);
            if (u == 0 && v == 0) return true;
            if (u == 0) return Hash.chance(s, i, k, 10, 0.42f);
            if (v == 0) return Hash.chance(s, i, k, 11, 0.42f);
            return false;
        }
    }
}
