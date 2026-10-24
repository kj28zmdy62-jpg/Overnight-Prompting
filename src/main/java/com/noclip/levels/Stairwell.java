package com.noclip.levels;

import com.noclip.block.StairDigitBlock;
import com.noclip.block.StairwellDoorBlock;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Stairwell. One concrete fire stair that never ends, its floor numbers
 * stencilled beside every door. Going up takes you down: climb and the numbers
 * fall. Climb far enough and floor 1 lets you out; keep going and you are in the
 * basements, where the numbers stop being numbers. Go down instead and the
 * numbers climb toward a roof that is not a floor. Every door is locked except
 * the right one. The eye of the stairwell goes down forever, and you can jump.
 */
public final class Stairwell {
    public static final String ID = "stairwell";
    static final int BASE = 2, H = 6, OX = 3, OZ = 5;
    static final int ROOF = 30, DEEP = -12;

    private Stairwell() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Stairwell").order(1180)
                .tagline("Going up takes you down. Watch the numbers.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Numbers())
                .visuals(new Visuals().tint(0.94f, 1.02f, 0.96f).saturation(0.72f).contrast(1.08f).brightness(1.25f)
                        .grain(0.08f).aberration(0.0014f).vignette(0.6f).bloom(0.4f).fog(3f, 30f).fogColor(0x1E2420))
                .sanityDrain(0.03f)
                .spawnY(80, 110)
                .exit(ExitType.NOCLIP, "level_1", 2).exit(ExitType.NOCLIP, "level_2", 1).exit(ExitType.NOCLIP, "lift_lobby", 1)
                .exit(ExitType.DOOR, "lift_lobby", 3).exit(ExitType.DOOR, "level_0", 1).exit(ExitType.DOOR, "waiting_room", 1)
                .exit(ExitType.DOOR, "hospital", 1).exit(ExitType.DOOR, "school", 1)
                .exit(ExitType.ASCEND, "rooftops", 3).exit(ExitType.ASCEND, "sky_rooms", 1).exit(ExitType.ASCEND, "level_11", 1)
                .exit(ExitType.DESCEND, "boiler_room", 2).exit(ExitType.DESCEND, "level_2", 2).exit(ExitType.DESCEND, "bunker", 1)
                .exit(ExitType.DESCEND, "parking_garage", 1).exit(ExitType.DESCEND, "storm_drains", 1)
                .exit(ExitType.FALL, "the_abyss", 2).exit(ExitType.FALL, "elevator_shaft", 1).exit(ExitType.FALL, "level_6", 1)
                .build();
    }

    // ---------------------------------------------------------------- the shaft

    static int flight1(int u) {
        return u - 2;
    }

    static int flight2(int u) {
        return Math.floorMod(4 + (5 - u), H);
    }

