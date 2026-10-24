package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.StaticMan;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** The Broadcast flickers between three frames of static. */
public final class StaticManRenderers {
    private StaticManRenderers() {}

    public static void register() {
        EntityRendererRegistry.register(StaticMan.TYPE, ctx -> new GenericRenderer<StaticMan>(ctx,
                new GenericModel<>(ctx.bakeLayer(EntityRenderers.layer("static_man")), GenericModel.Style.STIFF), 0f, 1.08f,
                e -> GenericRenderer.tex("static_man_" + ((e.tickCount / 2) % 3)), null));
    }
}
