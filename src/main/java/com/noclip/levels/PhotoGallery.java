package com.noclip.levels;

import com.noclip.entity.PgPhotographer;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Photo Gallery. Quiet white rooms hung with framed photographs of these same
 * rooms. When a photograph leaves your sight it is retaken: the next time you
 * look, there is someone in it, a figure from behind, standing in the gallery.
 * It is you. The photos follow you closer, and then there is someone behind
 * you in them too. When the last photograph is of the back of your head, the
 * flash goes off. Somewhere among them is a photograph of an open door, full
 * of daylight; reach into it.
 */
public final class PhotoGallery {
    static final int FLOOR = 8, CEIL = FLOOR + 6;

    private PhotoGallery() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("photo_gallery")
                .number("").title("Photo Gallery").order(4130)
                .tagline("Photographs of these rooms. Look again: you are in them now.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Retaken())
                .visuals(new Visuals().tint(1.04f, 1.0f, 0.94f).saturation(0.55f).contrast(1.06f).brightness(1.28f)
                        .grain(0.07f).aberration(0.0012f).vignette(0.55f).fog(6f, 44f).fogColor(0x2A2622).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "mirror_level", 1).exit(ExitType.DOOR, "museum", 1)
                .exit(ExitType.DOOR, "recursion", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "museum", 1)
                .build();
    }

    static String name(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }

    // ---------------------------------------------------------------- behaviour

    static final class Retaken implements LevelBehavior {
        static final class St {
            int stage, changes;
            Set<BlockPos> seen = new HashSet<>();
            int photographer = -1;
        }

