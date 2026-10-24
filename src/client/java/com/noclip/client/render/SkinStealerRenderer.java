package com.noclip.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noclip.entity.SkinStealer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Renders the Skin-Stealer wearing the skin of the player it copied. */
public class SkinStealerRenderer extends HumanoidMobRenderer<SkinStealer, PlayerModel<SkinStealer>> {
    private static final ResourceLocation FALLBACK = GenericRenderer.tex("skin_stealer");

    public SkinStealerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SkinStealer e) {
        String d = e.disguise();
        if (!d.isEmpty() && Minecraft.getInstance().getConnection() != null) {
            try {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(UUID.fromString(d));
                if (info != null) return info.getSkin().texture();
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FALLBACK;
    }

    @Override
    protected void scale(SkinStealer e, PoseStack pose, float partialTick) {
        // just a little too tall
        pose.scale(1.0f, 1.12f, 1.0f);
    }

}
