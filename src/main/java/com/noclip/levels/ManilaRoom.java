package com.noclip.levels;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.LevelBehavior;
import com.noclip.level.LevelGenerator;
import com.noclip.level.Visuals;
import com.noclip.registry.ModBlocks;
import com.noclip.world.ChunkWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Manila Room. A single quiet room the colour of a file folder, lit from
 * nowhere. A desk, a chair, a folder with your name on it, a clock with no hands,
 * a photograph of this room with someone standing in it, and a door that is only
 * painted on the wall (it opens anyway). You are safe here. The chair in the
 * corner turns to face you whenever you look away, and then it comes closer.
 */
public final class ManilaRoom {
    static final int FLOOR = 10, GRID = 32;

    private ManilaRoom() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("manila_room")
                .number("").title("The Manila Room").order(950)
                .tagline("A small, quiet room. You are safe here. Mostly.")
                .danger(BackroomsLevel.Danger.SAFE)
                .generator(Gen::new)
                .behavior(new Chair())
                .visuals(new Visuals().tint(1.06f, 1.0f, 0.86f).saturation(0.8f).contrast(0.96f).brightness(1.06f)
                        .grain(0.04f).aberration(0.0008f).vignette(0.32f).fog(6f, 40f).fogColor(0xE2CB96).bloom(0.5f)
                        .noRec())
                .sanityDrain(-0.25f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .exit(ExitType.DOOR, "level_0", 2).exit(ExitType.DOOR, "level_11", 2).exit(ExitType.DOOR, "level_4", 1)
                .exit(ExitType.NOCLIP, "level_0", 2).exit(ExitType.NOCLIP, "level_1", 1)
                .build();
    }

    static final class Gen implements LevelGenerator {
        final BlockState wall = ModBlocks.state("manila_wall");
        final BlockState base = ModBlocks.state("manila_wall_base");
        final BlockState carpet = ModBlocks.state("manila_carpet");
        final BlockState ceiling = ModBlocks.state("manila_ceiling");
        final BlockState clock = ModBlocks.state("manila_clock");
        final BlockState door = ModBlocks.state("manila_painted_door");
        final BlockState doorTop = ModBlocks.state("manila_painted_door_top");
        final BlockState picture = ModBlocks.state("manila_picture");
        final BlockState folder = ModBlocks.state("manila_folder");
        final BlockState desk = ModBlocks.state("office_desk");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState filing = ModBlocks.state("filing_cabinet");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");

        @Override
        public void generate(ChunkWriter w) {
            w.fill(w.minX, 0, w.minZ, w.maxX, 30, w.maxZ, filler);
            for (int i = Math.floorDiv(w.minX, GRID); i <= Math.floorDiv(w.maxX, GRID); i++)
                for (int k = Math.floorDiv(w.minZ, GRID); k <= Math.floorDiv(w.maxZ, GRID); k++)
                    room(w, i * GRID + 8, k * GRID + 8);
        }

        void room(ChunkWriter w, int x0, int z0) {
            for (int dx = 0; dx <= 8; dx++)
                for (int dz = 0; dz <= 7; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    w.set(x, FLOOR - 1, z, sub);
                    boolean edge = dx == 0 || dx == 8 || dz == 0 || dz == 7;
                    w.set(x, FLOOR, z, edge ? base : carpet);
                    for (int y = FLOOR + 1; y <= FLOOR + 4; y++) w.set(x, y, z, edge ? (y == FLOOR + 1 ? base : wall) : ModBlocks.AIR);
                    w.set(x, FLOOR + 5, z, edge ? wall : ceiling);
                }
            w.set(x0 + 4, FLOOR + 1, z0, door);
            w.set(x0 + 4, FLOOR + 2, z0, doorTop);
            w.set(x0 + 2, FLOOR + 1, z0 + 6, ClassicsKit.facing(desk, Direction.SOUTH));
            w.set(x0 + 3, FLOOR + 1, z0 + 6, ClassicsKit.facing(desk, Direction.SOUTH));
            w.set(x0 + 3, FLOOR + 2, z0 + 6, ClassicsKit.facing(folder, Direction.SOUTH));
            w.set(x0 + 2, FLOOR + 1, z0 + 5, ClassicsKit.facing(chair, Direction.SOUTH));
            w.set(x0 + 7, FLOOR + 1, z0 + 6, ClassicsKit.facing(filing, Direction.EAST));
            w.set(x0, FLOOR + 3, z0 + 3, clock);
            w.set(x0 + 7, FLOOR + 3, z0 + 3, ClassicsKit.facing(picture, Direction.EAST));
            w.set(x0 + 7, FLOOR + 1, z0 + 1, ClassicsKit.facing(chair, Direction.NORTH));
        }
    }

    /** The corner chair. Turns to face you when unseen; once it faces you, it creeps a block closer. */
    static final class Chair implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.tickCount % 40 != 0 || p.isSpectator()) return;
            int x0 = Math.floorDiv(p.getBlockX(), GRID) * GRID + 8, z0 = Math.floorDiv(p.getBlockZ(), GRID) * GRID + 8;
            Block chairB = ModBlocks.get("office_chair");
            BlockPos deskChair = new BlockPos(x0 + 2, FLOOR + 1, z0 + 5);
            for (int dx = 1; dx <= 7; dx++)
                for (int dz = 1; dz <= 6; dz++) {
                    BlockPos c = new BlockPos(x0 + dx, FLOOR + 1, z0 + dz);
                    if (c.equals(deskChair)) continue;
                    BlockState st = level.getBlockState(c);
                    if (!st.is(chairB) || ClassicsKit.seenByAnyone(level, c)) continue;
                    Direction toward = ClassicsKit.dir(p.getBlockX() - c.getX(), p.getBlockZ() - c.getZ());
                    if (st.getValue(HorizontalDirectionalBlock.FACING) != toward) {
                        level.setBlock(c, st.setValue(HorizontalDirectionalBlock.FACING, toward), Block.UPDATE_CLIENTS);
                        return;
                    }
                    BlockPos next = c.relative(toward);
                    if (p.getRandom().nextInt(3) == 0 && level.getBlockState(next).isAir() && !next.equals(deskChair)
                            && next.distManhattan(p.blockPosition()) > 1) {
                        level.setBlock(c, ModBlocks.AIR, Block.UPDATE_CLIENTS);
                        level.setBlock(next, st, Block.UPDATE_CLIENTS);
                        ClassicsKit.sound(level, next, "event.manila_room_scrape", 0.6f, 0.9f);
                    }
                    return;
                }
        }
    }
}
