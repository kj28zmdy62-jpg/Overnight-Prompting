package com.noclip.levels;

import com.noclip.block.GaugeBlock;
import com.noclip.block.ReliefValveBlock;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boiler Room. A cathedral of soot-black brick and riveted boilers, lit almost
 * only by their fireboxes. The pressure in the system always climbs: the gauges
 * creep toward the red, the pipes start to knock, and at the top the relief
 * valves blow scalding steam across the aisles. Turning a bleed valve buys time.
 */
public final class BoilerRoom {
    static final int FLOOR = 12, CEIL = 28;
    static final int GAUGE_Y = FLOOR + 5;

    private BoilerRoom() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("boiler_room")
                .number("").title("Boiler Room").order(2020)
                .tagline("Watch the gauges. When the pipes start knocking, get away from the valves.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Pressure())
                .visuals(new Visuals().darkness(0.12f).tint(1.14f, 0.93f, 0.78f).saturation(0.92f).contrast(1.14f)
                        .brightness(1.25f).grain(0.09f).aberration(0.0018f).vignette(0.6f).warp(0.035f)
                        .fog(3f, 34f).fogColor(0x2E1608).bloom(0.85f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(20, 32).weight(1))
                .exit(ExitType.NOCLIP, "level_2", 3).exit(ExitType.NOCLIP, "level_3", 1).exit(ExitType.NOCLIP, "level_5", 1)
                .exit(ExitType.DESCEND, "level_2", 3).exit(ExitType.DESCEND, "sewers", 1)
                .exit(ExitType.DOOR, "level_2", 2).exit(ExitType.DOOR, "steel_mill", 1).exit(ExitType.DOOR, "power_plant", 1)
                .exit(ExitType.DOOR, "level_3", 1)
                .exit(ExitType.ASCEND, "level_5", 3).exit(ExitType.ASCEND, "level_4", 1)
                .build();
    }

    // ---------------------------------------------------------------- pressure

    private static final Map<ResourceKey<Level>, double[]> STATE = new ConcurrentHashMap<>();

    /** {pressure 0..100, blowout end tick, last bleed tick} */
    static double[] state(ServerLevel level) {
        return STATE.computeIfAbsent(level.dimension(), k -> new double[]{35, 0, -1000});
    }

    static int stage(double p) {
        return p < 45 ? 0 : p < 70 ? 1 : p < 88 ? 2 : 3;
    }

    /** Called by a bleed valve wheel. */
    public static void bleed(ServerLevel level, BlockPos pos, Direction facing, Player who) {
        double[] st = state(level);
        long now = level.getGameTime();
        if (now - st[2] < 60) return;
        st[2] = now;
        st[0] = Math.max(0, st[0] - 28);
        level.playSound(null, pos, ModSounds.get("event.boiler_room_bleed"), SoundSource.BLOCKS, 1.3f, 0.9f + level.random.nextFloat() * 0.2f);
        Direction out = facing.getOpposite();
        for (int n = 0; n < 14; n++) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5 + out.getStepX() * 0.3, pos.getY() + 0.9,
                    pos.getZ() + 0.5 + out.getStepZ() * 0.3, 0, out.getStepX() * 0.3 + (level.random.nextDouble() - 0.5) * 0.2,
                    0.25, out.getStepZ() * 0.3 + (level.random.nextDouble() - 0.5) * 0.2, 0.6);
        }
        if (who instanceof ServerPlayer sp)
            sp.displayClientMessage(Component.literal("The wheel shrieks. The needles drop.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState plate = ModBlocks.state("br_boiler_plate");
        final BlockState seam = ModBlocks.state("br_boiler_seam");
        final BlockState hot = ModBlocks.state("br_boiler_hot");
        final BlockState firebox = ModBlocks.state("br_firebox");
        final BlockState fireboxOpen = ModBlocks.state("br_firebox_open");
        final BlockState ash = ModBlocks.state("br_ash_door");
        final BlockState brick = ModBlocks.state("br_brick_wall");
        final BlockState brickBase = ModBlocks.state("br_brick_base");
        final BlockState chalk = ModBlocks.state("br_chalkboard");
        final BlockState floor = ModBlocks.state("br_floor");
        final BlockState ceiling = ModBlocks.state("br_ceiling");
        final BlockState grate = ModBlocks.state("br_ash_grate");
        final BlockState door = ModBlocks.state("br_maint_door");
        final BlockState stairLight = ModBlocks.state("br_stair_light");
        final BlockState stairs = ModBlocks.state("br_brick_stairs");
        final BlockState pipeH = ModBlocks.state("br_pipe_h");
        final BlockState pipeV = ModBlocks.state("br_pipe_v");
        final BlockState lagH = ModBlocks.state("br_pipe_lagged_h");
        final BlockState lagV = ModBlocks.state("br_pipe_lagged_v");
        final BlockState lamp = ModBlocks.state("br_lamp");
        final BlockState coal = ModBlocks.state("br_coal_pile");
        final BlockState cot = ModBlocks.state("br_cot");
        final BlockState gauge = ModBlocks.state("br_gauge");
        final BlockState valve = ModBlocks.state("br_valve");
        final BlockState relief = ModBlocks.state("br_relief");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 16, 2);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, g, s, i, k, x0, z0, wd, dp));
        }

        static BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void cell(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            long h = g.hash(i, k, 70);
            // brick pier at every corner, with a chalk pressure log on some
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) {
                    w.set(x0 + dx, FLOOR + 1, z0 + dz, brickBase);
                    w.fill(x0 + dx, FLOOR + 2, z0 + dz, x0 + dx, CEIL - 1, z0 + dz, brick);
                }
            if (Hash.unit(h >>> 4) < 0.12f) w.set(x0 + 1, FLOOR + 3, z0 + 1, chalk);
            // a steam header runs along z over every cell
            for (int z = z0; z < z0 + dp; z++) w.set(x0 + 9, CEIL - 2, z, face(lagH, Direction.NORTH));
            for (int x = x0 + 2; x < x0 + wd; x++) w.set(x, CEIL - 4, z0 + 3, face(pipeH, Direction.EAST));
            // partition wall with an arch on some north edges
            boolean wall = Hash.unit(h >>> 8) < 0.18f;
            if (wall) partition(w, h, x0, z0, wd);
            // a hanging caged lamp on a rod over the aisle
            for (int[] l : new int[][]{{x0 + 5, z0 + 2}, {x0 + 12, z0 + 14}}) {
                w.fill(l[0], FLOOR + 6, l[1], l[0], CEIL - 1, l[1], pipeV);
                w.set(l[0], FLOOR + 5, l[1], lamp);
            }
            float r = Hash.unit(h);
            if (r < 0.70f) boiler(w, s, h, x0, z0);
            else if (r < 0.82f) bunker(w, h, x0, z0, wd, dp);
            else if (r < 0.90f) pumps(w, h, x0, z0);
            else if (r < 0.95f) restCorner(w, h, x0, z0);
            else if (r < 0.965f) stairUp(w, x0, z0);
        }

        void partition(ChunkWriter w, long h, int x0, int z0, int wd) {
            int a0 = x0 + 6, a1 = x0 + 10;
            boolean hasDoor = Hash.unit(h >>> 30) < 0.25f;
            for (int x = x0 + 2; x < x0 + wd; x++) {
                for (int y = FLOOR + 1; y < CEIL; y++) {
                    boolean arch = x >= a0 && x <= a1 && y <= FLOOR + 5 + (x == a0 || x == a1 ? -1 : 0);
                    if (arch) continue;
                    w.set(x, y, z0, y == FLOOR + 1 ? brickBase : brick);
                }
            }
            if (hasDoor) {
                w.set(x0 + 13, FLOOR + 1, z0, door);
                w.set(x0 + 13, FLOOR + 2, z0, door);
            }
        }

        /** A horizontal fire-tube boiler lying along x, front (firebox) facing west. */
        void boiler(ChunkWriter w, long s, long h, int x0, int z0) {
            int bx0 = x0 + 4, bx1 = x0 + 13, cz = z0 + 8, cy = FLOOR + 4;
            for (int x = bx0; x <= bx1; x++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dy * dy + dz * dz > 10) continue;
                        BlockState b = (x - bx0) % 3 == 0 ? seam : (dy <= -1 && x - bx0 < 3 ? hot : plate);
                        w.set(x, cy + dy, cz + dz, b);
                    }
                }
            }
            // brick saddles under the shell
            for (int x : new int[]{bx0 + 1, bx1 - 1})
                for (int dz = -3; dz <= 3; dz++) {
                    w.set(x, FLOOR + 1, cz + dz, brickBase);
                    if (Math.abs(dz) == 3) w.set(x, FLOOR + 2, cz + dz, brick);
                }
            boolean open = Hash.unit(h >>> 12) < 0.15f;
            w.set(bx0, FLOOR + 2, cz, open ? fireboxOpen : firebox);
            w.set(bx0, FLOOR + 2, cz - 1, hot);
            w.set(bx0, FLOOR + 2, cz + 1, hot);
            // ash-pit doors glowing low along both flanks
            for (int x = bx0 + 2; x <= bx1 - 2; x += 3) {
                w.set(x, FLOOR + 2, cz - 3, ash);
                w.set(x, FLOOR + 2, cz + 3, ash);
            }
            // gauges on the front plate, a bleed valve beside the firebox
            w.set(bx0 - 1, GAUGE_Y, cz - 1, face(gauge, Direction.EAST));
            if (Hash.unit(h >>> 16) < 0.6f) w.set(bx0 - 1, GAUGE_Y, cz + 1, face(gauge, Direction.EAST));
            w.set(bx0 - 1, FLOOR + 3, cz + 2, face(valve, Direction.EAST));
            // steam riser from the dome to the header
            w.set(x0 + 9, cy + 4, cz, relief);
            w.fill(x0 + 9, cy + 4, cz + 1, x0 + 9, cy + 4, cz + 1, face(lagH, Direction.NORTH));
            w.fill(x0 + 9, cy + 4, cz + 2, x0 + 9, CEIL - 3, cz + 2, lagV);
            // relief valves standing in the aisles
            for (int n = 0; n < 2; n++) {
                long hh = Hash.hash(h, n, 90);
                if (Hash.unit(hh) < 0.65f) {
                    int rx = bx0 + 1 + Hash.range(hh >>> 8, 8);
                    int rz = n == 0 ? cz - 5 : cz + 5;
                    w.set(rx, FLOOR + 1, rz, face(relief, n == 0 ? Direction.SOUTH : Direction.NORTH));
                }
            }
            if (Hash.unit(h >>> 20) < 0.06f) {
                for (int dz = -1; dz <= 1; dz++) w.set(bx0 - 2, FLOOR, cz + dz, grate);
            }
            if (Hash.unit(h >>> 24) < 0.08f) w.set(bx0 - 2, FLOOR + 1, cz + 4, face(coal, Direction.NORTH));
        }

        void bunker(ChunkWriter w, long h, int x0, int z0, int wd, int dp) {
            for (int n = 0; n < 7; n++) {
                long hh = Hash.hash(h, n, 91);
                int x = x0 + 3 + Hash.range(hh, wd - 5), z = z0 + 3 + Hash.range(hh >>> 8, dp - 5);
                w.set(x, FLOOR + 1, z, face(coal, Direction.from2DDataValue(Hash.range(hh >>> 16, 4))));
                if (Hash.unit(hh >>> 24) < 0.3f) w.set(x, FLOOR + 2, z, face(coal, Direction.from2DDataValue(Hash.range(hh >>> 28, 4))));
            }
            // a low brick bin wall
            for (int x = x0 + 2; x < x0 + 9; x++) {
                w.set(x, FLOOR + 1, z0 + dp - 2, brickBase);
                w.set(x, FLOOR + 2, z0 + dp - 2, brick);
            }
        }

        void pumps(ChunkWriter w, long h, int x0, int z0) {
            for (int n = 0; n < 3; n++) {
                int x = x0 + 4 + n * 3, z = z0 + 8;
                w.fill(x, FLOOR + 1, z, x, CEIL - 3, z, n == 1 ? lagV : pipeV);
                w.set(x, FLOOR + 2, z + 1, face(valve, Direction.NORTH));
                w.set(x + 1, FLOOR + 1, z - 2, face(relief, Direction.EAST));
            }
            w.set(x0 + 4, GAUGE_Y, z0 + 9, face(gauge, Direction.NORTH));
        }

        /** Someone lived here: a cot, a lamp, a crate, a tally of days on the brick. */
        void restCorner(ChunkWriter w, long h, int x0, int z0) {
            w.set(x0 + 3, FLOOR + 1, z0 + 3, face(cot, Direction.EAST));
            w.set(x0 + 1, FLOOR + 3, z0 + 1, chalk);
            w.set(x0 + 5, FLOOR + 1, z0 + 3, Hash.unit(h >>> 40) < 0.5f ? supply : bottle);
            w.set(x0 + 4, FLOOR + 1, z0 + 6, face(coal, Direction.SOUTH));
        }

        /** A brick stair climbing to a warm, lit doorway: upstairs, something like a hotel. */
        void stairUp(ChunkWriter w, int x0, int z0) {
            int z = z0 + 6;
            for (int step = 0; step < 6; step++) {
                int x = x0 + 4 + step;
                for (int dz = 0; dz < 3; dz++) {
                    for (int y = FLOOR + 1; y < FLOOR + 1 + step; y++) w.set(x, y, z + dz, brick);
                    w.set(x, FLOOR + 1 + step, z + dz, stairs.setValue(StairBlock.FACING, Direction.EAST));
                }
            }
            int lx = x0 + 10;
            for (int dz = -1; dz <= 3; dz++)
                for (int y = FLOOR + 1; y <= FLOOR + 11; y++) w.set(lx + 1, y, z + dz, brick);
            for (int dz = 0; dz < 3; dz++) {
                for (int y = FLOOR + 1; y < FLOOR + 7; y++) w.set(lx, y, z + dz, brick);
                w.set(lx, FLOOR + 7, z + dz, stairs.setValue(StairBlock.FACING, Direction.EAST));
            }
            w.set(lx + 1, FLOOR + 8, z + 1, stairLight);
            w.set(lx + 1, FLOOR + 9, z + 1, stairLight);
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final class Pressure implements LevelBehavior {
        private final Map<java.util.UUID, List<BlockPos>> valves = new ConcurrentHashMap<>();

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            double[] st = state(level);
            long now = level.getGameTime();
            if (now < st[1]) return;
            if (st[1] > 0) {
                st[0] = 12;
                st[1] = 0;
            }
            st[0] = Math.min(100, st[0] + 0.022);
            if (st[0] >= 100) {
                st[1] = now + 70;
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.boiler_room_blowout"), SoundSource.BLOCKS, 1.6f, 0.9f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.8f));
                    p.displayClientMessage(Component.literal("The relief valves lift.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), true);
                }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            double[] st = state(level);
            long now = level.getGameTime();
            boolean blowing = now < st[1];
            int t = p.tickCount + p.getId();
            if (t % 20 == 0) {
                updateGauges(p, level, stage(blowing ? 100 : st[0]));
                valves.put(p.getUUID(), findValves(p, level));
            }
            if (!blowing && st[0] >= 82 && t % 50 == 0) {
                Vec3 at = p.position().add((p.getRandom().nextDouble() - 0.5) * 16, 6, (p.getRandom().nextDouble() - 0.5) * 16);
                level.playSound(null, BlockPos.containing(at), ModSounds.get("event.boiler_room_knock"), SoundSource.BLOCKS, 1.2f,
                        0.9f + (float) (st[0] - 82) / 60f);
            }
            if (p.isSpectator()) return;
            if (t % 10 == 0 && !p.isCreative()) heat(p, level);
            if (blowing && t % 3 == 0) blow(p, level, valves.getOrDefault(p.getUUID(), List.of()));
        }

        private static void updateGauges(ServerPlayer p, ServerLevel level, int stage) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = p.getBlockX() - 24; x <= p.getBlockX() + 24; x++) {
                for (int z = p.getBlockZ() - 24; z <= p.getBlockZ() + 24; z++) {
                    m.set(x, GAUGE_Y, z);
                    BlockState b = level.getBlockState(m);
                    if (b.getBlock() instanceof GaugeBlock && b.getValue(GaugeBlock.AGE) != stage)
                        level.setBlock(m, b.setValue(GaugeBlock.AGE, stage), Block.UPDATE_CLIENTS);
                }
            }
        }

        private static List<BlockPos> findValves(ServerPlayer p, ServerLevel level) {
            List<BlockPos> out = new ArrayList<>();
            for (int y : new int[]{FLOOR + 1, FLOOR + 8}) {
                for (BlockPos q : BlockPos.betweenClosed(p.getBlockX() - 16, y, p.getBlockZ() - 16, p.getBlockX() + 16, y, p.getBlockZ() + 16)) {
                    if (level.getBlockState(q).getBlock() instanceof ReliefValveBlock) out.add(q.immutable());
                }
            }
            return out;
        }

        private static void heat(ServerPlayer p, ServerLevel level) {
            Block fb = ModBlocks.get("br_firebox"), fo = ModBlocks.get("br_firebox_open");
            BlockPos at = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
                BlockState b = level.getBlockState(q);
                if (b.is(fb) || b.is(fo)) {
                    p.hurt(level.damageSources().inFire(), b.is(fo) ? 2f : 1f);
                    if (p.tickCount % 40 == 0)
                        p.displayClientMessage(Component.literal("The heat off the firebox blisters your skin.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
                    return;
                }
            }
        }

        private static void blow(ServerPlayer p, ServerLevel level, List<BlockPos> valves) {
            for (BlockPos v : valves) {
                double cx = v.getX() + 0.5, cy = v.getY() + 1.0, cz = v.getZ() + 0.5;
                for (int n = 0; n < 4; n++) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    level.sendParticles(ParticleTypes.CLOUD, cx, cy, cz, 0, Math.cos(a) * 0.5, 0.35 + level.random.nextDouble() * 0.3,
                            Math.sin(a) * 0.5, 0.8);
                }
                if (p.isCreative()) continue;
                double d2 = p.distanceToSqr(cx, cy - 0.5, cz);
                if (d2 < 3.4 * 3.4 && p.invulnerableTime <= 10) {
                    p.hurt(level.damageSources().hotFloor(), 4f);
                    Vec3 away = p.position().subtract(cx, p.getY(), cz).normalize();
                    p.push(away.x * 0.6, 0.25, away.z * 0.6);
                    p.hurtMarked = true;
                    Sanity.add(p, -3f);
                }
            }
        }
    }
}
