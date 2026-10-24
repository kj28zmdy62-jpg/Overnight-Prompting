package com.noclip.levels;

import com.noclip.entity.GrHorizon;
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
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Grid. A black plain ruled every eight blocks with glowing cyan lines,
 * wireframe cubes and towers standing on it or hanging over it, and on the
 * northern horizon a striped sun that stays exactly as far away however long
 * you walk toward it. Light pulses run along the lines, always toward the
 * same kind of place: a node where the lines fail to meet and the floor
 * simply is not there. Follow them.
 */
public final class TheGrid {
    static final int FLOOR = 16, LINE = 8, REGION = 96, HORIZON = 84;

    private TheGrid() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("the_grid")
                .number("").title("The Grid").order(4050)
                .tagline("The horizon never gets closer. Follow the light along the lines.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Pulses())
                .visuals(new Visuals().tint(0.92f, 1.0f, 1.1f).saturation(1.2f).contrast(1.15f).brightness(1.0f)
                        .grain(0.05f).aberration(0.0025f).vignette(0.55f).scanlines(0.08f).fog(26f, 150f).fogColor(0x02040A)
                        .bloom(1.0f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("gr_sentinel"), 2, 0.22f).dist(22, 40))
                .exit(ExitType.FALL, "the_void", 2).exit(ExitType.FALL, "level_0", 1).exit(ExitType.FALL, "loading_screen", 1)
                .exit(ExitType.FALL, "glitch_level", 1)
                .exit(ExitType.NOCLIP, "glitch_level", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .build();
    }

    static int[] fault(long s, int rx, int rz) {
        return new int[]{rx * REGION + LINE * (2 + Hash.range(s, rx, rz, 0xFA, REGION / LINE - 4)),
                rz * REGION + LINE * (2 + Hash.range(s, rx, rz, 0xFB, REGION / LINE - 4))};
    }

    /** Within the broken patch around a fault: the hole, and the lines that stop short of it. */
    static int faultDist(long s, int x, int z) {
        int[] f = fault(s, Math.floorDiv(x, REGION), Math.floorDiv(z, REGION));
        return Math.max(Math.abs(x - f[0]), Math.abs(z - f[1]));
    }

    static BlockState lineAt(int x, int z) {
        boolean ax = Math.floorMod(z, LINE) == 0, az = Math.floorMod(x, LINE) == 0;
        if (ax && az) return ModBlocks.state("gr_cross");
        if (ax) return ModBlocks.state("gr_line_x");
        if (az) return ModBlocks.state("gr_line_z");
        return ModBlocks.state("gr_black");
    }

    // ---------------------------------------------------------------- behaviour

    static final class Pulses implements LevelBehavior {
        static final class Pulse {
            int x, z, tx, tz, life;
        }

        static final class St {
            int horizon = -1;
            final List<Pulse> pulses = new ArrayList<>();
        }

        private final Map<UUID, St> state = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St s = state.computeIfAbsent(p.getUUID(), u -> new St());
            // the horizon: always the same distance to the north
            GrHorizon h = level.getEntity(s.horizon) instanceof GrHorizon g ? g : null;
            if (h == null) {
                h = GrHorizon.TYPE.create(level);
                if (h == null) return;
                h.moveTo(p.getX(), FLOOR - 6, p.getZ() - HORIZON, 0f, 0f);
                level.addFreshEntity(h);
                s.horizon = h.getId();
            }
            h.absMoveTo(p.getX(), FLOOR - 6, p.getZ() - HORIZON, 0f, 0f);
            h.setYHeadRot(0f);
            h.yBodyRot = 0f;

            if (p.tickCount % 2 != 0) return;
            long seed = level.getSeed();
            if (p.tickCount % 36 == 0 && s.pulses.size() < 4) spawn(p, seed, s);
            for (Iterator<Pulse> it = s.pulses.iterator(); it.hasNext(); ) {
                Pulse q = it.next();
                BlockPos old = new BlockPos(q.x, FLOOR, q.z);
                if (level.isLoaded(old) && level.getBlockState(old).is(ModBlocks.get("gr_pulse")))
                    level.setBlock(old, lineAt(q.x, q.z), Block.UPDATE_CLIENTS);
                if (q.x != q.tx) q.x += Integer.signum(q.tx - q.x);
                else if (Math.abs(q.z - q.tz) > 4) q.z += Integer.signum(q.tz - q.z);
                else q.life = 0;
                if (--q.life <= 0) {
                    it.remove();
                    continue;
                }
                BlockPos at = new BlockPos(q.x, FLOOR, q.z);
                if (!level.isLoaded(at)) continue;
                BlockState cur = level.getBlockState(at);
                if (cur.is(ModBlocks.get("gr_line_x")) || cur.is(ModBlocks.get("gr_line_z")) || cur.is(ModBlocks.get("gr_cross"))) {
                    level.setBlock(at, ModBlocks.state("gr_pulse"), Block.UPDATE_CLIENTS);
                    if (at.distSqr(p.blockPosition()) < 9) level.playSound(null, at, ModSounds.get("event.the_grid_blip"), SoundSource.AMBIENT, 0.3f, 1f);
                }
            }
        }

        /** A pulse starts at a node near you and runs for the nearest place the lines break. */
        void spawn(ServerPlayer p, long seed, St s) {
            int nx = Math.round((float) (p.getX() / LINE)) * LINE + (p.getRandom().nextInt(3) - 1) * LINE;
            int nz = Math.round((float) (p.getZ() / LINE)) * LINE + (p.getRandom().nextInt(3) - 1) * LINE;
            int rx = Math.floorDiv(nx, REGION), rz = Math.floorDiv(nz, REGION);
            int[] best = null;
            double bd = Double.MAX_VALUE;
            for (int a = -1; a <= 1; a++)
                for (int b = -1; b <= 1; b++) {
                    int[] f = fault(seed, rx + a, rz + b);
                    double d = Math.hypot(f[0] - nx, f[1] - nz);
                    if (d < bd) {
                        bd = d;
                        best = f;
                    }
                }
            Pulse q = new Pulse();
            q.x = nx;
            q.z = nz;
            q.tx = best[0];
            q.tz = best[1];
            q.life = 56;
            s.pulses.add(q);
        }
    }

