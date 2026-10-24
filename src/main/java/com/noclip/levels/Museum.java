package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Levels;
import com.noclip.level.SpawnRule;
import com.noclip.level.Visuals;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModItems;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.Blackouts;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Museum of You. White galleries opening into white galleries, parquet floors,
 * spotlights on plinths. Every exhibit is about you: your likeness, what you are
 * carrying, the levels you have been to, how many times you have died, how far
 * you have walked. The placards are written as you approach. The statues only move
 * when nobody is looking, and at closing time the lights go out. One plinth has
 * your name on it and nothing on top. Step up, and the museum accepts the donation.
 */
public final class Museum {
    public static final String ID = "museum";
    static final int FLOOR = 10, CEIL = 17, C = 16;

    private Museum() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("Museum of You").order(1140)
                .tagline("A retrospective. The subject is still at large.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Curator())
                .visuals(new Visuals().tint(1.0f, 0.99f, 0.96f).saturation(0.85f).contrast(1.05f).brightness(1.08f)
                        .grain(0.04f).aberration(0.0008f).vignette(0.4f).bloom(0.4f).fog(8f, 62f).fogColor(0xCFCAC0).noRec())
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("mannequin"), 3, 0.2f).dist(18, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(26, 44).weight(1))
                .exit(ExitType.NOCLIP, "archive", 2).exit(ExitType.NOCLIP, "dead_mall", 1).exit(ExitType.NOCLIP, "photo_gallery", 1)
                .exit(ExitType.DOOR, "dead_mall", 2).exit(ExitType.DOOR, "convention_center", 2).exit(ExitType.DOOR, "archive", 1)
                .exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.SECRET, "photo_gallery", 2).exit(ExitType.SECRET, "the_hub", 1).exit(ExitType.SECRET, "level_0", 1)
                .build();
    }

    enum Room { SINGLE, TRIPLE, GRID, PAINTINGS, YOURS, GIFT, CLOSED }

    static Room room(long s, int i, int k) {
        float r = Hash.unit(s, i, k, 0x3A5);
        if (r < 0.04f) return Room.YOURS;
        if (r < 0.07f) return Room.GIFT;
        if (r < 0.15f) return Room.CLOSED;
        if (r < 0.25f) return Room.GRID;
        if (r < 0.40f) return Room.PAINTINGS;
        if (r < 0.65f) return Room.TRIPLE;
        return Room.SINGLE;
    }

    static final class Gen implements LevelGenerator {
        final BlockState parquet = ModBlocks.state("mus_parquet");
        final BlockState wall = ModBlocks.state("mus_wall");
        final BlockState wallBase = ModBlocks.state("mus_wall_base");
        final BlockState wallTop = ModBlocks.state("mus_wall_top");
        final BlockState ceiling = ModBlocks.state("mus_ceiling");
        final BlockState plinth = ModBlocks.state("mus_plinth");
        final BlockState yours = ModBlocks.state("mus_plinth_empty");
        final BlockState vitrine = ModBlocks.state("mus_vitrine");
        final BlockState spot = ModBlocks.state("mus_spot");
        final BlockState spotOff = ModBlocks.state("mus_spot_off");
        final BlockState rope = ModBlocks.state("mus_rope");
        final BlockState bench = ModBlocks.state("mus_bench");
        final BlockState shelf = ModBlocks.state("mus_souvenir_shelf");
        final BlockState souvenir = ModBlocks.state("mus_souvenir");
        final BlockState giftSign = ModBlocks.state("mus_gift_sign");
        final BlockState exitDoor = ModBlocks.state("mus_exit_door");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final String[] arts = {"lobby", "dark", "warehouse", "you", "stairs", "door", "portrait", "blank"};
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, parquet);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, C), lz = Math.floorMod(z, C);
                    boolean wx = lx == 0 && !(lz >= 6 && lz <= 9), wz = lz == 0 && !(lx >= 6 && lx <= 9);
                    if (wx || wz || (lx == 0 && lz == 0)) {
                        w.set(x, FLOOR + 1, z, wallBase);
                        for (int y = FLOOR + 2; y < CEIL - 1; y++) w.set(x, y, z, wall);
                        w.set(x, CEIL - 1, z, wallTop);
                    } else if ((lx == 0 && lz >= 6 && lz <= 9) || (lz == 0 && lx >= 6 && lx <= 9)) {
                        // doorway lintel
                        w.set(x, CEIL - 1, z, wallTop);
                        w.set(x, CEIL - 2, z, wall);
                    }
                }
            int i0 = Math.floorDiv(w.minX, C) - 1, i1 = Math.floorDiv(w.maxX, C) + 1;
            int k0 = Math.floorDiv(w.minZ, C) - 1, k1 = Math.floorDiv(w.maxZ, C) + 1;
            for (int i = i0; i <= i1; i++)
                for (int k = k0; k <= k1; k++) room(w, s, i, k, i * C, k * C);
        }

        BlockState art(long s, int x, int z, Direction toWall) {
            String a = arts[Hash.range(s, x, z, 0xA27, arts.length)];
            return ModBlocks.state("mus_art_" + a).setValue(HorizontalDirectionalBlock.FACING, toWall);
        }

        void plinthAt(ChunkWriter w, int x, int z, boolean lit) {
            w.set(x, FLOOR + 1, z, plinth);
            w.set(x, FLOOR + 2, z, vitrine);
            w.set(x, CEIL - 1, z, lit ? spot : spotOff);
        }

        void room(ChunkWriter w, long s, int i, int k, int x0, int z0) {
            Room type = Museum.room(s, i, k);
            boolean lit = type != Room.CLOSED;
            // paintings on the walls between doorways
            int[] spots = type == Room.PAINTINGS ? new int[]{2, 4, 11, 13} : new int[]{3, 12};
            for (int a : spots) {
                if (!Hash.chance(s, i * 16 + a, k, 0xAA1, type == Room.PAINTINGS ? 0.95f : 0.6f)) continue;
                w.set(x0 + a, FLOOR + 3, z0 + 1, art(s, x0 + a, z0 + 1, Direction.NORTH));
                w.set(x0 + a, FLOOR + 3, z0 + 15, art(s, x0 + a, z0 + 15, Direction.SOUTH));
                w.set(x0 + 1, FLOOR + 3, z0 + a, art(s, x0 + 1, z0 + a, Direction.WEST));
                w.set(x0 + 15, FLOOR + 3, z0 + a, art(s, x0 + 15, z0 + a, Direction.EAST));
            }
            for (int a : new int[]{3, 12})
                for (int b : new int[]{3, 12}) w.set(x0 + a, CEIL - 1, z0 + b, lit ? spot : spotOff);
            switch (type) {
                case SINGLE, CLOSED -> {
                    plinthAt(w, x0 + 8, z0 + 8, lit);
                    for (int d = 6; d <= 10; d++) {
                        w.set(x0 + d, FLOOR + 1, z0 + 6, rope);
                        w.set(x0 + d, FLOOR + 1, z0 + 10, rope);
                    }
                    for (int d = 7; d <= 9; d++) {
                        w.set(x0 + 6, FLOOR + 1, z0 + d, rope.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                        w.set(x0 + 10, FLOOR + 1, z0 + d, rope.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    }
                    w.set(x0 + 8, FLOOR + 1, z0 + 13, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
                case TRIPLE -> {
                    for (int a : new int[]{4, 8, 12}) plinthAt(w, x0 + a, z0 + 8, lit);
                    w.set(x0 + 8, FLOOR + 1, z0 + 12, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    w.set(x0 + 8, FLOOR + 1, z0 + 4, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                }
                case GRID -> {
                    for (int a : new int[]{4, 8, 12})
                        for (int b : new int[]{4, 8, 12}) if (!(a == 8 && b == 8)) plinthAt(w, x0 + a, z0 + b, lit);
                }
                case PAINTINGS -> {
                    w.set(x0 + 7, FLOOR + 1, z0 + 8, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    w.set(x0 + 9, FLOOR + 1, z0 + 8, bench.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                }
                case YOURS -> {
                    w.set(x0 + 8, FLOOR + 1, z0 + 8, yours);
                    w.set(x0 + 8, CEIL - 1, z0 + 8, spot);
                }
                case GIFT -> {
                    for (int a : new int[]{4, 8, 12}) {
                        for (int b = 3; b <= 12; b++) {
                            if (b == 7 || b == 8) continue;
                            w.set(x0 + a, FLOOR + 1, z0 + b, shelf.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                            if (Hash.chance(s, x0 + a, z0 + b, 0x50F, 0.3f)) w.set(x0 + a, FLOOR + 2, z0 + b, souvenir);
                        }
                    }
                    w.set(x0 + 8, CEIL - 2, z0 + 7, giftSign);
                    w.set(x0 + 3, FLOOR + 1, z0, exitDoor);
                    w.set(x0 + 3, FLOOR + 2, z0, exitDoor);
                    w.set(x0 + 3, FLOOR + 3, z0 + 1, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the curator

    static final Map<Long, Long> SHOWN = new HashMap<>();   // plinth pos -> last time someone was near
    static long nextClosing = -1;

    static ItemLike souvenirFor(String level) {
        String block = switch (level) {
            case "level_0" -> "yellow_wallpaper";
            case "level_1" -> "wooden_crate";
            case "level_6" -> "exit_sign";
            case "lift_lobby" -> "lift_call_button";
            case "stairwell" -> "stair_digit";
            case "subway" -> "sub_rail";
            case "archive" -> "arc_shelf_a";
            case "museum" -> "mus_plinth";
            default -> null;
        };
        if (block != null && ModBlocks.BLOCKS.containsKey(block)) return ModBlocks.get(block);
        return ModItems.ALMOND_WATER;
    }

    record Exhibit(ItemStack item, String title, String text) {}

    static Exhibit exhibit(ServerPlayer p, ServerLevel level, BlockPos pos, boolean yours) {
        String name = p.getGameProfile().getName();
        String NAME = name.toUpperCase(Locale.ROOT);
        if (yours) return new Exhibit(ItemStack.EMPTY, NAME, "Acquired today. On loan from the Backrooms. Plinth awaiting its subject.");
        int idx = Hash.range(pos.asLong(), 0x4D5, 0, 10);
        var stats = p.getStats();
        switch (idx) {
            case 0 -> {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, new ResolvableProfile(p.getGameProfile()));
                return new Exhibit(head, "A LIKENESS", name + ". Wax, hair and something else. The eyes were done from memory.");
            }
            case 1 -> {
                ItemStack carried = p.getMainHandItem().isEmpty() ? ItemStack.EMPTY : p.getMainHandItem().copyWithCount(1);
                if (carried.isEmpty()) {
                    for (ItemStack st : p.getInventory().items) if (!st.isEmpty()) { carried = st.copyWithCount(1); break; }
                }
                if (carried.isEmpty()) return new Exhibit(ItemStack.EMPTY, "WHAT " + NAME + " CARRIED", "Nothing. They came with nothing. Note the empty hands.");
                return new Exhibit(carried, "WHAT " + NAME + " CARRIED", carried.getHoverName().getString() + ". Carried by " + name + " into this museum. Still warm.");
            }
            case 2, 3 -> {
                List<String> been = new ArrayList<>(PlayerData.visited(p));
                if (been.isEmpty()) been.add(ID);
                String lv = been.get(Math.floorMod((int) (pos.asLong() >>> 7), been.size()));
                String where = EmptyBuildingKit.pretty(lv);
                String text = lv.equals(ID) ? "You are here. The museum is aware of the irony."
                        : "Visited by " + name + ". A souvenir, taken without asking. They did leave, eventually.";
                return new Exhibit(new ItemStack(souvenirFor(lv)), where.toUpperCase(Locale.ROOT), text);
            }
            case 4 -> {
                int deaths = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
                if (deaths <= 0) return new Exhibit(ItemStack.EMPTY, "RESERVED", "For " + name + "'s first death. Coming soon.");
                return new Exhibit(new ItemStack(Items.SKELETON_SKULL), "IN MEMORIAM", name + " has died " + deaths
                        + (deaths == 1 ? " time" : " times") + ". The museum has kept one of each.");
            }
            case 5 -> {
                int cm = stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) + stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM));
                return new Exhibit(new ItemStack(Items.LEATHER_BOOTS), "THE WALK", String.format(Locale.ROOT,
                        "%.1f km on foot. The left sole is worn through. %s never walked in a straight line.", cm / 100000.0, name));
            }
            case 6 -> {
                long t = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20;
                return new Exhibit(new ItemStack(Items.CLOCK), "TIME SERVED", (t / 3600) + " hours, " + ((t / 60) % 60)
                        + " minutes. The hands stopped when " + name + " arrived.");
            }
            case 7 -> {
                int s = Math.round(PlayerData.sanity(p));
                return new Exhibit(new ItemStack(Items.GLASS_BOTTLE), "A JAR OF " + NAME, "Sanity, " + s + "% remaining. Do not open. Do not shake.");
            }
            case 8 -> {
                int k = stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
                return new Exhibit(new ItemStack(Items.IRON_SWORD), "EVERYTHING " + NAME + " KILLED", k + ". The collection is ongoing.");
            }
            default -> {
                return new Exhibit(ItemStack.EMPTY, "EXHIBIT 0: " + NAME + ", NOW", "Standing in a museum, reading about themself. Please do not touch the exhibit.");
            }
        }
    }

    static String tag(BlockPos pos) {
        return "mus_" + pos.asLong();
    }

    static void show(ServerPlayer p, ServerLevel level, BlockPos plinth) {
        boolean yours = level.getBlockState(plinth).is(ModBlocks.get("mus_plinth_empty"));
        Exhibit e = exhibit(p, level, plinth, yours);
        String t = tag(plinth);
        Vec3 c = Vec3.atBottomCenterOf(plinth);
        MutableComponent text = Component.literal(e.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\n" + e.text).withStyle(ChatFormatting.GRAY));
        double ty = yours ? c.y + 1.6 : c.y + 2.25;
        EmptyBuildingKit.text(level, new Vec3(c.x, ty, c.z), 0f, text, "vertical", 0.45f, 0xB0101010, 170, t);
        if (!e.item.isEmpty()) {
            float yaw = (float) Hash.range(plinth.asLong(), 7, 0, 360);
            EmptyBuildingKit.item(level, new Vec3(c.x, c.y + 1.45, c.z), yaw, e.item, 0.55f, t);
        }
    }

    static final class Curator implements LevelBehavior {
        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (nextClosing < 0) nextClosing = level.getGameTime() + 20 * 200;
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            // your plinth
            if (p.tickCount % 5 == 0 && p.onGround() && level.getBlockState(p.blockPosition().below()).is(ModBlocks.get("mus_plinth_empty"))) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.museum_acquire"), SoundSource.AMBIENT, 1.2f, 1f);
                EmptyBuildingKit.narrate(p, "The museum thanks you for your donation.");
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 1f));
                Travel.exit(p, ExitType.SECRET, p.blockPosition());
                return;
            }
            if (p.tickCount % 20 != 0) return;
            BlockPos c = p.blockPosition();
            long now = level.getGameTime();
            for (int dx = -9; dx <= 9; dx++)
                for (int dz = -9; dz <= 9; dz++) {
                    BlockPos bp = new BlockPos(c.getX() + dx, FLOOR + 1, c.getZ() + dz);
                    BlockState st = level.getBlockState(bp);
                    if (!st.is(ModBlocks.get("mus_plinth")) && !st.is(ModBlocks.get("mus_plinth_empty"))) continue;
                    Long seen = SHOWN.put(bp.asLong(), now);
                    if (seen == null) show(p, level, bp);
                }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            if (now % 40 == 0) {
                for (Iterator<Map.Entry<Long, Long>> it = SHOWN.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Long, Long> e = it.next();
                    if (now - e.getValue() < 20 * 15) continue;
                    BlockPos bp = BlockPos.of(e.getKey());
                    boolean near = false;
                    for (Player pl : level.players()) if (pl.blockPosition().distSqr(bp) < 30 * 30) near = true;
                    if (near) continue;
                    if (level.isLoaded(bp)) EmptyBuildingKit.clearDisplays(level, Vec3.atCenterOf(bp), 3, tag(bp));
                    it.remove();
                }
            }
            // closing time: the lights go out, and the statues are free to move
            if (nextClosing > 0 && now >= nextClosing) {
                nextClosing = now + 20 * (240 + level.random.nextInt(180));
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.museum_closing"), SoundSource.AMBIENT, 1f, 1f);
                    EmptyBuildingKit.say(p, "PA", ChatFormatting.DARK_AQUA, "The museum is now closing. Please make your way to the exit through the gift shop.");
                }
                CLOSING_AT = now + 20 * 8;
            }
            if (CLOSING_AT > 0 && now >= CLOSING_AT) {
                CLOSING_AT = -1;
                Blackouts.start(level, 20 * 22);
            }
        }
    }

    static long CLOSING_AT = -1;
}
