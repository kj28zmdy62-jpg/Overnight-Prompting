package com.noclip.levels;

import com.noclip.block.L5PlaqueBlock;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Level 5, "Terror Hotel". Corridors of a grand 1920s hotel: red carpet, damask,
 * brass sconces, chandeliers, and a gramophone waltz playing behind some wall.
 * Every door is numbered, and the numbers count down as you approach Room 000.
 * Room 000 opens onto the boiler room far below. Something in a red jacket
 * moves while you are not looking.
 */
public final class Level5 {
    static final int FLOOR = 20, CEIL = 25, BFLOOR = 4, BCEIL = 13, LINE = 12, SUPER = 8;

    private Level5() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_5")
                .number("5").title("Terror Hotel").order(50)
                .tagline("Check-in was a long time ago. The room numbers are counting down.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Terror())
                .visuals(new Visuals().tint(1.1f, 0.96f, 0.86f).saturation(0.9f).contrast(1.12f).brightness(1.12f)
                        .grain(0.1f).aberration(0.0015f).vignette(0.62f).fog(3f, 40f).fogColor(0x2A160E).bloom(0.6f)
                        .warp(0.025f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("bellhop"), 1, 0.18f).dist(20, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(12, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_6", 3).exit(ExitType.NOCLIP, "level_4", 1).exit(ExitType.NOCLIP, "level_13", 1)
                .exit(ExitType.DOOR, "level_6", 3).exit(ExitType.DOOR, "boiler_room", 2).exit(ExitType.DOOR, "level_7", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    /** Is there a corridor along z at x-line i between x-rows k and k+1? (All x-lines are corridors.) */
    static boolean segZ(long s, int i, int k) {
        return Hash.chance(s, i, k, 501, 0.55f);
    }

    static boolean lobby(long s, int i, int k) {
        return Hash.chance(s, i, k, 502, 0.035f) && (segZ(s, i, k) || segZ(s, i, k - 1));
    }

    static boolean open(long s, int x, int z) {
        int i = Math.round(x / (float) LINE), k = Math.round(z / (float) LINE);
        int dx = x - i * LINE, dz = z - k * LINE;
        if (Math.abs(dz) <= 1) return true;
        if (Math.abs(dx) <= 1 && segZ(s, i, Math.floorDiv(z, LINE))) return true;
        return lobby(s, i, k) && Math.abs(dx) <= 4 && Math.abs(dz) <= 4;
    }

    /** Room 000: one per 8x8 block of corridors, on the north wall of an x-corridor. Returns {x, z of the door}. */
    static int[] site(long s, int sx, int sk) {
        long h = Hash.hash(s, sx, sk, 503);
        int i = sx * SUPER + Hash.range(h, SUPER);
        int k = sk * SUPER + Hash.range(h >>> 16, SUPER);
        return new int[]{i * LINE + 6, k * LINE - 2};
    }

    static int roomNumber(long s, int x, int z) {
        int sx = Math.floorDiv(Math.floorDiv(x, LINE), SUPER), sk = Math.floorDiv(Math.floorDiv(z, LINE), SUPER);
        int best = Integer.MAX_VALUE;
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) {
                int[] p = site(s, sx + a, sk + b);
                best = Math.min(best, Math.abs(p[0] - x) + Math.abs(p[1] - z));
            }
        if (best == 0) return 0;
        return Math.max(1, Math.min(999, best / 3 + 1));
    }

    // ---------------------------------------------------------------- generator

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("hotel_carpet");
        final BlockState paper = ModBlocks.state("hotel_wallpaper");
        final BlockState paperStain = ModBlocks.state("hotel_wallpaper_stained");
        final BlockState paperScratch = ModBlocks.state("hotel_wallpaper_scratched");
        final BlockState crown = ModBlocks.state("hotel_crown_molding");
        final BlockState wainscot = ModBlocks.state("hotel_wainscot");
        final BlockState ceiling = ModBlocks.state("hotel_ceiling");
        final BlockState doorLow = ModBlocks.state("hotel_door_closed");
        final BlockState doorTop = ModBlocks.state("hotel_door_closed_top");
        final BlockState roomDoor = ModBlocks.state("hotel_room_door");
        final BlockState boilerDoor = ModBlocks.state("hotel_boiler_door");
        final BlockState boilerDoorTop = ModBlocks.state("hotel_boiler_door_top");
        final L5PlaqueBlock plaque = (L5PlaqueBlock) ModBlocks.get("hotel_room_plaque");
        final BlockState sconce = ModBlocks.state("hotel_sconce");
        final BlockState sconceDead = ModBlocks.state("hotel_sconce_dead");
        final BlockState chandelier = ModBlocks.state("hotel_chandelier");
        final BlockState chain = ModBlocks.state("hotel_chain");
        final BlockState[] paintings = {ModBlocks.state("hotel_painting_0"), ModBlocks.state("hotel_painting_1"),
                ModBlocks.state("hotel_painting_2")};
        final BlockState bed = ModBlocks.state("hotel_bed");
        final BlockState lamp = ModBlocks.state("hotel_nightstand_lamp");
        final BlockState plant = ModBlocks.state("office_plant");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState boiler = ModBlocks.state("hotel_boiler_shell");
        final BlockState furnace = ModBlocks.state("hotel_boiler_furnace");
        final BlockState bBrick = ModBlocks.state("hotel_boiler_brick");
        final BlockState bFloor = ModBlocks.state("hotel_boiler_floor");
        final BlockState grate = ModBlocks.state("hotel_boiler_grate");
        final BlockState hatch = ModBlocks.state("hotel_furnace_hatch");
        final BlockState riser = ModBlocks.state("scalding_riser");
        final BlockState vent = ModBlocks.state("steam_vent_grate");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, BFLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, BCEIL + 1, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            boilerLevel(w, s);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, FLOOR, z, carpet);
                    w.set(x, CEIL, z, ceiling);
                    if (!open(s, x, z)) {
                        w.set(x, FLOOR + 1, z, wainscot);
                        for (int y = FLOOR + 2; y < CEIL - 1; y++) {
                            float r = Hash.unit(s, x, z, y);
                            w.set(x, y, z, r < 0.03f ? paperStain : r < 0.04f ? paperScratch : paper);
                        }
                        w.set(x, CEIL - 1, z, crown);
                    }
                }
            }
            corridors(w, s);
        }

