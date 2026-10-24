package com.noclip.levels;

import com.noclip.block.CallPhoneBlock;
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
import com.noclip.sanity.PlayerData;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Call Center. Rows of cubicles under fluorescent panels, the queue board stuck at 999,
 * and phones ringing all over the floor. Pick one up and it is you on the line:
 * your name, your last level, your own footsteps played back down a bad connection.
 * Let too many ring out and every phone on the floor goes at once, the lights die,
 * and something that hunts by sound comes to see what the noise is. The red phone in
 * the supervisor's glass office puts you on hold, then transfers you.
 */
public final class CallCenter {
    public static final String ID = "call_center";
    static final int FLOOR = 10, CEIL = 15, PX = 16, PZ = 10;

    private CallCenter() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Call Center").order(1240)
                .tagline("Your call is important to us. It is also from you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Phones())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.04f).saturation(0.8f).contrast(1.06f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.48f).bloom(0.45f).fog(5f, 48f).fogColor(0x8A9098))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.12f).dist(18, 36).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.04f).dist(30, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_4", 3).exit(ExitType.NOCLIP, "archive", 1).exit(ExitType.NOCLIP, "level_0", 1)
                .exit(ExitType.SECRET, "lift_lobby", 2).exit(ExitType.SECRET, "waiting_room", 2).exit(ExitType.SECRET, "archive", 1)
                .exit(ExitType.SECRET, "level_4", 1).exit(ExitType.SECRET, "the_hub", 1)
                .exit(ExitType.DOOR, "level_4", 2).exit(ExitType.DOOR, "stairwell", 1)
                .build();
    }

    enum Cell { NORMAL, MEETING, SUPERVISOR, DARK }

    static Cell cell(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0xCA11);
        if (r < 0.035f) return Cell.SUPERVISOR;
        if (r < 0.095f) return Cell.MEETING;
        if (r < 0.17f) return Cell.DARK;
        return Cell.NORMAL;
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("cc_carpet");
        final BlockState ceiling = ModBlocks.state("cc_ceiling");
        final BlockState panel = ModBlocks.state("cc_panel");
        final BlockState flicker = ModBlocks.state("cc_panel_flicker");
        final BlockState dead = ModBlocks.state("cc_panel_dead");
        final BlockState part = ModBlocks.state("cc_partition");
        final BlockState notes = ModBlocks.state("cc_partition_notes");
        final BlockState glass = ModBlocks.state("cc_glass");
        final BlockState desk = ModBlocks.state("cc_desk");
        final BlockState monitor = ModBlocks.state("cc_monitor");
        final BlockState monitorOff = ModBlocks.state("cc_monitor_off");
        final BlockState phone = ModBlocks.state("cc_phone");
        final BlockState red = ModBlocks.state("cc_red_phone");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState board = ModBlocks.state("cc_board");
        final BlockState board2 = ModBlocks.state("cc_board2");
        final BlockState hold = ModBlocks.state("cc_board_youhold");
        final BlockState column = ModBlocks.state("cc_column");
        final BlockState whiteboard = ModBlocks.state("cc_whiteboard");
        final BlockState cooler = ModBlocks.state("cc_cooler");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, carpet);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    if (Math.floorMod(x, 4) != 1 || Math.floorMod(z, 5) != 1) continue;
                    int i = Math.floorDiv(x, PX), k = Math.floorDiv(z, PZ);
                    boolean dk = CallCenter.cell(s, i, k) == Cell.DARK;
                    float r = Hash.unit(s, x, z, 0x11E);
                    w.set(x, CEIL, z, dk ? (r < 0.15f ? flicker : dead) : (r < 0.05f ? dead : r < 0.1f ? flicker : panel));
                }
            int i0 = Math.floorDiv(w.minX, PX), i1 = Math.floorDiv(w.maxX, PX);
            int k0 = Math.floorDiv(w.minZ, PZ), k1 = Math.floorDiv(w.maxZ, PZ);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) cell(w, s, i, k, i * PX, k * PZ);
        }

        void cell(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            Cell type = CallCenter.cell(s, i, k);
            int y = FLOOR + 1;
            if ((i + k) % 2 == 0) for (int yy = y; yy < CEIL; yy++) w.set(x0 + 14, yy, z0 + 1, column);
            if (Math.floorMod(i * 3 + k, 3) == 0) {
                w.set(x0 + 5, CEIL - 1, z0 + 1, board2);
                w.set(x0 + 6, CEIL - 1, z0 + 1, board);
            }
            if (Hash.chance(s, i, k, 0xC00, 0.06f)) w.set(x0 + 13, y, z0 + 1, cooler);
            if (type == Cell.MEETING || type == Cell.SUPERVISOR) {
                for (int lx = 0; lx <= 12; lx++)
                    for (int yy = y; yy <= y + 2; yy++) {
                        w.set(x0 + lx, yy, z0 + 3, glass);
                        w.set(x0 + lx, yy, z0 + 9, glass);
                    }
                for (int lz = 3; lz <= 9; lz++)
                    for (int yy = y; yy <= y + 2; yy++) {
                        w.set(x0, yy, z0 + lz, glass);
                        w.set(x0 + 12, yy, z0 + lz, glass);
                    }
                w.set(x0 + 6, y, z0 + 3, ModBlocks.AIR);
                w.set(x0 + 6, y + 1, z0 + 3, ModBlocks.AIR);
                if (type == Cell.MEETING) {
                    for (int lx = 3; lx <= 9; lx++) {
                        w.set(x0 + lx, y, z0 + 6, desk);
                        if (lx % 2 == 1) {
                            w.set(x0 + lx, y, z0 + 5, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                            w.set(x0 + lx, y, z0 + 7, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                        }
                    }
                    w.set(x0 + 6, y + 1, z0 + 8, whiteboard.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 6, y + 1, z0 + 6, phone.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                } else {
                    w.set(x0 + 5, y, z0 + 7, desk);
                    w.set(x0 + 6, y, z0 + 7, desk);
                    w.set(x0 + 7, y, z0 + 7, desk);
                    w.set(x0 + 6, y + 1, z0 + 7, red.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    w.set(x0 + 5, y + 1, z0 + 7, monitorOff.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    w.set(x0 + 6, y, z0 + 6, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    w.set(x0 + 2, y, z0 + 8, boxes);
                    w.set(x0 + 6, CEIL - 1, z0 + 2, hold);
                }
                return;
            }
            // cubicles
            for (int lx = 0; lx <= 12; lx++)
                w.set(x0 + lx, y, z0 + 6, Hash.chance(s, x0 + lx, z0, 0x707, 0.25f) ? notes : part);
            for (int a : new int[]{0, 3, 6, 9, 12})
                for (int lz : new int[]{3, 4, 5, 7, 8, 9}) w.set(x0 + a, y, z0 + lz, part);
            for (int a : new int[]{0, 3, 6, 9}) {
                for (int side = 0; side < 2; side++) {
                    int dz = side == 0 ? 5 : 7, cz = side == 0 ? 4 : 8;
                    Direction face = side == 0 ? Direction.NORTH : Direction.SOUTH;
                    long h = Hash.hash(s, x0 + a, z0 + side, 0xDE5);
                    w.set(x0 + a + 1, y, z0 + dz, desk);
                    w.set(x0 + a + 2, y, z0 + dz, desk);
                    w.set(x0 + a + 1, y + 1, z0 + dz, phone.setValue(HorizontalDirectionalBlock.FACING, face));
                    w.set(x0 + a + 2, y + 1, z0 + dz, (Hash.unit(h) < 0.3f ? monitor : monitorOff).setValue(HorizontalDirectionalBlock.FACING, face));
                    float c = Hash.unit(h >>> 8);
                    if (c < 0.7f) {
                        w.set(x0 + a + 1 + (int) ((h >>> 16) & 1), y, z0 + cz, chair.setValue(HorizontalDirectionalBlock.FACING, face.getOpposite()));
                    } else if (c < 0.85f) {
                        // pushed back into the aisle, turned away
                        int az = side == 0 ? 2 : 0;
                        w.set(x0 + a + 1, y, z0 + az + (side == 0 ? 0 : 10), chair.setValue(HorizontalDirectionalBlock.FACING,
                                Direction.from2DDataValue(Hash.range(h >>> 20, 4))));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the phones

    static final Map<Long, Long> RINGING = new HashMap<>();      // phone pos -> started
    static final Map<UUID, Integer> MISSED = new HashMap<>();
    static final Map<UUID, Long> NEXT = new HashMap<>();
    static final Map<UUID, Call> CALLS = new HashMap<>();
    static ResourceKey<Level> dim;

    static final class Call {
        BlockPos phone;
        List<Component> lines;
        int t, idx;
        boolean transfer, red;
    }

    static boolean isPhone(BlockState s) {
        return s.getBlock() instanceof CallPhoneBlock;
    }

    static void setRinging(ServerLevel level, BlockPos pos, boolean ring) {
        BlockState s = level.getBlockState(pos);
        if (!isPhone(s)) return;
        if (s.getValue(CallPhoneBlock.RINGING) != ring) level.setBlock(pos, s.setValue(CallPhoneBlock.RINGING, ring), Block.UPDATE_CLIENTS);
        if (ring) RINGING.putIfAbsent(pos.asLong(), level.getGameTime());
        else RINGING.remove(pos.asLong());
    }

    static MutableComponent garble(String text, ServerPlayer p, float amount) {
        MutableComponent out = Component.empty();
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++) {
            boolean bad = p.getRandom().nextFloat() < amount && words[i].length() > 2;
            out.append(Component.literal(words[i]).withStyle(bad ? ChatFormatting.OBFUSCATED : ChatFormatting.WHITE));
            if (i < words.length - 1) out.append(" ");
        }
        return out;
    }

    static Component line(ServerPlayer p, int ext, String text, float garble) {
        return Component.literal("[EXT " + ext + "] ").withStyle(ChatFormatting.DARK_AQUA).append(garble(text, p, garble));
    }

    public static void onPhone(ServerPlayer p, BlockPos pos, BlockState state) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().location().getPath().equals(ID)) return;
        if (CALLS.containsKey(p.getUUID())) return;
        boolean ringing = state.getValue(CallPhoneBlock.RINGING);
        boolean red = state.is(ModBlocks.get("cc_red_phone"));
        level.playSound(null, pos, ModSounds.get("event.call_center_pickup"), SoundSource.BLOCKS, 1f, 1f);
        int ext = 100 + Math.floorMod((int) pos.asLong(), 900);
        String name = p.getGameProfile().getName();
        Call c = new Call();
        c.phone = pos.immutable();
        c.red = red;
        c.lines = new ArrayList<>();
        if (red) {
            c.lines.add(line(p, 0, "Supervisor's line. You have reached the supervisor.", 0f));
            c.lines.add(line(p, 0, "Please hold.", 0f));
            c.lines.add(line(p, 0, "Your call is important to us.", 0.1f));
            c.lines.add(line(p, 0, "Transferring you now, " + name + ".", 0.2f));
            c.transfer = true;
        } else if (!ringing) {
            p.displayClientMessage(Component.literal(p.getRandom().nextFloat() < 0.2f ? "Someone is breathing on the line."
                    : "Dial tone. Nobody there.").withStyle(ChatFormatting.GRAY), true);
            return;
        } else {
            setRinging(level, pos, false);
            MISSED.put(p.getUUID(), 0);
            List<String> been = new ArrayList<>(PlayerData.visited(p));
            been.remove(ID);
            String last = been.isEmpty() ? "nowhere" : EmptyBuildingKit.pretty(been.get(been.size() - 1));
            String[] middles = {
                    "It's me. It's you. I'm calling from " + last + ". You haven't left yet, here.",
                    "Don't turn around. I'm at the desk behind you. No. The other one.",
                    "You picked up on the fourth ring. You always pick up on the fourth ring.",
                    "Can you hear the phones from there? Are you counting them? I was counting them.",
                    "I've been calling every desk on the floor. You took a long time to get to this one.",
                    "Say something so I know it's you. ...No. Never mind. I know it's you."};
            c.lines.add(line(p, ext, "...hello? Hello? " + name + "?", 0.05f));
            int a = p.getRandom().nextInt(middles.length), b = (a + 1 + p.getRandom().nextInt(middles.length - 1)) % middles.length;
            c.lines.add(line(p, ext, middles[a], 0.12f));
            c.lines.add(line(p, ext, middles[b], 0.22f));
            if (p.getRandom().nextFloat() < 0.5f)
                c.lines.add(line(p, ext, "There's a red phone in a glass office. It goes somewhere. Ask them to transfer you.", 0.15f));
            c.lines.add(Component.literal("[The line plays back your footsteps. Your breathing. The click of the receiver you are holding.]")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            if (p.getRandom().nextFloat() < 0.08f) {
                c.lines.add(line(p, ext, "Please hold. I'm putting you through to where I am.", 0.3f));
                c.transfer = true;
            } else {
                c.lines.add(Component.literal("[The line goes dead.]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
        CALLS.put(p.getUUID(), c);
    }

    static void tickCall(ServerPlayer p, ServerLevel level, Call c) {
        c.t++;
        if (c.t % 50 == 1 && c.idx < c.lines.size()) {
            p.sendSystemMessage(c.lines.get(c.idx++));
            if (c.red && c.idx >= 2) level.playSound(null, c.phone, ModSounds.get("event.call_center_hold"), SoundSource.BLOCKS, 0.8f, 1f);
            else if (!c.red && c.idx <= c.lines.size() - 2)
                level.playSound(null, c.phone, ModSounds.get("event.call_center_voice"), SoundSource.BLOCKS, 0.9f, 0.95f + p.getRandom().nextFloat() * 0.1f);
            if (!c.red && c.idx == c.lines.size() - 1) {
                // your own sounds, down the line
                level.playSound(null, c.phone, SoundEvents.PLAYER_HURT, SoundSource.BLOCKS, 0.5f, 0.7f);
                for (int i = 0; i < 4; i++)
                    level.playSound(null, c.phone, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 0.6f, 0.75f + i * 0.02f);
            }
        }
        if (c.idx >= c.lines.size() && c.t % 50 == 40) {
            CALLS.remove(p.getUUID());
            if (c.transfer) {
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.9f));
                Travel.exit(p, ExitType.SECRET, c.phone);
            }
        }
    }

    static final class Phones implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            dim = level.dimension();
            NEXT.put(p.getUUID(), level.getGameTime() + 60);
            MISSED.put(p.getUUID(), 0);
            CALLS.remove(p.getUUID());
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            Call c = CALLS.get(p.getUUID());
            if (c != null) tickCall(p, level, c);
            Long next = NEXT.get(p.getUUID());
            if (next != null && now < next) return;
            NEXT.put(p.getUUID(), now + 20 * (5 + p.getRandom().nextInt(6)));
            int near = 0;
            for (long l : RINGING.keySet()) if (BlockPos.of(l).distSqr(p.blockPosition()) < 24 * 24) near++;
            if (near >= 3 || c != null) return;
            // pick a desk somewhere around the player and make it ring
            long s = Hash.hash(level.getSeed(), Hash.salt(ID));
            for (int attempt = 0; attempt < 12; attempt++) {
                int x = p.getBlockX() + p.getRandom().nextInt(41) - 20, z = p.getBlockZ() + p.getRandom().nextInt(41) - 20;
                int i = Math.floorDiv(x, PX), k = Math.floorDiv(z, PZ);
                if (cell(s, i, k) == Cell.MEETING || cell(s, i, k) == Cell.SUPERVISOR) continue;
                int a = new int[]{0, 3, 6, 9}[p.getRandom().nextInt(4)];
                BlockPos ph = new BlockPos(i * PX + a + 1, FLOOR + 2, k * PZ + (p.getRandom().nextBoolean() ? 5 : 7));
                if (ph.distSqr(p.blockPosition()) < 25 || !isPhone(level.getBlockState(ph))) continue;
                setRinging(level, ph, true);
                return;
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (now % 40 != 0) return;
            for (Iterator<Map.Entry<Long, Long>> it = RINGING.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> e = it.next();
                BlockPos pos = BlockPos.of(e.getKey());
                if (!level.isLoaded(pos) || !isPhone(level.getBlockState(pos))) {
                    it.remove();
                    continue;
                }
                if (now - e.getValue() > 20 * 30) {
                    BlockState st = level.getBlockState(pos);
                    level.setBlock(pos, st.setValue(CallPhoneBlock.RINGING, false), Block.UPDATE_CLIENTS);
                    it.remove();
                    for (ServerPlayer p : level.players())
                        if (p.blockPosition().distSqr(pos) < 32 * 32) MISSED.merge(p.getUUID(), 1, Integer::sum);
                    continue;
                }
                level.playSound(null, pos, ModSounds.get("event.call_center_ring"), SoundSource.BLOCKS, 1.0f, 1f);
            }
            // too many missed calls: every line at once
            for (ServerPlayer p : level.players()) {
                if (MISSED.getOrDefault(p.getUUID(), 0) < 4) continue;
                MISSED.put(p.getUUID(), 0);
                level.playSound(null, p.blockPosition(), ModSounds.get("event.call_center_all"), SoundSource.AMBIENT, 1.4f, 1f);
                BlockPos c = p.blockPosition();
                for (int dx = -20; dx <= 20; dx++)
                    for (int dz = -20; dz <= 20; dz++) {
                        BlockPos ph = new BlockPos(c.getX() + dx, FLOOR + 2, c.getZ() + dz);
                        if (isPhone(level.getBlockState(ph)) && level.random.nextFloat() < 0.6f) setRinging(level, ph, true);
                    }
                Blackouts.start(level, 20 * 14);
                EmptyBuildingKit.narrate(p, "Every phone on the floor is ringing. Something is coming to see why.");
                double ang = Math.toRadians(p.getYRot() + 180);
                for (int tries = 0; tries < 6; tries++) {
                    double d = 18 + level.random.nextInt(10);
                    BlockPos at = BlockPos.containing(p.getX() - Math.sin(ang) * d, FLOOR + 1, p.getZ() + Math.cos(ang) * d);
                    if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) {
                        ang += 0.6;
                        continue;
                    }
                    Mob m = ModEntities.HOUND.create(level);
                    if (m == null) break;
                    m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                    level.addFreshEntity(m);
                    break;
                }
            }
        }
    }
}
