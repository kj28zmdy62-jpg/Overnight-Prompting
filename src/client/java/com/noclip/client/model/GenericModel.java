package com.noclip.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;

/**
 * Animates any generated model by part name: head looks, arms and legs swing,
 * quadruped legs trot, wings flap, jaws work. Style flags tune the motion.
 */
public class GenericModel<T extends Mob> extends HierarchicalModel<T> {
    public enum Style { HUMANOID, ZOMBIE, STIFF, QUADRUPED, FLYER, FLOAT }

    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final Style style;

    public GenericModel(ModelPart root, Style style) {
        this.root = root;
        this.style = style;
        collect(root);
    }

    private void collect(ModelPart p) {
        p.getAllParts().forEach(part -> {});
        for (String name : new String[]{"head", "body", "jaw", "right_arm", "left_arm", "right_leg", "left_leg",
                "leg_fl", "leg_fr", "leg_bl", "leg_br", "wing_l", "wing_r", "tail", "antenna_l", "antenna_r"}) {
            ModelPart found = find(root, name);
            if (found != null) parts.put(name, found);
        }
    }

    private static ModelPart find(ModelPart p, String name) {
        if (p.hasChild(name)) return p.getChild(name);
        for (ModelPart child : p.getAllParts().toList()) {
            if (child != p && child.hasChild(name)) return child.getChild(name);
        }
        return null;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T e, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        ModelPart head = parts.get("head");
        if (head != null && style != Style.STIFF) {
            head.yRot += yaw * Mth.DEG_TO_RAD;
            head.xRot += pitch * Mth.DEG_TO_RAD;
        }
        float swing = Mth.cos(limbSwing * 0.6662f);
        float swingB = Mth.cos(limbSwing * 0.6662f + Mth.PI);
        switch (style) {
            case HUMANOID, ZOMBIE, STIFF -> {
                float k = style == Style.STIFF ? 0.6f : 1.4f;
                rot("right_leg", swing * k * amount, 0, 0);
                rot("left_leg", swingB * k * amount, 0, 0);
                if (style == Style.ZOMBIE && e.isAggressive()) {
                    rot("right_arm", -1.45f + Mth.sin(age * 0.09f) * 0.05f, 0, 0);
                    rot("left_arm", -1.45f - Mth.sin(age * 0.09f) * 0.05f, 0, 0);
                } else {
                    rot("right_arm", swingB * k * 0.7f * amount, 0, 0);
                    rot("left_arm", swing * k * 0.7f * amount, 0, 0);
                }
                if (style != Style.STIFF) {
                    rot("right_arm", 0, 0, Mth.cos(age * 0.09f) * 0.04f + 0.04f);
                    rot("left_arm", 0, 0, -Mth.cos(age * 0.09f) * 0.04f - 0.04f);
                }
            }
            case QUADRUPED -> {
                rot("leg_fl", swing * 1.2f * amount, 0, 0);
                rot("leg_br", swing * 1.2f * amount, 0, 0);
                rot("leg_fr", swingB * 1.2f * amount, 0, 0);
                rot("leg_bl", swingB * 1.2f * amount, 0, 0);
                ModelPart body = parts.get("body");
                if (body != null) body.y += Mth.sin(limbSwing * 1.3324f) * amount * 0.6f;
                if (head != null) head.y += Mth.sin(limbSwing * 1.3324f + 0.5f) * amount * 0.6f;
            }
            case FLYER -> {
                float flap = Mth.sin(age * 1.2f) * 0.75f;
                rot("wing_l", 0, 0, -flap);
                rot("wing_r", 0, 0, flap);
                float legs = Mth.sin(age * 0.2f) * 0.15f;
                rot("leg_fl", 0.3f + legs, 0, 0);
                rot("leg_fr", 0.3f - legs, 0, 0);
                rot("leg_bl", 0.3f - legs, 0, 0);
                rot("leg_br", 0.3f + legs, 0, 0);
                rot("antenna_l", Mth.sin(age * 0.15f) * 0.1f, 0, 0);
                rot("antenna_r", Mth.cos(age * 0.15f) * 0.1f, 0, 0);
                root.y += Mth.sin(age * 0.25f) * 1.5f;
            }
            case FLOAT -> root.y += Mth.sin(age * 0.08f) * 1.2f;
        }
        ModelPart jaw = parts.get("jaw");
        if (jaw != null) jaw.xRot += 0.15f + Mth.sin(age * 0.25f) * 0.1f + (e.isAggressive() ? 0.35f : 0f);
    }

    private void rot(String name, float x, float y, float z) {
        ModelPart p = parts.get(name);
        if (p == null) return;
        p.xRot += x;
        p.yRot += y;
        p.zRot += z;
    }
}
