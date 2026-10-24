package com.noclip.levels;

import com.noclip.entity.RecResident;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Recursion. The same sitting room, over and over: green velvet, a ticking
 * clock, a painting of a house on a hill. Every door leads into the room again,
 * and every time you pass through one, one more thing in it is wrong. The only
 * way out is through the wall where the newest change happened. After twelve
 * rooms it gives up and starts over, clean, as if you had never noticed anything.
 *
 * Rooms sit on a 24-block grid, sealed in nothing. Room (i, k) has depth
 * i mod 13; chain k has its own order of changes, shuffled tier by tier so
 * small things go wrong first and the faces come last.
 */
public final class Recursion {
    static final int FLOOR = 8, CEIL = FLOOR + 5, SPACING = 24, CYCLE = 13, SIZE = 14;
    /** Walls: 0 north, 1 east, 2 south, 3 west. Slots are tier * 4 + wall. */
    static final String[] WALL_NAMES = {"north", "east", "south", "west"};

    private Recursion() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("recursion")
                .number("").title("Recursion").order(4000)
                .tagline("One thing changes every time. Leave by its wall.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Rooms())
                .visuals(new Visuals().tint(1.06f, 0.98f, 0.86f).saturation(0.8f).contrast(1.06f).brightness(1.08f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.62f).fog(6f, 40f).fogColor(0x2A2016).bloom(0.45f))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.DOOR, "level_0", 3).exit(ExitType.DOOR, "long_hallway", 2)
                .exit(ExitType.DOOR, "room_of_doors", 2).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "mirror_level", 1)
                .exit(ExitType.NOCLIP, "red_room", 1)
                .build();
    }

    // ---------------------------------------------------------------- the rules

    static int depth(int i) {
        return Math.floorMod(i, CYCLE);
    }

    /** Slot changed at each step of this chain's cycle (perm[0] is the change seen in room 1). */
    static int[] order(long seed, int cycle, int k) {
        int[] perm = new int[12];
        for (int tier = 0; tier < 3; tier++) {
            int[] walls = {0, 1, 2, 3};
            long h = Hash.hash(seed ^ 0x5EC0A5E, cycle, k, tier);
            for (int j = 3; j > 0; j--) {
                int r = Hash.range(Hash.hash(h, j), j + 1);
                int t = walls[j];
                walls[j] = walls[r];
                walls[r] = t;
            }
            for (int j = 0; j < 4; j++) perm[tier * 4 + j] = tier * 4 + walls[j];
        }
        return perm;
    }

    static boolean[] anomalies(long seed, int i, int k) {
        boolean[] on = new boolean[12];
        int d = depth(i);
        int[] perm = order(seed, Math.floorDiv(i, CYCLE), k);
        for (int n = 0; n < d && n < 12; n++) on[perm[n]] = true;
        return on;
    }

    /** The wall you must leave by in room i, or -1 in the first room. */
    static int exitWall(long seed, int i, int k) {
        int d = depth(i);
        if (d == 0) return -1;
        return order(seed, Math.floorDiv(i, CYCLE), k)[d - 1] % 4;
    }

    /** Called by the door blocks. Either the way out, or the next room. */
    public static void useDoor(ServerPlayer p, BlockPos pos) {
        if (Travel.onCooldown(p)) return;
        ServerLevel level = p.serverLevel();
        long seed = level.getSeed();
        int i = Math.floorDiv(pos.getX(), SPACING), k = Math.floorDiv(pos.getZ(), SPACING);
        int u = pos.getX() - i * SPACING, v = pos.getZ() - k * SPACING;
        int wall = v == 0 ? 0 : u == SIZE ? 1 : v == SIZE ? 2 : u == 0 ? 3 : -1;
        if (wall < 0) return;
        level.playSound(null, pos, ModSounds.get("event.recursion_door"), SoundSource.BLOCKS, 0.9f, 0.95f + p.getRandom().nextFloat() * 0.1f);
        if (wall == exitWall(seed, i, k)) {
            clearResidents(level, p);
            Travel.exit(p, ExitType.DOOR, pos);
            return;
        }
        int ni = i + 1;
        int nd = depth(ni);
        // arrive through the opposite door of the next room, still facing the same way
        int ox = ni * SPACING, oz = k * SPACING;
        double tx, tz;
        switch (wall) {
            case 0 -> { tx = ox + 7.5; tz = oz + 13.3; }
            case 1 -> { tx = ox + 1.7; tz = oz + 7.5; }
            case 2 -> { tx = ox + 7.5; tz = oz + 1.7; }
            default -> { tx = ox + 13.3; tz = oz + 7.5; }
        }
        clearResidents(level, p);
        p.teleportTo(level, tx, FLOOR + 1, tz, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        level.playSound(null, BlockPos.containing(tx, FLOOR + 1, tz), ModSounds.get("event.recursion_door"),
                SoundSource.BLOCKS, 0.7f, 0.9f);
        if (nd == 0) {
            level.playSound(null, p.blockPosition(), ModSounds.get("event.recursion_reset"), SoundSource.AMBIENT, 1f, 1f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.6f));
            Sanity.add(p, -8f);
        } else if (nd >= 6) {
            Sanity.add(p, -1.5f);
        }
        if (nd >= 5) spawnResident(level, p, ox, oz, tx, tz, nd);
    }

    static void clearResidents(ServerLevel level, ServerPlayer p) {
        for (RecResident r : level.getEntitiesOfClass(RecResident.class, new AABB(p.blockPosition()).inflate(40))) r.discard();
    }

    /** He was not here before. Every room, he is a little nearer the door you came in by. */
    static void spawnResident(ServerLevel level, ServerPlayer p, int ox, int oz, double tx, double tz, int d) {
        double cx = ox + 7.5, cz = oz + 7.5;
        // the far corner, diagonally across from where you came in
        double fx = cx + Math.signum(cx - tx) * 5.0 + (Math.abs(cx - tx) < 1 ? 5.0 : 0);
        double fz = cz + Math.signum(cz - tz) * 5.0 + (Math.abs(cz - tz) < 1 ? 5.0 : 0);
        double t = Math.min(1.0, (d - 5) / 6.0);
        // at the end of the cycle he stands right beside the door, just behind you
        double side = 1.4;
        double nearX = tx + (Math.abs(cz - tz) > 1 ? side : 0), nearZ = tz + (Math.abs(cx - tx) > 1 ? side : 0);
        double x = fx + (nearX - fx) * t, z = fz + (nearZ - fz) * t;
        RecResident r = RecResident.TYPE.create(level);
        if (r == null) return;
        for (int attempt = 0; attempt < 6; attempt++) {
            double ax = x + (attempt == 0 ? 0 : (p.getRandom().nextDouble() - 0.5) * 3);
            double az = z + (attempt == 0 ? 0 : (p.getRandom().nextDouble() - 0.5) * 3);
            ax = Math.max(ox + 1.5, Math.min(ox + 13.5, ax));
            az = Math.max(oz + 1.5, Math.min(oz + 13.5, az));
            r.moveTo(ax, FLOOR + 1, az, 0f, 0f);
            if (level.noCollision(r)) {
                r.lookAtPlayer(p);
                level.addFreshEntity(r);
                if (d >= 10) level.playSound(null, r.blockPosition(), ModSounds.get("entity.rec_resident.breath"), SoundSource.HOSTILE, 0.8f, 0.9f);
                return;
            }
        }
    }

    static final class Rooms implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            // you always wake in the first room of a cycle, the clean one
            int i = Math.floorDiv(p.getBlockX(), SPACING), k = Math.floorDiv(p.getBlockZ(), SPACING);
            int i0 = i - depth(i);
            clearResidents(level, p);
            String dbg = System.getenv("NOCLIP_REC_DEPTH");
            if (dbg != null) {
                int dd = Integer.parseInt(dbg);
                p.teleportTo(level, (i0 + dd) * SPACING + 7.5, FLOOR + 1, k * SPACING + 10.5, 180f, 0f);
                spawnResident(level, p, (i0 + dd) * SPACING, k * SPACING, (i0 + dd) * SPACING + 7.5, k * SPACING + 13.3, dd);
                return;
            }
            p.teleportTo(level, i0 * SPACING + 7.5, FLOOR + 1, k * SPACING + 10.5, 180f, 0f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            // late in the cycle the room presses on you
            if (p.tickCount % 20 != 0) return;
            int d = depth(Math.floorDiv(p.getBlockX(), SPACING));
            if (d >= 9 && p.getRandom().nextInt(12) == 0) {
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.25f + 0.05f * (d - 9)));
            }
        }
    }

    // ---------------------------------------------------------------- the room

    static final class Gen implements LevelGenerator {
        final BlockState paper = ModBlocks.state("rec_wallpaper");
        final BlockState wain = ModBlocks.state("rec_wainscot");
        final BlockState cornice = ModBlocks.state("rec_cornice");
        final BlockState parquet = ModBlocks.state("rec_parquet");
        final BlockState rug = ModBlocks.state("rec_rug");
        final BlockState rugBorder = ModBlocks.state("rec_rug_border");
        final BlockState ceiling = ModBlocks.state("rec_ceiling");
        final BlockState doorLow = ModBlocks.state("rec_door_low");
        final BlockState doorHigh = ModBlocks.state("rec_door_high");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, FLOOR, w.minZ, w.maxX, CEIL + 2, w.maxZ, filler);
            CellGrid g = new CellGrid(s, SPACING, 0);
            g.forCells(w, (i, k, x0, z0, wd, dp) -> room(w, s, i, k, x0, z0));
        }

        BlockState f(String name, Direction d) {
            return ModBlocks.state(name).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void room(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            if (x0 > w.maxX || x0 + SIZE < w.minX || z0 > w.maxZ || z0 + SIZE < w.minZ) return;
            boolean[] a = anomalies(s, i, k);
            int d = depth(i);
            int F = FLOOR;
            // shell
            for (int u = 0; u <= SIZE; u++) {
                for (int v = 0; v <= SIZE; v++) {
                    int x = x0 + u, z = z0 + v;
                    boolean wall = u == 0 || v == 0 || u == SIZE || v == SIZE;
                    boolean inRug = u >= 4 && u <= 10 && v >= 4 && v <= 10;
                    boolean rugEdge = inRug && (u == 4 || u == 10 || v == 4 || v == 10);
                    w.set(x, F, z, wall ? parquet : rugEdge ? rugBorder : inRug ? rug : parquet);
                    w.set(x, CEIL, z, ceiling);
                    if (wall) {
                        w.set(x, F + 1, z, wain);
                        w.set(x, F + 2, z, paper);
                        w.set(x, F + 3, z, paper);
                        w.set(x, F + 4, z, cornice);
                    } else {
                        for (int y = F + 1; y < CEIL; y++) w.set(x, y, z, ModBlocks.AIR);
                    }
                }
            }
            // a door in the middle of every wall
            int[][] doors = {{7, 0}, {SIZE, 7}, {7, SIZE}, {0, 7}};
            for (int[] dd : doors) {
                w.set(x0 + dd[0], F + 1, z0 + dd[1], doorLow);
                w.set(x0 + dd[0], F + 2, z0 + dd[1], doorHigh);
            }
            // things that never change
            w.set(x0 + 7, F + 1, z0 + 7, f("rec_side_table", Direction.NORTH));
            w.set(x0 + 7, F + 4, z0 + 7, f("rec_ceiling_light", Direction.NORTH));
            if (d == 0) {
                w.set(x0 + 6, F + 4, z0 + 1, f("rec_sampler_l", Direction.NORTH));
                w.set(x0 + 7, F + 4, z0 + 1, f("rec_sampler_m", Direction.NORTH));
                w.set(x0 + 8, F + 4, z0 + 1, f("rec_sampler_r", Direction.NORTH));
            }

            // ---- north wall: clock (0), bookshelf (4), painting (8)
            w.set(x0 + 4, F + 3, z0 + 1, f(a[0] ? "rec_clock_nohands" : "rec_clock", Direction.NORTH));
            String shelf = a[4] ? "rec_bookshelf_empty" : "rec_bookshelf";
            for (int u = 1; u <= 2; u++)
                for (int y = F + 1; y <= F + 3; y++) w.set(x0 + u, y, z0 + 1, f(shelf, Direction.NORTH));
            String p = a[8] ? "rec_paintfig_" : "rec_paint_";
            w.set(x0 + 10, F + 3, z0 + 1, f(p + "tl", Direction.NORTH));
            w.set(x0 + 11, F + 3, z0 + 1, f(p + "tr", Direction.NORTH));
            w.set(x0 + 10, F + 2, z0 + 1, f(p + "bl", Direction.NORTH));
            w.set(x0 + 11, F + 2, z0 + 1, f(p + "br", Direction.NORTH));

            // ---- east wall: lamp (1), armchair (5), window (9)
            w.set(x0 + 13, F + 1, z0 + 12, f("rec_lamp_pole", Direction.NORTH));
            w.set(x0 + 13, F + 2, z0 + 12, f(a[1] ? "rec_lamp_off" : "rec_lamp_on", Direction.NORTH));
            w.set(x0 + 12, F + 1, z0 + 10, f("rec_armchair", a[5] ? Direction.EAST : Direction.WEST));
            String win = a[9] ? "rec_winface_" : "rec_win_";
            w.set(x0 + SIZE, F + 3, z0 + 3, ModBlocks.state(win + "tl"));
            w.set(x0 + SIZE, F + 3, z0 + 4, ModBlocks.state(win + "tr"));
            w.set(x0 + SIZE, F + 2, z0 + 3, ModBlocks.state(win + "bl"));
            w.set(x0 + SIZE, F + 2, z0 + 4, ModBlocks.state(win + "br"));

            // ---- south wall: plant (2), sofa (6), photograph (10)
            w.set(x0 + 12, F + 1, z0 + 13, f(a[2] ? "rec_plant_dead" : "rec_plant", Direction.NORTH));
            if (!a[6]) {
                w.set(x0 + 2, F + 1, z0 + 13, f("rec_sofa_r", Direction.NORTH));
                w.set(x0 + 3, F + 1, z0 + 13, f("rec_sofa_m", Direction.NORTH));
                w.set(x0 + 4, F + 1, z0 + 13, f("rec_sofa_l", Direction.NORTH));
            }
            w.set(x0 + 10, F + 3, z0 + 13, f(a[10] ? "rec_photo_scratched" : "rec_photo", Direction.SOUTH));

            // ---- west wall: vase (3), chair before the mirror (7), mirror (11)
            w.set(x0 + 1, F + 1, z0 + 9, f("rec_sideboard", Direction.WEST));
            w.set(x0 + 1, F + 1, z0 + 10, f(a[3] ? "rec_sideboard_fallen" : "rec_sideboard_vase", Direction.WEST));
            w.set(x0 + 1, F + 1, z0 + 11, f("rec_sideboard", Direction.WEST));
            if (a[7]) w.set(x0 + 2, F + 1, z0 + 4, f("rec_dining_chair", Direction.WEST));
            String m = a[11] ? "rec_mirrorfig_" : "rec_mirror_";
            w.set(x0, F + 3, z0 + 4, ModBlocks.state(m + "top"));
            w.set(x0, F + 2, z0 + 4, ModBlocks.state(m + "bot"));
        }
    }
}
