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
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parking Garage. Low concrete decks under sodium tubes, back-to-back rows of
 * parked cars, ramps up and down into the dark. Somewhere, always a minute's walk
 * away, a car alarm is going off. Hounds hunt by sound, and they are drawn to it.
 * The keys are in that car.
 */
public final class ParkingGarage {
    static final int FLOOR = 10, CEIL = FLOOR + 5, PZ = 16;
    static final String[] COLORS = {"maroon", "navy", "beige", "silver", "green", "white"};

    private ParkingGarage() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("parking_garage")
                .number("").title("Parking Garage").order(1050)
                .tagline("Level P3. A car alarm somewhere. It has been going for hours.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Alarm())
                .visuals(new Visuals().tint(1.1f, 0.98f, 0.82f).saturation(0.7f).contrast(1.12f).brightness(1.35f)
                        .grain(0.08f).aberration(0.0016f).vignette(0.55f).fog(5f, 46f).fogColor(0x2A2218).bloom(0.55f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 2, 0.15f).dist(24, 40).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.07f).seen().dist(28, 44).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "level_0", 1).exit(ExitType.NOCLIP, "level_1", 2).exit(ExitType.NOCLIP, "supermarket", 1)
                .exit(ExitType.NOCLIP, "dead_mall", 1)
                .exit(ExitType.SECRET, "endless_highway", 3).exit(ExitType.SECRET, "parking_lot", 2).exit(ExitType.SECRET, "motel", 1)
                .exit(ExitType.SECRET, "gas_station", 1)
                .exit(ExitType.DOOR, "stairwell", 3).exit(ExitType.DOOR, "level_1", 1).exit(ExitType.DOOR, "hospital", 1)
                .exit(ExitType.DOOR, "dead_mall", 1)
                .exit(ExitType.ASCEND, "dead_mall", 2).exit(ExitType.ASCEND, "rooftops", 2).exit(ExitType.ASCEND, "supermarket", 1)
                .exit(ExitType.DESCEND, "storm_drains", 1).exit(ExitType.DESCEND, "level_2", 1).exit(ExitType.DESCEND, "sewers", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("pg_floor");
        final BlockState oil = ModBlocks.state("pg_floor_oil");
        final BlockState line = ModBlocks.state("pg_floor_line");
        final BlockState hazard = ModBlocks.state("pg_floor_hazard");
        final BlockState pillar = ModBlocks.state("pg_pillar_p3");
        final BlockState pillarOdd = ModBlocks.state("pg_pillar_pm13");
        final BlockState ceil = ModBlocks.state("pg_ceiling");
        final BlockState beam = ModBlocks.state("pg_beam");
        final BlockState wall = ModBlocks.state("pg_wall");
        final BlockState light = ModBlocks.state("pg_light");
        final BlockState lightOff = ModBlocks.state("pg_light_off");
        final BlockState curb = ModBlocks.state("pg_curb");
        final BlockState door = ModBlocks.state("pg_stair_door");
        final BlockState doorTop = ModBlocks.state("pg_stair_door_top");
        final BlockState callBox = ModBlocks.state("pg_call_box");
        final BlockState signUp = ModBlocks.state("pg_sign_up");
        final BlockState signDown = ModBlocks.state("pg_sign_down");
        final BlockState ramp = ModBlocks.state("pg_ramp");
        final BlockState darkUp = ModBlocks.state("pg_dark_up");
        final BlockState darkDown = ModBlocks.state("pg_dark_down");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState barrier = Blocks.BARRIER.defaultBlockState();
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState[] cars = new BlockState[COLORS.length];

        Gen() {
            for (int i = 0; i < COLORS.length; i++) cars[i] = ModBlocks.state("pg_car_" + COLORS[i]);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(CEIL, ceil);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
            for (int sx = Math.floorDiv(w.minX, 3) - 1; sx <= Math.floorDiv(w.maxX, 3) + 1; sx++)
                for (int r = Math.floorDiv(w.minZ, PZ) - 1; r <= Math.floorDiv(w.maxZ, PZ) + 1; r++) {
                    stall(w, s, sx, r, true);
                    stall(w, s, sx, r, false);
                }
            for (int seg = Math.floorDiv(w.minX, 18) - 1; seg <= Math.floorDiv(w.maxX, 18) + 1; seg++)
                for (int r = Math.floorDiv(w.minZ, PZ) - 1; r <= Math.floorDiv(w.maxZ, PZ) + 1; r++) features(w, s, seg, r);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int v = Math.floorMod(z, PZ);
            boolean stallRow = (v >= 1 && v <= 5) || v >= 11;
            BlockState f = floor;
            float o = Hash.unit(s, x, z, 0x01);
            if (stallRow && Math.floorMod(x, 3) == 0) f = line;
            else if (o < 0.05f) f = oil;
            w.set(x, FLOOR, z, f);
            if (Math.floorMod(x, 9) == 0) w.set(x, CEIL - 1, z, beam);
            if (v == 0) {
                if (Math.floorMod(x, 9) == 0) {
                    w.set(x, FLOOR + 1, z, curb);
                    w.fill(x, FLOOR + 2, z, x, CEIL - 1, z, wall);
                    w.set(x, FLOOR + 3, z, Hash.chance(s, x, z, 0x13, 0.01f) ? pillarOdd : pillar);
                } else w.set(x, FLOOR + 1, z, curb);
                return;
            }
            if (v == 8 && Math.floorMod(x, 6) == 3) {
                float dark = Hash.noise(s ^ 0x6A, x / 40.0, z / 40.0);
                boolean on = Hash.unit(s, x, z, 0x17) > (dark > 0.7f ? 0.8f : 0.2f);
                w.set(x, CEIL - 1, z, (on ? light : lightOff).setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
            if (!stallRow && Hash.chance(s, x, z, 0x22, 0.006f)) w.set(x, FLOOR + 1, z, puddle);
        }

        void stall(ChunkWriter w, long s, int sx, int r, boolean north) {
            long h = Hash.hash(s, sx, r * 2 + (north ? 0 : 1), 0xCA2);
            if (Hash.unit(h) > 0.68f) return;
            int x = sx * 3 + 1, zc = r * PZ + (north ? 3 : 13);
            BlockState car = cars[Hash.range(h >>> 8, COLORS.length)]
                    .setValue(HorizontalDirectionalBlock.FACING, north ? Direction.NORTH : Direction.SOUTH);
            w.set(x, FLOOR + 1, zc, car);
            w.set(x, FLOOR + 1, zc - 1, barrier);
            w.set(x, FLOOR + 1, zc + 1, barrier);
        }

        void features(ChunkWriter w, long s, int seg, int r) {
            int x0 = seg * 18, z0 = r * PZ;
            long h = Hash.hash(s, seg, r, 0xFEA7);
            float f = Hash.unit(h);
            int y = FLOOR + 1;
            if (f < 0.025f) {
                // a ramp up through the deck, into black
                for (int st = 0; st < 4; st++)
                    for (int v = 7; v <= 9; v++) {
                        w.set(x0 + 4 + st, y + st, z0 + v, ramp.setValue(StairBlock.FACING, Direction.EAST));
                        w.fill(x0 + 4 + st, y, z0 + v, x0 + 4 + st, y + st - 1, z0 + v, floor);
                    }
                for (int v = 6; v <= 10; v += 4) w.fill(x0 + 3, y, z0 + v, x0 + 9, y, z0 + v, curb);
                for (int v = 7; v <= 9; v++) {
                    w.fill(x0 + 7, CEIL, z0 + v, x0 + 9, CEIL + 2, z0 + v, ModBlocks.AIR);
                    w.fill(x0 + 8, y + 4, z0 + v, x0 + 9, CEIL + 2, z0 + v, darkUp);
                    w.fill(x0 + 8, y, z0 + v, x0 + 9, y + 3, z0 + v, floor);
                }
                w.set(x0 + 3, CEIL - 1, z0 + 8, signUp);
            } else if (f < 0.045f) {
                // a ramp down
                for (int st = 0; st < 5; st++)
                    for (int v = 7; v <= 9; v++) {
                        w.fill(x0 + 4 + st, FLOOR - st, z0 + v, x0 + 4 + st, FLOOR, z0 + v, ModBlocks.AIR);
                        w.set(x0 + 4 + st, FLOOR - st, z0 + v, ramp.setValue(StairBlock.FACING, Direction.WEST));
                    }
                for (int v = 7; v <= 9; v++) w.fill(x0 + 9, FLOOR - 6, z0 + v, x0 + 10, FLOOR, z0 + v, darkDown);
                for (int v = 6; v <= 10; v += 4) w.fill(x0 + 4, y, z0 + v, x0 + 10, y, z0 + v, curb);
                w.set(x0 + 3, CEIL - 1, z0 + 8, signDown);
            } else if (f < 0.08f) {
                // a stair core in the spine with a door
                for (int x = x0 + 3; x <= x0 + 5; x++) w.fill(x, y, z0, x, CEIL - 1, z0, wall);
                w.set(x0 + 4, y, z0, door);
                w.set(x0 + 4, y + 1, z0, doorTop);
                w.set(x0 + 4, y + 2, z0 + 1, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                w.set(x0 + 4, y + 2, z0 - 1, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                // nobody parks in front of it
                for (int dz : new int[]{1, 2, 3, 4, -1, -2, -3, -4})
                    for (int dx = 3; dx <= 5; dx++) w.set(x0 + dx, y, z0 + dz, ModBlocks.AIR);
            } else if (f < 0.12f) {
                w.set(x0 + 9, y + 1, z0 + 1, callBox.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            }
        }
    }

    // ---------------------------------------------------------------- the alarm

    static final class Alarm implements LevelBehavior {
        private final Map<UUID, BlockPos> alarms = new ConcurrentHashMap<>();

        @Override
        public void onEnter(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            var r = p.getRandom();
            double ang = r.nextDouble() * Math.PI * 2;
            int dist = 45 + r.nextInt(30);
            int x = p.getBlockX() + (int) (Math.cos(ang) * dist), z = p.getBlockZ() + (int) (Math.sin(ang) * dist);
            x = Math.floorDiv(x, 3) * 3 + 1;
            z = Math.floorDiv(z, PZ) * PZ + 3;
            BlockPos c = new BlockPos(x, FLOOR + 1, z);
            level.getChunk(c);
            for (int dz = -1; dz <= 1; dz++) level.setBlock(c.offset(0, 0, dz), ModBlocks.AIR, Block.UPDATE_ALL);
            level.setBlock(c, ModBlocks.state("pg_car_alarm"), Block.UPDATE_ALL);
            level.setBlock(c.north(), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(c.south(), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_ALL);
            alarms.put(p.getUUID(), c);
            p.displayClientMessage(Component.literal("§7§oSomewhere on this level, a car alarm."), true);
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            BlockPos a = alarms.get(p.getUUID());
            if (a == null) return;
            if (p.tickCount % 40 == 0 && level.isLoaded(a))
                level.playSound(null, a, ModSounds.get("event.parking_garage_alarm"), SoundSource.AMBIENT, 6f, 1f);
            if (p.tickCount % 300 != 7 || p.isCreative() || p.isSpectator() || !level.isLoaded(a)) return;
            // the noise draws hounds; they circle the car
            var type = ModEntities.ALL.get("hound");
            if (type == null || level.getEntities(type, new net.minecraft.world.phys.AABB(a).inflate(24), e -> true).size() >= 2) return;
            var r = p.getRandom();
            for (int attempt = 0; attempt < 6; attempt++) {
                BlockPos at = new BlockPos(a.getX() + r.nextInt(25) - 12, FLOOR + 1, Math.floorDiv(a.getZ(), PZ) * PZ + 6 + r.nextInt(5));
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                if (at.distSqr(p.blockPosition()) < 16 * 16) continue;
                Mob m = type.create(level);
                if (m == null) return;
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360f, 0f);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                level.addFreshEntity(m);
                m.getNavigation().moveTo(a.getX() + 0.5, a.getY(), a.getZ() + 2.5, 1.0);
                return;
            }
        }
    }
}
