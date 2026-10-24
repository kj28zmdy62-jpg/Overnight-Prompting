package com.noclip.level;

/** The ways a wanderer can leave a level. Each level maps these to destination levels. */
public enum ExitType {
    /** Pushing through a wall long enough. The classic noclip. */
    NOCLIP,
    /** A door or doorway built into the level. */
    DOOR,
    /** Falling into a hole, pit, drain or the void. */
    FALL,
    /** Going down stairs, ladders or elevators. */
    DESCEND,
    /** Going up stairs, ladders or elevators. */
    ASCEND,
    /** Water: swimming down, drains, pools. */
    WATER,
    /** A level-specific secret. */
    SECRET
}
