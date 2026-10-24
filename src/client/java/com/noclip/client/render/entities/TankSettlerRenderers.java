package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.TankSettler;

public final class TankSettlerRenderers {
    private TankSettlerRenderers() {}

    public static void register() {
        EntityRenderers.generic(TankSettler.TYPE, "tank_settler", GenericModel.Style.FLOAT, 0f, 1.6f, null);
    }
}
