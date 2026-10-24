package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.entity.WindowWatcher;

/** Level 188's residents: stiff, pale, arms too long. */
public final class WindowWatcherRenderers {
    private WindowWatcherRenderers() {}

    public static void register() {
        EntityRenderers.generic(WindowWatcher.TYPE, "window_watcher", GenericModel.Style.STIFF, 0.4f, 1.05f, null);
    }
}
