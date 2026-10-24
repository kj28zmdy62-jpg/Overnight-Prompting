package com.noclip.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

/** World options for server owners. */
public final class ModGameRules {
    /** Suffocating in a wall in the real world can drop you into Level 0. */
    public static GameRules.Key<GameRules.BooleanValue> NOCLIP_FROM_SUFFOCATION;
    /** Pushing into a wall long enough can make it give way. */
    public static GameRules.Key<GameRules.BooleanValue> NOCLIP_FROM_WALLS;
    /** Sanity drains in the Backrooms. Turn off for a calmer game. */
    public static GameRules.Key<GameRules.BooleanValue> BACKROOMS_SANITY;

    private ModGameRules() {}

    public static void init() {
        NOCLIP_FROM_SUFFOCATION = GameRuleRegistry.register("noclipFromSuffocation", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
        NOCLIP_FROM_WALLS = GameRuleRegistry.register("noclipFromWalls", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
        BACKROOMS_SANITY = GameRuleRegistry.register("backroomsSanity", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    }
}
