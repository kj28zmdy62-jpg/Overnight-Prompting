"""Power Plant - turbine halls the size of churches, a control room where every alarm is
ringing and nobody has answered one in years. Answer them, or the plant trips and the
lights go with it."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, custom, pillar, BLOCK_MODELS
from content.ind_kit import anim_tex, text, rivets, steel, concrete, hazard_stripes, rust, vstreaks, box_model, \
    facing_states, glow_spot
import tex as T
import synth as Y

S = 32


def floor(seed="pwrf"):
    img = concrete(seed, "#6a6e68", "#7c8078", stain="#3a3c36", stains=2)
    img = T.blend(img, T.solid("#5a7a62"), np.full((S, S), 0.25))
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1 - (((xx % 32) == 0) | ((yy % 32) == 0)) * 0.2)


def floor_hazard():
    return hazard_stripes(floor("pwrfh"), 0, 32, "pwrfhs", width=6, worn=0.35)


def floor_walk():
    img = floor("pwrfw")
    paint = T.ramp(T.noise("pwrfwp", sigma=1), "#2e6a3e", "#3a7a4a")
    worn = (T.noise("pwrfww", sigma=0.8) > 0.2).astype(float)
    return T.blend(img, paint, worn * 0.85)


def wall(seed="pwrw"):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=3), "#c8c2aa", "#d6d0b8")
    img = T.shade(img, 1 - ((((yy % 16) == 0) | (((xx + (yy // 16) * 16) % 32) == 0))) * 0.08)
    img = vstreaks(img, seed + "v", "#7a7258", 2, 0.25)
    return T.grain(img, seed, 0.03)


def wall_low():
    img = wall("pwrwl")
    yy = np.mgrid[0:S, 0:S][0]
    green = T.ramp(T.noise("pwrwlg", sigma=2), "#2e5a3e", "#3a6a4a")
    img = T.blend(img, green, (yy > 8).astype(float))
    img[7:9] = T.hexc("#1a2a1e")
    return T.stains(img, "pwrwls", "#1e2a20", 2, 0.3, 7)


def wall_sign(label, sub, col="#c01818"):
    def f():
        img = wall("pwrws" + label)
        img = T.rect(img, 2, 6, 30, 26, "#f0ece0")
        img = T.rect(img, 2, 6, 30, 13, col)
        img = text(img, label, 16 - (len(label) * 4 - 1) // 2, 7, "#ffffff")
        img = text(img, sub, 16 - (len(sub) * 4 - 1) // 2, 17, "#1a1a1a")
        return T.border(img, "#2a2a2a", 1)
    return f


def casing(seed="pwrc"):
    img = T.ramp(T.fbm(seed, octaves=3, base=4), "#2e5a48", "#3e7058")
    img = rivets(img, [(x, 2) for x in range(2, 32, 6)] + [(x, 29) for x in range(5, 32, 6)], "#7aa08a", "#10201a")
    img = T.shade(img, 1 - ((np.mgrid[0:S, 0:S][1] % 16) == 0) * 0.25)
    img = vstreaks(img, seed + "v", "#1a2a20", 3, 0.3)
    return T.grain(T.stains(img, seed + "s", "#1a2a22", 2, 0.3, 6), seed, 0.03)


def casing_top():
    img = casing("pwrct")
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1.1 - np.abs(yy - 16) / 16 * 0.25)


def generator(label=False):
    img = T.ramp(T.fbm("pwrg", octaves=3, base=4), "#7a261c", "#963426")
    img = rivets(img, [(x, 3) for x in range(3, 32, 7)], "#e0a090", "#3a0a08")
    img = vstreaks(img, "pwrgv", "#3a1008", 3, 0.3)
    if label:
        img = T.rect(img, 4, 12, 28, 22, "#1a1a1a")
        img = text(img, "GEN 4", 6, 15, "#e0e0e0")
    return T.grain(img, "pwrg", 0.03)


def plinth():
    img = concrete("pwrpl", "#5a5c58", "#6c6e6a", crack=1)
    return hazard_stripes(img, 0, 6, "pwrplh", width=4)


def pipe_side():
    img = T.ramp(T.noise("pwrps", sigma=2), "#8a8a84", "#a4a49c")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 0.75 + 0.35 * np.sin(np.pi * xx / 31))
    return T.grain(img, "pwrps", 0.03)


def pipe_end():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(T.noise("pwrpe", sigma=1), "#6a6a64", "#8a8a84")
    return T.blend(img, T.solid("#3a3a36"), (d < 10).astype(float))


def panel(seed="pwrpn"):
    """Mimic board: beige steel, flow lines, round gauges, rows of toggle switches."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=2), "#b8b29a", "#c8c2aa")
    r = T.rng(seed)
    img[6, :] = T.hexc("#2a6a3a")
    img[6:20, 26] = T.hexc("#2a6a3a")
    for k in range(2):
        cx, cy = 8 + k * 12, 14
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        img = T.blend(img, T.solid("#f0eee4"), (d < 5).astype(float))
        img = T.blend(img, T.solid("#2a2a2a"), ((d >= 5) & (d < 6)).astype(float))
        a = r.random() * 3 + 0.5
        for t in range(4):
            img[int(cy - np.cos(a) * t), int(cx + np.sin(a) * t)] = T.hexc("#c01818")
    for k in range(7):
        x = 3 + k * 4
        img = T.rect(img, x, 24, x + 2, 28, "#3a3a3a")
        img[25 if r.random() < 0.5 else 27, x:x + 2] = T.hexc("#d0d0d0")
    return T.border(T.grain(img, seed, 0.02), "#6a6450", 1)


