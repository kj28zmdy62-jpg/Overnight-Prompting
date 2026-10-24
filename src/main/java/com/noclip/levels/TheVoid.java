package com.noclip.levels;

import com.noclip.entity.VoidFarThing;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Void. There is nothing here: no walls, no floor you can see, no light. Only
 * the hum, and, a very long way off, something pale and tall standing still.
 * It comes closer only while you look at it. Look away and it drifts back out.
 * If you can hold its gaze until it reaches you, it will show you the way out.
 */
public final class TheVoid {
    static final int FLOOR = 64;
    static final String ID = "the_void";

    private TheVoid() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Void").order(9020)
                .tagline("Nothing. Just you, the hum, and something very far away.")
                .danger(BackroomsLevel.Danger.UNKNOWN)
                .generator(Gen::new)
                .behavior(new Gaze())
                .visuals(new Visuals().darkness(1f).grain(0.09f).aberration(0.0025f).vignette(0.75f).saturation(0.4f)
                        .fog(150f, 260f).fogColor(0x000000).bloom(0.9f).flicker(0f).noRec())
                .sanityDrain(0.05f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.SECRET, "the_exit", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final net.minecraft.world.level.block.state.BlockState floor = ModBlocks.state("the_void_floor");

        @Override
        public void generate(ChunkWriter w) {
            w.layer(FLOOR, floor);
        }
    }

    static final class Gaze implements LevelBehavior {
        private final Map<UUID, Integer> thing = new HashMap<>();
        private final Map<UUID, Integer> arrived = new HashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            arrived.put(p.getUUID(), p.tickCount);
            thing.remove(p.getUUID());
        }

        VoidFarThing find(ServerPlayer p, ServerLevel level) {
            Integer id = thing.get(p.getUUID());
            Entity e = id != null ? level.getEntity(id) : null;
            if (e instanceof VoidFarThing t && t.isAlive()) return t;
            VoidFarThing t = VoidFarThing.TYPE.create(level);
            if (t == null) return null;
            t.watcher = p.getUUID();
            t.bearing = p.getYRot() + 140f + p.getRandom().nextFloat() * 80f;
            t.distance = 72;
            place(t, p);
            level.addFreshEntity(t);
            thing.put(p.getUUID(), t.getId());
            return t;
        }

        static void place(VoidFarThing t, ServerPlayer p) {
            double rad = Math.toRadians(t.bearing);
            double x = p.getX() - Math.sin(rad) * t.distance, z = p.getZ() + Math.cos(rad) * t.distance;
            float face = (float) Math.toDegrees(Math.atan2(-(p.getX() - x), p.getZ() - z));
            t.moveTo(x, FLOOR + 1, z, face, 0f);
            t.setYHeadRot(face);
            t.yBodyRot = face;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            if (p.getY() < FLOOR - 4) p.teleportTo(p.getX(), FLOOR + 1, p.getZ());
            VoidFarThing t = find(p, level);
            if (t == null) return;
            Vec3 head = t.position().add(0, 9, 0);
            Vec3 to = head.subtract(p.getEyePosition()).normalize();
            // its height makes it easy to find; holding it in the middle of your view is the hard part
            boolean gazing = p.getViewVector(1f).dot(to) > 0.96;
            if (gazing) {
                t.distance -= 0.04;
                if (p.tickCount % 20 == 0) Sanity.add(p, -0.6f);
                if (p.tickCount % 30 == 0) {
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", (float) Math.min(1.0, 1.2 - t.distance / 72.0)));
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.heartbeat"), SoundSource.AMBIENT,
                            (float) (0.3 + (72 - t.distance) / 72.0), 1f);
                }
            } else {
                t.distance = Math.min(72, t.distance + 0.12);
            }
            place(t, p);
            int secs = (p.tickCount - arrived.computeIfAbsent(p.getUUID(), u -> p.tickCount)) / 20;
            if (p.tickCount % 20 == 0) {
                if (secs == 40) hint(p, "Something is standing a very long way away. It is facing you.");
                if (secs == 100) hint(p, "It only comes closer while you look at it.");
                if (secs == 200) hint(p, "There is nothing else here. There never will be.");
            }
            if (t.distance < 4) {
                hint(p, "It bends down, very slowly, and opens a door in the dark.");
                t.discard();
                thing.remove(p.getUUID());
                Travel.exit(p, ExitType.SECRET, p.blockPosition());
            }
        }

        static void hint(ServerPlayer p, String s) {
            p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
