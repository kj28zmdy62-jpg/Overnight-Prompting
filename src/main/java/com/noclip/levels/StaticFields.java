package com.noclip.levels;

import com.noclip.block.StaticTvBlock;
import com.noclip.entity.StaticMan;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static Fields. Endless grey grass under a grey sky. Somewhere out there an old
 * television sits on its stand with an armchair facing it, playing static. Watch it
 * long enough and someone appears in the picture. Then the picture is empty, and
 * he is standing behind you.
 */
public final class StaticFields {
    static final String ID = "static_fields";
    static final int BASE = 40;

    private StaticFields() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Static Fields").order(3000)
                .tagline("Grey grass, grey sky, one television. Don't watch it for too long.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Broadcast())
                .visuals(new Visuals().tint(0.97f, 1.0f, 0.98f).saturation(0.42f).contrast(0.96f).brightness(1.0f)
                        .grain(0.11f).aberration(0.0022f).vignette(0.48f).scanlines(0.09f).warp(0.025f)
                        .fog(4f, 58f).fogColor(0x9A9D98).bloom(0.2f))
                .sanityDrain(0.03f)
                .spawnY(BASE - 8, BASE + 10)
                .exit(ExitType.SECRET, "the_signal", 3).exit(ExitType.SECRET, "loading_screen", 1)
                .exit(ExitType.SECRET, "glitch_level", 1)
                .exit(ExitType.FALL, "storm_drains", 1).exit(ExitType.FALL, "level_0", 1)
                .exit(ExitType.NOCLIP, "level_10", 3).exit(ExitType.NOCLIP, "the_signal", 2)
                .exit(ExitType.NOCLIP, "pole_forest", 1).exit(ExitType.NOCLIP, "golden_hour", 1)
                .exit(ExitType.NOCLIP, "salt_flats", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    static int surface(long s, int x, int z) {
        float h = Hash.fbm(s ^ 0x51L, x / 90.0, z / 90.0, 3);
        return BASE + (int) Math.floor((h - 0.5f) * 5f);
    }

    static boolean dead(long s, int x, int z) {
        return Hash.fbm(s ^ 0xDEADL, x / 30.0, z / 30.0, 3) > 0.66f;
    }

    static CellGrid tvGrid(long s) {
        return new CellGrid(s ^ 0x7EEL, 112, 30);
    }

    /** Where the television of a cell stands, or null if this cell has none. */
    static BlockPos tv(long s, CellGrid g, int i, int k) {
        long h = g.hash(i, k, 1);
        if (Hash.unit(h) > 0.6f) return null;
        int x = g.x0(i) + 28 + Hash.range(h >>> 8, 50);
        int z = g.z0(k) + 28 + Hash.range(h >>> 20, 50);
        return new BlockPos(x, surface(s, x, z) + 1, z);
    }

    static final class Gen implements LevelGenerator {
        final BlockState grass = ModBlocks.state("sf_grass");
        final BlockState deadGrass = ModBlocks.state("sf_dead_grass");
        final BlockState soil = ModBlocks.state("sf_soil");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState tall = ModBlocks.state("sf_tall_grass");
        final BlockState dry = ModBlocks.state("sf_dry_grass");
        final BlockState tvBlock = ModBlocks.state("sf_tv");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState imprintUp = ModBlocks.state("sf_imprint_upper");
        final BlockState imprintLow = ModBlocks.state("sf_imprint_lower");
        final BlockState fallen = ModBlocks.state("sf_tv_fallen");
        final BlockState post = ModBlocks.state("sf_fence_post");
        final BlockState pole = ModBlocks.state("sf_speaker_pole");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int h = surface(s, x, z);
                    w.fill(x, 0, z, x, h - 4, z, sub);
                    w.fill(x, h - 3, z, x, h - 1, z, soil);
                    boolean d = dead(s, x, z);
                    w.set(x, h, z, d ? deadGrass : grass);
                    float r = Hash.unit(s, x, z, 7);
                    if (r < (d ? 0.22f : 0.3f)) w.set(x, h + 1, z, d || r < 0.025f ? dry : tall);
                }
            }
            televisions(w, s);
            pieces(w, s);
        }

