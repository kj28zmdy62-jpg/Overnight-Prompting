package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.Seeker;

public final class SeekerRenderers {
    private SeekerRenderers() {}

    public static void register() {
        EntityRenderers.generic(Seeker.TYPE, "fp_seeker", GenericModel.Style.HUMANOID, 0.35f, 0.72f, null);
    }
}
