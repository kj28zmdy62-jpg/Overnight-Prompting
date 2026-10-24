package com.noclip.levels;

import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Small shared helpers for the "Outside, but wrong" levels. Not a level itself. */
public final class OutsideKit {
    private OutsideKit() {}

    /** The per-level generator seed, recomputed on the server side so behaviours can find generated features. */
    public static long seed(ServerLevel level, String id) {
        return Hash.hash(level.getSeed(), Hash.salt(id));
    }

    /** True during development screenshot runs (tools/autotest.sh). */
    public static boolean autotest() {
        String s = System.getenv("NOCLIP_AUTOTEST");
        return s != null && !s.isBlank();
    }

    /** Invisible vanilla light source. */
    public static BlockState light(int level) {
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.max(0, Math.min(15, level)));
    }

    public static BlockState facing(String block, Direction d) {
        return ModBlocks.state(block).setValue(HorizontalDirectionalBlock.FACING, d);
    }

    public static BlockState facing(BlockState s, Direction d) {
        return s.setValue(HorizontalDirectionalBlock.FACING, d);
    }

    /** True if the point is inside the player's view cone and not hidden behind blocks. */
    public static boolean seen(ServerPlayer p, Vec3 pt, double cone) {
        Vec3 eye = p.getEyePosition();
        Vec3 to = pt.subtract(eye).normalize();
        if (p.getViewVector(1f).dot(to) < cone) return false;
        return p.level().clip(new ClipContext(eye, pt, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
                .getType() == HitResult.Type.MISS;
    }

    public static boolean seen(ServerPlayer p, BlockPos pos, double cone) {
        return seen(p, Vec3.atCenterOf(pos), cone) || seen(p, Vec3.atCenterOf(pos.above()), cone);
    }

    /** Facing cone check only (no ray), for things that are visible through fog and clutter. */
    public static boolean inView(ServerPlayer p, Vec3 pt, double cone) {
        Vec3 to = pt.subtract(p.getEyePosition()).normalize();
        return p.getViewVector(1f).dot(to) >= cone;
    }

    /** Moves a player by an offset without a hitch: position, velocity and view are kept. */
    public static void shift(ServerPlayer p, double dx, double dy, double dz) {
        p.teleportTo(p.serverLevel(), dx, dy, dz, EnumSet.allOf(RelativeMovement.class), 0f, 0f);
    }

    public static void sound(ServerLevel level, BlockPos pos, String event, float vol, float pitch) {
        var e = ModSounds.getOrNull(event);
        if (e != null) level.playSound(null, pos, e, SoundSource.AMBIENT, vol, pitch);
    }

    /** A point behind the player at the given distance (horizontal). */
    public static Vec3 behind(ServerPlayer p, double dist, double spreadDeg, net.minecraft.util.RandomSource r) {
        double ang = Math.toRadians(p.getYRot() + 180 + (r.nextFloat() - 0.5f) * spreadDeg);
        return new Vec3(p.getX() - Math.sin(ang) * dist, p.getY(), p.getZ() + Math.cos(ang) * dist);
    }

    /** Where blocks go: a chunk being generated, or a live level. */
    public interface Setter {
        void set(int x, int y, int z, BlockState s);

        boolean air(int x, int y, int z);

        static Setter of(ChunkWriter w) {
            return new Setter() {
                public void set(int x, int y, int z, BlockState s) { w.set(x, y, z, s); }
                public boolean air(int x, int y, int z) { return w.isAir(x, y, z); }
            };
        }

        static Setter of(ServerLevel level) {
            return new Setter() {
                public void set(int x, int y, int z, BlockState s) {
                    level.setBlock(new BlockPos(x, y, z), s, net.minecraft.world.level.block.Block.UPDATE_ALL);
                }
                public boolean air(int x, int y, int z) {
                    BlockState b = level.getBlockState(new BlockPos(x, y, z));
                    return b.isAir() || b.is(Blocks.LIGHT);
                }
            };
        }
    }

    public static void car(ChunkWriter w, int x, int y, int z, Direction d, String colour, boolean lit) {
        car(Setter.of(w), x, y, z, d, colour, lit);
    }

    /** Places a 2x4 car (hood north before rotation) with its front facing {@code d}, base at y. */
    public static void car(Setter w, int x, int y, int z, Direction d, String colour, boolean lit) {
        String[] parts = {"hood", "cabin_front", "cabin_back", "trunk"};
        Direction back = d.getOpposite();
        Direction side = d.getClockWise();
        for (int i = 0; i < 4; i++) {
            String name = "ol_car_" + colour + "_" + parts[i] + (lit && i == 0 ? "_lit" : "");
            BlockState s = facing(name, d);
            for (int c = 0; c < 2; c++) {
                int bx = x + back.getStepX() * i + side.getStepX() * c;
                int bz = z + back.getStepZ() * i + side.getStepZ() * c;
                w.set(bx, y, bz, s);
            }
        }
        // the beam: a fading run of invisible light ahead of the headlights (or none)
        for (int k = 1; k <= 9; k++) {
            for (int c = 0; c < 2; c++) {
                int bx = x + d.getStepX() * k + side.getStepX() * c;
                int bz = z + d.getStepZ() * k + side.getStepZ() * c;
                if (w.air(bx, y, bz)) w.set(bx, y, bz, lit ? light(Math.max(4, 14 - k)) : ModBlocks.AIR);
            }
        }
    }

    /** Removes a car placed with {@link #car}. */
    public static void clearCar(Setter w, int x, int y, int z, Direction d) {
        Direction back = d.getOpposite();
        Direction side = d.getClockWise();
        for (int i = -9; i < 4; i++)
            for (int c = 0; c < 2; c++) {
                int bx = x + back.getStepX() * i + side.getStepX() * c, bz = z + back.getStepZ() * i + side.getStepZ() * c;
                if (i >= 0 || w.air(bx, y, bz)) w.set(bx, y, bz, ModBlocks.AIR);
            }
    }
}
