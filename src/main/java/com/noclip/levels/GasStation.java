package com.noclip.levels;

import com.noclip.block.PayphoneBlock;
import com.noclip.entity.GasAttendant;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repeating Gas Station. One lit station on a two-lane road at night, fields all
 * around. The world is a loop: walk away from the station in any direction and you
 * walk back into it from the other side. Every lap, something about it has changed.
 * Keep coming back and eventually the restroom door is open, the payphone rings, and
 * the man by the pumps stops waiting for you.
 */
public final class GasStation {
    static final String ID = "gas_station";
    static final int G = 40;
    /** The station repeats every P blocks along x and z. */
    static final int P = 128;

    private GasStation() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Repeating Gas Station").order(3030)
                .tagline("Last gas for miles. You keep arriving back at it.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Loop())
                .visuals(new Visuals().tint(1.0f, 0.98f, 1.04f).saturation(0.8f).contrast(1.1f).brightness(1.12f)
                        .grain(0.08f).aberration(0.0014f).vignette(0.55f).fog(3f, 40f).fogColor(0x0B0D14).bloom(0.55f).sky())
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .exit(ExitType.DOOR, "restrooms", 3).exit(ExitType.DOOR, "motel", 1).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.SECRET, "call_center", 2).exit(ExitType.SECRET, "endless_highway", 2)
                .exit(ExitType.SECRET, "the_hub", 1)
                .exit(ExitType.NOCLIP, "endless_highway", 3).exit(ExitType.NOCLIP, "parking_lot", 2)
                .exit(ExitType.NOCLIP, "motel", 1).exit(ExitType.NOCLIP, "supermarket", 1)
                .build();
    }

    // ---------------------------------------------------------------- the tile (local coordinates 0..P-1)

    static final int ROAD_A = 60, ROAD_B = 68, ROAD_MID = 64;
    static final int SHOP_X0 = 44, SHOP_X1 = 75, SHOP_Z0 = 22, SHOP_Z1 = 35;
    static final int CAN_X0 = 48, CAN_X1 = 76, CAN_Z0 = 39, CAN_Z1 = 54;
    static final int PUMP_A = 57, PUMP_B = 67;
    static final int SIGN_X = 86, SIGN_Z = 57;
    static final int DOOR_X = 59;
    static final int PHONE_X = 42, PHONE_Z = 36;
    static final int REST_Z = 29;

    static boolean canopyLight(int lx, int lz) {
        return Math.floorMod(lx, 4) == 2 && Math.floorMod(lz - CAN_Z0, 3) == 2 && lx > CAN_X0 && lx < CAN_X1 && lz > CAN_Z0 && lz < CAN_Z1;
    }

    static final class Gen implements LevelGenerator {
        final BlockState asphalt = ModBlocks.state("gs_asphalt");
        final BlockState yellow = ModBlocks.state("gs_asphalt_yellow");
        final BlockState white = ModBlocks.state("gs_asphalt_white");
        final BlockState gravel = ModBlocks.state("gs_gravel");
        final BlockState concrete = ModBlocks.state("gs_concrete");
        final BlockState oil = ModBlocks.state("gs_concrete_oil");
        final BlockState grass = ModBlocks.state("gs_grass");
        final BlockState dirt = ModBlocks.state("gs_dirt");
        final BlockState weeds = ModBlocks.state("gs_weeds");
        final BlockState wall = ModBlocks.state("gs_wall");
        final BlockState window = ModBlocks.state("gs_window");
        final BlockState tile = ModBlocks.state("gs_tile");
        final BlockState roof = ModBlocks.state("gs_roof");
        final BlockState ceiling = ModBlocks.state("ceiling_tile");
        final BlockState light = ModBlocks.state("fluorescent_light");
        final BlockState flicker = ModBlocks.state("flickering_fluorescent_light");
        final BlockState canopy = ModBlocks.state("gs_canopy");
        final BlockState canopyEdge = ModBlocks.state("gs_canopy_edge");
        final BlockState canopyLight = ModBlocks.state("gs_canopy_light");
        final BlockState pillar = ModBlocks.state("gs_pillar");
        final BlockState curb = ModBlocks.state("gs_curb");
        final BlockState post = ModBlocks.state("gs_sign_post");
        final BlockState pole = ModBlocks.state("gs_pole");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, P), lz = Math.floorMod(z, P);
                    w.fill(x, 0, z, x, G - 4, z, sub);
                    w.fill(x, G - 3, z, x, G - 1, z, dirt);
                    w.set(x, G, z, ground(s, lx, lz));
                    if (w.get(x, G, z) == grass && Hash.unit(s, lx, lz, 5) < 0.14f) w.set(x, G + 1, z, weeds);
                    column(w, s, x, z, lx, lz);
                }
            }
        }

        BlockState ground(long s, int lx, int lz) {
            if (lz >= ROAD_A && lz <= ROAD_B) {
                if (lz == ROAD_A || lz == ROAD_B) return white;
                if (lz == ROAD_MID && Math.floorMod(lx, 6) < 3) return yellow;
                return asphalt;
            }
            if ((lz >= ROAD_A - 2 && lz < ROAD_A) || (lz > ROAD_B && lz <= ROAD_B + 2)) return gravel;
            if (lx >= SHOP_X0 && lx <= SHOP_X1 && lz >= SHOP_Z0 && lz <= SHOP_Z1) return tile;
            if (lx >= 36 && lx <= 91 && lz >= 36 && lz < ROAD_A - 2) {
                boolean nearPumps = lz > CAN_Z0 + 2 && lz < CAN_Z1 - 1 && lx > CAN_X0 && lx < CAN_X1;
                return Hash.unit(s, lx, lz, 3) < (nearPumps ? 0.12f : 0.03f) ? oil : concrete;
            }
            if (lx >= 40 && lx <= 79 && lz >= 17 && lz < 36) return concrete;
            return grass;
        }

        void column(ChunkWriter w, long s, int x, int z, int lx, int lz) {
            // ---- the shop
            if (lx >= SHOP_X0 && lx <= SHOP_X1 && lz >= SHOP_Z0 && lz <= SHOP_Z1) {
                boolean edge = lx == SHOP_X0 || lx == SHOP_X1 || lz == SHOP_Z0 || lz == SHOP_Z1;
                w.set(x, G + 6, z, roof);
                if (edge) {
                    w.fill(x, G + 1, z, x, G + 5, z, wall);
                    if (lz == SHOP_Z1) front(w, x, z, lx);
                    if (lx == SHOP_X1 && lz == REST_Z) {
                        w.set(x, G + 1, z, OutsideKit.facing("gs_restroom_lower", Direction.EAST));
                        w.set(x, G + 2, z, OutsideKit.facing("gs_restroom_upper", Direction.EAST));
                    }
                } else {
                    boolean lamp = Math.floorMod(lx, 4) == 1 && Math.floorMod(lz, 4) == 1;
                    w.set(x, G + 5, z, lamp ? (Hash.unit(s, lx, lz, 8) < 0.2f ? flicker : light) : ceiling);
                    interior(w, s, x, z, lx, lz);
                }
                return;
            }
            // ---- canopy over the pumps
            if (lx >= CAN_X0 && lx <= CAN_X1 && lz >= CAN_Z0 && lz <= CAN_Z1) {
                boolean edge = lx == CAN_X0 || lx == CAN_X1 || lz == CAN_Z0 || lz == CAN_Z1;
                w.set(x, G + 6, z, edge ? canopyEdge : canopyLight(lx, lz) ? canopyLight : canopy);
                w.set(x, G + 7, z, edge ? canopyEdge : roof);
                if ((lx == CAN_X0 + 2 || lx == CAN_X1 - 2) && (lz == CAN_Z0 + 2 || lz == CAN_Z1 - 2))
                    w.fill(x, G + 1, z, x, G + 5, z, pillar);
                if ((lx == PUMP_A || lx == PUMP_B) && lz >= 43 && lz <= 50) {
                    if (lz == 45 || lz == 48) {
                        Direction d = lz == 45 ? Direction.EAST : Direction.WEST;
                        w.set(x, G + 1, z, OutsideKit.facing("gs_pump_lower", d));
                        w.set(x, G + 2, z, OutsideKit.facing("gs_pump_upper", d));
                    } else {
                        w.set(x, G + 1, z, OutsideKit.facing(curb, Direction.NORTH));
                    }
                }
                return;
            }
            // ---- odds and ends around the lot
            if (lx == PHONE_X && lz == PHONE_Z) w.set(x, G + 1, z, OutsideKit.facing("gs_payphone", Direction.SOUTH));
            if (lx == 78 && lz == 36) w.set(x, G + 1, z, OutsideKit.facing("gs_ice", Direction.SOUTH));
            if ((lx == 60 || lx == 61) && lz == 19) w.set(x, G + 1, z, OutsideKit.facing("gs_dumpster", Direction.NORTH));
            // price sign by the road
            if ((lx == SIGN_X || lx == SIGN_X + 1) && lz == SIGN_Z) {
                w.fill(x, G + 1, z, x, G + 6, z, post);
                String side = lx == SIGN_X ? "_l" : "_r";
                w.set(x, G + 7, z, OutsideKit.facing("gs_sign_price0" + side, Direction.NORTH));
                w.set(x, G + 8, z, OutsideKit.facing("gs_sign_brand" + side, Direction.NORTH));
            }
            // telephone poles and wires along the far side of the road
            if (lz == 72) {
                if (Math.floorMod(lx, 16) == 8) {
                    w.fill(x, G + 1, z, x, G + 8, z, pole);
                    w.set(x, G + 9, z, OutsideKit.facing("gs_pole_top", Direction.EAST));
                } else {
                    w.set(x, G + 9, z, OutsideKit.facing("gs_wire", Direction.EAST));
                }
            }
            // a billboard facing the road
            if (lz == 82 && lx >= 18 && lx <= 21) {
                if (lx == 18 || lx == 21) w.fill(x, G + 1, z, x, G + 4, z, post);
                w.set(x, G + 5, z, OutsideKit.facing("gs_billboard_" + (21 - lx) + "_1", Direction.NORTH));
                w.set(x, G + 6, z, OutsideKit.facing("gs_billboard_" + (21 - lx) + "_0", Direction.NORTH));
            }
            if (lz == 80 && (lx == 18 || lx == 21)) w.set(x, G + 2, z, OutsideKit.light(13));
            // the same few things in the fields, every lap
            if (lx == 101 && lz == 96) w.set(x, G + 1, z, OutsideKit.facing("gs_warning_sign", Direction.NORTH));
            if (lx == 24 && lz == 44) w.set(x, G + 1, z, OutsideKit.facing("gs_cart", Direction.WEST));
            if (lx == 112 && lz == 20) OutsideKit.car(w, x, G + 1, z, Direction.WEST, "green", false);
        }

        void front(ChunkWriter w, int x, int z, int lx) {
            if ((lx >= 46 && lx <= 56) || (lx >= 63 && lx <= 73)) w.fill(x, G + 2, z, x, G + 4, z, window);
            if (lx == DOOR_X || lx == DOOR_X + 1) {
                DoorHingeSide hinge = lx == DOOR_X ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
                BlockState d = ModBlocks.state("gs_glass_door").setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(DoorBlock.HINGE, hinge);
                w.set(x, G + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                w.set(x, G + 2, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                w.set(x, G + 3, z, window);
                w.set(x, G + 4, z, window);
            }
            if (lx >= 55 && lx <= 64) w.set(x, G + 5, z, OutsideKit.facing("gs_shopsign_" + (lx - 55), Direction.SOUTH));
            if (lx == 50) w.set(x, G + 3, z - 1, OutsideKit.facing("gs_open_sign", Direction.SOUTH));
        }

        void interior(ChunkWriter w, long s, int x, int z, int lx, int lz) {
            if (lz == SHOP_Z0 + 1 && lx >= 46 && lx <= 66) {
                w.set(x, G + 1, z, OutsideKit.facing("gs_cooler_" + Math.floorMod(lx, 3), Direction.SOUTH));
                w.set(x, G + 2, z, OutsideKit.facing("gs_cooler_" + Math.floorMod(lx + 1, 3), Direction.SOUTH));
            }
            if ((lz == 27 || lz == 30) && lx >= 47 && lx <= 58 && lx != 52) {
                BlockState shelf = OutsideKit.facing("metal_shelf", Direction.NORTH);
                w.set(x, G + 1, z, shelf);
                w.set(x, G + 2, z, shelf);
            }
            if (lz == 31 && lx >= 65 && lx <= 71) {
                w.set(x, G + 1, z, OutsideKit.facing(lx == 68 ? "gs_register" : "gs_counter", Direction.SOUTH));
            }
            if (lz == 33 && lx == 72 && Hash.unit(s, 1, 2, 3) < 2f) w.set(x, G + 1, z, OutsideKit.facing("cardboard_boxes", Direction.WEST));
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Keeps each player inside their own lap of the loop and changes the station between laps. */
    static final class Loop implements LevelBehavior {
        static final class St {
            int ox, oz, laps;
            boolean init;
            UUID attendant;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            home(st, p);
            states.put(p.getUUID(), st);
            if (OutsideKit.autotest()) st.laps = Integer.parseInt(System.getenv().getOrDefault("NOCLIP_AUTOTEST_LAPS", "0"));
            apply(level, st);
            if (OutsideKit.autotest()) {
                p.teleportTo(level, st.ox + 62.5, G + 1, st.oz + 58.5, 180f, 0f);
            }
        }

        static void home(St st, ServerPlayer p) {
            st.ox = Math.floorDiv(p.getBlockX(), P) * P;
            st.oz = Math.floorDiv(p.getBlockZ(), P) * P;
            st.init = true;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            if (!st.init) {
                home(st, p);
                apply(level, st);
            }
            double rx = p.getX() - st.ox, rz = p.getZ() - st.oz;
            double dx = rx < 0 ? P : rx >= P ? -P : 0;
            double dz = rz < 0 ? P : rz >= P ? -P : 0;
            if (dx != 0 || dz != 0) {
                OutsideKit.shift(p, dx, 0, dz);
                st.laps++;
                apply(level, st);
                // you hear the shop door before you see the lights again
                OutsideKit.sound(level, new BlockPos(st.ox + DOOR_X, G + 1, st.oz + SHOP_Z1), "event.gas_station_chime", 5f, 1f);
            }
            if (p.tickCount % 60 == 0) {
                BlockPos phone = new BlockPos(st.ox + PHONE_X, G + 1, st.oz + PHONE_Z);
                BlockState ph = level.getBlockState(phone);
                if (ph.getBlock() instanceof PayphoneBlock && ph.getValue(PayphoneBlock.RINGING)) {
                    OutsideKit.sound(level, phone, "event.gas_station_ring", 3.5f, 1f);
                }
            }
        }

        /** Brings the home station up to date with the number of laps. Only ever runs while the player is far away. */
        void apply(ServerLevel level, St st) {
            int ox = st.ox, oz = st.oz, laps = st.laps;
            OutsideKit.Setter set = OutsideKit.Setter.of(level);
            int v = Math.min(laps, 5);
            for (int side = 0; side < 2; side++) {
                String suf = side == 0 ? "_l" : "_r";
                put(level, ox + SIGN_X + side, G + 7, oz + SIGN_Z, OutsideKit.facing("gs_sign_price" + v + suf, Direction.NORTH));
            }
            if (laps >= 1 && laps < 5) {
                OutsideKit.car(set, ox + 59, G + 1, oz + 44, Direction.NORTH, "beige", laps >= 3);
            }
            if (laps >= 5) OutsideKit.clearCar(set, ox + 59, G + 1, oz + 44, Direction.NORTH);
            if (laps >= 2) {
                // canopy lights die, a few more each lap
                int dead = 0;
                for (int lx = CAN_X0 + 1; lx < CAN_X1; lx++)
                    for (int lz = CAN_Z0 + 1; lz < CAN_Z1; lz++) {
                        if (!canopyLight(lx, lz)) continue;
                        float r = Hash.unit(lx, lz, 77);
                        float limit = laps >= 5 ? 0.85f : laps >= 4 ? 0.6f : 0.2f;
                        if (r < limit) {
                            put(level, ox + lx, G + 6, oz + lz, ModBlocks.state(r < limit - 0.12f ? "gs_canopy_light_dead" : "gs_canopy_light_flicker"));
                            dead++;
                        }
                    }
            }
            if (laps >= 3) {
                for (int i = 0; i < 2; i++) {
                    BlockPos d = new BlockPos(ox + DOOR_X + i, G + 1, oz + SHOP_Z1);
                    BlockState ds = level.getBlockState(d);
                    if (ds.getBlock() instanceof DoorBlock && !ds.getValue(DoorBlock.OPEN)) {
                        level.setBlock(d, ds.setValue(DoorBlock.OPEN, true), Block.UPDATE_ALL);
                        BlockState up = level.getBlockState(d.above());
                        if (up.getBlock() instanceof DoorBlock) level.setBlock(d.above(), up.setValue(DoorBlock.OPEN, true), Block.UPDATE_ALL);
                    }
                }
                BlockPos phone = new BlockPos(ox + PHONE_X, G + 1, oz + PHONE_Z);
                BlockState ph = level.getBlockState(phone);
                if (ph.getBlock() instanceof PayphoneBlock && !ph.getValue(PayphoneBlock.RINGING) && laps % 2 == 1) {
                    level.setBlock(phone, ph.setValue(PayphoneBlock.RINGING, true), Block.UPDATE_ALL);
                }
            }
            if (laps >= 4) {
                GasAttendant a = st.attendant == null ? null : (level.getEntity(st.attendant) instanceof GasAttendant g ? g : null);
                if (a == null || a.isRemoved()) {
                    a = GasAttendant.TYPE.create(level);
                    if (a != null) {
                        a.moveTo(ox + 62.5, G + 1, oz + 47.5, 180f, 0f);
                        level.addFreshEntity(a);
                        st.attendant = a.getUUID();
                    }
                }
                if (a != null) a.setHunting(laps >= 6);
            }
            if (laps >= 5) {
                put(level, ox + SHOP_X1, G + 1, oz + REST_Z, ModBlocks.state("gs_restroom_open"));
                put(level, ox + SHOP_X1, G + 2, oz + REST_Z, ModBlocks.state("gs_restroom_open"));
            }
        }

        static void put(ServerLevel level, int x, int y, int z, BlockState s) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos) != s) level.setBlock(pos, s, Block.UPDATE_ALL);
        }
    }
}
