package com.noclip.levels;

import com.noclip.entity.Mannequin;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Dead Mall. A 1990s mall at night: terrazzo concourses, skylights full of
 * nothing, fountains still running, shuttered stores and slowed-down muzak.
 * The display mannequins step out of their windows when nobody is looking, and
 * every few minutes the PA announces closing time and the power goes.
 */
public final class DeadMall {
    static final int P = 34;
    static final int FLOOR = 10, CEIL = FLOOR + 7, CHAN = FLOOR + 9, COURT = FLOOR + 11, TOP = FLOOR + 12;

    private DeadMall() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("dead_mall")
                .number("").title("Dead Mall").order(1000)
                .tagline("The mall is now closed. Please do not turn your back on the displays.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Displays())
                .visuals(new Visuals().tint(1.02f, 0.96f, 1.06f).saturation(0.92f).contrast(1.1f).brightness(1.25f)
                        .grain(0.07f).aberration(0.0018f).vignette(0.55f).fog(6f, 54f).fogColor(0x2E2836).bloom(0.6f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("mannequin"), 3, 0.2f).dist(16, 34).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(28, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "supermarket", 3)
                .exit(ExitType.NOCLIP, "parking_garage", 2).exit(ExitType.NOCLIP, "waiting_room", 1)
                .exit(ExitType.NOCLIP, "arcade", 1).exit(ExitType.NOCLIP, "movie_theater", 1)
                .exit(ExitType.DOOR, "parking_garage", 3).exit(ExitType.DOOR, "supermarket", 2).exit(ExitType.DOOR, "level_4", 1)
                .exit(ExitType.ASCEND, "movie_theater", 2).exit(ExitType.ASCEND, "arcade", 2).exit(ExitType.ASCEND, "level_5", 1)
                .exit(ExitType.DESCEND, "parking_garage", 3).exit(ExitType.DESCEND, "level_2", 1)
                .build();
    }

    enum Front { SHUTTER, GRILLE, DISPLAY, OPEN, LIT }

    static Front front(long s, int i, int k, int row, int store) {
        float r = Hash.unit(s, Hash.hash(i, k), row * 3L + store, 0xF0);
        if (r < 0.40f) return Front.SHUTTER;
        if (r < 0.62f) return Front.GRILLE;
        if (r < 0.88f) return Front.DISPLAY;
        if (r < 0.97f) return Front.OPEN;
        return Front.LIT;
    }

    static final class Gen implements LevelGenerator {
        final BlockState terrazzo = ModBlocks.state("mall_terrazzo");
        final BlockState teal = ModBlocks.state("mall_terrazzo_teal");
        final BlockState salmon = ModBlocks.state("mall_terrazzo_salmon");
        final BlockState wall = ModBlocks.state("mall_wall");
        final BlockState wallBase = ModBlocks.state("mall_wall_base");
        final BlockState bulkhead = ModBlocks.state("mall_bulkhead");
        final BlockState fascia = ModBlocks.state("mall_fascia");
        final BlockState ceiling = ModBlocks.state("mall_ceiling");
        final BlockState ceilingStained = ModBlocks.state("mall_ceiling_stained");
        final BlockState can = ModBlocks.state("mall_canlight");
        final BlockState canFlicker = ModBlocks.state("mall_canlight_flicker");
        final BlockState canDead = ModBlocks.state("mall_canlight_dead");
        final BlockState sky = ModBlocks.state("mall_skylight");
        final BlockState shutter = ModBlocks.state("mall_shutter");
        final BlockState shutterTag = ModBlocks.state("mall_shutter_tagged");
        final BlockState shutterBottom = ModBlocks.state("mall_shutter_bottom");
        final BlockState grille = ModBlocks.state("mall_grille");
        final BlockState glass = ModBlocks.state("mall_glass");
        final BlockState carpet = ModBlocks.state("mall_store_carpet");
        final BlockState tile = ModBlocks.state("mall_store_tile");
        final BlockState storeWall = ModBlocks.state("mall_store_wall");
        final BlockState storeCeil = ModBlocks.state("mall_store_ceiling");
        final BlockState storeLight = ModBlocks.state("mall_store_light");
        final BlockState planter = ModBlocks.state("mall_planter");
        final BlockState trunk = ModBlocks.state("mall_palm_trunk");
        final BlockState fronds = ModBlocks.state("mall_palm_fronds");
        final BlockState bench = ModBlocks.state("mall_bench");
        final BlockState trash = ModBlocks.state("mall_trash_can");
        final BlockState wet = ModBlocks.state("mall_wet_floor_sign");
        final BlockState rim = ModBlocks.state("mall_fountain_rim");
        final BlockState spout = ModBlocks.state("mall_fountain_spout");
        final BlockState mosaic = ModBlocks.state("mall_mosaic_teal");
        final BlockState coins = ModBlocks.state("mall_mosaic_coins");
        final BlockState fLight = ModBlocks.state("mall_fountain_light");
        final BlockState mqLow = ModBlocks.state("mall_mannequin_display");
        final BlockState mqUp = ModBlocks.state("mall_mannequin_upper");
        final BlockState rack = ModBlocks.state("mall_clothing_rack");
        final BlockState post = ModBlocks.state("mall_directory_post");
        final BlockState directory = ModBlocks.state("mall_directory");
        final BlockState escBody = ModBlocks.state("mall_escalator_body");
        final BlockState esc = ModBlocks.state("mall_escalator");
        final BlockState darkUp = ModBlocks.state("mall_escalator_dark_up");
        final BlockState darkDown = ModBlocks.state("mall_escalator_dark_down");
        final BlockState door = ModBlocks.state("mall_service_door");
        final BlockState doorTop = ModBlocks.state("mall_service_door_top");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState shelf = ModBlocks.state("metal_shelf");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState[][] signs = new BlockState[11][3];

        Gen() {
            for (int n = 0; n < 10; n++)
                for (int p = 0; p < 3; p++) signs[n][p] = ModBlocks.state("mall_sign_" + n + "_" + p);
            for (int p = 0; p < 3; p++) signs[10][p] = ModBlocks.state("mall_sign_dead_" + p);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, TOP + 1, w.minZ, w.maxX, TOP + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = Math.floorDiv(x, P), k = Math.floorDiv(z, P);
                    int u = x - i * P, v = z - k * P;
                    if (u <= 8 || v <= 8) concourse(w, s, x, z, u, v);
                    else block(w, s, x, z, i, k, u, v);
                }
            }
            for (int i = Math.floorDiv(w.minX, P) - 1; i <= Math.floorDiv(w.maxX, P) + 1; i++)
                for (int k = Math.floorDiv(w.minZ, P) - 1; k <= Math.floorDiv(w.maxZ, P) + 1; k++) {
                    stores(w, s, i, k);
                    segment(w, s, i, k, true);
                    segment(w, s, i, k, false);
                    court(w, s, i, k);
                }
        }

