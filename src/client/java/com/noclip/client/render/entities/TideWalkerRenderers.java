package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.TideWalker;

public final class TideWalkerRenderers {
    private TideWalkerRenderers() {}

    public static void register() {
        EntityRenderers.generic(TideWalker.TYPE, "sb_tidewalker", GenericModel.Style.ZOMBIE, 0.5f, 1.05f, null);
    }
}
