package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.CaveCrawler;

/** Level 8's Crawler: long splayed legs, glowing red eyes. */
public final class CaveCrawlerRenderers {
    private CaveCrawlerRenderers() {}

    public static void register() {
        EntityRenderers.generic(CaveCrawler.TYPE, "cave_crawler", GenericModel.Style.QUADRUPED, 0.6f, 1.25f,
                GenericRenderer.tex("cave_crawler_glow"));
    }
}
