package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.DuctCrawler;

public final class DuctCrawlerRenderers {
    private DuctCrawlerRenderers() {}

    public static void register() {
        EntityRenderers.generic(DuctCrawler.TYPE, "duct_crawler", GenericModel.Style.QUADRUPED, 0.4f, 1f, null);
    }
}
