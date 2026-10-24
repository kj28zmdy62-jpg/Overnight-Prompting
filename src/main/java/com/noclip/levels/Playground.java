package com.noclip.levels;

import com.noclip.entity.PlayplaceMascot;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Indoor Playground. A dark hall of foam floors, netted climbing frames, crawl
 * tubes, slides and ball pits. Children laugh. Every few minutes someone counts
 * to ten and It comes looking: anyone It can see has been found. Hide in the
 * ball pits or the tubes until "olly olly oxen free". One of the ball pits has
 * no bottom.
 */
public final class Playground {
    static final int FLOOR = 10, CEIL = FLOOR + 10;
    static final String[] COLORS = {"red", "blue", "yellow", "green"};

    private Playground() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("playground")
                .number("").title("Indoor Playground").order(1060)
                .tagline("Ready or not, here I come.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new HideAndSeek())
                .visuals(new Visuals().tint(1.0f, 0.98f, 1.06f).saturation(1.05f).contrast(1.08f).brightness(1.2f)
                        .grain(0.07f).aberration(0.002f).vignette(0.55f).fog(6f, 50f).fogColor(0x14141C).bloom(0.5f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.35f).dark().dist(14, 28).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "level_fun", 2).exit(ExitType.NOCLIP, "school", 1)
                .exit(ExitType.NOCLIP, "arcade", 1)
                .exit(ExitType.DOOR, "daycare", 2).exit(ExitType.DOOR, "foggy_playground", 2).exit(ExitType.DOOR, "school", 1)
                .exit(ExitType.FALL, "level_fun", 2).exit(ExitType.FALL, "the_abyss", 1).exit(ExitType.FALL, "level_0", 1)
                .build();
    }

    // ---------------------------------------------------------------- hide and seek state

    private static boolean seeking;

    public static boolean seeking() {
        return seeking;
    }

