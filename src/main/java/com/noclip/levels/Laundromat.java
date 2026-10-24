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
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Laundromat. Rows of washers churning with nothing in them, under hard
 * fluorescent light that never goes off. Every so often one finishes, buzzes and
 * pops open; sometimes someone has left something. One in a few dozen dryers is
 * tumbling clothes that look exactly like yours. You can open it. The washer
 * marked BROKEN has a drum that goes back much further than it should.
 */
public final class Laundromat {
    static final int FLOOR = 10, CEIL = FLOOR + 4, PX = 20, PZ = 8, BLOCK_Z = 48;

    private Laundromat() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("laundromat")
                .number("").title("Laundromat").order(1110)
                .tagline("Open 24 hours. Nothing in the machines. One dryer is drying your clothes.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Cycles())
                .visuals(new Visuals().tint(0.96f, 1.03f, 1.0f).saturation(0.85f).contrast(1.05f).brightness(1.08f)
                        .grain(0.06f).aberration(0.0012f).vignette(0.4f).fog(10f, 64f).fogColor(0xC8D0C8).bloom(0.35f))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.3f).dist(12, 28).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(28, 44).weight(1))
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "motel", 1).exit(ExitType.NOCLIP, "supermarket", 1)
                .exit(ExitType.NOCLIP, "hospital", 1)
                .exit(ExitType.SECRET, "level_37", 2).exit(ExitType.SECRET, "level_7", 2).exit(ExitType.SECRET, "water_park", 1)
                .exit(ExitType.DOOR, "motel", 2).exit(ExitType.DOOR, "rain_city", 1).exit(ExitType.DOOR, "level_9", 1)
                .build();
    }

    enum Row { WASHERS, DRYERS, VENDING }

    static Row row(long s, int seg, int r) {
        float f = Hash.unit(s, seg, r, 0x1A0);
        return f < 0.6f ? Row.WASHERS : f < 0.9f ? Row.DRYERS : Row.VENDING;
    }

    static final class Gen implements LevelGenerator {
        final BlockState floor = ModBlocks.state("lm_floor");
        final BlockState wall = ModBlocks.state("lm_wall");
        final BlockState ceil = ModBlocks.state("lm_ceiling");
        final BlockState light = ModBlocks.state("lm_light");
        final BlockState flicker = ModBlocks.state("lm_light_flicker");
        final BlockState washer = ModBlocks.state("lm_washer");
        final BlockState fast = ModBlocks.state("lm_washer_fast");
        final BlockState done = ModBlocks.state("lm_washer_done");
        final BlockState open = ModBlocks.state("lm_washer_open");
        final BlockState broken = ModBlocks.state("lm_washer_broken");
        final BlockState dryer = ModBlocks.state("lm_dryer");
        final BlockState dryerEmpty = ModBlocks.state("lm_dryer_empty");
        final BlockState dryerClothes = ModBlocks.state("lm_dryer_clothes");
        final BlockState dryerTop = ModBlocks.state("lm_dryer_top");
        final BlockState table = ModBlocks.state("lm_folding_table");
        final BlockState chair = ModBlocks.state("lm_chair");
        final BlockState basket = ModBlocks.state("lm_basket");
        final BlockState change = ModBlocks.state("lm_change_machine");
        final BlockState detergent = ModBlocks.state("lm_detergent_machine");
        final BlockState board = ModBlocks.state("lm_board");
        final BlockState winNeon = ModBlocks.state("lm_window_neon");
        final BlockState win = ModBlocks.state("lm_window");
        final BlockState door = ModBlocks.state("dark_doorway");
        final BlockState exitSign = ModBlocks.state("exit_sign");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.layer(FLOOR, floor);
            w.layer(CEIL, ceil);
            w.fill(w.minX, CEIL + 1, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++)
                for (int z = w.minZ; z <= w.maxZ; z++) column(w, s, x, z);
        }

        void column(ChunkWriter w, long s, int x, int z) {
            int u = Math.floorMod(x, PX), v = Math.floorMod(z, PZ), bz = Math.floorMod(z, BLOCK_Z);
            int seg = Math.floorDiv(x, PX), r = Math.floorDiv(z, PZ);
            int y = FLOOR + 1;
            if (v == 4 && Math.floorMod(x, 3) == 1)
                w.set(x, CEIL, z, Hash.chance(s, x, z, 0x11, 0.08f) ? flicker : light);
            if (bz == BLOCK_Z - 1) {
                // the front of this laundromat: windows onto a night street, a door at the cross aisles
                if (u <= 1) return;
                long h = Hash.hash(s, seg, Math.floorDiv(z, BLOCK_Z), 0xF0);
                if (u == 10 && Hash.unit(h) < 0.06f) {
                    w.set(x, y, z, door);
                    w.set(x, y + 1, z, door);
                    w.set(x, y + 2, z, exitSign.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    return;
                }
                w.set(x, y, z, wall);
                w.fill(x, y + 1, z, x, y + 2, z, u % 12 == 6 ? winNeon : win);
                return;
            }
            if (u <= 1 || v >= 2) {
                aisle(w, s, x, z, u, v, seg, r);
                return;
            }
            Direction f = v == 0 ? Direction.NORTH : Direction.SOUTH;
            float m = Hash.unit(s, x, z, 0x3A);
            switch (row(s, seg, r)) {
                case WASHERS -> {
                    BlockState b = m < 0.008f ? broken : m < 0.08f ? fast : m < 0.15f ? done : m < 0.18f ? open : washer;
                    w.set(x, y, z, b == broken ? b : b.setValue(HorizontalDirectionalBlock.FACING, f));
                    if (b != broken) w.set(x, y + 1, z, dryerTop.setValue(HorizontalDirectionalBlock.FACING, f));
                    else w.set(x, y + 1, z, dryerTop.setValue(HorizontalDirectionalBlock.FACING, f));
                }
                case DRYERS -> {
                    BlockState b = m < 0.03f ? dryerClothes : m < 0.1f ? dryerEmpty : dryer;
                    w.set(x, y, z, b.setValue(HorizontalDirectionalBlock.FACING, f));
                    w.set(x, y + 1, z, dryer.setValue(HorizontalDirectionalBlock.FACING, f));
                    w.set(x, y + 2, z, dryerTop.setValue(HorizontalDirectionalBlock.FACING, f));
                }
                case VENDING -> {
                    if (u % 6 == 2) w.set(x, y, z, change.setValue(HorizontalDirectionalBlock.FACING, f));
                    else if (u % 6 == 4) w.set(x, y, z, detergent.setValue(HorizontalDirectionalBlock.FACING, f));
                    else w.set(x, y, z, wall);
                    w.fill(x, y + 1, z, x, CEIL - 1, z, u % 4 == 1 ? board : wall);
                }
            }
        }

        void aisle(ChunkWriter w, long s, int x, int z, int u, int v, int seg, int r) {
            if (u <= 1) return;
            int y = FLOOR + 1;
            long h = Hash.hash(s, seg, r, 0xA15);
            boolean tables = Hash.unit(h) < 0.7f;
            if (tables && (v == 4 || v == 5) && ((u >= 4 && u <= 7) || (u >= 12 && u <= 15))) {
                w.set(x, y, z, table.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                return;
            }
            if (v == 7 && u % 3 == 0 && Hash.chance(s, x, z, 0xC4, 0.6f))
                w.set(x, y, z, chair.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            else if (Hash.chance(s, x, z, 0xBA5, 0.012f))
                w.set(x, y, z, basket.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(s, x, z, 4))));
        }
    }

    // ---------------------------------------------------------------- behaviour

    public static void openDryer(ServerPlayer p, BlockPos pos, BlockState st) {
        ServerLevel level = p.serverLevel();
        Direction f = st.getValue(HorizontalDirectionalBlock.FACING);
        level.setBlock(pos, ModBlocks.state("lm_dryer_empty").setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, ModSounds.get("event.laundromat_dryer"), SoundSource.BLOCKS, 1f, 1f);
        if (p.getRandom().nextFloat() < 0.5f && !p.isCreative()) {
            p.displayClientMessage(Component.literal("§7§oThose are your clothes. Somebody is already wearing the rest of them."), true);
            var type = ModEntities.ALL.get("skin_stealer");
            if (type == null) return;
            BlockPos behind = BlockPos.containing(p.position().subtract(p.getLookAngle().multiply(5, 0, 5)));
            for (int dy = 0; dy < 2; dy++) {
                BlockPos at = new BlockPos(behind.getX(), FLOOR + 1, behind.getZ());
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) {
                    behind = behind.relative(Direction.from2DDataValue(p.getRandom().nextInt(4)));
                    continue;
                }
                Mob m = type.create(level);
                if (m == null) return;
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0f);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                level.addFreshEntity(m);
                Sanity.add(p, -10f);
                return;
            }
        } else {
            p.displayClientMessage(Component.literal("§7§oStill warm. Something was left in the pockets."), true);
            give(p, new ItemStack(ModItems.ALMOND_WATER));
            give(p, new ItemStack(ModItems.BATTERY, 2));
        }
    }

    static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    static final class Cycles implements LevelBehavior {
        /** Washers that have finished: position -> time they close and start again. */
        private final Map<Long, Long> finished = new ConcurrentHashMap<>();
        private final Map<UUID, Long> next = new ConcurrentHashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            Long due = next.get(p.getUUID());
            if (due == null) {
                next.put(p.getUUID(), now + 20 * (10 + p.getRandom().nextInt(15)));
                return;
            }
            if (now < due) return;
            next.put(p.getUUID(), now + 20 * (15 + p.getRandom().nextInt(25)));
            Block washer = ModBlocks.get("lm_washer");
            for (int attempt = 0; attempt < 20; attempt++) {
                BlockPos pos = new BlockPos(p.getBlockX() + p.getRandom().nextInt(21) - 10, FLOOR + 1, p.getBlockZ() + p.getRandom().nextInt(21) - 10);
                BlockState st = level.getBlockState(pos);
                if (!st.is(washer)) continue;
                Direction f = st.getValue(HorizontalDirectionalBlock.FACING);
                level.setBlock(pos, ModBlocks.state("lm_washer_open").setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, ModSounds.get("event.laundromat_buzzer"), SoundSource.BLOCKS, 1.2f, 1f);
                finished.put(pos.asLong(), now + 20 * 25);
                // and sometimes what came out of it is still on the floor in front
                BlockPos front = pos.relative(f);
                if (p.getRandom().nextFloat() < 0.25f && level.getBlockState(front).isAir())
                    level.setBlock(front, ModBlocks.state("almond_water_bottle").setValue(HorizontalDirectionalBlock.FACING, f), Block.UPDATE_CLIENTS);
                return;
            }
        }

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            for (Iterator<Map.Entry<Long, Long>> it = finished.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Long> e = it.next();
                if (now < e.getValue()) continue;
                it.remove();
                BlockPos pos = BlockPos.of(e.getKey());
                if (!level.isLoaded(pos)) continue;
                BlockState st = level.getBlockState(pos);
                if (!st.is(ModBlocks.get("lm_washer_open"))) continue;
                level.setBlock(pos, ModBlocks.state("lm_washer").setValue(HorizontalDirectionalBlock.FACING,
                        st.getValue(HorizontalDirectionalBlock.FACING)), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, ModSounds.get("event.laundromat_spin"), SoundSource.BLOCKS, 1f, 1f);
            }
        }
    }
}
