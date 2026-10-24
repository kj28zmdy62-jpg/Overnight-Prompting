package com.noclip.item;

import com.noclip.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Real light for flashlights: keeps a bright invisible light block where the
 * beam lands and a dim one at the holder's head, moving them as the player looks
 * around. Positions are "claimed" with a short expiry; unclaimed glows remove
 * themselves (see {@link com.noclip.block.GlowBlock}).
 */
public final class FlashlightLights {
    private static final Map<ResourceKey<Level>, Map<Long, Long>> CLAIMS = new HashMap<>();

    private FlashlightLights() {}

    public static boolean claimed(ServerLevel level, BlockPos pos) {
        Map<Long, Long> m = CLAIMS.get(level.dimension());
        if (m == null) return false;
        Long until = m.get(pos.asLong());
        return until != null && until >= level.getGameTime();
    }

    public static void tick(ServerPlayer p) {
        if (p.tickCount % 2 != 0 || !FlashlightItem.holding(p)) return;
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(16));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        BlockPos spot;
        if (hit.getType() == HitResult.Type.BLOCK) spot = hit.getBlockPos().relative(hit.getDirection());
        else spot = BlockPos.containing(end);
        place(level, spot, "flashlight_glow");
        // a second, softer light halfway along the beam keeps nearby walls readable
        place(level, BlockPos.containing(eye.add(hit.getLocation().subtract(eye).scale(0.45))), "flashlight_glow_dim");
        place(level, BlockPos.containing(eye), "flashlight_glow_dim");
    }

    private static void place(ServerLevel level, BlockPos pos, String block) {
        Map<Long, Long> m = CLAIMS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        long now = level.getGameTime();
        if (m.size() > 512) m.values().removeIf(t -> t < now);
        BlockState here = level.getBlockState(pos);
        BlockState glow = ModBlocks.state(block);
        if (here.is(glow.getBlock())) {
            m.put(pos.asLong(), now + 6);
            return;
        }
        if (!here.isAir() || !level.getFluidState(pos).isEmpty()) return;
        m.put(pos.asLong(), now + 6);
        level.setBlock(pos, glow, Block.UPDATE_CLIENTS);
    }
}
