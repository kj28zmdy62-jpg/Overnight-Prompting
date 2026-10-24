package com.noclip.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.noclip.Noclip;
import com.noclip.client.model.GenericModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/** Renderer for every generated-model entity: texture, scale and an optional glowing layer. */
public class GenericRenderer<T extends Mob> extends MobRenderer<T, GenericModel<T>> {
    private final Function<T, ResourceLocation> texture;
    private final float scale;

    public GenericRenderer(EntityRendererProvider.Context ctx, GenericModel<T> model, float shadow, float scale,
                           Function<T, ResourceLocation> texture, ResourceLocation glow) {
        super(ctx, model, shadow);
        this.texture = texture;
        this.scale = scale;
        if (glow != null) addLayer(new Glow<>(this, glow));
    }

    public static ResourceLocation tex(String name) {
        return Noclip.id("textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture.apply(entity);
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        if (scale != 1f) pose.scale(scale, scale, scale);
    }

    static class Glow<T extends Mob> extends RenderLayer<T, GenericModel<T>> {
        private final RenderType type;

        Glow(RenderLayerParent<T, GenericModel<T>> parent, ResourceLocation tex) {
            super(parent);
            this.type = RenderType.eyes(tex);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            VertexConsumer vc = buffers.getBuffer(type);
            getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}
