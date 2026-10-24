package com.noclip.level;

/**
 * Screen-space look of a level. Read by the client's post-processing shader and
 * fog code. All values are plain floats so the definition can live in common code.
 */
public final class Visuals {
    public float grain = 0.08f;
    public float aberration = 0.0015f;
    public float vignette = 0.45f;
    public float tintR = 1f, tintG = 1f, tintB = 1f;
    public float saturation = 0.85f;
    public float contrast = 1.05f;
    public float brightness = 1f;
    public float warp = 0.02f;
    public float scanlines = 0.04f;
    public float flicker = 0.02f;
    public float bloom = 0.35f;
    /** 0 = no darkness, 1 = everything outside the flashlight is black. */
    public float darkness = 0f;
    /** Fog start/end in blocks. A negative end leaves vanilla fog alone. */
    public float fogStart = 6f, fogEnd = 48f;
    /** Overrides the biome fog colour when r >= 0. */
    public float fogR = -1f, fogG, fogB;
    /** Show the camcorder REC overlay on this level. */
    public boolean recOverlay = true;
    /** Extra per-level shader effect id (0 = none). Interpreted by the fragment shader. */
    public float special = 0f;
    /** Draw the vanilla sky (outdoor levels). Indoor levels show only fog colour. */
    public boolean sky = false;
    /** Draw clouds. */
    public boolean clouds = false;
    /** Draw vanilla rain/snow when the world's weather has it. */
    public boolean weather = false;

    public Visuals grain(float v) { grain = v; return this; }
    public Visuals aberration(float v) { aberration = v; return this; }
    public Visuals vignette(float v) { vignette = v; return this; }
    public Visuals tint(float r, float g, float b) { tintR = r; tintG = g; tintB = b; return this; }
    public Visuals saturation(float v) { saturation = v; return this; }
    public Visuals contrast(float v) { contrast = v; return this; }
    public Visuals brightness(float v) { brightness = v; return this; }
    public Visuals warp(float v) { warp = v; return this; }
    public Visuals scanlines(float v) { scanlines = v; return this; }
    public Visuals flicker(float v) { flicker = v; return this; }
    public Visuals bloom(float v) { bloom = v; return this; }
    public Visuals darkness(float v) { darkness = v; return this; }
    public Visuals fog(float start, float end) { fogStart = start; fogEnd = end; return this; }
    public Visuals fogColor(int rgb) {
        fogR = ((rgb >> 16) & 255) / 255f;
        fogG = ((rgb >> 8) & 255) / 255f;
        fogB = (rgb & 255) / 255f;
        return this;
    }
    public Visuals noRec() { recOverlay = false; return this; }
    public Visuals special(float v) { special = v; return this; }
    public Visuals sky() { sky = true; return this; }
    public Visuals clouds() { clouds = true; return this; }
    public Visuals weather() { weather = true; return this; }
}
