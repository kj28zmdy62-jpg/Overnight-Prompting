package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.Groundskeeper;

public final class GroundskeeperRenderers {
    private GroundskeeperRenderers() {}

    public static void register() {
        EntityRenderers.generic(Groundskeeper.TYPE, "gc_groundskeeper", GenericModel.Style.STIFF, 0.5f, 1.08f, null);
    }
}
