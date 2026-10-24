"""Endless Parking Lot. Empty stalls in every direction under orange lights, and one car
with its headlights on, facing you. It is a little closer every time you look back."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, pane, level, sound
import assets as A
import tex as T
import synth as Y
from content.outside_kit import sliced, draw_text, centered, text_width, paint_metal

S = 32
tx = A.tex


def lot_asphalt(seed, mark=None):
    img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.5 + T.noise(seed + "f", sigma=0.4) * 0.5, "#2a2826", "#353330", "#403e3a")
    img = T.cracks(img, seed + "c", "#1a1918", count=2, alpha=0.5)
    img = T.stains(img, seed + "s", "#1e1d1c", count=1, strength=0.35, size=7)
    yy, xx = np.mgrid[0:S, 0:S]
    paint = T.grain(T.solid("#d8d2c0"), seed + "p", 0.08)
    worn = (T.noise(seed + "w", sigma=0.8) > 0.25).astype(float)
    if mark == "stall_w":          # stall divider line along z on the west edge
        img = T.blend(img, paint, (xx < 3) * worn * 0.9)
    elif mark == "stall_end":      # the line across the head of the stalls
        img = T.blend(img, paint, (yy < 3) * worn * 0.9)
    elif mark == "stall_corner":
        img = T.blend(img, paint, ((xx < 3) | (yy < 3)) * worn * 0.9)
    elif mark in ("arrow_e", "arrow_w", "arrow_n", "arrow_s"):
        m = np.zeros((S, S), bool)
        shaft = (np.abs(yy - 16) < 3) & (xx > 3) & (xx < 20)
        head = (xx >= 18) & (xx < 29) & (np.abs(yy - 16) < (29 - xx) * 0.9)
        m = shaft | head
        if mark == "arrow_w":
            m = m[:, ::-1]
        elif mark == "arrow_s":
            m = m.T
        elif mark == "arrow_n":
            m = m.T[::-1, :]
        img = T.blend(img, paint, m * worn * 0.92)
    elif mark == "exit_word":
        txt = np.zeros((S, S, 3))
        txt = centered(txt, "EXIT", 13, "#ffffff", scale=1)
        img = T.blend(img, paint, (txt[..., 0] > 0.5) * 0.95)
    elif mark == "handicap":
        img = T.blend(img, T.solid("#2a4a8a"), ((xx > 4) & (xx < 28) & (yy > 4) & (yy < 28)) * 0.85)
        img = T.blend(img, paint, ((((xx - 16) ** 2 + (yy - 18) ** 2) < 30) & (((xx - 16) ** 2 + (yy - 18) ** 2) > 14)) * 0.9)
    return T.grain(img, seed, 0.04)


MARKS = [None, "stall_w", "stall_end", "stall_corner", "arrow_e", "arrow_w", "arrow_n", "arrow_s", "exit_word", "handicap"]
for m in MARKS:
    name = "pl_asphalt" + ("" if m is None else "_" + m)
    tx(name, lambda m=m, name=name: lot_asphalt(name, m))
    cube(name, name, display="Parking Lot")


def curb():
    img = T.ramp(T.noise("plcurb", sigma=1), "#b8b0a0", "#c8c0b0")
    img = T.blend(img, T.solid("#c8a030"), (np.mgrid[0:S, 0:S][0] < 8) * 0.8)
    return T.grain(img, "plcurb", 0.04)


def mulch():
    img = T.ramp(T.noise("plmulch", sigma=0.6), "#3a2618", "#5a3a22", "#6a4a2a")
    return T.grain(img, "plmulch", 0.06)


def shrub():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("plshrub")
    for _ in range(14):
        x, y = 16 + (r.random() - 0.5) * 6, S - 1
        ang = (r.random() - 0.5) * 1.6
        for k in range(18):
            ang += (r.random() - 0.5) * 0.3
            x += np.sin(ang) * 0.9
            y -= np.cos(ang) * 0.9
            if 0 <= int(x) < S and 0 <= int(y) < S:
                rgb[int(y), int(x)] = T.hexc("#4a3a2a")
                a[int(y), int(x)] = 1
                if k > 8 and r.random() < 0.3:
                    rgb[int(y), int(x)] = T.hexc("#6a5a2a")
    return rgb, a


tx("pl_curb", curb)
tx("pl_mulch", mulch)
tx("pl_shrub", shrub)
cube_faces("pl_curb", top="pl_curb", bottom="pl_curb", side="pl_curb", display="Island Curb")
cube("pl_mulch", "pl_mulch", sound="gravel", display="Mulch")
cross("pl_shrub", "pl_shrub", display="Dead Shrub")


def light_head():
    img = paint_metal("plhead", "#4a4a48", "#5a5a58")
    return img


def light_lens():
    img = T.solid("#ffc070")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1.05 - np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) / 60)
    return T.border(img, "#5a5048", 2)


tx("pl_pole", lambda: paint_metal("plpole", "#6a6c6a", "#7a7c7a", rust=0.2))
tx("pl_head", light_head)
tx("pl_lens", light_lens)
tx("pl_base", lambda: T.grain(T.ramp(T.fbm("plbase", octaves=3, base=3), "#8a8478", "#9a948a"), "plbase", 0.04))
shaped("pl_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "p")], {"p": "pl_pole"}, display="Light Pole", unbreakable=True)
shaped("pl_base", [(3, 0, 3, 13, 12, 13, "b"), (6.5, 12, 6.5, 9.5, 16, 9.5, "p")], {"b": "pl_base", "p": "pl_pole"},
       display="Light Pole Base", unbreakable=True)
# four shoebox heads on a cross arm
shaped("pl_heads", [(6.5, 0, 6.5, 9.5, 10, 9.5, "p"), (0, 9, 7, 16, 10, 9, "p"), (7, 9, 0, 9, 10, 16, "p"),
                    (0, 7, 5, 4, 10, 11, {"all": "h", "down": "l"}), (12, 7, 5, 16, 10, 11, {"all": "h", "down": "l"}),
                    (5, 7, 0, 11, 10, 4, {"all": "h", "down": "l"}), (5, 7, 12, 11, 10, 16, {"all": "h", "down": "l"})],
       {"p": "pl_pole", "h": "pl_head", "l": "pl_lens"}, display="Parking Lot Lights", light=15, emissive=True,
       unbreakable=True, sound="metal", render="solid")


def corral():
    rgb = np.ones((S, S, 3)) * T.hexc("#8a8c8e")
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((yy < 3) | (yy > 28) | ((yy > 13) & (yy < 16)) | (xx < 2) | (xx > 29)).astype(float)
    return rgb, a


tx("pl_corral", corral)
pane("pl_corral", "pl_corral", display="Cart Corral", sound="metal")

# ------------------------------------------------------------------ the pay booth and the stair kiosk


def booth_wall():
    img = T.ramp(T.noise("plbw", sigma=2), "#c8c0a8", "#d8d0b8")
    img[-6:] = T.hexc("#2a4a6a")
    return T.grain(img, "plbw", 0.03)


def booth_window():
    rgb = T.ramp(T.noise("plbwin", sigma=3), "#d8c890", "#f0e0a8")
    a = np.ones((S, S)) * 0.55
    yy, xx = np.mgrid[0:S, 0:S]
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    rgb[frame] = T.hexc("#6a6a68")
    a[frame] = 1
    return rgb, a


def pay_sign():
    w, h = 2 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#1a3a6a")
    img = centered(img, "PAY HERE", 5, "#f0f0e8", scale=1)
    img = centered(img, "EXIT", 15, "#f0d040", scale=2)
    return T.border(img, "#e8e8e0", 1)


def gate_arm():
    img = T.solid("#e8e4dc")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.blend(img, T.solid("#c02a22"), (((xx // 8) % 2) == 0)[..., None] * np.ones(3))
    return img


tx("pl_booth_wall", booth_wall)
tx("pl_booth_window", booth_window)
sliced("pl_pay", pay_sign, 2, 1)
tx("pl_gate_arm", gate_arm)
cube("pl_booth_wall", "pl_booth_wall", display="Pay Booth")
pane("pl_booth_window", "pl_booth_window", display="Pay Booth Window", sound="glass", render="translucent")
for c in range(2):
    shaped(f"pl_pay_sign_{c}", [(0, 0, 6, 16, 16, 10, {"all": "p", "north": "s", "south": "s"})],
           {"p": "pl_pole", "s": f"pl_pay_{c}_0"}, display="Pay Sign", light=10, emissive=True, unbreakable=True,
           render="solid")
shaped("pl_gate_arm", [(0, 9, 7, 16, 11, 9, "a")], {"a": "pl_gate_arm"}, display="Gate Arm", collides=False,
       unbreakable=True, sound="metal")
shaped("pl_gate_post", [(4, 0, 4, 12, 14, 12, "b"), (0, 9, 7, 16, 11, 9, "a")], {"b": "pl_booth_wall", "a": "pl_gate_arm"},
       display="Gate Post", unbreakable=True, sound="metal")
tx("pl_lane_dark", lambda: T.blend(T.solid("#060504"), T.solid("#1a1208"), T.noise("plld", sigma=3) * 0.5))
exit_block("pl_exit_lane", "pl_lane_dark", "DOOR", display="Exit Lane")


def kiosk_sign():
    w, h = 2 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#e8e4dc")
    img = centered(img, "STAIRS", 5, "#1a1a1a", scale=1)
    img = centered(img, "P2", 14, "#c02a22", scale=2)
    return T.border(img, "#5a5a58", 1)


sliced("pl_kiosk", kiosk_sign, 2, 1)
for c in range(2):
    shaped(f"pl_kiosk_sign_{c}", [(0, 0, 6, 16, 16, 10, {"all": "w", "north": "s"})],
           {"w": "pl_booth_wall", "s": f"pl_kiosk_{c}_0"}, display="Stair Kiosk Sign", light=8, emissive=True,
           unbreakable=True, render="solid")
tx("pl_stairs_dark", lambda: T.solid("#030303"))
exit_block("pl_stairwell", "pl_stairs_dark", "DESCEND", display="Stairwell Down")

# ------------------------------------------------------------------ sound


def lot_loop():
    d = 24.0
    t = Y.t_axis(d)
    buzz = np.zeros_like(t)
    for f, g in ((120.0, 0.04), (240.0, 0.02), (9300.0, 0.004), (9370.0, 0.003)):
        buzz += np.sin(2 * np.pi * (round(f * d) / d) * t) * g
    freeway = Y.lp(Y.brown(d, "plfw"), 150) * 0.5
    wind = Y.bp(Y.pink(d, "plwind"), 200, 1500) * 0.08 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 12))
    x = Y.loopable(Y.norm(buzz + freeway + wind, 0.4), 1.5)
    return Y.stereoize(x, 0.0014), "stereo"


def cart_roll():
    d = 4.0
    t = Y.t_axis(d)
    rattle = Y.bp(Y.white(d, "plcart"), 1500, 6000) * (0.5 + 0.5 * (np.sin(2 * np.pi * 14 * t) > 0)) * np.sin(np.pi * t / d)
    squeak = np.sin(2 * np.pi * (2300 + 200 * np.sin(2 * np.pi * 2 * t)) * t) * 0.05 * (np.sin(2 * np.pi * 1.7 * t) > 0.7)
    out = np.zeros_like(t)
    Y.place(out, Y.impact("plcartbump", 0.3), 3.6, 0.6)
    return Y.reverb(Y.norm(rattle + squeak + out, 0.4), 2.5, 0.65, "plcart")


def alarm():
    d = 3.0
    t = Y.t_axis(d)
    chirp = np.sin(2 * np.pi * (1800 + 600 * np.sin(2 * np.pi * 6 * t)) * t) * ((t % 1.0) < 0.5)
    return Y.reverb(Y.lp(Y.norm(chirp * (t < 2.0), 0.3), 4000), 3.0, 0.8, "plalarm")


def door_slam():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("plslam", 0.45, ring=0.1), 0.1, 1.0)
    return Y.reverb(Y.lp(Y.norm(out, 0.4), 3000), 3.0, 0.8, "plslam")


def idle():
    d = 2.5
    t = Y.t_axis(d)
    f = 31.0
    x = np.sign(np.sin(2 * np.pi * f * t)) * 0.4 + np.sin(2 * np.pi * f * 2 * t) * 0.3
    x = Y.lp(x * (0.8 + 0.2 * np.sin(2 * np.pi * 7.75 * t)), 400) + Y.lp(Y.brown(d, "plidle"), 200) * 0.3
    return Y.norm(x * Y.env(len(t), 0.2, 0.2), 0.6)


def horn_hit():
    d = 2.5
    t = Y.t_axis(d)
    horn = (np.sign(np.sin(2 * np.pi * 330 * t)) + np.sign(np.sin(2 * np.pi * 415 * t))) * 0.25 * (t < 0.9)
    out = Y.lp(horn, 2500)
    Y.place(out, Y.impact("plhit", 1.2), 0.7, 1.0)
    return Y.norm(out, 0.95)


sound("ambient.parking_lot", [("ambient/parking_lot", lot_loop)], stream=True)
sound("ambient.parking_lot.additions",
      [("ambient/parking_lot_cart", cart_roll), ("ambient/parking_lot_alarm", alarm),
       ("ambient/parking_lot_slam", door_slam)],
      subtitle="Something in the lot")
sound("event.parking_lot_idle", [("event/parking_lot_idle", idle)], subtitle="An engine idles")
sound("event.parking_lot_horn", [("event/parking_lot_horn", horn_hit)], subtitle="A horn, right on top of you")

level("parking_lot", fog="#3a2414", sky="#2a1a10", ambient="ambient.parking_lot",
      additions="ambient.parking_lot.additions", additions_chance=0.003, fixed_time=18000, skylight=True,
      ceiling=False, height=64)
