package com.noclip.entity;

import com.noclip.sanity.Sanity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Base for hostile Backrooms entities: they frighten, and they leave when nobody is around. */
public abstract class BackroomsMonster extends Monster implements Sanity.Frightening {
    protected BackroomsMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    @Override
    public void checkDespawn() {
        if (isPersistenceRequired()) return;
        Player p = level().getNearestPlayer(this, -1);
        if (p == null || p.distanceToSqr(this) > 96 * 96) discard();
        else noActionTime = 0;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return distanceSq > 96 * 96;
    }
}
