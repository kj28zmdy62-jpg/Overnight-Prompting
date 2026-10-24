package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.WrappedOne;

public final class WrappedOneRenderers {
    private WrappedOneRenderers() {}

    public static void register() {
        EntityRenderers.generic(WrappedOne.TYPE, "wrapped_one", GenericModel.Style.STIFF, 0.3f, 0.75f, null);
    }
}