        private final Map<UUID, St> state = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            state.put(p.getUUID(), new St());
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 4 != 0 || p.isSpectator()) return;
            St s = state.computeIfAbsent(p.getUUID(), u -> new St());
            Set<BlockPos> now = new HashSet<>();
            BlockPos c = p.blockPosition();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dx = -10; dx <= 10; dx++)
                for (int dz = -10; dz <= 10; dz++) {
                    m.set(c.getX() + dx, FLOOR + 2, c.getZ() + dz);
                    BlockState st = level.getBlockState(m);
                    if (st.isAir()) continue;
                    String n = name(st);
                    if (!n.startsWith("pg_ph") || !n.endsWith("_bl")) continue;
                    BlockPos bl = m.immutable();
                    Direction f = st.getValue(HorizontalDirectionalBlock.FACING);
                    Vec3 centre = Vec3.atCenterOf(bl).add(0, 0.5, 0)
                            .add(Vec3.atLowerCornerOf(f.getClockWise().getNormal()).scale(0.5))
                            .add(Vec3.atLowerCornerOf(f.getNormal()).scale(-0.4));
                    if (visible(p, centre)) now.add(bl);
                    else if (s.seen.contains(bl) && !n.startsWith("pg_phx")) retake(p, level, s, bl, f);
                }
            s.seen = now;
        }

        boolean visible(ServerPlayer p, Vec3 point) {
            Vec3 eye = p.getEyePosition();
            Vec3 to = point.subtract(eye);
            if (to.lengthSqr() > 14 * 14) return false;
            if (p.getViewVector(1f).dot(to.normalize()) < 0.55) return false;
            BlockHitResult hit = p.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) < 1.0;
        }

        /** The photograph you just looked away from is taken again, of you, a little later. */
        void retake(ServerPlayer p, ServerLevel level, St s, BlockPos bl, Direction f) {
            if (p.getRandom().nextFloat() > 0.7f) return;
            s.changes++;
            if (s.changes % 2 == 0 && s.stage < 5) s.stage++;
            if (s.stage == 0) s.stage = 1;
            String base = "pg_ph" + s.stage + "_";
            BlockPos br = bl.relative(f.getClockWise());
            set(level, bl, base + "bl", f);
            set(level, bl.above(), base + "tl", f);
            set(level, br, base + "br", f);
            set(level, br.above(), base + "tr", f);
            if (s.stage >= 5) flash(p, level, s);
        }

        void set(ServerLevel level, BlockPos pos, String n, Direction f) {
            BlockState cur = level.getBlockState(pos);
            if (!name(cur).startsWith("pg_ph")) return;
            level.setBlock(pos, ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
        }

        /** The last photograph is the back of your head. Then the flash. Then it is right there. */
        void flash(ServerPlayer p, ServerLevel level, St s) {
            if (level.getEntity(s.photographer) instanceof PgPhotographer) return;
            level.playSound(null, p.blockPosition(), ModSounds.get("event.photo_gallery_charge"), SoundSource.HOSTILE, 0.8f, 1f);
            for (int attempt = 0; attempt < 10; attempt++) {
                double ang = Math.toRadians(p.getYRot() + 180 + (p.getRandom().nextFloat() - 0.5f) * 80);
                double d = 3 + p.getRandom().nextDouble() * 4;
                PgPhotographer g = PgPhotographer.TYPE.create(level);
                if (g == null) return;
                g.moveTo(p.getX() - Math.sin(ang) * d, FLOOR + 1, p.getZ() + Math.cos(ang) * d, p.getYRot(), 0f);
                if (!level.noCollision(g)) continue;
                level.addFreshEntity(g);
                s.photographer = g.getId();
                s.stage = 2;
                return;
            }
        }
    }

    /** Called by the Photographer when it reaches someone. */
    public static void photographed(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), ModSounds.get("event.photo_gallery_shutter"), SoundSource.HOSTILE, 1.2f, 1f);
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1.0f));
        com.noclip.sanity.Sanity.add(p, -15f);
        p.hurt(level.damageSources().magic(), 3f);
    }

    // ---------------------------------------------------------------- the rooms

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("pg_wall");
        final BlockState rail = ModBlocks.state("pg_wall_rail");
        final BlockState skirt = ModBlocks.state("pg_skirting");
        final BlockState floor = ModBlocks.state("pg_floor");
        final BlockState ceiling = ModBlocks.state("pg_ceiling");
        final BlockState spot = ModBlocks.state("pg_spot");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 12, 3);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, FLOOR, z, floor);
                    w.set(x, CEIL, z, ceiling);
                    if (isWall(s, g, x, z)) {
                        w.set(x, FLOOR + 1, z, skirt);
                        for (int y = FLOOR + 2; y <= FLOOR + 4; y++) w.set(x, y, z, wall);
                        w.set(x, FLOOR + 5, z, rail);
                    }
                }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> room(w, s, g, i, k, x0, z0, wd, dp));
        }

        /** Doorway on the west (axis 0) or north (axis 1) edge of a cell: its centre, or -1. */
        static int doorway(long s, int i, int k, int axis, int len) {
            return Hash.chance(s, i, k, 0x60 + axis, 0.68f) ? len / 2 : -1;
        }

        boolean isWall(long s, CellGrid g, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int u = x - g.x0(i), v = z - g.z0(k);
            if (u == 0 && v == 0) return true;
            if (u == 0) {
                int d = doorway(s, i, k, 0, g.depth(k));
                return d < 0 || Math.abs(v - d) > 1;
            }
            if (v == 0) {
                int d = doorway(s, i, k, 1, g.width(i));
                return d < 0 || Math.abs(u - d) > 1;
            }
            return false;
        }

        BlockState f(String n, Direction d) {
            return ModBlocks.state(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void room(ChunkWriter w, long s, CellGrid g, int i, int k, int x0, int z0, int wd, int dp) {
            int F = FLOOR;
            // spotlights on the ceiling
            w.set(x0 + wd / 2, CEIL, z0 + dp / 2, spot);
            if (wd > 11) w.set(x0 + wd / 2 - 3, CEIL, z0 + dp / 2, spot);
            if (Hash.chance(s, i, k, 0xBE, 0.3f))
                w.set(x0 + wd / 2, F + 1, z0 + dp / 2 + 1, f("pg_bench", Hash.chance(s, i, k, 0xBF, 0.5f) ? Direction.NORTH : Direction.EAST));
            int exitSlot = Hash.chance(s, i, k, 0xE7, 0.1f) ? Hash.range(s, i, k, 0xE8, 6) : -1;
            int slot = 0;
            // four walls: west, east, north, south
            int dW = doorway(s, i, k, 0, dp), dE = doorway(s, i + 1, k, 0, dp);
            int dN = doorway(s, i, k, 1, wd), dS = doorway(s, i, k + 1, 1, wd);
            for (int wall = 0; wall < 4; wall++) {
                boolean alongZ = wall < 2;
                int len = alongZ ? dp : wd;
                int door = wall == 0 ? dW : wall == 1 ? dE : wall == 2 ? dN : dS;
                Direction face = wall == 0 ? Direction.WEST : wall == 1 ? Direction.EAST : wall == 2 ? Direction.NORTH : Direction.SOUTH;
                for (int t = 2; t + 1 <= len - 2; t += 4) {
                    if (door >= 0 && t + 1 >= door - 2 && t <= door + 2) continue;
                    int ax, az, bx, bz;
                    // first block of the pair and the second, in world terms
                    if (alongZ) {
                        int x = wall == 0 ? x0 + 1 : x0 + wd - 1;
                        ax = x; bx = x; az = z0 + t; bz = z0 + t + 1;
                    } else {
                        int z = wall == 2 ? z0 + 1 : z0 + dp - 1;
                        az = z; bz = z; ax = x0 + t; bx = x0 + t + 1;
                    }
                    // which of the pair is on the viewer's left
                    boolean firstIsLeft = face == Direction.NORTH || face == Direction.EAST;
                    int lx = firstIsLeft ? ax : bx, lz = firstIsLeft ? az : bz;
                    int rx = firstIsLeft ? bx : ax, rz = firstIsLeft ? bz : az;
                    String ph = slot == exitSlot ? "pg_phx_" : Hash.chance(s, ax, az, 0x5A, 0.15f) ? "pg_ph1_" : "pg_ph0_";
                    slot++;
                    w.set(lx, F + 2, lz, f(ph + "bl", face));
                    w.set(lx, F + 3, lz, f(ph + "tl", face));
                    w.set(rx, F + 2, rz, f(ph + "br", face));
                    w.set(rx, F + 3, rz, f(ph + "tr", face));
                }
            }
        }
    }
}
