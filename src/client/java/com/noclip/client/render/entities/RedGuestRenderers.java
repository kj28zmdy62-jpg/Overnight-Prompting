package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.RedGuest;

public final class RedGuestRenderers {
    private RedGuestRenderers() {}

    public static void register() {
        EntityRenderers.generic(RedGuest.TYPE, "red_guest", GenericModel.Style.STIFF, 0.4f, 1.12f, null);
    }
}
