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
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Missing Textures. The world before anybody finished it: a plain of magenta
 * and black, fragments hanging in the air, ERROR plates standing in the field
 * like gravestones, now and then a scrap of a real world (grass, planks,
 * stone) that loaded by mistake. Some floor tiles never loaded at all: their
 * squares sit half a square out of step with their neighbours, and they will
 * not hold you. Stand still too long and the ground around you unloads too.
 * Somewhere stands one finished thing: a painted door in a white frame.
 */
public final class MissingTextures {
    static final int FLOOR = 20, REGION = 80;

    private MissingTextures() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("missing_textures")
                .number("").title("Missing Textures").order(4040)
                .tagline("Nothing here finished loading. Keep moving, or it forgets you too.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Unloading())
                .visuals(new Visuals().tint(1f, 0.96f, 1.02f).saturation(1.05f).contrast(1.05f).brightness(1.05f)
                        .grain(0.05f).aberration(0.003f).vignette(0.45f).scanlines(0.06f).fog(10f, 64f).fogColor(0x000000)
                        .bloom(0.35f))
                .sanityDrain(0.04f)
                .spawnY(FLOOR + 1, FLOOR + 7)
                .spawn(SpawnRule.of(ModEntities.lazy("mt_null"), 2, 0.3f).dist(16, 32))
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "level_11", 1).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.FALL, "glitch_level", 2).exit(ExitType.FALL, "loading_screen", 1).exit(ExitType.FALL, "the_void", 1)
                .exit(ExitType.NOCLIP, "glitch_level", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .build();
    }

    static int[] doorAt(long s, int rx, int rz) {
        return new int[]{rx * REGION + 12 + Hash.range(s, rx, rz, 0xD0, REGION - 24), rz * REGION + 12 + Hash.range(s, rx, rz, 0xD1, REGION - 24)};
    }

    static boolean nearDoor(long s, int x, int z) {
        int rx = Math.floorDiv(x, REGION), rz = Math.floorDiv(z, REGION);
        int[] d = doorAt(s, rx, rz);
        return Math.abs(x - d[0]) <= 4 && Math.abs(z - d[1]) <= 4;
    }

    // ---------------------------------------------------------------- behaviour

    static final class Unloading implements LevelBehavior {
        static final class St {
            double ax, az;
            int still;
        }

        record Hole(BlockPos pos, BlockState was, long until) {}

        private final Map<UUID, St> state = new ConcurrentHashMap<>();
        private final Deque<Hole> holes = new ArrayDeque<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St s = state.computeIfAbsent(p.getUUID(), u -> new St());
            double dx = p.getX() - s.ax, dz = p.getZ() - s.az;
            if (dx * dx + dz * dz > 2.5 * 2.5 || !p.onGround() && p.getDeltaMovement().y < -0.3) {
                s.ax = p.getX();
                s.az = p.getZ();
                s.still = 0;
            } else {
                s.still++;
            }
            if (p.isSpectator() || p.isCreative()) return;
            if (s.still == 160) {
                // warning: the ground under you starts to come apart
                BlockPos under = p.blockPosition().below();
                if (level.getBlockState(under).is(ModBlocks.get("mt_missing")))
                    level.setBlock(under, ModBlocks.state("mt_missing_dirty"), Block.UPDATE_CLIENTS);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.4f));
            }
            if (s.still >= 240 && s.still % 8 == 0) {
                if (s.still == 240) level.playSound(null, p.blockPosition(), ModSounds.get("event.missing_textures_unload"), SoundSource.AMBIENT, 1f, 1f);
                int r = Math.min(5, (s.still - 240) / 16);
                BlockPos c = p.blockPosition().below();
                for (int ox = -r; ox <= r; ox++)
                    for (int oz = -r; oz <= r; oz++) {
                        if (ox * ox + oz * oz > r * r + 1) continue;
                        BlockPos b = c.offset(ox, 0, oz);
                        BlockState was = level.getBlockState(b);
                        if (!was.is(ModBlocks.get("mt_missing")) && !was.is(ModBlocks.get("mt_missing_dirty"))) continue;
                        level.setBlock(b, ModBlocks.state("mt_unloaded"), Block.UPDATE_CLIENTS);
                        synchronized (holes) {
                            holes.add(new Hole(b, was, level.getGameTime() + 20 * 40));
                        }
                    }
            }
            // geometry pops in and out where you are not looking
            if (p.tickCount % 60 == 0) popFragment(p, level);
        }

        void popFragment(ServerPlayer p, ServerLevel level) {
            for (int attempt = 0; attempt < 6; attempt++) {
                BlockPos b = BlockPos.containing(p.getX() + p.getRandom().nextInt(41) - 20, FLOOR + 5 + p.getRandom().nextInt(14),
                        p.getZ() + p.getRandom().nextInt(41) - 20);
                if (seen(p, b)) continue;
                BlockState st = level.getBlockState(b);
                if (st.isAir()) {
                    level.setBlock(b, p.getRandom().nextInt(4) == 0 ? ModBlocks.state("mt_question") : ModBlocks.state("mt_wire"), Block.UPDATE_ALL);
                    return;
                }
                if (st.is(ModBlocks.get("mt_wire")) || st.is(ModBlocks.get("mt_question")) || st.is(ModBlocks.get("mt_missing"))) {
                    level.setBlock(b, ModBlocks.AIR, Block.UPDATE_ALL);
                    return;
                }
            }
        }

        static boolean seen(ServerPlayer p, BlockPos pos) {
            Vec3 c = Vec3.atCenterOf(pos);
            Vec3 to = c.subtract(p.getEyePosition()).normalize();
            if (p.getViewVector(1f).dot(to) < 0.3) return false;
            return p.level().clip(new ClipContext(p.getEyePosition(), c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))
                    .getType() == HitResult.Type.MISS;
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            synchronized (holes) {
                int guard = 64;
                while (!holes.isEmpty() && holes.peekFirst().until() <= now && guard-- > 0) {
                    Hole h = holes.pollFirst();
                    if (!level.isLoaded(h.pos())) continue;
                    if (level.getNearestPlayer(h.pos().getX(), h.pos().getY(), h.pos().getZ(), 5, false) != null) {
                        holes.add(new Hole(h.pos(), h.was(), now + 100));
                        continue;
                    }
                    if (level.getBlockState(h.pos()).is(ModBlocks.get("mt_unloaded"))) level.setBlock(h.pos(), h.was(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the unfinished world

    static final class Gen implements LevelGenerator {
        final BlockState miss = ModBlocks.state("mt_missing");
        final BlockState dirty = ModBlocks.state("mt_missing_dirty");
        final BlockState unloaded = ModBlocks.state("mt_unloaded");
        final BlockState question = ModBlocks.state("mt_question");
        final BlockState wire = ModBlocks.state("mt_wire");
        final BlockState frame = ModBlocks.state("mt_frame");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState[] real = {Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Blocks.BRICKS.defaultBlockState(), Blocks.DIRT.defaultBlockState()};

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(1, pit);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean fake = Hash.noise(s ^ 0xFA4E, x / 3.0, z / 3.0) > 0.78f && Hash.chance(s, x, z, 0xFB, 0.7f)
                            && !nearDoor(s, x, z);
                    float hill = Hash.fbm(s ^ 0x4111, x / 22.0, z / 22.0, 3);
                    int h = hill > 0.56f && !nearDoor(s, x, z) ? (int) ((hill - 0.56f) * 30) : 0;
                    w.set(x, FLOOR, z, fake ? unloaded : Hash.chance(s, x, z, 0xD1, 0.04f) ? dirty : miss);
                    for (int y = 1; y <= h; y++) w.set(x, FLOOR + y, z, Hash.chance(s, x, y * 7 + z, 0xD2, 0.02f) ? question : miss);
                    // the terrain was meant to have stepped edges; some steps never came
                    if (h > 0 && Hash.chance(s, x, z, 0xD3, 0.1f)) w.set(x, FLOOR + h, z, wire);
                }
            CellGrid g = new CellGrid(s, 16, 4);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, s, g, i, k, x0, z0));
            int rx = Math.floorDiv(w.minX, REGION), rz = Math.floorDiv(w.minZ, REGION);
            for (int a = rx - 1; a <= rx + 1; a++)
                for (int b = rz - 1; b <= rz + 1; b++) door(w, doorAt(s, a, b));
        }

        void cell(ChunkWriter w, long s, CellGrid g, int i, int k, int x0, int z0) {
            long h = g.hash(i, k, 0xF4A6);
            float r = Hash.unit(h);
            int cx = x0 + 3 + Hash.range(h >>> 8, 10), cz = z0 + 3 + Hash.range(h >>> 16, 10);
            if (nearDoor(s, cx, cz)) return;
            if (r < 0.32f) {
                // a fragment hanging in the air
                int sx = 2 + Hash.range(h >>> 24, 4), sy = 1 + Hash.range(h >>> 28, 3), sz = 2 + Hash.range(h >>> 32, 4);
                int y0 = FLOOR + 6 + Hash.range(h >>> 36, 13);
                boolean isReal = Hash.unit(h >>> 40) < 0.25f;
                BlockState realB = real[Hash.range(h >>> 44, real.length)];
                for (int dx = 0; dx < sx; dx++)
                    for (int dy = 0; dy < sy; dy++)
                        for (int dz = 0; dz < sz; dz++) {
                            if (Hash.chance(s, cx + dx, y0 + dy, cz + dz, 0.15f)) continue;
                            BlockState b = isReal ? realB : (Hash.chance(s, cx + dx * 3, dy, cz + dz, 0.2f) ? wire : miss);
                            w.set(cx + dx, y0 + dy, cz + dz, b);
                        }
            } else if (r < 0.40f) {
                Direction d = Direction.from2DDataValue(Hash.range(h >>> 24, 4));
                w.set(cx, FLOOR + 1, cz, ModBlocks.state("mt_error_sign").setValue(HorizontalDirectionalBlock.FACING, d));
            } else if (r < 0.46f) {
                w.set(cx, FLOOR + 3 + Hash.range(h >>> 24, 6), cz, question);
            } else if (r < 0.50f) {
                // a scrap of somewhere real, lying on the ground
                BlockState realB = real[Hash.range(h >>> 24, real.length)];
                for (int dx = 0; dx < 2; dx++)
                    for (int dz = 0; dz < 2; dz++) w.set(cx + dx, FLOOR, cz + dz, realB);
            }
        }

        /** The one finished thing: a white-framed door with light round its edges. */
        void door(ChunkWriter w, int[] d) {
            int x = d[0], z = d[1];
            for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                w.set(x - 1, y, z, frame);
                w.set(x + 1, y, z, frame);
            }
            w.set(x, FLOOR + 3, z, frame);
            w.set(x, FLOOR + 1, z, ModBlocks.state("mt_door_low"));
            w.set(x, FLOOR + 2, z, ModBlocks.state("mt_door_high"));
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -1; dz <= 1; dz++) w.set(x + dx, FLOOR, z + dz, Blocks.OAK_PLANKS.defaultBlockState());
        }
    }
}
