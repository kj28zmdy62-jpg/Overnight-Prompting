package com.noclip.levels;

import com.noclip.net.NoclipNet;
import com.noclip.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Small shared helpers for the wiki-classic levels (2, 3, 4, 5, 7, 8, 9, 10, 11, 13, Manila Room, Airport). */
final class ClassicsKit {
    private ClassicsKit() {}

    /** True if the player could plausibly see this point right now (in front of them, line of sight clear). */
    static boolean seen(ServerPlayer p, Vec3 point, double cone) {
        Vec3 to = point.subtract(p.getEyePosition()).normalize();
        if (p.getViewVector(1f).dot(to) < cone) return false;
        return p.level().clip(new ClipContext(p.getEyePosition(), point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
                .getType() == HitResult.Type.MISS;
    }

    static boolean seen(ServerPlayer p, BlockPos pos) {
        return seen(p, Vec3.atCenterOf(pos), 0.2);
    }

    /** Seen by any player in the level. */
    static boolean seenByAnyone(ServerLevel level, BlockPos pos) {
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(Vec3.atCenterOf(pos)) < 96 * 96 && seen(p, pos)) return true;
        return false;
    }

    static BlockState facing(BlockState s, Direction d) {
        return s.setValue(HorizontalDirectionalBlock.FACING, d);
    }

    static void event(ServerPlayer p, String name, float strength) {
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload(name, strength));
    }

    static void sound(ServerLevel level, BlockPos pos, String event, float vol, float pitch) {
        level.playSound(null, pos, ModSounds.get(event), SoundSource.AMBIENT, vol, pitch);
    }

    static void soundTo(ServerPlayer p, String event, float vol, float pitch) {
        p.serverLevel().playSound(null, p.blockPosition(), ModSounds.get(event), SoundSource.AMBIENT, vol, pitch);
    }

    /** Horizontal direction from a (dx, dz) offset, picking the dominant axis. */
    static Direction dir(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
