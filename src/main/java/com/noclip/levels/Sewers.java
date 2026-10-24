package com.noclip.levels;

import com.noclip.block.SludgeBlock;
import com.noclip.entity.SludgeLurker;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sewers. Victorian brick tunnels barely wider than your arms, a channel of
 * sludge down the middle, a crumbling ledge on either side, and almost no light
 * at all. Where the ledge has fallen in you have to wade, and wading is slow.
 * Something lives under the sludge and comes for whoever is in it, or too near
 * the edge.
 */
public final class Sewers {
    static final int FLOOR = 20, CELL = 14;

    private Sewers() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("sewers")
                .number("").title("Sewers").order(2120)
                .tagline("Keep to the ledges. Do not wade unless you have to. Watch for bubbles.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Wading())
                .visuals(new Visuals().darkness(0.85f).tint(0.98f, 1.02f, 0.86f).saturation(0.68f).contrast(1.1f)
                        .brightness(1.1f).grain(0.1f).aberration(0.0016f).vignette(0.72f).fog(2f, 24f).fogColor(0x0C0E08)
                        .bloom(0.6f))
                .sanityDrain(0.045f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.45f).dark().dist(10, 22).weight(3))
                .exit(ExitType.NOCLIP, "storm_drains", 2).exit(ExitType.NOCLIP, "level_2", 1)
                .exit(ExitType.ASCEND, "level_9", 2).exit(ExitType.ASCEND, "rain_city", 2).exit(ExitType.ASCEND, "level_11", 1)
                .exit(ExitType.DOOR, "storm_drains", 2).exit(ExitType.DOOR, "water_treatment", 1).exit(ExitType.DOOR, "mine_shaft", 1)
                .exit(ExitType.WATER, "level_7", 2).exit(ExitType.WATER, "water_treatment", 1).exit(ExitType.WATER, "storm_drains", 1)
                .build();
    }

    static boolean segZ(long s, int i, int k) {
        return Hash.chance(s, i, k, 11, 0.58f);
    }

    static boolean segX(long s, int i, int k) {
        return Hash.chance(s, i, k, 12, 0.58f);
    }

    static boolean centre(long s, int x, int z) {
        boolean lx = Math.floorMod(x, CELL) == 0, lz = Math.floorMod(z, CELL) == 0;
        int i = Math.floorDiv(x, CELL), k = Math.floorDiv(z, CELL);
        if (lx && lz) return segZ(s, i, k) || segZ(s, i, k - 1) || segX(s, i, k) || segX(s, i - 1, k);
        if (lx) return segZ(s, i, k);
        if (lz) return segX(s, i, k);
        return false;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState brick = ModBlocks.state("sw_brick");
        final BlockState slime = ModBlocks.state("sw_brick_slime");
        final BlockState darkBrick = ModBlocks.state("sw_brick_dark");
        final BlockState ledge = ModBlocks.state("sw_ledge");
        final BlockState sludge = ModBlocks.state("sw_sludge");
        final BlockState bed = ModBlocks.state("sw_bed");
        final BlockState plank = ModBlocks.state("sw_plank");
        final BlockState gate = ModBlocks.state("sw_gate");
        final BlockState manhole = ModBlocks.state("sw_manhole");
        final BlockState rungs = ModBlocks.state("sw_rungs");
        final BlockState outfall = ModBlocks.state("sw_outfall");
        final BlockState lamp = ModBlocks.state("sw_lamp");
        final BlockState[] scrawls = {ModBlocks.state("sw_scrawl_below"), ModBlocks.state("sw_scrawl_hands")};
        final BlockState sump = ModBlocks.state("sw_sump");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(0, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    for (int y = 1; y <= FLOOR + 8; y++) w.set(x, y, z, wallAt(s, x, y, z));
                    w.fill(x, FLOOR + 9, z, x, FLOOR + 11, z, filler);
                    carve(w, s, x, z);
                }
            }
            features(w, s);
        }

        BlockState wallAt(long s, int x, int y, int z) {
            if (y <= FLOOR + 1) return slime;
            float n = Hash.fbm(s ^ 0x5E3, x / 8.0, (z + y * 2) / 8.0, 2);
            return n > 0.64f ? slime : n < 0.3f ? darkBrick : brick;
        }

        /** Distance class from the nearest open drift centre line: 0..1 channel, 2 ledge, else wall. */
        int dist(long s, int x, int z) {
            int best = 9;
            for (int d = -2; d <= 2; d++) {
                for (int e = -2; e <= 2; e++) {
                    if (centre(s, x + d, z + e)) best = Math.min(best, Math.max(Math.abs(d), Math.abs(e)));
                }
            }
            return best;
        }

        void carve(ChunkWriter w, long s, int x, int z) {
            int d = dist(s, x, z);
            if (d > 2) return;
            boolean node = Math.abs(Math.floorMod(x + 7, CELL) - 7) <= 3 && Math.abs(Math.floorMod(z + 7, CELL) - 7) <= 3
                    && centre(s, Math.floorDiv(x + 7, CELL) * CELL, Math.floorDiv(z + 7, CELL) * CELL);
            int top = node ? FLOOR + 5 : d <= 1 ? FLOOR + 4 : FLOOR + 3;
            if (d <= 1) {
                w.set(x, FLOOR - 1, z, bed);
                w.set(x, FLOOR, z, sludge);
            } else {
                boolean fallen = Hash.fbm(s ^ 0xFA11, x / 4.0, z / 4.0, 2) > 0.7f;
                if (fallen) {
                    w.set(x, FLOOR - 1, z, bed);
                    w.set(x, FLOOR, z, sludge);
                } else {
                    w.set(x, FLOOR, z, ledge);
                }
            }
            for (int y = FLOOR + 1; y <= top; y++) w.set(x, y, z, ModBlocks.AIR);
        }

        static BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void features(ChunkWriter w, long s) {
            int i0 = Math.floorDiv(w.minX, CELL) - 1, i1 = Math.floorDiv(w.maxX, CELL) + 1;
            int k0 = Math.floorDiv(w.minZ, CELL) - 1, k1 = Math.floorDiv(w.maxZ, CELL) + 1;
            for (int i = i0; i <= i1; i++) {
                for (int k = k0; k <= k1; k++) {
                    int x0 = i * CELL, z0 = k * CELL;
                    long h = Hash.hash(s, i, k, 20);
                    if (segX(s, i, k)) drift(w, s, h, x0, z0, true);
                    if (segZ(s, i, k)) drift(w, s, h >>> 3, x0, z0, false);
                    if (!centre(s, x0, z0)) continue;
                    float r = Hash.unit(h >>> 30);
                    if (r < 0.18f) {
                        // rungs up a narrow shaft to a manhole with daylight in its holes
                        int mx = x0 + 3, mz = z0;
                        for (int y = FLOOR + 1; y <= FLOOR + 7; y++) {
                            w.set(mx, y, mz, rungs.setValue(LadderBlock.FACING, Direction.WEST));
                            w.set(mx + 1, y, mz, brick);
                        }
                        w.set(mx, FLOOR + 8, mz, manhole);
                    } else if (r < 0.35f) {
                        w.set(x0, FLOOR - 1, z0, sump);
                        w.set(x0, FLOOR, z0, sludge);
                    }
                    deadEnd(w, s, i, k, x0, z0);
                }
            }
        }

        /** Planks over the channel, a rare lamp, outfalls and scrawls on the walls. */
        void drift(ChunkWriter w, long s, long h, int x0, int z0, boolean alongX) {
            int bridgeAt = 4 + Hash.range(h, 6);
            for (int t = 4; t <= CELL - 4; t++) {
                int cx = alongX ? x0 + t : x0, cz = alongX ? z0 : z0 + t;
                if (t == bridgeAt && Hash.unit(h >>> 8) < 0.55f) {
                    for (int a = -1; a <= 1; a++)
                        w.set(alongX ? cx : cx + a, FLOOR, alongX ? cz + a : cz, face(plank, alongX ? Direction.EAST : Direction.NORTH));
                }
                long hh = Hash.hash(s, cx, cz, 21);
                float r = Hash.unit(hh);
                int side = (hh & 1) == 0 ? 1 : -1;
                int wx = alongX ? cx : cx + side * 3, wz = alongX ? cz + side * 3 : cz;
                Direction toWall = alongX ? (side > 0 ? Direction.SOUTH : Direction.NORTH) : (side > 0 ? Direction.EAST : Direction.WEST);
                if (r < 0.035f) w.set(wx, FLOOR + 2, wz, outfall);
                else if (r < 0.045f) w.set(wx, FLOOR + 2, wz, scrawls[(int) ((hh >>> 8) & 1)]);
                else if (r < 0.06f) w.set(alongX ? cx : cx + side * 2, FLOOR + 3, alongX ? cz + side * 2 : cz, face(lamp, toWall));
                else if (r < 0.065f) w.set(alongX ? cx : cx + side * 2, FLOOR + 1, alongX ? cz + side * 2 : cz, bottle);
            }
        }

        void deadEnd(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int open = 0;
            Direction only = null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (centre(s, x0 + d.getStepX(), z0 + d.getStepZ())) {
                    open++;
                    only = d;
                }
            }
            if (open != 1 || !Hash.chance(s, i, k, 22, 0.3f)) return;
            Direction back = only.getOpposite();
            for (int a = -1; a <= 1; a++) {
                int x = x0 + back.getStepX() * 3 + (back.getAxis() == Direction.Axis.Z ? a : 0);
                int z = z0 + back.getStepZ() * 3 + (back.getAxis() == Direction.Axis.X ? a : 0);
                for (int y = FLOOR + 1; y <= FLOOR + 3; y++) w.set(x, y, z, gate);
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Wading is slow and loud; hands find ankles; the lurker follows the channels. */
    static final class Wading implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            boolean in = level.getBlockState(p.blockPosition()).getBlock() instanceof SludgeBlock;
            if (in && p.tickCount % 12 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.0005)
                level.playSound(null, p.blockPosition(), ModSounds.get("event.sewers_wade"), SoundSource.PLAYERS, 0.8f,
                        0.8f + p.getRandom().nextFloat() * 0.3f);
            if (in && !p.isCreative() && p.tickCount % 30 == 0 && p.getRandom().nextFloat() < 0.22f) {
                p.hurt(level.damageSources().generic(), 2f);
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                level.playSound(null, p.blockPosition(), ModSounds.get("event.sewers_grab"), SoundSource.HOSTILE, 1.1f, 1f);
                Sanity.add(p, -3f);
                p.displayClientMessage(Component.literal("Something under the sludge closes around your ankle.")
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
            }
            if ((p.tickCount + p.getId()) % 200 == 0 && !p.isCreative()) lurk(p, level);
        }

        private static void lurk(ServerPlayer p, ServerLevel level) {
            if (!level.getEntities(SludgeLurker.TYPE, p.getBoundingBox().inflate(40), e -> true).isEmpty()) return;
            for (int attempt = 0; attempt < 24; attempt++) {
                double ang = level.random.nextDouble() * Math.PI * 2, dist = 12 + level.random.nextDouble() * 10;
                BlockPos c = BlockPos.containing(p.getX() + Math.cos(ang) * dist, FLOOR, p.getZ() + Math.sin(ang) * dist);
                if (!level.isLoaded(c) || !(level.getBlockState(c).getBlock() instanceof SludgeBlock)) continue;
                if (level.clip(new ClipContext(p.getEyePosition(), Vec3.atCenterOf(c), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS
                        && p.getViewVector(1f).dot(Vec3.atCenterOf(c).subtract(p.getEyePosition()).normalize()) > 0.3) continue;
                SludgeLurker e = SludgeLurker.TYPE.create(level);
                if (e == null) return;
                e.moveTo(c.getX() + 0.5, FLOOR - 0.7, c.getZ() + 0.5, 0f, 0f);
                e.finalizeSpawn(level, level.getCurrentDifficultyAt(c), MobSpawnType.EVENT, null);
                e.anchor(FLOOR + 0.8);
                level.addFreshEntity(e);
                return;
            }
        }
    }
}
