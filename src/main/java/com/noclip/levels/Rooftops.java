package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Rooftops. Flat roofs at night as far as you can see, separated by alleys with no
 * bottom: tar and gravel, parapets, condenser fans turning, wooden water tanks on legs,
 * antennae blinking red, a lit skylight here and there. Jump the gaps. The wind comes in
 * gusts, and you hear each one coming a couple of seconds before it shoves you. Most roof
 * doors are alarmed and locked; the ones that say EXIT are not.
 */
public final class Rooftops {
    static final String ID = "rooftops";
    static final int BASE = 44, PIT = 8;

    private Rooftops() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Rooftops").order(3180)
                .tagline("Jump the gaps. When you hear the wind coming, get low or get behind something.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Wind())
                .visuals(new Visuals().tint(1.04f, 0.97f, 1.04f).saturation(0.75f).contrast(1.04f).brightness(1.55f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.5f).fog(8f, 90f).fogColor(0x1A1418).bloom(0.85f).sky())
                .sanityDrain(0.03f)
                .spawnY(BASE + 1, BASE + 2)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.14f).seen().dist(24, 44).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.15f).dist(12, 26).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.3f).dark().dist(12, 24).weight(1))
                .exit(ExitType.DOOR, "level_11", 2).exit(ExitType.DOOR, "stairwell", 2).exit(ExitType.DOOR, "level_13", 1)
                .exit(ExitType.FALL, "rain_city", 2).exit(ExitType.FALL, "level_11", 1)
                .exit(ExitType.NOCLIP, "level_11", 2).exit(ExitType.NOCLIP, "sky_rooms", 1).exit(ExitType.NOCLIP, "construction_site", 1)
                .build();
    }

    static CellGrid grid(long s) {
        return new CellGrid(s, 16, 7);
    }

    static int height(CellGrid g, int i, int k) {
        float u = g.unit(i, k, 1);
        return u < 0.07f ? BASE + 7 : BASE + (u < 0.55f ? 0 : 1);
    }

    static int gapX(CellGrid g, int i, int k) {
        return 1 + Hash.range(g.hash(i, k, 2), 3);
    }

    static int gapZ(CellGrid g, int i, int k) {
        return 1 + Hash.range(g.hash(i, k, 3), 3);
    }

    static final class Gen implements LevelGenerator {
        final BlockState brick = ModBlocks.state("roof_brick");
        final BlockState brickB = ModBlocks.state("roof_brick_b");
        final BlockState window = ModBlocks.state("roof_window");
        final BlockState windowLit = ModBlocks.state("roof_window_lit");
        final BlockState tar = ModBlocks.state("roof_tar");
        final BlockState parapet = ModBlocks.state("roof_parapet");
        final BlockState parapetB = ModBlocks.state("roof_parapet_b");
        final BlockState ac = ModBlocks.state("roof_ac");
        final BlockState tank = ModBlocks.state("roof_tank");
        final BlockState tankCap = ModBlocks.state("roof_tank_cap");
        final BlockState leg = ModBlocks.state("roof_leg");
        final BlockState antenna = ModBlocks.state("roof_antenna");
        final BlockState beacon = ModBlocks.state("roof_beacon");
        final BlockState locked = ModBlocks.state("roof_door_locked");
        final BlockState door = ModBlocks.state("roof_door");
        final BlockState lamp = ModBlocks.state("roof_lamp");
        final BlockState skylight = ModBlocks.state("roof_skylight");
        final BlockState plank = ModBlocks.state("roof_plank");
        final BlockState dish = ModBlocks.state("roof_dish");
        final BlockState chair = ModBlocks.state("roof_chair");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, PIT - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, g, s, x, z);
        }

        void column(ChunkWriter w, CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int lx = x - g.x0(i), lz = z - g.z0(k);
            int wd = g.width(i), dp = g.depth(k);
            int gx = gapX(g, i, k), gz = gapZ(g, i, k);
            int h = height(g, i, k);
            boolean bLook = g.unit(i, k, 4) < 0.4f;
            if (lx < gx || lz < gz) {
                w.set(x, PIT, z, pit);
                // a plank across the alley, where two roofs line up
                int bz = g.z0(k) + gz + (dp - gz) / 2, bx = g.x0(i) + gx + (wd - gx) / 2;
                if (lx < gx && lz >= gz && z == bz && g.unit(i, k, 5) < 0.35f && height(g, i - 1, k) == h) w.set(x, h, z, plank.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
                if (lz < gz && lx >= gx && x == bx && g.unit(i, k, 6) < 0.35f && height(g, i, k - 1) == h) w.set(x, h, z, plank);
                return;
            }
            boolean edgeW = lx == gx, edgeE = lx == wd - 1, edgeN = lz == gz, edgeS = lz == dp - 1;
            boolean edge = edgeW || edgeE || edgeN || edgeS;
            // the building below: brick, rows of windows on the alley faces, most dark
            for (int y = PIT + 1; y < h; y++) {
                if (!edge) { w.set(x, y, z, sub); continue; }
                boolean win = y % 3 == 1 && ((edgeW || edgeE) ? Math.floorMod(z, 3) == 1 : Math.floorMod(x, 3) == 1);
                if (win) w.set(x, y, z, Hash.unit(s, x * 31L + y, z, 7) < 0.05f ? windowLit : window);
                else w.set(x, y, z, bLook ? brickB : brick);
            }
            w.set(x, h, z, tar);
            if (edge) {
                int bz = g.z0(k) + gz + (dp - gz) / 2, bx = g.x0(i) + gx + (wd - gx) / 2;
                boolean bridgeEnd = (edgeW && z == bz) || (edgeN && x == bx) || (edgeE && z == g.z0(k) + gapZ(g, i + 1, k) + (dp - gapZ(g, i + 1, k)) / 2)
                        || (edgeS && x == g.x0(i) + gapX(g, i, k + 1) + (wd - gapX(g, i, k + 1)) / 2);
                if (!bridgeEnd && Hash.unit(s, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 8) > 0.22f) w.set(x, h + 1, z, bLook ? parapetB : parapet);
                return;
            }
            roof(w, g, s, x, z, i, k, lx, lz, wd, dp, gx, gz, h);
        }

        void roof(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int lx, int lz, int wd, int dp, int gx, int gz, int h) {
            int iw = wd - gx, id = dp - gz;
            // the stair bulkhead: a brick box with a door, a lamp over it
            if (g.unit(i, k, 10) < 0.4f && iw >= 8 && id >= 8 && lx >= gx + 2 && lx <= gx + 4 && lz >= gz + 2 && lz <= gz + 4) {
                for (int y = h + 1; y <= h + 3; y++) w.set(x, y, z, brick);
                w.set(x, h + 4, z, tar);
                if (lx == gx + 3 && lz == gz + 4) {
                    boolean open = g.unit(i, k, 11) < 0.2f;
                    w.set(x, h + 1, z, open ? door : locked);
                    w.set(x, h + 2, z, open ? door : locked);
                    w.set(x, h + 3, z + 1, OutsideKit.facing(lamp, Direction.SOUTH));
                }
                return;
            }
            // a wooden water tank on four legs
            int cx = g.x0(i) + gx + iw / 2, cz = g.z0(k) + gz + id / 2;
            if (g.unit(i, k, 12) < 0.18f && iw >= 11 && id >= 11 && Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) {
                boolean corner = Math.abs(x - cx) == 1 && Math.abs(z - cz) == 1;
                if (corner) w.fill(x, h + 1, z, x, h + 4, z, leg);
                w.fill(x, h + 5, z, x, h + 8, z, tank);
                if (x == cx && z == cz) w.set(x, h + 9, z, tankCap);
                return;
            }
            // an antenna in the far corner
            if (g.unit(i, k, 13) < 0.25f && lx == wd - 3 && lz == dp - 3) {
                int len = 6 + Hash.range(g.hash(i, k, 14), 9);
                w.fill(x, h + 1, z, x, h + len, z, antenna);
                w.set(x, h + len + 1, z, beacon);
                return;
            }
            // a skylight, lit from inside
            if (g.unit(i, k, 15) < 0.2f && lx >= gx + 2 && lx <= gx + 3 && lz >= dp - 5 && lz <= dp - 4) {
                w.set(x, h, z, skylight);
                return;
            }
            float r = Hash.unit(s, x, z, 16);
            if (r < 0.03f) w.set(x, h + 1, z, OutsideKit.facing(ac, Direction.from2DDataValue(Hash.range(Hash.hash(s, x, z, 17), 4))));
            else if (r < 0.034f) w.set(x, h + 1, z, OutsideKit.facing(dish, Direction.from2DDataValue(Hash.range(Hash.hash(s, x, z, 18), 4))));
            else if (r < 0.0352f) {
                // a lawn chair, facing the drop
                Direction f = lx - gx < 3 ? Direction.WEST : lz - gz < 3 ? Direction.NORTH : lx > wd - 4 ? Direction.EAST : Direction.SOUTH;
                w.set(x, h + 1, z, OutsideKit.facing(chair, f));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Gusts: heard two seconds before they arrive, pushing everyone in the open one way. */
    static final class Wind implements LevelBehavior {
        /** {phase 0 calm / 1 rising / 2 gust, phase end, next gust, direction(radians*1000)} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 600 + level.random.nextInt(600), 0});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0 && now >= st[2] && !level.players().isEmpty()) {
                st[0] = 1;
                st[1] = now + 45;
                st[3] = (long) (level.random.nextDouble() * Math.PI * 2000);
                for (ServerPlayer p : level.players())
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.rooftops_gust"), SoundSource.WEATHER, 1.5f, 1f);
            } else if (st[0] == 1 && now >= st[1]) {
                st[0] = 2;
                st[1] = now + 40;
            } else if (st[0] == 2 && now >= st[1]) {
                st[0] = 0;
                st[2] = now + 500 + level.random.nextInt(900);
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            if (st[0] != 2 || p.isSpectator() || p.tickCount % 2 != 0) return;
            if (!level.canSeeSky(p.blockPosition().above())) return;
            double ang = st[3] / 1000.0;
            double force = p.isShiftKeyDown() ? 0.02 : 0.075;
            // something solid upwind is shelter
            Vec3 up = new Vec3(-Math.cos(ang), 0, -Math.sin(ang));
            if (!level.getBlockState(p.blockPosition().offset((int) Math.round(up.x), 0, (int) Math.round(up.z))).isAir()) force *= 0.2;
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x + Math.cos(ang) * force, v.y, v.z + Math.sin(ang) * force);
            p.hurtMarked = true;
            if (p.tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, p.getX() - Math.cos(ang) * 3, p.getY() + 1, p.getZ() - Math.sin(ang) * 3, 3, 1, 0.5, 1, 0.1);
                if (st[1] - level.getGameTime() > 30)
                    p.displayClientMessage(Component.literal("The wind leans on you.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }
}
