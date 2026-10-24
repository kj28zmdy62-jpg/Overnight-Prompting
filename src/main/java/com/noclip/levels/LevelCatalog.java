package com.noclip.levels;

import com.noclip.level.Levels;

/** Every level in the mod. Order here is the order shown in the journal. */
public final class LevelCatalog {
    private LevelCatalog() {}

    public static void registerAll() {
        Levels.register(Level0.create());
    }
}
