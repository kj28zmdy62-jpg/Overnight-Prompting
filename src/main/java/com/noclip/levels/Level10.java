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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Level 10, "Bumper Crop". Wheat taller than your eyes, in every direction,
 * under a flat grey sky. Dirt tracks, red barns, silos marked NO ENTRY, and
 * scarecrows that are always facing you when you look back. Every few minutes
 * an engine starts somewhere and the wheat begins to fall in a straight line,
 * coming your way. There is no machine. Get out of the field.
 */
public final class Level10 {
    static final int G = 20, CELL = 64;

    private Level10() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_10")
                .number("10").title("Bumper Crop").order(100)
                .tagline("A quiet field under a grey sky. Listen for the engine.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Harvest())
                .visuals(new Visuals().tint(1.02f, 1.0f, 0.94f).saturation(0.62f).contrast(0.98f).brightness(1.02f)
                        .grain(0.06f).aberration(0.001f).vignette(0.4f).fog(10f, 90f).fogColor(0x9A9C98).bloom(0.3f)
                        .sky().clouds())
                .sanityDrain(0.012f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(28, 44))
                .exit(ExitType.NOCLIP, "level_11", 4).exit(ExitType.NOCLIP, "level_9", 1)
                .exit(ExitType.DOOR, "level_11", 4).exit(ExitType.DOOR, "level_9", 1)
                .exit(ExitType.DESCEND, "level_8", 1).exit(ExitType.DESCEND, "storm_drains", 1)
                .build();
    }

    static boolean track(int x, int z) {
        return Math.floorMod(x + 1, CELL) <= 2 || Math.floorMod(z + 1, CELL) <= 2;
    }

    static boolean farmstead(long s, int i, int k) {
        return Hash.chance(s, i, k, 1001, 0.3f);
    }

    /** True if (x,z) is inside a farmstead yard (no wheat). */
    static boolean yard(long s, int x, int z) {
        int i = Math.floorDiv(x, CELL), k = Math.floorDiv(z, CELL);
        if (!farmstead(s, i, k)) return false;
        int u = x - i * CELL, v = z - k * CELL;
        return u >= 3 && u <= 30 && v >= 3 && v <= 26;
    }

