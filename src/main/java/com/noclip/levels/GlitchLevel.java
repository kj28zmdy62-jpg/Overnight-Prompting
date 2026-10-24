package com.noclip.levels;

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
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level ???. A level that was never saved properly: the Lobby, the warehouse
 * and the dark maze, torn into strips and pasted back shifted, stretched and
 * smeared. It cannot settle on a name; the name in the corner of your eye
 * changes every second or so. The torn black doorways go to whatever level the
 * place is calling itself at the instant you step through. If it is calling
 * itself nothing, you come out somewhere else inside it.
 */
public final class GlitchLevel {
    static final int FLOOR = 10;

    /** What it calls itself. A null id is a name that is not a place. */
    static final String[][] NAMES = {
            {"level_0", "LEVEL 0"}, {null, "LEVEL ???"}, {"level_1", "LEVEL 1"}, {null, "LEVEL NaN"},
            {"level_6", "LEVEL 6"}, {null, "LEVEL -1"}, {"level_37", "LEVEL 37"}, {null, "L E V E L"},
            {"recursion", "RECURSION"}, {null, "LEVEL 0.0000001"}, {"red_room", "THE RED ROOM"}, {null, "NULL"},
            {"level_fun", "LEVEL FUN =)"}, {null, "LEVEL ??"}, {"missing_textures", "MISSING TEXTURES"},
            {null, "LEVEL 0 0 0 0"}, {"zero_zero", "LEVEL ZERO-ZERO"}, {null, "YOU ARE NOT HERE"},
    };

