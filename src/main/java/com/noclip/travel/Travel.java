package com.noclip.travel;

import com.noclip.Noclip;
import com.noclip.block.ExitBlock;
import com.noclip.block.HazardBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.Levels;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Moves players between levels: noclipping, exits, and the way home. */
public final class Travel {
    /** Exit-table target meaning "back to the real world". */
    public static final String OVERWORLD = "overworld";
    private static final int COOLDOWN = 60;
    private static final Map<UUID, Long> lastTravel = new ConcurrentHashMap<>();

    private Travel() {}

    public static boolean onCooldown(ServerPlayer p) {
        Long t = lastTravel.get(p.getUUID());
        return t != null && p.server.getTickCount() - t < COOLDOWN;
    }

    /** Leave the player's current level through an exit of the given type. */
    public static void exit(ServerPlayer player, ExitType type, BlockPos from) {
        if (onCooldown(player)) return;
        BackroomsLevel here = Levels.get(player.level().dimension());
        String target;
        if (here == null) {
            target = "level_0";
        } else {
            target = here.pickExit(type, player.getRandom());
            if (target == null) return;
        }
        sendTo(player, target);
    }

    /** Teleport to a level by id (or {@link #OVERWORLD}). */
    public static boolean sendTo(ServerPlayer player, String levelId) {
        MinecraftServer server = player.server;
        lastTravel.put(player.getUUID(), (long) server.getTickCount());
        if (OVERWORLD.equals(levelId)) {
            ServerLevel ow = server.overworld();
            BlockPos spawn = player.getRespawnPosition() != null && player.getRespawnDimension() == Level.OVERWORLD
                    ? player.getRespawnPosition() : ow.getSharedSpawnPos();
            BlockPos safe = ow.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
            player.teleportTo(ow, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), 0f);
            player.resetFallDistance();
            ServerPlayNetworking.send(player, new NoclipNet.EnterPayload("", false, PlayerData.visited(player).size()));
            return true;
        }
        BackroomsLevel def = Levels.get(levelId);
        if (def == null) {
            Noclip.LOG.warn("Exit to unknown level {}", levelId);
            return false;
        }
        ServerLevel dest = server.getLevel(def.dimension);
        if (dest == null) {
            Noclip.LOG.error("Dimension for level {} is not loaded; is its JSON missing?", levelId);
            return false;
        }
        RandomSource r = player.getRandom();
        Vec3 spot = null;
        for (int attempt = 0; attempt < 12 && spot == null; attempt++) {
            int x = (r.nextInt(2) == 0 ? -1 : 1) * (2000 + r.nextInt(60000));
            int z = (r.nextInt(2) == 0 ? -1 : 1) * (2000 + r.nextInt(60000));
            spot = findSpawn(dest, def, x >> 4, z >> 4, r);
        }
        if (spot == null) {
            Noclip.LOG.warn("No safe spawn found in {}, placing at fallback", levelId);
            spot = new Vec3(0.5, def.spawnMinY + 1, 0.5);
        }
        float yaw = r.nextFloat() * 360f;
        player.teleportTo(dest, spot.x, spot.y, spot.z, yaw, 0f);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        arrive(player, dest, def);
        return true;
    }

    private static void arrive(ServerPlayer player, ServerLevel level, BackroomsLevel def) {
        boolean firstEver = PlayerData.visited(player).isEmpty();
        boolean first = PlayerData.visit(player, def.id);
        if (firstEver) {
            // you arrive with nothing but what was in your pockets
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.noclip.registry.ModItems.JOURNAL));
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.noclip.registry.ModItems.ALMOND_WATER));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("noclip.first_arrival")
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
        ServerPlayNetworking.send(player, new NoclipNet.EnterPayload(def.id, first, PlayerData.visited(player).size()));
        ServerPlayNetworking.send(player, new NoclipNet.JournalPayload(String.join(",", PlayerData.visited(player))));
        level.playSound(null, player.blockPosition(), ModSounds.NOCLIP, SoundSource.PLAYERS, 0.9f, 1f);
        com.noclip.world.Blackouts.sync(player);
        def.behavior.onEnter(player, level, def);
    }

    /** Scans one chunk (generating it if needed) for somewhere a player can stand. */
    public static Vec3 findSpawn(ServerLevel level, BackroomsLevel def, int cx, int cz, RandomSource r) {
        LevelChunk chunk = level.getChunk(cx, cz);
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minY = Math.max(def.spawnMinY, level.getMinBuildHeight() + 1);
        int maxY = Math.min(def.spawnMaxY, level.getMaxBuildHeight() - 3);
        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                int x = (cx << 4) + lx, z = (cz << 4) + lz;
                for (int y = maxY; y >= minY; y--) {
                    if (standable(level, chunk, m, x, y, z, def.allowWaterSpawn)) {
                        candidates.add(new BlockPos(x, y, z));
                        break;
                    }
                }
            }
        }
        if (candidates.isEmpty()) return null;
        BlockPos p = candidates.get(r.nextInt(candidates.size()));
        return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    private static boolean standable(ServerLevel level, LevelChunk chunk, BlockPos.MutableBlockPos m, int x, int y, int z, boolean water) {
        BlockState floor = chunk.getBlockState(m.set(x, y - 1, z));
        if (floor.getBlock() instanceof HazardBlock || floor.getBlock() instanceof ExitBlock) return false;
        if (!floor.isFaceSturdy(level, m, Direction.UP)) return false;
        for (int dy = 0; dy < 2; dy++) {
            BlockState s = chunk.getBlockState(m.set(x, y + dy, z));
            if (!s.getCollisionShape(level, m).isEmpty()) return false;
            if (s.getBlock() instanceof ExitBlock || s.getBlock() instanceof HazardBlock) return false;
            if (!s.getFluidState().isEmpty() && !water) return false;
        }
        return true;
    }

    public static void forget(ServerPlayer p) {
        lastTravel.remove(p.getUUID());
    }

}
