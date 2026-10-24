package com.noclip.levels;

import com.noclip.entity.Leviathan;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 7, "Thalassophobia". You wake in a small, damp room on stilts. Its door
 * opens straight onto a black ocean with no far side. Buoys blink red on the
 * swell. Down on the sea floor, sickly green lamps mark doorways out. The longer
 * you swim, the tighter something enormous circles beneath you.
 */
public final class Level7 {
    static final int SEA = 40, CELL = 40;

    private Level7() {}

    public static BackroomsLevel create() {
        Leviathan.sea = SEA;
        return BackroomsLevel.builder("level_7")
                .number("7").title("Thalassophobia").order(70)
                .tagline("The door opens onto the sea. The sea goes down a long way.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Depths())
                .visuals(new Visuals().tint(0.86f, 0.96f, 1.08f).saturation(0.7f).contrast(1.1f).brightness(1.1f)
                        .grain(0.09f).aberration(0.0018f).vignette(0.65f).warp(0.03f).fog(2f, 40f).fogColor(0x03070B)
                        .bloom(0.6f).darkness(0.35f))
                .sanityDrain(0.04f)
                .spawnY(SEA + 2, SEA + 2)
                .exit(ExitType.WATER, "level_8", 5).exit(ExitType.WATER, "level_37", 1)
                .exit(ExitType.NOCLIP, "level_8", 3).exit(ExitType.NOCLIP, "level_6", 1).exit(ExitType.NOCLIP, "silent_beach", 1)
                .build();
    }

    static int floorY(long s, int x, int z) {
        float n = Hash.fbm(s ^ 0x7E1L, x / 48.0, z / 48.0, 3);
        int y = 6 + (int) (n * 16);
        if (Hash.fbm(s ^ 0x7E2L, x / 90.0, z / 90.0, 2) > 0.68f) y -= 4;
        return Math.max(2, y);
    }

    /** The room in cell (i,k): {x, z, door direction 0-3, flags}. */
    static int[] room(long s, int i, int k) {
        long h = Hash.hash(s, i, k, 701);
        return new int[]{i * CELL + 6 + Hash.range(h, CELL - 20), k * CELL + 6 + Hash.range(h >>> 12, CELL - 20),
                Hash.range(h >>> 24, 4), (int) ((h >>> 32) & 0xFF)};
    }

    static final class Gen implements LevelGenerator {
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState silt = ModBlocks.state("abyssal_silt");
        final BlockState rock = ModBlocks.state("abyssal_rock");
        final BlockState paper = ModBlocks.state("stilt_room_wallpaper");
        final BlockState boards = ModBlocks.state("stilt_room_boards");
        final BlockState ceiling = ModBlocks.state("stilt_room_ceiling");
        final BlockState concrete = ModBlocks.state("stilt_concrete");
        final BlockState roof = ModBlocks.state("stilt_room_roof");
        final BlockState window = ModBlocks.state("stilt_room_window");
        final BlockState lamp = ModBlocks.state("stilt_room_lamp");
        final BlockState painting = ModBlocks.state("stilt_room_painting");
        final BlockState armchair = ModBlocks.state("stilt_room_armchair");
        final BlockState table = ModBlocks.state("stilt_room_table");
        final BlockState radio = ModBlocks.state("stilt_room_radio");
        final BlockState doorLamp = ModBlocks.state("stilt_door_lamp");
        final BlockState buoy = ModBlocks.state("ocean_buoy");
        final BlockState frame = ModBlocks.state("sunken_door_frame");
        final BlockState doorway = ModBlocks.state("sunken_doorway");
        final BlockState deepLamp = ModBlocks.state("deep_lamp");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int f = floorY(s, x, z);
                    w.fill(x, 0, z, x, f - 2, z, sub);
                    w.set(x, f - 1, z, rock);
                    w.set(x, f, z, Hash.fbm(s ^ 0x7E3L, x / 9.0, z / 9.0, 2) > 0.62f ? rock : silt);
                    w.fill(x, f + 1, z, x, SEA, z, water);
                }
            int i0 = Math.floorDiv(w.minX, CELL) - 1, i1 = Math.floorDiv(w.maxX, CELL) + 1;
            int k0 = Math.floorDiv(w.minZ, CELL) - 1, k1 = Math.floorDiv(w.maxZ, CELL) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) {
                    roomAt(w, s, room(s, i, k));
                    seaFeatures(w, s, i, k);
                }
        }

        void roomAt(ChunkWriter w, long s, int[] r) {
            int rx = r[0], rz = r[1], d = r[2], flags = r[3];
            if (rx + 9 < w.minX || rx - 3 > w.maxX || rz + 9 < w.minZ || rz - 3 > w.maxZ) return;
            Direction door = Direction.from2DDataValue(d);
            int y0 = SEA + 1;
            // stilts down to the sea floor
            for (int[] c : new int[][]{{0, 0}, {6, 0}, {0, 6}, {6, 6}, {3, 0}, {3, 6}}) {
                int x = rx + c[0], z = rz + c[1];
                w.fill(x, floorY(s, x, z), z, x, SEA, z, concrete);
            }
            for (int dx = 0; dx <= 6; dx++)
                for (int dz = 0; dz <= 6; dz++) {
                    int x = rx + dx, z = rz + dz;
                    w.set(x, y0, z, boards);
                    w.set(x, y0 + 4, z, roof);
                    boolean edge = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                    for (int y = y0 + 1; y <= y0 + 3; y++) w.set(x, y, z, edge ? paper : ModBlocks.AIR);
                    if (!edge) w.set(x, y0 + 4, z, ceiling);
                }
            boolean dark = (flags & 15) < 2;
            w.set(rx + 3, y0 + 4, rz + 3, dark ? ceiling : lamp);
            // the door, centred on one wall, and a short ledge out over the water
            int cx = rx + 3 + door.getStepX() * 3, cz = rz + 3 + door.getStepZ() * 3;
            w.set(cx, y0 + 1, cz, ModBlocks.AIR);
            w.set(cx, y0 + 2, cz, ModBlocks.AIR);
            for (int out = 1; out <= 2; out++)
                for (int side = -1; side <= 1; side++) {
                    int x = cx + door.getStepX() * out + (door.getAxis() == Direction.Axis.Z ? side : 0);
                    int z = cz + door.getStepZ() * out + (door.getAxis() == Direction.Axis.X ? side : 0);
                    w.set(x, y0, z, boards);
                    if (out == 2 && side == 0) w.fill(x, floorY(s, x, z), z, x, SEA, z, concrete);
                }
            w.set(cx + door.getStepX(), y0 + 3, cz + door.getStepZ(), ClassicsKit.facing(doorLamp, door.getOpposite()));
            // windows in the two side walls
            Direction side = door.getClockWise();
            for (Direction sd : new Direction[]{side, side.getOpposite()})
                w.set(rx + 3 + sd.getStepX() * 3, y0 + 2, rz + 3 + sd.getStepZ() * 3, window);
            // furniture: an armchair facing the door, a table, a painting of the sea on the back wall
            Direction back = door.getOpposite();
            int bx = rx + 3 + back.getStepX() * 2, bz = rz + 3 + back.getStepZ() * 2;
            w.set(bx, y0 + 1, bz, ClassicsKit.facing(armchair, door));
            int tx = bx + side.getStepX(), tz = bz + side.getStepZ();
            w.set(tx, y0 + 1, tz, table);
            if ((flags & 16) != 0) w.set(tx, y0 + 2, tz, ClassicsKit.facing(radio, back));
            else w.set(tx, y0 + 2, tz, bottle);
            if ((flags & 96) == 0) w.set(bx - side.getStepX(), y0 + 1, bz - side.getStepZ(), supply);
            w.set(rx + 3 + back.getStepX() * 2, y0 + 2, rz + 3 + back.getStepZ() * 2, ClassicsKit.facing(painting, back));
        }

        void seaFeatures(ChunkWriter w, long s, int i, int k) {
            long h = Hash.hash(s, i, k, 702);
            // a blinking buoy or two on the swell
            for (int q = 0; q < 2; q++) {
                long hq = Hash.hash(h, q);
                int x = i * CELL + Hash.range(hq, CELL), z = k * CELL + Hash.range(hq >>> 16, CELL);
                w.set(x, SEA, z, buoy);
            }
            // false lights on the sea floor
            for (int q = 0; q < 2; q++) {
                long hq = Hash.hash(h, q + 10);
                int x = i * CELL + Hash.range(hq, CELL), z = k * CELL + Hash.range(hq >>> 16, CELL);
                w.set(x, floorY(s, x, z) + 1, z, deepLamp);
            }
            // a doorway standing on a rock spire, twelve blocks down, lit green
            if (Hash.unit(h >>> 40) < 0.45f) {
                int x = i * CELL + 4 + Hash.range(h >>> 3, CELL - 8), z = k * CELL + 4 + Hash.range(h >>> 19, CELL - 8);
                int top = SEA - 12;
                boolean alongX = (h & 1) == 0;
                for (int a = -2; a <= 2; a++)
                    for (int b = -1; b <= 1; b++) {
                        int px = x + (alongX ? a : b), pz = z + (alongX ? b : a);
                        w.fill(px, floorY(s, px, pz), pz, px, top, pz, rock);
                    }
                for (int a = -1; a <= 1; a++)
                    for (int y = top + 1; y <= top + 4; y++) {
                        int px = x + (alongX ? a : 0), pz = z + (alongX ? 0 : a);
                        boolean hole = a == 0 && y <= top + 2;
                        w.set(px, y, pz, hole ? doorway : frame);
                    }
                w.set(x, top + 4, z, deepLamp);
            }
        }
    }

    /** It finds swimmers. Being deep, and being in the water at all, wears on you. */
    static final class Depths implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 40 != 0 || p.isSpectator()) return;
            if (!p.isInWater()) return;
            if (!p.isCreative() && p.getY() < SEA - 8) Sanity.add(p, -0.8f);
            if (!level.getEntities(Leviathan.TYPE, p.getBoundingBox().inflate(128), e -> true).isEmpty()) return;
            Leviathan l = Leviathan.TYPE.create(level);
            if (l == null) return;
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            l.moveTo(p.getX() + Math.cos(a) * 45, Math.max(3, floorY(Hash.hash(level.getSeed(), Hash.salt("level_7")),
                    p.getBlockX(), p.getBlockZ()) + 2), p.getZ() + Math.sin(a) * 45, 0f, 0f);
            level.addFreshEntity(l);
        }
    }
}
