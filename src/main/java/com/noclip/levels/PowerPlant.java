package com.noclip.levels;

import com.noclip.block.AlarmBeaconBlock;
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
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Power Plant. Turbine halls like cathedrals, green casings humming at fifty hertz,
 * warning beacons on every machine. Between them, control rooms where every annunciator
 * window is lit and the chairs are pushed back as if the shift just left. Every few
 * minutes a fault alarm sounds through the whole plant. Find a control room and press
 * ACK before the minute is up, or the turbines trip and the plant goes black.
 */
public final class PowerPlant {
    static final String ID = "power_plant";
    static final int FLOOR = 8, CEIL = 26, LOW = FLOOR + 6, CELL = 48, ALARM_TICKS = 1200;

    private PowerPlant() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Power Plant").order(2130)
                .tagline("Every alarm is ringing. Nobody has answered one in years. Answer it.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Alarms())
                .visuals(new Visuals().tint(0.94f, 1.04f, 0.96f).saturation(0.74f).contrast(1.12f).brightness(1.3f)
                        .grain(0.07f).aberration(0.0015f).vignette(0.5f).fog(5f, 50f).fogColor(0x1C2420).bloom(0.6f)
                        .scanlines(0.05f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 3, 0.6f).dark().dist(10, 24).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(26, 42).weight(1))
                .exit(ExitType.DOOR, "level_3", 2).exit(ExitType.DOOR, "dam", 2).exit(ExitType.DOOR, "server_farm", 1)
                .exit(ExitType.FALL, "level_2", 2).exit(ExitType.FALL, "boiler_room", 1)
                .exit(ExitType.NOCLIP, "level_3", 3).exit(ExitType.NOCLIP, "steel_mill", 1).exit(ExitType.NOCLIP, "dam", 1)
                .build();
    }

    static CellGrid grid(long s) {
        return new CellGrid(s, CELL, 0);
    }

