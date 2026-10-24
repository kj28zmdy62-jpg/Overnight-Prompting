package com.noclip.entity;

import com.noclip.net.NoclipNet;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * The Blank, of Level Zero-Zero. White on white: you see it only by its shadow,
 * the one shadow in the level that moves. It comes closer while you stand still
 * and stops when you walk. If it reaches you, it takes your bearings with it.
 */
public class ZzBlank extends BackroomsMonster {
    public static EntityType<ZzBlank> TYPE;
    private double lpx, lpz;
    private int still;

    public ZzBlank(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static void register() {
        TYPE = ModEntities.register("zz_blank", ZzBlank::new, MobCategory.MONSTER, 0.6f, 2.0f, createAttributes(),
                0xF2F2F0, 0xD8D8D8);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = level().getNearestPlayer(this, 48);
        if (p == null) return;
        double mv = (p.getX() - lpx) * (p.getX() - lpx) + (p.getZ() - lpz) * (p.getZ() - lpz);
        lpx = p.getX();
        lpz = p.getZ();
        still = mv < 0.0009 ? still + 1 : 0;
        getLookControl().setLookAt(p, 30f, 30f);
        if (p.isCreative() || p.isSpectator() || still < 10) {
            getNavigation().stop();
            return;
        }
        getNavigation().moveTo(p, 1.0);
        if (distanceToSqr(p) < 1.6 && p instanceof ServerPlayer sp) erase(sp);
    }

    /** Everything goes white, and you are somewhere else in it. */
    private void erase(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), ModSounds.get("event.zero_zero_erase"), SoundSource.HOSTILE, 1f, 1f);
        ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
        Sanity.add(p, -18f);
        for (int attempt = 0; attempt < 12; attempt++) {
            double a = random.nextDouble() * Math.PI * 2, d = 90 + random.nextInt(80);
            BlockPos t = BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d);
            level.getChunk(t);
            if (level.getBlockState(t).isAir() && level.getBlockState(t.above()).isAir()
                    && level.getBlockState(t.below()).isSolidRender(level, t.below())) {
                p.teleportTo(level, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                break;
            }
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) discard();
        return false;
    }

    @Override
    public float fear() {
        return 0.25f;
    }
}
