package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.ZzBlank;

public final class ZzBlankRenderers {
    private ZzBlankRenderers() {}

    public static void register() {
        // a heavy shadow: in Zero-Zero it is the only way to see it
        EntityRenderers.generic(ZzBlank.TYPE, "zz_blank", GenericModel.Style.STIFF, 1.0f, 1.02f, null);
    }
}
