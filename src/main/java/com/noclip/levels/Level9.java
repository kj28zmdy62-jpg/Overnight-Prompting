package com.noclip.levels;

import com.noclip.block.FlickerLightBlock;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Level 9, "Darkened Suburbs". One house, copied down every street forever, all
 * of them dark, at two in the morning. The streetlights work, until you walk past
 * them: the ones behind you go out, one by one, and the dark follows you.
 * Somewhere there is a house with every light on.
 */
public final class Level9 {
    static final int G = 20, XR = 36, ZR = 56;

    private Level9() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_9")
                .number("9").title("Darkened Suburbs").order(90)
                .tagline("Every house is the same house. Every light behind you goes out.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Streetlights())
                .visuals(new Visuals().tint(0.84f, 0.9f, 1.1f).saturation(0.62f).contrast(1.12f).brightness(1.05f)
                        .grain(0.1f).aberration(0.0016f).vignette(0.6f).fog(8f, 72f).fogColor(0x0A0E18).bloom(0.65f)
                        .sky())
                .sanityDrain(0.045f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 3, 0.6f).dark().dist(12, 28).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(28, 44).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.15f).dist(26, 40).weight(2))
                .exit(ExitType.NOCLIP, "level_10", 3).exit(ExitType.NOCLIP, "level_11", 1)
                .exit(ExitType.DOOR, "level_10", 4).exit(ExitType.DOOR, "level_11", 1)
                .exit(ExitType.DESCEND, "storm_drains", 2).exit(ExitType.DESCEND, "level_8", 1).exit(ExitType.DESCEND, "sewers", 1)
                .build();
    }

    static int nearest(int v, int step) {
        return Math.round(v / (float) step) * step;
    }

    /** Streetlight head positions within r of (x, z). */
    static List<BlockPos> lamps(int px, int pz, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int k = Math.floorDiv(pz - r, XR); k <= Math.floorDiv(pz + r, XR) + 1; k++) {
            for (int x = Math.floorDiv(px - r, 14) * 14; x <= px + r; x += 14) {
                int m = Math.floorMod(x, ZR);
                if (m == 14 || m == 42) out.add(new BlockPos(x, G + 6, k * XR - 3));
                if (m == 28) out.add(new BlockPos(x, G + 6, k * XR + 3));
            }
        }
        for (int i = Math.floorDiv(px - r, ZR); i <= Math.floorDiv(px + r, ZR) + 1; i++) {
            for (int z = Math.floorDiv(pz - r, 9) * 9; z <= pz + r; z += 9) {
                int m = Math.floorMod(z, XR);
                if (m == 18) out.add(new BlockPos(i * ZR - 3, G + 6, z));
                if (m == 9 || m == 27) out.add(new BlockPos(i * ZR + 3, G + 6, z));
            }
        }
        return out;
    }

    static final class Gen implements LevelGenerator {
        final BlockState grass = ModBlocks.state("suburb_grass");
        final BlockState road = ModBlocks.state("suburb_road");
        final BlockState lineX = ModBlocks.state("suburb_road_line_x");
        final BlockState lineZ = ModBlocks.state("suburb_road_line_z");
        final BlockState walk = ModBlocks.state("suburb_sidewalk");
        final BlockState siding = ModBlocks.state("suburb_siding");
        final BlockState window = ModBlocks.state("suburb_window");
        final BlockState windowLit = ModBlocks.state("suburb_window_lit");
        final BlockState windowFigure = ModBlocks.state("suburb_window_figure");
        final BlockState roofStairs = ModBlocks.state("suburb_roof_stairs");
        final BlockState trim = ModBlocks.state("suburb_trim");
        final BlockState frontDoor = ModBlocks.state("suburb_front_door");
        final BlockState doorClosed = ModBlocks.state("suburb_door_closed");
        final BlockState doorClosedTop = ModBlocks.state("suburb_door_closed_top");
        final BlockState porch = ModBlocks.state("suburb_porch_light");
        final BlockState porchOff = ModBlocks.state("suburb_porch_light_off");
        final BlockState lamp = ModBlocks.state("suburb_streetlamp");
        final BlockState pole = ModBlocks.state("suburb_lamp_pole");
        final BlockState picket = ModBlocks.state("suburb_picket_fence");
        final BlockState manhole = ModBlocks.state("suburb_manhole");
        final BlockState mailbox = ModBlocks.state("suburb_mailbox");
        final BlockState trunk = ModBlocks.state("suburb_tree_trunk");
        final BlockState leaves = ModBlocks.state("suburb_leaves");
        final BlockState floor = ModBlocks.state("suburb_floorboards");
        final BlockState couch = ModBlocks.state("suburb_couch");
        final BlockState tv = ModBlocks.state("suburb_tv");
        final BlockState tvOn = ModBlocks.state("suburb_tv_on");
        final BlockState table = ModBlocks.state("break_table");
        final BlockState basement = ModBlocks.state("suburb_basement_door");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int dz = z - nearest(z, XR), dx = x - nearest(x, ZR);
                    boolean rx = Math.abs(dz) <= 2, rz = Math.abs(dx) <= 2;
                    BlockState b;
                    if (rx || rz) {
                        if (rx && rz) b = road;
                        else if (rx) b = dz == 0 ? lineX : road;
                        else b = dx == 0 ? lineZ : road;
                    } else if (Math.abs(dz) == 3 || Math.abs(dx) == 3) {
                        b = walk;
                    } else {
                        b = grass;
                    }
                    w.set(x, G, z, b);
                    // back fences between the two rows of houses
                    if (Math.floorMod(z, XR) == 18 && Math.abs(dx) > 3) {
                        boolean e = Math.abs(x + 1 - nearest(x + 1, ZR)) > 3, wv = Math.abs(x - 1 - nearest(x - 1, ZR)) > 3;
                        w.set(x, G + 1, z, picket.setValue(CrossCollisionBlock.EAST, e).setValue(CrossCollisionBlock.WEST, wv));
                    }
                }
            // streetlights
            for (BlockPos l : lamps((w.minX + w.maxX) / 2, (w.minZ + w.maxZ) / 2, 14)) {
                if (!w.contains(l.getX(), l.getZ())) continue;
                for (int y = G + 1; y <= G + 5; y++) w.set(l.getX(), y, l.getZ(), pole);
                w.set(l.getX(), G + 6, l.getZ(), lamp);
            }
            // manholes
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (Math.floorMod(z, XR) == 0 && Math.floorMod(x, ZR) == 28 && Hash.chance(s, x, z, 91, 0.3f)) w.set(x, G, z, manhole);
                    if (Math.floorMod(x, ZR) == 0 && Math.floorMod(z, XR) == 18 && Hash.chance(s, x, z, 92, 0.25f)) w.set(x, G, z, manhole);
                }
            // houses: four per row, two rows per block, all the same
            for (int i = Math.floorDiv(w.minX, ZR) - 1; i <= Math.floorDiv(w.maxX, ZR) + 1; i++)
                for (int k = Math.floorDiv(w.minZ, XR) - 1; k <= Math.floorDiv(w.maxZ, XR) + 1; k++)
                    for (int j = 0; j < 4; j++) {
                        int ox = i * ZR + 5 + 12 * j;
                        if (ox + 10 < w.minX || ox - 2 > w.maxX) continue;
                        house(w, s, ox, k * XR + 7, true);
                        house(w, s, ox, (k + 1) * XR - 7, false);
                    }
        }

        void house(ChunkWriter w, long s, int ox, int oz, boolean facesNorth) {
            int sign = facesNorth ? 1 : -1;
            if (Math.min(oz - 4 * sign, oz + 11 * sign) > w.maxZ || Math.max(oz - 4 * sign, oz + 11 * sign) < w.minZ) return;
            Direction front = facesNorth ? Direction.NORTH : Direction.SOUTH;
            Direction back = front.getOpposite();
            long h = Hash.hash(s, ox, oz, 93);
            float r = Hash.unit(h);
            boolean lit = r < 0.025f, open = lit || r < 0.06f;
            boolean figure = !lit && Hash.unit(h >>> 8) < 0.006f;
            BlockState win = lit ? windowLit : window;
            // floor and walls
            for (int lx = 0; lx <= 8; lx++)
                for (int lz = 0; lz <= 7; lz++) {
                    int x = ox + lx, z = oz + lz * sign;
                    boolean edge = lx == 0 || lx == 8 || lz == 0 || lz == 7;
                    w.set(x, G + 1, z, edge ? walk : floor);
                    for (int y = G + 2; y <= G + 4; y++) w.set(x, y, z, edge ? siding : ModBlocks.AIR);
                    w.set(x, G + 5, z, trim);
                }
            // windows: two at the front, one each side, two at the back
            put(w, ox, oz, sign, 5, G + 3, 0, figure ? windowFigure : win);
            put(w, ox, oz, sign, 6, G + 3, 0, win);
            put(w, ox, oz, sign, 0, G + 3, 3, win);
            put(w, ox, oz, sign, 8, G + 3, 3, win);
            put(w, ox, oz, sign, 2, G + 3, 7, win);
            put(w, ox, oz, sign, 6, G + 3, 7, win);
            // the front door
            if (open) {
                BlockState d = frontDoor.setValue(DoorBlock.FACING, back).setValue(DoorBlock.OPEN, true);
                put(w, ox, oz, sign, 2, G + 2, 0, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                put(w, ox, oz, sign, 2, G + 3, 0, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            } else {
                put(w, ox, oz, sign, 2, G + 2, 0, doorClosed);
                put(w, ox, oz, sign, 2, G + 3, 0, doorClosedTop);
            }
            put(w, ox, oz, sign, 3, G + 4, -1, ClassicsKit.facing(lit ? porch : porchOff, back));
            for (int lx = 1; lx <= 3; lx++) put(w, ox, oz, sign, lx, G + 1, -1, walk);
            for (int lz = -2; lz >= -3; lz--) put(w, ox, oz, sign, 2, G, lz, walk);
            put(w, ox, oz, sign, 0, G + 1, -3, ClassicsKit.facing(mailbox, front));
            // gable roof, ridge running along x
            for (int lz = -1; lz <= 8; lz++) {
                boolean frontSlope = lz <= 3;
                int ys = G + 5 + (frontSlope ? lz + 1 : 8 - lz);
                BlockState st = roofStairs.setValue(StairBlock.FACING, frontSlope ? back : front);
                for (int lx = -1; lx <= 9; lx++) put(w, ox, oz, sign, lx, ys, lz, st);
                if (lz >= 0 && lz <= 7)
                    for (int lx = 0; lx <= 8; lx++)
                        for (int y = G + 6; y < ys; y++) put(w, ox, oz, sign, lx, y, lz, siding);
            }
            // inside: the same living room in every house
            put(w, ox, oz, sign, 4, G + 2, 5, ClassicsKit.facing(couch, front));
            put(w, ox, oz, sign, 5, G + 2, 5, ClassicsKit.facing(couch, front));
            put(w, ox, oz, sign, 4, G + 2, 1, ClassicsKit.facing(lit || Hash.unit(h >>> 16) < 0.05f ? tvOn : tv, front));
            put(w, ox, oz, sign, 2, G + 2, 5, table);
            if (Hash.unit(h >>> 20) < 0.2f) put(w, ox, oz, sign, 2, G + 3, 5, bottle);
            if (lit) {
                put(w, ox, oz, sign, 6, G + 2, 7, basement);
                put(w, ox, oz, sign, 6, G + 3, 7, basement);
            }
            // a tree out back, sometimes
            if (Hash.unit(h >>> 24) < 0.35f) {
                int tx = 3 + Hash.range(h >>> 28, 4);
                for (int y = G + 1; y <= G + 5; y++) put(w, ox, oz, sign, tx, y, 9, trunk.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                for (int a = -1; a <= 1; a++)
                    for (int b = -1; b <= 1; b++)
                        for (int y = G + 5; y <= G + 7; y++)
                            if (!(a == 0 && b == 0 && y == G + 5) && (Math.abs(a) + Math.abs(b) < 2 || y == G + 6))
                                put(w, ox, oz, sign, tx + a, y, 9 + b, leaves);
            }
        }

        static void put(ChunkWriter w, int ox, int oz, int sign, int lx, int y, int lz, BlockState st) {
            w.set(ox + lx, y, oz + lz * sign, st);
        }
    }

    /** The lights behind you go out. The ones far away come back on when nobody is near. */
    static final class Streetlights implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 12 != 0 || p.isSpectator()) return;
            Block lampBlock = ModBlocks.get("suburb_streetlamp");
            Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
            boolean killed = false;
            for (BlockPos l : lamps(p.getBlockX(), p.getBlockZ(), 72)) {
                if (!level.isLoaded(l)) continue;
                BlockState st = level.getBlockState(l);
                if (!st.is(lampBlock)) continue;
                boolean lit = st.getValue(FlickerLightBlock.LIT);
                Vec3 to = new Vec3(l.getX() + 0.5 - p.getX(), 0, l.getZ() + 0.5 - p.getZ());
                double d = to.length();
                if (lit && !killed && d > 9 && d < 52 && look.dot(to.normalize()) < -0.15
                        && !ClassicsKit.seen(p, Vec3.atCenterOf(l), 0.3)) {
                    level.setBlock(l, st.setValue(FlickerLightBlock.LIT, false), Block.UPDATE_CLIENTS);
                    ClassicsKit.sound(level, l, "event.level_9_lamp_off", 0.9f, 0.8f + p.getRandom().nextFloat() * 0.3f);
                    killed = true;
                } else if (!lit && d > 64 && !ClassicsKit.seenByAnyone(level, l)) {
                    level.setBlock(l, st.setValue(FlickerLightBlock.LIT, true), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
