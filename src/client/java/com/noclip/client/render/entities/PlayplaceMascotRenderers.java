package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.PlayplaceMascot;

public final class PlayplaceMascotRenderers {
    private PlayplaceMascotRenderers() {}

    public static void register() {
        EntityRenderers.generic(PlayplaceMascot.TYPE, "playplace_mascot", GenericModel.Style.ZOMBIE, 0.6f, 1.15f, null);
    }
}
