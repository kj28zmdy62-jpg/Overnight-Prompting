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
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Golden Hour Suburbs. The same house, the same lawn, the same mailbox, block after
 * block, under a sun that is always about to set. Long shadows lie across the street.
 * Nothing moves. It is so peaceful that your mind mends while you walk. Stop walking,
 * and a music box starts to play, and you get very, very sleepy.
 */
public final class GoldenHour {
    static final String ID = "golden_hour";
    static final int G = 40;
    /** City block size: road, curb, sidewalk, four lots. */
    static final int C = 40;
    static final int ROAD = 7;
    /** Shadow length per block of height (the sun is low in the west). */
    static final float K = 3.2f;
    static final int SCAN = 30;

    private GoldenHour() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Golden Hour Suburbs").order(3050)
                .tagline("The sun is always just about to set. Keep walking.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Stillness())
                .visuals(new Visuals().tint(1.12f, 0.98f, 0.82f).saturation(1.08f).contrast(1.06f).brightness(1.08f)
                        .grain(0.045f).aberration(0.0009f).vignette(0.42f).warp(0.01f).scanlines(0.02f)
                        .fog(12f, 92f).fogColor(0xE6A266).bloom(0.75f).sky().clouds())
                .sanityDrain(-0.07f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.035f).seen().dist(30, 46))
                .exit(ExitType.DOOR, "level_9", 3).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.FALL, "storm_drains", 2).exit(ExitType.FALL, "sewers", 1)
                .exit(ExitType.SECRET, "level_9", 2).exit(ExitType.SECRET, "frontrooms", 1).exit(ExitType.SECRET, "the_void", 1)
                .exit(ExitType.NOCLIP, "level_9", 3).exit(ExitType.NOCLIP, "static_fields", 1)
                .exit(ExitType.NOCLIP, "foggy_playground", 1).exit(ExitType.NOCLIP, "golf_course", 1)
                .build();
    }

    // ---------------------------------------------------------------- the plan, as pure functions of position

    enum Ground { ROAD, CURB, SIDEWALK, LAWN, DRIVE, PATH }

    /** Distance into the city block from its edge (negative = on the road). */
    static int depth(int x, int z) {
        int u = Math.floorMod(x, C), v = Math.floorMod(z, C);
        if (u < ROAD || v < ROAD) return -1;
        return Math.min(Math.min(u - ROAD, C - 1 - u), Math.min(v - ROAD, C - 1 - v));
    }

    /** A lot is 13x13; lots start 3 in from the block edge, with a one-block fence line between them. */
    record Lot(int bi, int bk, int a, int b, int x0, int z0, boolean west) {
        /** Local coordinates with lu = 0 at the street-facing edge. */
        int lu(int x) { return west ? x - x0 : x0 + 12 - x; }
        int lv(int z) { return z - z0; }
        int wx(int lu) { return west ? x0 + lu : x0 + 12 - lu; }
    }

    static Lot lot(int x, int z) {
        int bi = Math.floorDiv(x, C), bk = Math.floorDiv(z, C);
        int u = Math.floorMod(x, C), v = Math.floorMod(z, C);
        int a = u >= 10 && u <= 22 ? 0 : u >= 24 && u <= 36 ? 1 : -1;
        int b = v >= 10 && v <= 22 ? 0 : v >= 24 && v <= 36 ? 1 : -1;
        if (a < 0 || b < 0) return null;
        return new Lot(bi, bk, a, b, bi * C + (a == 0 ? 10 : 24), bk * C + (b == 0 ? 10 : 24), a == 0);
    }

    static long lotHash(long s, Lot l, int salt) {
        return Hash.hash(s, l.bi * 2L + l.a, l.bk * 2L + l.b, salt);
    }

    enum Odd { NONE, OPEN_DOOR, LIT, FIGURE, MISSING, TRIKE, HOOP, PHANTOM }

    static Odd odd(long s, Lot l) {
        float r = Hash.unit(lotHash(s, l, 1));
        if (r < 0.025f) return Odd.OPEN_DOOR;
        if (r < 0.055f) return Odd.LIT;
        if (r < 0.075f) return Odd.FIGURE;
        if (r < 0.09f) return Odd.MISSING;
        if (r < 0.12f) return Odd.TRIKE;
        if (r < 0.16f) return Odd.HOOP;
        if (r < 0.172f) return Odd.PHANTOM;
        return Odd.NONE;
    }

    static boolean tree(long s, Lot l) {
        return Hash.unit(lotHash(s, l, 2)) < 0.6f;
    }

    static int treeV(long s, Lot l) {
        return (lotHash(s, l, 3) & 1) == 0 ? 1 : 11;
    }

    /** Height above ground of whatever stands in this column; used to paint the long shadows. */
    static int height(long s, int x, int z) {
        int u = Math.floorMod(x, C), v = Math.floorMod(z, C);
        if (lamp(u, v)) return 6;
        Lot l = lot(x, z);
        if (l == null) {
            if ((u == 23 && v >= 10 && v <= 36) || (v == 23 && u >= 10 && u <= 36)) return 1;
            return 0;
        }
        int lu = l.lu(x), lv = l.lv(z);
        Odd odd = odd(s, l);
        int h = 0;
        if (odd != Odd.MISSING && lu >= 3 && lu <= 11 && lv >= 1 && lv <= 11) {
            h = 6 + Math.min(lu - 3, 11 - lu);
            if (lu == 3 || lu == 11 || lv == 1 || lv == 11) h = 6;
            if (lu >= 4 && lu <= 10 && lv >= 2 && lv <= 10) h = Math.max(h, 5);
            h = Math.min(h, 9);
        }
        if (tree(s, l)) {
            int tv = treeV(s, l);
            int du = lu - 1, dv = lv - tv;
            if (du == 0 && dv == 0) h = Math.max(h, 8);
            else if (du * du + dv * dv <= 5) h = Math.max(h, 8);
        }
        if (lu == 0 && lv == 4) h = Math.max(h, 1);
        if (odd == Odd.PHANTOM && lu == 2 && lv == 7) h = Math.max(h, 2);
        return h;
    }

    static boolean lamp(int u, int v) {
        return (u == ROAD + 1 || u == C - 2) && v == 23 || (v == ROAD + 1 || v == C - 2) && u == 23;
    }

    static boolean shadowed(long s, int x, int z) {
        for (int d = 1; d <= SCAN; d++) {
            int h = height(s, x - d, z);
            if (h > 0 && h * K >= d) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- generator

    static final class Gen implements LevelGenerator {
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState drain = ModBlocks.state("gh_storm_drain");
        final BlockState fence = ModBlocks.state("gh_picket_fence");
        final BlockState trunk = ModBlocks.state("gh_trunk");
        final BlockState leaves = ModBlocks.state("gh_leaves");
        final BlockState found = ModBlocks.state("gh_foundation");
        final BlockState trim = ModBlocks.state("gh_trim");
        final BlockState window = ModBlocks.state("gh_window");
        final BlockState windowLit = ModBlocks.state("gh_window_lit");
        final BlockState windowFig = ModBlocks.state("gh_window_figure");
        final BlockState roofBlock = ModBlocks.state("gh_roof_block");
        final BlockState lampPole = ModBlocks.state("gh_lamp_pole");
        final String[] colours = {"cream", "mint", "blue", "yellow", "salmon"};
        final String[] cars = {"red", "beige", "blue", "green"};

        BlockState ground(Ground g, boolean shadow) {
            String key = switch (g) {
                case ROAD -> "road";
                case CURB -> "curb";
                case SIDEWALK, PATH -> "sidewalk";
                case LAWN -> "lawn";
                case DRIVE -> "drive";
            };
            return ModBlocks.state("gh_" + key + (shadow ? "_shadow" : "_sun"));
        }

        Ground groundAt(long s, int x, int z) {
            int d = depth(x, z);
            if (d < 0) return Ground.ROAD;
            if (d == 0) return Ground.CURB;
            if (d <= 2) return Ground.SIDEWALK;
            Lot l = lot(x, z);
            if (l == null) return Ground.LAWN;
            int lu = l.lu(x), lv = l.lv(z);
            if (lv >= 11 && lu <= 6) return Ground.DRIVE;
            if (lv == 6 && lu <= 2) return Ground.PATH;
            return Ground.LAWN;
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.fill(x, 0, z, x, G - 1, z, sub);
                    Ground g = groundAt(s, x, z);
                    w.set(x, G, z, ground(g, shadowed(s, x, z)));
                    int u = Math.floorMod(x, C), v = Math.floorMod(z, C);
                    if (g == Ground.CURB && (u == ROAD && v == 15 || v == ROAD && u == 31)
                            && Hash.chance(s, Math.floorDiv(x, C), Math.floorDiv(z, C), 0.5f)) {
                        w.set(x, G, z, drain);
                    }
                    if (lamp(u, v)) {
                        w.fill(x, G + 1, z, x, G + 5, z, lampPole);
                        Direction toRoad = u == ROAD + 1 ? Direction.WEST : u == C - 2 ? Direction.EAST
                                : v == ROAD + 1 ? Direction.NORTH : Direction.SOUTH;
                        w.set(x, G + 6, z, OutsideKit.facing("gh_lamp_head", toRoad));
                    }
                    if (lot(x, z) == null && depth(x, z) >= 3 && ((u == 23 && v >= 10 && v <= 36) || (v == 23 && u >= 10 && u <= 36))) {
                        boolean alongZ = u == 23;
                        w.set(x, G + 1, z, fence.setValue(FenceBlock.NORTH, alongZ).setValue(FenceBlock.SOUTH, alongZ)
                                .setValue(FenceBlock.EAST, !alongZ).setValue(FenceBlock.WEST, !alongZ));
                    }
                }
            }
            // lots: every house is drawn from its own lot so it is whole across chunk borders
            int bi0 = Math.floorDiv(w.minX, C), bi1 = Math.floorDiv(w.maxX, C);
            int bk0 = Math.floorDiv(w.minZ, C), bk1 = Math.floorDiv(w.maxZ, C);
            for (int bi = bi0 - 1; bi <= bi1 + 1; bi++)
                for (int bk = bk0 - 1; bk <= bk1 + 1; bk++)
                    for (int a = 0; a < 2; a++)
                        for (int b = 0; b < 2; b++) {
                            Lot l = new Lot(bi, bk, a, b, bi * C + (a == 0 ? 10 : 24), bk * C + (b == 0 ? 10 : 24), a == 0);
                            if (l.x0 > w.maxX + 1 || l.x0 + 12 < w.minX - 1 || l.z0 > w.maxZ + 1 || l.z0 + 12 < w.minZ - 1) continue;
                            drawLot(w, s, l);
                        }
        }

        void drawLot(ChunkWriter w, long s, Lot l) {
            Odd odd = odd(s, l);
            String colour = colours[Hash.range(lotHash(s, l, 4), colours.length)];
            Direction out = l.west ? Direction.WEST : Direction.EAST;
            // mailbox by the sidewalk
            w.set(l.wx(0), G + 1, l.z0 + 4, OutsideKit.facing("gh_mailbox", out));
            if (tree(s, l)) {
                int tx = l.wx(1), tz = l.z0 + treeV(s, l);
                w.fill(tx, G + 1, tz, tx, G + 5, tz, trunk);
                for (int dx = -2; dx <= 2; dx++)
                    for (int dy = -2; dy <= 2; dy++)
                        for (int dz = -2; dz <= 2; dz++) {
                            int dd = dx * dx + dy * dy * 2 + dz * dz;
                            if (dd <= 6 && !(dx == 0 && dz == 0 && dy < 1)
                                    && Hash.unit(s, tx + dx, tz + dz, dy + 31) < (dd <= 3 ? 1f : 0.8f)) {
                                w.set(tx + dx, G + 7 + dy, tz + dz, leaves);
                            }
                        }
            }
            if (Hash.unit(lotHash(s, l, 5)) < 0.28f && odd != Odd.HOOP) {
                OutsideKit.car(w, l.wx(1), G + 1, l.z0 + 11, out, cars[Hash.range(lotHash(s, l, 6), cars.length)], false);
            }
            if (odd == Odd.HOOP) {
                w.set(l.wx(7), G + 1, l.z0 + 12, OutsideKit.facing("gh_lamp_pole", out));
                w.set(l.wx(7), G + 2, l.z0 + 12, OutsideKit.facing("gh_lamp_pole", out));
                w.set(l.wx(7), G + 3, l.z0 + 12, OutsideKit.facing("gh_hoop", l.west ? Direction.EAST : Direction.WEST));
            }
            if (odd == Odd.TRIKE) {
                w.set(l.wx(1), G + 1, l.z0 + 8, OutsideKit.facing("gh_tricycle", Direction.from2DDataValue(Hash.range(lotHash(s, l, 7), 4))));
            }
            house(w, s, l, odd, colour);
        }

        void house(ChunkWriter w, long s, Lot l, Odd odd, String colour) {
            BlockState siding = ModBlocks.state("gh_siding_" + colour);
            // foundation pad
            for (int lu = 4; lu <= 10; lu++)
                for (int lv = 2; lv <= 10; lv++) w.set(l.wx(lu), G + 1, l.z0 + lv, found);
            if (odd == Odd.MISSING) return;
            for (int lu = 4; lu <= 10; lu++)
                for (int lv = 2; lv <= 10; lv++) {
                    boolean wall = lu == 4 || lu == 10 || lv == 2 || lv == 10;
                    if (!wall) continue;
                    int x = l.wx(lu), z = l.z0 + lv;
                    for (int y = G + 2; y <= G + 5; y++) {
                        BlockState b = (lu == 4 || lu == 10) && (lv == 2 || lv == 10) ? trim : siding;
                        if (lu == 4 && (lv == 3 || lv == 4 || lv == 8 || lv == 9) && (y == G + 3 || y == G + 4)) {
                            b = window;
                            if (odd == Odd.LIT && lv == 8) b = windowLit;
                            if (odd == Odd.FIGURE && lv == 3 && y == G + 3) b = windowFig;
                        }
                        w.set(x, y, z, b);
                    }
                }
            // side windows
            for (int lv : new int[]{2, 10})
                for (int lu : new int[]{6, 8}) w.set(l.wx(lu), G + 4, l.z0 + lv, window);
            // front door, porch light
            int dx = l.wx(4), dz = l.z0 + 6;
            if (odd == Odd.OPEN_DOOR) {
                w.set(dx, G + 2, dz, ModBlocks.state("gh_open_door"));
                w.set(dx, G + 3, dz, ModBlocks.state("gh_open_door"));
            } else {
                w.set(dx, G + 2, dz, ModBlocks.state("gh_door_bottom"));
                w.set(dx, G + 3, dz, ModBlocks.state("gh_door_top"));
            }
            Direction out = l.west ? Direction.WEST : Direction.EAST;
            w.set(l.wx(3), G + 4, dz, OutsideKit.facing("gh_porch_light", out.getOpposite()));
            // gabled roof, ridge running along z
            Direction up = l.west ? Direction.EAST : Direction.WEST;
            for (int k = 0; k <= 3; k++) {
                int y = G + 6 + k;
                int ua = 3 + k, ub = 11 - k;
                for (int lv = 1; lv <= 11; lv++) {
                    int z = l.z0 + lv;
                    w.set(l.wx(ua), y, z, ModBlocks.state("gh_roof").setValue(StairBlock.FACING, up));
                    w.set(l.wx(ub), y, z, ModBlocks.state("gh_roof").setValue(StairBlock.FACING, up.getOpposite()));
                    if (lv == 2 || lv == 10) for (int lu = ua + 1; lu < ub; lu++) w.set(l.wx(lu), y, z, siding);
                }
            }
            for (int lv = 1; lv <= 11; lv++) w.set(l.wx(7), G + 9, l.z0 + lv, roofBlock);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Everything here is perfectly still, and it wants you to be still too. Stand in one
     * place for a few seconds and a music box starts. Stay, and you fall asleep on the
     * lawn and wake up somewhere else, somewhere worse.
     */
    static final class Stillness implements LevelBehavior {
        static final class St {
            Vec3 last;
            int still;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            states.put(p.getUUID(), new St());
            if (OutsideKit.autotest()) {
                int bx = Math.floorDiv(p.getBlockX(), C) * C, bz = Math.floorDiv(p.getBlockZ(), C) * C;
                level.getChunk(bx >> 4, bz >> 4);
                p.teleportTo(level, bx + 3.5, G + 1, bz + 30.5, 180f, 0f);
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 20 != 0 || p.isCreative() || p.isSpectator()) return;
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            Vec3 pos = p.position();
            boolean moved = st.last == null || st.last.distanceToSqr(pos) > 0.15 * 0.15;
            st.last = pos;
            if (moved) {
                st.still = 0;
                return;
            }
            st.still++;
            if (st.still == 5) OutsideKit.sound(level, p.blockPosition(), "event.golden_hour_lullaby", 0.9f, 1f);
            if (st.still >= 5) ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.2f + 0.08f * (st.still - 5)));
            if (st.still == 9) {
                OutsideKit.sound(level, p.blockPosition(), "event.golden_hour_lullaby", 1.0f, 0.84f);
                Sanity.add(p, 4f);
            }
            if (st.still == 12) OutsideKit.sound(level, p.blockPosition(), "event.heartbeat", 1.0f, 0.7f);
            if (st.still >= 15) {
                st.still = 0;
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                Travel.exit(p, ExitType.SECRET, p.blockPosition());
            }
        }
    }
}
