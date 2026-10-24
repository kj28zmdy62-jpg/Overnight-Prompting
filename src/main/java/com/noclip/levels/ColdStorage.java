package com.noclip.levels;

import com.noclip.block.SlickIceBlock;
import com.noclip.entity.WrappedOne;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cold Storage. Freezer rooms behind strip curtains, lit by vapor-proof lamps,
 * stocked with wrapped shapes hanging from overhead rails. The cold builds up in
 * you the whole time you are here, faster on the blast-freezer ice. Only the
 * rare break rooms, with their humming space heaters, give it back.
 */
public final class ColdStorage {
    static final int FLOOR = 10, CEIL = 16;

    private ColdStorage() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("cold_storage")
                .number("").title("Cold Storage").order(2030)
                .tagline("Keep moving. Find a heater. Do not count the bundles.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Cold())
                .visuals(new Visuals().tint(0.88f, 0.97f, 1.10f).saturation(0.55f).contrast(1.04f).brightness(1.22f)
                        .grain(0.07f).aberration(0.0012f).vignette(0.55f).fog(2.5f, 26f).fogColor(0xC7D4DC).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.NOCLIP, "level_1", 2).exit(ExitType.NOCLIP, "container_yard", 1).exit(ExitType.NOCLIP, "boiler_room", 1)
                .exit(ExitType.DOOR, "container_yard", 3).exit(ExitType.DOOR, "assembly_line", 1).exit(ExitType.DOOR, "level_1", 1)
                .exit(ExitType.WATER, "storm_drains", 2).exit(ExitType.WATER, "sewers", 1).exit(ExitType.WATER, "level_7", 1)
                .build();
    }

    // ---------------------------------------------------------------- the cold

    private static final Map<UUID, float[]> COLD = new ConcurrentHashMap<>();

