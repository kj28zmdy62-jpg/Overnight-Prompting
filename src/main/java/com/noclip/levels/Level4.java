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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Level 4, "Abandoned Office". Cubicles to the horizon, every light working,
 * nobody at any desk. The water coolers hold Almond Water. Most computers are
 * dead; the few still glowing point toward the stairwell, if one is near.
 * At closing time the lights go out for a while.
 */
public final class Level4 {
    static final int FLOOR = 6, CEIL = 10;

    private Level4() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_4")
                .number("4").title("Abandoned Office").order(40)
                .tagline("Everyone has gone home. The computers that still work want you to leave too.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new ClosingTime())
                .visuals(new Visuals().tint(0.98f, 1.0f, 1.04f).saturation(0.8f).contrast(1.02f).brightness(1.12f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.42f).fog(6f, 54f).fogColor(0xA8ACB0).bloom(0.4f))
                .sanityDrain(0.012f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 1, 0.12f).dist(22, 40).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_5", 3).exit(ExitType.NOCLIP, "level_2", 1).exit(ExitType.NOCLIP, "level_9", 1)
                .exit(ExitType.DESCEND, "level_5", 4).exit(ExitType.DESCEND, "level_3", 1)
                .exit(ExitType.SECRET, "level_11", 1)
                .build();
    }

    enum Kind { CUBICLES, STAIRS, BREAK, MEETING, OFFICES, COPY, WINDOWS, EMPTY }