        boolean dim(long s, int x, int z) {
            return Hash.fbm(s ^ 0x5D1L, x / 40.0, z / 40.0, 2) > 0.66f;
        }

        void corridors(ChunkWriter w, long s) {
            int i0 = Math.floorDiv(w.minX, LINE) - 1, i1 = Math.floorDiv(w.maxX, LINE) + 1;
            int k0 = Math.floorDiv(w.minZ, LINE) - 1, k1 = Math.floorDiv(w.maxZ, LINE) + 1;
            for (int k = k0; k <= k1; k++) {
                int zc = k * LINE;
                // x-corridor along z = zc: doors on both walls every 4 blocks, sconces between
                for (int x = w.minX - 2; x <= w.maxX + 2; x++) {
                    int dx = Math.floorMod(x, LINE);
                    boolean nearCross = dx <= 3 || dx >= LINE - 3;
                    if (Math.floorMod(x, 4) == 2 && !nearCross) {
                        doorAt(w, s, x, zc - 2, Direction.NORTH);
                        doorAt(w, s, x, zc + 2, Direction.SOUTH);
                    }
                    if (Math.floorMod(x, 4) == 0 && !nearCross) {
                        boolean north = Math.floorMod(x, 8) == 0;
                        int z = north ? zc - 1 : zc + 1;
                        Direction f = north ? Direction.NORTH : Direction.SOUTH;
                        if (!open(s, x, north ? zc - 2 : zc + 2)) {
                            boolean dead = dim(s, x, zc) ? Hash.chance(s, x, zc, 61, 0.8f) : Hash.chance(s, x, zc, 61, 0.06f);
                            w.set(x, FLOOR + 3, z, ClassicsKit.facing(dead ? sconceDead : sconce, f));
                        }
                    }
                    if (dx == 6 && !dim(s, x, zc)) {
                        w.set(x, CEIL - 1, zc, chandelier);
                    }
                }
                for (int i = i0; i <= i1; i++) {
                    int xc = i * LINE;
                    if (lobby(s, i, k)) lobbyAt(w, s, xc, zc);
                    if (!segZ(s, i, k)) continue;
                    for (int z = zc + 4; z <= zc + LINE - 4; z++) {
                        if (Math.floorMod(z, 4) == 2) {
                            closedDoor(w, s, xc - 2, z, Direction.WEST);
                            closedDoor(w, s, xc + 2, z, Direction.EAST);
                        }
                        if (Math.floorMod(z, 4) == 0) {
                            boolean west = Math.floorMod(z, 8) == 0;
                            int x = west ? xc - 1 : xc + 1;
                            if (!open(s, west ? xc - 2 : xc + 2, z)) {
                                boolean dead = dim(s, xc, z) ? Hash.chance(s, xc, z, 62, 0.8f) : Hash.chance(s, xc, z, 62, 0.06f);
                                w.set(x, FLOOR + 3, z, ClassicsKit.facing(dead ? sconceDead : sconce, west ? Direction.WEST : Direction.EAST));
                            }
                        }
                        if (z == zc + 6 && !dim(s, xc, z)) w.set(xc, CEIL - 1, z, chandelier);
                    }
                }
            }
        }

