package com.noclip.levels;

import com.noclip.entity.WindowWatcher;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Level 188, "The Courtyard of Windows". A small courtyard at night, enclosed on
 * all four sides by one building whose facade rises past the fog: hundreds of
 * windows, some lit. You cannot get in. The residents watch you from the lit
 * windows, and each time you look away, they are standing a floor or two lower.
 */
public final class Level188 {
    static final int P = 40, G = 4, LO = 8, HI = 31, TOP = 255;
    static final String ID = "level_188";
    /** Sides: 0 west facade (faces +x), 1 east, 2 north (faces +z), 3 south. */
    static final int MAX_FLOOR = (TOP - G - 3) / 4;

    private Level188() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("188").title("The Courtyard of Windows").order(1880)
                .tagline("Every window is somebody's home. None of them will let you in.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Residents())
                .visuals(new Visuals().tint(0.9f, 0.95f, 1.08f).saturation(0.82f).contrast(1.1f).brightness(1.15f)
                        .grain(0.07f).aberration(0.0015f).vignette(0.5f).bloom(0.75f).fog(14f, 130f).fogColor(0x0D1424).sky())
                .sanityDrain(0.03f)
                .spawnY(G + 1, G + 1)
                .exit(ExitType.FALL, "level_11", 2).exit(ExitType.FALL, "level_9", 1)
                .exit(ExitType.DOOR, "level_13", 2).exit(ExitType.DOOR, "level_5", 1)
                .exit(ExitType.SECRET, "level_13", 3).exit(ExitType.SECRET, "level_5", 1)
                .exit(ExitType.NOCLIP, "level_11", 2).exit(ExitType.NOCLIP, "level_37", 1).exit(ExitType.NOCLIP, "level_94", 1)
                .build();
    }

    static boolean windowColumn(int t) {
        return t >= LO && t <= HI && Math.floorMod(t - LO, 3) == 1;
    }

    static int windowY(int floor) {
        return G + 1 + floor * 4;
    }

    /** World position of the 1x2 pocket behind a window. */
    static BlockPos pocket(int cx, int cz, int side, int t, int floor) {
        int y = windowY(floor), bx = cx * P, bz = cz * P;
        return switch (side) {
            case 0 -> new BlockPos(bx + LO - 2, y, bz + t);
            case 1 -> new BlockPos(bx + HI + 2, y, bz + t);
            case 2 -> new BlockPos(bx + t, y, bz + LO - 2);
            default -> new BlockPos(bx + t, y, bz + HI + 2);
        };
    }

    static BlockPos glass(int cx, int cz, int side, int t, int floor) {
        BlockPos p = pocket(cx, cz, side, t, floor);
        return switch (side) {
            case 0 -> p.east();
            case 1 -> p.west();
            case 2 -> p.south();
            default -> p.north();
        };
    }

    static String roomKind(long s, int cx, int cz, int side, int t, int floor) {
        float r = Hash.unit(Hash.hash(s, cx * 7919L + cz, side * 1000L + t, floor));
        if (floor == 0 && t >= 18 && t <= 21 && side == 2) return "dark";
        if (r < 0.38f) return "dark";
        if (r < 0.66f) return "curtain";
        if (r < 0.77f) return "blinds";
        if (r < 0.85f) return "lamp";
        if (r < 0.92f) return "tv";
        if (r < 0.955f) return "sil";
        if (r < 0.97f) return "red";
        if (r < 0.98f) return "help";
        return "dark";
    }

    static final class Gen implements LevelGenerator {
        final BlockState facade = ModBlocks.state("courtyard_facade");
        final BlockState ledge = ModBlocks.state("courtyard_ledge");
        final BlockState plinth = ModBlocks.state("courtyard_facade_base");
        final BlockState window = ModBlocks.state("courtyard_window");
        final BlockState grass = ModBlocks.state("courtyard_grass");
        final BlockState path = ModBlocks.state("courtyard_path");
        final BlockState stone = ModBlocks.state("courtyard_fountain_stone");
        final BlockState leaves = ModBlocks.state("courtyard_dead_leaves");
        final BlockState drain = ModBlocks.state("courtyard_drain");
        final BlockState hedge = ModBlocks.state("courtyard_hedge");
        final BlockState bench = ModBlocks.state("courtyard_bench");
        final BlockState post = ModBlocks.state("courtyard_lamp_post");
        final BlockState lamp = ModBlocks.state("courtyard_lamp_head");
        final BlockState trunk = ModBlocks.state("courtyard_dead_trunk");
        final BlockState branches = ModBlocks.state("courtyard_dead_branches");
        final BlockState door = ModBlocks.state("courtyard_door");
        final BlockState doorTop = ModBlocks.state("courtyard_door_top");
        final BlockState ajar = ModBlocks.state("courtyard_door_ajar");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final Map<String, BlockState> rooms = new HashMap<>();

