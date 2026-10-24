package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.MtNull;

public final class MtNullRenderers {
    private MtNullRenderers() {}

    public static void register() {
        EntityRenderers.generic(MtNull.TYPE, "mt_null", GenericModel.Style.STIFF, 0.5f, 1.05f, null);
    }
}
