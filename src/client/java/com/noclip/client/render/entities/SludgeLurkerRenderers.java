package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.SludgeLurker;

public final class SludgeLurkerRenderers {
    private SludgeLurkerRenderers() {}

    public static void register() {
        EntityRenderers.generic(SludgeLurker.TYPE, "sludge_lurker", GenericModel.Style.ZOMBIE, 0f, 1f, null);
    }
}
