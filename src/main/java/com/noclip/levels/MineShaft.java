package com.noclip.levels;

import com.noclip.block.MineLanternBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mine Shaft. Timbered drifts through wet rock, rails down the main lines, ore
 * carts left where they stopped. Lanterns hang from the cap timbers, and the ones
 * behind you go out one by one. You can strike a match and relight them. In the
 * dark, something taps on the rock, and comes closer the longer you stay there.
 */
public final class MineShaft {
    static final int FLOOR = 40, CELL = 10, BOTTOM = 6;

    private MineShaft() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("mine_shaft")
                .number("").title("Mine Shaft").order(2070)
                .tagline("The lanterns behind you go out. Relight them, or keep going.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Lanterns())
                .visuals(new Visuals().darkness(0.6f).tint(1.1f, 0.97f, 0.84f).saturation(0.8f).contrast(1.1f)
                        .brightness(1.12f).grain(0.1f).aberration(0.0015f).vignette(0.7f).fog(2f, 30f).fogColor(0x0C0A08)
                        .bloom(0.7f))
                .sanityDrain(0.04f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 3, 0.55f).dark().dist(10, 24).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.25f).dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_8", 2).exit(ExitType.NOCLIP, "level_6", 1)
                .exit(ExitType.DESCEND, "level_8", 3).exit(ExitType.DESCEND, "sewers", 1)
                .exit(ExitType.FALL, "level_8", 2).exit(ExitType.FALL, "the_abyss", 1)
                .exit(ExitType.DOOR, "level_10", 2).exit(ExitType.DOOR, "static_fields", 1).exit(ExitType.DOOR, "container_yard", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    /** Drift segment on line x=10i between z=10k..10k+10. */
    static boolean segZ(long s, int i, int k) {
        return Math.floorMod(i, 4) == 0 || Hash.chance(s, i, k, 11, 0.5f);
    }

    /** Drift segment on line z=10k between x=10i..10i+10. */
    static boolean segX(long s, int i, int k) {
        return Math.floorMod(k, 4) == 0 || Hash.chance(s, i, k, 12, 0.5f);
    }

    /** Is (x, z) on an open drift centre line? */
    static boolean centre(long s, int x, int z) {
        boolean lx = Math.floorMod(x, CELL) == 0, lz = Math.floorMod(z, CELL) == 0;
        int i = Math.floorDiv(x, CELL), k = Math.floorDiv(z, CELL);
        if (lx && lz) return segZ(s, i, k) || segZ(s, i, k - 1) || segX(s, i, k) || segX(s, i - 1, k);
        if (lx) return segZ(s, i, k);
        if (lz) return segX(s, i, k);
        return false;
    }

    static boolean shaft(long s, int i, int k) {
        return Hash.chance(s, i, k, 30, 0.035f);
    }

    static final class Gen implements LevelGenerator {
        final BlockState rock = ModBlocks.state("ms_rock");
        final BlockState dark = ModBlocks.state("ms_rock_dark");
        final BlockState wet = ModBlocks.state("ms_rock_wet");
        final BlockState gold = ModBlocks.state("ms_ore_gold");
        final BlockState copper = ModBlocks.state("ms_ore_copper");
        final BlockState[] chalk = {ModBlocks.state("ms_chalk_arrow"), ModBlocks.state("ms_chalk_x"), ModBlocks.state("ms_chalk_text")};
        final BlockState floor = ModBlocks.state("ms_floor");
        final BlockState post = ModBlocks.state("ms_post");
        final BlockState cap = ModBlocks.state("ms_cap");
        final BlockState rails = ModBlocks.state("ms_rails");
        final BlockState cart = ModBlocks.state("ms_cart");
        final BlockState tnt = ModBlocks.state("ms_tnt");
        final BlockState signDanger = ModBlocks.state("ms_sign_danger");
        final BlockState signLevel = ModBlocks.state("ms_sign_level");
        final BlockState adit = ModBlocks.state("ms_adit_door");
        final BlockState pit = ModBlocks.state("ms_pit");
        final BlockState sump = ModBlocks.state("ms_sump");
        final BlockState ladder = ModBlocks.state("ms_ladder");
        final BlockState lantern = ModBlocks.state("ms_lantern");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(0, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    for (int y = 1; y <= FLOOR + 12; y++) w.set(x, y, z, rockAt(s, x, y, z));
                    w.fill(x, FLOOR + 13, z, x, FLOOR + 15, z, filler);
                    carve(w, s, x, z);
                }
            }
            features(w, s);
        }

