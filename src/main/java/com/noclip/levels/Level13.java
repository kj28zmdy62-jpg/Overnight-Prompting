package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level 13, "Infinite Apartments". Mustard hallways, orange carpet, dome lights
 * full of dead flies. Every door says 13 and opens onto the same apartment: same
 * couch, same stove, same window looking across at the same building. Most are
 * empty. If light shows under a door, somebody is home. Do not go in.
 */
public final class Level13 {
    static final int FLOOR = 8, CEIL = 12, ROW = 14, UNIT = 6, CROSS = 48;

    private Level13() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_13")
                .number("13").title("Infinite Apartments").order(130)
                .tagline("Every apartment is number 13. If there is light under the door, someone is home.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Home())
                .visuals(new Visuals().tint(1.08f, 1.0f, 0.82f).saturation(0.85f).contrast(1.08f).brightness(1.1f)
                        .grain(0.09f).aberration(0.0016f).vignette(0.58f).fog(3f, 36f).fogColor(0x3A2A14).bloom(0.5f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.15f).dist(18, 36).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_11", 2).exit(ExitType.NOCLIP, "level_5", 1).exit(ExitType.NOCLIP, "airport", 1)
                .exit(ExitType.DESCEND, "level_5", 2).exit(ExitType.DESCEND, "level_4", 1)
                .exit(ExitType.ASCEND, "level_11", 2).exit(ExitType.ASCEND, "airport", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    /** Where a column sits: kind 0 hall, 1 unit; for units: j, row, lx, lz (lz 0 = hall wall, 5 = back wall). */
    record Cell(int kind, int j, int row, int lx, int lz, Direction front) {}

    static boolean crossSeg(long s, int m, int k) {
        return Hash.chance(s, m, k, 1302, 0.6f);
    }

    static Cell cell(long s, int x, int z) {
        int zm = Math.floorMod(z, ROW), k = Math.floorDiv(z, ROW);
        if (zm <= 1) return new Cell(0, 0, 0, 0, 0, null);
        int j = Math.floorDiv(x, UNIT), lx = Math.floorMod(x, UNIT);
        if (Math.floorMod(j, CROSS / UNIT) == 0 && crossSeg(s, Math.floorDiv(x, CROSS), k) && (lx == 1 || lx == 2))
            return new Cell(0, j, 0, lx, 0, null);
        boolean south = zm <= 7;
        int lz = south ? zm - 2 : 13 - zm;
        int row = 2 * k + (south ? 0 : 1);
        return new Cell(1, j, row, lx, lz, south ? Direction.NORTH : Direction.SOUTH);
    }

    static boolean disabled(long s, int j, int row) {
        return Math.floorMod(j, CROSS / UNIT) == 0 && crossSeg(s, Math.floorDiv(j * UNIT, CROSS), Math.floorDiv(row, 2));
    }

    static boolean occupied(long s, int j, int row) {
        return !disabled(s, j, row) && Hash.chance(s, j, row, 1301, 0.05f);
    }

    static boolean stairUnit(long s, int j, int row) {
        return !disabled(s, j, row) && !occupied(s, j, row) && Hash.chance(s, j, row, 1303, 0.025f);
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("apartment_hall_carpet");
        final BlockState paper = ModBlocks.state("apartment_wallpaper");
        final BlockState peel = ModBlocks.state("apartment_wallpaper_peeling");
        final BlockState base = ModBlocks.state("apartment_baseboard");
        final BlockState popcorn = ModBlocks.state("apartment_popcorn_ceiling");
        final BlockState dome = ModBlocks.state("apartment_dome_light");
        final BlockState domeBad = ModBlocks.state("apartment_dome_light_bad");
        final BlockState domeDead = ModBlocks.state("apartment_dome_light_dead");
        final BlockState door = ModBlocks.state("apartment_door");
        final BlockState doorLit = ModBlocks.state("apartment_door_lit");
        final BlockState stairLow = ModBlocks.state("apartment_stair_door");
        final BlockState stairTop = ModBlocks.state("apartment_stair_door_top");
        final BlockState floor = ModBlocks.state("apartment_floor");
        final BlockState lino = ModBlocks.state("apartment_linoleum");
        final BlockState wall = ModBlocks.state("apartment_wall");
        final BlockState window = ModBlocks.state("apartment_window");
        final BlockState stove = ModBlocks.state("apartment_stove");
        final BlockState fridge = ModBlocks.state("apartment_fridge");
        final BlockState lamp = ModBlocks.state("apartment_lamp");
        final BlockState lampOff = ModBlocks.state("apartment_lamp_off");
        final BlockState counter = ModBlocks.state("break_counter");
        final BlockState couch = ModBlocks.state("suburb_couch");
        final BlockState tv = ModBlocks.state("suburb_tv");
        final BlockState tvOn = ModBlocks.state("suburb_tv_on");
        final BlockState bed = ModBlocks.state("hotel_bed");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    Cell c = cell(s, x, z);
                    if (c.kind() == 0) {
                        hall(w, s, x, z, c);
                        continue;
                    }
                    boolean off = disabled(s, c.j(), c.row());
                    boolean wallCol = c.lx() == 0 || c.lz() == 0 || c.lz() == 5 || off || stairUnit(s, c.j(), c.row());
                    if (wallCol) {
                        boolean hallSide = c.lz() == 0 || (off && (c.lx() == 0 || c.lx() == 3));
                        w.set(x, FLOOR, z, floor);
                        for (int y = FLOOR + 1; y < CEIL; y++)
                            w.set(x, y, z, hallSide ? (y == FLOOR + 1 ? base : Hash.chance(s, x, z, y, 0.06f) ? peel : paper) : wall);
                        w.set(x, CEIL, z, popcorn);
                    } else {
                        w.set(x, FLOOR, z, c.lz() == 4 ? lino : floor);
                        for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
                        w.set(x, CEIL, z, c.lx() == 3 && c.lz() == 2 ? (occupied(s, c.j(), c.row()) ? dome : domeDead) : popcorn);
                    }
                }
            // doors and furniture, unit by unit
            for (int j = Math.floorDiv(w.minX, UNIT); j <= Math.floorDiv(w.maxX, UNIT); j++)
                for (int k = Math.floorDiv(w.minZ, ROW) - 1; k <= Math.floorDiv(w.maxZ, ROW) + 1; k++) {
                    unit(w, s, j, 2 * k, k * ROW + 2, 1);
                    unit(w, s, j, 2 * k + 1, k * ROW + 13, -1);
                }
        }

        void hall(ChunkWriter w, long s, int x, int z, Cell c) {
            w.set(x, FLOOR, z, carpet);
            for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
            boolean main = Math.floorMod(z, ROW) <= 1;
            boolean light = main ? Math.floorMod(z, ROW) == 0 && Math.floorMod(x, 5) == 2 : c.lx() == 1 && Math.floorMod(z, 5) == 2;
            BlockState l = popcorn;
            if (light) {
                float r = Hash.unit(s, x, z, 1304);
                float dim = Hash.fbm(s ^ 0xD13L, x / 30.0, z / 30.0, 2);
                l = dim > 0.68f ? (r < 0.6f ? domeDead : domeBad) : r < 0.08f ? domeDead : r < 0.22f ? domeBad : dome;
            }
            w.set(x, CEIL, z, l);
        }

        /** One apartment. zWall = z of its hall wall row; dz = +1 if the unit extends toward +z. */
        void unit(ChunkWriter w, long s, int j, int row, int zWall, int dz) {
            if (disabled(s, j, row)) return;
            int x0 = j * UNIT;
            if (x0 + 6 < w.minX || x0 > w.maxX) return;
            Direction front = dz > 0 ? Direction.NORTH : Direction.SOUTH;   // toward the hall
            Direction back = front.getOpposite();
            if (stairUnit(s, j, row)) {
                w.set(x0 + 2, FLOOR + 1, zWall, stairLow);
                w.set(x0 + 2, FLOOR + 2, zWall, stairTop);
                return;
            }
            boolean home = occupied(s, j, row);
            BlockState d = (home ? doorLit : door).setValue(DoorBlock.FACING, back);
            w.set(x0 + 2, FLOOR + 1, zWall, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            w.set(x0 + 2, FLOOR + 2, zWall, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            int z1 = zWall + dz, z2 = zWall + 2 * dz, z3 = zWall + 3 * dz, z4 = zWall + 4 * dz;
            // kitchen along the back wall, window looking across at the same building
            w.set(x0 + 1, FLOOR + 1, z4, ClassicsKit.facing(fridge, back));
            w.set(x0 + 1, FLOOR + 2, z4, ClassicsKit.facing(fridge, back));
            w.set(x0 + 2, FLOOR + 1, z4, ClassicsKit.facing(stove, back));
            w.set(x0 + 3, FLOOR + 1, z4, ClassicsKit.facing(counter, back));
            w.set(x0 + 4, FLOOR + 2, zWall + 5 * dz, window);
            // living room: couch facing the television
            w.set(x0 + 4, FLOOR + 1, z3, ClassicsKit.facing(couch, front));
            w.set(x0 + 5, FLOOR + 1, z3, ClassicsKit.facing(couch, front));
            w.set(x0 + 4, FLOOR + 1, z1, ClassicsKit.facing(home ? tvOn : tv, front));
            w.set(x0 + 5, FLOOR + 1, z1, ClassicsKit.facing(home ? lamp : lampOff, front));
            w.set(x0 + 1, FLOOR + 1, z2, ClassicsKit.facing(bed, Direction.WEST));
            if (Hash.chance(s, j, row, 1305, 0.12f)) w.set(x0 + 3, FLOOR + 2, z4, bottle);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Walk into an apartment where somebody is home, and they are. */
    static final class Home implements LevelBehavior {
        private final Set<Long> visited = ConcurrentHashMap.newKeySet();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0 || p.isSpectator()) return;
            long s = Hash.hash(level.getSeed(), Hash.salt("level_13"));
            Cell c = cell(s, p.getBlockX(), p.getBlockZ());
            if (c.kind() != 1 || c.lz() < 1 || c.lz() > 4 || c.lx() < 1 || !occupied(s, c.j(), c.row())) return;
            long key = ((long) c.j() << 32) ^ (c.row() & 0xffffffffL);
            if (!visited.add(key)) return;
            int zWall = c.row() % 2 == 0 ? Math.floorDiv(c.row(), 2) * ROW + 2 : Math.floorDiv(c.row(), 2) * ROW + 13;
            int dz = c.row() % 2 == 0 ? 1 : -1;
            BlockPos corner = new BlockPos(c.j() * UNIT + 5, FLOOR + 1, zWall + 2 * dz);
            Mob m = (Mob) ModEntities.lazy("skin_stealer").get().create(level);
            if (m == null) return;
            m.moveTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5, p.getYRot() + 180, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(corner), MobSpawnType.EVENT, null);
            m.setTarget(p);
            level.addFreshEntity(m);
            ClassicsKit.sound(level, corner, "event.level_13_home", 1.2f, 0.8f);
            if (!p.isCreative()) Sanity.add(p, -8f);
        }
    }
}
