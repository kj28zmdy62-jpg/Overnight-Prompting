package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.RecResident;

public final class RecResidentRenderers {
    private RecResidentRenderers() {}

    public static void register() {
        EntityRenderers.generic(RecResident.TYPE, "rec_resident", GenericModel.Style.STIFF, 0.5f, 1.04f, null);
    }
}
