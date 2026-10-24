"""Boiler Room - a vast soot-black hall of riveted boilers, lit by their fireboxes.
The pressure always climbs. When the gauges hit red, the relief valves blow."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, hazard, level, sound, custom, stairs, BLOCK_MODELS
from content.ind_kit import anim_tex, text, rivets, vstreaks, rust, steel, box_model, facing_states, glow_spot
import tex as T
import synth as Y

S = 32


def soot(img, seed, amount=0.4):
    m = np.clip(T.fbm(seed, octaves=4, base=4) * 1.6 - (1.3 - amount), 0, 1)
    return T.blend(img, T.solid("#0e0c0b"), m * 0.8)


def plate(seed="brp"):
    img = steel(seed, "#2f2b28", "#46403a", brushed=False)
    img = rust(img, seed + "r", 0.3, "#4a2a18", "#6a3a1e")
    img = rivets(img, [(x, 3) for x in range(2, 32, 5)] + [(x, 28) for x in range(4, 32, 5)], "#6a625a", "#141210")
    img = T.shade(img, 1 - ((np.mgrid[0:S, 0:S][0] % 32) == 0) * 0.3)
    return T.grain(soot(img, seed + "s", 0.35), seed, 0.04)


def plate_seam():
    img = plate("brps")
    yy, xx = np.mgrid[0:S, 0:S]
    band = (np.abs(xx - 16) < 3)
    img = T.blend(img, steel("brpsb", "#3a3530", "#524a42", brushed=False), band.astype(float))
    img = rivets(img, [(14, y) for y in range(1, 32, 4)] + [(18, y) for y in range(3, 32, 4)], "#7a7068", "#141210")
    return vstreaks(img, "brpsv", "#6a3a1e", 3, 0.4)


def plate_hot():
    """Near the firebox the steel has gone straw, blue and purple with heat."""
    img = plate("brph")
    yy, xx = np.mgrid[0:S, 0:S]
    t = np.clip(1 - yy / 32.0 + T.noise("brpht", sigma=3) * 0.3, 0, 1)
    temper = T.ramp(t, "#3a2c4a", "#2c3c5a", "#7a6a3a")
    return T.blend(img, temper, t * 0.45)


def firebox(i):
    """Cast iron door with draft slots showing a living coal bed. Flickers frame to frame."""
    yy, xx = np.mgrid[0:S, 0:S]
    iron = T.ramp(T.noise("brfbi", sigma=1.2), "#1e1a18", "#2e2826")
    img = T.border(iron, "#121010", 2)
    img = rivets(img, [(3, 3), (28, 3), (3, 28), (28, 28)], "#5a524a", "#0a0808")
    coal = T.ramp(T.noise(f"brfbc{i % 3}", sigma=1.0) * 0.7 + T.noise(f"brfbd{i}", sigma=2.5) * 0.3,
                  "#5a0a00", "#d43a00", "#ff8a10", "#ffd060")
    slots = ((yy > 7) & (yy < 13) | (yy > 18) & (yy < 24)) & (xx > 5) & (xx < 27) & ((xx % 5) != 0)
    img = np.where(slots[..., None], coal, img)
    # glow spilling onto the door face
    g = glow_spot((S, S), 16, 16, 18, 1.5) * (0.25 + 0.05 * (i % 2))
    img = T.blend(img, T.solid("#ff6a10"), g * (~slots))
    img = T.rect(img, 13, 26, 19, 29, "#3a3430")
    return img


def firebox_open(i):
    yy, xx = np.mgrid[0:S, 0:S]
    coal = T.ramp(T.noise(f"brfo{i}", sigma=1.2) * 0.6 + T.noise(f"brfo2{i % 2}", sigma=3) * 0.4,
                  "#7a1400", "#e04a00", "#ffa020", "#fff0a0")
    flame = np.clip((1 - yy / 32.0) * 1.4 - T.noise(f"brfl{i}", sigma=1.5) * 0.9, 0, 1)
    img = T.blend(coal, T.solid("#fff4c0"), flame * 0.5)
    return T.border(img, "#1a1614", 3)


def gauge(stage):
    """Brass-bezel pressure gauge. Stage 0..3 moves the needle toward the red."""
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        cx, cy = 15.5, 15.5
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        ang = np.degrees(np.arctan2(-(yy - cy), xx - cx))  # 0 = right, ccw
        img = T.ramp(T.noise("brgface", sigma=2), "#d8d0b8", "#ebe4cc")
        # sweep runs clockwise from 225 deg to -45 deg
        sweep = ((225 - ang) % 360)
        on_arc = (sweep <= 270)
        red = on_arc & (sweep > 200) & (d > 9) & (d < 12.5)
        img = T.blend(img, T.solid("#b01a10"), red.astype(float))
        ticks = on_arc & (np.abs((sweep % 27) - 0) < 3.5) & (d > 10) & (d < 13)
        img = T.blend(img, T.solid("#201a14"), ticks.astype(float))
        img = text(img, "PSI", 11, 19, "#3a3228")
        frac = (0.12, 0.45, 0.72, 0.97)[stage]
        a = np.radians(225 - frac * 270)
        for r in np.linspace(0, 11, 40):
            px, py = int(round(cx + np.cos(a) * r)), int(round(cy - np.sin(a) * r))
            img[py, px] = T.hexc("#101010")
            img[min(31, py + 1), px] = T.hexc("#3a3a3a")
        img[14:18, 14:18] = T.hexc("#2a2420")
        bezel = (d >= 13) & (d < 15.5)
        brass = T.ramp(T.noise("brgb", sigma=1), "#7a5a20", "#c8a050")
        img = np.where(bezel[..., None], brass, img)
        glass = np.clip(1 - np.abs((xx - yy) - 4) / 3.0, 0, 1) * (d < 13) * 0.25
        img = T.blend(img, T.solid("#ffffff"), glass)
        a_mask = (d < 15.5).astype(float)
        return img, a_mask
    return f


def brick(seed="brb", lo="#4a2418", hi="#6a3420"):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm(seed, octaves=3, base=2), lo, hi)
    per = T.rng(seed + "per").random((4, 4)) * 0.3
    row = yy // 8
    col = ((xx + (row % 2) * 8) // 16)
    img = T.shade(img, 0.85 + per[row % 4, col % 2])
    mortar = ((yy % 8) == 0) | (((xx + (row % 2) * 8) % 16) == 0)
    img = T.blend(img, T.solid("#2a2420"), mortar.astype(float) * 0.85)
    return T.grain(soot(img, seed + "s", 0.45), seed, 0.05)


def brick_base():
    img = brick("brbb")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    tile = T.ramp(T.noise("brbt", sigma=1), "#5a5040", "#6a6050")
    img = np.where((yy > 18)[..., None], tile, img)
    img[18] = T.hexc("#2a2420")
    return soot(img, "brbbs", 0.5)


def chalk():
    img = T.ramp(T.noise("brch", sigma=2), "#1e2620", "#26302a")
    img = T.border(img, "#5a4028", 2)
    img = text(img, "PSI LOG", 3, 4, "#c8c8b8")
    yy, xx = np.mgrid[0:S, 0:S]
    for k, n in enumerate((5, 5, 5, 3)):
        for t in range(n):
            x = 4 + t * 2 + (k % 2) * 13
            y0 = 12 + (k // 2) * 9
            img[y0:y0 + 6, x] = T.hexc("#d8d8c8")
        if n == 5:
            for t in range(9):
                img[12 + (k // 2) * 9 + t * 6 // 9, 3 + (k % 2) * 13 + t] = T.hexc("#d8d8c8")
    img = text(img, "WHY", 18, 26, "#d8a8a0")
    return T.grain(img, "brch", 0.04)


def floor():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("brf", octaves=3, base=3), "#3a2a20", "#4e3a2c")
    joint = ((yy % 16) == 0) | ((xx % 16) == 0)
    img = T.blend(img, T.solid("#1e1612"), joint.astype(float) * 0.8)
    oil = T.stains(img, "brfo", "#120e0c", 3, 0.45, 9)
    return T.grain(soot(oil, "brfs", 0.3), "brf", 0.04)


def floor_grate():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("brfg", "#2a2622", "#3e3830", brushed=False)
    img = rust(img, "brfgr", 0.35, "#4a2a18", "#6a3a1e")
    a = (((xx % 5) < 2) | ((yy % 16) < 2)).astype(float)
    return img, a


def ceiling():
    img = T.ramp(T.fbm("brc", octaves=3, base=4), "#161210", "#221c18")
    return T.grain(soot(img, "brcs", 0.6), "brc", 0.04)


def pipe_bare():
    img = steel("brpb", "#3a3430", "#56504a", brushed=True)
    img = rust(img, "brpbr", 0.45, "#5a2a14", "#7a4220")
    yy, xx = np.mgrid[0:S, 0:S]
    flange = (yy % 16 < 2)
    img = T.blend(img, T.solid("#2a2420"), flange.astype(float) * 0.7)
    return T.grain(soot(img, "brpbs", 0.3), "brpb", 0.04)


def pipe_lagged():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("brpl", sigma=1.5), "#a8a090", "#c4bca8")
    weave = ((xx + yy) % 3 == 0).astype(float) * 0.05
    img = T.shade(img, 1 - weave)
    band = (yy % 16 < 2)
    img = T.blend(img, T.solid("#6a6458"), band.astype(float))
    img = T.stains(img, "brpls", "#5a4a30", 2, 0.4, 8)
    return T.grain(soot(img, "brpls2", 0.25), "brpl", 0.04)


def wheel():
    img = T.ramp(T.noise("brwh", sigma=1), "#8a1410", "#b4241a")
    return T.grain(rust(img, "brwhr", 0.3, "#3a1a10", "#5a2a18"), "brwh", 0.05)


def brass():
    return T.grain(T.ramp(T.noise("brbr", sigma=1.2), "#7a5a24", "#b48a40"), "brbr", 0.05)


def lamp_glass():
    img = T.ramp(T.noise("brlg", sigma=2), "#ffb060", "#ffd8a0")
    return img


def lamp_cage():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.solid("#2a2420")
    a = (((xx % 6) < 1) | ((yy % 8) < 1)).astype(float)
    return rgb, a


def coal():
    img = T.ramp(T.noise("brcoal", sigma=0.8), "#080808", "#262422")
    glints = (T.rng("brcg").random((S, S)) > 0.95).astype(float)
    return T.shade(img, 1 + glints * 0.8)


def maint_door():
    img = steel("brdoor", "#3a3a34", "#4a4a42", brushed=False)
    img = T.border(img, "#1a1a16", 2)
    img = T.rect(img, 23, 14, 28, 18, "#8a8070")
    img = T.rect(img, 5, 5, 27, 11, "#c8a020")
    img = text(img, "BOILER", 4, 6, "#1a1610")
    img = rust(img, "brdoorr", 0.25, "#4a2a18", "#6a3a1e")
    return soot(img, "brdoors", 0.3)


def stair_light():
    img = T.ramp(T.noise("brsl", sigma=3), "#e8c890", "#fff0d0")
    return img


tex("br_plate", lambda: plate())
tex("br_plate_seam", plate_seam)
tex("br_plate_hot", plate_hot)
anim_tex("br_firebox", firebox, 6, frametime=3, interpolate=True)
anim_tex("br_firebox_open", firebox_open, 6, frametime=3, interpolate=True)


def ash_door(i):
    yy, xx = np.mgrid[0:S, 0:S]
    img = plate("brad")
    slot = (yy > 20) & (yy < 27) & (xx > 6) & (xx < 26)
    coal = T.ramp(T.noise(f"bradc{i}", sigma=1.2), "#4a0800", "#c03000", "#ff7010")
    img = np.where(slot[..., None], coal, img)
    g = np.clip(1 - np.abs(yy - 23) / 9.0, 0, 1) * 0.3 * (~slot)
    return T.blend(img, T.solid("#ff5a10"), g)


anim_tex("br_ash_door", ash_door, 4, frametime=4, interpolate=True)
for st in range(4):
    tex(f"br_gauge_{st}", gauge(st))
tex("br_brick", lambda: brick())
tex("br_brick_base", brick_base)
tex("br_chalk", chalk)
tex("br_floor", floor)
tex("br_floor_grate", floor_grate)
tex("br_ceiling", ceiling)
tex("br_pipe", pipe_bare)
tex("br_pipe_lagged", pipe_lagged)
tex("br_wheel", wheel)
tex("br_brass", brass)
tex("br_lamp_glass", lamp_glass)
tex("br_lamp_cage", lamp_cage)
tex("br_coal", coal)
tex("br_door", maint_door)
tex("br_stair_light", stair_light)

cube("br_boiler_plate", "br_plate", display="Boiler Plate", sound="metal")
cube("br_boiler_seam", "br_plate_seam", display="Riveted Seam", sound="metal")
cube("br_boiler_hot", "br_plate_hot", display="Heat-Tempered Plate", sound="metal")
hazard("br_firebox", "br_firebox", damage=3.0, display="Firebox Door", light=15, emissive=True, sound="metal")
cube("br_ash_door", "br_ash_door", display="Ash Door", light=11, emissive=True, sound="metal")
hazard("br_firebox_open", "br_firebox_open", damage=6.0, display="Open Firebox", light=15, emissive=True, sound="metal")
cube("br_brick_wall", "br_brick", display="Sooty Brick")
cube("br_brick_base", "br_brick_base", display="Sooty Brick")
cube("br_chalkboard", "br_chalk", display="Pressure Log")
stairs("br_brick_stairs", "br_brick", "br_brick_wall", display="Brick Stairs")
cube("br_floor", "br_floor", display="Boiler Room Floor")
cube("br_ceiling", "br_ceiling", display="Sooty Vault", flat=True)
exit_block("br_ash_grate", "br_floor_grate", "DESCEND", trigger="USE", solid=True, render="cutout",
           display="Ash Pit Grate", sound="metal")
exit_block("br_maint_door", "br_door", "DOOR", trigger="USE", solid=True, display="Boiler Room Door", sound="metal")
exit_block("br_stair_light", "br_stair_light", "ASCEND", display="Warm Light Upstairs", light=12, emissive=True)

# pipes: horizontal (authored along z, rotate for x) and vertical, bare and lagged
for name, t in (("br_pipe", "br_pipe"), ("br_pipe_lagged", "br_pipe_lagged")):
    shaped(name + "_h", [(4, 4, 0, 12, 12, 16, t), (3, 3, 0, 13, 13, 1.5, "f"), (3, 3, 14.5, 13, 13, 16, "f")],
           {t: t, "f": "br_pipe"}, display="Steam Pipe", sound="metal", unbreakable=True)
    shaped(name + "_v", [(4, 0, 4, 12, 16, 12, t), (3, 0, 3, 13, 1.5, 13, "f")],
           {t: t, "f": "br_pipe"}, display="Steam Riser", sound="metal", unbreakable=True)
shaped("br_lamp", [(6, 4, 6, 10, 10, 10, "g"), (5, 3, 5, 11, 11, 11, "c"), (7.5, 11, 7.5, 8.5, 16, 8.5, "c"),
                   (6, 10, 6, 10, 11, 10, "c")],
       {"g": "br_lamp_glass", "c": "br_lamp_cage"}, display="Caged Lamp", light=12, emissive=True,
       collides=False, unbreakable=True, sound="glass")
shaped("br_coal_pile", [(0, 0, 0, 16, 4, 16, "c"), (2, 4, 1, 14, 8, 13, "c"), (4, 8, 3, 11, 11, 10, "c")],
       {"c": "br_coal"}, display="Coal", sound="gravel", unbreakable=True)
shaped("br_cot", [(1, 3, 0, 15, 5, 16, {"all": "canvas", "down": "frame"}), (1, 0, 0, 2, 3, 1, "frame"),
                  (14, 0, 0, 15, 3, 1, "frame"), (1, 0, 15, 2, 3, 16, "frame"), (14, 0, 15, 15, 3, 16, "frame"),
                  (2, 5, 1, 14, 6.5, 10, "blanket")],
       {"canvas": "br_pipe_lagged", "frame": "br_pipe", "blanket": "br_wheel"}, display="Cot", sound="wool")

# custom: gauges (needle moves with the level's pressure), valve wheels (bleed it), relief valves (blow)
for st in range(4):
    BLOCK_MODELS[f"br_gauge_{st}"] = box_model([(2, 2, 0, 14, 14, 1.5, {"all": "b", "south": "g"})],
                                               {"g": f"br_gauge_{st}", "b": "br_brass"})
custom("br_gauge", "com.noclip.block.GaugeBlock", BLOCK_MODELS["br_gauge_0"],
       blockstate=facing_states(lambda v: f"br_gauge_{v}", ("age", [0, 1, 2, 3])),
       display="Pressure Gauge", sound="metal", unbreakable=True, render="cutout")
custom("br_valve", "com.noclip.block.ValveWheelBlock",
       box_model([(7, 7, 0, 9, 9, 10, "p"), (2, 7.5, 10, 14, 8.5, 11.5, "w"), (7.5, 2, 10, 8.5, 14, 11.5, "w"),
                  (2, 2, 10, 3.5, 14, 11.5, "w"), (12.5, 2, 10, 14, 14, 11.5, "w"), (3.5, 2, 10, 12.5, 3.5, 11.5, "w"),
                  (3.5, 12.5, 10, 12.5, 14, 11.5, "w"), (6.5, 6.5, 9, 9.5, 9.5, 12, "p")],
                 {"w": "br_wheel", "p": "br_brass"}),
       blockstate=facing_states(lambda v: "br_valve"), display="Bleed Valve", sound="metal", unbreakable=True,
       render="cutout")
custom("br_relief", "com.noclip.block.ReliefValveBlock",
       box_model([(5, 0, 5, 11, 7, 11, "b"), (4, 7, 4, 12, 10, 12, "b"), (6.5, 10, 6.5, 9.5, 15, 9.5, "b"),
                  (5, 15, 5, 11, 16, 11, "p"), (0, 2, 6, 5, 6, 10, "p")],
                 {"b": "br_brass", "p": "br_pipe"}),
       blockstate=facing_states(lambda v: "br_relief"), display="Relief Valve", sound="metal", unbreakable=True,
       render="cutout")

# ------------------------------------------------------------------ sound


def burners():
    d = 26.0
    t = Y.t_axis(d)
    roar = Y.lp(Y.brown(d, "brroar"), 160) * 0.9
    flicker = 0.8 + 0.2 * Y.lp(Y.rng("brfl").standard_normal(len(t)), 3) * 30
    flame = Y.bp(Y.pink(d, "brflame"), 120, 900) * 0.35 * np.clip(flicker, 0.4, 1.3)
    hiss = Y.bp(Y.white(d, "brhiss"), 3000, 9000) * 0.035 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 13))
    pump = np.zeros(len(t))
    for k in range(int(d / 1.3)):
        Y.place(pump, Y.impact(f"brpump{k}", 0.5) * 0.25, k * 1.3)
    pump = Y.lp(pump, 200)
    x = roar + flame + hiss + pump
    return Y.stereoize(Y.loopable(Y.norm(x, 0.55), 1.5), 0.0016), "stereo"


def water_hammer(i):
    def f():
        out = np.zeros(int(3.5 * Y.SR))
        r = Y.rng(f"brwh{i}")
        tt = 0.05
        for k in range(3 + i * 2):
            Y.place(out, Y.impact(f"brwh{i}{k}", 0.4 + r.random() * 0.3, ring=0.9), tt, 0.5 + r.random() * 0.5)
            tt += 0.09 + r.random() * 0.2
        return Y.norm(Y.reverb(Y.lp(out, 3500), 3.0, 0.6, f"brwh{i}"), 0.8)
    return f


def hiss_burst(i):
    def f():
        d = 2.5 + i
        t = Y.t_axis(d)
        env = (1 - np.exp(-t * 20)) * np.exp(-t * (1.6 - i * 0.4))
        x = Y.bp(Y.white(d, f"brhb{i}"), 1500, 12000) * env
        return Y.norm(Y.reverb(x, 1.8, 0.4, f"brhb{i}"), 0.7)
    return f


def groan():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 55 + 12 * np.sin(2 * np.pi * 0.4 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    x += 0.4 * np.sin(2 * np.pi * np.cumsum(f0 * 2.03) / Y.SR) * np.sin(np.pi * t / d) ** 3
    return Y.norm(Y.reverb(Y.lp(x, 600), 3.5, 0.5, "brgroan"), 0.7)


def blowout():
    d = 5.0
    t = Y.t_axis(d)
    env = (1 - np.exp(-t * 30)) * np.clip((d - t) / 1.5, 0, 1)
    x = Y.bp(Y.white(d, "brbo"), 400, 14000) * env
    x += Y.lp(Y.brown(d, "brbo2"), 200) * env * 1.5
    whistle = np.sin(2 * np.pi * np.cumsum(2200 + 300 * np.sin(2 * np.pi * 3 * t)) / Y.SR) * 0.08 * env
    return Y.norm(Y.reverb(x + whistle, 2.5, 0.4, "brbo"), 0.95)


def bleed():
    d = 2.6
    t = Y.t_axis(d)
    squeal = np.sin(2 * np.pi * np.cumsum(900 - 300 * t / d) / Y.SR) * np.exp(-t * 6) * 0.3
    x = Y.bp(Y.white(d, "brbl"), 1000, 10000) * (1 - np.exp(-t * 15)) * np.exp(-t * 1.2)
    clank = np.zeros(len(t))
    Y.place(clank, Y.impact("brblc", 0.3, ring=0.6), 0.0, 0.7)
    return Y.norm(Y.reverb(x + squeal + clank, 1.5, 0.35, "brbl"), 0.85)


def knocking():
    """Pipes banging faster and faster as the pressure climbs."""
    out = np.zeros(int(3.0 * Y.SR))
    tt = 0.05
    for k in range(12):
        Y.place(out, Y.impact(f"brkn{k}", 0.35, ring=0.7), tt, 0.4 + k * 0.05)
        tt += max(0.08, 0.35 - k * 0.025)
    return Y.norm(Y.reverb(Y.lp(out, 3000), 2.5, 0.5, "brkn"), 0.85)


sound("ambient.boiler_room", [("ambient/boiler_room", burners)], stream=True)
sound("ambient.boiler_room.additions",
      [(f"ambient/boiler_room_hammer{i}", water_hammer(i)) for i in range(3)] +
      [(f"ambient/boiler_room_hiss{i}", hiss_burst(i)) for i in range(2)] + [("ambient/boiler_room_groan", groan)],
      subtitle="The boilers bang and hiss")
sound("event.boiler_room_blowout", [("event/boiler_room_blowout", blowout)], subtitle="A relief valve blows")
sound("event.boiler_room_bleed", [("event/boiler_room_bleed", bleed)], subtitle="Steam bleeds off")
sound("event.boiler_room_knock", [("event/boiler_room_knock", knocking)], subtitle="The pipes knock")

level("boiler_room", fog="#1c0f08", sky="#1c0f08", ambient="ambient.boiler_room",
      additions="ambient.boiler_room.additions", additions_chance=0.008, fixed_time=18000, height=64,
      particle="minecraft:ash", particle_chance=0.01)
