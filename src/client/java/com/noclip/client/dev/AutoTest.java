package com.noclip.client.dev;

import com.noclip.Noclip;
import com.noclip.registry.ModItems;
import com.noclip.travel.Travel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Development only. With NOCLIP_AUTOTEST=level_0,level_1 set, the client makes a
 * fresh world, visits each level, takes screenshots from a few angles and quits.
 * Used to check every level actually generates and looks right.
 */
public final class AutoTest {
    private static Deque<String> queue;
    private static int phase, timer;
    private static String current;
    private static boolean flashlight;
    private static int settle;

    private AutoTest() {}

    public static void init() {
        String spec = System.getenv("NOCLIP_AUTOTEST");
        if (spec == null || spec.isBlank()) return;
        queue = new ArrayDeque<>(Arrays.asList(spec.split(",")));
        flashlight = "1".equals(System.getenv("NOCLIP_AUTOTEST_FLASH"));
        settle = Integer.parseInt(System.getenv().getOrDefault("NOCLIP_AUTOTEST_SETTLE", "160"));
        ClientTickEvents.END_CLIENT_TICK.register(AutoTest::tick);
    }

    private static void tick(Minecraft mc) {
        timer++;
        switch (phase) {
            case 0 -> {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof com.noclip.client.screen.CatalogueScreen)) {
                    timer = 0;
                    return;
                }
                if (timer == 60) shot(mc, "title_screen");
                if (System.getenv("NOCLIP_AUTOTEST_CATALOGUE") != null) {
                    if (timer == 80) mc.setScreen(new com.noclip.client.screen.CatalogueScreen(mc.screen));
                    if (timer == 120) shot(mc, "catalogue_0");
                    if (timer == 125) mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN, 0, 0);
                    if (timer == 126) for (int k = 0; k < 16; k++) mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN, 0, 0);
                    if (timer == 160) shot(mc, "catalogue_1");
                    if (timer == 170) mc.stop();
                    return;
                }
                if (timer > 70) {
                    LevelSettings s = new LevelSettings("autotest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("autotest", s, new WorldOptions(1234L, false, false),
                            reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                    phase = 1;
                    timer = 0;
                }
            }
            case 1 -> {
                if (mc.player != null && mc.level != null && timer > 60) {
                    phase = 2;
                    timer = 0;
                }
            }
            case 2 -> {
                if (queue.isEmpty()) {
                    Noclip.LOG.info("AUTOTEST DONE");
                    mc.stop();
                    phase = 99;
                    return;
                }
                current = queue.poll();
                String id = current;
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
                    if (sp == null) return;
                    sp.setGameMode(GameType.CREATIVE);
                    if (flashlight) {
                        ItemStack fl = new ItemStack(ModItems.FLASHLIGHT);
                        fl.set(ModItems.LIGHT_ON, true);
                        sp.getInventory().items.set(sp.getInventory().selected, fl);
                    }
                    boolean ok = Travel.sendTo(sp, id);
                    Noclip.LOG.info("AUTOTEST travel {} -> {}", id, ok);
                    if (id.equals("overworld")) return;
                });
                phase = 3;
                timer = 0;
            }
            case 3 -> {
                Player p = mc.player;
                if (p == null) return;
                if (System.getenv("NOCLIP_AUTOTEST_THUMB") != null) {
                    // one clean shot per level for the catalogue: no HUD, facing the open view
                    mc.options.hideGui = true;
                    com.noclip.client.ClientState.enteredAt = System.currentTimeMillis() - 60000;
                    if (timer == settle) shot(mc, "thumb_" + current);
                    if (timer > settle + 2) {
                        phase = 2;
                        timer = 0;
                    }
                    return;
                }
                if (System.getenv("NOCLIP_AUTOTEST_PANORAMA") != null) {
                    panorama(mc, p);
                    return;
                }
                if (timer == 50) shot(mc, current + "_card");
                if (timer == settle - 40 && System.getenv("NOCLIP_AUTOTEST_ENTITIES") != null) spawnLineup(mc);
                int[] at = {settle, settle + 40, settle + 80, settle + 120};
                for (int i = 0; i < at.length; i++) {
                    if (timer == at[i] - 20) {
                        p.setYRot(p.getYRot() + 90f);
                        p.setXRot(i == 3 ? 25f : 0f);
                    }
                    if (timer == at[i]) shot(mc, current + "_" + i);
                }
                if (timer > at[at.length - 1] + 5) {
                    phase = 2;
                    timer = 0;
                }
            }
            default -> {
            }
        }
    }

    private static float panoYaw;

    /** Captures the six faces of a cube map for the title screen panorama. */
    private static void panorama(Minecraft mc, Player p) {
        com.noclip.client.ClientState.panorama = true;
        mc.options.hideGui = true;
        mc.options.bobView().set(false);
        mc.options.fov().set(90);
        if (timer == 5) panoYaw = p.getYRot();
        float[][] faces = {{0, 0}, {90, 0}, {180, 0}, {270, 0}, {0, -90}, {0, 90}};
        for (int i = 0; i < 6; i++) {
            int at = settle + i * 30;
            if (timer == at - 15) {
                p.setYRot(panoYaw + faces[i][0]);
                p.setXRot(faces[i][1]);
                p.yRotO = p.getYRot();
                p.xRotO = p.getXRot();
            }
            if (timer == at) shot(mc, "panorama_" + i);
        }
        if (timer > settle + 6 * 30) {
            phase = 2;
            timer = 0;
        }
    }

    /** Places each listed entity in a row in front of the camera, frozen, for model checks. */
    private static void spawnLineup(Minecraft mc) {
        String[] names = System.getenv("NOCLIP_AUTOTEST_ENTITIES").split(",");
        var server = mc.getSingleplayerServer();
        float yaw = mc.player.getYRot() + 90f * 1;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
            double rad = Math.toRadians(yaw);
            double fx = -Math.sin(rad), fz = Math.cos(rad);
            for (int i = 0; i < names.length; i++) {
                var type = com.noclip.registry.ModEntities.ALL.get(names[i]);
                if (type == null) continue;
                var e = type.create(sp.serverLevel());
                double side = (i - (names.length - 1) / 2.0) * 1.8;
                e.moveTo(sp.getX() + fx * 5 - fz * side, sp.getY(), sp.getZ() + fz * 5 + fx * side, yaw + 180f, 0f);
                e.setNoAi(true);
                e.setYHeadRot(yaw + 180f);
                e.setYBodyRot(yaw + 180f);
                e.setPersistenceRequired();
                sp.serverLevel().addFreshEntity(e);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(),
                c -> Noclip.LOG.info("AUTOTEST screenshot {}", name));
    }
}