    static Kind kind(CellGrid g, int i, int k) {
        float r = g.unit(i, k, 1);
        if (r < 0.035f) return Kind.STAIRS;
        if (r < 0.12f) return Kind.BREAK;
        if (r < 0.20f) return Kind.MEETING;
        if (r < 0.28f) return Kind.OFFICES;
        if (r < 0.33f) return Kind.COPY;
        if (r < 0.38f) return Kind.WINDOWS;
        if (r < 0.40f) return Kind.EMPTY;
        return Kind.CUBICLES;
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("office_carpet");
        final BlockState carpetStain = ModBlocks.state("office_carpet_stained");
        final BlockState ceiling = ModBlocks.state("office_ceiling_tile");
        final BlockState ceilingStain = ModBlocks.state("office_ceiling_tile_stained");
        final BlockState troffer = ModBlocks.state("office_troffer");
        final BlockState trofferFlicker = ModBlocks.state("office_troffer_flicker");
        final BlockState drywall = ModBlocks.state("office_drywall");
        final BlockState drywallBase = ModBlocks.state("office_drywall_base");
        final BlockState clock = ModBlocks.state("office_drywall_clock");
        final BlockState pillar = ModBlocks.state("office_pillar");
        final BlockState window = ModBlocks.state("office_window");
        final BlockState windowCity = ModBlocks.state("office_window_city");
        final BlockState partition = ModBlocks.state("cubicle_partition");
        final BlockState glass = ModBlocks.state("meeting_glass");
        final BlockState door = ModBlocks.state("office_door");
        final BlockState desk = ModBlocks.state("office_desk");
        final BlockState computer = ModBlocks.state("office_computer");
        final String[] messages = {"dos", "nosignal", "newmsg", "login", "stay", "time", "hello"};
        final BlockState cooler = ModBlocks.state("water_cooler");
        final BlockState vend = ModBlocks.state("vending_machine");
        final BlockState vendTop = ModBlocks.state("vending_machine_top");
        final BlockState filing = ModBlocks.state("filing_cabinet");
        final BlockState copier = ModBlocks.state("office_copier");
        final BlockState[] boards = {ModBlocks.state("office_whiteboard_a"), ModBlocks.state("office_whiteboard_b"),
                ModBlocks.state("office_whiteboard_c")};
        final BlockState confTable = ModBlocks.state("conference_table");
        final BlockState breakTable = ModBlocks.state("break_table");
        final BlockState counter = ModBlocks.state("break_counter");
        final BlockState coffee = ModBlocks.state("coffee_maker");
        final BlockState plant = ModBlocks.state("office_plant");
        final BlockState stairDoor = ModBlocks.state("stairwell_door");
        final BlockState stairDoorTop = ModBlocks.state("stairwell_door_top");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 16, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean stain = Hash.fbm(s ^ 0x4C4L, x / 12.0, z / 12.0, 3) > 0.7f;
                    w.set(x, FLOOR, z, stain ? carpetStain : carpet);
                    BlockState c = ceiling;
                    if (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 1) {
                        c = Hash.chance(s, x, z, 3, 0.06f) ? trofferFlicker : troffer;
                    } else if (stain && Hash.chance(s, x, z, 4, 0.3f)) {
                        c = ceilingStain;
                    }
                    w.set(x, CEIL, z, c);
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, g, s, i, k, x0, z0));
        }

        void cell(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0) {
            long h = g.hash(i, k, 9);
            w.set(x0, FLOOR + 1, z0, drywallBase);
            for (int y = FLOOR + 2; y < CEIL; y++) w.set(x0, y, z0, pillar);
            switch (kind(g, i, k)) {
                case CUBICLES -> cubicles(w, g, s, i, k, x0, z0, h);
                case STAIRS -> stairs(w, x0, z0);
                case BREAK -> breakRoom(w, x0, z0, h);
                case MEETING -> meeting(w, x0, z0, h);
                case OFFICES -> offices(w, g, s, i, k, x0, z0, h);
                case COPY -> copyRoom(w, x0, z0, h);
                case WINDOWS -> windows(w, x0, z0, h);
                case EMPTY -> w.set(x0 + 8, FLOOR + 1, z0 + 8, ClassicsKit.facing(chair, Direction.from2DDataValue(Hash.range(h, 4))));
            }
        }

        // -------------------------------------------------------- cubicles

        static boolean partitionAt(int u, int v) {
            if ((v == 2 || v == 11) && u >= 2 && u <= 14) return true;
            return (u == 2 || u == 6 || u == 10 || u == 14) && ((v >= 2 && v <= 5) || (v >= 8 && v <= 11));
        }

        BlockState pane(BlockState base, boolean n, boolean e, boolean so, boolean we) {
            return base.setValue(CrossCollisionBlock.NORTH, n).setValue(CrossCollisionBlock.EAST, e)
                    .setValue(CrossCollisionBlock.SOUTH, so).setValue(CrossCollisionBlock.WEST, we);
        }

        void cubicles(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, long h) {
            for (int u = 2; u <= 14; u++)
                for (int v = 2; v <= 11; v++) {
                    if (!partitionAt(u, v)) continue;
                    w.set(x0 + u, FLOOR + 1, z0 + v, pane(partition, partitionAt(u, v - 1), partitionAt(u + 1, v),
                            partitionAt(u, v + 1), partitionAt(u - 1, v)));
                }
            for (int c = 0; c < 3; c++) {
                int a = 3 + c * 4;
                cubicle(w, g, s, i, k, x0 + a, z0, 3, Direction.NORTH, Hash.hash(h, c, 1));
                cubicle(w, g, s, i, k, x0 + a, z0, 10, Direction.SOUTH, Hash.hash(h, c, 2));
            }
            // the wide aisle: a plant, a cooler, a copier
            float r = Hash.unit(h >>> 3);
            if (r < 0.18f) w.set(x0 + 12, FLOOR + 1, z0 + 13, ClassicsKit.facing(cooler, Direction.SOUTH));
            else if (r < 0.3f) w.set(x0 + 5, FLOOR + 1, z0 + 13, ClassicsKit.facing(copier, Direction.SOUTH));
            if (Hash.unit(h >>> 9) < 0.35f) w.set(x0 + 8, FLOOR + 1, z0 + 13, plant);
        }

        /** One cubicle. back = the row's back partition side; the desk sits against it. */
        void cubicle(ChunkWriter w, CellGrid g, long s, int i, int k, int ax, int z0, int deskV, Direction back, long h) {
            int dz = back == Direction.NORTH ? 1 : -1;
            int deskZ = z0 + deskV;
            float r = Hash.unit(h);
            for (int u = 0; u < 3; u++) w.set(ax + u, FLOOR + 1, deskZ, ClassicsKit.facing(desk, back));
            Direction side = back == Direction.NORTH ? Direction.WEST : Direction.EAST;
            int sideX = back == Direction.NORTH ? ax : ax + 2;
            w.set(sideX, FLOOR + 1, deskZ + dz, ClassicsKit.facing(desk, side));
            if (r < 0.12f) {
                w.set(ax + 1, FLOOR + 1, deskZ + 2 * dz, ClassicsKit.facing(boxes, back));
                return;
            }
            w.set(ax + 1, FLOOR + 2, deskZ, ClassicsKit.facing(screen(g, s, i, k, ax + 1, deskZ, back, h), back));
            if (r < 0.88f) {
                Direction cf = r < 0.8f ? back : Direction.from2DDataValue(Hash.range(h >>> 20, 4));
                w.set(ax + 1, FLOOR + 1, deskZ + 2 * dz, ClassicsKit.facing(chair, cf));
            }
            float q = Hash.unit(h >>> 30);
            if (q < 0.06f) w.set(sideX, FLOOR + 2, deskZ + dz, bottle);
            else if (q < 0.12f) w.set(sideX, FLOOR + 2, deskZ + dz, plant);
        }

        /**
         * Most screens are dead. A few glow: if a stairwell is within three blocks of
         * offices they point to it, as seen by someone sitting at the desk.
         */
        BlockState screen(CellGrid g, long s, int i, int k, int x, int z, Direction facing, long h) {
            if (Hash.unit(h >>> 40) > 0.16f) return computer;
            int best = Integer.MAX_VALUE, tx = 0, tz = 0;
            for (int di = -3; di <= 3; di++)
                for (int dk = -3; dk <= 3; dk++) {
                    if (kind(g, i + di, k + dk) != Kind.STAIRS) continue;
                    int d = Math.abs(di) + Math.abs(dk);
                    if (d < best) {
                        best = d;
                        tx = g.x0(i + di) + 7;
                        tz = g.z0(k + dk) + 10;
                    }
                }
            if (best == Integer.MAX_VALUE) return ModBlocks.state("office_computer_" + messages[Hash.range(h >>> 44, messages.length)]);
            Direction d = ClassicsKit.dir(tx - x, tz - z);
            String arrow;
            if (d == facing) arrow = "exit_up";
            else if (d == facing.getOpposite()) arrow = "exit_back";
            else if (d == facing.getClockWise()) arrow = "exit_right";
            else arrow = "exit_left";
            return ModBlocks.state("office_computer_" + arrow);
        }

        // -------------------------------------------------------- rooms

        void wallBox(ChunkWriter w, int x1, int z1, int x2, int z2) {
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    if (x != x1 && x != x2 && z != z1 && z != z2) continue;
                    w.set(x, FLOOR + 1, z, drywallBase);
                    for (int y = FLOOR + 2; y < CEIL; y++) w.set(x, y, z, drywall);
                }
        }

        void opening(ChunkWriter w, int x, int z, int wide, boolean alongX) {
            for (int d = 0; d < wide; d++)
                for (int y = FLOOR + 1; y < CEIL - 1; y++) w.set(alongX ? x + d : x, y, alongX ? z : z + d, ModBlocks.AIR);
        }

        void stairs(ChunkWriter w, int x0, int z0) {
            wallBox(w, x0 + 5, z0 + 5, x0 + 9, z0 + 9);
            w.set(x0 + 7, FLOOR + 1, z0 + 9, stairDoor);
            w.set(x0 + 7, FLOOR + 2, z0 + 9, stairDoorTop);
            w.set(x0 + 7, FLOOR + 3, z0 + 10, ClassicsKit.facing(exitSign, Direction.NORTH));
            for (int x = x0 + 6; x <= x0 + 8; x++)
                for (int z = z0 + 6; z <= z0 + 8; z++)
                    for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, drywall);
        }

        void breakRoom(ChunkWriter w, int x0, int z0, long h) {
            wallBox(w, x0 + 2, z0 + 2, x0 + 13, z0 + 13);
            opening(w, x0 + 7, z0 + 13, 2, true);
            if (Hash.unit(h >>> 5) < 0.5f) opening(w, x0 + 2, z0 + 6, 2, false);
            for (int x = x0 + 3; x <= x0 + 7; x++) w.set(x, FLOOR + 1, z0 + 3, ClassicsKit.facing(counter, Direction.NORTH));
            w.set(x0 + 4, FLOOR + 2, z0 + 3, ClassicsKit.facing(coffee, Direction.NORTH));
            for (int x = x0 + 10; x <= x0 + 11; x++) {
                w.set(x, FLOOR + 1, z0 + 3, ClassicsKit.facing(vend, Direction.NORTH));
                w.set(x, FLOOR + 2, z0 + 3, ClassicsKit.facing(vendTop, Direction.NORTH));
            }
            w.set(x0 + 12, FLOOR + 1, z0 + 6, ClassicsKit.facing(cooler, Direction.EAST));
            w.set(x0 + 12, FLOOR + 1, z0 + 8, ClassicsKit.facing(cooler, Direction.EAST));
            if (Hash.unit(h >>> 7) < 0.5f) w.set(x0 + 12, FLOOR + 1, z0 + 10, ClassicsKit.facing(cooler, Direction.EAST));
            for (int[] t : new int[][]{{6, 7}, {9, 9}}) {
                int tx = x0 + t[0], tz = z0 + t[1];
                w.set(tx, FLOOR + 1, tz, breakTable);
                w.set(tx - 1, FLOOR + 1, tz, ClassicsKit.facing(chair, Direction.EAST));
                w.set(tx + 1, FLOOR + 1, tz, ClassicsKit.facing(chair, Direction.WEST));
            }
            w.set(x0 + 2, FLOOR + 2, z0 + 10, clock);
        }

        void meeting(ChunkWriter w, int x0, int z0, long h) {
            int x1 = x0 + 3, z1 = z0 + 3, x2 = x0 + 12, z2 = z0 + 12;
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                    if (!edge) continue;
                    if (z == z2 && (x == x0 + 7 || x == x0 + 8)) continue;
                    boolean n = z > z1 && (x == x1 || x == x2), so = z < z2 && (x == x1 || x == x2);
                    boolean we = x > x1 && (z == z1 || z == z2) && !(z == z2 && x == x0 + 9);
                    boolean e = x < x2 && (z == z1 || z == z2) && !(z == z2 && x == x0 + 6);
                    BlockState p = pane(glass, n, e, so, we);
                    for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, p);
                }
            for (int x = x0 + 7; x <= x0 + 8; x++)
                for (int z = z0 + 5; z <= z0 + 9; z++) w.set(x, FLOOR + 1, z, confTable);
            for (int z = z0 + 5; z <= z0 + 9; z++) {
                if (Hash.unit(Hash.hash(h, z, 1)) < 0.85f) w.set(x0 + 6, FLOOR + 1, z, ClassicsKit.facing(chair, Direction.EAST));
                if (Hash.unit(Hash.hash(h, z, 2)) < 0.85f) w.set(x0 + 9, FLOOR + 1, z, ClassicsKit.facing(chair, Direction.WEST));
            }
            // one chair at the head of the table, turned to face the door
            w.set(x0 + 7, FLOOR + 1, z0 + 10, ClassicsKit.facing(chair, Direction.NORTH));
            w.set(x0 + 7, FLOOR + 1, z0 + 4, ClassicsKit.facing(boards[Hash.range(h >>> 11, 3)], Direction.NORTH));
            w.set(x0 + 8, FLOOR + 1, z0 + 4, ClassicsKit.facing(boards[Hash.range(h >>> 13, 3)], Direction.NORTH));
        }

        void offices(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, long h) {
            for (int u = 2; u <= 14; u++) {
                for (int v : new int[]{2, 7}) {
                    w.set(x0 + u, FLOOR + 1, z0 + v, drywallBase);
                    for (int y = FLOOR + 2; y < CEIL; y++) w.set(x0 + u, y, z0 + v, drywall);
                }
            }
            for (int u : new int[]{2, 6, 10, 14})
                for (int v = 3; v <= 6; v++) {
                    w.set(x0 + u, FLOOR + 1, z0 + v, drywallBase);
                    for (int y = FLOOR + 2; y < CEIL; y++) w.set(x0 + u, y, z0 + v, drywall);
                }
            for (int c = 0; c < 3; c++) {
                int a = x0 + 3 + c * 4;
                long hc = Hash.hash(h, c, 3);
                BlockState lower = door.setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                        .setValue(DoorBlock.OPEN, Hash.unit(hc) < 0.3f);
                w.set(a + 1, FLOOR + 1, z0 + 7, lower);
                w.set(a + 1, FLOOR + 2, z0 + 7, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                w.set(a + 1, FLOOR + 2, z0 + 2, Hash.unit(hc >>> 8) < 0.07f ? windowCity : window);
                w.set(a + 1, FLOOR + 1, z0 + 4, ClassicsKit.facing(desk, Direction.SOUTH));
                w.set(a + 1, FLOOR + 2, z0 + 4, ClassicsKit.facing(screen(g, s, i, k, a + 1, z0 + 4, Direction.SOUTH, hc), Direction.SOUTH));
                w.set(a + 1, FLOOR + 1, z0 + 3, ClassicsKit.facing(chair, Direction.SOUTH));
                w.set(a, FLOOR + 1, z0 + 6, ClassicsKit.facing(filing, Direction.WEST));
                if (Hash.unit(hc >>> 16) < 0.5f) w.set(a + 2, FLOOR + 1, z0 + 3, plant);
                else if (Hash.unit(hc >>> 16) < 0.6f) w.set(a + 2, FLOOR + 1, z0 + 3, bottle);
            }
        }

        void copyRoom(ChunkWriter w, int x0, int z0, long h) {
            for (int x = x0 + 3; x <= x0 + 12; x++) w.set(x, FLOOR + 1, z0 + 3, ClassicsKit.facing(filing, Direction.NORTH));
            for (int x = x0 + 3; x <= x0 + 12; x++)
                if (Hash.unit(Hash.hash(h, x, 4)) < 0.7f) w.set(x, FLOOR + 1, z0 + 9, ClassicsKit.facing(filing, Direction.SOUTH));
            w.set(x0 + 5, FLOOR + 1, z0 + 6, ClassicsKit.facing(copier, Direction.NORTH));
            w.set(x0 + 9, FLOOR + 1, z0 + 6, ClassicsKit.facing(copier, Direction.NORTH));
            for (int q = 0; q < 4; q++) {
                long hq = Hash.hash(h, q, 5);
                w.set(x0 + 3 + Hash.range(hq, 10), FLOOR + 1, z0 + 12 + Hash.range(hq >>> 8, 2),
                        ClassicsKit.facing(boxes, Direction.from2DDataValue(Hash.range(hq >>> 16, 4))));
            }
            w.set(x0 + 12, FLOOR + 1, z0 + 6, ClassicsKit.facing(cooler, Direction.EAST));
        }

        void windows(ChunkWriter w, int x0, int z0, long h) {
            int city = Hash.unit(h >>> 3) < 0.25f ? 3 + Hash.range(h >>> 7, 11) : -1;
            for (int u = 2; u <= 15; u++) {
                int x = x0 + u, z = z0 + 14;
                w.set(x, FLOOR + 1, z, drywallBase);
                w.set(x, FLOOR + 2, z, u == city ? windowCity : window);
                for (int y = FLOOR + 3; y < CEIL; y++) w.set(x, y, z, drywall);
            }
            int cx = x0 + 4 + Hash.range(h >>> 11, 8);
            w.set(cx, FLOOR + 1, z0 + 12, ClassicsKit.facing(chair, Direction.SOUTH));
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Closing time. Every few minutes the PA chimes, a tired voice says something
     * nobody can make out, and the lights go off floor-wide for half a minute.
     * The screens stay on.
     */
    static final class ClosingTime implements LevelBehavior {
        private long nextClose = -1;
        private int stage;

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (nextClose < 0) nextClose = now + 20 * (200 + level.random.nextInt(180));
            if (stage == 0 && now >= nextClose - 100) {
                stage = 1;
                for (ServerPlayer p : level.players()) ClassicsKit.soundTo(p, "event.level_4_chime", 1.0f, 1f);
            } else if (stage == 1 && now >= nextClose - 50) {
                stage = 2;
                for (ServerPlayer p : level.players()) {
                    ClassicsKit.soundTo(p, "event.level_4_closing", 1.0f, 0.9f);
                    p.displayClientMessage(Component.literal("\"...the office is now closed. Please make your way to the...\"")
                            .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
                }
            } else if (stage == 2 && now >= nextClose) {
                stage = 0;
                Blackouts.start(level, 20 * (25 + level.random.nextInt(15)));
                nextClose = now + 20 * (260 + level.random.nextInt(200));
            }
        }
    }
}
