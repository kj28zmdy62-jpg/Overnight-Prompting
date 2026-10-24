package com.noclip.entity;

import com.noclip.item.FlashlightItem;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Level 8: pale, long-legged things that crawl up the cave walls. They come for
 * you in the dark. Put your flashlight on one and it shrieks and scuttles away,
 * for a while. Batteries do not last forever.
 */
public class CaveCrawler extends Spider implements Sanity.Frightening {
    public static EntityType<CaveCrawler> TYPE;
    private int flee;

    public CaveCrawler(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static void register() {
        TYPE = ModEntities.register("cave_crawler", CaveCrawler::new, MobCategory.MONSTER, 1.2f, 0.8f, createAttributes(), 0xC8C0B0, 0x4A1A14);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player lit = null;
        for (Player p : level().players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > 18 * 18 || !FlashlightItem.holding(p)) continue;
            Vec3 to = position().add(0, 0.4, 0).subtract(p.getEyePosition()).normalize();
            if (p.getViewVector(1f).dot(to) > 0.93 && p.hasLineOfSight(this)) lit = p;
        }
        if (lit != null) {
            if (flee <= 0) playSound(ModSounds.get("entity.cave_crawler.hiss"), 1.2f, 0.9f + random.nextFloat() * 0.3f);
            flee = 60;
            Vec3 away = position().subtract(lit.position()).normalize().scale(10);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.5);
        }
        if (flee > 0) {
            flee--;
            setTarget(null);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof net.minecraft.server.level.ServerPlayer sp) Sanity.add(sp, -4f);
        return hit;
    }

    @Override
    public void checkDespawn() {
        Player p = level().getNearestPlayer(this, -1);
        if (p == null || p.distanceToSqr(this) > 96 * 96) discard();
        else noActionTime = 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("entity.cave_crawler.chitter");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("entity.cave_crawler.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("entity.cave_crawler.hurt");
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float fear() {
        return getTarget() != null ? 0.5f : 0.25f;
    }
}
