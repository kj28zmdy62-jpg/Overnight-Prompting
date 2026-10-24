package com.noclip.levels;

import com.noclip.entity.MrReflection;
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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Mirror Level. An endless dressing-room gallery cut lengthways by a wall
 * of glass. Beyond it everything is mirrored: the vanities, the checkered
 * floor, the brass plaques with their backwards words, and you, a few steps
 * late. Your reflection lags further behind the longer you stay, and sometimes
 * it stops copying altogether and just looks at you.
 *
 * Here and there a door stands on the far side where your side has only wall,
 * with a green sign reading TIXE above it. You cannot reach it. It can. Stand
 * where it must stand to walk through, and it will, and you go with it.
 */
public final class MirrorLevel {
    static final int FLOOR = 8, CEIL = FLOOR + 6, BAND = 32, GLASS = 14, DEPTH = 12, ROOM = 13;

    private MirrorLevel() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("mirror_level")
                .number("").title("Mirror Level").order(4030)
                .tagline("Everything is mirrored. Your reflection is running late.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Reflections())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.06f).saturation(0.7f).contrast(1.08f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0016f).vignette(0.5f).fog(6f, 44f).fogColor(0x3A3E44).bloom(0.6f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(14, 30))
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "recursion", 1).exit(ExitType.DOOR, "photo_gallery", 1)
                .exit(ExitType.DOOR, "level_5", 1).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "recursion", 1)
                .build();
    }

    static int glassZ(double z) {
        return Math.floorDiv((int) Math.floor(z), BAND) * BAND + GLASS;
    }

    /** Door x of the room starting at x0, or Integer.MIN_VALUE if this room has none. */
    static int doorX(long s, int room) {
        long h = Hash.hash(s, room, 0xD002);
        if (Hash.unit(h) > 0.4f) return Integer.MIN_VALUE;
        return room * ROOM + ((h & 256) == 0 ? 2 : 11);
    }

    // ---------------------------------------------------------------- the reflection

    static final class Reflections implements LevelBehavior {
        static final int HIST = 64;

        static final class St {
            final double[][] hist = new double[HIST][5];
            int n;
            int entityId = -1;
            int age;
            int diverge;
            int exiting = -1;
            BlockPos door;
        }

        private final Map<UUID, St> state = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St old = state.remove(p.getUUID());
            if (old != null) {
                Entity e = level.getEntity(old.entityId);
                if (e != null) e.discard();
            }
            // arrive on your side of the glass, facing it
            int gz = glassZ(p.getZ());
            if (p.getZ() > gz) p.teleportTo(level, p.getX(), FLOOR + 1, gz - (p.getZ() - gz), 180f - p.getYRot(), 0f);
            p.teleportTo(level, p.getX(), FLOOR + 1, p.getZ(), 0f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St s = state.computeIfAbsent(p.getUUID(), u -> new St());
            s.age++;
            double[] now = s.hist[s.n % HIST];
            now[0] = p.getX();
            now[1] = p.getY();
            now[2] = p.getZ();
            now[3] = p.getYRot();
            now[4] = p.getXRot();
            s.n++;
            MrReflection r = level.getEntity(s.entityId) instanceof MrReflection m ? m : null;
            int gz = glassZ(p.getZ());
            if (r == null || r.distanceToSqr(p) > 40 * 40) {
                if (r != null) r.discard();
                r = MrReflection.TYPE.create(level);
                if (r == null) return;
                r.setOwner(p.getUUID().toString());
                r.moveTo(p.getX(), p.getY(), 2 * gz + 1 - p.getZ(), 180f - p.getYRot(), p.getXRot());
                level.addFreshEntity(r);
                s.entityId = r.getId();
            }
            if (s.exiting >= 0) {
                exiting(p, level, s, r);
                return;
            }
            if (s.diverge > 0) {
                diverging(p, level, s, r);
                return;
            }
            // the longer you stay, the further behind it falls
            int lag = Math.min(34, 5 + s.age / 300);
            int idx = Math.max(0, s.n - 1 - lag);
            double[] h = s.hist[idx % HIST];
            int hz = glassZ(h[2]);
            double rz = 2 * hz + 1 - h[2];
            float yaw = 180f - (float) h[3];
            r.absMoveTo(h[0], h[1], rz, yaw, (float) h[4]);
            r.setYHeadRot(yaw);
            r.yBodyRot = yaw;
            if (s.age > 1800 && s.age % 600 == 0 && p.getRandom().nextFloat() < 0.3f) s.diverge = 140;
            // it is standing in a door on its side of the glass
            if (s.age > 40) {
                int room = Math.floorDiv((int) Math.floor(r.getX()), ROOM);
                for (int rr = room - 1; rr <= room + 1; rr++) {
                    int dx = doorX(level.getSeed(), rr);
                    if (dx == Integer.MIN_VALUE) continue;
                    double fx = dx + 0.5, fz = hz + DEPTH - 0.5;
                    double ddx = r.getX() - fx, ddz = r.getZ() - fz;
                    if (ddx * ddx + ddz * ddz < 0.7 * 0.7 + 0.6) {
                        s.exiting = 0;
                        s.door = new BlockPos(dx, FLOOR + 1, hz + DEPTH);
                        level.setBlock(s.door, ModBlocks.state("mr_door_open"), Block.UPDATE_CLIENTS);
                        level.setBlock(s.door.above(), ModBlocks.state("mr_door_open"), Block.UPDATE_CLIENTS);
                        level.playSound(null, s.door, ModSounds.get("event.mirror_level_door"), SoundSource.BLOCKS, 1f, 1f);
                        return;
                    }
                }
            }
        }

        /** It stops copying you. It turns to look at you. It knocks. */
        void diverging(ServerPlayer p, ServerLevel level, St s, MrReflection r) {
            s.diverge--;
            float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - r.getX()), p.getZ() - r.getZ()));
            r.setYRot(yaw);
            r.setYHeadRot(yaw);
            r.yBodyRot = yaw;
            if (s.diverge == 100) {
                level.playSound(null, BlockPos.containing(r.getX(), r.getY() + 1, glassZ(p.getZ())),
                        ModSounds.get("event.mirror_level_knock"), SoundSource.HOSTILE, 1f, 1f);
                Sanity.add(p, -4f);
            }
        }

        /** It walks through its door, and takes you with it. */
        void exiting(ServerPlayer p, ServerLevel level, St s, MrReflection r) {
            s.exiting++;
            r.absMoveTo(s.door.getX() + 0.5, r.getY(), r.getZ() + 0.04, 0f, 0f);
            r.setYHeadRot(0f);
            r.yBodyRot = 0f;
            if (s.exiting == 50) {
                BlockPos d = s.door;
                state.remove(p.getUUID());
                r.discard();
                level.setBlock(d, ModBlocks.state("mr_door_low"), Block.UPDATE_CLIENTS);
                level.setBlock(d.above(), ModBlocks.state("mr_door_high"), Block.UPDATE_CLIENTS);
                Travel.exit(p, ExitType.DOOR, d);
            }
        }
    }

    // ---------------------------------------------------------------- the gallery

    static final class Gen implements LevelGenerator {
        final BlockState paper = ModBlocks.state("mr_wallpaper");
        final BlockState wain = ModBlocks.state("mr_wainscot");
        final BlockState floorA = ModBlocks.state("mr_floor_a");
        final BlockState floorB = ModBlocks.state("mr_floor_b");
        final BlockState ceiling = ModBlocks.state("mr_ceiling");
        final BlockState glass = ModBlocks.state("mr_glass").setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 9);

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, FLOOR, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int gz = Math.floorDiv(z, BAND) * BAND + GLASS;
                    int dz = gz - z;
                    int ad = Math.abs(dz);
                    if (ad > DEPTH) continue;
                    boolean mirror = dz < 0;
                    column(w, s, x, z, ad, mirror, gz);
                }
        }

        BlockState f(String n, Direction d) {
            return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void wallColumn(ChunkWriter w, int x, int z) {
            w.set(x, FLOOR + 1, z, wain);
            for (int y = FLOOR + 2; y < CEIL; y++) w.set(x, y, z, paper);
        }

        /** Everything is drawn from your side's description and placed twice, the second time reflected. */
        void column(ChunkWriter w, long s, int x, int z, int ad, boolean mirror, int gz) {
            int F = FLOOR;
            w.set(x, F, z, Math.floorMod(x / 2 + z / 2, 2) == 0 ? floorA : floorB);
            w.set(x, CEIL, z, ceiling);
            Direction back = mirror ? Direction.SOUTH : Direction.NORTH;
            if (ad == 0) {
                for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, glass);
                return;
            }
            if (ad == DEPTH) {
                wallColumn(w, x, z);
                int room = Math.floorDiv(x, ROOM);
                if (mirror && doorX(s, room) == x) {
                    w.set(x, F + 1, z, ModBlocks.state("mr_door_low"));
                    w.set(x, F + 2, z, ModBlocks.state("mr_door_high"));
                }
                return;
            }
            for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
            int room = Math.floorDiv(x, ROOM), u = x - room * ROOM;
            // partitions between dressing rooms, open near the glass
            if (u == 0 && ad >= 4) {
                wallColumn(w, x, z);
                return;
            }
            if (ad == DEPTH - 1) {
                if (u == 4 || u == 8) {
                    w.set(x, F + 1, z, f("mr_vanity", back));
                    w.set(x, F + 2, z, f("mr_vanity_mirror", back));
                }
                // the plaque: on the far side its words run backwards, and its halves trade places
                if (u == 6 || u == 7) {
                    boolean left = u == 6;
                    String half = mirror ? (left ? "_m_r" : "_m_l") : (left ? "_l" : "_r");
                    w.set(x, F + 3, z, f("mr_plaque" + half, back));
                }
                if (mirror && doorX(s, room) == x) w.set(x, F + 3, z, f("mr_tixe", back));
            }
            if (ad == DEPTH - 3 && (u == 4 || u == 8)) w.set(x, F + 1, z, f("mr_stool", back));
            if (ad == 6 && u == 6) w.set(x, F + 4, z, light);
        }
    }
}