        BlockState rockAt(long s, int x, int y, int z) {
            float n = Hash.fbm(s ^ 0x40C, x / 7.0, (z + y * 3) / 7.0, 2);
            float r = Hash.unit(s, x, y, z);
            if (r < 0.006f) return gold;
            if (r < 0.011f) return copper;
            if (n > 0.66f) return wet;
            return n < 0.36f ? dark : rock;
        }

        void carve(ChunkWriter w, long s, int x, int z) {
            boolean near = false;
            for (int dx = -2; dx <= 2 && !near; dx++)
                for (int dz = -2; dz <= 2 && !near; dz++)
                    if ((dx == 0 || dz == 0 || Math.abs(dx) + Math.abs(dz) <= 2) && centre(s, x + dx, z + dz)) near = true;
            float stope = Hash.fbm(s ^ 0x5709E, x / 18.0, z / 18.0, 3);
            boolean big = stope > 0.7f;
            boolean rough = !near && Hash.fbm(s ^ 0x20B, x / 3.0, z / 3.0, 2) > 0.62f && nearRing(s, x, z);
            if (!near && !big && !rough) return;
            int top = big ? FLOOR + 4 + (int) ((stope - 0.7f) * 30) : FLOOR + 4 + (Hash.chance(s, x, z, 3, 0.25f) ? 1 : 0);
            w.set(x, FLOOR, z, floor);
            for (int y = FLOOR + 1; y <= Math.min(top, FLOOR + 11); y++) w.set(x, y, z, ModBlocks.AIR);
        }

