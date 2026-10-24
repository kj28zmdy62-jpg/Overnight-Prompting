package com.noclip.levels;

import com.noclip.block.AlarmBeaconBlock;
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
import com.noclip.world.CellGrid;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server Farm. Hall after hall of racks on a raised floor, cold aisles under
 * hard white panels, blinking LEDs, fans loud enough to make thinking hard. Glass
 * corridors run between the halls. Every few minutes the fire suppression in
 * your hall arms itself: thirty seconds of klaxon, then the halon dumps and the
 * air in that hall stops being air. Get into a corridor. And somewhere a console
 * is logged in under your name.
 */
public final class ServerFarm {
    static final int TILE = 11, CEIL = 17, CELL = 40;

    private ServerFarm() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("server_farm")
                .number("").title("Server Farm").order(2000)
                .tagline("When the klaxon sounds, you have thirty seconds to leave the hall.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Halon())
                .visuals(new Visuals().tint(0.90f, 1.0f, 1.12f).saturation(0.72f).contrast(1.1f).brightness(1.12f)
                        .grain(0.06f).aberration(0.0013f).vignette(0.45f).fog(4f, 42f).fogColor(0xA9B6C2).bloom(0.55f)
                        .scanlines(0.06f))
                .sanityDrain(0.03f)
                .spawnY(TILE + 1, TILE + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.25f).dist(18, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.08f).seen().dist(24, 38).weight(1))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.5f).dark().dist(12, 24).weight(2))
                .exit(ExitType.NOCLIP, "level_3", 3).exit(ExitType.NOCLIP, "air_vents", 1).exit(ExitType.NOCLIP, "call_center", 1)
                .exit(ExitType.DOOR, "level_3", 2).exit(ExitType.DOOR, "power_plant", 2).exit(ExitType.DOOR, "air_vents", 1)
                .exit(ExitType.FALL, "air_vents", 1).exit(ExitType.FALL, "level_3", 1)
                .build();
    }

    static CellGrid grid(long s) {
        return new CellGrid(s, CELL, 0);
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final BlockState[] racks = {ModBlocks.state("srv_rack_a"), ModBlocks.state("srv_rack_b"), ModBlocks.state("srv_rack_c")};
        final BlockState redRack = ModBlocks.state("srv_rack_red");
        final BlockState console = ModBlocks.state("srv_console");
        final BlockState tile = ModBlocks.state("srv_floor_tile");
        final BlockState scuffed = ModBlocks.state("srv_floor_scuffed");
        final BlockState solid = ModBlocks.state("srv_floor_solid");
        final BlockState under = ModBlocks.state("srv_underfloor");
        final BlockState wall = ModBlocks.state("srv_wall");
        final BlockState signHall = ModBlocks.state("srv_sign_hall");
        final BlockState signCold = ModBlocks.state("srv_sign_cold");
        final BlockState signHalon = ModBlocks.state("srv_sign_halon");
        final BlockState glass = ModBlocks.state("srv_glass");
        final BlockState ceiling = ModBlocks.state("srv_ceiling");
        final BlockState led = ModBlocks.state("srv_led");
        final BlockState ledOff = ModBlocks.state("srv_led_off");
        final BlockState tray = ModBlocks.state("srv_tray");
        final BlockState door = ModBlocks.state("srv_exit_door");
        final BlockState beacon = ModBlocks.state("srv_beacon");
        final BlockState pit = ModBlocks.state("bottomless_pit");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            CellGrid g = grid(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, TILE - 2, w.maxZ, sub);
            w.layer(TILE - 1, under);
            w.layer(CEIL, ceiling);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, g, s, x, z);
            }
        }

        static BlockState face(BlockState b, Direction d) {
            return b.setValue(HorizontalDirectionalBlock.FACING, d);
        }

        void column(ChunkWriter w, CellGrid g, long s, int x, int z) {
            int i = g.cellX(x), k = g.cellZ(z);
            int lx = x - g.x0(i), lz = z - g.z0(k);
            long hall = g.hash(i, k, 1);
            boolean dark = Hash.unit(hall) < 0.14f;
            boolean failing = Hash.unit(hall >>> 10) < 0.06f;
            // corridors: three wide, solid tiles, lit every few blocks
            if (lx < 3 || lz < 3) {
                w.set(x, TILE, z, solid);
                if (lx == 1 && lz == 1) {
                    for (int y = TILE + 1; y < CEIL; y++) w.set(x, y, z, y == TILE + 4 ? signHall : wall);
                    if (Hash.chance(s, i, k, 3, 0.3f)) {
                        w.set(x, TILE + 1, z, door);
                        w.set(x, TILE + 2, z, door);
                    }
                    return;
                }
                if ((lx == 1 && Math.floorMod(lz, 5) == 2) || (lz == 1 && Math.floorMod(lx, 5) == 2)) w.set(x, CEIL, z, led);
                return;
            }
            // glass walls with a doorway into each hall
            if (lx == 3 || lz == 3) {
                w.set(x, TILE, z, solid);
                int along = lx == 3 ? lz : lx;
                boolean doorway = along >= 18 && along <= 20;
                for (int y = TILE + 1; y < CEIL; y++) {
                    if (doorway && y <= TILE + 3) continue;
                    if (doorway) w.set(x, y, z, y == TILE + 4 && along == 19 ? signHalon : wall);
                    else if (lx == 3 && lz == 3) w.set(x, y, z, wall);
                    else w.set(x, y, z, glass.setValue(lx == 3 ? IronBarsBlock.NORTH : IronBarsBlock.EAST, true)
                                .setValue(lx == 3 ? IronBarsBlock.SOUTH : IronBarsBlock.WEST, true));
                }
                return;
            }
            // hall interior: raised floor, rows of racks
            float r = Hash.unit(s, x, z, 7);
            int row = Math.floorMod(lz - 6, 6);
            boolean inRows = lz >= 6 && lz <= CELL - 4 && lx >= 6 && lx <= CELL - 3 && !(lx >= 21 && lx <= 23);
            boolean rackA = inRows && row == 0, rackB = inRows && row == 3;
            boolean coldAisle = row == 1 || row == 2;
            w.set(x, TILE, z, r < 0.08f ? scuffed : tile);
            if (!rackA && !rackB && r > 0.9965f && lz > 4) {
                w.set(x, TILE, z, ModBlocks.AIR);
                if (Hash.chance(s, i, k, 4, 0.12f) && r > 0.9992f) w.set(x, TILE - 1, z, pit);
            }
            if (coldAisle && Math.floorMod(lx, 3) == 0 && lz >= 6) w.set(x, CEIL, z, dark ? ledOff : led);
            if (!coldAisle && lz >= 6 && Math.floorMod(lx, 9) == 4 && !dark) w.set(x, CEIL, z, led);
            if (rackA || rackB) {
                // fronts face the cold aisle between each pair of rows
                Direction f = rackA ? Direction.NORTH : Direction.SOUTH;
                long h = Hash.hash(s, x, z, 8);
                float q = Hash.unit(h);
                BlockState rk = failing || q < 0.012f ? redRack : racks[Hash.range(h >>> 8, racks.length)];
                w.set(x, TILE + 1, z, face(rk, f));
                w.set(x, TILE + 2, z, face(q > 0.996f ? console : rk, f));
                w.set(x, TILE + 4, z, face(tray, Direction.EAST));
                if ((lx == 6 || lx == CELL - 3) && row == 0) w.set(x, TILE + 3, z, beacon);
            }
            if (lx == 4 && lz == 4) {
                for (int y = TILE + 1; y < CEIL; y++) w.set(x, y, z, y == TILE + 3 ? signHalon : y == TILE + 2 ? signCold : wall);
            }
            // a chair left facing a rack; a bottle someone put down
            long hh = Hash.hash(s, x, z, 9);
            if (coldAisle && inRows && Hash.unit(hh) < 0.0025f)
                w.set(x, TILE + 1, z, face(chair, row == 1 ? Direction.SOUTH : Direction.NORTH));
            else if (coldAisle && inRows && Hash.unit(hh) < 0.0035f) w.set(x, TILE + 1, z, bottle);
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The halon alarm, and the console that knows your name. */
    static final class Halon implements LevelBehavior {
        /** {phase 0 idle / 1 armed / 2 discharge, phase end, cell i, cell k, next alarm} */
        private static final Map<ResourceKey<Level>, long[]> STATE = new ConcurrentHashMap<>();
        private static final Map<ResourceKey<Level>, Set<Long>> SHOWN = new ConcurrentHashMap<>();

        static long[] state(ServerLevel level) {
            return STATE.computeIfAbsent(level.dimension(), k -> new long[]{0, 0, 0, 0, level.getGameTime() + 1800 + level.random.nextInt(1800)});
        }

        static boolean inHall(ServerLevel level, long[] st, double px, double pz) {
            CellGrid g = grid(Hash.hash(level.getSeed(), Hash.salt("server_farm")));
            int x = (int) Math.floor(px), z = (int) Math.floor(pz);
            int i = g.cellX(x), k = g.cellZ(z);
            return i == st[2] && k == st[3] && x - g.x0(i) >= 4 && z - g.z0(k) >= 4;
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            long now = level.getGameTime();
            if (st[0] == 0) {
                if (now < st[4] || level.players().isEmpty()) return;
                ServerPlayer target = level.players().get(level.random.nextInt(level.players().size()));
                CellGrid g = grid(Hash.hash(level.getSeed(), Hash.salt("server_farm")));
                st[2] = g.cellX(target.getBlockX());
                st[3] = g.cellZ(target.getBlockZ());
                st[0] = 1;
                st[1] = now + 600;
                beacons(level, st, true);
                return;
            }
            if (now % 40 == 0 && st[0] == 1) {
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.server_farm_alarm"), SoundSource.BLOCKS, 1.3f, 1f);
                    if (inHall(level, st, p.getX(), p.getZ())) {
                        long left = (st[1] - now) / 20;
                        p.displayClientMessage(Component.literal("HALON DISCHARGE IN " + left + " S. LEAVE THE HALL.")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                    }
                }
            }
            if (now < st[1]) return;
            if (st[0] == 1) {
                st[0] = 2;
                st[1] = now + 500;
                for (ServerPlayer p : level.players()) {
                    level.playSound(null, p.blockPosition(), ModSounds.get("event.server_farm_discharge"), SoundSource.BLOCKS, 1.8f, 1f);
                    ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.9f));
                }
            } else {
                beacons(level, st, false);
                st[0] = 0;
                st[4] = now + 3000 + level.random.nextInt(2400);
            }
        }

        private static void beacons(ServerLevel level, long[] st, boolean on) {
            CellGrid g = grid(Hash.hash(level.getSeed(), Hash.salt("server_farm")));
            int x0 = g.x0((int) st[2]), z0 = g.z0((int) st[3]);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = x0 + 4; x < x0 + CELL; x++) {
                for (int z = z0 + 4; z < z0 + CELL; z++) {
                    m.set(x, TILE + 3, z);
                    if (!level.isLoaded(m)) continue;
                    BlockState b = level.getBlockState(m);
                    if (b.getBlock() instanceof AlarmBeaconBlock && b.getValue(AlarmBeaconBlock.LIT) != on)
                        level.setBlock(m, b.setValue(AlarmBeaconBlock.LIT, on), Block.UPDATE_CLIENTS);
                }
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long[] st = state(level);
            if (st[0] == 2 && !p.isSpectator() && inHall(level, st, p.getX(), p.getZ())) {
                if (p.tickCount % 3 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 6, 2.5, 1.0, 2.5, 0.02);
                }
                if (!p.isCreative() && p.tickCount % 20 == 0) {
                    p.hurt(level.damageSources().drown(), 2.5f);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
                    Sanity.add(p, -2f);
                    p.displayClientMessage(Component.literal("You can't breathe.").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
                }
            }
            if ((p.tickCount + p.getId()) % 40 == 0) consoles(p, level);
        }

        /** A console within reach logs in as you. */
        private static void consoles(ServerPlayer p, ServerLevel level) {
            Set<Long> shown = SHOWN.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
            Block console = ModBlocks.get("srv_console");
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = p.getBlockX() - 12; x <= p.getBlockX() + 12; x++) {
                for (int z = p.getBlockZ() - 12; z <= p.getBlockZ() + 12; z++) {
                    m.set(x, TILE + 2, z);
                    BlockState b = level.getBlockState(m);
                    if (!b.is(console) || shown.contains(m.asLong())) continue;
                    shown.add(m.asLong());
                    showName(level, m.immutable(), b.getValue(HorizontalDirectionalBlock.FACING).getOpposite(), p);
                }
            }
        }

        private static void showName(ServerLevel level, BlockPos pos, Direction front, ServerPlayer p) {
            Display.TextDisplay td = EntityType.TEXT_DISPLAY.create(level);
            if (td == null) return;
            String name = p.getGameProfile().getName().toUpperCase();
            long days = level.getGameTime() / 24000 + 1;
            MutableComponent text = Component.literal("> SESSION OPEN\n").withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal("> USER: " + name + "\n").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("> SHIFT DAY " + days + "\n").withStyle(ChatFormatting.DARK_GREEN))
                    .append(Component.literal("> DO NOT LOG OFF").withStyle(ChatFormatting.GREEN));
            CompoundTag tag = new CompoundTag();
            tag.putString("text", Component.Serializer.toJson(text, level.registryAccess()));
            tag.putInt("background", 0);
            tag.putInt("line_width", 200);
            tag.putString("billboard", "fixed");
            tag.putString("alignment", "left");
            CompoundTag bright = new CompoundTag();
            bright.putInt("sky", 15);
            bright.putInt("block", 15);
            tag.put("brightness", bright);
            CompoundTag tr = new CompoundTag();
            tr.put("translation", floats(0, 0, 0));
            tr.put("left_rotation", floats(0, 0, 0, 1));
            tr.put("scale", floats(0.16f, 0.16f, 0.16f));
            tr.put("right_rotation", floats(0, 0, 0, 1));
            tag.put("transformation", tr);
            td.load(tag);
            double x = pos.getX() + 0.5 + front.getStepX() * 0.52, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
            td.moveTo(x, pos.getY() + 0.22, z, front.toYRot(), 0f);
            level.addFreshEntity(td);
        }

        private static ListTag floats(float... v) {
            ListTag l = new ListTag();
            for (float f : v) l.add(FloatTag.valueOf(f));
            return l;
        }
    }
}
