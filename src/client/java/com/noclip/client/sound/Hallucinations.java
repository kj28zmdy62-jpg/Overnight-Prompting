package com.noclip.client.sound;

import com.noclip.client.ClientState;
import com.noclip.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * What a fraying mind hears. Below about 60 sanity, footsteps start following
 * you, things whisper just behind your shoulder, and your heartbeat gets loud.
 */
public final class Hallucinations {
    private static int heartbeatCooldown;

    private Hallucinations() {}

    public static void tick(Minecraft mc) {
        Player p = mc.player;
        if (p == null || mc.isPaused()) return;
        ClientState.noclipFlash = Math.max(0f, ClientState.noclipFlash - 0.035f);
        float ins = ClientState.insanity();

        // heartbeat below 25 sanity
        if (ClientState.shownSanity < 25f && ClientState.level != null) {
            if (--heartbeatCooldown <= 0) {
                float urgency = 1f - ClientState.shownSanity / 25f;
                heartbeatCooldown = (int) (22 - urgency * 10);
                ClientState.pulse = 0.4f + urgency * 0.6f;
                play(mc, ModSounds.get("event.heartbeat"), p.getX(), p.getY(), p.getZ(), 0.5f + urgency * 0.5f, 1f);
            }
        }
        ClientState.pulse *= 0.86f;

        if (ClientState.level == null || ins < 0.15f) return;
        RandomSource r = p.getRandom();
        if (r.nextFloat() < ins * ins * 0.006f) {
            String[] pool = {"halluc.whisper", "halluc.footsteps", "halluc.knock"};
            String ev = pool[r.nextInt(pool.length)];
            // always from behind
            double yaw = Math.toRadians(p.getYRot() + 180 + (r.nextFloat() - 0.5f) * 90);
            double d = ev.equals("halluc.whisper") ? 1.5 : 6 + r.nextFloat() * 10;
            play(mc, ModSounds.get(ev), p.getX() - Math.sin(yaw) * d, p.getEyeY(), p.getZ() + Math.cos(yaw) * d,
                    0.6f + ins * 0.4f, 0.9f + r.nextFloat() * 0.2f);
        }
    }

    public static void play(Minecraft mc, SoundEvent ev, double x, double y, double z, float vol, float pitch) {
        mc.getSoundManager().play(new SimpleSoundInstance(ev, SoundSource.AMBIENT, vol, pitch, RandomSource.create(), x, y, z));
    }
}
