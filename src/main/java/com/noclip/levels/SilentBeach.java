package com.noclip.levels;

import com.noclip.entity.Hound;
import com.noclip.entity.TideWalker;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silent Beach. A grey shore that runs forever north and south, dunes behind it, a grey
 * sea in front. The surf comes in and makes no sound at all. Things wash up along the
 * tideline. An old pier walks out into the water every so often. Because nothing here
 * makes a sound, everything you do carries: run or jump near the water and something
 * stands up in the shallows to come and find you.
 */
public final class SilentBeach {
    static final String ID = "silent_beach";
    static final int G = 40;
    static final int PIER = 320;

    private SilentBeach() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Silent Beach").order(3060)
                .tagline("The waves make no sound. So the sea can hear you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Hush())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.04f).saturation(0.38f).contrast(0.98f).brightness(1.06f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.42f).warp(0.012f)
                        .fog(6f, 72f).fogColor(0xA4A8AA).bloom(0.3f))
                .sanityDrain(0.03f)
                .spawnY(G - 2, G + 8)
                .waterSpawn()
                .exit(ExitType.WATER, "level_7", 3).exit(ExitType.WATER, "the_dock", 2)
                .exit(ExitType.DOOR, "level_37", 2).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.NOCLIP, "the_dock", 2).exit(ExitType.NOCLIP, "level_7", 1)
                .exit(ExitType.NOCLIP, "salt_flats", 1).exit(ExitType.NOCLIP, "foggy_playground", 1)
                .build();
    }

    static int shore(long s, int z) {
        return (int) ((Hash.fbm(s ^ 0x5B0L, 0.37, z / 90.0, 3) - 0.5f) * 44f);
    }

    /** Ground height; under water this is the sea floor. */
    static int height(long s, int x, int z) {
        int d = x - shore(s, z);
        if (d >= 4) return G - 1 - Math.min(14, (d - 4) / 3);
        if (d >= -3) return G - 1;
        if (d >= -14) return G - 1 + (-d - 3) / 5;
        float dune = Hash.fbm(s ^ 0xD00EL, x / 22.0, z / 22.0, 3);
        float k = Math.min(1f, (-d - 14) / 18f);
        return G + 1 + (int) (dune * 7 * k);
    }

    static final class Gen implements LevelGenerator {
        final BlockState sand = ModBlocks.state("sb_sand");
        final BlockState dune = ModBlocks.state("sb_sand_dune");
        final BlockState wet = ModBlocks.state("sb_sand_wet");
        final BlockState grass = ModBlocks.state("sb_beach_grass");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState planks = ModBlocks.state("sb_planks");
        final BlockState pile = ModBlocks.state("sb_pile");
        final BlockState rail = ModBlocks.state("sb_pier_rail");
        final BlockState deep = ModBlocks.state("sb_deep");
        final BlockState tower = ModBlocks.state("sb_tower_wall");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int d = x - shore(s, z);
                    int h = height(s, x, z);
                    w.fill(x, 0, z, x, h - 4, z, sub);
                    w.fill(x, h - 3, z, x, h - 1, z, d >= -3 ? wet : sand);
                    w.set(x, h, z, d >= -3 ? wet : d >= -14 ? sand : dune);
                    if (h < G - 1) w.fill(x, h + 1, z, x, G - 1, z, water);
                    if (d >= 0 && d < 4) w.set(x, G, z, ModBlocks.state("sb_foam_" + d));
                    if (d < -16 && Hash.unit(s, x, z, 3) < 0.22f) w.set(x, h + 1, z, grass);
                    // a sand fence along the foot of the dunes, with gaps
                    if (d == -20 && Math.floorMod(z, 11) < 8) {
                        w.set(x, h + 1, z, rail.setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true));
                    }
                    if (d >= -7 && d <= -1) washedUp(w, s, x, z, h);
                }
            pier(w, s);
            tower(w, s);
        }

        void washedUp(ChunkWriter w, long s, int x, int z, int h) {
            if (!w.isAir(x, h + 1, z)) return;
            float r = Hash.unit(s, x, z, 77);
            Direction d = Direction.from2DDataValue(Hash.range(s, x, z, 78, 4));
            if (r < 0.012f) w.set(x, h + 1, z, OutsideKit.facing("sb_driftwood", d));
            else if (r < 0.035f) w.set(x, h + 1, z, OutsideKit.facing("sb_kelp", d));
            else if (r < 0.0375f) w.set(x, h + 1, z, OutsideKit.facing("sb_suitcase", d));
            else if (r < 0.0405f) w.set(x, h + 1, z, OutsideKit.facing("sb_bottle", d));
            else if (r < 0.0415f) w.set(x, h + 1, z, OutsideKit.facing("sb_fridge", d));
            else if (r < 0.0422f) w.set(x, h + 1, z, OutsideKit.facing("sf_tv_fallen", d));
            else if (r < 0.0428f) w.set(x, h + 1, z, OutsideKit.facing("gs_cart", d));
            else if (r < 0.0436f && w.contains(x, z + 1) && w.isAir(x, h + 1, z + 1)) {
                w.set(x, h + 1, z, OutsideKit.facing("sb_boat_end", Direction.NORTH));
                w.set(x, h + 1, z + 1, OutsideKit.facing("sb_boat_mid", Direction.NORTH));
            }
        }

        void pier(ChunkWriter w, long s) {
            int k0 = Math.floorDiv(w.minZ, PIER), k1 = Math.floorDiv(w.maxZ, PIER);
            for (int k = k0; k <= k1; k++) {
                int z0 = k * PIER;
                if (z0 + 4 < w.minZ || z0 > w.maxZ) continue;
                int sx = shore(s, z0);
                for (int x = sx - 10; x <= sx + 64; x++) {
                    for (int dz = 0; dz <= 4; dz++) {
                        int z = z0 + dz;
                        if (!w.contains(x, z)) continue;
                        boolean edge = dz == 0 || dz == 4;
                        boolean hole = x >= sx + 60 && x <= sx + 62 && dz >= 1 && dz <= 3;
                        int h = height(s, x, z);
                        if (!hole) w.set(x, G + 1, z, planks);
                        w.fill(x, Math.max(h + 1, G + 2), z, x, G + 5, z, ModBlocks.AIR);
                        if (edge && Math.floorMod(x - sx, 4) == 0) w.fill(x, h + 1, z, x, G, z, pile);
                        if (edge && x > sx - 8) {
                            w.set(x, G + 2, z, rail.setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true));
                        }
                        if (dz == 0 && Math.floorMod(x - sx, 12) == 6 && x > sx) {
                            w.set(x, G + 2, z, ModBlocks.state("sb_lamp_post"));
                            w.set(x, G + 3, z, ModBlocks.state("sb_lamp_post"));
                            boolean last = x - sx >= 54;
                            w.set(x, G + 4, z, OutsideKit.facing(last ? "sb_lantern" : "sb_lantern_dead", Direction.NORTH));
                        }
                        if (hole) {
                            // straight down from the end of the pier: far deeper than the sea around it
                            for (int y = h - 10; y <= h; y++) w.set(x, y, z, water);
                            w.set(x, h - 11, z, deep);
                        }
                    }
                }
                // the end of the pier is closed off by railing, except the gap where the boards are missing
                for (int dz = 0; dz <= 4; dz++) {
                    int x = sx + 64, z = z0 + dz;
                    if (w.contains(x, z)) w.set(x, G + 2, z, rail.setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true));
                }
            }
        }

        void tower(ChunkWriter w, long s) {
            int k0 = Math.floorDiv(w.minZ - 160, PIER) - 1, k1 = Math.floorDiv(w.maxZ - 160, PIER) + 1;
            for (int k = k0; k <= k1; k++) {
                int tz = k * PIER + 160;
                int tx = shore(s, tz) - 10;
                if (tz + 3 < w.minZ || tz - 3 > w.maxZ) continue;
                int base = height(s, tx, tz);
                int floor = base + 4;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = tx + dx, z = tz + dz;
                        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) w.fill(x, height(s, x, z) + 1, z, x, floor - 1, z, pile);
                        w.set(x, floor, z, planks);
                        boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                        for (int y = 1; y <= 3; y++) w.set(x, floor + y, z, edge ? tower : ModBlocks.AIR);
                        w.set(x, floor + 4, z, tower);
                    }
                w.set(tx + 1, floor + 1, tz, ModBlocks.state("sb_tower_door"));
                w.set(tx + 1, floor + 2, tz, ModBlocks.state("sb_tower_door"));
                for (int y = base + 1; y <= floor; y++) {
                    w.set(tx + 2, y, tz + 1, ModBlocks.state("sb_ladder").setValue(LadderBlock.FACING, Direction.EAST));
                }
                w.set(tx + 2, floor, tz, planks);
                w.set(tx + 2, floor, tz + 1, ModBlocks.AIR);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Hush implements LevelBehavior {
        static final class St {
            float heard;
            UUID walker;
            int cooldown;
        }

        private final Map<UUID, St> states = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            states.put(p.getUUID(), new St());
            long s = OutsideKit.seed(level, ID);
            int z = p.getBlockZ();
            if (OutsideKit.autotest()) z = Math.round(z / (float) PIER) * PIER + 9;
            int x = shore(s, z) - 6;
            level.getChunk(new BlockPos(x, G, z));
            p.teleportTo(level, x + 0.5, height(s, x, z) + 1, z + 0.5, OutsideKit.autotest() ? 235f : 270f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0) return;
            St st = states.computeIfAbsent(p.getUUID(), u -> new St());
            long s = OutsideKit.seed(level, ID);
            int sx = shore(s, p.getBlockZ());
            int d = p.getBlockX() - sx;
            float n = Hound.noise(p);
            boolean nearWater = d > -40 && d < 30;
            st.heard = Math.max(0, st.heard * 0.97f + (nearWater && n > 0.3f ? n : 0));
            if (st.cooldown > 0) st.cooldown--;
            boolean alive = st.walker != null && level.getEntity(st.walker) instanceof TideWalker tw && !tw.isRemoved();
            if (!alive && st.heard > 12 && st.cooldown == 0 && !p.isCreative() && !p.isSpectator()) {
                spawn(p, level, s, st, sx);
            }
        }

        void spawn(ServerPlayer p, ServerLevel level, long s, St st, int sx) {
            for (int attempt = 0; attempt < 8; attempt++) {
                int z = p.getBlockZ() + p.getRandom().nextInt(21) - 10;
                int x = shore(s, z) + 7 + p.getRandom().nextInt(6);
                BlockPos at = new BlockPos(x, height(s, x, z) + 1, z);
                if (!level.isLoaded(at) || OutsideKit.seen(p, at, 0.6)) continue;
                TideWalker tw = TideWalker.TYPE.create(level);
                if (tw == null) return;
                tw.moveTo(x + 0.5, at.getY(), z + 0.5, 90f, 0f);
                tw.hunt(p, shore(s, z));
                level.addFreshEntity(tw);
                st.walker = tw.getUUID();
                st.heard = 0;
                st.cooldown = 60;
                OutsideKit.sound(level, at, "event.silent_beach_rise", 2.2f, 1f);
                return;
            }
        }
    }
}
