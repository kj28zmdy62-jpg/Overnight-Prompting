package com.noclip.registry;

import com.noclip.Noclip;
import com.noclip.entity.Deathmoth;
import com.noclip.entity.Faceling;
import com.noclip.entity.Hound;
import com.noclip.entity.Mannequin;
import com.noclip.entity.Partygoer;
import com.noclip.entity.ShadowFigure;
import com.noclip.entity.SkinStealer;
import com.noclip.entity.Smiler;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModEntities {
    public static final List<Item> SPAWN_EGGS = new ArrayList<>();
    /** Every registered entity by name, for the client renderer registry. */
    public static final Map<String, EntityType<? extends Mob>> ALL = new LinkedHashMap<>();

    public static EntityType<Hound> HOUND;
    public static EntityType<Smiler> SMILER;
    public static EntityType<Faceling> FACELING;
    public static EntityType<Mannequin> MANNEQUIN;
    public static EntityType<ShadowFigure> SHADOW_FIGURE;
    public static EntityType<SkinStealer> SKIN_STEALER;
    public static EntityType<Partygoer> PARTYGOER;
    public static EntityType<Deathmoth> DEATHMOTH;

    private ModEntities() {}

    public static void init() {
        HOUND = register("hound", Hound::new, MobCategory.MONSTER, 1.0f, 1.3f, Hound.createAttributes(), 0x1E1C1A, 0xB8AA98);
        SMILER = register("smiler", Smiler::new, MobCategory.MONSTER, 0.9f, 0.9f, Smiler.createAttributes(), 0x050505, 0xF6F2E4);
        FACELING = register("faceling", Faceling::new, MobCategory.CREATURE, 0.6f, 1.95f, Faceling.createAttributes(), 0xD9C8B4, 0x5D6B7A);
        MANNEQUIN = register("mannequin", Mannequin::new, MobCategory.MONSTER, 0.6f, 1.95f, Mannequin.createAttributes(), 0xE2DED6, 0x9A968E);
        SHADOW_FIGURE = register("shadow_figure", ShadowFigure::new, MobCategory.MONSTER, 0.6f, 2.3f, ShadowFigure.createAttributes(), 0x050505, 0x2A2A2E);
        SKIN_STEALER = register("skin_stealer", SkinStealer::new, MobCategory.MONSTER, 0.6f, 2.2f, SkinStealer.createAttributes(), 0xC9B6A2, 0x5A2A22);
        PARTYGOER = register("partygoer", Partygoer::new, MobCategory.MONSTER, 0.6f, 2.0f, Partygoer.createAttributes(), 0xF0D040, 0xE04070);
        DEATHMOTH = register("deathmoth", Deathmoth::new, MobCategory.MONSTER, 1.4f, 0.9f, Deathmoth.createAttributes(), 0x7A6C58, 0xE8DCC0);
        com.noclip.entity.EntityIndex.registerAll();
    }

    public static <T extends Mob> EntityType<T> register(String name, EntityType.EntityFactory<T> factory, MobCategory cat,
                                                        float w, float h, AttributeSupplier.Builder attrs, int egg1, int egg2) {
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, Noclip.id(name),
                EntityType.Builder.of(factory, cat).sized(w, h).clientTrackingRange(10).build(name));
        FabricDefaultAttributeRegistry.register(type, attrs);
        Item egg = Registry.register(BuiltInRegistries.ITEM, Noclip.id(name + "_spawn_egg"), new SpawnEggItem(type, egg1, egg2, new Item.Properties()));
        SPAWN_EGGS.add(egg);
        ALL.put(name, type);
        return type;
    }

    public static Supplier<EntityType<? extends Mob>> lazy(String name) {
        return () -> ALL.get(name);
    }
}