        /** A door in the wall at (x, wallZ); the corridor is on the side opposite to `wall`. */
        void doorAt(ChunkWriter w, long s, int x, int wallZ, Direction wall) {
            if (open(s, x, wallZ)) return;
            int cz = wallZ - wall.getStepZ();   // corridor-side column in front of the door
            int n = roomNumber(s, x, wallZ);
            if (n == 0) {
                w.set(x, FLOOR + 1, wallZ, boilerDoor);
                w.set(x, FLOOR + 2, wallZ, boilerDoorTop);
                w.set(x, FLOOR + 3, cz, plaque.withNumber(0).setValue(L5PlaqueBlock.FACING, wall));
                return;
            }
            w.set(x, FLOOR + 3, cz, plaque.withNumber(n).setValue(L5PlaqueBlock.FACING, wall));
            long h = Hash.hash(s, x, wallZ, 504);
            if (Hash.unit(h) < 0.07f) {
                // an unlocked room: three by three, a bed, a lamp, a painting
                BlockState lower = roomDoor.setValue(DoorBlock.FACING, wall.getOpposite()).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                w.set(x, FLOOR + 1, wallZ, lower);
                w.set(x, FLOOR + 2, wallZ, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                int dz = wall.getStepZ();
                for (int d = 1; d <= 3; d++)
                    for (int dx = -1; dx <= 1; dx++)
                        for (int y = FLOOR + 1; y < CEIL - 1; y++) w.set(x + dx, y, wallZ + d * dz, ModBlocks.AIR);
                int back = wallZ + 3 * dz;
                w.set(x, FLOOR + 1, back, ClassicsKit.facing(bed, wall));
                w.set(x - 1, FLOOR + 1, back, ClassicsKit.facing(lamp, wall));
                if (Hash.unit(h >>> 8) < 0.4f) w.set(x + 1, FLOOR + 1, back, bottle);
                w.set(x, FLOOR + 3, back, ClassicsKit.facing(paintings[Hash.range(h >>> 12, 3)], wall));
            } else {
                w.set(x, FLOOR + 1, wallZ, doorLow);
                w.set(x, FLOOR + 2, wallZ, doorTop);
                if (Hash.unit(h >>> 20) < 0.08f) {
                    int px = x + 2;
                    if (!open(s, px, wallZ)) w.set(px, FLOOR + 2, cz, ClassicsKit.facing(paintings[Hash.range(h >>> 24, 3)], wall));
                }
            }
        }

        void closedDoor(ChunkWriter w, long s, int wallX, int z, Direction wall) {
            if (open(s, wallX, z)) return;
            int cx = wallX - wall.getStepX();
            w.set(wallX, FLOOR + 1, z, doorLow);
            w.set(wallX, FLOOR + 2, z, doorTop);
            w.set(cx, FLOOR + 3, z, plaque.withNumber(roomNumber(s, wallX, z)).setValue(L5PlaqueBlock.FACING, wall));
        }

        void lobbyAt(ChunkWriter w, long s, int xc, int zc) {
            w.set(xc, CEIL - 1, zc, chandelier);
            for (int a = -3; a <= 3; a += 6)
                for (int b = -3; b <= 3; b += 6) {
                    w.set(xc + a, FLOOR + 1, zc + b, plant);
                    w.set(xc + a, CEIL - 1, zc + b, chandelier);
                }
        }

        // -------------------------------------------------------- the boiler room

        void boilerLevel(ChunkWriter w, long s) {
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int u = Math.floorMod(x, 8), v = Math.floorMod(z, 8);
                    w.set(x, BFLOOR, z, Hash.chance(s, x, z, 71, 0.05f) ? grate : bFloor);
                    w.set(x, BCEIL, z, bBrick);
                    if (u <= 1 && v <= 1) for (int y = BFLOOR + 1; y < BCEIL; y++) w.set(x, y, z, bBrick);
                }
            int i0 = Math.floorDiv(w.minX, 8) - 1, i1 = Math.floorDiv(w.maxX, 8) + 1;
            int k0 = Math.floorDiv(w.minZ, 8) - 1, k1 = Math.floorDiv(w.maxZ, 8) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) {
                    long h = Hash.hash(s, i, k, 72);
                    int bx = i * 8 + 3, bz = k * 8 + 3;
                    float r = Hash.unit(h);
                    if (r < 0.38f) {
                        BlockState shell = boiler.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                        for (int dx = 0; dx < 3; dx++)
                            for (int dz = 0; dz < 3; dz++)
                                for (int y = BFLOOR + 1; y <= BFLOOR + 5; y++) w.set(bx + dx, y, bz + dz, shell);
                        Direction f = Direction.from2DDataValue(Hash.range(h >>> 8, 4));
                        int fx = bx + 1 + f.getStepX(), fz = bz + 1 + f.getStepZ();
                        w.set(fx, BFLOOR + 1, fz, Hash.unit(h >>> 12) < 0.12f ? hatch : furnace);
                        for (int y = BFLOOR + 6; y < BCEIL; y++) w.set(bx + 1, y, bz + 1, riser);
                    } else if (r < 0.5f) {
                        for (int y = BFLOOR + 1; y < BCEIL; y++) w.set(bx + 1, y, bz + 1, riser);
                        w.set(bx + 2, BFLOOR, bz + 2, vent);
                    } else if (r < 0.56f) {
                        w.set(bx + 1, BFLOOR, bz + 1, vent);
                        w.set(bx + 2, BFLOOR, bz + 1, vent);
                    }
                }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Knocks on doors behind you, and the heat and roar of the boilers below. */
    static final class Terror implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            if (p.getY() < FLOOR - 2) {
                if (p.tickCount % 140 == 0) ClassicsKit.soundTo(p, "event.level_5_boiler", 1.0f, 0.9f + p.getRandom().nextFloat() * 0.2f);
                if (p.tickCount % 40 == 0 && !p.isCreative()) Sanity.add(p, -0.6f);
                return;
            }
            if (p.tickCount % 20 != 0 || p.getRandom().nextInt(28) != 0) return;
            Block door = ModBlocks.get("hotel_door_closed");
            BlockPos at = p.blockPosition();
            for (int attempt = 0; attempt < 30; attempt++) {
                BlockPos q = new BlockPos(at.getX() + p.getRandom().nextInt(25) - 12, FLOOR + 1, at.getZ() + p.getRandom().nextInt(25) - 12);
                if (q.distSqr(at) < 25 || !level.getBlockState(q).is(door)) continue;
                if (ClassicsKit.seen(p, Vec3.atCenterOf(q.above()), 0.5)) continue;
                ClassicsKit.sound(level, q, "event.level_5_knock", 1.3f, 0.85f + p.getRandom().nextFloat() * 0.3f);
                return;
            }
        }
    }

    /** Room 000: the stairs go down a long, long way, into the boiler room. */
    public static void descend(ServerLevel level, BlockPos door, ServerPlayer p) {
        ClassicsKit.sound(level, door, "event.level_5_descend", 1.4f, 1f);
        BlockPos best = null;
        for (int r = 0; r <= 8 && best == null; r++)
            for (int dx = -r; dx <= r && best == null; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos q = new BlockPos(door.getX() + dx, BFLOOR + 1, door.getZ() + dz);
                    if (level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                            && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                            && !level.getBlockState(q).is(ModBlocks.get("scalding_riser"))) {
                        best = q;
                        break;
                    }
                }
        if (best == null) best = new BlockPos(door.getX(), BFLOOR + 1, door.getZ());
        p.teleportTo(level, best.getX() + 0.5, best.getY(), best.getZ() + 0.5, p.getYRot(), 10f);
        p.resetFallDistance();
        ClassicsKit.event(p, "flash", 0.7f);
        ClassicsKit.soundTo(p, "event.level_5_boiler", 1.2f, 0.8f);
    }
}
