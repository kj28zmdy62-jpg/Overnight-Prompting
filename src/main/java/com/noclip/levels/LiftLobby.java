package com.noclip.levels;

import com.noclip.entity.LiftAttendant;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Lift Lobby. An enfilade of marble lift lobbies strung end to end, every one
 * with four brass doors and a dial above each that never quite settles on a
 * number. The lifts arrive by themselves, empty and lit, and take you wherever
 * they were going. Ring for one and the Lift Attendant steps out instead, and
 * asks which floor you would like. Answer him in chat. He means it.
 */
public final class LiftLobby {
    public static final String ID = "lift_lobby";
    static final int FLOOR = 10, CEIL = 16, C = 18;

    private LiftLobby() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Lift Lobby").order(1170)
                .tagline("Going up? The Attendant would like to know which floor.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Lifts())
                .visuals(new Visuals().tint(1.08f, 0.99f, 0.86f).saturation(0.92f).contrast(1.08f).brightness(1.04f)
                        .grain(0.05f).aberration(0.0010f).vignette(0.52f).bloom(0.55f).fog(4f, 46f).fogColor(0x3A2A1A))
                .sanityDrain(0.02f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("lift_attendant"), 1, 0.06f).dist(16, 30).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.12f).dist(20, 36).weight(1))
                .exit(ExitType.NOCLIP, "level_5", 2).exit(ExitType.NOCLIP, "ballroom", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.NOCLIP, "stairwell", 1)
                .exit(ExitType.DOOR, "stairwell", 5).exit(ExitType.DOOR, "ballroom", 1)
                .exit(ExitType.ASCEND, "rooftops", 2).exit(ExitType.ASCEND, "sky_rooms", 2).exit(ExitType.ASCEND, "level_11", 1)
                .exit(ExitType.ASCEND, "ballroom", 1).exit(ExitType.ASCEND, "convention_center", 1)
                .exit(ExitType.DESCEND, "parking_garage", 2).exit(ExitType.DESCEND, "subway", 2).exit(ExitType.DESCEND, "level_1", 1)
                .exit(ExitType.DESCEND, "level_5", 1).exit(ExitType.DESCEND, "archive", 1)
                .exit(ExitType.FALL, "elevator_shaft", 3).exit(ExitType.FALL, "the_abyss", 1).exit(ExitType.FALL, "level_6", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Room { NORMAL, SHAFT, OPEN_CAR, STAIRS, PODIUM, DIM, LUGGAGE }

    static Room room(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0x11F7);
        if (r < 0.012f) return Room.SHAFT;
        if (r < 0.032f) return Room.OPEN_CAR;
        if (r < 0.065f) return Room.STAIRS;
        if (r < 0.16f) return Room.PODIUM;
        if (r < 0.24f) return Room.DIM;
        if (r < 0.30f) return Room.LUGGAGE;
        return Room.NORMAL;
    }

    /** Is there a corridor through the north wall of cell (i,k)? */
    static boolean openNorth(long s, int i, int k) {
        return Hash.chance(s, i, k, 0x51, 0.35f);
    }

    /** Which of the four lifts (0..3) a special room uses. */
    static int specialLift(long s, int i, int k) {
        return Hash.range(s, i, k, 0x5EC, 4);
    }

    /** Lift n of a cell: n&1 = slot (west/east), n>>1 = side (0 north wall, 1 south wall). */
    static BlockPos liftPos(int i, int k, int n) {
        int xa = i * C + ((n & 1) == 0 ? 4 : 12);
        int wz = k * C + ((n >> 1) == 0 ? 4 : 13);
        return new BlockPos(xa, FLOOR + 1, wz);
    }

    /** Direction from the door toward the room. */
    static Direction liftFacing(int n) {
        return (n >> 1) == 0 ? Direction.SOUTH : Direction.NORTH;
    }

    static BlockPos podiumPos(int i, int k) {
        return new BlockPos(i * C + 11, FLOOR + 1, k * C + 6);
    }

    static final class Gen implements LevelGenerator {
        final BlockState cream = ModBlocks.state("lift_marble_cream");
        final BlockState black = ModBlocks.state("lift_marble_black");
        final BlockState runN = ModBlocks.state("lift_runner_n");
        final BlockState runS = ModBlocks.state("lift_runner_s");
        final BlockState wall = ModBlocks.state("lift_wall");
        final BlockState wainscot = ModBlocks.state("lift_wainscot");
        final BlockState cornice = ModBlocks.state("lift_cornice");
        final BlockState ceiling = ModBlocks.state("lift_ceiling");
        final BlockState light = ModBlocks.state("lift_ceiling_light");
        final BlockState flicker = ModBlocks.state("lift_ceiling_light_flicker");
        final BlockState dead = ModBlocks.state("lift_ceiling_light_dead");
        final BlockState frame = ModBlocks.state("lift_frame");
        final BlockState door = ModBlocks.state("lift_door");
        final BlockState doorTop = ModBlocks.state("lift_door_top");
        final BlockState dialL = ModBlocks.state("lift_dial_l");
        final BlockState dialR = ModBlocks.state("lift_dial_r");
        final BlockState wrongL = ModBlocks.state("lift_dial_wrong_l");
        final BlockState wrongR = ModBlocks.state("lift_dial_wrong_r");
        final BlockState button = ModBlocks.state("lift_call_button");
        final BlockState cabWall = ModBlocks.state("lift_cabin_wall");
        final BlockState mirror = ModBlocks.state("lift_cabin_mirror");
        final BlockState cabLight = ModBlocks.state("lift_cabin_light");
        final BlockState cabFloor = ModBlocks.state("lift_cabin_floor");
        final BlockState shaft = ModBlocks.state("lift_shaft_dark");
        final BlockState stairsDoor = ModBlocks.state("lift_stairs_door");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState bench = ModBlocks.state("lift_bench");
        final BlockState planter = ModBlocks.state("lift_planter");
        final BlockState sconce = ModBlocks.state("lift_sconce");
        final BlockState ashtray = ModBlocks.state("lift_ashtray");
        final BlockState luggage = ModBlocks.state("lift_luggage");
        final BlockState podium = ModBlocks.state("lift_podium");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = Math.floorDiv(x, C), k = Math.floorDiv(z, C);
                    int lx = x - i * C, lz = z - k * C;
                    boolean room = lx >= 3 && lx <= 14 && lz >= 5 && lz <= 12;
                    boolean ew = lz >= 7 && lz <= 10 && (lx <= 2 || lx >= 15);
                    boolean ns = lx >= 7 && lx <= 10 && ((lz <= 4 && openNorth(s, i, k)) || (lz >= 13 && openNorth(s, i, k + 1)));
                    BlockState f = ((x + z) & 1) == 0 ? cream : black;
                    if (lz == 8 && (room || ew)) f = runN;
                    if (lz == 9 && (room || ew)) f = runS;
                    if (ns && !room) f = (lx == 7 || lx == 10) ? black : cream;
                    w.set(x, FLOOR, z, f);
                    w.set(x, CEIL, z, ceiling);
                    if (!(room || ew || ns)) {
                        w.set(x, FLOOR + 1, z, wainscot);
                        for (int y = FLOOR + 2; y <= FLOOR + 4; y++) w.set(x, y, z, wall);
                        w.set(x, FLOOR + 5, z, cornice);
                    }
                }
            }
            new CellGrid(s, C, 0).forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, s, i, k, x0, z0));
        }

        void cell(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            Room type = room(s, i, k);
            int special = specialLift(s, i, k);
            boolean openN = openNorth(s, i, k), openS = openNorth(s, i, k + 1);
            // lifts
            for (int n = 0; n < 4; n++) {
                BlockPos p = liftPos(i, k, n);
                Direction f = liftFacing(n);
                boolean wrong = type == Room.DIM || (type == Room.SHAFT && n == special)
                        || Hash.chance(s, i * 4 + n, k, 0xD1A, 0.04f);
                int mode = 0;
                if (n == special && type == Room.SHAFT) mode = 2;
                if (n == special && type == Room.OPEN_CAR) mode = 1;
                lift(w, p.getX(), p.getZ(), f, wrong, mode);
            }
            // middle of each long wall: a corridor, or sconces, a call plate and maybe the stair door
            for (int side = 0; side < 2; side++) {
                boolean open = side == 0 ? openN : openS;
                int wz = z0 + (side == 0 ? 4 : 13);
                Direction toWall = side == 0 ? Direction.NORTH : Direction.SOUTH;
                int rz = wz + (side == 0 ? 1 : -1);
                for (int bx : new int[]{6, 11})
                    w.set(x0 + bx, FLOOR + 2, rz, button.setValue(HorizontalDirectionalBlock.FACING, toWall));
                if (open) continue;
                w.set(x0 + 7, FLOOR + 3, rz, sconce.setValue(HorizontalDirectionalBlock.FACING, toWall));
                w.set(x0 + 10, FLOOR + 3, rz, sconce.setValue(HorizontalDirectionalBlock.FACING, toWall));
                boolean stairsHere = type == Room.STAIRS && (side == 0 || openN);
                if (stairsHere && !(side == 1 && !openN)) {
                    w.set(x0 + 8, FLOOR + 1, wz, stairsDoor);
                    w.set(x0 + 8, FLOOR + 2, wz, stairsDoor);
                    w.set(x0 + 8, FLOOR + 3, rz, exitSign.setValue(HorizontalDirectionalBlock.FACING, toWall));
                } else if (type != Room.LUGGAGE && Hash.chance(s, i, k, 0xBE + side, 0.7f)) {
                    w.set(x0 + 8 + side, FLOOR + 1, rz, bench.setValue(HorizontalDirectionalBlock.FACING, toWall.getOpposite()));
                }
            }
            // end-wall sconces beside the corridors
            for (int ez : new int[]{6, 11}) {
                w.set(x0 + 3, FLOOR + 3, z0 + ez, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                w.set(x0 + 14, FLOOR + 3, z0 + ez, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            }
            // corners
            int[][] corners = {{3, 5}, {14, 5}, {3, 12}, {14, 12}};
            for (int c = 0; c < 4; c++) {
                if (Hash.chance(s, i * 4 + c, k, 0xC0, 0.75f))
                    w.set(x0 + corners[c][0], FLOOR + 1, z0 + corners[c][1], planter);
            }
            if (Hash.chance(s, i, k, 0xA5, 0.4f)) w.set(x0 + 7, FLOOR + 1, z0 + 6, ashtray);
            // ceiling lights
            int[][] lights = {{5, 7}, {5, 10}, {12, 7}, {12, 10}, {8, 8}, {9, 8}, {8, 9}, {9, 9}};
            int lucky = Hash.range(s, i, k, 0x1C, lights.length);
            for (int li = 0; li < lights.length; li++) {
                BlockState l;
                if (type == Room.DIM) l = li == lucky ? flicker : dead;
                else {
                    float r = Hash.unit(s, i * 8 + li, k, 0x1D);
                    l = r < 0.04f ? dead : r < 0.10f ? flicker : light;
                }
                w.set(x0 + lights[li][0], CEIL, z0 + lights[li][1], l);
            }
            for (int[] cl : new int[][]{{0, 8}, {0, 9}, {17, 8}, {17, 9}}) w.set(x0 + cl[0], CEIL, z0 + cl[1], light);
            if (openN) {
                w.set(x0 + 8, CEIL, z0, light);
                w.set(x0 + 9, CEIL, z0, light);
            }
            // room specials
            if (type == Room.PODIUM) {
                BlockPos pp = podiumPos(i, k);
                w.set(pp.getX(), pp.getY(), pp.getZ(), podium.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            } else if (type == Room.LUGGAGE) {
                for (int n = 0; n < 5; n++) {
                    long h = Hash.hash(s, i, k, 0x1100 + n);
                    int lx = 4 + Hash.range(h, 10), lz = 5 + Hash.range(h >>> 8, 8);
                    if (lz == 8 || lz == 9) lz = lz == 8 ? 6 : 11;
                    w.set(x0 + lx, FLOOR + 1, z0 + lz, luggage.setValue(HorizontalDirectionalBlock.FACING,
                            Direction.from2DDataValue(Hash.range(h >>> 16, 4))));
                }
            }
        }

        /** One lift: bronze surround, brass doors, dial, and the car behind them. mode 1 = open car, 2 = open shaft. */
        void lift(ChunkWriter w, int xa, int wz, Direction f, boolean wrong, int mode) {
            Direction b = f.getOpposite();
            int bz = b.getStepZ();
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                w.set(xa - 1, y, wz, frame);
                w.set(xa + 2, y, wz, frame);
            }
            int left = b.getCounterClockWise().getStepX() < 0 ? xa : xa + 1;
            for (int c = 0; c < 2; c++) {
                int x = xa + c;
                boolean isLeft = x == left;
                w.set(x, FLOOR + 4, wz, (isLeft ? (wrong ? wrongL : dialL) : (wrong ? wrongR : dialR))
                        .setValue(HorizontalDirectionalBlock.FACING, f));
                if (mode == 0) {
                    w.set(x, FLOOR + 1, wz, door.setValue(HorizontalDirectionalBlock.FACING, f));
                    w.set(x, FLOOR + 2, wz, door.setValue(HorizontalDirectionalBlock.FACING, f));
                    w.set(x, FLOOR + 3, wz, doorTop.setValue(HorizontalDirectionalBlock.FACING, f));
                } else {
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) w.set(x, y, wz, ModBlocks.AIR);
                }
                // the car
                for (int d = 1; d <= 2; d++) {
                    int z = wz + bz * d;
                    if (mode == 2) {
                        for (int y = FLOOR - 4; y <= FLOOR + 3; y++) w.set(x, y, z, shaft);
                        w.set(x, FLOOR + 4, z, filler);
                    } else {
                        w.set(x, FLOOR, z, cabFloor);
                        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) w.set(x, y, z, ModBlocks.AIR);
                        w.set(x, FLOOR + 4, z, cabLight);
                    }
                    BlockState side = mode == 2 ? filler : cabWall;
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                        w.set(xa - 1, y, z, side);
                        w.set(xa + 2, y, z, side);
                    }
                }
                int back = wz + bz * 3;
                w.set(x, FLOOR + 1, back, mode == 2 ? filler : cabWall);
                w.set(x, FLOOR + 2, back, mode == 2 ? filler : mirror);
                w.set(x, FLOOR + 3, back, mode == 2 ? filler : cabWall);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    record OpenLift(ResourceKey<Level> dim, int xa, int wz, Direction facing, long closeAt) {}

    record Ride(long at, String dest) {}

    record Call(long at, BlockPos button) {}

    static final List<OpenLift> OPEN = new ArrayList<>();
    static final Map<UUID, Ride> RIDES = new HashMap<>();
    static final Map<UUID, String> DEST = new HashMap<>();
    static final Map<UUID, Call> CALLS = new HashMap<>();
    static final Map<UUID, Long> NEXT_GHOST = new HashMap<>();

    /** A call plate was pressed. In the Lift Lobby, a lift (and usually its attendant) comes. */
    public static void onCall(ServerPlayer p, BlockPos button) {
        BackroomsLevel def = Levels.get(p.level().dimension());
        if (def == null || !ID.equals(def.id)) return;
        if (CALLS.containsKey(p.getUUID())) return;
        long now = p.serverLevel().getGameTime();
        CALLS.put(p.getUUID(), new Call(now + 50 + p.getRandom().nextInt(70), button.immutable()));
    }

    /** The attendant has a destination for this player: open the lift nearest him and hold it. */
    public static boolean prepareRide(ServerPlayer p, String dest, Vec3 near) {
        ServerLevel level = p.serverLevel();
        int[] lift = nearestLift(level, near, 20);
        DEST.put(p.getUUID(), dest);
        if (lift == null) return false;
        open(level, lift[0], lift[1], Direction.from3DDataValue(lift[2]), 20 * 25);
        return true;
    }

    /** Sends the player on at once (no lift at hand), with the doors-closing blackout. */
    public static void rideNow(ServerPlayer p, String dest) {
        DEST.put(p.getUUID(), dest);
        startRide(p);
    }

    public static Vec3 liftCentre(int xa, int wz, Direction f) {
        return new Vec3(xa + 1.0, FLOOR + 1, wz + 0.5 + f.getStepZ() * 1.2);
    }

    /** {xa, wz, facing3d} of the nearest working lift within range, or null. */
    static int[] nearestLift(ServerLevel level, Vec3 near, double range) {
        long s = seedOf(level);
        int ci = Math.floorDiv((int) Math.floor(near.x), C), ck = Math.floorDiv((int) Math.floor(near.z), C);
        int[] best = null;
        double bd = range * range;
        for (int i = ci - 1; i <= ci + 1; i++)
            for (int k = ck - 1; k <= ck + 1; k++) {
                Room type = room(s, i, k);
                for (int n = 0; n < 4; n++) {
                    if (type == Room.SHAFT && n == specialLift(s, i, k)) continue;
                    BlockPos p = liftPos(i, k, n);
                    if (!level.getBlockState(p).is(ModBlocks.get("lift_door")) && !level.getBlockState(p).isAir()) continue;
                    double d = near.distanceToSqr(p.getX() + 1.0, p.getY(), p.getZ() + 0.5);
                    if (d < bd) {
                        bd = d;
                        best = new int[]{p.getX(), p.getZ(), liftFacing(n).get3DDataValue()};
                    }
                }
            }
        return best;
    }

    /** The chunk generator's seed for this level (what ChunkWriter.seed is built from). */
    static long seedOf(ServerLevel level) {
        return Hash.hash(level.getSeed(), Hash.salt(ID));
    }

    static void open(ServerLevel level, int xa, int wz, Direction f, int ticks) {
        for (OpenLift o : OPEN) if (o.dim == level.dimension() && o.xa == xa && o.wz == wz) return;
        for (int c = 0; c < 2; c++)
            for (int y = FLOOR + 1; y <= FLOOR + 3; y++) level.setBlock(new BlockPos(xa + c, y, wz), ModBlocks.AIR, Block.UPDATE_ALL);
        BlockPos mid = new BlockPos(xa, FLOOR + 2, wz);
        level.playSound(null, mid, ModSounds.get("event.lift_lobby_ding"), SoundSource.BLOCKS, 1.4f, 1f);
        OPEN.add(new OpenLift(level.dimension(), xa, wz, f, level.getGameTime() + ticks));
    }

    static void close(ServerLevel level, OpenLift o) {
        BlockState door = ModBlocks.state("lift_door").setValue(HorizontalDirectionalBlock.FACING, o.facing);
        BlockState top = ModBlocks.state("lift_door_top").setValue(HorizontalDirectionalBlock.FACING, o.facing);
        for (int c = 0; c < 2; c++) {
            level.setBlock(new BlockPos(o.xa + c, FLOOR + 1, o.wz), door, Block.UPDATE_ALL);
            level.setBlock(new BlockPos(o.xa + c, FLOOR + 2, o.wz), door, Block.UPDATE_ALL);
            level.setBlock(new BlockPos(o.xa + c, FLOOR + 3, o.wz), top, Block.UPDATE_ALL);
        }
        level.playSound(null, new BlockPos(o.xa, FLOOR + 2, o.wz), ModSounds.get("event.lift_lobby_doors"),
                SoundSource.BLOCKS, 0.9f, 1f);
    }

    static boolean doorwayBlocked(ServerLevel level, OpenLift o) {
        AABB box = new AABB(o.xa, FLOOR + 1, o.wz, o.xa + 2, FLOOR + 4, o.wz + 1);
        return !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box).isEmpty();
    }

    static void startRide(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        // the doors close behind you
        for (Iterator<OpenLift> it = OPEN.iterator(); it.hasNext(); ) {
            OpenLift o = it.next();
            if (o.dim != level.dimension()) continue;
            if (p.distanceToSqr(o.xa + 1.0, FLOOR + 1, o.wz + 0.5) < 16 && !doorwayBlocked(level, o)) {
                close(level, o);
                it.remove();
            }
        }
        level.playSound(null, p.blockPosition(), ModSounds.get("event.lift_lobby_ding"), SoundSource.BLOCKS, 1.0f, 0.84f);
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("blackout", 2.2f));
        String dest = DEST.remove(p.getUUID());
        if (dest == null) dest = p.getRandom().nextBoolean() ? "#ASCEND" : "#DESCEND";
        RIDES.put(p.getUUID(), new Ride(now + 40, dest));
    }

    static void finishRide(ServerPlayer p, String dest) {
        if (dest.startsWith("#")) Travel.exit(p, ExitType.valueOf(dest.substring(1)), p.blockPosition());
        else if (!Travel.sendTo(p, dest)) Travel.exit(p, ExitType.ASCEND, p.blockPosition());
    }

    static final class Lifts implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            DEST.remove(p.getUUID());
            RIDES.remove(p.getUUID());
            CALLS.remove(p.getUUID());
            NEXT_GHOST.put(p.getUUID(), level.getGameTime() + 20 * 30);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            UUID id = p.getUUID();
            Ride ride = RIDES.get(id);
            if (ride != null) {
                if (now >= ride.at) {
                    RIDES.remove(id);
                    finishRide(p, ride.dest);
                }
                return;
            }
            if (p.tickCount % 4 == 0 && !p.isSpectator()) {
                BlockPos feet = p.blockPosition();
                if (level.getBlockState(feet).is(ModBlocks.get("lift_cabin_floor"))
                        || level.getBlockState(feet.below()).is(ModBlocks.get("lift_cabin_floor"))) {
                    startRide(p);
                    return;
                }
            }
            Call call = CALLS.get(id);
            if (call != null && now >= call.at) {
                CALLS.remove(id);
                arrive(p, level, call.button);
            }
            // lifts arrive on their own: empty, lit, waiting a moment, then gone again
            Long ghost = NEXT_GHOST.get(id);
            if (ghost != null && now >= ghost) {
                NEXT_GHOST.put(id, now + 20 * (35 + p.getRandom().nextInt(60)));
                int[] lift = nearestLift(level, p.position().add(p.getRandom().nextGaussian() * 6, 0, p.getRandom().nextGaussian() * 6), 22);
                if (lift != null) open(level, lift[0], lift[1], Direction.from3DDataValue(lift[2]), 20 * 7);
            }
            // the attendant keeps his podium
            if (p.tickCount % 100 == 37 && level.getEntitiesOfClass(LiftAttendant.class, p.getBoundingBox().inflate(48)).isEmpty()) {
                long s = seedOf(level);
                int ci = Math.floorDiv(p.getBlockX(), C), ck = Math.floorDiv(p.getBlockZ(), C);
                for (int i = ci - 1; i <= ci + 1; i++)
                    for (int k = ck - 1; k <= ck + 1; k++) {
                        if (room(s, i, k) != Room.PODIUM) continue;
                        BlockPos pp = podiumPos(i, k).south();
                        if (EmptyBuildingKit.seen(p, pp) || p.distanceToSqr(Vec3.atCenterOf(pp)) < 36) continue;
                        LiftAttendant a = LiftAttendant.TYPE.create(level);
                        if (a == null) return;
                        a.moveTo(pp.getX() + 0.5, pp.getY(), pp.getZ() + 0.5, 0f, 0f);
                        level.addFreshEntity(a);
                        return;
                    }
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            for (Iterator<OpenLift> it = OPEN.iterator(); it.hasNext(); ) {
                OpenLift o = it.next();
                if (o.dim != level.dimension() || now < o.closeAt) continue;
                if (doorwayBlocked(level, o)) continue;
                close(level, o);
                it.remove();
            }
        }
    }

    /** A called lift arrives: the nearest doors open and, most of the time, he steps out. */
    static void arrive(ServerPlayer p, ServerLevel level, BlockPos button) {
        int[] lift = nearestLift(level, Vec3.atCenterOf(button), 14);
        if (lift == null) return;
        Direction f = Direction.from3DDataValue(lift[2]);
        open(level, lift[0], lift[1], f, 20 * 12);
        List<LiftAttendant> near = level.getEntitiesOfClass(LiftAttendant.class, p.getBoundingBox().inflate(40));
        if (!near.isEmpty()) {
            near.get(0).summon(p);
            return;
        }
        if (p.getRandom().nextFloat() < 0.85f) {
            LiftAttendant a = LiftAttendant.TYPE.create(level);
            if (a == null) return;
            double x = lift[0] + 1.0, z = lift[1] + 0.5 - f.getStepZ() * 1.5;
            a.moveTo(x, FLOOR + 1, z, f.toYRot(), 0f);
            level.addFreshEntity(a);
            a.summon(p);
        }
    }

    /** Used by the attendant when he gives up on you or you hit him. */
    public static void dropThrough(ServerPlayer p) {
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("blackout", 1.5f));
        Travel.exit(p, ExitType.FALL, p.blockPosition());
    }

    static boolean isHere(Player p) {
        BackroomsLevel def = Levels.get(p.level().dimension());
        return def != null && ID.equals(def.id);
    }
}
