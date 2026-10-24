package com.noclip.levels;

import com.noclip.entity.TankSettler;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Water Treatment Plant. A covered hall of settling tanks thirty metres across,
 * full of still, dark green water. Grated catwalks run over them on rusted legs,
 * and some of the grating is rotten and drops away under your weight. The water
 * burns and pulls you down. Something circles in every tank, just under the
 * surface; from the catwalk you only see it pass.
 */
public final class WaterTreatment {
    static final int FLOOR = 30, ROOF = FLOOR + 18, CELL = 34, DEPTH = 10;

    private WaterTreatment() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("water_treatment")
                .number("").title("Water Treatment Plant").order(2080)
                .tagline("Stay on the catwalks. Watch the rusted grating. Do not look down too long.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Tanks())
                .visuals(new Visuals().darkness(0.3f).tint(0.92f, 1.06f, 0.86f).saturation(0.7f).contrast(1.08f)
                        .brightness(1.15f).grain(0.08f).aberration(0.0014f).vignette(0.55f).fog(3f, 38f).fogColor(0x22301A)
                        .bloom(0.6f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 2)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.1f).seen().dist(20, 34).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 24).weight(2))
                .exit(ExitType.NOCLIP, "sewers", 2).exit(ExitType.NOCLIP, "level_7", 1).exit(ExitType.NOCLIP, "dam", 1)
                .exit(ExitType.WATER, "sewers", 3).exit(ExitType.WATER, "storm_drains", 1).exit(ExitType.WATER, "level_7", 1)
                .exit(ExitType.DOOR, "dam", 2).exit(ExitType.DOOR, "sewers", 1).exit(ExitType.DOOR, "power_plant", 1)
                .build();
    }

    /** 0 = equipment yard, 1 = round clarifier, 2 = rectangular basin. */
    static int tank(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 1);
        return r < 0.56f ? 1 : r < 0.8f ? 2 : 0;
    }

    static boolean inTank(long s, int x, int z) {
        int i = Math.floorDiv(x, CELL), k = Math.floorDiv(z, CELL);
        int t = tank(s, i, k);
        int dx = x - (i * CELL + CELL / 2), dz = z - (k * CELL + CELL / 2);
        if (t == 1) return dx * dx + dz * dz < 13 * 13;
        if (t == 2) return Math.abs(dx) <= 12 && Math.abs(dz) <= 8;
        return false;
    }

    static boolean onRim(long s, int x, int z) {
        if (inTank(s, x, z)) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) if (inTank(s, x + d.getStepX(), z + d.getStepZ())) return true;
        return inTank(s, x + 1, z + 1) || inTank(s, x - 1, z - 1) || inTank(s, x + 1, z - 1) || inTank(s, x - 1, z + 1);
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState deck = ModBlocks.state("wt_deck");
        final BlockState wall = ModBlocks.state("wt_tank_wall");
        final BlockState band = ModBlocks.state("wt_tank_band");
        final BlockState rim = ModBlocks.state("wt_rim");
        final BlockState walk = ModBlocks.state("wt_catwalk");
        final BlockState rotten = ModBlocks.state("wt_catwalk_rust");
        final BlockState rail = ModBlocks.state("wt_railing");
        final BlockState support = ModBlocks.state("wt_support");
        final BlockState[] pipes = {ModBlocks.state("wt_pipe_blue"), ModBlocks.state("wt_pipe_green"), ModBlocks.state("wt_pipe_brown")};
        final BlockState chem = ModBlocks.state("wt_chem");
        final BlockState chemLabel = ModBlocks.state("wt_chem_label");
        final BlockState ring = ModBlocks.state("wt_life_ring");
        final BlockState[] signs = {ModBlocks.state("wt_sign_deep"), ModBlocks.state("wt_sign_swim"), ModBlocks.state("wt_sign_count")};
        final BlockState roof = ModBlocks.state("wt_roof");
        final BlockState lamp = ModBlocks.state("wt_lamp");
        final BlockState outlet = ModBlocks.state("wt_outlet");
        final BlockState door = ModBlocks.state("wt_door");
        final BlockState ladder = ModBlocks.state("wt_ladder");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12);
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.layer(0, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean in = inTank(s, x, z);
                    if (in) {
                        w.fill(x, 1, z, x, FLOOR - DEPTH - 1, z, wall);
                        w.fill(x, FLOOR - DEPTH, z, x, FLOOR - 1, z, water);
                    } else {
                        boolean edge = onRim(s, x, z);
                        w.fill(x, 1, z, x, FLOOR - 4, z, wall);
                        w.fill(x, FLOOR - 3, z, x, FLOOR - 1, z, edge ? band : wall);
                        w.set(x, FLOOR, z, deck);
                        if (edge) w.set(x, FLOOR + 1, z, rim);
                    }
                    w.set(x, ROOF, z, Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4 ? lamp : roof);
                    w.fill(x, ROOF + 1, z, x, ROOF + 3, z, filler);
                    if (Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4 && !in) w.set(x, FLOOR + 7, z, light);
                    if (Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4 && in) w.set(x, FLOOR + 5, z, light);
                }
            }
            int i0 = Math.floorDiv(w.minX, CELL) - 1, i1 = Math.floorDiv(w.maxX, CELL) + 1;
            int k0 = Math.floorDiv(w.minZ, CELL) - 1, k1 = Math.floorDiv(w.maxZ, CELL) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) cell(w, s, i, k);
        }

        void cell(ChunkWriter w, long s, int i, int k) {
            int x0 = i * CELL, z0 = k * CELL, cx = x0 + CELL / 2, cz = z0 + CELL / 2;
            int t = tank(s, i, k);
            long h = Hash.hash(s, i, k, 2);
            // overhead pipe rack along the cell's west edge
            for (int z = z0; z < z0 + CELL; z++) {
                for (int n = 0; n < 3; n++) w.set(x0 + 1, FLOOR + 6 + n, z, pipes[n].setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                if (Math.floorMod(z, 8) == 0) for (int y = FLOOR + 1; y <= FLOOR + 5; y++) w.set(x0 + 1, y, z, support);
            }
            if (t == 0) {
                equipment(w, h, x0, z0, cx, cz);
                return;
            }
            int half = t == 1 ? 13 : 12;
            // a catwalk straight across the tank, with railings, on legs
            bridge(w, s, cx - half - 1, cx + half + 1, cz, true);
            if (t == 1 && Hash.unit(h) < 0.5f) bridge(w, s, cz - half - 1, cz + half + 1, cx, false);
            // ladders out of the water at the four sides
            int hz = t == 1 ? 12 : 8;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int lx = cx + d.getStepX() * (t == 1 ? 12 : (d.getAxis() == Direction.Axis.X ? 12 : 0)) + (d.getAxis() == Direction.Axis.Z ? 4 : 0);
                int lz = cz + d.getStepZ() * hz + (d.getAxis() == Direction.Axis.X ? 4 : 0);
                for (int y = FLOOR - 4; y <= FLOOR + 1; y++)
                    w.set(lx, y, lz, ladder.setValue(LadderBlock.FACING, d.getOpposite()).setValue(LadderBlock.WATERLOGGED, y < FLOOR));
            }
            if (Hash.unit(h >>> 8) < 0.4f) w.set(cx + 4, FLOOR - DEPTH - 1, cz + 4, outlet);
            // life ring and a sign on a leg of the pipe rack
            w.set(x0 + 2, FLOOR + 3, z0 + 8, ring.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            w.set(x0 + 1, FLOOR + 2, z0 + 16, signs[Hash.range(h >>> 16, signs.length)]);
        }

        void bridge(ChunkWriter w, long s, int a0, int a1, int c, boolean alongX) {
            BlockState rl = rail.setValue(alongX ? IronBarsBlock.EAST : IronBarsBlock.NORTH, true)
                    .setValue(alongX ? IronBarsBlock.WEST : IronBarsBlock.SOUTH, true);
            for (int a = a0; a <= a1; a++) {
                for (int o = -1; o <= 2; o++) {
                    int x = alongX ? a : c + o, z = alongX ? c + o : a;
                    boolean walkway = o == 0 || o == 1;
                    w.set(x, FLOOR + 1, z, walkway && Hash.chance(s, x, z, 7, 0.08f) ? rotten : walk);
                    w.set(x, FLOOR + 2, z, walkway ? ModBlocks.AIR : rl);
                    if (Math.floorMod(a, 6) == 0 && !walkway) w.fill(x, FLOOR - DEPTH, z, x, FLOOR, z, support);
                }
            }
        }

        /** Chlorine tanks, a pump house with a door, pipes. */
        void equipment(ChunkWriter w, long h, int x0, int z0, int cx, int cz) {
            for (int n = 0; n < 3; n++) {
                int tx = cx - 8 + n * 6, tz = cz - 6;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        if (Math.abs(dx) + Math.abs(dz) < 2)
                            for (int y = FLOOR + 1; y <= FLOOR + 5; y++)
                                w.set(tx + dx, y, tz + dz, y == FLOOR + 3 && dz == 1 && dx == 0 ? chemLabel : chem);
            }
            int px = cx - 3, pz = cz + 3;
            for (int dx = 0; dx <= 6; dx++) {
                for (int dz = 0; dz <= 6; dz++) {
                    boolean edge = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                    for (int y = FLOOR + 1; y <= FLOOR + 4; y++) w.set(px + dx, y, pz + dz, edge ? wall : ModBlocks.AIR);
                    w.set(px + dx, FLOOR + 5, pz + dz, wall);
                }
            }
            w.set(px + 3, FLOOR + 1, pz, door);
            w.set(px + 3, FLOOR + 2, pz, door);
            w.set(px + 3, FLOOR + 3, pz - 1, ModBlocks.state("wt_sign_swim"));
            if (Hash.unit(h >>> 24) < 0.5f) w.set(cx + 8, FLOOR + 1, cz, Hash.unit(h >>> 30) < 0.4f ? supply : bottle);
            for (int x = x0 + 3; x < x0 + CELL - 2; x++) {
                w.set(x, FLOOR + 1, cz - 1, pipes[0].setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The water burns and pulls; something circles in each tank. */
    static final class Tanks implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            if (p.isInWater() && !p.isCreative()) {
                if (!p.onClimbable()) p.push(0, -0.018, 0);
                p.hurtMarked = true;
                if (p.tickCount % 60 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false));
                    Sanity.add(p, -2f);
                    p.displayClientMessage(Component.literal("The water burns. Get out.").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC), true);
                }
            }
            if ((p.tickCount + p.getId()) % 100 == 0) settle(p, level);
        }

        /** Keep one thing circling in the tank nearest the player. */
        private static void settle(ServerPlayer p, ServerLevel level) {
            long s = Hash.hash(level.getSeed(), Hash.salt("water_treatment"));
            int i = Math.floorDiv(p.getBlockX(), WaterTreatment.CELL), k = Math.floorDiv(p.getBlockZ(), WaterTreatment.CELL);
            int t = tank(s, i, k);
            if (t == 0) return;
            double cx = i * CELL + CELL / 2.0, cz = k * CELL + CELL / 2.0;
            if (!level.getEntities(TankSettler.TYPE, new net.minecraft.world.phys.AABB(cx - 16, FLOOR - 12, cz - 16, cx + 16, FLOOR + 2, cz + 16), e -> true).isEmpty())
                return;
            TankSettler e = TankSettler.TYPE.create(level);
            if (e == null) return;
            double r = t == 1 ? 8 : 6;
            e.moveTo(cx + r, FLOOR - 2.5, cz, 0f, 0f);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(cx, FLOOR, cz)), MobSpawnType.EVENT, null);
            e.anchor(cx, cz, FLOOR, r);
            level.addFreshEntity(e);
        }
    }
}
