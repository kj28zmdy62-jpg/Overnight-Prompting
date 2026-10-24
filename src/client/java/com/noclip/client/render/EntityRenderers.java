package com.noclip.client.render;

import com.noclip.client.model.GeneratedModels;
import com.noclip.client.model.GenericModel;
import com.noclip.Noclip;
import com.noclip.entity.Faceling;
import com.noclip.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;

/** Registers model layers for every generated model and a renderer for every entity. */
public final class EntityRenderers {
    private static final Map<String, ModelLayerLocation> LAYERS = new HashMap<>();

    private EntityRenderers() {}

    public static ModelLayerLocation layer(String model) {
        return LAYERS.computeIfAbsent(model, m -> new ModelLayerLocation(Noclip.id(m), "main"));
    }

    public static void init() {
        GeneratedModels.LAYERS.forEach((name, def) -> EntityModelLayerRegistry.registerModelLayer(layer(name), def::get));

        generic(ModEntities.HOUND, "hound", GenericModel.Style.QUADRUPED, 0.7f, 1.15f, null);
        generic(ModEntities.MANNEQUIN, "mannequin", GenericModel.Style.STIFF, 0.5f, 1f, null);
        generic(ModEntities.SHADOW_FIGURE, "shadow_figure", GenericModel.Style.STIFF, 0f, 1.1f, GenericRenderer.tex("shadow_figure_glow"));
        generic(ModEntities.PARTYGOER, "partygoer", GenericModel.Style.ZOMBIE, 0.5f, 1f, null);
        generic(ModEntities.DEATHMOTH, "deathmoth", GenericModel.Style.FLYER, 0.4f, 1.1f, null);
        EntityRendererRegistry.register(ModEntities.FACELING, ctx -> new GenericRenderer<Faceling>(ctx,
                new GenericModel<>(ctx.bakeLayer(layer("faceling")), GenericModel.Style.HUMANOID), 0.5f, 1f,
                e -> GenericRenderer.tex("faceling_" + e.variant()), null));
        EntityRendererRegistry.register(ModEntities.SMILER, SmilerRenderer::new);
        EntityRendererRegistry.register(ModEntities.SKIN_STEALER, SkinStealerRenderer::new);
        com.noclip.client.render.entities.ClientEntityIndex.registerAll();
    }

    /** Standard renderer: one texture named after the model. */
    public static <T extends Mob> void generic(EntityType<T> type, String model, GenericModel.Style style, float shadow,
                                               float scale, ResourceLocation glow) {
        ResourceLocation tex = GenericRenderer.tex(model);
        EntityRendererRegistry.register(type, ctx -> new GenericRenderer<T>(ctx,
                new GenericModel<>(ctx.bakeLayer(layer(model)), style), shadow, scale, e -> tex, glow));
    }
}
