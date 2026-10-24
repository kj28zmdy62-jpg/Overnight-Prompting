"""Level 3 - Electrical Station. A brick maze packed with roaring machinery and live wires.
The power surges on a schedule nobody wrote down. The noise carries."""
import numpy as np
from assets import cube, cube_faces, pillar, tex, shaped, exit_block, custom, flicker_light, level, sound
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, box_model, facing_states, metal, bricks

S = 32
yy, xx = np.mgrid[0:S, 0:S]


# ------------------------------------------------------------------ textures

def brick():
    img = bricks("l3b", "#5a2a20", "#7a3c2c", mortar="#3a3430", bw=8, bh=4)
    soot = T.fbm("l3soot", octaves=3, base=5)
    img = T.blend(img, T.solid("#1a1412"), np.clip(soot * 1.2 - 0.55, 0, 0.6))
    return T.grain(img, "l3b2", 0.03)


def brick_paint():
    """Lower wall: thick institutional green paint over brick, scuffed through."""
    img = brick()
    paint = T.ramp(T.noise("l3pt", sigma=1.5), "#4a5a4c", "#5c6e5e")
    mortar_lines = bricks("l3b", "#000000", "#000000", mortar="#ffffff", bw=8, bh=4)[..., 0]
    paint = T.shade(paint, 1 - mortar_lines * 0.12)
    scuff = np.clip(T.noise("l3scf", sigma=0.9) * 2.2 - 1.45, 0, 1)
    img = T.blend(paint, img, scuff)
    img = T.rect(img, 0, 0, 32, 2, "#c8a82a")
    img = T.rect(img, 0, 2, 32, 3, "#2a2a26")
    return T.grain(img, "l3pt", 0.03)


def brick_paint_low():
    img = brick_paint()
    img = T.blend(img, T.solid("#1e1e1a"), np.clip((yy - 22) / 10.0, 0, 1) * 0.5 * T.noise("l3lg", sigma=1.2))
    return img


def conduit():
    img = brick()
    for x0 in (6, 20):
        t = (np.arange(4) + 0.5) / 4
        body = T.ramp(T.noise(f"l3cd{x0}", sigma=1, shape=(S, 4)), "#7a7c78", "#9a9c96") * (0.6 + 0.5 * np.sin(np.pi * t))[None, :, None]
        img[:, x0:x0 + 4] = np.clip(body, 0, 1)
        img[:, x0 + 4] *= 0.55
        for y0 in (4, 20):
            img[y0:y0 + 2, x0 - 1:x0 + 5] = T.hexc("#4a4c48")
    img = T.rect(img, 11, 12, 19, 20, "#6a6c66")
    img = T.rect(img, 12, 13, 18, 19, "#80827c")
    img = T.rect(img, 14, 15, 16, 17, "#2a2a28")
    return T.grain(img, "l3cd", 0.03)


def danger_sign():
    img = brick()
    img = T.rect(img, 3, 3, 29, 29, "#e0c020")
    img = T.rect(img, 3, 3, 29, 11, "#141414")
    img = centered(img, "DANGER", 5, "#e8e0d0")
    img = centered(img, "HIGH", 13, "#141414")
    img = centered(img, "VOLTAGE", 19, "#141414")
    # lightning bolt
    for k, (x, y) in enumerate([(16, 24), (15, 25), (16, 26), (17, 27), (16, 28)]):
        img[y, x] = T.hexc("#141414")
    return T.grain(img, "l3ds", 0.03)


def ceiling():
    img = T.ramp(T.fbm("l3c", octaves=4, base=4), "#2c2c2a", "#3a3a36")
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.3)
    return T.grain(img, "l3c", 0.04)


