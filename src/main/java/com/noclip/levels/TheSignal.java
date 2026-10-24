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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Signal. Open moorland at night under a storm that never moves on. Every ninety-six
 * blocks a radio mast stands over a concrete hut, its red lights blinking in the rain.
 * Each hut has a radio and a locked door marked TX. Tune the radio (use it; sneak to tune
 * down). Somewhere on the band a voice reads numbers, and the door opens for whoever
 * hears it. Somewhere else on the band something is breathing, and if you land on it,
 * it hears you back. You can also climb the mast, if you don't mind the lightning.
 */
public final class TheSignal {
    static final String ID = "the_signal";
    static final int T = 64, C = 32, N = 41;

    private TheSignal() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Signal").order(3010)
                .tagline("Find the voice on the band. Don't stop on the one that's breathing.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Storm())
                .visuals(new Visuals().tint(0.92f, 0.97f, 1.08f).saturation(0.55f).contrast(1.1f).brightness(1.4f)
                        .grain(0.09f).aberration(0.0018f).vignette(0.55f).warp(0.014f).fog(4f, 76f).fogColor(0x14181E)
                        .bloom(0.8f))
                .sanityDrain(0.035f)
                .spawnY(14, 34)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.08f).dist(32, 48).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(28, 44).weight(3))
                .exit(ExitType.DOOR, "static_fields", 2).exit(ExitType.DOOR, "level_3", 1).exit(ExitType.DOOR, "the_hub", 1)
                .exit(ExitType.ASCEND, "sky_rooms", 2).exit(ExitType.ASCEND, "rooftops", 1)
                .exit(ExitType.NOCLIP, "static_fields", 2).exit(ExitType.NOCLIP, "pole_forest", 1).exit(ExitType.NOCLIP, "endless_highway", 1)
                .build();
    }

    static int rawHeight(long s, int x, int z) {
        return 17 + (int) (Hash.fbm(s ^ 0x51C0L, x / 44.0, z / 44.0, 3) * 11);
    }

    static int ground(long s, int x, int z) {
        int tx = Math.floorDiv(x, T), tz = Math.floorDiv(z, T);
        int lx = Math.floorMod(x, T), lz = Math.floorMod(z, T);
        int d = Math.max(Math.abs(lx - C - 4), Math.abs(lz - C));
        int raw = rawHeight(s, x, z);
        if (d > 22) return raw;
        int h0 = rawHeight(s, tx * T + C, tz * T + C);
        if (d <= 13) return h0;
        float f = (d - 13) / 9f;
        return Math.round(h0 + (raw - h0) * f);
    }

    static final class Gen implements LevelGenerator {
        final BlockState grass = ModBlocks.state("sig_grass");
        final BlockState peat = ModBlocks.state("sig_peat");
        final BlockState granite = ModBlocks.state("sig_granite");
        final BlockState drystone = ModBlocks.state("sig_drystone");
        final BlockState heather = ModBlocks.state("sig_heather");
        final BlockState tussock = ModBlocks.state("sig_tussock");
        final BlockState lattice = ModBlocks.state("sig_lattice");
        final BlockState platform = ModBlocks.state("sig_platform");
        final BlockState beacon = ModBlocks.state("sig_beacon");
        final BlockState hutWall = ModBlocks.state("sig_hut_wall");
        final BlockState roof = ModBlocks.state("sig_hut_roof");
        final BlockState hutFloor = ModBlocks.state("sig_hut_floor");
        final BlockState window = ModBlocks.state("sig_hut_window");
        final BlockState radio = ModBlocks.state("sig_radio");
        final BlockState txDoor = ModBlocks.state("sig_tx_door");
        final BlockState bulb = ModBlocks.state("sig_bulb");
        final BlockState sign = ModBlocks.state("sig_sign_tx");
        final BlockState ladder = ModBlocks.state("sig_ladder");
        final BlockState top = ModBlocks.state("sig_mast_top");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) terrain(w, s, x, z);
            int t0x = Math.floorDiv(w.minX, T), t1x = Math.floorDiv(w.maxX, T);
            int t0z = Math.floorDiv(w.minZ, T), t1z = Math.floorDiv(w.maxZ, T);
            for (int tx = t0x; tx <= t1x; tx++)
                for (int tz = t0z; tz <= t1z; tz++) site(w, s, tx, tz);
        }

        void terrain(ChunkWriter w, long s, int x, int z) {
            int h = ground(s, x, z);
            w.fill(x, 0, z, x, 4, z, sub);
            w.fill(x, 5, z, x, h - 1, z, peat);
            int lx = Math.floorMod(x, T), lz = Math.floorMod(z, T);
            int d = Math.max(Math.abs(lx - C - 4), Math.abs(lz - C));
            if (h <= 19 && d > 22) {
                // a bog pool
                w.set(x, h, z, peat);
                w.fill(x, h + 1, z, x, 20, z, water);
                return;
            }
            w.set(x, h, z, grass);
            if (d <= 14) return;
            float r = Hash.unit(s, x, z, 3);
            boolean wallZ = Math.floorMod(z, 64) == 20, wallX = Math.floorMod(x, 64) == 40;
            if ((wallZ && Hash.unit(s, Math.floorDiv(x, 7), z, 4) > 0.18f) || (wallX && Hash.unit(s, x, Math.floorDiv(z, 7), 5) > 0.18f)) {
                w.set(x, h + 1, z, drystone);
                if (Hash.unit(s, x, z, 6) < 0.3f) w.set(x, h + 2, z, drystone);
                return;
            }
            if (r < 0.004f) {
                w.set(x, h + 1, z, granite);
                if (r < 0.0015f) w.set(x, h + 2, z, granite);
            } else if (r < 0.2f) w.set(x, h + 1, z, heather);
            else if (r < 0.3f) w.set(x, h + 1, z, tussock);
        }

        void site(ChunkWriter w, long s, int tx, int tz) {
            int bx = tx * T, bz = tz * T;
            int h0 = rawHeight(s, bx + C, bz + C);
            int mt = h0 + 34;
            // the mast: a 3x3 red and white lattice with a ladder up the middle
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int x = bx + C + dx, z = bz + C + dz;
                    for (int y = h0 + 1; y <= mt; y++) {
                        if (dx == 0 && dz == 0) w.set(x, y, z, ladder.setValue(LadderBlock.FACING, Direction.NORTH));
                        else if (dx == 0 && dz == -1 && y <= h0 + 2) w.set(x, y, z, ModBlocks.AIR);
                        else if (dx != 0 && dz != 0 && y == h0 + 18) w.set(x, y, z, beacon);
                        else w.set(x, y, z, lattice);
                    }
                }
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    w.set(bx + C + dx, h0, bz + C + dz, hutFloor);
                    if (dx != 0 || dz != 0) w.set(bx + C + dx, mt + 1, bz + C + dz, platform);
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) w.set(bx + C + dx, mt + 2, bz + C + dz, beacon);
                }
            w.set(bx + C, mt + 1, bz + C, ladder.setValue(LadderBlock.FACING, Direction.NORTH));
            w.set(bx + C, mt + 2, bz + C, top);
            w.set(bx + C, h0 + 3, bz + C - 2, OutsideKit.facing(sign, Direction.SOUTH));
            hut(w, s, bx + C + 5, bz + C - 4, h0);
            // a radio left out on the moor, on a rock
            long hh = Hash.hash(s, tx, tz, 8);
            int rx = bx + 4 + Hash.range(hh, 10), rz = bz + 48 + Hash.range(hh >>> 12, 10);
            int rh = ground(s, rx, rz);
            if (rh > 19) {
                w.set(rx, rh + 1, rz, granite);
                w.set(rx, rh + 2, rz, OutsideKit.facing(radio, Direction.from2DDataValue(Hash.range(hh >>> 24, 4))));
            }
            // a car with its lights on, nobody in it
            if (Hash.unit(hh >>> 30) < 0.2f) {
                int cx = bx + 6, cz = bz + 8;
                OutsideKit.car(w, cx, ground(s, cx, cz) + 1, cz, Direction.WEST, "beige", true);
            }
        }

        /** The hut: concrete, one room, a radio on a desk, a door marked TX. x0,z0 is the north-west corner. */
        void hut(ChunkWriter w, long s, int x0, int z0, int h0) {
            for (int dx = 0; dx <= 6; dx++)
                for (int dz = 0; dz <= 6; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    boolean edge = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                    w.set(x, h0, z, hutFloor);
                    for (int y = h0 + 1; y <= h0 + 3; y++) w.set(x, y, z, edge ? hutWall : ModBlocks.AIR);
                    w.set(x, h0 + 4, z, roof);
                }
            // doorway facing the mast, windows on the south side
            w.set(x0, h0 + 1, z0 + 3, ModBlocks.AIR);
            w.set(x0, h0 + 2, z0 + 3, ModBlocks.AIR);
            w.set(x0 + 2, h0 + 2, z0 + 6, window);
            w.set(x0 + 4, h0 + 2, z0 + 6, window);
            w.set(x0 + 6, h0 + 2, z0 + 3, window);
            // the TX door in the north wall
            w.set(x0 + 3, h0 + 1, z0, txDoor);
            w.set(x0 + 3, h0 + 2, z0, txDoor);
            w.set(x0 + 3, h0 + 3, z0 + 1, OutsideKit.facing(sign, Direction.NORTH));
            w.set(x0 + 5, h0 + 1, z0 + 3, OutsideKit.facing(radio, Direction.EAST));
            w.set(x0 + 4, h0 + 1, z0 + 3, OutsideKit.facing(chair, Direction.EAST));
            w.set(x0 + 3, h0 + 3, z0 + 3, bulb);
        }
    }

    // ---------------------------------------------------------------- the radio

    private static final Map<UUID, Integer> FREQ = new ConcurrentHashMap<>();

    static int exitFreq(ServerLevel level) {
        return Hash.range(Hash.hash(OutsideKit.seed(level, ID), 1), N);
    }

    static int itsFreq(ServerLevel level) {
        long s = OutsideKit.seed(level, ID);
        return Math.floorMod(exitFreq(level) + 8 + Hash.range(Hash.hash(s, 2), N - 16), N);
    }

    static int cyc(int a, int b) {
        int d = Math.floorMod(a - b, N);
        return Math.min(d, N - d);
    }

    public static void tune(ServerLevel level, BlockPos pos, Player player, boolean down) {
        UUID id = player.getUUID();
        int f = Math.floorMod(FREQ.getOrDefault(id, Hash.range(id.getLeastSignificantBits(), N)) + (down ? -1 : 1), N);
        FREQ.put(id, f);
        int e = exitFreq(level), x = itsFreq(level);
        int dE = cyc(f, e), dX = cyc(f, x);
        String mhz = String.format("%.1f MHz  ", 87.5 + f * 0.5);
        level.playSound(null, pos, ModSounds.get("event.the_signal_tune"), SoundSource.BLOCKS, 0.9f, 0.8f + level.random.nextFloat() * 0.4f);
        if (dE == 0) {
            level.playSound(null, pos, ModSounds.get("event.the_signal_numbers"), SoundSource.BLOCKS, 1.3f, 1f);
            player.addTag("sig_tuned");
            player.displayClientMessage(Component.literal(mhz + "\"...four. Nine. Transmitter room is open to you. Come in out of the rain.\"")
                    .withStyle(ChatFormatting.GOLD), true);
            return;
        }
        if (dX == 0 && player instanceof ServerPlayer sp) {
            level.playSound(null, pos, ModSounds.get("event.the_signal_breathing"), SoundSource.BLOCKS, 1.5f, 1f);
            player.displayClientMessage(Component.literal(mhz + "It's breathing. It heard you.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            Sanity.add(sp, -10f);
            ServerPlayNetworking.send(sp, new NoclipNet.EventPayload("pulse", 0.8f));
            summon(level, sp);
            return;
        }
        int bars = Math.max(0, 5 - Math.min(dE, dX));
        StringBuilder b = new StringBuilder(mhz);
        for (int i = 0; i < 5; i++) b.append(i < bars ? '|' : '.');
        if (bars == 0) b.append("  static");
        else b.append(dE < dX ? "  ...tones, under the static" : "  ...something under the static");
        player.displayClientMessage(Component.literal(b.toString()).withStyle(bars > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY), true);
    }

    private static void summon(ServerLevel level, ServerPlayer p) {
        int made = 0;
        for (int attempt = 0; attempt < 16 && made < 2; attempt++) {
            double ang = level.random.nextDouble() * Math.PI * 2, d = 26 + level.random.nextInt(10);
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * d), z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
            if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Mob m = (Mob) ModEntities.lazy("hound").get().create(level);
            if (m == null) return;
            m.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
            m.setTarget(p);
            level.addFreshEntity(m);
            made++;
        }
    }

    public static void door(ServerLevel level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer sp && player.getTags().contains("sig_tuned")) {
            Travel.exit(sp, ExitType.DOOR, pos);
            return;
        }
        level.playSound(null, pos, ModSounds.get("event.the_signal_tune"), SoundSource.BLOCKS, 0.7f, 0.5f);
        player.displayClientMessage(Component.literal("Locked. A card behind wire glass: AUTHORISED FREQUENCY ONLY.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    // ---------------------------------------------------------------- the storm

    static final class Storm implements LevelBehavior {
        private static final Map<UUID, Long> NEXT = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            p.removeTag("sig_tuned");
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            long next = NEXT.computeIfAbsent(p.getUUID(), k -> now + 200 + level.random.nextInt(400));
            if (now < next) return;
            NEXT.put(p.getUUID(), now + 300 + level.random.nextInt(700));
            boolean high = p.getY() > ground(OutsideKit.seed(level, ID), p.getBlockX(), p.getBlockZ()) + 12;
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", high ? 1f : 0.55f));
            level.playSound(null, p.blockPosition(), ModSounds.get("event.the_signal_thunder"), SoundSource.WEATHER, high ? 2f : 1.2f,
                    0.9f + level.random.nextFloat() * 0.2f);
            if (high && !p.isCreative() && !p.isSpectator() && level.random.nextFloat() < 0.5f) {
                p.hurt(level.damageSources().lightningBolt(), 5f);
                p.displayClientMessage(Component.literal("The mast draws the lightning.").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
            }
        }
    }
}