    static List<BlockPos> scarecrows(long s, int x, int z, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = Math.floorDiv(x - r, CELL); i <= Math.floorDiv(x + r, CELL); i++)
            for (int k = Math.floorDiv(z - r, CELL); k <= Math.floorDiv(z + r, CELL); k++)
                for (int q = 0; q < 3; q++) {
                    long h = Hash.hash(s, i, k, 1002 + q);
                    int px = i * CELL + 4 + Hash.range(h, CELL - 8), pz = k * CELL + 4 + Hash.range(h >>> 16, CELL - 8);
                    if (!yard(s, px, pz) && !track(px, pz)) out.add(new BlockPos(px, G + 2, pz));
                }
        return out;
    }

    static final class Gen implements LevelGenerator {
        final BlockState soil = ModBlocks.state("crop_soil");
        final BlockState trackB = ModBlocks.state("crop_track");
        final BlockState grass = ModBlocks.state("crop_grass");
        final BlockState lower = ModBlocks.state("wheat_lower");
        final BlockState upper = ModBlocks.state("wheat_upper");
        final BlockState upperB = ModBlocks.state("wheat_upper_b");
        final BlockState planks = ModBlocks.state("barn_planks");
        final BlockState trim = ModBlocks.state("barn_trim");
        final BlockState roof = ModBlocks.state("barn_roof");
        final BlockState roofStairs = ModBlocks.state("barn_roof_stairs");
        final BlockState barnFloor = ModBlocks.state("barn_floor");
        final BlockState hay = ModBlocks.state("crop_hay_bale");
        final BlockState siloWall = ModBlocks.state("silo_wall");
        final BlockState siloDoor = ModBlocks.state("silo_door");
        final BlockState hatch = ModBlocks.state("barn_cellar_hatch");
        final BlockState fence = ModBlocks.state("crop_fence");
        final BlockState pole = ModBlocks.state("crop_pole");
        final BlockState body = ModBlocks.state("scarecrow_body");
        final BlockState head = ModBlocks.state("scarecrow_head");
        final BlockState lantern = ModBlocks.state("cave_lantern");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (track(x, z)) {
                        boolean centre = Math.floorMod(x + 1, CELL) == 1 || Math.floorMod(z + 1, CELL) == 1;
                        w.set(x, G, z, centre && !(Math.floorMod(x + 1, CELL) <= 2 && Math.floorMod(z + 1, CELL) <= 2) ? grass : trackB);
                        continue;
                    }
                    if (yard(s, x, z)) {
                        w.set(x, G, z, trackB);
                        continue;
                    }
                    w.set(x, G, z, soil);
                    boolean bare = Hash.fbm(s ^ 0x10AL, x / 30.0, z / 30.0, 2) > 0.74f;
                    if (bare) continue;
                    w.set(x, G + 1, z, lower);
                    w.set(x, G + 2, z, Hash.chance(s, x, z, 3, 0.5f) ? upperB : upper);
                }
            // poles beside the tracks, every 16 blocks
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean along = Math.floorMod(z + 1, CELL) == 3 && Math.floorMod(x, 16) == 8;
                    boolean across = Math.floorMod(x + 1, CELL) == 3 && Math.floorMod(z, 16) == 8;
                    if (along || across) {
                        for (int y = G + 1; y <= G + 7; y++) w.set(x, y, z, pole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                        for (int c = -1; c <= 1; c++)
                            w.set(x + (along ? 0 : c), G + 7, z + (along ? c : 0), pole.setValue(RotatedPillarBlock.AXIS, along ? Direction.Axis.Z : Direction.Axis.X));
                    }
                }
            for (int i = Math.floorDiv(w.minX, CELL) - 1; i <= Math.floorDiv(w.maxX, CELL); i++)
                for (int k = Math.floorDiv(w.minZ, CELL) - 1; k <= Math.floorDiv(w.maxZ, CELL); k++)
                    if (farmstead(s, i, k)) farm(w, s, i * CELL, k * CELL, Hash.hash(s, i, k, 1003));
            for (BlockPos p : scarecrows(s, (w.minX + w.maxX) / 2, (w.minZ + w.maxZ) / 2, 12)) {
                if (!w.contains(p.getX(), p.getZ())) continue;
                Direction f = Direction.from2DDataValue(Hash.range(Hash.hash(s, p.getX(), p.getZ(), 5), 4));
                w.set(p.getX(), G + 1, p.getZ(), ClassicsKit.facing(body, f));
                w.set(p.getX(), G + 2, p.getZ(), ClassicsKit.facing(body, f));
                w.set(p.getX(), G + 3, p.getZ(), ClassicsKit.facing(head, f));
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        if (dx != 0 || dz != 0) {
                            w.set(p.getX() + dx, G + 1, p.getZ() + dz, ModBlocks.AIR);
                            w.set(p.getX() + dx, G + 2, p.getZ() + dz, ModBlocks.AIR);
                        }
            }
        }

        /** A red barn, a silo marked NO ENTRY, a fenced yard. */
        void farm(ChunkWriter w, long s, int ox, int oz, long h) {
            int bx = ox + 6, bz = oz + 6;          // barn footprint 13 x 17
            int W = 12, D = 16;
            for (int dx = 0; dx <= W; dx++)
                for (int dz = 0; dz <= D; dz++) {
                    int x = bx + dx, z = bz + dz;
                    w.set(x, G, z, barnFloor);
                    boolean edge = dx == 0 || dx == W || dz == 0 || dz == D;
                    boolean doorway = (dz == 0 || dz == D) && dx >= 5 && dx <= 7;
                    for (int y = G + 1; y <= G + 6; y++) {
                        BlockState b = edge ? ((dx == 0 || dx == W) && (dz == 0 || dz == D) ? trim : planks) : ModBlocks.AIR;
                        if (doorway && y <= G + 4) b = ModBlocks.AIR;
                        if (doorway && y == G + 5) b = trim;
                        w.set(x, y, z, b);
                    }
                }
            // gable roof along z
            for (int dx = -1; dx <= W + 1; dx++) {
                int dist = Math.min(dx + 1, W + 1 - dx);
                int y = G + 7 + dist;
                boolean ridge = dx == W / 2;
                for (int dz = -1; dz <= D + 1; dz++) {
                    int x = bx + dx, z = bz + dz;
                    if (ridge) w.set(x, y, z, roof);
                    else w.set(x, y, z, roofStairs.setValue(StairBlock.FACING, dx < W / 2 ? Direction.EAST : Direction.WEST));
                    if (dz == 0 || dz == D)
                        for (int yy = G + 7; yy < y; yy++) if (dx >= 0 && dx <= W) w.set(x, yy, z, planks);
                }
            }
            // inside: hay bales, a lantern, sometimes a cellar hatch
            for (int q = 0; q < 10; q++) {
                long hq = Hash.hash(h, q);
                int x = bx + 1 + Hash.range(hq, 3) + ((hq & 1) == 0 ? 0 : W - 5), z = bz + 2 + Hash.range(hq >>> 8, D - 3);
                int height = 1 + Hash.range(hq >>> 16, 3);
                for (int y = 0; y < height; y++) w.set(x, G + 1 + y, z, hay.setValue(RotatedPillarBlock.AXIS, (hq & 2) == 0 ? Direction.Axis.X : Direction.Axis.Z));
            }
            w.set(bx + W / 2, G + 1, bz + D / 2, ClassicsKit.facing(lantern, Direction.NORTH));
            if (Hash.unit(h >>> 20) < 0.45f) w.set(bx + W / 2, G, bz + D / 2 + 2, hatch);
            if (Hash.unit(h >>> 24) < 0.5f) w.set(bx + 2, G + 1, bz + D - 2, supply);
            w.set(bx + W - 2, G + 1, bz + 2, bottle);
            // the silo
            int sx = ox + 24, sz = oz + 10;
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > 11) continue;
                    w.set(sx + dx, G, sz + dz, siloWall);
                    if (d2 >= 6) for (int y = G + 1; y <= G + 15; y++) w.set(sx + dx, y, sz + dz, siloWall);
                    else for (int y = G + 1; y <= G + 15; y++) w.set(sx + dx, y, sz + dz, ModBlocks.AIR);
                    int dome = G + 16 + (d2 <= 2 ? 2 : d2 <= 6 ? 1 : 0);
                    for (int y = G + 16; y <= dome; y++) w.set(sx + dx, y, sz + dz, siloWall);
                }
            w.set(sx, G + 1, sz - 3, siloDoor);
            w.set(sx, G + 2, sz - 3, siloDoor);
            // a fence around the yard
            for (int u = 3; u <= 30; u++) {
                fencePost(w, ox + u, oz + 27, true);
            }
            for (int v = 3; v <= 27; v++) fencePost(w, ox + 31, oz + v, false);
        }

        void fencePost(ChunkWriter w, int x, int z, boolean alongX) {
            BlockState f = fence.setValue(alongX ? CrossCollisionBlock.EAST : CrossCollisionBlock.NORTH, true)
                    .setValue(alongX ? CrossCollisionBlock.WEST : CrossCollisionBlock.SOUTH, true);
            w.set(x, G + 1, z, f);
            w.set(x, G + 2, z, ModBlocks.AIR);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * The harvest. An engine starts far off; a line of falling wheat sweeps across
     * the field toward and past you. Anyone still standing in the crop when it
     * passes is cut down with it. Tracks, yards and barns are safe. Meanwhile the
     * scarecrows turn, always when nobody is looking, to face the nearest person.
     */
    static final class Harvest implements LevelBehavior {
        private long next = -1;
        private Sweep sweep;

        static final class Sweep {
            Vec3 origin;
            Direction dir;
            double pos;
            int ticks;
            final java.util.Set<java.util.UUID> hit = new java.util.HashSet<>();
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (next < 0) next = now + 20 * (90 + level.random.nextInt(90));
            if (sweep == null && now >= next && !level.players().isEmpty()) {
                ServerPlayer p = level.players().get(level.random.nextInt(level.players().size()));
                sweep = new Sweep();
                sweep.dir = Direction.from2DDataValue(level.random.nextInt(4));
                sweep.origin = p.position().subtract(sweep.dir.getStepX() * 56.0, 0, sweep.dir.getStepZ() * 56.0);
                sweep.pos = 0;
            }
            if (sweep != null) step(level);
        }

        private void step(ServerLevel level) {
            Sweep sw = sweep;
            sw.ticks++;
            sw.pos += 0.3;
            Direction d = sw.dir;
            Direction side = d.getClockWise();
            Vec3 front = sw.origin.add(d.getStepX() * sw.pos, 0, d.getStepZ() * sw.pos);
            Block lowerB = ModBlocks.get("wheat_lower"), upperB = ModBlocks.get("wheat_upper"), upperB2 = ModBlocks.get("wheat_upper_b");
            BlockState stubble = ModBlocks.state("wheat_stubble");
            BlockParticleOption chaff = new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.state("wheat_upper"));
            for (int a = -40; a <= 40; a++) {
                for (int band = -1; band <= 1; band++) {
                    BlockPos lo = BlockPos.containing(front.x + side.getStepX() * a + d.getStepX() * band, G + 1,
                            front.z + side.getStepZ() * a + d.getStepZ() * band);
                    if (!level.isLoaded(lo)) continue;
                    if (level.getBlockState(lo).is(lowerB)) {
                        level.setBlock(lo, stubble, Block.UPDATE_CLIENTS);
                        BlockState up = level.getBlockState(lo.above());
                        if (up.is(upperB) || up.is(upperB2)) level.setBlock(lo.above(), ModBlocks.AIR, Block.UPDATE_CLIENTS);
                        if (band == 0 && level.random.nextInt(3) == 0)
                            level.sendParticles(chaff, lo.getX() + 0.5, lo.getY() + 1.2, lo.getZ() + 0.5, 6, 0.4, 0.6, 0.4, 0.15);
                    }
                }
            }
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                Vec3 rel = p.position().subtract(front);
                double along = rel.x * d.getStepX() + rel.z * d.getStepZ();
                double across = rel.x * side.getStepX() + rel.z * side.getStepZ();
                if (sw.ticks % 50 == 1) {
                    BlockPos at = BlockPos.containing(front.x + side.getStepX() * Math.max(-40, Math.min(40, across)), G + 1,
                            front.z + side.getStepZ() * Math.max(-40, Math.min(40, across)));
                    ClassicsKit.sound(level, at, "event.level_10_harvester", 4.0f, 1f);
                }
                if (Math.abs(along) < 1.6 && Math.abs(across) < 40 && !sw.hit.contains(p.getUUID()) && inCrop(level, p)) {
                    sw.hit.add(p.getUUID());
                    if (!p.isCreative()) {
                        p.hurt(level.damageSources().generic(), 9f);
                        Sanity.add(p, -15f);
                    }
                    p.setDeltaMovement(d.getStepX() * 1.2, 0.5, d.getStepZ() * 1.2);
                    p.hurtMarked = true;
                    ClassicsKit.event(p, "pulse", 1f);
                }
            }
            if (sw.pos > 112) {
                sweep = null;
                next = level.getGameTime() + 20 * (150 + level.random.nextInt(120));
            }
        }

        private static boolean inCrop(ServerLevel level, ServerPlayer p) {
            BlockState below = level.getBlockState(p.blockPosition().below());
            return below.is(ModBlocks.get("crop_soil")) && p.getY() < G + 2.5;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 30 != 0 || p.isSpectator()) return;
            long s = Hash.hash(level.getSeed(), Hash.salt("level_10"));
            Block bodyB = ModBlocks.get("scarecrow_body"), headB = ModBlocks.get("scarecrow_head");
            for (BlockPos sc : scarecrows(s, p.getBlockX(), p.getBlockZ(), 48)) {
                if (!level.isLoaded(sc) || !level.getBlockState(sc.above()).is(headB)) continue;
                if (ClassicsKit.seenByAnyone(level, sc.above()) || sc.distSqr(p.blockPosition()) > 48 * 48) continue;
                Direction toward = ClassicsKit.dir(p.getBlockX() - sc.getX(), p.getBlockZ() - sc.getZ());
                Direction f = toward.getOpposite();
                BlockState hs = level.getBlockState(sc.above());
                if (hs.getValue(HorizontalDirectionalBlock.FACING) == f) continue;
                level.setBlock(sc.above(), hs.setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
                for (BlockPos b : new BlockPos[]{sc, sc.below()}) {
                    BlockState bs = level.getBlockState(b);
                    if (bs.is(bodyB)) level.setBlock(b, bs.setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
