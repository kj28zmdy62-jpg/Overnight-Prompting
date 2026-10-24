"""Endless Highway. A divided highway at night under sodium lamps, no cars, mile markers
counting down. You can hear traffic coming. You will never see it. Get off the road."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, level, sound
import assets as A
import tex as T
import synth as Y
from content.outside_kit import sliced, draw_text, centered, text_width, paint_metal

S = 32
tx = A.tex


def asphalt(seed, line=None, wet=False):
    img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.5 + T.noise(seed + "f", sigma=0.4) * 0.5,
                 "#1a1a1c", "#242426", "#2e2e30")
    agg = (T.rng(seed + "a").random((S, S)) > 0.92).astype(float)
    img = T.blend(img, T.solid("#4a4a4c"), agg * 0.5)
    # tyre polish in the wheel paths
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 + ((np.abs(xx - 8) < 3) | (np.abs(xx - 24) < 3)) * 0.06)
    if line:
        col, lo, hi, dashed = line
        yy = np.mgrid[0:S, 0:S][0]
        band = (yy >= lo) & (yy < hi)
        worn = T.noise(seed + "w", sigma=0.8) > 0.2
        paint = T.grain(T.solid(col), seed + "p", 0.08)
        img = T.blend(img, paint, (band & worn) * 0.9)
        # reflective cat's eye
        img[lo:hi, 14:18] = T.hexc("#f0e8c0")
    return T.grain(img, seed, 0.04)


tx("hw_asphalt", lambda: asphalt("hwa"))
tx("hw_asphalt_dash", lambda: asphalt("hwd", ("#d8d4c8", 14, 18, True)))
tx("hw_asphalt_edge", lambda: asphalt("hwe", ("#d8d4c8", 2, 6, False)))
tx("hw_asphalt_yellow", lambda: asphalt("hwy", ("#c8a030", 26, 30, False)))
cube("hw_asphalt", "hw_asphalt", display="Highway Asphalt")
cube("hw_asphalt_dash", "hw_asphalt_dash", display="Highway Asphalt (Lane Line)")
cube("hw_asphalt_edge", "hw_asphalt_edge", display="Highway Asphalt (Edge Line)")
cube("hw_asphalt_yellow", "hw_asphalt_yellow", display="Highway Asphalt (Median Line)")


def gravel():
    img = T.ramp(T.noise("hwgr", sigma=0.5), "#2e2c28", "#4a4640", "#5a564e")
    return T.grain(img, "hwgr", 0.06)


def verge():
    img = T.ramp(T.fbm("hwv", octaves=4, base=4) * 0.6 + T.noise("hwv2", sigma=0.4) * 0.4, "#22281a", "#2e3622", "#3a422a")
    return T.grain(img, "hwv", 0.05)


def verge_side():
    img = T.grain(T.ramp(T.fbm("hwvs", octaves=3, base=3), "#26201a", "#322a22"), "hwvs", 0.05)
    yy = np.mgrid[0:S, 0:S][0]
    return T.blend(img, verge(), (yy < 5).astype(float))


def tall_grass():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("hwtg")
    for _ in range(22):
        x0, h = 2 + r.random() * 28, 12 + r.random() * 20
        lean = (r.random() - 0.5) * 0.4
        for y in range(int(h)):
            px = int(x0 + lean * y)
            if 0 <= px < S:
                rgb[S - 1 - y, px] = T.hexc("#3e4630") * (0.7 + 0.3 * y / h)
                a[S - 1 - y, px] = 1
    return rgb, a


tx("hw_gravel", gravel)
tx("hw_verge", verge)
tx("hw_verge_side", verge_side)
tx("hw_tall_grass", tall_grass)
cube("hw_gravel", "hw_gravel", sound="gravel", display="Shoulder Gravel")
cube_faces("hw_verge", top="hw_verge", bottom="hw_verge_side", side="hw_verge_side", sound="grass", display="Verge")
cross("hw_tall_grass", "hw_tall_grass", display="Verge Grass")


def jersey():
    img = T.ramp(T.fbm("hwj", octaves=3, base=4), "#8a8680", "#9a968e")
    img = T.stains(img, "hwjs", "#4a4640", count=2, strength=0.35, size=8)
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - np.clip((yy - 24) / 8, 0, 1) * 0.3)
    return T.grain(img, "hwj", 0.04)


tx("hw_jersey", jersey)
shaped("hw_barrier", [(2, 0, 0, 14, 4, 16, "c"), (5, 4, 0, 11, 14, 16, "c")], {"c": "hw_jersey"},
       display="Median Barrier", unbreakable=True, sound="stone")


def rail():
    img = paint_metal("hwrail", "#8a8c8e", "#a8aaac", rust=0.15)
    yy = np.mgrid[0:S, 0:S][0]
    return T.shade(img, 1 - ((yy % 10) == 0) * 0.25 + ((yy % 10) == 5) * 0.1)


tx("hw_rail", rail)
tx("hw_post", lambda: paint_metal("hwpost", "#5a5c5e", "#6a6c6e", rust=0.2))
shaped("hw_guard_rail", [(0, 7, 6, 16, 12, 8, "r")], {"r": "hw_rail"}, display="Guard Rail", unbreakable=True,
       sound="metal")
shaped("hw_guard_post", [(0, 7, 6, 16, 12, 8, "r"), (6, 0, 8, 10, 12, 11, "p")], {"r": "hw_rail", "p": "hw_post"},
       display="Guard Rail Post", unbreakable=True, sound="metal")

# ------------------------------------------------------------------ sodium lamps


def lamp_glow():
    img = T.solid("#ffb048")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 / 2 + (yy - 15.5) ** 2)
    img = T.shade(img, 1.05 - np.clip(d / 30, 0, 0.25))
    return T.border(img, "#3a3632", 3)


def lamp_dead():
    img = T.ramp(T.noise("hwld", sigma=2), "#4a4236", "#5a5040")
    return T.border(img, "#3a3632", 3)


tx("hw_lamp_glow", lamp_glow)
tx("hw_lamp_dead", lamp_dead)
tx("hw_lamp_pole", lambda: paint_metal("hwlp", "#4a4c4e", "#5e6062", rust=0.15))
shaped("hw_lamp_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "p")], {"p": "hw_lamp_pole"}, display="Lamp Pole", unbreakable=True)
shaped("hw_lamp_arm", [(6.5, 6, 0, 9.5, 9, 16, "p")], {"p": "hw_lamp_pole"}, display="Lamp Arm", unbreakable=True,
       collides=False)
shaped("hw_lamp_head", [(6.5, 8, 4, 9.5, 11, 16, "p"), (4, 5, 0, 12, 9, 10, {"all": "p", "down": "g"})],
       {"p": "hw_lamp_pole", "g": "hw_lamp_glow"}, display="Sodium Lamp", light=15, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")
shaped("hw_lamp_head_dead", [(6.5, 8, 4, 9.5, 11, 16, "p"), (4, 5, 0, 12, 9, 10, {"all": "p", "down": "g"})],
       {"p": "hw_lamp_pole", "g": "hw_lamp_dead"}, display="Dead Sodium Lamp", unbreakable=True, collides=False,
       sound="glass")

# ------------------------------------------------------------------ signs


def marker(n):
    def f():
        img = T.ramp(T.noise(f"hwmk{n}", sigma=2), "#1e5a32", "#246638")
        img = T.border(img, "#d8d8d0", 1)
        img = centered(img, "MILE", 5, "#e8e8e0")
        img = centered(img, str(n), 14, "#e8e8e0", scale=2)
        return T.grain(img, f"hwmk{n}", 0.03)
    return f


for n in range(10):
    tx(f"hw_marker_{n}", marker(n))
    shaped(f"hw_mile_{n}", [(7, 0, 7, 9, 10, 9, "p"), (3, 10, 7.5, 13, 16, 8.5, {"all": "p", "north": "m!", "south": "m!"})],
           {"p": "hw_post", "m": f"hw_marker_{n}"}, display=f"Mile Marker {n}", unbreakable=True, sound="metal")


def gantry(text1, text2, arrow=False):
    def f():
        w, h = 4 * S, 2 * S
        img = np.ones((h, w, 3)) * T.hexc("#1e5a32")
        img = T.shade(img, 0.92 + T.noise("hwg" + text1, shape=(h, w), sigma=4) * 0.1)
        img = T.border(img, "#e8e8e0", 2)
        img = draw_text(img, text1, (w - text_width(text1, 3, 2)) // 2, 10, "#f0f0e8", scale=3, gap=2)
        img = draw_text(img, text2, (w - text_width(text2, 2, 2)) // 2, 40, "#f0f0e8", scale=2, gap=2)
        if arrow:
            for k in range(8):
                img[44 - k // 2:46 + k // 2, 112 + k] = T.hexc("#f0f0e8")
            img[44:46, 100:112] = T.hexc("#f0f0e8")
        return T.grain(img, "hwg" + text1, 0.02)
    return f


sliced("hw_sign_next", gantry("EXIT 0", "NEXT RIGHT"), 4, 2)
sliced("hw_sign_exit", gantry("EXIT 0", "THIS EXIT", arrow=True), 4, 2)
for prefix in ("hw_sign_next", "hw_sign_exit"):
    for r in range(2):
        for c in range(4):
            shaped(f"{prefix}_{c}_{r}", [(0, 0, 7, 16, 16, 9, {"all": "b", "north": "f"})],
                   {"b": "hw_lamp_pole", "f": f"{prefix}_{c}_{r}"}, display="Highway Sign", unbreakable=True,
                   render="solid", sound="metal")

tx("hw_truss", lambda: (np.ones((S, S, 3)) * T.hexc("#6a6c6e"),
                       (((np.mgrid[0:S, 0:S][1] < 3) | (np.mgrid[0:S, 0:S][1] > 28) | (np.mgrid[0:S, 0:S][0] < 3) |
                         (np.mgrid[0:S, 0:S][0] > 28) | (np.abs(np.mgrid[0:S, 0:S][0] - np.mgrid[0:S, 0:S][1]) < 2))).astype(float)))
cube("hw_truss", "hw_truss", display="Sign Gantry", sound="metal", render="cutout")


def ramp_dark():
    img = T.solid("#020202")
    yy = np.mgrid[0:S, 0:S][0]
    return T.blend(img, T.solid("#14100a"), np.clip(1 - yy / 32, 0, 1) * 0.5)


tx("hw_underpass_dark", ramp_dark)
exit_block("hw_underpass", "hw_underpass_dark", "DESCEND", display="Underpass Dark")
tx("hw_culvert", lambda: T.grain(T.ramp(T.fbm("hwcu", octaves=3, base=3), "#4a4842", "#5a5850"), "hwcu", 0.05))
cube("hw_culvert", "hw_culvert", display="Culvert Concrete")


def flare(i):
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    stick = (np.abs(xx - 16) < 1.5) & (yy > 18) & (yy < 30)
    rgb[stick] = T.hexc("#a01a14")
    a[stick] = 1
    r = T.rng("hwfl", i)
    flame = ((xx - 16) ** 2 / (5 + r.random() * 4) + (yy - 14 + r.random() * 2) ** 2 / 20) < 1
    rgb[flame] = T.hexc("#ff4a2a")
    core = ((xx - 16) ** 2 / 2 + (yy - 16) ** 2 / 8) < 1
    rgb[core] = T.hexc("#ffe0c0")
    a[flame] = 1
    return rgb, a


from content.outside_kit import animated
animated("hw_flare", flare, 4, frametime=2)
cross("hw_flare", "hw_flare", display="Road Flare", light=12, emissive=True)

# ------------------------------------------------------------------ sound


def highway_loop():
    d = 26.0
    t = Y.t_axis(d)
    drone = Y.lp(Y.brown(d, "hwdrone"), 110) * 0.55
    hiss = Y.bp(Y.pink(d, "hwhiss"), 400, 2500) * 0.08 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 13))
    lamp = np.sin(2 * np.pi * (round(9100 * d) / d) * t) * 0.006 + Y.hum(d, 60, "hwlamp", buzz=0.3, harmonics=(2, 4)) * 0.02
    r = Y.rng("hwcr")
    crick = np.zeros_like(t)
    for k in range(3):
        f = round((4100 + r.random() * 600) * d) / d
        rate = round((1.5 + r.random()) * d) / d
        crick += np.sin(2 * np.pi * f * t) * (np.sin(2 * np.pi * rate * t + k) > 0.6) * 0.02
    x = Y.loopable(Y.norm(drone + hiss + lamp + crick, 0.45), 1.5)
    return Y.stereoize(x, 0.0016), "stereo"


def far_horn():
    d = 4.5
    t = Y.t_axis(d)
    env = np.clip(t / 0.15, 0, 1) * np.clip((3.0 - t) / 0.5, 0, 1)
    x = (np.sign(np.sin(2 * np.pi * 220 * t)) + np.sign(np.sin(2 * np.pi * 277 * t))) * 0.3 * env
    return Y.reverb(Y.lp(Y.norm(x, 0.3), 700), 4.0, 0.85, "hwhorn")


def rail_ping():
    d = 3.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * k) for f, k in ((610, 2.5), (1480, 4), (2350, 6)))
    out = np.zeros_like(t)
    Y.place(out, Y.impact("hwrp", 0.2), 0, 0.4)
    return Y.reverb(Y.norm(x * 0.6 + out, 0.4), 2.5, 0.7, "hwrp")


def radio():
    v = Y.bp(Y.whisper("hwradio", 2.4), 600, 2800) * 2
    music = np.zeros(int(3.0 * Y.SR))
    for i, f in enumerate((392, 440, 494, 440, 392, 330)):
        n = int(0.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(music, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 3), 0.2 + i * 0.4, 0.3)
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.bp(music + 0 * music, 500, 3000), 0.0)
    Y.place(out, v, 0.8, 0.6)
    out += Y.bp(Y.white(3.5, "hwrst"), 2000, 6000) * 0.05
    return Y.reverb(Y.norm(np.tanh(out * 2), 0.3), 2.5, 0.7, "hwradio")


def approach():
    """Something big coming up the road behind you, fast. It never shows."""
    d = 6.0
    t = Y.t_axis(d)
    near = np.exp(-((t - 4.6) ** 2) / 0.35)
    env = np.clip(t / 4.6, 0, 1) ** 3 * (t < 4.6) + near * (t >= 4.6)
    pitch = np.where(t < 4.6, 1.0, 0.82)
    f = 70 * pitch
    engine = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * 0.3 + Y.lp(Y.brown(d, "hwap"), 300) * 0.8
    tyres = Y.bp(Y.white(d, "hwtyre"), 600, 4000) * 0.4
    x = (engine + tyres) * env
    whoosh = Y.bp(Y.white(d, "hwwh"), 200, 6000) * np.exp(-((t - 4.6) ** 2) / 0.05) * 1.5
    return Y.norm(x + whoosh, 0.95)


def hit():
    out = np.zeros(int(2.0 * Y.SR))
    Y.place(out, Y.impact("hwhit", 1.4, ring=0.2), 0.0, 1.0)
    Y.place(out, Y.bp(Y.white(0.6, "hwglass"), 3000, 10000) * np.exp(-np.arange(int(0.6 * Y.SR)) / Y.SR * 8), 0.02, 0.6)
    return Y.reverb(Y.norm(out, 0.95), 1.5, 0.4, "hwhit")


sound("ambient.endless_highway", [("ambient/endless_highway", highway_loop)], stream=True)
sound("ambient.endless_highway.additions",
      [("ambient/endless_highway_horn", far_horn), ("ambient/endless_highway_rail", rail_ping),
       ("ambient/endless_highway_radio", radio)],
      subtitle="Something out on the highway")
sound("event.endless_highway_approach", [("event/endless_highway_approach", approach)], subtitle="Traffic coming up behind you")
sound("event.endless_highway_hit", [("event/endless_highway_hit", hit)], subtitle="Impact")

level("endless_highway", fog="#140c06", sky="#0a0604", ambient="ambient.endless_highway",
      additions="ambient.endless_highway.additions", additions_chance=0.003, fixed_time=18000, skylight=True,
      ceiling=False, height=80)
