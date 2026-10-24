"""Water Treatment Plant - a hall of huge settling tanks full of dark green water, crossed by grated
catwalks. The air tastes of chlorine. Some of the grating is rusted through. Something big lives in the tanks."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, ladder, level, sound, custom, lang, \
    entity_model, entity_tex, part, ITEM_MODELS
from entity_paint import Canvas, skin
from content.ind_kit import text, vstreaks, rust, steel, concrete, grate, hazard_stripes, glow_spot
import tex as T
import synth as Y

S = 32


def deck():
    img = concrete("wtd", "#5a5e56", "#6a6e64", stain="#3a3e34", stains=3, crack=1)
    sheen = T.noise("wtds", sigma=3)
    return T.shade(img, 0.9 + sheen * 0.15)


def tank_wall(seed="wtw", band=False):
    img = concrete(seed, "#5e625a", "#70746a", stain="#3a3e30", stains=2)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.15)
    if band:
        line = 12 + (T.noise(seed + "l", sigma=2)[0] * 3).astype(int)
        below = yy > line[None, :]
        slime = T.ramp(T.noise(seed + "sl", sigma=1), "#1e3014", "#344a20")
        img = np.where(below[..., None], T.blend(img, slime, 0.75), img)
    return vstreaks(img, seed + "v", "#2a3a1a", 3, 0.35, (8, 26))


def rim():
    img = concrete("wtr", "#7a7e74", "#8a8e84", stains=1)
    yy, xx = np.mgrid[0:S, 0:S]
    edge = yy < 6
    img = np.where(edge[..., None], hazard_stripes(img, 0, 6, "wtrh", "#d0a820", "#d0a820", 8, 0.35), img)
    return img


def catwalk():
    return grate("wtg", "#4a5048", "#646c60", pitch=4, bar=1, rust_amt=0.15)


def catwalk_rust():
    rgb, a = grate("wtgr", "#4a3a2a", "#6a4a30", pitch=4, bar=1, rust_amt=0.9)
    holes = (T.noise("wtgrh", sigma=1.2) > 0.72)
    a = np.where(holes, 0, a)
    return rgb, a


def railing():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("wtrail", sigma=1), "#b89a20", "#d0b030")
    rgb = rust(rgb, "wtrailr", 0.25, "#5a3a14", "#7a4a1c")
    a = (((yy % 14) < 2) | (np.abs(xx - 2) < 2) | (np.abs(xx - 29) < 2)).astype(float)
    return rgb, a


def support():
    img = steel("wtsup", "#3a4a3a", "#4a5a48", brushed=False)
    img = rust(img, "wtsupr", 0.4, "#4a2a14", "#6a3a1c")
    return vstreaks(img, "wtsupv", "#1e2a14", 3, 0.4, (10, 30))


def pipe(color, label):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.ramp(T.noise("wtp" + label, sigma=1.5), *color)
        img = T.shade(img, 1 - (np.abs(yy - 16) / 18.0) ** 2 * 0.5)
        img = T.rect(img, 0, 13, 32, 19, "#e8e4d0")
        img = text(img, label, 2, 14, "#1a1a14")
        return rust(img, "wtpr" + label, 0.15)
    return f


def chem_tank():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("wtct", sigma=2), "#c8ccc0", "#dadcd2")
    img = T.shade(img, 1 - ((yy % 32) == 0) * 0.2)
    return vstreaks(img, "wtctv", "#8a9070", 3, 0.3, (8, 20))


def chem_label():
    img = chem_tank()
    yy, xx = np.mgrid[0:S, 0:S]
    dia = (np.abs(xx - 16) + np.abs(yy - 16)) < 11
    img = np.where(dia[..., None], T.solid("#e0c020"), img)
    inner = (np.abs(xx - 16) + np.abs(yy - 16)) < 9
    img = np.where(inner[..., None], T.solid("#f0e8c0"), img)
    img = text(img, "CL2", 10, 13, "#1a1a14")
    return img


def life_ring():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ang = np.degrees(np.arctan2(yy - 15.5, xx - 15.5)) % 90
    rgb = np.where((ang < 45)[..., None], T.solid("#e05a18"), T.solid("#e8e4d8"))
    a = ((d > 8) & (d < 14)).astype(float)
    return rgb, a


def sign(lines, col, bg):
    def f():
        img = deck()
        img = T.rect(img, 2, 6, 30, 26, bg)
        y = 16 - len(lines) * 4
        for ln in lines:
            img = text(img, ln, 16 - (len(ln) * 4 - 1) // 2, y, col)
            y += 7
        return img
    return f


def roof():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("wtroof", sigma=2), "#1a1e18", "#262a22")
    truss = (np.abs(xx - 16) < 2) | (np.abs(xx - yy) < 1.5) | (np.abs(xx + yy - 31) < 1.5)
    return T.blend(img, T.solid("#3a4034"), truss.astype(float))


def lamp():
    img = T.ramp(T.noise("wtlamp", sigma=2), "#d8f0d0", "#f0fff0")
    return T.border(T.border(img, "#5a6050", 3), "#3a4034", 1)


def outlet():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(T.noise("wtout", sigma=2), "#020402", "#0a100a")
    bars = ((xx % 6) < 2) & (d < 13)
    img = np.where(bars[..., None], T.solid("#2a3a22"), img)
    return np.where((d >= 13)[..., None], tank_wall("wtoutr", True), img)


def pump_door():
    img = steel("wtdoor", "#3a5040", "#4a6050", brushed=False)
    img = T.border(img, "#1e2a20", 2)
    img = T.rect(img, 22, 14, 27, 18, "#a8a090")
    img = T.rect(img, 4, 4, 28, 11, "#e8e4d0")
    img = text(img, "PUMPS", 6, 5, "#1a1a14")
    return rust(img, "wtdoorr", 0.3)


tex("wt_deck", deck)
tex("wt_tank_wall", lambda: tank_wall())
tex("wt_tank_band", lambda: tank_wall("wtwb", True))
tex("wt_rim", rim)
tex("wt_catwalk", catwalk)
tex("wt_catwalk_rust", catwalk_rust)
tex("wt_railing", railing)
tex("wt_support", support)
tex("wt_pipe_blue", pipe(("#2a4a6a", "#36587a"), "RAW"))
tex("wt_pipe_green", pipe(("#2a5a3a", "#366a46"), "FINAL"))
tex("wt_pipe_brown", pipe(("#5a3a22", "#6a4a2a"), "SLUDGE"))
tex("wt_chem", chem_tank)
tex("wt_chem_label", chem_label)
tex("wt_life_ring", life_ring)
tex("wt_sign_deep", sign(["DANGER", "DEEP", "WATER"], "#ffffff", "#b01818"))
tex("wt_sign_swim", sign(["NO", "SWIMMING"], "#b01818", "#e8e4d8"))
tex("wt_sign_count", sign(["DONT", "LOOK", "DOWN"], "#1a1a14", "#e0c020"))
tex("wt_roof", roof)
tex("wt_lamp", lamp)
tex("wt_outlet", outlet)
tex("wt_door", pump_door)

cube("wt_deck", "wt_deck", display="Wet Deck")
cube("wt_tank_wall", "wt_tank_wall", display="Tank Wall")
cube("wt_tank_band", "wt_tank_band", display="Tank Wall")
cube_faces("wt_rim", top="wt_rim", bottom="wt_tank_wall", side="wt_tank_wall", display="Tank Rim")
cube("wt_catwalk", "wt_catwalk", display="Catwalk Grating", sound="metal", render="cutout")
custom("wt_catwalk_rust", "com.noclip.block.CollapsingGrateBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/wt_catwalk_rust"}},
       display="Corroded Grating", sound="metal", unbreakable=True, render="cutout")
pane("wt_railing", "wt_railing", display="Safety Railing", sound="metal")
cube("wt_support", "wt_support", display="Catwalk Support", sound="metal")
for c in ("blue", "green", "brown"):
    shaped(f"wt_pipe_{c}", [(1, 1, 0, 15, 15, 16, f"p")], {"p": f"wt_pipe_{c}"}, display="Process Pipe",
           sound="metal", unbreakable=True)
cube_faces("wt_chem", top="wt_deck", bottom="wt_deck", side="wt_chem", display="Chemical Tank", sound="metal")
cube_faces("wt_chem_label", top="wt_deck", bottom="wt_deck", side="wt_chem_label", display="Chlorine Tank",
           sound="metal")
shaped("wt_life_ring", [(2, 2, 0, 14, 14, 1.5, {"all": "r"})], {"r": "wt_life_ring"}, display="Life Ring",
       collides=False, unbreakable=True, sound="wool")
for n in ("deep", "swim", "count"):
    cube(f"wt_sign_{n}", f"wt_sign_{n}", display="Safety Sign")
cube("wt_roof", "wt_roof", display="Roof Trusses", flat=True)
cube_faces("wt_lamp", bottom="wt_lamp", top="wt_roof", side="wt_roof", display="Mercury Lamp", light=15,
           emissive=True, flat=True, sound="glass")
exit_block("wt_outlet", "wt_outlet", "WATER", display="Sludge Outlet")
exit_block("wt_door", "wt_door", "DOOR", trigger="USE", solid=True, display="Pump House Door", sound="metal")
ladder("wt_ladder", "wt_support", display="Tank Ladder", unbreakable=True)

# ------------------------------------------------------------------ entity: the thing in the tank


def settler_parts():
    return [
        part("body", (0, 20, 0), cubes=[(0, 0, -6, -3, -14, 12, 6, 28)]),
        part("head", (0, 20, -14), cubes=[(0, 34, -5, -3, -10, 10, 5, 10)]),
        part("tail", (0, 20, 14), cubes=[(40, 34, -3, -2, 0, 6, 4, 18)]),
    ]


def settler_tex():
    c = Canvas(128, 64)
    hide = skin("wtse", "#0a0e0a", "#161e14", sigma=0.8, spots="#24301e")

    def face(w, h):
        img = hide(w, h)
        img[1, 1:3] = T.hexc("#c8d0b0")
        img[1, w - 3:w - 1] = T.hexc("#c8d0b0")
        return img

    c.box(0, 0, 12, 6, 28, all=hide)
    c.box(0, 34, 10, 5, 10, all=hide, front=face)
    c.box(40, 34, 6, 4, 18, all=hide)
    return c.out()


entity_model("tank_settler", settler_parts(), tex_size=(128, 64))
entity_tex("tank_settler", settler_tex)
lang("entity.noclip.tank_settler", "Something in the Tank")
lang("item.noclip.tank_settler_spawn_egg", "Tank Settler Spawn Egg")
ITEM_MODELS["tank_settler_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def hall():
    d = 26.0
    t = Y.t_axis(d)
    trickle = Y.bp(Y.pink(d, "wttr"), 400, 3500) * 0.3
    aerator = Y.bp(Y.white(d, "wtaer"), 150, 700) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 1.7 * t + np.sin(2 * np.pi * 0.2 * t)))) * 0.4
    pump = Y.hum(d, 30.0, "wtpump", buzz=0.0, harmonics=(1, 2, 3, 4)) * 0.1
    room = Y.lp(Y.brown(d, "wtroom"), 80) * 0.5
    x = trickle + aerator + Y.lp(pump, 300) + room
    return Y.stereoize(Y.loopable(Y.norm(Y.reverb(x, 3.0, 0.4, "wthall")[:len(t)], 0.45), 2.0), 0.002), "stereo"


def blorp(i):
    def f():
        d = 2.0
        t = Y.t_axis(d)
        f0 = 120 + i * 40 + 260 * np.exp(-t * 8)
        x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.exp(-t * 5) * (1 - np.exp(-t * 60))
        x += Y.bp(Y.white(d, f"wtbl{i}"), 300, 2000) * np.exp(-t * 9) * 0.4
        return Y.norm(Y.reverb(x, 3.0, 0.6, f"wtbl{i}"), 0.6)
    return f


def catwalk_creak():
    d = 2.2
    t = Y.t_axis(d)
    f0 = 300 + 80 * np.sin(2 * np.pi * 1.1 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (Y.rng("wtcc").random(len(t)) > 0.6) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(Y.bp(x, 150, 4000), 2.5, 0.6, "wtcc"), 0.55)


def wake():
    d = 4.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 2
    x = Y.lp(Y.brown(d, "wtwake"), 140) * env * 1.5 + Y.bp(Y.pink(d, "wtwake2"), 200, 1200) * env * 0.5
    return Y.norm(Y.reverb(x, 3.5, 0.6, "wtwake"), 0.7)


def beep():
    d = 3.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * 2200 * t) * ((t % 1.0) < 0.12) * 0.3
    return Y.norm(Y.reverb(x, 3.0, 0.7, "wtbeep"), 0.3)


def collapse():
    out = np.zeros(int(2.5 * Y.SR))
    Y.place(out, Y.impact("wtcol", 0.6, ring=0.9), 0.2, 1.0)
    t = Y.t_axis(0.25)
    creak = np.sin(2 * np.pi * np.cumsum(800 - 400 * t) / Y.SR) * 0.4
    Y.place(out, creak, 0.0, 0.8)
    return Y.norm(Y.reverb(out, 2.0, 0.5, "wtcol"), 0.9)


sound("ambient.water_treatment", [("ambient/water_treatment", hall)], stream=True)
sound("ambient.water_treatment.additions",
      [(f"ambient/water_treatment_blorp{i}", blorp(i)) for i in range(2)] +
      [("ambient/water_treatment_creak", catwalk_creak), ("ambient/water_treatment_beep", beep)],
      subtitle="The tanks bubble")
sound("event.water_treatment_creak", [("event/water_treatment_creak", catwalk_creak)], subtitle="Grating groans")
sound("event.water_treatment_collapse", [("event/water_treatment_collapse", collapse)], subtitle="Grating gives way")
sound("entity.tank_settler.wake", [("entity/tank_settler_wake", wake)], subtitle="Something large moves under water")

level("water_treatment", fog="#22301a", sky="#22301a", water="#2a4a1e", water_fog="#0e1a08",
      ambient="ambient.water_treatment", additions="ambient.water_treatment.additions", additions_chance=0.006,
      fixed_time=18000, height=64, particle="minecraft:mycelium", particle_chance=0.01)
