package com.noclip.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.noclip.Noclip;
import com.noclip.block.ExitBlock;
import com.noclip.block.FlickerLightBlock;
import com.noclip.block.HazardBlock;
import com.noclip.block.PickupBlock;
import com.noclip.block.ShapedBlock;
import com.noclip.level.ExitType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers every block declared in {@code /noclip/blocks.json}. That file is
 * written by {@code tools/gen_assets.py}, which also writes the textures,
 * models and blockstates, so the Java side and the assets never drift apart.
 */
public final class ModBlocks {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, String> RENDER_LAYERS = new HashMap<>();
    public static final List<Block> CREATIVE_ORDER = new ArrayList<>();

    private ModBlocks() {}

    public static void init() {
        JsonArray specs = readSpec();
        // Two passes: stairs/slabs need their base block registered first.
        List<JsonObject> deferred = new ArrayList<>();
        for (JsonElement el : specs) {
            JsonObject o = el.getAsJsonObject();
            String kind = o.get("kind").getAsString();
            if (kind.equals("stairs")) deferred.add(o);
            else register(o);
        }
        for (JsonObject o : deferred) register(o);
        Noclip.LOG.info("Registered {} Backrooms blocks", BLOCKS.size());
    }

    private static JsonArray readSpec() {
        try (InputStream in = ModBlocks.class.getResourceAsStream("/noclip/blocks.json")) {
            if (in == null) throw new IllegalStateException("Missing /noclip/blocks.json");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void register(JsonObject o) {
        String name = o.get("name").getAsString();
        String kind = o.get("kind").getAsString();
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW);
        p.sound(sound(str(o, "sound", "stone")));
        if (bool(o, "unbreakable", false)) p.strength(-1f, 3600000f).noLootTable().pushReaction(PushReaction.BLOCK);
        else p.strength(num(o, "strength", 1.5f));
        int light = (int) num(o, "light", 0);
        if (light > 0) {
            if (kind.equals("flicker")) p.lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? light : 0);
            else p.lightLevel(s -> light);
        }
        String render = str(o, "render", "solid");
        if (!render.equals("solid") || kind.equals("shaped") || kind.equals("pickup") || kind.equals("door") || kind.equals("trapdoor") || kind.equals("ladder") || kind.equals("pane") || kind.equals("fence")) p.noOcclusion();
        if (bool(o, "no_collision", false)) p.noCollission();
        if (bool(o, "emissive", false)) p.emissiveRendering((s, l, pos) -> true).hasPostProcess((s, l, pos) -> true);
        if (render.equals("translucent") || render.equals("cutout")) {
            p.isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isValidSpawn((s, l, pos, t) -> false);
        }

        Block block = switch (kind) {
            case "cube" -> new Block(p);
            case "pillar" -> new RotatedPillarBlock(p);
            case "light" -> new Block(p);
            case "flicker" -> new FlickerLightBlock(p, num(o, "flicker", 0.25f));
            case "glass" -> new TransparentBlock(p);
            case "slab" -> new SlabBlock(p);
            case "stairs" -> new StairBlock(get(str(o, "base", "")).defaultBlockState(), p);
            case "pane" -> new IronBarsBlock(p);
            case "fence" -> new FenceBlock(p);
            case "carpet" -> new CarpetBlock(p);
            case "shaped" -> new ShapedBlock(p, boxes(o), bool(o, "collides", true));
            case "pickup" -> new PickupBlock(p, boxes(o), str(o, "item", "noclip:almond_water"));
            case "hazard" -> new HazardBlock(p, num(o, "damage", 2f), bool(o, "solid", true));
            case "exit" -> new ExitBlock(p, ExitType.valueOf(str(o, "exit", "DOOR")),
                    ExitBlock.Trigger.valueOf(str(o, "trigger", "INSIDE")), bool(o, "solid", false));
            case "door" -> new DoorBlock(str(o, "sound", "wood").equals("wood") ? BlockSetType.OAK : BlockSetType.COPPER, p);
            case "trapdoor" -> new TrapDoorBlock(str(o, "sound", "metal").equals("wood") ? BlockSetType.OAK : BlockSetType.COPPER, p);
            case "ladder" -> new LadderBlock(p);
            case "class" -> reflect(str(o, "class", ""), p);
            default -> throw new IllegalArgumentException("Unknown block kind " + kind + " for " + name);
        };
        ResourceLocation id = Noclip.id(name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
        BLOCKS.put(name, block);
        if (!bool(o, "hidden", false) && !name.startsWith("flashlight_glow")) CREATIVE_ORDER.add(block);
        if (!render.equals("solid")) RENDER_LAYERS.put(name, render);
    }

    /** Custom block classes live next to their level; they only need a (Properties) constructor. */
    private static Block reflect(String cls, BlockBehaviour.Properties p) {
        try {
            return (Block) Class.forName(cls).getConstructor(BlockBehaviour.Properties.class).newInstance(p);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create block class " + cls, e);
        }
    }

    private static List<double[]> boxes(JsonObject o) {
        List<double[]> out = new ArrayList<>();
        if (!o.has("boxes")) {
            out.add(new double[]{0, 0, 0, 16, 16, 16});
            return out;
        }
        for (JsonElement el : o.getAsJsonArray("boxes")) {
            JsonArray a = el.getAsJsonArray();
            double[] b = new double[6];
            for (int i = 0; i < 6; i++) b[i] = a.get(i).getAsDouble();
            out.add(b);
        }
        return out;
    }

    private static SoundType sound(String s) {
        return switch (s) {
            case "wool" -> SoundType.WOOL;
            case "metal" -> SoundType.METAL;
            case "glass" -> SoundType.GLASS;
            case "wood" -> SoundType.WOOD;
            case "gravel" -> SoundType.GRAVEL;
            case "sand" -> SoundType.SAND;
            case "grass" -> SoundType.GRASS;
            case "tile" -> SoundType.DEEPSLATE_TILES;
            case "mud" -> SoundType.MUD;
            case "snow" -> SoundType.SNOW;
            case "copper" -> SoundType.COPPER;
            case "chain" -> SoundType.CHAIN;
            case "lantern" -> SoundType.LANTERN;
            case "bone" -> SoundType.BONE_BLOCK;
            case "moss" -> SoundType.MOSS;
            case "concrete" -> SoundType.STONE;
            default -> SoundType.STONE;
        };
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) ? o.get(k).getAsString() : def;
    }

    private static float num(JsonObject o, String k, float def) {
        return o.has(k) ? o.get(k).getAsFloat() : def;
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        return o.has(k) ? o.get(k).getAsBoolean() : def;
    }

    public static Block get(String name) {
        Block b = BLOCKS.get(name);
        if (b == null) throw new IllegalArgumentException("Unknown noclip block '" + name + "'");
        return b;
    }

    public static BlockState state(String name) {
        return get(name).defaultBlockState();
    }

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();
}
