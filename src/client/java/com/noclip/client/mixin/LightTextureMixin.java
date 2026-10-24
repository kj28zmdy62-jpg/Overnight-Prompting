package com.noclip.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.noclip.client.ClientState;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * True darkness. In dark levels the lightmap's floor of ambient light (and the
 * brightness slider) are removed, so a block with no light is black, not grey.
 * Light sources, including flashlight beams, still light things normally.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void noclip$darken(float partialTick, CallbackInfo ci) {
        float d = ClientState.lightmapDarkness();
        if (d <= 0f) return;
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                int level = Math.max(sky, block);
                float t = level / 15f;
                float curve = t * t * (1.4f - 0.4f * t);
                float k = 1f - d + d * curve;
                int c = lightPixels.getPixelRGBA(block, sky);
                int r = (int) ((c & 0xFF) * k);
                int g = (int) (((c >> 8) & 0xFF) * k);
                int b = (int) (((c >> 16) & 0xFF) * k);
                lightPixels.setPixelRGBA(block, sky, 0xFF000000 | b << 16 | g << 8 | r);
            }
        }
    }
}