    /** In a ball pit, or inside a crawl tube. */
    public static boolean hidden(Player p) {
        BlockState feet = p.level().getBlockState(p.blockPosition());
        BlockState eyes = p.level().getBlockState(BlockPos.containing(p.getEyePosition()));
        BlockState below = p.level().getBlockState(p.blockPosition().below());
        Block balls = ModBlocks.get("pp_balls");
        return eyes.is(balls) || (feet.is(balls) && p.isShiftKeyDown()) || below.is(ModBlocks.get("pp_tube_floor"));
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState[] foam = new BlockState[4], pad = new BlockState[4], tube = new BlockState[4];
        final BlockState balls = ModBlocks.state("pp_balls");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState tubeFloor = ModBlocks.state("pp_tube_floor");
        final BlockState slide = ModBlocks.state("pp_slide");
        final BlockState steps = ModBlocks.state("pp_steps");
        final BlockState net = ModBlocks.state("pp_net");
        final BlockState wall = ModBlocks.state("pp_wall");
        final BlockState sun = ModBlocks.state("pp_mural_sun");
        final BlockState ceil = ModBlocks.state("pp_ceiling");
        final BlockState lamp = ModBlocks.state("pp_lamp");
        final BlockState cubby = ModBlocks.state("pp_cubby");
        final BlockState exit = ModBlocks.state("pp_exit");
        final BlockState exitTop = ModBlocks.state("pp_exit_top");
        final BlockState block = ModBlocks.state("pp_foam_block");
        final BlockState arch = ModBlocks.state("pp_foam_arch");
        final BlockState roll = ModBlocks.state("pp_foam_cylinder");
        final BlockState post = ModBlocks.state("pp_post");
        final BlockState postBlue = ModBlocks.state("pp_post_blue");
        final BlockState shoe = ModBlocks.state("pp_lone_shoe");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        Gen() {
            for (int i = 0; i < 4; i++) {
                foam[i] = ModBlocks.state("pp_foam_" + COLORS[i]);
                pad[i] = ModBlocks.state("pp_pad_" + COLORS[i]);
                tube[i] = ModBlocks.state("pp_tube_" + COLORS[i]);
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 22, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceil);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    // puzzle mats laid in patches of one colour
                    int patch = Hash.range(s, Math.floorDiv(x, 4), Math.floorDiv(z, 4), 0x9A7, 4);
                    w.set(x, FLOOR, z, foam[Hash.chance(s, x, z, 0x9A8, 0.08f) ? (patch + 1) % 4 : patch]);
                    if (Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4) {
                        w.set(x, FLOOR + 8, z, Hash.chance(s, x, z, 3, 0.35f) ? lamp.setValue(BlockStateProperties.LIT, false) : lamp);
                        w.set(x, FLOOR + 9, z, chain);
                    }
                }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> cell(w, g, s, i, k, x0, z0));
        }

        BlockState netState(boolean alongX) {
            return alongX ? net.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
                    : net.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
        }

        void cell(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0) {
            long h = g.hash(i, k, 0x91A);
            float r = Hash.unit(h);
            int y = FLOOR + 1;
            int col = Hash.range(h >>> 4, 4);
            if (r < 0.32f) ballPit(w, h, x0, z0, col);
            else if (r < 0.74f) frame(w, h, x0, z0, col);
            else if (r < 0.86f) {
                // toddler zone: foam shapes left where they fell
                for (int n = 0; n < 12; n++) {
                    long hh = Hash.hash(h, n);
                    int x = x0 + 4 + Hash.range(hh, 14), z = z0 + 4 + Hash.range(hh >>> 8, 14);
                    BlockState b = switch (Hash.range(hh >>> 16, 3)) { case 0 -> block; case 1 -> arch; default -> roll; };
                    w.set(x, y, z, b.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(hh >>> 20, 4))));
                }
            } else if (r < 0.92f) {
                // the cubbies, every one with a pair of small shoes in it, and the mural
                for (int x = x0 + 5; x <= x0 + 16; x++) {
                    w.set(x, y, z0 + 11, cubby);
                    w.set(x, y + 1, z0 + 11, cubby);
                    w.fill(x, y + 2, z0 + 11, x, y + 4, z0 + 11, wall);
                }
                w.set(x0 + 10, y + 3, z0 + 11, sun);
                w.set(x0 + 8, y, z0 + 13, shoe.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            } else if (r < 0.95f) {
                // a wall with a door in it, and a goodbye
                for (int x = x0 + 8; x <= x0 + 13; x++) w.fill(x, y, z0 + 11, x, y + 4, z0 + 11, wall);
                w.set(x0 + 10, y, z0 + 11, exit);
                w.set(x0 + 10, y + 1, z0 + 11, exitTop);
                w.set(x0 + 10, y + 2, z0 + 12, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                w.set(x0 + 10, y + 2, z0 + 10, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            } else if (r < 0.97f) {
                w.set(x0 + 11, y, z0 + 11, bottle.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
        }

        void ballPit(ChunkWriter w, long h, int x0, int z0, int col) {
            int y = FLOOR + 1;
            boolean deep = Hash.unit(h >>> 30) < 0.12f;
            for (int u = 4; u <= 17; u++)
                for (int v = 4; v <= 17; v++) {
                    int x = x0 + u, z = z0 + v;
                    boolean rim = u == 4 || u == 17 || v == 4 || v == 17;
                    if (rim) {
                        boolean gap = v == 4 && (u == 10 || u == 11);
                        if (!gap) {
                            w.set(x, y, z, pad[col]);
                            w.fill(x, y + 1, z, x, y + 2, z, netState(v == 4 || v == 17));
                        }
                        continue;
                    }
                    boolean core = deep && u >= 9 && u <= 12 && v >= 9 && v <= 12;
                    int depth = core ? 6 : 2;
                    w.fill(x, FLOOR - depth + 1, z, x, FLOOR, z, balls);
                    w.set(x, FLOOR - depth, z, core ? pit : pad[(col + 1) % 4]);
                }
            // posts at the corners of the netting
            for (int[] c : new int[][]{{4, 4}, {17, 4}, {4, 17}, {17, 17}}) w.fill(x0 + c[0], y, z0 + c[1], x0 + c[0], y + 3, z0 + c[1], post);
        }

        void frame(ChunkWriter w, long h, int x0, int z0, int col) {
            int y = FLOOR + 1, top = FLOOR + 5;
            // the deck
            for (int u = 6; u <= 15; u++)
                for (int v = 6; v <= 15; v++) w.set(x0 + u, top, z0 + v, pad[(col + ((u + v) % 2)) % 4]);
            for (int u = 6; u <= 15; u += 3)
                for (int v = 6; v <= 15; v += 3) w.fill(x0 + u, y, z0 + v, x0 + u, top - 1, z0 + v, (u + v) % 2 == 0 ? post : postBlue);
            // netting around the deck, with openings for the steps and the slide
            for (int u = 6; u <= 15; u++) {
                if (u != 10 && u != 11) w.fill(x0 + u, top + 1, z0 + 15, x0 + u, top + 2, z0 + 15, netState(true));
                w.fill(x0 + u, top + 1, z0 + 6, x0 + u, top + 2, z0 + 6, netState(true));
            }
            for (int v = 6; v <= 15; v++) {
                if (v != 7 && v != 8) w.fill(x0 + 6, top + 1, z0 + v, x0 + 6, top + 2, z0 + v, netState(false));
                w.fill(x0 + 15, top + 1, z0 + v, x0 + 15, top + 2, z0 + v, netState(false));
            }
            // foam steps up from the south
            for (int st = 0; st < 5; st++)
                for (int u = 10; u <= 11; u++) {
                    int z = z0 + 20 - st, yy = y + st;
                    w.fill(x0 + u, y, z, x0 + u, yy - 1, z, pad[(col + 2) % 4]);
                    w.set(x0 + u, yy, z, steps.setValue(StairBlock.FACING, Direction.NORTH));
                }
            // the slide, down to the west
            for (int st = 0; st < 5; st++)
                for (int v = 7; v <= 8; v++) w.set(x0 + 5 - st, top - st, z0 + v, slide.setValue(StairBlock.FACING, Direction.EAST));
            // a crawl tube under the deck: walk in, crouch, and nothing can see you
            BlockState tb = tube[(col + 3) % 4];
            for (int u = 3; u <= 18; u++) {
                int x = x0 + u;
                w.set(x, FLOOR, z0 + 11, tubeFloor);
                w.fill(x, y, z0 + 10, x, y + 1, z0 + 10, tb);
                w.fill(x, y, z0 + 12, x, y + 1, z0 + 12, tb);
                w.fill(x, y + 2, z0 + 10, x, y + 2, z0 + 12, tb);
                w.fill(x, y, z0 + 11, x, y + 1, z0 + 11, ModBlocks.AIR);
            }
        }
    }

    // ---------------------------------------------------------------- the game

    static final class HideAndSeek implements LevelBehavior {
        enum Phase { IDLE, COUNT, SEEK }

        private Phase phase = Phase.IDLE;
        private long end = -1;

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (end < 0) end = now + 20 * (50 + level.random.nextInt(60));
            if (now < end) return;
            switch (phase) {
                case IDLE -> {
                    phase = Phase.COUNT;
                    end = now + 150;
                    for (ServerPlayer p : level.players()) {
                        BlockPos behind = BlockPos.containing(p.position().subtract(p.getLookAngle().multiply(10, 0, 10)));
                        level.playSound(null, behind, ModSounds.get("event.playground_count"), SoundSource.HOSTILE, 2.5f, 1f);
                        p.displayClientMessage(Component.literal("§d§oSomeone is counting. Hide."), true);
                    }
                }
                case COUNT -> {
                    phase = Phase.SEEK;
                    seeking = true;
                    end = now + 20 * (40 + level.random.nextInt(20));
                    for (ServerPlayer p : level.players()) {
                        p.displayClientMessage(Component.literal("§d§o\"Ready or not, here I come!\""), true);
                        summon(p, level);
                    }
                }
                case SEEK -> {
                    phase = Phase.IDLE;
                    seeking = false;
                    end = now + 20 * (70 + level.random.nextInt(90));
                    for (ServerPlayer p : level.players()) {
                        level.playSound(null, p.blockPosition().offset(8, 2, 8), ModSounds.get("event.playground_free"), SoundSource.HOSTILE, 2f, 1f);
                        p.displayClientMessage(Component.literal("§7§o\"Olly olly oxen free.\""), true);
                    }
                }
            }
        }

        void summon(ServerPlayer p, ServerLevel level) {
            if (p.isCreative() || p.isSpectator()) return;
            var r = p.getRandom();
            for (int attempt = 0; attempt < 12; attempt++) {
                double ang = r.nextDouble() * Math.PI * 2;
                int d = 18 + r.nextInt(10);
                BlockPos at = new BlockPos(p.getBlockX() + (int) (Math.cos(ang) * d), FLOOR + 1, p.getBlockZ() + (int) (Math.sin(ang) * d));
                if (!level.isLoaded(at) || !level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()
                        || !level.getBlockState(at.above(2)).isAir()) continue;
                PlayplaceMascot m = PlayplaceMascot.TYPE.create(level);
                if (m == null) return;
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360f, 0f);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                level.addFreshEntity(m);
                return;
            }
        }
    }
}
