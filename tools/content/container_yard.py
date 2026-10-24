"""Container Yard - a maze of stacked shipping containers under sodium lights, at night, forever.
A few stand open and lit, with someone's things inside. Their doors do not always stay open."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, custom
from content.ind_kit import text, vstreaks, rust, steel, concrete, box_model, facing_states, rivets, glow_spot
import tex as T
import synth as Y

S = 32
COLORS = {
    "red": ("#7a2a1e", "#94382a"),
    "blue": ("#1e3a5e", "#2a4c74"),
    "green": ("#2a4a32", "#365c3e"),
    "grey": ("#5a5e60", "#6e7274"),
    "orange": ("#a0521e", "#b8642a"),
    "white": ("#b8b6ae", "#cecac0"),
}
BRANDS = {"blue": "HELIX", "red": "BRAMBLE", "green": "OCTAVE"}


def corrugated(color, seed, w=S, h=S, x_off=0):
    lo, hi = COLORS[color]
    yy, xx = np.mgrid[0:h, 0:w]
    rib = np.sin((xx + x_off) / 4.0 * np.pi)
    base = T.ramp(T.noise(seed, shape=(h, w), sigma=1.5) * 0.5 + 0.25 + rib * 0.25, lo, hi)
    base = T.shade(base, 1 + rib * 0.12)
    if w == S and h == S:
        # weathering only on block-sized tiles (big logo sheets get it per slice)
        base = rust(base, seed + "r", 0.28, "#4a2414", "#6a3418")
        base = vstreaks(base, seed + "v", "#3a2418", count=4, strength=0.35, length=(10, 30))
    dents = T.noise(seed + "d", shape=(h, w), sigma=3)
    return T.grain(T.shade(base, 0.92 + dents * 0.12), seed, 0.035)


def side(color):
    return lambda: corrugated(color, "cys" + color)


def roof(color):
    def f():
        lo, hi = COLORS[color]
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.ramp(T.noise("cyr" + color, sigma=2), lo, hi)
        img = T.shade(img, 1 - ((yy % 8) == 0) * 0.15)
        img = rust(img, "cyrr" + color, 0.5, "#4a2414", "#6a3418")
        return T.stains(img, "cyrs" + color, "#2a2420", 3, 0.4, 8)
    return f


def doors(color):
    """Container end doors: two leaves, four vertical locking bars, cam handles, a code stencil."""
    def f():
        img = corrugated(color, "cyd" + color)
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.shade(img, 1 - (np.abs(xx - 15.5) < 0.6) * 0.6)
        for bx in (5, 11, 20, 26):
            img = T.rect(img, bx, 0, bx + 2, 32, "#3a3a38")
            img[:, bx] = T.hexc("#6a6a66")
            img = T.rect(img, bx - 1, 14, bx + 3, 17, "#2a2a28")
        lab = {"red": "BRMU", "blue": "HLXU", "green": "OCTU", "grey": "GRYU", "orange": "TRCU", "white": "RFRU"}[color]
        img = text(img, lab, 7, 3, "#e8e4d8")
        return img
    return f


def logo_slice(color, brand, i, j, nx=4, ny=2):
    """Big painted brand name spanning nx x ny blocks of a container side."""
    def f():
        W, H = S * nx, S * ny
        big = corrugated(color, "cyl" + color, W, H)
        scale = min(6, (W - 8) // (len(brand) * 4))
        tw = len(brand) * 4 * scale - scale
        mask = np.zeros((H, W, 3))
        from content.ind_kit import text as tx
        mask = tx(mask, brand, (W - tw) // 2, (H - 5 * scale) // 2, "#ffffff", scale)
        m = mask[..., 0] * (T.noise("cylm", shape=(H, W), sigma=0.8) > 0.18)
        big = T.blend(big, T.solid("#e8e4d8", shape=(H, W)), m * 0.9)
        piece = big[(ny - 1 - j) * S:(ny - j) * S, i * S:(i + 1) * S]
        piece = rust(piece, f"cyl{color}{i}{j}", 0.25, "#4a2414", "#6a3418")
        return vstreaks(piece, f"cylv{color}{i}{j}", "#3a2418", 3, 0.3, (8, 24))
    return f


def interior():
    yy, xx = np.mgrid[0:S, 0:S]
    rib = np.sin(xx / 4.0 * np.pi)
    img = T.ramp(T.noise("cyin", sigma=1.5) * 0.5 + 0.25 + rib * 0.25, "#3a3430", "#4a423a")
    return rust(img, "cyinr", 0.5, "#4a2414", "#6a3418")


def ply():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("cyply", octaves=3, base=3), "#6a5038", "#80603e")
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.3)
    grain = np.sin(xx / 2.0 + T.noise("cyplyg", sigma=2) * 6) * 0.05
    return T.stains(T.shade(img, 1 + grain), "cyplys", "#3a2a1a", 2, 0.4, 7)


def asphalt(seed="cya", line=False, number=None):
    img = T.ramp(T.fbm(seed, octaves=4, base=3) * 0.6 + T.noise(seed + "n", sigma=0.5) * 0.4, "#26262a", "#36363a")
    agg = (T.rng(seed + "ag").random((S, S)) > 0.9).astype(float)
    img = T.shade(img, 1 + agg * 0.25)
    img = T.stains(img, seed + "oil", "#121214", 2, 0.5, 8)
    img = T.cracks(img, seed + "c", "#18181a", 1, 0.6)
    if line:
        yy, xx = np.mgrid[0:S, 0:S]
        paint = (np.abs(xx - 16) < 2) & (T.noise(seed + "pw", sigma=0.8) > 0.25)
        img = T.blend(img, T.solid("#c8a030"), paint.astype(float) * 0.85)
    if number:
        img = text(img, number, 16 - (len(number) * 8 - 2) // 2, 9, "#c8c0a0", 2)
    return T.grain(img, seed, 0.04)


def pole():
    return steel("cypole", "#4a4c4e", "#5e6062", brushed=True)


def sodium():
    img = T.ramp(T.noise("cysod", sigma=2), "#ffa040", "#ffd080")
    return T.border(img, "#2a2a2a", 3)


def reefer():
    img = T.ramp(T.noise("cyref", sigma=2), "#c8c6be", "#dad8d0")
    yy, xx = np.mgrid[0:S, 0:S]
    grille = (yy > 4) & (yy < 22) & (xx > 3) & (xx < 28) & ((yy % 3) == 0)
    img = T.blend(img, T.solid("#3a3a3a"), grille.astype(float))
    img = T.rect(img, 6, 24, 14, 29, "#101418")
    img = text(img, "-25", 7, 24, "#40c0ff")
    return vstreaks(img, "cyrefv", "#6a6458", 3, 0.3)


def gate():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = np.ones((S, S, 3)) * T.hexc("#8a8e90")
    a = (((xx + yy) % 8 == 0) | ((xx - yy) % 8 == 0)).astype(float)
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29) | (np.abs(yy - 16) < 1)
    a[frame] = 1
    rgb[frame] = T.hexc("#5a5e60")
    return rgb, a


def bulb():
    return T.ramp(T.noise("cybulb", sigma=2), "#ffd890", "#fff4d0")


def mattress():
    img = T.ramp(T.noise("cymat", sigma=1.5), "#8a8270", "#a09880")
    return T.stains(img, "cymats", "#5a4a30", 3, 0.5, 6)


tex("cy_asphalt", lambda: asphalt())
tex("cy_asphalt_line", lambda: asphalt("cyal", line=True))
for n in ("A7", "C12", "F3", "K9"):
    tex(f"cy_asphalt_{n.lower()}", (lambda nn: lambda: asphalt("cyan" + nn, number=nn))(n))
tex("cy_interior", interior)
tex("cy_ply", ply)
tex("cy_pole", pole)
tex("cy_sodium", sodium)
tex("cy_sodium_off", lambda: T.border(T.ramp(T.noise("cysodo", sigma=2), "#3a3028", "#4a3e32"), "#2a2a2a", 3))
tex("cy_reefer", reefer)
tex("cy_gate", gate)
tex("cy_bulb", bulb)
tex("cy_mattress", mattress)

for c in COLORS:
    tex(f"cy_side_{c}", side(c))
    tex(f"cy_roof_{c}", roof(c))
    tex(f"cy_doors_{c}", doors(c))
    cube_faces(f"cy_box_{c}", top=f"cy_roof_{c}", bottom="cy_interior", side=f"cy_side_{c}",
               display="Shipping Container", sound="metal")
    custom(f"cy_end_{c}", "com.noclip.block.FacingCubeBlock",
           {"parent": "minecraft:block/orientable", "textures": {"front": f"noclip:block/cy_doors_{c}",
                                                                 "side": f"noclip:block/cy_side_{c}",
                                                                 "top": f"noclip:block/cy_roof_{c}"}},
           blockstate=facing_states(lambda v, cc=c: f"cy_end_{cc}"), display="Container Doors", sound="metal",
           unbreakable=True)
for c, brand in BRANDS.items():
    for i in range(4):
        for j in range(2):
            tex(f"cy_logo_{c}_{i}{j}", logo_slice(c, brand, i, j))
            cube_faces(f"cy_logo_{c}_{i}{j}", top=f"cy_roof_{c}", bottom="cy_interior", side=f"cy_logo_{c}_{i}{j}",
                       display="Shipping Container", sound="metal")

custom("cy_reefer_unit", "com.noclip.block.FacingCubeBlock",
       {"parent": "minecraft:block/orientable", "textures": {"front": "noclip:block/cy_reefer",
                                                             "side": "noclip:block/cy_side_white",
                                                             "top": "noclip:block/cy_roof_white"}},
       blockstate=facing_states(lambda v: "cy_reefer_unit"), display="Reefer Unit", sound="metal",
       unbreakable=True, light=4)
exit_block("cy_reefer_door", "cy_doors_white", "SECRET", trigger="USE", solid=True, display="Reefer Doors",
           sound="metal")
cube("cy_asphalt", "cy_asphalt", display="Asphalt")
cube("cy_asphalt_line", "cy_asphalt_line", display="Bay Line")
for n in ("a7", "c12", "f3", "k9"):
    cube(f"cy_asphalt_{n}", f"cy_asphalt_{n}", display="Bay Number")
cube("cy_ply_floor", "cy_ply", display="Container Floor", sound="wood")
cube("cy_inner_wall", "cy_interior", display="Container Wall", sound="metal")
shaped("cy_mast", [(6, 0, 6, 10, 16, 10, "p")], {"p": "cy_pole"}, display="Light Mast", sound="metal",
       unbreakable=True)
shaped("cy_mast_head", [(6, 0, 6, 10, 10, 10, "p"), (0, 10, 4, 16, 13, 12, {"all": "p", "down": "l"}),
                        (4, 10, 0, 12, 13, 16, {"all": "p", "down": "l"})],
       {"p": "cy_pole", "l": "cy_sodium"}, display="Sodium Lamp", sound="metal", unbreakable=True, light=15,
       emissive=True)
shaped("cy_mast_head_dead", [(6, 0, 6, 10, 10, 10, "p"), (0, 10, 4, 16, 13, 12, {"all": "p", "down": "l"}),
                             (4, 10, 0, 12, 13, 16, {"all": "p", "down": "l"})],
       {"p": "cy_pole", "l": "cy_sodium_off"}, display="Dead Sodium Lamp", sound="metal", unbreakable=True)
cube("cy_slam_door", "cy_interior", display="Container Door (Inside)", sound="metal")
exit_block("cy_gate", "cy_gate", "DOOR", trigger="USE", solid=True, render="cutout", display="Yard Gate",
           sound="metal")
shaped("cy_bulb", [(6.5, 6, 6.5, 9.5, 10, 9.5, "b"), (7.5, 10, 7.5, 8.5, 16, 8.5, "p")],
       {"b": "cy_bulb", "p": "cy_pole"}, display="Bare Bulb", light=12, emissive=True, collides=False,
       unbreakable=True, sound="glass")
shaped("cy_mattress", [(0, 0, 1, 16, 4, 15, "m")], {"m": "cy_mattress"}, display="Mattress", sound="wool",
       unbreakable=True)

# ------------------------------------------------------------------ sound


def harbour():
    d = 28.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "cywind"), 120, 1400) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 14.0)) * 0.6
    far = Y.lp(Y.brown(d, "cyfar"), 90) * 0.7
    buzz = Y.hum(d, 60.0, "cybuzz", buzz=0.6, harmonics=(2, 4, 6)) * 0.05
    reefer_hum = Y.hum(d, 29.0, "cyrh", buzz=0.0, harmonics=(1, 2, 3)) * 0.06
    x = wind + far + Y.hp(buzz, 300) + Y.lp(reefer_hum, 300)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 2.0), 0.003), "stereo"


def foghorn():
    d = 7.0
    t = Y.t_axis(d)
    env = np.clip(t / 0.4, 0, 1) * np.clip((4.5 - t) / 0.8, 0, 1)
    x = sum(np.sin(2 * np.pi * f * t) * g for f, g in ((58, 1.0), (116, 0.6), (174, 0.3), (87, 0.4)))
    return Y.norm(Y.reverb(Y.lp(x * env, 400), 5.0, 0.8, "cyfog"), 0.6)


def beeping():
    d = 4.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * 1450 * t) * (((t % 0.8) < 0.4).astype(float))
    return Y.norm(Y.reverb(Y.lp(x, 1600) * 0.3, 3.0, 0.8, "cybeep"), 0.3)


def boom(i):
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        Y.place(out, Y.impact(f"cyboom{i}", 1.4, ring=0.6), 0.05)
        return Y.norm(Y.reverb(Y.lp(out, 1200), 4.0, 0.7, f"cyboom{i}"), 0.6)
    return f


def chains():
    d = 2.0
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("cych")
    for k in range(16):
        n = int(0.05 * Y.SR)
        link = Y.bp(Y.white(0.05, f"cych{k}"), 2500, 9000)[:n] * np.exp(-np.arange(n) / (0.008 * Y.SR))
        Y.place(out, link, 0.05 + k * 0.1 + r.random() * 0.04, 0.3 + r.random() * 0.4)
    return Y.norm(Y.reverb(out, 3.0, 0.7, "cych"), 0.4)


def slam():
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.impact("cyslam", 1.6, ring=1.0), 0.0, 1.0)
    Y.place(out, Y.impact("cylatch", 0.2, ring=0.4), 0.45, 0.7)
    Y.place(out, Y.impact("cylatch2", 0.2, ring=0.4), 0.6, 0.6)
    return Y.norm(Y.reverb(out, 2.0, 0.4, "cyslam"), 0.95)


def knock(i):
    def f():
        out = np.zeros(int(2.5 * Y.SR))
        for k in range(3 + i):
            Y.place(out, Y.impact(f"cykn{i}{k}", 0.5, ring=0.8), 0.1 + k * (0.32 - i * 0.04), 0.8)
        return Y.norm(Y.reverb(Y.lp(out, 2000), 1.5, 0.5, f"cykn{i}"), 0.9)
    return f


sound("ambient.container_yard", [("ambient/container_yard", harbour)], stream=True)
sound("ambient.container_yard.additions",
      [("ambient/container_yard_foghorn", foghorn), ("ambient/container_yard_beep", beeping),
       ("ambient/container_yard_chains", chains)] + [(f"ambient/container_yard_boom{i}", boom(i)) for i in range(2)],
      subtitle="The yard creaks")
sound("event.container_yard_slam", [("event/container_yard_slam", slam)], subtitle="Container doors slam")
sound("event.container_yard_knock", [(f"event/container_yard_knock{i}", knock(i)) for i in range(3)],
      subtitle="Knocking on steel")

level("container_yard", fog="#2a2016", sky="#120e0c", ambient="ambient.container_yard",
      additions="ambient.container_yard.additions", additions_chance=0.005, fixed_time=18000, skylight=True,
      ceiling=False, height=96)