        boolean nearRing(long s, int x, int z) {
            for (int d = -3; d <= 3; d += 6) {
                if (centre(s, x + d, z) || centre(s, x, z + d)) return true;
            }
            return false;
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
                    if (segX(s, i, k)) drift(w, s, x0, z0, true, Math.floorMod(k, 4) == 0);
                    if (segZ(s, i, k)) drift(w, s, x0, z0, false, Math.floorMod(i, 4) == 0);
                    deadEnd(w, s, i, k, x0, z0);
                    if (shaft(s, i, k) && centre(s, x0, z0)) shaftAt(w, s, x0, z0);
                }
            }
        }

        /** Timber sets every three metres along a drift, a lantern under every other cap, rails on main lines. */
        void drift(ChunkWriter w, long s, int x0, int z0, boolean alongX, boolean main) {
            for (int t = 2; t <= CELL - 2; t++) {
                int cx = alongX ? x0 + t : x0, cz = alongX ? z0 : z0 + t;
                if (main) w.set(cx, FLOOR + 1, cz, face(rails, alongX ? Direction.EAST : Direction.NORTH));
                if (t % 3 != 2) continue;
                for (int a = -2; a <= 2; a++) {
                    int x = alongX ? cx : cx + a, z = alongX ? cz + a : cz;
                    if (Math.abs(a) == 2) w.fill(x, FLOOR + 1, z, x, FLOOR + 3, z, post);
                    w.set(x, FLOOR + 4, z, cap);
                }
                long h = Hash.hash(s, cx, cz, 13);
                if (t == 5) w.set(cx, FLOOR + 3, cz, lantern);
                float r = Hash.unit(h);
                int sx = alongX ? cx : cx + 3, sz = alongX ? cz + 3 : cz;
                if (r < 0.04f) w.set(sx, FLOOR + 2, sz, chalk[Hash.range(h >>> 8, chalk.length)]);
                else if (r < 0.06f) w.set(sx, FLOOR + 2, sz, signLevel);
                if (main && Hash.unit(h >>> 16) < 0.05f) {
                    w.set(alongX ? cx + 1 : cx, FLOOR + 1, alongX ? cz : cz + 1, face(cart, alongX ? Direction.EAST : Direction.NORTH));
                }
                float q = Hash.unit(h >>> 24);
                int ex = alongX ? cx - 1 : cx + 1, ez = alongX ? cz + 1 : cz - 1;
                if (q < 0.03f) w.set(ex, FLOOR + 1, ez, face(tnt, alongX ? Direction.NORTH : Direction.EAST));
                else if (q < 0.045f) w.set(ex, FLOOR + 1, ez, bottle);
                else if (q < 0.05f) w.set(ex, FLOOR + 1, ez, supply);
            }
        }

        /** Dead-end drifts sometimes finish at an old adit door. */
        void deadEnd(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            if (!centre(s, x0, z0)) return;
            int open = 0;
            Direction only = null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (centre(s, x0 + d.getStepX(), z0 + d.getStepZ())) {
                    open++;
                    only = d;
                }
            }
            if (open != 1 || !Hash.chance(s, i, k, 14, 0.25f)) return;
            Direction back = only.getOpposite();
            int ex = x0 + back.getStepX() * 3, ez = z0 + back.getStepZ() * 3;
            w.set(ex, FLOOR + 1, ez, adit);
            w.set(ex, FLOOR + 2, ez, adit);
            w.set(x0 + back.getStepX() * 2, FLOOR + 3, z0 + back.getStepZ() * 2, signDanger);
        }

        /** A shaft straight down from a crossing: cribbed walls, a ladder, and a long way to the bottom. */
        void shaftAt(ChunkWriter w, long s, int x0, int z0) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    for (int y = BOTTOM; y <= FLOOR; y++) {
                        if (edge) w.set(x, y, z, (y % 4 == 0) ? post : rockAt(s, x, y, z));
                        else w.set(x, y, z, ModBlocks.AIR);
                    }
                    if (!edge) w.set(x, BOTTOM - 1, z, (dx == 0 && dz == -1) ? sump : pit);
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) w.fill(x, FLOOR + 1, z, x, FLOOR + 4, z, post);
                }
            }
            for (int y = BOTTOM; y <= FLOOR; y++) w.set(x0, y, z0 - 1, ladder.setValue(LadderBlock.FACING, Direction.SOUTH));
            for (int y = BOTTOM; y <= FLOOR; y++) w.set(x0, y, z0 - 2, post);
            for (int y = FLOOR - 8; y > BOTTOM; y -= 9) w.set(x0 + 1, y, z0 + 1, lantern);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Lanterns die behind you. The knocking finds you in the dark. */
    static final class Lanterns implements LevelBehavior {
        private final Map<UUID, int[]> dark = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            int t = p.tickCount + p.getId();
            if (t % 30 == 0 && !p.isCreative()) snuff(p, level);
            if (t % 20 == 0) knock(p, level);
        }

        private static void snuff(ServerPlayer p, ServerLevel level) {
            List<BlockPos> behind = new ArrayList<>();
            Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = p.getBlockX() - 18; x <= p.getBlockX() + 18; x++) {
                for (int z = p.getBlockZ() - 18; z <= p.getBlockZ() + 18; z++) {
                    m.set(x, FLOOR + 3, z);
                    BlockState b = level.getBlockState(m);
                    if (!(b.getBlock() instanceof MineLanternBlock) || !b.getValue(MineLanternBlock.LIT)) continue;
                    Vec3 to = Vec3.atCenterOf(m).subtract(p.position()).multiply(1, 0, 1);
                    double d = to.length();
                    if (d < 5 || d > 18 || look.dot(to.normalize()) > -0.1) continue;
                    if (level.clip(new ClipContext(p.getEyePosition(), Vec3.atCenterOf(m), ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS && look.dot(to.normalize()) > -0.4) continue;
                    behind.add(m.immutable());
                }
            }
            if (behind.isEmpty() || level.random.nextFloat() > 0.45f) return;
            BlockPos q = behind.get(level.random.nextInt(behind.size()));
            level.setBlock(q, level.getBlockState(q).setValue(MineLanternBlock.LIT, false), Block.UPDATE_CLIENTS);
            level.playSound(null, q, ModSounds.get("event.mine_shaft_out"), SoundSource.BLOCKS, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.SMOKE, q.getX() + 0.5, q.getY() + 0.6, q.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
        }

        /** The longer you stand in the dark, the closer the tapping. */
        private void knock(ServerPlayer p, ServerLevel level) {
            int[] d = dark.computeIfAbsent(p.getUUID(), k -> new int[]{0});
            int light = level.getBrightness(LightLayer.BLOCK, p.blockPosition());
            d[0] = light < 4 ? d[0] + 1 : Math.max(0, d[0] - 2);
            if (d[0] < 6 || level.random.nextFloat() > 0.35f) return;
            double dist = Math.max(2, 16 - d[0] * 0.6);
            double a = level.random.nextDouble() * Math.PI * 2;
            level.playSound(null, p.getX() + Math.cos(a) * dist, p.getY() + 1, p.getZ() + Math.sin(a) * dist,
                    ModSounds.get("event.mine_shaft_knock"), SoundSource.AMBIENT, 0.9f, 0.9f);
            if (d[0] == 12)
                p.displayClientMessage(Component.literal("Tap. Tap. Tap. Closer.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }
}
