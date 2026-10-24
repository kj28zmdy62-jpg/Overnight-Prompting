package com.noclip.levels;

import com.noclip.block.LockerOddBlock;
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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Locker Rooms. Long changing rooms of blue steel lockers and wet benches, each with a
 * tiled shower room behind it. The showers are always running, steam rolling off the
 * tiles, but only where you are not: walk up to one and it shuts off, and the one
 * behind you starts. The lockers knock when you look away. One locker in a row knocks
 * more than the rest. Its number is 1313. Open it.
 */
public final class LockerRooms {
    public static final String ID = "locker_rooms";
    static final int FLOOR = 10, CEIL = 14, P = 16;

    private LockerRooms() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Locker Rooms").order(1200)
                .tagline("The showers are running. Nobody is in them.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Steam())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.04f).saturation(0.75f).contrast(1.02f).brightness(1.06f)
                        .grain(0.06f).aberration(0.0014f).vignette(0.55f).bloom(0.75f).warp(0.03f).fog(2f, 26f).fogColor(0xB8C0C4))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(18, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 1, 0.06f).dist(16, 30).weight(1))
                .exit(ExitType.NOCLIP, "restrooms", 2).exit(ExitType.NOCLIP, "level_37", 1).exit(ExitType.NOCLIP, "school", 1)
                .exit(ExitType.DOOR, "restrooms", 2).exit(ExitType.DOOR, "school", 2).exit(ExitType.DOOR, "water_park", 1)
                .exit(ExitType.DOOR, "level_37", 1)
                .exit(ExitType.WATER, "level_37", 2).exit(ExitType.WATER, "water_park", 1).exit(ExitType.WATER, "sewers", 1)
                .build();
    }

    static final String[] TOPS = {"0412", "0413", "0711", "2201", "0999"};

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("lock_floor");
        final BlockState wall = ModBlocks.state("lock_wall");
        final BlockState band = ModBlocks.state("lock_wall_band");
        final BlockState mold = ModBlocks.state("lock_wall_mold");
        final BlockState ceiling = ModBlocks.state("lock_ceiling");
        final BlockState light = ModBlocks.state("lock_light");
        final BlockState lightBad = ModBlocks.state("lock_light_bad");
        final BlockState locker = ModBlocks.state("lock_locker");
        final BlockState odd = ModBlocks.state("lock_locker_odd");
        final BlockState bench = ModBlocks.state("lock_bench");
        final BlockState benchTowel = ModBlocks.state("lock_bench_towel");
        final BlockState shower = ModBlocks.state("lock_shower");
        final BlockState drain = ModBlocks.state("lock_drain_tile");
        final BlockState drainOpen = ModBlocks.state("lock_drain_open");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            int y = FLOOR + 1;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, P), lz = Math.floorMod(z, P);
                    int seg = Math.floorDiv(x, P), row = Math.floorDiv(z, P);
                    boolean gap = lx >= 14;
                    if (lz == 10 || lz == 15) {
                        if (lz == 10 && (lx == 6 || lx == 7)) continue;
                        boolean showers = lz == 15;
                        w.set(x, y, z, band);
                        w.set(x, y + 1, z, showers && Hash.chance(s, x, z, 0x30, 0.3f) ? mold : wall);
                        w.set(x, y + 2, z, showers && Hash.chance(s, x, z, 0x31, 0.4f) ? mold : wall);
                        continue;
                    }
                    if ((lz == 0 || lz == 4 || lz == 5 || lz == 9) && !(gap && lz != 0) && !(lz == 9 && (lx == 6 || lx == 7))) {
                        Direction face = (lz == 0 || lz == 5) ? Direction.SOUTH : Direction.NORTH;
                        w.set(x, y, z, locker.setValue(HorizontalDirectionalBlock.FACING, face));
                        boolean isOdd = lx == Hash.range(s, seg, row * 4 + lz, 0x0DD, 14) && Hash.chance(s, seg, row * 4 + lz, 0x0DE, 0.35f);
                        BlockState top = isOdd ? odd : ModBlocks.state("lock_locker_top_" + TOPS[Hash.range(s, x, z, 0x70, TOPS.length)]);
                        w.set(x, y + 1, z, top.setValue(HorizontalDirectionalBlock.FACING, face));
                        continue;
                    }
                    if ((lz == 2 || lz == 7) && lx >= 1 && lx <= 12)
                        w.set(x, y, z, (Hash.chance(s, x, z, 0x7A, 0.08f) ? benchTowel : bench)
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    if (lz >= 11 && lz <= 14) {
                        if (lz == 12 && lx % 4 == 2) w.set(x, FLOOR, z, Hash.chance(s, x, z, 0xD2A, 0.03f) ? drainOpen : drain);
                        if (lz == 14 && lx % 2 == 0) w.set(x, y + 2, z, shower.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        if (Hash.chance(s, x, z, 0x9D, 0.25f) && w.get(x, y, z).isAir()) w.set(x, y, z, puddle);
                    }
                    boolean lamp = (lz == 2 || lz == 7 || lz == 12) && lx % 4 == 1;
                    if (lamp) {
                        boolean dim = lz == 12 || Hash.chance(s, seg, row, 0xB1, 0.15f);
                        w.set(x, CEIL, z, dim && Hash.chance(s, x, z, 0xB2, 0.5f) ? lightBad : light);
                    }
                }
        }
    }

    // ---------------------------------------------------------------- behaviour

    static final Map<UUID, List<BlockPos>> HEADS = new HashMap<>();
    static final Map<UUID, Long> NEXT_BANG = new HashMap<>();

    public static void onOdd(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().location().getPath().equals(ID)) return;
        level.playSound(null, pos, ModSounds.get("event.locker_rooms_open"), SoundSource.BLOCKS, 1f, 0.9f);
        EmptyBuildingKit.narrate(p, "Locker 1313 swings open. Behind the hanging coats, the locker has no back.");
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.7f));
        Travel.exit(p, ExitType.DOOR, pos);
    }

    static final class Steam implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            BlockPos c = p.blockPosition();
            if (p.tickCount % 40 == 0) {
                List<BlockPos> heads = new ArrayList<>();
                for (int dx = -18; dx <= 18; dx++)
                    for (int dz = -18; dz <= 18; dz++) {
                        BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 3, c.getZ() + dz);
                        if (level.getBlockState(bp).is(ModBlocks.get("lock_shower"))) heads.add(bp);
                    }
                HEADS.put(p.getUUID(), heads);
            }
            List<BlockPos> heads = HEADS.get(p.getUUID());
            if (heads != null && p.tickCount % 4 == 0) {
                int running = 0;
                for (BlockPos h : heads) {
                    boolean someone = false;
                    for (Player pl : level.players()) if (pl.distanceToSqr(Vec3.atCenterOf(h)) < 25) someone = true;
                    if (someone) continue;
                    double d = p.distanceToSqr(Vec3.atCenterOf(h));
                    if (d < 14 * 14) running++;
                    level.sendParticles(ParticleTypes.FALLING_WATER, h.getX() + 0.5, h.getY() + 0.2, h.getZ() + 0.5, 4, 0.18, 0.05, 0.18, 0);
                    if (p.tickCount % 8 == 0)
                        level.sendParticles(ParticleTypes.CLOUD, h.getX() + 0.5, FLOOR + 1.2, h.getZ() + 0.5, 1, 0.4, 0.2, 0.4, 0.01);
                    if ((p.tickCount + h.getX() * 7) % 112 == 0 && d < 16 * 16)
                        level.playSound(null, h, ModSounds.get("event.locker_rooms_shower"), SoundSource.AMBIENT, 0.8f, 0.95f + (h.getX() & 3) * 0.03f);
                }
                if (running >= 3 && p.tickCount % 20 == 0) Sanity.add(p, -0.15f);
            }
            Long nb = NEXT_BANG.get(p.getUUID());
            if (nb == null || now >= nb) {
                NEXT_BANG.put(p.getUUID(), now + 20 * (3 + p.getRandom().nextInt(6)));
                List<BlockPos> odd = new ArrayList<>(), any = new ArrayList<>();
                for (int dx = -16; dx <= 16; dx++)
                    for (int dz = -16; dz <= 16; dz++) {
                        BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 2, c.getZ() + dz);
                        BlockState st = level.getBlockState(bp);
                        if (st.getBlock() instanceof LockerOddBlock) odd.add(bp);
                        else if (Math.abs(dx) < 9 && Math.abs(dz) < 9 && st.is(ModBlocks.get("lock_locker_top_0412"))) any.add(bp);
                    }
                BlockPos pick = null;
                if (!odd.isEmpty() && p.getRandom().nextFloat() < 0.6f) pick = odd.get(p.getRandom().nextInt(odd.size()));
                else if (!any.isEmpty()) pick = any.get(p.getRandom().nextInt(any.size()));
                if (pick != null && !EmptyBuildingKit.seen(p, Vec3.atCenterOf(pick), 0.3))
                    level.playSound(null, pick, ModSounds.get("event.locker_rooms_bang"), SoundSource.BLOCKS, 1.1f, 0.9f + p.getRandom().nextFloat() * 0.2f);
            }
        }
    }
}