    static boolean control(CellGrid g, int i, int k) {
        return g.unit(i, k, 11) < 0.25f;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("pwr_floor");
        final BlockState hazardFloor = ModBlocks.state("pwr_floor_hazard");
        final BlockState walk = ModBlocks.state("pwr_floor_walk");
        final BlockState wall = ModBlocks.state("pwr_wall");
        final BlockState wallLow = ModBlocks.state("pwr_wall_low");
        final BlockState signT = ModBlocks.state("pwr_sign_turbine");
        final BlockState signE = ModBlocks.state("pwr_sign_ears");
        final BlockState casing = ModBlocks.state("pwr_casing");
        final BlockState casingTop = ModBlocks.state("pwr_casing_top");
        final BlockState gen = ModBlocks.state("pwr_generator");
        final BlockState genLabel = ModBlocks.state("pwr_generator_label");
        final BlockState hallLamp = ModBlocks.state("pwr_hall_lamp");
        final BlockState plinth = ModBlocks.state("pwr_plinth");
        final BlockState pipe = ModBlocks.state("pwr_pipe");
        final BlockState panel = ModBlocks.state("pwr_panel");
        final BlockState panelB = ModBlocks.state("pwr_panel_b");
        final BlockState annun = ModBlocks.state("pwr_annunciator");
        final BlockState desk = ModBlocks.state("pwr_desk");
        final BlockState deskOdd = ModBlocks.state("pwr_desk_odd");
        final BlockState ack = ModBlocks.state("pwr_ack");
        final BlockState ceiling = ModBlocks.state("pwr_ceiling");
        final BlockState light = ModBlocks.state("pwr_light");
        final BlockState flood = ModBlocks.state("pwr_flood");
        final BlockState beacon = ModBlocks.state("pwr_beacon");
        final BlockState door = ModBlocks.state("pwr_exit_door");
        final BlockState grate = ModBlocks.state("pwr_grate");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, g, s, x, z);
        }

        void column(ChunkWriter w, CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int lx = x - g.x0(i), lz = z - g.z0(k);
            boolean ctrl = control(g, i, k);
            w.set(x, FLOOR, z, floor);
            // corridors on the cell borders, walled on both sides
            if (lx <= 2 || lz <= 2) {
                w.set(x, LOW - 1, z, (lx == 1 && Math.floorMod(lz, 6) == 3) || (lz == 1 && Math.floorMod(lx, 6) == 3) ? light : ceiling);
                w.fill(x, LOW, z, x, CEIL - 1, z, filler);
                w.set(x, FLOOR, z, walk);
                return;
            }
            boolean wallX = lx == 3 || lx == CELL - 1, wallZ = lz == 3 || lz == CELL - 1;
            if (wallX || wallZ) {
                int along = wallX ? lz : lx;
                boolean doorway = along >= 22 && along <= 24 && !(wallX && wallZ);
                for (int y = FLOOR + 1; y < CEIL; y++) {
                    if (doorway && y <= FLOOR + 3) continue;
                    BlockState b = y <= FLOOR + 2 ? wallLow : wall;
                    if (y == FLOOR + 4 && along == 20) b = ctrl ? signE : signT;
                    w.set(x, y, z, b);
                }
                if (ctrl && lz == CELL - 1 && lx == 30 && g.unit(i, k, 12) < 0.45f) {
                    w.set(x, FLOOR + 1, z, door);
                    w.set(x, FLOOR + 2, z, door);
                }
                // a floodlight on the hall side of the wall
                if (!ctrl && !doorway && Math.floorMod(along, 7) == 3 && !(wallX && wallZ)) {
                    Direction face = lx == 3 ? Direction.WEST : lx == CELL - 1 ? Direction.EAST : lz == 3 ? Direction.NORTH : Direction.SOUTH;
                    int ox = x - face.getStepX(), oz = z - face.getStepZ();
                    if (Hash.unit(s, x, z, 13) > 0.15f) w.set(ox, FLOOR + 7, oz, OutsideKit.facing(flood, face));
                }
                return;
            }
            if (ctrl) controlRoom(w, g, s, x, z, i, k, lx, lz);
            else hall(w, g, s, x, z, i, k, lx, lz);
        }

        void controlRoom(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int lx, int lz) {
            w.set(x, FLOOR, z, floor);
            w.set(x, LOW, z, Math.floorMod(lx, 4) == 2 && Math.floorMod(lz, 4) == 2 && Hash.unit(s, x, z, 21) > 0.12f ? light : ceiling);
            w.fill(x, LOW + 1, z, x, CEIL - 1, z, filler);
            // the board: a wall of panels and annunciators along the north side
            if (lz == 4 && lx >= 5 && lx <= CELL - 3) {
                for (int y = FLOOR + 1; y < LOW; y++) {
                    BlockState b = y >= FLOOR + 3 ? annun : (Math.floorMod(lx, 3) == 0 ? panelB : panel);
                    w.set(x, y, z, b);
                }
                return;
            }
            // two curved rows of operator desks facing the board
            for (int row : new int[]{12, 20}) {
                int bend = Math.abs(lx - 25) / 7;
                if (lz == row + bend && lx >= 8 && lx <= 42) {
                    boolean isAck = row == 12 && (lx == 24 || lx == 25);
                    float r = Hash.unit(s, x, z, 22);
                    w.set(x, FLOOR + 1, z, OutsideKit.facing(isAck ? ack : r < 0.02f ? deskOdd : desk, Direction.NORTH));
                    return;
                }
                if (lz == row + bend + 1 && lx >= 8 && lx <= 42 && Math.floorMod(lx, 3) == 1) {
                    float r = Hash.unit(s, x, z, 23);
                    if (r < 0.6f) w.set(x, FLOOR + 1, z, OutsideKit.facing(chair, r < 0.1f ? Direction.SOUTH : Direction.NORTH));
                    return;
                }
            }
            // a second ACK by the door, in case
            if (lx == 22 && lz == CELL - 3) w.set(x, FLOOR + 1, z, OutsideKit.facing(ack, Direction.SOUTH));
        }

        void hall(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int lx, int lz) {
            // hanging lamps over the hall
            if (Math.floorMod(lx, 6) == 0 && Math.floorMod(lz, 6) == 1) {
                w.fill(x, FLOOR + 11, z, x, CEIL - 1, z, chain);
                if (Hash.unit(s, x, z, 31) > 0.1f) w.set(x, FLOOR + 10, z, hallLamp);
            }
            // steam mains running up the walls
            if ((lx == 4 || lx == CELL - 2) && Math.floorMod(lz, 10) == 6 || (lz == 4 || lz == CELL - 2) && Math.floorMod(lx, 10) == 6) {
                w.fill(x, FLOOR + 1, z, x, CEIL - 1, z, pipe.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                return;
            }
            if (lz == 20 || lz == 30) w.set(x, FLOOR, z, walk);
            // the turbine: a long green casing on a plinth, the generator at the east end
            if (lz >= 22 && lz <= 28 && lx >= 8 && lx <= 42) {
                w.set(x, FLOOR + 1, z, plinth);
                int dz = Math.abs(lz - 25);
                boolean genEnd = lx >= 36;
                int top = dz == 3 ? 0 : (genEnd ? 6 : 5) - dz;
                for (int y = FLOOR + 2; y < FLOOR + 2 + top; y++) {
                    boolean last = y == FLOOR + 1 + top;
                    w.set(x, y, z, genEnd ? (lx == 42 && dz == 0 && y == FLOOR + 3 ? genLabel : gen) : last ? casingTop : casing);
                }
                if (dz == 0 && Math.floorMod(lx, 12) == 2) w.set(x, FLOOR + 2 + top, z, beacon);
                return;
            }
            // the pit hatch: an open grate by the plinth, a long drop under the plant
            if (g.unit(i, k, 14) < 0.3f && lz >= 16 && lz <= 18 && lx >= 12 && lx <= 14) {
                if (lz == 17 && lx == 13) {
                    w.fill(x, FLOOR - 5, z, x, FLOOR, z, ModBlocks.AIR);
                    w.set(x, FLOOR - 6, z, pit);
                } else w.set(x, FLOOR, z, hazardFloor);
                return;
            }
            // grating strips over cable trenches
            if (lx >= 8 && lx <= 42 && (lz == 21 || lz == 29)) w.set(x, FLOOR, z, grate);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** {phase 0 idle / 1 alarm, deadline, next alarm, alarms answered} */
    private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();

    static long[] state(ServerLevel level) {
        return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 1600 + level.random.nextInt(1600), 0});
    }

    static final String[] FAULTS = {"TURBINE 4 OVERSPEED", "GEN 2 STATOR TEMP HIGH", "FEEDWATER PUMP B TRIP",
            "CONDENSER VACUUM LOW", "BEARING 7 VIBRATION", "EXCITER FAULT", "LUBE OIL PRESSURE LOW"};

    /** Called by the ACK console. */
    public static void acknowledge(ServerLevel level, BlockPos pos, Player player) {
        long[] st = state(level);
        if (st[0] != 1) {
            level.playSound(null, pos, ModSounds.get("event.power_plant_ack"), SoundSource.BLOCKS, 0.6f, 0.7f);
            player.displayClientMessage(Component.literal("NO ACTIVE ALARMS. " + st[3] + " ACKNOWLEDGED THIS SHIFT.")
                    .withStyle(ChatFormatting.GREEN), true);
            return;
        }
        st[0] = 0;
        st[3]++;
        st[2] = level.getGameTime() + 2400 + level.random.nextInt(2400);
        beacons(level, false);
        level.playSound(null, pos, ModSounds.get("event.power_plant_ack"), SoundSource.BLOCKS, 1f, 1f);
        String note = st[3] == 1 ? "ACKNOWLEDGED. FIRST ANSWER IN 9,412 DAYS."
                : st[3] % 3 == 0 ? "ACKNOWLEDGED. RELIEF SHIFT NOTIFIED. USE THE STAFF EXIT."
                : "ACKNOWLEDGED. THANK YOU, OPERATOR.";
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.literal(note).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            if (p == player) Sanity.add(p, 6f);
        }
    }

    static void beacons(ServerLevel level, boolean on) {
        Block beacon = ModBlocks.get("pwr_beacon");
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (ServerPlayer p : level.players()) {
            for (int x = p.getBlockX() - 48; x <= p.getBlockX() + 48; x++)
                for (int z = p.getBlockZ() - 48; z <= p.getBlockZ() + 48; z++)
                    for (int y = FLOOR + 6; y <= FLOOR + 8; y++) {
                        m.set(x, y, z);
                        if (!level.isLoaded(m)) continue;
                        BlockState b = level.getBlockState(m);
                        if (b.is(beacon) && b.getValue(AlarmBeaconBlock.LIT) != on)
                            level.setBlock(m, b.setValue(AlarmBeaconBlock.LIT, on), Block.UPDATE_CLIENTS);
                    }
        }
    }

    static final class Alarms implements LevelBehavior {
        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0) {
                if (now < st[2] || level.players().isEmpty()) return;
                st[0] = 1;
                st[1] = now + ALARM_TICKS;
                st[2] = Hash.range(now, FAULTS.length);
                beacons(level, true);
                return;
            }
            if (now % 40 == 0) {
                long left = (st[1] - now) / 20;
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.power_plant_klaxon"), SoundSource.BLOCKS, 1.2f, 1f);
                    p.displayClientMessage(Component.literal(FAULTS[(int) st[2]] + " - ACKNOWLEDGE AT CONTROL - " + left + " S")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                }
            }
            if (now < st[1]) return;
            // nobody answered: the plant trips
            st[0] = 0;
            st[2] = now + 3000 + level.random.nextInt(2400);
            beacons(level, false);
            Blackouts.start(level, 700);
            for (ServerPlayer p : level.players()) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.power_plant_trip"), SoundSource.BLOCKS, 1.8f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 1f));
                Sanity.add(p, -10f);
                p.displayClientMessage(Component.literal("UNIT TRIP. NO OPERATOR RESPONSE.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            }
        }
    }
}
