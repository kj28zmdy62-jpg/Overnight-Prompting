#version 150

// NOCLIP screen pass: found-footage camcorder look, per-level grading,
// flashlight cone, darkness crush and sanity distortion.

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec2 InSize;
uniform float Time;
uniform float Grain;
uniform float Aberration;
uniform float Vignette;
uniform vec3 Tint;
uniform float Saturation;
uniform float Contrast;
uniform float Brightness;
uniform float Warp;
uniform float Scanlines;
uniform float Flicker;
uniform float Bloom;
uniform float Darkness;
uniform float Flash;
uniform float Insanity;
uniform float Noclip;
uniform float Special;
uniform float WallPush;
uniform float Far;
uniform float Pulse;
uniform float Camcorder;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}

float linearDepth(float d) {
    float n = 0.05;
    float f = Far;
    float z = d * 2.0 - 1.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 sampleCA(vec2 uv, vec2 dir) {
    return vec3(texture(DiffuseSampler, uv + dir).r,
                texture(DiffuseSampler, uv).g,
                texture(DiffuseSampler, uv - dir).b);
}

void main() {
    vec2 uv = texCoord;
    vec2 c = uv - 0.5;
    float aspect = InSize.x / InSize.y;

    // barrel distortion of a cheap camcorder lens
    float r2 = dot(c * vec2(aspect, 1.0), c * vec2(aspect, 1.0));
    uv = 0.5 + c * (1.0 + Warp * r2 * 1.5 - Warp * 0.35);

    // sanity loss: the world breathes and swims
    float ins = Insanity * Insanity;
    float breathe = sin(Time * 0.9) * 0.5 + 0.5;
    uv = 0.5 + (uv - 0.5) * (1.0 - ins * 0.012 * breathe);
    uv += vec2(sin(uv.y * 23.0 + Time * 1.7), cos(uv.x * 19.0 + Time * 1.3)) * (ins * 0.004 + WallPush * 0.0035);

    // VHS tracking: an occasional band of horizontal tearing
    float band = floor(uv.y * 90.0);
    float tear = step(0.992 - Insanity * 0.02 - Noclip * 0.3, hash(vec2(band, floor(Time * 14.0))));
    uv.x += tear * (hash(vec2(band, Time)) - 0.5) * 0.02 * (Grain * 6.0 + 0.2);
    float wobble = sin(uv.y * 3.0 + Time * 0.6) * 0.0006 * Grain * 10.0;
    uv.x += wobble;

    // chromatic aberration grows towards the edges
    vec2 dir = c * (Aberration + Insanity * 0.004 + Noclip * 0.02) * (1.0 + length(c) * 2.0);
    vec3 col = sampleCA(uv, dir);
    vec3 raw = col;

    // soft bloom from bright pixels (fluorescent tubes, exits, screens)
    vec3 glow = vec3(0.0);
    float rad = 0.004;
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 0.5236;
        vec2 o = vec2(cos(a) / aspect, sin(a));
        glow += max(texture(DiffuseSampler, uv + o * rad).rgb - 0.62, 0.0);
        glow += max(texture(DiffuseSampler, uv + o * rad * 2.6).rgb - 0.62, 0.0) * 0.7;
        glow += max(texture(DiffuseSampler, uv + o * rad * 5.5).rgb - 0.62, 0.0) * 0.4;
    }
    col += glow / 12.0 * Bloom * 1.6;

    float depth = linearDepth(texture(DiffuseDepthSampler, uv).r);

    // darkness: crush everything the light does not reach
    if (Darkness > 0.0) {
        float l = luma(col);
        float keep = smoothstep(0.02, 0.55, l);
        col *= mix(1.0, keep * keep, Darkness);
    }

    // flashlight: a soft cone that brightens what is close, with a hot centre
    if (Flash > 0.0) {
        vec2 fc = (texCoord - vec2(0.5, 0.49)) * vec2(aspect, 1.0);
        float d = length(fc);
        float cone = smoothstep(0.55, 0.05, d);
        float hot = smoothstep(0.18, 0.0, d);
        float reach = clamp(1.0 - depth / 30.0, 0.0, 1.0);
        reach *= reach;
        float spot = (cone * 0.85 + hot * 0.35) * reach * Flash;
        float ringN = noise(fc * 9.0) * 0.15;
        vec3 lit = raw * (2.2 + ringN) + raw * hot * 1.2;
        col = mix(col, lit * vec3(1.0, 0.97, 0.9), clamp(spot, 0.0, 1.0));
    }

    // grade
    col *= Tint * Brightness;
    float l = luma(col);
    col = mix(vec3(l), col, Saturation - Insanity * 0.25);
    col = (col - 0.5) * Contrast + 0.5;
    // lifted, slightly milky blacks of consumer video
    col = col * 0.94 + 0.025;

    // camcorder night-vision mode
    if (Camcorder > 0.0) {
        float nl = luma(raw) * 3.5 + 0.04;
        vec3 nv = vec3(0.25, 1.0, 0.35) * nl;
        nv *= 0.85 + 0.15 * sin(texCoord.y * InSize.y * 1.5);
        col = mix(col, nv, Camcorder);
    }

    // mains flicker: brief brightness dips
    float flick = step(1.0 - Flicker * 2.0, hash(vec2(floor(Time * 18.0), 7.0)));
    col *= 1.0 - flick * 0.18;

    // scanlines and grain
    col *= 1.0 - Scanlines * (0.5 + 0.5 * sin(texCoord.y * InSize.y * 1.5708));
    float g = hash(texCoord * InSize + vec2(fract(Time * 7.13) * 431.0, fract(Time * 3.7) * 217.0)) - 0.5;
    col += g * (Grain + Insanity * 0.08 + Camcorder * 0.06);
    // chroma noise
    col += (vec3(hash(texCoord * 731.0 + Time), hash(texCoord * 417.0 - Time), hash(texCoord * 263.0 + Time * 2.0)) - 0.5) * Grain * 0.35;

    // vignette
    float v = smoothstep(0.95, 0.2, length(c * vec2(aspect * 0.8, 1.0)) * (1.0 + Insanity * 0.35));
    col *= mix(1.0, v, clamp(Vignette + Insanity * 0.35 + Pulse * 0.3, 0.0, 1.0));

    // heartbeat pulse at the edges
    col = mix(col, col * vec3(1.25, 0.55, 0.5), Pulse * (1.0 - v) * 0.8);

    // noclip transition: the picture dissolves into static
    float stat = hash(floor(texCoord * InSize / 2.0) + vec2(Time * 60.0, Time * 13.0));
    col = mix(col, vec3(stat) * vec3(0.95, 0.92, 0.85), clamp(Noclip, 0.0, 1.0) * 0.9);

    // edges of the lens go fully dark when warped past the frame
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) col = vec3(0.0);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
