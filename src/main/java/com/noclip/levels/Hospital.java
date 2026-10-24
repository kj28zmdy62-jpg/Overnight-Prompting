package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hospital Ward. Low corridors with failing lights; wards of curtained bays you
 * cannot see into. Every heart monitor beeps in time with your own pulse, and
 * your pulse is your sanity: calm and slow, or racing, or flat. When it races,
 * the corridor lights stutter with it and the patients can hear it through the
 * walls.
 */
public final class Hospital {
    static final int FLOOR = 10, CEIL = FLOOR + 4, PZ = 25, PX = 14;

    private Hospital() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("hospital")
                .number("").title("Hospital Ward").order(1030)
                .tagline("Visiting hours are over. Keep your heart rate down.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Pulse())
                .visuals(new Visuals().tint(0.94f, 1.04f, 0.98f).saturation(0.72f).contrast(1.1f).brightness(1.12f)
                        .grain(0.08f).aberration(0.0017f).vignette(0.6f).fog(4f, 40f).fogColor(0x2A3430).bloom(0.5f).flicker(0.03f))
                .sanityDrain(0.04f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("ward_patient"), 3, 0.35f).dist(14, 30).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(26, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "waiting_room", 2).exit(ExitType.NOCLIP, "school", 1)
                .exit(ExitType.NOCLIP, "level_1", 1)
                .exit(ExitType.DESCEND, "cold_storage", 2).exit(ExitType.DESCEND, "lift_lobby", 2).exit(ExitType.DESCEND, "level_2", 1)
                .exit(ExitType.DOOR, "parking_garage", 2).exit(ExitType.DOOR, "waiting_room", 1).exit(ExitType.DOOR, "level_1", 1)
                .build();
    }

