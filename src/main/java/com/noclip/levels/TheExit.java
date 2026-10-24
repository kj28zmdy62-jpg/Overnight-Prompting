package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Exit. You arrive in what looks like the Lobby, facing down a long hall.
 * The further you walk, the more the colour drains out of it, until there is
 * only white; then the walls stop, and there is grass, and morning, and a door
 * standing on its own. Behind it is home. Do not look back.
 */
public final class TheExit {
    static final int G = 8, CEIL = G + 5, JOURNEY = 400, PITCH = 16;
    static final int FADE_START = 110, WHITE_START = 230, MEADOW = 300, DOOR = 372, GLARE = 392;
    static final String ID = "the_exit";

    private TheExit() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Exit").order(9040)
                .tagline("This is the way out. Don't look back.")
                .danger(BackroomsLevel.Danger.SAFE)
                .generator(Gen::new)
                .behavior(new Orpheus())
                .visuals(new Visuals().tint(1.0f, 1.0f, 1.0f).saturation(0.8f).contrast(0.98f).brightness(1.15f)
                        .grain(0.03f).aberration(0.0006f).vignette(0.3f).warp(0.0f).scanlines(0f).flicker(0f)
                        .bloom(1.0f).fog(18f, 110f).fogColor(0xF2F0EA).sky().noRec())
                .sanityDrain(0.0f)
                .spawnY(G + 1, G + 1)
                .exit(ExitType.SECRET, "overworld", 1)
                .build();
    }

    static int stage(int xl) {
        if (xl < FADE_START) return 0;
        if (xl >= WHITE_START) return 4;
        return 1 + (xl - FADE_START) * 3 / (WHITE_START - FADE_START);
    }

    static final class Gen implements LevelGenerator {
        final BlockState[] wall = new BlockState[5], base = new BlockState[5], carpet = new BlockState[5];
        final BlockState ceiling = ModBlocks.state("exit_ceiling_tile");
        final BlockState light = ModBlocks.state("exit_ceiling_light");
        final BlockState l0light = ModBlocks.state("fluorescent_light");
        final BlockState l0ceil = ModBlocks.state("ceiling_tile");
        final BlockState grass = ModBlocks.state("exit_meadow_grass");
        final BlockState[] plants = {ModBlocks.state("exit_tall_grass"), ModBlocks.state("exit_tall_grass"),
                ModBlocks.state("exit_flower_white"), ModBlocks.state("exit_flower_blue")};
        final BlockState door = ModBlocks.state("exit_final_door");
        final BlockState doorTop = ModBlocks.state("exit_final_door_top");
        final BlockState frame = ModBlocks.state("exit_door_frame");
        final BlockState glare = ModBlocks.state("exit_glare_wall");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        Gen() {
            for (int k = 0; k < 5; k++) {
                wall[k] = ModBlocks.state("exit_wallpaper_" + k);
                base[k] = ModBlocks.state("exit_baseboard_" + k);
                carpet[k] = ModBlocks.state("exit_carpet_" + k);
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                int xl = Math.floorMod(x, JOURNEY);
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int zl = Math.floorMod(z, PITCH);
                    if (xl >= GLARE) {
                        // the far edge of the field is just light
                        w.set(x, G, z, grass);
                        w.fill(x, G + 1, z, x, G + 30, z, glare);
                        continue;
                    }
                    if (xl >= MEADOW) {
                        w.set(x, G, z, grass);
                        if (Hash.chance(s, x, z, 5, 0.22f)) w.set(x, G + 1, z, plants[Hash.range(s, x, z, 6, plants.length)]);
                        if (xl == DOOR && zl == 4) {
                            w.set(x, G + 1, z, door);
                            w.set(x, G + 2, z, doorTop);
                            w.set(x, G + 3, z, frame);
                            w.set(x, G + 1, z - 1, frame);
                            w.set(x, G + 2, z - 1, frame);
                            w.set(x, G + 3, z - 1, frame);
                            w.set(x, G + 1, z + 1, frame);
                            w.set(x, G + 2, z + 1, frame);
                            w.set(x, G + 3, z + 1, frame);
                        }
                        continue;
                    }
                    int k = stage(xl);
                    w.set(x, G, z, carpet[k]);
                    if (xl == 0 || zl == 0 || zl == 8) {
                        // the start wall behind you, and the two long walls of the hall
                        w.set(x, G + 1, z, base[k]);
                        for (int y = G + 2; y < CEIL; y++) w.set(x, y, z, wall[k]);
                        w.set(x, CEIL, z, wall[k]);
                    } else if (zl > 8) {
                        w.fill(x, G + 1, z, x, CEIL, z, wall[k]);
                    } else {
                        boolean lamp = zl == 4 && Math.floorMod(x, k >= 3 ? 3 : 5) == 0;
                        if (k == 0) w.set(x, CEIL, z, lamp ? l0light : l0ceil);
                        else w.set(x, CEIL, z, lamp ? light : ceiling);
                    }
                    w.set(x, CEIL + 1, z, wall[k]);
                }
            }
        }
    }

    /** Walk out. Do not turn around. */
    static final class Orpheus implements LevelBehavior {
        private final Map<UUID, Integer> lookedBack = new HashMap<>();
        private final Map<UUID, Integer> said = new HashMap<>();
        static final int[] AT = {50, 130, 190, 245, 300, 350};
        static final String[] LINES = {
                "The hum is quieter here.",
                "The wallpaper is losing its colour.",
                "You remember what your front door looks like.",
                "You remember your name.",
                "Outside.",
                "There is a door. It is the only thing out here."};

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            int x0 = Math.floorDiv(p.getBlockX(), JOURNEY) * JOURNEY + 4;
            int z0 = Math.floorDiv(p.getBlockZ(), PITCH) * PITCH + 4;
            p.teleportTo(level, x0 + 0.5, G + 1, z0 + 0.5, -90f, 0f);
            lookedBack.put(p.getUUID(), 0);
            said.put(p.getUUID(), 0);
            DeepEndUtil.title(p, Component.literal("Don't look back.").withStyle(ChatFormatting.WHITE), null, 20, 60, 30);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            UUID id = p.getUUID();
            int xl = Math.floorMod(p.getBlockX(), JOURNEY);
            int n = said.getOrDefault(id, 0);
            if (n < AT.length && xl >= AT[n] && xl < AT[n] + 40) {
                p.sendSystemMessage(Component.literal(LINES[n]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                said.put(id, n + 1);
            }
            if (p.tickCount % 20 == 0) Sanity.add(p, xl >= MEADOW ? 2f : 0.5f);
            // Orpheus: past the first stretch, turning to face the way you came sends you back
            boolean back = p.getViewVector(1f).x < -0.55;
            int lb = back && xl > 30 && xl < DOOR && !p.isCreative() ? lookedBack.getOrDefault(id, 0) + 1 : 0;
            lookedBack.put(id, lb);
            if (lb == 8) {
                level.playSound(null, p.blockPosition(), ModSounds.get("halluc.whisper"), SoundSource.AMBIENT, 1f, 0.8f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.8f));
                DeepEndUtil.actionbar(p, Component.literal("Don't.").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
            }
            if (lb > 34) {
                p.sendSystemMessage(Component.literal("You looked back.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                lookedBack.put(id, 0);
                Travel.sendTo(p, "level_0");
            }
        }
    }
}
