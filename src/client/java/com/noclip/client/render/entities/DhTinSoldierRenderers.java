package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.DhTinSoldier;

public final class DhTinSoldierRenderers {
    private DhTinSoldierRenderers() {}

    public static void register() {
        EntityRenderers.generic(DhTinSoldier.TYPE, "dh_tin_soldier", GenericModel.Style.STIFF, 0.8f, 1.5f, null);
    }
}
