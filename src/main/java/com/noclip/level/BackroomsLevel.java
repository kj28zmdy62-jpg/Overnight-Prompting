package com.noclip.level;

import com.noclip.Noclip;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Everything that defines one level of the Backrooms. Blocks come from the
 * {@link LevelGenerator}; behaviour from the {@link LevelBehavior}; the look from
 * {@link Visuals}. Dimension, biome and ambient sound JSON live in the data pack
 * under the same id.
 */
public final class BackroomsLevel {
    public enum Danger {
        SAFE("Class 0", "Safe, secure, devoid of entities"),
        LOW("Class 1", "Unsafe, mostly secure, few entities"),
        MODERATE("Class 2", "Unsafe, insecure, some entities"),
        HIGH("Class 3", "Unsafe, insecure, many entities"),
        DEADLY("Class 4", "Deadly. Do not stay"),
        UNKNOWN("Class ?", "Unknown");

        public final String label, description;

        Danger(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    public record WeightedTarget(String level, int weight) {}

    public final String id;
    public final String number;
    public final String title;
    public final String tagline;
    public final Danger danger;
    public final LevelGenerator generator;
    public final LevelBehavior behavior;
    public final Visuals visuals;
    public final float sanityDrain;
    public final int spawnMinY, spawnMaxY;
    public final boolean allowWaterSpawn;
    public final Map<ExitType, List<WeightedTarget>> exits;
    public final List<SpawnRule> spawns;
    public final ResourceKey<Level> dimension;
    public final int order;

    private BackroomsLevel(Builder b) {
        this.id = b.id;
        this.number = b.number;
        this.title = b.title;
        this.tagline = b.tagline;
        this.danger = b.danger;
        this.generator = b.generator.get();
        this.behavior = b.behavior;
        this.visuals = b.visuals;
        this.sanityDrain = b.sanityDrain;
        this.spawnMinY = b.spawnMinY;
        this.spawnMaxY = b.spawnMaxY;
        this.allowWaterSpawn = b.allowWaterSpawn;
        this.exits = b.exits;
        this.spawns = List.copyOf(b.spawns);
        this.order = b.order;
        this.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Noclip.MOD_ID, id));
    }

    /** "LEVEL 0", "LEVEL FUN =)", or just the title for named rooms. */
    public String displayNumber() {
        if (number == null || number.isEmpty()) return title.toUpperCase();
        return "LEVEL " + number.toUpperCase();
    }

    /** Picks a destination for this exit type. Levels that are not built yet are skipped. */
    public String pickExit(ExitType type, RandomSource random) {
        String r = pickFrom(exits.get(type), random);
        return r != null ? r : pickFrom(exits.get(ExitType.NOCLIP), random);
    }

    private static String pickFrom(List<WeightedTarget> list, RandomSource random) {
        if (list == null) return null;
        int total = 0;
        for (WeightedTarget t : list) if (available(t.level)) total += t.weight;
        if (total == 0) return null;
        int r = random.nextInt(total);
        for (WeightedTarget t : list) {
            if (!available(t.level)) continue;
            r -= t.weight;
            if (r < 0) return t.level;
        }
        return null;
    }

    private static boolean available(String id) {
        return "overworld".equals(id) || Levels.get(id) != null;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String number = "";
        private String title = "";
        private String tagline = "";
        private Danger danger = Danger.LOW;
        private Supplier<LevelGenerator> generator;
        private LevelBehavior behavior = LevelBehavior.NONE;
        private Visuals visuals = new Visuals();
        private float sanityDrain = 0.02f;
        private int spawnMinY = 1, spawnMaxY = 120;
        private boolean allowWaterSpawn = false;
        private final Map<ExitType, List<WeightedTarget>> exits = new EnumMap<>(ExitType.class);
        private final List<SpawnRule> spawns = new ArrayList<>();
        private int order;

        private Builder(String id) {
            this.id = id;
        }

        public Builder number(String n) { this.number = n; return this; }
        public Builder title(String t) { this.title = t; return this; }
        public Builder tagline(String t) { this.tagline = t; return this; }
        public Builder danger(Danger d) { this.danger = d; return this; }
        public Builder generator(Supplier<LevelGenerator> g) { this.generator = g; return this; }
        public Builder behavior(LevelBehavior b) { this.behavior = b; return this; }
        public Builder visuals(Visuals v) { this.visuals = v; return this; }
        public Builder sanityDrain(float perSecond) { this.sanityDrain = perSecond; return this; }
        public Builder spawnY(int min, int max) { this.spawnMinY = min; this.spawnMaxY = max; return this; }
        public Builder waterSpawn() { this.allowWaterSpawn = true; return this; }
        public Builder order(int o) { this.order = o; return this; }

        public Builder exit(ExitType type, String level, int weight) {
            exits.computeIfAbsent(type, k -> new ArrayList<>()).add(new WeightedTarget(level, weight));
            return this;
        }

        public Builder exit(ExitType type, String level) {
            return exit(type, level, 1);
        }

        public Builder spawn(SpawnRule rule) {
            spawns.add(rule);
            return this;
        }

        public BackroomsLevel build() {
            if (generator == null) throw new IllegalStateException("Level " + id + " has no generator");
            return new BackroomsLevel(this);
        }
    }
}
