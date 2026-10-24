package com.noclip.levels;

import com.noclip.entity.RunChaser;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Level !, "Run For Your Life". An endless red corridor under a screaming alarm.
 * Something is behind you from the moment you arrive. It is slower than your
 * sprint and faster than your walk; it goes through anything in its way. The only
 * way out is forward: doorways full of white light, a few hundred metres apart.
 */
public final class LevelRun {
    static final int F = 8, CEIL = F + 5;
    static final String ID = "level_run";

    private LevelRun() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("!").title("Run For Your Life").order(910)
                .tagline("RUN. Don't stop. Don't look back. The only way out is forward.")
                .danger(BackroomsLevel.Danger.DEADLY)
                .generator(Gen::new)
                .behavior(new Chase())
                .visuals(new Visuals().tint(1.32f, 0.62f, 0.58f).saturation(0.9f).contrast(1.12f).brightness(1.3f)
                        .grain(0.11f).aberration(0.0035f).vignette(0.68f).warp(0.03f).flicker(0.06f).bloom(0.7f)
                        .fog(4f, 44f).fogColor(0x2A0606))
                .sanityDrain(0.06f)
                .spawnY(F + 1, F + 1)
                .exit(ExitType.DOOR, "level_0", 3).exit(ExitType.DOOR, "level_1", 1).exit(ExitType.DOOR, "level_37", 1)
                .exit(ExitType.FALL, "level_6", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Seg { CLEAR, RUBBLE, DESK, BARRICADE, PIT, NARROW, COLLAPSE, GURNEY, DOORS, EXIT }

    static Seg seg(long s, int sx, int m) {
        if (Hash.unit(s, sx, m, 50) < 0.06f) return Seg.EXIT;
        float r = Hash.unit(s, sx, m, 51);
        if (r < 0.34f) return Seg.CLEAR;
        if (r < 0.46f) return Seg.RUBBLE;
        if (r < 0.56f) return Seg.DESK;
        if (r < 0.64f) return Seg.BARRICADE;
        if (r < 0.70f) return Seg.PIT;
        if (r < 0.78f) return Seg.NARROW;
        if (r < 0.85f) return Seg.COLLAPSE;
        if (r < 0.91f) return Seg.GURNEY;
        return Seg.DOORS;
    }

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("run_corridor_wall");
        final BlockState bloody = ModBlocks.state("run_corridor_wall_bloody");
        final BlockState scratched = ModBlocks.state("run_corridor_wall_scratched");
        final BlockState run = ModBlocks.state("run_corridor_wall_run");
        final BlockState dont = ModBlocks.state("run_corridor_wall_dont");
        final BlockState stop = ModBlocks.state("run_corridor_wall_stop");
        final BlockState arrow = ModBlocks.state("run_corridor_wall_arrow");
        final BlockState base = ModBlocks.state("run_corridor_baseboard");
        final BlockState floor = ModBlocks.state("run_corridor_floor");
        final BlockState drag = ModBlocks.state("run_corridor_floor_drag");
        final BlockState ceiling = ModBlocks.state("run_corridor_ceiling");
        final BlockState light = ModBlocks.state("run_emergency_light");
        final BlockState strobe = ModBlocks.state("run_alarm_strobe");
        final BlockState exitLight = ModBlocks.state("run_exit_light");
        final BlockState exitSign = ModBlocks.state("run_exit_sign");
        final BlockState alarm = ModBlocks.state("run_fire_alarm");
        final BlockState rubble = ModBlocks.state("run_rubble");
        final BlockState barricade = ModBlocks.state("run_barricade");
        final BlockState desk = ModBlocks.state("run_overturned_desk");
        final BlockState gurney = ModBlocks.state("run_gurney");
        final BlockState wires = ModBlocks.state("run_hanging_wires");
        final BlockState door = ModBlocks.state("run_locked_door");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            int sx = Math.floorDiv(w.minX, 16);
            for (int z = w.minZ; z <= w.maxZ; z++) {
                int zl = Math.floorMod(z, 16), m = Math.floorDiv(z, 16);
                Seg sg = seg(s, sx, m);
                boolean nextExit = seg(s, sx + 1, m) == Seg.EXIT;
                for (int x = w.minX; x <= w.maxX; x++) {
                    int xl = x - w.minX;
                    if (zl > 6) {
                        w.fill(x, F, z, x, CEIL, z, wall);
                        continue;
                    }
                    w.set(x, CEIL, z, ceiling);
                    if (zl == 0 || zl == 6) {
                        w.set(x, F, z, floor);
                        w.set(x, F + 1, z, base);
                        for (int y = F + 2; y < CEIL; y++) w.set(x, y, z, wallBlock(s, x, y, z, sx, m, nextExit));
                        continue;
                    }
                    boolean dragged = (sg == Seg.GURNEY || Hash.unit(s, sx, m, 52) < 0.2f) && zl == 3;
                    w.set(x, F, z, dragged ? drag : floor);
                    if (zl == 3 && Math.floorMod(x, 4) == 0) {
                        boolean dead = Hash.chance(s, x, m, 53, 0.12f) || (sg == Seg.COLLAPSE && Math.abs(xl - 8) < 4);
                        BlockState l = Math.floorMod(x, 24) == 0 ? strobe : light;
                        w.set(x, CEIL, z, dead ? l.setValue(BlockStateProperties.LIT, false) : l);
                    }
                }
            }
            for (int m = Math.floorDiv(w.minZ, 16); m <= Math.floorDiv(w.maxZ, 16); m++) features(w, s, sx, m);
        }

