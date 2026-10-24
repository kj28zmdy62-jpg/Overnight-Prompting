package com.noclip.levels;

import com.noclip.entity.InvertedWalker;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Inverted Lobby. Level 0 turned over: you walk on the ceiling tiles, the
 * fluorescent tubes are set into the floor and blaze upward, and the damp
 * carpet hangs overhead like a low brown sky. The office chairs are up there.
 * So are the people, walking upside down, following you while you look ahead.
 */
public final class InvertedLobby {
    static final int F = 8, CEIL = F + 7;
    static final String ID = "inverted_lobby";

    private InvertedLobby() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Inverted Lobby").order(4010)
                .tagline("Keep looking up. They walk where you can't.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Upstairs())
                .visuals(new Visuals().tint(1.06f, 1.0f, 0.82f).brightness(1.12f).saturation(0.86f).contrast(1.08f)
                        .grain(0.07f).aberration(0.0016f).vignette(0.55f).warp(0.035f).bloom(0.6f).fog(4f, 44f).fogColor(0x3A3418))
                .sanityDrain(0.04f)
                .spawnY(F + 1, F + 1)
                .exit(ExitType.FALL, "level_0", 4).exit(ExitType.FALL, "level_fun", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_1", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> c = new HashMap<>();

        BlockState b(String n) {
            return c.computeIfAbsent(n, ModBlocks::state);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            LobbyLayout lay = new LobbyLayout(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, b("void_filler"));
            w.layer(CEIL + 1, b("subfloor"));
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, CEIL, z, b("inverted_carpet_sky"));
                    boolean lampSpot = Math.floorMod(x, 4) <= 1 && Math.floorMod(z, 4) == 2;
                    BlockState floor;
                    if (lampSpot) {
                        float r = Hash.unit(s, Math.floorDiv(x, 4), Math.floorDiv(z, 4), 11);
                        floor = r < 0.1f ? b("inverted_floor_light_flicker") : r < 0.18f ? b("inverted_floor_tile_cracked") : b("inverted_floor_light");
                    } else {
                        floor = Hash.chance(s, x, z, 3, 0.05f) ? b("inverted_floor_tile_cracked") : b("inverted_floor_tile");
                    }
                    w.set(x, F, z, floor);
                    if (lay.wall(x, z)) {
                        for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, y == CEIL - 1 ? b("inverted_baseboard") : b("inverted_wallpaper"));
                    } else if (Hash.chance(s, x, z, 4, 0.012f)) {
                        w.set(x, CEIL - 1, z, b("inverted_office_chair")
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x, z, 5, 4))));
                    }
                }
            }
            // a missing tile in the floor: through it, far below, is the Lobby the right way up
            lay.g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (lay.g.unit(i, k, 70) > 0.025f) return;
                int cx = x0 + wd / 2, cz = z0 + dp / 2;
                for (int dx = 0; dx < 2; dx++)
                    for (int dz = 0; dz < 2; dz++) {
                        w.set(cx + dx, F, cz + dz, ModBlocks.AIR);
                        w.fill(cx + dx, 2, cz + dz, cx + dx, F - 1, cz + dz, ModBlocks.AIR);
                        w.set(cx + dx, 1, cz + dz, b("inverted_missing_tile"));
                    }
            });
        }
    }

    /** Keeps a few people walking on the carpet above each player, out of sight when they arrive. */
    static final class Upstairs implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || p.tickCount % 40 != 0) return;
            List<InvertedWalker> near = level.getEntitiesOfClass(InvertedWalker.class, p.getBoundingBox().inflate(48));
            int want = 1 + Math.min(3, p.tickCount / (20 * 90));
            if (near.size() >= want || p.getRandom().nextFloat() > 0.5f) return;
            var r = p.getRandom();
            for (int attempt = 0; attempt < 12; attempt++) {
                double ang = r.nextDouble() * Math.PI * 2, d = 12 + r.nextDouble() * 12;
                BlockPos at = BlockPos.containing(p.getX() + Math.cos(ang) * d, CEIL - 1, p.getZ() + Math.sin(ang) * d);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).isAir()) continue;
                Vec3 pos = new Vec3(at.getX() + 0.5, CEIL - 1.95, at.getZ() + 0.5);
                if (DeepEndUtil.seen(p, pos.add(0, 1, 0), 0.3)) continue;
                InvertedWalker w = InvertedWalker.TYPE.create(level);
                if (w == null) return;
                w.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360f, 0f);
                level.addFreshEntity(w);
                return;
            }
        }
    }
}
