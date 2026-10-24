package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gravity Rooms. A tower of six bedrooms stacked one above another, and only
 * the first and last are the right way up. Each one in between is built
 * turned: onto its side, so the bed and the wardrobe stand out from a wall and
 * the door lies flat under your feet; or upside down, so you come in through
 * the carpet and the room pulls you back up against it before it lets you
 * fall to the lamp. You go down through hatches. In every turned room you fall
 * slowly, as if the room has not yet decided which way down is. At the bottom
 * is a door.
 */
public final class GravityRooms {
    static final int TOWER = 16, R = 10, STEP = 12, N = 6, BASE = 8;
    static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

    private GravityRooms() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("gravity_rooms")
                .number("").title("Gravity Rooms").order(4120)
                .tagline("Every room has its own idea of down. Go down anyway.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Falling())
                .visuals(new Visuals().tint(1.02f, 1.0f, 0.96f).saturation(0.85f).contrast(1.06f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0022f).vignette(0.55f).warp(0.07f).fog(6f, 40f).fogColor(0x20242C).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(BASE + 1, BASE + STEP * N)
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "level_9", 1).exit(ExitType.DOOR, "level_13", 1)
                .exit(ExitType.DOOR, "recursion", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "sky_rooms", 1)
                .build();
    }

    static int y0(int k) {
        return BASE + STEP * (N - 1 - k);
    }

    static int roomAt(double y) {
        int k = N - 1 - Math.floorDiv((int) Math.floor(y) - BASE, STEP);
        return Math.max(0, Math.min(N - 1, k));
    }

    static int orientation(long s, int tx, int tz, int k) {
        if (k == 0 || k == N - 1) return DOWN;
        int prev = orientation(s, tx, tz, k - 1);
        int o = 1 + Hash.range(s, tx, tz * 31 + k, 0x0E, 5);
        if (o == prev) o = 1 + (o % 5);
        return o;
    }

    /** The 2x2 hatch out of the bottom of room k, as offsets in the tower. */
    static int[] hatch(long s, int tx, int tz, int k) {
        return new int[]{2 + Hash.range(s, tx, tz * 17 + k, 0x4A, 6), 2 + Hash.range(s, tx, tz * 17 + k, 0x4B, 6)};
    }

    /** Local room coordinates (a across, b up from the room's own floor, c along) to tower offsets. */
    static int[] map(int o, int a, int b, int c) {
        return switch (o) {
            case UP -> new int[]{a, R - b, c};
            case NORTH -> new int[]{a, c, b};
            case SOUTH -> new int[]{a, c, R - b};
            case WEST -> new int[]{b, c, a};
            case EAST -> new int[]{R - b, c, a};
            default -> new int[]{a, b, c};
        };
    }

    // ---------------------------------------------------------------- behaviour

