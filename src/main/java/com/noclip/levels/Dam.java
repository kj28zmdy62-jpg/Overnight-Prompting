package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dam Interior. Mass-concrete chambers seventy blocks deep, a black pool at the bottom,
 * crossed by two-plank catwalks hung from chains. Work lights on the walls go down and
 * down into the dark. The chambers are joined by inspection galleries and by spillway
 * galleries, and when the siren sounds the spillways open: get up on a ledge or be
 * carried away. Looking down from a catwalk is a mistake. Jumping is a way out.
 */
public final class Dam {
    static final String ID = "dam";
    static final int CELL = 40, G = 72, TOP = G + 14, POOL = 20;

    private Dam() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Dam Interior").order(2140)
                .tagline("When the siren sounds, get off the spillway floor. Don't look down.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Spillway())
                .visuals(new Visuals().tint(0.96f, 1.0f, 1.04f).saturation(0.62f).contrast(1.14f).brightness(1.16f)
                        .grain(0.07f).aberration(0.0016f).vignette(0.55f).warp(0.016f).fog(6f, 64f).fogColor(0x1A1E20).bloom(0.65f))
                .sanityDrain(0.035f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.12f).seen().dist(24, 40).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.3f).dark().dist(12, 24).weight(1))
                .exit(ExitType.WATER, "level_7", 3).exit(ExitType.WATER, "storm_drains", 2).exit(ExitType.WATER, "sewers", 1)
                .exit(ExitType.DOOR, "power_plant", 2).exit(ExitType.DOOR, "water_treatment", 2).exit(ExitType.DOOR, "level_2", 1)
                .exit(ExitType.NOCLIP, "storm_drains", 2).exit(ExitType.NOCLIP, "power_plant", 1).exit(ExitType.NOCLIP, "level_7", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState conc = ModBlocks.state("dam_concrete");
        final BlockState concB = ModBlocks.state("dam_concrete_b");
        final BlockState wet = ModBlocks.state("dam_concrete_wet");
        final BlockState marker = ModBlocks.state("dam_marker");
        final BlockState seep = ModBlocks.state("dam_seep");
        final BlockState spill = ModBlocks.state("dam_spill_floor");
        final BlockState ledge = ModBlocks.state("dam_ledge");
        final BlockState catwalk = ModBlocks.state("dam_catwalk");
        final BlockState rail = ModBlocks.state("dam_rail");
        final BlockState penstock = ModBlocks.state("dam_penstock").setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        final BlockState light = ModBlocks.state("dam_work_light");
        final BlockState sluice = ModBlocks.state("dam_sluice");
        final BlockState door = ModBlocks.state("dam_door");
        final BlockState gLight = ModBlocks.state("dam_gallery_light");
        final BlockState ceiling = ModBlocks.state("dam_ceiling");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState water = Blocks.WATER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, 7, w.maxZ, sub);
            w.fill(w.minX, TOP + 1, w.minZ, w.maxX, TOP + 4, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
        }

        BlockState wall(long s, int x, int y, int z) {
            float r = Hash.unit(s, x * 31L + y, z, 2);
            if (r < 0.015f) return seep;
            if (r < 0.09f) return wet;
            if (r < 0.4f) return concB;
            return conc;
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
            int ci = Math.floorDiv(x, CELL), ck = Math.floorDiv(z, CELL);
            boolean xBand = lx <= 4, zBand = lz <= 6;
            if (xBand || zBand) {
                for (int y = 8; y <= TOP; y++) w.set(x, y, z, wall(s, x, y, z));
                boolean spillOpen = zBand && lz >= 1 && lz <= 5;
                boolean dryOpen = xBand && lx >= 1 && lx <= 3;
                if (spillOpen) {
                    boolean isLedge = lz == 1 || lz == 5;
                    w.set(x, G, z, isLedge ? conc : spill);
                    if (isLedge) w.set(x, G + 1, z, ledge);
                    w.fill(x, isLedge ? G + 2 : G + 1, z, x, G + 6, z, ModBlocks.AIR);
                    w.set(x, G + 7, z, lz == 3 && Math.floorMod(x, 8) == 0 ? gLight : ceiling);
                } else if (dryOpen) {
                    w.set(x, G, z, conc);
                    w.fill(x, G + 1, z, x, G + 4, z, ModBlocks.AIR);
                    w.set(x, G + 5, z, lx == 2 && Math.floorMod(z, 7) == 3 && Hash.unit(s, x, z, 3) > 0.1f ? gLight : ceiling);
                }
                // doorways onto the catwalks
                boolean xDoor = (lx == 0 || lx == 4) && (lz == 18 || lz == 19) && !zBand;
                boolean zDoor = (lz == 0 || lz == 6) && (lx == 21 || lx == 22) && !xBand;
                if (xDoor || zDoor) w.fill(x, G + 1, z, x, G + 3, z, ModBlocks.AIR);
                // the inspection door: one in some dry galleries
                if (lx == 0 && lz == 30 && Hash.unit(s, ci, ck, 4) < 0.35f) {
                    w.set(x, G + 1, z, door);
                    w.set(x, G + 2, z, door);
                }
                if (lx == 4 && lz == 12 && Hash.unit(s, ci, ck, 5) < 0.5f) w.set(x, G + 2, z, marker);
                return;
            }
            chamber(w, s, x, z, lx, lz, ci, ck);
        }

        void chamber(ChunkWriter w, long s, int x, int z, int lx, int lz, int ci, int ck) {
            w.set(x, 8, z, conc);
            w.fill(x, 9, z, x, POOL, z, water);
            w.set(x, TOP, z, conc);
            // intakes at the bottom of the pool
            if ((lx == 22 || lx == 23) && (lz == 23 || lz == 24)) w.set(x, 9, z, sluice);
            // penstocks in two corners
            if ((lx >= 7 && lx <= 9 && lz >= 9 && lz <= 11) || (lx >= 35 && lx <= 37 && lz >= 35 && lz <= 37)) {
                w.fill(x, 9, z, x, TOP - 1, z, penstock);
                return;
            }
            boolean xWalk = lz == 18 || lz == 19, zWalk = lx == 21 || lx == 22;
            boolean xRail = lz == 17 || lz == 20, zRail = lx == 20 || lx == 23;
            boolean cross = lx >= 20 && lx <= 23 && lz >= 17 && lz <= 20;
            if (xWalk || zWalk || cross) {
                boolean nearDoor = lx <= 6 || lx >= CELL - 2 || lz <= 8 || lz >= CELL - 2;
                boolean gap = !cross && !nearDoor && Hash.unit(s, x, z, 6) < 0.02f;
                if (!gap) w.set(x, G, z, catwalk);
                return;
            }
            if (xRail && lx >= 5 || zRail && lz >= 7) {
                boolean runsX = xRail;
                if (Hash.unit(s, x, z, 7) > 0.06f) {
                    w.set(x, G + 1, z, rail.setValue(runsX ? IronBarsBlock.EAST : IronBarsBlock.NORTH, true)
                            .setValue(runsX ? IronBarsBlock.WEST : IronBarsBlock.SOUTH, true));
                }
                if ((runsX ? Math.floorMod(lx, 6) : Math.floorMod(lz, 6)) == 0) w.fill(x, G + 2, z, x, TOP - 1, z, chain);
                return;
            }
            // work lights down the walls, deeper and deeper
            Direction face = lx == 5 ? Direction.WEST : lx == CELL - 1 ? Direction.EAST : lz == 7 ? Direction.NORTH : lz == CELL - 1 ? Direction.SOUTH : null;
            if (face != null) {
                int along = face.getAxis() == Direction.Axis.X ? lz : lx;
                if (Math.floorMod(along, 9) == 4) {
                    for (int y : new int[]{G + 3, G - 12, G - 27, G - 42}) {
                        if (Hash.unit(s, x * 7L + y, z, 8) > 0.2f) w.set(x, y, z, OutsideKit.facing(light, face));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The spillway siren and surge, and vertigo on the catwalks. */
    static final class Spillway implements LevelBehavior {
        /** {phase 0 idle / 1 warning / 2 surge, phase end, next siren} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, level.getGameTime() + 2000 + level.random.nextInt(2000)});
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0) {
                if (now < st[2] || level.players().isEmpty()) return;
                st[0] = 1;
                st[1] = now + 320;
                for (ServerPlayer p : level.players())
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.dam_siren"), SoundSource.AMBIENT, 1.6f, 1f);
                return;
            }
            if (st[0] == 1) {
                if (now % 20 == 0) {
                    for (ServerPlayer p : level.players())
                        p.displayClientMessage(Component.literal("SPILLWAY RELEASE IN " + (st[1] - now) / 20 + " S. CLEAR THE CHANNELS.")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
                    if ((st[1] - now) % 120 == 0)
                        for (ServerPlayer p : level.players())
                            level.playSound(null, p.blockPosition(), ModSounds.get("event.dam_siren"), SoundSource.AMBIENT, 1.4f, 1f);
                }
                if (now >= st[1]) {
                    st[0] = 2;
                    st[1] = now + 220;
                    for (ServerPlayer p : level.players())
                        level.playSound(null, p.blockPosition(), ModSounds.get("event.dam_surge"), SoundSource.AMBIENT, 1.8f, 1f);
                }
                return;
            }
            if (now >= st[1]) {
                st[0] = 0;
                st[2] = now + 3600 + level.random.nextInt(2400);
            } else if (now % 70 == 0) {
                for (ServerPlayer p : level.players())
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.dam_surge"), SoundSource.AMBIENT, 1.2f, 0.9f);
            }
        }

        /** You always arrive out on a catwalk, in the middle of a chamber. */
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            double x = Math.floorDiv(p.getBlockX(), CELL) * CELL + 22.0, z = Math.floorDiv(p.getBlockZ(), CELL) * CELL + 19.0;
            p.teleportTo(level, x, G + 1, z, level.random.nextBoolean() ? -90f : 90f, 10f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            BlockState under = level.getBlockState(p.blockPosition().below());
            long[] st = state(level);
            if (st[0] == 2 && under.is(ModBlocks.get("dam_spill_floor"))) {
                if (p.tickCount % 2 == 0) {
                    Vec3 v = p.getDeltaMovement();
                    p.setDeltaMovement(Math.min(v.x + 0.18, 0.7), v.y, v.z * 0.6 + (Math.floorMod(p.getBlockZ(), CELL) < 3 ? 0.03 : -0.03));
                    p.hurtMarked = true;
                    level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.3, p.getZ(), 12, 0.6, 0.2, 0.6, 0.2);
                }
                if (!p.isCreative() && p.tickCount % 20 == 0) {
                    p.hurt(level.damageSources().drown(), 2.5f);
                    Sanity.add(p, -2f);
                }
            }
            // vertigo: looking down through the grating
            if (p.tickCount % 20 == 0 && under.is(ModBlocks.get("dam_catwalk")) && p.getXRot() > 50f) {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false));
                Sanity.add(p, -1.5f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.25f));
                if (level.random.nextFloat() < 0.15f)
                    p.displayClientMessage(Component.literal("It's a long way down.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }
}
