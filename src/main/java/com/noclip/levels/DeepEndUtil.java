package com.noclip.levels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the levels in this file group (spawning out of sight, titles, sight checks). */
final class DeepEndUtil {
    private DeepEndUtil() {}

    /** Can the player see this point right now (inside a generous view cone and not blocked)? */
    static boolean seen(ServerPlayer p, Vec3 point, double cone) {
        Vec3 eye = p.getEyePosition();
        Vec3 to = point.subtract(eye).normalize();
        if (p.getViewVector(1f).dot(to) < cone) return false;
        return p.level().clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
                .getType() == HitResult.Type.MISS;
    }

    static boolean seen(ServerPlayer p, BlockPos pos) {
        return seen(p, Vec3.atCenterOf(pos), 0.25);
    }

    /**
     * Places an entity somewhere the player is not looking, between minD and maxD away,
     * standing on solid ground near the player's height. Returns null if no spot was found.
     */
    static <T extends Mob> T spawnNear(ServerLevel level, ServerPlayer p, EntityType<T> type, double minD, double maxD, boolean behind) {
        if (type == null) return null;
        var r = p.getRandom();
        for (int attempt = 0; attempt < 24; attempt++) {
            float yaw = behind ? p.getYRot() + 180f + (r.nextFloat() - 0.5f) * 200f : r.nextFloat() * 360f;
            double d = Mth.lerp(r.nextDouble(), minD, maxD);
            double rad = Math.toRadians(yaw);
            int x = Mth.floor(p.getX() - Math.sin(rad) * d), z = Mth.floor(p.getZ() + Math.cos(rad) * d);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            for (int y = p.getBlockY() + 3; y >= p.getBlockY() - 4; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
                if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) continue;
                if (!level.getFluidState(pos).isEmpty()) continue;
                if (seen(p, Vec3.atCenterOf(pos).add(0, 0.5, 0), 0.3)) break;
                T mob = type.create(level);
                if (mob == null) return null;
                mob.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360f, 0f);
                if (!level.noCollision(mob)) break;
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                level.addFreshEntity(mob);
                return mob;
            }
        }
        return null;
    }

    static void title(ServerPlayer p, Component title, Component sub, int in, int stay, int out) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        if (sub != null) p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    static void actionbar(ServerPlayer p, Component text) {
        p.connection.send(new ClientboundSetActionBarTextPacket(text));
    }

    static long seed(ServerLevel level, String id) {
        return com.noclip.util.Hash.hash(level.getSeed(), com.noclip.util.Hash.salt(id));
    }
}
