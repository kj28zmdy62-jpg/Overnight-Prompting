package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.GasAttendant;

public final class GasAttendantRenderers {
    private GasAttendantRenderers() {}

    public static void register() {
        EntityRenderers.generic(GasAttendant.TYPE, "gs_attendant", GenericModel.Style.STIFF, 0.5f, 1.0f, null);
    }
}
