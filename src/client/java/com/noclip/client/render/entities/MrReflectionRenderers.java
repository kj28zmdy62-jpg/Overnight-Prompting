package com.noclip.client.render.entities;

import com.noclip.entity.MrReflection;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Draws the Mirror Level reflection in the skin of the player it copies. */
public final class MrReflectionRenderers {
    private MrReflectionRenderers() {}

    public static void register() {
        EntityRendererRegistry.register(MrReflection.TYPE, Renderer::new);
    }

    static final class Renderer extends HumanoidMobRenderer<MrReflection, PlayerModel<MrReflection>> {
        Renderer(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(MrReflection e) {
            String o = e.owner();
            try {
                UUID id = UUID.fromString(o);
                if (Minecraft.getInstance().getConnection() != null) {
                    PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(id);
                    if (info != null) return info.getSkin().texture();
                }
                return DefaultPlayerSkin.get(id).texture();
            } catch (IllegalArgumentException ex) {
                return DefaultPlayerSkin.getDefaultTexture();
            }
        }
    }
}
