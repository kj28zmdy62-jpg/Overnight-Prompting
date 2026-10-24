package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Level 0, "The Lobby". An endless, irregular office space: mono-yellow
 * wallpaper, moist carpet, buzzing fluorescent lights. Mostly empty, which is
 * the problem. The layout quietly rearranges itself behind you.
 */
public final class Level0 {
    static final int FLOOR = 10, CEIL = 14;

    private Level0() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_0")
                .number("0").title("The Lobby").order(0)
                .tagline("Mono-yellow. Moist carpet. The hum never stops.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Shifting())
                .visuals(new Visuals().tint(1.08f, 1.02f, 0.80f).brightness(1.22f).saturation(0.86f).contrast(1.04f)
                        .grain(0.07f).aberration(0.0012f).vignette(0.5f).fog(3f, 42f).fogColor(0xB9A95E))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.NOCLIP, "level_1", 8).exit(ExitType.NOCLIP, "level_2", 2)
                .exit(ExitType.NOCLIP, "level_37", 1).exit(ExitType.NOCLIP, "level_fun", 1)
                .exit(ExitType.NOCLIP, "level_run", 1).exit(ExitType.NOCLIP, "manila_room", 1)
                .exit(ExitType.FALL, "level_1", 3).exit(ExitType.FALL, "level_27", 1)
                .exit(ExitType.DOOR, "level_2", 3).exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "level_fun", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Edge { OPEN, WALL, DOOR, HALF_A, HALF_B }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("moist_carpet");
        final BlockState damp = ModBlocks.state("damp_carpet");
        final BlockState paper = ModBlocks.state("yellow_wallpaper");
        final BlockState stained = ModBlocks.state("yellow_wallpaper_stained");
        final BlockState base = ModBlocks.state("yellow_wallpaper_baseboard");
        final BlockState peeling = ModBlocks.state("yellow_wallpaper_peeling");
        final BlockState outlet = ModBlocks.state("yellow_wallpaper_outlet");
        final BlockState smiley = ModBlocks.state("yellow_wallpaper_smiley");
        final BlockState ceiling = ModBlocks.state("ceiling_tile");
        final BlockState ceilingStained = ModBlocks.state("stained_ceiling_tile");
        final BlockState ceilingMissing = ModBlocks.state("missing_ceiling_tile");
        final BlockState light = ModBlocks.state("fluorescent_light");
        final BlockState flicker = ModBlocks.state("flickering_fluorescent_light");
        final BlockState dead = ModBlocks.state("dead_fluorescent_light");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState doorway = ModBlocks.state("dark_doorway");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState chair = ModBlocks.state("office_chair");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL + 1, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean wet = Hash.fbm(s ^ 0x77, x / 14.0, z / 14.0, 3) > 0.66;
                    w.set(x, FLOOR, z, wet ? damp : carpet);
                    if (isWall(s, x, z)) {
                        for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, wallBlock(s, x, y, z));
                        w.set(x, CEIL, z, ceiling);
                    } else {
                        w.set(x, CEIL, z, ceilingAt(s, x, z, wet));
                    }
                }
            }
            features(w, s);
        }

        BlockState wallBlock(long s, int x, int y, int z) {
            if (y == FLOOR + 1) return base;
            float n = Hash.fbm(s ^ 0x5151, x / 9.0 + y * 0.3, z / 9.0, 2);
            if (y == FLOOR + 2 && Hash.chance(s, x * 31 + 7, z, 0.015f)) return outlet;
            if (Hash.chance(s, x, y, z, 0.02f)) return peeling;
            return n > 0.63 ? stained : paper;
        }

        BlockState ceilingAt(long s, int x, int z, boolean wet) {
            if (isLightSpot(x, z)) {
                float dark = Hash.noise(s ^ 0xDA4C, x / 40.0, z / 40.0);
                int panel = Math.floorDiv(x, 4) * 7919 + Math.floorDiv(z, 4);
                float r = Hash.unit(s, panel, 11);
                if (dark > 0.72f) return r < 0.06f ? light : r < 0.22f ? flicker : dead;
                return r < 0.07f ? flicker : r < 0.12f ? dead : light;
            }
            float r = Hash.unit(s, x, z, 3);
            if (r < 0.008f) return ceilingMissing;
            if (r < (wet ? 0.25f : 0.04f)) return ceilingStained;
            return ceiling;
        }

        static boolean isLightSpot(int x, int z) {
            return Math.floorMod(x, 4) <= 1 && Math.floorMod(z, 4) == 2;
        }

        // grid lines are jittered so rooms come in uneven sizes (6 to 10 blocks)
        static int line(long s, long axis, int i) {
            return i * 8 + Hash.range(s, axis, i, 3);
        }

        static int cell(long s, long axis, int c) {
            int i = Math.floorDiv(c, 8);
            return c < line(s, axis, i) ? i - 1 : i;
        }

        static float zone(long s, int x, int z) {
            return Hash.noise(s ^ 0x20E, x / 56.0, z / 56.0);
        }

        boolean isWall(long s, int x, int z) {
            int i = cell(s, 1, x), k = cell(s, 2, z);
            int x0 = line(s, 1, i), z0 = line(s, 2, k);
            int lx = line(s, 1, i + 1) - x0, lz = line(s, 2, k + 1) - z0;
            float zone = zone(s, x0 + lx / 2, z0 + lz / 2);
            boolean pillars = zone < 0.16f;
            if (x == x0 && z == z0) return true;
            if (pillars) {
                int u = x - x0, v = z - z0;
                return u % 4 == 0 && v % 4 == 0 && Hash.chance(s, x, z, 9, 0.8f);
            }
            if (x == x0) return edgeSolid(s, 0, i, k, z - z0, lz, zone);
            if (z == z0) return edgeSolid(s, 1, i, k, x - x0, lx, zone);
            // short partial walls jutting into a room
            long ch = Hash.hash(s, i, k, 77);
            if (Hash.unit(ch) < 0.09f) {
                int len = 2 + Hash.range(ch >>> 8, 3);
                boolean along = (ch & 1) == 0;
                int mid = along ? x0 + lx / 2 : z0 + lz / 2;
                if (along && x == mid && z - z0 <= len) return true;
                if (!along && z == mid && x - x0 <= len) return true;
            }
            return false;
        }

        boolean sealed(long s, int i, int k) {
            return Hash.chance(s, i, k, 404, 0.006f);
        }

        Edge edge(long s, int dir, int i, int k, float zone) {
            if (sealed(s, i, k) || (dir == 0 ? sealed(s, i - 1, k) : sealed(s, i, k - 1))) return Edge.WALL;
            if (doorwayCell(s, i, k) && dir == 0) return Edge.WALL;
            float r = Hash.unit(s, i, k, 100 + dir);
            boolean dense = zone > 0.8f;
            float open = dense ? 0.14f : 0.34f;
            if (r < open) return Edge.OPEN;
            if (r < open + 0.24f) return Edge.WALL;
            if (r < open + 0.5f) return Edge.DOOR;
            return r < open + 0.58f ? Edge.HALF_A : Edge.HALF_B;
        }

        boolean edgeSolid(long s, int dir, int i, int k, int u, int len, float zone) {
            switch (edge(s, dir, i, k, zone)) {
                case OPEN: return false;
                case WALL: return true;
                case HALF_A: return u < len / 2;
                case HALF_B: return u >= len / 2;
                default:
                    long h = Hash.hash(s, i, k, 200 + dir);
                    int g = 1 + Hash.range(h, Math.max(1, len - 3));
                    int width = 1 + (int) ((h >>> 20) & 1);
                    return u < g || u >= g + width;
            }
        }

        static boolean doorwayCell(long s, int i, int k) {
            return Hash.chance(s, i, k, 303, 0.007f);
        }

        /** Per-room set pieces: pits, doorways, bottles, chairs, scrawls. */
        void features(ChunkWriter w, long s) {
            int i0 = cell(s, 1, w.minX) - 1, i1 = cell(s, 1, w.maxX) + 1;
            int k0 = cell(s, 2, w.minZ) - 1, k1 = cell(s, 2, w.maxZ) + 1;
            for (int i = i0; i <= i1; i++) {
                for (int k = k0; k <= k1; k++) {
                    int x0 = line(s, 1, i), z0 = line(s, 2, k);
                    int lx = line(s, 1, i + 1) - x0, lz = line(s, 2, k + 1) - z0;
                    int cx = x0 + lx / 2, cz = z0 + lz / 2;
                    if (zone(s, cx, cz) < 0.16f) continue;
                    long h = Hash.hash(s, i, k, 999);
                    float r = Hash.unit(h);
                    if (doorwayCell(s, i, k)) {
                        int z = z0 + lz / 2;
                        w.set(x0, FLOOR + 1, z, doorway);
                        w.set(x0, FLOOR + 2, z, doorway);
                    } else if (r < 0.010f) {
                        for (int x = cx - 1; x <= cx; x++)
                            for (int z = cz - 1; z <= cz; z++) {
                                for (int y = 1; y <= FLOOR; y++) w.set(x, y, z, y <= 3 ? pit : ModBlocks.AIR);
                            }
                    } else if (r < 0.040f) {
                        // a bottle left against a wall
                        int bx = x0 + 1 + Hash.range(h >>> 10, Math.max(1, lx - 2));
                        w.set(bx, FLOOR + 1, z0 + 1, bottle.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    } else if (r < 0.055f) {
                        Direction d = Direction.from2DDataValue(Hash.range(h >>> 12, 4));
                        int ox = cx + Hash.range(h >>> 16, 3) - 1, oz = cz + Hash.range(h >>> 20, 3) - 1;
                        w.set(ox, FLOOR + 1, oz, chair.setValue(HorizontalDirectionalBlock.FACING, d));
                    } else if (r < 0.060f) {
                        // someone drew on this wall
                        int z = z0 + lz / 2;
                        if (w.contains(x0, z) && w.get(x0, FLOOR + 2, z).is(paper.getBlock())) w.set(x0, FLOOR + 2, z, smiley);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Every so often a piece of wall behind the player, somewhere they cannot
     * see, disappears, or a new one grows across an empty corridor. Nobody ever
     * sees it happen.
     */
    static final class Shifting implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 200 != 0 || p.getRandom().nextFloat() > 0.6f) return;
            BlockState paper = ModBlocks.state("yellow_wallpaper");
            BlockState base = ModBlocks.state("yellow_wallpaper_baseboard");
            for (int attempt = 0; attempt < 10; attempt++) {
                double ang = Math.toRadians(p.getYRot() + 180 + (p.getRandom().nextFloat() - 0.5f) * 140);
                double d = 10 + p.getRandom().nextFloat() * 16;
                BlockPos col = BlockPos.containing(p.getX() - Math.sin(ang) * d, FLOOR + 1, p.getZ() + Math.cos(ang) * d);
                if (!level.isLoaded(col) || seen(p, col)) continue;
                BlockState bottom = level.getBlockState(col);
                if (bottom.is(base.getBlock())) {
                    for (int y = 0; y < 3; y++) level.setBlock(col.above(y), ModBlocks.AIR, Block.UPDATE_CLIENTS);
                    return;
                }
                if (bottom.isAir() && level.getBlockState(col.above()).isAir() && level.getBlockState(col.above(2)).isAir()
                        && level.getEntities(null, new net.minecraft.world.phys.AABB(col).inflate(1)).isEmpty()) {
                    int walls = 0;
                    for (Direction dir : Direction.Plane.HORIZONTAL) if (level.getBlockState(col.relative(dir)).is(base.getBlock())) walls++;
                    if (walls == 1) {
                        level.setBlock(col, base, Block.UPDATE_CLIENTS);
                        level.setBlock(col.above(), paper, Block.UPDATE_CLIENTS);
                        level.setBlock(col.above(2), paper, Block.UPDATE_CLIENTS);
                        return;
                    }
                }
            }
        }

        private static boolean seen(ServerPlayer p, BlockPos pos) {
            Vec3 c = Vec3.atCenterOf(pos.above());
            Vec3 to = c.subtract(p.getEyePosition()).normalize();
            if (p.getViewVector(1f).dot(to) < 0.2) return false;
            return p.level().clip(new ClipContext(p.getEyePosition(), c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))
                    .getType() == HitResult.Type.MISS;
        }
    }
}
