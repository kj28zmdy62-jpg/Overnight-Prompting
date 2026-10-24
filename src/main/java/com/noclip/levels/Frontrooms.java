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
import com.noclip.sanity.Sanity;
import com.noclip.travel.Travel;
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Frontrooms. A sunny street of identical houses, every one of them number
 * 13, with the TV on and nobody home. It looks so much like the real world.
 * Stay, and the rooms you are not looking at slowly turn back into the Lobby.
 */
public final class Frontrooms {
    static final int G = 8, ZP = 48, XP = 96, LOT = 16;
    static final String ID = "frontrooms";

    private Frontrooms() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Frontrooms").order(9030)
                .tagline("You made it home. Didn't you?")
                .danger(BackroomsLevel.Danger.SAFE)
                .generator(Gen::new)
                .behavior(new Reversion())
                .visuals(new Visuals().tint(1.02f, 1.0f, 0.98f).saturation(1.0f).contrast(1.02f).brightness(1.05f)
                        .grain(0.02f).aberration(0.0004f).vignette(0.18f).warp(0.0f).scanlines(0f).flicker(0f).bloom(0.3f)
                        .fog(40f, 150f).fogColor(0xC8DCEC).sky().clouds().noRec())
                .sanityDrain(0.012f)
                .spawnY(G + 1, G + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 4, 0.35f).dist(18, 40).weight(4))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.03f).seen().dist(30, 46).weight(1))
                .exit(ExitType.DOOR, "level_0", 1)
                .exit(ExitType.DESCEND, "level_9", 2).exit(ExitType.DESCEND, "level_0", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "level_9", 1)
                .build();
    }

    /** A house plot: which side of the street it faces and its x origin. */
    record Lot(int x0, int frontZ, int dir, long key) {
        /** World z of house depth hd (0 = front wall, 8 = back wall). */
        int z(int hd) {
            return frontZ + dir * hd;
        }

        /** The direction you walk to go deeper into the house. */
        Direction back() {
            return dir > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> c = new HashMap<>();
        final String[] sidings = {"white", "blue", "beige", "sage"};

        BlockState b(String name) {
            return c.computeIfAbsent(name, ModBlocks::state);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, G - 1, w.maxZ, b("subfloor"));
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int zl = Math.floorMod(z, ZP), xl = Math.floorMod(x, XP);
                    boolean crossStreet = xl < LOT;
                    BlockState ground;
                    if (zl == 0 || zl == 7) ground = b("frontroom_sidewalk");
                    else if (zl < 7) ground = crossStreet && xl >= 4 && xl <= 11 ? b("frontroom_asphalt")
                            : zl == 3 ? b("frontroom_asphalt_line") : b("frontroom_asphalt");
                    else if (crossStreet) ground = xl >= 4 && xl <= 11 ? b("frontroom_asphalt") : xl == 3 || xl == 12 ? b("frontroom_sidewalk") : b("frontroom_lawn");
                    else ground = b("frontroom_lawn");
                    w.set(x, G, z, ground);
                    // street lamps along the sidewalks
                    if (zl == 7 && Math.floorMod(x, 32) == 8 || zl == 0 && Math.floorMod(x, 32) == 24) {
                        w.set(x, G + 1, z, b("frontroom_street_lamp"));
                        w.set(x, G + 2, z, b("frontroom_street_lamp"));
                        w.set(x, G + 3, z, b("frontroom_street_lamp"));
                        w.set(x, G + 4, z, b("frontroom_street_lamp_head"));
                    }
                    if (zl == 27 && !crossStreet) w.set(x, G + 1, z, b("frontroom_picket_fence")
                            .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true));
                }
            }
            // houses whose lots touch this chunk
            for (int zi = Math.floorDiv(w.minZ, ZP) - 1; zi <= Math.floorDiv(w.maxZ, ZP) + 1; zi++) {
                for (int xi = Math.floorDiv(w.minX - 16, LOT); xi <= Math.floorDiv(w.maxX + 16, LOT); xi++) {
                    if (Math.floorMod(xi, XP / LOT) == 0) continue;
                    int x0 = xi * LOT;
                    house(w, s, new Lot(x0, zi * ZP + 13, 1, Hash.hash(s, xi, zi, 1)));
                    house(w, s, new Lot(x0, zi * ZP + 42, -1, Hash.hash(s, xi, zi, 2)));
                }
            }
        }

        void put(ChunkWriter w, Lot lot, int hx, int y, int hd, BlockState st) {
            w.set(lot.x0 + 2 + hx, y, lot.z(hd), st);
        }

        BlockState pane(boolean alongX) {
            return b("frontroom_window").setValue(alongX ? BlockStateProperties.EAST : BlockStateProperties.NORTH, true)
                    .setValue(alongX ? BlockStateProperties.WEST : BlockStateProperties.SOUTH, true);
        }

        void house(ChunkWriter w, long s, Lot lot) {
            long k = lot.key;
            if (lot.x0 + 18 < w.minX || lot.x0 - 2 > w.maxX) return;
            int zmin = Math.min(lot.z(-6), lot.z(10)), zmax = Math.max(lot.z(-6), lot.z(10));
            if (zmax < w.minZ || zmin > w.maxZ) return;
            float wrong = Hash.unit(k >>> 3);
            boolean turned = wrong < 0.03f;                 // this one already went back
            boolean bricked = wrong >= 0.03f && wrong < 0.08f;
            BlockState siding = b("frontroom_siding_" + sidings[Hash.range(k, 4)]);
            String roof = (k & 16) == 0 ? "grey" : "brown";
            BlockState iwall = turned ? b("yellow_wallpaper") : b("frontroom_wall");
            BlockState ceil = turned ? b("ceiling_tile") : b("frontroom_ceiling");
            Direction back = lot.back(), front = back.getOpposite();

            for (int hx = 0; hx <= 10; hx++) {
                for (int hd = 0; hd <= 8; hd++) {
                    boolean ext = hx == 0 || hx == 10 || hd == 0 || hd == 8;
                    boolean living = hx >= 1 && hx <= 5, kitchen = hx >= 7 && hd <= 3;
                    BlockState floor = turned ? b("moist_carpet") : living ? b("frontroom_carpet")
                            : kitchen ? b("frontroom_kitchen_tile") : b("frontroom_floorboards");
                    put(w, lot, hx, G, hd, floor);
                    for (int y = G + 1; y <= G + 3; y++) put(w, lot, hx, y, hd, ModBlocks.AIR);
                    put(w, lot, hx, G + 4, hd, ext ? siding : ceil);
                    if (ext) {
                        boolean corner = (hx == 0 || hx == 10) && (hd == 0 || hd == 8);
                        for (int y = G + 1; y <= G + 3; y++) put(w, lot, hx, y, hd, corner ? b("frontroom_trim") : siding);
                        boolean alongX = hd == 0 || hd == 8;
                        boolean win = (hd == 0 && (hx == 7 || hx == 8 || hx == 1 || hx == 2)) || (hd == 8 && hx == 8)
                                || ((hx == 0) && hd >= 3 && hd <= 5) || (hx == 10 && (hd == 2 || hd == 6));
                        if (win) put(w, lot, hx, G + 2, hd, bricked ? iwall : pane(alongX));
                    } else if (hx == 6 || (hd == 4 && hx > 6)) {
                        for (int y = G + 1; y <= G + 3; y++) put(w, lot, hx, y, hd, iwall);
                        if (hx == 6 && (hd == 2 || hd == 6)) {
                            put(w, lot, hx, G + 1, hd, ModBlocks.AIR);
                            put(w, lot, hx, G + 2, hd, ModBlocks.AIR);
                        }
                    }
                }
            }
            // gable roof along the street, eaves overhanging by one
            for (int hx = -1; hx <= 11; hx++) {
                for (int hd = -1; hd <= 9; hd++) {
                    int lvl = Math.min(hd + 1, 9 - hd);
                    int y = G + 4 + lvl;
                    BlockState st;
                    if (hd == 4) st = b("frontroom_roof_" + roof);
                    else st = b("frontroom_roof_" + roof + "_stairs").setValue(StairBlock.FACING, hd < 4 ? back : front);
                    put(w, lot, hx, y, hd, st);
                    if (hx >= 0 && hx <= 10 && hd >= 0 && hd <= 8) {
                        boolean gable = hx == 0 || hx == 10;
                        for (int yy = G + 5; yy < y; yy++) put(w, lot, hx, yy, hd, gable ? siding : b("void_filler"));
                    }
                }
            }
            // front door, number 13, and the path down to the sidewalk
            BlockState door = b("frontroom_front_door").setValue(DoorBlock.FACING, front);
            put(w, lot, 4, G + 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            put(w, lot, 4, G + 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            for (int hd = -5; hd <= -1; hd++) put(w, lot, 4, G, hd, b("frontroom_sidewalk"));
            for (int hx = 11; hx <= 13; hx++) for (int hd = -5; hd <= 8; hd++) put(w, lot, hx, G, hd, b("frontroom_asphalt"));
            put(w, lot, 6, G + 1, -5, b("frontroom_mailbox").setValue(HorizontalDirectionalBlock.FACING, front));
            if ((k & 32) == 0) tree(w, lot);

            if (turned) {
                // the light in this one hums
                put(w, lot, 3, G + 4, 4, b("fluorescent_light"));
                put(w, lot, 8, G + 4, 6, b("fluorescent_light"));
                return;
            }
            furnish(w, lot, k, back, front);
        }

        void tree(ChunkWriter w, Lot lot) {
            for (int y = G + 1; y <= G + 4; y++) put(w, lot, 8, y, -3, b("frontroom_trunk").setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            for (int dx = -1; dx <= 1; dx++)
                for (int dd = -1; dd <= 1; dd++)
                    for (int y = G + 4; y <= G + 6; y++) {
                        if (dx == 0 && dd == 0 && y == G + 4) continue;
                        if (Math.abs(dx) + Math.abs(dd) == 2 && y == G + 6) continue;
                        put(w, lot, 8 + dx, y, -3 + dd, b("frontroom_leaves"));
                    }
        }

        void furnish(ChunkWriter w, Lot lot, long k, Direction back, Direction front) {
            Direction east = Direction.EAST, west = Direction.WEST;
            // living room
            put(w, lot, 1, G + 1, 3, b("frontroom_sofa").setValue(HorizontalDirectionalBlock.FACING, east));
            put(w, lot, 1, G + 1, 4, b("frontroom_sofa").setValue(HorizontalDirectionalBlock.FACING, east));
            String[] tvs = {"static", "static", "off", "off", "lobby", "smile"};
            put(w, lot, 5, G + 1, 4, b("frontroom_tv_" + tvs[Hash.range(k >>> 8, tvs.length)]).setValue(HorizontalDirectionalBlock.FACING, west));
            put(w, lot, 3, G + 1, 4, b("frontroom_table").setValue(HorizontalDirectionalBlock.FACING, east));
            put(w, lot, 1, G + 1, 7, b("frontroom_floor_lamp").setValue(HorizontalDirectionalBlock.FACING, east));
            String[] clocks = {"normal", "normal", "nohands", "backwards", "thirteen"};
            put(w, lot, 5, G + 3, 6, b("frontroom_clock_" + clocks[Hash.range(k >>> 12, clocks.length)]).setValue(HorizontalDirectionalBlock.FACING, west));
            put(w, lot, 2, G + 3, 1, b("frontroom_photo_family").setValue(HorizontalDirectionalBlock.FACING, back));
            String[] photos = {"family", "you", "lobby", "house"};
            put(w, lot, 1, G + 3, 7, b("frontroom_photo_" + photos[Hash.range(k >>> 16, 4)]).setValue(HorizontalDirectionalBlock.FACING, front));
            // kitchen
            for (int hx = 7; hx <= 9; hx++) put(w, lot, hx, G + 1, 1, b("frontroom_counter"));
            put(w, lot, 9, G + 1, 3, b("frontroom_fridge"));
            put(w, lot, 9, G + 2, 3, b("frontroom_fridge_top"));
            put(w, lot, 7, G + 1, 3, b("frontroom_floor_lamp").setValue(HorizontalDirectionalBlock.FACING, east));
            // bedroom
            put(w, lot, 9, G + 1, 7, b("frontroom_bed").setValue(HorizontalDirectionalBlock.FACING, back));
            put(w, lot, 7, G + 1, 7, b("frontroom_floor_lamp").setValue(HorizontalDirectionalBlock.FACING, east));
            put(w, lot, 8, G + 2, 5, b("frontroom_mirror").setValue(HorizontalDirectionalBlock.FACING, back));
            put(w, lot, 6, G + 2, 1, b("frontroom_wall_switch"));

            float r = Hash.unit(k >>> 24);
            if (r < 0.06f) {
                // the closet stands open. Inside it is a different colour, and it goes back too far.
                put(w, lot, 3, G + 1, 8, ModBlocks.AIR);
                put(w, lot, 3, G + 2, 8, ModBlocks.AIR);
                for (int hd = 9; hd <= 11; hd++) {
                    put(w, lot, 2, G + 1, hd, b("yellow_wallpaper_baseboard"));
                    put(w, lot, 4, G + 1, hd, b("yellow_wallpaper_baseboard"));
                    for (int y = G + 2; y <= G + 3; y++) {
                        put(w, lot, 2, y, hd, b("yellow_wallpaper"));
                        put(w, lot, 4, y, hd, b("yellow_wallpaper"));
                    }
                    put(w, lot, 3, G, hd, b("moist_carpet"));
                    put(w, lot, 3, G + 1, hd, ModBlocks.AIR);
                    put(w, lot, 3, G + 2, hd, ModBlocks.AIR);
                    put(w, lot, 3, G + 3, hd, hd == 10 ? b("fluorescent_light") : b("ceiling_tile"));
                    for (int hx = 2; hx <= 4; hx++) put(w, lot, hx, G + 4, hd, b("void_filler"));
                }
                put(w, lot, 3, G + 1, 12, b("dark_doorway"));
                put(w, lot, 3, G + 2, 12, b("dark_doorway"));
                for (int y = G + 1; y <= G + 4; y++) {
                    put(w, lot, 2, y, 12, b("yellow_wallpaper"));
                    put(w, lot, 4, y, 12, b("yellow_wallpaper"));
                }
                put(w, lot, 3, G + 3, 12, b("yellow_wallpaper"));
            } else if (r < 0.28f) {
                put(w, lot, 3, G + 1, 8, b("frontroom_closet_door"));
                put(w, lot, 3, G + 2, 8, b("frontroom_closet_door"));
            }
            if (Hash.unit(k >>> 40) < 0.12f) {
                // a hole in the living room floor with a ladder going down into the dark
                Direction toWall = Direction.WEST;
                for (int y = G - 6; y <= G; y++) put(w, lot, 5, y, 7, ModBlocks.AIR);
                for (int y = G - 6; y <= G; y++) put(w, lot, 6, y, 7, b("subfloor"));
                for (int y = G - 3; y <= G; y++)
                    put(w, lot, 5, y, 7, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, toWall));
                put(w, lot, 5, G - 6, 7, b("frontroom_basement_dark"));
                put(w, lot, 5, G - 5, 7, b("frontroom_basement_dark"));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Wherever you are not looking, the house remembers what it really is:
     * painted walls become yellow wallpaper, floors become damp carpet. The longer
     * you stay, the faster it goes. Eventually you hear the hum.
     */
    static final class Reversion implements LevelBehavior {
        private final Map<UUID, Integer> arrived = new HashMap<>();
        private final java.util.Set<UUID> warned = new java.util.HashSet<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            arrived.put(p.getUUID(), p.tickCount);
            warned.remove(p.getUUID());
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            int secs = (p.tickCount - arrived.computeIfAbsent(p.getUUID(), u -> p.tickCount)) / 20;
            var r = p.getRandom();
            // indoors, a clock is always ticking somewhere
            if (p.tickCount % 80 == 0 && !level.getBlockState(p.blockPosition().above(3)).isAir()) {
                level.playSound(null, p.blockPosition(), ModSounds.get("event.frontrooms_tick"), SoundSource.AMBIENT, 0.35f, 1f);
            }
            if (secs < 40 || p.tickCount % 30 != 0) return;
            int count = Math.min(8, 1 + secs / 60);
            for (int n = 0; n < count; n++) {
                BlockPos at = p.blockPosition().offset(r.nextInt(25) - 12, r.nextInt(5) - 1, r.nextInt(25) - 12);
                if (at.getY() < G || at.getY() > G + 4) continue;
                BlockState st = level.getBlockState(at);
                BlockState to = revert(st, at.getY());
                if (to == null) continue;
                if (DeepEndUtil.seen(p, Vec3.atCenterOf(at), 0.35)) continue;
                level.setBlock(at, to, Block.UPDATE_CLIENTS);
            }
            if (secs >= 300 && warned.add(p.getUUID())) {
                p.sendSystemMessage(Component.literal("Under the birdsong, very faintly, you can hear fluorescent lights.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                level.playSound(null, p.blockPosition(), ModSounds.get("ambient.level_0"), SoundSource.AMBIENT, 0.4f, 1f);
                Sanity.add(p, -10f);
            }
            if (secs > 540 && !p.isCreative()) {
                p.sendSystemMessage(Component.literal("You were never really home.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                arrived.remove(p.getUUID());
                Travel.sendTo(p, "level_0");
            }
        }

        static BlockState revert(BlockState st, int y) {
            String n = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
            return switch (n) {
                case "frontroom_wall", "frontroom_wall_switch" -> ModBlocks.state(y == G + 1 ? "yellow_wallpaper_baseboard" : "yellow_wallpaper");
                case "frontroom_carpet", "frontroom_floorboards", "frontroom_kitchen_tile" -> ModBlocks.state("moist_carpet");
                case "frontroom_ceiling" -> ModBlocks.state("ceiling_tile");
                case "frontroom_lawn" -> y == G ? ModBlocks.state("damp_carpet") : null;
                default -> null;
            };
        }
    }
}
