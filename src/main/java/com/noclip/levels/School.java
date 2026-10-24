package com.noclip.levels;

import com.noclip.entity.HallMonitor;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * School After Hours. Double-loaded hallways of lockers, classrooms with
 * chalkboards, cross corridors every sixty blocks. A bell rings whenever it
 * wants: then class is in session and the Hall Monitor walks the halls looking
 * for anyone out of class. Get into a classroom, shut the door, wait for the
 * second bell. The chalkboards know the way out: follow the green arrows to the
 * stairs.
 */
public final class School {
    static final int FLOOR = 10, CEIL = FLOOR + 5, PZ = 26, PX = 10, CROSS = 60;

    private School() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("school")
                .number("").title("School After Hours").order(1020)
                .tagline("When the bell rings, get in a room. Wait for the second bell.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Bells())
                .visuals(new Visuals().tint(1.04f, 1.0f, 0.92f).saturation(0.82f).contrast(1.08f).brightness(1.18f)
                        .grain(0.07f).aberration(0.0015f).vignette(0.5f).fog(5f, 50f).fogColor(0x4A4438).bloom(0.45f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(28, 44).weight(2))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "hospital", 1).exit(ExitType.NOCLIP, "playground", 1)
                .exit(ExitType.NOCLIP, "dead_mall", 1)
                .exit(ExitType.DESCEND, "boiler_room", 2).exit(ExitType.DESCEND, "locker_rooms", 2).exit(ExitType.DESCEND, "level_2", 1)
                .exit(ExitType.DOOR, "locker_rooms", 2).exit(ExitType.DOOR, "playground", 1).exit(ExitType.DOOR, "daycare", 1)
                .build();
    }

    // ---------------------------------------------------------------- geometry shared with the Hall Monitor

    static boolean crossCorridor(int x) {
        int c = Math.floorMod(x, CROSS);
        return c >= 1 && c <= 5;
    }

    public static boolean inClassroom(int x, int z) {
        int v = Math.floorMod(z, PZ);
        if (crossCorridor(x)) return false;
        return (v >= 1 && v <= 9) || (v >= 17 && v <= 24);
    }

    public static boolean inHallway(int x, int z) {
        return !inClassroom(x, z);
    }

    static boolean stairwell(long s, int ci, int r) {
        return Hash.chance(s, ci, r, 0x57A1, 0.2f);
    }

    private static boolean session;

    public static boolean inSession() {
        return session;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("sch_floor");
        final BlockState floorM = ModBlocks.state("sch_floor_maroon");
        final BlockState floorB = ModBlocks.state("sch_floor_blue");
        final BlockState wall = ModBlocks.state("sch_wall");
        final BlockState wallLow = ModBlocks.state("sch_wall_lower");
        final BlockState ceil = ModBlocks.state("sch_ceiling");
        final BlockState light = ModBlocks.state("sch_troffer");
        final BlockState flicker = ModBlocks.state("sch_troffer_flicker");
        final BlockState door = ModBlocks.state("sch_door");
        final BlockState lockerOpen = ModBlocks.state("sch_locker_open");
        final BlockState lockerOpenTop = ModBlocks.state("sch_locker_open_top");
        final BlockState bell = ModBlocks.state("sch_bell");
        final BlockState desk = ModBlocks.state("sch_desk");
        final BlockState teacher = ModBlocks.state("sch_teacher_desk");
        final BlockState arrow = ModBlocks.state("sch_floor_arrow");
        final BlockState fountain = ModBlocks.state("sch_drinking_fountain");
        final BlockState steps = ModBlocks.state("sch_stairs");
        final BlockState dark = ModBlocks.state("sch_stairwell_dark");
        final BlockState stairSign = ModBlocks.state("sch_stair_sign");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] lockers = new BlockState[3], lockerTops = new BlockState[3], drawings = new BlockState[3];
        final BlockState[][] boards = new BlockState[8][8];

