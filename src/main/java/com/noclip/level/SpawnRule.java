package com.noclip.level;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.function.Supplier;

/**
 * How the entity director populates a level around each player.
 *
 * @param type        entity to spawn
 * @param weight      relative pick weight among this level's rules
 * @param maxNearby   cap of this type within 64 blocks of the player
 * @param minDist     never spawn closer than this
 * @param maxDist     never spawn further than this
 * @param chance      per-attempt probability (attempts happen every few seconds)
 * @param needsDark   only spawn where block light is below 5
 * @param unseen      only spawn outside the player's view
 */
public record SpawnRule(Supplier<? extends EntityType<? extends Mob>> type, int weight, int maxNearby,
                        int minDist, int maxDist, float chance, boolean needsDark, boolean unseen) {

    public static SpawnRule of(Supplier<? extends EntityType<? extends Mob>> type, int maxNearby, float chance) {
        return new SpawnRule(type, 1, maxNearby, 18, 40, chance, false, true);
    }

    public SpawnRule dark() {
        return new SpawnRule(type, weight, maxNearby, minDist, maxDist, chance, true, unseen);
    }

    public SpawnRule seen() {
        return new SpawnRule(type, weight, maxNearby, minDist, maxDist, chance, needsDark, false);
    }

    public SpawnRule dist(int min, int max) {
        return new SpawnRule(type, weight, maxNearby, min, max, chance, needsDark, unseen);
    }

    public SpawnRule weight(int w) {
        return new SpawnRule(type, w, maxNearby, minDist, maxDist, chance, needsDark, unseen);
    }
}
