package com.noclip.levels;

import com.noclip.entity.RedGuest;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Red Room. Nine blocks square, deep red, and a tone that never stops.
 * Every time something leaves your sight it may be different when you look
 * back: the chair has turned to face you, the woman in the portrait has turned
 * away, the lamp is in another corner. The door does the same: look away from
 * it and it is on another wall. It only opens for someone who holds their eyes
 * on it, and while you stare at it, the Guest walks up behind you.
 */
public final class RedRoom {
    static final int FLOOR = 8, CEIL = FLOOR + 5, SPACING = 32, SIZE = 10;
    static final int AJAR = 40, OPEN = 110;
    /** Door slots (u, v) on the north, east, south, west walls. */
    static final int[][] DOORS = {{5, 0}, {SIZE, 5}, {5, SIZE}, {0, 5}};
    static final int[][] LAMP_SPOTS = {{8, 8}, {2, 2}, {8, 2}, {2, 6}};
    static final int[] CHAIR = {6, 3}, TABLE = {2, 8}, PORTRAIT = {1, 2};

    private RedRoom() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("red_room")
                .number("").title("The Red Room").order(4090)
                .tagline("It changes when you look away. So does the door.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Watching())
                .visuals(new Visuals().tint(1.22f, 0.86f, 0.84f).saturation(1.15f).contrast(1.14f).brightness(1.12f)
                        .grain(0.1f).aberration(0.0022f).vignette(0.8f).fog(3f, 22f).fogColor(0x1A0204).bloom(0.75f)
                        .flicker(0.03f))
                .sanityDrain(0.07f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "level_run", 1).exit(ExitType.DOOR, "recursion", 1)
                .exit(ExitType.DOOR, "room_of_doors", 1).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.NOCLIP, "level_run", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .build();
    }

    // ---------------------------------------------------------------- state

    static final class Room {
        int ox, oz;
        int doorWall;
        int stare;
        boolean open;
        boolean doorSeen;
        int portrait;
        int lamp;
        Direction chair = Direction.WEST;
        boolean phoneOff;
        boolean[] seen = new boolean[4];
        int changes;
        int caughtAt = -1000;
    }

    static final Map<UUID, Room> ROOMS = new ConcurrentHashMap<>();

    /** True while someone in this room is holding the door open with their eyes. */
    public static boolean doorOpening(UUID player) {
        Room r = ROOMS.get(player);
        return r != null && r.stare >= AJAR;
    }

    static BlockPos at(Room r, int u, int y, int v) {
        return new BlockPos(r.ox + u, y, r.oz + v);
    }

    static BlockState f(String n, Direction d) {
        return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
    }

    static void placeDoor(ServerLevel level, Room r, String state) {
        int[] d = DOORS[r.doorWall];
        level.setBlock(at(r, d[0], FLOOR + 1, d[1]), ModBlocks.state("red_door_" + state + "_low"), Block.UPDATE_ALL);
        level.setBlock(at(r, d[0], FLOOR + 2, d[1]), ModBlocks.state("red_door_" + state + "_high"), Block.UPDATE_ALL);
    }

    static void clearDoor(ServerLevel level, Room r) {
        int[] d = DOORS[r.doorWall];
        level.setBlock(at(r, d[0], FLOOR + 1, d[1]), ModBlocks.state("red_baseboard"), Block.UPDATE_ALL);
        level.setBlock(at(r, d[0], FLOOR + 2, d[1]), ModBlocks.state("red_damask"), Block.UPDATE_ALL);
    }

