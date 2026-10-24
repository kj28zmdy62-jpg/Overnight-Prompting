package com.noclip.levels;

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
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Construction Site. An unfinished high-rise at night that has no edge: bare slabs on
 * columns, four floors and a roof that's only half poured, rebar starters, tripod work
 * lights, plastic sheeting breathing in the wind, tower cranes against the stars.
 * Some bays have no floor all the way down. Someone up there is still working: when the
 * whistle blows, something comes down, and you'd better be under a slab when it does.
 */
public final class ConstructionSite {
    static final String ID = "construction_site";
    static final int GROUND = 8, STORY = 6, FLOORS = 4, BAY = 8, CT = 66;

    private ConstructionSite() {}

    static int fl(int k) {
        return GROUND + STORY * k;
    }

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Construction Site").order(3160)
                .tagline("When the whistle blows, get under a slab.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Whistle())
                .visuals(new Visuals().tint(0.94f, 0.98f, 1.1f).saturation(0.7f).contrast(1.14f).brightness(1.3f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.52f).fog(6f, 72f).fogColor(0x121620).bloom(0.8f).sky())
                .sanityDrain(0.03f)
                .spawnY(GROUND + 1, fl(FLOORS) + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(26, 44).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(10, 22).weight(2))
                .exit(ExitType.FALL, "parking_garage", 2).exit(ExitType.FALL, "level_11", 1)
                .exit(ExitType.DOOR, "level_11", 2).exit(ExitType.DOOR, "stairwell", 1)
                .exit(ExitType.ASCEND, "rooftops", 3).exit(ExitType.ASCEND, "sky_rooms", 1)
                .exit(ExitType.NOCLIP, "rain_city", 1).exit(ExitType.NOCLIP, "level_11", 2).exit(ExitType.NOCLIP, "rooftops", 1)
                .build();
    }

    static boolean atrium(long s, int bx, int bz) {
        return Hash.unit(s, bx, bz, 1) < 0.045f && !core(bx, bz);
    }

    static boolean core(int bx, int bz) {
        return Math.floorMod(bx, 4) == 1 && Math.floorMod(bz, 4) == 1;
    }

    /** Is there floor at storey k in this bay? */
    static boolean slab(long s, int bx, int bz, int k) {
        if (atrium(s, bx, bz)) return false;
        if (core(bx, bz)) return true;
        if (k == FLOORS) return Hash.unit(s, bx, bz, 40) > 0.38f;
        return Hash.unit(s, bx, bz, 10 + k) > 0.05f;
    }

    static final class Gen implements LevelGenerator {
        final BlockState slab = ModBlocks.state("cons_slab");
        final BlockState column = ModBlocks.state("cons_column");
        final BlockState stairs = ModBlocks.state("cons_stairs");
        final BlockState rebar = ModBlocks.state("cons_rebar");
        final BlockState mesh = ModBlocks.state("cons_mesh");
        final BlockState plywood = ModBlocks.state("cons_plywood");
        final BlockState scaffold = ModBlocks.state("cons_scaffold");
        final BlockState board = ModBlocks.state("cons_board");
        final BlockState sheeting = ModBlocks.state("cons_sheeting");
        final BlockState mud = ModBlocks.state("cons_mud");
        final BlockState worklight = ModBlocks.state("cons_worklight");
        final BlockState crane = ModBlocks.state("cons_crane");
        final BlockState cab = ModBlocks.state("cons_cab");
        final BlockState weight = ModBlocks.state("cons_counterweight");
        final BlockState beacon = ModBlocks.state("cons_beacon");
        final BlockState barrier = ModBlocks.state("cons_barrier");
        final BlockState pallet = ModBlocks.state("cons_blocks_pallet");
        final BlockState rubble = ModBlocks.state("cons_rubble");
        final BlockState sign = ModBlocks.state("cons_sign");
        final BlockState hoist = ModBlocks.state("cons_hoist");
        final BlockState door = ModBlocks.state("cons_door");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, GROUND - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            for (int rx = Math.floorDiv(w.minX - 40, 64); rx <= Math.floorDiv(w.maxX + 40, 64); rx++)
                for (int rz = Math.floorDiv(w.minZ - 4, 64); rz <= Math.floorDiv(w.maxZ + 4, 64); rz++)
                    if (Hash.unit(s, rx, rz, 50) < 0.75f) crane(w, s, rx * 64 + 36, rz * 64 + 36);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int bx = Math.floorDiv(x, BAY), bz = Math.floorDiv(z, BAY);
            int lx = Math.floorMod(x, BAY), lz = Math.floorMod(z, BAY);
            boolean atr = atrium(s, bx, bz);
            w.set(x, GROUND, z, atr && lx > 0 && lz > 0 ? pit : Hash.unit(s, x, z, 2) < 0.03f ? water : mud);
            for (int k = 1; k <= FLOORS; k++) {
                if (slab(s, bx, bz, k)) w.set(x, fl(k), z, slab);
                else if (k == FLOORS && !atr && Hash.unit(s, bx, bz, 41) < 0.5f) w.set(x, fl(k), z, mesh);
            }
            if (lx == 0 && lz == 0) {
                for (int y = GROUND + 1; y < fl(FLOORS); y++) w.set(x, y, z, column);
                w.set(x, fl(FLOORS) + 1, z, rebar);
                return;
            }
            if (atr) {
                atriumScaffold(w, s, x, z, bx, bz, lx, lz);
                return;
            }
            if (core(bx, bz)) {
                coreStairs(w, s, x, z, lx, lz);
            }
            for (int k = 0; k < FLOORS + 1; k++) {
                int y = fl(k);
                if (k > 0 && !slab(s, bx, bz, k)) continue;
                edge(w, s, x, z, y, k, lx, lz, bx, bz);
                if (core(bx, bz)) continue;
                props(w, s, x, z, y, k, lx, lz, bx, bz);
            }
        }