def annunciator(i):
    """A grid of alarm windows. Most are lit. Some flash. None have been acknowledged."""
    img = T.ramp(T.noise("pwran", sigma=1), "#1a1a18", "#24241e")
    r = T.rng("pwranr")
    labels = ["HI", "LO", "TRP", "OVR", "VIB", "FLT", "TMP", "PRS", "AUX"]
    for row in range(4):
        for col in range(3):
            x0, y0 = 1 + col * 10, 1 + row * 8
            state = r.random()
            flash = r.random() < 0.5
            on = state < 0.75 and (not flash or (i + row + col) % 2 == 0)
            c = ("#ff3a20" if state < 0.45 else "#ffb020") if on else "#3a3428"
            img = T.rect(img, x0, y0, x0 + 9, y0 + 7, c)
            lab = labels[int(r.random() * len(labels))]
            img = text(img, lab, x0 + 1, y0 + 1, "#1a0a08" if on else "#5a5040")
    return img


def screen(i, odd=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("pwrscr", sigma=2), "#041008", "#08180c")
    img = T.shade(img, 1 - ((yy % 2) == 0) * 0.15)
    if odd:
        img = text(img, "ARE", 4, 6, "#60ff80")
        img = text(img, "YOU", 4, 13, "#60ff80")
        img = text(img, "THERE" if i % 2 == 0 else "", 4, 20, "#60ff80")
    else:
        for x in range(3, 29):
            v = np.sin((x + i * 2) / 3.0) * 4 + np.sin((x + i) / 1.3) * 1.5 + 16
            img[int(np.clip(v, 3, 28)), x] = T.hexc("#60ff80")
        img[8, 3:29] = T.hexc("#ff3020") * 0.7
    return T.border(img, "#1a1c1a", 2)


def desk_side():
    img = T.ramp(T.noise("pwrds", sigma=1.5), "#8a8470", "#9a947e")
    return T.border(T.grain(img, "pwrds", 0.03), "#5a5444", 1)


def desk_top():
    img = T.ramp(T.noise("pwrdt", sigma=2), "#3a3a36", "#46463e")
    return T.stains(img, "pwrdts", "#2a2218", 1, 0.4, 4)


def ack_top():
    img = desk_top()
    img = T.rect(img, 3, 3, 29, 9, "#e0d8c0")
    img = text(img, "ACK", 10, 4, "#c01818")
    return img


def ack_button():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 16, 0, 1), "#7a0a08", "#e02a1a", "#ff8a70")
    return img


def ceiling():
    img = T.ramp(T.noise("pwrcl", sigma=1), "#c8c6be", "#d6d4cc")
    pits = (T.rng("pwrclp").random((S, S)) > 0.85).astype(float)
    img = T.shade(img, 1 - pits * 0.12)
    img = T.border(img, "#8a887e", 1)
    return T.stains(img, "pwrcls", "#a09668", 1, 0.3, 8)


