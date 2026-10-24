package com.noclip;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.levels.LevelCatalog;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModItems;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import com.noclip.sanity.Sanity;
import com.noclip.travel.NoclipCommand;
import com.noclip.travel.NoclipTriggers;
import com.noclip.travel.Travel;
import com.noclip.world.BackroomsChunkGenerator;
import com.noclip.world.EntityDirector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Noclip implements ModInitializer {
    public static final String MOD_ID = "noclip";
    public static final Logger LOG = LoggerFactory.getLogger("NOCLIP");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        PlayerData.init();
        NoclipNet.init();
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("backrooms"), BackroomsChunkGenerator.CODEC);
        LevelCatalog.registerAll();
        LOG.info("{} Backrooms levels registered", Levels.count());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.noclip"))
                .icon(() -> new ItemStack(ModItems.ALMOND_WATER))
                .displayItems((params, out) -> {
                    ModItems.CREATIVE_ORDER.forEach(out::accept);
                    ModEntities.SPAWN_EGGS.forEach(out::accept);
                    ModBlocks.CREATIVE_ORDER.forEach(out::accept);
                })
                .build());

        NoclipTriggers.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> NoclipCommand.register(dispatcher));

        ServerPlayNetworking.registerGlobalReceiver(NoclipNet.WallPushPayload.TYPE,
                (payload, ctx) -> ctx.server().execute(() -> NoclipTriggers.onWallPush(ctx.player(), payload.ticks())));

        ServerTickEvents.END_WORLD_TICK.register(this::tickLevel);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            Sanity.sync(p, true);
            BackroomsLevel def = Levels.get(p.level().dimension());
            ServerPlayNetworking.send(p, new NoclipNet.JournalPayload(String.join(",", PlayerData.visited(p))));
            if (def != null) ServerPlayNetworking.send(p, new NoclipNet.EnterPayload(def.id, false, PlayerData.visited(p).size()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Sanity.forget(handler.getPlayer());
            Travel.forget(handler.getPlayer());
            NoclipTriggers.forget(handler.getPlayer());
        });
    }

    private void tickLevel(ServerLevel level) {
        BackroomsLevel def = Levels.get(level.dimension());
        for (ServerPlayer p : level.players()) {
            Sanity.tick(p);
            if (def != null) {
                def.behavior.tickPlayer(p, level, def);
                EntityDirector.tick(p, level, def);
            }
        }
        if (def != null && !level.players().isEmpty()) def.behavior.tickLevel(level, def);
    }
}