        /** Edge protection where the floor stops at a hole. */
        void edge(ChunkWriter w, long s, int x, int z, int y, int k, int lx, int lz, int bx, int bz) {
            if (k == 0) return;
            boolean west = lx == 1 && !slab(s, bx - 1, bz, k), east = lx == BAY - 1 && !slab(s, bx + 1, bz, k);
            boolean north = lz == 1 && !slab(s, bx, bz - 1, k), south = lz == BAY - 1 && !slab(s, bx, bz + 1, k);
            if (!(west || east || north || south)) return;
            if (Hash.unit(s, x * 13L + y, z, 3) < 0.12f) return;
            boolean ns = west || east;
            w.set(x, y + 1, z, barrier.setValue(ns ? IronBarsBlock.NORTH : IronBarsBlock.EAST, true)
                    .setValue(ns ? IronBarsBlock.SOUTH : IronBarsBlock.WEST, true));
        }

        void props(ChunkWriter w, long s, int x, int z, int y, int k, int lx, int lz, int bx, int bz) {
            long h = Hash.hash(s, bx * 31L + k, bz, 4);
            float r = Hash.unit(h);
            boolean dark = Hash.unit(s, Math.floorDiv(x, 24) * 7L + k, Math.floorDiv(z, 24), 5) < 0.22f;
            if (lx == 4 && lz == 4 && r < 0.4f && !dark) {
                Direction f = Direction.from2DDataValue(Hash.range(h >>> 8, 4));
                w.set(x, y + 1, z, OutsideKit.facing(worklight, f));
                return;
            }
            if (r > 0.4f && r < 0.52f && lx >= 2 && lx <= 3 && lz >= 5 && lz <= 6) {
                w.set(x, y + 1, z, pallet);
                if ((lx + lz) % 2 == 0) w.set(x, y + 2, z, pallet);
                return;
            }
            if (r > 0.52f && r < 0.6f && lz == 6 && lx >= 2 && lx <= 6) {
                w.set(x, y + 1, z, plywood);
                return;
            }
            if (k > 0 && k < FLOORS && Hash.unit(s, bx * 7L + k, bz, 7) < 0.14f && lz == 0) {
                for (int yy = y + 1; yy < y + STORY; yy++)
                    w.set(x, yy, z, sheeting.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));
                return;
            }
            if (Hash.unit(s, x * 3L + y, z, 8) < 0.004f) w.set(x, y + 1, z, rubble);
            if (k == 0 && lx == 3 && lz == 7 && r < 0.3f) w.set(x, y + 1, z, OutsideKit.facing(sign, Direction.NORTH));
        }

        /** A straight flight from each floor to the next, in the core bays; a site office on the ground. */
        void coreStairs(ChunkWriter w, long s, int x, int z, int lx, int lz) {
            // switchback: even flights rise east in lanes 2-3, odd flights rise west in lanes 5-6
            if (lx >= 1 && lx <= 6) {
                for (int k = 0; k < FLOORS; k++) {
                    boolean even = k % 2 == 0;
                    if (even ? (lz != 2 && lz != 3) : (lz != 5 && lz != 6)) continue;
                    int step = even ? lx : 7 - lx;
                    int y = fl(k) + step;
                    w.set(x, y, z, stairs.setValue(StairBlock.FACING, even ? Direction.EAST : Direction.WEST));
                    for (int yy = fl(k) + 1; yy < y; yy++) w.set(x, yy, z, slab);
                    if (step <= 5) w.set(x, fl(k + 1), z, ModBlocks.AIR);
                }
            }
            // site office on the ground floor
            if (lx >= 5 && lx <= 7 && lz >= 5 && lz <= 7) {
                for (int y = GROUND + 1; y <= GROUND + 3; y++) w.set(x, y, z, lx == 6 && lz == 5 && y <= GROUND + 2 ? door : plywood);
            }
        }

        void atriumScaffold(ChunkWriter w, long s, int x, int z, int bx, int bz, int lx, int lz) {
            if (Hash.unit(s, bx, bz, 6) > 0.6f || lx > 2 || lz == 0) return;
            for (int k = 1; k <= FLOORS; k++) {
                if (lx == 1) {
                    for (int y = fl(k - 1) + 1; y <= fl(k); y++)
                        w.set(x, y, z, scaffold.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
                } else w.set(x, fl(k), z, board);
            }
            // the hoist: a lift cage on the scaffold, its controls on every floor
            if (lx == 2 && lz == 4 && Hash.unit(s, bx, bz, 9) < 0.5f) {
                for (int k = 1; k <= FLOORS; k++) w.set(x, fl(k) + 2, z, hoist);
            }
        }

        void crane(ChunkWriter w, long s, int x0, int z0) {
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    for (int y = GROUND + 1; y <= CT; y++) w.set(x0 + dx, y, z0 + dz, crane);
            // cab and slewing ring
            w.fill(x0 - 1, CT + 1, z0 - 1, x0 + 2, CT + 1, z0 + 2, crane);
            w.fill(x0, CT + 2, z0 + 1, x0 + 1, CT + 3, z0 + 2, cab);
            // jib and counter-jib
            for (int x = x0 - 12; x <= x0 + 32; x++) {
                w.set(x, CT + 2, z0, crane);
                if (x < x0 - 6) w.set(x, CT + 1, z0, weight);
            }
            for (int y = CT + 3; y <= CT + 6; y++) w.set(x0, y, z0, crane);
            w.set(x0, CT + 7, z0, beacon);
            w.set(x0 + 32, CT + 3, z0, beacon);
            w.set(x0 - 12, CT + 3, z0, beacon);
            // the hook, and a load hanging over nothing
            int hx = x0 + 10 + Hash.range(Hash.hash(s, x0, z0, 51), 20);
            int bottom = fl(FLOORS) + 6 + Hash.range(Hash.hash(s, x0, z0, 52), 14);
            for (int y = bottom + 2; y <= CT + 1; y++) w.set(hx, y, z0, chain);
            w.fill(hx, bottom, z0, hx + 1, bottom + 1, z0 + 1, pallet);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The whistle, then whatever was dropped. */
    static final class Whistle implements LevelBehavior {
        /** per player: {next whistle, drop at} */
        private static final Map<UUID, long[]> STATE = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            long[] st = STATE.computeIfAbsent(p.getUUID(), k -> new long[]{now + 1200 + level.random.nextInt(1200), 0});
            if (st[1] == 0 && now >= st[0]) {
                st[1] = now + 60;
                level.playSound(null, p.getX(), p.getY() + 20, p.getZ(), ModSounds.get("event.construction_site_whistle"),
                        SoundSource.AMBIENT, 2.5f, 1f);
                if (level.canSeeSky(p.blockPosition()))
                    p.displayClientMessage(Component.literal("A whistle, from somewhere above you.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return;
            }
            if (st[1] == 0 || now < st[1]) return;
            st[1] = 0;
            st[0] = now + 1600 + level.random.nextInt(1800);
            BlockPos at = p.blockPosition();
            if (!level.canSeeSky(at) || p.isSpectator()) {
                level.playSound(null, at.offset(level.random.nextInt(20) - 10, 6, level.random.nextInt(20) - 10),
                        ModSounds.get("event.construction_site_crash"), SoundSource.AMBIENT, 1.4f, 0.9f);
                return;
            }
            BlockState rubble = ModBlocks.state("cons_rubble");
            for (int i = 0; i < 4; i++) {
                BlockPos from = at.offset(level.random.nextInt(3) - 1, 18 + i * 2, level.random.nextInt(3) - 1);
                if (!level.getBlockState(from).isAir() || from.getY() >= level.getMaxBuildHeight() - 1) continue;
                FallingBlockEntity fb = FallingBlockEntity.fall(level, from, rubble);
                fb.setHurtsEntities(0.6f, 10);
                fb.dropItem = false;
            }
            level.playSound(null, at, ModSounds.get("event.construction_site_crash"), SoundSource.AMBIENT, 1.6f, 1f);
            Sanity.add(p, -3f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.4f));
        }
    }
}
