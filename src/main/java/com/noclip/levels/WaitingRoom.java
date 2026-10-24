package com.noclip.levels;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Waiting Room. Take a number. When the display reaches it, a counter door
 * opens for a few seconds. Miss it and you find yourself in the same room, only
 * bigger: more chairs, more counters, further to walk, and your number is gone.
 *
 * Each size of room is a separate, sealed room ("tier") laid out along the x axis.
 */
public final class WaitingRoom {
    static final int FLOOR = 10, SPAN = 4096, TIERS = 6;
    static final int[] HX = {11, 19, 31, 47, 71, 101};
    static final int[] HZ = {9, 15, 25, 39, 59, 85};
    static final int[] H = {5, 6, 8, 10, 13, 16};

    private WaitingRoom() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("waiting_room")
                .number("").title("The Waiting Room").order(1010)
                .tagline("Please take a number. Please do not miss your turn.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Queue())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.04f).saturation(0.8f).contrast(1.06f).brightness(1.15f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.45f).fog(10f, 90f).fogColor(0x8A9498).bloom(0.4f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 3, 0.3f).dist(10, 30).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "hospital", 2).exit(ExitType.NOCLIP, "dead_mall", 1)
                .exit(ExitType.NOCLIP, "airport", 1)
                .exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "hospital", 2).exit(ExitType.DOOR, "lift_lobby", 2)
                .exit(ExitType.DOOR, "call_center", 1).exit(ExitType.DOOR, "airport", 1).exit(ExitType.DOOR, "level_11", 1)
                .build();
    }

    static int tierOf(int x) {
        return Math.floorDiv(x + SPAN / 2, SPAN);
    }

    static int counters(int t) {
        return 3 + 2 * t;
    }

    /** x of counter j's window (the door is at x + 1). */
    static int counterX(int t, int j) {
        int n = counters(t);
        return t * SPAN + Math.round(-HX[t] + (j + 0.5f) * (2 * HX[t] + 1) / n) - 1;
    }

    static int displayZ(int t) {
        return -HZ[t] + 3;
    }

    static int[] displayXs(int t) {
        if (t < 2) return new int[]{t * SPAN};
        int n = 1 + 2 * ((HX[t] - 8) / 40);
        int[] xs = new int[n];
        for (int j = 0; j < n; j++) xs[j] = t * SPAN + (j - n / 2) * 40;
        return xs;
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("wr_floor");
        final BlockState worn = ModBlocks.state("wr_floor_worn");
        final BlockState upper = ModBlocks.state("wr_wall_upper");
        final BlockState lower = ModBlocks.state("wr_wall_lower");
        final BlockState ceil = ModBlocks.state("wr_ceiling");
        final BlockState light = ModBlocks.state("wr_light");
        final BlockState flicker = ModBlocks.state("wr_light_flicker");
        final BlockState chair = ModBlocks.state("wr_chair");
        final BlockState tipped = ModBlocks.state("wr_chair_tipped");
        final BlockState table = ModBlocks.state("wr_side_table");
        final BlockState post = ModBlocks.state("wr_dispenser_post");
        final BlockState dispenser = ModBlocks.state("wr_ticket_dispenser");
        final BlockState cooler = ModBlocks.state("wr_water_cooler");
        final BlockState plant = ModBlocks.state("wr_plant");
        final BlockState entrance = ModBlocks.state("wr_entrance");
        final BlockState entranceSign = ModBlocks.state("wr_entrance_sign");
        final BlockState door = ModBlocks.state("wr_counter_door");
        final BlockState doorTop = ModBlocks.state("wr_counter_door_top");
        final BlockState window = ModBlocks.state("wr_counter_window");
        final BlockState desk = ModBlocks.state("wr_counter_desk");
        final BlockState clock = ModBlocks.state("wr_clock");
        final BlockState strip = ModBlocks.state("strip_light");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] posters = new BlockState[5];
        final BlockState[] plates = new BlockState[14];

        Gen() {
            for (int n = 0; n < 5; n++) posters[n] = ModBlocks.state("wr_poster_" + n);
            for (int n = 1; n < 14; n++) plates[n] = ModBlocks.state("wr_plate_" + n);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            int top = FLOOR + 20;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                int t = tierOf(x);
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (t < 0 || t >= TIERS) {
                        w.fill(x, FLOOR, z, x, top, z, filler);
                        continue;
                    }
                    int lx = x - t * SPAN, lz = z;
                    int hx = HX[t], hz = HZ[t], h = H[t];
                    if (Math.abs(lx) > hx + 1 || Math.abs(lz) > hz + 1) {
                        w.fill(x, FLOOR, z, x, top, z, filler);
                        continue;
                    }
                    w.fill(x, FLOOR + h + 2, z, x, top, z, filler);
                    boolean wallCol = Math.abs(lx) == hx + 1 || Math.abs(lz) == hz + 1;
                    if (wallCol) {
                        w.set(x, FLOOR, z, floor);
                        w.fill(x, FLOOR + 1, z, x, FLOOR + 2, z, lower);
                        w.fill(x, FLOOR + 3, z, x, FLOOR + h + 1, z, upper);
                        continue;
                    }
                    float wear = Hash.fbm(s ^ 0x3EA, x / 7.0, z / 7.0, 2);
                    boolean path = Math.abs(lx) <= 1 || lz < -hz + 5 || lz > hz - 3;
                    w.set(x, FLOOR, z, (path && wear > 0.45f) || wear > 0.7f ? worn : floor);
                    boolean lamp = Math.floorMod(lx, 4) == 2 && Math.floorMod(lz, 4) == 2;
                    BlockState c = ceil;
                    if (lamp) {
                        float r = Hash.unit(s, x, z, 12);
                        c = r < 0.06f ? light.setValue(BlockStateProperties.LIT, false) : r < 0.16f ? flicker : light;
                    }
                    w.set(x, FLOOR + h + 1, z, c);
                    if (t >= 3 && Math.floorMod(lx, 6) == 3 && Math.floorMod(lz, 6) == 3) {
                        w.set(x, FLOOR + 6, z, strip);
                        w.fill(x, FLOOR + 7, z, x, FLOOR + h, z, chain);
                    }
                    furnish(w, s, t, x, z, lx, lz);
                }
            }
            for (int t = 0; t < TIERS; t++) walls(w, s, t);
        }

        void furnish(ChunkWriter w, long s, int t, int x, int z, int lx, int lz) {
            int hx = HX[t], hz = HZ[t];
            int y = FLOOR + 1;
            int rowStart = -hz + 6;
            if (lz >= rowStart && lz <= hz - 3 && (lz - rowStart) % 3 == 0 && Math.abs(lx) > 1 && Math.abs(lx) <= hx - 2) {
                int m = Math.floorMod(lx, 8);
                float r = Hash.unit(s, x, z, 31);
                if (m >= 1 && m <= 5) {
                    if (r < 0.02f) return;  // a gap, as if someone took the chair
                    if (r < 0.03f) w.set(x, y, z, tipped.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x, z, 4))));
                    else if (r < 0.055f) w.set(x, y, z, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    else w.set(x, y, z, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                } else if (m == 6 && ((lz - rowStart) / 3) % 2 == 0 && r < 0.6f) {
                    w.set(x, y, z, table.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
            }
        }

        /** Wall fittings: counters, displays, dispensers, posters. Clipped to the chunk by the writer. */
        void walls(ChunkWriter w, long s, int t) {
            int x0 = t * SPAN, hx = HX[t], hz = HZ[t], h = H[t];
            if (x0 + hx + 2 < w.minX || x0 - hx - 2 > w.maxX || hz + 2 < w.minZ || -hz - 2 > w.maxZ) return;
            int y = FLOOR + 1;
            int nz = -hz - 1;
            for (int j = 0; j < counters(t); j++) {
                int cx = counterX(t, j);
                w.set(cx - 1, y, nz + 1, desk);
                w.set(cx, y, nz + 1, desk);
                w.fill(cx - 1, y + 1, nz, cx, y + 2, nz, window);
                w.set(cx - 1, y, nz, lower);
                w.set(cx, y, nz, lower);
                w.set(cx, y + 3, nz, plates[j + 1]);
                w.set(cx + 1, y, nz, door);
                w.set(cx + 1, y + 1, nz, doorTop);
            }
            for (int dx : displayXs(t)) display(w, dx, displayZ(t), h, "000", "  ");
            // the way you came in, locked
            for (int x = x0 - 1; x <= x0; x++) {
                w.fill(x, y, hz + 1, x, y + 2, hz + 1, entrance);
                w.set(x, y + 1, hz + 1, entranceSign);
            }
            // dispensers just inside the entrance
            for (int j = 0; j <= t; j++) {
                int dx = x0 + (j % 2 == 0 ? 3 + j * 6 : -4 - (j - 1) * 6);
                if (Math.abs(dx - x0) > hx - 2) continue;
                w.set(dx, y, hz - 1, post);
                w.set(dx, y + 1, hz - 1, dispenser.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
            // water coolers and plants along the side walls
            for (int z = -hz + 4; z <= hz - 4; z += 18) {
                w.set(x0 - hx, y, z, cooler.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                w.set(x0 + hx, y, z + 9 <= hz - 2 ? z + 9 : z, plant.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            }
            w.set(x0 - hx, y, -hz, plant.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            w.set(x0 + hx, y, hz, plant.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            // notices on the side walls and a clock that says 3:07
            for (int z = -hz + 2; z <= hz - 2; z += 7) {
                int n = Hash.range(s, t, z, 5);
                w.set(x0 - hx - 1, y + 2, z, posters[n]);
                w.set(x0 + hx + 1, y + 2, z + 3 > hz ? z : z + 3, posters[(n + 2) % 5]);
            }
            w.set(x0 + hx + 1, y + 3, 0, clock);
            w.set(x0 - 3, y + 2, hz + 1, posters[0]);
            w.set(x0 + 2, y + 2, hz + 1, posters[4]);
        }

        void display(ChunkWriter w, int cx, int z, int h, String serving, String counter) {
            int y1 = FLOOR + h - 1;
            setDisplay(w::set, cx, z, h, serving, counter);
            for (int x = cx - 2; x <= cx + 2; x += 4) w.fill(x, y1 + 1, z, x, FLOOR + h, z, chain);
        }
    }

    interface Setter {
        void set(int x, int y, int z, BlockState st);
    }

    static void setDisplay(Setter w, int cx, int z, int h, String serving, String counter) {
        int y1 = FLOOR + h - 1, y2 = FLOOR + h - 2;
        w.set(cx - 2, y1, z, ModBlocks.state("wr_label_now_0"));
        w.set(cx - 1, y1, z, ModBlocks.state("wr_label_now_1"));
        w.set(cx - 2, y2, z, ModBlocks.state("wr_label_counter_0"));
        w.set(cx - 1, y2, z, ModBlocks.state("wr_label_counter_1"));
        w.set(cx, y2, z, ModBlocks.state("wr_digit_blank"));
        for (int i = 0; i < 3; i++) w.set(cx + i, y1, z, digit(serving.charAt(i)));
        for (int i = 0; i < 2; i++) w.set(cx + 1 + i, y2, z, digit(counter.charAt(i)));
    }

    static BlockState digit(char c) {
        return c >= '0' && c <= '9' ? ModBlocks.state("wr_digit_" + c) : ModBlocks.state("wr_digit_blank");
    }

    // ---------------------------------------------------------------- the queue

    static final class Ticket {
        int number = -1;
        long deadline = -1;
        int counter = -1;
    }

    static final Map<UUID, Ticket> TICKETS = new ConcurrentHashMap<>();
    /** Open counter doors: packed position -> game time they close. */
    static final Map<Long, Long> OPEN = new ConcurrentHashMap<>();
    static int serving = -1;
    static long nextAdvance = -1;

    public static void takeTicket(ServerPlayer p, BlockPos pos) {
        Ticket tk = TICKETS.computeIfAbsent(p.getUUID(), u -> new Ticket());
        if (serving < 0) serving = 20 + p.getRandom().nextInt(60);
        if (tk.number >= 0) {
            p.displayClientMessage(Component.literal("§7You already have number §f" + fmt(tk.number) + "§7. Please wait to be called."), true);
            return;
        }
        tk.number = serving + 2 + p.getRandom().nextInt(3);
        tk.deadline = -1;
        ItemStack ticket = new ItemStack(Items.PAPER);
        ticket.set(DataComponents.CUSTOM_NAME, Component.literal("Ticket No. " + fmt(tk.number)));
        if (!p.getInventory().add(ticket)) p.drop(ticket, false);
        p.level().playSound(null, pos, ModSounds.get("event.waiting_room_ticket"), SoundSource.BLOCKS, 0.9f, 1f);
        p.displayClientMessage(Component.literal("§7Your number is §f" + fmt(tk.number) + "§7. Please wait to be called."), true);
    }

    static String fmt(int n) {
        return String.format("%03d", Math.floorMod(n, 1000));
    }

    static void dropTickets(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(Items.PAPER) && st.has(DataComponents.CUSTOM_NAME) && st.getHoverName().getString().startsWith("Ticket No."))
                inv.setItem(i, ItemStack.EMPTY);
        }
    }

    static void refresh(ServerLevel level, int t, String counter) {
        for (int dx : displayXs(t))
            setDisplay((x, y, z, st) -> level.setBlock(new BlockPos(x, y, z), st, Block.UPDATE_CLIENTS),
                    dx, displayZ(t), H[t], fmt(serving), counter);
    }

    static void door(ServerLevel level, int t, int j, boolean open) {
        BlockPos pos = new BlockPos(counterX(t, j) + 1, FLOOR + 1, -HZ[t] - 1);
        BlockState low = open ? ModBlocks.state("wr_counter_door_open") : ModBlocks.state("wr_counter_door");
        BlockState up = open ? ModBlocks.state("wr_counter_door_open") : ModBlocks.state("wr_counter_door_top");
        level.setBlock(pos, low, Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(), up, Block.UPDATE_CLIENTS);
    }

    static final class Queue implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            TICKETS.put(p.getUUID(), new Ticket());
            dropTickets(p);
            int hx = HX[0], hz = HZ[0];
            int x = -hx + 2 + p.getRandom().nextInt(2 * hx - 3);
            p.teleportTo(level, x + 0.5, FLOOR + 1, hz - 2 + 0.5, 180f, 0f);
            if (serving < 0) serving = 20 + p.getRandom().nextInt(60);
            refresh(level, 0, "  ");
            p.displayClientMessage(Component.literal("§7Please take a number."), true);
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            for (Iterator<Map.Entry<Long, Long>> it = OPEN.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> e = it.next();
                if (now < e.getValue()) continue;
                BlockPos pos = BlockPos.of(e.getKey());
                level.setBlock(pos, ModBlocks.state("wr_counter_door"), Block.UPDATE_CLIENTS);
                level.setBlock(pos.above(), ModBlocks.state("wr_counter_door_top"), Block.UPDATE_CLIENTS);
                it.remove();
            }
            if (serving < 0) serving = 20 + level.random.nextInt(60);
            if (nextAdvance < 0) nextAdvance = now + 200;
            if (now < nextAdvance) return;
            serving++;
            nextAdvance = now + 20 * (14 + level.random.nextInt(10));
            int[] counterFor = new int[TIERS];
            for (int t = 0; t < TIERS; t++) counterFor[t] = level.random.nextInt(counters(t));
            boolean[] refreshed = new boolean[TIERS];
            for (ServerPlayer p : level.players()) {
                int t = Math.max(0, Math.min(TIERS - 1, tierOf(p.getBlockX())));
                int j = counterFor[t];
                Ticket tk = TICKETS.computeIfAbsent(p.getUUID(), u -> new Ticket());
                if (!refreshed[t]) {
                    refreshed[t] = true;
                    refresh(level, t, String.format("%02d", j + 1));
                }
                level.playSound(null, p.blockPosition().above(3), ModSounds.get("event.waiting_room_call"), SoundSource.AMBIENT, 1.2f, 1f);
                p.displayClientMessage(Component.literal("§cNOW SERVING " + fmt(serving) + "  §7·  COUNTER " + (j + 1)), true);
                if (tk.number == serving) {
                    tk.counter = j;
                    tk.deadline = now + 220 + 90L * t;
                    door(level, t, j, true);
                    BlockPos dp = new BlockPos(counterX(t, j) + 1, FLOOR + 1, -HZ[t] - 1);
                    OPEN.put(dp.asLong(), tk.deadline);
                    level.playSound(null, dp, ModSounds.get("event.waiting_room_door"), SoundSource.BLOCKS, 1.5f, 1f);
                    p.sendSystemMessage(Component.literal("§eTicket " + fmt(serving) + ", please proceed to counter " + (j + 1) + "."));
                }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0) return;
            Ticket tk = TICKETS.get(p.getUUID());
            if (tk == null || tk.deadline < 0 || level.getGameTime() < tk.deadline) return;
            missed(p, level, tk);
        }

        void missed(ServerPlayer p, ServerLevel level, Ticket tk) {
            int t = Math.max(0, Math.min(TIERS - 1, tierOf(p.getBlockX())));
            p.displayClientMessage(Component.literal("§7Ticket " + fmt(tk.number) + ". §cNo show."), true);
            tk.number = -1;
            tk.deadline = -1;
            dropTickets(p);
            Sanity.add(p, -8f);
            level.playSound(null, p.blockPosition(), ModSounds.get("event.waiting_room_missed"), SoundSource.AMBIENT, 1.2f, 1f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 1f));
            int nt = Math.min(TIERS - 1, t + 1);
            int lx = p.getBlockX() - t * SPAN, lz = p.getBlockZ();
            if (nt == t) lz = HZ[t] - 2;  // as big as it gets: you are simply sent to the back
            lx = Math.max(-HX[nt] + 1, Math.min(HX[nt] - 1, lx));
            lz = Math.max(-HZ[nt] + 1, Math.min(HZ[nt] - 1, lz));
            BlockPos target = free(level, new BlockPos(nt * SPAN + lx, FLOOR + 1, lz));
            level.getChunk(target);
            p.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, p.getYRot(), p.getXRot());
            refresh(level, nt, "  ");
        }

        static BlockPos free(ServerLevel level, BlockPos c) {
            for (int r = 0; r <= 3; r++)
                for (int dx = -r; dx <= r; dx++)
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos q = c.offset(dx, 0, dz);
                        level.getChunk(q);
                        if (level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                                && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty())
                            return q;
                    }
            return c;
        }
    }
}