        Gen() {
            for (String k : new String[]{"curtain", "blinds", "lamp", "dark", "red", "tv", "sil_top", "sil_bottom", "help"})
                rooms.put(k, ModBlocks.state("courtyard_room_" + k));
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, P), lz = Math.floorMod(z, P);
                    int cx = Math.floorDiv(x, P), cz = Math.floorDiv(z, P);
                    boolean inX = lx >= LO && lx <= HI, inZ = lz >= LO && lz <= HI;
                    if (inX && inZ) {
                        ground(w, s, x, z, lx, lz, cx, cz);
                        continue;
                    }
                    w.fill(x, 0, z, x, TOP, z, filler);
                    if (inZ && (lx == LO - 1 || lx == HI + 1)) facadeColumn(w, s, x, z, cx, cz, lx == LO - 1 ? 0 : 1, lz);
                    else if (inX && (lz == LO - 1 || lz == HI + 1)) facadeColumn(w, s, x, z, cx, cz, lz == LO - 1 ? 2 : 3, lx);
                    else if (inZ && (lx == LO - 2 || lx == HI + 2)) pocketColumn(w, x, z, lz);
                    else if (inX && (lz == LO - 2 || lz == HI + 2)) pocketColumn(w, x, z, lx);
                    else if (inZ && (lx == LO - 3 || lx == HI + 3)) backColumn(w, s, x, z, cx, cz, lx == LO - 3 ? 0 : 1, lz);
                    else if (inX && (lz == LO - 3 || lz == HI + 3)) backColumn(w, s, x, z, cx, cz, lz == LO - 3 ? 2 : 3, lx);
                }
            }
        }

        boolean doorway(int side, int t) {
            return side == 2 && (t == 19 || t == 20);
        }

        void facadeColumn(ChunkWriter w, long s, int x, int z, int cx, int cz, int side, int t) {
            boolean ns = side >= 2;
            BlockState pane = window.setValue(ns ? BlockStateProperties.EAST : BlockStateProperties.NORTH, true)
                    .setValue(ns ? BlockStateProperties.WEST : BlockStateProperties.SOUTH, true);
            for (int y = 0; y <= G; y++) w.set(x, y, z, plinth);
            for (int y = G + 1; y <= TOP; y++) {
                int rel = y - G - 1, f = rel / 4, r = rel % 4;
                BlockState b;
                if (r < 2 && windowColumn(t) && f <= MAX_FLOOR) b = pane;
                else if (r == 2) b = ledge;
                else b = f == 0 ? plinth : facade;
                w.set(x, y, z, b);
            }
            if (doorway(side, t)) {
                boolean open = Hash.unit(s, cx, cz, 188) < 0.1f;
                w.set(x, G + 1, z, open ? ajar : door);
                w.set(x, G + 2, z, open ? ajar : doorTop);
            }
        }

        void pocketColumn(ChunkWriter w, int x, int z, int t) {
            if (!windowColumn(t)) return;
            for (int f = 0; f <= MAX_FLOOR; f++) {
                int y = windowY(f);
                w.set(x, y, z, ModBlocks.AIR);
                w.set(x, y + 1, z, ModBlocks.AIR);
            }
        }

        void backColumn(ChunkWriter w, long s, int x, int z, int cx, int cz, int side, int t) {
            if (!windowColumn(t)) return;
            for (int f = 0; f <= MAX_FLOOR; f++) {
                int y = windowY(f);
                String k = roomKind(s, cx, cz, side, t, f);
                if (k.equals("sil")) {
                    w.set(x, y, z, rooms.get("sil_bottom"));
                    w.set(x, y + 1, z, rooms.get("sil_top"));
                } else {
                    BlockState b = rooms.get(k);
                    w.set(x, y, z, b);
                    w.set(x, y + 1, z, b);
                }
            }
        }

        void ground(ChunkWriter w, long s, int x, int z, int lx, int lz, int cx, int cz) {
            w.fill(x, 0, z, x, G - 1, z, sub);
            double d = Math.sqrt((lx - 19.5) * (lx - 19.5) + (lz - 19.5) * (lz - 19.5));
            boolean cross = Math.abs(lx - 19.5) < 1.6 || Math.abs(lz - 19.5) < 1.6;
            boolean ring = d >= 4.6 && d < 6.4;
            // the dry fountain, and the drain in the bottom of it
            if (d < 4.6) {
                if (d >= 3.6) {
                    w.set(x, G, z, stone);
                    w.set(x, G + 1, z, stone);
                } else {
                    w.set(x, G - 1, z, stone);
                    w.set(x, G, z, ModBlocks.AIR);
                    if (lx >= 19 && lx <= 20 && lz >= 19 && lz <= 20) {
                        for (int y = G; y <= G + 2; y++) w.set(x, y, z, stone);
                    } else if (lx == 22 && lz == 19) {
                        w.set(x, G - 1, z, drain);
                        w.set(x, G - 2, z, pit);
                        w.set(x, G - 3, z, pit);
                    } else if (Hash.chance(s, x, z, 3, 0.45f)) {
                        w.set(x, G, z, leaves);
                    }
                }
                return;
            }
            w.set(x, G, z, cross || ring ? path : grass);
            if (!cross && !ring && Hash.chance(s, x, z, 4, 0.06f)) w.set(x, G + 1, z, leaves);
            boolean edge = lx == LO || lx == HI || lz == LO || lz == HI;
            if (edge && !cross) w.set(x, G + 1, z, hedge);
            // lamp posts at the four corners of the ring
            if ((lx == 14 || lx == 25) && (lz == 14 || lz == 25)) {
                w.set(x, G + 1, z, post);
                w.set(x, G + 2, z, post);
                w.set(x, G + 3, z, lamp);
            }
            if (lz == 12 && (lx == 16 || lx == 23)) w.set(x, G + 1, z, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            if (lz == 27 && (lx == 16 || lx == 23)) w.set(x, G + 1, z, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            if ((lx == 11 && lz == 11) || (lx == 28 && lz == 28 && Hash.unit(s, cx, cz, 5) < 0.6f)) {
                for (int y = G + 1; y <= G + 4; y++) w.set(x, y, z, trunk.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                w.set(x, G + 5, z, branches);
                w.set(x + 1, G + 4, z, branches);
                w.set(x, G + 4, z - 1, branches);
            }
        }
    }

    // ---------------------------------------------------------------- the residents

    static final class Residents implements LevelBehavior {
        private final Map<UUID, Integer> arrived = new HashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            arrived.put(p.getUUID(), p.tickCount);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || p.tickCount % 10 != 0) return;
            var r = p.getRandom();
            int cx = Math.floorDiv(p.getBlockX(), P), cz = Math.floorDiv(p.getBlockZ(), P);
            int secs = (p.tickCount - arrived.computeIfAbsent(p.getUUID(), u -> p.tickCount)) / 20;
            int want = Math.min(7, 1 + secs / 40);
            List<WindowWatcher> mine = level.getEntitiesOfClass(WindowWatcher.class, p.getBoundingBox().inflate(70));
            long s = DeepEndUtil.seed(level, ID);

            if (mine.size() < want && r.nextFloat() < 0.35f) {
                int side = r.nextInt(4), t = LO + 1 + 3 * r.nextInt(8), f = 3 + r.nextInt(10);
                if (!roomKind(s, cx, cz, side, t, f).equals("dark")) {
                    BlockPos at = pocket(cx, cz, side, t, f);
                    if (!DeepEndUtil.seen(p, Vec3.atCenterOf(at).add(0, 0.5, 0), 0.5)) place(level, null, cx, cz, side, t, f);
                }
            }
            for (WindowWatcher w : mine) {
                if (w.outside) continue;
                if (Math.floorDiv(w.getBlockX(), P) != cx || Math.floorDiv(w.getBlockZ(), P) != cz) {
                    w.discard();
                    continue;
                }
                if (DeepEndUtil.seen(p, w.position().add(0, 1.6, 0), 0.45)) continue;
                if (r.nextFloat() > 0.09f) continue;
                if (w.floor == 0) {
                    if (!p.isCreative() && r.nextFloat() < 0.5f) emerge(level, w, cx, cz);
                    continue;
                }
                // a floor or two lower, a window or two closer to you
                int nf = Math.max(0, w.floor - 1 - r.nextInt(2));
                int pt = (w.side < 2 ? Math.floorMod(p.getBlockZ(), P) : Math.floorMod(p.getBlockX(), P));
                int nt = w.column + Integer.signum(pt - w.column) * 3 * r.nextInt(2);
                if (!windowColumn(nt)) nt = w.column;
                place(level, w, cx, cz, w.side, nt, nf);
            }
        }

        static void place(ServerLevel level, WindowWatcher existing, int cx, int cz, int side, int t, int f) {
            BlockPos at = pocket(cx, cz, side, t, f);
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) return;
            WindowWatcher w = existing != null ? existing : WindowWatcher.TYPE.create(level);
            if (w == null) return;
            float yaw = switch (side) {
                case 0 -> -90f;
                case 1 -> 90f;
                case 2 -> 0f;
                default -> 180f;
            };
            w.side = side;
            w.column = t;
            w.floor = f;
            w.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
            w.setYHeadRot(yaw);
            if (existing == null) level.addFreshEntity(w);
        }

        /** The ground-floor window breaks, and it steps out onto the grass. */
        static void emerge(ServerLevel level, WindowWatcher w, int cx, int cz) {
            BlockPos g = glass(cx, cz, w.side, w.column, 0);
            level.setBlock(g, ModBlocks.AIR, Block.UPDATE_ALL);
            level.setBlock(g.above(), ModBlocks.AIR, Block.UPDATE_ALL);
            level.playSound(null, g, ModSounds.get("entity.window_watcher.break"), SoundSource.HOSTILE, 1.6f, 0.9f);
            BlockPos out = switch (w.side) {
                case 0 -> g.east();
                case 1 -> g.west();
                case 2 -> g.south();
                default -> g.north();
            };
            level.setBlock(out, ModBlocks.AIR, Block.UPDATE_ALL);
            w.outside = true;
            w.moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, w.getYRot(), 0f);
        }
    }
}
