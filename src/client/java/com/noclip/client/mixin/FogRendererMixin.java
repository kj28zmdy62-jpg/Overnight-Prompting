package com.noclip.client.mixin;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.noclip.client.ClientState;
import com.noclip.level.Visuals;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Per-level fog distance and colour: the Backrooms always fade into haze. */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void noclip$fog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thick, float partialTick, CallbackInfo ci) {
        if (ClientState.level == null || camera.getFluidInCamera() != FogType.NONE) return;
        Visuals v = ClientState.level.visuals;
        if (v.fogEnd < 0) return;
        float squeeze = 1f - ClientState.insanity() * 0.35f;
        float end = Math.min(v.fogEnd * squeeze, renderDistance);
        float start = Math.min(v.fogStart * squeeze, end - 1f);
        if (mode == FogRenderer.FogMode.FOG_SKY) start = 0f;
        RenderSystem.setShaderFogStart(start);
        RenderSystem.setShaderFogEnd(end);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void noclip$fogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darken, CallbackInfo ci) {
        if (ClientState.level == null || camera.getFluidInCamera() != FogType.NONE) return;
        Visuals v = ClientState.level.visuals;
        if (v.fogR < 0) return;
        FogRenderer.fogRed = v.fogR;
        FogRenderer.fogGreen = v.fogG;
        FogRenderer.fogBlue = v.fogB;
        RenderSystem.clearColor(v.fogR, v.fogG, v.fogB, 0f);
    }
}
