package com.noclip.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.noclip.Noclip;
import com.noclip.client.ClientState;
import com.noclip.item.FlashlightItem;
import com.noclip.level.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

/**
 * Owns the full-screen post chain. Every frame it eases the uniforms toward the
 * current level's {@link Visuals}, then runs the shader over the world image.
 */
public final class BackroomsPost {
    private static PostChain chain;
    private static boolean failed;
    private static int width, height;
    private static final long start = System.nanoTime();
    private static final Visuals NONE = new Visuals().grain(0).aberration(0).vignette(0).saturation(1).contrast(1)
            .warp(0).scanlines(0).flicker(0).bloom(0).fog(-1, -1);
    private static float flash;

    private BackroomsPost() {}

    public static void render(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || failed) return;
        Visuals target = ClientState.level != null ? ClientState.level.visuals : NONE;
        float strengthTarget = ClientState.level != null || ClientState.insanity() > 0 || ClientState.camcorder ? 1f : 0f;
        ClientState.effectStrength += (strengthTarget - ClientState.effectStrength) * 0.05f;
        if (ClientState.effectStrength < 0.01f && strengthTarget == 0f) return;
        ease(ClientState.current, target, 0.04f);

        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), Noclip.id("shaders/post/backrooms.json"));
                width = height = -1;
            } catch (Exception e) {
                failed = true;
                Noclip.LOG.error("NOCLIP screen shader failed to load; continuing without it", e);
                return;
            }
        }
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }

        boolean lit = mc.player != null && FlashlightItem.holding(mc.player);
        flash += ((lit ? 1f : 0f) - flash) * 0.25f;
        float t = ((System.nanoTime() - start) / 1e9f) % 3600f;
        Visuals v = ClientState.current;
        float k = ClientState.effectStrength;
        for (PostPass pass : chain.passes) {
            EffectInstance e = pass.getEffect();
            e.safeGetUniform("Time").set(t);
            e.safeGetUniform("Grain").set(v.grain * k);
            e.safeGetUniform("Aberration").set(v.aberration * k);
            e.safeGetUniform("Vignette").set(v.vignette * k);
            e.safeGetUniform("Tint").set(lerp(1, v.tintR, k), lerp(1, v.tintG, k), lerp(1, v.tintB, k));
            e.safeGetUniform("Saturation").set(lerp(1, v.saturation, k));
            e.safeGetUniform("Contrast").set(lerp(1, v.contrast, k));
            e.safeGetUniform("Brightness").set(lerp(1, v.brightness, k));
            e.safeGetUniform("Warp").set(v.warp * k);
            e.safeGetUniform("Scanlines").set(v.scanlines * k);
            e.safeGetUniform("Flicker").set(v.flicker * k);
            e.safeGetUniform("Bloom").set(v.bloom * k);
            e.safeGetUniform("Darkness").set(v.darkness * k);
            e.safeGetUniform("Flash").set(flash * Math.max(0.35f, v.darkness));
            e.safeGetUniform("Insanity").set(ClientState.insanity());
            e.safeGetUniform("Noclip").set(ClientState.noclipFlash);
            e.safeGetUniform("Special").set(v.special);
            e.safeGetUniform("WallPush").set(Math.max(0f, Math.min(1f, (ClientState.wallPushTicks - 30) / 120f)));
            e.safeGetUniform("Far").set(mc.gameRenderer.getDepthFar());
            e.safeGetUniform("Pulse").set(ClientState.pulse);
            e.safeGetUniform("Camcorder").set(ClientState.camcorder ? 1f : 0f);
        }
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partialTick);
        mc.getMainRenderTarget().bindWrite(true);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void ease(Visuals c, Visuals t, float k) {
        c.grain += (t.grain - c.grain) * k;
        c.aberration += (t.aberration - c.aberration) * k;
        c.vignette += (t.vignette - c.vignette) * k;
        c.tintR += (t.tintR - c.tintR) * k;
        c.tintG += (t.tintG - c.tintG) * k;
        c.tintB += (t.tintB - c.tintB) * k;
        c.saturation += (t.saturation - c.saturation) * k;
        c.contrast += (t.contrast - c.contrast) * k;
        c.brightness += (t.brightness - c.brightness) * k;
        c.warp += (t.warp - c.warp) * k;
        c.scanlines += (t.scanlines - c.scanlines) * k;
        c.flicker += (t.flicker - c.flicker) * k;
        c.bloom += (t.bloom - c.bloom) * k;
        c.darkness += (t.darkness - c.darkness) * k * 2;
        c.special = t.special;
    }

    public static void reset() {
        if (chain != null) chain.close();
        chain = null;
        failed = false;
    }
}