        BlockState wallBlock(long s, int x, int y, int z, int sx, int m, boolean nextExit) {
            if (nextExit && y == F + 2 && Math.floorMod(x, 4) == 0) return arrow;
            float r = Hash.unit(s, x, y * 64 + z, 60);
            if (r < 0.010f && y == F + 2) return run;
            if (r < 0.014f && y == F + 3) return dont;
            if (r < 0.018f && y == F + 2) return stop;
            if (r < 0.06f) return bloody;
            if (r < 0.09f) return scratched;
            return wall;
        }

        void features(ChunkWriter w, long s, int sx, int m) {
            int x0 = sx * 16, z0 = m * 16;
            Seg sg = seg(s, sx, m);
            long h = Hash.hash(s, sx, m, 70);
            boolean north = (h & 1) == 0;
            // a fire alarm somebody already pulled
            if (Hash.unit(h >>> 3) < 0.35f) {
                if (north) w.set(x0 + 3, F + 2, z0 + 1, face(alarm, Direction.SOUTH));
                else w.set(x0 + 3, F + 2, z0 + 5, face(alarm, Direction.NORTH));
            }
            switch (sg) {
                case RUBBLE -> {
                    int za = north ? 1 : 3;
                    for (int dx = 7; dx <= 9; dx++)
                        for (int dz = 0; dz < 3; dz++) w.set(x0 + dx, F + 1, z0 + za + dz, face(rubble, Direction.from2DDataValue(Hash.range(h, dx, dz, 4))));
                }
                case DESK -> {
                    for (int dz = 2; dz <= 4; dz++) w.set(x0 + 8, F + 1, z0 + dz, face(desk, Direction.EAST));
                }
                case BARRICADE -> {
                    for (int dz = 1; dz <= 5; dz++) w.set(x0 + 8, F + 1, z0 + dz, face(barricade, Direction.EAST));
                }
                case PIT -> {
                    for (int dx = 7; dx <= 8; dx++)
                        for (int dz = 1; dz <= 5; dz++) {
                            w.fill(x0 + dx, F - 3, z0 + dz, x0 + dx, F - 2, z0 + dz, pit);
                            w.fill(x0 + dx, F - 1, z0 + dz, x0 + dx, F, z0 + dz, ModBlocks.AIR);
                        }
                }
                case NARROW -> {
                    int za = north ? 1 : 4;
                    for (int dx = 4; dx <= 12; dx++)
                        for (int dz = 0; dz < 2; dz++)
                            for (int y = F + 1; y < CEIL; y++)
                                w.set(x0 + dx, y, z0 + za + dz, y == F + 2 && dx % 4 == 0 ? arrow : y == F + 1 ? base : wall);
                }
                case COLLAPSE -> {
                    int za = north ? 1 : 4;
                    for (int dx = 6; dx <= 10; dx++)
                        for (int dz = 0; dz < 2; dz++) w.set(x0 + dx, F + 1, z0 + za + dz, face(rubble, Direction.from2DDataValue(Hash.range(h, dx, dz, 4))));
                    for (int dx = 5; dx <= 11; dx += 2) w.set(x0 + dx, CEIL - 1, z0 + 2 + Hash.range(h, dx, 9, 3), wires);
                    w.set(x0 + 8, CEIL, z0 + 3, filler);
                    w.set(x0 + 9, CEIL, z0 + 2, filler);
                }
                case GURNEY -> {
                    w.set(x0 + 6, F + 1, z0 + (north ? 2 : 4), face(gurney, Direction.NORTH));
                    w.set(x0 + 7, F + 1, z0 + (north ? 2 : 4), face(gurney, Direction.NORTH));
                }
                case DOORS -> {
                    for (int dx = 4; dx <= 12; dx += 4) {
                        int wz = (dx / 4) % 2 == 0 ? z0 : z0 + 6;
                        w.set(x0 + dx, F + 1, wz, door);
                        w.set(x0 + dx, F + 2, wz, door);
                    }
                }
                case EXIT -> {
                    // a doorway in the south wall, full of white light. Run into it.
                    for (int dx = 7; dx <= 8; dx++) {
                        for (int y = F + 1; y <= F + 3; y++) w.set(x0 + dx, y, z0 + 6, exitLight);
                        w.set(x0 + dx, F, z0 + 6, floor);
                    }
                    w.set(x0 + 5, CEIL - 1, z0 + 3, face(exitSign, Direction.EAST));
                    w.set(x0 + 9, F + 1, z0 + 6, base);
                }
                default -> {
                }
            }
        }
    }

    // ---------------------------------------------------------------- the chase

    static final class Chase implements LevelBehavior {
        private final Map<UUID, Double> startX = new HashMap<>();
        private final Map<UUID, Integer> chaserId = new HashMap<>();
        private final Map<UUID, Integer> still = new HashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            // always face down the corridor
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 0f);
            startX.put(p.getUUID(), p.getX());
            still.put(p.getUUID(), 0);
            spawnChaser(p, level, 24);
            DeepEndUtil.title(p, Component.literal("RUN").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("Don't stop. Don't look back.").withStyle(ChatFormatting.RED), 0, 40, 20);
            level.playSound(null, p.blockPosition(), ModSounds.get("event.level_run_start"), SoundSource.HOSTILE, 1.2f, 1f);
        }

        void spawnChaser(ServerPlayer p, ServerLevel level, double behind) {
            Integer old = chaserId.get(p.getUUID());
            if (old != null) {
                Entity e = level.getEntity(old);
                if (e != null) e.discard();
            }
            RunChaser c = RunChaser.TYPE.create(level);
            if (c == null) return;
            c.moveTo(p.getX() - behind, p.getY(), p.getZ(), -90f, 0f);
            c.hunt(p);
            level.addFreshEntity(c);
            chaserId.put(p.getUUID(), c.getId());
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator() || p.isDeadOrDying()) return;
            UUID id = p.getUUID();
            Integer cid = chaserId.get(id);
            Entity c = cid != null ? level.getEntity(cid) : null;
            if (c == null || !c.isAlive()) {
                spawnChaser(p, level, 30);
                return;
            }
            double sx = startX.computeIfAbsent(id, u -> p.getX());
            double run = p.getX() - sx;
            double gap = Math.sqrt(Math.pow(c.getX() - p.getX(), 2) + Math.pow(c.getZ() - p.getZ(), 2));

            if (p.tickCount % 5 == 0) {
                boolean close = gap < 10;
                boolean blink = close && (p.tickCount / 5) % 2 == 0;
                MutableComponent bar = Component.literal(">> " + Math.max(0, (int) run) + " m   ").withStyle(ChatFormatting.WHITE)
                        .append(Component.literal("IT IS " + (int) gap + " m BEHIND YOU")
                                .withStyle(blink ? ChatFormatting.WHITE : close ? ChatFormatting.RED : ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                DeepEndUtil.actionbar(p, bar);
            }
            if (p.isCreative()) return;
            // adrenaline: you will not run out of breath here. Only out of corridor.
            if (p.tickCount % 40 == 0 && p.getFoodData().getFoodLevel() < 8) p.getFoodData().setFoodLevel(8);
            double speed = p.getDeltaMovement().horizontalDistance();
            int st = still.getOrDefault(id, 0);
            st = speed < 0.08 ? st + 1 : 0;
            still.put(id, st);
            if (st == 30) {
                p.sendSystemMessage(Component.literal("Why did you stop?").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
                Sanity.add(p, -4f);
            }
            if (p.tickCount % 100 == 0 && run < -8) {
                p.sendSystemMessage(Component.literal("Wrong way.").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}
