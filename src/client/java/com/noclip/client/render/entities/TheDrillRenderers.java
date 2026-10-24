package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.TheDrill;

public final class TheDrillRenderers {
    private TheDrillRenderers() {}

    public static void register() {
        EntityRenderers.generic(TheDrill.TYPE, "the_drill", GenericModel.Style.STIFF, 0.5f, 1.05f,
                GenericRenderer.tex("the_drill_glow"));
    }
}
