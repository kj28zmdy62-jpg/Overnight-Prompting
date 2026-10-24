package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModItems;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.Sanity;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Level Fun =). A birthday party that has been going on forever in pastel rooms
 * full of balloons, cake and streamers. The Partygoers are delighted you came.
 * Every balloon you pop tells them where you are. Every present might be a
 * surprise. And every few minutes the lights go out, and everyone yells.
 */
public final class LevelFun {
    static final int F = 10, CEIL = F + 4;
    static final String ID = "level_fun";
    static final String[] PAPERS = {"pink", "mint", "blue", "lemon"};
    static final String[] BALLOONS = {"red", "blue", "yellow", "pink", "green"};

    private LevelFun() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("Fun =)").title("Fun =)").order(900)
                .tagline("Hello friend =) Are you here for the party? =)")
                .danger(BackroomsLevel.Danger.DEADLY)
                .generator(Gen::new)
                .behavior(new Party())
                .visuals(new Visuals().tint(1.06f, 0.99f, 1.02f).saturation(1.12f).contrast(1.06f).brightness(1.25f)
                        .grain(0.06f).aberration(0.0018f).vignette(0.42f).bloom(0.6f).flicker(0.01f)
                        .fog(5f, 46f).fogColor(0xF2D8E0))
                .sanityDrain(0.05f)
                .spawnY(F + 1, F + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("partygoer"), 3, 0.3f).dist(16, 34).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("partygoer"), 5, 0.12f).dist(10, 20).weight(1))
                .exit(ExitType.DOOR, "level_0", 3).exit(ExitType.DOOR, "level_run", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_run", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    enum Room { PARTY, EMPTY, BALLOONS, PRESENTS, BANQUET, QUIET, EXIT }

    static Room room(CellGrid g, int i, int k) {
        float r = g.unit(i, k, 1);
        if (r < 0.34f) return Room.PARTY;
        if (r < 0.62f) return Room.EMPTY;
        if (r < 0.74f) return Room.BALLOONS;
        if (r < 0.82f) return Room.PRESENTS;
        if (r < 0.90f) return Room.BANQUET;
        if (r < 0.95f) return Room.QUIET;
        if (r < 0.975f) return Room.EXIT;
        return Room.PARTY;
    }

    static final class Gen implements LevelGenerator {
        final BlockState[] paper = new BlockState[4];
        final BlockState[] scrawl = new BlockState[7];
        final BlockState base = ModBlocks.state("party_baseboard");
        final BlockState carpet = ModBlocks.state("party_carpet");
        final BlockState stained = ModBlocks.state("party_carpet_stained");
        final BlockState confetti = ModBlocks.state("party_confetti");
        final BlockState ceiling = ModBlocks.state("party_ceiling");
        final BlockState light = ModBlocks.state("party_light");
        final BlockState flicker = ModBlocks.state("party_light_flicker");
        final BlockState streamA = ModBlocks.state("party_streamers_a");
        final BlockState streamB = ModBlocks.state("party_streamers_b");
        final BlockState bunting = ModBlocks.state("party_bunting");
        final BlockState table = ModBlocks.state("party_table");
        final BlockState cake = ModBlocks.state("party_cake");
        final BlockState hat = ModBlocks.state("party_hat");
        final BlockState presentA = ModBlocks.state("party_present_a");
        final BlockState presentB = ModBlocks.state("party_present_b");
        final BlockState door = ModBlocks.state("party_exit_door");
        final BlockState doorTop = ModBlocks.state("party_exit_door_top");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] low = new BlockState[5], high = new BlockState[5];

        Gen() {
            for (int i = 0; i < 4; i++) paper[i] = ModBlocks.state("party_wallpaper_" + PAPERS[i]);
            for (int i = 0; i < 7; i++) scrawl[i] = ModBlocks.state("party_wallpaper_scrawl_" + i);
            for (int i = 0; i < 5; i++) {
                low[i] = ModBlocks.state("party_balloon_" + BALLOONS[i]);
                high[i] = ModBlocks.state("party_balloon_" + BALLOONS[i] + "_high");
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = new CellGrid(s, 12, 4);
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int i = g.cellX(x), k = g.cellZ(z);
                    int u = x - g.x0(i), v = z - g.z0(k);
                    Room room = room(g, i, k);
                    boolean wet = Hash.fbm(s ^ 0x9A9, x / 10.0, z / 10.0, 2) > 0.66f;
                    w.set(x, F, z, wet ? stained : carpet);
                    if (u == 0 || v == 0) {
                        wall(w, g, s, x, z, i, k, u, v);
                        w.set(x, CEIL, z, ceiling);
                        continue;
                    }
                    boolean lit = u % 3 == 2 && v % 3 == 2;
                    if (lit) {
                        boolean dead = room == Room.QUIET || Hash.chance(s, x, z, 5, 0.04f);
                        w.set(x, CEIL, z, dead ? ModBlocks.state("party_light_flicker").setValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false)
                                : Hash.chance(s, x, z, 6, 0.08f) ? flicker : light);
                    } else {
                        w.set(x, CEIL, z, ceiling);
                    }
                    if (room != Room.QUIET && Hash.chance(s, x, z, 7, 0.18f)) w.set(x, F + 1, z, confetti);
                    // crepe streamers sag from the ceiling in long diagonal runs
                    if (room != Room.QUIET && Math.floorMod(x + z * 2 + (int) (g.hash(i, k, 3) & 7), 7) == 0
                            && Hash.chance(s, x, z, 8, 0.45f) && !lit) {
                        w.set(x, CEIL - 1, z, (x & 1) == 0 ? streamA : streamB);
                    }
                }
            }
            g.forCells(w, (i, k, x0, z0, wd, dp) -> furnish(w, g, s, i, k, x0, z0, wd, dp));
        }

        void wall(ChunkWriter w, CellGrid g, long s, int x, int z, int i, int k, int u, int v) {
            BlockState p = paper[(int) Math.floorMod(g.hash(i, k, 2), 4)];
            for (int y = F + 1; y < CEIL; y++) {
                BlockState b = y == F + 1 ? base : p;
                if (y == F + 2 && Hash.chance(s, x, z, 11, 0.05f)) b = scrawl[Hash.range(s, x, z, 12, 7)];
                w.set(x, y, z, b);
            }
            if (u == 0 && v == 0) return;
            // one doorway per wall, sometimes an open archway
            boolean alongX = u == 0;
            int len = alongX ? g.depth(k) : g.width(i);
            int along = alongX ? v : u;
            long h = g.hash(i, k, alongX ? 20 : 21);
            if (Hash.unit(h >>> 30) < 0.1f) return;
            boolean arch = Hash.unit(h >>> 40) < 0.2f;
            int width = arch ? Math.min(3, len - 2) : 1 + (int) ((h >>> 50) & 1);
            int start = 1 + Hash.range(h, Math.max(1, len - width - 1));
            if (along < start || along >= start + width) return;
            int top = arch ? CEIL - 1 : F + 2;
            for (int y = F + 1; y <= top; y++) w.set(x, y, z, ModBlocks.AIR);
            // a balloon tied in the doorway, at face height. Mind it.
            if (!arch && Hash.unit(h >>> 20) < 0.22f && along == start) {
                w.set(x, F + 2, z, low[Hash.range(h >>> 12, 5)]);
            }
        }

        BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void furnish(ChunkWriter w, CellGrid g, long s, int i, int k, int x0, int z0, int wd, int dp) {
            Room room = room(g, i, k);
            long h = g.hash(i, k, 77);
            int cx = x0 + wd / 2, cz = z0 + dp / 2;
            // high balloons drifting at the ceiling in most rooms
            if (room != Room.QUIET) {
                int n = 2 + Hash.range(h >>> 3, 5);
                for (int m = 0; m < n; m++) {
                    long hh = Hash.hash(h, m, 5);
                    int bx = x0 + 1 + Hash.range(hh, Math.max(1, wd - 1)), bz = z0 + 1 + Hash.range(hh >>> 8, Math.max(1, dp - 1));
                    if (w.contains(bx, bz) && w.isAir(bx, CEIL - 1, bz)) w.set(bx, CEIL - 1, bz, high[Hash.range(hh >>> 16, 5)]);
                }
            }
            // bunting strung along the north wall
            if (room != Room.QUIET && Hash.unit(h >>> 7) < 0.5f) {
                for (int u = 1; u < wd; u++) w.set(x0 + u, CEIL - 1, z0 + 1, face(bunting, Direction.NORTH));
            }
            switch (room) {
                case PARTY -> {
                    w.set(cx, F + 1, cz, face(table, Direction.NORTH));
                    w.set(cx + 1, F + 1, cz, face(table, Direction.NORTH));
                    w.set(cx, F + 2, cz, face(cake, Direction.SOUTH));
                    w.set(cx + 1, F + 2, cz, face(hat, Direction.NORTH));
                    w.set(cx - 1, F + 1, cz, face(chair, Direction.EAST));
                    w.set(cx + 2, F + 1, cz, face(chair, Direction.WEST));
                    w.set(cx, F + 1, cz - 1, face(chair, Direction.SOUTH));
                    if (Hash.unit(h >>> 11) < 0.6f) w.set(x0 + 1, F + 1, z0 + dp - 1, (h & 1) == 0 ? presentA : presentB);
                    // balloons tied to the chairs, bobbing at face height
                    w.set(cx - 1, F + 2, cz + 1, low[Hash.range(h >>> 13, 5)]);
                    w.set(cx + 2, F + 2, cz - 1, low[Hash.range(h >>> 17, 5)]);
                }
                case BALLOONS -> {
                    // a room full to the walls. You cannot cross it quietly.
                    for (int u = 1; u < wd; u++)
                        for (int v = 1; v < dp; v++) {
                            long hh = Hash.hash(s, x0 + u, z0 + v, 99);
                            if (Hash.unit(hh) < 0.55f) w.set(x0 + u, F + 1 + (int) ((hh >>> 30) & 1), z0 + v, low[Hash.range(hh >>> 8, 5)]);
                        }
                }
                case PRESENTS -> {
                    for (int m = 0; m < 6; m++) {
                        long hh = Hash.hash(h, m, 8);
                        int px = cx - 1 + Hash.range(hh, 3), pz = cz - 1 + Hash.range(hh >>> 8, 3);
                        w.set(px, F + 1, pz, (hh & 1) == 0 ? presentA : presentB);
                    }
                }
                case BANQUET -> {
                    for (int u = 2; u < wd - 1; u++) {
                        w.set(x0 + u, F + 1, cz, face(table, Direction.NORTH));
                        BlockState top = (u % 3 == 0) ? cake : hat;
                        w.set(x0 + u, F + 2, cz, face(top, u % 2 == 0 ? Direction.NORTH : Direction.SOUTH));
                        if (u % 2 == 0) {
                            w.set(x0 + u, F + 1, cz - 1, face(chair, Direction.SOUTH));
                            w.set(x0 + u, F + 1, cz + 1, face(chair, Direction.NORTH));
                        }
                    }
                    banner(w, x0, z0, wd);
                }
                case QUIET -> {
                    // no streamers, no lights, one present. Every wall says the same thing.
                    w.set(cx, F + 1, cz, presentB);
                    for (int u = 1; u < wd; u++) {
                        if (w.contains(x0 + u, z0) && !w.isAir(x0 + u, F + 2, z0)) w.set(x0 + u, F + 2, z0, scrawl[0]);
                        if (w.contains(x0 + u, z0) && !w.isAir(x0 + u, F + 3, z0) && u % 2 == 0) w.set(x0 + u, F + 3, z0, scrawl[2]);
                    }
                }
                case EXIT -> {
                    // a plain door in the west wall that somebody labelled, helpfully
                    int dz = z0 + 1 + Hash.range(h >>> 21, Math.max(1, dp - 2));
                    w.set(x0, F + 1, dz, door);
                    w.set(x0, F + 2, dz, doorTop);
                    w.set(x0 + 1, F + 1, dz, ModBlocks.AIR);
                    w.set(x0 + 1, F + 2, dz, ModBlocks.AIR);
                    w.set(x0 + 1, CEIL - 1, dz, ModBlocks.AIR);
                }
                default -> {
                    if (Hash.unit(h >>> 25) < 0.3f) w.set(cx, F + 1, cz, face(chair, Direction.from2DDataValue(Hash.range(h >>> 27, 4))));
                    if (Hash.unit(h >>> 29) < 0.15f) w.set(cx + 1, F + 1, cz + 1, face(hat, Direction.NORTH));
                }
            }
        }

        void banner(ChunkWriter w, int x0, int z0, int wd) {
            String word = wd - 2 >= 8 ? "BIRTHDAY" : "HAPPY";
            int start = x0 + 1 + (wd - 1 - word.length()) / 2;
            for (int n = 0; n < word.length(); n++) {
                BlockState b = ModBlocks.state("party_banner_" + Character.toLowerCase(word.charAt(n)));
                w.set(start + n, CEIL - 1, z0 + 1, b.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
        }
    }

    // ---------------------------------------------------------------- the party

    static final String[] POP_LINES = {"I heard that =)", "Found you =)", "There you are =)", "Don't pop them =)",
            "That was MY balloon =)", "=) =) =)"};

    @SuppressWarnings("unchecked")
    static EntityType<? extends Mob> partygoer() {
        return ModEntities.ALL.get("partygoer");
    }

    /** A balloon pops: everyone at the party turns to look. */
    public static void pop(ServerLevel level, BlockPos pos, ServerPlayer p) {
        BlockState st = level.getBlockState(pos);
        level.removeBlock(pos, false);
        level.playSound(null, pos, ModSounds.get("event.level_fun_pop"), SoundSource.BLOCKS, 1.6f, 0.9f + level.random.nextFloat() * 0.25f);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.3f, 0.4f), 1.2f), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                16, 0.25, 0.25, 0.25, 0.1);
        BackroomsLevel here = com.noclip.level.Levels.get(level.dimension());
        if (p.isCreative() || here == null || !ID.equals(here.id)) return;
        Sanity.add(p, -1.5f);
        int alerted = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40), m -> m.getType() == partygoer())) {
            m.setTarget(p);
            alerted++;
        }
        if (alerted < 4 && level.random.nextFloat() < 0.6f) {
            Mob m = DeepEndUtil.spawnNear(level, p, partygoer(), 10, 18, true);
            if (m != null) m.setTarget(p);
        }
        if (level.random.nextFloat() < 0.5f) say(p, POP_LINES[level.random.nextInt(POP_LINES.length)]);
    }

    static void say(ServerPlayer p, String line) {
        p.sendSystemMessage(Component.literal("<Partygoer> " + line).withStyle(ChatFormatting.YELLOW));
    }

    /** Open a present. Sometimes it is something nice. */
    public static void openPresent(ServerLevel level, BlockPos pos, ServerPlayer p) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, ModSounds.get("event.level_fun_present"), SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.95f, 0.8f, 0.3f), 1f), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                24, 0.3, 0.3, 0.3, 0.1);
        float r = level.random.nextFloat();
        if (r < 0.45f) {
            ItemStack[] gifts = {new ItemStack(ModItems.ALMOND_WATER), new ItemStack(ModItems.ENERGY_BAR),
                    new ItemStack(ModItems.BATTERY), new ItemStack(ModItems.CANNED_BEANS)};
            int n = 1 + level.random.nextInt(2);
            for (int i = 0; i < n; i++) {
                ItemStack st = gifts[level.random.nextInt(gifts.length)];
                if (!p.getInventory().add(st.copy())) p.drop(st.copy(), false);
            }
            p.sendSystemMessage(Component.literal("A gift. For you. =)").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (r < 0.75f && !p.isCreative()) {
            surprise(level, p, 2);
        } else if (r < 0.9f) {
            p.sendSystemMessage(Component.literal("The box is empty except for a note: =)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            Sanity.add(p, -3f);
        } else {
            ItemStack hat = new ItemStack(ModBlocks.get("party_hat"));
            if (!p.getInventory().add(hat)) p.drop(hat, false);
            p.sendSystemMessage(Component.literal("A party hat. Put it on. =)").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The lights go out. When they come back, everyone is here. */
    static void surprise(ServerLevel level, ServerPlayer p, int guests) {
        Blackouts.start(level, 50);
        level.playSound(null, p.blockPosition(), ModSounds.get("event.level_fun_surprise"), SoundSource.HOSTILE, 1.4f, 1f);
        DeepEndUtil.title(p, Component.literal("SURPRISE =)").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), null, 2, 30, 10);
        Sanity.add(p, -8f);
        for (int i = 0; i < guests; i++) {
            Mob m = DeepEndUtil.spawnNear(level, p, partygoer(), 4, 9, false);
            if (m != null) m.setTarget(p);
        }
    }

    static final class Party implements LevelBehavior {
        private final Map<UUID, Integer> nextSurprise = new HashMap<>();
        static final String[] LINES = {"Where are you going? =)", "The cake is ready =)", "Come back to the party =)",
                "Somebody's birthday =)", "We saved you a seat =)", "Happy birthday =)"};

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            nextSurprise.put(p.getUUID(), p.tickCount + 20 * (150 + p.getRandom().nextInt(120)));
            p.sendSystemMessage(Component.literal("<Partygoer> Hello friend =)").withStyle(ChatFormatting.YELLOW));
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            int due = nextSurprise.computeIfAbsent(p.getUUID(), u -> p.tickCount + 20 * 200);
            if (p.tickCount >= due && !p.isCreative()) {
                surprise(level, p, 3);
                nextSurprise.put(p.getUUID(), p.tickCount + 20 * (180 + p.getRandom().nextInt(150)));
            }
            if (p.tickCount % 20 == 0 && p.getRandom().nextFloat() < 0.008f && !p.isCreative()) {
                say(p, LINES[p.getRandom().nextInt(LINES.length)]);
            }
        }
    }
}
