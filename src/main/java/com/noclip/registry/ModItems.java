package com.noclip.registry;

import com.mojang.serialization.Codec;
import com.noclip.Noclip;
import com.noclip.item.AlmondWaterItem;
import com.noclip.item.FlashlightItem;
import com.noclip.item.JournalItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final List<Item> CREATIVE_ORDER = new ArrayList<>();

    public static final DataComponentType<Boolean> LIGHT_ON = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Noclip.id("light_on"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static Item ALMOND_WATER;
    public static Item FLASHLIGHT;
    public static Item BATTERY;
    public static Item JOURNAL;
    public static Item CANNED_BEANS;
    public static Item ENERGY_BAR;
    public static Item CAMCORDER;
    public static Item CHALK;

    private ModItems() {}

    public static void init() {
        ALMOND_WATER = register("almond_water", new AlmondWaterItem(new Item.Properties().stacksTo(16), 35f));
        FLASHLIGHT = register("flashlight", new FlashlightItem(new Item.Properties().durability(900)));
        BATTERY = register("battery", new Item(new Item.Properties().stacksTo(16)));
        JOURNAL = register("journal", new JournalItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
        CANNED_BEANS = register("canned_beans", new Item(new Item.Properties().food(
                new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build())));
        ENERGY_BAR = register("energy_bar", new Item(new Item.Properties().food(
                new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).fast().build())));
        CAMCORDER = register("camcorder", new com.noclip.item.CamcorderItem(new Item.Properties().stacksTo(1)));
        CHALK = register("chalk", new com.noclip.item.ChalkItem(new Item.Properties().durability(64)));
    }

    public static Item register(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, Noclip.id(name), item);
        CREATIVE_ORDER.add(item);
        return item;
    }
}
