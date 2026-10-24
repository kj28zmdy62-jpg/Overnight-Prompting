package com.noclip.levels;

import com.noclip.block.AlarmBeaconBlock;
import com.noclip.entity.TheDrill;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bunker. Corridors painted olive below and cream above with a red stripe
 * between, bunk rooms, map rooms, radio rooms, all under caged bulbs. Blast doors
 * stand open in the corridors. Every few minutes the alarm starts: the power
 * drops to red emergency light, the blast doors close wherever you are, and the
 * Drill walks the halls until it stops.
 */
public final class Bunker {
    static final int FLOOR = 10, CEIL = 14, CELL = 14;

    private Bunker() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("bunker")
                .number("").title("The Bunker").order(2060)
                .tagline("When the klaxon sounds, the doors close. Stay out of the corridors.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Lockdown())
                .visuals(new Visuals().tint(1.02f, 1.0f, 0.88f).saturation(0.72f).contrast(1.1f).brightness(1.3f)
                        .grain(0.09f).aberration(0.0015f).vignette(0.6f).fog(3f, 34f).fogColor(0x1C1E18).bloom(0.6f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(18, 32).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.45f).dark().dist(10, 22).weight(2))
                .exit(ExitType.NOCLIP, "level_3", 2).exit(ExitType.NOCLIP, "power_plant", 1).exit(ExitType.NOCLIP, "elevator_shaft", 1)
                .exit(ExitType.DOOR, "power_plant", 2).exit(ExitType.DOOR, "elevator_shaft", 1).exit(ExitType.DOOR, "level_3", 1)
                .exit(ExitType.ASCEND, "static_fields", 2).exit(ExitType.ASCEND, "level_11", 1).exit(ExitType.ASCEND, "construction_site", 1)
                .exit(ExitType.SECRET, "the_hub", 2).exit(ExitType.SECRET, "level_0", 1)
                .build();
    }

