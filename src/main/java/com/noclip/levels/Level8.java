package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 8, "Cave System". Caverns big enough to lose a cathedral in, joined by
 * crawlways. No light but glowing fungus, the odd abandoned lantern, and what you
 * carry. Crawlers come up the walls in the dark and flee your flashlight. Every
 * so often the rock shifts overhead.
 */
public final class Level8 {
    static final int TOP = 76, LAKE = 17, CELL = 48;

    private Level8() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_8")
                .number("8").title("Cave System").order(80)
                .tagline("Big dark caves. Keep your light on the walls.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Shifting())
                .visuals(new Visuals().tint(0.98f, 0.96f, 0.9f).saturation(0.7f).contrast(1.15f).brightness(1.05f)
                        .grain(0.12f).aberration(0.002f).vignette(0.7f).fog(2f, 28f).fogColor(0x000000).bloom(0.6f)
                        .darkness(0.85f))
                .sanityDrain(0.06f)
                .spawnY(13, 70)
                .spawn(SpawnRule.of(ModEntities.lazy("cave_crawler"), 3, 0.5f).dark().dist(14, 30).weight(5))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.2f).dist(16, 28).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.3f).dark().dist(16, 30).weight(1))
                .exit(ExitType.NOCLIP, "level_9", 3).exit(ExitType.NOCLIP, "level_7", 1).exit(ExitType.NOCLIP, "level_6", 1)
                .exit(ExitType.ASCEND, "level_9", 4).exit(ExitType.ASCEND, "level_10", 1)
                .exit(ExitType.FALL, "the_abyss", 2).exit(ExitType.FALL, "level_6", 1)
                .exit(ExitType.DOOR, "mine_shaft", 3).exit(ExitType.DOOR, "level_9", 1)
                .build();
    }

    // ---------------------------------------------------------------- shape

    static int floor(long s, int x, int z) {
        return 12 + (int) (Hash.fbm(s ^ 0x81L, x / 52.0, z / 52.0, 3) * 26);
    }

    static float chamber(long s, int x, int z) {
        return Math.max(0f, Math.min(1f, (0.47f - Hash.fbm(s ^ 0x82L, x / 70.0, z / 70.0, 3)) * 7f));
    }

    static boolean open(long s, int x, int z) {
        if (chamber(s, x, z) > 0f) {
            return !(chamber(s, x, z) > 0.4f && Hash.fbm(s ^ 0x86L, x / 5.0, z / 5.0, 2) > 0.76f);   // rock pillars
        }
        if (Math.abs(Hash.noise(s ^ 0x83L, x / 34.0, z / 34.0) - 0.5f) < 0.045f) return true;
        return Math.abs(Hash.noise(s ^ 0x84L, x / 23.0 + 100, z / 23.0) - 0.5f) < 0.03f;
    }

    static int ceiling(long s, int x, int z) {
        float c = chamber(s, x, z);
        return floor(s, x, z) + 4 + (int) (c * 26) + (int) (Hash.fbm(s ^ 0x85L, x / 9.0, z / 9.0, 2) * 3);
    }

    static final class Gen implements LevelGenerator {
        final BlockState rock = ModBlocks.state("cave_rock");
        final BlockState dark = ModBlocks.state("cave_rock_dark");
        final BlockState vein = ModBlocks.state("cave_rock_vein");
        final BlockState wet = ModBlocks.state("cave_rock_wet");
        final BlockState gravel = ModBlocks.state("cave_gravel");
        final BlockState mud = ModBlocks.state("cave_mud");
        final BlockState stalagmite = ModBlocks.state("cave_stalagmite");
        final BlockState stalactite = ModBlocks.state("cave_stalactite");
        final BlockState fungus = ModBlocks.state("cave_glow_fungus");
        final BlockState lantern = ModBlocks.state("cave_lantern");
        final BlockState rope = ModBlocks.state("cave_rope_coil");
        final BlockState daylight = ModBlocks.state("cave_daylight");
        final BlockState chasm = ModBlocks.state("cave_chasm_bottom");
        final BlockState mineDoor = ModBlocks.state("cave_mine_door");
        final BlockState ladder = ModBlocks.state("stilt_ladder");
        final BlockState bedroll = ModBlocks.state("wanderer_bedroll");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, TOP + 1, w.minZ, w.maxX, TOP + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int f = floor(s, x, z);
                    w.fill(x, 0, z, x, f - 4, z, sub);
                    boolean o = open(s, x, z);
                    int c = o ? ceiling(s, x, z) : TOP + 1;
                    for (int y = f - 3; y <= TOP; y++) {
                        if (y > f && y < c) continue;
                        w.set(x, y, z, rockAt(s, x, y, z));
                    }
                    if (!o) continue;
                    boolean lake = f < LAKE;
                    w.set(x, f, z, lake ? mud : (Hash.fbm(s ^ 0x87L, x / 11.0, z / 11.0, 2) > 0.55f ? gravel : rockAt(s, x, f, z)));
                    if (lake) {
                        for (int y = f + 1; y <= LAKE && y < c; y++) w.set(x, y, z, water);
                        continue;
                    }
                    float r = Hash.unit(s, x, z, 88);
                    if (r < 0.025f) w.set(x, f + 1, z, ClassicsKit.facing(stalagmite, Direction.NORTH));
                    else if (Hash.fbm(s ^ 0x89L, x / 14.0, z / 14.0, 2) > 0.66f && r < 0.3f) w.set(x, f + 1, z, ClassicsKit.facing(fungus, Direction.NORTH));
                    if (c - f > 5 && Hash.chance(s, x, z, 90, 0.04f)) w.set(x, c - 1, z, ClassicsKit.facing(stalactite, Direction.NORTH));
                }
            for (int i = Math.floorDiv(w.minX, CELL) - 1; i <= Math.floorDiv(w.maxX, CELL) + 1; i++)
                for (int k = Math.floorDiv(w.minZ, CELL) - 1; k <= Math.floorDiv(w.maxZ, CELL) + 1; k++) features(w, s, i, k);
        }

        BlockState rockAt(long s, int x, int y, int z) {
            float r = Hash.unit(s, x, y, z);
            if (r < 0.04f) return vein;
            if (r < 0.1f) return wet;
            return ((y / 3 + (int) (Hash.noise(s ^ 0x8AL, x / 20.0, z / 20.0) * 4)) % 3 == 0) ? dark : rock;
        }

        /** An open column in the cell next to a wall to its north, or null. */
        int[] besideWall(long s, long h, int i, int k) {
            for (int q = 0; q < 24; q++) {
                long hq = Hash.hash(h, q);
                int x = i * CELL + Hash.range(hq, CELL), z = k * CELL + Hash.range(hq >>> 16, CELL);
                if (open(s, x, z) && !open(s, x, z - 1) && floor(s, x, z) >= LAKE) return new int[]{x, z};
            }
            return null;
        }

        void features(ChunkWriter w, long s, int i, int k) {
            long h = Hash.hash(s, i, k, 801);
            float r = Hash.unit(h);
            // a shaft with daylight at the top and a ladder bolted to the rock
            if (r < 0.10f) {
                int[] p = besideWall(s, h, i, k);
                if (p != null) {
                    int f = floor(s, p[0], p[1]);
                    for (int y = f + 1; y < TOP; y++) {
                        w.set(p[0], y, p[1], ladder.setValue(LadderBlock.FACING, Direction.SOUTH));
                    }
                    w.set(p[0], TOP, p[1], daylight);
                    w.set(p[0] + 1, f + 1, p[1], ClassicsKit.facing(rope, Direction.NORTH));
                }
            }
            // an abandoned camp: lantern still burning
            long h2 = Hash.hash(h, 2);
            if (Hash.unit(h2) < 0.14f) {
                int x = i * CELL + 8 + Hash.range(h2 >>> 4, CELL - 16), z = k * CELL + 8 + Hash.range(h2 >>> 20, CELL - 16);
                if (open(s, x, z) && floor(s, x, z) >= LAKE && ceiling(s, x, z) - floor(s, x, z) >= 4) {
                    int f = floor(s, x, z);
                    w.set(x, f + 1, z, ClassicsKit.facing(lantern, Direction.NORTH));
                    if (open(s, x + 1, z) && floor(s, x + 1, z) == f) w.set(x + 1, f + 1, z, ClassicsKit.facing(supply, Direction.WEST));
                    if (open(s, x, z + 1) && floor(s, x, z + 1) == f) w.set(x, f + 1, z + 1, bedroll);
                    if (open(s, x - 1, z) && floor(s, x - 1, z) == f) w.set(x - 1, f + 1, z, bottle);
                }
            }
            // a chasm in a big chamber floor
            long h3 = Hash.hash(h, 3);
            if (Hash.unit(h3) < 0.07f) {
                int x = i * CELL + 10 + Hash.range(h3 >>> 4, CELL - 20), z = k * CELL + 10 + Hash.range(h3 >>> 20, CELL - 20);
                if (chamber(s, x, z) > 0.6f && floor(s, x, z) >= LAKE) {
                    for (int dx = -3; dx <= 3; dx++)
                        for (int dz = -3; dz <= 3; dz++) {
                            if (dx * dx + dz * dz > 10 || !open(s, x + dx, z + dz)) continue;
                            int f = floor(s, x + dx, z + dz);
                            for (int y = 1; y <= f + 1; y++) w.set(x + dx, y, z + dz, y <= 3 ? chasm : ModBlocks.AIR);
                        }
                }
            }
            // an old mine door set into the rock
            long h4 = Hash.hash(h, 4);
            if (Hash.unit(h4) < 0.08f) {
                int[] p = besideWall(s, h4, i, k);
                if (p != null) {
                    int f = floor(s, p[0], p[1]);
                    w.set(p[0], f + 1, p[1] - 1, mineDoor);
                    w.set(p[0], f + 2, p[1] - 1, mineDoor);
                    w.set(p[0] + 1, f + 1, p[1], ClassicsKit.facing(lantern, Direction.NORTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The rock shifts. A rumble, dust from the ceiling, and sometimes a stone finds you. */
    static final class Shifting implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 20 != 0 || p.isSpectator() || p.getRandom().nextInt(70) != 0) return;
            ClassicsKit.soundTo(p, "event.level_8_rumble", 1.5f, 0.6f);
            BlockState dust = ModBlocks.state("cave_rock");
            for (int q = 0; q < 6; q++) {
                double x = p.getX() + (p.getRandom().nextDouble() - 0.5) * 10, z = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 10;
                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, dust), x, p.getY() + 4, z, 20, 0.8, 0.5, 0.8, 0.02);
            }
            BlockPos above = p.blockPosition().above(2);
            boolean open = true;
            for (int y = 0; y < 6; y++) if (!level.getBlockState(above.above(y)).isAir()) { open = false; break; }
            if (!open && !p.isCreative() && p.getRandom().nextInt(3) == 0) {
                p.hurt(level.damageSources().fallingStalactite(null), 3f);
                ClassicsKit.event(p, "pulse", 0.6f);
            }
        }
    }
}
