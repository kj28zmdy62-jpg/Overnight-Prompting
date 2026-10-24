"""Cold Storage - freezer rooms behind plastic strip curtains. The cold gets into you.
Rows of wrapped shapes hang from the rails. One of them is not cargo."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound, custom, lang, \
    entity_model, entity_tex, cross, ITEM_MODELS, part
from entity_paint import Canvas
from content.ind_kit import text, vstreaks, glow_spot, steel, rivets
import tex as T
import synth as Y

S = 32


def frost_mask(seed, coverage=0.4, sigma=1.0):
    n = T.fbm(seed, octaves=4, base=3)
    fine = T.noise(seed + "f", sigma=0.4)
    return np.clip((n * 0.7 + fine * 0.3) * 2.2 - (2.2 - coverage * 2.4), 0, 1)


def frost(img, seed, coverage=0.4, bottom=False):
    m = frost_mask(seed, coverage)
    if bottom:
        yy = np.arange(S)[:, None] * np.ones((1, S))
        m = np.clip(m + np.clip((yy - 18) / 14.0, 0, 1) * 0.7 * T.noise(seed + "b", sigma=1), 0, 1)
    crystals = T.ramp(T.noise(seed + "c", sigma=0.5), "#dfeaf2", "#ffffff")
    return T.blend(img, crystals, m * 0.85)


def panel(seed="csp"):
    """Insulated sandwich panel: off-white steel skin with shallow ribs and a seam."""
    yy, xx = np.mgrid[0:S, 0:S]
    base = T.ramp(T.noise(seed, sigma=2.5) * 0.6 + T.fbm(seed + "b", octaves=3, base=4) * 0.4, "#c9cfd2", "#e2e6e8")
    rib = ((xx % 8) == 3).astype(float) * 0.06 - ((xx % 8) == 4).astype(float) * 0.08
    img = T.shade(base, 1 + rib)
    seam = (xx == 0) | (xx == 31)
    img = T.blend(img, T.solid("#8d9599"), seam.astype(float) * 0.8)
    img = vstreaks(img, seed + "v", "#a3a69a", count=3, strength=0.18, length=(6, 18))
    return T.grain(T.stains(img, seed + "s", "#9aa0a0", 1, 0.25, 7), seed, 0.025)


def panel_frost():
    return frost(panel("cspf"), "cspfr", 0.55, bottom=True)


def panel_low():
    """Bottom row: a scuffed stainless kick plate and frost creeping up from the floor."""
    img = panel("cspl")
    yy, xx = np.mgrid[0:S, 0:S]
    kick = steel("cspk", "#7e868c", "#a2aab0")
    img = np.where((yy > 21)[..., None], kick, img)
    img[21] = T.hexc("#5d6569")
    img = rivets(img, [(4, 26), (14, 26), (24, 26)])
    return frost(img, "csplf", 0.25, bottom=True)


def panel_sign(lines, color="#1e4b8a", bg="#e8eef2"):
    def f():
        img = panel("css" + lines[0])
        img = T.rect(img, 3, 7, 29, 25, bg)
        img = T.rect(img, 3, 7, 29, 9, color)
        y = 11
        for ln in lines:
            w = len(ln) * 4 - 1
            img = text(img, ln, 16 - w // 2, y, color)
            y += 7
        img = T.border(img, "#8d9599", 1, "ew")
        return frost(img, "cssf" + lines[0], 0.18)
    return f


def floor(seed="csf"):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "n", sigma=0.6) * 0.3, "#7d8689", "#959ea1")
    img = T.cracks(img, seed + "c", "#5e6669", 1, 0.4)
    img = T.stains(img, seed + "s", "#5f6a6e", 2, 0.25, 8)
    return frost(T.grain(img, seed, 0.03), seed + "fr", 0.22)


def floor_drain():
    img = floor("csfd")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#3d4447"), (d < 7).astype(float))
    slots = (d < 6) & ((xx % 3) == 0)
    img = T.blend(img, T.solid("#0a0c0d"), slots.astype(float))
    ring = np.abs(d - 7) < 0.8
    return T.blend(img, T.solid("#aab3b7"), ring.astype(float))


def floor_steps():
    """Frozen boot prints that lead into the wall."""
    img = floor("csfs")
    yy, xx = np.mgrid[0:S, 0:S]
    m = np.zeros((S, S))
    for (cx, cy) in ((10, 4), (21, 12), (10, 20), (21, 28)):
        sole = ((xx - cx) / 2.6) ** 2 + ((yy - cy) / 4.2) ** 2 < 1
        m = np.maximum(m, sole.astype(float))
    return T.blend(img, T.solid("#5e686c"), m * 0.7)


def ice():
    n = T.fbm("csice", octaves=4, base=4)
    img = T.ramp(n, "#9fc3d6", "#c8e2ee", "#e8f5fa")
    img = T.cracks(img, "csicec", "#f8ffff", 3, 0.7)
    img = T.cracks(img, "csiced", "#6f97ab", 2, 0.4)
    bub = (T.rng("csiceb").random((S, S)) > 0.97).astype(float)
    return T.grain(T.shade(img, 1 + bub * 0.12), "csice", 0.02)


def ice_hole():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("cshole", sigma=2), "#020406", "#0a1014")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) + T.noise("csholee", sigma=1.5) * 4
    rim = ice()
    return T.blend(img, rim, np.clip((d - 13) / 2, 0, 1))


def ceiling():
    img = panel("csc")
    return T.shade(img, 0.92)


def lamp(on=True):
    img = T.ramp(T.noise("csl" + str(on), sigma=2), "#e8f4ff", "#ffffff") if on else \
        T.ramp(T.noise("csloff", sigma=2), "#7f8a91", "#8f9aa1")
    yy, xx = np.mgrid[0:S, 0:S]
    prism = ((xx + yy) % 4 == 0).astype(float) * 0.05
    img = T.shade(img, 1 - prism)
    img = T.border(img, "#9ba5aa", 4)
    img = T.border(img, "#6d777c", 2)
    if not on:
        img = frost(img, "cslf", 0.35)
    return img


def curtain():
    """PVC strip curtain: overlapping translucent strips, scuffed and frosted at the bottom."""
    yy, xx = np.mgrid[0:S, 0:S]
    strip = xx % 8
    rgb = T.ramp(T.noise("cscur", sigma=1.5), "#b9d0d8", "#dceaf0")
    edge = (strip == 0) | (strip == 7)
    rgb = T.blend(rgb, T.solid("#e9f4f8"), edge.astype(float) * 0.6)
    rgb = vstreaks(rgb, "cscurv", "#8a9aa0", count=5, strength=0.3, length=(8, 24))
    a = np.full((S, S), 0.42)
    a[edge] = 0.62
    gap = (strip == 7) & (yy > 20)
    a[gap] = 0.0
    frostb = np.clip((yy - 22) / 10.0, 0, 1) * T.noise("cscurf", sigma=1)
    rgb = T.blend(rgb, T.solid("#ffffff"), frostb * 0.7)
    a = np.clip(a + frostb * 0.3, 0, 1)
    return rgb, a


def freezer_door():
    img = steel("csdoor", "#8b9499", "#adb5b9")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.border(img, "#555e63", 2)
    # round porthole, iced over
    d = np.sqrt((xx - 16) ** 2 + (yy - 9) ** 2)
    img = T.blend(img, T.ramp(T.noise("csph", sigma=1), "#c8dde6", "#eef7fa"), (d < 5).astype(float))
    img = T.blend(img, T.solid("#3b4246"), (np.abs(d - 5.5) < 0.8).astype(float))
    img = T.rect(img, 24, 15, 29, 25, "#30363a")
    img = T.rect(img, 25, 16, 28, 24, "#c7cdd0")
    img = text(img, "DOCK", 7, 20, "#1e4b8a")
    img = T.rect(img, 4, 27, 28, 29, "#e0b020")
    return frost(img, "csdoorf", 0.3, bottom=True)


def exit_light():
    img = T.ramp(T.noise("csel", sigma=1), "#40ff70", "#a0ffb8")
    return T.border(img, "#2b3236", 3)


def bundle_tex(seed, lo="#c9d2d0", hi="#e4ebe8"):
    """Cloudy plastic wrap over something dark."""
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        wrap = T.ramp(T.noise(seed, sigma=1.2) * 0.5 + T.fbm(seed + "b", octaves=3, base=3) * 0.5, lo, hi)
        folds = np.sin((yy + xx * 0.4 + T.noise(seed + "w", sigma=2) * 8) / 2.2) * 0.07
        wrap = T.shade(wrap, 1 + folds)
        under = np.exp(-((xx - 16) ** 2) / 60.0) * (0.35 + 0.2 * T.noise(seed + "u", sigma=3))
        wrap = T.blend(wrap, T.solid("#4a3b38"), under)
        tape = (np.abs((yy % 11) - 5) < 1).astype(float) * 0.6
        wrap = T.blend(wrap, T.solid("#d8cfa8"), tape)
        return frost(T.grain(wrap, seed, 0.03), seed + "fr", 0.15)
    return f


def rail_metal():
    return T.grain(T.ramp(T.noise("csrail", sigma=1), "#6c7377", "#8e969a"), "csrail", 0.04)


def boxes():
    img = T.ramp(T.noise("csbox", sigma=1.5), "#a8875a", "#bc9a66")
    img = T.rect(img, 0, 14, 32, 18, "#d8c49a")
    img = text(img, "FROZEN", 4, 5, "#2a4a8a")
    return frost(T.grain(img, "csbox", 0.04), "csboxf", 0.35)


def pallet():
    img = T.ramp(T.noise("cspal", sigma=1), "#8a7350", "#9e8660")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.3)
    return frost(img, "cspalf", 0.25)


def wire():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("cswire", sigma=1), "#b8c0c6", "#e8eef2")
    a = (((xx % 4) == 0) | ((yy % 16) == 0)).astype(float)
    return frost(rgb, "cswiref", 0.3), a


def wire_post():
    return frost(T.ramp(T.noise("cswp", sigma=1), "#a9b2b8", "#dfe6ea"), "cswpf", 0.2)


def heater_body():
    img = T.ramp(T.noise("csheat", sigma=1), "#3a3633", "#4a4542")
    return T.border(img, "#2a2725", 1)


def heater_coils():
    yy, xx = np.mgrid[0:S, 0:S]
    glow = T.ramp(T.noise("cscoil", sigma=1), "#ff6a10", "#ffb050")
    coil = (np.abs((yy % 6) - 3) < 1.3)
    img = np.where(coil[..., None], glow, T.solid("#5a1a06"))
    return T.grain(img, "cscoil", 0.04)


def office_wall():
    img = T.ramp(T.fbm("csow", octaves=3, base=4), "#b8a888", "#c8b898")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = T.shade(img, 1 - (yy > 28) * 0.25)
    return T.grain(T.stains(img, "csows", "#8a7a5a", 1, 0.25, 8), "csow", 0.03)


def office_calendar():
    img = office_wall()
    img = T.rect(img, 8, 4, 24, 26, "#f0ece0")
    img = T.rect(img, 8, 4, 24, 12, "#4a7ab0")
    yy, xx = np.mgrid[0:S, 0:S]
    grid = ((xx - 8) % 3 == 0) & (yy > 13) & (yy < 25) & (xx > 8) & (xx < 24)
    img = T.blend(img, T.solid("#9a968a"), grid.astype(float))
    # every day crossed out
    for k in range(5):
        img = T.rect(img, 9 + k * 3, 14 + (k % 3) * 3, 11 + k * 3, 16 + (k % 3) * 3, "#b02020")
    return img


def office_floor():
    yy, xx = np.mgrid[0:S, 0:S]
    a = T.ramp(T.noise("csof", sigma=1), "#8a7a62", "#97876e")
    b = T.ramp(T.noise("csof2", sigma=1), "#6e6250", "#7a6c58")
    chk = (((xx // 16) + (yy // 16)) % 2 == 0)
    img = np.where(chk[..., None], a, b)
    return T.grain(T.border(img, "#5a5040", 1), "csof", 0.03)


def ice_figure():
    n = T.fbm("csif", octaves=3, base=3)
    rgb = T.ramp(n, "#a8cfe0", "#e2f2f8")
    rgb = T.cracks(rgb, "csifc", "#ffffff", 2, 0.6)
    under = np.exp(-((np.mgrid[0:S, 0:S][1] - 16) ** 2) / 50.0) * 0.4
    rgb = T.blend(rgb, T.solid("#5a6a78"), under)
    return rgb, np.full((S, S), 0.82)


def icicles():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("csicl", sigma=1), "#cfe6f0", "#ffffff")
    a = np.zeros((S, S))
    r = T.rng("csiclr")
    for _ in range(9):
        x = int(r.random() * 30) + 1
        ln = 6 + int(r.random() * 20)
        for y in range(ln):
            wd = max(0, int(2 * (1 - y / ln)))
            a[y, max(0, x - wd):x + wd + 1] = 0.9
    return rgb, a


tex("cs_panel", lambda: panel())
tex("cs_panel_frost", panel_frost)
tex("cs_panel_low", panel_low)
tex("cs_sign_temp", panel_sign(["FREEZER", "-25 C"]))
tex("cs_sign_ppe", panel_sign(["PPE", "REQUIRED"], "#a01818", "#f2eadc"))
tex("cs_sign_alone", panel_sign(["NEVER", "WORK", "ALONE"], "#a01818", "#f2eadc"))
tex("cs_sign_count", panel_sign(["COUNT", "THEM"], "#1e4b8a"))
tex("cs_floor", lambda: floor())
tex("cs_floor_drain", floor_drain)
tex("cs_floor_steps", floor_steps)
tex("cs_ice", ice)
tex("cs_ice_hole", ice_hole)
tex("cs_ceiling", ceiling)
tex("cs_lamp", lambda: lamp(True))
tex("cs_lamp_off", lambda: lamp(False))
tex("cs_curtain", curtain)
tex("cs_freezer_door", freezer_door)
tex("cs_exit_light", exit_light)
tex("cs_bundle", bundle_tex("csb"))
tex("cs_bundle_b", bundle_tex("csb2", "#b8c4c4", "#d6dfdd"))
tex("cs_rail", rail_metal)
tex("cs_boxes", boxes)
tex("cs_pallet", pallet)
tex("cs_wire", wire)
tex("cs_wire_post", wire_post)
tex("cs_heater_body", heater_body)
tex("cs_heater_coils", heater_coils)
tex("cs_office_wall", office_wall)
tex("cs_office_calendar", office_calendar)
tex("cs_office_floor", office_floor)
tex("cs_ice_figure", ice_figure)
tex("cs_icicles", icicles)

cube("cs_panel_wall", "cs_panel", display="Insulated Panel", sound="metal")
cube("cs_panel_frosted", "cs_panel_frost", display="Frosted Panel", sound="metal")
cube("cs_panel_base", "cs_panel_low", display="Insulated Panel Kickplate", sound="metal")
cube("cs_sign_temp", "cs_sign_temp", display="Freezer Sign", sound="metal")
cube("cs_sign_ppe", "cs_sign_ppe", display="Safety Sign", sound="metal")
cube("cs_sign_alone", "cs_sign_alone", display="Safety Sign", sound="metal")
cube("cs_sign_count", "cs_sign_count", display="Handwritten Sign", sound="metal")
cube("cs_floor", "cs_floor", display="Frosted Epoxy Floor")
cube("cs_floor_drain", "cs_floor_drain", display="Floor Drain")
cube("cs_floor_steps", "cs_floor_steps", display="Frozen Bootprints")
custom("cs_ice_floor", "com.noclip.block.SlickIceBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/cs_ice"}},
       display="Floor Ice", sound="glass", unbreakable=True)
exit_block("cs_ice_hole", "cs_ice_hole", "WATER", display="Hole in the Ice", sound="glass")
cube("cs_ceiling", "cs_ceiling", display="Freezer Ceiling", sound="metal", flat=True)
flicker_light("cs_lamp", "cs_lamp", "cs_lamp_off", "cs_ceiling", light=14, flicker=0.05, display="Vapor-Proof Lamp")
flicker_light("cs_lamp_bad", "cs_lamp", "cs_lamp_off", "cs_ceiling", light=14, flicker=0.7, display="Failing Lamp")
cube_faces("cs_lamp_dead", bottom="cs_lamp_off", top="cs_ceiling", side="cs_ceiling", display="Dead Lamp",
           sound="glass", flat=True)
shaped("cs_curtain", [(0, 0, 7.5, 16, 16, 8.5, "c")], {"c": "cs_curtain"}, display="Strip Curtain",
       collides=False, render="translucent", unbreakable=True, sound="wool")
exit_block("cs_freezer_door", "cs_freezer_door", "DOOR", trigger="USE", solid=True, display="Loading Dock Door",
           sound="metal")
shaped("cs_exit_light", [(5, 4, 0, 11, 10, 3, {"all": "frame", "south": "lamp"})],
       {"lamp": "cs_exit_light", "frame": "cs_rail"}, display="Door Light", light=9, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")

# the meat rail and what hangs from it: rail (y=CEIL-1), chain, bundle top, bundle bottom
shaped("cs_rail", [(7, 13, 0, 9, 15, 16, "m"), (6, 15, 0, 10, 16, 16, "m")], {"m": "cs_rail"},
       display="Overhead Rail", collides=False, unbreakable=True, sound="metal")
shaped("cs_hook_chain", [(7, 13, 0, 9, 15, 16, "m"), (6, 15, 0, 10, 16, 16, "m"), (7.5, 0, 7.5, 8.5, 13, 8.5, "m")],
       {"m": "cs_rail"}, display="Hook Chain", collides=False, unbreakable=True, sound="chain")
shaped("cs_hook", [(7, 13, 0, 9, 15, 16, "m"), (6, 15, 0, 10, 16, 16, "m"), (7.5, 5, 7.5, 8.5, 13, 8.5, "m"),
                   (6.5, 3, 7.5, 9.5, 5, 8.5, "m"), (6.5, 3, 7.5, 7.5, 7, 8.5, "m")], {"m": "cs_rail"},
       display="Empty Hook", collides=False, unbreakable=True, sound="metal")
shaped("cs_bundle_top", [(7.5, 12, 7.5, 8.5, 16, 8.5, "m"), (6.5, 9, 6.5, 9.5, 12, 9.5, "w"),
                         (5, 6, 5.5, 11, 9, 10.5, "w"), (4.5, 0, 5, 11.5, 6, 11, "w")],
       {"m": "cs_rail", "w": "cs_bundle"}, display="Wrapped Bundle", unbreakable=True, sound="wool")
shaped("cs_bundle_bottom", [(4.5, 3, 5, 11.5, 16, 11, "w"), (5.5, 0, 6, 10.5, 3, 10, "w")],
       {"w": "cs_bundle"}, display="Wrapped Bundle", unbreakable=True, sound="wool")
shaped("cs_bundle_bottom_b", [(5, 5, 4.5, 11, 16, 11.5, "w"), (6, 1, 5.5, 10, 5, 10.5, "w")],
       {"w": "cs_bundle_b"}, display="Wrapped Bundle", unbreakable=True, sound="wool")

shaped("cs_wire_shelf", [
    (0, 0, 0, 1, 16, 1, "p"), (15, 0, 0, 16, 16, 1, "p"), (0, 0, 15, 1, 16, 16, "p"), (15, 0, 15, 16, 16, 16, "p"),
    (0, 1, 0, 16, 1.5, 16, "w"), (0, 8, 0, 16, 8.5, 16, "w"), (0, 15, 0, 16, 15.5, 16, "w"),
    (2, 1.5, 2, 9, 7, 13, "b"), (9.5, 1.5, 4, 14.5, 5, 12, "b"), (3, 8.5, 3, 13, 13, 12, "b"),
], {"p": "cs_wire_post", "w": "cs_wire", "b": "cs_boxes"}, display="Wire Shelving", sound="metal", unbreakable=True)
shaped("cs_pallet_boxes", [(0, 0, 0, 16, 2, 16, "p"), (1, 2, 1, 15, 10, 15, "b"), (2, 10, 2, 9, 15, 9, "b")],
       {"p": "cs_pallet", "b": "cs_boxes"}, display="Frozen Stock", unbreakable=True, sound="wool")
shaped("cs_heater", [(2, 0, 4, 14, 14, 12, {"all": "b", "north": "c"}), (1, 0, 3, 15, 1, 13, "b")],
       {"b": "cs_heater_body", "c": "cs_heater_coils"}, display="Space Heater", sound="metal", unbreakable=True,
       light=13, emissive=True)
cube("cs_office_wall", "cs_office_wall", display="Break Room Wall", sound="wool")
cube("cs_office_calendar", "cs_office_calendar", display="Calendar", sound="wool")
cube("cs_office_floor", "cs_office_floor", display="Break Room Floor")
shaped("cs_ice_figure", [(4, 0, 5, 12, 12, 11, "i"), (5, 12, 6, 11, 16, 10, "i")], {"i": "cs_ice_figure"},
       display="Frozen Figure", render="translucent", unbreakable=True, sound="glass")
shaped("cs_ice_figure_top", [(5, 0, 6, 11, 4, 10, "i"), (5.5, 4, 5.5, 10.5, 9, 10.5, "i")], {"i": "cs_ice_figure"},
       display="Frozen Figure", render="translucent", unbreakable=True, sound="glass")
cross("cs_icicles", "cs_icicles", display="Icicles", sound="glass")

# ------------------------------------------------------------------ entity: the wrapped one


def wrapped_parts():
    # one long wrapped body; the arms are bound against it
    return [
        part("body", (0, 0, 0), cubes=[(0, 0, -4, -6, -3, 8, 28, 6)]),
        part("head", (0, -6, 0), cubes=[(28, 0, -3, -6, -3, 6, 6, 6)]),
        part("right_leg", (-1.5, 22, 0), cubes=[(0, 34, -2, 0, -2, 3, 2, 4)]),
        part("left_leg", (1.5, 22, 0), cubes=[(14, 34, -1, 0, -2, 3, 2, 4)]),
    ]


def wrapped_tex():
    c = Canvas(64, 64)
    painter = bundle_tex("cswr", "#9aa4a2", "#b8c2c0")
    big = painter()

    def wrap(w, h):
        img = np.tile(big, (h // S + 1, w // S + 1, 1))[:h, :w]
        return img

    def face(w, h):
        img = wrap(w, h)
        # the plastic pressed into a face from the inside
        img[2, 1] = img[2, 4] = T.hexc("#6a5a56")
        img[4, 2:4] = T.hexc("#5a4a46")
        return img

    c.box(0, 0, 8, 28, 6, all=wrap)
    c.box(28, 0, 6, 6, 6, all=wrap, front=face)
    c.box(0, 34, 3, 2, 4, all=wrap)
    c.box(14, 34, 3, 2, 4, all=wrap)
    return c.out()


entity_model("wrapped_one", wrapped_parts())
entity_tex("wrapped_one", wrapped_tex)
lang("entity.noclip.wrapped_one", "The Wrapped")
lang("item.noclip.wrapped_one_spawn_egg", "The Wrapped Spawn Egg")
ITEM_MODELS["wrapped_one_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def compressor():
    d = 24.0
    t = Y.t_axis(d)
    # big reciprocating compressor: a 29.75 Hz thrum with knocking harmonics
    thrum = Y.hum(d, 29.75, "cscomp", buzz=0.0, harmonics=(1, 2, 3, 4, 6)) * 0.5
    knock = ((t * 29.75) % 1.0 < 0.08).astype(float)
    knock = Y.lp(knock * Y.white(d, "csk"), 600) * 0.4
    fans = Y.bp(Y.pink(d, "csfans"), 200, 2500) * 0.25 * (0.85 + 0.15 * np.sin(2 * np.pi * t / 8))
    hiss = Y.bp(Y.white(d, "cshiss"), 4000, 12000) * 0.025
    # the cold itself: a thin high whine
    whine = np.sin(2 * np.pi * (round(3520 * d) / d) * t) * 0.006 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 12))
    x = Y.lp(thrum + knock, 500) + fans + hiss + whine
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 1.5), 0.0011), "stereo"


def ice_crack(i):
    def f():
        d = 1.8
        out = np.zeros(int(d * Y.SR))
        r = Y.rng(f"csic{i}")
        for k in range(5 + i * 3):
            n = int(0.02 * Y.SR)
            c = Y.bp(Y.white(0.02, f"csic{i}{k}"), 1500, 12000)[:n] * np.exp(-np.arange(n) / (0.003 * Y.SR))
            Y.place(out, c, 0.05 + r.random() * 0.5 + k * 0.03, 0.3 + r.random() * 0.7)
        boom = np.sin(2 * np.pi * 90 * Y.t_axis(0.6)) * np.exp(-Y.t_axis(0.6) * 6) * 0.4
        Y.place(out, boom, 0.05)
        return Y.norm(Y.reverb(out, 2.5, 0.55, f"csic{i}"), 0.75)
    return f


def curtain_rustle():
    d = 2.0
    t = Y.t_axis(d)
    out = np.zeros(len(t))
    r = Y.rng("csrus")
    for k in range(10):
        n = int(0.09 * Y.SR)
        slap = Y.bp(Y.white(0.09, f"csr{k}"), 400, 5000)[:n] * np.exp(-np.arange(n) / (0.015 * Y.SR))
        Y.place(out, slap, 0.1 + k * 0.09 + r.random() * 0.06, 0.5 + r.random() * 0.5)
    return Y.norm(Y.reverb(out, 1.8, 0.5, "csrus"), 0.6)


def hook_creak(i):
    def f():
        d = 2.8
        t = Y.t_axis(d)
        f0 = 620 + i * 90 + 70 * np.sin(2 * np.pi * 0.7 * t)
        x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t + i)) ** 4
        x = x * (0.6 + 0.4 * (Y.rng(f"cshk{i}").random(len(t)) > 0.5))
        return Y.norm(Y.reverb(Y.bp(x, 300, 5000), 2.5, 0.6, f"cshk{i}"), 0.5)
    return f


def door_seal():
    d = 3.0
    t = Y.t_axis(d)
    out = np.zeros(len(t))
    Y.place(out, Y.impact("csdoor", 1.2, ring=0.2), 0.2)
    whoosh = Y.bp(Y.white(d, "csdw"), 200, 3000) * np.exp(-((t - 0.15) ** 2) / 0.01) * 0.6
    return Y.norm(Y.reverb(out + whoosh, 3.0, 0.6, "csdoor"), 0.7)


def wrapped_drop():
    d = 1.4
    t = Y.t_axis(d)
    crinkle = Y.bp(Y.white(d, "cswd"), 2000, 10000) * (Y.rng("cswd2").random(len(t)) > 0.7) * np.exp(-t * 3)
    out = np.zeros(len(t))
    Y.place(out, Y.impact("cswdi", 1.0), 0.25, 1.0)
    return Y.norm(crinkle * 0.6 + out, 0.95)


def wrapped_breath():
    d = 2.4
    t = Y.t_axis(d)
    env = np.clip(np.sin(np.pi * t / d), 0, None) ** 2
    x = Y.bp(Y.white(d, "cswb"), 300, 1500) * env
    crinkle = Y.bp(Y.white(d, "cswb2"), 3000, 9000) * (Y.rng("cswb3").random(len(t)) > 0.85) * env * 0.4
    return Y.norm(x + crinkle, 0.6)


def wrapped_hurt():
    d = 0.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "cswh"), 800, 6000) * np.exp(-t * 8) + Y.lp(Y.white(d, "cswh2"), 300) * np.exp(-t * 10)
    return Y.norm(x, 0.8)


sound("ambient.cold_storage", [("ambient/cold_storage", compressor)], stream=True)
sound("ambient.cold_storage.additions",
      [(f"ambient/cold_storage_ice{i}", ice_crack(i)) for i in range(2)] +
      [(f"ambient/cold_storage_hook{i}", hook_creak(i)) for i in range(2)] +
      [("ambient/cold_storage_curtain", curtain_rustle), ("ambient/cold_storage_door", door_seal)],
      subtitle="Something shifts in the cold")
sound("event.cold_storage_curtain", [("event/cold_storage_curtain", curtain_rustle)], subtitle="Plastic strips slap")
sound("entity.wrapped_one.drop", [("entity/wrapped_one_drop", wrapped_drop)], subtitle="Something drops from a hook")
sound("entity.wrapped_one.breath", [("entity/wrapped_one_breath", wrapped_breath)], subtitle="Breathing under plastic")
sound("entity.wrapped_one.hurt", [("entity/wrapped_one_hurt", wrapped_hurt)], subtitle="Plastic tears")

level("cold_storage", fog="#c7d4dc", sky="#c7d4dc", ambient="ambient.cold_storage",
      additions="ambient.cold_storage.additions", additions_chance=0.005, fixed_time=6000, height=48,
      particle="minecraft:snowflake", particle_chance=0.006)
