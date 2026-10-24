package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Long Hallway. A narrow hotel corridor with one real door at the far end.
 * The hall is built in repeats of 56 blocks; while you walk toward the door and
 * look at it, every time you cross a repeat you are quietly set back by one,
 * so the hall simply goes on. Facing away, you make real progress. The doors on
 * the walls are painted on. Somebody chalked the rule on the wall where you start.
 */
public final class LongHallway {
    static final int FLOOR = 8, CEIL = FLOOR + 5;
    static final int SPACING = 48, LENGTH = 160, PERIOD = 56, PHASE = 40, FREE = 34;

    private LongHallway() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("long_hallway")
                .number("").title("The Long Hallway").order(4070)
                .tagline("The door at the end is real. The hall does not want you to see it arrive.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Growing())
                .visuals(new Visuals().tint(1.08f, 0.98f, 0.84f).saturation(0.78f).contrast(1.07f).brightness(1.05f)
                        .grain(0.08f).aberration(0.0015f).vignette(0.6f).fog(4f, 44f).fogColor(0x2A2014).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.DOOR, "level_5", 2).exit(ExitType.DOOR, "room_of_doors", 2).exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.DOOR, "the_hub", 1).exit(ExitType.DOOR, "red_room", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "level_5", 1).exit(ExitType.NOCLIP, "recursion", 1)
                .build();
    }

    static int hallX(double x) {
        return (int) Math.round(x / SPACING) * SPACING;
    }

    // ---------------------------------------------------------------- behaviour

    static final class Growing implements LevelBehavior {
        private final Map<UUID, double[]> state = new ConcurrentHashMap<>(); // lastZ, loops, lastStepsTick

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            int xh = hallX(p.getX());
            int z0 = Math.floorDiv(p.getBlockZ(), LENGTH) * LENGTH;
            p.teleportTo(level, xh + 0.5, FLOOR + 1, z0 + 3.5, 0f, 0f);
            state.put(p.getUUID(), new double[]{p.getZ(), 0, 0});
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            double[] st = state.computeIfAbsent(p.getUUID(), u -> new double[]{p.getZ(), 0, 0});
            double z = p.getZ();
            int z0 = Math.floorDiv((int) Math.floor(z), LENGTH) * LENGTH;
            double zl = z - z0, prev = st[0] - z0;
            st[0] = z;
            if (p.isSpectator() || Math.abs(p.getX() - hallX(p.getX())) > 2.5) return;
            double lookZ = p.getLookAngle().z;
            double toDoor = LENGTH - 1 - zl;
            boolean crossed = Math.floorDiv((int) Math.floor(zl) - PHASE, PERIOD) > Math.floorDiv((int) Math.floor(prev) - PHASE, PERIOD)
                    && zl > prev && zl - prev < 2;
            if (crossed && lookZ > -0.3 && toDoor > FREE && zl - PERIOD >= 4) {
                // set back by one repeat; every block around you is identical, so nothing seems to happen
                p.connection.teleport(p.getX(), p.getY(), z - PERIOD, p.getYRot(), p.getXRot(), EnumSet.allOf(RelativeMovement.class));
                st[0] = z - PERIOD;
                st[1]++;
                int loops = (int) st[1];
                p.playNotifySound(ModSounds.get("event.long_hallway_loop"), SoundSource.AMBIENT, 0.35f, 1f);
                Sanity.add(p, -0.8f);
                if (loops >= 3 && loops % 3 == 0) standAhead(p, level);
                if (loops % 2 == 0) {
                    BlockPos knock = BlockPos.containing(p.getX(), FLOOR + 2, z - PERIOD + 18 + p.getRandom().nextInt(10));
                    level.playSound(null, knock, ModSounds.get("event.long_hallway_thud"), SoundSource.AMBIENT, 0.6f, 0.7f);
                }
                return;
            }
            // walking away from the door works. Something walks behind you while you do it.
            if (zl > prev && lookZ < -0.3 && p.tickCount - st[2] > 160 && p.getRandom().nextInt(40) == 0) {
                st[2] = p.tickCount;
                level.playSound(null, BlockPos.containing(p.getX(), FLOOR + 1, z + 12), ModSounds.get("halluc.footsteps"),
                        SoundSource.HOSTILE, 0.7f, 0.85f);
            }
        }

        /** Someone standing down the hall, between you and the door, at the edge of the light. */
        private void standAhead(ServerPlayer p, ServerLevel level) {
            var type = ModEntities.lazy("shadow_figure").get();
            if (type == null) return;
            if (!level.getEntities(type, new AABB(p.blockPosition()).inflate(64), e -> true).isEmpty()) return;
            Mob m = type.create(level);
            if (m == null) return;
            m.moveTo(hallX(p.getX()) + 0.5, FLOOR + 1, p.getZ() + 30, 180f, 0f);
            if (!level.noCollision(m)) return;
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(m);
        }
    }

    // ---------------------------------------------------------------- the hall

    static final class Gen implements LevelGenerator {
        final BlockState paper = ModBlocks.state("lh_wallpaper");
        final BlockState wain = ModBlocks.state("lh_wainscot");
        final BlockState boards = ModBlocks.state("lh_boards");
        final BlockState runL = ModBlocks.state("lh_runner_l");
        final BlockState runM = ModBlocks.state("lh_runner_m");
        final BlockState runR = ModBlocks.state("lh_runner_r");
        final BlockState ceiling = ModBlocks.state("lh_ceiling");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, FLOOR, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                int xh = hallX(x);
                int dx = x - xh;
                if (Math.abs(dx) > 2) continue;
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, dx, z);
            }
        }

        BlockState f(String n, Direction d) {
            return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void column(ChunkWriter w, long s, int x, int dx, int z) {
            int z0 = Math.floorDiv(z, LENGTH) * LENGTH;
            int zl = z - z0;
            int F = FLOOR;
            boolean endWall = zl == 0 || zl == LENGTH - 1;
            if (Math.abs(dx) == 2 || endWall) {
                w.set(x, F, z, boards);
                w.set(x, F + 1, z, wain);
                for (int y = F + 2; y <= F + 4; y++) w.set(x, y, z, paper);
                w.set(x, CEIL, z, ceiling);
                if (zl == 0 && Math.abs(dx) <= 1) {
                    String c = dx < 0 ? "l" : dx == 0 ? "m" : "r";
                    w.set(x, F + 3, z, ModBlocks.state("lh_chalk_t" + c));
                    w.set(x, F + 2, z, ModBlocks.state("lh_chalk_b" + c));
                }
                if (zl == LENGTH - 1 && dx == 0) {
                    w.set(x, F + 1, z, ModBlocks.state("lh_door_low"));
                    w.set(x, F + 2, z, ModBlocks.state("lh_door_high"));
                }
                if (Math.abs(dx) == 2 && !endWall && zl >= 3 && zl <= LENGTH - 4) paintedDoor(w, s, x, dx, z, zl);
                return;
            }
            w.set(x, F, z, dx < 0 ? runL : dx == 0 ? runM : runR);
            for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
            w.set(x, CEIL, z, ceiling);
            if (zl == LENGTH - 2 && dx == 0) w.set(x, F + 3, z, f("lh_sconce", Direction.SOUTH));
            // sconces: one every seven blocks, alternating sides, periodic with the hall
            int q = Math.floorMod(zl - 4, PERIOD);
            if (Math.abs(dx) == 1 && q % 7 == 3 && zl > 2 && zl < LENGTH - 3) {
                int j = q / 7;
                int side = (j % 2 == 0) ? 1 : -1;
                if (dx == side) {
                    boolean dead = Hash.unit(s, j, 0x5C0) < 0.18f;
                    w.set(x, F + 3, z, f(dead ? "lh_sconce_dead" : "lh_sconce", side < 0 ? Direction.WEST : Direction.EAST));
                }
            }
        }

        /** Doors every seven blocks, alternating walls. All of them paint. Periodic, so the repeats match. */
        void paintedDoor(ChunkWriter w, long s, int x, int dx, int z, int zl) {
            int q = Math.floorMod(zl - 4, PERIOD);
            if (q % 7 != 0) return;
            int j = q / 7;
            int side = (j % 2 == 0) ? -2 : 2;
            if (dx != side) return;
            String v = Hash.unit(s, j, 0xD00) < 0.5f ? "lh_pdoor" : "lh_pdoor2";
            w.set(x, FLOOR + 1, z, ModBlocks.state(v + "_low"));
            w.set(x, FLOOR + 2, z, ModBlocks.state(v + "_high"));
        }
    }
}