def light_panel():
    img = T.ramp(T.noise("pwrlp", sigma=3), "#f4f8ee", "#ffffff")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.08)
    return T.border(T.border(img, "#c8ccc0", 2), "#8a8e84", 1)


def flood():
    yy, xx = np.mgrid[0:S, 0:S]
    g = glow_spot((S, S), 15.5, 15.5, 14, 1.2)
    img = T.ramp(g, "#806a3a", "#ffe6b0", "#ffffff")
    return T.border(img, "#2a2a2a", 3)


def beacon(i, n):
    yy, xx = np.mgrid[0:S, 0:S]
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    sweep = np.cos(ang - 2 * np.pi * i / n) * 0.5 + 0.5
    img = T.ramp(sweep ** 3, "#5a2a04", "#ff8a10", "#fff4d0")
    return T.border(img, "#2a2a2a", 2)


def beacon_off():
    return T.border(T.ramp(T.noise("pwrbo", sigma=2), "#3a2408", "#4a2e10"), "#2a2a2a", 2)


def exit_door():
    img = steel("pwrdoor", "#4a6a54", "#5a7a64", brushed=False)
    img = T.border(img, "#2a3a2e", 2)
    img = T.rect(img, 4, 4, 28, 12, "#f0ece0")
    img = text(img, "STAFF", 6, 5, "#1a1a1a")
    img = T.rect(img, 22, 16, 27, 19, "#2a2e2a")
    return img


def grate():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("pwrgr", "#3a3e42", "#545a60", brushed=False)
    holes = ((xx % 4) != 0) & ((yy % 4) != 0)
    return T.blend(img, T.solid("#0a0a0a"), holes.astype(float) * 0.85)


tex("pwr_floor", lambda: floor())
tex("pwr_floor_hazard", floor_hazard)
tex("pwr_floor_walk", floor_walk)
tex("pwr_wall", lambda: wall())
tex("pwr_wall_low", wall_low)
tex("pwr_sign_turbine", wall_sign("DANGER", "TURBINE"))
tex("pwr_sign_ears", wall_sign("NOTICE", "EARS", "#1a4a9a"))
tex("pwr_casing", lambda: casing())
tex("pwr_casing_top", casing_top)
tex("pwr_generator", generator)
tex("pwr_generator_label", lambda: generator(True))
tex("pwr_plinth", plinth)
tex("pwr_pipe_side", pipe_side)
tex("pwr_pipe_end", pipe_end)
tex("pwr_panel", lambda: panel())
tex("pwr_panel_b", lambda: panel("pwrpn2"))
anim_tex("pwr_annunciator", annunciator, 2, frametime=8)
anim_tex("pwr_screen", lambda i: screen(i), 8, frametime=4)
anim_tex("pwr_screen_odd", lambda i: screen(i, True), 2, frametime=16)
tex("pwr_desk_side", desk_side)
tex("pwr_desk_top", desk_top)
tex("pwr_ack_top", ack_top)
tex("pwr_ack_button", ack_button)
tex("pwr_ceiling", ceiling)
tex("pwr_light", light_panel)
tex("pwr_flood", flood)
anim_tex("pwr_beacon", lambda i: beacon(i, 6), 6, frametime=1)
tex("pwr_beacon_off", beacon_off)
tex("pwr_door", exit_door)
tex("pwr_grate", grate)

cube("pwr_floor", "pwr_floor", display="Turbine Hall Floor")
cube("pwr_floor_hazard", "pwr_floor_hazard", display="Hazard Floor")
cube("pwr_floor_walk", "pwr_floor_walk", display="Walkway")
cube("pwr_wall", "pwr_wall", display="Painted Block")
cube("pwr_wall_low", "pwr_wall_low", display="Painted Block")
cube("pwr_sign_turbine", "pwr_sign_turbine", display="Danger Sign")
cube("pwr_sign_ears", "pwr_sign_ears", display="Hearing Protection Sign")
cube("pwr_casing", "pwr_casing", display="Turbine Casing", sound="metal")
cube_faces("pwr_casing_top", top="pwr_casing_top", bottom="pwr_casing", side="pwr_casing", display="Turbine Casing",
           sound="metal")