        Gen() {
            for (int v = 0; v < 3; v++) {
                lockers[v] = ModBlocks.state("sch_locker_" + v);
                lockerTops[v] = ModBlocks.state("sch_locker_top_" + v);
                drawings[v] = ModBlocks.state("sch_drawing_" + v);
            }
            for (int n = 0; n < 8; n++)
                for (int p = 0; p < 8; p++) boards[n][p] = ModBlocks.state("sch_board_" + n + "_" + p);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            int s0 = Math.floorDiv(w.minX, PX) - 1, s1 = Math.floorDiv(w.maxX, PX) + 1;
            int r0 = Math.floorDiv(w.minZ, PZ) - 1, r1 = Math.floorDiv(w.maxZ, PZ) + 1;
            for (int sx = s0; sx <= s1; sx++)
                for (int r = r0; r <= r1; r++) {
                    classroom(w, s, sx, r, true);
                    classroom(w, s, sx, r, false);
                    hallFittings(w, s, sx, r);
                }
            for (int ci = Math.floorDiv(w.minX, CROSS) - 1; ci <= Math.floorDiv(w.maxX, CROSS) + 1; ci++)
                for (int r = r0; r <= r1; r++) closet(w, s, ci, r);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int v = Math.floorMod(z, PZ), u = Math.floorMod(x, PX), c = Math.floorMod(x, CROSS);
            float fr = Hash.unit(s, x, z, 0xF1);
            if (fr < 0.07f) w.set(x, FLOOR, z, fr < 0.045f ? floorM : floorB);
            boolean cross = c >= 1 && c <= 5;
            BlockState ceiling = ceil;
            if (cross) {
                if (c == 3 && Math.floorMod(z, 4) == 1) ceiling = lamp(s, x, z, 0f);
                w.set(x, CEIL, z, ceiling);
                return;
            }
            boolean hallWall = v == 10 || v == 16;
            if (v == 0 || v == 25 || (u == 0 && !(v >= 11 && v <= 15)) || ((c >= 6 && c <= 9) && !(v >= 10 && v <= 16))) {
                solid(w, x, z);
                return;
            }
            if (hallWall) {
                long h = Hash.hash(s, x, z, 0x10C);
                boolean doorway = u == 5 && !(c >= 0 && c <= 9);
                if (doorway) {
                    // the classroom door, sometimes left open
                    boolean open = Hash.unit(h) < 0.55f;
                    Direction f = v == 10 ? Direction.SOUTH : Direction.NORTH;
                    BlockState d = door.setValue(DoorBlock.FACING, f).setValue(DoorBlock.OPEN, open);
                    w.set(x, FLOOR + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                    w.set(x, FLOOR + 2, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                    w.fill(x, FLOOR + 3, z, x, CEIL, z, wall);
                    return;
                }
                if (u == 0) {
                    solid(w, x, z);
                    return;
                }
                if (Hash.unit(h >>> 20) < 0.006f) {
                    w.set(x, FLOOR + 1, z, lockerOpen);
                    w.set(x, FLOOR + 2, z, lockerOpenTop);
                } else {
                    int var = Hash.range(h >>> 8, 3);
                    Direction face = v == 10 ? Direction.SOUTH : Direction.NORTH;  // toward the hallway
                    w.set(x, FLOOR + 1, z, lockers[var].setValue(HorizontalDirectionalBlock.FACING, face));
                    w.set(x, FLOOR + 2, z, lockerTops[var].setValue(HorizontalDirectionalBlock.FACING, face));
                }
                w.set(x, FLOOR + 3, z, Hash.unit(h >>> 30) < 0.04f ? drawings[Hash.range(h >>> 33, 3)] : wall);
                w.fill(x, FLOOR + 4, z, x, CEIL, z, wall);
                return;
            }
            if (v >= 11 && v <= 15) {
                if (v == 13 && Math.floorMod(x, 4) == 1) ceiling = lamp(s, x, z, 0.1f);
            } else {
                // classroom: lights on a 4-grid, all off in some rooms
                int sx = Math.floorDiv(x, PX), r = Math.floorDiv(z, PZ);
                boolean north = v <= 9;
                boolean darkRoom = Hash.chance(s, sx, r * 2 + (north ? 0 : 1), 0xDA, 0.28f);
                int lv = north ? v : v - 16;
                if ((u == 3 || u == 7) && (lv == 3 || lv == 7)) {
                    ceiling = darkRoom ? light.setValue(BlockStateProperties.LIT, false) : lamp(s, x, z, 0f);
                }
            }
            w.set(x, CEIL, z, ceiling);
        }

        BlockState lamp(long s, int x, int z, float dead) {
            float r = Hash.unit(s, x, z, 0x1A);
            if (r < dead) return light.setValue(BlockStateProperties.LIT, false);
            return r < dead + 0.15f ? flicker : light;
        }

        void solid(ChunkWriter w, int x, int z) {
            w.set(x, FLOOR + 1, z, wallLow);
            w.fill(x, FLOOR + 2, z, x, CEIL, z, wall);
        }

        void classroom(ChunkWriter w, long s, int sx, int r, boolean north) {
            int x0 = sx * PX;
            int c0 = Math.floorMod(x0, CROSS);
            if (c0 == 0) return;  // this slot is the cross corridor and the closet
            int z0 = r * PZ;
            long h = Hash.hash(s, sx, r * 2 + (north ? 0 : 1), 0xC1A55);
            int y = FLOOR + 1;
            Direction faces = north ? Direction.NORTH : Direction.SOUTH;
            // chalkboard, 4x2, in the front wall
            int board = Hash.unit(h) < 0.4f ? 7 : Hash.range(h >>> 4, 7);
            int bz = north ? z0 : z0 + 25;
            for (int j = 0; j < 4; j++) {
                int bx = north ? x0 + 3 + j : x0 + 6 - j;
                w.set(bx, y + 2, bz, boards[board][j]);
                w.set(bx, y + 1, bz, boards[board][4 + j]);
            }
            int tz = north ? z0 + 2 : z0 + 23;
            w.set(x0 + 7, y, tz, teacher.setValue(HorizontalDirectionalBlock.FACING, faces));
            float kind = Hash.unit(h >>> 12);
            int[] rows = north ? new int[]{4, 6, 8} : new int[]{21, 19, 17};
            if (kind < 0.02f) {
                // one desk in the middle of an empty room, and something left on it
                w.set(x0 + 5, y, z0 + (north ? 5 : 20), desk.setValue(HorizontalDirectionalBlock.FACING, faces));
                w.set(x0 + 5, y, z0 + (north ? 4 : 21), bottle.setValue(HorizontalDirectionalBlock.FACING, faces));
                return;
            }
            Direction deskFace = kind < 0.04f ? faces.getOpposite() : faces;  // everyone facing the back wall
            for (int row : rows)
                for (int dx = 2; dx <= 8; dx += 2) {
                    if (Hash.unit(h >>> (row + dx)) < 0.08f) continue;
                    w.set(x0 + dx, y, z0 + row, desk.setValue(HorizontalDirectionalBlock.FACING, deskFace));
                }
            if (kind > 0.96f) {
                // desks piled against the door from the inside
                int dz = north ? z0 + 9 : z0 + 17;
                for (int dx = 4; dx <= 6; dx++)
                    w.set(x0 + dx, y, dz, desk.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> (40 + dx), 4))));
            }
        }

        void hallFittings(ChunkWriter w, long s, int sx, int r) {
            int x0 = sx * PX, z0 = r * PZ;
            long h = Hash.hash(s, sx, r, 0xFA11);
            int y = FLOOR + 1;
            if (Math.floorMod(sx, 4) == 1) w.set(x0 + 2, y + 3, z0 + 11, bell.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            if (Hash.unit(h) < 0.05f && Math.floorMod(x0, CROSS) != 0)
                w.set(x0 + 8, y, z0 + 15, fountain.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            // floor arrows towards the nearest stairwell in this hallway
            for (int x = x0; x < x0 + PX; x++) {
                if (Math.floorMod(x, 20) != 10 || crossCorridor(x)) continue;
                int ci = Math.floorDiv(x, CROSS);
                int best = Integer.MAX_VALUE;
                for (int cc = ci - 6; cc <= ci + 6; cc++) {
                    if (!stairwell(s, cc, r)) continue;
                    int sxw = cc * CROSS + 8;
                    if (Math.abs(sxw - x) < Math.abs(best)) best = sxw - x;
                }
                if (best == Integer.MAX_VALUE || Math.abs(best) < 3) continue;
                w.set(x, y, z0 + 13, arrow.setValue(HorizontalDirectionalBlock.FACING, best > 0 ? Direction.EAST : Direction.WEST));
            }
        }

        /** The 3-wide slot beside each cross corridor: solid, or a stairwell going down. */
        void closet(ChunkWriter w, long s, int ci, int r) {
            if (!stairwell(s, ci, r)) return;
            int x0 = ci * CROSS, z0 = r * PZ;
            int y = FLOOR + 1;
            // doorway from the hallway, with a sign
            w.fill(x0 + 7, y, z0 + 10, x0 + 9, y + 2, z0 + 10, ModBlocks.AIR);
            w.set(x0 + 8, y + 3, z0 + 10, stairSign);
            w.fill(x0 + 7, y, z0 + 8, x0 + 9, CEIL - 1, z0 + 9, ModBlocks.AIR);
            for (int st = 0; st < 6; st++) {
                int z = z0 + 7 - st, yy = FLOOR - st;
                for (int x = x0 + 7; x <= x0 + 9; x++) {
                    w.fill(x, yy + 1, z, x, CEIL - 1, z, ModBlocks.AIR);
                    w.set(x, yy, z, steps.setValue(StairBlock.FACING, Direction.SOUTH));
                }
            }
            for (int x = x0 + 7; x <= x0 + 9; x++) {
                w.fill(x, FLOOR - 7, z0 + 1, x, CEIL - 1, z0 + 1, ModBlocks.AIR);
                w.fill(x, FLOOR - 7, z0 + 1, x, FLOOR - 3, z0 + 1, dark);
            }
        }
    }

    // ---------------------------------------------------------------- the bell

    /**
     * The bell rings whenever it wants. First bell: class is in session and the
     * Hall Monitor comes for anyone in the halls. Second bell: passing period.
     */
    static final class Bells implements LevelBehavior {
        private long nextBell = -1;

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (nextBell < 0) {
                session = false;
                nextBell = now + 20 * (45 + level.random.nextInt(75));
            }
            if (now < nextBell) return;
            session = !session;
            nextBell = now + (session ? 20 * (35 + level.random.nextInt(30)) : 20 * (50 + level.random.nextInt(130)));
            for (ServerPlayer p : level.players()) {
                level.playSound(null, p.blockPosition().above(3), ModSounds.get("event.school_bell"), SoundSource.AMBIENT, 2.0f, 1f);
                p.displayClientMessage(Component.literal(session ? "§c§oClass is in session." : "§7§oPassing period."), true);
                if (session) summon(p, level);
            }
        }

        void summon(ServerPlayer p, ServerLevel level) {
            if (p.isCreative() || p.isSpectator()) return;
            int r = Math.floorDiv(p.getBlockZ(), PZ);
            int hz = r * PZ + 13;
            for (int attempt = 0; attempt < 10; attempt++) {
                int side = p.getRandom().nextBoolean() ? 1 : -1;
                int x = p.getBlockX() + side * (22 + p.getRandom().nextInt(14));
                BlockPos pos = new BlockPos(x, FLOOR + 1, hz);
                if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                HallMonitor m = HallMonitor.TYPE.create(level);
                if (m == null) return;
                m.moveTo(x + 0.5, FLOOR + 1, hz + 0.5, side > 0 ? 90f : -90f, 0f);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                level.addFreshEntity(m);
                return;
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (!session || p.tickCount % 20 != 0 || p.isCreative() || p.isSpectator()) return;
            if (inHallway(p.getBlockX(), p.getBlockZ())) Sanity.add(p, -0.15f);
        }
    }
}
