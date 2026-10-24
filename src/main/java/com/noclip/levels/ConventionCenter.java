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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Convention Center. An exhibition hall of empty booths under a black truss roof,
 * pipe-and-drape in every direction, the seams in the aisle carpet running off to
 * the vanishing point. Every lanyard on every rack says HELLO, MY NAME IS and then
 * your name. The PA pages you to the information desk, aisle such-and-such; take
 * too long and the desk has moved.
 */
public final class ConventionCenter {
    public static final String ID = "convention_center";
    static final int FLOOR = 10, CEIL = 22, P = 10;
    static final String ROWS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private ConventionCenter() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Convention Center").order(1230)
                .tagline("Attendee badges are ready for collection. Yours has been ready for a while.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Pager())
                .visuals(new Visuals().tint(0.98f, 0.99f, 1.03f).saturation(0.82f).contrast(1.06f).brightness(1.25f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.5f).bloom(0.5f).fog(8f, 70f).fogColor(0x1C2028))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("mannequin"), 3, 0.2f).dist(18, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(30, 44).weight(1))
                .exit(ExitType.NOCLIP, "dead_mall", 2).exit(ExitType.NOCLIP, "airport", 1).exit(ExitType.NOCLIP, "museum", 1)
                .exit(ExitType.DOOR, "museum", 2).exit(ExitType.DOOR, "airport", 2).exit(ExitType.DOOR, "lift_lobby", 1)
                .exit(ExitType.DOOR, "ballroom", 1).exit(ExitType.DOOR, "dead_mall", 1)
                .build();
    }

    enum Booth { EMPTY, TABLE, SCREEN, STANDEE, LANYARDS }

    static Booth booth(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0xB007);
        if (r < 0.2f) return Booth.EMPTY;
        if (r < 0.27f) return Booth.LANYARDS;
        if (r < 0.45f) return Booth.STANDEE;
        if (r < 0.65f) return Booth.SCREEN;
        return Booth.TABLE;
    }

    static final class Gen implements LevelGenerator {
        final BlockState aisle = ModBlocks.state("conv_aisle");
        final BlockState aisleX = ModBlocks.state("conv_aisle_x");
        final String[] carpets = {"red", "green", "grey", "tan"};
        final BlockState concrete = ModBlocks.state("conv_concrete");
        final BlockState roof = ModBlocks.state("conv_roof");
        final BlockState truss = ModBlocks.state("conv_truss");
        final BlockState bay = ModBlocks.state("conv_high_bay");
        final BlockState bayBad = ModBlocks.state("conv_high_bay_bad");
        final BlockState drape = ModBlocks.state("conv_drape");
        final BlockState pipe = ModBlocks.state("conv_pipe");
        final BlockState table = ModBlocks.state("conv_table");
        final BlockState tv = ModBlocks.state("conv_tv");
        final BlockState standee = ModBlocks.state("conv_standee");
        final BlockState lanyards = ModBlocks.state("conv_lanyards");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            int y = FLOOR + 1;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, roof);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = Math.floorDiv(x, P), k = Math.floorDiv(z, P);
                    int lx = x - i * P, lz = z - k * P;
                    boolean ax = lz >= 6, az = lx >= 6;
                    BlockState f;
                    if (ax && az) f = concrete;
                    else if (ax) f = aisle;
                    else if (az) f = aisleX;
                    else f = ConventionCenter.booth(s, i, k) == Booth.EMPTY ? concrete
                                : ModBlocks.state("conv_booth_" + carpets[Hash.range(s, i, k, 0xCA, 4)]);
                    w.set(x, FLOOR, z, f);
                    if (lz == 8 && lx % 2 == 0) w.set(x, CEIL - 1, z, truss);
                    if (lx == 8 && lz == 8)
                        w.set(x, CEIL - 1, z, Hash.chance(s, i, k, 0xBA, 0.12f) ? bayBad : bay);
                }
            int i0 = Math.floorDiv(w.minX, P), i1 = Math.floorDiv(w.maxX, P);
            int k0 = Math.floorDiv(w.minZ, P), k1 = Math.floorDiv(w.maxZ, P);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) booth(w, s, i, k, i * P, k * P);
        }

        void booth(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int y = FLOOR + 1;
            Booth type = ConventionCenter.booth(s, i, k);
            for (int yy = y; yy <= y + 2; yy++) {
                w.set(x0, yy, z0, pipe);
                w.set(x0 + 5, yy, z0, pipe);
                for (int lx = 1; lx <= 4; lx++) w.set(x0 + lx, yy, z0, drape);
            }
            for (int lz = 1; lz <= 2; lz++) {
                w.set(x0, y, z0 + lz, drape);
                w.set(x0 + 5, y, z0 + lz, drape);
            }
            if (type == Booth.EMPTY) {
                if (Hash.chance(s, i, k, 0xB0C, 0.4f)) w.set(x0 + 3, y, z0 + 2, boxes);
                return;
            }
            BlockState banner = ModBlocks.state("conv_banner_" + Hash.range(s, i, k, 0xBA77, 8));
            w.set(x0 + 2, y + 3, z0, banner);
            w.set(x0 + 3, y + 3, z0, banner);
            switch (type) {
                case TABLE -> {
                    w.set(x0 + 2, y, z0 + 3, table);
                    w.set(x0 + 3, y, z0 + 3, table);
                    if (Hash.chance(s, i, k, 0xC1, 0.7f)) w.set(x0 + 2, y, z0 + 2, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                }
                case SCREEN -> {
                    w.set(x0 + 2, y + 1, z0 + 1, tv.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 3, y + 1, z0 + 1, tv.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 3, y, z0 + 4, table);
                }
                case STANDEE -> {
                    w.set(x0 + 4, y, z0 + 3, standee.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 4, y + 1, z0 + 3, ModBlocks.AIR);
                    w.set(x0 + 1, y, z0 + 4, table);
                }
                case LANYARDS -> {
                    for (int lx = 1; lx <= 4; lx++) w.set(x0 + lx, y, z0 + 3, table);
                    w.set(x0 + 2, y + 1, z0 + 1, lanyards.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 3, y + 1, z0 + 1, lanyards.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                }
                default -> {
                }
            }
        }
    }

    // ---------------------------------------------------------------- the page

    static final class St {
        int ti, tk;
        boolean has;
        long nextPage, deadline;
        BlockPos desk;
        int pages;
    }

    static final Map<UUID, St> STATES = new HashMap<>();
    static final Map<Long, Long> SIGNS = new HashMap<>();
    static final Map<Long, Long> BADGES = new HashMap<>();

    static String row(int k) {
        return String.valueOf(ROWS.charAt(Math.floorMod(k, ROWS.length())));
    }

    static String aisle(int i) {
        return String.valueOf(100 + Math.floorMod(i, 900));
    }

    public static void onDesk(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, pos, ModSounds.get("event.convention_bell"), SoundSource.BLOCKS, 1f, 1f);
        if (!level.dimension().location().getPath().equals(ID)) return;
        EmptyBuildingKit.narrate(p, "Nobody comes to the desk. On the counter, a badge on a lanyard: VISITOR - EXIT - "
                + p.getGameProfile().getName().toUpperCase(Locale.ROOT) + ". You put it on.");
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.7f));
        Travel.exit(p, ExitType.DOOR, pos);
    }

    static void placeDesk(ServerLevel level, St st) {
        BlockPos d = new BlockPos(st.ti * P + 7, FLOOR + 1, st.tk * P + 7);
        if (!level.isLoaded(d)) {
            st.desk = null;
            return;
        }
        level.setBlock(d, ModBlocks.state("conv_info"), Block.UPDATE_ALL);
        st.desk = d;
        EmptyBuildingKit.text(level, new Vec3(d.getX() + 0.5, d.getY() + 3.2, d.getZ() + 0.5), 0f,
                Component.literal("INFORMATION").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), "vertical", 1.6f,
                0xFF1A3A8A, 200, "conv_desk");
    }

    static void removeDesk(ServerLevel level, St st) {
        if (st.desk == null) return;
        if (level.isLoaded(st.desk) && level.getBlockState(st.desk).is(ModBlocks.get("conv_info")))
            level.setBlock(st.desk, ModBlocks.AIR, Block.UPDATE_ALL);
        EmptyBuildingKit.clearDisplays(level, Vec3.atCenterOf(st.desk), 4, "conv_desk");
        st.desk = null;
    }

    static final class Pager implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.nextPage = level.getGameTime() + 20 * 12;
            STATES.put(p.getUUID(), st);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = STATES.computeIfAbsent(p.getUUID(), u -> new St());
            long now = level.getGameTime();
            int pi = Math.floorDiv(p.getBlockX(), P), pk = Math.floorDiv(p.getBlockZ(), P);
            String name = p.getGameProfile().getName();
            if (st.has && st.desk == null && Math.abs(pi - st.ti) < 6 && Math.abs(pk - st.tk) < 6) placeDesk(level, st);
            if (now >= st.nextPage) {
                st.nextPage = now + 20 * 45;
                boolean move = st.has && now > st.deadline;
                if (!st.has || move) {
                    removeDesk(level, st);
                    int dist = 4 + p.getRandom().nextInt(4) + (move ? 2 : 0);
                    st.ti = pi + (p.getRandom().nextBoolean() ? dist : -dist);
                    st.tk = pk + p.getRandom().nextInt(5) - 2;
                    st.has = true;
                    st.deadline = now + 20 * 150;
                }
                String where = "aisle " + aisle(st.ti) + ", row " + row(st.tk);
                String[] lines = {"Would " + name + " please come to the information desk. That's " + where + ".",
                        name + ", your party is waiting for you at the information desk, " + where + ".",
                        "Paging " + name + ". " + name + " to the information desk, " + where + ", please."};
                String text = move ? "Attention please. The information desk has moved to " + where + ". " + name + ", we are still waiting."
                        : lines[st.pages % lines.length];
                st.pages++;
                level.playSound(null, p.getX(), p.getY() + 6, p.getZ(), ModSounds.get("event.convention_pa"), SoundSource.AMBIENT, 1.6f, 1f);
                EmptyBuildingKit.say(p, "PA", ChatFormatting.AQUA, text);
            }
            if (p.tickCount % 40 == 0) signs(p, level, pi, pk, name);
        }

        void signs(ServerPlayer p, ServerLevel level, int pi, int pk, String name) {
            long now = level.getGameTime();
            for (int i = pi - 2; i <= pi + 2; i++)
                for (int k = pk - 2; k <= pk + 2; k++) {
                    long key = ((long) i << 32) ^ (k & 0xffffffffL);
                    if (SIGNS.put(key, now) == null) {
                        Vec3 at = new Vec3(i * P + 8, FLOOR + 6.5, k * P + 8);
                        EmptyBuildingKit.text(level, at, 0f, Component.literal("AISLE " + aisle(i) + "  |  ROW " + row(k))
                                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), "vertical", 1.3f, 0xE0222838, 300, "conv_sign_" + key);
                    }
                    if (booth(Hash.hash(level.getSeed(), Hash.salt(ID)), i, k) == Booth.LANYARDS && BADGES.put(key, now) == null) {
                        for (int c = 0; c < 2; c++) {
                            Vec3 at = new Vec3(i * P + 2.5 + c, FLOOR + 2.2, k * P + 1.62);
                            EmptyBuildingKit.text(level, at, 0f, Component.literal("HELLO\nmy name is\n").withStyle(ChatFormatting.WHITE)
                                            .append(Component.literal(name).withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD)),
                                    "fixed", 0.22f, 0xFFC83020, 80, "conv_badge_" + key);
                        }
                    }
                }
            for (Map<Long, Long> m : java.util.List.of(SIGNS, BADGES)) {
                String prefix = m == SIGNS ? "conv_sign_" : "conv_badge_";
                for (Iterator<Map.Entry<Long, Long>> it = m.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Long, Long> e = it.next();
                    if (now - e.getValue() < 20 * 10) continue;
                    int i = (int) (e.getKey() >> 32), k = (int) (long) e.getKey().intValue();
                    boolean near = false;
                    for (Player pl : level.players())
                        if (Math.abs(Math.floorDiv(pl.getBlockX(), P) - i) <= 3 && Math.abs(Math.floorDiv(pl.getBlockZ(), P) - k) <= 3) near = true;
                    if (near) continue;
                    EmptyBuildingKit.clearDisplays(level, new Vec3(i * P + 5, FLOOR + 4, k * P + 5), 9, prefix + e.getKey());
                    it.remove();
                }
            }
        }
    }
}
