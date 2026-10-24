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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level Zero-Zero. Every surface is the same white, lit from everywhere at once,
 * so there are no edges and no shadows and a wall is only a wall when you walk
 * into it. You leave footprints, and they fade. Other footprints, bare ones,
 * do not fade: they were left by someone who found the only shadow in the
 * level, a grey patch on the floor with nothing above it to cast it. Step in.
 */
public final class ZeroZero {
    static final int FLOOR = 6, CEIL = FLOOR + 7, EXIT_GRID = 48, TRAIL = 40;

    private ZeroZero() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("zero_zero")
                .number("").title("Level Zero-Zero").order(4020)
                .tagline("No edges. No shadows. Follow the footprints that do not fade.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Footprints())
                .visuals(new Visuals().tint(1f, 1f, 1.01f).saturation(0.3f).contrast(1.0f).brightness(1.3f)
                        .grain(0.025f).aberration(0.0004f).vignette(0.08f).warp(0.01f).scanlines(0f).flicker(0f)
                        .fog(0f, 34f).fogColor(0xF0F0EE).bloom(0.9f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("zz_blank"), 1, 0.12f).dist(20, 32))
                .exit(ExitType.FALL, "level_37", 2).exit(ExitType.FALL, "salt_flats", 1).exit(ExitType.FALL, "the_void", 1)
                .exit(ExitType.FALL, "level_0", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "level_37", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    static boolean isWall(long s, CellGrid g, int x, int z) {
        int i = g.cellX(x), k = g.cellZ(z);
        int u = x - g.x0(i), v = z - g.z0(k);
        if (u == 0 && v == 0) return Hash.chance(s, i, k, 0x21, 0.5f);
        if (u == 0) {
            long h = Hash.hash(s, i, k, 0x22);
            if (Hash.unit(h) > 0.38f) return false;
            int a = Hash.range(h >>> 8, 6), len = 4 + Hash.range(h >>> 16, 8);
            return v >= a && v < a + len;
        }
        if (v == 0) {
            long h = Hash.hash(s, i, k, 0x23);
            if (Hash.unit(h) > 0.38f) return false;
            int a = Hash.range(h >>> 8, 6), len = 4 + Hash.range(h >>> 16, 8);
            return u >= a && u < a + len;
        }
        // a free-standing pillar in some rooms
        long h = Hash.hash(s, i, k, 0x24);
        if (Hash.unit(h) < 0.2f) {
            int px = 3 + Hash.range(h >>> 8, Math.max(1, g.width(i) - 6)), pz = 3 + Hash.range(h >>> 16, Math.max(1, g.depth(k) - 6));
            return (u == px || u == px + 1) && (v == pz || v == pz + 1);
        }
        return false;
    }

    /** Exit point of an exit-grid cell, or null. */
    static int[] exitAt(long s, int ei, int ek) {
        long h = Hash.hash(s, ei, ek, 0xE417);
        if (Hash.unit(h) > 0.55f) return null;
        return new int[]{ei * EXIT_GRID + 8 + Hash.range(h >>> 8, EXIT_GRID - 16), ek * EXIT_GRID + 8 + Hash.range(h >>> 20, EXIT_GRID - 16)};
    }

    static boolean nearExit(long s, int x, int z) {
        int ei = Math.floorDiv(x, EXIT_GRID), ek = Math.floorDiv(z, EXIT_GRID);
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) {
                int[] e = exitAt(s, ei + a, ek + b);
                if (e != null && Math.abs(x - e[0]) <= 4 && Math.abs(z - e[1]) <= 4) return true;
            }
        return false;
    }

    static final class Gen implements LevelGenerator {
        final BlockState white = ModBlocks.state("zz_white");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState pit = ModBlocks.state("bottomless_pit");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 14, 4);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, FLOOR, w.minZ, w.maxX, FLOOR, w.maxZ, white);
            w.fill(w.minX, CEIL, w.minZ, w.maxX, CEIL + 2, w.maxZ, white);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++)
                    if (isWall(s, g, x, z) && !nearExit(s, x, z)) w.fill(x, FLOOR + 1, z, x, CEIL - 1, z, white);
            // the shadows, and the bare footprints that lead to them
            int ei0 = Math.floorDiv(w.minX, EXIT_GRID), ek0 = Math.floorDiv(w.minZ, EXIT_GRID);
            for (int ei = ei0 - 1; ei <= ei0 + 1; ei++)
                for (int ek = ek0 - 1; ek <= ek0 + 1; ek++) {
                    int[] e = exitAt(s, ei, ek);
                    if (e == null) continue;
                    String[] q = {"tl", "tr", "bl", "br"};
                    for (int n = 0; n < 4; n++) {
                        int x = e[0] + (n & 1), z = e[1] + (n >> 1);
                        w.fill(x, 1, z, x, FLOOR - 1, z, ModBlocks.AIR);
                        w.set(x, 1, z, pit);
                        w.set(x, FLOOR, z, ModBlocks.state("zz_shadow_" + q[n]));
                    }
                    for (int t = 0; t < 2; t++) trail(w, s, g, ei, ek, t, e);
                }
        }

        void trail(ChunkWriter w, long s, CellGrid g, int ei, int ek, int t, int[] e) {
            long h = Hash.hash(s, ei, ek, 0x7A11 + t);
            double ang = Hash.unit(h) * Math.PI * 2;
            double sx = e[0] + 0.5 + Math.cos(ang) * TRAIL, sz = e[1] + 0.5 + Math.sin(ang) * TRAIL;
            double dx = e[0] + 1 - sx, dz = e[1] + 1 - sz;
            double len = Math.sqrt(dx * dx + dz * dz);
            Direction toes = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            BlockState fp = ModBlocks.state("zz_footprints_bare").setValue(HorizontalDirectionalBlock.FACING, toes);
            double wob = 1.5 + Hash.unit(h >>> 20) * 2;
            for (double d = 0; d < len - 4; d += 1.6) {
                double f = d / len;
                double side = Math.sin(d * 0.15 + t) * wob;
                int x = (int) Math.floor(sx + dx * f - dz / len * side);
                int z = (int) Math.floor(sz + dz * f + dx / len * side);
                if (!w.contains(x, z) || isWall(s, g, x, z)) continue;
                w.set(x, FLOOR + 1, z, fp);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Your footprints, and how long they last. */
    static final class Footprints implements LevelBehavior {
        record Mark(BlockPos pos, long expires) {}

        private final Deque<Mark> marks = new ArrayDeque<>();
        private final Map<UUID, double[]> last = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || !p.onGround()) return;
            double[] l = last.computeIfAbsent(p.getUUID(), u -> new double[]{p.getX(), p.getZ()});
            double dx = p.getX() - l[0], dz = p.getZ() - l[1];
            if (dx * dx + dz * dz < 1.6 * 1.6) return;
            l[0] = p.getX();
            l[1] = p.getZ();
            BlockPos at = p.blockPosition();
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).is(ModBlocks.get("zz_white"))) return;
            Direction d = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            level.setBlock(at, ModBlocks.state("zz_footprints").setValue(HorizontalDirectionalBlock.FACING, d), Block.UPDATE_CLIENTS);
            synchronized (marks) {
                marks.add(new Mark(at, level.getGameTime() + 20 * 45));
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            synchronized (marks) {
                while (!marks.isEmpty() && marks.peekFirst().expires() <= now) {
                    BlockPos pos = marks.pollFirst().pos();
                    if (level.isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.get("zz_footprints")))
                        level.setBlock(pos, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
