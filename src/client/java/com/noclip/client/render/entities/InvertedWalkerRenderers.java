package com.noclip.client.render.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.InvertedWalker;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** The people on the Inverted Lobby's carpet are drawn upside down: feet on the carpet above you. */
public final class InvertedWalkerRenderers {
    private InvertedWalkerRenderers() {}

    public static void register() {
        EntityRendererRegistry.register(InvertedWalker.TYPE, ctx -> new GenericRenderer<InvertedWalker>(ctx,
                new GenericModel<>(ctx.bakeLayer(EntityRenderers.layer("inverted_walker")), GenericModel.Style.STIFF), 0f, 1f,
                e -> GenericRenderer.tex("inverted_walker"), null) {
            @Override
            protected void setupRotations(InvertedWalker e, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
                super.setupRotations(e, pose, bob, yBodyRot, partialTick, scale);
                pose.translate(0.0F, (e.getBbHeight() + 0.1F) / scale, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
        });
    }
}
