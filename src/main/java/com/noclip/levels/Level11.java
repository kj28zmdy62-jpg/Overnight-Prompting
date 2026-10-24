package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 11, "The Endless City". Towers and empty avenues in a dusk that never
 * ends. Windows lit, shops open, nobody behind any counter. Lobbies with vending
 * machines that still give. It is safe: your nerves settle here, and nothing
 * hostile that follows you in survives the first street.
 */
public final class Level11 {
    static final int G = 10, BLOCK = 48;

    private Level11() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_11")
                .number("11").title("The Endless City").order(110)
                .tagline("A city at dusk, forever. Rest here. Nothing follows you in.")
                .danger(BackroomsLevel.Danger.SAFE)
                .generator(Gen::new)
                .behavior(new Sanctuary())
                .visuals(new Visuals().tint(1.08f, 0.98f, 0.9f).saturation(0.95f).contrast(1.04f).brightness(1.08f)
                        .grain(0.04f).aberration(0.0008f).vignette(0.35f).fog(20f, 140f).fogColor(0xC8906A).bloom(0.55f)
                        .sky().clouds().noRec())
                .sanityDrain(-0.12f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.15f).dist(24, 44))
                .exit(ExitType.NOCLIP, "level_13", 2).exit(ExitType.NOCLIP, "level_10", 1).exit(ExitType.NOCLIP, "airport", 1)
                .exit(ExitType.DESCEND, "subway", 3).exit(ExitType.DESCEND, "level_13", 1).exit(ExitType.DESCEND, "level_9", 1)
                .exit(ExitType.DOOR, "level_13", 2).exit(ExitType.DOOR, "airport", 2).exit(ExitType.DOOR, "manila_room", 1)
                .exit(ExitType.DOOR, "level_4", 1).exit(ExitType.DOOR, "level_0", 1)
                .build();
    }

    static final String[] KINDS = {"concrete", "brick", "glass", "stone"};

    static final class Gen implements LevelGenerator {
        final BlockState road = ModBlocks.state("city_road");
        final BlockState lineX = ModBlocks.state("city_road_line_x");
        final BlockState lineZ = ModBlocks.state("city_road_line_z");
        final BlockState cross = ModBlocks.state("city_crosswalk");
        final BlockState pave = ModBlocks.state("city_pavement");
        final BlockState shop = ModBlocks.state("city_shopfront");
        final BlockState awning = ModBlocks.state("city_awning");
        final BlockState roof = ModBlocks.state("city_roof");
        final BlockState ac = ModBlocks.state("city_ac_unit");
        final BlockState lobbyFloor = ModBlocks.state("city_lobby_floor");
        final BlockState lobbyWall = ModBlocks.state("city_lobby_wall");
        final BlockState lobbyLight = ModBlocks.state("city_lobby_light");
        final BlockState lamp = ModBlocks.state("city_streetlamp");
        final BlockState pole = ModBlocks.state("city_lamp_pole");
        final BlockState signal = ModBlocks.state("city_signal");
        final BlockState bench = ModBlocks.state("city_bench");
        final BlockState[] signs = {ModBlocks.state("city_sign_diner"), ModBlocks.state("city_sign_open"),
                ModBlocks.state("city_sign_hotel"), ModBlocks.state("city_sign_safe")};
        final BlockState vending = ModBlocks.state("city_vending");
        final BlockState subwaySign = ModBlocks.state("city_subway_sign");
        final BlockState subway = ModBlocks.state("city_subway_stairs");
        final BlockState transit = ModBlocks.state("city_transit_door");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState plant = ModBlocks.state("office_plant");
        final BlockState stairs = ModBlocks.state("concrete_stairs");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int dx = x - Math.round(x / (float) BLOCK) * BLOCK, dz = z - Math.round(z / (float) BLOCK) * BLOCK;
                    boolean rx = Math.abs(dz) <= 4, rz = Math.abs(dx) <= 4;
                    BlockState b;
                    if (rx && rz) b = road;
                    else if (rx) b = Math.abs(dx) <= 7 ? cross : dz == 0 ? lineX : road;
                    else if (rz) b = Math.abs(dz) <= 7 ? cross : dx == 0 ? lineZ : road;
                    else b = pave;
                    w.set(x, G, z, b);
                    // streetlamps along the kerbs, signals at the corners
                    boolean kerbX = Math.abs(dz) == 5 && Math.abs(dx) > 7, kerbZ = Math.abs(dx) == 5 && Math.abs(dz) > 7;
                    if ((kerbX && Math.floorMod(x, 12) == 6) || (kerbZ && Math.floorMod(z, 12) == 6)) {
                        for (int y = G + 1; y <= G + 5; y++) w.set(x, y, z, pole);
                        w.set(x, G + 6, z, lamp);
                    } else if (Math.abs(dx) == 6 && Math.abs(dz) == 6) {
                        for (int y = G + 1; y <= G + 3; y++) w.set(x, y, z, pole);
                        w.set(x, G + 4, z, signal);
                    } else if ((kerbX && Math.floorMod(x, 12) == 0) && Hash.chance(s, x, z, 1, 0.4f)) {
                        w.set(x, G + 1, z, ClassicsKit.facing(bench, dz > 0 ? Direction.SOUTH : Direction.NORTH));
                    }
                }
            for (int i = Math.floorDiv(w.minX, BLOCK) - 1; i <= Math.floorDiv(w.maxX, BLOCK) + 1; i++)
                for (int k = Math.floorDiv(w.minZ, BLOCK) - 1; k <= Math.floorDiv(w.maxZ, BLOCK) + 1; k++)
                    cityBlock(w, s, i, k);
        }

        void cityBlock(ChunkWriter w, long s, int i, int k) {
            int x0 = i * BLOCK + 8, z0 = k * BLOCK + 8, size = BLOCK - 16;   // 32 x 32 of buildable land
            if (x0 > w.maxX + 1 || x0 + size < w.minX - 1 || z0 > w.maxZ + 1 || z0 + size < w.minZ - 1) return;
            long h = Hash.hash(s, i, k, 1101);
            int split = Hash.range(h, 3);   // 0: one tower, 1: two, 2: four
            int nx = split == 2 ? 2 : (split == 1 && (h & 8) == 0 ? 2 : 1);
            int nz = split == 2 ? 2 : (split == 1 && (h & 8) != 0 ? 2 : 1);
            int lw = size / nx, ld = size / nz;
            for (int a = 0; a < nx; a++)
                for (int b = 0; b < nz; b++) {
                    long hb = Hash.hash(h, a, b);
                    building(w, s, x0 + a * lw, z0 + b * ld, x0 + (a + 1) * lw - 2, z0 + (b + 1) * ld - 2, hb,
                            a == 0 ? Direction.WEST : Direction.EAST, b == 0 ? Direction.NORTH : Direction.SOUTH);
                }
            // a subway entrance on the corner pavement
            if (Hash.unit(h >>> 30) < 0.08f) {
                int sx = i * BLOCK + 6, sz = k * BLOCK + 9;
                for (int dz = 0; dz < 3; dz++) {
                    w.set(sx, G, sz + dz, stairs.setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH));
                    w.set(sx, G - 1, sz + dz + 1, subway);
                }
                w.set(sx, G - 1, sz + 3, subway);
                w.set(sx, G - 2, sz + 3, subway);
                w.set(sx + 1, G + 1, sz, subwaySign);
            }
        }

        void building(ChunkWriter w, long s, int ax, int az, int bx, int bz, long h, Direction sideX, Direction sideZ) {
            String kind = KINDS[Hash.range(h >>> 4, KINDS.length)];
            BlockState dark = ModBlocks.state("city_" + kind), lit = ModBlocks.state("city_" + kind + "_lit"),
                    tv = ModBlocks.state("city_" + kind + "_tv");
            int height = 14 + Hash.range(h >>> 8, 60);
            if (Hash.unit(h >>> 14) < 0.25f) height = 8 + Hash.range(h >>> 18, 8);
            int top = G + height;
            boolean shops = Hash.unit(h >>> 22) < 0.45f;
            boolean lobby = Hash.unit(h >>> 26) < 0.4f;
            float litChance = 0.08f + Hash.unit(h >>> 30) * 0.25f;
            for (int x = ax; x <= bx; x++)
                for (int z = az; z <= bz; z++) {
                    if (!w.contains(x, z)) continue;
                    boolean wall = x == ax || x == bx || z == az || z == bz;
                    w.set(x, G, z, wall ? pave : lobbyFloor);
                    for (int y = G + 1; y <= top; y++) {
                        BlockState b;
                        if (!wall) b = dark;
                        else if (y <= G + 3 && shops) b = shop;
                        else {
                            float r = Hash.unit(s, x, y, z);
                            b = r < litChance ? lit : r < litChance + 0.015f ? tv : dark;
                        }
                        w.set(x, y, z, b);
                    }
                    w.set(x, top + 1, z, roof);
                    if (!wall && Hash.chance(s, x, z, 1102, 0.03f)) w.set(x, top + 2, z, ClassicsKit.facing(ac, Direction.NORTH));
                }
            int mx = (ax + bx) / 2, mz = (az + bz) / 2;
            int fz = sideZ == Direction.NORTH ? az : bz;
            Direction out = sideZ;
            if (shops) {
                for (int x = ax; x <= bx; x++) w.set(x, G + 4, fz + out.getStepZ(), awning);
                w.set(mx + 3, G + 5, fz + out.getStepZ(), ClassicsKit.facing(signs[Hash.range(h >>> 34, signs.length)], out.getOpposite()));
            }
            if (!lobby) return;
            // a lobby: carved out of the ground floor, door on the street side
            for (int x = ax + 1; x < bx; x++)
                for (int z = az + 1; z < bz; z++) {
                    for (int y = G + 1; y <= G + 4; y++) w.set(x, y, z, ModBlocks.AIR);
                    w.set(x, G + 5, z, Math.floorMod(x, 4) == 1 && Math.floorMod(z, 4) == 1 ? lobbyLight : lobbyWall);
                }
            for (int d = 0; d <= 1; d++)
                for (int y = G + 1; y <= G + 3; y++) w.set(mx + d, y, fz, ModBlocks.AIR);
            int backZ = fz == az ? bz - 1 : az + 1;
            w.set(ax + 1, G + 1, backZ, ClassicsKit.facing(vending, out));
            w.set(ax + 2, G + 1, backZ, ClassicsKit.facing(vending, out));
            w.set(bx - 1, G + 1, backZ, plant);
            w.set(mx, G + 1, mz, ClassicsKit.facing(bench, Direction.EAST));
            if (Hash.unit(h >>> 38) < 0.35f) w.set(bx - 1, G + 1, mz, supply);
            if (Hash.unit(h >>> 40) < 0.5f) w.set(ax + 1, G + 1, mz, bottle);
            if (Hash.unit(h >>> 42) < 0.2f) {
                int wz = fz == az ? bz : az;
                w.set(mx, G + 1, wz, transit);
                w.set(mx, G + 2, wz, transit);
            }
        }
    }

    /** Nothing hostile survives long here, and your mind recovers. */
    static final class Sanctuary implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 40 != 0) return;
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64), e -> e instanceof Enemy)) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
                m.discard();
            }
        }
    }
}