    static boolean service(int sx) {
        return Math.floorMod(sx, 5) == 0;
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("hosp_floor");
        final BlockState stripe = ModBlocks.state("hosp_floor_stripe");
        final BlockState stripeRed = ModBlocks.state("hosp_floor_stripe_red");
        final BlockState wall = ModBlocks.state("hosp_wall");
        final BlockState rail = ModBlocks.state("hosp_wall_rail");
        final BlockState ceil = ModBlocks.state("hosp_ceiling");
        final BlockState track = ModBlocks.state("hosp_ceiling_track");
        final BlockState light = ModBlocks.state("hosp_troffer");
        final BlockState flicker = ModBlocks.state("hosp_troffer_flicker");
        final BlockState curtain = ModBlocks.state("hosp_curtain");
        final BlockState curtainTop = ModBlocks.state("hosp_curtain_top");
        final BlockState head = ModBlocks.state("hosp_bed_head");
        final BlockState foot = ModBlocks.state("hosp_bed_foot");
        final BlockState footUnmade = ModBlocks.state("hosp_bed_foot_unmade");
        final BlockState headOcc = ModBlocks.state("hosp_bed_head_occupied");
        final BlockState footOcc = ModBlocks.state("hosp_bed_foot_occupied");
        final BlockState monitor = ModBlocks.state("hosp_monitor");
        final BlockState monitorFlat = ModBlocks.state("hosp_monitor_flat");
        final BlockState iv = ModBlocks.state("hosp_iv_pole");
        final BlockState gurney = ModBlocks.state("hosp_gurney");
        final BlockState wheelchair = ModBlocks.state("hosp_wheelchair");
        final BlockState chair = ModBlocks.state("hosp_waiting_chair");
        final BlockState counter = ModBlocks.state("hosp_nurse_counter");
        final BlockState cart = ModBlocks.state("hosp_medicine_cart");
        final BlockState signWard = ModBlocks.state("hosp_sign_ward");
        final BlockState signIcu = ModBlocks.state("hosp_sign_icu");
        final BlockState signLifts = ModBlocks.state("hosp_sign_lifts");
        final BlockState panel = ModBlocks.state("hosp_elevator_panel");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState door = ModBlocks.state("dark_doorway");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] lift = {ModBlocks.state("hosp_elevator_0"), ModBlocks.state("hosp_elevator_1")};
        final BlockState[] liftTop = {ModBlocks.state("hosp_elevator_top_0"), ModBlocks.state("hosp_elevator_top_1")};

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            for (int sx = Math.floorDiv(w.minX, PX) - 1; sx <= Math.floorDiv(w.maxX, PX) + 1; sx++)
                for (int r = Math.floorDiv(w.minZ, PZ) - 1; r <= Math.floorDiv(w.maxZ, PZ) + 1; r++) {
                    if (service(sx)) serviceSlot(w, s, sx, r);
                    else {
                        ward(w, s, sx, r, true);
                        ward(w, s, sx, r, false);
                    }
                    corridor(w, s, sx, r);
                }
        }

        void solid(ChunkWriter w, int x, int z) {
            w.set(x, FLOOR + 1, z, rail);
            w.fill(x, FLOOR + 2, z, x, CEIL, z, wall);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int v = Math.floorMod(z, PZ), u = Math.floorMod(x, PX);
            int sx = Math.floorDiv(x, PX);
            boolean svc = service(sx);
            w.set(x, FLOOR, z, floor);
            w.set(x, CEIL, z, ceil);
            boolean corridor = v >= 11 && v <= 14;
            if (svc && u >= 5 && u <= 8) {
                // cross corridor
                if (u == 6 && !corridor) w.set(x, FLOOR, z, stripeRed);
                if (u == 6 && Math.floorMod(z, 3) == 0) w.set(x, CEIL, z, lamp(s, x, z));
                return;
            }
            if (corridor) {
                if (v == 12) w.set(x, FLOOR, z, svc && u >= 5 && u <= 8 ? stripeRed : stripe);
                if (v == 13 && Math.floorMod(x, 3) == 0) w.set(x, CEIL, z, lamp(s, x, z));
                return;
            }
            if (v == 0 || v == 10 || v == 15 || u == 0) {
                solid(w, x, z);
                return;
            }
            if (svc) {
                // nurses' station (north-west), elevators (south-west), solid elsewhere: filled in serviceSlot
                boolean station = v <= 9 && u <= 4;
                if (station) {
                    if (u == 2 && v == 5) w.set(x, CEIL, z, light);
                    return;
                }
                solid(w, x, z);
                return;
            }
            // ward interior: curtain tracks over the curtain lines, a few lights over the aisle
            int wv = v <= 9 ? v : v - 15;
            boolean curtainLine = ((wv == 4 || wv == 6) && u <= 12) || ((u == 4 || u == 8 || u == 12) && wv != 5);
            if (curtainLine) w.set(x, CEIL, z, track);
            if (wv == 5 && (u == 2 || u == 6 || u == 10)) {
                float r = Hash.unit(s, x, z, 0x55);
                w.set(x, CEIL, z, r < 0.55f ? light.setValue(BlockStateProperties.LIT, false) : r < 0.75f ? flicker : light);
            }
        }

        BlockState lamp(long s, int x, int z) {
            float dark = Hash.noise(s ^ 0xD4, x / 36.0, z / 36.0);
            float r = Hash.unit(s, x, z, 0x11);
            if (dark > 0.66f) return r < 0.25f ? flicker : light.setValue(BlockStateProperties.LIT, false);
            if (r < 0.18f) return light.setValue(BlockStateProperties.LIT, false);
            if (r < 0.5f) return flicker;
            return light;
        }

        void ward(ChunkWriter w, long s, int sx, int r, boolean north) {
            int x0 = sx * PX, base = r * PZ + (north ? 0 : 15);
            long h = Hash.hash(s, sx, r * 2 + (north ? 0 : 1), 0x3A2D);
            int y = FLOOR + 1;
            // curtains: fronts along the aisle and dividers between bays
            for (int u = 1; u <= 12; u++)
                for (int wv : new int[]{4, 6}) hang(w, x0 + u, base + wv, Direction.NORTH);
            for (int u : new int[]{4, 8, 12})
                for (int wv = 1; wv <= 9; wv++)
                    if (wv != 5) hang(w, x0 + u, base + wv, Direction.EAST);
            // doorway from the corridor, with a sign over it
            int dz = north ? base + 10 : base;
            w.set(x0 + 13, y, dz, ModBlocks.AIR);
            w.set(x0 + 13, y + 1, dz, ModBlocks.AIR);
            w.set(x0 + 13, y + 2, dz, Hash.unit(h) < 0.2f ? signIcu : signWard);
            // six bays
            for (int a : new int[]{1, 5, 9})
                for (int row = 0; row < 2; row++) bay(w, x0 + a, base, row == 0, Hash.hash(h, a, row));
        }

        void hang(ChunkWriter w, int x, int z, Direction d) {
            w.set(x, FLOOR + 1, z, curtain.setValue(HorizontalDirectionalBlock.FACING, d));
            w.set(x, FLOOR + 2, z, curtain.setValue(HorizontalDirectionalBlock.FACING, d));
            w.set(x, FLOOR + 3, z, curtainTop.setValue(HorizontalDirectionalBlock.FACING, d));
        }

        /** One curtained bay: 3x3 behind the curtain, bed against the wall. */
        void bay(ChunkWriter w, int a, int base, boolean northRow, long h) {
            int y = FLOOR + 1;
            int headZ = northRow ? base + 1 : base + 9;
            int footZ = northRow ? base + 2 : base + 8;
            Direction f = northRow ? Direction.NORTH : Direction.SOUTH;
            float r = Hash.unit(h);
            int bx = a + 1;
            boolean bed = true;
            BlockState hd = head, ft = foot;
            BlockState mon = monitor;
            if (r < 0.12f) {
                hd = headOcc;
                ft = footOcc;
            } else if (r < 0.30f) {
                ft = footUnmade;
            } else if (r < 0.34f) {
                bed = false;  // the bed has been taken away; the monitor is still running
            } else if (r < 0.36f) {
                mon = monitorFlat;
            }
            if (bed) {
                w.set(bx, y, headZ, hd.setValue(HorizontalDirectionalBlock.FACING, f));
                w.set(bx, y, footZ, ft.setValue(HorizontalDirectionalBlock.FACING, f));
            }
            w.set(a, y, headZ, mon.setValue(HorizontalDirectionalBlock.FACING, f.getOpposite()));
            if (Hash.unit(h >>> 9) < 0.7f) w.set(a + 2, y, headZ, iv.setValue(HorizontalDirectionalBlock.FACING, f));
            float r2 = Hash.unit(h >>> 20);
            if (r2 < 0.05f) w.set(a + 2, y, footZ + (northRow ? 1 : -1), bottle.setValue(HorizontalDirectionalBlock.FACING, f));
            else if (r2 < 0.08f) w.set(a, y, footZ, wheelchair.setValue(HorizontalDirectionalBlock.FACING, f.getOpposite()));
        }

        void serviceSlot(ChunkWriter w, long s, int sx, int r) {
            int x0 = sx * PX, z0 = r * PZ;
            long h = Hash.hash(s, sx, r, 0x5E7);
            int y = FLOOR + 1;
            // nurses' station: an alcove off the cross corridor behind a waist-high counter
            for (int v = 1; v <= 9; v++) w.set(x0 + 4, y, z0 + v, v == 5 ? ModBlocks.AIR : counter);
            w.set(x0 + 2, y, z0 + 3, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            w.set(x0 + 2, y, z0 + 7, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            w.set(x0 + 3, y, z0 + 2, monitor.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            if (Hash.unit(h) < 0.6f) w.set(x0 + 1, y, z0 + 8, cart.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            // the lifts: two pairs of steel doors on the cross corridor
            for (int pair = 0; pair < 2; pair++) {
                int vz = z0 + 17 + pair * 4;
                for (int p = 0; p < 2; p++) {
                    w.set(x0 + 4, y, vz + p, lift[p]);
                    w.set(x0 + 4, y + 1, vz + p, liftTop[p]);
                }
                w.set(x0 + 4, y + 2, vz, panel);
                w.set(x0 + 4, y + 2, vz + 1, signLifts);
            }
            // and sometimes, across the corridor, a way out
            if (Hash.unit(h >>> 10) < 0.35f) {
                int vz = z0 + 20;
                w.set(x0 + 9, y, vz, door);
                w.set(x0 + 9, y + 1, vz, door);
                w.set(x0 + 8, y + 2, vz, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            }
            for (int v = 16; v <= 24; v += 2)
                if (Hash.unit(h >>> (v + 20)) < 0.6f && (v < 19 || v > 21))
                    w.set(x0 + 8, y, z0 + v, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        }

        void corridor(ChunkWriter w, long s, int sx, int r) {
            int x0 = sx * PX, z0 = r * PZ;
            int y = FLOOR + 1;
            for (int u = 1; u < PX; u++) {
                if (u == 13 || (service(sx) && u >= 4 && u <= 9)) continue;
                long h = Hash.hash(s, x0 + u, r, 0xC0E);
                float f = Hash.unit(h);
                int side = (h & 1) == 0 ? 11 : 14;
                if (f < 0.05f) w.set(x0 + u, y, z0 + side, gurney.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                else if (f < 0.07f) w.set(x0 + u, y, z0 + side, wheelchair.setValue(HorizontalDirectionalBlock.FACING, side == 11 ? Direction.SOUTH : Direction.NORTH));
                else if (f < 0.09f) w.set(x0 + u, y, z0 + side, iv.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                else if (f < 0.092f) {
                    // a gurney left across the corridor, sheet still warm
                    w.set(x0 + u, y, z0 + 12, gurney.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    w.set(x0 + u, y, z0 + 13, gurney.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the pulse

    static final class Pulse implements LevelBehavior {
        private final Map<UUID, Long> next = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            Long due = next.get(p.getUUID());
            if (due != null && now < due && due - now < 200) return;
            float s = PlayerData.sanity(p);
            if (s < 10f) {
                p.playNotifySound(ModSounds.get("event.hospital_flatline"), SoundSource.AMBIENT, 0.45f, 1f);
                next.put(p.getUUID(), now + 60);
                stutter(p, level, 10);
                return;
            }
            int interval = (int) (6 + s * 0.18f);
            next.put(p.getUUID(), now + interval);
            p.playNotifySound(ModSounds.get("event.hospital_beep"), SoundSource.AMBIENT, 0.3f, 1f + (100f - s) / 420f);
            if (s < 45f) stutter(p, level, s < 25f ? 6 : 3);
        }

        /** The corridor lights near you cut out for a moment, in time with your heart. */
        static void stutter(ServerPlayer p, ServerLevel level, int tries) {
            Block a = ModBlocks.get("hosp_troffer"), b = ModBlocks.get("hosp_troffer_flicker");
            for (int i = 0; i < tries; i++) {
                BlockPos pos = new BlockPos(p.getBlockX() + p.getRandom().nextInt(13) - 6, CEIL, p.getBlockZ() + p.getRandom().nextInt(13) - 6);
                BlockState st = level.getBlockState(pos);
                if ((st.is(a) || st.is(b)) && st.getValue(BlockStateProperties.LIT)) {
                    level.setBlock(pos, st.setValue(BlockStateProperties.LIT, false), Block.UPDATE_CLIENTS);
                    level.scheduleTick(pos, st.getBlock(), 3 + p.getRandom().nextInt(4));
                }
            }
        }
    }
}
