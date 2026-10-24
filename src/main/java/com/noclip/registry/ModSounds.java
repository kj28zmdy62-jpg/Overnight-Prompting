package com.noclip.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.noclip.Noclip;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Registers one SoundEvent per entry in assets/noclip/sounds.json. */
public final class ModSounds {
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    public static SoundEvent NOCLIP;

    private ModSounds() {}

    public static void init() {
        try (InputStream in = ModSounds.class.getResourceAsStream("/assets/noclip/sounds.json")) {
            if (in == null) throw new IllegalStateException("Missing sounds.json");
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : o.keySet()) {
                SoundEvent e = SoundEvent.createVariableRangeEvent(Noclip.id(key));
                Registry.register(BuiltInRegistries.SOUND_EVENT, Noclip.id(key), e);
                SOUNDS.put(key, e);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        NOCLIP = get("event.noclip");
    }

    public static SoundEvent get(String name) {
        SoundEvent e = SOUNDS.get(name);
        if (e == null) throw new IllegalArgumentException("Unknown noclip sound '" + name + "'");
        return e;
    }

    public static SoundEvent getOrNull(String name) {
        return SOUNDS.get(name);
    }
}
