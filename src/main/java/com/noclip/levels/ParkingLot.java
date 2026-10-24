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
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endless Parking Lot. Painted stalls to the horizon under shoebox lights, all of them
 * empty. Except one car, headlights on, engine idling, facing you. It never moves while
 * you watch it. Look away and it is closer. Painted arrows on the asphalt point toward
 * an exit lane with a pay booth; a stair kiosk somewhere leads down.
 */
public final class ParkingLot {
    static final String ID = "parking_lot";
    static final int G = 40;
    static final int PZ = 17, CROSS = 128, EXIT_X = 384, BOOTH_Z = 68;

    private ParkingLot() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Endless Parking Lot").order(3040)
                .tagline("Every space is empty but one. Its headlights are on, and it is facing you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new TheCar())
                .visuals(new Visuals().tint(1.12f, 0.96f, 0.78f).saturation(0.62f).contrast(1.05f).brightness(1.25f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.5f).warp(0.01f)
                        .fog(6f, 60f).fogColor(0x3A2414).bloom(0.6f))
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.04f).seen().dist(32, 48))
                .exit(ExitType.DOOR, "supermarket", 2).exit(ExitType.DOOR, "dead_mall", 1).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.DESCEND, "parking_garage", 3).exit(ExitType.DESCEND, "subway", 1)
                .exit(ExitType.NOCLIP, "endless_highway", 2).exit(ExitType.NOCLIP, "gas_station", 1)
                .exit(ExitType.NOCLIP, "supermarket", 1)
                .build();
    }

    static boolean crossAisle(int x) {
        return Math.floorMod(x, CROSS) < 7;
    }

    static boolean exitRoad(int x) {
        return Math.floorMod(x, EXIT_X) < 7;
    }

    static boolean kiosk(long s, int x, int z) {
        int cx = Math.floorDiv(x, CROSS), cz = Math.floorDiv(z, PZ);
        return Hash.unit(s, cx, cz, 61) < 0.05f && Math.floorMod(x, CROSS) >= 60 && Math.floorMod(x, CROSS) <= 62
                && Math.floorMod(z, PZ) >= 7 && Math.floorMod(z, PZ) <= 9;
    }

    static final class Gen implements LevelGenerator {
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState curb = ModBlocks.state("pl_curb");
        final BlockState mulch = ModBlocks.state("pl_mulch");
        final BlockState shrub = ModBlocks.state("pl_shrub");
        final BlockState pole = ModBlocks.state("pl_pole");
        final BlockState wall = ModBlocks.state("pl_booth_wall");
        final BlockState window = ModBlocks.state("pl_booth_window");

        BlockState asphalt(String mark) {
            return ModBlocks.state(mark == null ? "pl_asphalt" : "pl_asphalt_" + mark);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.fill(x, 0, z, x, G - 1, z, sub);
                    int v = Math.floorMod(z, PZ), u = Math.floorMod(x, CROSS);
                    String mark = null;
                    boolean stalls = v >= 7 && !crossAisle(x);
                    if (stalls) {
                        boolean div = Math.floorMod(x, 3) == 0, head = v == 12;
                        mark = div && head ? "stall_corner" : div ? "stall_w" : head ? "stall_end" : null;
                        if (!div && v == 9 && Hash.unit(s, x, z, 4) < 0.03f) mark = "handicap";
                    } else if (!crossAisle(x) && v == 3) {
                        int e = Math.floorMod(x, EXIT_X);
                        if (Math.floorMod(x, 24) == 6) mark = e < EXIT_X / 2 ? "arrow_w" : "arrow_e";
                        if (Math.floorMod(x, 24) == 9) mark = "exit_word";
                    } else if (exitRoad(x) && Math.floorMod(x, EXIT_X) == 3 && Math.floorMod(z, 17) == 8) {
                        mark = Math.floorMod(z, BOOTH_Z) < BOOTH_Z / 2 ? "arrow_n" : "arrow_s";
                    }
                    w.set(x, G, z, asphalt(mark));
                    // islands at the ends of the stall rows
                    if (v >= 7 && (u == 7 || u == 8)) {
                        w.set(x, G + 1, z, v == 7 || v == 16 ? curb : mulch);
                        if ((v == 10 || v == 13) && u == 7) w.set(x, G + 2, z, shrub);
                    }
                    // light poles in the middle of the double rows
                    if (v == 12 && Math.floorMod(x, 24) == 13 && !crossAisle(x) && u > 9) {
                        w.set(x, G + 1, z, OutsideKit.facing("pl_base", Direction.NORTH));
                        w.fill(x, G + 2, z, x, G + 8, z, pole);
                        w.set(x, G + 9, z, OutsideKit.facing("pl_heads", Direction.NORTH));
                        for (int dx = -5; dx <= 5; dx += 5)
                            for (int dz = -5; dz <= 5; dz += 5)
                                if (dx != 0 || dz != 0) w.set(x + dx, G + 5, z + dz, OutsideKit.light(13));
                    }
                    if (exitRoad(x)) booth(w, x, z);
                    if (kiosk(s, x, z)) drawKiosk(w, x, z);
                    // the odd stray cart
                    if (stalls && Hash.unit(s, x, z, 9) < 0.0015f) w.set(x, G + 1, z, OutsideKit.facing("gs_cart", Direction.from2DDataValue(Hash.range(s, x, z, 10, 4))));
                }
        }

        void booth(ChunkWriter w, int x, int z) {
            int e = Math.floorMod(x, EXIT_X), b = Math.floorMod(z, BOOTH_Z);
            if (b < 2 || b > 4) return;
            if (e == 1 || e == 2) {
                boolean end = b == 2 || b == 4;
                w.set(x, G + 1, z, wall);
                w.set(x, G + 2, z, end ? wall : window);
                w.set(x, G + 3, z, end ? wall : window);
                w.set(x, G + 4, z, wall);
                if (b == 3) w.set(x, G + 5, z, OutsideKit.facing("pl_pay_sign_" + (e - 1), Direction.NORTH));
                if (b == 3 && e == 2) w.set(x, G + 2, z, OutsideKit.light(12));
            }
            if (b == 3 && e >= 3 && e <= 6) {
                w.set(x, G + 1, z, ModBlocks.state("pl_exit_lane"));
                w.set(x, G + 2, z, e == 3 ? OutsideKit.facing("pl_gate_post", Direction.NORTH) : OutsideKit.facing("pl_gate_arm", Direction.NORTH));
                w.set(x, G + 3, z, ModBlocks.state("pl_exit_lane"));
            }
        }

        void drawKiosk(ChunkWriter w, int x, int z) {
            int u = Math.floorMod(x, CROSS) - 60, v = Math.floorMod(z, PZ) - 7;
            boolean edge = u != 1 || v != 1;
            if (edge) {
                boolean door = u == 1 && v == 0;
                for (int y = 1; y <= 3; y++) w.set(x, G + y, z, door && y <= 2 ? ModBlocks.AIR : wall);
            } else {
                w.set(x, G, z, ModBlocks.state("pl_stairwell"));
                w.set(x, G - 1, z, ModBlocks.state("pl_stairwell"));
                w.set(x, G + 3, z, OutsideKit.light(8));
            }
            w.set(x, G + 4, z, wall);
            if (v == 0 && u <= 1) w.set(x, G + 5, z, OutsideKit.facing("pl_kiosk_sign_" + (1 - u), Direction.NORTH));
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Each visitor has their own car. It closes in only while unwatched. */
    static final class TheCar implements LevelBehavior {
        static final class Car {
            int x, z;
            Direction facing;
            boolean placed;
            int cooldown;
        }

        private final Map<UUID, Car> cars = new ConcurrentHashMap<>();
        private static final String[] COLOURS = {"red", "beige"};

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Car c = cars.remove(p.getUUID());
            if (c != null && c.placed) OutsideKit.clearCar(OutsideKit.Setter.of(level), c.x, G + 1, c.z, c.facing);
            Car car = new Car();
            cars.put(p.getUUID(), car);
            double ang = OutsideKit.autotest() ? Math.toRadians(p.getYRot()) : p.getRandom().nextDouble() * Math.PI * 2;
            double dist = OutsideKit.autotest() ? 12 : 34;
            place(level, p, car, p.getX() - Math.sin(ang) * dist, p.getZ() + Math.cos(ang) * dist);
        }

        boolean place(ServerLevel level, ServerPlayer p, Car car, double tx, double tz) {
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = (int) Math.floor(tx) + (attempt == 0 ? 0 : p.getRandom().nextInt(7) - 3);
                int z = (int) Math.floor(tz) + (attempt == 0 ? 0 : p.getRandom().nextInt(7) - 3);
                double dx = p.getX() - x, dz = p.getZ() - z;
                Direction face = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                        : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                if (!clear(level, x, z, face)) continue;
                OutsideKit.Setter set = OutsideKit.Setter.of(level);
                if (car.placed) OutsideKit.clearCar(set, car.x, G + 1, car.z, car.facing);
                car.x = x;
                car.z = z;
                car.facing = face;
                car.placed = true;
                OutsideKit.car(set, x, G + 1, z, face, COLOURS[Math.floorMod(p.getUUID().hashCode(), 2)], true);
                return true;
            }
            return false;
        }

        boolean clear(ServerLevel level, int x, int z, Direction d) {
            Direction back = d.getOpposite(), side = d.getClockWise();
            for (int i = -1; i < 5; i++)
                for (int c = 0; c < 2; c++) {
                    BlockPos q = new BlockPos(x + back.getStepX() * i + side.getStepX() * c, G + 1, z + back.getStepZ() * i + side.getStepZ() * c);
                    if (!level.isLoaded(q)) return false;
                    BlockState st = level.getBlockState(q);
                    if (!st.isAir() && !st.is(net.minecraft.world.level.block.Blocks.LIGHT) && !st.getBlock().getDescriptionId().contains("ol_car")) return false;
                }
            return true;
        }

        Vec3 centre(Car car) {
            Direction back = car.facing.getOpposite(), side = car.facing.getClockWise();
            return new Vec3(car.x + 0.5 + back.getStepX() * 1.5 + side.getStepX() * 0.5, G + 1.6,
                    car.z + 0.5 + back.getStepZ() * 1.5 + side.getStepZ() * 0.5);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Car car = cars.get(p.getUUID());
            if (car == null || !car.placed || p.tickCount % 10 != 0) return;
            Vec3 c = centre(car);
            double dist = Math.sqrt(c.distanceToSqr(p.getX(), c.y, p.getZ()));
            boolean watched = OutsideKit.inView(p, c, 0.5);
            if (watched) {
                if (p.tickCount % 50 == 0 && dist < 40) OutsideKit.sound(level, BlockPos.containing(c), "event.parking_lot_idle", 1.6f, 1f);
                return;
            }
            if (p.isCreative() || p.isSpectator()) return;
            if (dist > 70) {
                // it never loses you
                double ang = Math.toRadians(p.getYRot() + 180);
                place(level, p, car, p.getX() - Math.sin(ang) * 30, p.getZ() + Math.cos(ang) * 30);
                return;
            }
            if (dist < 6.5) {
                OutsideKit.sound(level, p.blockPosition(), "event.parking_lot_horn", 1.6f, 1f);
                p.hurt(level.damageSources().flyIntoWall(), 7f);
                Sanity.add(p, -12f);
                Vec3 push = p.position().subtract(c).multiply(1, 0, 1).normalize();
                p.push(push.x * 1.2, 0.4, push.z * 1.2);
                p.hurtMarked = true;
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                double ang = Math.toRadians(p.getYRot());
                place(level, p, car, p.getX() + Math.sin(ang) * 36, p.getZ() - Math.cos(ang) * 36);
                return;
            }
            if (--car.cooldown > 0) return;
            car.cooldown = 4 + p.getRandom().nextInt(4);
            double step = Math.min(10, dist - 5);
            Vec3 to = new Vec3(p.getX() - c.x, 0, p.getZ() - c.z).normalize();
            place(level, p, car, car.x + to.x * step, car.z + to.z * step);
        }
    }
}