    static final class Falling implements LevelBehavior {
        private final Map<UUID, Integer> lastRoom = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            int tx = Math.round((float) (p.getX() / TOWER)) * TOWER, tz = Math.round((float) (p.getZ() / TOWER)) * TOWER;
            // stand clear of the hatch at first: let the player find it
            int[] h = hatch(level.getSeed(), tx, tz, 0);
            boolean blocked = h[0] <= 5 && h[0] + 1 >= 5 && h[1] <= 2 && h[1] + 1 >= 2;
            p.teleportTo(level, tx + 5.5, y0(0) + 1, tz + (blocked ? 9.5 : 2.5), blocked ? 180f : 0f, 10f);
            lastRoom.put(p.getUUID(), 0);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            int tx = Math.floorDiv(p.getBlockX(), TOWER) * TOWER, tz = Math.floorDiv(p.getBlockZ(), TOWER) * TOWER;
            int k = roomAt(p.getY());
            int o = orientation(level.getSeed(), tx, tz, k);
            Integer last = lastRoom.put(p.getUUID(), k);
            if (last != null && last != k) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.gravity_rooms_shift"), SoundSource.AMBIENT, 0.9f,
                        0.8f + 0.1f * k);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.25f));
                // an upside-down room pulls you back up against its carpet before it lets you go
                if (o == UP) p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 34, 1, false, false));
                if (k == N - 1 && p.getRandom().nextFloat() < 0.4f) waiting(p, level, tx, tz);
            }
            if (!p.onGround() && k > 0) {
                MobEffectInstance cur = p.getEffect(MobEffects.SLOW_FALLING);
                if (cur == null || cur.getDuration() < 10)
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            }
        }

        /** Somebody is standing in the bottom room. They were not there when you looked down the hatch. */
        void waiting(ServerPlayer p, ServerLevel level, int tx, int tz) {
            var type = ModEntities.lazy("mannequin").get();
            if (type == null) return;
            Mob m = type.create(level);
            if (m == null) return;
            m.moveTo(tx + 8.5, y0(N - 1) + 1, tz + 8.5, 135f, 0f);
            if (!level.noCollision(m)) return;
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(m);
        }
    }

    // ---------------------------------------------------------------- the tower

    static final class Gen implements LevelGenerator {
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState paper = ModBlocks.state("gv_wallpaper");
        final BlockState boards = ModBlocks.state("gv_boards");
        final BlockState carpet = ModBlocks.state("gv_carpet");
        final BlockState ceiling = ModBlocks.state("gv_ceiling");
        final BlockState light = ModBlocks.state("gv_ceiling_light");
        final BlockState wood = ModBlocks.state("gv_wood");
        final BlockState quilt = ModBlocks.state("gv_quilt");
        final BlockState mattress = ModBlocks.state("gv_mattress");
        final BlockState wardrobe = ModBlocks.state("gv_wardrobe");
        final BlockState clock = ModBlocks.state("gv_clock");
        final BlockState window = ModBlocks.state("gv_window");
        final BlockState shade = ModBlocks.state("gv_shade");
        final BlockState frame = ModBlocks.state("gv_frame");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, BASE + STEP * N + 2, w.maxZ, filler);
            int tx0 = Math.floorDiv(w.minX, TOWER) * TOWER, tz0 = Math.floorDiv(w.minZ, TOWER) * TOWER;
            for (int tx = tx0; tx <= w.maxX; tx += TOWER)
                for (int tz = tz0; tz <= w.maxZ; tz += TOWER)
                    for (int k = 0; k < N; k++) room(w, s, tx, tz, k);
        }

        void put(ChunkWriter w, int tx, int y, int tz, int o, int a, int b, int c, BlockState st) {
            int[] d = map(o, a, b, c);
            w.set(tx + d[0], y + d[1], tz + d[2], st);
        }

        void room(ChunkWriter w, long s, int tx, int tz, int k) {
            if (tx > w.maxX || tx + R < w.minX || tz > w.maxZ || tz + R < w.minZ) return;
            int o = orientation(s, tx, tz, k);
            int y = y0(k);
            // the shell, dressed according to the room's own idea of floor, wall and ceiling
            for (int a = 0; a <= R; a++)
                for (int b = 0; b <= R; b++)
                    for (int c = 0; c <= R; c++) {
                        boolean face = a == 0 || b == 0 || c == 0 || a == R || b == R || c == R;
                        BlockState st;
                        if (!face) st = ModBlocks.AIR;
                        else if (b == 0) st = (a >= 3 && a <= 7 && c >= 3 && c <= 7) ? carpet : boards;
                        else if (b == R) st = (a == 5 && c == 5) ? light : ceiling;
                        else st = paper;
                        put(w, tx, y, tz, o, a, b, c, st);
                    }
            // a clock on one wall, daylight through another
            put(w, tx, y, tz, o, 0, 5, 5, clock);
            for (int b = 3; b <= 5; b++)
                for (int c = 4; c <= 6; c++) put(w, tx, y, tz, o, R, b, c, window);
            // the bed
            for (int a = 1; a <= 3; a++) {
                for (int b = 1; b <= 3; b++) put(w, tx, y, tz, o, a, b, 1, wood);
                for (int c = 2; c <= 5; c++) {
                    put(w, tx, y, tz, o, a, 1, c, wood);
                    put(w, tx, y, tz, o, a, 2, c, c == 2 ? mattress : quilt);
                }
            }
            // table and chair
            for (int[] leg : new int[][]{{6, 6}, {8, 6}, {6, 8}, {8, 8}})
                for (int b = 1; b <= 2; b++) put(w, tx, y, tz, o, leg[0], b, leg[1], wood);
            for (int a = 6; a <= 8; a++)
                for (int c = 6; c <= 8; c++) put(w, tx, y, tz, o, a, 3, c, wood);
            put(w, tx, y, tz, o, 4, 1, 7, wood);
            for (int b = 1; b <= 3; b++) put(w, tx, y, tz, o, 3, b, 7, wood);
            // wardrobe and lamp
            for (int a = 8; a <= 9; a++)
                for (int c = 1; c <= 2; c++)
                    for (int b = 1; b <= 4; b++) put(w, tx, y, tz, o, a, b, c, wardrobe);
            put(w, tx, y, tz, o, 9, 1, 9, wood);
            put(w, tx, y, tz, o, 9, 2, 9, wood);
            put(w, tx, y, tz, o, 9, 3, 9, shade);

            // hatches: out through the bottom of this room, in through the top (the hatch of the room above)
            if (k < N - 1) {
                int[] h = hatch(s, tx, tz, k);
                hole(w, tx + h[0], y, tz + h[1], true);
                // a clear way down to it
                w.fill(tx + h[0], y + 1, tz + h[1], tx + h[0] + 1, y + 4, tz + h[1] + 1, ModBlocks.AIR);
            }
            if (k > 0) {
                int[] h = hatch(s, tx, tz, k - 1);
                hole(w, tx + h[0], y + R, tz + h[1], false);
                w.fill(tx + h[0], y + R - 4, tz + h[1], tx + h[0] + 1, y + R - 1, tz + h[1] + 1, ModBlocks.AIR);
            }
            if (k == N - 1) {
                // the bottom room is the right way up, and it has a door
                for (int yy = y + 1; yy <= y + 3; yy++)
                    for (int z = tz + 4; z <= tz + 6; z++) w.set(tx + R, yy, z, frame);
                w.set(tx + R, y + 1, tz + 5, ModBlocks.state("gv_door_low"));
                w.set(tx + R, y + 2, tz + 5, ModBlocks.state("gv_door_high"));
                w.fill(tx + R - 1, y + 1, tz + 4, tx + R - 1, y + 3, tz + 6, ModBlocks.AIR);
            }
        }

        /** A 2x2 hole through a floor slab, framed like a hatch; bottom=true also cuts the slab below. */
        void hole(ChunkWriter w, int x, int y, int z, boolean bottom) {
            for (int dx = -1; dx <= 2; dx++)
                for (int dz = -1; dz <= 2; dz++) {
                    boolean in = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
                    w.set(x + dx, y, z + dz, in ? ModBlocks.AIR : frame);
                    if (bottom) w.set(x + dx, y - 1, z + dz, in ? ModBlocks.AIR : frame);
                }
        }
    }
}
