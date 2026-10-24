package com.noclip.levels;

import com.noclip.block.BingoCardBlock;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bingo Hall. Rows of folding tables under smoke-stained tiles, a card at every seat,
 * a caller's podium with nobody behind it. The numbers are called anyway, and the
 * cards get marked when you are not looking. Somebody in here is going to win. Claim
 * a card of your own and race them: get a full card first and the prize room opens.
 * Let the empty seats win, and the lights go out while they celebrate.
 */
public final class BingoHall {
    public static final String ID = "bingo_hall";
    static final int FLOOR = 10, CEIL = 17, CX = 26, CZ = 30;

    private BingoHall() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Bingo Hall").order(1220)
                .tagline("Eyes down. Somebody here is about to win.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Caller())
                .visuals(new Visuals().tint(1.06f, 1.04f, 0.86f).saturation(0.78f).contrast(1.05f).brightness(1.05f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.55f).bloom(0.5f).fog(4f, 40f).fogColor(0x7A7454))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(20, 34).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.08f).dist(18, 34).weight(1))
                .exit(ExitType.NOCLIP, "ballroom", 2).exit(ExitType.NOCLIP, "waiting_room", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.DOOR, "ballroom", 2).exit(ExitType.DOOR, "waiting_room", 2).exit(ExitType.DOOR, "arcade", 1)
                .exit(ExitType.DOOR, "level_fun", 1).exit(ExitType.DOOR, "school", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState lino = ModBlocks.state("bingo_lino");
        final BlockState panel = ModBlocks.state("bingo_panel");
        final BlockState wall = ModBlocks.state("bingo_wall");
        final BlockState ceiling = ModBlocks.state("bingo_ceiling");
        final BlockState tube = ModBlocks.state("bingo_tube");
        final BlockState tubeBad = ModBlocks.state("bingo_tube_bad");
        final BlockState stage = ModBlocks.state("bingo_stage");
        final BlockState curtain = ModBlocks.state("bingo_curtain");
        final BlockState board = ModBlocks.state("bingo_board");
        final BlockState table = ModBlocks.state("bingo_table");
        final BlockState chair = ModBlocks.state("bingo_chair");
        final BlockState card = ModBlocks.state("bingo_card");
        final BlockState cage = ModBlocks.state("bingo_cage");
        final BlockState podium = ModBlocks.state("bingo_podium");
        final BlockState ashtray = ModBlocks.state("bingo_ashtray");
        final BlockState prize = ModBlocks.state("bingo_prize_locked");
        final BlockState fire = ModBlocks.state("bingo_fire_exit");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            int y = FLOOR + 1;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, lino);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, CX), lz = Math.floorMod(z, CZ);
                    boolean wx = lx == 0 && !(lz >= 12 && lz <= 15), wz = lz == 0;
                    if (wx || wz) {
                        w.set(x, y, z, panel);
                        w.set(x, y + 1, z, panel);
                        for (int yy = y + 2; yy < CEIL; yy++) w.set(x, yy, z, wall);
                    } else if (lx % 4 == 2 && lz % 5 == 3) {
                        w.set(x, CEIL, z, Hash.chance(s, x, z, 0xB7, 0.1f) ? tubeBad : tube);
                    }
                }
            int i0 = Math.floorDiv(w.minX, CX), i1 = Math.floorDiv(w.maxX, CX);
            int k0 = Math.floorDiv(w.minZ, CZ), k1 = Math.floorDiv(w.maxZ, CZ);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) hall(w, s, i, k, i * CX, k * CZ);
        }

        void hall(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int y = FLOOR + 1;
            for (int lx = 6; lx <= 19; lx++)
                for (int lz = 1; lz <= 4; lz++) w.set(x0 + lx, y, z0 + lz, stage);
            for (int lx = 4; lx <= 21; lx++)
                for (int yy = y + 2; yy <= y + 5; yy++) w.set(x0 + lx, yy, z0, board);
            for (int lx : new int[]{3, 22})
                for (int yy = y; yy < CEIL; yy++) w.set(x0 + lx, yy, z0 + 1, curtain);
            w.set(x0 + 12, y + 1, z0 + 4, podium.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            w.set(x0 + 14, y + 1, z0 + 3, cage);
            w.set(x0 + 23, y, z0, prize);
            w.set(x0 + 23, y + 1, z0, prize);
            if (Hash.chance(s, i, k, 0xF1E, 0.08f)) {
                w.set(x0 + 1, y, z0, fire);
                w.set(x0 + 1, y + 1, z0, fire);
            }
            for (int lz : new int[]{9, 14, 19, 24}) {
                for (int lx = 3; lx <= 22; lx++) {
                    if (lx == 12 || lx == 13) continue;
                    w.set(x0 + lx, y, z0 + lz, table);
                    long h = Hash.hash(s, x0 + lx, z0 + lz, 0xCA4D);
                    for (int side = -1; side <= 1; side += 2) {
                        Direction face = side < 0 ? Direction.SOUTH : Direction.NORTH;
                        int cz = z0 + lz + side;
                        float r = Hash.unit(h >>> (side < 0 ? 0 : 12));
                        if (r < 0.9f) {
                            w.set(x0 + lx, y, cz, chair.setValue(HorizontalDirectionalBlock.FACING,
                                    r < 0.08f ? Direction.from2DDataValue(Hash.range(h >>> 24, 4)) : face));
                        }
                    }
                    if (Hash.unit(h >>> 30) < 0.08f) w.set(x0 + lx, y + 1, z0 + lz, ashtray);
                    else w.set(x0 + lx, y + 1, z0 + lz, card.setValue(BingoCardBlock.DAUBS, Hash.range(h >>> 36, 3))
                            .setValue(HorizontalDirectionalBlock.FACING, Hash.unit(h >>> 40) < 0.5f ? Direction.NORTH : Direction.SOUTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the game

    static final class Game {
        final List<Integer> called = new ArrayList<>();
        int ghost;
        long nextCall;
        BlockPos mine;
        UUID owner;
        boolean over;
    }

    static final Map<Long, Game> GAMES = new HashMap<>();

    static long hallKey(BlockPos p) {
        return ((long) Math.floorDiv(p.getX(), CX) << 32) ^ (Math.floorDiv(p.getZ(), CZ) & 0xffffffffL);
    }

    static BlockPos origin(BlockPos p) {
        return new BlockPos(Math.floorDiv(p.getX(), CX) * CX, FLOOR + 1, Math.floorDiv(p.getZ(), CZ) * CZ);
    }

    static String letter(int n) {
        return "BINGO".substring((n - 1) / 15, (n - 1) / 15 + 1);
    }

    public static void onCard(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().location().getPath().equals(ID)) return;
        Game g = GAMES.computeIfAbsent(hallKey(pos), k -> new Game());
        if (g.mine != null && g.owner != null && g.owner.equals(p.getUUID())) {
            p.displayClientMessage(Component.literal("You already have a card. Eyes down.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BlockState st = level.getBlockState(pos);
        level.setBlock(pos, st.setValue(BingoCardBlock.DAUBS, 0), Block.UPDATE_CLIENTS);
        g.mine = pos.immutable();
        g.owner = p.getUUID();
        EmptyBuildingKit.clearDisplays(level, Vec3.atCenterOf(pos), 2, "bingo_mine");
        EmptyBuildingKit.text(level, new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5), 0f,
                Component.literal(p.getGameProfile().getName().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                "center", 0.4f, 0x00000000, 100, "bingo_mine");
        EmptyBuildingKit.narrate(p, "You take the card. The seat is still warm. Somebody else in the room starts marking faster.");
    }

    static void updateBoard(ServerLevel level, BlockPos o, Game g, Integer last) {
        EmptyBuildingKit.clearDisplays(level, new Vec3(o.getX() + 13, o.getY() + 4, o.getZ() + 1), 6, "bingo_board");
        MutableComponent board = Component.empty();
        for (int row = 0; row < 5; row++) {
            board.append(Component.literal("BINGO".charAt(row) + "  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (int c = 1; c <= 15; c++) {
                int n = row * 15 + c;
                boolean on = g.called.contains(n);
                board.append(Component.literal(String.format(Locale.ROOT, "%2d ", n))
                        .withStyle(on ? (last != null && n == last ? ChatFormatting.RED : ChatFormatting.YELLOW) : ChatFormatting.DARK_GRAY));
            }
            if (row < 4) board.append("\n");
        }
        EmptyBuildingKit.text(level, new Vec3(o.getX() + 13, o.getY() + 2.4, o.getZ() + 1.05), 0f, board, "fixed", 0.9f,
                0xFF0A0806, 400, "bingo_board");
        if (last != null) {
            EmptyBuildingKit.text(level, new Vec3(o.getX() + 13, o.getY() + 4.7, o.getZ() + 1.05), 0f,
                    Component.literal(letter(last) + " " + last).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), "fixed", 2.2f,
                    0xFF0A0806, 200, "bingo_board");
        }
    }

    static List<BlockPos> cardsNear(ServerLevel level, BlockPos c, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 2, c.getZ() + dz);
                if (level.getBlockState(bp).getBlock() instanceof BingoCardBlock) out.add(bp);
            }
        return out;
    }

    static final class Caller implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long key = hallKey(p.blockPosition());
            Game g = GAMES.computeIfAbsent(key, k -> new Game());
            long now = level.getGameTime();
            if (g.nextCall == 0) {
                g.nextCall = now + 60;
                updateBoard(level, origin(p.blockPosition()), g, null);
            }
            if (now < g.nextCall || g.over) return;
            g.nextCall = now + 20 * 8;
            BlockPos o = origin(p.blockPosition());
            BlockPos podium = o.offset(12, 1, 4);
            if (g.called.size() >= 75) g.called.clear();
            int n;
            do n = 1 + p.getRandom().nextInt(75); while (g.called.contains(n));
            g.called.add(n);
            level.playSound(null, podium.offset(2, 0, -1), ModSounds.get("event.bingo_ball"), SoundSource.AMBIENT, 1f, 1f);
            level.playSound(null, podium, ModSounds.get("event.bingo_call"), SoundSource.AMBIENT, 2.0f, 0.95f + p.getRandom().nextFloat() * 0.1f);
            String[] words = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
            String spoken = n < 10 ? words[n] : n / 10 + "... " + n % 10;
            EmptyBuildingKit.say(p, "Caller", ChatFormatting.GOLD, letter(n) + "... " + n + ". " + letter(n) + ", " + spoken + ".");
            updateBoard(level, o, g, n);
            // the empty seats play along
            if (p.getRandom().nextFloat() < 0.32f) {
                g.ghost++;
                List<BlockPos> cards = cardsNear(level, p.blockPosition(), 12);
                cards.removeIf(c -> c.equals(g.mine) || EmptyBuildingKit.seen(p, Vec3.atCenterOf(c), 0.3));
                if (!cards.isEmpty()) {
                    BlockPos c = cards.get(p.getRandom().nextInt(cards.size()));
                    BlockState st = level.getBlockState(c);
                    level.setBlock(c, st.setValue(BingoCardBlock.DAUBS, Math.min(4, st.getValue(BingoCardBlock.DAUBS) + 1)), Block.UPDATE_CLIENTS);
                    level.playSound(null, c, ModSounds.get("event.bingo_dauber"), SoundSource.AMBIENT, 0.8f, 1f);
                }
            }
            // yours
            if (g.mine != null && p.getUUID().equals(g.owner) && level.getBlockState(g.mine).getBlock() instanceof BingoCardBlock
                    && p.getRandom().nextFloat() < 0.33f) {
                BlockState st = level.getBlockState(g.mine);
                int d = Math.min(4, st.getValue(BingoCardBlock.DAUBS) + 1);
                level.setBlock(g.mine, st.setValue(BingoCardBlock.DAUBS, d), Block.UPDATE_CLIENTS);
                level.playSound(null, g.mine, ModSounds.get("event.bingo_dauber"), SoundSource.AMBIENT, 1f, 1.1f);
                p.displayClientMessage(Component.literal(n + " is on your card.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                if (d == 4) {
                    win(p, level, g, o);
                    return;
                }
            }
            if (g.ghost >= 5) lose(p, level, g, o);
        }

        void win(ServerPlayer p, ServerLevel level, Game g, BlockPos o) {
            g.over = true;
            level.playSound(null, p.blockPosition(), ModSounds.get("event.bingo_shout"), SoundSource.PLAYERS, 1.2f, 0.85f);
            EmptyBuildingKit.narrate(p, "You shout BINGO. Yours is the only voice in the room.");
            EmptyBuildingKit.say(p, "Caller", ChatFormatting.GOLD, "We have a winner. " + p.getGameProfile().getName()
                    + ", please come up and collect your prize.");
            for (int dy = 0; dy < 2; dy++)
                level.setBlock(o.offset(23, dy, 0), ModBlocks.state("bingo_prize_door"), Block.UPDATE_ALL);
            level.setBlock(o.offset(23, 2, 1), ModBlocks.state("exit_sign").setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        }

        void lose(ServerPlayer p, ServerLevel level, Game g, BlockPos o) {
            List<BlockPos> cards = cardsNear(level, p.blockPosition(), 14);
            BlockPos where = cards.isEmpty() ? p.blockPosition() : cards.get(p.getRandom().nextInt(cards.size()));
            level.playSound(null, where, ModSounds.get("event.bingo_shout"), SoundSource.AMBIENT, 2f, 1f);
            EmptyBuildingKit.say(p, "???", ChatFormatting.DARK_GRAY, "BINGO!");
            EmptyBuildingKit.say(p, "Caller", ChatFormatting.GOLD, "We have a winner on table " + (1 + Math.floorMod(where.getZ(), CZ) / 5)
                    + ". Congratulations. Lights, please.");
            Blackouts.start(level, 20 * 9);
            Sanity.add(p, -8f);
            for (BlockPos c : cards) {
                BlockState st = level.getBlockState(c);
                level.setBlock(c, st.setValue(BingoCardBlock.DAUBS, 0), Block.UPDATE_CLIENTS);
            }
            g.called.clear();
            g.ghost = 0;
            g.mine = null;
            g.owner = null;
            EmptyBuildingKit.clearDisplays(level, p.position(), 30, "bingo_mine");
            g.nextCall = level.getGameTime() + 20 * 12;
            updateBoard(level, o, g, null);
        }
    }
}
