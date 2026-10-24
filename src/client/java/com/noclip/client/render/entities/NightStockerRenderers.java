package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.NightStocker;

public final class NightStockerRenderers {
    private NightStockerRenderers() {}

    public static void register() {
        EntityRenderers.generic(NightStocker.TYPE, "night_stocker", GenericModel.Style.ZOMBIE, 0.5f, 1.04f, null);
    }
}
