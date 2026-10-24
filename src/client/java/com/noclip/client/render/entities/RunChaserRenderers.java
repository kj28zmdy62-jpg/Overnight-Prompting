package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.RunChaser;

/** Level !'s pursuer: tall, reaching, eyes lit. */
public final class RunChaserRenderers {
    private RunChaserRenderers() {}

    public static void register() {
        EntityRenderers.generic(RunChaser.TYPE, "run_chaser", GenericModel.Style.ZOMBIE, 0.6f, 1.3f, GenericRenderer.tex("run_chaser_glow"));
    }
}
