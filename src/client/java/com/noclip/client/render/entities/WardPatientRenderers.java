package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.WardPatient;

public final class WardPatientRenderers {
    private WardPatientRenderers() {}

    public static void register() {
        EntityRenderers.generic(WardPatient.TYPE, "ward_patient", GenericModel.Style.ZOMBIE, 0.5f, 1.0f, null);
    }
}
