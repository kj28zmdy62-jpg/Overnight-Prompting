package com.noclip.client.screen;

import com.noclip.client.ClientState;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The wanderer's journal: every level, the ones you have seen filled in by hand. */
public class JournalScreen extends Screen {
    private double scroll;

    /** Notes the wanderer wrote to themself on the first page. */
    private static final String[] NOTES = {
            "Notes to self:",
            "- Almond water keeps your head clear. Drink it.",
            "- Lean into a wall long enough and it lets you through.",
            "- Hounds hear you. Crouch. Don't run.",
            "- Smilers hate being stared at. Don't light them up.",
            "- If you hear footsteps when you stop, keep walking.",
            "- The camcorder sees what I can't.",
            "- Never turn your back on a mannequin.",
            "- Stay with people. It's easier with people.",
            "- Look for doors, holes, stairs, signs. Every place has a way out."
    };

    public JournalScreen() {
        super(Component.translatable("item.noclip.journal"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = Math.min(340, width - 20);
        int x0 = (width - w) / 2, y0 = 16, y1 = height - 16;
        g.fill(x0 - 6, y0 - 6, x0 + w + 6, y1 + 6, 0xF0E8DCBE);
        g.fill(x0 - 6, y0 - 6, x0 + w + 6, y0 - 5, 0xFF6B4A2A);
        g.fill(x0 - 6, y1 + 5, x0 + w + 6, y1 + 6, 0xFF6B4A2A);
        List<BackroomsLevel> all = new ArrayList<>(Levels.all());
        String head = "WANDERER'S JOURNAL   " + ClientState.visited.size() + " / " + all.size() + " levels";
        g.drawString(font, head, x0, y0, 0xFF3A2A1A, false);
        g.enableScissor(x0 - 4, y0 + 14, x0 + w + 4, y1);
        int y = y0 + 16 - (int) scroll;
        for (String note : NOTES) {
            if (y > y0 && y < y1) g.drawString(font, note, x0 + 4, y, 0xFF4A3A26, false);
            y += 10;
        }
        y += 8;
        for (BackroomsLevel l : all) {
            boolean seen = ClientState.visited.contains(l.id);
            if (y > y0 && y < y1) {
                if (seen) {
                    g.drawString(font, l.displayNumber() + (l.number.isEmpty() ? "" : "  -  " + l.title), x0, y, 0xFF2A1E12, false);
                    g.drawString(font, l.tagline, x0 + 6, y + 10, 0xFF5A4A36, false);
                    g.drawString(font, l.danger.label, x0 + w - font.width(l.danger.label), y, 0xFF7A3A2A, false);
                } else {
                    g.drawString(font, "LEVEL ???", x0, y, 0xFF9A8A70, false);
                    g.drawString(font, "You have not been here. Yet.", x0 + 6, y + 10, 0xFFB0A288, false);
                }
            }
            y += 24;
        }
        g.disableScissor();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, all.size() * 24 + NOTES.length * 10 + 8 - (y1 - y0 - 20))));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        scroll -= dy * 18;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
