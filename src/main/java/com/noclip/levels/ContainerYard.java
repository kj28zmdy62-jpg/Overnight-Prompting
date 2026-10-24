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
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container Yard. Night, sodium light, and steel boxes stacked three high in
 * every direction, a maze with lanes between the blocks. A few containers stand
 * open with a bulb burning inside and someone's mattress and supplies. Go in for
 * the supplies and sometimes the doors swing shut behind you, and something
 * outside starts knocking.
 */
public final class ContainerYard {
    static final int FLOOR = 40, CX = 26, CZ = 22;
    static final String[] COLORS = {"red", "blue", "green", "grey", "orange", "white"};

    private ContainerYard() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder("container_yard")
                .number("").title("Container Yard").order(2050)
                .tagline("Some of them are open, and lit, and stocked. Keep an eye on the doors.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Doors())
                .visuals(new Visuals().sky().darkness(0.25f).tint(1.12f, 0.96f, 0.78f).saturation(0.75f).contrast(1.1f)
                        .brightness(1.1f).grain(0.08f).aberration(0.0012f).vignette(0.5f).fog(6f, 70f).fogColor(0x2A2016)
                        .bloom(0.7f))
                .sanityDrain(0.025f)
                .spawnY(FLOOR + 1, FLOOR + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.12f).dist(30, 44).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("shadow_figure"), 1, 0.1f).seen().dist(24, 40).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 2, 0.4f).dark().dist(12, 26).weight(2))
                .exit(ExitType.NOCLIP, "parking_lot", 2).exit(ExitType.NOCLIP, "the_dock", 1).exit(ExitType.NOCLIP, "level_1", 1)
                .exit(ExitType.DOOR, "the_dock", 3).exit(ExitType.DOOR, "parking_lot", 2).exit(ExitType.DOOR, "endless_highway", 1)
                .exit(ExitType.SECRET, "cold_storage", 1)
                .build();
    }

    // ---------------------------------------------------------------- layout

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> box = new ConcurrentHashMap<>(), end = new ConcurrentHashMap<>();
        final BlockState asphalt = ModBlocks.state("cy_asphalt");
        final BlockState line = ModBlocks.state("cy_asphalt_line");
        final BlockState[] numbers = {ModBlocks.state("cy_asphalt_a7"), ModBlocks.state("cy_asphalt_c12"),
                ModBlocks.state("cy_asphalt_f3"), ModBlocks.state("cy_asphalt_k9")};
        final BlockState ply = ModBlocks.state("cy_ply_floor");
        final BlockState reefer = ModBlocks.state("cy_reefer_unit");
        final BlockState reeferDoor = ModBlocks.state("cy_reefer_door");
        final BlockState mast = ModBlocks.state("cy_mast");
        final BlockState head = ModBlocks.state("cy_mast_head");
        final BlockState headDead = ModBlocks.state("cy_mast_head_dead");
        final BlockState gate = ModBlocks.state("cy_gate");
        final BlockState fence = ModBlocks.state("chain_link_fence");
        final BlockState bulb = ModBlocks.state("cy_bulb");
        final BlockState mattress = ModBlocks.state("cy_mattress");
        final BlockState chair = ModBlocks.state("office_chair");
        final BlockState supply = ModBlocks.state("supply_crate");
        final BlockState bottle = ModBlocks.state("almond_water_bottle");
        final BlockState puddle = ModBlocks.state("puddle");
        final BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13);
        final BlockState sub = ModBlocks.state("subfloor");

        Gen() {
            for (String c : COLORS) {
                box.put(c, ModBlocks.state("cy_box_" + c));
                end.put(c, ModBlocks.state("cy_end_" + c));
            }
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            w.fill(w.minX, 0, w.minZ, w.maxX, FLOOR - 1, w.maxZ, sub);
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    int lx = Math.floorMod(x, CX), lz = Math.floorMod(z, CZ);
                    BlockState g = asphalt;
                    if ((lz == 3 && lx >= 6 && Math.floorMod(x, 6) < 3) || (lx == 3 && lz >= 6 && Math.floorMod(z, 6) < 3)) g = line;
                    if (lx == 4 && lz == 9) g = numbers[Hash.range(s, Math.floorDiv(x, CX), Math.floorDiv(z, CZ), 4, numbers.length)];
                    w.set(x, FLOOR, z, g);
                    if (Hash.fbm(s ^ 0x9D, x / 9.0, z / 9.0, 2) > 0.7f && Hash.chance(s, x, z, 2, 0.5f) && (lx < 6 || lz < 6))
                        w.set(x, FLOOR + 1, z, puddle);
                }
            }
            int ci0 = Math.floorDiv(w.minX, CX) - 1, ci1 = Math.floorDiv(w.maxX, CX) + 1;
            int ck0 = Math.floorDiv(w.minZ, CZ) - 1, ck1 = Math.floorDiv(w.maxZ, CZ) + 1;
            for (int ci = ci0; ci <= ci1; ci++)
                for (int ck = ck0; ck <= ck1; ck++) cell(w, s, ci, ck, ci * CX, ck * CZ);
        }

        void cell(ChunkWriter w, long s, int ci, int ck, int x0, int z0) {
            // high mast at the lane crossing
            long mh = Hash.hash(s, ci, ck, 10);
            boolean lit = Hash.unit(mh) > 0.22f;
            w.fill(x0 + 2, FLOOR + 1, z0 + 2, x0 + 2, FLOOR + 13, z0 + 2, mast);
            w.set(x0 + 2, FLOOR + 14, z0 + 2, lit ? head : headDead);
            if (lit) {
                for (int[] o : new int[][]{{0, 3}, {0, -3}, {3, 0}, {-3, 0}})
                    w.set(x0 + 2 + o[0], FLOOR + 5, z0 + 2 + o[1], light);
            }
            // a chain-link fence across a lane, with a gate
            if (Hash.unit(mh >>> 20) < 0.05f) {
                int fz = z0 + 13;
                for (int x = x0; x <= x0 + 5; x++) {
                    boolean g = x == x0 + 2 || x == x0 + 3;
                    for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                        if (g && y <= FLOOR + 2) w.set(x, y, fz, gate);
                        else w.set(x, y, fz, fence.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));
                    }
                }
            }
            for (int sx = 0; sx < 2; sx++) {
                for (int sz = 0; sz < 4; sz++) {
                    int X0 = x0 + 6 + sx * 10, Z0 = z0 + 6 + sz * 4;
                    long h = Hash.hash(s, ci * 2 + sx, ck * 4 + sz, 11);
                    float tall = Hash.noise(s ^ 0x7A11, (ci * 2 + sx) / 3.0, (ck * 4 + sz) / 3.0);
                    float r = Hash.unit(h);
                    int n = r < 0.18f ? 0 : 1 + (int) Math.min(2, tall * 3.2f + Hash.unit(h >>> 8) * 0.8f);
                    for (int lvl = 0; lvl < n; lvl++) {
                        long hc = Hash.hash(h, lvl);
                        container(w, hc, X0, FLOOR + 1 + lvl * 4, Z0, sx, lvl == 0 && Hash.unit(hc >>> 30) < 0.08f);
                    }
                }
            }
        }

        void container(ChunkWriter w, long h, int X0, int y0, int Z0, int sx, boolean open) {
            String c = COLORS[Hash.range(h, COLORS.length)];
            boolean isReefer = c.equals("white") && Hash.unit(h >>> 12) < 0.4f;
            boolean doorWest = open ? sx == 0 : (h >>> 5 & 1) == 0;
            int doorDx = doorWest ? 0 : 9, backDx = doorWest ? 9 : 0;
            Direction doorFace = doorWest ? Direction.WEST : Direction.EAST;
            BlockState b = box.get(c);
            boolean brand = c.equals("red") || c.equals("blue") || c.equals("green");
            for (int dx = 0; dx < 10; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    for (int dy = 0; dy < 4; dy++) {
                        int x = X0 + dx, y = y0 + dy, z = Z0 + dz;
                        boolean inside = dx >= 1 && dx <= 8 && dz >= 1 && dz <= 2 && dy >= 1 && dy <= 2;
                        if (open && (inside || (dx == doorDx && dz >= 1 && dz <= 2 && dy >= 1 && dy <= 2))) {
                            w.set(x, y, z, ModBlocks.AIR);
                            continue;
                        }
                        if (open && dy == 0 && dx >= 1 && dx <= 8 && dz >= 1 && dz <= 2) {
                            w.set(x, y, z, ply);
                            continue;
                        }
                        if (dx == doorDx) {
                            if (isReefer && dz >= 1 && dz <= 2 && dy <= 2) w.set(x, y, z, reeferDoor);
                            else w.set(x, y, z, end.get(c).setValue(HorizontalDirectionalBlock.FACING, doorFace));
                        } else if (dx == backDx && isReefer && dz >= 1 && dz <= 2 && dy >= 1 && dy <= 2) {
                            w.set(x, y, z, reefer.setValue(HorizontalDirectionalBlock.FACING, doorFace.getOpposite()));
                        } else if (brand && (dz == 0 || dz == 3) && dx >= 3 && dx <= 6 && dy >= 1 && dy <= 2) {
                            int i = dz == 3 ? dx - 3 : 6 - dx;
                            w.set(x, y, z, ModBlocks.state("cy_logo_" + c + "_" + i + (dy - 1)));
                        } else {
                            w.set(x, y, z, b);
                        }
                    }
                }
            }
            if (open) {
                int zc = Z0 + 1;
                int far = doorWest ? X0 + 8 : X0 + 1, mid = X0 + 4 + (doorWest ? 1 : 0);
                w.set(mid, y0 + 2, ((h >>> 40) & 1) == 0 ? zc : zc + 1, bulb);
                w.set(far, y0 + 1, zc, mattress.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                w.set(far, y0 + 1, zc + 1, Hash.unit(h >>> 44) < 0.6f ? supply : bottle);
                w.set(mid, y0 + 1, zc + 1, chair.setValue(HorizontalDirectionalBlock.FACING, doorFace));
            }
        }
    }

    // ---------------------------------------------------------------- behaviour

    /** The open containers' doors. */
    static final class Doors implements LevelBehavior {
        private record Shut(ServerLevel level, List<BlockPos> blocks, long until, BlockPos at) {}

        private final Map<Long, Long> cooldown = new ConcurrentHashMap<>();
        private final List<Shut> shut = new ArrayList<>();

        @Override
        public void tickLevel(ServerLevel level, BackroomsLevel def) {
            long now = level.getGameTime();
            synchronized (shut) {
                shut.removeIf(sh -> {
                    if (sh.level != level) return false;
                    if (now % 50 == 0 && now < sh.until) {
                        Vec3 c = Vec3.atCenterOf(sh.at);
                        double a = level.random.nextDouble() * Math.PI * 2;
                        level.playSound(null, c.x + Math.cos(a) * 3, c.y, c.z + Math.sin(a) * 3,
                                ModSounds.get("event.container_yard_knock"), SoundSource.HOSTILE, 1.3f, 0.8f + level.random.nextFloat() * 0.3f);
                    }
                    if (now < sh.until) return false;
                    for (BlockPos p : sh.blocks) {
                        if (level.getBlockState(p).is(ModBlocks.get("cy_slam_door"))) level.setBlock(p, ModBlocks.AIR, Block.UPDATE_ALL);
                    }
                    level.playSound(null, sh.at, ModSounds.get("event.container_yard_slam"), SoundSource.BLOCKS, 0.5f, 1.4f);
                    return true;
                });
            }
        }

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isCreative() || p.isSpectator() || (p.tickCount + p.getId()) % 10 != 0) return;
            BlockPos at = p.blockPosition();
            Block ply = ModBlocks.get("cy_ply_floor");
            if (!level.getBlockState(at.below()).is(ply)) return;
            for (int dir = -1; dir <= 1; dir += 2) {
                for (int d = 1; d <= 10; d++) {
                    BlockPos q = at.offset(dir * d, 0, 0);
                    if (level.getBlockState(q.below()).is(ply)) continue;
                    if (!level.getBlockState(q).isAir() || d < 3) break;
                    long key = q.asLong();
                    long now = level.getGameTime();
                    if (cooldown.getOrDefault(key, -99999L) > now) return;
                    cooldown.put(key, now + 2400);
                    Vec3 to = Vec3.atCenterOf(q).subtract(p.getEyePosition()).normalize();
                    if (p.getViewVector(1f).dot(to) > 0.2 || level.random.nextFloat() > 0.4f) return;
                    slam(level, q, p);
                    return;
                }
            }
        }

        private void slam(ServerLevel level, BlockPos door, ServerPlayer p) {
            List<BlockPos> placed = new ArrayList<>();
            BlockState slab = ModBlocks.state("cy_slam_door");
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos q = door.offset(0, dy, dz);
                    if (level.getBlockState(q).isAir()) {
                        level.setBlock(q, slab, Block.UPDATE_ALL);
                        placed.add(q);
                    }
                }
            level.playSound(null, door, ModSounds.get("event.container_yard_slam"), SoundSource.BLOCKS, 1.6f, 1f);
            ServerPlayNetworking.send(p, new NoclipNet.EventPayload("pulse", 0.7f));
            Sanity.add(p, -6f);
            p.displayClientMessage(Component.literal("The doors swing shut. Something outside starts knocking.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            synchronized (shut) {
                shut.add(new Shut(level, placed, level.getGameTime() + 360 + level.random.nextInt(240), door.immutable()));
            }
        }
    }
}