    private GlitchLevel() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("glitch_level")
                .number("").title("Level ???").order(4140)
                .tagline("It cannot decide what it is called. Its doors go wherever it is called right now.")
                .danger(BackroomsLevel.Danger.UNKNOWN)
                .generator(Gen::new)
                .behavior(new Names())
                .visuals(new Visuals().tint(1.0f, 0.96f, 1.06f).saturation(1.0f).contrast(1.12f).brightness(1.05f)
                        .grain(0.12f).aberration(0.006f).vignette(0.5f).warp(0.05f).scanlines(0.14f).flicker(0.08f)
                        .fog(4f, 40f).fogColor(0x1A1424).bloom(0.5f))
                .sanityDrain(0.05f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.06f).seen().dist(28, 44).weight(1))
                .exit(ExitType.FALL, "glitch_level", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "missing_textures", 1)
                .exit(ExitType.NOCLIP, "loading_screen", 1)
                .build();
    }

    // ---------------------------------------------------------------- names

    static final Map<UUID, Integer> CURRENT = new ConcurrentHashMap<>();

    static boolean real(int i) {
        String id = NAMES[i][0];
        return id != null && Levels.get(id) != null;
    }

    public static void enterDoor(ServerPlayer p) {
        if (Travel.onCooldown(p)) return;
        int i = CURRENT.getOrDefault(p.getUUID(), 1);
        if (real(i)) {
            Travel.sendTo(p, NAMES[i][0]);
        } else {
            // a name that is not a place: you come out somewhere else in here
            Sanity.add(p, -8f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.8f));
            Travel.sendTo(p, "glitch_level");
        }
    }

    static Component show(int i) {
        String t = NAMES[i][1];
        if (real(i)) return Component.literal(t).withStyle(ChatFormatting.WHITE);
        MutableComponent c = Component.empty();
        for (int k = 0; k < t.length(); k++) {
            String ch = t.substring(k, k + 1);
            boolean scramble = ch.equals("?") || (k % 3 == 1 && !ch.equals(" "));
            c.append(Component.literal(ch).withStyle(scramble ? ChatFormatting.OBFUSCATED : ChatFormatting.RESET)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return c;
    }

    static final class Names implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            // after the title card has had its moment
            if (p.tickCount % 5 != 0) return;
            int cur = CURRENT.getOrDefault(p.getUUID(), 1);
            int period = 20 + (int) (Hash.unit(Hash.hash(p.getUUID().getLeastSignificantBits(), p.tickCount / 25)) * 25);
            if (p.tickCount % period < 5) {
                int next;
                do {
                    next = p.getRandom().nextInt(NAMES.length);
                } while (next == cur || (NAMES[next][0] != null && !real(next)));
                cur = next;
                CURRENT.put(p.getUUID(), cur);
                p.playNotifySound(ModSounds.get("event.glitch_level_tick"), SoundSource.AMBIENT, 0.15f, 0.8f + p.getRandom().nextFloat() * 0.4f);
            }
            p.displayClientMessage(show(cur), true);
        }
    }

    // ---------------------------------------------------------------- torn geometry

    static final class Gen implements LevelGenerator {
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState door = ModBlocks.state("gl_doorway");
        final BlockState stripes = ModBlocks.state("gl_stripes");
        final BlockState stat = ModBlocks.state("gl_static");
        final BlockState sign = ModBlocks.state("gl_sign");
        // the Lobby
        final BlockState paper = ModBlocks.state("yellow_wallpaper"), paperS = ModBlocks.state("yellow_wallpaper_stained");
        final BlockState smear = ModBlocks.state("gl_smear"), base = ModBlocks.state("yellow_wallpaper_baseboard");
        final BlockState carpet = ModBlocks.state("moist_carpet"), carpetG = ModBlocks.state("gl_carpet");
        final BlockState tile = ModBlocks.state("ceiling_tile"), light = ModBlocks.state("fluorescent_light");
        final BlockState flick = ModBlocks.state("flickering_fluorescent_light");
        // the warehouse
        final BlockState conc = ModBlocks.state("warehouse_concrete"), concG = ModBlocks.state("gl_concrete");
        final BlockState pillar = ModBlocks.state("concrete_pillar"), wCeil = ModBlocks.state("warehouse_ceiling");
        final BlockState strip = ModBlocks.state("strip_light");
        // the dark
        final BlockState cinder = ModBlocks.state("cinder_block"), rough = ModBlocks.state("rough_concrete");
        final BlockState low = ModBlocks.state("low_ceiling");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid l0 = new CellGrid(s, 8, 3), l6 = new CellGrid(s ^ 0x66, 3, 0);
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    // the copy is torn into bands, each pasted back a little to one side
                    int band = Math.floorDiv(z, 5 + Hash.range(s, Math.floorDiv(z, 7), 0x5B, 4));
                    int shift = Hash.chance(s, band, 0x5C, 0.45f) ? Hash.range(s, band, 0x5D, 9) - 4 : 0;
                    int vband = Math.floorDiv(x, 11);
                    int vshift = Hash.chance(s, vband, 0x5E, 0.25f) ? Hash.range(s, vband, 0x5F, 7) - 3 : 0;
                    int sx = x + shift, sz = z + vshift;
                    float st = Hash.noise(s ^ 0x57, sx / 34.0, sz / 34.0);
                    int style = st < 0.42f ? 0 : st < 0.7f ? 1 : 2;
                    boolean seam = Hash.chance(s, band, 0x5A, 0.3f) && Math.floorMod(z, 5 + Hash.range(s, Math.floorDiv(z, 7), 0x5B, 4)) == 0;
                    float tear = Hash.noise(s ^ 0x7EA, x / 18.0, z / 18.0);
                    boolean hole = Hash.noise(s ^ 0x401E, x / 9.0, z / 9.0) > 0.86f;
                    column(w, s, l0, l6, x, z, sx, sz, style, seam, tear, hole);
                }
            }
        }

        void column(ChunkWriter w, long s, CellGrid l0, CellGrid l6, int x, int z, int sx, int sz, int style,
                    boolean seam, float tear, boolean hole) {
            int F = FLOOR;
            int ceil = style == 1 ? F + 8 : F + 4;
            boolean wall;
            BlockState floor, wallB, ceilB;
            switch (style) {
                case 0 -> {
                    wall = l0Wall(s, l0, sx, sz);
                    floor = Hash.chance(s, sx, sz, 1, 0.08f) ? carpetG : carpet;
                    wallB = Hash.chance(s, sx, sz, 2, 0.1f) ? smear : Hash.chance(s, sx, sz, 3, 0.1f) ? paperS : paper;
                    ceilB = (Math.floorMod(sx, 4) <= 1 && Math.floorMod(sz, 4) == 2) ? (Hash.chance(s, sx, sz, 4, 0.2f) ? flick : light) : tile;
                }
                case 1 -> {
                    wall = Math.floorMod(sx, 12) <= 1 && Math.floorMod(sz, 12) <= 1;
                    floor = Hash.chance(s, sx, sz, 5, 0.1f) ? concG : conc;
                    wallB = pillar;
                    ceilB = wCeil;
                }
                default -> {
                    int i = l6.cellX(sx), k = l6.cellZ(sz);
                    int u = sx - l6.x0(i), v = sz - l6.z0(k);
                    wall = (u == 0 && v == 0) || (u == 0 && Hash.chance(s, i, k, 10, 0.42f)) || (v == 0 && Hash.chance(s, i, k, 11, 0.42f));
                    floor = rough;
                    wallB = cinder;
                    ceilB = low;
                }
            }
            if (hole && !wall) {
                for (int y = 2; y <= F; y++) w.set(x, y, z, ModBlocks.AIR);
                w.set(x, 1, z, pit);
            } else {
                w.set(x, F, z, floor);
            }
            boolean open = tear > 0.74f;
            if (wall || seam) {
                // walls in torn areas keep going, far past where the ceiling should have been
                int top = open ? ceil + 3 + Hash.range(s, x, z, 0x70, 9) : ceil - 1;
                for (int y = F + 1; y <= top; y++) {
                    BlockState b = seam ? stripes : (style == 0 && y == F + 1) ? base : wallB;
                    if (!seam && Hash.chance(s, x, y * 31 + z, 0x71, 0.025f)) b = stat;
                    w.set(x, y, z, b);
                }
                if (!seam && style == 0 && Hash.chance(s, x, z, 0x72, 0.004f)) w.set(x, F + 3, z, sign);
            } else {
                for (int y = F + 1; y < ceil; y++) w.set(x, y, z, ModBlocks.AIR);
            }
            if (!open) {
                w.set(x, ceil, z, ceilB);
                if (style == 1 && Math.floorMod(sx, 6) == 3 && Math.floorMod(sz, 6) == 3 && !wall) w.set(x, ceil - 1, z, strip);
                w.fill(x, ceil + 1, z, x, ceil + 2, z, filler);
            } else if (Hash.noise(s ^ 0xF10A, x / 6.0, z / 6.0) > 0.62f) {
                // a slab of floor that came loose and floats where the ceiling was
                w.set(x, ceil + 6, z, floor);
            }
            // torn doorways in the walls: rare, and black
            if (wall && !seam && !open && style != 1 && Hash.chance(s, Math.floorDiv(x, 6), Math.floorDiv(z, 6), 0xD0, 0.05f)
                    && Math.floorMod(x + z, 6) == 0) {
                w.set(x, F + 1, z, door);
                w.set(x, F + 2, z, door);
            }
        }

        boolean l0Wall(long s, CellGrid g, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int u = x - g.x0(i), v = z - g.z0(k);
            if (u == 0 && v == 0) return true;
            if (u == 0) {
                float r = Hash.unit(s, i, k, 100);
                if (r < 0.35f) return false;
                int gap = 1 + Hash.range(s, i, k, 101, Math.max(1, g.depth(k) - 3));
                return r < 0.6f || v < gap || v > gap + 1;
            }
            if (v == 0) {
                float r = Hash.unit(s, i, k, 102);
                if (r < 0.35f) return false;
                int gap = 1 + Hash.range(s, i, k, 103, Math.max(1, g.width(i) - 3));
                return r < 0.6f || u < gap || u > gap + 1;
            }
            return false;
        }
    }
}
