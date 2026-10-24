package com.noclip.levels;

import com.noclip.block.VentFanBlock;
import com.noclip.entity.DuctCrawler;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Air Vents. A maze of galvanized ducts one block high: you crawl everywhere.
 * Booster fans strobe light through their blades and cut whatever passes while
 * they spin. Every few minutes the air handlers kick on and the whole system
 * blows, pushing you down the ducts. The Duct Crawler hunts in the dark between
 * the fans and will not follow you into their light.
 */
public final class AirVents {
    static final int FLOOR = 20, AIR = 21, CEIL = 22;

    private AirVents() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("air_vents")
                .number("").title("Air Vents").order(2110)
                .tagline("One block high. Keep crawling. Stay in the fan light.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Airflow())
                .visuals(new Visuals().darkness(0.5f).tint(0.95f, 1.0f, 1.05f).saturation(0.7f).contrast(1.1f)
                        .grain(0.1f).aberration(0.0022f).vignette(0.78f).warp(0.05f).fog(1.5f, 20f).fogColor(0x0B0C0D)
                        .bloom(0.6f).flicker(0.03f))
                .sanityDrain(0.05f)
                .spawnY(AIR, AIR)
                .exit(ExitType.NOCLIP, "level_2", 3).exit(ExitType.NOCLIP, "level_3", 1).exit(ExitType.NOCLIP, "server_farm", 1)
                .exit(ExitType.FALL, "level_0", 3).exit(ExitType.FALL, "level_4", 1).exit(ExitType.FALL, "sewers", 1)
                .exit(ExitType.DOOR, "boiler_room", 2).exit(ExitType.DOOR, "server_farm", 2).exit(ExitType.DOOR, "level_2", 2)
                .build();
    }

    // ---------------------------------------------------------------- layout

    static boolean trunkAlongX(int z) {
        int m = Math.floorMod(z, 32);
        return m == 0 || m == 1;
    }

    static boolean trunkAlongZ(int x) {
        int m = Math.floorMod(x, 32);
        return m == 0 || m == 1;
    }

    /** Grille room cells: a duct segment on line z=4k between x=4i..4i+4 that looks down into a lit room. */
    static boolean grilleSeg(long s, int i, int k) {
        return Math.floorMod(i, 4) == 1 && Math.floorMod(k, 4) == 1
                && Hash.chance(s, Math.floorDiv(i, 4), Math.floorDiv(k, 4), 77, 0.06f);
    }

    /** Segment on line x=4i between z=4k..4k+4. */
    static boolean segZ(long s, int i, int k) {
        return Hash.chance(s, i, k, 11, 0.55f);
    }

    /** Segment on line z=4k between x=4i..4i+4. */
    static boolean segX(long s, int i, int k) {
        return grilleSeg(s, i, k) || Hash.chance(s, i, k, 12, 0.55f);
    }

    static boolean plenumCell(long s, int pi, int pk) {
        return Hash.chance(s, pi, pk, 21, 0.5f) && !Hash.chance(s, pi, pk, 77, 0.06f);
    }

    /** Plenum bounds for a 16-cell: {x0, z0, x1, z1}. */
    static int[] plenum(long s, int pi, int pk) {
        int hx = 1 + Hash.range(s, pi, pk, 22, 2), hz = 1 + Hash.range(s, pi, pk, 23, 2);
        int cx = pi * 16 + 8, cz = pk * 16 + 8;
        return new int[]{cx - hx, cz - hz, cx + hx, cz + hz};
    }

    static boolean inPlenum(long s, int x, int z) {
        int pi = Math.floorDiv(x, 16), pk = Math.floorDiv(z, 16);
        if (!plenumCell(s, pi, pk)) return false;
        int[] r = plenum(s, pi, pk);
        return x >= r[0] && x <= r[2] && z >= r[1] && z <= r[3];
    }

    static boolean duct(long s, int x, int z) {
        if (trunkAlongX(z) || trunkAlongZ(x)) return true;
        if (inPlenum(s, x, z)) return true;
        boolean lx = Math.floorMod(x, 4) == 0, lz = Math.floorMod(z, 4) == 0;
        int i = Math.floorDiv(x, 4), k = Math.floorDiv(z, 4);
        if (lx && lz) return segZ(s, i, k) || segZ(s, i, k - 1) || segX(s, i, k) || segX(s, i - 1, k);
        if (lx) return segZ(s, i, k);
        if (lz) return segX(s, i, k);
        return false;
    }