cube("pwr_generator", "pwr_generator", display="Generator", sound="metal")
cube("pwr_generator_label", "pwr_generator_label", display="Generator", sound="metal")
shaped("pwr_hall_lamp", [(3, 10, 3, 13, 15, 13, {"all": "h", "down": "l"}), (7, 15, 7, 9, 16, 9, "h")],
       {"l": "pwr_light", "h": "pwr_grate"}, display="Hall Lamp", sound="metal", unbreakable=True, light=15,
       emissive=True, collides=False)
cube("pwr_plinth", "pwr_plinth", display="Turbine Plinth")
pillar("pwr_pipe", "pwr_pipe_end", "pwr_pipe_side", display="Steam Main", sound="metal")
cube_faces("pwr_panel", top="pwr_desk_top", bottom="pwr_desk_side", side="pwr_panel", display="Control Board",
           sound="metal")
cube_faces("pwr_panel_b", top="pwr_desk_top", bottom="pwr_desk_side", side="pwr_panel_b", display="Control Board",
           sound="metal")
cube_faces("pwr_annunciator", top="pwr_desk_top", bottom="pwr_desk_side", side="pwr_annunciator",
           display="Annunciator Panel", sound="metal", light=5, emissive=True)
DESK = {"side": "pwr_desk_side", "top": "pwr_desk_top"}
shaped("pwr_desk", [(0, 0, 0, 16, 12, 16, {"all": "side", "up": "top"}),
                    (2, 12, 2, 14, 22, 6, {"all": "side", "south": "scr"})],
       dict(DESK, scr="pwr_screen"), display="Operator Console", sound="metal", unbreakable=True, light=5,
       emissive=True)
shaped("pwr_desk_odd", [(0, 0, 0, 16, 12, 16, {"all": "side", "up": "top"}),
                        (2, 12, 2, 14, 22, 6, {"all": "side", "south": "scr"})],
       dict(DESK, scr="pwr_screen_odd"), display="Operator Console", sound="metal", unbreakable=True, light=5,
       emissive=True)
custom("pwr_ack", "com.noclip.block.PwrAckBlock",
       box_model([(0, 0, 0, 16, 12, 16, {"all": "side", "up": "acktop"}), (5, 12, 5, 11, 15, 11, "btn")],
                 {"side": "pwr_desk_side", "acktop": "pwr_ack_top", "btn": "pwr_ack_button"}),
       blockstate=facing_states(lambda v: "pwr_ack"), display="Alarm Acknowledge", sound="metal", unbreakable=True,
       light=4, render="cutout")
cube("pwr_ceiling", "pwr_ceiling", display="Acoustic Tile", flat=True)
cube_faces("pwr_light", bottom="pwr_light", top="pwr_ceiling", side="pwr_ceiling", display="Light Panel", light=15,
           emissive=True, flat=True, sound="glass")
shaped("pwr_flood", [(3, 3, 0, 13, 13, 4, {"all": "pwr_desk_side", "south": "f"})],
       {"f": "pwr_flood", "pwr_desk_side": "pwr_grate"}, display="Floodlight", sound="metal", unbreakable=True,
       light=15, emissive=True, collides=False)
BLOCK_MODELS["pwr_beacon_on"] = box_model([(5, 0, 5, 11, 6, 11, "b"), (4, 0, 4, 12, 1, 12, "f")],
                                          {"b": "pwr_beacon", "f": "pwr_grate"})
custom("pwr_beacon", "com.noclip.block.AlarmBeaconBlock",
       box_model([(5, 0, 5, 11, 6, 11, "b"), (4, 0, 4, 12, 1, 12, "f")], {"b": "pwr_beacon_off", "f": "pwr_grate"}),
       blockstate={"variants": {"lit=false": {"model": "noclip:block/pwr_beacon"},
                                "lit=true": {"model": "noclip:block/pwr_beacon_on"}}},
       display="Warning Beacon", sound="glass", unbreakable=True, render="cutout")
exit_block("pwr_exit_door", "pwr_door", "DOOR", trigger="USE", solid=True, display="Staff Exit", sound="metal")
cube("pwr_grate", "pwr_grate", display="Floor Grating", sound="metal")


# ------------------------------------------------------------------ sound