    /** Adds to a player's cold (0 = warm, 140 = frozen through). */
    public static void chill(ServerPlayer p, float amount) {
        float[] c = COLD.computeIfAbsent(p.getUUID(), k -> new float[]{30f, 0.1f, 0f});
        c[0] = Math.min(320f, c[0] + amount);
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState panel = ModBlocks.state("cs_panel_wall");
        final BlockState frosted = ModBlocks.state("cs_panel_frosted");
        final BlockState base = ModBlocks.state("cs_panel_base");
        final BlockState[] signs = {ModBlocks.state("cs_sign_temp"), ModBlocks.state("cs_sign_ppe"),
                ModBlocks.state("cs_sign_alone"), ModBlocks.state("cs_sign_count")};
        final BlockState floor = ModBlocks.state("cs_floor");
        final BlockState drain = ModBlocks.state("cs_floor_drain");
        final BlockState steps = ModBlocks.state("cs_floor_steps");
        final BlockState ice = ModBlocks.state("cs_ice_floor");
        final BlockState hole = ModBlocks.state("cs_ice_hole");
        final BlockState ceiling = ModBlocks.state("cs_ceiling");
        final BlockState lamp = ModBlocks.state("cs_lamp");
        final BlockState lampBad = ModBlocks.state("cs_lamp_bad");
        final BlockState lampDead = ModBlocks.state("cs_lamp_dead");
        final BlockState curtain = ModBlocks.state("cs_curtain");
        final BlockState door = ModBlocks.state("cs_freezer_door");
        final BlockState doorLight = ModBlocks.state("cs_exit_light");
        final BlockState rail = ModBlocks.state("cs_rail");
        final BlockState chain = ModBlocks.state("cs_hook_chain");
        final BlockState hook = ModBlocks.state("cs_hook");
        final BlockState bundleTop = ModBlocks.state("cs_bundle_top");
        final BlockState bundleBottom = ModBlocks.state("cs_bundle_bottom");
        final BlockState bundleBottomB = ModBlocks.state("cs_bundle_bottom_b");
        final BlockState stock = ModBlocks.state("cs_pallet_boxes");
        final BlockState heater = ModBlocks.state("cs_heater");
        final BlockState officeWall = ModBlocks.state("cs_office_wall");
        final BlockState calendar = ModBlocks.state("cs_office_calendar");
        final BlockState officeFloor = ModBlocks.state("cs_office_floor");
        final BlockState figure = ModBlocks.state("cs_ice_figure");
        final BlockState figureTop = ModBlocks.state("cs_ice_figure_top");
        final BlockState icicles = ModBlocks.state("cs_icicles");
        final BlockState shelf = ModBlocks.state("cs_wire_shelf");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState tube = ModBlocks.state("fluorescent_light");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 16, 5);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL + 1, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    float r = Hash.unit(s, x, z, 3);
                    w.set(x, FLOOR, z, r < 0.004f ? steps : floor);
                    w.set(x, CEIL, z, ceilingAt(s, x, z));
                    int i = g.cellX(x), k = g.cellZ(z);
                    boolean wx = x == g.x0(i), wz = z == g.z0(k);
                    if (wx || wz) {
                        for (int y = FLOOR + 1; y < CEIL; y++) w.set(x, y, z, wallAt(s, x, y, z));
                    }
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                doorways(w, g, s, i, k, x0, z0, wd, dp);
                room(w, g, s, i, k, x0, z0, wd, dp);
            });
        }

        BlockState ceilingAt(long s, int x, int z) {
            if (Math.floorMod(x, 5) != 2 || Math.floorMod(z, 5) != 2) return ceiling;
            float dark = Hash.noise(s ^ 0xC01D, x / 36.0, z / 36.0);
            float r = Hash.unit(s, x, z, 9);
            if (dark > 0.78f) return r < 0.3f ? lampBad : lampDead;
            return r < 0.08f ? lampBad : r < 0.12f ? lampDead : lamp;
        }

        BlockState wallAt(long s, int x, int y, int z) {
            if (y == FLOOR + 1) return base;
            float r = Hash.unit(s, x, y, z);
            if (y == FLOOR + 3 && r < 0.03f) return signs[Hash.range(Hash.hash(s, x, z, 44), signs.length)];
            return Hash.fbm(s ^ 0xF205, x / 10.0, (y + z) / 10.0, 2) > 0.6f ? frosted : panel;
        }

        /** West and north edge of every cell: usually a 3-wide doorway hung with strip curtain. */
        void doorways(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            for (int side = 0; side < 2; side++) {
                long h = g.hash(i, k, 50 + side);
                float r = Hash.unit(h);
                if (r < 0.2f) continue; // solid wall
                int len = side == 0 ? dp : wd;
                int at = 2 + Hash.range(h >>> 8, Math.max(1, len - 6));
                boolean bare = r > 0.88f;
                for (int d = at; d < at + 3; d++) {
                    int x = side == 0 ? x0 : x0 + d, z = side == 0 ? z0 + d : z0;
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                        w.set(x, y, z, bare ? ModBlocks.AIR
                                : curtain.setValue(HorizontalDirectionalBlock.FACING, side == 0 ? Direction.EAST : Direction.NORTH));
                    }
                }
            }
        }

        void room(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            long h = g.hash(i, k, 60);
            float r = Hash.unit(h);
            int cx = x0 + wd / 2, cz = z0 + dp / 2;
            if (r < 0.40f) hangingRoom(w, s, h, x0, z0, wd, dp);
            else if (r < 0.60f) stockRoom(w, h, x0, z0, wd, dp);
            else if (r < 0.70f) iceRoom(w, s, h, x0, z0, wd, dp, cx, cz);
            else if (r < 0.77f) {
                // empty and spotless: a floor drain, and prints that walk into the wall
                w.set(cx, FLOOR, cz, drain);
                for (int d = 0; d < wd / 2; d++) w.set(cx + d, FLOOR, cz + (d % 2), steps);
            } else if (r < 0.845f) breakRoom(w, h, x0, z0, wd, dp);
            else if (r < 0.865f) dockDoor(w, x0, z0, wd);
            else {
                w.set(cx, FLOOR, cz, drain);
                for (int n = 0; n < 5; n++) {
                    long hh = Hash.hash(h, n);
                    w.set(x0 + 2 + Hash.range(hh, wd - 3), FLOOR + 1, z0 + 2 + Hash.range(hh >>> 8, dp - 3),
                            stock.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(hh >>> 16, 4))));
                }
                if (Hash.unit(h >>> 30) < 0.3f) w.set(cx + 1, FLOOR + 1, cz - 1, supply);
                else if (Hash.unit(h >>> 30) < 0.5f) w.set(cx - 1, FLOOR + 1, cz + 1, bottle);
            }
        }

        /** Rows of wrapped bundles on overhead rails, hanging low enough to brush your head. */
        void hangingRoom(ChunkWriter w, long s, long h, int x0, int z0, int wd, int dp) {
            for (int z = z0 + 2; z <= z0 + dp - 2; z += 3) {
                for (int x = x0 + 1; x < x0 + wd; x++) {
                    w.set(x, CEIL - 1, z, rail.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    if (x < x0 + 2 || x > x0 + wd - 2 || Math.floorMod(x, 2) != 0) continue;
                    float b = Hash.unit(s, x, z, 61);
                    if (b < 0.18f) {
                        w.set(x, CEIL - 1, z, hook.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    } else {
                        Direction f = Direction.from2DDataValue(Hash.range(s, x, z, 62, 4));
                        w.set(x, CEIL - 1, z, chain.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                        w.set(x, CEIL - 2, z, bundleTop.setValue(HorizontalDirectionalBlock.FACING, f));
                        w.set(x, CEIL - 3, z, (b < 0.45f ? bundleBottomB : bundleBottom).setValue(HorizontalDirectionalBlock.FACING, f));
                    }
                }
            }
        }

        void stockRoom(ChunkWriter w, long h, int x0, int z0, int wd, int dp) {
            for (int x = x0 + 2; x <= x0 + wd - 2; x += 3) {
                for (int z = z0 + 2; z <= z0 + dp - 3; z++) {
                    if (z == z0 + dp / 2) continue;
                    boolean shelves = Hash.unit(h >>> 3) < 0.4f;
                    if (shelves) {
                        BlockState sh = shelf.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
                        w.set(x, FLOOR + 1, z, sh);
                        w.set(x, FLOOR + 2, z, sh);
                    } else {
                        Direction f = Direction.from2DDataValue(Hash.range(Hash.hash(h, x, z), 4));
                        w.set(x, FLOOR + 1, z, stock.setValue(HorizontalDirectionalBlock.FACING, f));
                        if (Hash.unit(Hash.hash(h, x, z, 2)) < 0.6f)
                            w.set(x, FLOOR + 2, z, stock.setValue(HorizontalDirectionalBlock.FACING, f.getOpposite()));
                    }
                }
            }
        }

        /** Blast freezer: an ice floor, icicles, sometimes a hole, sometimes someone who stood still too long. */
        void iceRoom(ChunkWriter w, long s, long h, int x0, int z0, int wd, int dp, int cx, int cz) {
            for (int x = x0 + 1; x < x0 + wd; x++) {
                for (int z = z0 + 1; z < z0 + dp; z++) {
                    w.set(x, FLOOR, z, ice);
                    if (Hash.chance(s, x, z, 63, 0.12f)) w.set(x, CEIL - 1, z, icicles);
                }
            }
            float r = Hash.unit(h >>> 12);
            if (r < 0.3f) {
                for (int x = cx; x <= cx + 1; x++)
                    for (int z = cz; z <= cz + 1; z++) {
                        w.set(x, FLOOR, z, hole);
                        w.set(x, CEIL - 1, z, ModBlocks.AIR);
                    }
            } else if (r < 0.55f) {
                Direction f = Direction.from2DDataValue(Hash.range(h >>> 20, 4));
                w.set(cx, FLOOR + 1, cz, figure.setValue(HorizontalDirectionalBlock.FACING, f));
                w.set(cx, FLOOR + 2, cz, figureTop.setValue(HorizontalDirectionalBlock.FACING, f));
                w.set(cx, CEIL - 1, cz, ModBlocks.AIR);
            }
        }

        /** A warm little break room: beige walls, a calendar with every day crossed out, a heater. */
        void breakRoom(ChunkWriter w, long h, int x0, int z0, int wd, int dp) {
            int bx0 = x0 + 3, bz0 = z0 + 3, bx1 = x0 + Math.min(wd - 3, 10), bz1 = z0 + Math.min(dp - 3, 9);
            int doorX = bx0 + 2;
            for (int x = bx0; x <= bx1; x++) {
                for (int z = bz0; z <= bz1; z++) {
                    boolean edge = x == bx0 || x == bx1 || z == bz0 || z == bz1;
                    w.set(x, FLOOR, z, officeFloor);
                    for (int y = FLOOR + 1; y < CEIL; y++) {
                        if (!edge) w.set(x, y, z, ModBlocks.AIR);
                        else if (y >= FLOOR + 4) w.set(x, y, z, panel);
                        else w.set(x, y, z, (z == bz1 && x == bx0 + 3 && y == FLOOR + 2) ? calendar : officeWall);
                    }
                    if (!edge) w.set(x, CEIL, z, ceiling);
                }
            }
            // door in the north wall
            w.set(doorX, FLOOR + 1, bz0, ModBlocks.AIR);
            w.set(doorX, FLOOR + 2, bz0, ModBlocks.AIR);
            w.set(doorX, FLOOR + 3, bz0, curtain.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set(bx1 - 1, FLOOR + 1, bz1 - 1, heater.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            w.set((bx0 + bx1) / 2, CEIL, (bz0 + bz1) / 2, tube);
            w.set(bx0 + 2, FLOOR + 1, bz1 - 2, chair.setValue(HorizontalDirectionalBlock.FACING,
                    Direction.from2DDataValue(Hash.range(h >>> 9, 4))));
            if (Hash.unit(h >>> 40) < 0.5f) w.set(bx1 - 2, FLOOR + 1, bz0 + 1, bottle);
        }

        void dockDoor(ChunkWriter w, int x0, int z0, int wd) {
            int x = x0 + wd / 2;
            w.set(x, FLOOR + 1, z0, door);
            w.set(x, FLOOR + 2, z0, door);
            w.set(x, FLOOR + 3, z0, panel);
            w.set(x, FLOOR + 3, z0 + 1, doorLight.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            for (int d = -1; d <= 1; d += 2) w.set(x + d, FLOOR, z0 + 1, drain);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The cold builds while you are here; heaters give it back. And things hang among the stock. */
    static final class Cold implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            COLD.put(p.getUUID(), new float[]{30f, 0.1f, 0f});
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || p.isCreative()) return;
            float[] c = COLD.computeIfAbsent(p.getUUID(), k -> new float[]{30f, 0.1f, 0f});
            if (p.tickCount % 10 == 0) c[1] = rate(p, level);
            float before = c[0];
            c[0] = Math.max(0f, Math.min(320f, c[0] + c[1]));
            p.setTicksFrozen(Math.min((int) c[0], 160));
            if (c[0] >= 140 && p.tickCount % 40 == 20) p.hurt(level.damageSources().freeze(), 1f);
            if (c[0] >= 200 && p.tickCount % 20 == 0)
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, c[0] >= 270 ? 1 : 0, true, false));
            if (before < 100 && c[0] >= 100) say(p, "Your fingers are going numb.");
            if (before < 140 && c[0] >= 140) say(p, "The cold is inside you now. Find somewhere warm.");
            if (c[1] < 0 && before > 60 && c[0] <= 60) say(p, "The heater ticks. You can feel your hands again.");
            // breath you can see
            if (c[0] > 40 && p.tickCount % 50 == 0) {
                Vec3 look = p.getViewVector(1f);
                level.sendParticles(ParticleTypes.POOF, p.getX() + look.x * 0.5, p.getEyeY() - 0.1, p.getZ() + look.z * 0.5,
                        2, 0.05, 0.02, 0.05, 0.005);
            }
            if (p.tickCount % 12 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.003
                    && level.getBlockState(p.blockPosition().above()).is(ModBlocks.get("cs_curtain"))) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.cold_storage_curtain"), SoundSource.BLOCKS, 0.6f,
                        0.9f + p.getRandom().nextFloat() * 0.2f);
            }
            if ((p.tickCount + p.getId()) % 160 == 0 && p.getRandom().nextFloat() < 0.45f) hangOne(p, level);
        }

        /** Cold gained per tick: negative near a heater, worse on the blast-freezer ice. */
        private static float rate(ServerPlayer p, ServerLevel level) {
            Block heater = ModBlocks.get("cs_heater");
            BlockPos at = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-4, -1, -4), at.offset(4, 2, 4))) {
                if (level.getBlockState(q).is(heater)) return -2.2f;
            }
            if (level.getBlockState(at.below()).getBlock() instanceof SlickIceBlock) return 0.34f;
            return 0.12f;
        }

        private static void say(ServerPlayer p, String msg) {
            p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);
        }

        /** Puts something on an empty hook nearby, out of sight. It looks like all the others. */
        private static void hangOne(ServerPlayer p, ServerLevel level) {
            if (!level.getEntities(WrappedOne.TYPE, p.getBoundingBox().inflate(40), e -> true).isEmpty()) return;
            Block hook = ModBlocks.get("cs_hook");
            List<BlockPos> hooks = new ArrayList<>();
            BlockPos at = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(at.getX() - 16, CEIL - 1, at.getZ() - 16, at.getX() + 16, CEIL - 1, at.getZ() + 16)) {
                if (level.getBlockState(q).is(hook) && q.distSqr(at) > 36) hooks.add(q.immutable());
            }
            if (hooks.isEmpty()) return;
            BlockPos q = hooks.get(p.getRandom().nextInt(hooks.size()));
            Vec3 c = new Vec3(q.getX() + 0.5, CEIL - 3, q.getZ() + 0.5);
            Vec3 to = c.add(0, 0.8, 0).subtract(p.getEyePosition()).normalize();
            if (p.getViewVector(1f).dot(to) > 0.3 && level.clip(new ClipContext(p.getEyePosition(), c.add(0, 0.8, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS) return;
            WrappedOne e = WrappedOne.TYPE.create(level);
            if (e == null) return;
            e.moveTo(c.x, c.y, c.z, p.getRandom().nextFloat() * 360f, 0f);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null);
            e.hang();
            level.addFreshEntity(e);
        }
    }
}