    static final class Gen implements LevelGenerator {
        final BlockState white = ModBlocks.state("stair_wall_white");
        final BlockState green = ModBlocks.state("stair_wall_green");
        final BlockState band = ModBlocks.state("stair_wall_band");
        final BlockState base = ModBlocks.state("stair_wall_base");
        final BlockState landing = ModBlocks.state("stair_landing");
        final BlockState under = ModBlocks.state("stair_underside");
        final BlockState stepE = ModBlocks.state("stair_steps").setValue(StairBlock.FACING, Direction.EAST);
        final BlockState stepW = ModBlocks.state("stair_steps").setValue(StairBlock.FACING, Direction.WEST);
        final BlockState railX = ModBlocks.state("stair_handrail");
        final BlockState railZ = ModBlocks.state("stair_handrail").setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        final BlockState lamp = ModBlocks.state("stair_lamp");
        final BlockState lampFlicker = ModBlocks.state("stair_lamp_flicker");
        final BlockState lampDead = ModBlocks.state("stair_lamp_dead");
        final BlockState digit = ModBlocks.state("stair_digit");
        final BlockState door = ModBlocks.state("stair_door").setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        final BlockState doorTop = ModBlocks.state("stair_door_top").setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        final BlockState hose = ModBlocks.state("stair_hose");
        final BlockState hoseTop = ModBlocks.state("stair_hose_top");
        final BlockState chair = ModBlocks.state("stair_chair");
        final BlockState papers = ModBlocks.state("stair_paper");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState[] graffiti = {ModBlocks.state("stair_graffiti_out"), ModBlocks.state("stair_graffiti_up"),
                ModBlocks.state("stair_graffiti_count"), ModBlocks.state("stair_graffiti_arrow")};
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            int top = w.maxY;
            int ox = w.minX + OX, oz = w.minZ + OZ;
            w.fill(w.minX, 0, w.minZ, w.maxX, top, w.maxZ, filler);
            w.fill(w.minX, 0, w.minZ, w.maxX, BASE - 1, w.maxZ, sub);
            for (int u = -1; u <= 9; u++) {
                for (int v = -1; v <= 7; v++) {
                    int x = ox + u, z = oz + v;
                    boolean ring = u == -1 || u == 9 || v == -1 || v == 7;
                    for (int y = BASE; y <= top - 2; y++) {
                        int ym = Math.floorMod(y - BASE, H);
                        w.set(x, y, z, ring ? wall(u, v, ym) : inside(u, v, ym));
                    }
                }
            }
            // per-floor fittings
            for (int idx = 0; BASE + idx * H + H <= top - 2; idx++) {
                int ya = BASE + idx * H, yb = ya + 3;
                long h = Hash.hash(s, w.minX >> 4, w.minZ >> 4, idx);
                w.set(ox - 1, ya + 3, oz + 1, digit);
                w.set(ox - 1, ya + 3, oz, digit);
                w.set(ox - 1, ya + 1, oz + 4, door);
                w.set(ox - 1, ya + 2, oz + 4, doorTop);
                w.set(ox - 1, ya + 3, oz + 6, lampFor(Hash.unit(h)));
                w.set(ox + 9, ya + 6, oz + 3, lampFor(Hash.unit(h >>> 3)));
                w.set(ox + 4, ya + 4, oz - 1, lampFor(Hash.unit(h >>> 27)));
                w.set(ox + 4, ya + 7, oz + 7, lampFor(Hash.unit(h >>> 33)));
                float r = Hash.unit(h >>> 6);
                if (r < 0.07f) {
                    float g = Hash.unit(h >>> 9);
                    int gi = g < 0.4f ? 0 : g < 0.7f ? 1 : g < 0.9f ? 2 : 3;
                    w.set(ox + 9, yb + 2, oz + 1, graffiti[gi]);
                } else if (r < 0.32f) {
                    w.set(ox + 9, yb + 1, oz + 5, hose);
                    w.set(ox + 9, yb + 2, oz + 5, hoseTop);
                }
                if (Hash.unit(h >>> 12) < 0.12f)
                    w.set(ox + 7, yb + 1, oz, chair.setValue(HorizontalDirectionalBlock.FACING,
                            Direction.from2DDataValue(Hash.range(h >>> 15, 4))));
                if (Hash.unit(h >>> 18) < 0.10f) w.set(ox + 1, ya + 1, oz + 5, papers);
                if (Hash.unit(h >>> 21) < 0.06f) w.set(ox + 7, yb + 1, oz + 4, papers);
                if (Hash.unit(h >>> 24) < 0.05f) w.set(ox + 1, ya + 1, oz + 2, puddle);
            }
        }

        BlockState lampFor(float r) {
            return r < 0.15f ? lampDead : r < 0.30f ? lampFlicker : lamp;
        }

        BlockState wall(int u, int v, int ym) {
            int sl;
            if (u == -1) sl = 0;
            else if (u == 9) sl = 3;
            else if (u <= 2) sl = 0;
            else if (u >= 6) sl = 3;
            else sl = v <= 0 ? flight1(u) : flight2(u);
            int rel = Math.floorMod(ym - sl, H);
            return switch (rel) {
                case 0 -> base;
                case 1 -> green;
                case 2 -> band;
                default -> white;
            };
        }

