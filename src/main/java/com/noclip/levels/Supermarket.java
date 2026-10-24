package com.noclip.levels;

import com.noclip.entity.NightStocker;
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
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Night Supermarket. Aisles forever, under hanging strip lights, numbered by how
 * far they are from the front of the store, where the automatic doors are. The
 * intercom knows your name. Sometimes it tells you which aisle you are standing
 * in, and a little later it announces a clean-up there: the lights in that aisle
 * go out and someone from the night shift comes to deal with it.
 */
public final class Supermarket {
    static final int FLOOR = 10, CEIL = FLOOR + 9, LIGHT_Y = FLOOR + 7, PFX = 128, FRONT = 24, PZ = 40, RUN = 34;

    private Supermarket() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("supermarket")
                .number("").title("Night Supermarket").order(1040)
                .tagline("Attention shoppers. We can see you.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Intercom())
                .visuals(new Visuals().tint(0.98f, 1.02f, 1.04f).saturation(0.95f).contrast(1.1f).brightness(1.15f)
                        .grain(0.06f).aberration(0.0014f).vignette(0.5f).fog(6f, 64f).fogColor(0x1C1E20).bloom(0.5f))
                .sanityDrain(0.03f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.45f).dark().dist(12, 26).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(28, 46).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("night_stocker"), 1, 0.04f).dist(24, 40).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "dead_mall", 2).exit(ExitType.NOCLIP, "cold_storage", 1)
                .exit(ExitType.NOCLIP, "waiting_room", 1)
                .exit(ExitType.DOOR, "parking_garage", 3).exit(ExitType.DOOR, "parking_lot", 2).exit(ExitType.DOOR, "dead_mall", 2)
                .exit(ExitType.DOOR, "gas_station", 1).exit(ExitType.DOOR, "level_11", 1)
                .build();
    }

    /** Aisle number for a column: 1 is next to the front, counting up as you go further in. */
    static int aisleNumber(int x) {
        int fx = Math.floorMod(x, PFX);
        if (fx < FRONT) return 0;
        int ai = (fx - FRONT) / 4;
        int n = (PFX - FRONT) / 4;
        return Math.min(ai + 1, n - ai);
    }

    /** True if (x, z) is inside an aisle (between shelves, not a cross aisle or the front). */
    static boolean inAisle(int x, int z) {
        int fx = Math.floorMod(x, PFX);
        return fx >= FRONT && Math.floorMod(x, 4) != 0 && Math.floorMod(z, PZ) < RUN;
    }

    static final String[] KINDS = {"cans", "cereal", "bottles", "bags", "jugs"};

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("smkt_floor");
        final BlockState accent = ModBlocks.state("smkt_floor_accent");
        final BlockState ceil = ModBlocks.state("smkt_ceiling");
        final BlockState wall = ModBlocks.state("smkt_wall");
        final BlockState over = ModBlocks.state("smkt_overstock");
        final BlockState endcap = ModBlocks.state("smkt_endcap");
        final BlockState empty = ModBlocks.state("smkt_shelf_empty");
        final BlockState fzTop = ModBlocks.state("smkt_freezer_top");
        final BlockState fzMid = ModBlocks.state("smkt_freezer_mid");
        final BlockState fzLow = ModBlocks.state("smkt_freezer_low");
        final BlockState window = ModBlocks.state("smkt_window");
        final BlockState door = ModBlocks.state("smkt_auto_door");
        final BlockState light = ModBlocks.state("smkt_light");
        final BlockState lightOff = ModBlocks.state("smkt_light_off");
        final BlockState cart = ModBlocks.state("smkt_cart");
        final BlockState cartFull = ModBlocks.state("smkt_cart_full");
        final BlockState checkout = ModBlocks.state("smkt_checkout");
        final BlockState register = ModBlocks.state("smkt_register");
        final BlockState laneOpen = ModBlocks.state("smkt_lane_light_open");
        final BlockState unknown = ModBlocks.state("smkt_aisle_unknown");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState boxes = ModBlocks.state("cardboard_boxes");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState chain = Blocks.CHAIN.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] shelves = new BlockState[KINDS.length];
        final BlockState[] signs = new BlockState[14];
        final BlockState[] lanes = new BlockState[9];
        final BlockState[] bins = {ModBlocks.state("smkt_bin_apples"), ModBlocks.state("smkt_bin_oranges"),
                ModBlocks.state("smkt_bin_limes"), ModBlocks.state("smkt_bin_rotten")};

        Gen() {
            for (int i = 0; i < KINDS.length; i++) shelves[i] = ModBlocks.state("smkt_shelf_" + KINDS[i]);
            for (int n = 1; n < 14; n++) signs[n] = ModBlocks.state("smkt_aisle_" + n);
            for (int n = 1; n < 9; n++) lanes[n] = ModBlocks.state("smkt_lane_light_" + n);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceil);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int fx = Math.floorMod(x, PFX), v = Math.floorMod(z, PZ);
                    if (fx < FRONT) front(w, s, x, z, fx);
                    else aisles(w, s, x, z, v);
                }
            int a0 = Math.floorDiv(w.minX, 4) - 1, a1 = Math.floorDiv(w.maxX, 4) + 1;
            for (int a = a0; a <= a1; a++)
                for (int sr = Math.floorDiv(w.minZ, PZ) - 1; sr <= Math.floorDiv(w.maxZ, PZ) + 1; sr++) aisleDressing(w, s, a, sr);
        }

        void hangLight(ChunkWriter w, long s, int x, int z) {
            w.set(x, LIGHT_Y, z, Hash.chance(s, x, z, 0x1E, 0.07f) ? lightOff : light);
            w.fill(x, LIGHT_Y + 1, z, x, CEIL - 1, z, chain);
        }

        void aisles(ChunkWriter w, long s, int x, int z, int v) {
            int u = Math.floorMod(x, 4);
            if (v >= RUN) {
                if (v == RUN || v == PZ - 1) w.set(x, FLOOR, z, accent);
                if (u == 2 && v == RUN + 3 && Math.floorMod(x, 8) == 2) hangLight(w, s, x, z);
                return;
            }
            if (u != 0) {
                if (u == 2 && Math.floorMod(v, 6) == 3) hangLight(w, s, x, z);
                return;
            }
            int si = Math.floorDiv(x, 4), sr = Math.floorDiv(z, PZ);
            long h = Hash.hash(s, si, sr, 0x5E1F);
            float dept = Hash.unit(h);
            int y = FLOOR + 1;
            if (dept < 0.12f) {
                w.set(x, y, z, fzLow);
                w.set(x, y + 1, z, fzMid);
                w.set(x, y + 2, z, fzTop);
                return;
            }
            if (dept < 0.16f) {
                w.set(x, y, z, bins[Hash.range(s, x, z, 4) == 0 && Hash.unit(h >>> 5) < 0.3f ? 3 : Hash.range(h >>> 9, 3)]
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                return;
            }
            boolean stripped = dept > 0.93f;
            int kind = Hash.range(h >>> 3, KINDS.length);
            if (v == 0 || v == RUN - 1) {
                w.set(x, y, z, stripped ? empty : shelves[kind]);
                w.set(x, y + 1, z, stripped ? empty : shelves[(kind + 1) % KINDS.length]);
                w.set(x, y + 2, z, endcap);
                return;
            }
            for (int dy = 0; dy < 3; dy++) {
                float r = Hash.unit(s, x, z, 40 + dy);
                BlockState b = stripped || r < 0.04f ? empty : r < 0.12f ? shelves[Hash.range(s, x, z, 50 + dy, KINDS.length)] : shelves[kind];
                w.set(x, y + dy, z, b);
            }
            if (Hash.chance(s, x, z, 60, 0.35f)) w.set(x, y + 3, z, over);
        }

        void front(ChunkWriter w, long s, int x, int z, int fx) {
            int y = FLOOR + 1;
            boolean west = fx < 12;
            int f = west ? fx : 23 - fx;  // distance from the store side of the front zone
            int lane = Math.floorDiv(z, 5);
            if (f == 0 || f == 1 || f == 2 || f == 3) {
                if (f == 2 && Math.floorMod(z, 6) == 3) hangLight(w, s, x, z);
                if (f == 3 && Hash.chance(s, x, z, 0xCA, 0.01f)) w.set(x, y, z, cart.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x, z, 4))));
                return;
            }
            if (f >= 4 && f <= 6) {
                if (Math.floorMod(z, 5) == 0) {
                    Direction face = west ? Direction.EAST : Direction.WEST;
                    if (f == 6) w.set(x, y, z, register.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    else w.set(x, y, z, checkout.setValue(HorizontalDirectionalBlock.FACING, face));
                    if (f == 4) {
                        boolean open = Hash.chance(s, lane, west ? 1 : 2, 0x0E, 0.06f);
                        w.set(x, y + 1, z, (open ? laneOpen : lanes[Math.floorMod(lane, 8) + 1]).setValue(HorizontalDirectionalBlock.FACING, face));
                    }
                }
                return;
            }
            if (f >= 7 && f <= 10) {
                if (f == 8 && Math.floorMod(z, 6) == 3) hangLight(w, s, x, z);
                int c = Math.floorMod(z, 24);
                if (f == 10 && c >= 2 && c <= 5) w.set(x, y, z, cart.setValue(HorizontalDirectionalBlock.FACING, west ? Direction.EAST : Direction.WEST));
                if (f == 10 && (c == 9 || c == 12)) w.set(x, y + 3, z, exitSign.setValue(HorizontalDirectionalBlock.FACING, west ? Direction.EAST : Direction.WEST));
                return;
            }
            // the front wall: dark glass, automatic doors every 24 blocks
            int c = Math.floorMod(z, 24);
            if (c == 10 || c == 11) {
                w.fill(x, y, z, x, y + 2, z, door);
                w.fill(x, y + 3, z, x, CEIL - 1, z, wall);
            } else {
                w.fill(x, y, z, x, y + 4, z, window);
                w.fill(x, y + 5, z, x, CEIL - 1, z, wall);
            }
        }

        /** Per aisle segment: its hanging number sign and the odd thing left behind. */
        void aisleDressing(ChunkWriter w, long s, int a, int sr) {
            int x0 = a * 4;
            if (Math.floorMod(x0, PFX) < FRONT) return;
            int z0 = sr * PZ;
            int n = aisleNumber(x0 + 2);
            long h = Hash.hash(s, a, sr, 0xA15);
            BlockState sign = Hash.unit(h) < 0.02f ? unknown : signs[Math.max(1, Math.min(13, n))];
            w.set(x0 + 2, FLOOR + 6, z0 + 16, sign);
            w.fill(x0 + 2, FLOOR + 7, z0 + 16, x0 + 2, CEIL - 1, z0 + 16, chain);
            float r = Hash.unit(h >>> 8);
            int y = FLOOR + 1;
            int zz = z0 + 3 + Hash.range(h >>> 16, RUN - 6);
            int xx = x0 + 1 + Hash.range(h >>> 24, 3);
            Direction d = Direction.from2DDataValue(Hash.range(h >>> 28, 4));
            if (r < 0.03f) w.set(xx, y, zz, cart.setValue(HorizontalDirectionalBlock.FACING, d));
            else if (r < 0.045f) w.set(xx, y, zz, cartFull.setValue(HorizontalDirectionalBlock.FACING, d));
            else if (r < 0.07f) {
                for (int k = 0; k < 4; k++) w.set(x0 + 1 + (k % 3), y, zz + k, puddle);
            } else if (r < 0.085f) {
                w.set(xx, y, zz, boxes.setValue(HorizontalDirectionalBlock.FACING, d));
                w.set(xx, y, zz + 1, boxes.setValue(HorizontalDirectionalBlock.FACING, d.getClockWise()));
            } else if (r < 0.1f) {
                w.set(xx, y, zz, bottle.setValue(HorizontalDirectionalBlock.FACING, d));
            }
        }
    }

    // ---------------------------------------------------------------- the intercom

    record Cleanup(int aisle, int section, int number, long at) {}

    static final class Intercom implements LevelBehavior {
        private final Map<UUID, Long> next = new ConcurrentHashMap<>();
        private final Map<UUID, Cleanup> pending = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            next.put(p.getUUID(), level.getGameTime() + 20 * (35 + p.getRandom().nextInt(40)));
            pending.remove(p.getUUID());
        }

        static void say(ServerPlayer p, String text) {
            p.sendSystemMessage(Component.literal("§3[INTERCOM] §f" + text));
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 10 != 0) return;
            long now = level.getGameTime();
            String name = p.getName().getString();
            Cleanup c = pending.get(p.getUUID());
            if (c != null && now >= c.at()) {
                pending.remove(p.getUUID());
                cleanup(p, level, c);
            }
            Long due = next.get(p.getUUID());
            if (due == null) {
                next.put(p.getUUID(), now + 20 * 40);
                return;
            }
            if (now < due) return;
            next.put(p.getUUID(), now + 20 * (70 + p.getRandom().nextInt(80)));
            float r = p.getRandom().nextFloat();
            int x = p.getBlockX(), z = p.getBlockZ();
            if (r < 0.6f && inAisle(x, z) && !p.isCreative()) {
                int n = aisleNumber(x);
                say(p, name + ", we can see you in aisle " + n + ".");
                p.playNotifySound(ModSounds.get("event.supermarket_intercom"), SoundSource.AMBIENT, 1f, 1f);
                pending.put(p.getUUID(), new Cleanup(Math.floorDiv(x, 4), Math.floorDiv(z, PZ), n, now + 20 * 12));
            } else if (r < 0.85f) {
                say(p, "Attention shoppers. " + name + ", please come to the front of the store.");
                p.playNotifySound(ModSounds.get("event.supermarket_intercom"), SoundSource.AMBIENT, 1f, 1f);
            } else {
                say(p, name + ". " + name + ". We know you're still in here.");
                p.playNotifySound(ModSounds.get("event.supermarket_intercom"), SoundSource.AMBIENT, 1f, 0.85f);
                Sanity.add(p, -3f);
            }
        }

        void cleanup(ServerPlayer p, ServerLevel level, Cleanup c) {
            say(p, "Clean-up on aisle " + c.number() + ".");
            p.playNotifySound(ModSounds.get("event.supermarket_cleanup"), SoundSource.AMBIENT, 1f, 1f);
            int x0 = c.aisle() * 4, z0 = c.section() * PZ;
            BlockState off = ModBlocks.state("smkt_light_off");
            Block on = ModBlocks.get("smkt_light");
            for (int x = x0 + 1; x <= x0 + 3; x++)
                for (int z = z0; z < z0 + RUN; z++) {
                    BlockPos pos = new BlockPos(x, LIGHT_Y, z);
                    if (level.getBlockState(pos).is(on)) level.setBlock(pos, off, Block.UPDATE_CLIENTS);
                }
            boolean stillHere = Math.floorDiv(p.getBlockX(), 4) == c.aisle() && Math.floorDiv(p.getBlockZ(), PZ) == c.section()
                    && inAisle(p.getBlockX(), p.getBlockZ());
            // the night shift arrives from whichever end of the aisle is further from you
            int far = Math.abs(p.getBlockZ() - z0) > Math.abs(p.getBlockZ() - (z0 + RUN - 1)) ? z0 + 1 : z0 + RUN - 2;
            BlockPos at = new BlockPos(x0 + 2, FLOOR + 1, far);
            if (!level.isLoaded(at) || !level.getBlockState(at).isAir()) return;
            NightStocker st = NightStocker.TYPE.create(level);
            if (st == null) return;
            st.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, far == z0 + 1 ? 0f : 180f, 0f);
            st.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            level.addFreshEntity(st);
            if (stillHere && !p.isCreative() && !p.isSpectator()) {
                st.setTarget(p);
                Sanity.add(p, -5f);
            }
        }
    }
}