        // ------------------------------------------------------------ concourses

        void concourse(ChunkWriter w, long s, int x, int z, int u, int v) {
            boolean court = u <= 8 && v <= 8;
            BlockState f = terrazzo;
            if (court) {
                int d = Math.abs(u - 4) + Math.abs(v - 4);
                if (d == 5 || d == 2) f = teal;
                else if (d == 7) f = salmon;
            } else {
                int c = u <= 8 ? u : v;
                int along = u <= 8 ? v : u;
                if (c == 1 || c == 7) f = teal;
                else if (along == 21 && c > 1 && c < 7) f = salmon;
            }
            w.set(x, FLOOR, z, f);
            int ceilY;
            BlockState ceil;
            if (court) {
                int ring = Math.max(Math.abs(u - 4), Math.abs(v - 4));
                if (ring <= 2) {
                    ceilY = COURT;
                    ceil = sky;
                } else {
                    ceilY = CEIL;
                    ceil = ((u == 0 || u == 8) && (v == 0 || v == 8 || v == 4)) || ((v == 0 || v == 8) && u == 4)
                            ? light(s, x, z) : ceiling;
                }
            } else {
                int c = u <= 8 ? u : v;
                int along = u <= 8 ? v : u;
                if (c >= 3 && c <= 5) {
                    ceilY = CHAN;
                    ceil = sky;
                } else {
                    ceilY = CEIL;
                    if ((c == 1 || c == 7) && Math.floorMod(along, 4) == 2) ceil = light(s, x, z);
                    else ceil = Hash.chance(s, x, z, 41, 0.05f) ? ceilingStained : ceiling;
                }
            }
            w.set(x, ceilY, z, ceil);
            if (ceilY < TOP) w.fill(x, ceilY + 1, z, x, TOP, z, fascia);
        }

