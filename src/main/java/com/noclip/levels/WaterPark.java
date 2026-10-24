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
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Closed Water Park. An indoor dome at night: drained pools glowing blue from
 * their underwater lights, slide towers whose tubes start in the dark, lifeguard
 * chairs, loungers, chlorine in the air. No running on the deck: the lifeguard
 * you cannot see will blow the whistle, and the deck is wet. Every few minutes
 * the wave pool opens again: it fills in seconds, and the undertow pulls toward
 * the main drain.
 */
public final class WaterPark {
    static final int FLOOR = 12, CEIL = FLOOR + 16, CELL = 30;

    private WaterPark() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("water_park")
                .number("").title("Closed Water Park").order(1070)
                .tagline("Pools closed. No running. The wave pool opens every hour.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Lifeguard())
                .visuals(new Visuals().tint(0.92f, 1.04f, 1.02f).saturation(0.85f).contrast(1.06f).brightness(1.25f)
                        .grain(0.07f).aberration(0.0016f).vignette(0.5f).fog(4f, 46f).fogColor(0x4A6A62).bloom(0.6f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(26, 44).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.35f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_37", 2).exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "laundromat", 1)
                .exit(ExitType.FALL, "level_37", 2).exit(ExitType.FALL, "level_7", 1).exit(ExitType.FALL, "storm_drains", 1)
                .exit(ExitType.WATER, "level_7", 2).exit(ExitType.WATER, "sewers", 1).exit(ExitType.WATER, "level_37", 1)
                .build();
    }

    enum Kind { LAP, WAVE, SLIDE, KIDDIE, LOUNGE }

    static Kind kind(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0x3A7E);
        if (r < 0.28f) return Kind.LAP;
        if (r < 0.42f) return Kind.WAVE;
        if (r < 0.57f) return Kind.SLIDE;
        if (r < 0.70f) return Kind.KIDDIE;
        return Kind.LOUNGE;
    }

    /** Wave pool floor height at local (u, v), or Integer.MIN_VALUE outside the pool. */
    static int waveBottom(int u, int v) {
        if (u < 3 || u > 26 || v < 3 || v > 26) return Integer.MIN_VALUE;
        return FLOOR - 1 - (v - 3) / 6;
    }

    static final class Gen implements LevelGenerator {
        final BlockState deck = ModBlocks.state("wp_deck");
        final BlockState pool = ModBlocks.state("wp_pool");
        final BlockState lane = ModBlocks.state("wp_pool_lane");
        final BlockState puddle = ModBlocks.state("wp_pool_puddle");
        final BlockState coping = ModBlocks.state("wp_coping");
        final BlockState drain = ModBlocks.state("wp_drain");
        final BlockState roof = ModBlocks.state("wp_glass_roof");
        final BlockState scaffold = ModBlocks.state("wp_scaffold");
        final BlockState grating = ModBlocks.state("wp_grating");
        final BlockState steps = ModBlocks.state("wp_steps");
        final BlockState ladder = ModBlocks.state("wp_ladder");
        final BlockState mouth = ModBlocks.state("wp_slide_mouth");
        final BlockState poolLight = ModBlocks.state("wp_pool_light");
        final BlockState lamp = ModBlocks.state("wp_lamp");
        final BlockState lounger = ModBlocks.state("wp_lounger");
        final BlockState guardBase = ModBlocks.state("wp_lifeguard_base");
        final BlockState guardSeat = ModBlocks.state("wp_lifeguard_seat");
        final BlockState pole = ModBlocks.state("wp_umbrella_pole");
        final BlockState umbrella = ModBlocks.state("wp_umbrella_top");
        final BlockState palm = ModBlocks.state("wp_palm_leaves");
        final BlockState ring = ModBlocks.state("wp_ring");
        final BlockState wall = ModBlocks.state("wp_wall");
        final BlockState signClosed = ModBlocks.state("wp_sign_closed");
        final BlockState signNoRun = ModBlocks.state("wp_sign_norun");
        final BlockState signWaves = ModBlocks.state("wp_sign_waves");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] slides = {ModBlocks.state("wp_slide_orange"), ModBlocks.state("wp_slide_teal"), ModBlocks.state("wp_slide_purple")};
        final BlockState[] depth = {ModBlocks.state("wp_pool_3ft"), ModBlocks.state("wp_pool_5ft"), ModBlocks.state("wp_pool_9ft")};

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, CELL, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, deck);
            w.layer(CEIL, roof);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, s, i, k, x0, z0));
        }

        void lampPost(ChunkWriter w, int x, int z) {
            w.fill(x, FLOOR + 1, z, x, FLOOR + 3, z, pole.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set(x, FLOOR + 4, z, lamp);
        }

        void cell(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            long h = Hash.hash(s, i, k, 0x77);
            int y = FLOOR + 1;
            lampPost(w, x0, z0);
            lampPost(w, x0 + 15, z0);
            switch (kind(s, i, k)) {
                case LAP -> lap(w, h, x0, z0);
                case WAVE -> wave(w, h, x0, z0);
                case SLIDE -> slide(w, h, x0, z0);
                case KIDDIE -> kiddie(w, h, x0, z0);
                case LOUNGE -> lounge(w, h, x0, z0);
            }
        }

        void dig(ChunkWriter w, int x, int z, int bottom, BlockState floorB) {
            w.fill(x, bottom + 1, z, x, FLOOR, z, ModBlocks.AIR);
            w.set(x, bottom, z, floorB);
            w.fill(x, bottom - 2, z, x, bottom - 1, z, pool);
        }

        void lap(ChunkWriter w, long h, int x0, int z0) {
            int y = FLOOR + 1;
            for (int u = 4; u <= 25; u++)
                for (int v = 5; v <= 24; v++) {
                    int x = x0 + u, z = z0 + v;
                    boolean edge = u == 4 || u == 25 || v == 5 || v == 24;
                    if (edge) {
                        w.set(x, FLOOR, z, coping);
                        continue;
                    }
                    int bottom = v <= 11 ? FLOOR - 3 : FLOOR - 5;
                    BlockState f = (u % 4 == 2) ? lane : Hash.chance(h, x, z, 0.08f) ? puddle : pool;
                    if (u == 14 && v == 20) f = ModBlocks.state("wp_drain");
                    dig(w, x, z, bottom, f);
                    // the walls are tile; the first ring inside the coping is wall down to the floor
                    if (u == 5 || u == 24 || v == 6 || v == 23) {
                        w.fill(x, bottom + 1, z, x, FLOOR - 1, z, pool);
                        if ((u == 5 || u == 24) && v % 5 == 0) w.set(x, FLOOR - 2 + (v <= 11 ? 0 : -1), z, poolLight);
                        if ((u == 5 || u == 24) && (v == 8 || v == 16)) w.set(x, FLOOR - 1, z, depth[v == 8 ? 0 : 2]);
                    }
                }
            // a ladder at the shallow end
            for (int yy = FLOOR - 2; yy <= FLOOR; yy++) {
                w.set(x0 + 10, yy, z0 + 7, ladder.setValue(LadderBlock.FACING, Direction.SOUTH));
                w.set(x0 + 10, yy, z0 + 6, pool);
            }
            w.set(x0 + 10, FLOOR, z0 + 6, coping);
            // the lifeguard chair, empty
            w.set(x0 + 2, y, z0 + 14, guardBase.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            w.set(x0 + 2, y + 1, z0 + 14, guardSeat.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            w.set(x0 + 14, y, z0 + 3, signClosed);
        }

        void wave(ChunkWriter w, long h, int x0, int z0) {
            for (int u = 2; u <= 27; u++)
                for (int v = 2; v <= 27; v++) {
                    int x = x0 + u, z = z0 + v;
                    int b = waveBottom(u, v);
                    if (b == Integer.MIN_VALUE) {
                        if (v >= 2 && (u == 2 || u == 27 || v == 27)) w.set(x, FLOOR, z, coping);
                        continue;
                    }
                    BlockState f = Hash.chance(h, x, z, 0.06f) ? puddle : pool;
                    if (u == 14 && v == 25) f = ModBlocks.state("wp_drain");
                    dig(w, x, z, b, f);
                    // side and back walls of tile with lights
                    if (u == 3 || u == 26 || v == 26) {
                        w.fill(x, b + 1, z, x, FLOOR - 1, z, pool);
                        if (v % 6 == 0 || (v == 26 && u % 6 == 0)) w.set(x, Math.min(FLOOR - 1, b + 1), z, poolLight);
                    }
                }
            w.set(x0 + 14, FLOOR + 1, z0 + 1, signWaves);
            w.set(x0 + 15, FLOOR + 1, z0 + 1, signNoRun);
        }

        void slide(ChunkWriter w, long h, int x0, int z0) {
            int y = FLOOR + 1, top = FLOOR + 8;
            BlockState tube = slides[Hash.range(h, 3)];
            // tower
            for (int u = 12; u <= 15; u++)
                for (int v = 12; v <= 15; v++) {
                    boolean corner = (u == 12 || u == 15) && (v == 12 || v == 15);
                    if (corner) w.fill(x0 + u, y, z0 + v, x0 + u, top - 1, z0 + v, scaffold);
                    w.set(x0 + u, top, z0 + v, grating);
                }
            // stairs up from the west
            for (int st = 0; st < 8; st++)
                for (int v = 13; v <= 14; v++) {
                    int x = x0 + 4 + st;
                    w.set(x, y + st, z0 + v, steps.setValue(StairBlock.FACING, Direction.EAST));
                    if (st > 0) w.fill(x, y, z0 + v, x, y + st - 1, z0 + v, scaffold);
                }
            // the slide starts here. inside it is completely dark.
            for (int v = 13; v <= 14; v++) w.fill(x0 + 16, top + 1, z0 + v, x0 + 16, top + 2, z0 + v, mouth);
            for (int v = 12; v <= 15; v += 3) w.fill(x0 + 16, top + 1, z0 + v, x0 + 16, top + 3, z0 + v, tube);
            w.fill(x0 + 16, top + 3, z0 + 13, x0 + 16, top + 3, z0 + 14, tube);
            // and the tube winds down and away, outside the tower
            for (int st = 1; st <= 10; st++) {
                int x = x0 + 16 + st, yy = top + 2 - st;
                if (yy < y) break;
                w.fill(x, yy, z0 + 12, x, yy + 1, z0 + 15, tube);
            }
        }

        void kiddie(ChunkWriter w, long h, int x0, int z0) {
            for (int u = 8; u <= 21; u++)
                for (int v = 8; v <= 21; v++) {
                    double d = Math.hypot(u - 14.5, v - 14.5);
                    int x = x0 + u, z = z0 + v;
                    if (d > 6.8) continue;
                    if (d > 5.8) {
                        w.set(x, FLOOR, z, coping);
                        continue;
                    }
                    // the only pool that still has water in it. it is very still.
                    w.set(x, FLOOR - 1, z, d < 1 ? poolLight : pool);
                    w.set(x, FLOOR, z, water);
                }
            w.set(x0 + 14, FLOOR + 1, z0 + 6, umbrella.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }

        void lounge(ChunkWriter w, long h, int x0, int z0) {
            int y = FLOOR + 1;
            for (int row = 0; row < 4; row++)
                for (int n = 0; n < 6; n++) {
                    long hh = Hash.hash(h, row, n);
                    if (Hash.unit(hh) < 0.15f) continue;
                    int x = x0 + 4 + n * 4, z = z0 + 5 + row * 6;
                    Direction d = Hash.unit(hh >>> 8) < 0.06f ? Direction.from2DDataValue(Hash.range(hh >>> 12, 4)) : Direction.SOUTH;
                    w.set(x, y, z, lounger.setValue(HorizontalDirectionalBlock.FACING, d));
                    if (n % 2 == 1 && Hash.unit(hh >>> 20) < 0.6f) {
                        w.set(x + 2, y, z, pole.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                        w.set(x + 2, y + 1, z, umbrella.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    }
                }
            // a wall of lockers that isn't, with a life ring and the rules
            for (int u = 6; u <= 22; u++) w.fill(x0 + u, y, z0 + 28, x0 + u, y + 3, z0 + 28, wall);
            w.set(x0 + 10, y + 1, z0 + 28, ring);
            w.set(x0 + 14, y + 1, z0 + 28, signNoRun);
            w.set(x0 + 18, y + 1, z0 + 28, signClosed);
            for (int[] p : new int[][]{{2, 2}, {27, 2}, {2, 27}}) {
                w.set(x0 + p[0], y, z0 + p[1], wall);
                w.set(x0 + p[0], y + 1, z0 + p[1], palm.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
        }
    }

    // ---------------------------------------------------------------- the lifeguard and the waves

    static final class Wave {
        final int i, k;
        int level;          // current water top y, or bottom-1 when empty
        int phase;          // 0 filling, 1 holding, 2 draining
        long until;

        Wave(int i, int k) {
            this.i = i;
            this.k = k;
            this.level = FLOOR - 6;
        }
    }

    static final class Lifeguard implements LevelBehavior {
        private final Map<UUID, Integer> sprint = new ConcurrentHashMap<>();
        private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
        private final List<Wave> waves = new ArrayList<>();
        private long nextWave = -1;

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isCreative() || p.isSpectator()) return;
            // no running on the deck
            if (p.tickCount % 5 == 0) {
                int n = p.isSprinting() && p.onGround() ? sprint.getOrDefault(p.getUUID(), 0) + 1 : 0;
                long now = level.getGameTime();
                if (n >= 4 && now > cooldown.getOrDefault(p.getUUID(), 0L)) {
                    n = 0;
                    cooldown.put(p.getUUID(), now + 120);
                    BlockPos from = p.blockPosition().offset(p.getRandom().nextInt(17) - 8, 3, p.getRandom().nextInt(17) - 8);
                    level.playSound(null, from, ModSounds.get("event.water_park_whistle"), SoundSource.HOSTILE, 2f, 1f);
                    p.displayClientMessage(Component.literal("§c*TWEET* §fNo running on the deck."), true);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    Sanity.add(p, -2f);
                    if (p.getRandom().nextFloat() < 0.35f) {
                        // the deck is wet
                        p.hurt(level.damageSources().generic(), 2f);
                        p.setDeltaMovement(p.getDeltaMovement().add(p.getLookAngle().multiply(0.6, 0, 0.6)).add(0, 0.2, 0));
                        p.hurtMarked = true;
                    }
                }
                sprint.put(p.getUUID(), n);
            }
            // the undertow, while a wave pool is full
            if (p.isInWater()) {
                for (Wave wv : waves) {
                    if (wv.phase != 1) continue;
                    int u = p.getBlockX() - wv.i * CELL, v = p.getBlockZ() - wv.k * CELL;
                    if (waveBottom(u, v) == Integer.MIN_VALUE) continue;
                    Vec3 drain = new Vec3(wv.i * CELL + 14.5, waveBottom(14, 25), wv.k * CELL + 25.5);
                    Vec3 pull = drain.subtract(p.position()).normalize().scale(0.035);
                    p.setDeltaMovement(p.getDeltaMovement().add(pull).add(0, -0.02, 0));
                    p.hurtMarked = true;
                }
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (nextWave < 0) nextWave = now + 20 * (90 + level.random.nextInt(120));
            if (now >= nextWave) {
                nextWave = now + 20 * (200 + level.random.nextInt(200));
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition().above(5), ModSounds.get("event.water_park_waves"), SoundSource.AMBIENT, 2f, 1f);
                    p.displayClientMessage(Component.literal("§b§o\"The wave pool is now open.\""), true);
                    int pi = Math.floorDiv(p.getBlockX(), CELL), pk = Math.floorDiv(p.getBlockZ(), CELL);
                    for (int i = pi - 2; i <= pi + 2; i++)
                        for (int k = pk - 2; k <= pk + 2; k++) {
                            if (kind(levelSeed(level), i, k) != Kind.WAVE) continue;
                            final int fi = i, fk = k;
                            if (waves.stream().anyMatch(wv -> wv.i == fi && wv.k == fk)) continue;
                            Wave wv = new Wave(i, k);
                            wv.until = now;
                            waves.add(wv);
                            level.playSound(null, new BlockPos(i * CELL + 14, FLOOR, k * CELL + 25), ModSounds.get("event.water_park_surge"), SoundSource.AMBIENT, 2.5f, 1f);
                        }
                }
            }
            for (Iterator<Wave> it = waves.iterator(); it.hasNext(); ) {
                Wave wv = it.next();
                if (now < wv.until) continue;
                switch (wv.phase) {
                    case 0 -> {
                        wv.level++;
                        layer(level, wv, wv.level, true);
                        wv.until = now + 12;
                        if (wv.level >= FLOOR) {
                            wv.phase = 1;
                            wv.until = now + 20 * 30;
                        }
                    }
                    case 1 -> {
                        wv.phase = 2;
                        wv.until = now;
                    }
                    default -> {
                        layer(level, wv, wv.level, false);
                        wv.level--;
                        wv.until = now + 10;
                        if (wv.level < FLOOR - 6) it.remove();
                    }
                }
            }
        }

        /** The same seed the chunk generator hands to this level's generator. */
        static long levelSeed(ServerLevel level) {
            return Hash.hash(level.getSeed(), Hash.salt("water_park"));
        }

        void layer(ServerLevel level, Wave wv, int y, boolean fill) {
            BlockState water = Blocks.WATER.defaultBlockState();
            for (int u = 3; u <= 26; u++)
                for (int v = 3; v <= 26; v++) {
                    int b = waveBottom(u, v);
                    if (b == Integer.MIN_VALUE || y <= b) continue;
                    BlockPos pos = new BlockPos(wv.i * CELL + u, y, wv.k * CELL + v);
                    if (!level.isLoaded(pos)) continue;
                    BlockState st = level.getBlockState(pos);
                    if (fill && st.isAir()) level.setBlock(pos, water, Block.UPDATE_CLIENTS);
                    else if (!fill && st.is(Blocks.WATER)) level.setBlock(pos, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                }
        }
    }
}
