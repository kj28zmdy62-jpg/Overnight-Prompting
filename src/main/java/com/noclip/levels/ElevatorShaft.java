package com.noclip.levels;

import com.noclip.entity.ElevatorCar;
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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elevator Shaft. A grid of hoistways, each with a four-by-four car lane down the
 * middle and a ring of maintenance platforms around it, every twelve metres,
 * joined by ladders. It goes up forever: climb high enough and you are somewhere
 * you have already been. It also goes down, and falling is a way out. Every so
 * often the hoist ropes start to sing, and the car comes through.
 */
public final class ElevatorShaft {
    static final int BASE = 8, PERIOD = 48, STEP = 12, CELL = 12;
    static final int WRAP_HIGH = 200, WRAP = PERIOD * 3, FALL_OUT = 40, TOP = 250;

    private ElevatorShaft() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("elevator_shaft")
                .number("").title("Elevator Shaft").order(2100)
                .tagline("There is no top. When the ropes start to sing, get out of the lane.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Hoistway())
                .visuals(new Visuals().darkness(0.45f).tint(0.95f, 0.98f, 1.04f).saturation(0.62f).contrast(1.12f)
                        .brightness(1.15f).grain(0.09f).aberration(0.0016f).vignette(0.65f).warp(0.045f)
                        .fog(3f, 40f).fogColor(0x0E1114).bloom(0.7f))
                .sanityDrain(0.04f)
                .spawnY(60, 190)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.1f).seen().dist(18, 34).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.25f).dist(14, 28).weight(1))
                .exit(ExitType.NOCLIP, "lift_lobby", 2).exit(ExitType.NOCLIP, "stairwell", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.FALL, "the_abyss", 2).exit(ExitType.FALL, "level_0", 1).exit(ExitType.FALL, "mine_shaft", 1)
                .exit(ExitType.DOOR, "lift_lobby", 3).exit(ExitType.DOOR, "level_4", 1).exit(ExitType.DOOR, "bunker", 1)
                .build();
    }

    // ---------------------------------------------------------------- geometry

    static int lx(int x) {
        return Math.floorMod(x, CELL);
    }

    static boolean lane(int x, int z) {
        int a = lx(x), b = lx(z);
        return a >= 4 && a <= 7 && b >= 4 && b <= 7;
    }

    /** 0..3: which platform level of the period this y is in. */
    static int level(int y) {
        return Math.floorMod(y - BASE, PERIOD) / STEP;
    }

    static int h(int y) {
        return Math.floorMod(y - BASE, STEP);
    }

    /** Side index (0 N, 1 E, 2 S, 3 W) of a ring column, or -1 for corners (two sides) / lane. */
    static int side(int a, int b) {
        boolean n = b >= 1 && b <= 3, s = b >= 8, w = a >= 1 && a <= 3, e = a >= 8;
        boolean midX = a >= 4 && a <= 7, midZ = b >= 4 && b <= 7;
        if (n && midX) return 0;
        if (s && midX) return 2;
        if (w && midZ) return 3;
        if (e && midZ) return 1;
        return -1;
    }

    static Direction dir(int side) {
        return switch (side) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("el_wall");
        final BlockState[] numbers;
        final BlockState door = ModBlocks.state("el_landing_door");
        final BlockState doorTop = ModBlocks.state("el_landing_door_top");
        final BlockState openDoor = ModBlocks.state("el_open_door");
        final BlockState grating = ModBlocks.state("el_grating");
        final BlockState beam = ModBlocks.state("el_beam");
        final BlockState rail = ModBlocks.state("el_rail");
        final BlockState cable = ModBlocks.state("el_cable");
        final BlockState rungs = ModBlocks.state("el_rungs");
        final BlockState lamp = ModBlocks.state("el_lamp");
        final BlockState call = ModBlocks.state("el_call");
        final BlockState bottom = ModBlocks.state("el_bottom");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState filler = ModBlocks.state("void_filler");

        Gen() {
            String[] labs = {"13", "13a", "14", "b9", "112", "1203", "4", "0"};
            numbers = new BlockState[labs.length];
            for (int i = 0; i < labs.length; i++) numbers[i] = ModBlocks.state("el_num_" + labs[i]);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(BASE - 1, bottom);
            w.fill(w.minX, 0, w.minZ, w.maxX, BASE - 2, w.maxZ, filler);
            w.fill(w.minX, TOP, w.minZ, w.maxX, w.maxY, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int a = lx(x), b = lx(z);
                    if (a == 0 || b == 0) {
                        wallColumn(w, s, x, z, a, b);
                        continue;
                    }
                    boolean post = (a == 3 || a == 8) && (b == 3 || b == 8);
                    if (post) {
                        w.fill(x, BASE, z, x, TOP - 1, z, rail.setValue(HorizontalDirectionalBlock.FACING,
                                (a == 3) == (b == 3) ? Direction.NORTH : Direction.EAST));
                        continue;
                    }
                    if (lane(x, z)) {
                        if ((a == 5 || a == 6) && (b == 5 || b == 6))
                            for (int y = BASE; y < TOP; y++) w.set(x, y, z, cable);
                        laneBeams(w, x, z, a, b);
                        continue;
                    }
                    ringColumn(w, s, x, z, a, b);
                }
            }
            landings(w, s);
        }

        void wallColumn(ChunkWriter w, long s, int x, int z, int a, int b) {
            for (int y = BASE; y < TOP; y++) {
                int L = level(y), hh = h(y);
                boolean xWall = a == 0 && b != 0, zWall = b == 0 && a != 0;
                boolean evenWalls = Math.floorMod(L, 2) == 0;
                int along = xWall ? b : a;
                boolean doorway = hh >= 1 && hh <= 3 && (along == 9 || along == 10)
                        && ((xWall && evenWalls) || (zWall && !evenWalls))
                        && Hash.chance(s, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL) * 7 + (y - BASE) / STEP, 3, 0.6f);
                w.set(x, y, z, doorway ? ModBlocks.AIR : wall);
            }
        }

        void ringColumn(ChunkWriter w, long s, int x, int z, int a, int b) {
            int sd = side(a, b);
            for (int y = BASE; y < TOP; y++) {
                int L = level(y), hh = h(y);
                if (hh != 0) continue;
                int missing = L % 4;
                if (sd == missing) continue;
                w.set(x, y, z, grating);
            }
            // ladder from each platform up to the next, on the wall opposite that level's gap
            for (int L = 0; L < 4; L++) {
                int ls = (L + 2) % 4;
                Direction d = dir(ls);
                boolean here = switch (ls) {
                    case 0 -> b == 1 && a == 6;
                    case 1 -> a == 11 && b == 6;
                    case 2 -> b == 11 && a == 6;
                    default -> a == 1 && b == 6;
                };
                if (!here) continue;
                BlockState r = rungs.setValue(LadderBlock.FACING, d.getOpposite());
                for (int y0 = BASE + L * STEP; y0 < TOP; y0 += PERIOD) {
                    for (int y = y0 + 1; y <= y0 + STEP && y < TOP; y++) w.set(x, y, z, r);
                }
            }
        }

        /** I-beams across the lane on the levels whose east and west platforms both exist. */
        void laneBeams(ChunkWriter w, int x, int z, int a, int b) {
            if (b != 5) return;
            for (int y = BASE; y < TOP; y++) {
                if (h(y) != 0) continue;
                int L = level(y);
                if (L == 0 || L == 2) w.set(x, y, z, beam);
            }
        }

        /** Landing doors with the wrong numbers, a call button, a lamp. */
        void landings(ChunkWriter w, long s) {
            int ci0 = Math.floorDiv(w.minX, CELL), ci1 = Math.floorDiv(w.maxX, CELL);
            int ck0 = Math.floorDiv(w.minZ, CELL), ck1 = Math.floorDiv(w.maxZ, CELL);
            for (int ci = ci0; ci <= ci1 + 1; ci++) {
                for (int ck = ck0; ck <= ck1 + 1; ck++) {
                    int x0 = ci * CELL, z0 = ck * CELL;
                    for (int y0 = BASE; y0 + STEP < TOP; y0 += STEP) {
                        int L = level(y0);
                        int ds = (L + 3) % 4;
                        long hsh = Hash.hash(s, ci, ck, y0);
                        // the wall the door is set into, and the ring cell in front of it
                        int[] at = switch (ds) {
                            case 0 -> new int[]{x0 + 5, z0, 1, 0, 0, 1};
                            case 1 -> new int[]{x0 + CELL, z0 + 5, 0, 1, -1, 0};
                            case 2 -> new int[]{x0 + 5, z0 + CELL, 1, 0, 0, -1};
                            default -> new int[]{x0, z0 + 5, 0, 1, 1, 0};
                        };
                        int wx = at[0], wz = at[1], sx = at[2], sz = at[3], fx = at[4], fz = at[5];
                        boolean open = Hash.unit(hsh) < 0.05f;
                        for (int k = 0; k < 2; k++) {
                            int x = wx + sx * k, z = wz + sz * k;
                            w.set(x, y0 + 1, z, open ? openDoor : door);
                            w.set(x, y0 + 2, z, open ? openDoor : door);
                            w.set(x, y0 + 3, z, doorTop);
                        }
                        w.set(wx, y0 + 4, wz, numbers[Hash.range(hsh >>> 8, numbers.length)]);
                        w.set(wx + sx, y0 + 4, wz + sz, numbers[Hash.range(hsh >>> 8, numbers.length)]);
                        Direction toWall = ds == 0 ? Direction.NORTH : ds == 1 ? Direction.EAST : ds == 2 ? Direction.SOUTH : Direction.WEST;
                        // button beside the door, lamp above it
                        w.set(wx + sx * 2 + fx, y0 + 2, wz + sz * 2 + fz, call.setValue(HorizontalDirectionalBlock.FACING, toWall));
                        w.set(wx + fx, y0 + 5, wz + fz, lamp.setValue(HorizontalDirectionalBlock.FACING, toWall));
                        float r = Hash.unit(hsh >>> 20);
                        if (r < 0.04f) w.set(wx - sx + fx, y0 + 1, wz - sz + fz, bottle);
                        else if (r < 0.06f) w.set(wx - sx + fx, y0 + 1, wz - sz + fz, supply);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Endless climb, the drop, and the car. */
    static final class Hoistway implements LevelBehavior {
        private final Map<UUID, Long> nextCar = new ConcurrentHashMap<>();
        private static final List<long[]> SUMMONS = new ArrayList<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            if (p.getY() > WRAP_HIGH) {
                p.connection.teleport(p.getX(), p.getY() - WRAP, p.getZ(), p.getYRot(), p.getXRot());
                p.resetFallDistance();
                return;
            }
            if (p.getY() < FALL_OUT && !p.isCreative()) {
                Travel.exit(p, ExitType.FALL, p.blockPosition());
                return;
            }
            long now = level.getGameTime();
            long due = nextCar.computeIfAbsent(p.getUUID(), k -> now + 600 + level.random.nextInt(900));
            if (now >= due) {
                nextCar.put(p.getUUID(), now + 900 + level.random.nextInt(1500));
                int ci = Math.floorDiv(p.getBlockX(), CELL), ck = Math.floorDiv(p.getBlockZ(), CELL);
                if (level.random.nextFloat() < 0.35f) {
                    ci += level.random.nextInt(3) - 1;
                    ck += level.random.nextInt(3) - 1;
                }
                double dirV = level.random.nextBoolean() ? -1 : 1;
                sendCar(level, ci, ck, p.getY() - dirV * 60, dirV * 0.8, p);
            }
            synchronized (SUMMONS) {
                SUMMONS.removeIf(sm -> {
                    if (now < sm[0]) return false;
                    sendCar(level, (int) sm[1], (int) sm[2], sm[3] + 70, -0.8, null);
                    return true;
                });
            }
        }

        static void sendCar(ServerLevel level, int ci, int ck, double y, double vy, Player warn) {
            ElevatorCar car = ElevatorCar.TYPE.create(level);
            if (car == null) return;
            double cx = ci * CELL + 6.0, cz = ck * CELL + 6.0;
            car.moveTo(cx, Math.max(BASE + 2, Math.min(TOP - 6, y)), cz, 0f, 0f);
            car.launch(vy);
            level.addFreshEntity(car);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(cx, p.getY(), cz) > 40 * 40) continue;
                level.playSound(null, p.blockPosition(), ModSounds.get("event.elevator_shaft_cables"), SoundSource.AMBIENT, 1.4f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.35f));
            }
        }
    }

    /** A call button was pressed. Something answers. */
    public static void summon(ServerLevel level, BlockPos pos, Player who) {
        level.playSound(null, pos, ModSounds.get("event.elevator_shaft_ding"), SoundSource.BLOCKS, 1.2f, 1f);
        int ci = Math.floorDiv(pos.getX(), CELL), ck = Math.floorDiv(pos.getZ(), CELL);
        synchronized (Hoistway.SUMMONS) {
            Hoistway.SUMMONS.add(new long[]{level.getGameTime() + 60, ci, ck, pos.getY()});
        }
        if (who instanceof ServerPlayer sp)
            sp.displayClientMessage(Component.literal("Somewhere above, a bell answers.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }
}
