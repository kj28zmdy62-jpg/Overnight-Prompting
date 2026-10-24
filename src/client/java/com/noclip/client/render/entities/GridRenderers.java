package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.GrHorizon;
import com.noclip.entity.GrSentinel;

public final class GridRenderers {
    private GridRenderers() {}

    public static void register() {
        EntityRenderers.generic(GrHorizon.TYPE, "gr_horizon", GenericModel.Style.STIFF, 0f, 12f, GenericRenderer.tex("gr_horizon"));
        EntityRenderers.generic(GrSentinel.TYPE, "gr_sentinel", GenericModel.Style.ZOMBIE, 0.4f, 1.05f, GenericRenderer.tex("gr_sentinel_glow"));
    }
}