    static boolean seen(ServerPlayer p, Vec3 point, BlockPos target, double cone) {
        Vec3 eye = p.getEyePosition();
        Vec3 to = point.subtract(eye);
        if (to.lengthSqr() < 0.01) return true;
        if (p.getViewVector(1f).dot(to.normalize()) < cone) return false;
        BlockHitResult hit = p.level().clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target)
                || hit.getLocation().distanceToSqr(point) < 0.8;
    }

    static Vec3 doorPoint(Room r, int wall) {
        int[] d = DOORS[wall];
        double x = r.ox + d[0] + 0.5, z = r.oz + d[1] + 0.5;
        // the face that looks into the room
        if (wall == 0) z += 0.5;
        else if (wall == 1) x -= 0.5;
        else if (wall == 2) z -= 0.5;
        else x += 0.5;
        return new Vec3(x, FLOOR + 2.0, z);
    }

    /** Put the door on a wall the player is not looking at. */
    static void relocateDoor(ServerPlayer p, ServerLevel level, Room r) {
        clearDoor(level, r);
        int best = (r.doorWall + 2) % 4;
        double bestDot = 2;
        for (int k = 0; k < 4; k++) {
            int w = (r.doorWall + 1 + k) % 4;
            if (w == r.doorWall) continue;
            Vec3 to = doorPoint(r, w).subtract(p.getEyePosition()).normalize();
            double dot = p.getViewVector(1f).dot(to) + p.getRandom().nextDouble() * 0.2;
            if (dot < bestDot) {
                bestDot = dot;
                best = w;
            }
        }
        r.doorWall = best;
        r.stare = 0;
        r.doorSeen = false;
        placeDoor(level, r, "closed");
    }

    /** The Guest reached you. Everything starts over. */
    public static void caught(ServerPlayer p, RedGuest g) {
        Room r = ROOMS.get(p.getUUID());
        ServerLevel level = p.serverLevel();
        if (r == null || p.tickCount - r.caughtAt < 60) return;
        r.caughtAt = p.tickCount;
        level.playSound(null, p.blockPosition(), ModSounds.get("event.red_room_caught"), SoundSource.HOSTILE, 1.2f, 1f);
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 1f));
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.8f));
        Sanity.add(p, -12f);
        p.hurt(level.damageSources().magic(), 3f);
        if (!r.open) relocateDoor(p, level, r);
        placeGuest(level, p, r, g);
    }

    /** The far corner from wherever you are standing. */
    static void placeGuest(ServerLevel level, ServerPlayer p, Room r, RedGuest g) {
        double px = p.getX() - r.ox, pz = p.getZ() - r.oz;
        double gx = r.ox + (px < 5.5 ? 8.5 : 1.5), gz = r.oz + (pz < 5.5 ? 8.5 : 1.5);
        g.moveTo(gx, FLOOR + 1, gz, 0f, 0f);
        if (!level.noCollision(g)) g.moveTo(r.ox + 5.5, FLOOR + 1, r.oz + (pz < 5.5 ? 8.5 : 1.5), 0f, 0f);
        g.lookAtPlayer(p);
    }

    // ---------------------------------------------------------------- behaviour

    static final class Watching implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Room r = new Room();
            r.ox = Math.floorDiv(p.getBlockX(), SPACING) * SPACING;
            r.oz = Math.floorDiv(p.getBlockZ(), SPACING) * SPACING;
            Room old = ROOMS.put(p.getUUID(), r);
            for (int w = 0; w < 4; w++) {
                r.doorWall = w;
                clearDoor(level, r);
            }
            float yaw = p.getRandom().nextInt(4) * 90f;
            p.teleportTo(level, r.ox + 5.5, FLOOR + 1, r.oz + 5.5, yaw, 0f);
            // the door starts behind you
            Vec3 back = p.getViewVector(1f).scale(-1);
            r.doorWall = Math.abs(back.x) > Math.abs(back.z) ? (back.x > 0 ? 1 : 3) : (back.z > 0 ? 2 : 0);
            placeDoor(level, r, "closed");
            resetObjects(level, r);
            for (RedGuest g : level.getEntitiesOfClass(RedGuest.class, new AABB(p.blockPosition()).inflate(24))) g.discard();
            RedGuest g = RedGuest.TYPE.create(level);
            if (g != null) {
                placeGuest(level, p, r, g);
                g.owner = p.getUUID();
                level.addFreshEntity(g);
            }
        }

        void resetObjects(ServerLevel level, Room r) {
            for (int[] s : LAMP_SPOTS) level.setBlock(at(r, s[0], FLOOR + 1, s[1]), ModBlocks.AIR, Block.UPDATE_ALL);
            r.lamp = 0;
            level.setBlock(at(r, LAMP_SPOTS[0][0], FLOOR + 1, LAMP_SPOTS[0][1]), f("red_lamp", Direction.NORTH), Block.UPDATE_ALL);
            r.chair = Direction.WEST;
            level.setBlock(at(r, CHAIR[0], FLOOR + 1, CHAIR[1]), f("red_chair", r.chair), Block.UPDATE_ALL);
            r.phoneOff = false;
            level.setBlock(at(r, TABLE[0], FLOOR + 1, TABLE[1]), f("red_phone_table", Direction.EAST), Block.UPDATE_ALL);
            r.portrait = 0;
            level.setBlock(at(r, PORTRAIT[0], FLOOR + 3, PORTRAIT[1]), f("red_portrait_0", Direction.WEST), Block.UPDATE_ALL);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Room r = ROOMS.get(p.getUUID());
            if (r == null || p.isSpectator()) return;
            if (p.getX() < r.ox || p.getX() > r.ox + SIZE + 1 || p.getZ() < r.oz || p.getZ() > r.oz + SIZE + 1) return;
            // ---- the door
            if (!r.open) {
                int[] d = DOORS[r.doorWall];
                boolean look = seen(p, doorPoint(r, r.doorWall), at(r, d[0], FLOOR + 2, d[1]), r.stare > 0 ? 0.72 : 0.8);
                if (look) {
                    r.doorSeen = true;
                    r.stare++;
                    if (r.stare == AJAR) {
                        placeDoor(level, r, "ajar");
                        level.playSound(null, at(r, d[0], FLOOR + 1, d[1]), ModSounds.get("event.red_room_creak"), SoundSource.BLOCKS, 0.8f, 1f);
                    } else if (r.stare >= OPEN) {
                        placeDoor(level, r, "open");
                        r.open = true;
                        level.playSound(null, at(r, d[0], FLOOR + 1, d[1]), ModSounds.get("event.red_room_creak"), SoundSource.BLOCKS, 1f, 0.7f);
                    }
                } else if (r.doorSeen) {
                    // you looked away. it is somewhere else now
                    relocateDoor(p, level, r);
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.red_room_shift"), SoundSource.AMBIENT, 0.35f, 0.9f);
                    r.changes++;
                }
            }
            // ---- the furniture
            if (p.tickCount % 2 != 0) return;
            for (int k = 0; k < 4; k++) {
                BlockPos bp = objectPos(r, k);
                boolean now = seen(p, Vec3.atCenterOf(bp), bp, 0.55);
                if (r.seen[k] && !now && p.getRandom().nextFloat() < 0.6f) change(p, level, r, k);
                r.seen[k] = now;
            }
            if (p.tickCount % 40 == 0 && r.changes > 6) {
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", Math.min(0.6f, 0.05f * r.changes)));
            }
        }

        BlockPos objectPos(Room r, int k) {
            return switch (k) {
                case 0 -> at(r, CHAIR[0], FLOOR + 1, CHAIR[1]);
                case 1 -> at(r, PORTRAIT[0], FLOOR + 3, PORTRAIT[1]);
                case 2 -> at(r, TABLE[0], FLOOR + 1, TABLE[1]);
                default -> at(r, LAMP_SPOTS[r.lamp][0], FLOOR + 1, LAMP_SPOTS[r.lamp][1]);
            };
        }

        void change(ServerPlayer p, ServerLevel level, Room r, int k) {
            BlockPos bp = objectPos(r, k);
            switch (k) {
                case 0 -> {
                    // the chair turns to face you
                    double dx = p.getX() - (bp.getX() + 0.5), dz = p.getZ() - (bp.getZ() + 0.5);
                    Direction face = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                            : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                    if (face == r.chair) face = face.getClockWise();
                    r.chair = face;
                    level.setBlock(bp, f("red_chair", face), Block.UPDATE_ALL);
                }
                case 1 -> {
                    r.portrait = (r.portrait + 1) % 4;
                    level.setBlock(bp, f("red_portrait_" + r.portrait, Direction.WEST), Block.UPDATE_ALL);
                }
                case 2 -> {
                    r.phoneOff = !r.phoneOff;
                    level.setBlock(bp, f(r.phoneOff ? "red_phone_table_off" : "red_phone_table", Direction.EAST), Block.UPDATE_ALL);
                }
                default -> {
                    // the lamp is in a corner you are not looking at
                    for (int n = 1; n < LAMP_SPOTS.length; n++) {
                        int next = (r.lamp + n) % LAMP_SPOTS.length;
                        BlockPos np = at(r, LAMP_SPOTS[next][0], FLOOR + 1, LAMP_SPOTS[next][1]);
                        if (seen(p, Vec3.atCenterOf(np), np, 0.4) || !level.getBlockState(np).isAir()) continue;
                        if (!level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(np)).isEmpty()) continue;
                        level.setBlock(bp, ModBlocks.AIR, Block.UPDATE_ALL);
                        level.setBlock(np, f("red_lamp", Direction.NORTH), Block.UPDATE_ALL);
                        r.lamp = next;
                        break;
                    }
                }
            }
            r.changes++;
            level.playSound(null, bp, ModSounds.get("event.red_room_shift"), SoundSource.AMBIENT, 0.25f, 0.8f + p.getRandom().nextFloat() * 0.3f);
        }
    }

    // ---------------------------------------------------------------- the room

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("red_damask");
        final BlockState base = ModBlocks.state("red_baseboard");
        final BlockState carpet = ModBlocks.state("red_carpet");
        final BlockState ceiling = ModBlocks.state("red_ceiling");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, FLOOR, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            CellGrid g = new CellGrid(w.seed, SPACING, 0);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> room(w, x0, z0));
        }

        void room(ChunkWriter w, int x0, int z0) {
            if (x0 > w.maxX || x0 + SIZE < w.minX || z0 > w.maxZ || z0 + SIZE < w.minZ) return;
            int F = FLOOR;
            for (int u = 0; u <= SIZE; u++)
                for (int v = 0; v <= SIZE; v++) {
                    int x = x0 + u, z = z0 + v;
                    w.set(x, F, z, carpet);
                    w.set(x, CEIL, z, ceiling);
                    boolean edge = u == 0 || v == 0 || u == SIZE || v == SIZE;
                    for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, edge ? (y == F + 1 ? base : wall) : ModBlocks.AIR);
                }
            w.set(x0 + 5, F + 4, z0 + 5, f("red_bulb", Direction.NORTH));
            w.set(x0 + LAMP_SPOTS[0][0], F + 1, z0 + LAMP_SPOTS[0][1], f("red_lamp", Direction.NORTH));
            w.set(x0 + CHAIR[0], F + 1, z0 + CHAIR[1], f("red_chair", Direction.WEST));
            w.set(x0 + TABLE[0], F + 1, z0 + TABLE[1], f("red_phone_table", Direction.EAST));
            w.set(x0 + PORTRAIT[0], F + 3, z0 + PORTRAIT[1], f("red_portrait_0", Direction.WEST));
            w.set(x0 + DOORS[0][0], F + 1, z0 + DOORS[0][1], ModBlocks.state("red_door_closed_low"));
            w.set(x0 + DOORS[0][0], F + 2, z0 + DOORS[0][1], ModBlocks.state("red_door_closed_high"));
        }
    }
}
