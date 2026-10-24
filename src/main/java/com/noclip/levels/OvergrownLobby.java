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
import com.noclip.util.Hash;
import com.noclip.world.ChunkWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Overgrown Lobby. Level 0, decades after anyone last walked it. Moss has
 * eaten the carpet, ivy the wallpaper, and the roof has fallen in, so real
 * daylight comes down in shafts onto ferns. Stand still too long and the roots
 * take hold. Somewhere a tree has grown right through the ceiling; climb it.
 */
public final class OvergrownLobby {
    static final int F = 10, CEIL = 14;
    static final String ID = "overgrown_lobby";

    private OvergrownLobby() {}

    public static BackroomsLevel create() {
        return BackroomsLevel.builder(ID)
                .number("").title("The Overgrown Lobby").order(3140)
                .tagline("The hum is still here, under the birdsong. Keep moving.")
                .danger(BackroomsLevel.Danger.MODERATE)
                .generator(Gen::new)
                .behavior(new Roots())
                .visuals(new Visuals().tint(0.98f, 1.04f, 0.9f).saturation(0.95f).contrast(1.06f).brightness(1.12f)
                        .grain(0.05f).aberration(0.0012f).vignette(0.45f).bloom(0.7f).fog(6f, 56f).fogColor(0xA8B07A).sky())
                .sanityDrain(0.025f)
                .spawnY(F + 1, F + 1)
                .spawn(SpawnRule.of(ModEntities.lazy("hound"), 1, 0.12f).dist(26, 42).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("deathmoth"), 2, 0.2f).dist(16, 30).weight(2))
                .spawn(SpawnRule.of(ModEntities.lazy("smiler"), 1, 0.3f).dark().dist(14, 26).weight(1))
                .exit(ExitType.ASCEND, "level_10", 2).exit(ExitType.ASCEND, "static_fields", 1).exit(ExitType.ASCEND, "golden_hour", 1)
                .exit(ExitType.NOCLIP, "level_0", 3).exit(ExitType.NOCLIP, "level_1", 1)
                .build();
    }

    static boolean hole(long s, int x, int z) {
        return Hash.fbm(s ^ 0x0E1, x / 9.0, z / 9.0, 3) > 0.62f;
    }

    static boolean treeRoom(LobbyLayout lay, int x, int z) {
        return lay.room(x, z, 900) < 0.05f;
    }

    static final class Gen implements LevelGenerator {
        final Map<String, BlockState> c = new HashMap<>();

        BlockState b(String n) {
            return c.computeIfAbsent(n, ModBlocks::state);
        }

        @Override
        public void generate(ChunkWriter w) {
            long s = w.seed;
            LobbyLayout lay = new LobbyLayout(s);
            w.fill(w.minX, 0, w.minZ, w.maxX, F - 1, w.maxZ, b("subfloor"));
            for (int x = w.minX; x <= w.maxX; x++) {
                for (int z = w.minZ; z <= w.maxZ; z++) {
                    boolean open = hole(s, x, z);
                    float moss = Hash.fbm(s ^ 0x3055, x / 12.0, z / 12.0, 3);
                    BlockState floor = open ? b("overgrown_moss") : moss > 0.58f ? b("overgrown_carpet")
                            : Hash.chance(s, x, z, 3, 0.03f) ? b("overgrown_roots") : b("moist_carpet");
                    if (!open && moss > 0.5f && Hash.chance(s, x, z, 4, 0.12f)) floor = b("overgrown_roots");
                    w.set(x, F, z, floor);
                    if (lay.wall(x, z)) {
                        int top = open && Hash.chance(s, x, z, 5, 0.6f) ? F + 1 + Hash.range(s, x, z, 6, 2) : CEIL - 1;
                        for (int y = F + 1; y <= top; y++) w.set(x, y, z, wallAt(s, x, y, z, moss, open));
                    } else {
                        if (open && Hash.chance(s, x, z, 7, 0.38f)) {
                            String[] plants = {"overgrown_fern", "overgrown_fern", "overgrown_fern", "overgrown_flower_white", "overgrown_flower_purple"};
                            w.set(x, F + 1, z, b(plants[Hash.range(s, x, z, 8, plants.length)]));
                        } else if (!open && moss > 0.55f && Hash.chance(s, x, z, 9, 0.05f)) {
                            w.set(x, F + 1, z, b(Hash.chance(s, x, z, 10, 0.4f) ? "overgrown_mushrooms" : "overgrown_fern"));
                        }
                    }
                    if (open) continue;
                    boolean lamp = Math.floorMod(x, 4) <= 1 && Math.floorMod(z, 4) == 2;
                    BlockState ceil = lamp ? (Hash.chance(s, x, z, 11, 0.2f) ? b("flickering_fluorescent_light") : b("dead_fluorescent_light"))
                            : b("overgrown_ceiling_tile");
                    w.set(x, CEIL, z, ceil);
                    w.set(x, CEIL + 1, z, b("overgrown_roof"));
                    if (!lay.wall(x, z) && Hash.chance(s, x, z, 12, moss > 0.55f ? 0.08f : 0.02f)) w.set(x, CEIL - 1, z, b("overgrown_hanging_vines"));
                }
            }
            lay.g.forCells(w, (i, k, x0, z0, wd, dp) -> {
                if (lay.g.unit(i, k, 900) >= 0.05f) return;
                tree(w, x0 + wd / 2, z0 + dp / 2);
            });
        }

        BlockState wallAt(long s, int x, int y, int z, float moss, boolean open) {
            float m = moss + (open ? 0.2f : 0f) - (y - F) * 0.06f + Hash.unit(s, x, y * 31 + z, 13) * 0.1f;
            if (Hash.chance(s, x, y * 17 + z, 14, 0.05f)) return b("overgrown_wallpaper_ivy");
            if (m > 0.72f) return b("overgrown_wallpaper_lost");
            if (m > 0.56f) return b("overgrown_wallpaper_mossier");
            if (y == F + 1) return m > 0.45f ? b("overgrown_wallpaper_mossier") : b("yellow_wallpaper_baseboard");
            return m > 0.45f ? b("overgrown_wallpaper") : b("yellow_wallpaper");
        }

        /** A tree that grew straight up through the Lobby and out of the roof, with vines you can climb. */
        void tree(ChunkWriter w, int tx, int tz) {
            int top = CEIL + 7;
            for (int dx = -3; dx <= 4; dx++)
                for (int dz = -3; dz <= 4; dz++) {
                    w.set(tx + dx, CEIL, tz + dz, ModBlocks.AIR);
                    w.set(tx + dx, CEIL + 1, tz + dz, ModBlocks.AIR);
                    w.set(tx + dx, CEIL - 1, tz + dz, ModBlocks.AIR);
                    w.set(tx + dx, F, tz + dz, b("overgrown_moss"));
                    for (int y = F + 1; y < CEIL - 1; y++) w.set(tx + dx, y, tz + dz, ModBlocks.AIR);
                }
            BlockState trunk = b("overgrown_trunk").setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    for (int y = F; y <= top; y++) w.set(tx + dx, y, tz + dz, trunk);
            // roots spreading over the floor
            for (int d = 2; d <= 4; d++) {
                w.set(tx - d, F, tz, b("overgrown_roots"));
                w.set(tx + 1 + d, F, tz + 1, b("overgrown_roots"));
                w.set(tx + 1, F, tz - d, b("overgrown_roots"));
            }
            for (int dx = -3; dx <= 4; dx++)
                for (int dz = -3; dz <= 4; dz++)
                    for (int y = top - 3; y <= top + 2; y++) {
                        double r = Math.sqrt(Math.pow(dx - 0.5, 2) + Math.pow(dz - 0.5, 2) + Math.pow((y - top) * 1.4, 2));
                        if (r > 4.2) continue;
                        if (dx == 0 && dz == 2) continue;                  // the vine's way up
                        if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1 && y <= top) continue;
                        w.set(tx + dx, y, tz + dz, b("overgrown_leaves"));
                    }
            BlockState vine = b("overgrown_climbing_vine").setValue(LadderBlock.FACING, Direction.SOUTH);
            for (int y = F + 1; y <= top; y++) w.set(tx, y, tz + 2, vine);
            w.set(tx, top + 1, tz + 2, b("overgrown_sunlight"));
            w.set(tx, top + 2, tz + 2, b("overgrown_sunlight"));
        }
    }

    /** Stand still where things grow and they grow over you. Meanwhile the Lobby keeps losing to the moss. */
    static final class Roots implements LevelBehavior {
        private final Map<UUID, Integer> still = new HashMap<>();

        @Override
        public void tickPlayer(ServerPlayer p, ServerLevel level, BackroomsLevel def) {
            if (p.isSpectator()) return;
            UUID id = p.getUUID();
            String under = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.blockPosition().below()).getBlock()).getPath();
            boolean growing = under.startsWith("overgrown_") || under.equals("moist_carpet");
            boolean moving = p.getDeltaMovement().horizontalDistanceSqr() > 0.0009;
            int t = growing && !moving && p.onGround() && !p.isCreative() ? still.getOrDefault(id, 0) + 1 : 0;
            still.put(id, t);
            if (t == 90) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                level.playSound(null, p.blockPosition(), ModSounds.get("event.overgrown_lobby_roots"), SoundSource.BLOCKS, 1f, 0.9f);
                p.displayClientMessage(Component.literal("Roots curl up over your shoes.").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
                if (level.getBlockState(p.blockPosition()).isAir()) level.setBlock(p.blockPosition(), ModBlocks.state("overgrown_fern"), Block.UPDATE_ALL);
            }
            if (t > 90 && t % 40 == 0) {
                p.hurt(level.damageSources().sweetBerryBush(), 1.5f);
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                Sanity.add(p, -3f);
            }
            if (p.tickCount % 40 == 0) {
                var r = p.getRandom();
                for (int n = 0; n < 3; n++) {
                    BlockPos at = p.blockPosition().offset(r.nextInt(25) - 12, r.nextInt(5) - 1, r.nextInt(25) - 12);
                    String name = BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getPath();
                    String to = switch (name) {
                        case "yellow_wallpaper" -> "overgrown_wallpaper";
                        case "overgrown_wallpaper" -> "overgrown_wallpaper_mossier";
                        case "yellow_wallpaper_baseboard" -> "overgrown_wallpaper_mossier";
                        case "moist_carpet" -> "overgrown_carpet";
                        default -> null;
                    };
                    if (to == null || DeepEndUtil.seen(p, Vec3.atCenterOf(at), 0.35)) continue;
                    level.setBlock(at, ModBlocks.state(to), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
