package com.noclip.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side rules that make a level more than a set of blocks. */
public interface LevelBehavior {
    LevelBehavior NONE = new LevelBehavior() {};

    /** Called every tick for each player inside the level. */
    default void tickPlayer(ServerPlayer player, ServerLevel level, BackroomsLevel def) {}

    /** Called once when a player arrives. */
    default void onEnter(ServerPlayer player, ServerLevel level, BackroomsLevel def) {}

    /** Called every tick for the level while at least one player is inside. */
    default void tickLevel(ServerLevel level, BackroomsLevel def) {}
}
