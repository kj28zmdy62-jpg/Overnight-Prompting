package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Photographer of the Photo Gallery: a tall man in black with an old
 * bellows camera where his head should be. He only moves while you are not
 * looking, and when he is close enough, he takes your picture.
 */
public class PgPhotographer extends BackroomsMonster {
    public static EntityType<PgPhotographer> TYPE;
    private boolean wasSeen = true;

    public PgPhotographer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("pg_photographer", PgPhotographer::new, MobCategory.MONSTER, 0.6f, 2.1f,
                createAttributes(), 0x141416, 0x6A6A70);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 40);
        if (p == null || tickCount > 20 * 120) {
            discard();
            return;
        }
        boolean seen = Mannequin.seenByAnyone(level(), this, 0.5);
        if (seen) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (!wasSeen) level().playSound(null, blockPosition(), ModSounds.get("event.photo_gallery_charge"), SoundSource.HOSTILE, 0.5f, 1.2f);
        } else if (!p.isCreative() && !p.isSpectator()) {
            getNavigation().moveTo(p, 1.0);
            if (distanceToSqr(p) < 3.0 && p instanceof ServerPlayer sp) {
                com.noclip.levels.PhotoGallery.photographed(sp);
                discard();
            }
        }
        wasSeen = seen;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) discard();
        return false;
    }

    @Override
    public float fear() {
        return 0.4f;
    }
}
