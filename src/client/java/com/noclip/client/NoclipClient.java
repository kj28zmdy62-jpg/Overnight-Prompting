package com.noclip.client;

import com.noclip.client.hud.Overlay;
import com.noclip.client.render.BackroomsPost;
import com.noclip.client.screen.JournalScreen;
import com.noclip.client.sound.Hallucinations;
import com.noclip.item.JournalItem;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.net.NoclipNet;
import com.noclip.registry.ModBlocks;
import com.noclip.registry.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Arrays;

public class NoclipClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModBlocks.RENDER_LAYERS.forEach((name, layer) -> BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.get(name),
                switch (layer) {
                    case "translucent" -> RenderType.translucent();
                    case "cutout_mipped" -> RenderType.cutoutMipped();
                    default -> RenderType.cutout();
                }));

        for (BackroomsLevel l : Levels.all()) {
            if (!l.visuals.sky) DimensionRenderingRegistry.registerSkyRenderer(l.dimension, ctx -> {});
            if (!l.visuals.clouds) DimensionRenderingRegistry.registerCloudRenderer(l.dimension, ctx -> {});
            if (!l.visuals.weather) DimensionRenderingRegistry.registerWeatherRenderer(l.dimension, ctx -> {});
        }

        ClientPlayNetworking.registerGlobalReceiver(NoclipNet.SanityPayload.TYPE,
                (p, ctx) -> ClientState.sanity = p.sanity());
        ClientPlayNetworking.registerGlobalReceiver(NoclipNet.EnterPayload.TYPE, (p, ctx) -> {
            ClientState.enter(p.level(), p.firstVisit(), p.discovered());
            if (p.firstVisit() && ctx.player() != null) {
                ctx.client().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        ModSounds.get("event.sting"), 1f, 0.6f));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NoclipNet.JournalPayload.TYPE, (p, ctx) -> {
            ClientState.visited = p.visited().isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p.visited().split(",")));
            if (ctx.player() != null && ctx.player().getMainHandItem().getItem() instanceof JournalItem
                    && ctx.client().screen == null && ctx.player().isUsingItem() == false && journalRequested) {
                journalRequested = false;
                ctx.client().setScreen(new JournalScreen());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NoclipNet.EventPayload.TYPE, (p, ctx) -> ClientEvents.handle(p.event(), p.strength()));
        JournalItem.openScreen = () -> journalRequested = true;

        com.noclip.client.render.EntityRenderers.init();
        HudRenderCallback.EVENT.register(Overlay::render);
        com.noclip.client.dev.AutoTest.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return com.noclip.Noclip.id("post_reload");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                BackroomsPost.reset();
            }
        });
    }

    private static boolean journalRequested;

    private void tick(Minecraft mc) {
        ClientState.tick(mc);
        ClientState.camcorder = mc.player != null && com.noclip.item.CamcorderItem.viewing(mc.player);
        Hallucinations.tick(mc);
        if (mc.player == null) return;
        boolean pushing = mc.player.horizontalCollision && mc.player.input.forwardImpulse > 0.1f
                && !mc.player.isShiftKeyDown() && mc.player.onGround();
        ClientState.wallPushTicks = pushing ? ClientState.wallPushTicks + 1 : Math.max(0, ClientState.wallPushTicks - 3);
        if (pushing && ClientState.wallPushTicks >= 100 && ClientState.wallPushTicks % 20 == 0) {
            ClientPlayNetworking.send(new NoclipNet.WallPushPayload(ClientState.wallPushTicks));
        }
        if (pushing && ClientState.wallPushTicks == 60) {
            mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    ModSounds.get("event.wall_thin"), SoundSource.PLAYERS, 0.5f, 1f, mc.player.getRandom(),
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
    }
}
