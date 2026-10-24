package com.noclip.levels;

import com.noclip.block.RestroomStallBlock;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endless Restrooms. Bay after bay of stalls facing a row of sinks and mirrors, joined
 * by gaps in the tiling. In every bay one stall is locked, with a pair of shoes
 * showing under the door. Knock and something knocks back. Knock a third time and
 * the lock slides open on an empty stall, and the water in the bowl is black and
 * very deep. The hand dryers start by themselves; the mirrors show someone standing
 * behind you, until you look properly.
 */
public final class Restrooms {
    public static final String ID = "restrooms";
    static final int FLOOR = 10, CEIL = 15, P = 12, SEG = 24;

    private Restrooms() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Endless Restrooms").order(1190)
                .tagline("Occupied. It is always occupied.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Plumbing())
                .visuals(new Visuals().tint(0.95f, 1.03f, 0.99f).saturation(0.7f).contrast(1.08f).brightness(1.12f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.5f).bloom(0.5f).fog(4f, 40f).fogColor(0xC8D0C8))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 1, 0.06f).dist(18, 34).weight(1))
                .exit(ExitType.NOCLIP, "locker_rooms", 2).exit(ExitType.NOCLIP, "level_37", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.WATER, "level_37", 3).exit(ExitType.WATER, "sewers", 2).exit(ExitType.WATER, "water_park", 1)
                .exit(ExitType.WATER, "flooded_lobby", 1)
                .exit(ExitType.FALL, "sewers", 2).exit(ExitType.FALL, "storm_drains", 1)
                .build();
    }

    static int lockedStall(long s, int seg, int bay) {
        return Hash.range(s, seg, bay, 0x10C, 7);
    }

    static final class Gen implements LevelGenerator {
        final BlockState tile = ModBlocks.state("rest_tile");
        final BlockState band = ModBlocks.state("rest_tile_band");
        final BlockState base = ModBlocks.state("rest_tile_base");
        final BlockState floor = ModBlocks.state("rest_floor");
        final BlockState ceiling = ModBlocks.state("rest_ceiling");
        final BlockState light = ModBlocks.state("rest_light");
        final BlockState bad = ModBlocks.state("rest_light_bad");
        final BlockState dead = ModBlocks.state("rest_light_dead");
        final BlockState part = ModBlocks.state("rest_partition");
        final BlockState partTop = ModBlocks.state("rest_partition_top");
        final BlockState door = ModBlocks.state("rest_stall_door");
        final BlockState locked = ModBlocks.state("rest_stall_locked");
        final BlockState toilet = ModBlocks.state("rest_toilet");
        final BlockState dark = ModBlocks.state("rest_toilet_dark");
        final BlockState sink = ModBlocks.state("rest_sink");
        final BlockState mirror = ModBlocks.state("rest_mirror");
        final BlockState dryer = ModBlocks.state("rest_dryer");
        final BlockState shoes = ModBlocks.state("rest_shoes");
        final BlockState paper = ModBlocks.state("rest_paper");
        final BlockState drain = ModBlocks.state("rest_drain");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, SEG), lz = Math.floorMod(z, P);
                    int seg = Math.floorDiv(x, SEG), bay = Math.floorDiv(z, P);
                    boolean passage = lx >= 22;
                    boolean wall = !passage && (lz == 0 || lz == 10 || lz == 11);
                    if (wall) {
                        w.set(x, FLOOR + 1, z, base);
                        w.set(x, FLOOR + 2, z, tile);
                        w.set(x, FLOOR + 3, z, band);
                        w.set(x, FLOOR + 4, z, tile);
                        continue;
                    }
                    boolean dim = Hash.chance(s, seg, bay, 0xD1, 0.15f);
                    if (lz >= 5 && lz <= 8 && (lz == 6 || lz == 7) && lx % 3 == 1) {
                        float r = Hash.unit(s, x, z, 0x1E);
                        w.set(x, CEIL, z, dim ? (r < 0.3f ? bad : dead) : (r < 0.06f ? dead : r < 0.16f ? bad : light));
                    }
                    if (lz >= 5 && lz <= 8 && Hash.chance(s, x, z, 0xDA1, 0.003f)) w.set(x, FLOOR, z, drain);
                    if (lz >= 5 && lz <= 8 && Hash.chance(s, x, z, 0x9A9, 0.03f)) w.set(x, FLOOR + 1, z, paper);
                    if (lz >= 5 && lz <= 8 && Hash.chance(s, x, z, 0x9AA, 0.02f)) w.set(x, FLOOR + 1, z, puddle);
                }
            int i0 = Math.floorDiv(w.minX, SEG), i1 = Math.floorDiv(w.maxX, SEG);
            int k0 = Math.floorDiv(w.minZ, P), k1 = Math.floorDiv(w.maxZ, P);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) bay(w, s, i, k, i * SEG, k * P);
        }

        BlockState doorState(DoubleBlockHalf half, boolean open, boolean hingeRight) {
            return door.setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, half).setValue(DoorBlock.OPEN, open)
                    .setValue(DoorBlock.HINGE, hingeRight ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT
                            : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
        }

        void bay(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int y = FLOOR + 1;
            int lockedIdx = lockedStall(s, i, k);
            BlockState zPart = part.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
            BlockState zPartTop = partTop.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
            for (int st = 0; st < 7; st++) {
                int a = x0 + st * 3;
                for (int lz = 1; lz <= 3; lz++) {
                    w.set(a, y, z0 + lz, zPart);
                    w.set(a, y + 1, z0 + lz, zPartTop);
                    if (st == 6) {
                        w.set(a + 3, y, z0 + lz, zPart);
                        w.set(a + 3, y + 1, z0 + lz, zPartTop);
                    }
                }
                w.set(a, y, z0 + 4, part);
                w.set(a, y + 1, z0 + 4, partTop);
                w.set(a + 2, y, z0 + 4, part);
                w.set(a + 2, y + 1, z0 + 4, partTop);
                if (st == 6) {
                    w.set(a + 3, y, z0 + 4, part);
                    w.set(a + 3, y + 1, z0 + 4, partTop);
                }
                long h = Hash.hash(s, a, z0, 0x57A);
                if (st == lockedIdx) {
                    w.set(a + 1, y, z0 + 4, locked.setValue(RestroomStallBlock.HALF, DoubleBlockHalf.LOWER));
                    w.set(a + 1, y + 1, z0 + 4, locked.setValue(RestroomStallBlock.HALF, DoubleBlockHalf.UPPER));
                    w.set(a + 1, y, z0 + 1, dark);
                    w.set(a + 1, y, z0 + 2, shoes.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                } else {
                    boolean open = Hash.unit(h) < 0.45f;
                    w.set(a + 1, y, z0 + 4, doorState(DoubleBlockHalf.LOWER, open, (h & 2) != 0));
                    w.set(a + 1, y + 1, z0 + 4, doorState(DoubleBlockHalf.UPPER, open, (h & 2) != 0));
                    w.set(a + 1 + (int) ((h >>> 4) & 1), y, z0 + 1, toilet);
                    if (Hash.unit(h >>> 8) < 0.15f) w.set(a + 2 - (int) ((h >>> 4) & 1), y, z0 + 2, paper);
                }
            }
            // sinks, mirrors and dryers along the far wall
            for (int lx = 0; lx < 22; lx++) {
                int x = x0 + lx;
                if (lx % 8 == 7) {
                    w.set(x, y + 1, z0 + 9, dryer.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    continue;
                }
                w.set(x, y, z0 + 9, sink.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                w.set(x, y + 1, z0 + 9, mirror.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                w.set(x, y + 2, z0 + 9, mirror.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final Map<Long, Integer> KNOCKS = new HashMap<>();
    static final Map<UUID, Long> NEXT_DRYER = new HashMap<>();
    static final Map<UUID, Long> NEXT_MIRROR = new HashMap<>();
    static final Map<UUID, BlockPos> HAUNTED = new HashMap<>();
    static final Map<UUID, Integer> STARE = new HashMap<>();
    static final List<long[]> PENDING = new ArrayList<>();   // {pos, at, stage}

    public static void onKnock(ServerPlayer p, BlockPos lower) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, lower, ModSounds.get("event.restrooms_knock"), SoundSource.BLOCKS, 1f, 1f);
        int n = KNOCKS.merge(lower.asLong(), 1, Integer::sum);
        long now = level.getGameTime();
        if (n <= 2) PENDING.add(new long[]{lower.asLong(), now + 25 + n * 15, n});
        else if (n == 3) PENDING.add(new long[]{lower.asLong(), now + 70, 3});
    }

    static void resolve(ServerLevel level, BlockPos lower, int stage) {
        if (stage < 3) {
            level.playSound(null, lower.north(2), ModSounds.get("event.restrooms_knock_back"), SoundSource.BLOCKS, 1f, stage == 2 ? 0.8f : 1f);
            return;
        }
        if (!(level.getBlockState(lower).getBlock() instanceof RestroomStallBlock)) return;
        level.playSound(null, lower, ModSounds.get("event.restrooms_unlock"), SoundSource.BLOCKS, 1f, 1f);
        level.setBlock(lower.above(), ModBlocks.AIR, Block.UPDATE_ALL);
        level.setBlock(lower, ModBlocks.AIR, Block.UPDATE_ALL);
        BlockPos shoes = lower.north(2);
        if (level.getBlockState(shoes).is(ModBlocks.get("rest_shoes"))) level.setBlock(shoes, ModBlocks.AIR, Block.UPDATE_ALL);
        level.playSound(null, lower.north(3), ModSounds.get("event.restrooms_flush"), SoundSource.BLOCKS, 0.8f, 0.7f);
    }

    static final class Plumbing implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            NEXT_DRYER.put(p.getUUID(), level.getGameTime() + 20 * 15);
            NEXT_MIRROR.put(p.getUUID(), level.getGameTime() + 20 * 40);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            Long nd = NEXT_DRYER.get(p.getUUID());
            if (nd != null && now >= nd) {
                NEXT_DRYER.put(p.getUUID(), now + 20 * (18 + p.getRandom().nextInt(30)));
                BlockPos d = find(p, level, "rest_dryer", 14, FLOOR + 2);
                if (d != null) {
                    level.playSound(null, d, ModSounds.get("event.restrooms_dryer"), SoundSource.BLOCKS, 1.3f, 0.95f + p.getRandom().nextFloat() * 0.1f);
                    level.sendParticles(ParticleTypes.CLOUD, d.getX() + 0.5, d.getY() + 0.1, d.getZ() + 0.5, 12, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // a mirror shows someone behind you; look properly and they are gone
            BlockPos h = HAUNTED.get(p.getUUID());
            if (h != null) {
                if (!level.getBlockState(h).is(ModBlocks.get("rest_mirror_fig"))) {
                    HAUNTED.remove(p.getUUID());
                } else if (EmptyBuildingKit.seen(p, Vec3.atCenterOf(h), 0.9)) {
                    int st = STARE.merge(p.getUUID(), 1, Integer::sum);
                    if (st > 12) {
                        BlockState ms = level.getBlockState(h);
                        level.setBlock(h, ModBlocks.state("rest_mirror").setValue(HorizontalDirectionalBlock.FACING,
                                ms.getValue(HorizontalDirectionalBlock.FACING)), Block.UPDATE_CLIENTS);
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.sting"), SoundSource.AMBIENT, 0.6f, 1.2f);
                        Sanity.add(p, -4f);
                        HAUNTED.remove(p.getUUID());
                        STARE.remove(p.getUUID());
                    }
                }
            } else {
                Long nm = NEXT_MIRROR.get(p.getUUID());
                if (nm != null && now >= nm) {
                    NEXT_MIRROR.put(p.getUUID(), now + 20 * (50 + p.getRandom().nextInt(60)));
                    BlockPos m = find(p, level, "rest_mirror", 10, FLOOR + 2);
                    if (m != null && !EmptyBuildingKit.seen(p, Vec3.atCenterOf(m), 0.2)) {
                        BlockState ms = level.getBlockState(m);
                        level.setBlock(m, ModBlocks.state("rest_mirror_fig").setValue(HorizontalDirectionalBlock.FACING,
                                ms.getValue(HorizontalDirectionalBlock.FACING)), Block.UPDATE_CLIENTS);
                        HAUNTED.put(p.getUUID(), m);
                        STARE.put(p.getUUID(), 0);
                    }
                }
            }
        }

        BlockPos find(ServerPlayer p, ServerLevel level, String block, int r, int y) {
            List<BlockPos> found = new ArrayList<>();
            BlockPos c = p.blockPosition();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos bp = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
                    if (level.getBlockState(bp).is(ModBlocks.get(block))) found.add(bp);
                }
            return found.isEmpty() ? null : found.get(p.getRandom().nextInt(found.size()));
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            for (int i = PENDING.size() - 1; i >= 0; i--) {
                long[] e = PENDING.get(i);
                if (now < e[1]) continue;
                PENDING.remove(i);
                resolve(level, BlockPos.of(e[0]), (int) e[2]);
            }
        }
    }
}
