"""Storm Drains - enormous concrete culverts under a city you never reach. Daylight falls through
grates thirty metres up. The water rises in surges. Your footsteps come back late."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, custom
from content.ind_kit import text, vstreaks, rust, steel, concrete, grate, anim_tex
import tex as T
import synth as Y

S = 32


def culvert(seed="sdc", algae=0.0):
    yy, xx = np.mgrid[0:S, 0:S]
    img = concrete(seed, "#5c5d58", "#74756e", stain="#3a3c36", stains=2)
    # formwork lines from the precast segments
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.12 - ((xx % 32) == 0) * 0.18)
    # efflorescence: white mineral weeping from the joints
    img = vstreaks(img, seed + "ef", "#c8c6b8", count=3, strength=0.35, length=(6, 20))
    img = vstreaks(img, seed + "dk", "#2c2e2a", count=3, strength=0.3, length=(10, 30))
    if algae:
        m = np.clip(T.fbm(seed + "al", octaves=3, base=3) * 1.5 - (1.2 - algae), 0, 1)
        img = T.blend(img, T.ramp(T.noise(seed + "alc", sigma=0.8), "#1e2a18", "#34442a"), m * 0.85)
    return img


def tide():
    """The flood line: everything below it darker, silted, slick."""
    img = culvert("sdt")
    yy, xx = np.mgrid[0:S, 0:S]
    line = 10 + (T.noise("sdtl", sigma=2)[0] * 3).astype(int)
    below = yy > line[None, :]
    wet = T.shade(culvert("sdtw", algae=0.5), 0.72)
    img = np.where(below[..., None], wet, img)
    edge = (yy == line[None, :]) | (yy == line[None, :] + 1)
    return T.blend(img, T.solid("#4a4636"), edge.astype(float) * 0.8)


def low_wall():
    return T.shade(culvert("sdlw", algae=0.7), 0.7)


def graffiti(kind):
    def f():
        img = culvert("sdg" + kind)
        if kind == "arrow":
            col = T.hexc("#d04a1c")
            for i in range(14):
                img[16, 6 + i] = col
                img[15, 6 + i] = col
            for d in range(6):
                img[16 - d, 20 + 6 - d] = col
                img[16 + d, 20 + 6 - d] = col
            img = text(img, "UP?", 9, 21, "#d04a1c")
        elif kind == "count":
            img = text(img, "DAY", 4, 5, "#e0e0d0")
            for t in range(11):
                img[13:19, 4 + t * 2] = T.hexc("#e0e0d0")
            img = text(img, "NO SKY", 4, 23, "#e0e0d0")
        else:
            img = text(img, "LISTEN", 4, 8, "#3a7ad0", 1)
            img = text(img, "FOR IT", 4, 16, "#3a7ad0", 1)
        return T.grain(img, "sdg" + kind, 0.03)
    return f


def ruler():
    img = culvert("sdr")
    img = T.rect(img, 12, 0, 20, 32, "#d8d2b0")
    for y in range(0, 32, 4):
        img = T.rect(img, 12, y, 16 if y % 8 else 19, y + 1, "#1a1a18")
    img = T.rect(img, 12, 0, 20, 3, "#c02018")
    return vstreaks(img, "sdrv", "#4a4636", 2, 0.4)


def floor():
    img = concrete("sdf", "#4c4d48", "#5e5f58", stain="#2e302a", stains=3, crack=1)
    sheen = T.noise("sdfsh", sigma=3)
    silt = np.clip(T.fbm("sdfsi", octaves=3, base=3) * 1.6 - 0.8, 0, 1)
    img = T.blend(img, T.ramp(T.noise("sdfs2", sigma=0.6), "#4a4230", "#5a5038"), silt * 0.7)
    return T.shade(img, 0.92 + sheen * 0.15)


def bed():
    img = T.ramp(T.fbm("sdbed", octaves=4, base=3), "#2a2a1e", "#3e3a28")
    stones = (T.rng("sdbst").random((S, S)) > 0.93).astype(float)
    img = T.shade(img, 1 + stones * 0.35)
    return T.grain(img, "sdbed", 0.05)


def ceiling():
    return T.shade(culvert("sdce"), 0.8)


def shaft():
    img = culvert("sdsh")
    return vstreaks(img, "sdshr", "#6a4020", count=2, strength=0.35, length=(14, 30))


def sky_grate():
    """Looking straight up through a storm grate at an overcast sky. You never get closer."""
    yy, xx = np.mgrid[0:S, 0:S]
    sky = T.ramp(T.fbm("sdsky", octaves=3, base=5), "#c8d4dc", "#eef4f8")
    img = sky.copy()
    bars = ((xx % 5) < 2)
    iron = T.ramp(T.noise("sdsgi", sigma=1), "#141414", "#262624")
    img = np.where(bars[..., None], iron, img)
    img = T.border(img, "#1a1a18", 2)
    return img


def beam():
    """A shaft of daylight: a faint, dusty, soft-edged sheet."""
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("sdbeam", sigma=2), "#e8f0f4", "#ffffff")
    soft = np.sin(np.pi * xx / 31.0) ** 2
    dust = (T.rng("sdbd").random((S, S)) > 0.985).astype(float)
    a = soft * 0.075 + dust * 0.35
    return rgb, a


def rungs():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = steel("sdrung", "#5a4a3a", "#7a6450", brushed=False)
    rgb = rust(rgb, "sdrungr", 0.5, "#5a2a14", "#7a3a1c")
    a = (((yy % 8) < 2) & (xx > 3) & (xx < 28)) | (((xx < 5) | (xx > 26)) & ((yy % 8) < 4) & ((yy % 8) >= 2))
    return rgb, a.astype(float)


def outfall():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(T.noise("sdout", sigma=2), "#020303", "#0a0c0a")
    rim = (d > 12) & (d < 16)
    img = np.where(rim[..., None], culvert("sdoutr", algae=0.6), img)
    return img


def maint_door():
    img = steel("sddoor", "#3e4a44", "#4e5a54", brushed=False)
    img = rust(img, "sddoorr", 0.45, "#5a2a14", "#7a3a1c")
    img = T.border(img, "#1e2420", 2)
    img = T.rect(img, 22, 14, 27, 18, "#9a9488")
    img = text(img, "NO", 6, 6, "#d8d0b0")
    img = text(img, "ENTRY", 6, 13, "#d8d0b0")
    return img


def lamp():
    img = T.ramp(T.noise("sdlamp", sigma=2), "#ffe8b0", "#fff6dc")
    return T.border(T.border(img, "#2a2a26", 3), "#4a4a44", 1)


def trash():
    img = T.ramp(T.noise("sdtrash", sigma=0.8), "#3a3424", "#5a4e30")
    bits = T.rng("sdtb").random((S, S))
    img = np.where((bits > 0.92)[..., None], T.solid("#8a8a7a"), img)
    img = np.where((bits < 0.04)[..., None], T.solid("#6a2a1a"), img)
    return T.grain(img, "sdtrash", 0.06)


def cart_metal():
    return T.grain(T.ramp(T.noise("sdcart", sigma=1), "#7a7c7c", "#a4a6a6"), "sdcart", 0.04)


def cone():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("sdcone", sigma=1), "#c84a10", "#e86a20")
    band = ((yy % 16) > 9) & ((yy % 16) < 13)
    img = np.where(band[..., None], T.solid("#e0e0d8"), img)
    return T.stains(img, "sdcones", "#3a2a1a", 2, 0.4, 7)


tex("sd_culvert", lambda: culvert())
tex("sd_culvert_algae", lambda: culvert("sdca", algae=0.55))
tex("sd_tide", tide)
tex("sd_low_wall", low_wall)
tex("sd_graffiti_arrow", graffiti("arrow"))
tex("sd_graffiti_count", graffiti("count"))
tex("sd_graffiti_listen", graffiti("listen"))
tex("sd_ruler", ruler)
tex("sd_floor", floor)
tex("sd_bed", bed)
tex("sd_ceiling", ceiling)
tex("sd_shaft", shaft)
tex("sd_sky_grate", sky_grate)
tex("sd_beam", beam)
tex("sd_rungs", rungs)
tex("sd_outfall", outfall)
tex("sd_door", maint_door)
tex("sd_lamp", lamp)
tex("sd_trash", trash)
tex("sd_cart", cart_metal)
tex("sd_cone", cone)
tex("sd_bridge", lambda: grate("sdbr", "#3a3630", "#57524a", pitch=4, bar=1, rust_amt=0.45))

cube("sd_culvert", "sd_culvert", display="Culvert Concrete")
cube("sd_culvert_algae", "sd_culvert_algae", display="Slimed Culvert")
cube("sd_tide_line", "sd_tide", display="Flood Line")
cube("sd_low_wall", "sd_low_wall", display="Waterline Concrete")
cube("sd_graffiti_arrow", "sd_graffiti_arrow", display="Graffiti")
cube("sd_graffiti_count", "sd_graffiti_count", display="Graffiti")
cube("sd_graffiti_listen", "sd_graffiti_listen", display="Graffiti")
cube("sd_ruler", "sd_ruler", display="Flood Gauge")
cube("sd_floor", "sd_floor", display="Silted Ledge")
cube("sd_bed", "sd_bed", display="Channel Bed", sound="gravel")
cube("sd_ceiling", "sd_ceiling", display="Culvert Crown", flat=True)
cube("sd_bridge", "sd_bridge", display="Grating Bridge", sound="metal", render="cutout")
cube("sd_shaft", "sd_shaft", display="Shaft Wall")
cube_faces("sd_sky_grate", bottom="sd_sky_grate", top="sd_culvert", side="sd_culvert", display="Storm Grate",
           light=15, emissive=True, flat=True, sound="metal")
exit_block("sd_loose_grate", "sd_sky_grate", "ASCEND", trigger="USE", solid=True, display="Loose Storm Grate",
           light=15, emissive=True, sound="metal")
shaped("sd_beam", [(0, 0, 7.5, 16, 16, 8.5, "b"), (7.5, 0, 0, 8.5, 16, 16, "b")], {"b": "sd_beam"},
       display="Daylight", collides=False, render="translucent", unbreakable=True, emissive=True, sound="glass")
ladder("sd_rungs", "sd_rungs", display="Rusted Rungs", unbreakable=True)
exit_block("sd_outfall", "sd_outfall", "WATER", display="Outfall", sound="stone")
exit_block("sd_door", "sd_door", "DOOR", trigger="USE", solid=True, display="Maintenance Door", sound="metal")
shaped("sd_lamp", [(4, 4, 0, 12, 12, 3, {"all": "f", "south": "l"})], {"l": "sd_lamp", "f": "sd_cart"},
       display="Work Lamp", light=13, emissive=True, collides=False, unbreakable=True, sound="glass")
shaped("sd_trash", [(0, 0, 0, 16, 3, 16, "t"), (2, 3, 3, 11, 6, 12, "t"), (9, 3, 1, 14, 5, 7, "t")],
       {"t": "sd_trash"}, display="Storm Debris", sound="gravel", unbreakable=True)
shaped("sd_cart", [(2, 4, 1, 14, 5, 15, "m"), (2, 5, 1, 3, 13, 15, "m"), (13, 5, 1, 14, 13, 15, "m"),
                   (2, 5, 1, 14, 13, 2, "m"), (2, 5, 14, 14, 13, 15, "m"), (3, 0, 2, 4, 4, 3, "m"),
                   (12, 0, 2, 13, 4, 3, "m"), (3, 0, 13, 4, 4, 14, "m"), (12, 0, 13, 13, 4, 14, "m"),
                   (2, 13, 15, 14, 14, 16, "m")], {"m": "sd_cart"}, display="Shopping Cart", sound="metal",
       unbreakable=True)
shaped("sd_cone", [(3, 0, 3, 13, 1, 13, "c"), (5, 1, 5, 11, 6, 11, "c"), (6, 6, 6, 10, 11, 10, "c"),
                   (7, 11, 7, 9, 14, 9, "c")], {"c": "sd_cone"}, display="Traffic Cone", sound="wool")

# ------------------------------------------------------------------ sound


def flow():
    d = 28.0
    t = Y.t_axis(d)
    water = Y.bp(Y.pink(d, "sdflow"), 150, 2500) * 0.6
    gurgle = Y.bp(Y.white(d, "sdgur"), 400, 1400) * (0.5 + 0.5 * np.sin(2 * np.pi * 3.1 * t + 2 * np.sin(2 * np.pi * 0.37 * t))) * 0.25
    tunnel = Y.lp(Y.brown(d, "sdtun"), 70) * 0.8
    wind = Y.bp(Y.pink(d, "sdwind"), 500, 900) * 0.12 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 14))
    out = np.zeros(len(t))
    r = Y.rng("sddrips")
    for k in range(18):
        n = int(0.15 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 900 + r.random() * 900 - tt * 2000
        Y.place(out, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 30), 1 + r.random() * (d - 2), 0.15 + r.random() * 0.2)
    x = water + gurgle + tunnel + wind + Y.reverb(out, 3.5, 0.8, "sddr")[:len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 2.0), 0.002), "stereo"


def far_splash(i):
    def f():
        d = 3.0
        t = Y.t_axis(d)
        x = Y.bp(Y.white(d, f"sdsp{i}"), 300, 4000) * np.exp(-t * 6) * (1 - np.exp(-t * 60))
        return Y.norm(Y.reverb(Y.lp(x, 1800), 4.0, 0.75, f"sdsp{i}"), 0.6)
    return f


def traffic():
    """Something heavy passing over a road far above. A world up there you cannot get to."""
    d = 6.0
    t = Y.t_axis(d)
    env = np.exp(-((t - 3.0) ** 2) / 1.6)
    x = Y.lp(Y.brown(d, "sdtraf"), 120) * env * 1.5 + Y.bp(Y.pink(d, "sdtraf2"), 60, 400) * env * 0.5
    clunk = np.zeros(len(t))
    Y.place(clunk, Y.impact("sdgrate", 0.9, ring=0.5), 2.9, 0.6)
    Y.place(clunk, Y.impact("sdgrate2", 0.9, ring=0.5), 3.15, 0.5)
    return Y.norm(Y.reverb(x + Y.lp(clunk, 900), 3.5, 0.6, "sdtraf"), 0.6)


def whistle():
    d = 4.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 2
    x = Y.bp(Y.white(d, "sdwh"), 1150, 1250) * env * 3 + Y.bp(Y.white(d, "sdwh2"), 1700, 1780) * env * 1.5
    return Y.norm(Y.reverb(x, 3.0, 0.6, "sdwh"), 0.4)


def surge():
    d = 9.0
    t = Y.t_axis(d)
    env = np.clip(t / 6.0, 0, 1) ** 2.5
    roar = Y.lp(Y.brown(d, "sdsurge"), 250) * env * 1.6 + Y.bp(Y.pink(d, "sdsurge2"), 200, 3000) * env * 0.9
    return Y.stereoize(Y.norm(Y.reverb(roar, 3.0, 0.6, "sdsurge"), 0.95), 0.003), "stereo"


def echo_step(i):
    def f():
        out = np.zeros(int(2.4 * Y.SR))
        Y.place(out, Y.footstep(f"sdes{i}", "concrete"), 0.02, 1.0)
        Y.place(out, Y.footstep(f"sdesw{i}", "water"), 0.02, 0.4)
        return Y.norm(Y.reverb(Y.lp(out, 2400), 2.2, 0.75, f"sdes{i}"), 0.7)
    return f


sound("ambient.storm_drains", [("ambient/storm_drains", flow)], stream=True)
sound("ambient.storm_drains.additions",
      [(f"ambient/storm_drains_splash{i}", far_splash(i)) for i in range(2)] +
      [("ambient/storm_drains_traffic", traffic), ("ambient/storm_drains_whistle", whistle)],
      subtitle="Water moves in the tunnels")
sound("event.storm_drains_surge", [("event/storm_drains_surge", surge)], subtitle="A roar of water upstream")
sound("event.storm_drains_step", [(f"event/storm_drains_step{i}", echo_step(i)) for i in range(4)],
      subtitle="Footsteps echo")

level("storm_drains", fog="#1a1e1c", sky="#1a1e1c", water="#3b4a3a", water_fog="#141a14",
      ambient="ambient.storm_drains", additions="ambient.storm_drains.additions", additions_chance=0.005,
      fixed_time=6000, height=64, particle="minecraft:falling_water", particle_chance=0.002)
