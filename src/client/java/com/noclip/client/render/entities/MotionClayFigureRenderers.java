package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.MotionClayFigure;

/** Level 94's clay people: stiff poses, big heads. */
public final class MotionClayFigureRenderers {
    private MotionClayFigureRenderers() {}

    public static void register() {
        EntityRenderers.generic(MotionClayFigure.TYPE, "motion_clay_figure", GenericModel.Style.STIFF, 0.5f, 1.15f, null);
    }
}
