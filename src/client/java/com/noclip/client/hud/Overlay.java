package com.noclip.client.hud;

import com.noclip.client.ClientState;
import com.noclip.item.FlashlightItem;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The camcorder HUD: blinking REC, a running timestamp, a battery meter while the
 * flashlight is out, and the title card that types itself out when you arrive.
 */
public final class Overlay {
    private Overlay() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        BackroomsLevel level = ClientState.level;
        int w = g.guiWidth(), h = g.guiHeight();
        Font font = mc.font;
        long now = System.currentTimeMillis();

        if (level != null && level.visuals.recOverlay) {
            int a = 0xB0;
            if ((now / 600) % 2 == 0) g.fill(10, 11, 16, 17, (a << 24) | 0xE02020);
            g.drawString(font, "REC", 20, 10, (a << 24) | 0xF0F0F0, false);
            long secs = ClientState.millisInLevel() / 1000 + 3 * 3600 + 12 * 60;
            String stamp = String.format("AUG 14 1996  %02d:%02d:%02d", (secs / 3600) % 24, (secs / 60) % 60, secs % 60);
            g.drawString(font, stamp, w - font.width(stamp) - 10, h - 18, (a << 24) | 0xF0F0F0, false);
            g.drawString(font, "SP", 10, h - 18, (0x80 << 24) | 0xF0F0F0, false);
            // your pulse, as the camera's biometric strap reads it
            float ins = ClientState.insanity();
            int bpm = (int) (68 + ins * 82 + Math.sin(now / 700.0) * 2 + (ClientState.pulse > 0.3f ? 6 : 0));
            boolean beat = (now % (60000 / Math.max(40, bpm))) < 120;
            int col = ins > 0.6f ? 0xE04040 : ins > 0.3f ? 0xE0B060 : 0xD0D0D0;
            String hr = (beat ? "\u2665 " : "  ") + bpm + " BPM";
            g.drawString(font, hr, 10, h - 30, (0xA0 << 24) | col, false);
        }

        ItemStack light = flashlight(mc);
        if (!light.isEmpty()) {
            float frac = 1f - light.getDamageValue() / (float) light.getMaxDamage();
            int x = w - 34, y = 10;
            int col = frac > 0.25f ? 0xD0E8E8E8 : ((now / 300) % 2 == 0 ? 0xD0E03030 : 0x60E03030);
            g.fill(x, y, x + 20, y + 1, col);
            g.fill(x, y + 8, x + 20, y + 9, col);
            g.fill(x, y, x + 1, y + 9, col);
            g.fill(x + 19, y, x + 20, y + 9, col);
            g.fill(x + 20, y + 3, x + 22, y + 6, col);
            int bars = Math.round(frac * 4);
            for (int i = 0; i < bars; i++) g.fill(x + 2 + i * 4, y + 2, x + 5 + i * 4, y + 7, col);
        }

        if (level != null) titleCard(g, font, level, w, h);
    }

    private static ItemStack flashlight(Minecraft mc) {
        ItemStack m = mc.player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (m.getItem() instanceof FlashlightItem) return m;
        ItemStack o = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (o.getItem() instanceof FlashlightItem) return o;
        return ItemStack.EMPTY;
    }

    private static void titleCard(GuiGraphics g, Font font, BackroomsLevel level, int w, int h) {
        long t = ClientState.millisInLevel();
        if (t > 7500) return;
        float fadeIn = Math.min(1f, Math.max(0f, (t - 900) / 400f));
        float fadeOut = Math.min(1f, Math.max(0f, (7500 - t) / 1200f));
        float alpha = fadeIn * fadeOut;
        if (alpha <= 0.02f) return;
        int a = (int) (alpha * 255) << 24;

        String number = level.displayNumber();
        int typed = (int) Math.min(number.length(), Math.max(0, (t - 900) / 70));
        String shown = number.substring(0, typed);
        int jitter = (t / 50) % 23 == 0 ? 2 : 0;

        g.pose().pushPose();
        float scale = 3f;
        g.pose().translate(w / 2f, h * 0.32f, 0);
        g.pose().scale(scale, scale, 1);
        int tw = font.width(number);
        g.drawString(font, shown, -tw / 2 + jitter, 0, a | 0xF2F2EA, true);
        g.pose().popPose();

        if (t > 900 + number.length() * 70L + 300) {
            float sub = Math.min(1f, (t - (900 + number.length() * 70L + 300)) / 600f) * fadeOut;
            int sa = (int) (sub * 255) << 24;
            String title = level.number.isEmpty() ? level.tagline : level.title.toUpperCase();
            g.pose().pushPose();
            g.pose().translate(w / 2f, h * 0.32f + 30, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawString(font, title, -font.width(title) / 2, 0, sa | 0xD8D2B8, true);
            g.pose().popPose();
            if (!level.number.isEmpty()) {
                String tag = level.tagline;
                g.drawString(font, tag, w / 2 - font.width(tag) / 2, (int) (h * 0.32f) + 48, sa | 0xA8A290, true);
            }
            String danger = "Survival Difficulty: " + level.danger.label + "  -  " + level.danger.description;
            g.drawString(font, danger, w / 2 - font.width(danger) / 2, (int) (h * 0.32f) + 62, sa | dangerColor(level.danger), true);
            if (ClientState.firstVisit) {
                String found = "NEW LEVEL DISCOVERED  " + ClientState.discovered + " / " + Levels.count();
                g.drawString(font, found, w / 2 - font.width(found) / 2, (int) (h * 0.32f) + 80, sa | 0xE8C860, true);
            }
        }
    }

    private static int dangerColor(BackroomsLevel.Danger d) {
        return switch (d) {
            case SAFE -> 0x7FD07F;
            case LOW -> 0xC8D07F;
            case MODERATE -> 0xE0B060;
            case HIGH -> 0xE07850;
            case DEADLY -> 0xE03838;
            case UNKNOWN -> 0xA080D0;
        };
    }
}
