package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.Bellhop;

/** Level 5's Bellhop: a stiff, slightly too tall humanoid. */
public final class BellhopRenderers {
    private BellhopRenderers() {}

    public static void register() {
        EntityRenderers.generic(Bellhop.TYPE, "bellhop", GenericModel.Style.STIFF, 0.5f, 1.12f, null);
    }
}
