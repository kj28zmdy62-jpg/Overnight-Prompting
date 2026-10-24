package com.noclip.levels;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Small shared helpers for the empty-building levels (Lift Lobby, Stairwell, Subway,
 * Archive, Museum, Call Center...): sight checks, in-world speech, and display
 * entities (floating placards and exhibits) built from NBT so no access wideners are needed.
 */
public final class EmptyBuildingKit {
    public static final String DISPLAY_TAG = "noclip_eb_display";

    private EmptyBuildingKit() {}

    /** True if the player could plausibly see this block right now. */
    public static boolean seen(ServerPlayer p, BlockPos pos) {
        return seen(p, Vec3.atCenterOf(pos), 0.2);
    }

    public static boolean seen(ServerPlayer p, Vec3 c, double cone) {
        Vec3 to = c.subtract(p.getEyePosition()).normalize();
        if (p.getViewVector(1f).dot(to) < cone) return false;
        return p.level().clip(new ClipContext(p.getEyePosition(), c, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
                .getType() == HitResult.Type.MISS;
    }

    /** A line of speech in chat: "Speaker: text". */
    public static void say(ServerPlayer p, String speaker, ChatFormatting color, String text) {
        MutableComponent c = Component.literal(speaker).withStyle(color, ChatFormatting.BOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(c);
    }

    /** A quiet stage direction in chat. */
    public static void narrate(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static CompoundTag base(Vec3 pos, float yaw, String billboard, float scale, String tag) {
        CompoundTag t = new CompoundTag();
        ListTag p = new ListTag();
        p.add(DoubleTag.valueOf(pos.x));
        p.add(DoubleTag.valueOf(pos.y));
        p.add(DoubleTag.valueOf(pos.z));
        t.put("Pos", p);
        ListTag r = new ListTag();
        r.add(FloatTag.valueOf(yaw));
        r.add(FloatTag.valueOf(0f));
        t.put("Rotation", r);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(DISPLAY_TAG));
        if (tag != null) tags.add(StringTag.valueOf(tag));
        t.put("Tags", tags);
        t.putString("billboard", billboard);
        t.putFloat("view_range", 0.6f);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0));
        tr.put("scale", floats(scale, scale, scale));
        t.put("transformation", tr);
        return t;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }

    /** Floating text. billboard: "fixed", "vertical", "center". background: ARGB. */
    public static Display.TextDisplay text(ServerLevel level, Vec3 pos, float yaw, Component text, String billboard,
                                           float scale, int background, int lineWidth, String tag) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(level);
        if (d == null) return null;
        CompoundTag t = base(pos, yaw, billboard, scale, tag);
        t.putString("text", Component.Serializer.toJson(text, level.registryAccess()));
        t.putInt("background", background);
        t.putInt("line_width", lineWidth);
        t.putBoolean("shadow", false);
        t.putString("alignment", "center");
        d.load(t);
        level.addFreshEntity(d);
        return d;
    }

    /** A floating item, e.g. an exhibit on a plinth. */
    public static Display.ItemDisplay item(ServerLevel level, Vec3 pos, float yaw, ItemStack stack, float scale, String tag) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(level);
        if (d == null || stack.isEmpty()) return null;
        CompoundTag t = base(pos, yaw, "fixed", scale, tag);
        t.put("item", stack.save(level.registryAccess()));
        t.putString("item_display", "fixed");
        d.load(t);
        level.addFreshEntity(d);
        return d;
    }

    /** Removes this kit's displays carrying {@code tag} around a point. */
    public static void clearDisplays(ServerLevel level, Vec3 around, double radius, String tag) {
        for (Entity e : level.getEntitiesOfClass(Display.class, new AABB(around, around).inflate(radius),
                e -> e.getTags().contains(DISPLAY_TAG) && (tag == null || e.getTags().contains(tag)))) {
            e.discard();
        }
    }

    public static String pretty(String levelId) {
        com.noclip.level.BackroomsLevel l = com.noclip.level.Levels.get(levelId);
        if (l == null) return levelId.replace('_', ' ');
        return l.number.isEmpty() ? l.title : "Level " + l.number + ", " + l.title;
    }
}
