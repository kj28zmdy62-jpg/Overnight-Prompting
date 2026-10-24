package com.noclip.levels;

import com.noclip.block.ArchiveCrtBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Archive. Stacks of VHS tapes under green pendant lamps, aisle after aisle.
 * The Archive is always recording: every so often it plays back your own footsteps
 * from a minute ago, walking the route you walked, coming up behind you. In the
 * viewing booths the tapes are labelled with your name. Watch one to the end,
 * sitting where it was filmed, and it rewinds you to somewhere you have been.
 */
public final class Archive {
    public static final String ID = "archive";
    static final int FLOOR = 10, CEIL = 16, CX = 15, CZ = 16;

    private Archive() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Archive").order(1150)
                .tagline("Every tape is labelled. Some of them with your name.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Recording())
                .visuals(new Visuals().tint(1.03f, 0.98f, 0.9f).saturation(0.7f).contrast(1.1f).brightness(1.0f)
                        .grain(0.1f).scanlines(0.1f).aberration(0.0028f).vignette(0.62f).bloom(0.55f).warp(0.035f)
                        .fog(3f, 34f).fogColor(0x16120E))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(22, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("skin_stealer"), 1, 0.05f).dark().dist(24, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_4", 2).exit(ExitType.NOCLIP, "museum", 1).exit(ExitType.NOCLIP, "photo_gallery", 1)
                .exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.DOOR, "museum", 2).exit(ExitType.DOOR, "movie_theater", 2).exit(ExitType.DOOR, "call_center", 1)
                .exit(ExitType.DOOR, "photo_gallery", 1).exit(ExitType.DOOR, "level_4", 1)
                .exit(ExitType.SECRET, "level_0", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Cell { STACKS, BOOTHS, READING, SPILL, VAULT, DARK }

    static Cell cell(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0xA7C);
        if (r < 0.10f) return Cell.BOOTHS;
        if (r < 0.14f) return Cell.READING;
        if (r < 0.18f) return Cell.SPILL;
        if (r < 0.20f) return Cell.VAULT;
        if (r < 0.30f) return Cell.DARK;
        return Cell.STACKS;
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("arc_carpet");
        final BlockState ceiling = ModBlocks.state("arc_ceiling");
        final BlockState[] shelves = {ModBlocks.state("arc_shelf_a"), ModBlocks.state("arc_shelf_b"),
                ModBlocks.state("arc_shelf_c"), ModBlocks.state("arc_shelf_gap")};
        final BlockState[] ends = {ModBlocks.state("arc_shelf_end_a13"), ModBlocks.state("arc_shelf_end_c77"),
                ModBlocks.state("arc_shelf_end_f02"), ModBlocks.state("arc_shelf_end_k40"), ModBlocks.state("arc_shelf_end_y0u")};
        final BlockState lamp = ModBlocks.state("arc_lamp");
        final BlockState lampDead = ModBlocks.state("arc_lamp_dead");
        final BlockState partition = ModBlocks.state("arc_partition");
        final BlockState desk = ModBlocks.state("arc_desk");
        final BlockState crt = ModBlocks.state("arc_crt");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState pile = ModBlocks.state("arc_tape_pile");
        final BlockState cart = ModBlocks.state("arc_cart");
        final BlockState vault = ModBlocks.state("arc_vault_door");
        final BlockState vaultTop = ModBlocks.state("arc_vault_door_top");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, carpet);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            int i0 = Math.floorDiv(w.minX, CX), i1 = Math.floorDiv(w.maxX, CX);
            int k0 = Math.floorDiv(w.minZ, CZ), k1 = Math.floorDiv(w.maxZ, CZ);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) cell(w, s, i, k, i * CX, k * CZ);
        }

        BlockState shelf(long s, int x, int y, int z) {
            float r = Hash.unit(s, x * 31 + y, z, 0x5E1);
            return shelves[r < 0.08f ? 3 : r < 0.4f ? 0 : r < 0.7f ? 1 : 2];
        }

        void lampAt(ChunkWriter w, long s, int x, int z, boolean dark, boolean lucky) {
            boolean on = dark ? lucky : !Hash.chance(s, x, z, 0x1A3, 0.15f);
            w.set(x, CEIL - 1, z, on ? lamp : lampDead);
        }

        void cell(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            Cell type = Archive.cell(s, i, k);
            long h = Hash.hash(s, i, k, 0xCE11);
            boolean dark = type == Cell.DARK;
            int lucky = Hash.range(h, 8);
            if (type == Cell.BOOTHS) {
                booths(w, s, x0, z0, h);
                return;
            }
            if (type == Cell.READING) {
                for (int lx = 1; lx <= 12; lx++) {
                    w.set(x0 + lx, FLOOR + 1, z0 + 8, desk);
                    if (lx % 2 == 0) {
                        w.set(x0 + lx, FLOOR + 1, z0 + 7, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        w.set(x0 + lx, FLOOR + 1, z0 + 9, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    }
                }
                for (int lx : new int[]{3, 7, 11}) lampAt(w, s, x0 + lx, z0 + 8, false, true);
                if (Hash.unit(h >>> 4) < 0.5f) w.set(x0 + 6, FLOOR + 1, z0 + 2, bottle);
                return;
            }
            int[] rows = {1, 5, 9, 13};
            for (int ri = 0; ri < rows.length; ri++) {
                int z = z0 + rows[ri];
                boolean spilled = type == Cell.SPILL && ri == 2;
                for (int lx = 0; lx <= 11; lx++) {
                    int x = x0 + lx;
                    for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                        BlockState b = (lx == 0 || lx == 11) ? ends[Hash.range(s, i * 4 + ri, k, 0xE7D, ends.length)] : shelf(s, x, y, z);
                        if (spilled && lx > 2 && lx < 10 && y > FLOOR + 1) b = ModBlocks.AIR;
                        w.set(x, y, z, b);
                    }
                    if (spilled && lx > 2 && lx < 10) {
                        w.set(x, FLOOR + 1, z - 1, pile.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(lx % 4)));
                        if (lx % 2 == 0) w.set(x, FLOOR + 1, z + 1, pile);
                    }
                }
                if (type == Cell.VAULT && ri == 1) {
                    w.set(x0 + 5, FLOOR + 1, z, vault);
                    w.set(x0 + 5, FLOOR + 2, z, vaultTop);
                }
            }
            int n = 0;
            for (int lz : new int[]{3, 7, 11, 15}) {
                for (int lx : new int[]{2, 8}) lampAt(w, s, x0 + lx, z0 + lz, dark, n++ == lucky);
            }
            lampAt(w, s, x0 + 13, z0 + 3, dark, false);
            lampAt(w, s, x0 + 13, z0 + 11, dark, false);
            if (Hash.unit(h >>> 8) < 0.12f) {
                int lz = rows[Hash.range(h >>> 12, 4)] + 2;
                w.set(x0 + 3 + Hash.range(h >>> 16, 7), FLOOR + 1, z0 + lz,
                        cart.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> 20, 4))));
            }
            if (Hash.unit(h >>> 24) < 0.05f) w.set(x0 + 13, FLOOR + 1, z0 + 6, bottle);
        }

        void booths(ChunkWriter w, long s, int x0, int z0, long h) {
            for (int side = 0; side < 2; side++) {
                int back = side == 0 ? 0 : 15, dz = side == 0 ? 1 : -1;
                Direction screen = side == 0 ? Direction.SOUTH : Direction.NORTH;
                for (int b = 0; b < 3; b++) {
                    int a = 1 + b * 4;
                    for (int y = FLOOR + 1; y <= FLOOR + 2; y++) {
                        for (int lx = a; lx <= a + 3; lx++) w.set(x0 + lx, y, z0 + back, partition);
                        for (int d = 1; d <= 3; d++) {
                            w.set(x0 + a, y, z0 + back + dz * d, partition);
                            w.set(x0 + a + 3, y, z0 + back + dz * d, partition);
                        }
                    }
                    int tv = a + 1 + (int) ((h >>> (b + side * 3)) & 1);
                    w.set(x0 + a + 1, FLOOR + 1, z0 + back + dz, desk);
                    w.set(x0 + a + 2, FLOOR + 1, z0 + back + dz, desk);
                    w.set(x0 + tv, FLOOR + 2, z0 + back + dz, crt.setValue(HorizontalDirectionalBlock.FACING, screen));
                    w.set(x0 + tv, FLOOR + 1, z0 + back + dz * 2, chair.setValue(HorizontalDirectionalBlock.FACING, screen.getOpposite()));
                }
            }
            for (int lx : new int[]{3, 7, 11}) lampAt(w, s, x0 + lx, z0 + 7, false, true);
            lampAt(w, s, x0 + 13, z0 + 8, false, true);
        }
    }

    // ---------------------------------------------------------------- the recording

    record Sample(Vec3 pos, float yaw) {}

    static final class Rec {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        double walked;
        int lookedBack;
        long nextEcho;
        List<Vec3> echo;
        int echoIdx;
        Session session;
    }

    static final class Session {
        BlockPos crt;
        int t;
        List<String> lines;
        int lineIdx;
        List<Vec3> replay;
        int replayIdx;
        boolean stayed = true;
    }

    static final Map<UUID, Rec> RECS = new HashMap<>();
    static final int MAX = 360;

    static Rec rec(ServerPlayer p) {
        return RECS.computeIfAbsent(p.getUUID(), u -> new Rec());
    }

    public static void onCrt(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        BackroomsLevel def = Levels.get(level.dimension());
        Rec r = rec(p);
        if (def == null || !ID.equals(def.id)) return;
        if (r.session != null) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("The tape is still playing.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        Session s = new Session();
        s.crt = pos.immutable();
        s.lines = tapeLines(p, r);
        List<Sample> path = new ArrayList<>(r.samples);
        s.replay = new ArrayList<>();
        for (int i = 0; i < path.size(); i += 2) s.replay.add(path.get(i).pos);
        r.session = s;
        level.playSound(null, pos, ModSounds.get("event.archive_insert"), SoundSource.BLOCKS, 1f, 1f);
        setTape(level, pos, 1);
        EmptyBuildingKit.narrate(p, "You push a tape into the slot. The label is handwritten: " + p.getGameProfile().getName() + ".");
    }

    static void setTape(ServerLevel level, BlockPos pos, int tape) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof ArchiveCrtBlock) level.setBlock(pos, st.setValue(ArchiveCrtBlock.TAPE, tape), Block.UPDATE_CLIENTS);
    }

    static String clock(long ticks) {
        long sec = ticks / 20;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60);
    }

    static List<String> tapeLines(ServerPlayer p, Rec r) {
        List<String> out = new ArrayList<>();
        int tapeNo = 100 + Math.floorMod(p.getUUID().hashCode(), 900);
        long played = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        int deaths = p.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        out.add("TAPE " + tapeNo + ".  REC  " + clock(played) + ".  The picture rolls, then holds.");
        List<String> been = new ArrayList<>(PlayerData.visited(p));
        been.remove(ID);
        String[] shots = {"You stand in the middle of the room for a long time.", "You walk out of frame. The camera waits for you to come back.",
                "You look straight into the lens.", "You are running. Nothing is chasing you on the tape.",
                "You stop and listen. The tape has no sound here.", "You sit down against a wall and do not move."};
        Collections.reverse(been);
        for (int i = 0; i < Math.min(3, been.size()); i++) {
            out.add(EmptyBuildingKit.pretty(been.get(i)) + ". " + shots[Math.floorMod(been.get(i).hashCode() + i, shots.length)]);
        }
        if (been.isEmpty()) out.add("A yellow room. You are not in it yet.");
        if (deaths > 0) out.add("The tape skips. " + deaths + (deaths == 1 ? " time" : " times")
                + " you fall and do not get up. Each time, the next frame shows you walking again.");
        else out.add("There is a gap in the tape where something should have happened to you. Not yet.");
        out.add("Here. The stacks. You walk " + (int) r.walked + " metres between the shelves and look over your shoulder "
                + r.lookedBack + (r.lookedBack == 1 ? " time." : " times."));
        out.add("The camera turns toward the viewing booths. Toward this one. You are watching yourself watch.");
        return out;
    }

    static final class Recording implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Rec r = rec(p);
            r.samples.clear();
            r.walked = 0;
            r.lookedBack = 0;
            r.session = null;
            r.echo = null;
            r.nextEcho = level.getGameTime() + 20 * 70;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            Rec r = rec(p);
            long now = level.getGameTime();
            if (p.tickCount % 10 == 0) {
                Sample last = r.samples.peekLast();
                if (last != null) {
                    double d = last.pos.distanceTo(p.position());
                    if (d < 8) r.walked += d;
                    float turn = Math.abs(((p.getYRot() - last.yaw) % 360 + 540) % 360 - 180);
                    if (turn > 120) r.lookedBack++;
                }
                r.samples.addLast(new Sample(p.position(), p.getYRot()));
                if (r.samples.size() > MAX) r.samples.removeFirst();
            }
            // the Archive plays back where you walked a minute ago
            if (r.echo == null && now >= r.nextEcho && r.session == null) {
                r.nextEcho = now + 20 * (60 + p.getRandom().nextInt(60));
                List<Sample> all = new ArrayList<>(r.samples);
                if (all.size() > 120) {
                    r.echo = new ArrayList<>();
                    for (int i = Math.max(0, all.size() - 300); i < all.size() - 60; i++) r.echo.add(all.get(i).pos);
                    r.echoIdx = 0;
                }
            }
            if (r.echo != null && p.tickCount % 10 == 0) {
                if (r.echoIdx >= r.echo.size()) r.echo = null;
                else {
                    Vec3 at = r.echo.get(r.echoIdx);
                    Vec3 prev = r.echoIdx > 0 ? r.echo.get(r.echoIdx - 1) : at;
                    r.echoIdx++;
                    if (at.distanceTo(prev) > 0.6 && at.distanceTo(p.position()) > 3)
                        level.playSound(null, at.x, at.y, at.z, ModSounds.get("event.archive_step"), SoundSource.PLAYERS, 0.9f,
                                0.9f + p.getRandom().nextFloat() * 0.1f);
                }
            }
            if (r.session != null) play(p, level, r);
        }

        void play(ServerPlayer p, ServerLevel level, Rec r) {
            Session s = r.session;
            s.t++;
            Vec3 tv = Vec3.atCenterOf(s.crt);
            if (p.position().distanceTo(tv) > 5) s.stayed = false;
            if (s.t == 40) {
                setTape(level, s.crt, 2);
                level.playSound(null, s.crt, ModSounds.get("event.archive_play"), SoundSource.BLOCKS, 0.8f, 1f);
            }
            if (s.t >= 40 && (s.t - 40) % 70 == 0 && s.lineIdx < s.lines.size()) {
                EmptyBuildingKit.say(p, "TAPE", net.minecraft.ChatFormatting.DARK_RED, s.lines.get(s.lineIdx++));
                if (s.lineIdx % 2 == 0)
                    level.playSound(null, s.crt, ModSounds.get("event.archive_play"), SoundSource.BLOCKS, 0.6f, 0.9f);
            }
            // your own footsteps, out of the speaker grille and then out among the shelves
            if (s.t > 60 && s.t % 10 == 0 && s.replayIdx < s.replay.size()) {
                Vec3 at = s.replay.get(s.replayIdx++);
                level.playSound(null, at.x, at.y, at.z, ModSounds.get("event.archive_step"), SoundSource.PLAYERS, 0.7f, 1f);
            }
            int end = 40 + 70 * s.lines.size() + 40;
            if (s.t == end) {
                setTape(level, s.crt, 1);
                level.playSound(null, s.crt, ModSounds.get("event.archive_static"), SoundSource.BLOCKS, 1f, 1f);
            }
            if (s.t == end + 30) {
                r.session = null;
                if (s.stayed && p.position().distanceTo(tv) <= 5) {
                    level.playSound(null, s.crt, ModSounds.get("event.archive_rewind"), SoundSource.BLOCKS, 1.2f, 1f);
                    EmptyBuildingKit.narrate(p, "The tape rewinds. So do you.");
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                    setTape(level, s.crt, 0);
                    List<String> been = new ArrayList<>(PlayerData.visited(p));
                    been.remove(ID);
                    been.removeIf(id -> Levels.get(id) == null);
                    if (been.isEmpty()) Travel.exit(p, ExitType.SECRET, s.crt);
                    else Travel.sendTo(p, been.get(p.getRandom().nextInt(been.size())));
                } else {
                    EmptyBuildingKit.narrate(p, "Somewhere behind you, the tape ejects.");
                    setTape(level, s.crt, 0);
                    level.playSound(null, s.crt, ModSounds.get("event.archive_insert"), SoundSource.BLOCKS, 1f, 0.8f);
                }
            }
        }
    }
}
