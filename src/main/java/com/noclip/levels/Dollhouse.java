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
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giant's Dollhouse. A house at ten times scale: floorboards a block wide with
 * gaps you could lose a foot in, table legs like pillars, a tabletop for a sky,
 * crayons like fallen logs. Something enormous lives here. Every minute or two
 * the floor begins to shake with its footsteps and its shadow falls over the
 * room; if you are not under something when it looks down, it picks you up and
 * puts you somewhere else. Mouse holes in the skirting lead out. In the
 * children's rooms stands a toy house exactly your size, with a door that opens.
 */
public final class Dollhouse {
    static final int FLOOR = 4, CEIL = FLOOR + 27, ROOM = 56;

    private Dollhouse() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("dollhouse")
                .number("").title("Giant's Dollhouse").order(4060)
                .tagline("You are very small. When the floor shakes, get under something.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Giant())
                .visuals(new Visuals().tint(1.06f, 1.0f, 0.9f).saturation(0.92f).contrast(1.04f).brightness(1.15f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.42f).fog(18f, 120f).fogColor(0xC8B898).bloom(0.55f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("dh_tin_soldier"), 2, 0.25f).dist(18, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.15f).dist(20, 40).weight(1))
                .exit(ExitType.DOOR, "air_vents", 2).exit(ExitType.DOOR, "level_2", 1).exit(ExitType.DOOR, "sewers", 1)
                .exit(ExitType.DOOR, "storm_drains", 1).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.SECRET, "recursion", 2).exit(ExitType.SECRET, "manila_room", 1).exit(ExitType.SECRET, "daycare", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "playground", 1).exit(ExitType.NOCLIP, "daycare", 1)
                .build();
    }

    // ---------------------------------------------------------------- the Giant

    static final class Giant implements LevelBehavior {
        static final int IDLE = 0, APPROACH = 1, LOOK = 2, LEAVE = 3;

        static final class St {
            int phase = IDLE, timer = 900, step, drop = -1;
            double dirX, dirZ;
        }

        private final Map<UUID, St> state = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St s = new St();
            s.timer = 600 + p.getRandom().nextInt(600);
            state.put(p.getUUID(), s);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St s = state.computeIfAbsent(p.getUUID(), u -> new St());
            if (s.drop > 0 && --s.drop == 0) putDown(p, level);
            if (--s.timer > 0) return;
            switch (s.phase) {
                case IDLE -> {
                    s.phase = APPROACH;
                    s.step = 0;
                    double a = p.getRandom().nextDouble() * Math.PI * 2;
                    s.dirX = Math.cos(a);
                    s.dirZ = Math.sin(a);
                    s.timer = 1;
                }
                case APPROACH -> {
                    // footsteps from one direction, nearer each time
                    s.step++;
                    float vol = 0.5f + s.step * 0.18f;
                    double d = 40 - s.step * 6;
                    BlockPos at = BlockPos.containing(p.getX() + s.dirX * d, FLOOR + 2, p.getZ() + s.dirZ * d);
                    level.playSound(null, at, ModSounds.get("event.dollhouse_step"), SoundSource.AMBIENT, vol, 0.85f + p.getRandom().nextFloat() * 0.1f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.1f + s.step * 0.06f));
                    if (s.step >= 6) {
                        s.phase = LOOK;
                        s.step = 0;
                        s.timer = 30;
                        // its shadow falls across the whole room
                        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("blackout", 5.5f));
                    } else {
                        s.timer = 26 - s.step * 2;
                    }
                }
                case LOOK -> {
                    s.step++;
                    if (!covered(p, level) && !p.isCreative() && !p.isSpectator() && s.drop < 0) pickUp(p, level, s);
                    if (s.step >= 3) {
                        s.phase = LEAVE;
                        s.step = 0;
                    }
                    s.timer = 35;
                }
                case LEAVE -> {
                    s.step++;
                    BlockPos at = BlockPos.containing(p.getX() - s.dirX * (20 + s.step * 12), FLOOR + 2, p.getZ() - s.dirZ * (20 + s.step * 12));
                    level.playSound(null, at, ModSounds.get("event.dollhouse_step"), SoundSource.AMBIENT, 1.2f - s.step * 0.35f, 0.9f);
                    if (s.step >= 3) {
                        s.phase = IDLE;
                        s.timer = 1500 + p.getRandom().nextInt(1300);
                    } else {
                        s.timer = 24;
                    }
                }
                default -> s.phase = IDLE;
            }
        }

        /** Anything solid over your head between you and the ceiling: a table, a chair seat, a bed. */
        static boolean covered(ServerPlayer p, ServerLevel level) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int y = (int) Math.floor(p.getY()) + 2; y < CEIL - 1; y++) {
                m.set(p.getBlockX(), y, p.getBlockZ());
                if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) return true;
            }
            return false;
        }

        void pickUp(ServerPlayer p, ServerLevel level, St s) {
            level.playSound(null, p.blockPosition(), ModSounds.get("event.dollhouse_lift"), SoundSource.HOSTILE, 1f, 1f);
            p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 5, false, false));
            Sanity.add(p, -15f);
            s.drop = 50;
        }

        /** ...and set you down somewhere else in the house, from a height. */
        void putDown(ServerPlayer p, ServerLevel level) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            double d = 160 + p.getRandom().nextInt(160);
            int i = Math.floorDiv((int) (p.getX() + Math.cos(a) * d), ROOM);
            int k = Math.floorDiv((int) (p.getZ() + Math.sin(a) * d), ROOM);
            double x = i * ROOM + 46.5, z = k * ROOM + 46.5;
            p.removeEffect(MobEffects.LEVITATION);
            p.teleportTo(level, x, CEIL - 8, z, p.getRandom().nextFloat() * 360f, 30f);
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.7f));
        }
    }

    // ---------------------------------------------------------------- the house

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("dh_floor");
        final BlockState joint = ModBlocks.state("dh_floor_joint");
        final BlockState base = ModBlocks.state("dh_baseboard");
        final BlockState plaster = ModBlocks.state("dh_plaster");
        final BlockState frame = ModBlocks.state("dh_frame");
        final BlockState glass = ModBlocks.state("dh_glass");
        final BlockState rug = ModBlocks.state("dh_rug");
        final BlockState hole = ModBlocks.state("dh_mousehole");
        final BlockState wood = ModBlocks.state("dh_wood");
        final BlockState woodH = ModBlocks.state("dh_wood_h");
        final BlockState chair = ModBlocks.state("dh_chair");
        final BlockState sofa = ModBlocks.state("dh_sofa");
        final BlockState gingham = ModBlocks.state("dh_gingham");
        final BlockState mattress = ModBlocks.state("dh_mattress");
        final BlockState pillow = ModBlocks.state("dh_pillow");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14);
        final BlockState[] paper = {ModBlocks.state("dh_wall_tl"), ModBlocks.state("dh_wall_tr"),
                ModBlocks.state("dh_wall_bl"), ModBlocks.state("dh_wall_br")};

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                int u = Math.floorMod(x, ROOM);
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int v = Math.floorMod(z, ROOM);
                    int off = Hash.range(s, z, 0xF100, 12);
                    w.set(x, FLOOR, z, Math.floorMod(x + off, 12) == 0 ? joint : floor);
                    w.set(x, CEIL, z, plaster);
                    if (u < 2 || v < 2) {
                        w.set(x, FLOOR + 1, z, base);
                        int along = u < 2 ? z : x;
                        for (int y = FLOOR + 2; y < CEIL; y++)
                            w.set(x, y, z, paper[(Math.floorMod(-y, 2) * 2) + Math.floorMod(along, 2)]);
                    } else if (u % 9 == 5 && v % 9 == 5) {
                        w.set(x, FLOOR + 15, z, light);
                    }
                }
            }
            CellGrid g = new CellGrid(s, ROOM, 0);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> room(w, s, i, k, x0, z0));
        }

        BlockState f(String n, Direction d) {
            return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void box(ChunkWriter w, int x1, int y1, int z1, int x2, int y2, int z2, BlockState b) {
            w.fill(x1, y1, z1, x2, y2, z2, b);
        }

        void room(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            if (x0 > w.maxX || x0 + ROOM < w.minX || z0 > w.maxZ || z0 + ROOM < w.minZ) return;
            wall(w, s, i, k, x0, z0, true);
            wall(w, s, i, k, x0, z0, false);
            // ceiling light: a frosted dome far overhead
            int cx = x0 + 29, cz = z0 + 29;
            box(w, cx - 3, CEIL - 1, cz - 3, cx + 2, CEIL - 1, cz + 2, frame);
            box(w, cx - 2, CEIL - 1, cz - 2, cx + 1, CEIL - 1, cz + 1, glass);
            int type = Hash.range(s, i, k, 0x7E9E, 20);
            if (type < 7) dining(w, x0, z0);
            else if (type < 14) kids(w, s, i, k, x0, z0);
            else living(w, x0, z0);
            toys(w, s, i, k, x0, z0, type < 7 ? 0 : type < 14 ? 1 : 2);
        }

        /** The west (along z) or north (along x) wall of this room: doorway, window, mouse hole, or nothing. */
        void wall(ChunkWriter w, long s, int i, int k, int x0, int z0, boolean west) {
            long h = Hash.hash(s, i, k, west ? 0xD1 : 0xD2);
            float r = Hash.unit(h);
            int F = FLOOR;
            if (r < 0.62f) {
                int a = 8 + Hash.range(h >>> 8, 34), width = 10, top = F + 21;
                for (int t = a - 1; t <= a + width; t++)
                    for (int y = F + 1; y <= top + 1; y++)
                        for (int d = 0; d < 2; d++) {
                            int x = west ? x0 + d : x0 + t, z = west ? z0 + t : z0 + d;
                            boolean inside = t >= a && t < a + width && y <= top;
                            w.set(x, y, z, inside ? ModBlocks.AIR : frame);
                        }
            } else {
                if (r < 0.80f) {
                    // a window full of afternoon
                    int a = 14 + Hash.range(h >>> 8, 24);
                    for (int t = a - 1; t <= a + 12; t++)
                        for (int y = F + 7; y <= F + 22; y++)
                            for (int d = 0; d < 2; d++) {
                                int x = west ? x0 + d : x0 + t, z = west ? z0 + t : z0 + d;
                                boolean edge = t == a - 1 || t == a + 12 || y == F + 7 || y == F + 22;
                                boolean mullion = t == a + 5 || t == a + 6 || y == F + 14;
                                w.set(x, y, z, edge || mullion ? frame : glass);
                            }
                }
                if (Hash.unit(h >>> 20) < 0.7f) {
                    // a mouse hole, arched, gnawed through the skirting
                    int a = 4 + Hash.range(h >>> 30, 46);
                    if (r < 0.80f) a = 3 + Hash.range(h >>> 30, 8);
                    for (int d = 0; d < 2; d++)
                        for (int t = 0; t < 3; t++)
                            for (int y = F + 1; y <= F + 3; y++) {
                                if (y == F + 3 && t != 1) continue;
                                int x = west ? x0 + d : x0 + a + t, z = west ? z0 + a + t : z0 + d;
                                w.set(x, y, z, hole);
                            }
                }
            }
        }

        // ---- furniture

        void dining(ChunkWriter w, int x0, int z0) {
            int tx = x0 + 20, tz = z0 + 18, F = FLOOR;
            int lx = 14, lz = 20;
            for (int[] c : new int[][]{{1, 1}, {lx - 3, 1}, {1, lz - 3}, {lx - 3, lz - 3}})
                box(w, tx + c[0], F + 1, tz + c[1], tx + c[0] + 1, F + 7, tz + c[1] + 1, wood);
            // apron and top: a wooden sky
            box(w, tx + 1, F + 7, tz + 1, tx + lx - 2, F + 7, tz + 1, woodH);
            box(w, tx + 1, F + 7, tz + lz - 2, tx + lx - 2, F + 7, tz + lz - 2, woodH);
            box(w, tx + 1, F + 7, tz + 1, tx + 1, F + 7, tz + lz - 2, woodH);
            box(w, tx + lx - 2, F + 7, tz + 1, tx + lx - 2, F + 7, tz + lz - 2, woodH);
            box(w, tx, F + 8, tz, tx + lx - 1, F + 8, tz + lz - 1, woodH);
            chair(w, tx - 4, tz + 3, true);
            chair(w, tx - 4, tz + 11, true);
            chair(w, tx + lx - 2, tz + 3, false);
            chair(w, tx + lx - 2, tz + 11, false);
        }

        /** A 6x6 chair; west=true puts its back on the west side. */
        void chair(ChunkWriter w, int cx, int cz, boolean west) {
            int F = FLOOR;
            for (int[] c : new int[][]{{0, 0}, {5, 0}, {0, 5}, {5, 5}})
                box(w, cx + c[0], F + 1, cz + c[1], cx + c[0], F + 4, cz + c[1], chair);
            box(w, cx, F + 5, cz, cx + 5, F + 5, cz + 5, chair);
            int bx = west ? cx : cx + 5;
            box(w, bx, F + 6, cz, bx, F + 13, cz, chair);
            box(w, bx, F + 6, cz + 5, bx, F + 13, cz + 5, chair);
            box(w, bx, F + 9, cz, bx, F + 9, cz + 5, chair);
            box(w, bx, F + 12, cz, bx, F + 13, cz + 5, chair);
        }

        void kids(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int F = FLOOR;
            box(w, x0 + 20, F, z0 + 24, x0 + 46, F, z0 + 46, rug);
            // the bed, with room to crawl underneath
            int bx = x0 + 4, bz = z0 + 32;
            for (int[] c : new int[][]{{0, 0}, {10, 0}, {0, 19}, {10, 19}})
                box(w, bx + c[0], F + 1, bz + c[1], bx + c[0], F + 2, bz + c[1], wood);
            box(w, bx, F + 3, bz, bx + 10, F + 3, bz + 19, woodH);
            box(w, bx, F + 4, bz, bx + 10, F + 5, bz + 18, mattress);
            box(w, bx, F + 6, bz, bx + 10, F + 6, bz + 12, gingham);
            box(w, bx - 1 < x0 + 2 ? bx : bx - 1, F + 3, bz, bx - 1 < x0 + 2 ? bx : bx - 1, F + 5, bz + 12, gingham);
            box(w, bx + 2, F + 6, bz + 15, bx + 8, F + 7, bz + 17, pillow);
            box(w, bx, F + 1, bz + 19, bx + 10, F + 12, bz + 19, wood);
            // a toy house exactly your size, with a door that opens
            int hx = x0 + 34, hz = z0 + 10;
            BlockState hw = ModBlocks.state("dh_th_wall"), hwin = ModBlocks.state("dh_th_window"), roof = ModBlocks.state("dh_th_roof");
            for (int u = 0; u < 9; u++)
                for (int v = 0; v < 7; v++)
                    for (int y = F + 1; y <= F + 6; y++) {
                        boolean face = u == 0 || u == 8 || v == 0 || v == 6;
                        boolean win = face && (y == F + 3 || y == F + 4) && ((v == 6 && (u == 2 || u == 6)) || ((u == 0 || u == 8) && v == 3));
                        w.set(hx + u, y, hz + v, win ? hwin : hw);
                    }
            for (int layer = 0; layer < 5; layer++)
                box(w, hx - 1, F + 7 + layer, hz - 1 + layer, hx + 9, F + 7 + layer, hz + 7 - layer, roof);
            w.set(hx + 4, F + 1, hz + 6, ModBlocks.state("dh_th_door_low"));
            w.set(hx + 4, F + 2, hz + 6, ModBlocks.state("dh_th_door_high"));
        }

        void living(ChunkWriter w, int x0, int z0) {
            int F = FLOOR;
            box(w, x0 + 8, F, z0 + 15, x0 + 34, F, z0 + 32, rug);
            int sx = x0 + 10, sz = z0 + 4;
            box(w, sx, F + 1, sz, sx + 21, F + 3, sz + 8, sofa);
            box(w, sx, F + 4, sz, sx + 21, F + 9, sz + 1, sofa);
            box(w, sx, F + 4, sz, sx + 1, F + 6, sz + 8, sofa);
            box(w, sx + 20, F + 4, sz, sx + 21, F + 6, sz + 8, sofa);
            box(w, sx + 2, F + 4, sz + 2, sx + 19, F + 4, sz + 8, sofa);
            // coffee table: low enough to hide under, just
            int cx = x0 + 14, cz = z0 + 20;
            for (int[] c : new int[][]{{0, 0}, {13, 0}, {0, 7}, {13, 7}})
                box(w, cx + c[0], F + 1, cz + c[1], cx + c[0], F + 4, cz + c[1], wood);
            box(w, cx, F + 5, cz, cx + 13, F + 5, cz + 7, woodH);
        }

        // ---- toys and debris, only where the room's furniture leaves space

        void toys(ChunkWriter w, long s, int i, int k, int x0, int z0, int type) {
            int[][] clear = switch (type) {
                case 0 -> new int[][]{{38, 4, 52, 52}, {4, 42, 34, 52}};
                case 1 -> new int[][]{{20, 22, 52, 52}, {46, 4, 52, 20}};
                default -> new int[][]{{38, 4, 52, 52}, {4, 36, 34, 52}};
            };
            int n = 3 + Hash.range(s, i, k, 0x70A, 4);
            for (int t = 0; t < n; t++) {
                long h = Hash.hash(s, i, k, 0x7000 + t);
                int[] c = clear[Hash.range(h, clear.length)];
                int x = x0 + c[0] + Hash.range(h >>> 8, Math.max(1, c[2] - c[0] - 10));
                int z = z0 + c[1] + Hash.range(h >>> 16, Math.max(1, c[3] - c[1] - 10));
                toy(w, h >>> 24, x, z);
            }
            if (Hash.chance(s, i, k, 0x707E, 0.35f)) {
                int[] c = clear[0];
                int x = x0 + c[0] + 2, z = z0 + c[1] + 2;
                String[] q = {"tl", "tm", "tr", "bl", "bm", "br"};
                for (int dz = 0; dz < 2; dz++)
                    for (int dx = 0; dx < 3; dx++)
                        w.set(x + dx, FLOOR + 1, z + dz, f("dh_note_" + q[dz * 3 + dx], Direction.NORTH));
            }
        }

        static final String[] COLORS = {"red", "blue", "yellow", "green"};
        static final String[] LETTERS = {"a", "b", "c", "d"};

        void toy(ChunkWriter w, long h, int x, int z) {
            int F = FLOOR;
            switch (Hash.range(h, 7)) {
                case 0 -> {
                    // alphabet blocks, sometimes stacked
                    int stack = 1 + Hash.range(h >>> 4, 2);
                    for (int b = 0; b < stack; b++) {
                        int c = Hash.range(h >>> (8 + b * 4), 4);
                        toyBlock(w, x + b, F + 1 + b * 3, z + b, COLORS[c], LETTERS[c]);
                    }
                }
                case 1 -> {
                    BlockState a = ModBlocks.state("dh_ball_a"), b = ModBlocks.state("dh_ball_b");
                    for (int dx = -3; dx <= 3; dx++)
                        for (int dy = -3; dy <= 3; dy++)
                            for (int dz = -3; dz <= 3; dz++)
                                if (dx * dx + dy * dy + dz * dz <= 11)
                                    w.set(x + 3 + dx, F + 4 + dy, z + 3 + dz, Math.abs(dx + dz) % 4 < 2 ? a : b);
                }
                case 2 -> {
                    BlockState c = ModBlocks.state(Hash.range(h >>> 4, 2) == 0 ? "dh_crayon_red" : "dh_crayon_blue");
                    boolean alongX = (h & 32) != 0;
                    for (int t = 0; t < 8; t++) w.set(alongX ? x + t : x, F + 1, alongX ? z : z + t, c);
                }
                case 3 -> {
                    boolean alongX = (h & 32) != 0;
                    for (int t = 0; t < 10; t++) {
                        String b = t == 0 ? "dh_eraser" : t == 1 ? "dh_ferrule" : t == 8 ? "dh_pencil_wood" : t == 9 ? "dh_lead" : "dh_pencil";
                        w.set(alongX ? x + t : x, F + 1, alongX ? z : z + t, ModBlocks.state(b));
                    }
                }
                case 4 -> {
                    BlockState coin = ModBlocks.state("dh_coin");
                    for (int dx = -2; dx <= 2; dx++)
                        for (int dz = -2; dz <= 2; dz++)
                            if (dx * dx + dz * dz <= 5) w.set(x + 2 + dx, F + 1, z + 2 + dz, coin);
                }
                case 5 -> {
                    BlockState lego = ModBlocks.state("dh_lego");
                    box(w, x, F + 1, z, x + 3, F + 1, z + 1, lego);
                    for (int dx = 0; dx < 4; dx++)
                        for (int dz = 0; dz < 2; dz++) w.set(x + dx, F + 2, z + dz, f("dh_lego_stud", Direction.NORTH));
                }
                default -> {
                    BlockState dust = ModBlocks.state("dh_dust");
                    for (int dx = 0; dx < 4; dx++)
                        for (int dz = 0; dz < 3; dz++) {
                            int ht = Hash.range(h >>> (dx * 3 + dz), 3);
                            for (int y = 0; y < ht; y++) w.set(x + dx, F + 1 + y, z + dz, dust);
                        }
                }
            }
        }

        void toyBlock(ChunkWriter w, int x, int y, int z, String color, String letter) {
            BlockState edge = ModBlocks.state("dh_toy_" + color + "_edge");
            BlockState face = ModBlocks.state("dh_toy_letter_" + letter);
            for (int dx = 0; dx < 3; dx++)
                for (int dy = 0; dy < 3; dy++)
                    for (int dz = 0; dz < 3; dz++) {
                        int mids = (dx == 1 ? 1 : 0) + (dy == 1 ? 1 : 0) + (dz == 1 ? 1 : 0);
                        w.set(x + dx, y + dy, z + dz, mids == 2 ? face : edge);
                    }
        }
    }
}