        BlockState light(long s, int x, int z) {
            float dark = Hash.noise(s ^ 0xDA11, x / 48.0, z / 48.0);
            float r = Hash.unit(s, x, z, 77);
            if (dark > 0.68f) return r < 0.15f ? canFlicker : canDead;
            if (r < 0.22f) return canDead;
            if (r < 0.32f) return canFlicker;
            return can;
        }

        // ------------------------------------------------------------ store blocks

        void block(ChunkWriter w, long s, int x, int z, int i, int k, int u, int v) {
            boolean frontLine = v == 9 || v == 33;
            if (u == 9 || u == 33) {
                // side wall facing a north-south concourse
                w.set(x, FLOOR, z, terrazzo);
                w.set(x, FLOOR + 1, z, wallBase);
                w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, wall);
                w.fill(x, FLOOR + 5, z, x, TOP, z, bulkhead);
                return;
            }
            if (frontLine) {
                w.set(x, FLOOR, z, terrazzo);
                if (u == 17 || u == 25) {
                    w.set(x, FLOOR + 1, z, wallBase);
                    w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, wall);
                    w.fill(x, FLOOR + 5, z, x, TOP, z, bulkhead);
                    return;
                }
                int row = v == 9 ? 0 : 1;
                int store = (u - 10) / 8, o = u - 10 - store * 8;
                storefront(w, s, x, z, i, k, row, store, o);
                return;
            }
            if (u == 17 || u == 25 || v == 20 || v == 21) {
                w.fill(x, FLOOR, z, x, TOP, z, storeWall);
                return;
            }
            int row = v <= 20 ? 0 : 1;
            int store = (u - 10) / 8;
            Front fr = front(s, i, k, row, store);
            boolean tiled = Hash.chance(s, Hash.hash(i, k), row * 3L + store, 0x7A, 0.35f);
            w.set(x, FLOOR, z, sub);
            w.set(x, FLOOR + 1, z, tiled ? tile : carpet);
            int dx = u - 10 - store * 8, dz = v;
            boolean lamp = fr == Front.LIT && dx % 3 == 1 && dz % 3 == 1;
            w.set(x, FLOOR + 6, z, lamp ? storeLight : storeCeil);
            w.fill(x, FLOOR + 7, z, x, TOP, z, storeCeil);
        }

        void storefront(ChunkWriter w, long s, int x, int z, int i, int k, int row, int store, int o) {
            Front fr = front(s, i, k, row, store);
            long h = Hash.hash(s, Hash.hash(i, k), row * 3L + store, 0x51);
            switch (fr) {
                case SHUTTER -> {
                    w.set(x, FLOOR + 1, z, shutterBottom);
                    BlockState sh = Hash.unit(h >>> 7) < 0.25f ? shutterTag : shutter;
                    w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, sh);
                }
                case GRILLE -> {
                    w.set(x, FLOOR + 1, z, terrazzo);
                    w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, grille);
                }
                case DISPLAY -> {
                    w.set(x, FLOOR + 1, z, bulkhead);
                    w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, (o <= 1 || o >= 5) ? glass : grille);
                    if (o >= 2 && o <= 4) w.set(x, FLOOR + 1, z, terrazzo);
                }
                case OPEN, LIT -> {
                    w.set(x, FLOOR + 1, z, terrazzo);
                    if (o == 0 || o == 6) w.fill(x, FLOOR + 2, z, x, FLOOR + 4, z, glass);
                }
            }
            // the sign: three tiles, ordered so they read left to right from the concourse
            BlockState b = bulkhead;
            if (o >= 2 && o <= 4) {
                int sign = Hash.range(h >>> 3, 10);
                if (fr == Front.SHUTTER && Hash.unit(h >>> 13) < 0.45f) sign = 10;
                else if (Hash.unit(h >>> 17) < 0.12f) sign = 10;
                int part = row == 0 ? 4 - o : o - 2;
                b = signs[sign][part];
            }
            w.set(x, FLOOR + 5, z, b);
            w.fill(x, FLOOR + 6, z, x, TOP, z, bulkhead);
        }

        /** Store contents. Store floors are raised one step, so nobody arrives inside a locked store. */
        void stores(ChunkWriter w, long s, int i, int k) {
            int x0 = i * P, z0 = k * P;
            for (int row = 0; row < 2; row++) {
                for (int store = 0; store < 3; store++) {
                    Front fr = front(s, i, k, row, store);
                    if (fr == Front.SHUTTER) continue;
                    long h = Hash.hash(s, Hash.hash(i, k), row * 3L + store, 0x99);
                    int xs = x0 + 10 + store * 8;
                    int zf = row == 0 ? z0 + 10 : z0 + 32;
                    int dir = row == 0 ? 1 : -1;
                    Direction out = row == 0 ? Direction.NORTH : Direction.SOUTH;
                    int y = FLOOR + 2;
                    if (fr == Front.DISPLAY) {
                        mannequin(w, xs + 1, y, zf, out);
                        mannequin(w, xs + 5, y, zf, out);
                    }
                    float kind = Hash.unit(h);
                    if (kind < 0.55f) {
                        for (int r = 0; r < 2; r++)
                            for (int dx = 1; dx <= 5; dx += 2)
                                if (Hash.unit(h >>> (r * 6 + dx)) < 0.7f)
                                    w.set(xs + dx, y, zf + dir * (4 + r * 3), rack.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                        if (fr != Front.DISPLAY) mannequin(w, xs + 3, y, zf + dir * 2, out);
                    } else if (kind < 0.8f) {
                        for (int dx = 0; dx <= 6; dx++) {
                            w.set(xs + dx, y, zf + dir * 8, shelf.setValue(HorizontalDirectionalBlock.FACING, out));
                            w.set(xs + dx, y + 1, zf + dir * 8, shelf.setValue(HorizontalDirectionalBlock.FACING, out));
                        }
                        w.set(xs + 2, y, zf + dir * 4, boxes.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    } else {
                        // gutted: boxes, a single mannequin facing the back wall
                        w.set(xs + 1, y, zf + dir * 6, boxes.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                        w.set(xs + 5, y, zf + dir * 3, boxes.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                        mannequin(w, xs + 3, y, zf + dir * 8, out.getOpposite());
                    }
                    if ((fr == Front.OPEN || fr == Front.LIT) && Hash.unit(h >>> 30) < 0.5f)
                        w.set(xs + 5, y, zf + dir * 7, Hash.unit(h >>> 33) < 0.5f ? supply : bottle);
                    if (fr == Front.LIT) {
                        // one store is still open: lights on, swept, one mannequin waiting by the door
                        mannequin(w, xs + 3, y, zf + dir, out);
                    }
                }
            }
        }

        void mannequin(ChunkWriter w, int x, int y, int z, Direction d) {
            w.set(x, y, z, mqLow.setValue(HorizontalDirectionalBlock.FACING, d));
            w.set(x, y + 1, z, mqUp.setValue(HorizontalDirectionalBlock.FACING, d));
        }

        // ------------------------------------------------------------ concourse furniture

        /** ns = the north-south concourse beside block (i,k); otherwise the east-west one. */
        void segment(ChunkWriter w, long s, int i, int k, boolean ns) {
            long h = Hash.hash(s, i, k, ns ? 0x5E1 : 0x5E2);
            float r = Hash.unit(h);
            int x0 = i * P, z0 = k * P;
            // local frame: a = across (0..8), b = along (9..33)
            int y = FLOOR + 1;
            Direction across0 = ns ? Direction.WEST : Direction.NORTH;
            Direction across1 = across0.getOpposite();
            if (r < 0.55f) {
                for (int a = 3; a <= 5; a++)
                    for (int b = 19; b <= 23; b++) put(w, ns, x0, z0, a, b, y, planter);
                for (int b : new int[]{20, 22}) {
                    put(w, ns, x0, z0, 4, b, y + 1, trunk);
                    put(w, ns, x0, z0, 4, b, y + 2, trunk);
                    put(w, ns, x0, z0, 4, b, y + 3, fronds);
                }
                put(w, ns, x0, z0, 2, 21, y, rot(bench, ns, across0));
                put(w, ns, x0, z0, 6, 21, y, rot(bench, ns, across1));
                put(w, ns, x0, z0, 2, 17, y, trash);
            } else if (r < 0.75f) {
                put(w, ns, x0, z0, 4, 15, y, rot(bench, ns, across0));
                put(w, ns, x0, z0, 4, 27, y, rot(bench, ns, across1));
                if (Hash.unit(h >>> 9) < 0.5f) put(w, ns, x0, z0, 3, 21, y, wet.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> 11, 4))));
            } else if (r < 0.78f) {
                // a lost child's things: a bottle on a bench
                put(w, ns, x0, z0, 4, 21, y, rot(bench, ns, across0));
                put(w, ns, x0, z0, 5, 21, y, bottle.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
            if (!ns) return;
            // fittings on the side wall (u = 9, facing west into this concourse)
            float r2 = Hash.unit(h >>> 20);
            if (r2 < 0.08f) {
                int z = z0 + 14;
                w.set(x0 + 9, y, z, door);
                w.set(x0 + 9, y + 1, z, doorTop);
                w.set(x0 + 8, y + 2, z, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            } else if (r2 < 0.24f) {
                w.set(x0 + 7, y, z0 + 13, post.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                w.set(x0 + 7, y + 1, z0 + 13, directory.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            }
        }

        static BlockState rot(BlockState st, boolean ns, Direction d) {
            return st.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void put(ChunkWriter w, boolean ns, int x0, int z0, int a, int b, int y, BlockState st) {
            if (ns) w.set(x0 + a, y, z0 + b, st);
            else w.set(x0 + b, y, z0 + a, st);
        }

        void court(ChunkWriter w, long s, int i, int k) {
            long h = Hash.hash(s, i, k, 0xC0);
            float r = Hash.unit(h);
            int x0 = i * P, z0 = k * P, cx = x0 + 4, cz = z0 + 4;
            int y = FLOOR + 1;
            if (r < 0.05f) {
                // a stopped escalator up into nothing
                for (int st = 0; st < 6; st++) {
                    int z = z0 + 1 + st, yy = FLOOR + 1 + st;
                    for (int x = x0 + 3; x <= x0 + 5; x++) {
                        w.fill(x, FLOOR + 1, z, x, yy - 1, z, escBody);
                        w.set(x, yy, z, esc.setValue(StairBlock.FACING, Direction.SOUTH));
                    }
                    w.fill(x0 + 2, FLOOR + 1, z, x0 + 2, yy + 1, z, escBody);
                    w.fill(x0 + 6, FLOOR + 1, z, x0 + 6, yy + 1, z, escBody);
                }
                w.fill(x0 + 2, FLOOR + 1, z0 + 7, x0 + 6, FLOOR + 6, z0 + 8, escBody);
                w.fill(x0 + 3, FLOOR + 7, z0 + 7, x0 + 5, COURT - 1, z0 + 8, darkUp);
            } else if (r < 0.09f) {
                // an escalator down, into the lower level that is not there
                for (int st = 0; st < 6; st++) {
                    int z = z0 + 2 + st, yy = FLOOR - st;
                    for (int x = x0 + 3; x <= x0 + 5; x++) {
                        w.fill(x, yy + 1, z, x, FLOOR, z, ModBlocks.AIR);
                        w.set(x, yy, z, esc.setValue(StairBlock.FACING, Direction.NORTH));
                    }
                }
                w.fill(x0 + 3, FLOOR - 6, z0 + 8, x0 + 5, FLOOR, z0 + 8, darkDown);
                for (int z = z0 + 1; z <= z0 + 8; z++) {
                    w.set(x0 + 2, y, z, glass);
                    w.set(x0 + 6, y, z, glass);
                }
                for (int x = x0 + 3; x <= x0 + 5; x++) w.set(x, y, z0 + 1, glass);
            } else if (r < 0.10f) {
                // they gathered around the fountain
                fountain(w, h, cx, cz, false);
                int[][] ring = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}, {-3, -3}, {3, 3}, {-3, 3}, {3, -3}};
                for (int[] p : ring) {
                    Direction d = Math.abs(p[0]) >= Math.abs(p[1])
                            ? (p[0] < 0 ? Direction.EAST : Direction.WEST) : (p[1] < 0 ? Direction.SOUTH : Direction.NORTH);
                    // displays face their front (north) toward where they look
                    mannequin(w, cx + p[0], y, cz + p[1], d.getOpposite());
                }
            } else if (r < 0.72f) {
                fountain(w, h, cx, cz, Hash.unit(h >>> 8) < 0.15f);
            } else {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    w.set(cx + d.getStepX() * 3, y, cz + d.getStepZ() * 3, bench.setValue(HorizontalDirectionalBlock.FACING,
                            d.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST));
                }
                w.set(cx, y, cz, post);
                w.set(cx, y + 1, cz, directory.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> 5, 4))));
            }
        }

        void fountain(ChunkWriter w, long h, int cx, int cz, boolean dry) {
            int y = FLOOR + 1;
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    int x = cx + dx, z = cz + dz;
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        w.set(x, y, z, rim.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    } else {
                        boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                        if (dry) {
                            w.set(x, FLOOR, z, coins);
                        } else {
                            w.set(x, FLOOR - 1, z, corner ? fLight : (Hash.unit(h >>> (dx + 3 + dz * 5 + 20)) < 0.5f ? coins : mosaic));
                            w.set(x, FLOOR, z, water);
                        }
                    }
                }
            if (Hash.unit(h >>> 40) < 0.06f) mannequin(w, cx, dry ? y : FLOOR + 1, cz, Direction.from2DDataValue(Hash.range(h >>> 44, 4)));
            else w.set(cx, y, cz, spout.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Displays: every display mannequin out of sight may step down off its stand
     * and into the concourse behind you. The stand stays, empty. Every few
     * minutes the PA announces closing time and the power goes.
     */
    static final class Displays implements LevelBehavior {
        private long nextClosing = -1, blackoutAt = -1;

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long t = level.getGameTime();
            if (nextClosing < 0) nextClosing = t + 20 * (150 + level.random.nextInt(180));
            if (t >= nextClosing && !Blackouts.active(level)) {
                nextClosing = t + 20 * (240 + level.random.nextInt(240));
                blackoutAt = t + 150;
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition().above(4), ModSounds.get("event.dead_mall_closing"), SoundSource.AMBIENT, 1.6f, 1f);
                    p.displayClientMessage(Component.literal("§7§o\"The mall is now closed. Please make your way to the nearest exit.\""), true);
                }
            }
            if (blackoutAt > 0 && t >= blackoutAt) {
                blackoutAt = -1;
                Blackouts.start(level, 20 * (30 + level.random.nextInt(20)));
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || (p.tickCount + p.getId()) % 20 != 0) return;
            float chance = Blackouts.active(level) ? 0.35f : 0.12f;
            if (p.getRandom().nextFloat() > chance) return;
            var type = ModEntities.MANNEQUIN;
            if (level.getEntities(type, p.getBoundingBox().inflate(40), e -> true).size() >= 5) return;
            Block low = ModBlocks.get("mall_mannequin_display");
            List<BlockPos> found = new ArrayList<>();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            int px = p.getBlockX(), pz = p.getBlockZ();
            for (int x = px - 22; x <= px + 22; x++)
                for (int z = pz - 22; z <= pz + 22; z++)
                    for (int y = FLOOR + 1; y <= FLOOR + 2; y++) {
                        m.set(x, y, z);
                        if (level.isLoaded(m) && level.getBlockState(m).is(low)) found.add(m.immutable());
                    }
            if (found.isEmpty()) return;
            BlockPos pos = found.get(p.getRandom().nextInt(found.size()));
            if (pos.distSqr(p.blockPosition()) < 7 * 7 || seen(p, pos)) return;
            BlockState st = level.getBlockState(pos);
            Direction out = st.getValue(HorizontalDirectionalBlock.FACING);
            // step out of the window into the concourse
            BlockPos spot = pos;
            if (pos.getY() == FLOOR + 2) {
                spot = new BlockPos(pos.getX(), FLOOR + 1, pos.getZ()).relative(out, 2);
                if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()) return;
                if (seen(p, spot)) return;
            }
            Mob mq = type.create(level);
            if (mq == null) return;
            level.setBlock(pos, ModBlocks.state("mall_pedestal").setValue(HorizontalDirectionalBlock.FACING, out), Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), ModBlocks.AIR, Block.UPDATE_CLIENTS);
            mq.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, out.toYRot(), 0f);
            mq.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
            level.addFreshEntity(mq);
            level.playSound(null, pos, ModSounds.get("event.dead_mall_step"), SoundSource.HOSTILE, 0.7f, 0.9f + p.getRandom().nextFloat() * 0.2f);
            Sanity.add(p, -1.5f);
        }

        static boolean seen(ServerPlayer p, BlockPos pos) {
            Vec3 eye = p.getEyePosition();
            for (double hgt : new double[]{0.5, 1.5}) {
                Vec3 c = new Vec3(pos.getX() + 0.5, pos.getY() + hgt, pos.getZ() + 0.5);
                Vec3 to = c.subtract(eye).normalize();
                if (p.getViewVector(1f).dot(to) < 0.25) continue;
                if (p.level().clip(new ClipContext(eye, c, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS)
                    return true;
                // glass and grilles do not hide anything
                HitResult hit = p.level().clip(new ClipContext(eye, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(c) < 2.5) return true;
            }
            return false;
        }
    }
}
