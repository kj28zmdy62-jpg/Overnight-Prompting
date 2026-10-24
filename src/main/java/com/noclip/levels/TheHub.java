package com.noclip.levels;

import com.noclip.block.HubDoorBlock;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Hub. A hotel hallway that does not end, lined on both sides with doors,
 * each with a brass plaque naming the level behind it. Most of them open. Some
 * are locked because there is nothing behind them yet. And while you are not
 * looking, the plaques change places.
 */
public final class TheHub {
    static final int F = 6, CEIL = F + 5, PITCH = 12, CROSS = 48;
    static final String ID = "the_hub";

    private TheHub() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Hub").order(9000)
                .tagline("Every door goes somewhere. Not every door goes back.")
                .danger(BackroomsLevel.Danger.LOW)
                .generator(Gen::new)
                .behavior(new Shuffle())
                .visuals(new Visuals().tint(1.08f, 0.98f, 0.88f).saturation(0.88f).contrast(1.08f).brightness(1.1f)
                        .grain(0.06f).aberration(0.0014f).vignette(0.55f).bloom(0.55f).fog(5f, 52f).fogColor(0x1A0E0A))
                .sanityDrain(0.008f)
                .spawnY(F + 1, F + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("faceling"), 2, 0.2f).dist(16, 36).weight(3))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.05f).seen().dist(30, 46).weight(1))
                .exit(ExitType.SECRET, "the_exit", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_11", 1)
                .build();
    }

    static boolean doorSpot(int along) {
        return Math.floorMod(along, 3) == 1;
    }

    static final class Gen implements LevelGenerator {
        final BlockState paper = ModBlocks.state("hub_wallpaper");
        final BlockState wainscot = ModBlocks.state("hub_wainscot");
        final BlockState carpet = ModBlocks.state("hub_carpet_runner");
        final BlockState parquet = ModBlocks.state("hub_parquet");
        final BlockState ceiling = ModBlocks.state("hub_ceiling");
        final BlockState sconce = ModBlocks.state("hub_sconce");
        final BlockState pendant = ModBlocks.state("hub_pendant");
        final BlockState white = ModBlocks.state("hub_white_door");
        final BlockState whiteTop = ModBlocks.state("hub_white_door_top");
        final BlockState sub = ModBlocks.state("subfloor");
        final BlockState filler = ModBlocks.state("void_filler");
        final String[] styles = {"oak", "oak", "oak", "white", "green", "steel"};
        final List<String> plaques = HubDoorBlock.allPlaques();

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, sub);
            w.fill(w.minX, CEIL, w.minZ, w.maxX, CEIL + 3, w.maxZ, filler);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int xl = Math.floorMod(x, CROSS), zl = Math.floorMod(z, PITCH);
                    boolean inMain = zl >= 1 && zl <= 5, inCross = xl <= 4;
                    if (inMain || inCross) {
                        boolean runner = inMain ? zl >= 2 && zl <= 4 : xl >= 1 && xl <= 3;
                        w.set(x, F, z, runner ? carpet : parquet);
                        w.set(x, CEIL, z, ceiling);
                        if ((inMain && zl == 3 && Math.floorMod(x, 9) == 0) || (!inMain && xl == 2 && Math.floorMod(z, 9) == 0))
                            w.set(x, CEIL - 1, z, pendant);
                        continue;
                    }
                    w.set(x, F, z, parquet);
                    boolean mainWall = zl == 0 || zl == 6;
                    boolean crossWall = xl == 5 || xl == CROSS - 1;
                    if (mainWall || crossWall) wall(w, s, x, z, mainWall && !crossWall, mainWall ? x : z, zl, xl);
                    else w.fill(x, F, z, x, CEIL, z, filler);
                }
            }
        }

        void wall(ChunkWriter w, long s, int x, int z, boolean main, int along, int zl, int xl) {
            w.set(x, F + 1, z, wainscot);
            for (int y = F + 2; y < CEIL; y++) w.set(x, y, z, paper);
            w.set(x, CEIL, z, ceiling);
            boolean corner = main ? (xl == 6 || xl == CROSS - 2) : (zl == 7 || zl == 11 || zl == 6 || zl == 0);
            if (corner) return;
            if (doorSpot(along)) {
                long h = Hash.hash(s, x, z, 31);
                float r = Hash.unit(h);
                if (r < 0.003f) {
                    // a plain white door with no plaque. It is warm to the touch.
                    w.set(x, F + 1, z, white);
                    w.set(x, F + 2, z, whiteTop);
                    return;
                }
                String style = styles[Hash.range(h >>> 8, styles.length)];
                w.set(x, F + 1, z, ModBlocks.state("hub_door_" + style));
                w.set(x, F + 2, z, ModBlocks.state("hub_door_" + style + "_top"));
                w.set(x, F + 3, z, ModBlocks.state(HubDoorBlock.PLAQUE + pickPlaque(h)));
            } else if (Math.floorMod(along, 6) == 3) {
                // sconces between the doors, on the hall side
                if (main) {
                    if (zl == 0) w.set(x, F + 3, z + 1, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    else w.set(x, F + 3, z - 1, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                } else {
                    if (xl == 5) w.set(x - 1, F + 3, z, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    else w.set(x + 1, F + 3, z, sconce.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                }
            }
        }

        String pickPlaque(long h) {
            float r = Hash.unit(h >>> 20);
            int n = plaques.size();
            if (r < 0.03f) return "unmarked";
            // the famous levels come up more often than the rest
            if (r < 0.40f) return plaques.get(Hash.range(h >>> 30, Math.min(20, n)));
            return plaques.get(Hash.range(h >>> 34, n));
        }
    }

    /** The hall rearranges its plaques behind your back, and sometimes something knocks. */
    static final class Shuffle implements LevelBehavior {
        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            var r = p.getRandom();
            if (p.tickCount % 30 == 0) {
                List<String> plaques = HubDoorBlock.allPlaques();
                for (int attempt = 0; attempt < 16; attempt++) {
                    BlockPos at = p.blockPosition().offset(r.nextInt(41) - 20, 0, r.nextInt(21) - 10).atY(F + 3);
                    if (HubDoorBlock.target(level.getBlockState(at)) == null) continue;
                    if (at.distSqr(p.blockPosition()) < 36 || DeepEndUtil.seen(p, Vec3.atCenterOf(at), 0.2)) continue;
                    String id = plaques.get(r.nextInt(plaques.size()));
                    level.setBlock(at, ModBlocks.state(HubDoorBlock.PLAQUE + id), Block.UPDATE_CLIENTS);
                    break;
                }
            }
            if (p.tickCount % 400 == 0 && r.nextFloat() < 0.35f) {
                // somebody knocks, from the other side of a door near you
                for (int attempt = 0; attempt < 10; attempt++) {
                    BlockPos at = p.blockPosition().offset(r.nextInt(17) - 8, 0, r.nextInt(13) - 6).atY(F + 2);
                    if (!(level.getBlockState(at).getBlock() instanceof HubDoorBlock)) continue;
                    level.playSound(null, at, ModSounds.get("halluc.knock"), SoundSource.AMBIENT, 1f, 0.9f);
                    break;
                }
            }
        }
    }
}
