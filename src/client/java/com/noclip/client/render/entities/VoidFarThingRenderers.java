package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.VoidFarThing;

/** The Void's distant figure: black body, a full-bright pale layer, five times your height. */
public final class VoidFarThingRenderers {
    private VoidFarThingRenderers() {}

    public static void register() {
        EntityRenderers.generic(VoidFarThing.TYPE, "void_far_thing", GenericModel.Style.STIFF, 0f, 5.0f, GenericRenderer.tex("void_far_thing_glow"));
    }
}
