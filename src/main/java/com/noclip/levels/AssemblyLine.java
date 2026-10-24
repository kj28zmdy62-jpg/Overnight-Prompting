package com.noclip.levels;

import com.noclip.block.AsmBeltBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assembly Line. A factory floor that goes on forever, making office chairs. Raised belts
 * run the length of the hall between rows of orange robot arms; finished chairs stack up
 * to the trusses. Step on a belt and it carries you, toward the presses, toward the
 * reject chutes. When the shift horn sounds the line speeds up. And one unit in a few
 * thousand is not a chair: it is wrapped like one, sitting like one, and it breathes.
 * It is never where you last saw it.
 */
public final class AssemblyLine {
    static final String ID = "assembly_line";
    static final int FLOOR = 8, CEIL = 18, P = 12, SEG = 40;

    private AssemblyLine() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Assembly Line").order(2040)
                .tagline("The line never stops. Don't ride the belts. Count the chairs.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Line())
                .visuals(new Visuals().tint(1.08f, 1.0f, 0.86f).saturation(0.78f).contrast(1.12f).brightness(1.32f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.5f).fog(5f, 46f).fogColor(0x2A2620).bloom(0.6f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("mannequin"), 3, 0.3f).dist(14, 30).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(26, 40).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.4f).dark().dist(12, 24).weight(1))
                .exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "container_yard", 2).exit(ExitType.DOOR, "call_center", 1)
                .exit(ExitType.FALL, "steel_mill", 2).exit(ExitType.FALL, "boiler_room", 1)
                .exit(ExitType.NOCLIP, "level_4", 2).exit(ExitType.NOCLIP, "server_farm", 1).exit(ExitType.NOCLIP, "level_3", 1)
                .build();
    }

    /** Which way line row r runs. */
    static Direction runs(long s, int row) {
        return (Hash.hash(s, row, 77) & 1) == 0 ? Direction.EAST : Direction.WEST;
    }

    static boolean chute(long s, int seg, int row) {
        return Hash.unit(s, seg, row, 31) < 0.2f;
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("asm_floor");
        final BlockState line = ModBlocks.state("asm_floor_line");
        final BlockState belt = ModBlocks.state("asm_belt");
        final BlockState armBase = ModBlocks.state("asm_arm_base");
        final BlockState armHead = ModBlocks.state("asm_arm_head");
        final BlockState press = ModBlocks.state("asm_press");
        final BlockState pressHead = ModBlocks.state("asm_press_head");
        final BlockState column = ModBlocks.state("asm_column");
        final BlockState ceiling = ModBlocks.state("asm_ceiling");
        final BlockState lamp = ModBlocks.state("asm_lamp");
        final BlockState wrapped = ModBlocks.state("asm_wrapped");
        final BlockState bin = ModBlocks.state("asm_bin");
        final BlockState counter = ModBlocks.state("asm_counter");
        final BlockState door = ModBlocks.state("asm_shipping_door");
        final BlockState booth = ModBlocks.state("asm_booth_wall");
        final BlockState pallet = ModBlocks.state("asm_pallet");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState sign = ModBlocks.state("exit_sign");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int row = Math.floorDiv(z, P), lz = Math.floorMod(z, P);
            int seg = Math.floorDiv(x, SEG), lx = Math.floorMod(x, SEG);
            Direction run = runs(s, row);
            boolean chute = chute(s, seg, row);
            boolean pressZone = !chute && (lx == 20 || lx == 21);
            boolean chuteZone = chute && (lx == 30 || lx == 31);
            w.set(x, FLOOR, z, lz == 0 || lz == 5 ? line : floor);
            // trusses and lamps
            if (lz == 8 && Math.floorMod(x, 6) == 3 || (lz == 3 && Math.floorMod(x, 5) == 0 && !pressZone)) {
                boolean dead = Hash.unit(s, x, z, 3) < 0.08f;
                w.set(x, CEIL - 1, z, chain);
                if (!dead) w.set(x, CEIL - 2, z, lamp);
                else w.set(x, CEIL - 2, z, chain);
            }
            if (lz == 2 || lz == 3) {
                if (chuteZone) {
                    // the belt stops at a hole: rejects fall through
                    w.fill(x, FLOOR - 4, z, x, FLOOR, z, ModBlocks.AIR);
                    w.set(x, FLOOR - 5, z, pit);
                    return;
                }
                w.set(x, FLOOR + 1, z, OutsideKit.facing(belt, run.getOpposite()));
                if (pressZone) w.set(x, FLOOR + 4, z, pressHead);
                // the product, every few blocks, facing down the line
                int phase = lz == 2 ? 0 : 2;
                if (Math.floorMod(x, 4) == phase && !pressZone && !(chute && lx > 26 && lx < 30)) {
                    float r = Hash.unit(s, x, z, 9);
                    if (r < 0.006f) w.set(x, FLOOR + 2, z, OutsideKit.facing(wrapped, run));
                    else if (r < 0.9f) w.set(x, FLOOR + 2, z, OutsideKit.facing(chair, run));
                }
                return;
            }
            if (lz == 1 || lz == 4) {
                if (pressZone || (chute && (lx == 30 || lx == 31))) {
                    w.fill(x, FLOOR + 1, z, x, FLOOR + 4, z, press);
                    w.set(x, FLOOR + 5, z, press);
                    if (lz == 1) w.fill(x, FLOOR + 5, z + 1, x, FLOOR + 5, z + 2, press);
                    return;
                }
                if (Math.floorMod(x, 8) == (lz == 1 ? 3 : 7) && Hash.unit(s, x, z, 4) > 0.1f) {
                    Direction reach = lz == 1 ? Direction.SOUTH : Direction.NORTH;
                    w.set(x, FLOOR + 1, z, OutsideKit.facing(armBase, reach));
                    w.set(x, FLOOR + 3, z, OutsideKit.facing(armHead, reach));
                }
                return;
            }
            if (lz == 8 && Math.floorMod(x, 16) == 0) {
                for (int y = FLOOR + 1; y < CEIL; y++)
                    w.set(x, y, z, y == FLOOR + 3 && Hash.unit(s, x, z, 5) < 0.5f ? counter : column);
                return;
            }
            if (lz >= 6) open(w, s, x, z, lx, lz, seg, row, run);
        }

        /** The open floor between lines: stacks of finished chairs, bins of parts, a shipping office. */
        void open(ChunkWriter w, long s, int x, int z, int lx, int lz, int seg, int row, Direction run) {
            boolean office = Hash.unit(s, seg, row, 41) < 0.12f;
            if (office && lx >= 4 && lx <= 9 && lz >= 7 && lz <= 10) {
                boolean doorway = lz == 7 && lx == 6;
                for (int y = FLOOR + 1; y <= FLOOR + 3; y++) w.set(x, y, z, doorway && y <= FLOOR + 2 ? door : booth);
                w.set(x, FLOOR + 4, z, booth);
                if (lz == 7 && lx == 7) w.set(x, FLOOR + 3, z - 1, OutsideKit.facing(sign, Direction.SOUTH));
                return;
            }
            // chair stacks: piles of finished units in 3x2 lots, two or three high
            int bx = Math.floorDiv(x, 4), bz = Math.floorDiv(lz - 6, 3);
            int ix = Math.floorMod(x, 4), iz = Math.floorMod(lz - 6, 3);
            long h = Hash.hash(s, bx, row * 4L + bz, 51);
            float r = Hash.unit(h);
            if (ix == 3 || iz == 2) return;
            if (r < 0.28f) {
                int high = 1 + Hash.range(Hash.hash(s, x, z, 52), 3);
                Direction f = Direction.from2DDataValue(Hash.range(Hash.hash(s, x, z, 53), 4));
                w.set(x, FLOOR + 1, z, pallet);
                for (int k = 0; k < high; k++) w.set(x, FLOOR + 2 + k, z, OutsideKit.facing(chair, k % 2 == 0 ? f : f.getOpposite()));
            } else if (r < 0.40f) {
                w.set(x, FLOOR + 1, z, OutsideKit.facing(bin, Direction.NORTH));
                if (Hash.unit(s, x, z, 54) < 0.5f) w.set(x, FLOOR + 2, z, OutsideKit.facing(bin, Direction.NORTH));
            } else if (r < 0.42f && ix == 1 && iz == 0) {
                // one chair set apart from everything, turned to face the line
                w.set(x, FLOOR + 1, z, OutsideKit.facing(chair, Direction.SOUTH));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** Line speed-ups, the presses, and the unit that is not a chair. */
    static final class Line implements LevelBehavior {
        /** {boost until, next horn} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Map<ResourceKey<Level>, Set<Long>> NOTICED = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, level.getGameTime() + 2400 + level.random.nextInt(2400)});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (now < st[1]) return;
            st[0] = now + 700;
            st[1] = now + 3600 + level.random.nextInt(3000);
            for (ServerPlayer p : level.players()) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.assembly_line_horn"), SoundSource.AMBIENT, 1.6f, 1f);
                p.displayClientMessage(Component.literal("LINE SPEED INCREASE. QUOTA NOT MET.")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            long now = level.getGameTime();
            boolean boost = now < state(level)[0];
            BlockPos feet = p.blockPosition();
            BlockState under = level.getBlockState(feet.below());
            if (under.getBlock() instanceof AsmBeltBlock && p.onGround()) {
                if (boost && p.tickCount % 4 == 0) {
                    Direction d = AsmBeltBlock.runs(under);
                    Vec3 v = p.getDeltaMovement();
                    if (v.x * d.getStepX() + v.z * d.getStepZ() < 0.4) {
                        p.setDeltaMovement(v.add(d.getStepX() * 0.22, 0, d.getStepZ() * 0.22));
                        p.hurtMarked = true;
                    }
                }
                // the presses: anyone carried under a ram is stamped
                if (level.getBlockState(feet.above(2)).is(ModBlocks.get("asm_press_head")) && p.tickCount % (boost ? 8 : 14) == 0) {
                    level.playSound(null, feet, ModSounds.get("event.assembly_line_stamp"), SoundSource.BLOCKS, 1.5f, 0.9f + level.random.nextFloat() * 0.2f);
                    if (!p.isCreative()) p.hurt(level.damageSources().generic(), 5f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.6f));
                }
            }
            if ((p.tickCount + p.getId()) % 10 == 0) units(p, level);
        }

        /** The wrapped thing: looking at it costs you; once you've seen one, it moves while you aren't looking. */
        private static void units(ServerPlayer p, ServerLevel level) {
            Set<Long> noticed = NOTICED.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
            Block wrapped = ModBlocks.get("asm_wrapped");
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            int y = FLOOR + 2;
            for (int x = p.getBlockX() - 10; x <= p.getBlockX() + 10; x++) {
                for (int z = p.getBlockZ() - 10; z <= p.getBlockZ() + 10; z++) {
                    int lz = Math.floorMod(z, P);
                    if (lz != 2 && lz != 3) continue;
                    m.set(x, y, z);
                    if (!level.getBlockState(m).is(wrapped)) continue;
                    long key = m.asLong();
                    if (OutsideKit.seen(p, m.immutable(), 0.93)) {
                        if (noticed.add(key)) {
                            level.playSound(null, m, ModSounds.get("event.assembly_line_breath"), SoundSource.AMBIENT, 1.0f, 1f);
                            Sanity.add(p, -6f);
                            p.displayClientMessage(Component.literal("That is not a chair.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                        }
                    } else if (noticed.contains(key) && p.tickCount % 60 < 10) {
                        // shuffle along the belt to the next free slot, out of sight
                        BlockState st = level.getBlockState(m);
                        Direction d = runs(OutsideKit.seed(level, ID), Math.floorDiv(z, P));
                        BlockPos to = m.immutable().relative(d, 4);
                        BlockState there = level.getBlockState(to);
                        if (!level.isLoaded(to) || !(there.isAir() || there.is(ModBlocks.get("office_chair")))) continue;
                        if (OutsideKit.seen(p, to, 0.6)) continue;
                        if (!(level.getBlockState(to.below()).getBlock() instanceof AsmBeltBlock)) continue;
                        level.setBlock(to, st, Block.UPDATE_ALL);
                        level.setBlock(m.immutable(), there, Block.UPDATE_ALL);
                        noticed.remove(key);
                        noticed.add(to.asLong());
                        return;
                    }
                }
            }
        }
    }
}
