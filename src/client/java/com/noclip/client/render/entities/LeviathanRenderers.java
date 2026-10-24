package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.Leviathan;

/** Level 7's thing below: the generated model scaled up enormously, with pale glowing eyes. */
public final class LeviathanRenderers {
    private LeviathanRenderers() {}

    public static void register() {
        EntityRenderers.generic(Leviathan.TYPE, "leviathan", GenericModel.Style.FLOAT, 0f, 4.5f, GenericRenderer.tex("leviathan_glow"));
    }
}
