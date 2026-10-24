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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dead Arcade. Blacklight carpet, rows of dark cabinets that wake up as you walk
 * past and go dark again behind you. Stare into a running screen and it pays out
 * prize tickets, and takes a little of your mind each time. Twelve tickets buy
 * the only prize the counter at the back still has: the door behind it.
 */
public final class Arcade {
    static final int FLOOR = 10, CEIL = FLOOR + 6, ZX = 26, ZZ = 28, PRICE = 12;
    static final String[] GAMES = {"blaster", "muncher", "fury", "knockout", "bricks", "level0"};
    static final String TICKET = "Prize Ticket";

    private Arcade() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("arcade")
                .number("").title("Dead Arcade").order(1080)
                .tagline("Insert coin. Twelve tickets for the door.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Cabinets())
                .visuals(new Visuals().tint(1.02f, 0.94f, 1.12f).saturation(1.15f).contrast(1.15f).brightness(1.2f)
                        .grain(0.07f).aberration(0.0025f).vignette(0.6f).fog(5f, 44f).fogColor(0x08060E).bloom(0.8f)
                        .scanlines(0.08f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.2f).dist(14, 30).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(26, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "dead_mall", 2).exit(ExitType.NOCLIP, "playground", 1)
                .exit(ExitType.NOCLIP, "bowling_alley", 1)
                .exit(ExitType.DOOR, "level_fun", 2).exit(ExitType.DOOR, "bowling_alley", 2).exit(ExitType.DOOR, "movie_theater", 1)
                .exit(ExitType.DOOR, "playground", 1)
                .build();
    }

    enum Zone { CABINETS, CLAWS, SKEE, PRIZES }

    static Zone zone(long s, int zi, int zk) {
        float r = Hash.unit(s, zi, zk, 0x2A0E);
        if (r < 0.66f) return Zone.CABINETS;
        if (r < 0.80f) return Zone.CLAWS;
        if (r < 0.90f) return Zone.SKEE;
        return Zone.PRIZES;
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("arc_carpet");
        final BlockState carpetB = ModBlocks.state("arc_carpet_b");
        final BlockState ceil = ModBlocks.state("arc_ceiling");
        final BlockState wall = ModBlocks.state("arc_wall");
        final BlockState pink = ModBlocks.state("arc_neon_pink");
        final BlockState cyan = ModBlocks.state("arc_neon_cyan");
        final BlockState hockey = ModBlocks.state("arc_hockey");
        final BlockState clawLow = ModBlocks.state("arc_claw_lower");
        final BlockState clawUp = ModBlocks.state("arc_claw_upper");
        final BlockState lane = ModBlocks.state("arc_skee_lane");
        final BlockState target = ModBlocks.state("arc_skee_target");
        final BlockState shelf = ModBlocks.state("arc_prize_shelf");
        final BlockState counter = ModBlocks.state("arc_prize_counter");
        final BlockState bell = ModBlocks.state("arc_prize_bell");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] lower = new BlockState[GAMES.length], upper = new BlockState[GAMES.length];

        Gen() {
            for (int i = 0; i < GAMES.length; i++) {
                lower[i] = ModBlocks.state("arc_cab_lower_" + GAMES[i]);
                upper[i] = ModBlocks.state("arc_cab_upper_" + GAMES[i]);
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceil);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, FLOOR, z, Hash.noise(s ^ 0xCA, x / 9.0, z / 9.0) > 0.6f ? carpetB : carpet);
                    int v = Math.floorMod(z, 7);
                    if (v == 4 && Math.floorMod(x, 4) == 0)
                        w.set(x, CEIL - 1, z, (Math.floorMod(x, 8) == 0 ? pink : cyan).setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    column(w, s, x, z, v);
                }
            for (int zi = Math.floorDiv(w.minX, ZX) - 1; zi <= Math.floorDiv(w.maxX, ZX) + 1; zi++)
                for (int zk = Math.floorDiv(w.minZ, ZZ) - 1; zk <= Math.floorDiv(w.maxZ, ZZ) + 1; zk++)
                    if (zone(s, zi, zk) == Zone.PRIZES) prizes(w, zi * ZX, zk * ZZ);
        }