def turbine_loop():
    d = 24.0
    t = Y.t_axis(d)
    hum = Y.hum(d, 50, "pwrhum", buzz=0.3, harmonics=(1, 2, 3, 4, 6)) * 0.35
    rush = Y.bp(Y.pink(d, "pwrrush"), 120, 3000) * 0.5
    whine = sum(np.sin(2 * np.pi * (round(f * d) / d) * t) * g for f, g in ((1500, 0.025), (3000, 0.012), (4510, 0.006)))
    beat = 1 + 0.06 * np.sin(2 * np.pi * t / 8.0)
    x = Y.reverb((hum + rush + whine) * beat, 3.0, 0.45, "pwrrv")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.6), 1.0), 0.004), "stereo"


def steam():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "pwrst"), 1500, 10000) * Y.env(len(t), 0.05, 1.5)
    return Y.norm(Y.reverb(x, 2.5, 0.5, "pwrstr"), 0.5)


def relay():
    out = np.zeros(int(2.0 * Y.SR))
    for k in range(3):
        Y.place(out, Y.impact(f"pwrrl{k}", 0.25, ring=0.4), 0.1 + k * 0.17, 1.0 - k * 0.2)
    return Y.norm(Y.reverb(out, 2.0, 0.5, "pwrrlr"), 0.5)


def pa():
    """A voice on the PA, too distorted to be words."""
    d = 3.0
    t = Y.t_axis(d)
    syl = (np.sin(2 * np.pi * 3.2 * t) > 0.1) * (t > 0.3) * (t < 2.6)
    f0 = 140 + 30 * np.sin(2 * np.pi * 0.7 * t)
    buzz = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * 0.4
    form = Y.bp(buzz, 500, 2500) + Y.bp(Y.white(d, "pwrpa"), 2000, 4000) * 0.1
    x = np.clip(form * syl * 3, -0.6, 0.6)
    return Y.norm(Y.reverb(Y.bp(x, 400, 3200), 3.5, 0.6, "pwrpar"), 0.4)


def bang():
    return Y.norm(Y.reverb(Y.impact("pwrbang", 1.8, ring=0.6), 4.0, 0.6, "pwrbangr"), 0.6)


def klaxon():
    d = 2.0
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(np.full(len(t), 440.0) + 80 * (t % 0.5 < 0.25)) / Y.SR)) * 0.4
    x = Y.lp(x, 3000) * ((t % 1.0) < 0.7)
    return Y.norm(Y.reverb(x, 2.5, 0.5, "pwrkl"), 0.85)


def trip():
    d = 7.0
    t = Y.t_axis(d)
    f = 1500 * np.exp(-t / 2.0) + 30
    whine = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.3 * np.clip((d - t) / 3, 0, 1)
    hum = np.sin(2 * np.pi * np.cumsum(50 * np.exp(-t / 3.0) + 5) / Y.SR) * 0.4 * np.clip((d - t) / 4, 0, 1)
    x = whine + hum
    Y.place(x, Y.impact("pwrtrip", 1.6, ring=0.5), 0.0, 1.2)
    return Y.norm(Y.reverb(x, 3.0, 0.5, "pwrtripr"), 0.95)


def ack():
    d = 1.0
    t = Y.t_axis(d)
    x = (np.sin(2 * np.pi * 880 * t) * (t < 0.15) + np.sin(2 * np.pi * 660 * t) * (t > 0.2) * (t < 0.4)) * 0.4
    return Y.norm(Y.reverb(x, 1.0, 0.3, "pwrack"), 0.5)


sound("ambient.power_plant", [("ambient/power_plant", turbine_loop)], stream=True)
sound("ambient.power_plant.additions",
      [("ambient/power_plant_steam", steam), ("ambient/power_plant_relay", relay), ("ambient/power_plant_pa", pa),
       ("ambient/power_plant_bang", bang)], subtitle="The plant runs itself")
sound("event.power_plant_klaxon", [("event/power_plant_klaxon", klaxon)], subtitle="Alarm, unanswered")
sound("event.power_plant_trip", [("event/power_plant_trip", trip)], subtitle="Turbines trip")
sound("event.power_plant_ack", [("event/power_plant_ack", ack)], subtitle="Alarm acknowledged")

level("power_plant", fog="#1c2420", sky="#1c2420", ambient="ambient.power_plant",
      additions="ambient.power_plant.additions", additions_chance=0.008, fixed_time=18000, height=48)
