package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hotel Ballroom. Hall after hall laid for a wedding reception: round tables in white,
 * gold chairs, centrepieces burning, string lights, a band stage with the instruments
 * still out, a cake nobody has cut. The band plays a waltz for you, and only while
 * you dance: step off the dance floor and every bar is slower than the last. When the
 * music winds all the way down, the lights go out, and the guests arrive.
 */
public final class Ballroom {
    public static final String ID = "ballroom";
    static final int FLOOR = 10, CEIL = 18, C = 32;

    private Ballroom() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Hotel Ballroom").order(1210)
                .tagline("The reception is in full swing. Do keep dancing.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Band())
                .visuals(new Visuals().tint(1.1f, 0.96f, 0.84f).saturation(0.95f).contrast(1.08f).brightness(1.3f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.6f).bloom(0.85f).fog(5f, 52f).fogColor(0x2A1A14))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("partygoer"), 1, 0.03f).dark().dist(20, 36))
                .exit(ExitType.NOCLIP, "level_5", 2).exit(ExitType.NOCLIP, "level_fun", 1).exit(ExitType.NOCLIP, "lift_lobby", 1)
                .exit(ExitType.DOOR, "lift_lobby", 3).exit(ExitType.DOOR, "level_5", 2).exit(ExitType.DOOR, "bingo_hall", 1)
                .exit(ExitType.DOOR, "convention_center", 1).exit(ExitType.DOOR, "level_fun", 1)
                .build();
    }

    static boolean doorClosed(long s, int i, int k, int side) {
        return side == 0 ? Hash.chance(s, i, k, 0xD00, 0.25f) : Hash.chance(s, i, k, 0xD01, 0.5f);
    }

    static final class Gen implements LevelGenerator {
        final BlockState carpet = ModBlocks.state("ball_carpet");
        final BlockState dance = ModBlocks.state("ball_dance_floor");
        final BlockState wall = ModBlocks.state("ball_wall");
        final BlockState wallBase = ModBlocks.state("ball_wall_base");
        final BlockState drape = ModBlocks.state("ball_drape");
        final BlockState ceiling = ModBlocks.state("ball_ceiling");
        final BlockState table = ModBlocks.state("ball_table");
        final BlockState chair = ModBlocks.state("ball_chair");
        final BlockState centre = ModBlocks.state("ball_centerpiece");
        final BlockState cake = ModBlocks.state("ball_cake");
        final BlockState bulbs = ModBlocks.state("ball_bulbs");
        final BlockState chandelier = ModBlocks.state("ball_chandelier");
        final BlockState stage = ModBlocks.state("ball_stage");
        final BlockState drum = ModBlocks.state("ball_drum");
        final BlockState piano = ModBlocks.state("ball_piano");
        final BlockState mic = ModBlocks.state("ball_mic");
        final BlockState door = ModBlocks.state("ball_door");
        final BlockState exitDoor = ModBlocks.state("ball_exit_door");
        final BlockState backstage = ModBlocks.state("ball_backstage");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            int y = FLOOR + 1;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = Math.floorDiv(x, C), k = Math.floorDiv(z, C);
                    int lx = x - i * C, lz = z - k * C;
                    boolean df = lx >= 12 && lx <= 20 && lz >= 12 && lz <= 20;
                    w.set(x, FLOOR, z, df ? dance : carpet);
                    boolean wx = lx == 0, wz = lz == 0;
                    if (wx || wz) {
                        boolean doorway = (wx && lz >= 14 && lz <= 17) || (wz && lx >= 14 && lx <= 17);
                        int side = wx ? 0 : 1;
                        if (doorway && !(wx && wz)) {
                            if (doorClosed(s, i, k, side)) {
                                boolean exit = Hash.chance(s, i, k, 0xE0 + side, 0.18f) && (lx == 15 || lz == 15);
                                w.set(x, y, z, exit ? exitDoor : door);
                                w.set(x, y + 1, z, exit ? exitDoor : door);
                                w.set(x, y + 2, z, door);
                                for (int yy = y + 3; yy < CEIL; yy++) w.set(x, yy, z, wall);
                            } else {
                                for (int yy = y + 4; yy < CEIL; yy++) w.set(x, yy, z, wall);
                            }
                            continue;
                        }
                        int along = wx ? lz : lx;
                        boolean drapes = along % 6 == 3;
                        w.set(x, y, z, drapes ? drape : wallBase);
                        for (int yy = y + 1; yy < CEIL; yy++) w.set(x, yy, z, drapes ? drape : wall);
                    }
                }
            int i0 = Math.floorDiv(w.minX, C), i1 = Math.floorDiv(w.maxX, C);
            int k0 = Math.floorDiv(w.minZ, C), k1 = Math.floorDiv(w.maxZ, C);
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) hall(w, s, i, k, i * C, k * C);
        }

        void hall(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            int y = FLOOR + 1;
            long h = Hash.hash(s, i, k, 0xBA11);
            // stage, band and a curtain behind it
            for (int lx = 9; lx <= 23; lx++) {
                for (int lz = 2; lz <= 6; lz++) w.set(x0 + lx, y, z0 + lz, stage);
                for (int yy = y; yy < CEIL; yy++) w.set(x0 + lx, yy, z0 + 1, drape);
            }
            w.set(x0 + 12, y + 1, z0 + 4, drum.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            w.set(x0 + 20, y + 1, z0 + 4, piano.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            w.set(x0 + 16, y + 1, z0 + 5, mic);
            if (Hash.unit(h) < 0.25f) {
                w.set(x0 + 16, y + 1, z0 + 1, backstage);
                w.set(x0 + 16, y + 2, z0 + 1, backstage);
            }
            // tables round the dance floor
            for (int tx : new int[]{5, 10, 22, 27})
                for (int tz : new int[]{10, 16, 22, 27}) table(w, s, x0 + tx, z0 + tz, h);
            // the cake, on a little dais, uncut
            w.set(x0 + 16, y, z0 + 26, stage);
            w.set(x0 + 16, y + 1, z0 + 26, cake);
            // light: chandeliers and swags of bulbs
            for (int[] c : new int[][]{{16, 16}, {8, 8}, {24, 8}, {8, 24}, {24, 24}})
                w.set(x0 + c[0], CEIL - 1, z0 + c[1], chandelier);
            for (int lz : new int[]{10, 16, 22})
                for (int lx = 2; lx <= 30; lx++)
                    if (!Hash.chance(s, x0 + lx, z0 + lz, 0xB0, 0.05f)) w.set(x0 + lx, CEIL - 2, z0 + lz, bulbs);
        }

        void table(ChunkWriter w, long s, int x, int z, long h) {
            int y = FLOOR + 1;
            w.set(x, y, z, table);
            if (!Hash.chance(s, x, z, 0xCE, 0.1f)) w.set(x, y + 1, z, centre);
            Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction d : dirs) {
                int cx = x + d.getStepX(), cz = z + d.getStepZ();
                float r = Hash.unit(s, cx, cz, 0xC4A);
                if (r < 0.06f) continue;
                Direction face = r < 0.14f ? Direction.from2DDataValue(Hash.range(s, cx, cz, 0xC4B, 4)) : d.getOpposite();
                w.set(cx, y, cz, chair.setValue(HorizontalDirectionalBlock.FACING, face));
            }
        }
    }

    // ---------------------------------------------------------------- the band

    static final class St {
        float pitch = 1f;
        long nextClip;
        boolean danced;
        long darkUntil = -1;
        boolean hinted;
    }

    static final Map<UUID, St> STATES = new HashMap<>();
    static final Set<Long> CARDS = new HashSet<>();

    static final class Band implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = new St();
            st.nextClip = level.getGameTime() + 40;
            STATES.put(p.getUUID(), st);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            St st = STATES.computeIfAbsent(p.getUUID(), u -> new St());
            long now = level.getGameTime();
            if (p.tickCount % 5 == 0 && level.getBlockState(p.blockPosition().below()).is(ModBlocks.get("ball_dance_floor")))
                st.danced = true;
            if (st.darkUntil > 0) {
                if (now < st.darkUntil) return;
                st.darkUntil = -1;
                st.pitch = 0.95f;
                st.nextClip = now + 20;
            }
            if (now < st.nextClip) return;
            int i = Math.floorDiv(p.getBlockX(), C), k = Math.floorDiv(p.getBlockZ(), C);
            BlockPos micPos = new BlockPos(i * C + 16, FLOOR + 3, k * C + 5);
            st.pitch = st.danced ? Math.min(1f, st.pitch + 0.1f) : st.pitch - 0.075f;
            st.danced = false;
            if (st.pitch < 0.5f) {
                level.playSound(null, micPos, ModSounds.get("event.ballroom_stop"), SoundSource.RECORDS, 2f, 1f);
                p.sendSystemMessage(Component.literal("The music winds down to nothing. In the dark, chairs scrape back from every table.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                Blackouts.start(level, 20 * 25);
                st.darkUntil = now + 20 * 25;
                guests(p, level);
                return;
            }
            level.playSound(null, micPos, ModSounds.get("event.ballroom_waltz"), SoundSource.RECORDS, 2.2f, st.pitch);
            st.nextClip = now + (long) (12 * 20 / st.pitch) - 2;
            if (!st.hinted && st.pitch < 0.86f) {
                st.hinted = true;
                EmptyBuildingKit.say(p, "Bandleader", ChatFormatting.GOLD, "We can only keep playing if somebody dances.");
            }
            cards(p, level, i, k);
        }

        void guests(ServerPlayer p, ServerLevel level) {
            int made = 0;
            for (int attempt = 0; attempt < 20 && made < 3; attempt++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2;
                double d = 8 + p.getRandom().nextInt(10);
                BlockPos at = BlockPos.containing(p.getX() + Math.cos(ang) * d, FLOOR + 1, p.getZ() + Math.sin(ang) * d);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                Mob m = ModEntities.PARTYGOER.create(level);
                if (m == null) return;
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
                level.addFreshEntity(m);
                made++;
            }
        }

        /** A place card at the nearest table, with your name on it. */
        void cards(ServerPlayer p, ServerLevel level, int i, int k) {
            BlockPos t = new BlockPos(i * C + 22, FLOOR + 1, k * C + 22);
            if (!CARDS.add(t.asLong())) return;
            Component name = Component.literal(p.getGameProfile().getName().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal("\nTable 1 - Bride's side").withStyle(ChatFormatting.DARK_GRAY));
            EmptyBuildingKit.text(level, new Vec3(t.getX() + 0.5, t.getY() + 0.95, t.getZ() - 0.05), 180f, name, "fixed", 0.3f,
                    0xFFF8F4EA, 120, "ball_card");
        }
    }
}
