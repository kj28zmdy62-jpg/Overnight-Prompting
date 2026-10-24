package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.LiftAttendant;

/** The Lift Attendant: tall, stiff, polite. */
public final class LiftAttendantRenderers {
    private LiftAttendantRenderers() {}

    public static void register() {
        EntityRenderers.generic(LiftAttendant.TYPE, "lift_attendant", GenericModel.Style.STIFF, 0.5f, 1.06f, null);
    }
}
