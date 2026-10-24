package com.noclip.entity;

import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Level 5: the Bellhop. Tall, red-jacketed, no face. Like the mannequins it only
 * moves while nobody is looking. If it reaches you it shows you to your room:
 * somewhere else in the hotel, far from where you were.
 */
public class Bellhop extends Mannequin {
    public static EntityType<Bellhop> TYPE;

    public Bellhop(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    public static void register() {
        TYPE = ModEntities.register("bellhop", Bellhop::new, MobCategory.MONSTER, 0.6f, 2.2f, createAttributes(), 0x7A1014, 0xD8B458);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount < 40) return;
        Player p = level().getNearestPlayer(this, 2.0);
        if (p instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator() && !Travel.onCooldown(sp)) {
            level().playSound(null, blockPosition(), ModSounds.get("entity.bellhop.bell"), SoundSource.HOSTILE, 1.5f, 1.0f);
            sp.displayClientMessage(Component.literal("\"Your room is ready.\"")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), true);
            Sanity.add(sp, -12f);
            Travel.sendTo(sp, "level_5");
            discard();
        }
    }

    @Override
    public float fear() {
        return 0.3f;
    }
}
