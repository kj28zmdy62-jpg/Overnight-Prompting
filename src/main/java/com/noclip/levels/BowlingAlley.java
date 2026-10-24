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
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bowling Alley. Rows of maple lanes running south into a dark masking unit.
 * Nobody is here, but every so often a ball comes rolling down one of the lanes
 * near you, the pins go down, and the pinsetter resets them. Do not be standing
 * on the lane when it comes. The pit behind the pins goes a long way down; to
 * reach it you have to walk the length of a lane.
 */
public final class BowlingAlley {
    static final int FLOOR = 10, CEIL = FLOOR + 6, PX = 8, PZ = 40, FOUL = 6, PINS = 27;

    private BowlingAlley() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("bowling_alley")
                .number("").title("Bowling Alley").order(1090)
                .tagline("Player 2 is up. Player 2 is always up.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Lanes())
                .visuals(new Visuals().tint(1.0f, 0.96f, 1.08f).saturation(1.05f).contrast(1.12f).brightness(1.2f)
                        .grain(0.07f).aberration(0.002f).vignette(0.55f).fog(6f, 56f).fogColor(0x0C0A18).bloom(0.65f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.1f).seen().dist(24, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "arcade", 2).exit(ExitType.NOCLIP, "movie_theater", 1)
                .exit(ExitType.FALL, "level_1", 2).exit(ExitType.FALL, "boiler_room", 1).exit(ExitType.FALL, "the_abyss", 1)
                .exit(ExitType.DOOR, "arcade", 2).exit(ExitType.DOOR, "dead_mall", 1).exit(ExitType.DOOR, "level_4", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState lane = ModBlocks.state("bowl_lane");
        final BlockState arrows = ModBlocks.state("bowl_lane_arrows");
        final BlockState dots = ModBlocks.state("bowl_lane_dots");
        final BlockState foul = ModBlocks.state("bowl_lane_foul");
        final BlockState gutter = ModBlocks.state("bowl_gutter");
        final BlockState capping = ModBlocks.state("bowl_capping");
        final BlockState carpet = ModBlocks.state("bowl_carpet");
        final BlockState wall = ModBlocks.state("bowl_wall");
        final BlockState pit = ModBlocks.state("bowl_pit");
        final BlockState counter = ModBlocks.state("bowl_counter");
        final BlockState shoes = ModBlocks.state("bowl_shoes");
        final BlockState laneLight = ModBlocks.state("bowl_lane_light");
        final BlockState pins = ModBlocks.state("bowl_pins");
        final BlockState pinsDown = ModBlocks.state("bowl_pins_down");
        final BlockState ret = ModBlocks.state("bowl_return");
        final BlockState seat = ModBlocks.state("bowl_seat");
        final BlockState door = ModBlocks.state("dark_doorway");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState dark = ModBlocks.state("void_filler");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState[][] mask = new BlockState[4][2];
        final BlockState[] score = {ModBlocks.state("bowl_score_0"), ModBlocks.state("bowl_score_1")};

        Gen() {
            for (int n = 0; n < 4; n++)
                for (int p = 0; p < 2; p++) mask[n][p] = ModBlocks.state("bowl_mask_" + n + "_" + p);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, dark);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, dark);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int u = Math.floorMod(x, PX), v = Math.floorMod(z, PZ);
            int pair = Math.floorDiv(x, PX), band = Math.floorDiv(z, PZ);
            long h = Hash.hash(s, pair, band, 0xB0);
            int y = FLOOR + 1;
            boolean laneCol = u == 2 || u == 6;
            boolean gutterCol = u == 1 || u == 3 || u == 5 || u == 7;
            if (v <= 5) {
                // the concourse
                w.set(x, FLOOR, z, carpet);
                if (v == 2 && Math.floorMod(x, 4) == 2) w.set(x, CEIL, z, laneLight);
                if (v == 3 && (u == 1 || u == 2 || u == 6 || u == 7) && Hash.unit(h >>> 3) < 0.8f)
                    w.set(x, y, z, seat.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                if (u == 4 && (v == 4 || v == 5)) w.set(x, y, z, ret.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                if (v == 5 && (u == 2 || u == 3)) w.set(x, FLOOR + 4, z, score[3 - u]);
                if (v == 0) {
                    // the shoe counter against the back of the next hall
                    float r = Hash.unit(h >>> 9);
                    if (r < 0.3f) w.set(x, y, z, counter);
                }
                return;
            }
            if (v <= 29) {
                BlockState f = laneCol ? (v == FOUL ? foul : v == 8 ? dots : v == 13 ? arrows : lane) : gutterCol ? gutter : capping;
                w.set(x, FLOOR, z, f);
                boolean lit = Hash.unit(h >>> 5) < 0.35f;
                if (laneCol && v == 22 && lit) w.set(x, CEIL, z, laneLight);
                if (v >= 24) {
                    // the masking unit over the pin decks
                    w.fill(x, FLOOR + 3, z, x, CEIL - 1, z, v == 24 ? mask[Hash.range(h, 4)][1 - u % 2] : dark);
                    if (u == 0 || u == 4) w.fill(x, y, z, x, FLOOR + 2, z, wall);
                }
                if (v >= 28 && u != 0 && u != 4) {
                    w.set(x, FLOOR, z, pit);
                    w.set(x, FLOOR - 1, z, pit);
                }
                if (v == PINS && laneCol) {
                    boolean stuck = Hash.chance(s, x, band, 0x91, 0.06f);
                    w.set(x, y, z, (stuck ? pinsDown : pins).setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
                return;
            }
            // behind the pits: machinery, and the back wall of the next concourse
            w.fill(x, FLOOR, z, x, CEIL - 1, z, wall);
            if (v == PZ - 1) {
                float r = Hash.unit(h >>> 9);
                if (r < 0.3f) w.fill(x, y + 1, z, x, y + 2, z, shoes);
                if (u == 4 && Hash.unit(h >>> 20) < 0.06f) {
                    w.set(x, y, z, door);
                    w.set(x, y + 1, z, door);
                    w.set(x, y + 2, z + 1, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the balls

    static final class Ball {
        final double x;
        double z;
        final double endZ;
        final Display.BlockDisplay display;
        boolean hit;
        int age;

        Ball(double x, double z, double endZ, Display.BlockDisplay d) {
            this.x = x;
            this.z = z;
            this.endZ = endZ;
            this.display = d;
        }
    }

    static final class Lanes implements LevelBehavior {
        private final List<Ball> balls = new ArrayList<>();
        private final Map<Long, Long> resets = new ConcurrentHashMap<>();
        private final Map<UUID, Long> next = new ConcurrentHashMap<>();
        private int strikes;

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            Long due = next.get(p.getUUID());
            if (due == null) {
                next.put(p.getUUID(), now + 20 * (12 + p.getRandom().nextInt(15)));
                return;
            }
            if (now < due || p.isSpectator()) return;
            next.put(p.getUUID(), now + 20 * (18 + p.getRandom().nextInt(30)));
            // a lane near you: often the one you are standing in
            int pair = Math.floorDiv(p.getBlockX(), PX);
            int u = Math.floorMod(p.getBlockX(), PX);
            int laneX = pair * PX + (u < 4 ? 2 : 6);
            if (p.getRandom().nextFloat() < 0.4f) laneX += (p.getRandom().nextBoolean() ? 1 : -1) * 4 * (1 + p.getRandom().nextInt(2));
            int z0 = Math.floorDiv(p.getBlockZ(), PZ) * PZ;
            roll(level, laneX, z0);
        }

        void roll(ServerLevel level, int laneX, int z0) {
            Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
            CompoundTag tag = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(laneX));
            pos.add(DoubleTag.valueOf(FLOOR + 1));
            pos.add(DoubleTag.valueOf(z0 + FOUL));
            tag.put("Pos", pos);
            tag.put("block_state", NbtUtils.writeBlockState(ModBlocks.state("bowl_ball")));
            tag.putInt("teleport_duration", 2);
            d.load(tag);
            d.moveTo(laneX, FLOOR + 1, z0 + FOUL, 0f, 0f);
            level.addFreshEntity(d);
            balls.add(new Ball(laneX + 0.5, z0 + FOUL + 0.5, z0 + PINS + 0.5, d));
            level.playSound(null, new BlockPos(laneX, FLOOR + 1, z0 + FOUL + 4), ModSounds.get("event.bowling_roll"), SoundSource.AMBIENT, 1.6f, 0.9f + level.random.nextFloat() * 0.2f);
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            for (Iterator<Ball> it = balls.iterator(); it.hasNext(); ) {
                Ball b = it.next();
                b.z += 0.36;
                b.age++;
                b.display.moveTo(b.x - 0.5, FLOOR + 1, b.z - 0.5, 0f, 0f);
                if (b.age == 30) level.playSound(null, BlockPos.containing(b.x, FLOOR + 1, b.z), ModSounds.get("event.bowling_roll"), SoundSource.AMBIENT, 1.4f, 1f);
                if (!b.hit) {
                    for (ServerPlayer p : level.players()) {
                        if (p.isSpectator() || p.isCreative()) continue;
                        double dx = p.getX() - b.x, dz = p.getZ() - b.z;
                        if (dx * dx + dz * dz < 0.9 * 0.9 && p.getY() < FLOOR + 2.2) {
                            p.hurt(level.damageSources().generic(), 5f);
                            p.setDeltaMovement(p.getDeltaMovement().add(new Vec3(dx > 0 ? 0.6 : -0.6, 0.35, 0.5)));
                            p.hurtMarked = true;
                            Sanity.add(p, -5f);
                            b.hit = true;
                        }
                    }
                }
                if (b.z >= b.endZ || b.age > 200) {
                    b.display.discard();
                    it.remove();
                    BlockPos pins = BlockPos.containing(b.x, FLOOR + 1, b.endZ);
                    level.playSound(null, pins, ModSounds.get("event.bowling_strike"), SoundSource.AMBIENT, 2f, 1f);
                    BlockState st = level.getBlockState(pins);
                    if (st.is(ModBlocks.get("bowl_pins"))) {
                        level.setBlock(pins, ModBlocks.state("bowl_pins_down").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_CLIENTS);
                        resets.put(pins.asLong(), now + 90);
                    }
                    strikes++;
                    for (ServerPlayer p : level.players())
                        if (p.distanceToSqr(b.x, FLOOR + 1, b.z) < 48 * 48)
                            p.displayClientMessage(Component.literal("§eSTRIKE! §7Player 2: " + strikes + " in a row."), true);
                }
            }
            for (Iterator<Map.Entry<Long, Long>> it = resets.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> e = it.next();
                if (now < e.getValue()) continue;
                BlockPos pos = BlockPos.of(e.getKey());
                it.remove();
                if (!level.isLoaded(pos)) continue;
                level.setBlock(pos, ModBlocks.state("bowl_pins").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, ModSounds.get("event.bowling_reset"), SoundSource.AMBIENT, 1.4f, 1f);
            }
        }
    }
}
