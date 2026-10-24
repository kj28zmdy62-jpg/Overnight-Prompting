package com.noclip.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.noclip.entity.Smiler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** A grin and two eyes hanging in the dark, always turned toward you. */
public class SmilerRenderer extends EntityRenderer<Smiler> {
    private static final ResourceLocation TEX = GenericRenderer.tex("smiler");

    public SmilerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(Smiler e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.6 + Math.sin((e.tickCount + partialTick) * 0.05) * 0.05, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = 0.9f;
        pose.scale(s, s, s);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(TEX));
        float w = 1f, h = 0.5f;
        float flick = (e.tickCount / 3 + e.getId()) % 37 == 0 ? 0.4f : 1f;
        int c = (int) (255 * flick);
        vertex(vc, m, -w, -h, 0, 1, c);
        vertex(vc, m, w, -h, 1, 1, c);
        vertex(vc, m, w, h, 1, 0, c);
        vertex(vc, m, -w, h, 0, 0, c);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float u, float v, int c) {
        vc.addVertex(m, x, y, 0).setColor(c, c, c, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(Smiler e) {
        return TEX;
    }
}
