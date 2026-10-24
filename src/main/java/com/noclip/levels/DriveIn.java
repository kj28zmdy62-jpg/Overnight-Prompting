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
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drive-In Theater. A screen four storeys tall glowing over rows of parked cars, every
 * one of them empty, every speaker post still talking. The film is a shot of this lot,
 * from the screen: the cars, the posts, and someone walking between them toward the
 * camera, a little closer each time it loops. When the reel runs out the screen and
 * the lot go dark. Stand by a speaker until it comes back; the speakers keep talking,
 * and whatever walks the lot in the dark doesn't go near them.
 */
public final class DriveIn {
    static final String ID = "drive_in";
    static final int G = 16, TX = 96, TZ = 112, SCR_X = 36, SCR_Z = 6, COLS = 24, ROWS = 12;
    static final int[] CAR_ROWS = {24, 34, 44, 74, 84, 94};
    static final String[] COLOURS = {"red", "beige", "blue", "green"};

    private DriveIn() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Drive-In Theater").order(3170)
                .tagline("The film is of this lot. When the reel runs out, stand by a speaker.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Reels())
                .visuals(new Visuals().tint(1.0f, 0.98f, 1.06f).saturation(0.7f).contrast(1.04f).brightness(1.5f)
                        .grain(0.09f).aberration(0.0016f).vignette(0.55f).fog(10f, 120f).fogColor(0x0C0E14).bloom(0.9f).sky())
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(12, 24).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(26, 44).weight(3))
                .exit(ExitType.DOOR, "movie_theater", 3).exit(ExitType.DOOR, "parking_lot", 1).exit(ExitType.DOOR, "arcade", 1)
                .exit(ExitType.NOCLIP, "parking_lot", 2).exit(ExitType.NOCLIP, "endless_highway", 1).exit(ExitType.NOCLIP, "carnival", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState[][] screen = new BlockState[COLS][ROWS];
        final BlockState back = ModBlocks.state("dri_screen_back");
        final BlockState frame = ModBlocks.state("dri_screen_frame");
        final BlockState gravel = ModBlocks.state("dri_gravel");
        final BlockState asphalt = ModBlocks.state("dri_asphalt");
        final BlockState line = ModBlocks.state("dri_asphalt_line");
        final BlockState speaker = ModBlocks.state("dri_speaker");
        final BlockState wall = ModBlocks.state("dri_booth_wall");
        final BlockState window = ModBlocks.state("dri_booth_window");
        final BlockState neon = ModBlocks.state("dri_neon_snacks");
        final BlockState door = ModBlocks.state("dri_booth_door");
        final BlockState lamp = ModBlocks.state("dri_lamp");
        final BlockState pole = ModBlocks.state("dri_pole");
        final BlockState fence = ModBlocks.state("dri_fence");
        final BlockState dark = ModBlocks.state("dark_doorway");
        final BlockState sub = ModBlocks.state("subfloor");

        Gen() {
            for (int c = 0; c < COLS; c++)
                for (int r = 0; r < ROWS; r++) screen[c][r] = ModBlocks.state("dri_scr_" + c + "_" + r);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            for (int tx = Math.floorDiv(w.minX - 4, TX); tx <= Math.floorDiv(w.maxX + 4, TX); tx++)
                for (int tz = Math.floorDiv(w.minZ - 4, TZ); tz <= Math.floorDiv(w.maxZ + 4, TZ); tz++) cars(w, s, tx, tz);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int lx = Math.floorMod(x, TX), lz = Math.floorMod(z, TZ);
            w.set(x, G, z, lz <= 3 ? (lz == 1 && Math.floorMod(x, 6) < 3 ? line : asphalt) : gravel);
            // the screen: film on the south face, a braced back, a black doorway under it
            if ((lz == SCR_Z || lz == SCR_Z - 1) && lx >= SCR_X - 1 && lx <= SCR_X + COLS) {
                boolean edge = lx == SCR_X - 1 || lx == SCR_X + COLS;
                for (int y = G + 1; y <= G + 5 + ROWS; y++) {
                    if (lz == SCR_Z - 1) { w.set(x, y, z, back); continue; }
                    int r = G + 4 + ROWS - y;
                    if (edge || y == G + 4 || y == G + 5 + ROWS) w.set(x, y, z, frame);
                    else if (r >= 0 && r < ROWS) w.set(x, y, z, screen[lx - SCR_X][r]);
                    else w.set(x, y, z, (lx == SCR_X + 11 || lx == SCR_X + 12) && y <= G + 2 ? dark : back);
                }
                return;
            }
            // the projection booth and snack bar in the middle of the lot
            if (lx >= 42 && lx <= 53 && lz >= 58 && lz <= 66) {
                for (int y = G + 1; y <= G + 5; y++) {
                    BlockState b = wall;
                    if (lz == 58 && y == G + 3 && lx >= 46 && lx <= 49) b = window;
                    if (lz == 66 && y == G + 3 && (lx == 47 || lx == 48)) b = neon;
                    if (lz == 66 && lx == 44 && y <= G + 2) b = door;
                    w.set(x, y, z, b);
                }
                return;
            }
            if (lx == 0 && lz >= 8 && lz <= TZ - 4 && Hash.unit(s, x, Math.floorDiv(z, 5), 3) > 0.12f)
                w.fill(x, G + 1, z, x, G + 2, z, fence.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
            if (Math.floorMod(lx, 24) == 12 && (lz == 54 || lz == 104)) {
                w.fill(x, G + 1, z, x, G + 4, z, pole);
                w.set(x, G + 5, z, OutsideKit.facing(lamp, Direction.NORTH));
            }
        }

        void cars(ChunkWriter w, long s, int tx, int tz) {
            for (int rz : CAR_ROWS) {
                for (int j = 0; j < 14; j++) {
                    int x = tx * TX + 8 + j * 6, z = tz * TZ + rz;
                    long h = Hash.hash(s, x, z, 11);
                    float r = Hash.unit(h);
                    w.set(x + 3, G + 1, z + 1, OutsideKit.facing(speaker, Direction.WEST));
                    if (r < 0.32f) continue;
                    String colour = COLOURS[Hash.range(h >>> 8, COLOURS.length)];
                    if (r > 0.985f) OutsideKit.car(w, x + 1, G + 1, z + 3, Direction.SOUTH, colour, false);
                    else if (r > 0.96f) OutsideKit.car(w, x, G + 1, z, Direction.NORTH, (h & 1) == 0 ? "red" : "beige", true);
                    else OutsideKit.car(w, x, G + 1, z, Direction.NORTH, colour, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Reel changes: the dark, the speakers, and what walks the lot. */
    static final class Reels implements LevelBehavior {
        /** {phase 0 film / 1 dark, phase end, next reel change} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Set<UUID> WARNED = ConcurrentHashMap.newKeySet();
        private static final Set<UUID> VISITED = ConcurrentHashMap.newKeySet();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 2000 + level.random.nextInt(1600)});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0 && now >= st[2] && !level.players().isEmpty()) {
                st[0] = 1;
                st[1] = now + 500;
                WARNED.clear();
                VISITED.clear();
                Blackouts.start(level, 500);
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.drive_in_reel_end"), SoundSource.AMBIENT, 1.6f, 1f);
                    p.displayClientMessage(Component.literal("The reel runs out. The lot goes dark.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                }
            } else if (st[0] == 1 && now >= st[1]) {
                st[0] = 0;
                st[2] = now + 3000 + level.random.nextInt(1800);
            }
        }

        static boolean nearSpeaker(ServerLevel level, BlockPos at) {
            Block sp = ModBlocks.get("dri_speaker");
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 1, 2)))
                if (level.getBlockState(q).is(sp)) return true;
            return false;
        }

        /** You arrive in the lane behind the front row, facing the screen. */
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            double x = Math.floorDiv(p.getBlockX(), TX) * TX + 30 + level.random.nextInt(36) + 0.5;
            double z = Math.floorDiv(p.getBlockZ(), TZ) * TZ + 30 + level.random.nextInt(3) + 0.5;
            p.teleportTo(level, x, G + 1, z, 180f + (level.random.nextFloat() - 0.5f) * 30f, -8f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            if (st[0] != 1 || p.isSpectator() || (p.tickCount + p.getId()) % 40 != 0) return;
            BlockPos at = p.blockPosition();
            if (nearSpeaker(level, at)) {
                level.playSound(null, at, ModSounds.get("event.drive_in_voice"), SoundSource.AMBIENT, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
                if (WARNED.add(p.getUUID()))
                    p.displayClientMessage(Component.literal("The speaker keeps talking. Stay with it.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return;
            }
            Vec3 b = OutsideKit.behind(p, 7, 60, level.random);
            level.playSound(null, BlockPos.containing(b), ModSounds.get("event.drive_in_footsteps"), SoundSource.HOSTILE, 1.2f, 1f);
            Sanity.add(p, -3f);
            if (level.getGameTime() > st[1] - 400 && VISITED.add(p.getUUID())) {
                Mob m = (Mob) ModEntities.lazy("smiler").get().create(level);
                if (m != null) {
                    Vec3 v = OutsideKit.behind(p, 11, 40, level.random);
                    m.moveTo(v.x, G + 1, v.z, p.getYRot(), 0);
                    level.addFreshEntity(m);
                }
            }
        }
    }
}
