package com.noclip.levels;

import com.noclip.block.DaycareCubbyBlock;
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
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Daycare. Low bright rooms in primary colours, foam floor tiles, tiny chairs,
 * crayon drawings, nap mats in rows. Nobody is here. The toys are never where you
 * left them: whenever you are not looking they shuffle a little closer, and
 * sometimes there is a new one right behind you. At nap time the music box slows
 * and the lights go out, and the toys do not wait for you to look away. Every
 * cubby has a child's name over it. One of them has yours.
 */
public final class Daycare {
    public static final String ID = "daycare";
    static final int FLOOR = 10, CEIL = 14, C = 13;
    static final String[] KIDS = {"MIA", "TOM", "AVA", "LEO", "ELLA", "SAM", "NOAH", "ZOE", "JACK", "LILY", "BEN", "ROSE",
            "OWEN", "ISLA", "THEO", "?", "MAX", "EVIE"};

    private Daycare() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Daycare").order(1130)
                .tagline("Pick-up was at six. Nobody came.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Toys())
                .visuals(new Visuals().tint(1.04f, 1.0f, 0.94f).saturation(1.08f).contrast(1.04f).brightness(1.12f)
                        .grain(0.05f).aberration(0.001f).vignette(0.42f).bloom(0.45f).fog(6f, 44f).fogColor(0xE8DCC0))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.04f).seen().dist(20, 36))
                .exit(ExitType.NOCLIP, "level_fun", 1).exit(ExitType.NOCLIP, "playground", 2).exit(ExitType.NOCLIP, "school", 2)
                .exit(ExitType.NOCLIP, "dollhouse", 1)
                .exit(ExitType.DOOR, "playground", 2).exit(ExitType.DOOR, "school", 2).exit(ExitType.DOOR, "foggy_playground", 1)
                .exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.SECRET, "dollhouse", 2).exit(ExitType.SECRET, "frontrooms", 1).exit(ExitType.SECRET, "level_0", 1)
                .build();
    }

    enum Room { CLASS, NAP, PLAY, CUBBIES, EXIT }

    static Room room(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0xDC);
        if (r < 0.03f) return Room.EXIT;
        if (r < 0.15f) return Room.CUBBIES;
        if (r < 0.35f) return Room.NAP;
        if (r < 0.62f) return Room.PLAY;
        return Room.CLASS;
    }

    static boolean hasCubbies(Room r) {
        return r == Room.CLASS || r == Room.CUBBIES;
    }

    static final class Gen implements LevelGenerator {
        final String[] colours = {"red", "yellow", "blue", "green"};
        final BlockState ceiling = ModBlocks.state("dc_ceiling");
        final BlockState light = ModBlocks.state("dc_light");
        final BlockState flicker = ModBlocks.state("dc_light_flicker");
        final BlockState frieze = ModBlocks.state("dc_frieze");
        final BlockState wrong = ModBlocks.state("dc_frieze_wrong");
        final BlockState cubby = ModBlocks.state("dc_cubby");
        final BlockState table = ModBlocks.state("dc_table");
        final BlockState[] chairs = {ModBlocks.state("dc_chair_red"), ModBlocks.state("dc_chair_blue")};
        final BlockState[] mats = {ModBlocks.state("dc_mat_red"), ModBlocks.state("dc_mat_blue"), ModBlocks.state("dc_mat_green")};
        final BlockState[] drawings = {ModBlocks.state("dc_drawing_sun"), ModBlocks.state("dc_drawing_fam"),
                ModBlocks.state("dc_drawing_tall"), ModBlocks.state("dc_drawing_door")};
        final BlockState door = ModBlocks.state("dc_fire_door");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = Math.floorDiv(x, C), k = Math.floorDiv(z, C);
                    int lx = x - i * C, lz = z - k * C;
                    long rh = Hash.hash(s, i, k, 0xC01);
                    w.set(x, FLOOR, z, ModBlocks.state((rh & 1) == 0 ? "dc_foam_ry" : "dc_foam_bg"));
                    boolean wallX = lx == 0 && !(lz == 5 || lz == 6);
                    boolean wallZ = lz == 0 && !((lx == 10 || lx == 11) && Hash.chance(s, i, k, 0xD0, 0.65f));
                    if (wallX || wallZ) {
                        BlockState paint = ModBlocks.state("dc_wall_" + colours[Hash.range(rh >>> 4, 4)]);
                        w.set(x, FLOOR + 1, z, paint);
                        w.set(x, FLOOR + 2, z, ModBlocks.state("dc_wall_plain"));
                        w.set(x, FLOOR + 3, z, Hash.chance(s, x, z, 0xF1, 0.03f) ? wrong : frieze);
                    } else if (lx % 4 == 2 && lz % 4 == 2) {
                        w.set(x, CEIL, z, Hash.chance(s, x, z, 0x11, 0.08f) ? flicker : light);
                    }
                }
            int i0 = Math.floorDiv(w.minX, C), i1 = Math.floorDiv(w.maxX, C);
            int k0 = Math.floorDiv(w.minZ, C), k1 = Math.floorDiv(w.maxZ, C);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) room(w, s, i, k, i * C, k * C);
        }

        void room(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            Room type = Daycare.room(s, i, k);
            long h = Hash.hash(s, i, k, 0xD4);
            int y = FLOOR + 1;
            if (hasCubbies(type)) {
                for (int lx = 2; lx <= 9; lx++) {
                    for (int dy = 0; dy < 2; dy++) {
                        int coat = Hash.range(s, x0 + lx, z0 + dy, 0xC0A7, 5);
                        w.set(x0 + lx, y + dy, z0, cubby.setValue(DaycareCubbyBlock.COAT, coat > 2 ? 0 : coat));
                    }
                }
            }
            // drawings taped to the west wall, low down
            for (int lz : new int[]{2, 9}) {
                if (Hash.chance(s, x0, z0 + lz, 0xD7, 0.7f))
                    w.set(x0 + 1, y + 1, z0 + lz, drawings[Hash.range(s, x0, z0 + lz, 0xD8, 4)]
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            }
            switch (type) {
                case CLASS, CUBBIES -> {
                    if (type == Room.CUBBIES) break;
                    for (int[] t : new int[][]{{3, 4}, {7, 4}, {3, 8}, {7, 8}}) {
                        w.set(x0 + t[0], y, z0 + t[1], table);
                        w.set(x0 + t[0] + 1, y, z0 + t[1], table);
                        for (int c = 0; c < 2; c++) {
                            BlockState ch = chairs[(int) ((h >>> (t[0] + t[1] + c)) & 1)];
                            if (Hash.chance(s, x0 + t[0] + c, z0 + t[1], 0xC4, 0.85f))
                                w.set(x0 + t[0] + c, y, z0 + t[1] - 1, ch.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                            if (Hash.chance(s, x0 + t[0] + c, z0 + t[1], 0xC5, 0.85f))
                                w.set(x0 + t[0] + c, y, z0 + t[1] + 1, ch.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        }
                    }
                }
                case NAP -> {
                    for (int lx = 2; lx <= 10; lx += 2)
                        for (int lz = 3; lz <= 9; lz += 3) {
                            w.set(x0 + lx, y, z0 + lz, mats[Hash.range(s, x0 + lx, z0 + lz, 0x3A, 3)]);
                            if (Hash.chance(s, x0 + lx, z0 + lz, 0x3B, 0.12f)) {
                                // the mat is taken by something that is not a child
                                w.set(x0 + lx, y, z0 + lz + 1, ModBlocks.state("dc_toy_bear")
                                        .setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x0 + lx, z0 + lz, 0x3C, 4))));
                            }
                        }
                }
                case PLAY -> {
                    String[] toys = {"dc_toy_a", "dc_toy_b", "dc_toy_c", "dc_toy_u", "dc_toy_bear", "dc_toy_ball"};
                    int n = 3 + Hash.range(h, 5);
                    for (int t = 0; t < n; t++) {
                        long th = Hash.hash(h, t);
                        w.set(x0 + 2 + Hash.range(th, 9), y, z0 + 2 + Hash.range(th >>> 8, 9),
                                ModBlocks.state(toys[Hash.range(th >>> 16, toys.length)])
                                        .setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(th >>> 20, 4))));
                    }
                }
                case EXIT -> {
                    w.set(x0 + 4, y, z0, door);
                    w.set(x0 + 4, y + 1, z0, door);
                    w.set(x0 + 4, y + 2, z0 + 1, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the toys

    static final Map<UUID, Long> NEXT_NEW = new HashMap<>();
    static final Map<UUID, Long> CROWD = new HashMap<>();
    static final Map<Long, Long> LABELS = new HashMap<>();
    static long napUntil = -1, nextNap = -1;

    static boolean isToy(BlockState s) {
        String n = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return n.startsWith("dc_toy_");
    }

    static boolean yours(BlockPos pos) {
        return Hash.unit(pos.asLong(), 0xC0B, 0) < 0.13f;
    }

    public static void onCubby(ServerPlayer p, BlockPos pos) {
        if (!p.level().dimension().location().getPath().equals(ID)) return;
        pos = pos.getY() == FLOOR + 2 ? pos.below() : pos;
        if (yours(pos)) {
            EmptyBuildingKit.narrate(p, "Inside, folded small: a coat that fits you, and a crayon drawing of a door. Your name is on the back, in your handwriting.");
            p.serverLevel().playSound(null, pos, ModSounds.get("event.daycare_giggle"), SoundSource.AMBIENT, 1f, 1f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.8f));
            Travel.exit(p, ExitType.SECRET, pos);
            return;
        }
        String[] inside = {"A lunchbox. Still cold.", "A drawing of a tall man with no face.", "Nothing. Somebody has scratched the inside.",
                "A single shoe, very small.", "A drawing of this room, with you in it.", "A juice box. Almond flavour."};
        p.displayClientMessage(Component.literal(inside[Hash.range(pos.asLong(), 0x1A, 0, inside.length)]).withStyle(ChatFormatting.GRAY), true);
    }

    static final class Toys implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            NEXT_NEW.put(p.getUUID(), level.getGameTime() + 20 * 40);
            if (nextNap < 0) nextNap = level.getGameTime() + 20 * 150;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            boolean nap = now < napUntil;
            if (p.tickCount % (nap ? 10 : 30) == 0) shuffle(p, level, nap);
            Long nn = NEXT_NEW.get(p.getUUID());
            if (nn != null && now >= nn) {
                NEXT_NEW.put(p.getUUID(), now + 20 * (40 + p.getRandom().nextInt(50)));
                behind(p, level);
            }
            if (p.tickCount % 40 == 0) labels(p, level);
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (nextNap > 0 && now >= nextNap) {
                nextNap = now + 20 * (180 + level.random.nextInt(120));
                napUntil = now + 20 * 18;
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.daycare_nap"), SoundSource.AMBIENT, 1f, 1f);
                    EmptyBuildingKit.narrate(p, "It's nap time.");
                }
                Blackouts.start(level, 20 * 18);
            }
            if (now % 100 == 0) {
                for (Iterator<Map.Entry<Long, Long>> it = LABELS.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Long, Long> e = it.next();
                    if (now - e.getValue() < 20 * 20) continue;
                    BlockPos bp = BlockPos.of(e.getKey());
                    boolean near = false;
                    for (Player pl : level.players()) if (pl.blockPosition().distSqr(bp) < 28 * 28) near = true;
                    if (near) continue;
                    if (level.isLoaded(bp)) EmptyBuildingKit.clearDisplays(level, Vec3.atCenterOf(bp), 2, "dc_" + bp.asLong());
                    it.remove();
                }
            }
        }

        /** Unseen toys creep one step closer. */
        void shuffle(ServerPlayer p, ServerLevel level, boolean nap) {
            BlockPos c = p.blockPosition();
            List<BlockPos> toys = new ArrayList<>();
            int crowd = 0;
            for (int dx = -14; dx <= 14; dx++)
                for (int dz = -14; dz <= 14; dz++) {
                    BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 1, c.getZ() + dz);
                    if (!isToy(level.getBlockState(bp))) continue;
                    if (dx * dx + dz * dz <= 9) crowd++;
                    else toys.add(bp);
                }
            if (crowd >= 4) {
                Long last = CROWD.get(p.getUUID());
                if (last == null || level.getGameTime() - last > 20 * 30) {
                    CROWD.put(p.getUUID(), level.getGameTime());
                    level.playSound(null, c, ModSounds.get("event.daycare_giggle"), SoundSource.AMBIENT, 1f, 1f);
                    Sanity.add(p, -6f);
                    EmptyBuildingKit.narrate(p, "They want you to play.");
                }
            }
            if (toys.isEmpty()) return;
            int moves = nap ? 3 : 1;
            for (int m = 0; m < moves && !toys.isEmpty(); m++) {
                BlockPos from = toys.remove(p.getRandom().nextInt(toys.size()));
                if (!nap && EmptyBuildingKit.seen(p, Vec3.atCenterOf(from), 0.1)) continue;
                int sx = Integer.signum(c.getX() - from.getX()), sz = Integer.signum(c.getZ() - from.getZ());
                BlockPos to = Math.abs(c.getX() - from.getX()) > Math.abs(c.getZ() - from.getZ()) ? from.offset(sx, 0, 0) : from.offset(0, 0, sz);
                if (!level.getBlockState(to).isAir() || !level.getBlockState(to.below()).isFaceSturdy(level, to.below(), Direction.UP)) continue;
                if (!nap && EmptyBuildingKit.seen(p, Vec3.atCenterOf(to), 0.1)) continue;
                BlockState st = level.getBlockState(from);
                Direction face = Direction.getNearest(c.getX() - to.getX(), 0, c.getZ() - to.getZ());
                if (face.getAxis().isVertical()) face = Direction.NORTH;
                level.setBlock(from, ModBlocks.AIR, Block.UPDATE_ALL);
                level.setBlock(to, st.setValue(HorizontalDirectionalBlock.FACING, face), Block.UPDATE_ALL);
                if (p.getRandom().nextFloat() < 0.2f)
                    level.playSound(null, to, ModSounds.get("event.daycare_toy"), SoundSource.AMBIENT, 0.4f, 1f + p.getRandom().nextFloat() * 0.3f);
            }
        }

        /** A toy that was not there before, just behind you. */
        void behind(ServerPlayer p, ServerLevel level) {
            String[] toys = {"dc_toy_a", "dc_toy_b", "dc_toy_c", "dc_toy_u", "dc_toy_bear", "dc_toy_ball"};
            for (int attempt = 0; attempt < 8; attempt++) {
                double ang = Math.toRadians(p.getYRot() + 180 + (p.getRandom().nextFloat() - 0.5f) * 90);
                double d = 2.5 + p.getRandom().nextFloat() * 2;
                BlockPos bp = BlockPos.containing(p.getX() - Math.sin(ang) * d, FLOOR + 1, p.getZ() + Math.cos(ang) * d);
                if (!level.getBlockState(bp).isAir() || EmptyBuildingKit.seen(p, Vec3.atCenterOf(bp), 0.1)) continue;
                if (!level.getBlockState(bp.below()).isFaceSturdy(level, bp.below(), Direction.UP)) continue;
                Direction face = Direction.getNearest(p.getX() - bp.getX(), 0, p.getZ() - bp.getZ());
                if (face.getAxis().isVertical()) face = Direction.NORTH;
                level.setBlock(bp, ModBlocks.state(toys[p.getRandom().nextInt(toys.length)]).setValue(HorizontalDirectionalBlock.FACING, face), Block.UPDATE_ALL);
                return;
            }
        }

        /** Names over the cubbies near you. One of them is yours. */
        void labels(ServerPlayer p, ServerLevel level) {
            BlockPos c = p.blockPosition();
            long now = level.getGameTime();
            String me = p.getGameProfile().getName().toUpperCase(Locale.ROOT);
            for (int dx = -8; dx <= 8; dx++)
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 2, c.getZ() + dz);
                    if (!(level.getBlockState(bp).getBlock() instanceof DaycareCubbyBlock)) continue;
                    BlockPos low = bp.below();
                    Long prev = LABELS.put(bp.asLong(), now);
                    if (prev != null) continue;
                    String name = yours(low) ? me : KIDS[Hash.range(bp.asLong(), 0x4A, 0, KIDS.length)];
                    Component text = Component.literal(name).withStyle(yours(low) ? ChatFormatting.DARK_RED : ChatFormatting.DARK_BLUE, ChatFormatting.BOLD);
                    EmptyBuildingKit.text(level, new Vec3(bp.getX() + 0.5, bp.getY() + 0.62, bp.getZ() + 1.03), 0f, text, "fixed",
                            0.5f, 0xFFF4ECDA, 60, "dc_" + bp.asLong());
                }
        }
    }
}
