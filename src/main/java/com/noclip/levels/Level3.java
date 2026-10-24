package com.noclip.levels;

import com.noclip.entity.Hound;
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
import com.noclip.world.Blackouts;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level 3, "Electrical Station". Brick halls stuffed with switchgear, transformers
 * and turbines, all of it running, all of it loud. The power surges every minute
 * or two: stay off the steel plates and away from bare wire when the whine rises.
 * Linger by the loud machines and their noise brings something looking for you.
 * The main breaker stops it all, and turns out every light.
 */
public final class Level3 {
    static final int FLOOR = 6, CEIL = 12;

    private Level3() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("level_3")
                .number("3").title("Electrical Station").order(30)
                .tagline("Everything here is running. Everything here is loud. Mind the current.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Grid())
                .visuals(new Visuals().tint(0.92f, 1.04f, 0.96f).saturation(0.78f).contrast(1.16f).brightness(1.12f)
                        .grain(0.1f).aberration(0.0026f).vignette(0.55f).scanlines(0.09f).fog(3f, 36f).fogColor(0x1C221E)
                        .bloom(0.5f).flicker(0.05f))
                .sanityDrain(0.05f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 2, 0.25f).dist(22, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 3, 0.6f).dark().dist(10, 24).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("skin_stealer"), 1, 0.05f).dist(26, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_4", 4).exit(ExitType.NOCLIP, "level_2", 1).exit(ExitType.NOCLIP, "level_6", 1)
                .exit(ExitType.ASCEND, "level_4", 5).exit(ExitType.ASCEND, "level_11", 1)
                .exit(ExitType.DESCEND, "level_5", 2).exit(ExitType.DESCEND, "level_2", 1)
                .exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "power_plant", 2).exit(ExitType.DOOR, "server_farm", 1)
                .build();
    }

    // ---------------------------------------------------------------- generator

    enum Edge { OPEN, WALL, DOOR }

    static final class Gen implements LevelGenerator {
        final BlockState brick = ModBlocks.state("station_brick");
        final BlockState painted = ModBlocks.state("station_brick_painted");
        final BlockState paintedLow = ModBlocks.state("station_brick_painted_low");
        final BlockState conduit = ModBlocks.state("station_conduit_wall");
        final BlockState danger = ModBlocks.state("station_danger_sign");
        final BlockState ceiling = ModBlocks.state("station_ceiling");
        final BlockState floor = ModBlocks.state("station_floor");
        final BlockState plate = ModBlocks.state("station_floor_plate");
        final BlockState stripe = ModBlocks.state("station_floor_stripe");
        final BlockState cabinet = ModBlocks.state("switchgear_cabinet");
        final BlockState cabinetTop = ModBlocks.state("switchgear_cabinet_top");
        final BlockState turbine = ModBlocks.state("turbine_casing");
        final BlockState transformer = ModBlocks.state("station_transformer");
        final BlockState transformerTop = ModBlocks.state("station_transformer_top");
        final BlockState tube = ModBlocks.state("station_tube_light");
        final BlockState tubeBad = ModBlocks.state("station_tube_light_bad");
        final BlockState tubeDead = ModBlocks.state("station_tube_light_dead");
        final BlockState tray = ModBlocks.state("cable_tray");
        final BlockState spool = ModBlocks.state("cable_spool");
        final BlockState bulkhead = ModBlocks.state("bulkhead_lamp");
        final BlockState console = ModBlocks.state("control_console");
        final BlockState crt = ModBlocks.state("control_crt");
        final BlockState wire = ModBlocks.state("live_wire");
        final BlockState wireFloor = ModBlocks.state("live_wire_floor");
        final BlockState wireEnd = ModBlocks.state("live_wire_end");
        final BlockState breaker = ModBlocks.state("main_breaker");
        final BlockState elevLow = ModBlocks.state("elevator_door_low");
        final BlockState elevUp = ModBlocks.state("elevator_door_up");
        final BlockState callPanel = ModBlocks.state("call_panel_wall");
        final BlockState stairDark = ModBlocks.state("station_stair_dark");
        final BlockState serviceDoor = ModBlocks.state("station_service_door");
        final BlockState stairs = ModBlocks.state("concrete_stairs");
        final BlockState fence = ModBlocks.state("chain_link_fence");
        final BlockState crate = ModBlocks.state("wooden_crate");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 11, 4);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean wall = isWall(g, s, x, z);
                    w.set(x, FLOOR, z, wall ? brick : (doorwayFloor(g, s, x, z) ? stripe : floor));
                    w.set(x, CEIL, z, ceiling);
                    if (wall) {
                        for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, wallBlock(s, x, y, z));
                    } else if (Math.floorMod(z, 4) == 0 && Hash.fbm(s ^ 0x7A7L, x / 20.0, z / 20.0, 2) > 0.42f) {
                        w.set(x, CEIL - 1, z, ClassicsKit.facing(tray, Direction.NORTH));
                    } else if (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) {
                        float dim = Hash.fbm(s ^ 0xD1L, x / 36.0, z / 36.0, 2);
                        float r = Hash.unit(s, x, z, 7);
                        BlockState l = dim > 0.66f ? (r < 0.5f ? tubeDead : tubeBad) : (r < 0.08f ? tubeDead : r < 0.22f ? tubeBad : tube);
                        w.set(x, CEIL, z, l);
                    }
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> room(w, g, s, i, k, x0, z0, wd, dp));
        }

        BlockState wallBlock(long s, int x, int y, int z) {
            if (y == FLOOR + 1) return paintedLow;
            if (y == FLOOR + 2) {
                float r = Hash.unit(s, x, z, 11);
                return r < 0.02f ? danger : painted;
            }
            return Hash.chance(s, x, z, y, 0.07f) ? conduit : brick;
        }

        static Edge edge(CellGrid g, int dir, int i, int k) {
            float r = g.unit(i, k, 100 + dir);
            if (r < 0.1f) return Edge.OPEN;
            if (r < 0.32f) return Edge.WALL;
            return Edge.DOOR;
        }

        static int gap(CellGrid g, int dir, int i, int k, int len) {
            return 2 + Hash.range(g.hash(i, k, 200 + dir), Math.max(1, len - 5));
        }

        static boolean edgeSolid(CellGrid g, int dir, int i, int k, int u, int len) {
            switch (edge(g, dir, i, k)) {
                case OPEN: return u == len / 2 && len > 8;   // a lone pillar
                case WALL: return true;
                default:
                    int gp = gap(g, dir, i, k, len);
                    return u < gp || u > gp + 1;
            }
        }

        static boolean isWall(CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int u = x - g.x0(i), v = z - g.z0(k);
            if (u == 0 && v == 0) return true;
            if (u == 0) return edgeSolid(g, 0, i, k, v, g.depth(k));
            if (v == 0) return edgeSolid(g, 1, i, k, u, g.width(i));
            return false;
        }

        static boolean doorwayFloor(CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int u = x - g.x0(i), v = z - g.z0(k);
            if (u == 0 && v > 0 && edge(g, 0, i, k) == Edge.DOOR) return true;
            return v == 0 && u > 0 && edge(g, 1, i, k) == Edge.DOOR;
        }

        void room(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            long h = g.hash(i, k, 77);
            float r = Hash.unit(h);
            int ax = x0 + 2, az = z0 + 2, bx = x0 + wd - 2, bz = z0 + dp - 2;   // interior, leaving a ring corridor
            int cx = x0 + wd / 2, cz = z0 + dp / 2;
            boolean alongX = wd >= dp;
            if (r < 0.26f) {
                // switchgear: rows of tall cabinets with aisles between, cable trays overhead
                for (int row = 0; ; row++) {
                    int c = (alongX ? az : ax) + row * 3;
                    if (c > (alongX ? bz : bx)) break;
                    int lo = alongX ? ax : az, hi = alongX ? bx : bz;
                    int passage = lo + 1 + Hash.range(Hash.hash(h, row), Math.max(1, hi - lo - 1));
                    for (int a = lo; a <= hi; a++) {
                        if (a == passage) continue;
                        int x = alongX ? a : c, z = alongX ? c : a;
                        w.set(x, FLOOR + 1, z, cabinet);
                        w.set(x, FLOOR + 2, z, cabinetTop);
                    }
                    int aisle = c + 1;
                    for (int a = lo; a <= hi; a++) {
                        int x = alongX ? a : aisle, z = alongX ? aisle : a;
                        w.set(x, CEIL - 1, z, ClassicsKit.facing(tray, alongX ? Direction.NORTH : Direction.EAST));
                    }
                }
            } else if (r < 0.44f) {
                // transformer yard: four humming transformers, warning signs, a plate floor
                for (int x = ax; x <= bx; x++) for (int z = az; z <= bz; z++) w.set(x, FLOOR, z, plate);
                // big 2x2 units, two blocks tall, bushings on top, in a grid with aisles
                for (int x = ax; x + 1 <= bx; x += 4)
                    for (int z = az; z + 1 <= bz; z += 4) {
                        if (Hash.unit(Hash.hash(h, x, z)) < 0.2f) continue;
                        for (int dx = 0; dx < 2; dx++)
                            for (int dz = 0; dz < 2; dz++) {
                                w.set(x + dx, FLOOR + 1, z + dz, transformer);
                                w.set(x + dx, FLOOR + 2, z + dz, transformer);
                                w.set(x + dx, FLOOR + 3, z + dz, transformerTop);
                            }
                    }
            } else if (r < 0.56f) {
                // cable room: live wires hanging to head height, sparking cable in the puddles
                int strands = 5 + Hash.range(h >>> 8, 6);
                for (int q = 0; q < strands; q++) {
                    long hq = Hash.hash(h, q, 5);
                    int x = ax + Hash.range(hq, Math.max(1, bx - ax + 1)), z = az + Hash.range(hq >>> 16, Math.max(1, bz - az + 1));
                    int bottom = FLOOR + 2 + Hash.range(hq >>> 32, 2);
                    for (int y = bottom; y < CEIL; y++) w.set(x, y, z, y == bottom ? wireEnd : wire);
                }
                for (int q = 0; q < 3; q++) {
                    long hq = Hash.hash(h, q, 6);
                    int x = ax + Hash.range(hq, Math.max(1, bx - ax + 1)), z = az + Hash.range(hq >>> 16, Math.max(1, bz - az + 1));
                    w.set(x, FLOOR + 1, z, wireFloor);
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                        if ((dx != 0 || dz != 0) && Hash.unit(Hash.hash(hq, dx, dz)) < 0.6f) w.set(x + dx, FLOOR + 1, z + dz, puddle);
                }
                for (int a = ax; a <= bx; a++) w.set(a, CEIL - 1, cz, ClassicsKit.facing(tray, Direction.NORTH));
            } else if (r < 0.64f) {
                // turbine hall: one long machine on a steel plate floor
                for (int x = ax - 1; x <= bx + 1; x++) for (int z = az - 1; z <= bz + 1; z++) w.set(x, FLOOR, z, plate);
                Direction.Axis axis = alongX ? Direction.Axis.X : Direction.Axis.Z;
                BlockState t = turbine.setValue(RotatedPillarBlock.AXIS, axis);
                int lo = (alongX ? ax : az) + 1, hi = (alongX ? bx : bz) - 1;
                for (int a = lo; a <= hi; a++)
                    for (int c = -1; c <= 1; c++)
                        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                            if (y == FLOOR + 3 && c != 0 && (a == lo || a == hi)) continue;
                            int x = alongX ? a : cx + c, z = alongX ? cz + c : a;
                            w.set(x, y, z, t);
                        }
            } else if (r < 0.72f) {
                // storage: spools and crates, sometimes supplies
                for (int q = 0; q < 6; q++) {
                    long hq = Hash.hash(h, q, 9);
                    int x = ax + Hash.range(hq, Math.max(1, bx - ax + 1)), z = az + Hash.range(hq >>> 16, Math.max(1, bz - az + 1));
                    float rr = Hash.unit(hq >>> 24);
                    if (rr < 0.45f) w.set(x, FLOOR + 1, z, ClassicsKit.facing(spool, Direction.from2DDataValue(Hash.range(hq >>> 40, 4))));
                    else if (rr < 0.85f) w.set(x, FLOOR + 1, z, crate);
                    else w.set(x, FLOOR + 1, z, supply);
                }
            } else if (r < 0.765f) {
                // control room: consoles, monitors showing the load climbing, and the main breaker
                for (int a = ax; a <= Math.min(bx, ax + 4); a++) {
                    w.set(a, FLOOR + 1, az, ClassicsKit.facing(console, Direction.NORTH));
                    w.set(a, FLOOR + 2, az, ClassicsKit.facing(crt, Direction.NORTH));
                }
                w.set(ax + 2, FLOOR + 1, az + 2, ClassicsKit.facing(chair, Direction.NORTH));
                // breaker on the west wall of the room if it is solid there
                for (int z = az; z <= bz; z++) {
                    if (isWall(g, s, x0, z) && !isWall(g, s, x0 + 1, z)) {
                        w.set(x0 + 1, FLOOR + 2, z, ClassicsKit.facing(breaker, Direction.WEST));
                        w.set(x0 + 1, FLOOR + 4, z, ClassicsKit.facing(bulkhead, Direction.WEST));
                        break;
                    }
                }
                w.set(bx, FLOOR + 1, bz, bottle);
            } else if (r < 0.785f) {
                // a service elevator in the north wall: the floor indicator is stuck on 4
                for (int x = x0 + 2; x <= x0 + wd - 4; x++) {
                    if (isWall(g, s, x, z0) && isWall(g, s, x + 1, z0) && isWall(g, s, x + 2, z0)) {
                        w.set(x, FLOOR + 1, z0, elevLow);
                        w.set(x + 1, FLOOR + 1, z0, elevLow);
                        w.set(x, FLOOR + 2, z0, elevUp);
                        w.set(x + 1, FLOOR + 2, z0, elevUp);
                        w.set(x + 2, FLOOR + 2, z0, callPanel);
                        w.set(x, FLOOR + 4, z0 + 1, ClassicsKit.facing(bulkhead, Direction.NORTH));
                        break;
                    }
                }
            } else if (r < 0.795f) {
                // stairs down into the dark, fenced off
                for (int dx = -1; dx <= 1; dx++) {
                    for (int step = 0; step < 4; step++) {
                        int y = FLOOR - step;
                        w.set(cx + dx, y, cz - step, stairs.setValue(StairBlock.FACING, Direction.SOUTH));
                        for (int yy = y + 1; yy <= FLOOR; yy++) w.set(cx + dx, yy, cz - step, ModBlocks.AIR);
                    }
                    w.fill(cx + dx, FLOOR - 5, cz - 5, cx + dx, FLOOR, cz - 4, ModBlocks.AIR);
                    w.set(cx + dx, FLOOR - 5, cz - 4, stairDark);
                    w.set(cx + dx, FLOOR - 5, cz - 5, stairDark);
                    w.set(cx + dx, FLOOR - 4, cz - 5, stairDark);
                }
                for (int step = 0; step < 6; step++) {
                    w.set(cx - 2, FLOOR + 1, cz - step, fence);
                    w.set(cx + 2, FLOOR + 1, cz - step, fence);
                }
            } else if (r < 0.81f) {
                // a door that says KEEP OUT
                int z = z0 + 2 + Hash.range(h >>> 12, Math.max(1, dp - 4));
                if (isWall(g, s, x0, z) && isWall(g, s, x0, z + 1) && !isWall(g, s, x0 + 1, z)) {
                    w.set(x0, FLOOR + 1, z, serviceDoor);
                    w.set(x0, FLOOR + 2, z, serviceDoor);
                    w.set(x0 + 1, FLOOR + 4, z, ClassicsKit.facing(bulkhead, Direction.WEST));
                }
            } else {
                // odds and ends: a cabinet or two against a wall, a spool
                if (Hash.unit(h >>> 20) < 0.5f) {
                    w.set(ax, FLOOR + 1, az, cabinet);
                    w.set(ax, FLOOR + 2, az, cabinetTop);
                    w.set(ax + 1, FLOOR + 1, az, cabinet);
                }
                if (Hash.unit(h >>> 24) < 0.3f) w.set(bx, FLOOR + 1, bz, ClassicsKit.facing(spool, Direction.EAST));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * The station's rules. A surge every minute or two, announced by a rising
     * whine: anyone on a steel plate or near bare wire when it hits gets the
     * current. Standing next to the big machines too long draws a Hound; the
     * noise carries. The main breaker stops the machines and the surges for a
     * while, and plunges the level into darkness.
     */
    static final class Grid implements LevelBehavior {
        private long nextSurge = -1;
        private boolean warned;
        private final Map<UUID, Float> noise = new ConcurrentHashMap<>();

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (nextSurge < 0) nextSurge = now + 900 + level.random.nextInt(900);
            if (Blackouts.active(level)) {
                nextSurge = Math.max(nextSurge, now + 300);
                warned = false;
                return;
            }
            if (!warned && now >= nextSurge - 64) {
                warned = true;
                for (ServerPlayer p : level.players()) ClassicsKit.soundTo(p, "event.level_3_whine", 1.2f, 1f);
            }
            if (now >= nextSurge) {
                surge(level);
                warned = false;
                nextSurge = now + 1400 + level.random.nextInt(1200);
            }
        }

        private void surge(ServerLevel level) {
            Blackouts.start(level, 30);
            for (ServerPlayer p : level.players()) {
                ClassicsKit.soundTo(p, "event.level_3_surge", 1.5f, 0.9f + level.random.nextFloat() * 0.2f);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 30, 3, 1.5, 3, 0.4);
                if (p.isSpectator()) continue;
                if (exposed(level, p)) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 60, 0.4, 0.8, 0.4, 0.6);
                    ClassicsKit.event(p, "flash", 0.9f);
                    if (!p.isCreative()) {
                        p.hurt(level.damageSources().lightningBolt(), 6f);
                        Sanity.add(p, -6f);
                    }
                } else {
                    ClassicsKit.event(p, "flash", 0.25f);
                }
            }
        }

        private static boolean exposed(ServerLevel level, ServerPlayer p) {
            BlockPos feet = p.blockPosition();
            if (level.getBlockState(feet.below()).is(ModBlocks.get("station_floor_plate"))) return true;
            Block wire = ModBlocks.get("live_wire_end"), strand = ModBlocks.get("live_wire"), wireFloor = ModBlocks.get("live_wire_floor"),
                    pud = ModBlocks.get("puddle");
            for (BlockPos q : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 3, 2))) {
                BlockState st = level.getBlockState(q);
                if (st.is(wire) || st.is(strand) || st.is(wireFloor)) return true;
                if (st.is(pud) && q.distManhattan(feet) <= 1) return true;
            }
            return false;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 20 != 0 || p.isSpectator() || p.isCreative()) return;
            float n = noise.getOrDefault(p.getUUID(), 0f);
            if (!Blackouts.active(level) && nearMachines(level, p.blockPosition())) n += 1f;
            else n = Math.max(0f, n - 0.5f);
            if (n >= 9f) {
                n = 0f;
                if (summonHound(level, p)) Sanity.add(p, -3f);
            }
            noise.put(p.getUUID(), n);
        }

        private static boolean nearMachines(ServerLevel level, BlockPos at) {
            Block t = ModBlocks.get("station_transformer"), tu = ModBlocks.get("turbine_casing");
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-3, 0, -3), at.offset(3, 2, 3))) {
                BlockState st = level.getBlockState(q);
                if (st.is(t) || st.is(tu)) return true;
            }
            return false;
        }

        private static boolean summonHound(ServerLevel level, ServerPlayer p) {
            if (level.getEntities(ModEntities.HOUND, p.getBoundingBox().inflate(48), e -> true).size() >= 3) return false;
            for (int attempt = 0; attempt < 20; attempt++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2;
                double d = 16 + p.getRandom().nextDouble() * 12;
                BlockPos pos = BlockPos.containing(p.getX() + Math.cos(ang) * d, FLOOR + 1, p.getZ() + Math.sin(ang) * d);
                if (!level.hasChunkAt(pos)) continue;
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
                if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) continue;
                if (ClassicsKit.seen(p, Vec3.atCenterOf(pos), 0.3)) continue;
                Hound h = ModEntities.HOUND.create(level);
                if (h == null) return false;
                h.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getRandom().nextFloat() * 360f, 0f);
                h.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                h.setTarget(p);
                level.addFreshEntity(h);
                ClassicsKit.sound(level, pos, "entity.hound.howl", 1.6f, 0.85f);
                return true;
            }
            return false;
        }

        void pull(ServerLevel level, BlockPos pos) {
            if (Blackouts.active(level)) return;
            ClassicsKit.sound(level, pos, "event.level_3_breaker", 1.6f, 1f);
            Blackouts.start(level, 20 * 40);
            nextSurge = level.getGameTime() + 20 * 40 + 900;
        }
    }

    /** Called by the main breaker block. */
    public static void pullBreaker(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BackroomsLevel def = Levels.get(level.dimension());
        if (def != null && def.behavior instanceof Grid grid) grid.pull(level, pos);
        else ClassicsKit.sound(level, pos, "event.level_3_breaker", 1.2f, 1.2f);
    }
}
