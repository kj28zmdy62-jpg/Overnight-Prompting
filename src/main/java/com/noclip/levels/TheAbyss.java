package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Abyss. You arrive falling and you never stop. Pieces of other levels drift
 * past in the dark: a lit corner of the Lobby, a slab of pool tile, a hotel door
 * with its plaque, a birthday table. Try to land on one and it crumbles. And far
 * below, getting no closer until it suddenly is, something is looking up.
 */
public final class TheAbyss {
    static final String ID = "the_abyss";
    static final int CELL = 22, VCELL = 26, LOW = 130, HIGH = 362, WRAP = 200, FALLS = 6;

    private TheAbyss() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Abyss").order(9010)
                .tagline("Landing is impossible. The bottom is not.")
                .danger(BackroomsLevel.Danger.HIGH)
                .generator(Gen::new)
                .behavior(new Fall())
                .visuals(new Visuals().darkness(0.8f).grain(0.08f).aberration(0.003f).vignette(0.7f).warp(0.05f)
                        .saturation(0.8f).bloom(0.8f).fog(18f, 120f).fogColor(0x05060A))
                .sanityDrain(0.06f)
                .spawnY(60, 370)
                .exit(ExitType.FALL, "the_void", 4).exit(ExitType.FALL, "level_0", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> c = new HashMap<>();

        BlockState b(String n) {
            return c.computeIfAbsent(n, ModBlocks::state);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            // the bottom: one enormous eye per 96 blocks, looking up
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    double ex = Math.floorMod(x, 96) - 47.5, ez = Math.floorMod(z, 96) - 47.5;
                    double d = Math.sqrt(ex * ex + ez * ez * 1.9);
                    BlockState st = d < 7 ? b("abyss_eye_pupil") : d < 17 ? b("abyss_eye_iris")
                            : d < 44 ? b("abyss_eye_sclera") : b("abyss_eye_flesh");
                    w.set(x, 1, z, st);
                    w.set(x, 0, z, b("void_filler"));
                }
            }
            int cx0 = Math.floorDiv(w.minX, CELL) - 1, cx1 = Math.floorDiv(w.maxX, CELL) + 1;
            int cz0 = Math.floorDiv(w.minZ, CELL) - 1, cz1 = Math.floorDiv(w.maxZ, CELL) + 1;
            for (int cx = cx0; cx <= cx1; cx++)
                for (int cz = cz0; cz <= cz1; cz++)
                    for (int cy = LOW / VCELL + 1; cy * VCELL < HIGH; cy++) {
                        long h = Hash.hash(s, cx * 4099L + cz, cy, 61);
                        if (Hash.unit(h) > 0.42f) continue;
                        int fx = cx * CELL + 3 + Hash.range(h >>> 8, CELL - 8);
                        int fz = cz * CELL + 3 + Hash.range(h >>> 16, CELL - 8);
                        int fy = cy * VCELL + Hash.range(h >>> 24, VCELL - 6);
                        fragment(w, Hash.range(h >>> 32, 9), h, fx, fy, fz);
                    }
        }

        BlockState f(String n, Direction d) {
            return b(n).setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void fragment(ChunkWriter w, int kind, long h, int x, int y, int z) {
            if (x + 8 < w.minX || x - 2 > w.maxX || z + 8 < w.minZ || z - 2 > w.maxZ) return;
            switch (kind) {
                case 0 -> { // a corner of the Lobby, still humming
                    for (int dx = 0; dx < 5; dx++)
                        for (int dz = 0; dz < 5; dz++) {
                            w.set(x + dx, y, z + dz, b("moist_carpet"));
                            if (dx == 0 || dz == 0) {
                                w.set(x + dx, y + 1, z + dz, b("yellow_wallpaper_baseboard"));
                                w.set(x + dx, y + 2, z + dz, b("yellow_wallpaper"));
                                w.set(x + dx, y + 3, z + dz, b("yellow_wallpaper"));
                            }
                        }
                    w.set(x + 2, y + 4, z + 2, b("flickering_fluorescent_light"));
                    if ((h & 1) == 0) w.set(x + 3, y + 1, z + 3, f("almond_water_bottle", Direction.NORTH));
                }
                case 1 -> { // a slab of the Poolrooms
                    for (int dx = 0; dx < 4; dx++)
                        for (int dz = 0; dz < 4; dz++) w.set(x + dx, y, z + dz, dx == 0 || dz == 3 ? b("pool_coping") : b("pool_deck_tile"));
                    w.set(x + 1, y - 1, z + 1, b("pool_underwater_light"));
                    w.set(x + 3, y + 1, z + 1, f("pool_lounger", Direction.WEST));
                }
                case 2 -> { // warehouse floor with crates
                    for (int dx = 0; dx < 6; dx++)
                        for (int dz = 0; dz < 3; dz++) w.set(x + dx, y, z + dz, b("warehouse_concrete"));
                    w.set(x + 1, y + 1, z + 1, b("wooden_crate"));
                    w.set(x + 1, y + 2, z + 1, b("wooden_crate"));
                    w.set(x + 2, y + 1, z + 1, b("wooden_crate"));
                    w.set(x + 4, y + 3, z + 1, b("strip_light"));
                    w.set(x + 4, y + 1, z + 2, f("supply_crate", Direction.NORTH));
                }
                case 3 -> { // a staircase going down into nothing
                    for (int n = 0; n < 7; n++)
                        for (int dz = 0; dz < 2; dz++)
                            w.set(x + n, y - n, z + dz, b("concrete_stairs").setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST));
                }
                case 4 -> { // a hotel door, plaque and all, standing on its own
                    for (int dx = -1; dx <= 1; dx++) {
                        w.set(x + dx, y, z, b("hub_parquet"));
                        if (dx != 0) for (int dy = 1; dy <= 3; dy++) w.set(x + dx, y + dy, z, b("hub_wallpaper"));
                    }
                    w.set(x, y + 1, z, b("hub_door_oak"));
                    w.set(x, y + 2, z, b("hub_door_oak_top"));
                    w.set(x, y + 3, z, b("hub_plaque_unmarked"));
                    w.set(x, y + 3, z + 1, f("hub_sconce", Direction.SOUTH));
                }
                case 5 -> { // somebody's birthday
                    for (int dx = 0; dx < 3; dx++)
                        for (int dz = 0; dz < 3; dz++) w.set(x + dx, y, z + dz, b("party_carpet"));
                    w.set(x + 1, y + 1, z + 1, f("party_table", Direction.NORTH));
                    w.set(x + 1, y + 2, z + 1, f("party_cake", Direction.SOUTH));
                    w.set(x, y + 3, z + 2, b("party_balloon_red_high"));
                    w.set(x + 2, y + 2, z, b("party_balloon_yellow_high"));
                }
                case 6 -> { // a streetlight and a square of pavement
                    for (int dx = 0; dx < 2; dx++)
                        for (int dz = 0; dz < 2; dz++) w.set(x + dx, y, z + dz, b("frontroom_sidewalk"));
                    for (int dy = 1; dy <= 3; dy++) w.set(x, y + dy, z, b("frontroom_street_lamp"));
                    w.set(x, y + 4, z, b("frontroom_street_lamp_head"));
                }
                case 7 -> { // a living room, sofa facing a TV full of static
                    for (int dx = 0; dx < 4; dx++)
                        for (int dz = 0; dz < 3; dz++) w.set(x + dx, y, z + dz, b("frontroom_carpet"));
                    w.set(x, y + 1, z + 1, f("frontroom_sofa", Direction.EAST));
                    w.set(x + 3, y + 1, z + 1, f("frontroom_tv_static", Direction.WEST));
                    w.set(x + 2, y + 1, z + 2, f("frontroom_floor_lamp", Direction.EAST));
                }
                default -> { // a cinder block wall with an exit sign over nothing
                    for (int dx = 0; dx < 4; dx++)
                        for (int dy = 0; dy < 4; dy++) w.set(x + dx, y + dy, z, b("cinder_block"));
                    w.set(x + 1, y + 2, z + 1, f("exit_sign", Direction.SOUTH));
                    w.set(x + 3, y + 3, z + 1, f("run_exit_sign", Direction.SOUTH));
                }
            }
        }
    }

    static final class Fall implements LevelBehavior {
        private final Map<UUID, Integer> falls = new HashMap<>();
        private final Map<UUID, Integer> grounded = new HashMap<>();
        static final String[] PASSING = {"You fall past a room you are sure you have been in.",
                "Something on a ledge watches you go by.", "You have been falling for a long time.",
                "There is a sound below you, like breathing, but slower.", "You stop being able to tell which way is up. Then you can again."};

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            falls.put(p.getUUID(), 0);
            grounded.put(p.getUUID(), 0);
            p.teleportTo(level, p.getX(), HIGH - 6, p.getZ(), p.getYRot(), 30f);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            UUID id = p.getUUID();
            p.resetFallDistance();
            Vec3 v = p.getDeltaMovement();
            if (v.y < -1.15 && !p.getAbilities().flying) {
                p.setDeltaMovement(v.x * 0.9, -1.1, v.z * 0.9);
                p.hurtMarked = true;
            }
            int n = falls.getOrDefault(id, 0);
            if (p.getY() < LOW && n < FALLS) {
                falls.put(id, n + 1);
                p.teleportTo(level, p.getX(), p.getY() + WRAP, p.getZ(), p.getYRot(), p.getXRot());
                if (n % 2 == 1) p.sendSystemMessage(Component.literal(PASSING[(n / 2) % PASSING.length]).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                return;
            }
            if (n >= FALLS && p.getY() < LOW && falls.get(id) == FALLS) {
                falls.put(id, FALLS + 1);
                level.playSound(null, p.blockPosition().below(30), ModSounds.get("event.the_abyss_blink"), SoundSource.AMBIENT, 4f, 0.7f);
                DeepEndUtil.title(p, Component.literal("It is looking up at you.").withStyle(ChatFormatting.DARK_RED), null, 10, 50, 20);
            }
            // you cannot land. Whatever you land on gives way.
            if (p.onGround() && p.getY() > 4) {
                int g = grounded.getOrDefault(id, 0) + 1;
                grounded.put(id, g);
                if (g > 25 && !p.isCreative()) {
                    BlockPos base = p.blockPosition().below();
                    for (int dx = -2; dx <= 2; dx++)
                        for (int dz = -2; dz <= 2; dz++)
                            for (int dy = -1; dy <= 0; dy++) level.setBlock(base.offset(dx, dy, dz), ModBlocks.AIR, Block.UPDATE_ALL);
                    level.playSound(null, base, net.minecraft.sounds.SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
                    grounded.put(id, 0);
                }
            } else {
                grounded.put(id, 0);
            }
        }
    }
}
