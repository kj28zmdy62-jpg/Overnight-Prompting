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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Flooded Lobby. Level 0 under a foot of still, yellowish water. The
 * ceiling lights glare off the surface. In some rooms a fixture has fallen in
 * and is still switched on, and the whole room's water is live in pulses: cross
 * when it goes dark. Something long moves under the surface, and brushes past.
 */
public final class FloodedLobby {
    static final int F = 10, W = F + 1, CEIL = 14;
    static final String ID = "flooded_lobby";

    private FloodedLobby() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Flooded Lobby").order(4080)
                .tagline("Mind the lights in the water.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new LiveWater())
                .visuals(new Visuals().tint(1.04f, 1.02f, 0.82f).brightness(1.15f).saturation(0.84f).contrast(1.05f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.5f).bloom(0.65f).fog(4f, 40f).fogColor(0x8A8448))
                .sanityDrain(0.035f)
                .spawnY(W, W)
                .waterSpawn()
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(14, 28).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.WATER, "level_37", 2).exit(ExitType.WATER, "level_7", 2)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_37", 1)
                .build();
    }

    /** Rooms with a live fixture lying in the water. */
    static boolean liveRoom(LobbyLayout lay, int x, int z) {
        return lay.room(x, z, 500) < 0.14f;
    }

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> c = new HashMap<>();
        final BlockState water = Blocks.WATER.defaultBlockState();

        BlockState b(String n) {
            return c.computeIfAbsent(n, ModBlocks::state);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            LobbyLayout lay = new LobbyLayout(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, b("subfloor"));
            w.layer(CEIL + 1, b("void_filler"));
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, F, z, b("flooded_carpet"));
                    if (lay.wall(x, z)) {
                        w.set(x, W, z, b("flooded_baseboard"));
                        w.set(x, W + 1, z, b("flooded_wallpaper_tide"));
                        w.set(x, W + 2, z, b("flooded_wallpaper"));
                        w.set(x, CEIL, z, b("flooded_ceiling_tile"));
                        continue;
                    }
                    w.set(x, W, z, water);
                    boolean lamp = Math.floorMod(x, 4) <= 1 && Math.floorMod(z, 4) == 2;
                    boolean live = liveRoom(lay, x, z);
                    BlockState ceil;
                    if (lamp) {
                        float r = Hash.unit(s, x >> 2, z >> 2, 9);
                        ceil = live ? b("dead_fluorescent_light") : r < 0.1f ? b("flickering_fluorescent_light")
                                : r < 0.2f ? b("dead_fluorescent_light") : b("fluorescent_light");
                    } else {
                        ceil = Hash.chance(s, x, z, 3, 0.01f) ? b("missing_ceiling_tile") : b("flooded_ceiling_tile");
                    }
                    w.set(x, CEIL, z, ceil);
                    if (Hash.chance(s, x, z, 4, 0.012f)) w.set(x, W + 1, z, b("flooded_floating_tile")
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x, z, 5, 4))));
                }
            }
            lay.g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                float r = lay.g.unit(i, k, 500);
                int cx = x0 + wd / 2, cz = z0 + dp / 2;
                if (r < 0.14f) {
                    // the fixture came down from right here, and it is still on
                    w.set(cx, F, cz, b("flooded_fallen_light"));
                    w.set(cx + 1, F, cz, b("flooded_fallen_light"));
                    w.set(cx, CEIL, cz, b("missing_ceiling_tile"));
                    w.set(cx + 1, CEIL, cz, b("missing_ceiling_tile"));
                } else if (r < 0.165f) {
                    // a hole in the floor; the water goes down a long way here
                    for (int dx = 0; dx < 2; dx++)
                        for (int dz = 0; dz < 2; dz++) {
                            w.fill(cx + dx, F - 7, cz + dz, cx + dx, F, cz + dz, water);
                            w.set(cx + dx, F - 8, cz + dz, b("flooded_drain"));
                        }
                } else if (r < 0.2f) {
                    w.set(cx, W, cz, b("office_chair").setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, i, k, 4))));
                }
            });
        }
    }

    /**
     * Fallen fixtures switch on and off in a slow rhythm; while on, the water
     * around them is live. Under the surface, something swims up to you.
     */
    static final class LiveWater implements LevelBehavior {
        record Trail(Vec3 from, int start) {}

        private final Map<UUID, Trail> trails = new HashMap<>();

        static boolean on(BlockPos p, long time) {
            return Math.floorMod(time / 20 + Hash.range(p.asLong(), 7, 0, 5), 8) < 5;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            long time = level.getGameTime();
            if (p.tickCount % 5 == 0) {
                List<BlockPos> live = new ArrayList<>();
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
                for (int dx = -12; dx <= 12; dx++)
                    for (int dz = -12; dz <= 12; dz++) {
                        m.set(p.getBlockX() + dx, F, p.getBlockZ() + dz);
                        BlockState st = level.getBlockState(m);
                        if (!st.is(ModBlocks.get("flooded_fallen_light"))) continue;
                        boolean lit = on(m, time);
                        if (st.getValue(BlockStateProperties.LIT) != lit) level.setBlock(m, st.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS);
                        if (lit) live.add(m.immutable());
                    }
                for (BlockPos l : live) {
                    if (p.getRandom().nextInt(3) == 0)
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, l.getX() + p.getRandom().nextDouble() * 2, W + 0.9,
                                l.getZ() + p.getRandom().nextDouble(), 4, 0.6, 0.05, 0.6, 0.2);
                    if (p.isInWater() && !p.isCreative() && l.distToCenterSqr(p.getX(), F + 0.5, p.getZ()) < 4.5 * 4.5 && p.tickCount % 10 == 0) {
                        p.hurt(level.damageSources().lightningBolt(), 2.5f);
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.flooded_lobby_zap"), SoundSource.BLOCKS, 1f, 0.9f + p.getRandom().nextFloat() * 0.2f);
                        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.35f));
                        Sanity.add(p, -2f);
                    }
                }
            }
            // something under the surface
            UUID id = p.getUUID();
            Trail t = trails.get(id);
            if (t == null) {
                if (p.tickCount % 200 == 0 && p.getRandom().nextFloat() < 0.4f && p.isInWater()) {
                    double ang = p.getRandom().nextDouble() * Math.PI * 2;
                    Vec3 from = new Vec3(p.getX() + Math.cos(ang) * 11, W + 0.85, p.getZ() + Math.sin(ang) * 11);
                    trails.put(id, new Trail(from, p.tickCount));
                    level.playSound(null, BlockPos.containing(from), ModSounds.get("event.flooded_lobby_brush"), SoundSource.AMBIENT, 0.6f, 0.6f);
                }
                return;
            }
            double k = (p.tickCount - t.start) / 60.0;
            Vec3 at = t.from.lerp(new Vec3(p.getX(), W + 0.85, p.getZ()), Math.min(1, k));
            if (level.getBlockState(BlockPos.containing(at.x, W, at.z)).is(Blocks.WATER)) {
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 6, 0.3, 0.02, 0.3, 0.1);
                level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y - 0.4, at.z, 3, 0.2, 0.1, 0.2, 0.02);
            }
            if (k >= 1) {
                trails.remove(id);
                if (p.isInWater() && !p.isCreative()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.flooded_lobby_brush"), SoundSource.HOSTILE, 1f, 0.8f);
                    p.displayClientMessage(Component.literal("Something long and cold slides past your legs.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    Sanity.add(p, -5f);
                }
            }
        }
    }
}