    /** Corridor cross-section at a blast door: {x, z, along-x?} or null. */
    static int[] blastDoor(long s, int i, int k, int dir) {
        if (!Hash.chance(s, i, k, 40 + dir, 0.45f)) return null;
        int x0 = i * CELL, z0 = k * CELL;
        return dir == 0 ? new int[]{x0 + 6, z0, 1} : new int[]{x0, z0 + 6, 0};
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState low = ModBlocks.state("bk_wall_low");
        final BlockState stripe = ModBlocks.state("bk_wall_stripe");
        final BlockState high = ModBlocks.state("bk_wall_high");
        final BlockState[] stencils = {ModBlocks.state("bk_stencil_sector"), ModBlocks.state("bk_stencil_air"),
                ModBlocks.state("bk_stencil_nosmoke"), ModBlocks.state("bk_stencil_masks")};
        final BlockState ceiling = ModBlocks.state("bk_ceiling");
        final BlockState floor = ModBlocks.state("bk_floor");
        final BlockState frame = ModBlocks.state("bk_frame");
        final BlockState bunk = ModBlocks.state("bk_bunk");
        final BlockState locker = ModBlocks.state("bk_locker");
        final BlockState radio = ModBlocks.state("bk_radio");
        final BlockState table = ModBlocks.state("bk_table");
        final BlockState bulb = ModBlocks.state("bk_bulb");
        final BlockState beacon = ModBlocks.state("bk_beacon");
        final BlockState hatch = ModBlocks.state("bk_hatch");
        final BlockState door = ModBlocks.state("bk_steel_door");
        final BlockState ladder = ModBlocks.state("bk_ladder");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState crate = ModBlocks.state("wooden_crate");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
                    boolean corridor = lx <= 2 || lz <= 2;
                    boolean wall = !corridor && (lx == 3 || lz == 3 || lx == CELL - 1 || lz == CELL - 1);
                    if (wall) {
                        w.set(x, FLOOR + 1, z, low);
                        w.set(x, FLOOR + 2, z, Hash.chance(s, x, z, 5, 0.025f) ? stencils[Hash.range(s, x, z, 6, stencils.length)] : stripe);
                        w.set(x, FLOOR + 3, z, high);
                    } else if (corridor) {
                        boolean centre = (lx == 1 && Math.floorMod(z, 5) == 2) || (lz == 1 && Math.floorMod(x, 5) == 2);
                        if (centre) w.set(x, CEIL - 1, z, bulb);
                        if (lx == 1 && lz == 1) w.set(x, CEIL - 1, z, beacon);
                        else if ((lx == 1 && lz == 7) || (lz == 1 && lx == 7)) w.set(x, CEIL - 1, z, beacon);
                    }
                }
            }
            int ci0 = Math.floorDiv(w.minX, CELL) - 1, ci1 = Math.floorDiv(w.maxX, CELL) + 1;
            int ck0 = Math.floorDiv(w.minZ, CELL) - 1, ck1 = Math.floorDiv(w.maxZ, CELL) + 1;
            for (int i = ci0; i <= ci1; i++)
                for (int k = ck0; k <= ck1; k++) {
                    for (int dir = 0; dir < 2; dir++) blastFrame(w, blastDoor(s, i, k, dir));
                    room(w, s, i, k, i * CELL, k * CELL);
                }
        }

        void blastFrame(ChunkWriter w, int[] bd) {
            if (bd == null) return;
            for (int a = -1; a <= 3; a++) {
                int x = bd[2] == 1 ? bd[0] : bd[0] + a, z = bd[2] == 1 ? bd[1] + a : bd[1];
                w.set(x, FLOOR, z, frame);
                w.set(x, CEIL, z, frame);
                if (a == -1 || a == 3) for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, frame);
            }
        }

        static BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void room(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            long h = Hash.hash(s, i, k, 50);
            float r = Hash.unit(h);
            int ax = x0 + 4, az = z0 + 4, bx = x0 + CELL - 2, bz = z0 + CELL - 2;
            // doorways from the corridors
            boolean west = Hash.unit(h >>> 8) < 0.7f, north = !west || Hash.unit(h >>> 12) < 0.4f;
            if (west) for (int y = FLOOR + 1; y <= FLOOR + 2; y++) w.set(x0 + 3, y, z0 + 8, ModBlocks.AIR);
            if (north) for (int y = FLOOR + 1; y <= FLOOR + 2; y++) w.set(x0 + 8, y, z0 + 3, ModBlocks.AIR);
            if (Hash.unit(h >>> 16) < 0.07f) {
                w.set(x0 + 10, FLOOR + 1, z0 + 3, door);
                w.set(x0 + 10, FLOOR + 2, z0 + 3, door);
            }
            boolean lit = r < 0.86f || r >= 0.96f;
            if (lit) {
                w.set(x0 + 8, CEIL - 1, z0 + 6, bulb);
                w.set(x0 + 8, CEIL - 1, z0 + 10, bulb);
            }
            if (r < 0.30f) {
                // bunks against the east and south walls, lockers by the door
                for (int z = az + 1; z <= bz; z += 2) w.set(bx, FLOOR + 1, z, face(bunk, Direction.NORTH));
                for (int x = ax + 2; x < bx; x += 2) w.set(x, FLOOR + 1, bz, face(bunk, Direction.EAST));
                for (int z = az; z < az + 3; z++) w.set(ax, FLOOR + 1, z + 1, face(locker, Direction.EAST));
                w.set(ax + 3, FLOOR + 1, az + 3, table);
                if (Hash.unit(h >>> 20) < 0.3f) w.set(ax + 3, FLOOR + 2, az + 3, bottle);
            } else if (r < 0.45f) {
                mapRoom(w, h, x0, z0, false);
            } else if (r < 0.60f) {
                for (int x = ax + 1; x < bx; x += 3) {
                    for (int z = az + 1; z < bz; z++) {
                        w.set(x, FLOOR + 1, z, table);
                        if (Hash.chance(h, x, z, 0.4f)) w.set(x + 1, FLOOR + 1, z, face(chair, Direction.WEST));
                    }
                }
            } else if (r < 0.72f) {
                for (int z = az + 1; z < bz; z++) {
                    w.set(bx, FLOOR + 1, z, table);
                    w.set(bx, FLOOR + 2, z, face(radio, Direction.WEST));
                    w.set(bx - 1, FLOOR + 1, z, face(chair, Direction.EAST));
                }
            } else if (r < 0.80f) {
                for (int x = ax + 1; x <= bx; x += 2)
                    for (int y = FLOOR + 1; y <= FLOOR + 2; y++) w.set(x, y, bz, face(locker, Direction.NORTH));
                if (Hash.unit(h >>> 24) < 0.5f) w.set(ax + 2, FLOOR + 1, bz - 2, supply);
            } else if (r < 0.86f) {
                // a dark room with one chair facing the corner
                w.set(bx - 1, FLOOR + 1, bz - 1, face(chair, Direction.SOUTH));
            } else if (r < 0.90f) {
                for (int y = FLOOR + 1; y < CEIL; y++) w.set(ax + 4, y, az + 4, ladder.setValue(LadderBlock.FACING, Direction.SOUTH));
                w.set(ax + 4, FLOOR + 1, az + 3, low);
                w.set(ax + 4, FLOOR + 2, az + 3, stripe);
                w.set(ax + 4, FLOOR + 3, az + 3, high);
                w.set(ax + 4, CEIL, az + 4, hatch);
            } else if (r < 0.94f) {
                mapRoom(w, h, x0, z0, true);
            } else {
                for (int n = 0; n < 6; n++) {
                    long hh = Hash.hash(h, n);
                    w.set(ax + 1 + Hash.range(hh, 7), FLOOR + 1, az + 1 + Hash.range(hh >>> 8, 7), Hash.unit(hh >>> 16) < 0.15f ? supply : crate);
                }
            }
        }

        /** A table in the middle and a big map standing against the west wall. */
        void mapRoom(ChunkWriter w, long h, int x0, int z0, boolean command) {
            String kind = command ? "bk_map_br_" : "bk_map_topo_";
            int x = x0 + 4;
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 2; j++) w.set(x, FLOOR + 2 + j, z0 + 11 - i, ModBlocks.state(kind + i + j));
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) w.set(x0 + 8 + dx, FLOOR + 1, z0 + 8 + dz, table);
            w.set(x0 + 7, FLOOR + 1, z0 + 8, face(ModBlocks.state("office_chair"), Direction.EAST));
            if (command) {
                w.set(x0 + 12, FLOOR + 1, z0 + 6, table);
                w.set(x0 + 12, FLOOR + 2, z0 + 6, face(radio, Direction.WEST));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The alarm: red light, sealed doors, and the Drill. */
    static final class Lockdown implements LevelBehavior {
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Map<ResourceKey<Level>, List<BlockPos>> SEALED = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 2000 + level.random.nextInt(2000)});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0 && now >= st[2]) {
                st[0] = 1;
                st[1] = now + 900;
                Blackouts.start(level, 900);
                long seed = Hash.hash(level.getSeed(), Hash.salt("bunker"));
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.bunker_pa"), SoundSource.AMBIENT, 1.2f, 0.9f);
                    p.displayClientMessage(Component.literal("LOCKDOWN. REMAIN WHERE YOU ARE.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                    seal(level, p, seed, true);
                    beacons(level, p, true);
                    if (!p.isCreative() && !p.isSpectator()) spawnDrill(level, p);
                }
            } else if (st[0] == 1) {
                if (now % 40 == 0) {
                    for (ServerPlayer p : level.players())
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.bunker_klaxon"), SoundSource.AMBIENT, 1.0f, 1f);
                }
                if (now >= st[1]) {
                    st[0] = 0;
                    st[2] = now + 3600 + level.random.nextInt(3000);
                    unseal(level);
                    for (ServerPlayer p : level.players()) {
                        beacons(level, p, false);
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.bunker_pa"), SoundSource.AMBIENT, 1.0f, 1.1f);
                        p.displayClientMessage(Component.literal("Drill concluded.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    }
                    for (TheDrill d : level.getEntities(TheDrill.TYPE, e -> true)) d.discard();
                }
            }
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (state(level)[0] == 1) beacons(level, p, true);
        }

        private static void seal(ServerLevel level, ServerPlayer p, long seed, boolean close) {
            List<BlockPos> list = SEALED.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
            BlockState door = ModBlocks.state("bk_blast_door");
            int pi = Math.floorDiv(p.getBlockX(), CELL), pk = Math.floorDiv(p.getBlockZ(), CELL);
            for (int i = pi - 2; i <= pi + 2; i++) {
                for (int k = pk - 2; k <= pk + 2; k++) {
                    for (int dir = 0; dir < 2; dir++) {
                        int[] bd = blastDoor(seed, i, k, dir);
                        if (bd == null) continue;
                        boolean any = false;
                        for (int a = 0; a < 3; a++) {
                            for (int y = FLOOR + 1; y < CEIL; y++) {
                                BlockPos q = new BlockPos(bd[2] == 1 ? bd[0] : bd[0] + a, y, bd[2] == 1 ? bd[1] + a : bd[1]);
                                if (!level.isLoaded(q) || !level.getBlockState(q).isAir()) continue;
                                level.setBlock(q, door, Block.UPDATE_ALL);
                                list.add(q);
                                any = true;
                            }
                        }
                        if (any) level.playSound(null, new BlockPos(bd[0], FLOOR + 2, bd[1]), ModSounds.get("event.bunker_blast_door"),
                                SoundSource.BLOCKS, 1.4f, 0.9f + level.random.nextFloat() * 0.2f);
                    }
                }
            }
        }

        private static void unseal(ServerLevel level) {
            List<BlockPos> list = SEALED.remove(level.dimension());
            if (list == null) return;
            Block door = ModBlocks.get("bk_blast_door");
            for (BlockPos q : list) {
                if (level.isLoaded(q) && level.getBlockState(q).is(door)) level.setBlock(q, ModBlocks.AIR, Block.UPDATE_ALL);
            }
        }

        private static void beacons(ServerLevel level, ServerPlayer p, boolean on) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = p.getBlockX() - 32; x <= p.getBlockX() + 32; x++) {
                for (int z = p.getBlockZ() - 32; z <= p.getBlockZ() + 32; z++) {
                    m.set(x, CEIL - 1, z);
                    if (!level.isLoaded(m)) continue;
                    BlockState b = level.getBlockState(m);
                    if (b.getBlock() instanceof AlarmBeaconBlock && b.getValue(AlarmBeaconBlock.LIT) != on)
                        level.setBlock(m, b.setValue(AlarmBeaconBlock.LIT, on), Block.UPDATE_CLIENTS);
                }
            }
        }

        private static void spawnDrill(ServerLevel level, ServerPlayer p) {
            for (int attempt = 0; attempt < 20; attempt++) {
                double ang = level.random.nextDouble() * Math.PI * 2, dist = 12 + level.random.nextDouble() * 12;
                BlockPos c = BlockPos.containing(p.getX() + Math.cos(ang) * dist, FLOOR + 1, p.getZ() + Math.sin(ang) * dist);
                if (!level.isLoaded(c) || !level.getBlockState(c).isAir() || !level.getBlockState(c.above()).isAir()) continue;
                if (level.clip(new ClipContext(p.getEyePosition(), Vec3.atCenterOf(c.above()), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS) continue;
                TheDrill d = TheDrill.TYPE.create(level);
                if (d == null) return;
                d.moveTo(c.getX() + 0.5, FLOOR + 1, c.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
                d.finalizeSpawn(level, level.getCurrentDifficultyAt(c), MobSpawnType.EVENT, null);
                level.addFreshEntity(d);
                return;
            }
        }
    }
}
