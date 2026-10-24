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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Movie Theater. A multiplex corridor of numbered auditoriums. In each one the
 * film is a shot of the seats, taken from the screen, and a few seconds after you
 * sit down you are in it. Stay seated long enough and the film ends: THE END,
 * and the exit beside the screen unlocks. Wander the aisles during the film and
 * the audience you cannot see will shush you.
 */
public final class MovieTheater {
    static final int FLOOR = 10, CEIL = FLOOR + 16, PX = 32, PZ = 40, FILM = 600;

    private MovieTheater() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("movie_theater")
                .number("").title("Movie Theater").order(1100)
                .tagline("Please take your seat. The film is about you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Screening())
                .visuals(new Visuals().tint(1.04f, 0.96f, 1.0f).saturation(0.9f).contrast(1.1f).brightness(1.45f)
                        .grain(0.1f).aberration(0.0018f).vignette(0.65f).fog(6f, 60f).fogColor(0x0A060C).bloom(0.7f).flicker(0.03f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 8)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.45f).dark().dist(12, 26).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(20, 36).weight(2))
                .exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "dead_mall", 2).exit(ExitType.NOCLIP, "bowling_alley", 1)
                .exit(ExitType.DOOR, "drive_in", 2).exit(ExitType.DOOR, "dead_mall", 1).exit(ExitType.DOOR, "arcade", 1)
                .exit(ExitType.DOOR, "level_0", 1)
                .build();
    }

    static int floorAt(int v) {
        if (v < 12) return FLOOR;
        int r = (v - 12) / 2;
        return FLOOR + (r + 1) / 2;
    }

    static boolean seat(int u, int v) {
        return v >= 12 && v <= 38 && u >= 4 && u <= 27;
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("mt_carpet");
        final BlockState aud = ModBlocks.state("mt_carpet_aud");
        final BlockState sticky = ModBlocks.state("mt_sticky");
        final BlockState steps = ModBlocks.state("mt_steps");
        final BlockState fabric = ModBlocks.state("mt_wall_fabric");
        final BlockState wallC = ModBlocks.state("mt_wall_corridor");
        final BlockState screenA = ModBlocks.state("mt_screen");
        final BlockState screenB = ModBlocks.state("mt_screen_b");
        final BlockState seatB = ModBlocks.state("mt_seat");
        final BlockState spill = ModBlocks.state("mt_popcorn_spill");
        final BlockState machine = ModBlocks.state("mt_popcorn_machine");
        final BlockState box = ModBlocks.state("mt_popcorn_box");
        final BlockState booth = ModBlocks.state("mt_booth_window");
        final BlockState locked = ModBlocks.state("mt_exit_locked");
        final BlockState lockedTop = ModBlocks.state("mt_exit_locked_top");
        final BlockState counter = ModBlocks.state("mt_counter");
        final BlockState doorTop = ModBlocks.state("mt_door_top");
        final BlockState light = ModBlocks.state("fluorescent_light");
        final BlockState flicker = ModBlocks.state("flickering_fluorescent_light");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] numbers = new BlockState[13], posters = new BlockState[4];

        Gen() {
            for (int n = 1; n < 13; n++) numbers[n] = ModBlocks.state("mt_number_" + n);
            for (int n = 0; n < 4; n++) posters[n] = ModBlocks.state("mt_poster_" + n);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            for (int i = Math.floorDiv(w.minX, PX) - 1; i <= Math.floorDiv(w.maxX, PX) + 1; i++)
                for (int k = Math.floorDiv(w.minZ, PZ) - 1; k <= Math.floorDiv(w.maxZ, PZ) + 1; k++) fittings(w, s, i, k);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int u = Math.floorMod(x, PX), v = Math.floorMod(z, PZ);
            int y = FLOOR + 1;
            if (v <= 5) {
                w.set(x, FLOOR, z, Hash.chance(s, x, z, 0x57, 0.04f) ? sticky : carpet);
                w.set(x, FLOOR + 5, z, v == 3 && Math.floorMod(x, 8) == 4 ? (Hash.chance(s, x, z, 0x1F, 0.3f) ? flicker : light) : fabric);
                w.fill(x, FLOOR + 6, z, x, CEIL, z, filler);
                if (Hash.chance(s, x, z, 0x99, 0.01f)) w.set(x, y, z, spill.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                return;
            }
            if (v == 6) {
                // the screen wall: corridor face below, the screen above on the inside
                w.set(x, FLOOR, z, carpet);
                w.fill(x, y, z, x, FLOOR + 5, z, wallC);
                w.fill(x, FLOOR + 6, z, x, CEIL, z, fabric);
                if (u >= 5 && u <= 26)
                    for (int yy = FLOOR + 3; yy <= FLOOR + 12; yy++)
                        w.set(x, yy, z, Hash.chance(s, x, yy, 0x5C, 0.5f) ? screenA : screenB);
                if (u == 2) {
                    w.set(x, y, z, ModBlocks.AIR);
                    w.set(x, y + 1, z, ModBlocks.AIR);
                }
                return;
            }
            if (u == 0 || u == 31 || v == 39) {
                w.set(x, FLOOR, z, aud);
                w.fill(x, y, z, x, CEIL, z, v == 39 ? wallC : fabric);
                return;
            }
            int top = floorAt(v);
            w.fill(x, FLOOR, z, x, top, z, aud);
            if (v < 12 && Hash.chance(s, x, z, 0x57, 0.15f)) w.set(x, FLOOR, z, sticky);
            boolean aisle = u <= 3 || u >= 28;
            if (aisle && v >= 12 && floorAt(v) > floorAt(v - 1))
                w.set(x, top, z, steps.setValue(StairBlock.FACING, Direction.SOUTH));
            w.set(x, CEIL, z, fabric);
            if (seat(u, v) && (v - 12) % 2 == 1 && !Hash.chance(s, x, z, 0x5EA, 0.03f))
                w.set(x, top + 1, z, seatB.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            else if (seat(u, v) && Hash.chance(s, x, z, 0x99, 0.02f))
                w.set(x, top + 1, z, spill.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }

        void fittings(ChunkWriter w, long s, int i, int k) {
            int x0 = i * PX, z0 = k * PZ;
            int y = FLOOR + 1;
            int n = 1 + Math.floorMod(i * 7 + k * 3, 12);
            w.set(x0 + 2, y + 2, z0 + 6, numbers[n]);
            w.set(x0 + 1, y + 1, z0 + 6, doorTop);
            w.set(x0 + 3, y + 1, z0 + 6, doorTop);
            // the exit beside the screen: locked until the film is over
            w.set(x0 + 31, y, z0 + 9, locked);
            w.set(x0 + 31, y + 1, z0 + 9, lockedTop);
            w.set(x0 + 30, y + 2, z0 + 9, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            // the projection booth
            w.set(x0 + 15, FLOOR + 12, z0 + 39, booth);
            w.set(x0 + 16, FLOOR + 12, z0 + 39, booth);
            // posters along the corridor (on the back wall of the auditorium behind)
            for (int u = 6; u <= 26; u += 10) w.set(x0 + u, y + 1, z0 + 39, posters[Hash.range(s, i, k, u, 4)]);
            if (Math.floorMod(i + k, 4) == 0) {
                // concessions: a counter, a machine still warm, a box nobody collected
                for (int u = 10; u <= 20; u++) w.set(x0 + u, y, z0 + PZ + 2, counter);
                w.set(x0 + 12, y, z0 + PZ + 0, machine.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                w.set(x0 + 17, y + 1, z0 + PZ + 2, box.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
        }
    }

    // ---------------------------------------------------------------- the screening

    static final Set<Long> ENDED = ConcurrentHashMap.newKeySet();

    static final class Viewer {
        final ArrayDeque<BlockPos> history = new ArrayDeque<>();
        BlockPos tile;
        int still;
        BlockPos lastPos;
        final Map<Long, Integer> seated = new ConcurrentHashMap<>();
    }

    static final class Screening implements LevelBehavior {
        private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            viewers.put(p.getUUID(), new Viewer());
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0 || p.isSpectator()) return;
            Viewer vw = viewers.computeIfAbsent(p.getUUID(), id -> new Viewer());
            BlockPos here = p.blockPosition();
            vw.history.addLast(here);
            while (vw.history.size() > 5) vw.history.removeFirst();
            vw.still = here.equals(vw.lastPos) ? vw.still + 10 : 0;
            vw.lastPos = here;
            int i = Math.floorDiv(here.getX(), PX), k = Math.floorDiv(here.getZ(), PZ);
            int u = Math.floorMod(here.getX(), PX), v = Math.floorMod(here.getZ(), PZ);
            long key = (long) i * 1_000_003L + k;
            boolean inAud = v >= 7 && v <= 38 && u >= 1 && u <= 30;
            boolean ended = ENDED.contains(key);
            BlockPos past = vw.history.peekFirst();
            int pu = Math.floorMod(past.getX(), PX), pv = Math.floorMod(past.getZ(), PZ);
            boolean pastSeated = Math.floorDiv(past.getX(), PX) == i && Math.floorDiv(past.getZ(), PZ) == k && seat(pu, pv);
            if (!ended && inAud && pastSeated) {
                // you, on the screen, two seconds behind
                int col = 5 + Math.round((pu - 4) * 21f / 23f);
                int row = (pv - 12) / 2;
                BlockPos t = new BlockPos(i * PX + col, FLOOR + 3 + Math.round(row * 9f / 13f), k * PZ + 6);
                if (!t.equals(vw.tile)) {
                    restore(level, vw.tile);
                    vw.tile = t;
                }
                set(level, t, ModBlocks.state(vw.still > 200 ? "mt_screen_you_turned" : "mt_screen_you"));
            }
            if (ended) return;
            if (inAud && seat(u, v)) {
                int n = vw.seated.merge(key, 10, Integer::sum);
                if (n >= FILM) end(level, i, k, vw);
            } else if (inAud && p.getRandom().nextFloat() < 0.03f) {
                level.playSound(null, here.offset(p.getRandom().nextInt(9) - 4, 2, p.getRandom().nextInt(9) - 4),
                        ModSounds.get("event.movie_theater_shush"), SoundSource.AMBIENT, 1f, 0.9f + p.getRandom().nextFloat() * 0.2f);
                Sanity.add(p, -1.5f);
            }
        }

        static void set(ServerLevel level, BlockPos pos, BlockState st) {
            if (level.isLoaded(pos) && level.getBlockState(pos) != st) level.setBlock(pos, st, Block.UPDATE_CLIENTS);
        }

        static void restore(ServerLevel level, BlockPos tile) {
            if (tile == null) return;
            BlockState st = level.getBlockState(tile);
            if (st.is(ModBlocks.get("mt_screen_you")) || st.is(ModBlocks.get("mt_screen_you_turned")))
                level.setBlock(tile, ModBlocks.state("mt_screen"), Block.UPDATE_CLIENTS);
        }

        void end(ServerLevel level, int i, int k, Viewer vw) {
            ENDED.add((long) i * 1_000_003L + k);
            int x0 = i * PX, z0 = k * PZ;
            BlockState dark = ModBlocks.state("mt_screen_dark");
            for (int u = 5; u <= 26; u++)
                for (int y = FLOOR + 3; y <= FLOOR + 12; y++) set(level, new BlockPos(x0 + u, y, z0 + 6), dark);
            String text = "THE END";
            for (int c = 0; c < text.length(); c++) {
                if (text.charAt(c) == ' ') continue;
                set(level, new BlockPos(x0 + 12 + c, FLOOR + 8, z0 + 6),
                        ModBlocks.state("mt_screen_letter_" + Character.toLowerCase(text.charAt(c))));
            }
            BlockState door = ModBlocks.state("dark_doorway");
            set(level, new BlockPos(x0 + 31, FLOOR + 1, z0 + 9), door);
            set(level, new BlockPos(x0 + 31, FLOOR + 2, z0 + 9), door);
            vw.tile = null;
            level.playSound(null, new BlockPos(x0 + 16, FLOOR + 6, z0 + 12), ModSounds.get("event.movie_theater_end"), SoundSource.AMBIENT, 2.5f, 1f);
            for (ServerPlayer p : level.players())
                if (Math.floorDiv(p.getBlockX(), PX) == i && Math.floorDiv(p.getBlockZ(), PZ) == k)
                    p.displayClientMessage(Component.literal("§7§oThe film has ended. Please exit by the door beside the screen."), true);
        }
    }

    @SuppressWarnings("unused")
    private static boolean lit(BlockState st) {
        return st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT);
    }
}
