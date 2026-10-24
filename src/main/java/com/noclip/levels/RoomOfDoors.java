package com.noclip.levels;

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
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Room of Doors. A marble hall planted with freestanding doors in rows, in
 * every direction, hundreds of them. Every wanderer before you tried them, and
 * the wrong ones are marked: claw marks at the base, handprints, a chalk X.
 * Most wrong doors put you back in the hall somewhere else; some open onto
 * somewhere worse. In every block of 252 doors exactly one has never been touched.
 */
public final class RoomOfDoors {
    static final int FLOOR = 6, CEIL = FLOOR + 12;
    static final int COL = 3, ROW = 5, REGION_X = 63, REGION_Z = 60;
    static final String[] COLORS = {"white", "oak", "green"};

    private RoomOfDoors() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("room_of_doors")
                .number("").title("Room of Doors").order(4100)
                .tagline("Hundreds of doors. Everyone before you tried the wrong ones.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(LevelBehavior.NONE)
                .visuals(new Visuals().tint(1.04f, 0.98f, 0.9f).saturation(0.82f).contrast(1.1f).brightness(1.2f)
                        .grain(0.07f).aberration(0.0014f).vignette(0.55f).fog(6f, 46f).fogColor(0x1A1612).bloom(0.6f))
                .sanityDrain(0.035f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 3, 0.35f).dist(14, 34).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.08f).dist(30, 44).weight(1))
                .exit(ExitType.SECRET, "level_11", 3).exit(ExitType.SECRET, "the_hub", 2)
                .exit(ExitType.SECRET, "manila_room", 1).exit(ExitType.SECRET, "level_1", 1)
                .exit(ExitType.DOOR, "level_6", 2).exit(ExitType.DOOR, "level_run", 2).exit(ExitType.DOOR, "red_room", 1)
                .exit(ExitType.DOOR, "level_8", 1).exit(ExitType.DOOR, "long_hallway", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "recursion", 1)
                .build();
    }

    // ---------------------------------------------------------------- doors

    public static void useDoor(ServerPlayer p, BlockPos pos, boolean clean) {
        if (Travel.onCooldown(p)) return;
        ServerLevel level = p.serverLevel();
        RandomSource r = p.getRandom();
        level.playSound(null, pos, ModSounds.get("event.room_of_doors_open"), SoundSource.BLOCKS, 1f, 0.9f + r.nextFloat() * 0.2f);
        if (clean) {
            level.playSound(null, pos, ModSounds.get("event.room_of_doors_right"), SoundSource.AMBIENT, 1f, 1f);
            Sanity.add(p, 15f);
            Travel.exit(p, ExitType.SECRET, pos);
            return;
        }
        float roll = r.nextFloat();
        if (roll < 0.62f) {
            // back into the hall, somewhere else, facing another row of doors
            for (int attempt = 0; attempt < 10; attempt++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double d = 50 + r.nextDouble() * 80;
                int x = (int) Math.floor((p.getX() + Math.cos(ang) * d) / COL) * COL + 2;
                int z = (int) Math.floor((p.getZ() + Math.sin(ang) * d) / ROW) * ROW + 2;
                BlockPos t = new BlockPos(x, FLOOR + 1, z);
                level.getChunk(t);
                if (!level.getBlockState(t).isAir() || !level.getBlockState(t.above()).isAir()) continue;
                p.teleportTo(level, x + 0.5, FLOOR + 1, z + 0.5, r.nextBoolean() ? 0f : 180f, 0f);
                level.playSound(null, t, ModSounds.get("event.room_of_doors_slam"), SoundSource.BLOCKS, 0.9f, 1f);
                ServerPlayNetworking.send(p, new NoclipNet.EventPayload("flash", 0.35f));
                Sanity.add(p, -3f);
                return;
            }
        } else if (roll < 0.88f) {
            Travel.exit(p, ExitType.DOOR, pos);
            return;
        }
        // the door opens onto a wall of black, and something in the hall heard it
        level.playSound(null, pos, ModSounds.get("event.room_of_doors_slam"), SoundSource.BLOCKS, 1.2f, 0.8f);
        Sanity.add(p, -5f);
        var type = ModEntities.lazy("hound").get();
        if (type == null) return;
        for (int attempt = 0; attempt < 8; attempt++) {
            double ang = Math.toRadians(p.getYRot() + 180 + (r.nextFloat() - 0.5f) * 90);
            int x = (int) Math.floor(p.getX() - Math.sin(ang) * 20);
            int z = (int) Math.floor((p.getZ() + Math.cos(ang) * 20) / ROW) * ROW + 2;
            Mob m = type.create(level);
            if (m == null) return;
            m.moveTo(x + 0.5, FLOOR + 1, z + 0.5, r.nextFloat() * 360, 0);
            if (!level.noCollision(m)) continue;
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(m);
            return;
        }
    }

    // ---------------------------------------------------------------- the hall

    static final class Gen implements LevelGenerator {
        final BlockState white = ModBlocks.state("rod_marble_white");
        final BlockState black = ModBlocks.state("rod_marble_black");
        final BlockState ceiling = ModBlocks.state("rod_ceiling");
        final BlockState chandelier = ModBlocks.state("rod_chandelier");
        final BlockState fallen = ModBlocks.state("rod_fallen_door");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13);

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL, w.minZ, w.maxX, CEIL + 2, w.maxZ, ceiling);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    w.set(x, FLOOR, z, ((x + z) & 1) == 0 ? white : black);
                    if (Math.floorMod(x, 12) == 1 && Math.floorMod(z, 10) == 3) w.set(x, CEIL - 1, z, chandelier);
                    if (Math.floorMod(x, 6) == 1 && Math.floorMod(z, ROW) == 3) w.set(x, FLOOR + 5, z, light);
                    if (Math.floorMod(x, COL) == 0 && Math.floorMod(z, ROW) == 0) door(w, s, x, z);
                }
            }
            // somebody who got out left a note on the floor, once in every block of doors
            int rx = Math.floorDiv(w.minX, REGION_X), rz = Math.floorDiv(w.minZ, REGION_Z);
            for (int ix = rx - 1; ix <= rx + 1; ix++)
                for (int iz = rz - 1; iz <= rz + 1; iz++) {
                    if (!Hash.chance(s, ix, iz, 0x5C4A, 0.7f)) continue;
                    int bx = ix * REGION_X + COL * (2 + Hash.range(s, ix, iz, 0x5C4B, 16)) + 1;
                    int bz = iz * REGION_Z + ROW * Hash.range(s, ix, iz, 0x5C4C, 10) + 2;
                    BlockState tl = ModBlocks.state("rod_scrawl_tl"), tr = ModBlocks.state("rod_scrawl_tr");
                    BlockState bl = ModBlocks.state("rod_scrawl_bl"), br = ModBlocks.state("rod_scrawl_br");
                    w.set(bx, FLOOR + 1, bz, tl);
                    w.set(bx + 1, FLOOR + 1, bz, tr);
                    w.set(bx, FLOOR + 1, bz + 1, bl);
                    w.set(bx + 1, FLOOR + 1, bz + 1, br);
                }
        }

        static boolean isCleanSlot(long s, int x, int z) {
            int rx = Math.floorDiv(x, REGION_X), rz = Math.floorDiv(z, REGION_Z);
            int cx = rx * REGION_X + COL * Hash.range(s, rx, rz, 0xC1EA, REGION_X / COL);
            int cz = rz * REGION_Z + ROW * Hash.range(s, rx, rz, 0xC1EB, REGION_Z / ROW);
            return x == cx && z == cz;
        }

        void door(ChunkWriter w, long s, int x, int z) {
            long h = Hash.hash(s, x, z, 0xD0025);
            String color = COLORS[Hash.range(h, 3)];
            if (isCleanSlot(s, x, z)) {
                w.set(x, FLOOR + 1, z, ModBlocks.state("rod_" + color + "_low_clean"));
                w.set(x, FLOOR + 2, z, ModBlocks.state("rod_" + color + "_high_clean"));
                return;
            }
            float r = Hash.unit(h >>> 3);
            if (r < 0.05f) {
                // a gap in the row: the door has come off and lies flat
                w.set(x, FLOOR + 1, z, fallen.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(Hash.range(h >>> 9, 4))));
                return;
            }
            if (r < 0.08f) return;
            // most marks are obvious; a fair number are only a single smudge on one half
            int m = Hash.range(h >>> 12, 8);
            String low, high;
            switch (m) {
                case 0 -> { low = "scratch"; high = "chalk"; }
                case 1 -> { low = "hands"; high = "hands"; }
                case 2 -> { low = "scratch"; high = "clean"; }
                case 3 -> { low = "clean"; high = "chalk"; }
                case 4 -> { low = "hands"; high = "clean"; }
                case 5 -> { low = "clean"; high = "hands"; }
                case 6 -> { low = "scratch"; high = "hands"; }
                default -> { low = "hands"; high = "chalk"; }
            }
            w.set(x, FLOOR + 1, z, ModBlocks.state("rod_" + color + "_low_" + low));
            w.set(x, FLOOR + 2, z, ModBlocks.state("rod_" + color + "_high_" + high));
        }
    }
}
