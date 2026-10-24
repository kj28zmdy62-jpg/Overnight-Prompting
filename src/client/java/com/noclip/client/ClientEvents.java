package com.noclip.client;

/** Scripted client effects triggered by the server (scares, flashes, blackouts). */
public final class ClientEvents {
    private ClientEvents() {}

    public static void handle(String event, float strength) {
        switch (event) {
            case "flash" -> ClientState.noclipFlash = Math.max(ClientState.noclipFlash, strength);
            case "pulse" -> ClientState.pulse = Math.max(ClientState.pulse, strength);
            case "blackout" -> ClientState.blackoutUntil = System.currentTimeMillis() + (long) (strength * 1000);
            default -> {
            }
        }
    }
}
