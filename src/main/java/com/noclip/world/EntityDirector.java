package com.noclip.world;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.SpawnRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Replaces vanilla mob spawning in the Backrooms. Every few seconds it considers
 * each player and maybe places one of the level's entities somewhere nearby,
 * usually behind them and out of sight, so threats arrive rather than pop in.
 */
public final class EntityDirector {
    private EntityDirector() {}

    public static void tick(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
        if (def.spawns.isEmpty() || p.isSpectator() || p.isCreative()) return;
        if ((p.tickCount + p.getId()) % 60 != 0) return;
        RandomSource r = p.getRandom();
        int total = 0;
        for (SpawnRule s : def.spawns) total += s.weight();
        int pick = r.nextInt(total);
        SpawnRule rule = def.spawns.get(0);
        for (SpawnRule s : def.spawns) {
            pick -= s.weight();
            if (pick < 0) {
                rule = s;
                break;
            }
        }
        if (r.nextFloat() >= rule.chance()) return;
        EntityType<? extends Mob> type = rule.type().get();
        int nearby = level.getEntities(type, p.getBoundingBox().inflate(64), e -> true).size();
        if (nearby >= rule.maxNearby()) return;
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos pos = candidate(p, level, def, rule, r);
            if (pos == null) continue;
            Mob mob = type.create(level);
            if (mob == null) return;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            if (!level.noCollision(mob)) continue;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    private static BlockPos candidate(ServerPlayer p, ServerLevel level, BackroomsLevel def, SpawnRule rule, RandomSource r) {
        float yaw;
        if (rule.unseen()) {
            // Somewhere behind the player: 90..270 degrees away from where they look.
            yaw = p.getYRot() + 180f + (r.nextFloat() - 0.5f) * 180f;
        } else {
            yaw = r.nextFloat() * 360f;
        }
        double dist = Mth.lerp(r.nextDouble(), rule.minDist(), rule.maxDist());
        double rad = Math.toRadians(yaw);
        int x = Mth.floor(p.getX() - Math.sin(rad) * dist);
        int z = Mth.floor(p.getZ() + Math.cos(rad) * dist);
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        int startY = Mth.clamp(p.getBlockY() + 6, def.spawnMinY, def.spawnMaxY);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= Math.max(def.spawnMinY, p.getBlockY() - 12); y--) {
            m.set(x, y - 1, z);
            BlockState floor = level.getBlockState(m);
            if (!floor.isFaceSturdy(level, m, Direction.UP)) continue;
            m.set(x, y, z);
            if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) continue;
            if (!level.getBlockState(m.above()).getCollisionShape(level, m.above()).isEmpty()) continue;
            if (!level.getFluidState(m).isEmpty() && !def.allowWaterSpawn) continue;
            if (rule.needsDark() && level.getBrightness(LightLayer.BLOCK, m) >= 5) return null;
            if (rule.unseen() && visible(p, Vec3.atCenterOf(m))) return null;
            return m.immutable();
        }
        return null;
    }

    private static boolean visible(ServerPlayer p, Vec3 target) {
        Vec3 look = p.getViewVector(1f);
        Vec3 to = target.subtract(p.getEyePosition()).normalize();
        if (look.dot(to) < 0.35) return false;
        return p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), target,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
}