        void televisions(ChunkWriter w, long s) {
            CellGrid g = tvGrid(s);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                BlockPos tv = tv(s, g, i, k);
                if (tv == null) return;
                // a mown, level circle around the set, a chair facing it
                int ty = tv.getY() - 1;
                for (int dx = -5; dx <= 5; dx++)
                    for (int dz = -5; dz <= 6; dz++)
                        if (dx * dx + dz * dz <= 26) {
                            int x = tv.getX() + dx, z = tv.getZ() + dz;
                            if (!w.contains(x, z)) continue;
                            int h = surface(s, x, z);
                            w.fill(x, ty + 1, z, x, Math.max(h + 1, ty + 1), z, ModBlocks.AIR);
                            w.fill(x, ty - 3, z, x, ty - 1, z, soil);
                            w.set(x, ty, z, grass);
                        }
                w.set(tv.getX(), tv.getY(), tv.getZ(), tvBlock);
                w.set(tv.getX(), tv.getY(), tv.getZ() + 3, OutsideKit.facing("sf_armchair", Direction.NORTH));
                // the power cord wanders off east and goes into the ground
                long h = g.hash(i, k, 2);
                int len = 22 + Hash.range(h, 26);
                int x = tv.getX() + 1, z = tv.getZ();
                for (int n = 0; n < len; n++, x++) {
                    if (n > 2 && Hash.unit(h, n, 3) < 0.12f) {
                        int dz = Hash.unit(h, n, 4) < 0.5f ? 1 : -1;
                        z += dz;
                        cord(w, s, x, z, Direction.NORTH);
                    }
                    cord(w, s, x, z, Direction.EAST);
                }
            });
        }

        void cord(ChunkWriter w, long s, int x, int z, Direction d) {
            if (!w.contains(x, z)) return;
            int h = surface(s, x, z);
            for (int y = h + 2; y >= h - 3; y--) {
                if (w.isAir(x, y, z) && !w.isAir(x, y - 1, z)) {
                    w.set(x, y, z, OutsideKit.facing("sf_cord", d));
                    return;
                }
            }
        }

        void pieces(ChunkWriter w, long s) {
            CellGrid g = new CellGrid(s ^ 0x5E7L, 20, 6);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                long h = g.hash(i, k, 9);
                float r = Hash.unit(h);
                int x = x0 + 4 + Hash.range(h >>> 8, Math.max(1, wd - 8));
                int z = z0 + 4 + Hash.range(h >>> 16, Math.max(1, dp - 8));
                int y = surface(s, x, z);
                Direction d = Direction.from2DDataValue(Hash.range(h >>> 24, 4));
                if (r < 0.006f) {
                    // a second armchair, alone, facing nothing
                    w.set(x, y + 1, z, OutsideKit.facing("sf_armchair", d));
                } else if (r < 0.014f) {
                    // someone lay here for a long time
                    if (surface(s, x, z + 1) == y) {
                        w.set(x, y, z, imprintUp);
                        w.set(x, y, z + 1, imprintLow);
                        w.set(x, y + 1, z, ModBlocks.AIR);
                        w.set(x, y + 1, z + 1, ModBlocks.AIR);
                    }
                } else if (r < 0.024f) {
                    // a perfect circle of dead grass
                    int rad = 3 + Hash.range(h >>> 30, 3);
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++) {
                            int dd = dx * dx + dz * dz;
                            if (dd <= rad * rad && dd >= (rad - 1) * (rad - 1)) {
                                int xx = x + dx, zz = z + dz;
                                int hh = surface(s, xx, zz);
                                w.set(xx, hh, zz, deadGrass);
                                if (w.contains(xx, zz) && !w.isAir(xx, hh + 1, zz)) w.set(xx, hh + 1, zz, ModBlocks.AIR);
                            }
                        }
                } else if (r < 0.036f) {
                    // an old fence line that starts and stops for no reason
                    int len = 6 + Hash.range(h >>> 30, 10);
                    Direction along = (h & 1) == 0 ? Direction.EAST : Direction.SOUTH;
                    for (int n = 0; n < len; n++) {
                        int xx = x + along.getStepX() * n, zz = z + along.getStepZ() * n;
                        int hh = surface(s, xx, zz);
                        if (n % 3 == 0) w.set(xx, hh + 1, zz, post);
                        else w.set(xx, hh + 1, zz, OutsideKit.facing("sf_fence_wire", along == Direction.EAST ? Direction.NORTH : Direction.EAST));
                    }
                } else if (r < 0.040f) {
                    // a hole, two blocks long, straight down into the dark
                    for (int dz = 0; dz < 2; dz++) {
                        int hh = surface(s, x, z + dz);
                        for (int yy = 1; yy <= hh + 1; yy++) w.set(x, yy, z + dz, yy <= 3 ? pit : ModBlocks.AIR);
                    }
                } else if (r < 0.043f) {
                    w.set(x, y + 1, z, OutsideKit.facing(fallen, d));
                } else if (r < 0.047f) {
                    // a loudspeaker on a pole, pointed at the ground
                    for (int yy = 1; yy <= 5; yy++) w.set(x, y + yy, z, pole);
                    w.set(x, y + 6, z, OutsideKit.facing("sf_speaker", d));
                }
            });
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Each player has their own broadcast. Watch a television from close by and,
     * sooner or later, the man shows up in the picture. The moment you look away
     * he is out of the screen and behind you. Face him down and the set shows you a
     * door; turn your back on him and the set goes dark.
     */
    static final class Broadcast implements LevelBehavior {
        static final class St {
            int phase, look, timer, faced;
            long exposure;
            BlockPos tv;
            StaticMan man;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        Broadcast() {
            StaticMan.onResolved = (sp, faced) -> {
                St st = states.get(sp.getUUID());
                if (st == null || st.tv == null) return;
                if (faced) st.faced++;
                setMode(sp.serverLevel(), st.tv, faced ? 2 : 3);
            };
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            states.put(p.getUUID(), new St());
            if (OutsideKit.autotest()) {
                // development screenshots: stand in front of the nearest set
                long s = OutsideKit.seed(level, ID);
                CellGrid g = tvGrid(s);
                int i = g.cellX(p.getBlockX()), k = g.cellZ(p.getBlockZ());
                for (int r = 0; r < 4; r++) {
                    for (int di = -r; di <= r; di++)
                        for (int dk = -r; dk <= r; dk++) {
                            BlockPos tv = tv(s, g, i + di, k + dk);
                            if (tv == null) continue;
                            level.getChunk(tv);
                            p.teleportTo(level, tv.getX() + 1.5, tv.getY(), tv.getZ() + 5.5, 166f, 5f);
                            return;
                        }
                }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            st.exposure++;
            long s = OutsideKit.seed(level, ID);
            if (p.tickCount % 50 == 0) {
                BlockPos near = nearestTv(level, s, p, 56);
                if (near != null && mode(level, near) != 3) {
                    OutsideKit.sound(level, near, "event.static_fields_tv", 3.2f, mode(level, near) == 1 ? 0.7f : 1f);
                }
            }
            switch (st.phase) {
                case 0 -> {
                    if (p.tickCount % 5 != 0) return;
                    BlockPos tv = nearestTv(level, s, p, 26);
                    if (tv == null || mode(level, tv) != 0) {
                        st.look = Math.max(0, st.look - 5);
                        return;
                    }
                    if (OutsideKit.seen(p, Vec3.atCenterOf(tv), 0.9)) st.look += 5;
                    else st.look = Math.max(0, st.look - 3);
                    float chance = 0.05f + Math.min(0.25f, st.exposure / 24000f);
                    if (st.look > 70 && p.getRandom().nextFloat() < chance) {
                        st.tv = tv;
                        st.phase = 1;
                        st.timer = 0;
                        setMode(level, tv, 1);
                        OutsideKit.sound(level, tv, "event.static_fields_sting", 1.4f, 1f);
                    }
                }
                case 1 -> {
                    st.timer++;
                    Vec3 c = Vec3.atCenterOf(st.tv);
                    if (st.timer > 20 * 45 || p.distanceToSqr(c) > 48 * 48) {
                        setMode(level, st.tv, 0);
                        st.phase = 0;
                        st.look = 0;
                        return;
                    }
                    if (st.timer > 10 && !OutsideKit.inView(p, c, 0.3)) {
                        setMode(level, st.tv, 0);
                        st.man = spawnBehind(p, level);
                        st.phase = st.man != null ? 2 : 0;
                    }
                }
                case 2 -> {
                    if (st.man == null || st.man.isRemoved()) {
                        st.phase = 3;
                        st.timer = 20 * 25;
                        st.look = 0;
                    }
                }
                default -> {
                    if (--st.timer <= 0) st.phase = 0;
                }
            }
        }

        static StaticMan spawnBehind(ServerPlayer p, ServerLevel level) {
            for (int attempt = 0; attempt < 6; attempt++) {
                Vec3 at = OutsideKit.behind(p, 3.2 + attempt * 0.4, 50, p.getRandom());
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
                for (int y = p.getBlockY() + 3; y >= p.getBlockY() - 5; y--) {
                    m.set(Math.floor(at.x), y, Math.floor(at.z));
                    BlockPos below = m.below();
                    if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) continue;
                    if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) break;
                    if (!level.getBlockState(m.above()).getCollisionShape(level, m.above()).isEmpty()) break;
                    StaticMan man = StaticMan.TYPE.create(level);
                    if (man == null) return null;
                    man.moveTo(at.x, y, at.z, 0f, 0f);
                    man.haunt(p);
                    level.addFreshEntity(man);
                    OutsideKit.sound(level, m.immutable(), "event.static_fields_burst", 0.55f, 1.2f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.8f));
                    return man;
                }
            }
            return null;
        }

        static int mode(ServerLevel level, BlockPos pos) {
            BlockState st = level.getBlockState(pos);
            return st.getBlock() instanceof StaticTvBlock ? st.getValue(StaticTvBlock.MODE) : 3;
        }

        static void setMode(ServerLevel level, BlockPos pos, int mode) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof StaticTvBlock && st.getValue(StaticTvBlock.MODE) != mode) {
                level.setBlock(pos, st.setValue(StaticTvBlock.MODE, mode), Block.UPDATE_CLIENTS);
            }
        }

        static BlockPos nearestTv(ServerLevel level, long s, ServerPlayer p, double range) {
            CellGrid g = tvGrid(s);
            int px = p.getBlockX(), pz = p.getBlockZ();
            BlockPos best = null;
            double bd = range * range;
            for (int i = g.cellX(px - (int) range) ; i <= g.cellX(px + (int) range); i++)
                for (int k = g.cellZ(pz - (int) range); k <= g.cellZ(pz + (int) range); k++) {
                    BlockPos tv = tv(s, g, i, k);
                    if (tv == null) continue;
                    double d = p.distanceToSqr(Vec3.atCenterOf(tv));
                    if (d < bd && level.isLoaded(tv)) {
                        bd = d;
                        best = tv;
                    }
                }
            return best;
        }
    }
}
