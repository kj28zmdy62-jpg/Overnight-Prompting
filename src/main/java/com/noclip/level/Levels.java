package com.noclip.level;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of every Backrooms level, keyed by id and by dimension. */
public final class Levels {
    private static final Map<String, BackroomsLevel> BY_ID = new LinkedHashMap<>();
    private static final Map<ResourceKey<Level>, BackroomsLevel> BY_DIM = new HashMap<>();
    private static final List<BackroomsLevel> ORDERED = new ArrayList<>();

    private Levels() {}

    public static BackroomsLevel register(BackroomsLevel level) {
        if (BY_ID.put(level.id, level) != null) throw new IllegalStateException("Duplicate level " + level.id);
        BY_DIM.put(level.dimension, level);
        ORDERED.add(level);
        ORDERED.sort((a, b) -> Integer.compare(a.order, b.order));
        return level;
    }

    public static BackroomsLevel get(String id) {
        return BY_ID.get(id);
    }

    public static BackroomsLevel get(ResourceKey<Level> dim) {
        return BY_DIM.get(dim);
    }

    public static boolean isBackrooms(ResourceKey<Level> dim) {
        return BY_DIM.containsKey(dim);
    }

    public static Collection<BackroomsLevel> all() {
        return Collections.unmodifiableList(ORDERED);
    }

    public static int count() {
        return ORDERED.size();
    }
}