        BlockState inside(int u, int v, int ym) {
            if (u <= 2) return ym == 0 ? landing : ModBlocks.AIR;
            if (u >= 6) return ym == 3 ? landing : ym == 2 ? under : ModBlocks.AIR;
            if (v <= 1) {
                int st = flight1(u);
                if (ym == st) return stepE;
                if (ym == Math.floorMod(st - 1, H)) return under;
                return ModBlocks.AIR;
            }
            if (v >= 5) {
                int st = flight2(u);
                if (ym == st) return stepW;
                if (ym == Math.floorMod(st - 1, H)) return under;
                return ModBlocks.AIR;
            }
            if (v == 2) return ym == Math.floorMod(flight1(u) + 1, H) ? railX : ModBlocks.AIR;
            if (v == 4) return ym == Math.floorMod(flight2(u) + 1, H) ? railX : ModBlocks.AIR;
            if (u == 3 && ym == 1) return railZ;
            if (u == 5 && ym == 4) return railZ;
            return ModBlocks.AIR;
        }
    }

    // ---------------------------------------------------------------- the numbers

    static final class St {
        int value, idx, moves, cx, cz;
        final Map<Integer, Integer> values = new HashMap<>();
        long nextEvent, nextFigure;
    }

    static final Map<UUID, St> STATES = new HashMap<>();

    static int idxAt(double y) {
        return Math.floorDiv((int) Math.floor(y + 0.01) - BASE - 1, H);
    }

    static boolean isExit(int v) {
        return v == 1 || v >= ROOF || v <= DEEP;
    }

    /** {tens, units} glyphs for a floor value. */
    static int[] glyphs(int v, long salt) {
        float r = Hash.unit(salt);
        if (v >= ROOF) return new int[]{StairDigitBlock.EYE, StairDigitBlock.EYE};
        if (v >= 24) {
            int tens = v >= 27 ? (r < 0.5f ? StairDigitBlock.QUESTION : StairDigitBlock.SMEAR) : (v / 10) % 10;
            int units = r < 0.33f ? StairDigitBlock.QUESTION : r < 0.66f ? StairDigitBlock.SMEAR : (v * 7 + 3) % 10;
            return new int[]{tens, units};
        }
        if (v >= 10) return new int[]{(v / 10) % 10, v % 10};
        if (v >= 0) return new int[]{StairDigitBlock.BLANK, v};
        if (v >= -9) return new int[]{StairDigitBlock.B, -v};
        if (v > DEEP) return new int[]{StairDigitBlock.B, StairDigitBlock.QUESTION};
        return new int[]{StairDigitBlock.SMEAR, StairDigitBlock.EYE};
    }

    static void writeFloor(ServerLevel level, St st, int idx, int value) {
        int ox = st.cx * 16 + OX, oz = st.cz * 16 + OZ;
        int ya = BASE + idx * H;
        if (ya < BASE || ya + 3 >= level.getMaxBuildHeight() - 2) return;
        int[] g = glyphs(value, Hash.hash(idx, st.cx, st.cz, value));
        setGlyph(level, new BlockPos(ox - 1, ya + 3, oz + 1), g[0]);
        setGlyph(level, new BlockPos(ox - 1, ya + 3, oz), g[1]);
        boolean exit = isExit(value);
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos dp = new BlockPos(ox - 1, ya + dy, oz + 4);
            BlockState ds = level.getBlockState(dp);
            if (ds.getBlock() instanceof StairwellDoorBlock && ds.getValue(StairwellDoorBlock.OPEN) != exit)
                level.setBlock(dp, ds.setValue(StairwellDoorBlock.OPEN, exit), Block.UPDATE_CLIENTS);
        }
        BlockPos sign = new BlockPos(ox, ya + 3, oz + 4);
        BlockState ss = level.getBlockState(sign);
        if (exit && ss.isAir()) {
            level.setBlock(sign, ModBlocks.state("exit_sign").setValue(HorizontalDirectionalBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        } else if (!exit && ss.is(ModBlocks.get("exit_sign"))) {
            level.setBlock(sign, ModBlocks.AIR, Block.UPDATE_ALL);
        }
    }

    static void setGlyph(ServerLevel level, BlockPos p, int glyph) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof StairDigitBlock && s.getValue(StairDigitBlock.GLYPH) != glyph)
            level.setBlock(p, s.setValue(StairDigitBlock.GLYPH, glyph), Block.UPDATE_CLIENTS);
    }

    static void predict(ServerLevel level, St st, ServerPlayer p) {
        for (int j = -2; j <= 2; j++) {
            int v = st.value - j;
            if (j != 0 && Math.abs(j) == 1 && st.moves >= 2) {
                float r = p.getRandom().nextFloat();
                if (r < 0.13f) v = st.value;              // the same floor again
                else if (r < 0.17f) v = st.value + j;     // the wrong way
            }
            if (j == 0) v = st.value;
            st.values.put(st.idx + j, v);
            writeFloor(level, st, st.idx + j, v);
        }
    }

    public static void onDoor(ServerPlayer p, BlockPos pos) {
        St st = STATES.get(p.getUUID());
        ServerLevel level = p.serverLevel();
        BackroomsLevel def = Levels.get(level.dimension());
        Integer v = st == null || def == null || !ID.equals(def.id) ? null : st.values.get(Math.floorDiv(pos.getY() - BASE - 1, H));
        if (v != null && isExit(v)) {
            level.playSound(null, pos, ModSounds.get("event.stairwell_door"), SoundSource.BLOCKS, 1f, 1f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.6f));
            ExitType type = v == 1 ? ExitType.DOOR : v >= ROOF ? ExitType.ASCEND : ExitType.DESCEND;
            Travel.exit(p, type, pos);
            return;
        }
        level.playSound(null, pos, ModSounds.get("event.stairwell_locked"), SoundSource.BLOCKS, 1f, 0.9f + p.getRandom().nextFloat() * 0.2f);
        String[] msg = {"Locked.", "Locked.", "The bar won't move.", "Locked. It feels held from the other side.",
                "Locked. Not this floor."};
        p.displayClientMessage(Component.literal(msg[p.getRandom().nextInt(msg.length)]).withStyle(ChatFormatting.GRAY), true);
        if (p.getRandom().nextFloat() < 0.06f)
            level.playSound(null, pos, ModSounds.get("halluc.knock"), SoundSource.BLOCKS, 0.8f, 0.8f);
    }

    static final class Numbers implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.value = 8 + p.getRandom().nextInt(7);
            st.idx = idxAt(p.getY());
            st.cx = p.getBlockX() >> 4;
            st.cz = p.getBlockZ() >> 4;
            st.nextEvent = level.getGameTime() + 20 * 20;
            st.nextFigure = level.getGameTime() + 20 * 90;
            STATES.put(p.getUUID(), st);
            predict(level, st, p);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = STATES.get(p.getUUID());
            if (st == null) {
                onEnter(p, level, def);
                return;
            }
            // the eye goes all the way down
            if (p.fallDistance > 13f && !p.getAbilities().flying) {
                p.resetFallDistance();
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.8f));
                Travel.exit(p, ExitType.FALL, p.blockPosition());
                return;
            }
            // one stairwell: nobody ever reaches its top or bottom
            if (p.tickCount % 2 == 0) {
                if (p.getY() > 140) shift(p, level, st, -10);
                else if (p.getY() < 50) shift(p, level, st, 10);
            }
            int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
            if (cx != st.cx || cz != st.cz) {
                st.cx = cx;
                st.cz = cz;
                st.values.clear();
                st.idx = idxAt(p.getY());
                predict(level, st, p);
            }
            if (p.tickCount % 5 == 0 && p.onGround()) {
                int u = p.getBlockX() - (cx * 16 + OX);
                if (u >= 0 && u <= 2) {
                    int idx = idxAt(p.getY());
                    if (idx != st.idx) arrive(p, level, st, idx);
                }
            }
            long now = level.getGameTime();
            if (now >= st.nextEvent) {
                st.nextEvent = now + 20 * (25 + p.getRandom().nextInt(50));
                ambientEvent(p, level, st);
            }
            if (now >= st.nextFigure) {
                st.nextFigure = now + 20 * (80 + p.getRandom().nextInt(80));
                if (p.getRandom().nextFloat() < 0.45f) figure(p, level, st);
            }
        }

        void arrive(ServerPlayer p, ServerLevel level, St st, int idx) {
            int delta = idx - st.idx;
            int prev = st.value;
            Integer shown = st.values.get(idx);
            st.value = shown != null ? shown : st.value - delta;
            st.idx = idx;
            st.moves += Math.abs(delta);
            if (st.value == prev && delta != 0)
                level.playSound(null, p.blockPosition(), ModSounds.get("event.stairwell_loop"), SoundSource.AMBIENT, 0.5f, 1f);
            predict(level, st, p);
        }

        void shift(ServerPlayer p, ServerLevel level, St st, int floors) {
            p.connection.teleport(0, floors * H, 0, 0f, 0f, RelativeMovement.ALL);
            Map<Integer, Integer> moved = new HashMap<>();
            st.values.forEach((k, v) -> moved.put(k + floors, v));
            st.values.clear();
            st.values.putAll(moved);
            st.idx += floors;
            for (int j = -2; j <= 2; j++) {
                Integer v = st.values.get(st.idx + j);
                if (v != null) writeFloor(level, st, st.idx + j, v);
            }
        }

        void ambientEvent(ServerPlayer p, ServerLevel level, St st) {
            double cx = st.cx * 16 + OX + 4.5, cz = st.cz * 16 + OZ + 3.5;
            float r = p.getRandom().nextFloat();
            if (r < 0.5f) {
                // someone one or two floors above, always just out of sight
                double y = p.getY() + 8 + p.getRandom().nextInt(8);
                level.playSound(null, cx, y, cz, ModSounds.get("event.stairwell_steps"), SoundSource.AMBIENT, 1.2f, 0.95f + p.getRandom().nextFloat() * 0.1f);
            } else if (r < 0.8f) {
                double y = p.getY() - 12 - p.getRandom().nextInt(14);
                level.playSound(null, cx - 4, y, cz, ModSounds.get("event.stairwell_slam"), SoundSource.AMBIENT, 2.0f, 0.9f + p.getRandom().nextFloat() * 0.2f);
            } else {
                // the lights on the floor below give up for a moment
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.4f));
            }
        }

        /** Somebody on a landing far below, looking up the eye at you. */
        void figure(ServerPlayer p, ServerLevel level, St st) {
            int idx = st.idx - 3 - p.getRandom().nextInt(2);
            int yb = BASE + idx * H + 3;
            BlockPos at = new BlockPos(st.cx * 16 + OX + 7, yb + 1, st.cz * 16 + OZ + 3);
            if (yb < BASE || !level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) return;
            Mob m = ModEntities.SHADOW_FIGURE.create(level);
            if (m == null) return;
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90f, -60f);
            level.addFreshEntity(m);
        }
    }
}
