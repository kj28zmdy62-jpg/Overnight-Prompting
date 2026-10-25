package com.noclip.client.screen;

import com.noclip.Noclip;
import com.noclip.client.ClientState;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.ExitType;
import com.noclip.level.Levels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Level Catalogue: every level in the mod with a photo, its survival class,
 * description and where its exits lead. Opened from the title screen or with K.
 */
public class CatalogueScreen extends Screen {
    private static final int ROW = 22;
    private final Screen parent;
    private final List<BackroomsLevel> all = new ArrayList<>(Levels.all());
    private List<BackroomsLevel> shown = all;
    private int selected;
    private double scroll;
    private EditBox search;

    public CatalogueScreen(Screen parent) {
        super(Component.literal("Level Catalogue"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listW = listWidth();
        search = new EditBox(font, 12, 30, listW - 4, 16, Component.literal("Search"));
        search.setHint(Component.literal("Search levels..."));
        search.setResponder(this::filter);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width - 92, height - 28, 80, 20).build());
    }

    private int listWidth() {
        return Math.max(150, Math.min(220, width / 3));
    }

    private void filter(String q) {
        String s = q.toLowerCase(Locale.ROOT).trim();
        shown = s.isEmpty() ? all : all.stream().filter(l -> (l.displayNumber() + " " + l.title + " " + l.tagline + " " + l.id)
                .toLowerCase(Locale.ROOT).contains(s)).toList();
        selected = 0;
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF0101010);
        g.drawString(font, "NOCLIP  -  LEVEL CATALOGUE", 12, 12, 0xFFE8DC9A, false);
        String count = shown.size() + " / " + all.size() + " levels";
        g.drawString(font, count, listWidth() + 8 - font.width(count), 12, 0xFF8A8670, false);

        // list
        int listW = listWidth();
        int top = 52, bottom = height - 10;
        g.fill(10, top - 2, 10 + listW, bottom, 0xFF181816);
        g.enableScissor(10, top, 10 + listW, bottom);
        int y = top - (int) scroll;
        for (int i = 0; i < shown.size(); i++) {
            BackroomsLevel l = shown.get(i);
            if (y + ROW > top && y < bottom) {
                boolean sel = i == selected;
                boolean hover = mouseX >= 10 && mouseX < 10 + listW && mouseY >= y && mouseY < y + ROW;
                if (sel) g.fill(10, y, 10 + listW, y + ROW, 0xFF3A3420);
                else if (hover) g.fill(10, y, 10 + listW, y + ROW, 0xFF26241C);
                g.fill(10, y, 13, y + ROW, dangerColor(l.danger) | 0xFF000000);
                String name = l.number.isEmpty() ? l.title : "Level " + l.number;
                g.drawString(font, trim(name, listW - 16), 18, y + 3, sel ? 0xFFF2E8B0 : 0xFFD8D2B8, false);
                String sub = l.number.isEmpty() ? l.danger.label : l.title;
                g.drawString(font, trim(sub, listW - 16), 18, y + 12, 0xFF8A8670, false);
            }
            y += ROW;
        }
        g.disableScissor();

        if (!shown.isEmpty()) detail(g, shown.get(Math.min(selected, shown.size() - 1)), listW + 22);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void detail(GuiGraphics g, BackroomsLevel l, int x) {
        int w = width - x - 12;
        int imgW = Math.min(w, (int) ((height - 150) * 16 / 9f));
        int imgH = imgW * 9 / 16;
        int y = 30;
        ResourceLocation pic = Noclip.id("textures/gui/levels/" + l.id + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(pic).isPresent()) {
            g.blit(pic, x, y, 0, 0, imgW, imgH, imgW, imgH);
        } else {
            g.fill(x, y, x + imgW, y + imgH, 0xFF000000);
            g.drawCenteredString(font, "NO SIGNAL", x + imgW / 2, y + imgH / 2 - 4, 0xFF6A6A6A);
        }
        g.fill(x - 1, y - 1, x + imgW + 1, y, 0xFF5A5440);
        g.fill(x - 1, y + imgH, x + imgW + 1, y + imgH + 1, 0xFF5A5440);
        y += imgH + 8;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.6f, 1.6f, 1);
        g.drawString(font, l.displayNumber(), 0, 0, 0xFFF2F2EA, true);
        g.pose().popPose();
        y += 16;
        if (!l.number.isEmpty()) {
            g.drawString(font, l.title.toUpperCase(Locale.ROOT), x, y, 0xFFD8D2B8, false);
            y += 11;
        }
        g.drawString(font, "Survival Difficulty: " + l.danger.label + "  -  " + l.danger.description, x, y,
                dangerColor(l.danger) | 0xFF000000, false);
        y += 13;
        for (FormattedCharSequence line : font.split(Component.literal(l.tagline), w)) {
            g.drawString(font, line, x, y, 0xFFC8C2A8, false);
            y += 10;
        }
        y += 4;
        String exits = exits(l);
        if (!exits.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.literal("Exits lead to: " + exits), w)) {
                g.drawString(font, line, x, y, 0xFF8A8670, false);
                y += 10;
            }
        }
        boolean seen = ClientState.visited.contains(l.id);
        g.drawString(font, seen ? "You have been here." : "You have not been here. Yet.", x, height - 22,
                seen ? 0xFF9AD08A : 0xFF6A6656, false);
    }

    private static String exits(BackroomsLevel l) {
        Set<String> names = new LinkedHashSet<>();
        for (ExitType t : ExitType.values()) {
            var list = l.exits.get(t);
            if (list == null) continue;
            for (BackroomsLevel.WeightedTarget wt : list) {
                if ("overworld".equals(wt.level())) names.add("the real world");
                BackroomsLevel d = Levels.get(wt.level());
                if (d != null) names.add(d.number.isEmpty() ? d.title : "Level " + d.number);
            }
        }
        return String.join(", ", names);
    }

    private String trim(String s, int w) {
        return font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("...")) + "...";
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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int listW = listWidth();
        if (mx >= 10 && mx < 10 + listW && my >= 52 && my < height - 10) {
            int i = (int) ((my - 52 + scroll) / ROW);
            if (i >= 0 && i < shown.size()) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, Math.min(scroll - dy * ROW * 2, Math.max(0, shown.size() * ROW - (height - 62))));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!search.isFocused() || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            if (key == GLFW.GLFW_KEY_DOWN && selected < shown.size() - 1) {
                select(selected + 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP && selected > 0) {
                select(selected - 1);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void select(int i) {
        selected = i;
        int top = i * ROW, view = height - 62;
        if (top < scroll) scroll = top;
        if (top + ROW > scroll + view) scroll = top + ROW - view;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
