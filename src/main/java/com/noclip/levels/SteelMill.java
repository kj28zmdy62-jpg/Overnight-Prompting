package com.noclip.levels;

import com.noclip.block.AlarmBeaconBlock;
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
import com.noclip.util.Hash;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Steel Mill. Bay after bay of a casting hall under a roof you cannot see: crane
 * girders, ladles hanging from hooks with their tops still glowing, roller tables
 * with red-hot slabs, sparks drifting down. A refractory channel runs the length
 * of every bay with plate bridges across it. When the siren winds up and the
 * amber lights turn, a ladle tips somewhere upstream and molten steel comes down
 * the channel faster than you can walk.
 */
public final class SteelMill {
    static final int FLOOR = 20, CEIL = FLOOR + 26, BAY = 32, CH = 16;

    private SteelMill() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("steel_mill")
                .number("").title("Steel Mill").order(2090)
                .tagline("When the siren starts, get off the channels and cross only on the plates.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Pours())
                .visuals(new Visuals().darkness(0.3f).tint(1.18f, 0.94f, 0.74f).saturation(0.95f).contrast(1.16f)
                        .brightness(1.15f).grain(0.09f).aberration(0.002f).vignette(0.6f).warp(0.04f)
                        .fog(4f, 46f).fogColor(0x2A140A).bloom(0.95f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(20, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "boiler_room", 2).exit(ExitType.NOCLIP, "power_plant", 1).exit(ExitType.NOCLIP, "assembly_line", 1)
                .exit(ExitType.DOOR, "boiler_room", 2).exit(ExitType.DOOR, "power_plant", 1).exit(ExitType.DOOR, "assembly_line", 1)
                .exit(ExitType.FALL, "mine_shaft", 1).exit(ExitType.FALL, "level_8", 1)
                .build();
    }

    static boolean channel(int x) {
        int b = Math.floorMod(x, BAY);
        return b == CH || b == CH + 1;
    }

    static boolean bridge(int z) {
        return Math.floorMod(z, 16) == 8 || Math.floorMod(z, 16) == 9;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("sm_floor");
        final BlockState hazardPlate = ModBlocks.state("sm_floor_hazard");
        final BlockState refractory = ModBlocks.state("sm_refractory");
        final BlockState column = ModBlocks.state("sm_column");
        final BlockState girder = ModBlocks.state("sm_girder");
        final BlockState ladle = ModBlocks.state("sm_ladle");
        final BlockState pool = ModBlocks.state("sm_pool");
        final BlockState crust = ModBlocks.state("sm_crust");
        final BlockState roller = ModBlocks.state("sm_roller");
        final BlockState slab = ModBlocks.state("sm_slab");
        final BlockState machine = ModBlocks.state("sm_machine");
        final BlockState glass = ModBlocks.state("sm_pulpit_glass");
        final BlockState beacon = ModBlocks.state("sm_beacon");
        final BlockState door = ModBlocks.state("sm_door");
        final BlockState chute = ModBlocks.state("sm_chute");
        final BlockState lamp = ModBlocks.state("sm_lamp");
        final BlockState ceiling = ModBlocks.state("sm_ceiling");
        final BlockState hook = ModBlocks.state("sm_hook");
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 10);
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 2, w.maxZ, sub);
            w.layer(FLOOR - 1, refractory);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int b = Math.floorMod(x, BAY);
                    if (channel(x)) {
                        w.set(x, FLOOR, z, bridge(z) ? floor : ModBlocks.AIR);
                    } else {
                        w.set(x, FLOOR, z, (b == CH - 1 || b == CH + 2) ? hazardPlate : floor);
                    }
                    if ((b == CH - 1 || b == CH + 2) && Math.floorMod(z, 16) == 7) w.set(x, FLOOR + 1, z, beacon);
                    if ((b == 0 || b == 1) && Math.floorMod(z, 8) < 2) w.fill(x, FLOOR + 1, z, x, CEIL - 1, z, column);
                    if ((b == 8 || b == 24) && Math.floorMod(z, 12) == 6) {
                        w.set(x, CEIL, z, lamp);
                        w.set(x, FLOOR + 7, z, light);
                    }
                    if (b == 8 || b == 24) w.set(x, CEIL - 5, z, girder);
                    if (b >= 25 && b <= 27) w.set(x, FLOOR + 1, z, roller);
                }
            }
            int i0 = Math.floorDiv(w.minX, BAY) - 1, i1 = Math.floorDiv(w.maxX, BAY) + 1;
            int k0 = Math.floorDiv(w.minZ, 48) - 1, k1 = Math.floorDiv(w.maxZ, 48) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) section(w, s, i, k, i * BAY, k * 48);
        }

        static BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        /** One 32 x 48 stretch of a bay: crane, ladles, presses, slabs. */
        void section(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            long h = Hash.hash(s, i, k, 3);
            // crane bridge across the bay with a ladle hanging from the hook
            if (Hash.unit(h) < 0.6f) {
                int cz = z0 + 20;
                for (int x = x0 + 8; x <= x0 + 24; x++) w.set(x, CEIL - 4, cz, girder);
                int hx = x0 + 11 + Hash.range(h >>> 4, 10);
                for (int y = FLOOR + 12; y < CEIL - 4; y++) w.set(hx, y, cz, hook);
                bowl(w, hx, FLOOR + 8, cz, Hash.unit(h >>> 8) < 0.7f);
            }
            // a ladle on the floor by the head of the channel, spilling into it
            if (Hash.unit(h >>> 12) < 0.35f) {
                int lx = x0 + 11, lz = z0 + 34;
                bowl(w, lx, FLOOR + 1, lz, true);
                for (int x = lx + 3; x < x0 + CH; x++) w.set(x, FLOOR, lz, pool);
            }
            // press housings along the far side
            for (int n = 0; n < 2; n++) {
                int mz = z0 + 4 + n * 24;
                for (int x = x0 + 3; x <= x0 + 6; x++)
                    for (int z = mz; z <= mz + 6; z++)
                        for (int y = FLOOR + 1; y <= FLOOR + 5; y++) w.set(x, y, z, machine);
                if (Hash.unit(h >>> (16 + n)) < 0.18f) {
                    w.set(x0 + 7 - 1, FLOOR + 1, mz + 3, door);
                    w.set(x0 + 7 - 1, FLOOR + 2, mz + 3, door);
                }
            }
            // red-hot slabs lying on the roller table
            if (Hash.unit(h >>> 24) < 0.5f) {
                int sz = z0 + 6 + Hash.range(h >>> 28, 30);
                for (int z = sz; z < sz + 6; z++)
                    for (int x = x0 + 25; x <= x0 + 27; x++) w.set(x, FLOOR + 2, z, slab);
            }
            // a slag chute: a glowing hole in the floor
            if (Hash.unit(h >>> 34) < 0.06f) {
                for (int x = x0 + 20; x <= x0 + 21; x++)
                    for (int z = z0 + 40; z <= z0 + 41; z++) w.set(x, FLOOR, z, chute);
            }
            // the control pulpit: a glass box up on legs
            if (Hash.unit(h >>> 40) < 0.15f) {
                int px = x0 + 20, pz = z0 + 2;
                for (int x = px; x <= px + 3; x++)
                    for (int z = pz; z <= pz + 3; z++) {
                        boolean edge = x == px || x == px + 3 || z == pz || z == pz + 3;
                        if (edge && (x == px || x == px + 3) && (z == pz || z == pz + 3))
                            w.fill(x, FLOOR + 1, z, x, FLOOR + 5, z, column);
                        w.set(x, FLOOR + 6, z, machine);
                        if (edge) {
                            w.set(x, FLOOR + 7, z, glass);
                            w.set(x, FLOOR + 8, z, glass);
                        }
                        w.set(x, FLOOR + 9, z, machine);
                    }
            }
        }

        /** A ladle: a steel bowl five across with a skin of molten steel on top. */
        void bowl(ChunkWriter w, int cx, int y0, int cz, boolean hot) {
            for (int dy = 0; dy < 4; dy++) {
                int r = dy == 0 ? 1 : 2;
                for (int dx = -r; dx <= r; dx++)
                    for (int dz = -r; dz <= r; dz++) {
                        boolean rim = Math.abs(dx) == r || Math.abs(dz) == r;
                        if (dy == 3 && !rim) w.set(cx + dx, y0 + dy, cz + dz, hot ? pool : crust);
                        else w.set(cx + dx, y0 + dy, cz + dz, ladle);
                    }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The pour: a siren, amber lights, then molten steel down the nearest channel. */
    static final class Pours implements LevelBehavior {
        /** {phase 0 idle/1 siren/2 pouring/3 cooling, phaseEnd, channel x, front z (x100), dir, next} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Map<ResourceKey<Level>, List<BlockPos>> POURED = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, 0, 0, 1, level.getGameTime() + 1600 + level.random.nextInt(1600)});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (level.players().isEmpty()) return;
            ServerPlayer p = level.players().get(0);
            switch ((int) st[0]) {
                case 0 -> {
                    if (now < st[5]) return;
                    st[0] = 1;
                    st[1] = now + 120;
                    st[2] = Math.floorDiv(p.getBlockX() - CH + BAY / 2, BAY) * BAY + CH;
                    st[4] = level.random.nextBoolean() ? 1 : -1;
                    st[3] = (long) ((p.getZ() - st[4] * 52) * 100);
                    for (ServerPlayer q : level.players()) {
                        level.playSound(null, q.blockPosition(), ModSounds.get("event.steel_mill_siren"), SoundSource.BLOCKS, 1.8f, 1f);
                        q.displayClientMessage(Component.literal("The pour siren. Off the channels.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                        beacons(level, q, true);
                    }
                }
                case 1 -> {
                    if (now < st[1]) return;
                    st[0] = 2;
                    double fz = st[3] / 100.0;
                    level.playSound(null, BlockPos.containing(st[2], FLOOR + 2, fz), ModSounds.get("event.steel_mill_pour"),
                            SoundSource.BLOCKS, 3f, 0.9f);
                    for (ServerPlayer q : level.players()) ServerPlayNetworking.send(q, new NoclipNet.EventPayload("pulse", 0.4f));
                }
                case 2 -> pourTick(level, st, p);
                case 3 -> {
                    if (now == st[1] - 300) setAll(level, ModBlocks.state("sm_crust"));
                    if (now < st[1]) return;
                    setAll(level, ModBlocks.AIR);
                    POURED.remove(level.dimension());
                    st[0] = 0;
                    st[5] = now + 2400 + level.random.nextInt(1800);
                    for (ServerPlayer q : level.players()) beacons(level, q, false);
                }
                default -> st[0] = 0;
            }
        }

        private static void pourTick(ServerLevel level, long[] st, ServerPlayer p) {
            List<BlockPos> list = POURED.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
            double before = st[3] / 100.0, after = before + st[4] * 0.32;
            st[3] = (long) (after * 100);
            BlockState molten = ModBlocks.state("sm_molten");
            int z0 = (int) Math.floor(Math.min(before, after)), z1 = (int) Math.floor(Math.max(before, after));
            for (int z = z0; z <= z1; z++) {
                if (bridge(z)) continue;
                for (int x = (int) st[2]; x <= st[2] + 1; x++) {
                    BlockPos q = new BlockPos(x, FLOOR, z);
                    if (!level.isLoaded(q) || !level.getBlockState(q).isAir()) continue;
                    level.setBlock(q, molten, Block.UPDATE_CLIENTS);
                    list.add(q);
                }
            }
            long now = level.getGameTime();
            if (now % 3 == 0)
                level.sendParticles(ParticleTypes.LAVA, st[2] + 1, FLOOR + 1, after, 3, 0.5, 0.1, 0.5, 0.1);
            if (now % 12 == 0)
                level.playSound(null, BlockPos.containing(st[2] + 1, FLOOR + 1, after), ModSounds.get("event.steel_mill_sizzle"),
                        SoundSource.BLOCKS, 1.5f, 0.9f + level.random.nextFloat() * 0.2f);
            if ((after - p.getZ()) * st[4] > 52 || list.size() > 400) {
                st[0] = 3;
                st[1] = now + 500;
            }
        }

        private static void setAll(ServerLevel level, BlockState to) {
            List<BlockPos> list = POURED.get(level.dimension());
            if (list == null) return;
            for (BlockPos q : list) if (level.isLoaded(q)) level.setBlock(q, to, Block.UPDATE_CLIENTS);
        }

        private static void beacons(ServerLevel level, ServerPlayer p, boolean on) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = p.getBlockX() - 40; x <= p.getBlockX() + 40; x++) {
                for (int z = p.getBlockZ() - 40; z <= p.getBlockZ() + 40; z++) {
                    m.set(x, FLOOR + 1, z);
                    if (!level.isLoaded(m)) continue;
                    BlockState b = level.getBlockState(m);
                    if (b.getBlock() instanceof AlarmBeaconBlock && b.getValue(AlarmBeaconBlock.LIT) != on)
                        level.setBlock(m, b.setValue(AlarmBeaconBlock.LIT, on), Block.UPDATE_CLIENTS);
                }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || p.isCreative()) return;
            Block molten = ModBlocks.get("sm_molten"), pool = ModBlocks.get("sm_pool");
            BlockState feet = level.getBlockState(p.blockPosition());
            if (feet.is(molten) || feet.is(pool)) p.setRemainingFireTicks(120);
        }
    }
}
