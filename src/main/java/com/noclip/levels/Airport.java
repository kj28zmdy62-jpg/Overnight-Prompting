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
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Airport. A terminal that goes on forever at night: terrazzo concourse, moving
 * walkways humming along with nobody on them, gate after gate of empty seats,
 * departure boards where every flight home is cancelled. Planes wait dark on
 * the apron. Every so often the PA calls final boarding for a gate nearby, and
 * for a short while that gate's jetway door is open.
 */
public final class Airport {
    static final int FLOOR = 10, PERIOD = 48, GATE = 24;

    private Airport() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("airport")
                .number("").title("Airport").order(960)
                .tagline("All flights delayed. Listen for your gate.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Boarding())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.06f).saturation(0.78f).contrast(1.05f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.45f).fog(8f, 80f).fogColor(0x0A0E16).bloom(0.55f)
                        .sky())
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 3, 0.25f).dist(20, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(30, 44).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.3f).dark().dist(14, 28).weight(1))
                .exit(ExitType.DOOR, "level_11", 2).exit(ExitType.DOOR, "level_0", 1).exit(ExitType.DOOR, "level_10", 1)
                .exit(ExitType.DOOR, "frontrooms", 1)
                .exit(ExitType.NOCLIP, "level_11", 2).exit(ExitType.NOCLIP, "level_13", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState terrazzo = ModBlocks.state("airport_terrazzo");
        final BlockState carpet = ModBlocks.state("airport_gate_carpet");
        final BlockState wall = ModBlocks.state("airport_wall");
        final BlockState ceiling = ModBlocks.state("airport_ceiling");
        final BlockState light = ModBlocks.state("airport_skylight");
        final BlockState lightBad = ModBlocks.state("airport_skylight_bad");
        final BlockState glass = ModBlocks.state("airport_glass");
        final BlockState walkway = ModBlocks.state("airport_walkway");
        final BlockState rail = ModBlocks.state("airport_walkway_rail");
        final BlockState seats = ModBlocks.state("airport_seats");
        final BlockState[] boards = {ModBlocks.state("airport_board_a"), ModBlocks.state("airport_board_b"),
                ModBlocks.state("airport_board_c")};
        final BlockState desk = ModBlocks.state("airport_gate_desk");
        final BlockState jetway = ModBlocks.state("airport_jetway_closed");
        final BlockState tarmac = ModBlocks.state("airport_tarmac");
        final BlockState taxi = ModBlocks.state("airport_taxi_line");
        final BlockState blue = ModBlocks.state("airport_light_blue");
        final BlockState beacon = ModBlocks.state("airport_light_red");
        final BlockState fuselage = ModBlocks.state("airport_fuselage");
        final BlockState fuselagePlain = ModBlocks.state("airport_fuselage_plain");
        final BlockState cockpit = ModBlocks.state("airport_cockpit");
        final BlockState wing = ModBlocks.state("airport_wing");
        final BlockState engine = ModBlocks.state("airport_engine");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            BlockState glassX = glass.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int v = Math.floorMod(z, PERIOD), u = Math.floorMod(x, GATE);
                    if (v >= 29) {
                        w.set(x, FLOOR, z, v == 38 ? taxi : tarmac);
                        if ((v == 29 || v == 47) && Math.floorMod(x, 4) == 0) w.set(x, FLOOR + 1, z, blue);
                        continue;
                    }
                    boolean concourse = v >= 8 && v <= 19;
                    w.set(x, FLOOR, z, concourse ? terrazzo : (v == 0 || v == 28 ? terrazzo : carpet));
                    int ceil = concourse ? FLOOR + 8 : FLOOR + 6;
                    for (int y = ceil + 1; y <= FLOOR + 9; y++) w.set(x, y, z, wall);
                    w.set(x, ceil, z, ceiling);
                    if (v == 0 || v == 28) {
                        for (int y = FLOOR + 1; y < ceil; y++) w.set(x, y, z, glassX);
                        continue;
                    }
                    if (concourse) {
                        if (Math.floorMod(x, 6) == 3 && (v == 10 || v == 14 || v == 18))
                            w.set(x, ceil, z, Hash.chance(s, x, z, 1, 0.1f) ? lightBad : light);
                        boolean gap = u >= 21;
                        if (!gap && (v == 12 || v == 13)) w.set(x, FLOOR, z, ClassicsKit.facing(walkway, Direction.EAST));
                        if (!gap && (v == 15 || v == 16)) w.set(x, FLOOR, z, ClassicsKit.facing(walkway, Direction.WEST));
                        if (!gap && (v == 11 || v == 14 || v == 17)) w.set(x, FLOOR + 1, z, ClassicsKit.facing(rail, Direction.EAST));
                        if ((v == 8 || v == 19) && Math.floorMod(x, 12) == 0)
                            for (int y = FLOOR + 1; y < ceil; y++) w.set(x, y, z, wall);
                    } else {
                        if (Math.floorMod(x, 8) == 4 && (v == 4 || v == 24)) w.set(x, ceil, z, light);
                    }
                }
            int g0 = Math.floorDiv(w.minX, GATE) - 1, g1 = Math.floorDiv(w.maxX, GATE) + 1;
            int r0 = Math.floorDiv(w.minZ, PERIOD) - 1, r1 = Math.floorDiv(w.maxZ, PERIOD) + 1;
            for (int g = g0; g <= g1; g++)
                for (int r = r0; r <= r1; r++) {
                    int gx = g * GATE, base = r * PERIOD;
                    gate(w, s, g, gx, base + 28, base + 27, -1, Direction.SOUTH);
                    gate(w, s, g, gx, base, base + 1, 1, Direction.NORTH);
                    if (Hash.chance(s, g, r, 1401, 0.45f)) plane(w, gx + 4, base + 30);
                    // departure boards on the concourse pillars
                    if (Math.floorMod(g, 2) == 0) {
                        long h = Hash.hash(s, g, r, 1402);
                        w.set(gx, FLOOR + 4, base + 9, ClassicsKit.facing(boards[Hash.range(h, 3)], Direction.NORTH));
                        w.set(gx, FLOOR + 5, base + 9, ClassicsKit.facing(boards[Hash.range(h >>> 8, 3)], Direction.NORTH));
                        w.set(gx + 12, FLOOR + 4, base + 18, ClassicsKit.facing(boards[Hash.range(h >>> 16, 3)], Direction.SOUTH));
                    }
                }
        }

        /** A gate on one side: jetway door in the glass, desk, sign, rows of seats facing the window. */
        void gate(ChunkWriter w, long s, int g, int gx, int glassZ, int firstZ, int dz, Direction window) {
            w.set(gx + 4, FLOOR + 1, glassZ, jetway);
            w.set(gx + 5, FLOOR + 1, glassZ, jetway);
            w.set(gx + 4, FLOOR + 2, glassZ, jetway);
            w.set(gx + 5, FLOOR + 2, glassZ, jetway);
            int deskZ = firstZ + dz;
            w.set(gx + 8, FLOOR + 1, deskZ, ClassicsKit.facing(desk, window));
            w.set(gx + 9, FLOOR + 1, deskZ, ClassicsKit.facing(desk, window));
            int n = Math.floorMod(g, 9) + 1;
            w.set(gx + 6, FLOOR + 5, firstZ + 3 * dz, ClassicsKit.facing(ModBlocks.state("airport_gate_sign_" + n), Direction.NORTH));
            for (int row = 0; row < 3; row++) {
                int z = firstZ + (2 + row * 2) * dz;
                for (int x = gx + 10; x <= gx + 20; x++) {
                    if (x == gx + 15) continue;
                    w.set(x, FLOOR + 1, z, ClassicsKit.facing(seats, window));
                }
            }
            long h = Hash.hash(s, g, glassZ, 1403);
            if (Hash.unit(h) < 0.25f) w.set(gx + 12 + Hash.range(h >>> 4, 6), FLOOR + 1, firstZ + dz, ClassicsKit.facing(boxes, window));
            if (Hash.unit(h >>> 10) < 0.2f) w.set(gx + 11, FLOOR + 2, firstZ + 2 * dz, bottle);
        }

        /** A dark airliner parked nose-in at a south gate. */
        void plane(ChunkWriter w, int cx, int nose) {
            for (int dz = 0; dz <= 16; dz++)
                for (int dx = -1; dx <= 1; dx++)
                    for (int y = FLOOR + 2; y <= FLOOR + 4; y++) {
                        boolean side = dx != 0 && y == FLOOR + 3;
                        BlockState b = dz == 0 ? (y == FLOOR + 3 ? cockpit : fuselagePlain) : side ? fuselage : fuselagePlain;
                        if (dz == 0 && dx != 0 && y == FLOOR + 4) continue;
                        if (dz == 16 && y == FLOOR + 2) continue;
                        w.set(cx + dx, y, nose + dz, b);
                    }
            for (int dx = -9; dx <= 9; dx++)
                for (int dz = 6; dz <= 8; dz++) {
                    if (Math.abs(dx) <= 1) continue;
                    if (dz == 8 && Math.abs(dx) > 6) continue;
                    if (dz == 6 && Math.abs(dx) > 8) continue;
                    w.set(cx + dx, FLOOR + 2, nose + dz, wing);
                }
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                w.set(cx + sgn * 4, FLOOR + 1, nose + 6, engine);
                w.set(cx + sgn * 4, FLOOR + 1, nose + 7, engine);
                w.set(cx + sgn * 9, FLOOR + 3, nose + 6, beacon);
            }
            w.set(cx, FLOOR + 1, nose + 2, wall);
            w.set(cx, FLOOR + 1, nose + 9, wall);
            for (int y = FLOOR + 5; y <= FLOOR + 8; y++)
                for (int dz = 13 + (y - FLOOR - 5); dz <= 16; dz++) w.set(cx, y, nose + dz, fuselagePlain);
            for (int dx = -3; dx <= 3; dx++) w.set(cx + dx, FLOOR + 4, nose + 16, wing);
            w.set(cx, FLOOR + 9, nose + 16, beacon);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Final boarding calls. A gate near you opens its jetway, briefly. */
    static final class Boarding implements LevelBehavior {
        private long next = -1, closeAt = -1;
        private final List<BlockPos> open = new ArrayList<>();

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (next < 0) next = now + 20 * (40 + level.random.nextInt(60));
            if (closeAt > 0 && now >= closeAt) {
                for (BlockPos p : open)
                    if (level.isLoaded(p) && level.getBlockState(p).is(ModBlocks.get("airport_jetway_open")))
                        level.setBlock(p, ModBlocks.state("airport_jetway_closed"), Block.UPDATE_CLIENTS);
                open.clear();
                closeAt = -1;
            }
            if (now < next || level.players().isEmpty()) return;
            next = now + 20 * (100 + level.random.nextInt(80));
            ServerPlayer p = level.players().get(level.random.nextInt(level.players().size()));
            int row = Math.floorDiv(p.getBlockZ(), PERIOD);
            int g = Math.floorDiv(p.getBlockX(), GATE) + (level.random.nextBoolean() ? 1 : -1) * (1 + level.random.nextInt(3));
            boolean south = level.random.nextBoolean();
            int z = row * PERIOD + (south ? 28 : 0);
            BlockState openB = ModBlocks.state("airport_jetway_open");
            for (int dx = 4; dx <= 5; dx++)
                for (int y = FLOOR + 1; y <= FLOOR + 2; y++) {
                    BlockPos q = new BlockPos(g * GATE + dx, y, z);
                    if (level.isLoaded(q) && level.getBlockState(q).is(ModBlocks.get("airport_jetway_closed"))) {
                        level.setBlock(q, openB, Block.UPDATE_CLIENTS);
                        open.add(q);
                    }
                }
            if (open.isEmpty()) return;
            closeAt = now + 20 * 45;
            int n = Math.floorMod(g, 9) + 1;
            for (ServerPlayer q : level.players()) {
                ClassicsKit.soundTo(q, "event.airport_boarding", 1.3f, 1f);
                q.displayClientMessage(Component.literal("\"Flight 13 is now boarding at gate " + n + ". This is the final call.\"")
                        .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.ITALIC), true);
            }
        }
    }
}
