package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roadside Motel. A road, a parking lot, a strip of doors under a buzzing sign,
 * an alley, a field, and the same again forever. Every door is room 6, and every
 * door opens into the same room 6. The room is a little different each time you
 * come back to it. Leave it and you are somewhere else on the strip.
 */
public final class Motel {
    static final int FLOOR = 10, PZ = 64, SEG = 96, W = 6;
    /** Room 6 lives here, far from anywhere a wanderer can arrive. */
    static final int RX = 0, RZ = -1000;

    private Motel() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("motel")
                .number("").title("Roadside Motel").order(1120)
                .tagline("Every door is room 6.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Strip())
                .visuals(new Visuals().tint(0.95f, 0.97f, 1.08f).saturation(0.9f).contrast(1.12f).brightness(1.3f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.55f).fog(8f, 70f).fogColor(0x0C1018).bloom(0.7f).sky())
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.1f).seen().dist(30, 48).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("skin_stealer"), 1, 0.05f).dark().dist(24, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_9", 2).exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "parking_lot", 1)
                .exit(ExitType.NOCLIP, "dead_mall", 1)
                .exit(ExitType.SECRET, "level_5", 2).exit(ExitType.SECRET, "level_13", 2).exit(ExitType.SECRET, "the_hub", 1)
                .exit(ExitType.DOOR, "endless_highway", 2).exit(ExitType.DOOR, "gas_station", 2).exit(ExitType.DOOR, "level_9", 1)
                .build();
    }

    static boolean reserved(int x, int z) {
        return Math.abs(x - RX) <= 12 && z >= RZ - 12 && z <= RZ + 16;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState asphalt = ModBlocks.state("motel_asphalt");
        final BlockState line = ModBlocks.state("motel_asphalt_line");
        final BlockState yellow = ModBlocks.state("motel_asphalt_yellow");
        final BlockState concrete = ModBlocks.state("motel_concrete");
        final BlockState curb = ModBlocks.state("motel_curb");
        final BlockState brick = ModBlocks.state("motel_brick");
        final BlockState door = ModBlocks.state("motel_door");
        final BlockState doorTop = ModBlocks.state("motel_door_top");
        final BlockState winLit = ModBlocks.state("motel_window_lit");
        final BlockState winDark = ModBlocks.state("motel_window_dark");
        final BlockState soffit = ModBlocks.state("motel_soffit");
        final BlockState bulb = ModBlocks.state("motel_bulb");
        final BlockState fascia = ModBlocks.state("motel_fascia");
        final BlockState gravel = ModBlocks.state("motel_gravel");
        final BlockState pole = ModBlocks.state("motel_pole");
        final BlockState woodPole = ModBlocks.state("motel_wood_pole");
        final BlockState lamp = ModBlocks.state("motel_lamp");
        final BlockState dirt = ModBlocks.state("motel_dirt");
        final BlockState grass = ModBlocks.state("motel_dry_grass");
        final BlockState stop = ModBlocks.state("motel_wheel_stop");
        final BlockState post = ModBlocks.state("motel_post");
        final BlockState ac = ModBlocks.state("motel_ac");
        final BlockState dumpster = ModBlocks.state("motel_dumpster");
        final BlockState ice = ModBlocks.state("motel_ice_machine");
        final BlockState vending = ModBlocks.state("motel_vending");
        final BlockState officeWin = ModBlocks.state("motel_office_window");
        final BlockState officeDoor = ModBlocks.state("motel_office_door");
        final BlockState fence = ModBlocks.state("chain_link_fence").setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] signMotel = new BlockState[3], signVacancy = new BlockState[3], signOffice = new BlockState[3];

        Gen() {
            for (int p = 0; p < 3; p++) {
                signMotel[p] = ModBlocks.state("motel_sign_motel_" + p);
                signVacancy[p] = ModBlocks.state("motel_sign_vacancy_" + p);
                signOffice[p] = ModBlocks.state("motel_sign_office_" + p);
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (reserved(x, z)) {
                        w.fill(x, FLOOR, z, x, FLOOR + 8, z, filler);
                        continue;
                    }
                    column(w, s, x, z);
                }
            room6(w);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int v = Math.floorMod(z, PZ), fx = Math.floorMod(x, SEG), u = Math.floorMod(x, W);
            int room = fx / W;
            int y = FLOOR + 1;
            if (v <= 7) {
                w.set(x, FLOOR, z, v == 3 && Math.floorMod(x, 8) < 4 ? yellow : asphalt);
                return;
            }
            if (v == 8) {
                w.set(x, FLOOR, z, curb);
                return;
            }
            if (v <= 10) {
                w.set(x, FLOOR, z, concrete);
                if (v == 9 && fx == 60) {
                    w.fill(x, y, z, x, y + 5, z, pole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                }
                if (v == 9 && fx >= 59 && fx <= 61) {
                    // the sign, readable from the lot
                    w.set(x, y + 7, z, signMotel[fx - 59]);
                    w.set(x, y + 6, z, signVacancy[fx - 59]);
                }
                return;
            }
            if (v <= 28) {
                boolean stall = v >= 13 && v <= 22 && Math.floorMod(x, 3) == 0;
                w.set(x, FLOOR, z, stall ? line : asphalt);
                if (v == 23 && Math.floorMod(x, 3) == 1 && Hash.chance(s, x, z, 7, 0.7f))
                    w.set(x, y, z, stop.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                if (v == 17 && Math.floorMod(x, 24) == 12) {
                    w.fill(x, y, z, x, y + 6, z, pole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                    w.set(x, y + 7, z, Hash.chance(s, x, z, 9, 0.2f) ? pole : lamp);
                }
                return;
            }
            if (v <= 31) {
                // covered walkway
                w.set(x, FLOOR, z, concrete);
                w.set(x, FLOOR + 4, z, v == 30 && u == 2 && room != 7 ? bulb : soffit);
                w.set(x, FLOOR + 5, z, fascia);
                if (v == 29 && u == 5) w.fill(x, y, z, x, y + 2, z, post);
                if (v == 31 && u == 4 && room != 0 && room != 7)
                    w.set(x, y, z, ac.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                if (v == 29 && room == 0 && u >= 1 && u <= 3) w.set(x, FLOOR + 5, z, signOffice[3 - u]);
                return;
            }
            if (v <= 41) {
                w.set(x, FLOOR, z, concrete);
                w.set(x, FLOOR + 5, z, v == 32 ? fascia : gravel);
                if (room == 7 && v <= 34 && u >= 1) {
                    // the ice-and-vending alcove
                    w.set(x, FLOOR + 4, z, v == 33 && u == 3 ? bulb : soffit);
                    if (v == 34 && u == 2) w.set(x, y, z, ice.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    if (v == 34 && u == 4) w.set(x, y, z, vending.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    return;
                }
                if (v > 32) {
                    w.fill(x, y, z, x, FLOOR + 4, z, brick);
                    return;
                }
                // the front wall: door, window, wall
                w.fill(x, y, z, x, FLOOR + 4, z, brick);
                if (room == 0) {
                    if (u == 1 || u == 2) {
                        w.set(x, y, z, officeDoor);
                        w.set(x, y + 1, z, officeDoor);
                    } else if (u >= 3) {
                        w.set(x, y, z, officeWin);
                        w.fill(x, y + 1, z, x, y + 2, z, winLit);
                    }
                    return;
                }
                if (u == 1) {
                    w.set(x, y, z, door);
                    w.set(x, y + 1, z, doorTop);
                } else if (u == 3 || u == 4) {
                    boolean lit = Hash.chance(s, Math.floorDiv(x, W), Math.floorDiv(z, PZ), 0x11, 0.45f);
                    w.set(x, y + 1, z, lit ? winLit : winDark);
                    w.set(x, y + 2, z, lit ? winLit : winDark);
                }
                return;
            }
            if (v <= 47) {
                w.set(x, FLOOR, z, asphalt);
                if (v == 47) w.fill(x, y, z, x, y + 1, z, fence);
                if (v == 43 && Math.floorMod(x, 48) == 20) w.set(x, y, z, dumpster.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                return;
            }
            w.set(x, FLOOR, z, dirt);
            if (v == 56 && Math.floorMod(x, 32) == 0) {
                w.fill(x, y, z, x, y + 9, z, woodPole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                w.fill(x - 1, y + 8, z, x + 1, y + 8, z, woodPole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
                w.set(x, y + 8, z, woodPole.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            } else if (Hash.chance(s, x, z, 3, 0.3f)) w.set(x, y, z, grass);
        }

        /** Room 6. Local x -3..3, local z 0..12 (bathroom 0..2, bedroom 4..12), door in the front wall at z 13. */
        void room6(ChunkWriter w) {
            if (RX + 12 < w.minX || RX - 12 > w.maxX || RZ + 16 < w.minZ || RZ - 12 > w.maxZ) return;
            BlockState carpet = ModBlocks.state("motel_carpet"), paper = ModBlocks.state("motel_wallpaper");
            BlockState ceil = ModBlocks.state("motel_popcorn"), tile = ModBlocks.state("motel_bathroom_tile");
            int y = FLOOR + 1;
            for (int lx = -4; lx <= 4; lx++)
                for (int lz = -1; lz <= 13; lz++) {
                    int x = RX + lx, z = RZ + lz;
                    boolean bath = lz <= 2;
                    boolean wallCol = Math.abs(lx) == 4 || lz == -1 || lz == 13 || (lz == 3 && lx != -2);
                    w.set(x, FLOOR, z, bath ? tile : carpet);
                    w.set(x, FLOOR + 4, z, ceil);
                    if (wallCol) w.fill(x, y, z, x, FLOOR + 3, z, bath && lz != 3 ? tile : paper);
                    else w.fill(x, y, z, x, FLOOR + 3, z, ModBlocks.AIR);
                }
            w.set(RX - 2, FLOOR + 3, RZ + 3, paper);
            // front wall: the door and the window with the parking lot glowing through the curtains
            w.set(RX + 1, y, RZ + 13, ModBlocks.state("motel_door_inside"));
            w.set(RX + 1, y + 1, RZ + 13, ModBlocks.state("motel_door_inside_top"));
            w.set(RX - 2, y + 1, RZ + 13, ModBlocks.state("motel_window_lit"));
            w.set(RX - 1, y + 1, RZ + 13, ModBlocks.state("motel_window_lit"));
            w.set(RX - 2, y, RZ + 12, ModBlocks.state("motel_ac").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            // bed against the west wall, a lamp and a phone either side
            for (int lz = 7; lz <= 8; lz++)
                for (int lx = -3; lx <= -2; lx++) w.set(RX + lx, y, RZ + lz, ModBlocks.state("motel_bed").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set(RX - 3, y, RZ + 6, ModBlocks.state("motel_lamp_table"));
            w.set(RX - 3, y, RZ + 9, ModBlocks.state("motel_phone").setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            w.set(RX - 4, y + 2, RZ + 7, ModBlocks.state("motel_painting_lake"));
            w.set(RX + 3, y, RZ + 7, ModBlocks.state("motel_tv_off").setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            w.set(RX + 3, y, RZ + 10, ModBlocks.state("motel_lamp_table"));
            // bathroom
            w.set(RX + 1, y, RZ, ModBlocks.state("motel_sink").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set(RX + 1, y + 1, RZ - 1, ModBlocks.state("motel_mirror"));
            w.set(RX + 3, y, RZ, ModBlocks.state("motel_toilet").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set(RX - 1, FLOOR + 3, RZ + 1, ModBlocks.state("motel_bulb"));
        }
    }

    // ---------------------------------------------------------------- room 6

    static final Map<UUID, Integer> VISITS = new ConcurrentHashMap<>();
    static final Map<UUID, BlockPos> LAST_DOOR = new ConcurrentHashMap<>();

    static BlockPos at(int lx, int y, int lz) {
        return new BlockPos(RX + lx, y, RZ + lz);
    }

    public static void enterRoom(ServerPlayer p, BlockPos door) {
        if (!(p.level() instanceof ServerLevel level) || Levels.get(level.dimension()) == null
                || !"motel".equals(Levels.get(level.dimension()).id)) return;
        int n = VISITS.merge(p.getUUID(), 1, Integer::sum);
        LAST_DOOR.put(p.getUUID(), door);
        level.playSound(null, door, ModSounds.get("event.motel_door"), SoundSource.BLOCKS, 1f, 1f);
        level.getChunk(at(0, FLOOR, 6));
        arrange(level, n);
        p.teleportTo(level, RX + 1.5, FLOOR + 1, RZ + 11.5, 180f, 0f);
        level.playSound(null, at(1, FLOOR + 1, 12), ModSounds.get("event.motel_door"), SoundSource.BLOCKS, 0.8f, 0.9f);
        String msg = switch (Math.min(n, 6)) {
            case 1 -> "Room 6.";
            case 2 -> "Room 6. Someone left the TV on.";
            case 3 -> "Room 6. The bed has been slept in.";
            case 4 -> "Room 6. The painting has changed.";
            case 5 -> "Room 6. You are not alone in here.";
            default -> "Room 6. There is a door that was not there before.";
        };
        p.displayClientMessage(Component.literal("§7§o" + msg), true);
        if (n >= 3) level.playSound(null, at(-1, FLOOR + 1, 1), ModSounds.get("event.motel_shower"), SoundSource.AMBIENT, 0.8f, 1f);
        if (n >= 4) level.playSound(null, at(-3, FLOOR + 1, 9), ModSounds.get("event.motel_phone"), SoundSource.BLOCKS, 1f, 1f);
        if (n >= 5) Sanity.add(p, -5f);
    }

    /** Sets room 6 to how it looks on a wanderer's nth visit. */
    static void arrange(ServerLevel level, int n) {
        int y = FLOOR + 1;
        set(level, at(3, y, 7), ModBlocks.state(n >= 5 ? "motel_tv_room" : n >= 2 ? "motel_tv_static" : "motel_tv_off")
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        BlockState bed = ModBlocks.state(n >= 3 ? "motel_bed_unmade" : "motel_bed").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int lz = 7; lz <= 8; lz++)
            for (int lx = -3; lx <= -2; lx++) set(level, at(lx, y, lz), bed);
        set(level, at(-4, y + 2, 7), ModBlocks.state(n >= 4 ? "motel_painting_motel" : "motel_painting_lake"));
        set(level, at(3, y, 10), ModBlocks.state(n >= 5 ? "motel_lamp_table_off" : "motel_lamp_table"));
        boolean seven = n >= 6;
        set(level, at(1, y + 1, -1), seven ? ModBlocks.state("motel_bathroom_tile") : ModBlocks.state("motel_mirror"));
        set(level, at(-2, y, -1), seven ? ModBlocks.state("motel_door_7") : ModBlocks.state("motel_bathroom_tile"));
        set(level, at(-2, y + 1, -1), seven ? ModBlocks.state("motel_door_7") : ModBlocks.state("motel_bathroom_tile"));
    }

    static void set(ServerLevel level, BlockPos pos, BlockState st) {
        if (level.getBlockState(pos) != st) level.setBlock(pos, st, Block.UPDATE_CLIENTS);
    }

    /** Out of room 6 and onto the strip: always another door, somewhere else. */
    public static void leaveRoom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        BlockPos last = LAST_DOOR.getOrDefault(p.getUUID(), new BlockPos(5000, FLOOR + 1, 5000 + 32));
        var r = p.getRandom();
        int seg = Math.floorDiv(last.getX(), SEG) + (r.nextBoolean() ? 1 : -1) * (3 + r.nextInt(20));
        int room = 1 + r.nextInt(15);
        if (room == 7) room = 8;
        int x = seg * SEG + room * W + 1;
        int row = Math.floorDiv(last.getZ(), PZ) + r.nextInt(7) - 3;
        int z = row * PZ + 31;
        if (reserved(x, z)) x += SEG * 4;
        level.getChunk(new BlockPos(x, FLOOR, z));
        level.playSound(null, at(1, FLOOR + 1, 12), ModSounds.get("event.motel_door"), SoundSource.BLOCKS, 0.8f, 1.1f);
        p.teleportTo(level, x + 0.5, FLOOR + 1, z + 0.5, 180f, 0f);
        level.playSound(null, new BlockPos(x, FLOOR + 1, z), ModSounds.get("event.motel_door"), SoundSource.BLOCKS, 0.8f, 1f);
        LAST_DOOR.put(p.getUUID(), new BlockPos(x, FLOOR + 1, z + 1));
    }

    static final class Strip implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            VISITS.remove(p.getUUID());
            LAST_DOOR.put(p.getUUID(), p.blockPosition());
        }
    }
}