    // ---------------------------------------------------------------- the plain

    static final class Gen implements LevelGenerator {
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState black = ModBlocks.state("gr_black");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState node = ModBlocks.state("gr_wire_node");
        final BlockState wy = ModBlocks.state("gr_wire_y");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 2, w.minZ, w.maxX, FLOOR - 1, w.maxZ, filler);
            w.layer(1, pit);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int fd = faultDist(s, x, z);
                    if (fd <= 1) {
                        // the floor is not there
                        w.fill(x, 2, z, x, FLOOR, z, ModBlocks.AIR);
                    } else if (fd <= 4) {
                        w.set(x, FLOOR, z, black);
                    } else {
                        w.set(x, FLOOR, z, lineAt(x, z));
                    }
                }
            CellGrid g = new CellGrid(s, 28, 6);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> shape(w, s, g, i, k, x0, z0));
        }

        BlockState h(Direction d) {
            return ModBlocks.state("gr_wire_h").setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void shape(ChunkWriter w, long s, CellGrid g, int i, int k, int x0, int z0) {
            long hh = g.hash(i, k, 0x5A9E);
            float r = Hash.unit(hh);
            if (r > 0.45f) return;
            int n = 3 + Hash.range(hh >>> 8, 7);
            int tall = r < 0.12f ? n * 3 : n;
            int x = x0 + 3 + Hash.range(hh >>> 16, 10), z = z0 + 3 + Hash.range(hh >>> 24, 10);
            if (faultDist(s, x + n / 2, z + n / 2) < n + 4) return;
            int y = FLOOR + 1 + (Hash.unit(hh >>> 32) < 0.5f ? 0 : 3 + Hash.range(hh >>> 36, 12));
            box(w, x, y, z, n, tall, n, Hash.unit(hh >>> 44) < 0.15f);
        }

        /** The twelve edges of a box, glowing. */
        void box(ChunkWriter w, int x, int y, int z, int sx, int sy, int sz, boolean red) {
            BlockState vert = red ? ModBlocks.state("gr_wire_red") : wy;
            for (int[] c : new int[][]{{0, 0}, {sx, 0}, {0, sz}, {sx, sz}}) {
                for (int t = 1; t < sy; t++) w.set(x + c[0], y + t, z + c[1], vert);
                w.set(x + c[0], y, z + c[1], node);
                w.set(x + c[0], y + sy, z + c[1], node);
            }
            for (int yy : new int[]{y, y + sy}) {
                for (int t = 1; t < sx; t++) {
                    w.set(x + t, yy, z, h(Direction.EAST));
                    w.set(x + t, yy, z + sz, h(Direction.EAST));
                }
                for (int t = 1; t < sz; t++) {
                    w.set(x, yy, z + t, h(Direction.NORTH));
                    w.set(x + sx, yy, z + t, h(Direction.NORTH));
                }
            }
        }
    }
}
