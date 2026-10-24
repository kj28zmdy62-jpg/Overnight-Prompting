package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.PgPhotographer;

public final class PgPhotographerRenderers {
    private PgPhotographerRenderers() {}

    public static void register() {
        EntityRenderers.generic(PgPhotographer.TYPE, "pg_photographer", GenericModel.Style.STIFF, 0.5f, 1.1f, null);
    }
}