    static boolean boosterFan(long s, int x, int z) {
        if (trunkAlongX(z) && !trunkAlongZ(x) && Math.floorMod(x, 40) == 20) return true;
        if (trunkAlongZ(x) && !trunkAlongX(z) && Math.floorMod(z, 40) == 20) return true;
        if (trunkAlongX(z) || trunkAlongZ(x) || inPlenum(s, x, z)) return false;
        boolean lx = Math.floorMod(x, 4) == 0, lz = Math.floorMod(z, 4) == 0;
        if (lz && Math.floorMod(x, 4) == 2 && segX(s, Math.floorDiv(x, 4), Math.floorDiv(z, 4))
                && !grilleSeg(s, Math.floorDiv(x, 4), Math.floorDiv(z, 4)))
            return Hash.chance(s, x, z, 31, 0.06f);
        if (lx && Math.floorMod(z, 4) == 2 && segZ(s, Math.floorDiv(x, 4), Math.floorDiv(z, 4)))
            return Hash.chance(s, x, z, 31, 0.06f);
        return false;
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("av_duct_floor");
        final BlockState floorHands = ModBlocks.state("av_duct_floor_hands");
        final BlockState floorDrag = ModBlocks.state("av_duct_floor_drag");
        final BlockState floorFan = ModBlocks.state("av_floor_fan");
        final BlockState wall = ModBlocks.state("av_duct_wall");
        final BlockState dented = ModBlocks.state("av_duct_dented");
        final BlockState rusted = ModBlocks.state("av_duct_rusted");
        final BlockState scratched = ModBlocks.state("av_duct_scratched");
        final BlockState flex = ModBlocks.state("av_flex_duct");
        final BlockState[] messages = {ModBlocks.state("av_duct_msg_stop"), ModBlocks.state("av_duct_msg_light"),
                ModBlocks.state("av_duct_msg_hears")};
        final BlockState ceiling = ModBlocks.state("av_duct_ceiling");
        final BlockState lamp = ModBlocks.state("av_ceiling_light");
        final BlockState ceilingFan = ModBlocks.state("av_ceiling_fan");
        final BlockState louver = ModBlocks.state("av_louver");
        final BlockState fan = ModBlocks.state("av_vent_fan");
        final BlockState grille = ModBlocks.state("av_floor_grille");
        final BlockState panel = ModBlocks.state("av_access_panel");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState carpet = ModBlocks.state("moist_carpet");
        final BlockState paper = ModBlocks.state("yellow_wallpaper");
        final BlockState base = ModBlocks.state("yellow_wallpaper_baseboard");
        final BlockState tile = ModBlocks.state("ceiling_tile");
        final BlockState tube = ModBlocks.state("fluorescent_light");
        final BlockState chair = ModBlocks.state("office_chair");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 4, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean d = duct(s, x, z);
                    float r = Hash.unit(s, x, z, 5);
                    w.set(x, FLOOR, z, r < 0.02f ? floorHands : r < 0.026f ? floorDrag : floor);
                    w.set(x, CEIL, z, ceiling);
                    if (d) {
                        w.set(x, AIR, z, boosterFan(s, x, z) ? fan : ModBlocks.AIR);
                        boolean node = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
                        if (node && Hash.chance(s, x, z, 6, 0.09f)) w.set(x, CEIL, z, lamp);
                    } else {
                        w.set(x, AIR, z, wallAt(s, x, z));
                    }
                }
            }
            deadEnds(w, s);
            plenums(w, s);
            grilleRooms(w, s);
        }

        BlockState wallAt(long s, int x, int z) {
            float zone = Hash.fbm(s ^ 0xF1E7, x / 24.0, z / 24.0, 2);
            if (zone > 0.72f) return flex;
            float r = Hash.unit(s, x, z, 7);
            if (r < 0.014f) return louver;
            if (r < 0.04f) return dented;
            if (r < 0.055f) return scratched;
            float wet = Hash.fbm(s ^ 0x7057, x / 30.0, z / 30.0, 2);
            if (wet > 0.66f && r < 0.4f) return rusted;
            return wall;
        }

        /** Dead ends get a fan in the end wall, an access panel, a scrawl, or a bottle. */
        void deadEnds(ChunkWriter w, long s) {
            int x0 = w.minX - 1 - Math.floorMod(w.minX - 1, 4);
            int z0 = w.minZ - 1 - Math.floorMod(w.minZ - 1, 4);
            for (int x = x0; x <= w.maxX + 1; x += 4) {
                for (int z = z0; z <= w.maxZ + 1; z += 4) {
                    if (!duct(s, x, z) || inPlenum(s, x, z)) continue;
                    int open = 0;
                    Direction only = null;
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        if (duct(s, x + dir.getStepX(), z + dir.getStepZ())) {
                            open++;
                            only = dir;
                        }
                    }
                    if (open != 1) continue;
                    Direction back = only.getOpposite();
                    int ex = x + back.getStepX(), ez = z + back.getStepZ();
                    float r = Hash.unit(s, x, z, 41);
                    if (r < 0.62f) w.set(ex, AIR, ez, fan);
                    else if (r < 0.67f) w.set(ex, AIR, ez, panel);
                    else if (r < 0.75f) w.set(ex, AIR, ez, messages[Hash.range(s, x, z, 42, messages.length)]);
                    else if (r < 0.80f) w.set(x, AIR, z, bottle.setValue(HorizontalDirectionalBlock.FACING, only));
                }
            }
        }

        void plenums(ChunkWriter w, long s) {
            for (int pi = Math.floorDiv(w.minX, 16) - 1; pi <= Math.floorDiv(w.maxX, 16) + 1; pi++) {
                for (int pk = Math.floorDiv(w.minZ, 16) - 1; pk <= Math.floorDiv(w.maxZ, 16) + 1; pk++) {
                    if (!plenumCell(s, pi, pk)) continue;
                    int[] r = plenum(s, pi, pk);
                    int cx = pi * 16 + 8, cz = pk * 16 + 8;
                    long h = Hash.hash(s, pi, pk, 24);
                    int type = Hash.range(h, 3);
                    if (type == 0) {
                        // floor fans under grating: light pours up through the blades
                        for (int x = cx - 1; x <= cx + 1; x++)
                            for (int z = cz - 1; z <= cz + 1; z++) w.set(x, FLOOR, z, floorFan);
                    } else if (type == 1) {
                        w.set(cx, CEIL, cz, lamp);
                    }
                    // a short riser in one corner, with a fan at the top: the only place you could stand
                    int rx = r[0], rz = r[1];
                    w.set(rx, CEIL, rz, ModBlocks.AIR);
                    w.set(rx, CEIL + 1, rz, ceilingFan);
                    for (Direction dir : Direction.Plane.HORIZONTAL)
                        w.set(rx + dir.getStepX(), CEIL + 1, rz + dir.getStepZ(), wall);
                    if (Hash.unit(h >>> 20) < 0.18f) {
                        w.set(r[2], AIR, r[3], supply.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    }
                }
            }
        }

        /** A grate in the duct floor looking down into a small lit office nobody can reach. */
        void grilleRooms(ChunkWriter w, long s) {
            for (int pi = Math.floorDiv(w.minX, 16) - 1; pi <= Math.floorDiv(w.maxX, 16) + 1; pi++) {
                for (int pk = Math.floorDiv(w.minZ, 16) - 1; pk <= Math.floorDiv(w.maxZ, 16) + 1; pk++) {
                    if (!Hash.chance(s, pi, pk, 77, 0.06f)) continue;
                    int gx = pi * 16 + 6, gz = pk * 16 + 4;
                    int fy = FLOOR - 6;
                    for (int x = gx - 2; x <= gx + 2; x++) {
                        for (int z = gz - 2; z <= gz + 2; z++) {
                            boolean edge = x == gx - 2 || x == gx + 2 || z == gz - 2 || z == gz + 2;
                            w.set(x, fy, z, carpet);
                            for (int y = fy + 1; y < FLOOR; y++) {
                                w.set(x, y, z, edge ? (y == fy + 1 ? base : paper) : ModBlocks.AIR);
                            }
                            if (!edge) w.set(x, FLOOR, z, z == gz ? grille : (x == gx && z == gz + 1 ? tube : tile));
                        }
                    }
                    long h = Hash.hash(s, pi, pk, 78);
                    w.set(gx + (Hash.range(h, 3) - 1), fy + 1, gz + 1,
                            chair.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> 8, 4))));
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Fan cycles, air surges, the forced crawl, and the thing in the ducts. */
    static final class Airflow implements LevelBehavior {
        private static final Map<ResourceKey<Level>, long[]> SURGE = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return SURGE.computeIfAbsent(level.dimension(),
                    k -> new long[]{level.getGameTime() + 900 + level.random.nextInt(1200), 0, 0});
        }

        static boolean surging(ServerLevel level) {
            long[] st = state(level);
            return level.getGameTime() < st[1];
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (now >= st[0]) {
                st[1] = now + 220;
                st[2] = level.random.nextInt(4);
                st[0] = st[1] + 1400 + level.random.nextInt(2000);
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.air_vents_surge"), SoundSource.AMBIENT, 1.4f, 1f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.6f));
                    if (!p.isCreative() && !p.isSpectator() && level.random.nextFloat() < 0.6f) {
                        Direction wind = Direction.from2DDataValue((int) st[2]);
                        spawnCrawler(p, level, wind.getOpposite(), 14, 26);
                    }
                }
            }
        }

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            crawl(p, level);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            if (p.tickCount % 5 == 0) crawl(p, level);
            boolean surge = surging(level);
            if (surge && p.getY() < AIR + 1.5) {
                Direction wind = Direction.from2DDataValue((int) state(level)[2]);
                BlockPos ahead = BlockPos.containing(p.getX() + wind.getStepX() * 0.8, AIR, p.getZ() + wind.getStepZ() * 0.8);
                if (level.getBlockState(ahead).getCollisionShape(level, ahead).isEmpty()) {
                    p.push(wind.getStepX() * 0.035, 0, wind.getStepZ() * 0.035);
                    p.hurtMarked = true;
                }
                if (p.tickCount % 3 == 0) {
                    for (int n = 0; n < 3; n++) {
                        double back = 1 + p.getRandom().nextDouble() * 5;
                        level.sendParticles(p, ParticleTypes.POOF, false,
                                p.getX() - wind.getStepX() * back + (p.getRandom().nextDouble() - 0.5) * 0.8,
                                AIR + 0.2 + p.getRandom().nextDouble() * 0.6,
                                p.getZ() - wind.getStepZ() * back + (p.getRandom().nextDouble() - 0.5) * 0.8,
                                0, wind.getStepX(), 0, wind.getStepZ(), 0.35);
                    }
                }
            }
            if ((p.tickCount + p.getId()) % 10 == 0) cycleFans(p, level, surge);
            if ((p.tickCount + p.getId()) % 100 == 0 && !p.isCreative() && level.random.nextFloat() < 0.22f) {
                int near = level.getEntities(DuctCrawler.TYPE, p.getBoundingBox().inflate(48), e -> true).size();
                if (near < 2) spawnCrawler(p, level, null, 14, 30);
            }
        }

        /** Nobody stands up in the ducts: anyone standing in a riser is put back into a 1-high duct. */
        private static void crawl(ServerPlayer p, ServerLevel level) {
            if (p.getPose() != Pose.STANDING && p.getPose() != Pose.CROUCHING) return;
            if (p.getAbilities().flying || p.getY() < AIR - 0.5 || p.getY() > CEIL + 2) return;
            BlockPos at = p.blockPosition();
            Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            int start = p.getRandom().nextInt(4);
            for (int n = 0; n < 4; n++) {
                Direction d = dirs[(start + n) % 4];
                BlockPos c = new BlockPos(at.getX() + d.getStepX(), AIR, at.getZ() + d.getStepZ());
                if (level.getBlockState(c).isAir() && !level.getBlockState(c.above()).isAir()) {
                    p.connection.teleport(c.getX() + 0.5, AIR, c.getZ() + 0.5, d.toYRot(), p.getXRot());
                    return;
                }
            }
        }

        private static void cycleFans(ServerPlayer p, ServerLevel level, boolean surge) {
            long t = level.getGameTime() / 20;
            int px = p.getBlockX(), pz = p.getBlockZ();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = px - 20; x <= px + 20; x++) {
                for (int z = pz - 20; z <= pz + 20; z++) {
                    m.set(x, AIR, z);
                    BlockState st = level.getBlockState(m);
                    if (!(st.getBlock() instanceof VentFanBlock)) continue;
                    int phase = (int) Math.floorMod(t + Hash.range(Hash.hash(31, x, z), 14), 14L);
                    boolean on = surge || phase < 9;
                    if (st.getValue(VentFanBlock.LIT) != on) {
                        level.setBlock(m, st.setValue(VentFanBlock.LIT, on), Block.UPDATE_CLIENTS);
                        if (on && m.distSqr(p.blockPosition()) < 144)
                            level.playSound(null, m, ModSounds.get("event.air_vents_fan"), SoundSource.BLOCKS, 0.7f,
                                    0.9f + level.random.nextFloat() * 0.2f);
                    }
                }
            }
        }

        /** Puts a crawler in a dark duct out of the player's sight, upwind if a direction is given. */
        static void spawnCrawler(ServerPlayer p, ServerLevel level, Direction from, int min, int max) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double ang = from != null ? Math.toRadians(from.toYRot()) + (level.random.nextDouble() - 0.5) * 0.9
                        : Math.toRadians(p.getYRot() + 180 + (level.random.nextDouble() - 0.5) * 200);
                double dist = min + level.random.nextDouble() * (max - min);
                BlockPos c = BlockPos.containing(p.getX() - Math.sin(ang) * dist, AIR, p.getZ() + Math.cos(ang) * dist);
                if (!level.isLoaded(c) || !level.getBlockState(c).isAir() || level.getBlockState(c.above()).isAir()) continue;
                if (level.getBrightness(LightLayer.BLOCK, c) >= 7) continue;
                Vec3 at = Vec3.atCenterOf(c);
                if (p.level().clip(new ClipContext(p.getEyePosition(), at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))
                        .getType() == HitResult.Type.MISS) continue;
                DuctCrawler e = DuctCrawler.TYPE.create(level);
                if (e == null) return;
                e.moveTo(c.getX() + 0.5, AIR, c.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
                e.finalizeSpawn(level, level.getCurrentDifficultyAt(c), MobSpawnType.EVENT, null);
                level.addFreshEntity(e);
                return;
            }
        }
    }
}