        void cabinet(ChunkWriter w, long s, int x, int z, Direction f) {
            float r = Hash.unit(s, x, z, 0xCAB);
            if (r < 0.06f) return;
            int g = r < 0.08f ? 5 : Hash.range(s, x, z, 0xCA7, 5);
            Direction d = r < 0.09f ? f.getOpposite() : f;  // one of them faces the wrong way
            w.set(x, FLOOR + 1, z, lower[g].setValue(HorizontalDirectionalBlock.FACING, d));
            w.set(x, FLOOR + 2, z, upper[g].setValue(HorizontalDirectionalBlock.FACING, d));
        }

        void column(ChunkWriter w, long s, int x, int z, int v) {
            int u = Math.floorMod(x, ZX);
            Zone zn = zone(s, Math.floorDiv(x, ZX), Math.floorDiv(z, ZZ));
            if (u <= 1) return;  // cross aisle
            int y = FLOOR + 1;
            switch (zn) {
                case CABINETS -> {
                    if (v == 0) cabinet(w, s, x, z, Direction.NORTH);
                    else if (v == 1) cabinet(w, s, x, z, Direction.SOUTH);
                }
                case CLAWS -> {
                    if ((v == 0 || v == 1) && u % 2 == 0) {
                        Direction d = v == 0 ? Direction.NORTH : Direction.SOUTH;
                        w.set(x, y, z, clawLow.setValue(HorizontalDirectionalBlock.FACING, d));
                        w.set(x, y + 1, z, clawUp.setValue(HorizontalDirectionalBlock.FACING, d));
                    } else if (v == 4 && u % 6 == 3) {
                        w.set(x, y, z, hockey.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                        w.set(x + 1, y, z, hockey.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    }
                }
                case SKEE -> {
                    if (u % 3 == 1) {
                        if (v >= 3 && v <= 5) w.set(x, y, z, lane.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                        else if (v == 2) {
                            w.set(x, y, z, target.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                            w.set(x, y + 1, z, target.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        }
                    }
                }
                case PRIZES -> {
                }
            }
        }

        /** The prize counter: shelves of prizes behind a counter, a bell, and a gap at the end to walk round. */
        void prizes(ChunkWriter w, int x0, int z0) {
            int y = FLOOR + 1;
            for (int u = 3; u <= 22; u++) {
                w.fill(x0 + u, y, z0 + 14, x0 + u, CEIL - 1, z0 + 14, u == 3 || u == 22 ? wall : shelf);
                if (u >= 4 && u <= 20) w.set(x0 + u, y, z0 + 10, counter);
            }
            for (int v = 10; v <= 14; v++) {
                w.fill(x0 + 3, y, z0 + v, x0 + 3, CEIL - 1, z0 + v, wall);
                w.fill(x0 + 22, y, z0 + v, x0 + 22, CEIL - 1, z0 + v, wall);
            }
            w.set(x0 + 12, y + 1, z0 + 10, bell);
            for (int u = 4; u <= 20; u += 2) {
                w.set(x0 + u, y, z0 + 4, clawLow.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                w.set(x0 + u, y + 1, z0 + 4, clawUp.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
        }
    }

    // ---------------------------------------------------------------- tickets and the prize

    static int countTickets(ServerPlayer p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (isTicket(st)) n += st.getCount();
        }
        return n;
    }

    static boolean isTicket(ItemStack st) {
        return st.is(Items.PAPER) && st.has(DataComponents.CUSTOM_NAME) && st.getHoverName().getString().equals(TICKET);
    }

    static void giveTickets(ServerPlayer p, int n) {
        ItemStack t = new ItemStack(Items.PAPER, n);
        t.set(DataComponents.CUSTOM_NAME, Component.literal(TICKET));
        if (!p.getInventory().add(t)) p.drop(t, false);
    }

    public static void redeem(ServerPlayer p, BlockPos bell) {
        ServerLevel level = p.serverLevel();
        int have = countTickets(p);
        if (have < PRICE && !p.isCreative()) {
            level.playSound(null, bell, ModSounds.get("event.arcade_denied"), SoundSource.BLOCKS, 1f, 1f);
            p.displayClientMessage(Component.literal("§d" + PRICE + " tickets for a prize. §7You have " + have + "."), true);
            return;
        }
        int left = PRICE;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0 && !p.isCreative(); i++) {
            ItemStack st = inv.getItem(i);
            if (!isTicket(st)) continue;
            int take = Math.min(left, st.getCount());
            st.shrink(take);
            left -= take;
        }
        BlockPos door = new BlockPos(bell.getX(), FLOOR + 1, bell.getZ() + 4);
        level.setBlock(door, ModBlocks.state("dark_doorway"), Block.UPDATE_ALL);
        level.setBlock(door.above(), ModBlocks.state("dark_doorway"), Block.UPDATE_ALL);
        level.playSound(null, bell, ModSounds.get("event.arcade_fanfare"), SoundSource.BLOCKS, 1.2f, 1f);
        p.displayClientMessage(Component.literal("§dYou win: §fthe door behind the counter."), true);
    }

    static final class Cabinets implements LevelBehavior {
        /** Running cabinets: packed position -> last time someone was near. */
        private final Map<Long, Long> running = new ConcurrentHashMap<>();
        private final Map<UUID, Integer> stare = new ConcurrentHashMap<>();
        private Map<Block, Block> toOn, toOff;

        void maps() {
            if (toOn != null) return;
            toOn = new HashMap<>();
            toOff = new HashMap<>();
            for (String g : GAMES) {
                Block off = ModBlocks.get("arc_cab_upper_" + g), on = ModBlocks.get("arc_cab_upper_" + g + "_on");
                toOn.put(off, on);
                toOff.put(on, off);
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            maps();
            long now = level.getGameTime();
            if (p.tickCount % 5 == 0) {
                int woke = 0;
                for (int dx = -3; dx <= 3 && woke < 2; dx++)
                    for (int dz = -3; dz <= 3 && woke < 2; dz++) {
                        BlockPos pos = new BlockPos(p.getBlockX() + dx, FLOOR + 2, p.getBlockZ() + dz);
                        BlockState st = level.getBlockState(pos);
                        Block on = toOn.get(st.getBlock());
                        if (on != null) {
                            level.setBlock(pos, on.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                                    st.getValue(HorizontalDirectionalBlock.FACING)), Block.UPDATE_CLIENTS);
                            level.playSound(null, pos, ModSounds.get("event.arcade_poweron"), SoundSource.BLOCKS, 0.6f, 0.9f + p.getRandom().nextFloat() * 0.2f);
                            woke++;
                        }
                        if (toOff.containsKey(st.getBlock()) || on != null) running.put(pos.asLong(), now);
                    }
            }
            if (p.tickCount % 10 == 0 && !p.isSpectator()) {
                HitResult hit = p.pick(4.5, 1f, false);
                boolean looking = hit instanceof BlockHitResult bh && toOff.containsKey(level.getBlockState(bh.getBlockPos()).getBlock());
                int n = looking ? stare.getOrDefault(p.getUUID(), 0) + 10 : 0;
                if (n >= 70) {
                    int t = 1 + p.getRandom().nextInt(3);
                    giveTickets(p, t);
                    Sanity.add(p, -3f);
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.arcade_tickets"), SoundSource.BLOCKS, 0.9f, 1f);
                    p.displayClientMessage(Component.literal("§d+" + t + " TICKETS §7(" + countTickets(p) + ")"), true);
                    n = -40;
                }
                stare.put(p.getUUID(), n);
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            if (level.getGameTime() % 20 != 0) return;
            maps();
            long now = level.getGameTime();
            for (Iterator<Map.Entry<Long, Long>> it = running.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> e = it.next();
                if (now - e.getValue() < 160) continue;
                BlockPos pos = BlockPos.of(e.getKey());
                it.remove();
                if (!level.isLoaded(pos)) continue;
                BlockState st = level.getBlockState(pos);
                Block off = toOff.get(st.getBlock());
                if (off != null) level.setBlock(pos, off.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                        st.getValue(HorizontalDirectionalBlock.FACING)), Block.UPDATE_CLIENTS);
            }
        }
    }
}