def floor():
    img = T.ramp(T.fbm("l3f", octaves=4, base=5), "#3c463e", "#4c584e")
    worn = np.clip(T.fbm("l3fw", octaves=3, base=6) * 1.6 - 0.7, 0, 1)
    img = T.blend(img, T.ramp(T.noise("l3fc", sigma=0.6), "#3a3a36", "#4a4a44"), worn)
    img = T.stains(img, "l3oil", "#1a1c1a", count=1, strength=0.4, size=6)
    return T.grain(T.cracks(img, "l3fcr", "#252825", 1, 0.4), "l3f", 0.04)


def plate():
    img = metal("l3pl", "#4a4c4e", "#62666a")
    d1 = ((xx + yy * 2) % 8 == 0) & ((yy % 8) < 3)
    d2 = ((xx * 2 - yy) % 8 == 0) & ((yy % 8) >= 4)
    img = T.shade(img, 1 + (d1 | d2) * 0.28)
    img = T.border(img, "#2a2c2e", 1)
    return T.grain(img, "l3pl", 0.03)


def stripe():
    img = floor()
    st = (((xx + yy) // 5) % 2 == 0)
    col = np.where(st[..., None], T.hexc("#c8a422"), T.hexc("#181816"))
    worn = (T.noise("l3stw", sigma=0.8) > 0.3)
    return T.blend(img, col, worn.astype(float) * 0.9)


def cabinet(seed, top=False):
    def f():
        img = metal(seed, "#8a8a7e", "#9e9e92", sigma=2)
        img = T.border(img, "#4a4a44", 1)
        img = T.rect(img, 15, 1, 16, 31, "#4a4a44")
        if top:
            # meters and indicator lamps
            for cx in (7, 23):
                d = np.sqrt((xx - cx) ** 2 + (yy - 9) ** 2)
                img = T.blend(img, T.solid("#e8e4d4"), (d < 4.5).astype(float))
                img = T.blend(img, T.solid("#2a2a2a"), (np.abs(d - 4.5) < 0.8).astype(float))
                img[9, cx:cx + 3] = T.hexc("#101010")
            for k, c in enumerate(("#30e050", "#e03020", "#e0b020", "#30e050")):
                img = T.rect(img, 4 + k * 7, 19, 7 + k * 7, 22, c)
            img = centered(img, "4KV", 25, "#2a2a26")
        else:
            for y in range(4, 14, 2):
                img = T.rect(img, 3, y, 13, y + 1, "#3a3a34")
                img = T.rect(img, 19, y, 29, y + 1, "#3a3a34")
            img = T.rect(img, 12, 18, 14, 24, "#2a2a28")
            img = T.rect(img, 17, 18, 19, 24, "#2a2a28")
        return T.grain(T.stains(img, seed + "s", "#5a5040", 1, 0.25, 6), seed, 0.03)
    return f


def transformer():
    img = metal("l3tr", "#3e4a42", "#4e5c52", sigma=1.5)
    fin = (xx % 4) < 1
    img = T.shade(img, 1 - fin * 0.4 + ((xx % 4) == 1) * 0.15)
    img = T.rect(img, 0, 0, 32, 3, "#2e3830")
    img = T.rect(img, 10, 12, 22, 20, "#d8d0b8")
    img = centered(img, "HV", 14, "#a01810")
    return T.grain(T.stains(img, "l3trs", "#5a3a20", 2, 0.3, 6), "l3tr", 0.03)


def insulator():
    img = T.ramp(T.noise("l3ins", sigma=1), "#6a3a20", "#8a5030")
    img = T.shade(img, 0.8 + 0.4 * np.sin(np.pi * (xx % 16) / 16.0))
    return T.grain(img, "l3ins", 0.04)


def turbine_side():
    img = metal("l3tu", "#4a5866", "#5e6e7c", sigma=2, rust=("#4a3a2a", "#6a4a30"), rust_amt=-0.3)
    img = T.shade(img, 0.75 + 0.45 * np.sin(np.pi * (yy + 0.5) / S))
    for y in (2, 29):
        for x in range(2, 32, 6):
            img[y, x] = T.hexc("#2a3038")
    img = T.rect(img, 0, 14, 32, 18, "#a83020")
    return T.grain(img, "l3tu", 0.03)


def turbine_end():
    img = metal("l3te", "#3a4652", "#4e5c6a", sigma=2)
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.shade(img, 1 - (np.abs(d - 12) < 1) * 0.4 - (np.abs(d - 5) < 1) * 0.4)
    img = T.blend(img, T.solid("#22282e"), (d < 3).astype(float))
    return T.grain(img, "l3te", 0.03)


def tube_on():
    img = T.solid("#2e3234")
    for x0 in (6, 20):
        img = T.rect(img, x0, 2, x0 + 6, 30, "#f2fff6")
        img = T.rect(img, x0 + 1, 2, x0 + 5, 30, "#ffffff")
    img = T.border(img, "#5a5e60", 1)
    return img


def tube_off():
    img = T.solid("#2e3234")
    for x0 in (6, 20):
        img = T.rect(img, x0, 2, x0 + 6, 30, "#7a807c")
        img = T.rect(img, x0, 2, x0 + 6, 5, "#3a3e3c")
    img = T.border(img, "#5a5e60", 1)
    return img


def _strand(rgb, a, x, y0, y1, seed, col, wobble=0.5):
    r = T.rng(seed)
    for y in range(y0, y1):
        x += (r.random() - 0.5) * wobble
        xi = int(round(x)) % S
        for dx, c in ((0, col), (1, "#3a3a3a")):
            rgb[y, (xi + dx) % S] = T.hexc(c)
            a[y, (xi + dx) % S] = 1
        rgb[y, (xi - 1) % S] = T.hexc("#0a0a0a")
        a[y, (xi - 1) % S] = 1
    return x


def wire():
    """Full-height hanging cables (stack these)."""
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for k, (x, col) in enumerate(((8, "#1a1a1a"), (16, "#5a1a12"), (23, "#1a1a1a"))):
        _strand(rgb, a, x, 0, S, f"l3ws{k}", col, wobble=0.0)
    return rgb, a


def wire_end():
    """The bottom of a hanging bundle: frayed copper, still live."""
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("l3we")
    for k, (x, col, ln) in enumerate(((8, "#1a1a1a", 20), (16, "#5a1a12", 26), (23, "#1a1a1a", 16))):
        xe = _strand(rgb, a, x, 0, ln, f"l3ws{k}", col, wobble=0.0)
        for dy in range(4):
            for dx in (-2, -1, 0, 1, 2):
                if r.random() < 0.55 - dy * 0.1:
                    yy_, xx_ = min(S - 1, ln + dy), (int(xe) + dx) % S
                    rgb[yy_, xx_] = T.hexc("#e08a3a") if r.random() < 0.6 else T.hexc("#fff4c8")
                    a[yy_, xx_] = 1
    return rgb, a


def wire_floor():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("l3wf")
    for k, col in enumerate(("#141414", "#6a1a12", "#202020", "#141414")):
        x, y = 4 + k * 7 + r.random() * 3, 31.0
        for _ in range(40):
            y -= 0.45
            x += (r.random() - 0.5) * 1.6
            if y < 12 + k * 3:
                break
            xi, yi = int(x) % S, int(np.clip(y, 0, S - 1))
            for dx in (0, 1):
                rgb[yi, (xi + dx) % S] = T.hexc(col)
                a[yi, (xi + dx) % S] = 1
        xi, yi = int(x) % S, int(np.clip(y, 0, S - 1))
        rgb[max(0, yi - 2):yi + 1, max(0, xi - 1):xi + 3] = T.hexc("#ffe8a0")
        a[max(0, yi - 2):yi + 1, max(0, xi - 1):xi + 3] = 1
    return rgb, a


def breaker():
    img = metal("l3brk", "#5a5e5a", "#6e726c", sigma=2)
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 6, 4, 26, 10, "#d8d0b8")
    img = centered(img, "MAIN", 5, "#a01810")
    img = T.rect(img, 13, 12, 19, 28, "#1a1a1a")
    img = centered(img, "ON", 12, "#30a040")
    img = centered(img, "OFF", 23, "#a03020")
    return T.grain(img, "l3brk", 0.03)


def lever():
    return T.grain(T.ramp(T.noise("l3lev", sigma=1), "#a01810", "#d02a1a"), "l3lev", 0.04)


def console():
    img = metal("l3con", "#4a4e4a", "#5a5e5a", sigma=2)
    r = T.rng("l3btn")
    for y in range(4, 28, 6):
        for x in range(3, 29, 5):
            c = ("#c02818", "#28a038", "#d8b020", "#2848c0", "#e8e4d4")[int(r.random() * 5)]
            img = T.rect(img, x, y, x + 3, y + 3, c)
    return T.grain(img, "l3con", 0.03)


def crt():
    img = T.solid("#06140a")
    t = np.arange(S)
    wave = (14 + 6 * np.sin(t / 3.0) * np.sin(t / 11.0)).astype(int)
    for x in range(2, 30):
        img[wave[x], x] = T.hexc("#60ff90")
        img[wave[x] + 1, x] = T.hexc("#208040")
    img = draw_text(img, "LOAD", 3, 3, "#60ff90")
    img = draw_text(img, "140%", 17, 3, "#ff5040")
    img = draw_text(img, "SURGE", 3, 24, "#ff5040")
    img = T.shade(img, 1 - ((yy % 2) == 0) * 0.25)
    return T.border(img, "#1a1c1a", 2)


def casing():
    return metal("l3case", "#2e302e", "#3e403c", sigma=2)


def elevator(upper):
    def f():
        img = metal("l3el" + str(upper), "#8a8e90", "#a2a6a8", sigma=0.6)
        img = T.shade(img, 1 + ((xx % 3) == 0) * 0.05)
        img = T.rect(img, 15, 0, 17, 32, "#3a3c3e")
        if upper:
            img = T.rect(img, 0, 0, 32, 9, "#2a2c2e")
            img = T.rect(img, 11, 1, 21, 8, "#0a0806")
            img = draw_text(img, "4", 15, 2, "#ff6a20")
            img = draw_text(img, "^", 11, 2, "#ff6a20")
        else:
            img = T.rect(img, 0, 30, 32, 32, "#2a2c2e")
        return T.border(img, "#5a5e60", 1, sides="we")
    return f


def call_panel():
    img = brick_paint()
    img = T.rect(img, 11, 8, 21, 26, "#8a8e90")
    d = np.sqrt((xx - 16) ** 2 + (yy - 14) ** 2)
    img = T.blend(img, T.solid("#ffb040"), (d < 2.5).astype(float))
    d = np.sqrt((xx - 16) ** 2 + (yy - 21) ** 2)
    img = T.blend(img, T.solid("#3a3a38"), (d < 2.5).astype(float))
    return img


def tray():
    img = metal("l3tray", "#6a6c6a", "#7e807c", sigma=1)
    for x0 in range(2, 30, 5):
        img = T.rect(img, x0, 0, x0 + 4, 32, ("#1a1a1a", "#3a1a12", "#1a1a2a", "#2a2a1a")[(x0 // 5) % 4])
    return T.grain(img, "l3tray", 0.03)


def spool_side():
    img = T.ramp(T.fbm("l3sp", octaves=3, base=3), "#7a5a34", "#94703e")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#3a2a1a"), (np.abs(d - 4) < 1).astype(float))
    return T.grain(img, "l3sp", 0.04)


def spool_cable():
    img = T.solid("#181818")
    img = T.shade(img, 1 + ((yy % 3) == 0) * 1.2)
    return T.grain(img, "l3spc", 0.05)


def bulkhead():
    d = np.sqrt((xx - 15.5) ** 2 / 1.0 + (yy - 15.5) ** 2 / 0.6)
    img = T.ramp(np.clip(1 - d / 22, 0, 1), "#c87a2a", "#ffcf80", "#fff4d8")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.5)
    return img


def stair_dark():
    return T.grain(T.ramp(T.noise("l3sd", sigma=3), "#000000", "#08090a"), "l3sd", 0.02)


def service_door():
    img = metal("l3sdr", "#5a4a2a", "#6e5c36", rust=("#3a2414", "#5a3418"), rust_amt=0.1)
    img = T.border(img, "#2a2418", 2)
    img = T.rect(img, 5, 5, 27, 13, "#e0c020")
    img = centered(img, "KEEP", 6, "#141414")
    img = T.rect(img, 22, 17, 26, 21, "#b0aca0")
    img = centered(img, "OUT", 24, "#e0c020")
    return T.grain(img, "l3sdr", 0.04)


tex("l3_brick", brick)
tex("l3_brick_paint", brick_paint)
tex("l3_brick_paint_low", brick_paint_low)
tex("l3_conduit", conduit)
tex("l3_danger", danger_sign)
tex("l3_ceiling", ceiling)
tex("l3_floor", floor)
tex("l3_plate", plate)
tex("l3_stripe", stripe)
tex("l3_cabinet", cabinet("l3cab"))
tex("l3_cabinet_top", cabinet("l3cabt", top=True))
tex("l3_cabinet_lid", lambda: metal("l3lid", "#7a7a70", "#8a8a80"))
tex("l3_transformer", transformer)
tex("l3_insulator", insulator)
tex("l3_turbine", turbine_side)
tex("l3_turbine_end", turbine_end)
tex("l3_tube", tube_on)
tex("l3_tube_off", tube_off)
tex("l3_wire", wire)
tex("l3_wire_floor", wire_floor)
tex("l3_wire_end", wire_end)
tex("l3_breaker", breaker)
tex("l3_lever", lever)
tex("l3_console", console)
tex("l3_crt", crt)
tex("l3_casing", casing)
tex("l3_elevator_low", elevator(False))
tex("l3_elevator_up", elevator(True))
tex("l3_call_panel", call_panel)
tex("l3_tray", tray)
tex("l3_spool_side", spool_side)
tex("l3_spool_cable", spool_cable)
tex("l3_bulkhead", bulkhead)
tex("l3_stair_dark", stair_dark)
tex("l3_service_door", service_door)

# ------------------------------------------------------------------ blocks

cube("station_brick", "l3_brick", display="Station Brick")
cube("station_brick_painted", "l3_brick_paint", display="Painted Station Brick")
cube("station_brick_painted_low", "l3_brick_paint_low", display="Scuffed Station Brick")
cube("station_conduit_wall", "l3_conduit", display="Conduit Wall")
cube("station_danger_sign", "l3_danger", display="High Voltage Sign")
cube("station_ceiling", "l3_ceiling", display="Station Ceiling", flat=True)
cube("station_floor", "l3_floor", display="Station Floor")
cube("station_floor_plate", "l3_plate", display="Steel Floor Plate", sound="metal")
cube("station_floor_stripe", "l3_stripe", display="Hazard Stripe Floor")
cube("call_panel_wall", "l3_call_panel", display="Elevator Call Panel")
cube_faces("switchgear_cabinet", top="l3_cabinet_lid", bottom="l3_cabinet_lid", side="l3_cabinet", sound="metal",
           display="Switchgear Cabinet")
cube_faces("switchgear_cabinet_top", top="l3_cabinet_lid", bottom="l3_cabinet_lid", side="l3_cabinet_top", sound="metal",
           display="Switchgear Meters")
pillar("turbine_casing", "l3_turbine_end", "l3_turbine", display="Turbine Casing", sound="metal")
shaped("station_transformer", [
    (1, 0, 1, 15, 16, 15, "body"),
    (0, 2, 3, 1, 14, 13, "body"), (15, 2, 3, 16, 14, 13, "body"),
    (3, 2, 0, 13, 14, 1, "body"), (3, 2, 15, 13, 14, 16, "body"),
], {"body": "l3_transformer"}, display="Transformer", sound="metal", unbreakable=True)
shaped("station_transformer_top", [
    (1, 0, 1, 15, 2, 15, "lid"),
    (7, 2, 7, 9, 13, 9, "ins"), (6, 4, 6, 10, 5, 10, "ins"), (6, 7, 6, 10, 8, 10, "ins"), (6, 10, 6, 10, 11, 10, "ins"),
    (7.5, 13, 7.5, 8.5, 16, 8.5, "lid"),
], {"lid": "l3_cabinet_lid", "ins": "l3_insulator"}, display="Transformer Bushing", sound="metal", unbreakable=True)
flicker_light("station_tube_light", "l3_tube", "l3_tube_off", "l3_ceiling", light=14, flicker=0.05,
              display="Tube Light")
flicker_light("station_tube_light_bad", "l3_tube", "l3_tube_off", "l3_ceiling", light=14, flicker=0.7,
              display="Failing Tube Light")
cube_faces("station_tube_light_dead", bottom="l3_tube_off", top="l3_ceiling", side="l3_ceiling", flat=True,
           display="Dead Tube Light", sound="glass")
shaped("cable_tray", [(0, 12, 3, 16, 13, 13, {"all": "tray", "up": "tray"}), (0, 13, 3, 16, 14.5, 4, "case"),
                      (0, 13, 12, 16, 14.5, 13, "case"), (7, 13, 4, 9, 16, 12, "case")],
       {"tray": "l3_tray", "case": "l3_casing"}, display="Cable Tray", sound="metal", unbreakable=True, collides=False)
shaped("cable_spool", [(1, 0, 2, 15, 14, 4, "side"), (1, 0, 12, 15, 14, 14, "side"), (3, 2, 4, 13, 12, 12, "cable")],
       {"side": "l3_spool_side", "cable": "l3_spool_cable"}, display="Cable Spool", sound="wood")
shaped("bulkhead_lamp", [(4, 4, 0, 12, 12, 1, "case"), (5, 5, 1, 11, 11, 4, "glass")],
       {"case": "l3_casing", "glass": "l3_bulkhead"}, display="Bulkhead Lamp", light=12, emissive=True,
       collides=False, sound="glass", unbreakable=True)
shaped("control_console", [(0, 0, 2, 16, 10, 16, {"all": "case", "up": "panel"}),
                           (0, 10, 2, 16, 12, 8, {"all": "case", "up": "panel"})],
       {"case": "l3_casing", "panel": "l3_console"}, display="Control Console", sound="metal", unbreakable=True)
shaped("control_crt", [(2, 0, 4, 14, 11, 14, {"all": "case", "north": "screen"}), (4, 2, 14, 12, 9, 16, "case")],
       {"case": "l3_casing", "screen": "l3_crt"}, display="Control Monitor", light=4, emissive=True,
       sound="glass", unbreakable=True)

WIRE_MODEL = {"parent": "minecraft:block/cross", "textures": {"cross": "noclip:block/l3_wire"}}
custom("live_wire", "com.noclip.block.L3LiveWireBlock", WIRE_MODEL, display="Live Wire", sound="chain",
       unbreakable=True, render="cutout", no_collision=True,
       item_model={"parent": "minecraft:item/generated", "textures": {"layer0": "noclip:block/l3_wire"}})
custom("live_wire_end", "com.noclip.block.L3LiveWireBlock",
       {"parent": "minecraft:block/cross", "textures": {"cross": "noclip:block/l3_wire_end"}},
       display="Frayed Live Wire", sound="chain", unbreakable=True, render="cutout", no_collision=True,
       item_model={"parent": "minecraft:item/generated", "textures": {"layer0": "noclip:block/l3_wire_end"}})
custom("live_wire_floor", "com.noclip.block.L3LiveWireBlock",
       {"parent": "minecraft:block/cross", "textures": {"cross": "noclip:block/l3_wire_floor"}},
       display="Sparking Cable", sound="chain", unbreakable=True, render="cutout", no_collision=True,
       item_model={"parent": "minecraft:item/generated", "textures": {"layer0": "noclip:block/l3_wire_floor"}})

BREAKER = [(2, 1, 0, 14, 15, 3, {"all": "case", "south": "panel"}), (7, 6, 3, 9, 10, 5, "case"),
           (6.5, 9, 4, 9.5, 14, 6, "lever")]
custom("main_breaker", "com.noclip.block.L3BreakerBlock",
       box_model(BREAKER, {"case": "l3_casing", "panel": "l3_breaker", "lever": "l3_lever"}),
       blockstate=facing_states("main_breaker"), display="Main Breaker", sound="metal", unbreakable=True,
       render="cutout")

exit_block("elevator_door_low", "l3_elevator_low", "ASCEND", trigger="USE", solid=True, display="Elevator Door",
           sound="metal")
exit_block("elevator_door_up", "l3_elevator_up", "ASCEND", trigger="USE", solid=True, display="Elevator Door",
           sound="metal")
exit_block("station_stair_dark", "l3_stair_dark", "DESCEND", display="Stairs Into Dark")
exit_block("station_service_door", "l3_service_door", "DOOR", trigger="USE", solid=True, display="Keep Out Door",
           sound="metal")

# ------------------------------------------------------------------ sound


def mains(d, base, seed, gain=1.0):
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    r = Y.rng(seed)
    for h in (1, 2, 3, 4, 5, 6, 7, 9, 11):
        x += np.sin(2 * np.pi * base * h * t + r.random() * 6) / h ** 0.8
    return x * gain


def station_loop():
    d = 24.0
    t = Y.t_axis(d)
    hum = mains(d, 60.0, "l3m", 0.6)
    hum *= 0.8 + 0.2 * np.sin(2 * np.pi * t * 4 / d)
    # a pump thudding at a steady 1.5 Hz somewhere in the walls
    pump = np.zeros_like(t)
    for k in range(int(d * 1.5)):
        n = int(0.18 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(pump, np.sin(2 * np.pi * 52 * tt) * np.exp(-tt * 18), k / 1.5, 0.9 if k % 2 == 0 else 0.6)
    whirr = Y.bp(Y.pink(d, "l3wh"), 600, 2400) * 0.15 * (0.7 + 0.3 * np.sin(2 * np.pi * t * 6 / d))
    buzz = Y.hp(np.sign(np.sin(2 * np.pi * 120 * t)) * 0.05, 1500) * (0.5 + 0.5 * np.sin(2 * np.pi * t * 3 / d + 2))
    rumble = Y.lp(Y.brown(d, "l3rum"), 80) * 0.6
    x = Y.lp(hum, 3000) + Y.lp(pump, 300) * 0.5 + whirr + buzz + rumble
    x = Y.loopable(Y.norm(x, 0.6), 1.0)
    return Y.stereoize(x, 0.0009), "stereo"


def arc(seed, dur=0.9, gain=0.9):
    t = Y.t_axis(dur)
    r = Y.rng(seed)
    crack = (r.random(len(t)) > 0.985).astype(float) * Y.white(dur, seed + "w")
    crack *= np.exp(-t * 3)
    buzz = np.sign(np.sin(2 * np.pi * (120 + 40 * r.random()) * t)) * np.exp(-t * 5) * 0.4
    x = Y.hp(crack, 1500) * 2 + Y.bp(buzz, 200, 4000)
    return Y.norm(x, gain)


def relay(i):
    def f():
        out = np.zeros(int(1.5 * Y.SR))
        for k in range(2 + i):
            n = int(0.05 * Y.SR)
            tt = np.arange(n) / Y.SR
            click = Y.bp(Y.white(0.05, f"l3r{i}{k}"), 1000, 8000)[:n] * np.exp(-tt * 120)
            thunk = np.sin(2 * np.pi * 180 * tt) * np.exp(-tt * 60)
            Y.place(out, click + thunk * 0.5, 0.05 + k * 0.17, 0.8)
        return Y.reverb(out, 1.8, 0.5, f"l3rv{i}")
    return f


def add_arc(i):
    def f():
        return Y.reverb(arc(f"l3a{i}", 1.2), 1.6, 0.4, f"l3av{i}")
    return f


def spin_up():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 30 + 220 * (t / d) ** 1.5
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + 0.4 * np.sin(4 * np.pi * np.cumsum(f0) / Y.SR)
    x *= np.sin(np.pi * t / d) ** 0.5
    return Y.norm(Y.reverb(x, 2.5, 0.5, "l3su"), 0.6)


def slam():
    out = np.zeros(int(4 * Y.SR))
    Y.place(out, Y.impact("l3slam", 0.9, ring=0.5), 0.05)
    return Y.reverb(Y.lp(out, 2500), 3.0, 0.65, "l3slam")


def whine():
    """Surge warning: a rising electrical whine with crackle, three seconds."""
    d = 3.2
    t = Y.t_axis(d)
    f0 = 200 + 2600 * (t / d) ** 2
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (t / d) ** 0.8
    x += mains(d, 60.0, "l3wm", 0.3) * (t / d)
    x += arc("l3wa", d, 0.5) * (t / d) * 0.6
    return Y.norm(x, 0.85)


def surge():
    d = 2.5
    t = Y.t_axis(d)
    x = arc("l3sa", d, 1.0) * 0.9
    boom = np.sin(2 * np.pi * np.cumsum(90 * np.exp(-t * 2) + 30) / Y.SR) * np.exp(-t * 2.2)
    x += boom * 0.9
    return Y.norm(Y.reverb(x, 2.0, 0.35, "l3sv"), 0.95)


def breaker_sound():
    d = 3.5
    t = Y.t_axis(d)
    clunk = np.zeros_like(t)
    Y.place(clunk, Y.impact("l3bk", 0.5, ring=0.3), 0.0)
    down = np.sin(2 * np.pi * np.cumsum(60 * np.exp(-t * 0.8) + 5) / Y.SR) * np.exp(-t * 0.9) * 0.6
    return Y.norm(clunk + down, 0.9)


def zap():
    return Y.reverb(arc("l3zap", 0.5, 0.9), 0.8, 0.2, "l3zap")


sound("ambient.level_3", [("ambient/level_3", station_loop)], stream=True)
sound("ambient.level_3.additions",
      [(f"ambient/level_3_relay{i}", relay(i)) for i in range(2)] +
      [(f"ambient/level_3_arc{i}", add_arc(i)) for i in range(2)] +
      [("ambient/level_3_spinup", spin_up), ("ambient/level_3_slam", slam)],
      subtitle="Machinery clatters")
sound("event.level_3_whine", [("event/level_3_whine", whine)], subtitle="The current rises")
sound("event.level_3_surge", [("event/level_3_surge", surge)], subtitle="Power surges")
sound("event.level_3_breaker", [("event/level_3_breaker", breaker_sound)], subtitle="The breaker clunks off")
sound("event.level_3_zap", [("event/level_3_zap", zap)], subtitle="A wire sparks")

level("level_3", fog="#1c221e", sky="#1c221e", ambient="ambient.level_3", additions="ambient.level_3.additions",
      additions_chance=0.008, fixed_time=18000, height=32)
